package com.arfipod.madridinyourwrist.transit

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MadridTransitSnapshotTest {
    @Test
    fun headlineChoosesSoonestTransitAcrossLoadedResults() {
        val metroFavorite = requireNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "metro_4_54_pinar_de_chamartin",
                place = MadridTransitPlace.PROFILE_1,
            )
        )
        val busFavorite = requireNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "bus_emt_e3_1064_valderrivas",
                place = MadridTransitPlace.PROFILE_1,
            )
        )
        val snapshot = MadridTransitSnapshots.fromResults(
            updatedAt = "08:15",
            results = listOf(
                MadridTransitLoadResult.Bus(
                    favorite = busFavorite,
                    arrivals = listOf(
                        EmtBusArrival(
                            lineId = "E3",
                            destination = "VALDERRIVAS",
                            secondsUntil = 4 * 60,
                            metersAway = 90,
                        )
                    ),
                ),
                MadridTransitLoadResult.Metro(
                    favorite = metroFavorite,
                    result = MetroDeparturesResult(
                        departures = listOf(
                            MetroDeparture(
                                stopName = "Argüelles",
                                routeName = "4",
                                destination = "PINAR DE CHAMARTÍN",
                                departureTime = LocalDateTime.parse("2026-06-05T08:17:00"),
                                minutesUntil = 2,
                            )
                        ),
                        isStale = false,
                        validUntil = null,
                    ),
                ),
            ),
        )

        assertEquals("L4", snapshot.headline?.routeLabel)
        assertEquals("2m", snapshot.headline?.timeLabel)
    }

    @Test
    fun codecRoundTripsUtf8FieldsAndPlace() {
        val snapshot = MadridTransitSnapshot(
            updatedAt = "08:15",
            storedAtEpochMillis = 1_234L,
            items = listOf(
                MadridTransitSnapshotItem(
                    optionId = "metro_4_54_pinar_de_chamartin",
                    kind = MadridTransitKind.METRO,
                    source = MadridTransitSource.METRO_NAP,
                    place = MadridTransitPlace.PROFILE_3,
                    optionLabel = "Argüelles L4",
                    detail = "Pinar de Chamartín",
                    routeLabel = "L4",
                    destination = "PINAR DE CHAMARTÍN",
                    timeLabel = "Ahora",
                    rankMinutes = 0,
                )
            ),
        )

        val decoded = MadridTransitSnapshotCodec.decode(MadridTransitSnapshotCodec.encode(snapshot))

        assertEquals(snapshot, decoded)
    }

    @Test
    fun decoderAcceptsLegacySnapshotsWithoutStoredTimestamp() {
        val raw = listOf(
            "v1\t08%3A15",
            "bus_emt_e3_1064_valderrivas\tBUS\tprofile_1\tDaroca+E3\tValderrivas\tE3\tVALDERRIVAS\t4m\t4",
        ).joinToString("\n")

        val decoded = MadridTransitSnapshotCodec.decode(raw)

        assertEquals("08:15", decoded?.updatedAt)
        assertEquals(0L, decoded?.storedAtEpochMillis)
        assertEquals("bus_emt_e3_1064_valderrivas", decoded?.items?.single()?.optionId)
        assertEquals(MadridTransitSource.EMT_OPENAPI, decoded?.items?.single()?.source)
    }

    @Test
    fun mergePlaceReplacesOnlyTheSelectedPlace() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = "home", rankMinutes = 2),
                item(place = MadridTransitPlace.PROFILE_2, optionId = "work", rankMinutes = 4),
            ),
        )

        val merged = MadridTransitSnapshots.mergePlace(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            updatedAt = "08:15",
            replacementItems = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = "new_home", rankMinutes = 1)),
        )

        assertEquals("08:15", merged.updatedAt)
        assertEquals(listOf("new_home", "work"), merged.items.map { item -> item.optionId })
    }

    @Test
    fun mergeRefreshUpdatesFreshOptionsAndKeepsFailedOptionCache() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = "fresh", rankMinutes = 8),
                item(place = MadridTransitPlace.PROFILE_1, optionId = "failed", rankMinutes = 4),
                item(place = MadridTransitPlace.PROFILE_2, optionId = "other", rankMinutes = 2),
            ),
        )

        val merged = MadridTransitSnapshots.mergeRefresh(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            updatedAt = "08:15",
            refreshedOptionIds = setOf("fresh"),
            replacementItems = listOf(item(place = MadridTransitPlace.PROFILE_1, optionId = "fresh", rankMinutes = 1)),
        )

        assertEquals("08:15", merged.updatedAt)
        assertEquals(listOf("fresh", "failed", "other"), merged.items.map { item -> item.optionId })
        assertEquals(1, merged.items.first { item -> item.optionId == "fresh" }.rankMinutes)
        assertEquals(4, merged.items.first { item -> item.optionId == "failed" }.rankMinutes)
    }

    @Test
    fun mergeRefreshClearsCacheForFreshOptionWithNoArrivals() {
        val previous = MadridTransitSnapshot(
            updatedAt = "08:00",
            items = listOf(
                item(place = MadridTransitPlace.PROFILE_1, optionId = "empty_success", rankMinutes = 8),
                item(place = MadridTransitPlace.PROFILE_1, optionId = "failed", rankMinutes = 4),
            ),
        )

        val merged = MadridTransitSnapshots.mergeRefresh(
            previous = previous,
            place = MadridTransitPlace.PROFILE_1,
            updatedAt = "08:15",
            refreshedOptionIds = setOf("empty_success"),
            replacementItems = emptyList(),
        )

        assertEquals(listOf("failed"), merged.items.map { item -> item.optionId })
    }

    @Test
    fun decoderRejectsBlankAndUnknownVersions() {
        assertNull(MadridTransitSnapshotCodec.decode(null))
        assertNull(MadridTransitSnapshotCodec.decode(""))
        assertNull(MadridTransitSnapshotCodec.decode("v0\t08%3A15"))
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
}
