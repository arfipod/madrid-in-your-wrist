package com.arfipod.wearosplayground

import java.util.Locale
import kotlin.math.sqrt

object SensorSampleFormatter {
    fun accelerometerSample(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
    ): String {
        require(timestampNanos >= 0) { "Timestamp must be non-negative." }

        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        val timestampMillis = timestampNanos / 1_000_000L

        return String.format(
            Locale.US,
            "Accelerometer t=%dms x=%.2f y=%.2f z=%.2f |g|=%.2f",
            timestampMillis,
            x,
            y,
            z,
            magnitude,
        )
    }
}
