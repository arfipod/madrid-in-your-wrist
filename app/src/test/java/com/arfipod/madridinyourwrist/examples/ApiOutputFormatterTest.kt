package com.arfipod.madridinyourwrist.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiOutputFormatterTest {
    @Test
    fun previewCompactsBlankLines() {
        val preview = ApiOutputFormatter.preview("\n  hello  \n\n world \n")

        assertEquals("hello\nworld", preview)
    }

    @Test
    fun previewTruncatesLongOutput() {
        val preview = ApiOutputFormatter.preview("abcdefghijklmnopqrstuvwxyz", maxChars = 10)

        assertEquals(10, preview.length)
        assertTrue(preview.endsWith("..."))
    }

    @Test
    fun tooSmallLimitIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiOutputFormatter.preview("hello", maxChars = 4)
        }
    }
}
