# Codex Task Prompts

Reusable prompts for future agent-driven work. Keep package identifiers stable
and ask for watch/ADB validation whenever runtime behavior changes.

## Review Or Refactor

```text
You are working in the madrid-in-your-wrist repository.

Goal:
Review the current code and refactor only where it clearly improves clarity,
testability, or maintainability.

Constraints:
1. Preserve package/app identifiers under `com.arfipod.madridinyourwrist`.
2. Keep the `WearLoop` log tag.
3. Do not introduce large dependencies or broad architectural rewrites.
4. Add JVM tests for new non-UI behavior.
5. Verify with:
   source ./scripts/common.sh
   ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
6. If runtime behavior changes, install/launch/screenshot/logcat on the selected
   ADB target with `ANDROID_SERIAL`.

Expected output:
- Findings first if reviewing.
- Clear summary of any refactor.
- Commands that passed.
- Watch screenshot path when runtime validation was performed.
```

## Improve Madrid Wrist

```text
You are working in the madrid-in-your-wrist repository.

Goal:
Improve the Madrid Wrist transport dashboard while keeping it usable on a small
Wear OS screen.

Current behavior to preserve:
1. Generic profiles: `Perfil 1`, `Perfil 2`, `Perfil 3`.
2. Metro/EMT favorites saved on the watch.
3. Cached top glance card.
4. Online refresh when validated internet is available.
5. Offline/cache fallback when network or APIs fail.
6. Tile and complication read cached snapshots only.

Expected output:
- Focused code changes.
- Updated docs for any behavior change.
- JVM tests for policy, codec, formatting, or persistence changes.
- Build/test verification plus watch runtime validation when available.
```

## Document A Workflow Change

```text
You are working in the madrid-in-your-wrist repository.

Goal:
Document a workflow or tooling change so a human engineer and a coding agent can
repeat it without rediscovering project context.

Tasks:
1. Read README.md, AGENTS.md, docs/engineering-handbook.md, and affected scripts.
2. Update the smallest useful set of documentation files.
3. Keep docs clear, compact, and command-oriented.
4. Include exact commands, expected states, and known fallbacks.
5. Mention whether Docker, local Gradle, ADB Wi-Fi, CI, or real-watch validation
   is affected.

Expected output:
- Updated documentation with no stale package names.
- Clear verification commands.
- Summary of changed docs.
```

## Add Or Update A Tile

```text
You are working in the madrid-in-your-wrist repository.

Goal:
Add or improve the Madrid Wrist Wear OS Tile.

Constraints:
1. Use AndroidX Wear Tiles and ProtoLayout.
2. Do not use Compose UI inside TileService.
3. Do not run network work from the Tile.
4. Read cached selected-profile snapshots.
5. Keep text short and glanceable.
6. Add JVM tests for pure tile content helpers.
7. Document build, install, and tile-picker validation.

Expected output:
- APK builds with `:app:testDebugUnitTest :app:assembleDebug`.
- Tile provider remains registered in AndroidManifest.xml.
- Watch/phone picker instructions are updated if metadata changes.
```

## Add Or Update A Complication

```text
You are working in the madrid-in-your-wrist repository.

Goal:
Add or improve the Madrid Wrist Wear OS complication data source.

Constraints:
1. Implement a data source only; do not add a watch face unless explicitly asked.
2. Use AndroidX Watch Face complication data source APIs.
3. Do not run network work from the complication.
4. Read cached selected-profile snapshots.
5. Keep data privacy-safe, short, and glanceable.
6. Use a regular update period of at least 900 seconds unless using push updates.
7. Add JVM tests for pure complication content helpers.

Expected output:
- APK builds with `:app:testDebugUnitTest :app:assembleDebug`.
- Provider remains registered in AndroidManifest.xml.
- Watch-face picker instructions are updated if metadata changes.
```
