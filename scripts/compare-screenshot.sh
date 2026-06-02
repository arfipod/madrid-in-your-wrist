#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

usage() {
  cat >&2 <<'USAGE'
Usage:
  ./scripts/compare-screenshot.sh BASELINE_PNG ACTUAL_PNG

Environment:
  SCREENSHOT_COMPARE_MAX_DIFF_PIXELS  Allowed absolute pixel difference. Default: 0.

Output:
  Writes diff images to artifacts/screenshot-diffs/ when ImageMagick is available.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

baseline="${1:-}"
actual="${2:-}"

if [[ -z "$baseline" || -z "$actual" ]]; then
  usage
  exit 2
fi

[[ -f "$baseline" ]] || fail "baseline screenshot not found: $baseline"
[[ -f "$actual" ]] || fail "actual screenshot not found: $actual"

threshold="${SCREENSHOT_COMPARE_MAX_DIFF_PIXELS:-0}"
if [[ ! "$threshold" =~ ^[0-9]+$ ]]; then
  fail "SCREENSHOT_COMPARE_MAX_DIFF_PIXELS must be a non-negative integer."
fi

mkdir -p "$ARTIFACTS_DIR/screenshot-diffs"
diff_path="$ARTIFACTS_DIR/screenshot-diffs/diff-$(date +%Y%m%d-%H%M%S).png"

if command -v magick >/dev/null 2>&1; then
  identify_cmd=(magick identify)
  compare_cmd=(magick compare)
elif command -v identify >/dev/null 2>&1 && command -v compare >/dev/null 2>&1; then
  identify_cmd=(identify)
  compare_cmd=(compare)
else
  if cmp -s "$baseline" "$actual"; then
    echo "Screenshots match exactly: $baseline == $actual"
    exit 0
  fi

  fail "screenshots differ and ImageMagick is not installed. Install it with 'sudo apt install imagemagick' to get pixel metrics and diff images."
fi

baseline_size="$("${identify_cmd[@]}" -format '%wx%h' "$baseline")"
actual_size="$("${identify_cmd[@]}" -format '%wx%h' "$actual")"

if [[ "$baseline_size" != "$actual_size" ]]; then
  fail "screenshot dimensions differ: baseline=$baseline_size actual=$actual_size"
fi

metric_raw="$("${compare_cmd[@]}" -metric AE "$baseline" "$actual" "$diff_path" 2>&1 || true)"
if [[ "$metric_raw" =~ ([0-9]+) ]]; then
  diff_pixels="${BASH_REMATCH[1]}"
else
  fail "could not parse ImageMagick comparison metric: $metric_raw"
fi

echo "Baseline: $baseline"
echo "Actual:   $actual"
echo "Size:     $actual_size"
echo "Diff px:  $diff_pixels"
echo "Diff PNG: $diff_path"

if (( diff_pixels > threshold )); then
  echo "Result: screenshots differ above threshold ($threshold)." >&2
  exit 1
fi

echo "Result: screenshots match within threshold ($threshold)."
