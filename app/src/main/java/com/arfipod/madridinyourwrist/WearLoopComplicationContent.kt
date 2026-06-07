package com.arfipod.madridinyourwrist

import com.arfipod.madridinyourwrist.transit.MadridTransitPlaceProfile
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshot

object WearLoopComplicationContent {
    const val SUPPORTED_TYPE = "SHORT_TEXT"
    const val UPDATE_PERIOD_SECONDS = 900

    fun text(snapshot: MadridTransitSnapshot?): String {
        val headline = snapshot?.headline ?: return "MAD"
        val compact = "${headline.displayRouteLabel} ${headline.timeLabel.complicationTimeLabel()}"
        return if (compact.length <= 7) compact else compact.take(7)
    }

    fun title(
        snapshot: MadridTransitSnapshot?,
        selectedProfile: MadridTransitPlaceProfile? = null,
    ): String {
        return selectedProfile?.shortLabel ?: snapshot?.headline?.place?.shortLabel ?: "Transit"
    }

    fun contentDescription(
        snapshot: MadridTransitSnapshot?,
        buildTimestamp: String,
    ): String {
        val headline = snapshot?.headline
        if (headline != null) {
            return "Madrid Wrist: ${headline.optionLabel}, ${headline.displayRouteLabel} hacia " +
                "${headline.destination}, ${headline.timeLabel}. Actualizado ${snapshot.updatedAt}"
        }
        val shortTimestamp = buildTimestamp.substringBefore('.').removeSuffix("Z")
        return "Madrid Wrist sin datos recientes. Build $shortTimestamp"
    }

    private fun String.complicationTimeLabel(): String = if (this == "Ahora") "Ya" else this
}
