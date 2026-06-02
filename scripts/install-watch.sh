#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  echo "Run ./scripts/docker-build-apk.sh or ./scripts/gradle-build-local.sh first." >&2
  exit 1
fi

require_adb_device
adb_cmd install -r "$APK_PATH"
