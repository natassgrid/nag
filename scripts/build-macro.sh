#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Build all NAG macro-service Docker images

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
TAG="${IMAGE_TAG:-latest}"

echo "================================================================="
echo "Building NAG Macro-Services"
echo "Registry: ${REGISTRY}"
echo "Tag:      ${TAG}"
echo "================================================================="

cd "${ROOT_DIR}"

APPS=(
  "auth-admin-app"
  "content-app"
  "execution-app"
  "post-exam-app"
)

for APP in "${APPS[@]}"; do
  echo "--> Building ${APP}..."
  docker build \
    --build-arg SERVICE_NAME="${APP}" \
    -f backend/Dockerfile \
    -t "${REGISTRY}/exam/${APP}:${TAG}" \
    .
  echo "    Tagged: ${REGISTRY}/exam/${APP}:${TAG}"
done

echo "--> Building standalone audit-service (isolated for compliance)..."
docker build \
  --build-arg SERVICE_NAME="audit-service" \
  -f backend/Dockerfile \
  -t "${REGISTRY}/exam/audit-service:${TAG}" \
  .

echo "--> Building api-gateway..."
docker build \
  --build-arg SERVICE_NAME="api-gateway" \
  -f backend/Dockerfile \
  -t "${REGISTRY}/exam/api-gateway:${TAG}" \
  .

echo "================================================================="
echo "All macro images built successfully."
echo "To run with Docker Compose:"
echo "  docker compose -f infrastructure/docker-compose/docker-compose.yml -f infrastructure/docker-compose/docker-compose.macro.yml up"
echo "================================================================="
