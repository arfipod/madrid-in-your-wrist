package com.arfipod.wearosplayground

object WearLoopTileContent {
    const val RESOURCES_VERSION = "wear-loop-tile-v1"
    const val FRESHNESS_INTERVAL_MILLIS = 15 * 60 * 1000L

    fun title(): String = "Wear Loop"

    fun body(buildTimestamp: String): String {
        val shortTimestamp = buildTimestamp.substringBefore('.').removeSuffix("Z")
        return "Build $shortTimestamp"
    }

    fun footer(): String = "Open app for counter and sensors"
}
