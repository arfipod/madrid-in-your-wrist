# Closed-Loop Workflow

Use this page when you only need the command loop. Use
[`engineering-handbook.md`](engineering-handbook.md) for the full operating
manual.

## Required First Check

```bash
./scripts/doctor.sh
```

The expected target is a Pixel Watch 3 in `device` state. If more than one ADB
target is visible, pass `ANDROID_SERIAL` explicitly in every device command.

## Keep ADB Wi-Fi Alive

Wireless debugging can drop during idle sessions. Leave this running in a
terminal while iterating:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
```

By default it sends a cheap ADB ping every 25 seconds and does not wake the
display. Tune it when needed:

```bash
ADB_KEEP_ALIVE_INTERVAL_SECONDS=15 ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
ADB_KEEP_ALIVE_WAKE_EVERY=20 ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
```

## Full Loop

Docker build, install, launch, and screenshot:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

Local Gradle build instead of Docker:

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

## Manual Loop

```bash
./scripts/docker-build-apk.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

For local builds:

```bash
./scripts/gradle-build-local.sh
```

## Tests

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

## Screenshot Comparison

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
SCREENSHOT_COMPARE_MAX_DIFF_PIXELS=25 ./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

Install ImageMagick for pixel metrics and visual diff images.

## Script Behavior

- Device scripts source `scripts/common.sh`.
- `ANDROID_SERIAL` selects the watch or emulator.
- ADB scripts fail early unless the selected target is in `device` state.
- Launch and screenshot scripts wake the display first.
- `SCREENSHOT_WAKE_DELAY_SECONDS` controls screenshot wait time.
- Runtime outputs are written under `artifacts/`, which is ignored by Git.
