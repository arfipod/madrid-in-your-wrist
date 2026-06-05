# madrid-in-your-wrist

A Docker-first Wear OS app for Pixel Watch 3 / Wear OS 6.1 development, now
centered on the `Madrid Wrist` transport dashboard.

The goal of this repository is not to replace Android Studio. The goal is to create a fast closed loop for AI-assisted development:

```text
VS Code + Codex
→ edit Kotlin/Android code
→ reproducible Gradle build inside Docker
→ install APK on Pixel Watch 3 through ADB Wi-Fi
→ launch app
→ capture screenshot
→ inspect logcat
→ iterate
```

## Target

- Device: Google Pixel Watch 3
- OS: Wear OS 6.1
- App type: native Android/Wear OS app
- Language: Kotlin
- Build system: Gradle + Android Gradle Plugin
- Runtime validation: ADB wireless debugging
- Host workflow: WSL2 Ubuntu + Docker + VS Code

## Repository layout

```text
wearos_playground/
├── app/                         # Minimal Wear OS Android app
├── scripts/                     # Closed-loop build/install/launch/capture helpers
├── docs/                        # Setup and workflow docs
├── .vscode/                     # VS Code tasks for fast iteration
├── Dockerfile                   # Android SDK + Gradle build environment
├── docker-compose.yml           # Mounts the repo into the build container
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

## Documentation map

- [Engineering handbook](docs/engineering-handbook.md): end-to-end build,
  testing, ADB Wi-Fi, runtime validation, CI, troubleshooting, and change
  guidelines.
- [Madrid In Your Wrist](docs/madrid-in-your-wrist.md): product behavior,
  favorites, selectors, nearby sorting, screenshots, catalog seeds, and code
  map.
- [Closed-loop workflow](docs/closed-loop-workflow.md): the shortest build,
  install, launch, screenshot, and logcat loops.
- [Pixel Watch 3 setup](docs/pixel-watch-3-setup.md): pairing and connecting
  over ADB Wi-Fi.
- [Docker Android SDK environment](docs/docker-android-sdk.md): reproducible
  Docker build image details.
- [Screenshot comparison](docs/screenshot-comparison.md): compare captured watch
  PNGs and generate visual diffs when ImageMagick is available.
- [Sensor logging experiments](docs/sensor-logging-experiments.md): activate and
  verify accelerometer logging through `WearLoop`.
- [Tiles baseline](docs/tiles-baseline.md): minimal Wear OS Tile provider and
  verification notes.
- [Complications baseline](docs/complications-baseline.md): minimal
  `SHORT_TEXT` complication data source and picker notes.
- [Examples gallery](docs/examples.md): Flappy Bird, API output, 3D, audio,
  and video examples plus direct ADB launch commands.
- [Metro Madrid NAP example](docs/metro-madrid-nap.md): NAP API key setup and
  GTFS schedule calculation notes.
- [EMT Madrid OpenAPI example](docs/emt-madrid-openapi.md): MobilityLabs
  credential setup and E3 arrival notes.
- [Codex task prompts](docs/codex-prompts.md): reusable prompts for future
  agent-driven changes.
- [Agent guide](AGENTS.md): quick operating rules for coding agents.

## What the app does

The app keeps the fast closed loop from the original playground, but the launcher
now opens the `Madrid Wrist` transport dashboard.

Current app features:

- Launchable Wear OS Activity.
- Manages Metro and EMT bus favorites directly on the watch.
- Adds Metro stations and bus stops from separate watch pickers.
- Lets each favorite choose how many upcoming transports to show.
- Shows Metro de Madrid scheduled departures through the NAP/GTFS helper.
- Shows EMT Madrid realtime bus arrivals through the MobilityLabs helper.
- Sorts the local Metro/bus catalog by nearby location when permission and a
  last known location are available.
- Persists favorites and counts on the watch.
- Emits logs with the tag `WearLoop`.
- Performs short haptic feedback on watch actions.
- Uses Jetpack Compose for Wear OS for the baseline UI.
- Provides a minimal Wear OS Tile showing build status.
- Provides a minimal `SHORT_TEXT` complication data source.
- Keeps direct ADB routes for the old examples: Flappy Bird, API output, Metro,
  EMT, 3D rendering, audio, and video playback.

## Compose dependency choices

The app uses a deliberately small Compose surface:

- `androidx.activity:activity-compose` supplies `setContent` for the Activity.
- `androidx.compose:compose-bom` pins compatible stable Compose UI/runtime artifacts.
- `androidx.wear.compose:compose-material3` supplies Wear-specific Material 3 components.

Navigation, previews, animation add-ons, and other larger Compose integrations are
left out until the closed loop needs them.

Current versions track the AndroidX stable channel: Compose BOM `2026.05.00`,
Activity `1.13.0`, and Wear Compose `1.6.2`. The build uses Android Gradle
Plugin `8.9.1` with Gradle `8.11.1` so these AndroidX artifacts satisfy their
published metadata requirements.

## Tile dependency choices

The Tile baseline uses AndroidX Wear Tiles `1.6.0` with ProtoLayout `1.4.0`.
The Tile UI uses ProtoLayout Material 3, not Compose, because tiles are rendered
by the Wear OS tile renderer rather than by an Activity.

## Complication dependency choices

The complication baseline uses AndroidX Watch Face complication data source KTX
`1.3.0`. The app only provides raw complication data; it does not implement a
watch face or render complications itself.

## Quick start

Check the host environment first:

```bash
./scripts/doctor.sh
```

Optional transit credentials can live in a local `.env` file at the repository
root. The helper scripts load it automatically and pass these values into local
and Docker Gradle builds:

```bash
NAP_API_KEY=YOUR_KEY
EMT_CLIENT_ID=YOUR_CLIENT_ID
EMT_PASS_KEY=YOUR_PASS_KEY
EMT_EMAIL=YOUR_EMAIL
EMT_PASSWORD=YOUR_PASSWORD
```

### 1. Build the Docker image

```bash
./scripts/docker-build-image.sh
```

### 2. Build the APK inside Docker

```bash
./scripts/docker-build-apk.sh
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a local non-Docker build, use the Gradle wrapper:

