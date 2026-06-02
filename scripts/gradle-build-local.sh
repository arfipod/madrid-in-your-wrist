#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source ./scripts/common.sh

if [[ -x ./gradlew ]]; then
  ./gradlew --no-daemon :app:assembleDebug
else
  gradle --no-daemon :app:assembleDebug
fi
