package com.arfipod.madridinyourwrist.transit

object MadridTransitColors {
    const val EMT_BUS_BRAND_ARGB: Long = 0xFF0055A0
    const val INTERURBAN_BUS_BRAND_ARGB: Long = 0xFF64A70B
    const val FALLBACK_BUS_BRAND_ARGB: Long = INTERURBAN_BUS_BRAND_ARGB

    fun brandArgbForOption(option: MadridTransitOption): Long {
        return brandArgbFor(
            kind = option.kind,
            source = option.source,
            routeLabel = option.metroTarget?.routeNameQuery ?: option.busTarget?.lineId ?: option.label,
        )
    }

    fun textArgbForOption(option: MadridTransitOption): Long {
        return textArgbForBrand(brandArgbForOption(option))
    }

    fun brandArgbForSnapshotItem(item: MadridTransitSnapshotItem): Long {
        return brandArgbFor(
            kind = item.kind,
            source = item.source,
            routeLabel = item.routeLabel,
        )
    }

    fun textArgbForSnapshotItem(item: MadridTransitSnapshotItem): Long {
        return textArgbForBrand(brandArgbForSnapshotItem(item))
    }

    fun textArgbForMetroLine(routeLabel: String?): Long {
        val brand = MadridMetroLineColors.colorArgbForLine(routeLabel)
            ?: MadridMetroLineColors.FALLBACK_ARGB
        return textArgbForBrand(brand)
    }

    private fun brandArgbFor(
        kind: MadridTransitKind,
        source: MadridTransitSource,
        routeLabel: String?,
    ): Long {
        return when (kind) {
            MadridTransitKind.METRO -> MadridMetroLineColors.colorArgbForLine(routeLabel)
                ?: MadridMetroLineColors.FALLBACK_ARGB
            MadridTransitKind.BUS -> when (source) {
                MadridTransitSource.EMT_OPENAPI -> EMT_BUS_BRAND_ARGB
                MadridTransitSource.CRTM_STATIC_GTFS -> INTERURBAN_BUS_BRAND_ARGB
                MadridTransitSource.METRO_NAP -> FALLBACK_BUS_BRAND_ARGB
            }
        }
    }

    private fun textArgbForBrand(brandArgb: Long): Long {
        val red = ((brandArgb shr 16) and 0xFF).toInt()
        val green = ((brandArgb shr 8) and 0xFF).toInt()
        val blue = (brandArgb and 0xFF).toInt()
        val luminance = (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
        val whiteMix = when {
            luminance < 80 -> 0.42
            luminance < 120 -> 0.30
            luminance < 150 -> 0.18
            else -> 0.0
        }
        if (whiteMix == 0.0) return brandArgb

        return 0xFF000000 or
            (mixChannel(red, whiteMix).toLong() shl 16) or
            (mixChannel(green, whiteMix).toLong() shl 8) or
            mixChannel(blue, whiteMix).toLong()
    }

    private fun mixChannel(channel: Int, whiteMix: Double): Int {
        return (channel + ((255 - channel) * whiteMix)).toInt().coerceIn(0, 255)
    }
}
