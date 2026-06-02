#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="com.angelrubiodev.wearosplayground.debug"
MAIN_ACTIVITY="com.angelrubiodev.wearosplayground.MainActivity"
APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
ARTIFACTS_DIR="$PROJECT_ROOT/artifacts"

mkdir -p "$ARTIFACTS_DIR"
