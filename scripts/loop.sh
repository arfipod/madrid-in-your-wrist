#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/docker-build-apk.sh
./scripts/install-watch.sh
./scripts/launch-watch.sh
sleep 2
./scripts/screenshot-watch.sh
