# Docker Android SDK environment

The Docker image contains:

- Ubuntu 24.04
- OpenJDK 17
- Android command-line tools
- Android SDK platform tools
- Android platform API 36
- Android build tools 35.0.0
- Gradle 8.10.2

## Build image

```bash
./scripts/docker-build-image.sh
```

## Build app

```bash
./scripts/docker-build-apk.sh
```

## Why the emulator is not inside Docker

The Android emulator depends heavily on host virtualization and GPU acceleration. For WSL-based development, the most robust setup is:

- Docker: reproducible builds.
- Android Studio on Windows: SDK manager and emulator/device manager.
- ADB Wi-Fi: real Pixel Watch 3 deployment.

This avoids coupling project builds to fragile nested virtualization/GPU setups.
