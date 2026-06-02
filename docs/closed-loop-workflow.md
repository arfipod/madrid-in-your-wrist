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

## Manual loop

```bash
./scripts/docker-build-apk.sh
./scripts/install-watch.sh
./scripts/launch-watch.sh
./scripts/screenshot-watch.sh
./scripts/logcat-watch.sh
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
├── bugreports/
└── logs/
```

`artifacts/` is ignored by Git.
