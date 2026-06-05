package com.arfipod.wearosplayground.transit

import com.arfipod.wearosplayground.examples.EmtBusArrival
import com.arfipod.wearosplayground.examples.EmtMadridClient
import com.arfipod.wearosplayground.examples.EmtMadridCredentials
import com.arfipod.wearosplayground.examples.MetroDeparturesResult
import com.arfipod.wearosplayground.examples.MetroGtfsSchedule
import com.arfipod.wearosplayground.examples.NapMetroClient

sealed interface MadridTransitLoadResult {
    val favorite: MadridTransitFavorite

    data class Metro(
        override val favorite: MadridTransitFavorite,
        val result: MetroDeparturesResult,
    ) : MadridTransitLoadResult

    data class Bus(
        override val favorite: MadridTransitFavorite,
        val arrivals: List<EmtBusArrival>,
    ) : MadridTransitLoadResult

    data class MissingConfig(
        override val favorite: MadridTransitFavorite,
        val message: String,
    ) : MadridTransitLoadResult

    data class Failed(
        override val favorite: MadridTransitFavorite,
        val message: String,
    ) : MadridTransitLoadResult
}

class MadridTransitRuntime(
    napApiKey: String,
    emtCredentials: EmtMadridCredentials,
    private val metroSchedule: MetroGtfsSchedule = MetroGtfsSchedule(),
) {
    private val metroClient = NapMetroClient(apiKey = napApiKey)
    private val emtClient = EmtMadridClient(credentials = emtCredentials)
    private val hasNapApiKey = napApiKey.isNotBlank()
    private val hasEmtCredentials = emtCredentials.hasAnyLogin
    private var metroGtfsZipBytes: ByteArray? = null

    suspend fun load(favorites: List<MadridTransitFavorite>): List<MadridTransitLoadResult> {
        return favorites.map { favorite ->
            runCatching { loadFavorite(favorite) }
                .getOrElse { error ->
                    MadridTransitLoadResult.Failed(
                        favorite = favorite,
                        message = error.message ?: "Unknown transport error",
                    )
                }
        }
    }

    private suspend fun loadFavorite(favorite: MadridTransitFavorite): MadridTransitLoadResult {
        return when (favorite.option.kind) {
            MadridTransitKind.METRO -> loadMetro(favorite)
            MadridTransitKind.BUS -> loadBus(favorite)
        }
    }

    private suspend fun loadMetro(favorite: MadridTransitFavorite): MadridTransitLoadResult {
        val target = favorite.option.metroTarget
            ?: return MadridTransitLoadResult.Failed(favorite, "Metro target missing")
        if (!hasNapApiKey) {
            return MadridTransitLoadResult.MissingConfig(favorite, "Set NAP_API_KEY")
        }

        val zipBytes = metroGtfsZipBytes ?: metroClient.fetchMetroGtfsZip()
            .also { metroGtfsZipBytes = it }
        return MadridTransitLoadResult.Metro(
            favorite = favorite,
            result = metroSchedule.nextDeparturesResult(
                gtfsZipBytes = zipBytes,
                target = target,
                count = favorite.clampedCount,
            ),
        )
    }

    private suspend fun loadBus(favorite: MadridTransitFavorite): MadridTransitLoadResult {
        val target = favorite.option.busTarget
            ?: return MadridTransitLoadResult.Failed(favorite, "Bus target missing")
        if (!hasEmtCredentials) {
            return MadridTransitLoadResult.MissingConfig(favorite, "Set EMT login")
        }

        return MadridTransitLoadResult.Bus(
            favorite = favorite,
            arrivals = emtClient.fetchArrivals(
                target = target,
                limit = favorite.clampedCount,
            ),
        )
    }
}
