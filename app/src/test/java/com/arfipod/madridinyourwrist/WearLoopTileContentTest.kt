package com.arfipod.madridinyourwrist

import com.arfipod.madridinyourwrist.transit.MadridTransitKind
import com.arfipod.madridinyourwrist.transit.MadridTransitPlace
import com.arfipod.madridinyourwrist.transit.MadridTransitPlaceProfile
import com.arfipod.madridinyourwrist.transit.MadridTransitProfileIcon
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshot
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshotItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLoopTileContentTest {
    @Test
    fun tileUsesCachedHeadlineForSelectedPlace() {
        val snapshot = snapshot(
            item(
                place = MadridTransitPlace.PROFILE_1,
                optionLabel = "Daroca E3",
                routeLabel = "E3",
                timeLabel = "4m",
            )
        )

        assertEquals(
            "Trabajo · Madrid",
            WearLoopTileContent.title(
                snapshot,
                MadridTransitPlaceProfile(
                    place = MadridTransitPlace.PROFILE_1,
                    customName = "Trabajo",
                    icon = MadridTransitProfileIcon.BRIEFCASE,
                ),
            ),
        )
        assertEquals("E3 4m", WearLoopTileContent.body(snapshot))
        assertEquals("Daroca E3 · 08:15", WearLoopTileContent.footer(snapshot))
    }

    @Test
    fun tileFallbackTextStaysUsefulWithoutCache() {
        assertEquals(
            "María · Madrid",
            WearLoopTileContent.title(
                null,
                MadridTransitPlaceProfile(
                    place = MadridTransitPlace.PROFILE_2,
                    customName = "María",
                    icon = MadridTransitProfileIcon.HEART,
                ),
            ),
        )
        assertEquals("Sin datos", WearLoopTileContent.body(null))
        assertEquals("Abre la app y toca ↻", WearLoopTileContent.footer(null))
    }

    @Test
    fun tileShowsMetroLineNumberWithoutLegacyLPrefix() {
        val snapshot = snapshot(
            item(
                kind = MadridTransitKind.METRO,
                place = MadridTransitPlace.PROFILE_1,
                optionLabel = "Argüelles",
                routeLabel = "L4",
                timeLabel = "2m",
            )
        )

        assertEquals("4 2m", WearLoopTileContent.body(snapshot))
    }

    @Test
    fun freshnessIntervalAvoidsOverlyFrequentUpdates() {
        assertTrue(WearLoopTileContent.FRESHNESS_INTERVAL_MILLIS >= 15 * 60 * 1000L)
    }

    private fun snapshot(vararg items: MadridTransitSnapshotItem): MadridTransitSnapshot =
        MadridTransitSnapshot(updatedAt = "08:15", items = items.toList())

    private fun item(
        kind: MadridTransitKind = MadridTransitKind.BUS,
        place: MadridTransitPlace,
        optionLabel: String,
        routeLabel: String,
        timeLabel: String,
    ): MadridTransitSnapshotItem = MadridTransitSnapshotItem(
        optionId = optionLabel,
        kind = kind,
        place = place,
        optionLabel = optionLabel,
        detail = "Valderrivas",
        routeLabel = routeLabel,
        destination = "VALDERRIVAS",
        timeLabel = timeLabel,
        rankMinutes = timeLabel.removeSuffix("m").toIntOrNull() ?: 0,
    )
}
