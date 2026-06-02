#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
require_adb_device
adb_cmd shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
sleep "${SCREENSHOT_WAKE_DELAY_SECONDS:-1}"
mkdir -p "$ARTIFACTS_DIR/screenshots"
out="$ARTIFACTS_DIR/screenshots/watch-$(date +%Y%m%d-%H%M%S).png"
adb_cmd exec-out screencap -p > "$out"
echo "Saved screenshot: $out"
