package com.arfipod.madridinyourwrist.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadridTransitSearchTest {
    @Test
    fun normalizesAccentsAndPunctuation() {
        assertEquals(
            "arguelles linea 4 pinar de chamartin",
            MadridTransitSearch.run {
                "Argüelles, Línea 4 - Pinar de Chamartín".normalizedSearchText()
            },
        )
    }

    @Test
    fun findsMetroByStationLineAndDestination() {
        assertEquals(
            "metro_l4_arguelles_pinar",
            MadridTransitCatalog.searchOptions("arguelles l4 chamartin", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
        assertEquals(
            "metro_l2_goya_las_rosas",
            MadridTransitCatalog.searchOptions("Goya Las Rosas", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
    }

    @Test
    fun findsBusByStopIdStopAliasLineAndDestination() {
        assertEquals(
            "bus_e3_daroca_valderrivas",
            MadridTransitCatalog.searchOptions("parada 1064 casalarreina e3", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
        assertEquals(
            "bus_e3_felipe_valderrivas",
            MadridTransitCatalog.searchOptions("755 Felipe II Valderrivas", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
    }

    @Test
    fun blankQueryReturnsOptionsInCatalogOrder() {
        val results = MadridTransitCatalog.searchOptions("", kind = MadridTransitKind.METRO)

        assertEquals(MadridTransitCatalog.metroOptions.map { option -> option.id }, results.map { option -> option.id })
    }

    @Test
    fun unknownQueryReturnsNoResults() {
        assertTrue(MadridTransitCatalog.searchOptions("no existe nada").isEmpty())
    }
}
