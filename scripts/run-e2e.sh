#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
#
# NAG E2E Test Runner — Local Developer Quickstart
# Usage: ./scripts/run-e2e.sh [--backend-only | --frontend-only | --down]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_BASE="-f $ROOT_DIR/infrastructure/docker-compose/docker-compose.yml"
COMPOSE_MONOLITH="-f $ROOT_DIR/infrastructure/docker-compose/docker-compose.monolith.yml"
COMPOSE_E2E="-f $ROOT_DIR/infrastructure/docker-compose/docker-compose.e2e.yml"

command=${1:---all}

start_stack() {
  echo "[E2E] Starting Docker Compose E2E stack..."
  docker compose $COMPOSE_BASE $COMPOSE_MONOLITH $COMPOSE_E2E up -d --wait
  echo "[E2E] Stack is up. Waiting for monolith health..."
  for i in $(seq 1 30); do
    if curl -sf http://localhost:9000/actuator/health > /dev/null 2>&1; then
      echo "[E2E] Monolith healthy after $((i * 5))s"
      break
    fi
    sleep 5
  done
}

seed_data() {
  echo "[E2E] Seeding E2E data..."
  docker exec exam-postgres psql -U exam_admin -d exam_platform \
    -f /docker-entrypoint-initdb.d/e2e-seed-data.sql || true
}

run_backend_e2e() {
  echo "[E2E] Running backend E2E tests..."
  cd "$ROOT_DIR"
  ./gradlew :e2e-tests:test -PrunE2E --info
  echo "[E2E] Backend report: e2e-tests/build/reports/tests/test/index.html"
}

run_frontend_e2e() {
  echo "[E2E] Running Playwright frontend E2E tests..."
  cd "$ROOT_DIR/nag-frontend-workspace"
  npx playwright test --config=e2e/playwright.config.ts
  echo "[E2E] Playwright report: nag-frontend-workspace/e2e-report/index.html"
}

tear_down() {
  echo "[E2E] Tearing down Docker Compose E2E stack..."
  docker compose $COMPOSE_BASE $COMPOSE_MONOLITH $COMPOSE_E2E down -v
  echo "[E2E] Stack torn down."
}

case "$command" in
  --backend-only)
    start_stack
    seed_data
    run_backend_e2e
    ;;
  --frontend-only)
    run_frontend_e2e
    ;;
  --down)
    tear_down
    ;;
  --all | *)
    start_stack
    seed_data
    run_backend_e2e
    run_frontend_e2e
    ;;
esac
