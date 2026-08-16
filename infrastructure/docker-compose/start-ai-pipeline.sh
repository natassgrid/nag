#!/bin/bash

# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# =============================================================================
# Minimal local stack for testing the AI Question Generation Pipeline
#
# Starts the services needed for the pipeline + basic app access:
#   Infrastructure:
#   - PostgreSQL (question storage + embeddings)
#   - Redis (caching, rate limiting)
#   - Vault (encryption keys — dev mode)
#   - Kafka (audit events)
#   - Keycloak (IAM / auth)
#   AI:
#   - Ollama (runs Qwen3 8B + nomic-embed-text)
#   - LiteLLM (unified OpenAI-compatible gateway)
#   Application:
#   - Identity Service (auth, tokens)
#   - Question Bank Service (question CRUD + AI pipeline)
#   - API Gateway (routes /api → services)
#   - Frontend (Angular SPA on port 4200)
#
# Usage:
#   ./start-ai-pipeline.sh          # Start full pipeline stack
#   ./start-ai-pipeline.sh --clean  # Clean volumes and restart fresh
#   ./start-ai-pipeline.sh --stop   # Stop all pipeline services
#   ./start-ai-pipeline.sh --status # Show status of pipeline services
#   ./start-ai-pipeline.sh --logs   # Tail logs from Ollama and LiteLLM
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE="docker compose -f docker-compose.yml"
COMPOSE_SERVICES="docker compose -f docker-compose.yml -f docker-compose.services.yml"
INFRA_SERVICES="postgres redis vault vault-init kafka keycloak ollama ollama-pull litellm indictrans2"
APP_SERVICES="identity-service question-bank-service api-gateway frontend"

ACTION="start"

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean) ACTION="clean"; shift ;;
        --stop) ACTION="stop"; shift ;;
        --status) ACTION="status"; shift ;;
        --logs) ACTION="logs"; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo "============================================="
echo "  AI Question Pipeline — Minimal Stack"
echo "============================================="
echo ""

case $ACTION in

  stop)
    echo "▶ Stopping pipeline services..."
    $COMPOSE stop $INFRA_SERVICES
    $COMPOSE_SERVICES stop $APP_SERVICES 2>/dev/null || true
    echo ""
    echo "✓ Pipeline services stopped."
    ;;

  status)
    echo "▶ Pipeline service status:"
    echo ""
    echo "--- Infrastructure ---"
    $COMPOSE ps postgres redis vault kafka keycloak ollama litellm indictrans2 2>/dev/null || $COMPOSE ps
    echo ""
    echo "--- Application ---"
    $COMPOSE_SERVICES ps identity-service question-bank-service api-gateway frontend 2>/dev/null || true
    echo ""
    echo "▶ Ollama models:"
    docker exec exam-ollama ollama list 2>/dev/null || echo "  (Ollama not running)"
    echo ""
    echo "▶ LiteLLM health:"
    curl -sf http://localhost:4000/health 2>/dev/null && echo " OK" || echo "  NOT REACHABLE"
    echo ""
    echo "▶ API Gateway health:"
    curl -sf http://localhost:9000/actuator/health 2>/dev/null | head -c 100 || echo "  NOT REACHABLE"
    echo ""
    ;;

  logs)
    echo "▶ Tailing Ollama + LiteLLM + Question Bank logs (Ctrl+C to stop)..."
    echo ""
    $COMPOSE_SERVICES logs -f ollama litellm question-bank-service 2>/dev/null || $COMPOSE logs -f ollama litellm
    ;;

  clean)
    echo "▶ Stopping and removing pipeline volumes..."
    $COMPOSE_SERVICES down -v --remove-orphans 2>/dev/null || true
    $COMPOSE down -v --remove-orphans 2>/dev/null || true
    echo ""
    echo "▶ Starting fresh..."
    $COMPOSE up -d $INFRA_SERVICES
    echo ""
    echo "⏳ Waiting for infrastructure to be healthy..."
    $COMPOSE up --wait -d postgres vault ollama
    echo ""
    echo "▶ Starting application services..."
    $COMPOSE_SERVICES up -d $APP_SERVICES
    echo ""
    echo "✓ Clean start complete."
    echo ""
    echo "  Ollama is pulling models in the background (ollama-pull container)."
    echo "  Monitor with: docker logs -f exam-ollama-pull"
    echo ""
    echo "  Frontend: http://localhost:4200"
    echo "  API Gateway: http://localhost:9000"
    ;;

  start)
    echo "▶ Starting pipeline infrastructure..."
    echo "  Infra: postgres, redis, vault, kafka, keycloak, ollama, litellm, indictrans2"
    echo "  Apps:  identity-service, question-bank-service, api-gateway, frontend"
    echo ""
    $COMPOSE up -d $INFRA_SERVICES
    echo ""
    echo "⏳ Waiting for infrastructure to be healthy..."
    $COMPOSE up --wait -d postgres vault ollama 2>/dev/null || true
    echo ""
    echo "▶ Starting application services..."
    $COMPOSE_SERVICES up -d $APP_SERVICES
    echo ""
    echo "============================================="
    echo "  ✓ AI Pipeline stack is running!"
    echo "============================================="
    echo ""
    echo "  Infrastructure:"
    echo "    PostgreSQL   → localhost:5432"
    echo "    Redis        → localhost:6379"
    echo "    Vault        → localhost:8200 (token: vault_root_token)"
    echo "    Kafka        → localhost:29092"
    echo "    Keycloak     → localhost:8080 (admin/admin_secret)"
    echo "    Ollama       → localhost:11434"
    echo "    LiteLLM      → localhost:4000 (key: sk-litellm-dev-key)"
    echo "    IndicTrans2  → localhost:7860 (English → 22 Indian languages)"
    echo ""
    echo "  Application:"
    echo "    Identity     → localhost:8081"
    echo "    Question Bank→ localhost:8083"
    echo "    API Gateway  → localhost:9000"
    echo "    Frontend     → localhost:4200"
    echo ""
    echo "  Models are being pulled (first run only):"
    echo "    docker logs -f exam-ollama-pull"
    echo ""
    echo "  Test embedding:"
    echo "    curl http://localhost:4000/v1/embeddings \\"
    echo "      -H 'Authorization: Bearer sk-litellm-dev-key' \\"
    echo "      -H 'Content-Type: application/json' \\"
    echo "      -d '{\"model\": \"nomic-embed-text\", \"input\": \"What is gravity?\"}'"
    echo ""
    echo "  Test generation:"
    echo "    curl http://localhost:4000/v1/chat/completions \\"
    echo "      -H 'Authorization: Bearer sk-litellm-dev-key' \\"
    echo "      -H 'Content-Type: application/json' \\"
    echo "      -d '{\"model\": \"qwen3-8b\", \"messages\": [{\"role\": \"user\", \"content\": \"Generate 1 MCQ on gravity\"}]}'"
    ;;

esac
