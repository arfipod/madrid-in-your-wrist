#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

check() {
  local name="$1"
  local cmd="$2"
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "[OK] $name: $(command -v "$cmd")"
  else
    echo "[MISSING] $name: $cmd"
  fi
}

echo "== Host tools =="
check "Docker" docker
check "ADB" adb
check "Gradle local" gradle

echo
echo "== Docker Compose =="
if docker compose version >/dev/null 2>&1; then
  docker compose version
else
  echo "[MISSING] docker compose plugin"
fi

echo
echo "== Android target =="
echo "App ID: $APP_ID"
echo "APK: $APK_PATH"

if [[ -f "$APK_PATH" ]]; then
  echo "[OK] APK exists"
else
  echo "[INFO] APK not built yet"
fi

echo
echo "== ADB devices =="
if command -v adb >/dev/null 2>&1; then
  adb devices -l || true
else
  echo "ADB not installed in this shell."
fi
