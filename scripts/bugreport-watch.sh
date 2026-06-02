#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
require_adb_device
mkdir -p "$ARTIFACTS_DIR/bugreports"
out="$ARTIFACTS_DIR/bugreports/bugreport-$(date +%Y%m%d-%H%M%S).zip"
adb_cmd bugreport "$out"
echo "Saved bugreport: $out"
