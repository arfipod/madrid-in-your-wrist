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

class WearLoopComplicationContentTest {
    @Test
    fun complicationShowsCompactCachedHeadline() {
        val snapshot = snapshot(timeLabel = "4m")

        assertEquals("E3 4m", WearLoopComplicationContent.text(snapshot))
        assertTrue(WearLoopComplicationContent.text(snapshot).length <= 7)
        assertEquals(
            "♥",
            WearLoopComplicationContent.title(
                snapshot,
                MadridTransitPlaceProfile(
                    place = MadridTransitPlace.PROFILE_1,
                    customName = "María",
                    icon = MadridTransitProfileIcon.HEART,
                ),
            ),
        )
    }

    @Test
    fun nowLabelIsMadeShortEnoughForComplication() {
        val snapshot = snapshot(timeLabel = "Ahora")

        assertEquals("E3 Ya", WearLoopComplicationContent.text(snapshot))
        assertTrue(WearLoopComplicationContent.text(snapshot).length <= 7)
    }

    @Test
    fun complicationShowsMetroLineNumberWithoutLegacyLPrefix() {
        val snapshot = snapshot(
            kind = MadridTransitKind.METRO,
            optionLabel = "Argüelles",
            routeLabel = "L4",
            destination = "PINAR DE CHAMARTÍN",
            timeLabel = "2m",
        )

        assertEquals("4 2m", WearLoopComplicationContent.text(snapshot))
        assertEquals(
            "Madrid Wrist: Argüelles, 4 hacia PINAR DE CHAMARTÍN, 2m. Actualizado 08:15",
            WearLoopComplicationContent.contentDescription(snapshot, "ignored"),
        )
    }

    @Test
    fun complicationFallbackTextIsShort() {
        assertEquals("MAD", WearLoopComplicationContent.text(null))
        assertEquals(
            "⌂",
            WearLoopComplicationContent.title(
                null,
                MadridTransitPlaceProfile(
                    place = MadridTransitPlace.PROFILE_2,
                    customName = "Casa",
                    icon = MadridTransitProfileIcon.HOME,
                ),
            ),
        )
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
        assertTrue(WearLoopComplicationContent.UPDATE_PERIOD_SECONDS >= 900)
    }

    private fun snapshot(
        kind: MadridTransitKind = MadridTransitKind.BUS,
        optionLabel: String = "Daroca E3",
        routeLabel: String = "E3",
        destination: String = "VALDERRIVAS",
        timeLabel: String,
    ): MadridTransitSnapshot = MadridTransitSnapshot(
        updatedAt = "08:15",
        items = listOf(
            MadridTransitSnapshotItem(
                optionId = "bus_emt_e3_1064_valderrivas",
                kind = kind,
                place = MadridTransitPlace.PROFILE_1,
                optionLabel = optionLabel,
                detail = "Valderrivas",
                routeLabel = routeLabel,
                destination = destination,
                timeLabel = timeLabel,
                rankMinutes = timeLabel.removeSuffix("m").toIntOrNull() ?: 0,
            )
        ),
    )
}
