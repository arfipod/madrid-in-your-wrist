package com.arfipod.wearosplayground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLoopTileContentTest {
    @Test
    fun tileTextIsStableAndShort() {
        assertEquals("Wear Loop", WearLoopTileContent.title())
        assertEquals("Open app for counter and sensors", WearLoopTileContent.footer())
    }

    @Test
    fun bodyShortensIsoTimestamp() {
        val body = WearLoopTileContent.body("2026-06-02T18:13:29.534290194Z")

        assertEquals("Build 2026-06-02T18:13:29", body)
    }

    @Test
    fun freshnessIntervalAvoidsOverlyFrequentUpdates() {
        assertTrue(WearLoopTileContent.FRESHNESS_INTERVAL_MILLIS >= 60_000L)
    }
}