```bash
./scripts/gradle-build-local.sh
```

This is useful in WSL while Docker Desktop or the Compose plugin is not
available inside the distro.

Run the JVM unit tests with:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest
```

### 3. Connect the Pixel Watch 3 over ADB Wi-Fi

On the watch:

```text
Settings → System → About → Build number → tap 7 times
Settings → Developer options → ADB debugging: ON
Settings → Developer options → Wireless debugging: ON
```

Then pair/connect from the host:

```bash
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:ADB_PORT
adb devices -l
```

For details, see:

```text
docs/pixel-watch-3-setup.md
```

### 4. Install and launch

```bash
./scripts/install-watch.sh
./scripts/launch-watch.sh
```

### 5. Capture screenshot

```bash
./scripts/screenshot-watch.sh
```

Screenshots are saved under:

```text
artifacts/screenshots/
```

The launch and screenshot scripts wake the watch first so captures are less
likely to record the ambient, charging, or launcher screen instead of the app.

### 6. Watch logs

```bash
./scripts/logcat-watch.sh
```

### 7. Compare screenshots

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

Install ImageMagick for pixel metrics and visual diff images:

```bash
sudo apt install imagemagick
```

## VS Code tasks

Open the repo in VS Code and run:

```text
Terminal → Run Task...
```

Available tasks:

- `doctor`
- `docker: build image`
- `docker: build apk`
- `adb: devices`
- `adb: install watch`
- `adb: launch watch`
- `adb: screenshot watch`
- `adb: compare screenshots`
- `adb: logcat WearLoop`

## Recommended closed loop

```bash
./scripts/docker-build-apk.sh \
  && ./scripts/install-watch.sh \
  && ./scripts/launch-watch.sh \
  && ./scripts/screenshot-watch.sh
```

Or:

```bash
./scripts/loop.sh
```

If more than one device or emulator is connected, set `ANDROID_SERIAL` before
running install, launch, screenshot, logcat, or loop scripts:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
```

When Docker is unavailable locally, the same closed loop can use the Gradle
wrapper:

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

## CI

GitHub Actions runs the debug unit tests and assembles the debug APK on pushes
and pull requests to `main`.

## Important WSL note

Your WSL environment may not expose USB devices directly. This repo assumes ADB Wi-Fi for the real Pixel Watch 3.

The Android emulator is better managed from Android Studio on Windows. Keep Docker focused on reproducible builds and scripts.

## Suggested next milestones

1. Expand the Metro and EMT catalogs beyond the initial curated seed list.
2. Add manual station/stop search from the watch.
3. Add an Android phone companion for faster list management.
