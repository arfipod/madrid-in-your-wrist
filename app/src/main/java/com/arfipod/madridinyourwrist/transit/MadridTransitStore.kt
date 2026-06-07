package com.arfipod.madridinyourwrist.transit

import android.content.Context
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal const val MADRID_TRANSIT_PREFS_NAME = "madrid_transit"
private const val KEY_FAVORITES = "favorites"
private const val KEY_SELECTED_PLACE = "selected_place"
private const val KEY_PROFILES = "profiles"

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

    fun loadProfiles(): Map<MadridTransitPlace, MadridTransitPlaceProfile> {
        return MadridTransitProfilesCodec.decode(preferences.getString(KEY_PROFILES, null))
    }

    fun loadProfile(place: MadridTransitPlace): MadridTransitPlaceProfile {
        return MadridTransitProfileCustomization.profileFor(loadProfiles(), place)
    }

    fun saveProfiles(profiles: Map<MadridTransitPlace, MadridTransitPlaceProfile>) {
        preferences.edit()
            .putString(KEY_PROFILES, MadridTransitProfilesCodec.encode(profiles))
            .apply()
    }
}

object MadridTransitProfilesCodec {
    fun encode(profiles: Map<MadridTransitPlace, MadridTransitPlaceProfile>): String {
        return MadridTransitPlace.selectable
            .mapNotNull { place ->
                val profile = profiles[place] ?: return@mapNotNull null
                val customName = MadridTransitProfileCustomization.normalizeName(profile.customName)
                val iconId = profile.icon?.id
                if (customName == null && iconId == null) return@mapNotNull null
                listOf(
                    place.id,
                    customName.orEmpty().encodedPreferenceField(),
                    iconId.orEmpty().encodedPreferenceField(),
                ).joinToString(separator = ",")
            }
            .joinToString(separator = ";")
    }

    fun decode(raw: String?): Map<MadridTransitPlace, MadridTransitPlaceProfile> {
        val defaults = MadridTransitProfileCustomization.defaultProfiles()
        if (raw.isNullOrBlank()) return defaults

        val decodedProfiles = raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(",")
                val place = MadridTransitPlace.fromId(parts.getOrNull(0)) ?: return@mapNotNull null
                val name = parts.getOrNull(1)?.decodedPreferenceField()
                val icon = MadridTransitProfileIcon.fromId(parts.getOrNull(2)?.decodedPreferenceField())
                place to MadridTransitPlaceProfile(place = place).withCustomization(
                    name = name,
                    nextIcon = icon,
                )
            }
            .toMap()

        return defaults + decodedProfiles
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
                favorite.normalizedCustomName.orEmpty().encodedPreferenceField(),
                favorite.normalizedCustomIcon.orEmpty().encodedPreferenceField(),
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
                val customName = parts.getOrNull(4)?.decodedPreferenceField()
                val customIcon = parts.getOrNull(5)?.decodedPreferenceField()
                MadridTransitCatalog.favoriteFor(
                    optionId = optionId,
                    count = count,
                    place = place,
                    proximityTriggerMeters = proximityTriggerMeters,
                    customName = customName,
                    customIcon = customIcon,
                )
            }
            .distinctBy { favorite -> "${favorite.place.id}:${favorite.option.id}" }
    }

}

private fun String.encodedPreferenceField(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())

private fun String.decodedPreferenceField(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.name())
