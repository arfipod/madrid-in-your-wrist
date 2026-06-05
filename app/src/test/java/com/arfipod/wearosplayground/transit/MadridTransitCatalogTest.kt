package com.arfipod.wearosplayground.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadridTransitCatalogTest {
    @Test
    fun defaultFavoritesIncludeMetroAndBusAtHome() {
        val kinds = MadridTransitCatalog.defaultFavorites.map { favorite -> favorite.option.kind }.toSet()
        val places = MadridTransitCatalog.defaultFavorites.map { favorite -> favorite.place }.toSet()

        assertEquals(setOf(MadridTransitKind.METRO, MadridTransitKind.BUS), kinds)
        assertEquals(setOf(MadridTransitPlace.HOME), places)
    }

    @Test
    fun selectablePlacesMatchDailyContexts() {
        assertEquals(
            listOf(MadridTransitPlace.HOME, MadridTransitPlace.WORK, MadridTransitPlace.MARIA),
            MadridTransitPlace.selectable,
        )
        assertEquals(MadridTransitPlace.MARIA, MadridTransitPlace.fromId("maria"))
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
                place = MadridTransitPlace.HOME,
            ),
            MadridTransitCatalog.favoriteFor(
                optionId = "bus_e3_daroca_valderrivas",
                count = 1,
                place = MadridTransitPlace.WORK,
            ),
        )
        val raw = MadridTransitFavoritesCodec.encode(favorites) +
            ";unknown,4,maria;metro_l4_arguelles_pinar,2,home"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals("metro_l4_arguelles_pinar", decoded[0].option.id)
        assertEquals(3, decoded[0].clampedCount)
        assertEquals(MadridTransitPlace.HOME, decoded[0].place)
        assertEquals("bus_e3_daroca_valderrivas", decoded[1].option.id)
        assertEquals(1, decoded[1].clampedCount)
        assertEquals(MadridTransitPlace.WORK, decoded[1].place)
    }

    @Test
    fun sameOptionCanBeSavedInDifferentPlaces() {
        val raw = "metro_l4_arguelles_pinar,2,home;metro_l4_arguelles_pinar,1,work"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals(MadridTransitPlace.HOME, decoded[0].place)
        assertEquals(MadridTransitPlace.WORK, decoded[1].place)
    }

    @Test
    fun legacyFavoritesWithoutPlaceDefaultToHome() {
        val decoded = MadridTransitFavoritesCodec.decode("metro_l4_arguelles_pinar,3")

        assertEquals(1, decoded.size)
        assertEquals(MadridTransitPlace.HOME, decoded.single().place)
        assertEquals(3, decoded.single().clampedCount)
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
