package com.arfipod.wearosplayground

object WearLoopTileContent {
    const val RESOURCES_VERSION = "madrid-wrist-tile-v1"
    const val FRESHNESS_INTERVAL_MILLIS = 15 * 60 * 1000L

    fun title(): String = "Madrid Wrist"

    fun body(buildTimestamp: String): String {
        val shortTimestamp = buildTimestamp.substringBefore('.').removeSuffix("Z")
        return "Build $shortTimestamp"
    }

    fun footer(): String = "Open app for Metro and bus"
}
