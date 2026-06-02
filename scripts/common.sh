#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="com.arfipod.wearosplayground.debug"
MAIN_ACTIVITY="com.arfipod.wearosplayground.MainActivity"
APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
ARTIFACTS_DIR="$PROJECT_ROOT/artifacts"

DEFAULT_ANDROID_SDK="$HOME/Android/Sdk"
if [[ -z "${ANDROID_HOME:-}" && -d "$DEFAULT_ANDROID_SDK" ]]; then
  export ANDROID_HOME="$DEFAULT_ANDROID_SDK"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi
if [[ -n "${ANDROID_HOME:-}" ]]; then
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
fi

DEFAULT_JDK="$HOME/opt/jdk-17"
if [[ -z "${JAVA_HOME:-}" && -x "$DEFAULT_JDK/bin/javac" ]]; then
  export JAVA_HOME="$DEFAULT_JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

ADB="${ADB:-adb}"
if ! command -v "$ADB" >/dev/null 2>&1 && [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then
  ADB="$ANDROID_HOME/platform-tools/adb"
fi

fail() {
  echo "error: $*" >&2
  exit 1
}

require_cmd() {
  local name="$1"
  local hint="$2"

  if ! command -v "$name" >/dev/null 2>&1; then
    fail "$name not found. $hint"
  fi
}

adb_cmd() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    "$ADB" -s "$ANDROID_SERIAL" "$@"
  else
    "$ADB" "$@"
  fi
}

require_adb() {
  if ! command -v "$ADB" >/dev/null 2>&1; then
    fail "adb not found. Install it with 'sudo apt install adb' or set ANDROID_HOME to an Android SDK."
  fi
}

require_adb_device() {
  require_adb

  local state
  if ! state="$(adb_cmd get-state 2>/dev/null)"; then
    echo "No ready ADB target found." >&2
    echo "Connected targets:" >&2
    "$ADB" devices -l >&2 || true
    echo >&2
    echo "Pair/connect the watch, or select it explicitly:" >&2
    echo "  adb connect WATCH_IP:ADB_PORT" >&2
    echo "  ANDROID_SERIAL=WATCH_IP:ADB_PORT $0" >&2
    exit 1
  fi

  if [[ "$state" != "device" ]]; then
    echo "ADB target is not ready: $state" >&2
    echo "Connected targets:" >&2
    "$ADB" devices -l >&2 || true
    exit 1
  fi
}

docker_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
  elif command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
  else
    fail "Docker Compose not found. Install Docker Desktop with the Compose plugin, then retry."
  fi
}

mkdir -p "$ARTIFACTS_DIR"
