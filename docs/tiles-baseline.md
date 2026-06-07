# Madrid Wrist Tile

The app includes a Wear OS Tile provider for Madrid Wrist. The Activity is the
only surface that refreshes Metro and EMT data; the Tile reads the last cached
snapshot for the selected profile from `SharedPreferences`.

## Behavior

With cached data:

```text
Title:  Trabajo · Madrid
Body:   E3 4m
Footer: Daroca E3 · 08:15
```

The title uses the selected profile's custom name when one is stored.

Without cached data, it shows `Sin datos` and prompts the user to open the app
and refresh. The Tile freshness interval is 15 minutes because the Tile only
reads cache; live refreshes happen in the Activity. Tiles must stay network-free
and should not run Compose UI.

## Code Map

```text
WearLoopTileService.kt          TileService registered in AndroidManifest.xml
WearLoopTileContent.kt          Testable text/freshness helper
WearLoopTileContentTest.kt      JVM tests
transit/MadridTransitSnapshot.kt Cached next-arrival snapshot model and codec
```

## Dependencies

```kotlin
implementation("androidx.wear.tiles:tiles:1.6.0")
implementation("androidx.wear.protolayout:protolayout:1.4.0")
implementation("androidx.wear.protolayout:protolayout-material3:1.4.0")
debugImplementation("androidx.wear.tiles:tiles-renderer:1.6.0")
```

## Verify

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
```

Add the Tile manually from the watch or paired phone tile picker. Tiles are not
launched like activities. Android Studio can also deploy/activate a Tile through
a Wear OS Tile run configuration.

Expected provider metadata:

```text
Label: Madrid Wrist
Description: Quick status tile for Madrid Wrist.
Service: com.arfipod.madridinyourwrist.WearLoopTileService
```
