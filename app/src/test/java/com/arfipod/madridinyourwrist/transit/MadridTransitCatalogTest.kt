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
    fun selectablePlacesAreGenericProfilesWithLegacyIds() {
        assertEquals(
            listOf(MadridTransitPlace.PROFILE_1, MadridTransitPlace.PROFILE_2, MadridTransitPlace.PROFILE_3),
            MadridTransitPlace.selectable,
        )
        assertEquals("Perfil 1", MadridTransitPlace.PROFILE_1.label)
        assertEquals("P2", MadridTransitPlace.PROFILE_2.shortLabel)
        assertEquals(MadridTransitPlace.PROFILE_1, MadridTransitPlace.fromId("home"))
        assertEquals(MadridTransitPlace.PROFILE_3, MadridTransitPlace.fromId("maria"))
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
                optionId = "metro_l4_arguelles_pinar",
                count = 3,
                place = MadridTransitPlace.PROFILE_1,
            ),
            MadridTransitCatalog.favoriteFor(
                optionId = "bus_e3_daroca_valderrivas",
                count = 1,
                place = MadridTransitPlace.PROFILE_2,
                proximityTriggerMeters = 1000,
            ),
        )
        val raw = MadridTransitFavoritesCodec.encode(favorites) +
            ";unknown,4,maria;metro_l4_arguelles_pinar,2,home"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals("metro_l4_arguelles_pinar", decoded[0].option.id)
        assertEquals(3, decoded[0].clampedCount)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded[0].place)
        assertEquals("bus_e3_daroca_valderrivas", decoded[1].option.id)
        assertEquals(1, decoded[1].clampedCount)
        assertEquals(MadridTransitPlace.PROFILE_2, decoded[1].place)
        assertEquals(1000, decoded[1].normalizedProximityTriggerMeters)
    }

    @Test
    fun sameOptionCanBeSavedInDifferentPlaces() {
        val raw = "metro_l4_arguelles_pinar,2,home;metro_l4_arguelles_pinar,1,work"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded[0].place)
        assertEquals(MadridTransitPlace.PROFILE_2, decoded[1].place)
    }

    @Test
    fun legacyFavoritesWithoutPlaceDefaultToHome() {
        val decoded = MadridTransitFavoritesCodec.decode("metro_l4_arguelles_pinar,3")

        assertEquals(1, decoded.size)
        assertEquals(MadridTransitPlace.PROFILE_1, decoded.single().place)
        assertEquals(3, decoded.single().clampedCount)
    }

    @Test
    fun proximityTriggerCyclesThroughSupportedRadii() {
        val favorite = requireNotNull(
            MadridTransitCatalog.favoriteFor("metro_l4_arguelles_pinar")
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
                optionId = "metro_l4_arguelles_pinar",
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

        assertEquals("metro_l2_goya_las_rosas", nearby.first().first.id)
        assertTrue(nearby.first().second < nearby.last().second)
    }
}
