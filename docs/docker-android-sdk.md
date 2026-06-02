# Docker Android SDK environment

The Docker image contains:

- Ubuntu 24.04
- OpenJDK 17
- Android command-line tools
- Android SDK platform tools
- Android platform API 36
- Android build tools 35.0.0
- Gradle 8.11.1

## Build image

```bash
./scripts/docker-build-image.sh
```

## Build app

```bash
./scripts/docker-build-apk.sh
```

The Docker scripts use `docker compose` when the Compose plugin is installed,
and fall back to `docker-compose` when only the legacy command is present.

If Docker is not available in the current WSL shell, run:

```bash
./scripts/gradle-build-local.sh
```

## Why the emulator is not inside Docker

The Android emulator depends heavily on host virtualization and GPU acceleration. For WSL-based development, the most robust setup is:

- Docker: reproducible builds.
- Android Studio on Windows: SDK manager and emulator/device manager.
- ADB Wi-Fi: real Pixel Watch 3 deployment.

This avoids coupling project builds to fragile nested virtualization/GPU setups.
