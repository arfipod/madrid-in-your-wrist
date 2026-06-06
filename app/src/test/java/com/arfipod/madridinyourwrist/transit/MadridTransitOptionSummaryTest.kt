package com.arfipod.madridinyourwrist.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MadridTransitOptionSummaryTest {
    @Test
    fun emtNearbySummaryKeepsLineStopNameStopNumberAndDestinationSeparate() {
        val option = requireNotNull(MadridTransitCatalog.optionById("bus_emt_e3_1064_valderrivas"))

        val summary = MadridTransitOptionSummaries.from(option)

        assertEquals("E3", summary.lineLabel)
        assertEquals("Avenida De Daroca - Casalarreina", summary.stopName)
        assertEquals("EMT", summary.serviceLabel)
        assertEquals("Parada 1064", summary.stopCodeLabel)
        assertEquals("Valderrivas", summary.destinationLabel)
        assertNull(summary.availabilityLabel)
    }

    @Test
    fun interurbanNearbySummaryMarksCatalogOnlyService() {
        val option = requireNotNull(
            MadridTransitCatalog.optionById("bus_interurbano_421_08046_p_delicias_plaza_de_legazpi")
        )

        val summary = MadridTransitOptionSummaries.from(option)

        assertEquals("421", summary.lineLabel)
        assertEquals("Alfaro-Iglesia", summary.stopName)
        assertEquals("Interurbano", summary.serviceLabel)
        assertEquals("Parada 08046", summary.stopCodeLabel)
        assertEquals("solo catálogo", summary.availabilityLabel)
    }

    @Test
    fun metroNearbySummaryIncludesLineStationIdAndDestination() {
        val option = requireNotNull(MadridTransitCatalog.optionById("metro_4_54_pinar_de_chamartin"))

        val summary = MadridTransitOptionSummaries.from(option)

        assertEquals("L4", summary.lineLabel)
        assertEquals("Argüelles", summary.stopName)
        assertEquals("Metro", summary.serviceLabel)
        assertEquals("Estación 54", summary.stopCodeLabel)
        assertEquals("Pinar DE Chamartin", summary.destinationLabel)
        assertNull(summary.availabilityLabel)
    }
}
