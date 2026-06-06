package com.arfipod.madridinyourwrist.transit

import android.content.Context

internal const val MADRID_TRANSIT_PREFS_NAME = "madrid_transit"
private const val KEY_FAVORITES = "favorites"
private const val KEY_SELECTED_PLACE = "selected_place"

class MadridTransitStore(context: Context) {
    private val preferences = context.getSharedPreferences(MADRID_TRANSIT_PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFavorites(): List<MadridTransitFavorite> {
        val raw = preferences.getString(KEY_FAVORITES, null)
        return MadridTransitFavoritesCodec.decode(raw)
    }

    fun saveFavorites(favorites: List<MadridTransitFavorite>) {
        preferences.edit()
            .putString(KEY_FAVORITES, MadridTransitFavoritesCodec.encode(favorites))
            .apply()
    }

    fun loadSelectedPlace(): MadridTransitPlace {
        return MadridTransitPlace.fromId(preferences.getString(KEY_SELECTED_PLACE, null))
            ?: MadridTransitPlace.DEFAULT
    }

    fun saveSelectedPlace(place: MadridTransitPlace) {
        preferences.edit()
            .putString(KEY_SELECTED_PLACE, place.id)
            .apply()
    }
}

object MadridTransitFavoritesCodec {
    fun encode(favorites: List<MadridTransitFavorite>): String {
        return favorites.joinToString(separator = ";") { favorite ->
            listOf(
                favorite.option.id,
                favorite.clampedCount.toString(),
                favorite.place.id,
                favorite.normalizedProximityTriggerMeters?.toString().orEmpty(),
            ).joinToString(separator = ",")
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
                val place = MadridTransitPlace.fromId(parts.getOrNull(2)) ?: MadridTransitPlace.DEFAULT
                val proximityTriggerMeters = parts.getOrNull(3)?.toIntOrNull()
                MadridTransitCatalog.favoriteFor(
                    optionId = optionId,
                    count = count,
                    place = place,
                    proximityTriggerMeters = proximityTriggerMeters,
                )
            }
            .distinctBy { favorite -> "${favorite.place.id}:${favorite.option.id}" }
    }
}
