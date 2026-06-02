package com.arfipod.wearosplayground.examples

enum class ExampleKind(
    val route: String,
    val buttonLabel: String,
    val title: String,
) {
    FLAPPY(route = "flappy", buttonLabel = "FLAPPY", title = "Flappy Bird"),
    API(route = "api", buttonLabel = "API", title = "API Output"),
    METRO(route = "metro", buttonLabel = "METRO", title = "Metro Madrid"),
    EMT(route = "emt", buttonLabel = "EMT E3", title = "EMT E3"),
    CUBE_3D(route = "3d", buttonLabel = "3D CUBE", title = "3D Cube"),
    AUDIO(route = "audio", buttonLabel = "AUDIO", title = "Audio"),
    VIDEO(route = "video", buttonLabel = "VIDEO", title = "Video");

    companion object {
        fun fromRoute(route: String?): ExampleKind? {
            val normalized = route
                ?.trim()
                ?.lowercase()
                ?.replace("_", "-")
                ?: return null

            return when (normalized) {
                "flappy", "flappy-bird" -> FLAPPY
                "api", "network" -> API
                "metro", "metro-madrid", "nap" -> METRO
                "emt", "emt-e3", "bus", "e3" -> EMT
                "3d", "cube", "cube-3d", "three-d" -> CUBE_3D
                "audio", "sound" -> AUDIO
                "video", "movie" -> VIDEO
                else -> entries.firstOrNull { it.route == normalized }
            }
        }
    }
}
