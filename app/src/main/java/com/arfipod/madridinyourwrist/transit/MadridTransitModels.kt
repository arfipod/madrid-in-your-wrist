package com.arfipod.madridinyourwrist.transit
import java.util.PriorityQueue
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
    val customName: String? = null,
    val customIcon: String? = null,
) {
    val clampedCount: Int
        get() = MadridTransitCounts.clamp(count)

    val normalizedProximityTriggerMeters: Int?
        get() = MadridTransitProximity.normalize(proximityTriggerMeters)

    val normalizedCustomName: String?
        get() = MadridTransitFavoriteCustomization.normalizeName(customName)

    val normalizedCustomIcon: String?
        get() = MadridTransitFavoriteCustomization.normalizeIcon(customIcon)

    fun withCount(nextCount: Int): MadridTransitFavorite = copy(
        count = MadridTransitCounts.clamp(nextCount),
    )

    fun withPlace(nextPlace: MadridTransitPlace): MadridTransitFavorite = copy(place = nextPlace)

    fun withNextProximityTrigger(): MadridTransitFavorite = copy(
        proximityTriggerMeters = MadridTransitProximity.next(normalizedProximityTriggerMeters),
    )

    fun withCustomization(
        name: String?,
        icon: String?,
    ): MadridTransitFavorite = copy(
        customName = MadridTransitFavoriteCustomization.normalizeName(name),
        customIcon = MadridTransitFavoriteCustomization.normalizeIcon(icon),
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

object MadridTransitFavoriteCustomization {
    const val NAME_MAX_CHARS = 28
    const val ICON_MAX_CODE_POINTS = 4

    fun normalizeName(name: String?): String? {
        return name
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.take(NAME_MAX_CHARS)
            ?.takeIf { value -> value.isNotBlank() }
    }

    fun normalizeIcon(icon: String?): String? {
        return icon
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.takeCodePoints(ICON_MAX_CODE_POINTS)
            ?.takeIf { value -> value.isNotBlank() }
    }

    private fun String.takeCodePoints(maxCodePoints: Int): String {
        if (codePointCount(0, length) <= maxCodePoints) return this
        return substring(0, offsetByCodePoints(0, maxCodePoints))
    }
}

object MadridTransitCatalog {
    val metroOptions: List<MadridTransitOption> by lazy {
        MadridGeneratedTransitCatalog.metroOptions
    }

    val busOptions: List<MadridTransitOption> by lazy {
        MadridGeneratedTransitCatalog.busOptions
    }

    val allOptions: List<MadridTransitOption> by lazy { MadridGeneratedTransitCatalog.options }

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
        customName: String? = null,
        customIcon: String? = null,
    ): MadridTransitFavorite? = optionById(optionId)?.let { option ->
        MadridTransitFavorite(
            option = option,
            count = MadridTransitCounts.clamp(count),
            place = place,
            proximityTriggerMeters = MadridTransitProximity.normalize(proximityTriggerMeters),
            customName = MadridTransitFavoriteCustomization.normalizeName(customName),
            customIcon = MadridTransitFavoriteCustomization.normalizeIcon(customIcon),
        )
    }

    fun nearbyOptions(
        from: MadridGeoPoint,
        kind: MadridTransitKind? = null,
        limit: Int = 4,
    ): List<Pair<MadridTransitOption, Int>> {
        val resultLimit = limit.coerceAtLeast(1)
        val nearest = PriorityQueue<Pair<MadridTransitOption, Int>>(
            compareByDescending<Pair<MadridTransitOption, Int>> { (_, meters) -> meters }
                .thenByDescending { (option, _) -> option.label }
                .thenByDescending { (option, _) -> option.id }
        )

        optionsForKind(kind).forEach { option ->
            val location = option.location ?: return@forEach
            val candidate = option to distanceMeters(from = from, to = location)
            if (nearest.size < resultLimit) {
                nearest += candidate
            } else if (NEARER_FIRST.compare(candidate, nearest.peek()) < 0) {
                nearest.poll()
                nearest += candidate
            }
        }

        return nearest.toList().sortedWith(NEARER_FIRST)
    }

    fun searchOptions(
        query: String,
        kind: MadridTransitKind? = null,
        limit: Int = 8,
    ): List<MadridTransitOption> {
        return MadridTransitSearch.search(
            options = optionsForKind(kind),
            query = query,
            limit = limit,
        )
    }

    private fun optionsForKind(kind: MadridTransitKind?): List<MadridTransitOption> = when (kind) {
        MadridTransitKind.METRO -> metroOptions
        MadridTransitKind.BUS -> busOptions
        null -> allOptions
    }

    private val NEARER_FIRST = compareBy<Pair<MadridTransitOption, Int>> { (_, meters) -> meters }
        .thenBy { (option, _) -> option.label }
        .thenBy { (option, _) -> option.id }
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
