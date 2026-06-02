#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"
