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
1. Keep package name and app id stable.
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
