package com.arfipod.wearosplayground.transit

import android.content.Context
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

private const val KEY_SNAPSHOT = "last_snapshot"
private const val SNAPSHOT_VERSION = "v1"
private const val FIELD_SEPARATOR = "\t"
private const val LINE_SEPARATOR = "\n"

data class MadridTransitSnapshot(
    val updatedAt: String,
    val items: List<MadridTransitSnapshotItem>,
) {
    val headline: MadridTransitSnapshotItem?
        get() = items.minWithOrNull(
            compareBy<MadridTransitSnapshotItem> { item -> item.rankMinutes }
                .thenBy { item -> item.optionLabel }
        )

    fun forPlace(place: MadridTransitPlace): MadridTransitSnapshot = copy(
        items = items.filter { item -> item.place == place },
    )
}

data class MadridTransitSnapshotItem(
    val optionId: String,
    val kind: MadridTransitKind,
    val place: MadridTransitPlace,
    val optionLabel: String,
    val detail: String,
    val routeLabel: String,
    val destination: String,
    val timeLabel: String,
    val rankMinutes: Int,
)

class MadridTransitSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences(MADRID_TRANSIT_PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSnapshot(): MadridTransitSnapshot? {
        return MadridTransitSnapshotCodec.decode(preferences.getString(KEY_SNAPSHOT, null))
    }

    fun saveSnapshot(snapshot: MadridTransitSnapshot) {
        val editor = preferences.edit()
        if (snapshot.updatedAt.isBlank() || snapshot.items.isEmpty()) {
            editor.remove(KEY_SNAPSHOT)
        } else {
            editor.putString(KEY_SNAPSHOT, MadridTransitSnapshotCodec.encode(snapshot))
        }
        editor.apply()
    }
}

object MadridTransitSnapshots {
    fun fromResults(
        results: List<MadridTransitLoadResult>,
        updatedAt: String,
    ): MadridTransitSnapshot {
        return MadridTransitSnapshot(
            updatedAt = updatedAt,
            items = sortItems(results.mapNotNull { result -> result.toSnapshotItem() }),
        )
    }

    fun mergePlace(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        updatedAt: String,
        replacementItems: List<MadridTransitSnapshotItem>,
    ): MadridTransitSnapshot {
        return MadridTransitSnapshot(
            updatedAt = updatedAt,
            items = sortItems(
                previous.orEmptyItems().filterNot { item -> item.place == place } + replacementItems,
            ),
        )
    }

    fun mergeRefresh(
        previous: MadridTransitSnapshot?,
        place: MadridTransitPlace,
        updatedAt: String,
        refreshedOptionIds: Set<String>,
        replacementItems: List<MadridTransitSnapshotItem>,
    ): MadridTransitSnapshot {
        if (refreshedOptionIds.isEmpty()) {
            return previous ?: MadridTransitSnapshot(updatedAt = updatedAt, items = emptyList())
        }

        return MadridTransitSnapshot(
            updatedAt = updatedAt,
            items = sortItems(
                previous.orEmptyItems().filterNot { item ->
                    item.place == place && item.optionId in refreshedOptionIds
                } + replacementItems,
            ),
        )
    }

    private fun MadridTransitSnapshot?.orEmptyItems(): List<MadridTransitSnapshotItem> = this?.items.orEmpty()

    private fun sortItems(items: List<MadridTransitSnapshotItem>): List<MadridTransitSnapshotItem> {
        return items.sortedWith(
            compareBy<MadridTransitSnapshotItem> { item -> item.place.ordinal }
                .thenBy { item -> item.rankMinutes }
                .thenBy { item -> item.optionLabel }
        )
    }
}

object MadridTransitSnapshotCodec {
    fun encode(snapshot: MadridTransitSnapshot): String {
        val header = listOf(SNAPSHOT_VERSION, snapshot.updatedAt.encoded()).joinToString(FIELD_SEPARATOR)
        val items = snapshot.items.map { item ->
            listOf(
                item.optionId,
                item.kind.name,
                item.place.id,
                item.optionLabel,
                item.detail,
                item.routeLabel,
                item.destination,
                item.timeLabel,
                item.rankMinutes.toString(),
            ).joinToString(FIELD_SEPARATOR) { field -> field.encoded() }
        }
        return (listOf(header) + items).joinToString(LINE_SEPARATOR)
    }

