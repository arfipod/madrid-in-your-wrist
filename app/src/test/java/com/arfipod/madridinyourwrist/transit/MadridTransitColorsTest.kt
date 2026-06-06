package com.arfipod.madridinyourwrist.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadridTransitColorsTest {
    @Test
    fun emtAndInterurbanBusesUseDifferentBrandColors() {
        val emt = requireNotNull(MadridTransitCatalog.optionById("bus_emt_001_5135_moncloa"))
        val interurban = requireNotNull(
            MadridTransitCatalog.optionById("bus_interurbano_421_08046_p_delicias_plaza_de_legazpi")
        )

        assertEquals(MadridTransitColors.EMT_BUS_BRAND_ARGB, MadridTransitColors.brandArgbForOption(emt))
        assertEquals(MadridTransitColors.INTERURBAN_BUS_BRAND_ARGB, MadridTransitColors.brandArgbForOption(interurban))
        assertTrue(MadridTransitColors.textArgbForOption(emt) != MadridTransitColors.textArgbForOption(interurban))
    }

    @Test
    fun metroUsesLineSpecificBrandColors() {
        val line1 = requireNotNull(MadridTransitCatalog.optionById("metro_1_12_valdecarros"))
        val line4 = requireNotNull(MadridTransitCatalog.optionById("metro_4_54_pinar_de_chamartin"))
        val lightRail = requireNotNull(MadridTransitCatalog.optionById("metro_ligero_ml2_21_colonia_jardin"))

        assertEquals(0xFF00A3E0, MadridTransitColors.brandArgbForOption(line1))
        assertEquals(0xFF8B5A3C, MadridTransitColors.brandArgbForOption(line4))
        assertEquals(0xFF9B1B80, MadridTransitColors.brandArgbForOption(lightRail))
    }

    @Test
    fun textColorsKeepHueButLiftDarkBrandColorsForWatchUi() {
        val emt = requireNotNull(MadridTransitCatalog.optionById("bus_emt_e3_1064_valderrivas"))

        assertTrue(MadridTransitColors.textArgbForOption(emt) != MadridTransitColors.EMT_BUS_BRAND_ARGB)
        assertEquals(
            MadridMetroLineColors.colorArgbForLine("L3"),
            MadridTransitColors.textArgbForMetroLine("L3"),
        )
    }

    @Test
    fun snapshotItemsCarrySourceForCachedBusColors() {
        val item = MadridTransitSnapshotItem(
            optionId = "bus_interurbano_421_08046_p_delicias_plaza_de_legazpi",
            kind = MadridTransitKind.BUS,
            source = MadridTransitSource.CRTM_STATIC_GTFS,
            place = MadridTransitPlace.PROFILE_1,
            optionLabel = "P. Delicias 421",
            detail = "Plaza de Legazpi",
            routeLabel = "421",
            destination = "PLAZA DE LEGAZPI",
            timeLabel = "8m",
            rankMinutes = 8,
        )

        assertEquals(
            MadridTransitColors.INTERURBAN_BUS_BRAND_ARGB,
            MadridTransitColors.brandArgbForSnapshotItem(item),
        )
    }
}
