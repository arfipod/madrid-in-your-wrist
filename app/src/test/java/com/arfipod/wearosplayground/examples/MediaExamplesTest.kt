package com.arfipod.wearosplayground.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaExamplesTest {
    @Test
    fun audioMelodyHasFiniteDuration() {
        assertEquals(520, AudioExampleMelody.totalDurationMs())
        assertEquals(0, AudioExampleMelody.activeBars(progress = 0f))
        assertEquals(12, AudioExampleMelody.activeBars(progress = 1f))
    }

    @Test
    fun videoSourceUsesHttpsMp4() {
        assertTrue(VideoExampleSource.isHttpsMp4())
        assertEquals("interactive-examples.mdn.mozilla.net", VideoExampleSource.host())
    }
}
