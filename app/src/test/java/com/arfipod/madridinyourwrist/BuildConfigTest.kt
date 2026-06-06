package com.arfipod.madridinyourwrist

import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildConfigTest {
    @Test
    fun buildTimestampIsIsoInstant() {
        val timestamp = Instant.parse(BuildConfig.BUILD_TIMESTAMP)

        assertTrue(timestamp.epochSecond > 0)
    }
}
