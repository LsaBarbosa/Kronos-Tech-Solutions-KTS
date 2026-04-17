#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.testcontainers.yml"

if [[ $# -eq 0 ]]; then
  set -- test
fi

GRADLE_TASKS_AND_ARGS="$*"

docker compose -f "$COMPOSE_FILE" run --rm gradle-tests \
  bash -lc "chmod +x ./gradlew && ./gradlew ${GRADLE_TASKS_AND_ARGS} --no-daemon"
