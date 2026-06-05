package com.arfipod.wearosplayground.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadridTransitCatalogTest {
    @Test
    fun defaultFavoritesIncludeMetroAndBus() {
        val kinds = MadridTransitCatalog.defaultFavorites.map { it.option.kind }.toSet()

        assertEquals(setOf(MadridTransitKind.METRO, MadridTransitKind.BUS), kinds)
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
            MadridTransitCatalog.favoriteFor("metro_l4_arguelles_pinar", count = 3),
            MadridTransitCatalog.favoriteFor("bus_e3_daroca_valderrivas", count = 1),
        )
        val raw = MadridTransitFavoritesCodec.encode(favorites) +
            ";unknown,4;metro_l4_arguelles_pinar,2"

        val decoded = MadridTransitFavoritesCodec.decode(raw)

        assertEquals(2, decoded.size)
        assertEquals("metro_l4_arguelles_pinar", decoded[0].option.id)
        assertEquals(3, decoded[0].clampedCount)
        assertEquals("bus_e3_daroca_valderrivas", decoded[1].option.id)
        assertEquals(1, decoded[1].clampedCount)
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
