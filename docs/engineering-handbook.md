# Engineering handbook

This document is the working manual for engineers and coding agents operating in
this repository. It explains the project invariants, the build and device loops,
and the shortest reliable path from a code change to a verified Pixel Watch 3
run.

## Project purpose

`wearos_playground` is a small Wear OS app used to validate a fast closed loop:

```text
edit Kotlin/Android code
-> build a debug APK
-> install it on a Pixel Watch 3 over ADB Wi-Fi
-> launch it
-> capture a screenshot
-> inspect logcat
-> iterate
```

The repository should stay dependency-light until the closed loop is stable.
Avoid adding large frameworks, architecture layers, services, background work,
or sensor integrations unless the current task explicitly asks for them.

## Stable identifiers

Keep these identifiers consistent across Gradle, source files, tests, and
scripts:

```text
Namespace:      com.arfipod.wearosplayground
Application ID: com.arfipod.wearosplayground
Debug app ID:   com.arfipod.wearosplayground.debug
Main Activity:  com.arfipod.wearosplayground.MainActivity
Logcat tag:     WearLoop
```

The debug build adds `.debug` through `applicationIdSuffix`, so ADB launch
commands must use the debug app ID and the non-suffixed activity class name.

## Repository map

```text
app/                         Android app module
app/src/main/...             Compose Wear OS Activity and app code
app/src/main/...Tile*.kt      Wear OS Tile provider and tile content helpers
app/src/main/...Complication*.kt Wear OS complication data source helpers
app/src/test/...             JVM unit tests
scripts/                     Build, ADB, screenshot, logcat, and loop helpers
docs/                        Workflow and environment documentation
.github/workflows/android.yml GitHub Actions unit-test and debug build workflow
Dockerfile                   Reproducible Android SDK build image
docker-compose.yml           Compose service for Docker builds
gradle/wrapper/              Checked-in Gradle wrapper
```

Runtime outputs belong in `artifacts/`, which is ignored by Git.

## Build stack

The app currently uses:

- Gradle wrapper `8.11.1`.
- Android Gradle Plugin `8.9.1`.
- Kotlin `2.0.21`.
- Compose compiler plugin through `org.jetbrains.kotlin.plugin.compose`.
- Android SDK compile/target API `36`.
- Android build tools `35.0.0`.
- Java/Kotlin target `17`.

The Compose surface is intentionally small:

- `androidx.activity:activity-compose` for `setContent`.
- `androidx.compose:compose-bom` for compatible Compose UI artifacts.
- `androidx.wear.compose:compose-material3` for Wear-specific Material 3 UI.
- `androidx.wear.tiles:tiles` for the TileService entry point.
- `androidx.wear.protolayout:protolayout` for tile layout primitives.
- `androidx.wear.protolayout:protolayout-material3` for Material 3 tile UI.
- `androidx.wear.tiles:tiles-renderer` in debug builds for tile tooling.
- `androidx.wear.watchface:watchface-complications-data-source-ktx` for
  complication data sources.

The sensor experiment uses platform Android sensor APIs only. It does not add
new libraries.

## First command

Always start with:

```bash
./scripts/doctor.sh
```

This checks Docker, Docker Compose, ADB, local Gradle, the expected APK path,
and currently visible ADB targets.

## Build processes

### Docker build

Docker is the preferred reproducible build path:

```bash
./scripts/docker-build-image.sh
./scripts/docker-build-apk.sh
```

The Docker scripts support both `docker compose` and the legacy
`docker-compose` command. The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Local wrapper build

Use the local Gradle wrapper when Docker is unavailable inside WSL or during
fast iteration:

```bash
./scripts/gradle-build-local.sh
```

This script sources `scripts/common.sh`, which can automatically use
`~/Android/Sdk` and `~/opt/jdk-17` when they exist.

### Unit tests

Run the JVM unit tests with:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest
```

Run tests and build together with:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

## Pixel Watch 3 ADB Wi-Fi process

On the watch:

```text
Settings -> System -> About -> Build number -> tap 7 times
Settings -> Developer options -> ADB debugging: ON
Settings -> Developer options -> Wireless debugging: ON
```

Pair with the pairing address and pairing code:

```bash
adb pair WATCH_IP:PAIRING_PORT
```

Connect with the regular wireless debugging address:

```bash
adb connect WATCH_IP:ADB_PORT
./scripts/adb-devices.sh
```

Expected state:

```text
WATCH_IP:ADB_PORT device product:sol model:Pixel_Watch_3 device:sol ...
```

If multiple ADB targets are visible, always set `ANDROID_SERIAL`:

```bash
export ANDROID_SERIAL=WATCH_IP:ADB_PORT
```

Or pass it per command:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
```

