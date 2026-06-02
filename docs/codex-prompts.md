# Codex prompts

## Prompt 1 — Stabilize baseline

Reasoning effort: medium

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

## Prompt 2 — Add Compose for Wear OS

Reasoning effort: high

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

## Prompt 3 — Add screenshot verification

Reasoning effort: medium-high

```text
You are working in the wearos_playground repository.

Goal:
Add a lightweight screenshot verification workflow for the Wear OS app.

Tasks:
1. Extend screenshot-watch.sh to save metadata next to the PNG.
2. Add a script that captures before/after screenshots with timestamped names.
3. Add a docs section explaining how screenshots are used in the development loop.
4. Do not add heavyweight visual regression tooling yet.

Expected output:
- screenshots saved under artifacts/screenshots
- metadata JSON per screenshot
- Commit message: feat: add screenshot capture metadata
```
