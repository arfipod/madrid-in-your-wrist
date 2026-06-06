package com.arfipod.madridinyourwrist.transit

import com.arfipod.madridinyourwrist.examples.EmtBusArrival
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MadridTransitRefreshPolicyTest {
    @Test
    fun clearSelectionRemovesOnlySelectedProfileFromSavedSnapshot() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = "selected", rankMinutes = 2),
                item(place = MadridTransitPlace.PROFILE_2, optionId = "other", rankMinutes = 4),
            ),
        )

        val decision = MadridTransitRefreshPolicy.clearSelection(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            updatedAt = "08:15",
        )

        assertEquals(MadridTransitRefreshSource.EMPTY_SELECTION, decision.source)
        assertEquals(listOf("other"), decision.snapshot?.items?.map { item -> item.optionId })
        assertEquals(listOf("other"), decision.snapshotToSave?.items?.map { item -> item.optionId })
        assertEquals("08:15", decision.snapshotToSave?.updatedAt)
    }

    @Test
    fun offlineUsesCacheWhenSelectedProfileHasSnapshotItems() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 4)),
            storedAtEpochMillis = 1_000L,
        )

        val decision = MadridTransitRefreshPolicy.offline(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID),
        )

        assertEquals(MadridTransitRefreshSource.OFFLINE_CACHE, decision.source)
        assertEquals(previous, decision.snapshot)
        assertNull(decision.snapshotToSave)
    }

    @Test
    fun automaticCacheUsesRecentCompleteSnapshotWithoutSavingAgain() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 4),
                item(place = MadridTransitPlace.PROFILE_1, optionId = METRO_ID, rankMinutes = 2),
            ),
            storedAtEpochMillis = 1_000L,
        )

        val decision = MadridTransitRefreshPolicy.automaticCache(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID, METRO_ID),
            nowEpochMillis = 1_000L + MadridTransitRefreshPolicy.AUTO_REFRESH_CACHE_MAX_AGE_MILLIS,
        )

        val actual = requireNotNull(decision)
        assertEquals(MadridTransitRefreshSource.AUTO_CACHE, actual.source)
        assertEquals(previous, actual.snapshot)
        assertNull(actual.snapshotToSave)
    }

    @Test
    fun automaticCacheRequiresFreshCompleteSnapshot() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 4)),
            storedAtEpochMillis = 1_000L,
        )

        assertNull(
            MadridTransitRefreshPolicy.automaticCache(
                previous = previous,
                place = MadridTransitPlace.PROFILE_1,
                optionIds = setOf(BUS_ID),
                nowEpochMillis = 1_000L + MadridTransitRefreshPolicy.AUTO_REFRESH_CACHE_MAX_AGE_MILLIS + 1,
            )
        )
        assertNull(
            MadridTransitRefreshPolicy.automaticCache(
                previous = previous,
                place = MadridTransitPlace.PROFILE_1,
                optionIds = setOf(BUS_ID, METRO_ID),
                nowEpochMillis = 1_000L,
            )
        )
    }

    @Test
    fun distanceSkippedUsesCacheWhenAvailableWithoutSavingAgain() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 4)),
            storedAtEpochMillis = 1_000L,
        )

        val decision = MadridTransitRefreshPolicy.distanceSkipped(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID),
        )

        assertEquals(MadridTransitRefreshSource.DISTANCE_SKIPPED_CACHE, decision.source)
        assertEquals(previous, decision.snapshot)
        assertNull(decision.snapshotToSave)
    }

    @Test
    fun offlineKeepsExistingSnapshotButReportsEmptyWhenSelectedProfileHasNoCache() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_2, optionId = BUS_ID, rankMinutes = 4)),
        )

        val decision = MadridTransitRefreshPolicy.offline(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID),
        )

        assertEquals(MadridTransitRefreshSource.OFFLINE_EMPTY, decision.source)
        assertEquals(previous, decision.snapshot)
        assertNull(decision.snapshotToSave)
    }

    @Test
    fun onlineSuccessPersistsLiveRefreshAndKeepsFailedFavoriteCache() {
        val busFavorite = favorite(BUS_ID, MadridTransitPlace.PROFILE_1)
        val metroFavorite = favorite(METRO_ID, MadridTransitPlace.PROFILE_1)
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 9),
                item(place = MadridTransitPlace.PROFILE_1, optionId = METRO_ID, rankMinutes = 4),
                item(place = MadridTransitPlace.PROFILE_2, optionId = "other", rankMinutes = 1),
            ),
        )

        val decision = MadridTransitRefreshPolicy.onlineSuccess(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID, METRO_ID),
            updatedAt = "08:15",
            results = listOf(
                MadridTransitLoadResult.Bus(
                    favorite = busFavorite,
                    arrivals = listOf(
                        EmtBusArrival(
                            lineId = "E3",
                            destination = "VALDERRIVAS",
                            secondsUntil = 2 * 60,
                            metersAway = 90,
                        )
                    ),
                ),
                MadridTransitLoadResult.Failed(
                    favorite = metroFavorite,
                    message = "Metro unavailable",
                ),
            ),
        )

        val saved = requireNotNull(decision.snapshotToSave)
        assertEquals(MadridTransitRefreshSource.ONLINE_LIVE, decision.source)
        assertEquals("08:15", saved.updatedAt)
        assertEquals(listOf(BUS_ID, METRO_ID, "other"), saved.items.map { item -> item.optionId })
        assertEquals(2, saved.items.first { item -> item.optionId == BUS_ID }.rankMinutes)
        assertEquals(4, saved.items.first { item -> item.optionId == METRO_ID }.rankMinutes)
    }

    @Test
    fun onlineSuccessWithOnlyFailedResultsFallsBackToCache() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = BUS_ID, rankMinutes = 4)),
        )

        val decision = MadridTransitRefreshPolicy.onlineSuccess(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID),
            updatedAt = "08:15",
            results = listOf(
                MadridTransitLoadResult.Failed(
                    favorite = favorite(BUS_ID, MadridTransitPlace.PROFILE_1),
                    message = "Bus unavailable",
                )
            ),
        )

        assertEquals(MadridTransitRefreshSource.ONLINE_CACHE_FALLBACK, decision.source)
        assertEquals(previous, decision.snapshot)
        assertNull(decision.snapshotToSave)
    }

    @Test
    fun onlineFailureWithoutSelectedCacheReportsOnlineEmpty() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(item(place = MadridTransitPlace.PROFILE_2, optionId = BUS_ID, rankMinutes = 4)),
        )

        val decision = MadridTransitRefreshPolicy.onlineFailure(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            optionIds = setOf(BUS_ID),
        )

        assertEquals(MadridTransitRefreshSource.ONLINE_EMPTY, decision.source)
        assertEquals(previous, decision.snapshot)
        assertNull(decision.snapshotToSave)
    }

    private fun favorite(
        optionId: String,
        place: MadridTransitPlace,
    ): MadridTransitFavorite {
        return requireNotNull(MadridTransitCatalog.favoriteFor(optionId = optionId, place = place))
    }

    private fun item(
        place: MadridTransitPlace,
        optionId: String,
        rankMinutes: Int,
    ): MadridTransitSnapshotItem = MadridTransitSnapshotItem(
        optionId = optionId,
        kind = MadridTransitKind.BUS,
        place = place,
        optionLabel = optionId,
        detail = "Detail",
        routeLabel = "E3",
        destination = "VALDERRIVAS",
        timeLabel = "${rankMinutes}m",
        rankMinutes = rankMinutes,
    )

    private companion object {
        const val BUS_ID = "bus_e3_daroca_valderrivas"
        const val METRO_ID = "metro_l4_arguelles_pinar"
    }
}