## Closed-loop processes

### Full Docker loop

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

By default, `loop.sh` builds with Docker, installs the APK, launches the app,
waits briefly, and captures a screenshot.

### Full local loop

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

Use this when Docker is not available but the local Android SDK and Gradle
wrapper work.

### Manual loop

```bash
./scripts/docker-build-apk.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

For a local build, replace the first command with:

```bash
./scripts/gradle-build-local.sh
```

## Script behavior

All scripts that talk to the watch source `scripts/common.sh`.

Important shared behavior:

- `ANDROID_SERIAL` selects the target device or emulator.
- ADB commands fail early if the selected target is not in the `device` state.
- `install-watch.sh` requires the debug APK to exist first.
- `launch-watch.sh` sends `KEYCODE_WAKEUP` before launching the app.
- `screenshot-watch.sh` sends `KEYCODE_WAKEUP`, waits, then captures PNG output.
- Screenshot delay can be overridden with `SCREENSHOT_WAKE_DELAY_SECONDS`.
- `compare-screenshot.sh` compares two PNGs and writes visual diffs when
  ImageMagick is installed.

Useful commands:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/bugreport-watch.sh
```

Screenshot comparison:

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
SCREENSHOT_COMPARE_MAX_DIFF_PIXELS=25 ./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

## Logs and artifacts

Screenshots:

```text
artifacts/screenshots/watch-YYYYMMDD-HHMMSS.png
```

Screenshot diffs:

```text
artifacts/screenshot-diffs/diff-YYYYMMDD-HHMMSS.png
```

Bugreports:

```text
artifacts/bugreports/bugreport-YYYYMMDD-HHMMSS.zip
```

Logcat focus:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

The app logs with `WearLoop` when the Activity is created and when Madrid
transit refreshes complete or fail.

Madrid Wrist should prefer live API data only when validated internet is
available. When offline, or when a refresh fails, preserve and render the latest
stored snapshot for the selected profile instead of deleting useful cached data.

## Tiles baseline

The app registers `WearLoopTileService` as a Wear OS tile provider. It renders a
small ProtoLayout Material 3 tile backed by the last cached Madrid transit
snapshot for the selected profile. The Activity performs Metro/EMT refreshes;
the tile only reads the snapshot so tile rendering stays deterministic and
network-free.

Code map:

```text
WearLoopTileService.kt        TileService registered in AndroidManifest.xml
WearLoopTileContent.kt        Testable tile strings and refresh interval
WearLoopTileContentTest.kt    JVM tests for tile content
```

Tiles are not activities. Install the APK, then add the tile from the watch or
paired phone tile picker. Android Studio's Wear OS Tile run/debug configuration
can deploy and activate the tile during active development.

## Complications baseline

The app registers `WearLoopComplicationService` as a `SHORT_TEXT` complication
data source. It exposes a compact cached Madrid transit headline such as
`E3 4m` to compatible watch faces; watch faces control rendering. When no
snapshot is available, it falls back to short Madrid Wrist status text.

Code map:

```text
WearLoopComplicationService.kt        Data source service
WearLoopComplicationContent.kt        Testable strings and update interval
WearLoopComplicationContentTest.kt    JVM tests for text and metadata
ic_complication_wear_loop.xml         Monochrome picker icon
```

Complication data sources are not activities. Install the APK, then select
`Madrid Wrist` in a watch face complication picker. The update period is 300
seconds, the minimum regular interval enforced by the platform.

## Sensor logging experiment

The repository still includes a minimal accelerometer logging experiment helper,
but the Madrid Wrist launcher no longer exposes it from the home screen. Reusing
it in a future screen should keep activation opt-in.

Runtime behavior:

- Registers `Sensor.TYPE_ACCELEROMETER`.
- Logs one formatted sample per second with `WearLoop`.
- Logs start, stop, unavailable sensor, and accuracy changes.
- Stops listening when the owning screen or feature tears it down.

Expected log shape:

