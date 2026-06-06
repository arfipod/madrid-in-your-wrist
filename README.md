# madrid-in-your-wrist

Wear OS app and closed-loop development workspace for `Madrid Wrist`, a Pixel
Watch 3 transport dashboard for Madrid Metro and EMT bus favorites.

The repository is optimized for AI-assisted iteration:

```text
edit Kotlin/Android code
-> build a debug APK
-> install on Pixel Watch 3 over ADB Wi-Fi
-> launch
-> capture screenshot
-> inspect logcat
-> iterate
```

## Project Facts

```text
Namespace:      com.arfipod.madridinyourwrist
Application ID: com.arfipod.madridinyourwrist
Debug app ID:   com.arfipod.madridinyourwrist.debug
Main Activity:  com.arfipod.madridinyourwrist.MainActivity
Logcat tag:     WearLoop
Target device:  Pixel Watch 3 / Wear OS 6.1
```

Host workflow: WSL2 Ubuntu, Docker, Gradle wrapper, VS Code/Codex, and ADB Wi-Fi.

## What The App Does

- Launches a Wear OS Activity for `Madrid Wrist`.
- Stores Metro and bus favorites directly on the watch.
- Groups favorites into generic profiles: `Perfil 1`, `Perfil 2`, `Perfil 3`.
- Shows the soonest cached or freshly loaded arrival in a top glance card.
- Adds Metro, Metro Ligero, EMT, and interurban bus options from watch
  search/pickers and nearby sorting.
- Supports on-watch text search across the local catalog by station, stop,
  stop ID, line, destination, and aliases, with accent-insensitive matching.
- Ships a generated local catalog from official CRTM GTFS feeds: 37,308
  line/stop/destination options at the time of generation.
- Uses each Metro line's own color in Metro labels and glance highlights.
- Lets each favorite configure visible arrival count and optional proximity
  trigger radius (`500m`, `1km`, `2km`).
- Refreshes only the selected profile.
- Skips automatic API refreshes when the selected profile already has a recent
  complete cache; tapping `↻` still forces a live refresh when online.
- Uses proximity trigger radii to avoid automatic API calls for favorites that
  are outside the selected distance.
- Reads only last-known location and rechecks it sparingly for proximity/nearby
  flows instead of running continuous location tracking.
- Uses live Metro/EMT APIs when validated internet is available. Options from
  feeds without a live integration remain searchable/addable as catalog-only
  favorites.
- Falls back to the last stored snapshot when offline or when an API refresh
  fails.
- Feeds the Tile and `SHORT_TEXT` complication from cached snapshots only.
- Keeps developer-only direct ADB routes for examples: Flappy Bird, API output,
  Metro, EMT, 3D, audio, and video.

## Repository Layout

```text
app/                         Android app module
app/src/main/...             Compose Activity, Madrid Wrist UI, Tile, complication
app/src/main/.../transit     Catalog, favorites, runtime, cache, refresh policy
app/src/main/.../examples    Direct developer example routes
app/src/test/...             JVM unit tests
scripts/                     Build, ADB, screenshot, logcat, bugreport helpers
tools/                       Reproducible data generators
docs/                        Product, workflow, API, and device documentation
artifacts/                   Runtime outputs, ignored by Git
```

## Documentation Map

- [Engineering handbook](docs/engineering-handbook.md): canonical workflow,
  invariants, build/test/device loops, CI, troubleshooting, and change rules.
- [Madrid Wrist](docs/madrid-in-your-wrist.md): product behavior, profiles,
  online/offline policy, catalog, screenshots, and code map.
- [Closed-loop workflow](docs/closed-loop-workflow.md): shortest build,
  install, launch, screenshot, and logcat commands.
- [Pixel Watch 3 setup](docs/pixel-watch-3-setup.md): ADB Wi-Fi pairing and
  target selection.
- [Docker Android SDK](docs/docker-android-sdk.md): reproducible build image and
  WSL notes.
- [Tile](docs/tiles-baseline.md): cached Madrid Wrist Tile behavior.
- [Complication](docs/complications-baseline.md): cached `SHORT_TEXT` data
  source behavior.
- [Screenshot comparison](docs/screenshot-comparison.md): PNG comparison and
  ImageMagick diff output.
- [Examples](docs/examples.md): direct developer routes and screenshots.
- [Metro NAP](docs/metro-madrid-nap.md): Metro GTFS/NAP setup and schedule
  behavior.
- [EMT OpenAPI](docs/emt-madrid-openapi.md): MobilityLabs credential setup and
  arrival behavior.
- [Sensor logging](docs/sensor-logging-experiments.md): opt-in accelerometer
  logger kept for future experiments.
- [Codex prompts](docs/codex-prompts.md): reusable prompts for future agent work.
- [Agent guide](AGENTS.md): concise operating rules for coding agents.

## Credentials

Transit credentials are optional for builds, but live Metro/EMT refreshes need
them. Keep them in `.env` or pass them as environment variables; never commit
real secrets.

```bash
cat > .env <<'EOF'
NAP_API_KEY=YOUR_KEY
EMT_CLIENT_ID=YOUR_CLIENT_ID
EMT_PASS_KEY=YOUR_PASS_KEY
EMT_EMAIL=YOUR_EMAIL
EMT_PASSWORD=YOUR_PASSWORD
EOF
```

The scripts source `.env` automatically. Gradle exposes values through
`BuildConfig`.

## Quick Start

Check the workstation and connected targets:

```bash
./scripts/doctor.sh
```

Build and test locally with the Gradle wrapper:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Build reproducibly with Docker:

```bash
./scripts/docker-build-image.sh
./scripts/docker-build-apk.sh
```

Pair/connect the watch:

```bash
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:ADB_PORT
adb devices -l
```

Install, launch, screenshot, and inspect logs:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

Or run the full loop:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

Use the local Gradle loop when Docker is unavailable:

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

## Useful Outputs

```text
APK:         app/build/outputs/apk/debug/app-debug.apk
Screenshots: artifacts/screenshots/watch-YYYYMMDD-HHMMSS.png
Diffs:       artifacts/screenshot-diffs/
Bugreports:  artifacts/bugreports/
```

Compare screenshots:

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

Install ImageMagick for pixel metrics and visual diff PNGs:

```bash
sudo apt install imagemagick
```

## VS Code Tasks

Use `Terminal -> Run Task...` for:

```text
doctor
docker: build image
docker: build apk
adb: devices
adb: install watch
adb: launch watch
adb: screenshot watch
adb: compare screenshots
adb: logcat WearLoop
```

## CI

GitHub Actions runs:

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

CI does not install on a watch. Runtime validation is still done through the
local ADB Wi-Fi loop.
