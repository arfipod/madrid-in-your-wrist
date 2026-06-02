#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
mkdir -p "$ARTIFACTS_DIR/screenshots"
out="$ARTIFACTS_DIR/screenshots/watch-$(date +%Y%m%d-%H%M%S).png"
adb exec-out screencap -p > "$out"
echo "Saved screenshot: $out"