```text
WearLoop: Sensor experiment started: ...
WearLoop: Accelerometer t=...ms x=... y=... z=... |g|=...
WearLoop: Sensor experiment stopped
```

Code map:

```text
SensorExperimentLogger.kt      Android SensorEventListener wrapper
SensorSampleFormatter.kt       Pure formatter for log output
SensorSampleFormatterTest.kt   JVM tests for stable formatting
```

## Examples gallery

The app keeps compact examples under:

```text
app/src/main/java/com/arfipod/wearosplayground/examples/
```

Examples:

```text
flappy  Compose Canvas Flappy Bird-style game
api     HttpURLConnection GET to https://api.github.com/zen
metro   NAP/GTFS scheduled Metro de Madrid departure
emt     EMT E3 realtime arrival at Felipe II toward Valderrivas
3d      Software-projected rotating cube rendered in Compose Canvas
audio   Platform ToneGenerator tone playback
video   Platform VideoView streaming a small HTTPS MP4
```

Launch a specific example directly from ADB with:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example flappy
```

See `docs/examples.md` for the full route list and runtime validation loop.
See `docs/metro-madrid-nap.md` for NAP API key configuration.
See `docs/emt-madrid-openapi.md` for EMT MobilityLabs credential configuration.

## CI process

GitHub Actions runs on pushes and pull requests to `main`, plus manual
dispatch. The workflow:

1. Checks out the repository.
2. Sets up Temurin JDK 17.
3. Sets up the Android SDK.
4. Installs `platforms;android-36` and `build-tools;35.0.0`.
5. Runs:

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

CI does not install on a real watch. Runtime validation still happens through
the local ADB Wi-Fi loop.

## Troubleshooting

### Docker is missing

Symptom:

```text
error: Docker Compose not found.
```

Use the local loop:

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

Or enable Docker Desktop WSL integration / install Docker and the Compose plugin
inside the distro.

### ADB target is offline

Restart ADB and reconnect:

```bash
adb kill-server
adb start-server
adb connect WATCH_IP:ADB_PORT
./scripts/adb-devices.sh
```

The watch may show a new wireless debugging port after toggling wireless
debugging or reconnecting Wi-Fi.

### More than one target is connected

Set `ANDROID_SERIAL` before running scripts:

```bash
export ANDROID_SERIAL=WATCH_IP:ADB_PORT
```

### Screenshot shows ambient, charging, or launcher UI

The screenshot script already wakes the display. Increase the wait:

```bash
SCREENSHOT_WAKE_DELAY_SECONDS=2 ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
```

Then confirm the Activity is focused:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell dumpsys activity activities | rg "ResumedActivity|com.arfipod"
```

### Screenshot comparison fails without metrics

Install ImageMagick:

```bash
sudo apt install imagemagick
```

Without ImageMagick, `compare-screenshot.sh` only supports exact byte-identical
matches.

### App does not launch

Check the app ID and Activity pair:

```bash
source ./scripts/common.sh
echo "$APP_ID/$MAIN_ACTIVITY"
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start -n "$APP_ID/$MAIN_ACTIVITY"
```

Expected debug component:

```text
com.arfipod.wearosplayground.debug/com.arfipod.wearosplayground.MainActivity
```

## Change guidelines

When changing the app:

- Keep the package under `com.arfipod.wearosplayground`.
- Keep the `WearLoop` log tag unless intentionally replacing the logging flow.
- Keep `scripts/` compatible with `ANDROID_SERIAL`.
- Keep runtime outputs under `artifacts/`.
- Prefer small, testable Kotlin classes for non-UI logic.
- Keep sensor experiments opt-in from the UI or an explicit feature flag.
- Document sensor permissions and power cost when adding new sensors.
- Keep tile content small, glanceable, and backed by testable helpers.
- Do not use Compose UI inside a TileService; use ProtoLayout.
- Keep complication data short, privacy-safe, and backed by testable helpers.
- Do not implement a watch face unless explicitly requested.
- Add or update unit tests when behavior changes.
- Run at least `:app:testDebugUnitTest :app:assembleDebug` before finishing.
- If the task touches runtime behavior, run install, launch, screenshot, and
  logcat on the watch when ADB Wi-Fi is available.

## Good verification checklist

Use this checklist before handing off a change:

```bash
./scripts/doctor.sh
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

If Docker is available, also run:

```bash
./scripts/docker-build-apk.sh
```
