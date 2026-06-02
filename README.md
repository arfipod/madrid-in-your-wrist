# wearos_playground

A minimal, Docker-first Wear OS playground for Pixel Watch 3 / Wear OS 6.1 development.

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

## What this initial app does

The app intentionally stays simple. It validates the end-to-end loop before adding sensors, tiles, complications, Health Services, background workers, or advanced Compose UI.

Current app features:

- Launchable Wear OS Activity.
- Shows build timestamp.
- Has a counter button.
- Emits logs with the tag `WearLoop`.
- Performs short haptic feedback on button press.
- Uses plain Android UI instead of Compose to minimize dependency friction in the first closed-loop milestone.

Compose for Wear OS can be added once this baseline loop is stable.

## Quick start

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

### 6. Watch logs

```bash
./scripts/logcat-watch.sh
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

## Important WSL note

Your WSL environment may not expose USB devices directly. This repo assumes ADB Wi-Fi for the real Pixel Watch 3.

The Android emulator is better managed from Android Studio on Windows. Keep Docker focused on reproducible builds and scripts.

## Suggested next milestones

1. Stabilize Docker build and ADB Wi-Fi install.
2. Add screenshot comparison support.
3. Add Compose for Wear OS.
4. Add tiles.
5. Add complications.
6. Add Health Services experiments.
7. Add sensor logging experiments.
8. Add Codex task prompts for repeatable app feature creation.
