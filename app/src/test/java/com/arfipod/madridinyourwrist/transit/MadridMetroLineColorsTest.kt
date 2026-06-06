package com.arfipod.madridinyourwrist.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MadridMetroLineColorsTest {
    @Test
    fun normalizesMetroLineLabels() {
        assertEquals("4", MadridMetroLineColors.normalizeLineId("4"))
        assertEquals("4", MadridMetroLineColors.normalizeLineId("L4"))
        assertEquals("4", MadridMetroLineColors.normalizeLineId("linea 4"))
        assertEquals("10", MadridMetroLineColors.normalizeLineId("Line 10"))
    }

    @Test
    fun supportsRamalAndMetroLigeroLineLabels() {
        assertEquals("R", MadridMetroLineColors.normalizeLineId("R"))
        assertEquals("R", MadridMetroLineColors.normalizeLineId("Ramal"))
        assertEquals("ML1", MadridMetroLineColors.normalizeLineId("ML1"))
        assertEquals("ML2", MadridMetroLineColors.normalizeLineId("ml 2"))
        assertEquals("ML3", MadridMetroLineColors.normalizeLineId("ml-3"))
    }

    @Test
    fun returnsExpectedLineColors() {
        assertEquals(0xFF8B5A3C, MadridMetroLineColors.colorArgbForLine("L4"))
        assertEquals(0xFFE30613, MadridMetroLineColors.colorArgbForLine("L2"))
        assertEquals(0xFF00A3E0, MadridMetroLineColors.colorArgbForLine("L1"))
        assertEquals(0xFF005CA9, MadridMetroLineColors.colorArgbForLine("Ramal"))
        assertEquals(0xFF9B1B80, MadridMetroLineColors.colorArgbForLine("ML2"))
    }

    @Test
    fun unknownLineHasNoSpecificColor() {
        assertNull(MadridMetroLineColors.normalizeLineId("unknown"))
        assertNull(MadridMetroLineColors.colorArgbForLine("unknown"))
    }
}
