package com.arfipod.wearosplayground.transit

import com.arfipod.wearosplayground.examples.EmtBusArrival
import com.arfipod.wearosplayground.examples.MetroDeparture
import com.arfipod.wearosplayground.examples.MetroDeparturesResult
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MadridTransitSnapshotTest {
    @Test
    fun headlineChoosesSoonestTransitAcrossLoadedResults() {
        val metroFavorite = requireNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "metro_l4_arguelles_pinar",
                place = MadridTransitPlace.PROFILE_1,
            )
        )
        val busFavorite = requireNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "bus_e3_daroca_valderrivas",
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
            items = listOf(
                MadridTransitSnapshotItem(
                    optionId = "metro_l4_arguelles_pinar",
                    kind = MadridTransitKind.METRO,
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
