#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
require_adb_device
adb_cmd shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb_cmd shell am start -n "$APP_ID/$MAIN_ACTIVITY"
