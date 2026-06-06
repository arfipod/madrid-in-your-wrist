#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

INTERVAL_SECONDS="${ADB_KEEP_ALIVE_INTERVAL_SECONDS:-25}"
VERBOSE_EVERY="${ADB_KEEP_ALIVE_VERBOSE_EVERY:-12}"
WAKE_EVERY="${ADB_KEEP_ALIVE_WAKE_EVERY:-0}"

require_non_negative_integer() {
  local name="$1"
  local value="$2"

  if [[ -z "$value" || "$value" == *[!0-9]* ]]; then
    fail "$name must be a non-negative integer."
  fi
}

try_reconnect() {
  if [[ -z "${ANDROID_SERIAL:-}" || "$ANDROID_SERIAL" != *:* ]]; then
    return 1
  fi

  echo "Trying to reconnect $ANDROID_SERIAL..." >&2
  "$ADB" connect "$ANDROID_SERIAL" >/dev/null 2>&1 || true
  adb_cmd shell true >/dev/null 2>&1
}

require_non_negative_integer "ADB_KEEP_ALIVE_INTERVAL_SECONDS" "$INTERVAL_SECONDS"
require_non_negative_integer "ADB_KEEP_ALIVE_VERBOSE_EVERY" "$VERBOSE_EVERY"
require_non_negative_integer "ADB_KEEP_ALIVE_WAKE_EVERY" "$WAKE_EVERY"

if (( INTERVAL_SECONDS < 5 )); then
  fail "ADB_KEEP_ALIVE_INTERVAL_SECONDS must be at least 5 seconds."
fi

require_adb_device

target="${ANDROID_SERIAL:-selected ADB target}"
echo "Keeping ADB alive for $target every ${INTERVAL_SECONDS}s."
echo "Press Ctrl-C to stop. The display is not woken unless ADB_KEEP_ALIVE_WAKE_EVERY is set."

trap 'echo; echo "Stopped ADB keep-alive."; exit 0' INT TERM

ping_count=0
while true; do
  if adb_cmd shell true >/dev/null 2>&1 || try_reconnect; then
    ping_count=$((ping_count + 1))
    if (( VERBOSE_EVERY > 0 && (ping_count == 1 || ping_count % VERBOSE_EVERY == 0) )); then
      printf '[%s] ADB alive (%s)\n' "$(date '+%H:%M:%S')" "$target"
    fi
    if (( WAKE_EVERY > 0 && ping_count % WAKE_EVERY == 0 )); then
      adb_cmd shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    fi
  else
    printf '[%s] ADB keep-alive ping failed.\n' "$(date '+%H:%M:%S')" >&2
    echo "Connected targets:" >&2
    "$ADB" devices -l >&2 || true
    exit 1
  fi

  sleep "$INTERVAL_SECONDS"
done
