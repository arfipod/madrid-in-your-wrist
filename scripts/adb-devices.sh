#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
require_adb
"$ADB" devices -l
