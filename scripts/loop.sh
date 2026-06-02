#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

case "${BUILD_MODE:-docker}" in
  docker)
    ./scripts/docker-build-apk.sh
    ;;
  local)
    ./scripts/gradle-build-local.sh
    ;;
  *)
    echo "error: unsupported BUILD_MODE='${BUILD_MODE}'" >&2
    echo "Use BUILD_MODE=docker or BUILD_MODE=local." >&2
    exit 1
    ;;
esac

./scripts/install-watch.sh
./scripts/launch-watch.sh
sleep 2
./scripts/screenshot-watch.sh
