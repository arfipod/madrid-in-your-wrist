# Tiles baseline

The app includes a minimal Wear OS Tile provider. The goal is to validate the
Tile plumbing without introducing app state synchronization, remote resources,
or high-frequency updates yet.

## Runtime behavior

The tile shows:

- `Wear Loop` as the title.
- The current debug build timestamp as the main text.
- A short footer: `Open app for counter and sensors`.

The tile refresh interval is intentionally conservative: 15 minutes. Tiles
should not be used as a high-frequency logging surface.

## Code map

```text
WearLoopTileService.kt        TileService entry point registered in the manifest
WearLoopTileContent.kt        Small testable text/freshness constants
WearLoopTileContentTest.kt    JVM tests for stable tile content
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
Label: Wear Loop
Description: Quick status tile for the Wear Loop playground.
Service: com.arfipod.wearosplayground.WearLoopTileService
```

