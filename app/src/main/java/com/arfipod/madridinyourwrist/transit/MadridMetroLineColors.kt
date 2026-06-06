package com.arfipod.madridinyourwrist.transit

import java.util.Locale

object MadridMetroLineColors {
    const val FALLBACK_ARGB: Long = 0xFF82B1FF

    private val colorsByLineId: Map<String, Long> = mapOf(
        "1" to 0xFF00A3E0,
        "2" to 0xFFE30613,
        "3" to 0xFFFFCC00,
        "4" to 0xFF8B5A3C,
        "5" to 0xFF4AA82D,
        "6" to 0xFF9D9D9C,
        "7" to 0xFFF39200,
        "8" to 0xFFE85298,
        "9" to 0xFF9B1B80,
        "10" to 0xFF0072BC,
        "11" to 0xFF006B3F,
        "12" to 0xFF9E9D24,
        "R" to 0xFF005CA9,
        "ML1" to 0xFF005CA9,
        "ML2" to 0xFF9B1B80,
        "ML3" to 0xFFE30613,
    )

    fun colorArgbForLine(rawLine: String?): Long? {
        return colorsByLineId[normalizeLineId(rawLine)]
    }

    fun normalizeLineId(rawLine: String?): String? {
        val compact = rawLine
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.replace(" ", "")
            ?.replace("-", "")
            ?.takeIf { value -> value.isNotBlank() }
            ?: return null

        val withoutLinePrefix = compact
            .removePrefix("LINEA")
            .removePrefix("LINE")
            .removePrefix("L")

        return when {
            compact.startsWith("ML") -> compact.takeIf { value -> value in colorsByLineId }
            compact == "RAMAL" -> "R"
            compact == "R" -> "R"
            withoutLinePrefix == "R" -> "R"
            withoutLinePrefix in colorsByLineId -> withoutLinePrefix
            else -> null
        }
    }
}
