package com.arfipod.madridinyourwrist

import com.arfipod.madridinyourwrist.transit.MadridTransitPlaceProfile
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshot

object WearLoopTileContent {
    const val RESOURCES_VERSION = "madrid-wrist-tile-v2"
    const val FRESHNESS_INTERVAL_MILLIS = 15 * 60 * 1000L

    fun title(
        snapshot: MadridTransitSnapshot?,
        selectedProfile: MadridTransitPlaceProfile? = null,
    ): String {
        val label = selectedProfile?.label ?: snapshot?.headline?.place?.label
        return label?.let { "$it · Madrid" } ?: "Madrid Wrist"
    }

    fun body(snapshot: MadridTransitSnapshot?): String {
        val headline = snapshot?.headline ?: return "Sin datos"
        return "${headline.displayRouteLabel} ${headline.timeLabel}"
    }

    fun footer(snapshot: MadridTransitSnapshot?): String {
        val headline = snapshot?.headline ?: return "Abre la app y toca ↻"
        return "${headline.optionLabel} · ${snapshot.updatedAt}"
    }
}
