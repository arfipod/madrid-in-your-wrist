# Closed-loop workflow

The intended loop is:

```text
edit code
→ build APK
→ install on watch
→ launch
→ screenshot
→ logcat
→ iterate
```

## Full loop

```bash
./scripts/loop.sh
```

The default loop builds with Docker. If Docker is not available in the current
shell, use the local Gradle wrapper:

```bash
BUILD_MODE=local ./scripts/loop.sh
```

## Manual loop

```bash
./scripts/docker-build-apk.sh
./scripts/install-watch.sh
./scripts/launch-watch.sh
./scripts/screenshot-watch.sh
./scripts/logcat-watch.sh
```

Compare screenshots after capture:

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

Set `SCREENSHOT_COMPARE_MAX_DIFF_PIXELS` to allow small rendering differences.

For local builds without Docker:

```bash
./scripts/gradle-build-local.sh
```

For a specific connected watch or emulator:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/loop.sh
```

The install, launch, screenshot, logcat, and bugreport scripts now check that
the selected ADB target is in the `device` state before running.

The launch and screenshot steps also send `KEYCODE_WAKEUP` before interacting
with the watch. Override the screenshot wait if the display needs more time:

```bash
SCREENSHOT_WAKE_DELAY_SECONDS=2 ./scripts/screenshot-watch.sh
```

For unit test verification:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest
```

## What Codex should modify first

Recommended first tasks:

1. Improve the UI layout while keeping it dependency-light.
2. Add a Compose for Wear OS variant.
3. Add one health/sensor read experiment behind a feature flag.
4. Add screenshot capture after every successful launch.
5. Add a small testable domain module before growing UI complexity.

## Artifact policy

Runtime outputs should go under:

```text
artifacts/
├── screenshots/
├── screenshot-diffs/
├── bugreports/
└── logs/
```

`artifacts/` is ignored by Git.
