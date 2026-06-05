package com.arfipod.wearosplayground

import com.arfipod.wearosplayground.transit.MadridTransitKind
import com.arfipod.wearosplayground.transit.MadridTransitPlace
import com.arfipod.wearosplayground.transit.MadridTransitSnapshot
import com.arfipod.wearosplayground.transit.MadridTransitSnapshotItem
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

        assertEquals("Perfil 1 · Madrid", WearLoopTileContent.title(snapshot, MadridTransitPlace.PROFILE_1))
        assertEquals("E3 4m", WearLoopTileContent.body(snapshot))
        assertEquals("Daroca E3 · 08:15", WearLoopTileContent.footer(snapshot))
    }

    @Test
    fun tileFallbackTextStaysUsefulWithoutCache() {
        assertEquals("Perfil 2 · Madrid", WearLoopTileContent.title(null, MadridTransitPlace.PROFILE_2))
        assertEquals("Sin datos", WearLoopTileContent.body(null))
        assertEquals("Abre la app y toca ↻", WearLoopTileContent.footer(null))
    }

    @Test
    fun freshnessIntervalAvoidsOverlyFrequentUpdates() {
        assertTrue(WearLoopTileContent.FRESHNESS_INTERVAL_MILLIS >= 60_000L)
    }

    private fun snapshot(vararg items: MadridTransitSnapshotItem): MadridTransitSnapshot =
        MadridTransitSnapshot(updatedAt = "08:15", items = items.toList())

    private fun item(
        place: MadridTransitPlace,
        optionLabel: String,
        routeLabel: String,
        timeLabel: String,
    ): MadridTransitSnapshotItem = MadridTransitSnapshotItem(
        optionId = optionLabel,
        kind = MadridTransitKind.BUS,
        place = place,
        optionLabel = optionLabel,
        detail = "Valderrivas",
        routeLabel = routeLabel,
        destination = "VALDERRIVAS",
        timeLabel = timeLabel,
        rankMinutes = timeLabel.removeSuffix("m").toIntOrNull() ?: 0,
    )
}
