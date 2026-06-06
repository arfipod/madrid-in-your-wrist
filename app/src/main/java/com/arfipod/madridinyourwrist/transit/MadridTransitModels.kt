package com.arfipod.madridinyourwrist.transit

import com.arfipod.madridinyourwrist.examples.EmtMadridStopTarget
import com.arfipod.madridinyourwrist.examples.MetroScheduleTarget
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class MadridTransitKind(val label: String) {
    METRO("Metro"),
    BUS("Bus"),
}

enum class MadridTransitSource(
    val label: String,
    val hasLiveArrivals: Boolean,
) {
    METRO_NAP(label = "Metro GTFS/NAP", hasLiveArrivals = true),
    EMT_OPENAPI(label = "EMT OpenAPI", hasLiveArrivals = true),
    CRTM_STATIC_GTFS(label = "Solo catálogo CRTM", hasLiveArrivals = false),
    ;

    companion object {
        fun defaultFor(kind: MadridTransitKind): MadridTransitSource = when (kind) {
            MadridTransitKind.METRO -> METRO_NAP
            MadridTransitKind.BUS -> EMT_OPENAPI
        }
    }
}

enum class MadridTransitPlace(
    val id: String,
    val label: String,
    val shortLabel: String,
) {
    PROFILE_1(id = "profile_1", label = "Perfil 1", shortLabel = "P1"),
    PROFILE_2(id = "profile_2", label = "Perfil 2", shortLabel = "P2"),
    PROFILE_3(id = "profile_3", label = "Perfil 3", shortLabel = "P3"),
    ;

    companion object {
        val DEFAULT: MadridTransitPlace = PROFILE_1
        val selectable: List<MadridTransitPlace> = entries.toList()

        fun fromId(id: String?): MadridTransitPlace? = entries.firstOrNull { place -> place.id == id }
    }
}

data class MadridGeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class MadridTransitOption(
    val id: String,
    val kind: MadridTransitKind,
    val source: MadridTransitSource = MadridTransitSource.defaultFor(kind),
    val label: String,
    val detail: String,
    val location: MadridGeoPoint?,
    val searchAliases: List<String> = emptyList(),
    val metroTarget: MetroScheduleTarget? = null,
    val busTarget: EmtMadridStopTarget? = null,
) {
    init {
        require((metroTarget == null) != (busTarget == null)) {
            "Transit option must point to exactly one transport target."
        }
    }
}

data class MadridTransitFavorite(
    val option: MadridTransitOption,
    val count: Int,
    val place: MadridTransitPlace = MadridTransitPlace.DEFAULT,
    val proximityTriggerMeters: Int? = null,
) {
    val clampedCount: Int
        get() = MadridTransitCounts.clamp(count)

    val normalizedProximityTriggerMeters: Int?
        get() = MadridTransitProximity.normalize(proximityTriggerMeters)

    fun withCount(nextCount: Int): MadridTransitFavorite = copy(
        count = MadridTransitCounts.clamp(nextCount),
    )

    fun withPlace(nextPlace: MadridTransitPlace): MadridTransitFavorite = copy(place = nextPlace)

    fun withNextProximityTrigger(): MadridTransitFavorite = copy(
        proximityTriggerMeters = MadridTransitProximity.next(normalizedProximityTriggerMeters),
    )

    fun isWithinProximityTrigger(distanceMeters: Int?): Boolean {
        val triggerMeters = normalizedProximityTriggerMeters ?: return true
        return distanceMeters != null && distanceMeters <= triggerMeters
    }
}

object MadridTransitCounts {
    const val MIN = 1
    const val DEFAULT = 2
    const val MAX = 4

    fun clamp(count: Int): Int = count.coerceIn(MIN, MAX)
}

object MadridTransitProximity {
    val RADIUS_OPTIONS_METERS: List<Int?> = listOf(null, 500, 1000, 2000)

    fun normalize(meters: Int?): Int? = meters?.takeIf { value ->
        RADIUS_OPTIONS_METERS.filterNotNull().contains(value)
    }

    fun next(currentMeters: Int?): Int? {
        val currentIndex = RADIUS_OPTIONS_METERS.indexOf(normalize(currentMeters))
            .takeIf { index -> index >= 0 }
            ?: 0
        return RADIUS_OPTIONS_METERS[(currentIndex + 1) % RADIUS_OPTIONS_METERS.size]
    }

    fun label(meters: Int?): String = when (meters) {
        null -> "Manual"
        500 -> "Auto 500m"
        1000 -> "Auto 1km"
        2000 -> "Auto 2km"
        else -> "Manual"
    }
}

object MadridTransitCatalog {
    val metroOptions: List<MadridTransitOption> by lazy {
        MadridGeneratedTransitCatalog.options.filter { option -> option.kind == MadridTransitKind.METRO }
    }

    val busOptions: List<MadridTransitOption> by lazy {
        MadridGeneratedTransitCatalog.options.filter { option -> option.kind == MadridTransitKind.BUS }
    }

    val allOptions: List<MadridTransitOption> by lazy { metroOptions + busOptions }

    val defaultFavorites: List<MadridTransitFavorite> by lazy {
        listOfNotNull(
            favoriteFor("metro_4_54_pinar_de_chamartin", place = MadridTransitPlace.PROFILE_1),
            favoriteFor("bus_emt_e3_1064_valderrivas", place = MadridTransitPlace.PROFILE_1),
        )
    }

    fun optionById(id: String): MadridTransitOption? = MadridGeneratedTransitCatalog.optionById(id)

    fun favoriteFor(
        optionId: String,
        count: Int = MadridTransitCounts.DEFAULT,
        place: MadridTransitPlace = MadridTransitPlace.DEFAULT,
        proximityTriggerMeters: Int? = null,
    ): MadridTransitFavorite? = optionById(optionId)?.let { option ->
        MadridTransitFavorite(
            option = option,
            count = MadridTransitCounts.clamp(count),
            place = place,
            proximityTriggerMeters = MadridTransitProximity.normalize(proximityTriggerMeters),
        )
    }

    fun nearbyOptions(
        from: MadridGeoPoint,
        kind: MadridTransitKind? = null,
        limit: Int = 4,
    ): List<Pair<MadridTransitOption, Int>> {
        return allOptions
            .asSequence()
            .filter { kind == null || it.kind == kind }
            .mapNotNull { option ->
                val location = option.location ?: return@mapNotNull null
                option to distanceMeters(from = from, to = location)
            }
            .sortedBy { it.second }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    fun searchOptions(
        query: String,
        kind: MadridTransitKind? = null,
        limit: Int = 8,
    ): List<MadridTransitOption> {
        return MadridTransitSearch.search(
            options = allOptions.filter { option -> kind == null || option.kind == kind },
            query = query,
            limit = limit,
        )
    }

}

fun distanceMeters(from: MadridGeoPoint, to: MadridGeoPoint): Int {
    val earthRadiusMeters = 6_371_000.0
    val fromLat = Math.toRadians(from.latitude)
    val toLat = Math.toRadians(to.latitude)
    val deltaLat = Math.toRadians(to.latitude - from.latitude)
    val deltaLon = Math.toRadians(to.longitude - from.longitude)

    val a = sin(deltaLat / 2).pow(2) +
        cos(fromLat) * cos(toLat) * sin(deltaLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadiusMeters * c).toInt()
}
