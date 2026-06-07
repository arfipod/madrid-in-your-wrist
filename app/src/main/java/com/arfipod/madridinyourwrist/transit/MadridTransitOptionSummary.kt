package com.arfipod.madridinyourwrist.transit

import java.util.Locale

data class MadridTransitOptionSummary(
    val lineLabel: String,
    val stopName: String,
    val serviceLabel: String,
    val stopCodeLabel: String?,
    val destinationLabel: String?,
    val availabilityLabel: String?,
) {
    val metadataLabel: String
        get() = listOfNotNull(serviceLabel, stopCodeLabel)
            .filter { value -> value.isNotBlank() }
            .joinToString(" · ")

    val destinationLine: String?
        get() {
            val destination = destinationLabel?.takeIf { value -> value.isNotBlank() }
            val availability = availabilityLabel?.takeIf { value -> value.isNotBlank() }
            return listOfNotNull(
                destination?.let { value -> "→ $value" },
                availability,
            ).joinToString(" · ").takeIf { value -> value.isNotBlank() }
        }
}

object MadridTransitOptionSummaries {
    fun from(option: MadridTransitOption): MadridTransitOptionSummary {
        val lineLabel = option.lineLabel()
        return MadridTransitOptionSummary(
            lineLabel = lineLabel,
            stopName = option.displayStopName(lineLabel = lineLabel),
            serviceLabel = option.serviceLabel(lineLabel = lineLabel),
            stopCodeLabel = option.stopCodeLabel(),
            destinationLabel = option.detail.takeIf { value -> value.isNotBlank() },
            availabilityLabel = "solo catálogo".takeIf { !option.source.hasLiveArrivals },
        )
    }

    private fun MadridTransitOption.lineLabel(): String {
        return when (kind) {
            MadridTransitKind.METRO -> metroTarget
                ?.routeNameQuery
                ?.madridTransitShortRoute()
                ?: labelLastToken()?.madridTransitShortRoute()
                ?: labelLastToken()
                ?: MadridTransitKind.METRO.label
            MadridTransitKind.BUS -> busTarget
                ?.lineId
                ?.uppercase(Locale.ROOT)
                ?: labelLastToken()
                ?: MadridTransitKind.BUS.label
            MadridTransitKind.TRAIN -> trainTarget
                ?.lineId
                ?.uppercase(Locale.ROOT)
                ?: labelLastToken()
                ?: "Cercanías"
        }
    }

    private fun MadridTransitOption.displayStopName(lineLabel: String): String {
        return stopNameFromLabel(lineLabel = lineLabel)
            ?: when (kind) {
                MadridTransitKind.METRO -> metroTarget?.stopNameQuery
                MadridTransitKind.BUS -> busTarget?.stopName ?: busTarget?.label?.substringBefore("->")?.trim()
                MadridTransitKind.TRAIN -> trainTarget?.stopName ?: trainTarget?.label?.substringBefore("->")?.trim()
            }?.takeIf { value -> value.isNotBlank() }
            ?: label
    }

    private fun MadridTransitOption.serviceLabel(lineLabel: String): String {
        return when (kind) {
            MadridTransitKind.METRO -> if (lineLabel.startsWith("ML")) "Metro Ligero" else "Metro"
            MadridTransitKind.BUS -> when (source) {
                MadridTransitSource.EMT_OPENAPI -> "EMT"
                MadridTransitSource.CRTM_STATIC_GTFS -> "Interurbano"
                MadridTransitSource.METRO_NAP -> "Bus"
            }
            MadridTransitKind.TRAIN -> "Cercanías"
        }
    }

    private fun MadridTransitOption.stopCodeLabel(): String? {
        return when (kind) {
            MadridTransitKind.METRO -> metroTarget
                ?.stopId
                ?.takeIf { value -> value.isNotBlank() }
                ?.let { value -> "Estación $value" }
            MadridTransitKind.BUS -> busTarget
                ?.stopId
                ?.takeIf { value -> value.isNotBlank() }
                ?.let { value -> "Parada $value" }
            MadridTransitKind.TRAIN -> trainTarget
                ?.stopId
                ?.takeIf { value -> value.isNotBlank() }
                ?.let { value -> "Estación $value" }
        }
    }

    private fun MadridTransitOption.stopNameFromLabel(lineLabel: String): String? {
        val trimmed = label.trim()
        val suffixes = listOfNotNull(
            " $lineLabel",
            if (kind == MadridTransitKind.METRO && lineLabel.all { char -> char.isDigit() }) {
                " L$lineLabel"
            } else {
                null
            },
        )
        suffixes.forEach { suffix ->
            if (trimmed.uppercase(Locale.ROOT).endsWith(suffix.uppercase(Locale.ROOT))) {
                return trimmed.dropLast(suffix.length).trim().takeIf { value -> value.isNotBlank() }
            }
        }
        return null
    }

    private fun MadridTransitOption.labelLastToken(): String? {
        return label
            .trim()
            .substringAfterLast(' ', missingDelimiterValue = "")
            .takeIf { value -> value.isNotBlank() }
    }
}
