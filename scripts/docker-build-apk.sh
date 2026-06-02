#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose run --rm dev gradle --no-daemon :app:assembleDebug
