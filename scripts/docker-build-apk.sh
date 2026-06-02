#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source ./scripts/common.sh
compose="$(docker_compose_cmd)"
$compose run --rm dev ./gradlew --no-daemon :app:assembleDebug
