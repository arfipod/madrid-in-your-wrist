#!/usr/bin/env bash
set -euo pipefail
adb logcat -s WearLoop ActivityTaskManager AndroidRuntime
