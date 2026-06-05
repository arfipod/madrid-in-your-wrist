package com.arfipod.wearosplayground.transit

import android.content.Context

private const val PREFS_NAME = "madrid_transit"
private const val KEY_FAVORITES = "favorites"

class MadridTransitStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFavorites(): List<MadridTransitFavorite> {
        val raw = preferences.getString(KEY_FAVORITES, null)
        return MadridTransitFavoritesCodec.decode(raw)
    }

    fun saveFavorites(favorites: List<MadridTransitFavorite>) {
        preferences.edit()
            .putString(KEY_FAVORITES, MadridTransitFavoritesCodec.encode(favorites))
            .apply()
    }
}

object MadridTransitFavoritesCodec {
    fun encode(favorites: List<MadridTransitFavorite>): String {
        return favorites.joinToString(separator = ";") { favorite ->
            "${favorite.option.id},${favorite.clampedCount}"
        }
    }

    fun decode(raw: String?): List<MadridTransitFavorite> {
        if (raw == null) return MadridTransitCatalog.defaultFavorites
        if (raw.isBlank()) return emptyList()

        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(",")
                val optionId = parts.getOrNull(0).orEmpty()
                val count = parts.getOrNull(1)?.toIntOrNull() ?: MadridTransitCounts.DEFAULT
                MadridTransitCatalog.favoriteFor(optionId = optionId, count = count)
            }
            .distinctBy { it.option.id }
    }
}
