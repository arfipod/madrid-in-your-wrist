#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
require_adb_device
adb_cmd logcat -s WearLoop ActivityTaskManager AndroidRuntime
