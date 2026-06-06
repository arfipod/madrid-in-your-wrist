package com.arfipod.madridinyourwrist.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadridTransitCatalogTest {
    @Test
    fun defaultFavoritesIncludeMetroAndBusInFirstProfile() {
        val kinds = MadridTransitCatalog.defaultFavorites.map { favorite -> favorite.option.kind }.toSet()
        val places = MadridTransitCatalog.defaultFavorites.map { favorite -> favorite.place }.toSet()

        assertEquals(setOf(MadridTransitKind.METRO, MadridTransitKind.BUS), kinds)
        assertEquals(setOf(MadridTransitPlace.PROFILE_1), places)
    }

    @Test
    fun generatedOfficialCatalogIsAvailable() {
        assertEquals(37_308, MadridGeneratedTransitCatalog.OPTION_COUNT)
        assertTrue(MadridGeneratedTransitCatalog.options.size >= 37_000)
        assertTrue(MadridTransitCatalog.metroOptions.size >= 590)
        assertTrue(MadridTransitCatalog.busOptions.size >= 36_000)
        assertTrue(MadridTransitCatalog.allOptions.size >= MadridGeneratedTransitCatalog.OPTION_COUNT)
    }

    @Test
    fun generatedCatalogIncludesLiveAndStaticSources() {
        val emt = requireNotNull(MadridTransitCatalog.optionById("bus_emt_001_5135_moncloa"))
        val interurban = requireNotNull(
            MadridTransitCatalog.optionById("bus_interurbano_421_08046_p_delicias_plaza_de_legazpi")
        )
        val lightRail = requireNotNull(MadridTransitCatalog.optionById("metro_ligero_ml1_5_las_tablas"))

        assertEquals(MadridTransitSource.EMT_OPENAPI, emt.source)
        assertTrue(emt.source.hasLiveArrivals)
        assertEquals(MadridTransitSource.CRTM_STATIC_GTFS, interurban.source)
        assertTrue(!interurban.source.hasLiveArrivals)
        assertEquals(MadridTransitSource.CRTM_STATIC_GTFS, lightRail.source)
    }

    @Test
    fun selectablePlacesAreGenericProfiles() {
        assertEquals(
            listOf(MadridTransitPlace.PROFILE_1, MadridTransitPlace.PROFILE_2, MadridTransitPlace.PROFILE_3),
            MadridTransitPlace.selectable,
        )
        assertEquals("Perfil 1", MadridTransitPlace.PROFILE_1.label)
        assertEquals("P2", MadridTransitPlace.PROFILE_2.shortLabel)
        assertEquals(null, MadridTransitPlace.fromId("home"))
        assertEquals(null, MadridTransitPlace.fromId("maria"))
        assertEquals(MadridTransitPlace.PROFILE_2, MadridTransitPlace.fromId("profile_2"))
    }

    @Test
    fun favoriteCountsAreClampedForWatchUi() {
        val option = MadridTransitCatalog.metroOptions.first()

        assertEquals(
            MadridTransitCounts.MIN,
            MadridTransitFavorite(option = option, count = -1).clampedCount,
        )
        assertEquals(
            MadridTransitCounts.MAX,
            MadridTransitFavorite(option = option, count = 99).clampedCount,
        )
    }

    @Test
    fun favoritesCodecRoundTripsKnownOptionsAndDropsUnknownOnes() {
        val favorites = listOfNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "metro_4_54_pinar_de_chamartin",
                count = 3,
                place = MadridTransitPlace.PROFILE_1,
            ),
            MadridTransitCatalog.favoriteFor(
                optionId = "bus_emt_e3_1064_valderrivas",
                count = 1,
                place = MadridTransitPlace.PROFILE_2,
                proximityTriggerMeters = 1000,
                customName = "Bus rápido",
                customIcon = "E3",
            ),
        )
        val raw = MadridTransitFavoritesCodec.encode(favorites) +
            ";unknown,4,profile_3;metro_4_54_pinar_de_chamartin,2,profile_1"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals("metro_4_54_pinar_de_chamartin", decoded[0].option.id)
        assertEquals(3, decoded[0].clampedCount)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded[0].place)
        assertEquals("bus_emt_e3_1064_valderrivas", decoded[1].option.id)
        assertEquals(1, decoded[1].clampedCount)
        assertEquals(MadridTransitPlace.PROFILE_2, decoded[1].place)
        assertEquals(1000, decoded[1].normalizedProximityTriggerMeters)
        assertEquals("Bus rápido", decoded[1].normalizedCustomName)
        assertEquals("E3", decoded[1].normalizedCustomIcon)
    }

    @Test
    fun sameOptionCanBeSavedInDifferentPlaces() {
        val raw = "metro_4_54_pinar_de_chamartin,2,profile_1;metro_4_54_pinar_de_chamartin,1,profile_2"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded[0].place)
        assertEquals(MadridTransitPlace.PROFILE_2, decoded[1].place)
    }

    @Test
    fun favoritesWithoutPlaceDefaultToFirstProfile() {
        val decoded = MadridTransitFavoritesCodec.decode("metro_4_54_pinar_de_chamartin,3")

        assertEquals(1, decoded.size)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded.single().place)
        assertEquals(3, decoded.single().clampedCount)
        assertEquals(null, decoded.single().normalizedCustomName)
        assertEquals(null, decoded.single().normalizedCustomIcon)
    }

    @Test
    fun favoriteCustomizationNormalizesKeyboardText() {
        val favorite = requireNotNull(
            MadridTransitCatalog.favoriteFor("metro_4_54_pinar_de_chamartin")
        ).withCustomization(
            name = "  Argüelles\nMetro favorito con texto demasiado largo  ",
            icon = " L4!!extra ",
        )

        assertEquals("Argüelles Metro favorito con", favorite.normalizedCustomName)
        assertEquals("L4!!", favorite.normalizedCustomIcon)
    }

    @Test
    fun proximityTriggerCyclesThroughSupportedRadii() {
        val favorite = requireNotNull(
            MadridTransitCatalog.favoriteFor("metro_4_54_pinar_de_chamartin")
        )

        assertEquals(500, favorite.withNextProximityTrigger().normalizedProximityTriggerMeters)
        assertEquals(
            1000,
            favorite.withNextProximityTrigger()
                .withNextProximityTrigger()
                .normalizedProximityTriggerMeters,
        )
        assertEquals(
            null,
            favorite.copy(proximityTriggerMeters = 2000)
                .withNextProximityTrigger()
                .normalizedProximityTriggerMeters,
        )
    }

    @Test
    fun proximityTriggerUsesDistanceWhenEnabled() {
        val favorite = requireNotNull(
            MadridTransitCatalog.favoriteFor(
                optionId = "metro_4_54_pinar_de_chamartin",
                proximityTriggerMeters = 500,
            )
        )

        assertTrue(favorite.isWithinProximityTrigger(300))
        assertTrue(!favorite.isWithinProximityTrigger(700))
        assertTrue(!favorite.isWithinProximityTrigger(null))
        assertTrue(favorite.copy(proximityTriggerMeters = null).isWithinProximityTrigger(null))
    }

    @Test
    fun blankStoredFavoritesMeansUserClearedTheList() {
        assertTrue(MadridTransitFavoritesCodec.decode("").isEmpty())
    }

    @Test
    fun missingStoredFavoritesUseDefaults() {
        assertEquals(
            MadridTransitCatalog.defaultFavorites,
            MadridTransitFavoritesCodec.decode(null),
        )
    }

    @Test
    fun nearbyOptionsAreSortedByDistance() {
        val goya = MadridGeoPoint(latitude = 40.4247, longitude = -3.6757)

        val nearby = MadridTransitCatalog.nearbyOptions(
            from = goya,
            kind = MadridTransitKind.METRO,
            limit = 2,
        )

        assertEquals("Goya L2", nearby.first().first.label)
        assertTrue(nearby.first().second <= nearby.last().second)
    }
}
