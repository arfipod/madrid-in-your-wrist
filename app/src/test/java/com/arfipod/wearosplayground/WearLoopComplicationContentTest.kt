package com.arfipod.wearosplayground

import com.arfipod.wearosplayground.transit.MadridTransitKind
import com.arfipod.wearosplayground.transit.MadridTransitPlace
import com.arfipod.wearosplayground.transit.MadridTransitSnapshot
import com.arfipod.wearosplayground.transit.MadridTransitSnapshotItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLoopComplicationContentTest {
    @Test
    fun complicationShowsCompactCachedHeadline() {
        val snapshot = snapshot(timeLabel = "4m")

        assertEquals("E3 4m", WearLoopComplicationContent.text(snapshot))
        assertTrue(WearLoopComplicationContent.text(snapshot).length <= 7)
        assertEquals("Casa", WearLoopComplicationContent.title(snapshot, MadridTransitPlace.HOME))
    }

    @Test
    fun nowLabelIsMadeShortEnoughForComplication() {
        val snapshot = snapshot(timeLabel = "Ahora")

        assertEquals("E3 Ya", WearLoopComplicationContent.text(snapshot))
        assertTrue(WearLoopComplicationContent.text(snapshot).length <= 7)
    }

    @Test
    fun complicationFallbackTextIsShort() {
        assertEquals("MAD", WearLoopComplicationContent.text(null))
        assertEquals("Trabajo", WearLoopComplicationContent.title(null, MadridTransitPlace.WORK))
        assertTrue(WearLoopComplicationContent.text(null).length <= 7)
    }

    @Test
    fun contentDescriptionDescribesCachedTransitOrBuildFallback() {
        assertEquals(
            "Madrid Wrist: Daroca E3, E3 hacia VALDERRIVAS, 4m. Actualizado 08:15",
            WearLoopComplicationContent.contentDescription(snapshot(timeLabel = "4m"), "ignored"),
        )
        assertEquals(
            "Madrid Wrist sin datos recientes. Build 2026-06-02T18:13:29",
            WearLoopComplicationContent.contentDescription(
                null,
                "2026-06-02T18:13:29.534290194Z",
            ),
        )
    }

    @Test
    fun updatePeriodUsesMinimumBatteryFriendlyInterval() {
        assertEquals("SHORT_TEXT", WearLoopComplicationContent.SUPPORTED_TYPE)
        assertTrue(WearLoopComplicationContent.UPDATE_PERIOD_SECONDS >= 300)
    }

    private fun snapshot(timeLabel: String): MadridTransitSnapshot = MadridTransitSnapshot(
        updatedAt = "08:15",
        items = listOf(
            MadridTransitSnapshotItem(
                optionId = "bus_e3_daroca_valderrivas",
                kind = MadridTransitKind.BUS,
                place = MadridTransitPlace.HOME,
                optionLabel = "Daroca E3",
                detail = "Valderrivas",
                routeLabel = "E3",
                destination = "VALDERRIVAS",
                timeLabel = timeLabel,
                rankMinutes = timeLabel.removeSuffix("m").toIntOrNull() ?: 0,
            )
        ),
    )
}
