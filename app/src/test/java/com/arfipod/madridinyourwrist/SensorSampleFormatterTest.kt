package com.arfipod.madridinyourwrist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SensorSampleFormatterTest {
    @Test
    fun accelerometerSampleFormatsStableUsLocaleOutput() {
        val sample = SensorSampleFormatter.accelerometerSample(
            x = 1.234f,
            y = -2.345f,
            z = 9.806f,
            timestampNanos = 1_234_000_000L,
        )

        assertEquals(
            "Accelerometer t=1234ms x=1.23 y=-2.35 z=9.81 |g|=10.16",
            sample,
        )
    }

    @Test
    fun negativeTimestampIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SensorSampleFormatter.accelerometerSample(
                x = 0f,
                y = 0f,
                z = 0f,
                timestampNanos = -1L,
            )
        }
    }
}
