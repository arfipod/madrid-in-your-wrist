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
    fun queryReadinessAvoidsSingleLetterCatalogScansButAllowsNumbers() {
        assertTrue(!MadridTransitSearch.isQueryReady("a"))
        assertTrue(MadridTransitSearch.isQueryReady("go"))
        assertTrue(MadridTransitSearch.isQueryReady("4"))
        assertTrue(MadridTransitSearch.isQueryReady("1064"))
    }

    @Test
    fun multiWordQueriesIgnoreCommonConnectorWords() {
        assertEquals(
            listOf("puerta", "arganda"),
            MadridTransitSearch.run {
                "Puerta de arganda".normalizedSearchTerms()
            },
        )
    }

    @Test
    fun compactTransitQueriesSplitStopLineAndDestination() {
        assertEquals(
            listOf("54", "l4", "pinar"),
            MadridTransitSearch.run {
                "54l4pinar".normalizedSearchTerms()
            },
        )
        assertEquals(
            listOf("1064", "e3", "valderrivas"),
            MadridTransitSearch.run {
                "1064e3valderrivas".normalizedSearchTerms()
            },
        )
        assertEquals(
            listOf("se718"),
            MadridTransitSearch.run {
                "se718".normalizedSearchTerms()
            },
        )
        assertEquals(
            listOf("c3", "atocha"),
            MadridTransitSearch.run {
                "c3atocha".normalizedSearchTerms()
            },
        )
    }

    @Test
    fun findsMetroByStationLineAndDestination() {
        assertEquals(
            "metro_4_54_pinar_de_chamartin",
            MadridTransitCatalog.searchOptions("arguelles l4 chamartin", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
        assertEquals(
            "metro_2_30_las_rosas",
            MadridTransitCatalog.searchOptions("Goya Las Rosas", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
    }

    @Test
    fun findsBusByStopIdStopAliasLineAndDestination() {
        assertEquals(
            "bus_emt_e3_1064_valderrivas",
            MadridTransitCatalog.searchOptions("parada 1064 casalarreina e3", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
        assertEquals(
            "bus_emt_e3_755_valderrivas",
            MadridTransitCatalog.searchOptions("755 Felipe II Valderrivas", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
    }

    @Test
    fun findsUsabilityTestFavoritesWithCompactWearKeyboardQueries() {
        mapOf(
            "54l4pinar" to "metro_4_54_pinar_de_chamartin",
            "300l9puerta" to "metro_9_300_puerta_de_arganda",
            "182l9paco" to "metro_9_182_paco_de_lucia",
            "182l9arganda" to "metro_9_182_arganda_del_rey",
            "61l4arguelles" to "metro_4_61_arguelles",
        ).forEach { (query, optionId) ->
            assertEquals(
                optionId,
                MadridTransitCatalog.searchOptions(query, kind = MadridTransitKind.METRO)
                    .first()
                    .id,
            )
        }

        mapOf(
            "1064e3valderrivas" to "bus_emt_e3_1064_valderrivas",
            "1065e3felipe" to "bus_emt_e3_1065_felipe_ii",
            "755e3valderrivas" to "bus_emt_e3_755_valderrivas",
        ).forEach { (query, optionId) ->
            assertEquals(
                optionId,
                MadridTransitCatalog.searchOptions(query, kind = MadridTransitKind.BUS)
                    .first()
                    .id,
            )
        }
    }

    @Test
    fun findsUsabilityTestFavoritesWithSpaceFreeStationLineDestinationQueries() {
        mapOf(
            "arguellesl4pinar" to "metro_4_54_pinar_de_chamartin",
            "rivasfutural9puerta" to "metro_9_300_puerta_de_arganda",
            "puertadeargandal9paco" to "metro_9_182_paco_de_lucia",
            "puertadeargandal9arganda" to "metro_9_182_arganda_del_rey",
            "goyal4arguelles" to "metro_4_61_arguelles",
        ).forEach { (query, optionId) ->
            assertEquals(
                optionId,
                MadridTransitCatalog.searchOptions(query, kind = MadridTransitKind.METRO)
                    .first()
                    .id,
            )
        }

        mapOf(
            "darocacasalarreinae3valderrivas" to "bus_emt_e3_1064_valderrivas",
            "darocacasalarreinae3felipe" to "bus_emt_e3_1065_felipe_ii",
            "felipeiie3valderrivas" to "bus_emt_e3_755_valderrivas",
        ).forEach { (query, optionId) ->
            assertEquals(
                optionId,
                MadridTransitCatalog.searchOptions(query, kind = MadridTransitKind.BUS)
                    .first()
                    .id,
            )
        }
    }

    @Test
    fun findsGeneratedOfficialMetroLightRailEmtAndInterurbanOptions() {
        assertEquals(
            "metro_1_12_valdecarros",
            MadridTransitCatalog.searchOptions("sol l1 valdecarros", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
        assertEquals(
            "metro_ligero_ml1_5_las_tablas",
            MadridTransitCatalog.searchOptions("alvarez villaamil ml1 las tablas", kind = MadridTransitKind.METRO)
                .first()
                .id,
        )
        assertEquals(
            "bus_emt_001_5135_moncloa",
            MadridTransitCatalog.searchOptions("circulo bellas artes 001 moncloa", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
        assertEquals(
            "bus_interurbano_421_08046_p_delicias_plaza_de_legazpi",
            MadridTransitCatalog.searchOptions("alfaro iglesia 421 legazpi", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
        assertEquals(
            "Puerta Arganda",
            MadridTransitCatalog.searchOptions("Puerta de arganda", kind = MadridTransitKind.BUS)
                .first()
                .busTarget
                ?.stopName,
        )
        assertEquals(
            "bus_emt_se718_5839_puerta_arganda",
            MadridTransitCatalog.searchOptions("Puerta de arganda se718", kind = MadridTransitKind.BUS)
                .first()
                .id,
        )
        assertEquals(
            "tren_cercanias_c3_11_aranjuez",
            MadridTransitCatalog.searchOptions("atocha c3 aranjuez", kind = MadridTransitKind.TRAIN)
                .first()
                .id,
        )
        assertEquals(
            "tren_cercanias_c1_142_aeropuerto_t4",
            MadridTransitCatalog.searchOptions("aeropuerto t4 c1", kind = MadridTransitKind.TRAIN)
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

    @Test
    fun nonBlankSearchRespectsResultLimit() {
        val results = MadridTransitCatalog.searchOptions("moncloa", kind = MadridTransitKind.BUS, limit = 3)

        assertEquals(3, results.size)
    }
}
