# Screenshot Comparison

This repository supports lightweight screenshot comparison for the closed loop.
The comparison script is intentionally outside Gradle so it can be used against
any captured watch PNG.

## Capture a screenshot

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
```

Screenshots are written to:

```text
artifacts/screenshots/
```

## Compare two screenshots

```bash
./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

By default, the script allows zero differing pixels. To allow a small tolerance:

```bash
SCREENSHOT_COMPARE_MAX_DIFF_PIXELS=25 \
  ./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG
```

## ImageMagick support

When ImageMagick is installed, the script reports the absolute pixel difference
and writes a visual diff image under:

```text
artifacts/screenshot-diffs/
```

Install ImageMagick on Ubuntu/WSL with:

```bash
sudo apt install imagemagick
```

If ImageMagick is not installed, the script still passes for exact byte-identical
files using `cmp`. If files differ, it exits with an instruction to install
ImageMagick for proper pixel metrics.

## Suggested Workflow

1. Capture a known-good watch screenshot.
2. Keep the baseline somewhere stable outside `artifacts/`, because
   `artifacts/` is ignored by Git.
3. Capture a new screenshot after a UI change.
4. Compare the baseline and actual screenshot.

Example:

```bash
mkdir -p docs/baselines
cp artifacts/screenshots/watch-YYYYMMDD-HHMMSS.png docs/baselines/madrid-wrist.png

ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
./scripts/compare-screenshot.sh \
  docs/baselines/madrid-wrist.png \
  artifacts/screenshots/watch-YYYYMMDD-HHMMSS.png
```
