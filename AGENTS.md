# Agent guide

This repository is a minimal Wear OS closed-loop playground. Prefer small,
reversible changes that keep the build, ADB Wi-Fi install, launch, screenshot,
and logcat loop working.

## Non-negotiable project facts

```text
Package/namespace: com.arfipod.wearosplayground
Debug app ID:     com.arfipod.wearosplayground.debug
Main Activity:    com.arfipod.wearosplayground.MainActivity
Logcat tag:       WearLoop
Target device:    Pixel Watch 3 / Wear OS 6.1
```

Do not reintroduce `com.angelrubiodev`. The current package owner is `arfipod`.

## First steps

```bash
./scripts/doctor.sh
rg -n "angelrubiodev|com\\.arfipod|WearLoop" app scripts docs README.md
```

Read [docs/engineering-handbook.md](docs/engineering-handbook.md) before making
workflow changes.

## Build and test

Preferred local verification:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Docker verification when available:

```bash
./scripts/docker-build-apk.sh
```

If Docker is unavailable in WSL, use:

```bash
./scripts/gradle-build-local.sh
```

## Watch loop

Always select the watch explicitly when more than one ADB target may exist:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
```

Full local loop:

```bash
BUILD_MODE=local ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

Full Docker loop:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

## Editing guidance

- Preserve `scripts/docker-build-apk.sh`, `install-watch.sh`, `launch-watch.sh`,
  `screenshot-watch.sh`, and `logcat-watch.sh`.
- Keep scripts Bash-only and dependency-light.
- Put runtime outputs under `artifacts/`.
- Keep UI dependencies minimal unless the task explicitly expands the app.
- Keep sensor experiments opt-in and log through `WearLoop`.
- Keep TileService work on ProtoLayout, not Compose UI.
- Keep complication data sources short and data-only; watch faces render them.
- Add JVM unit tests for new non-UI behavior.
- Update docs when a workflow or command changes.

## Screenshot comparison

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
SCREENSHOT_COMPARE_MAX_DIFF_PIXELS=25 ./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

Install ImageMagick when pixel metrics or visual diffs are required.

## Handoff checklist

Report:

- What changed.
- Which commands passed.
- Whether Docker was actually available.
- Whether the APK was installed/launched on a real watch.
- Screenshot path when runtime validation was performed.
