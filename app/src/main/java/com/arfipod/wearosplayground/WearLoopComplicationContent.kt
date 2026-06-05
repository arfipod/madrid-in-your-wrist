package com.arfipod.wearosplayground

object WearLoopComplicationContent {
    const val SUPPORTED_TYPE = "SHORT_TEXT"
    const val UPDATE_PERIOD_SECONDS = 300

    fun text(): String = "MAD"

    fun title(): String = "Transit"

    fun contentDescription(buildTimestamp: String): String {
        val shortTimestamp = buildTimestamp.substringBefore('.').removeSuffix("Z")
        return "Madrid Wrist ready. Build $shortTimestamp"
    }
}
