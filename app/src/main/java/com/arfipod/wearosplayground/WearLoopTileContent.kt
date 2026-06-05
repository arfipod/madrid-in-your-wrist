package com.arfipod.wearosplayground

import com.arfipod.wearosplayground.transit.MadridTransitPlace
import com.arfipod.wearosplayground.transit.MadridTransitSnapshot

object WearLoopTileContent {
    const val RESOURCES_VERSION = "madrid-wrist-tile-v2"
    const val FRESHNESS_INTERVAL_MILLIS = 5 * 60 * 1000L

    fun title(
        snapshot: MadridTransitSnapshot?,
        selectedPlace: MadridTransitPlace? = null,
    ): String {
        val place = snapshot?.headline?.place ?: selectedPlace
        return place?.let { "${it.label} · Madrid" } ?: "Madrid Wrist"
    }

    fun body(snapshot: MadridTransitSnapshot?): String {
        val headline = snapshot?.headline ?: return "Sin datos"
        return "${headline.routeLabel} ${headline.timeLabel}"
    }

    fun footer(snapshot: MadridTransitSnapshot?): String {
        val headline = snapshot?.headline ?: return "Abre la app y toca ↻"
        return "${headline.optionLabel} · ${snapshot.updatedAt}"
    }
}
