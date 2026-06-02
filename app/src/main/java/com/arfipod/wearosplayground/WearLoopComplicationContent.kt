package com.arfipod.wearosplayground

object WearLoopComplicationContent {
    const val SUPPORTED_TYPE = "SHORT_TEXT"
    const val UPDATE_PERIOD_SECONDS = 300

    fun text(): String = "Loop"

    fun title(): String = "Ready"

    fun contentDescription(buildTimestamp: String): String {
        val shortTimestamp = buildTimestamp.substringBefore('.').removeSuffix("Z")
        return "Wear Loop ready. Build $shortTimestamp"
    }
}
