package com.arfipod.wearosplayground.transit

import com.arfipod.wearosplayground.examples.EmtMadridStopTarget
import com.arfipod.wearosplayground.examples.MetroScheduleTarget
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class MadridTransitKind(val label: String) {
    METRO("Metro"),
    BUS("Bus"),
}

data class MadridGeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class MadridTransitOption(
    val id: String,
    val kind: MadridTransitKind,
    val label: String,
    val detail: String,
    val location: MadridGeoPoint?,
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
) {
    val clampedCount: Int
        get() = MadridTransitCounts.clamp(count)

    fun withCount(nextCount: Int): MadridTransitFavorite = copy(
        count = MadridTransitCounts.clamp(nextCount),
    )
}

object MadridTransitCounts {
    const val MIN = 1
    const val DEFAULT = 2
    const val MAX = 4

    fun clamp(count: Int): Int = count.coerceIn(MIN, MAX)
}

object MadridTransitCatalog {
    val metroOptions: List<MadridTransitOption> = listOf(
        MadridTransitOption(
            id = "metro_l4_arguelles_pinar",
            kind = MadridTransitKind.METRO,
            label = "Arguelles L4",
            detail = "Pinar de Chamartin",
            location = MadridGeoPoint(latitude = 40.4304, longitude = -3.7159),
            metroTarget = MetroScheduleTarget(
                label = "L4 Arguelles -> Pinar de Chamartin",
                stopNameQuery = "Argüelles",
                routeNameQuery = "4",
                destinationQuery = "Pinar de Chamartín",
            ),
        ),
        MadridTransitOption(
            id = "metro_l2_goya_las_rosas",
            kind = MadridTransitKind.METRO,
            label = "Goya L2",
            detail = "Las Rosas",
            location = MadridGeoPoint(latitude = 40.4247, longitude = -3.6757),
            metroTarget = MetroScheduleTarget(
                label = "L2 Goya -> Las Rosas",
                stopNameQuery = "Goya",
                routeNameQuery = "2",
                destinationQuery = "Las Rosas",
            ),
        ),
        MadridTransitOption(
            id = "metro_l1_sol_pinar",
            kind = MadridTransitKind.METRO,
            label = "Sol L1",
            detail = "Pinar de Chamartin",
            location = MadridGeoPoint(latitude = 40.4168, longitude = -3.7038),
            metroTarget = MetroScheduleTarget(
                label = "L1 Sol -> Pinar de Chamartin",
                stopNameQuery = "Sol",
                routeNameQuery = "1",
                destinationQuery = "Pinar de Chamartín",
            ),
        ),
    )

    val busOptions: List<MadridTransitOption> = listOf(
        MadridTransitOption(
            id = "bus_e3_felipe_valderrivas",
            kind = MadridTransitKind.BUS,
            label = "Felipe II E3",
            detail = "Valderrivas",
            location = MadridGeoPoint(latitude = 40.4244, longitude = -3.6754),
            busTarget = EmtMadridStopTarget(
                stopId = "755",
                label = "Felipe II E3 -> Valderrivas",
                lineId = "E3",
                destination = "VALDERRIVAS",
            ),
        ),
        MadridTransitOption(
            id = "bus_e3_daroca_valderrivas",
            kind = MadridTransitKind.BUS,
            label = "Daroca E3",
            detail = "Valderrivas",
            location = MadridGeoPoint(latitude = 40.421145, longitude = -3.669232),
            busTarget = EmtMadridStopTarget(
                stopId = "1064",
                label = "Avenida de Daroca E3 -> Valderrivas",
                lineId = "E3",
                destination = "VALDERRIVAS",
            ),
        ),
        MadridTransitOption(
            id = "bus_e3_valderrivas_felipe",
            kind = MadridTransitKind.BUS,
            label = "Valderrivas E3",
            detail = "Felipe II",
            location = MadridGeoPoint(latitude = 40.4008, longitude = -3.6043),
            busTarget = EmtMadridStopTarget(
                stopId = "5116",
                label = "Valderrivas E3 -> Felipe II",
                lineId = "E3",
                destination = "FELIPE II",
            ),
        ),
    )

    val allOptions: List<MadridTransitOption> = metroOptions + busOptions

    val defaultFavorites: List<MadridTransitFavorite> = listOf(
        favoriteFor("metro_l4_arguelles_pinar"),
        favoriteFor("bus_e3_daroca_valderrivas"),
    ).filterNotNull()

    fun optionById(id: String): MadridTransitOption? = allOptions.firstOrNull { it.id == id }

    fun favoriteFor(
        optionId: String,
        count: Int = MadridTransitCounts.DEFAULT,
    ): MadridTransitFavorite? = optionById(optionId)?.let { option ->
        MadridTransitFavorite(
            option = option,
            count = MadridTransitCounts.clamp(count),
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
