#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  echo "Run ./scripts/docker-build-apk.sh first." >&2
  exit 1
fi

adb install -r "$APK_PATH"
