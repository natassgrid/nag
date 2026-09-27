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

# ---------------------------------------------------------------------------
# fix_java_home: unset JAVA_HOME when it points to a path that does not exist
# in the current shell (common in WSL when an IDE like IntelliJ exports a
# Windows-style /mnt/<drive>/... path into the environment).
# Gradle will then locate Java via its own toolchain resolution or PATH.
# ---------------------------------------------------------------------------
fix_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]] && [[ ! -d "$JAVA_HOME" ]]; then
    echo "[E2E] WARN: JAVA_HOME='$JAVA_HOME' does not exist in this shell — clearing it."
    echo "[E2E]       Gradle will locate Java via toolchain / PATH instead."
    unset JAVA_HOME
  fi
}

start_stack() {
  echo "[E2E] Starting Docker Compose E2E stack..."
  # Do NOT use --wait: it blocks until every container is healthy, which
  # fails if WireMock/MailHog take longer than their start_period.
  # The monolith poll loop below is the real readiness gate.
  docker compose $COMPOSE_BASE $COMPOSE_MONOLITH $COMPOSE_E2E up -d
  echo "[E2E] Stack started. Waiting for monolith to become healthy..."
  for i in $(seq 1 36); do
    if curl -sf http://localhost:9000/actuator/health > /dev/null 2>&1; then
      echo "[E2E] Monolith healthy after $((i * 5))s"
      break
    fi
    if [ "$i" -eq 36 ]; then
      echo "[E2E] ERROR: Monolith did not become healthy within 180s. Dumping logs:"
      docker compose $COMPOSE_BASE $COMPOSE_MONOLITH $COMPOSE_E2E logs --tail=100 monolith-app
      exit 1
    fi
    echo "[E2E] Attempt $i/36 — waiting 5s..."
    sleep 5
  done
}

seed_data() {
  echo "[E2E] Seeding E2E data..."
  # Pipe the SQL file from the host via stdin — avoids the need to mount it
  # inside the container or use docker cp.
  docker exec -i exam-postgres psql \
    -U exam_admin \
    -d exam_platform \
    < "$ROOT_DIR/infrastructure/docker-compose/e2e-seed-data.sql" \
    && echo "[E2E] E2E seed data applied." \
    || echo "[E2E] WARN: Seed may have partially applied (idempotent ON CONFLICT — safe to ignore)."
}

run_backend_e2e() {
  echo "[E2E] Running backend E2E tests..."
  fix_java_home
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
