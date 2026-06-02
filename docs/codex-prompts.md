# Codex task prompts

## Stabilize closed-loop baseline

```text
You are working in the wearos_playground repository.

Goal:
Verify and improve the baseline Wear OS closed-loop app without adding large dependencies yet.

Tasks:
1. Inspect the Gradle configuration, Dockerfile, scripts, and app source.
2. Fix any build issues.
3. Keep the app minimal and dependency-light.
4. Preserve the closed-loop scripts:
   - docker-build-apk.sh
   - install-watch.sh
   - launch-watch.sh
   - screenshot-watch.sh
   - logcat-watch.sh
5. Add concise documentation if you change the workflow.

Expected output:
- A buildable debug APK.
- No unnecessary architectural rewrites.
- Clear commit message: chore: stabilize wear os closed loop baseline
```

## Add Compose Wear baseline UI

```text
You are working in the wearos_playground repository.

Goal:
Migrate the minimal Activity UI to Jetpack Compose for Wear OS while keeping the existing closed-loop scripts working.

Constraints:
1. Keep package name and app id stable under `com.arfipod.wearosplayground`.
2. Do not break Docker build.
3. Use current stable AndroidX Compose/Wear Compose dependencies.
4. Keep the same app behavior:
   - title
   - build timestamp
   - counter button
   - logcat tag WearLoop
   - short haptic feedback
5. Add a small README section explaining the Compose dependency choices.

Expected output:
- APK builds in Docker.
- App launches on Pixel Watch 3.
- Commit message: feat: add compose wear baseline ui
```

## Document a workflow change

```text
You are working in the wearos_playground repository.

Goal:
Document a workflow or tooling change so a human engineer and a coding agent can
repeat it without rediscovering project context.

Tasks:
1. Read README.md, AGENTS.md, docs/engineering-handbook.md, and the affected scripts.
2. Update the smallest useful set of documentation files.
3. Keep docs in English.
4. Include exact commands, expected outputs or states, and known fallbacks.
5. Mention whether Docker, local Gradle, ADB Wi-Fi, CI, or the real Pixel Watch
   validation path is affected.

Expected output:
- Updated documentation with no stale package names.
- Clear verification commands.
- Commit message: docs: document wear os workflow change
```

## Add a minimal Wear OS Tile

```text
You are working in the wearos_playground repository.

Goal:
Add or improve a minimal Wear OS Tile while keeping the app dependency-light and
the closed-loop scripts working.

Constraints:
1. Keep package name and app id stable under `com.arfipod.wearosplayground`.
2. Use AndroidX Wear Tiles and ProtoLayout, not Compose UI, inside TileService.
3. Keep tile text short and glanceable.
4. Register the TileService in AndroidManifest.xml with BIND_TILE_PROVIDER.
5. Add JVM tests for any pure tile content helpers.
6. Document how to build, install, and add the tile to the watch carousel.

Expected output:
- APK builds with `:app:testDebugUnitTest :app:assembleDebug`.
- Tile provider is registered in the manifest.
- Commit message: feat: add wear os tile baseline
```

## Add a minimal Wear OS complication data source

```text
You are working in the wearos_playground repository.

Goal:
Add or improve a minimal Wear OS complication data source while keeping the app
dependency-light and the closed-loop scripts working.

Constraints:
1. Keep package name and app id stable under `com.arfipod.wearosplayground`.
2. Implement a data source only; do not add a watch face unless explicitly asked.
3. Use AndroidX Watch Face complication data source APIs.
4. Keep data privacy-safe, short, and glanceable.
5. Register the service in AndroidManifest.xml with BIND_COMPLICATION_PROVIDER.
6. Use a battery-friendly update period of at least 300 seconds unless using
   push updates.
7. Add JVM tests for pure complication content helpers.
8. Document how to build, install, and select the data source from a watch face.

Expected output:
- APK builds with `:app:testDebugUnitTest :app:assembleDebug`.
- Complication provider is registered in the manifest.
- Commit message: feat: add wear os complication baseline
```
