# Tiles baseline

The app includes a Wear OS Tile provider for Madrid Wrist. The Activity is the
only surface that refreshes Metro and EMT data; the tile reads the last cached
snapshot for the selected context from `SharedPreferences`.

## Runtime behavior

When cached data is available, the tile shows:

- The selected context and app name, for example `Casa · Madrid`.
- The compact next transit result, for example `E3 4m`.
- The favorite label and cache time, for example `Daroca E3 · 08:15`.

Without cached data, it shows `Sin datos` and prompts the user to open the app
and refresh. The tile refresh interval is intentionally conservative: 5 minutes.
Tiles should not be used as a network refresh surface.

## Code map

```text
WearLoopTileService.kt        TileService entry point registered in the manifest
WearLoopTileContent.kt        Small testable text/freshness helpers
WearLoopTileContentTest.kt    JVM tests for cached and fallback tile content
transit/MadridTransitSnapshot.kt Cached next-arrival snapshot model and codec
```

## Dependencies

The tile uses the AndroidX Wear Tiles and ProtoLayout stack:

```kotlin
implementation("androidx.wear.tiles:tiles:1.6.0")
implementation("androidx.wear.protolayout:protolayout:1.4.0")
implementation("androidx.wear.protolayout:protolayout-material3:1.4.0")
debugImplementation("androidx.wear.tiles:tiles-renderer:1.6.0")
```

Keep future tile work on ProtoLayout. Do not use Compose UI inside a TileService.

## Build verification

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

## Device verification

Install the app on the watch:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
```

Then add the tile manually from the watch or paired phone tile picker. Tiles are
not launched like activities. During active development, Android Studio also
supports a Wear OS Tile run/debug configuration that deploys and activates a tile
for faster iteration.

Expected tile provider metadata:

```text
Label: Madrid Wrist
Description: Quick status tile for Madrid Wrist.
Service: com.arfipod.wearosplayground.WearLoopTileService
```