    fun decode(raw: String?): MadridTransitSnapshot? {
        if (raw.isNullOrBlank()) return null

        val lines = raw.lineSequence().filter { line -> line.isNotBlank() }.toList()
        val headerParts = lines.firstOrNull()?.split(FIELD_SEPARATOR).orEmpty()
        if (headerParts.getOrNull(0) != SNAPSHOT_VERSION) return null
        val updatedAt = headerParts.getOrNull(1)?.decoded().orEmpty()
        if (updatedAt.isBlank()) return null

        val items = lines.drop(1).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR).map { part -> part.decoded() }
            val kind = parts.getOrNull(1)?.let { value ->
                runCatching { MadridTransitKind.valueOf(value) }.getOrNull()
            } ?: return@mapNotNull null
            val place = MadridTransitPlace.fromId(parts.getOrNull(2)) ?: MadridTransitPlace.DEFAULT
            val rank = parts.getOrNull(8)?.toIntOrNull() ?: return@mapNotNull null
            MadridTransitSnapshotItem(
                optionId = parts.getOrNull(0).orEmpty(),
                kind = kind,
                place = place,
                optionLabel = parts.getOrNull(3).orEmpty(),
                detail = parts.getOrNull(4).orEmpty(),
                routeLabel = parts.getOrNull(5).orEmpty(),
                destination = parts.getOrNull(6).orEmpty(),
                timeLabel = parts.getOrNull(7).orEmpty(),
                rankMinutes = rank,
            )
        }

        return MadridTransitSnapshot(updatedAt = updatedAt, items = items)
    }
}

internal fun MadridTransitLoadResult.toSnapshotItem(): MadridTransitSnapshotItem? = when (this) {
    is MadridTransitLoadResult.Metro -> result.departures.firstOrNull()?.let { departure ->
        MadridTransitSnapshotItem(
            optionId = favorite.option.id,
            kind = MadridTransitKind.METRO,
            place = favorite.place,
            optionLabel = favorite.option.label,
            detail = favorite.option.detail,
            routeLabel = departure.routeName.madridTransitShortRoute(),
            destination = departure.destination,
            timeLabel = departure.minutesUntil.madridTransitMinuteLabel(),
            rankMinutes = departure.minutesUntil.coerceAtLeast(0L).toInt(),
        )
    }

    is MadridTransitLoadResult.Bus -> arrivals.firstOrNull()?.let { arrival ->
        MadridTransitSnapshotItem(
            optionId = favorite.option.id,
            kind = MadridTransitKind.BUS,
            place = favorite.place,
            optionLabel = favorite.option.label,
            detail = favorite.option.detail,
            routeLabel = arrival.lineId.uppercase(Locale.ROOT),
            destination = arrival.destination,
            timeLabel = arrival.secondsUntil.madridTransitBusMinuteLabel(),
            rankMinutes = arrival.secondsUntil.madridTransitBusRankMinutes(),
        )
    }

    is MadridTransitLoadResult.MissingConfig -> null
    is MadridTransitLoadResult.Failed -> null
}

internal fun MadridTransitLoadResult.refreshedOptionId(): String? = when (this) {
    is MadridTransitLoadResult.Metro -> favorite.option.id
    is MadridTransitLoadResult.Bus -> favorite.option.id
    is MadridTransitLoadResult.MissingConfig -> null
    is MadridTransitLoadResult.Failed -> null
}

internal fun Long.madridTransitMinuteLabel(): String = if (this <= 0L) "Ahora" else "${this}m"

internal fun Int.madridTransitBusMinuteLabel(): String = when {
    this == 0 -> "Ahora"
    this >= 999_999 -> "+20m"
    else -> "${(this + 59) / 60}m"
}

internal fun Int.madridTransitBusRankMinutes(): Int = when {
    this == 0 -> 0
    this >= 999_999 -> 21
    else -> (this + 59) / 60
}

internal fun String.madridTransitShortRoute(): String {
    val firstToken = trim().substringBefore(" ").uppercase(Locale.ROOT)
    return if (firstToken.startsWith("L")) firstToken else "L$firstToken"
}

private fun String.encoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

private fun String.decoded(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())
