package com.arfipod.wearosplayground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLoopComplicationContentTest {
    @Test
    fun complicationTextIsShort() {
        assertEquals("Loop", WearLoopComplicationContent.text())
        assertTrue(WearLoopComplicationContent.text().length <= 7)
    }

    @Test
    fun complicationTitleIsShort() {
        assertEquals("Ready", WearLoopComplicationContent.title())
        assertTrue(WearLoopComplicationContent.title().length <= 7)
    }

    @Test
    fun contentDescriptionIncludesShortBuildTimestamp() {
        assertEquals(
            "Wear Loop ready. Build 2026-06-02T18:13:29",
            WearLoopComplicationContent.contentDescription(
                "2026-06-02T18:13:29.534290194Z"
            ),
        )
    }

    @Test
    fun updatePeriodUsesMinimumBatteryFriendlyInterval() {
        assertEquals("SHORT_TEXT", WearLoopComplicationContent.SUPPORTED_TYPE)
        assertTrue(WearLoopComplicationContent.UPDATE_PERIOD_SECONDS >= 300)
    }
}
