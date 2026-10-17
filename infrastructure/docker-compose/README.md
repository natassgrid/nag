# NextGen Assessment Grid (NAG) — Multi-Provider AI Gateway & Infrastructure Guide

This directory contains the Docker Compose configurations, orchestration scripts, and LiteLLM gateway definitions for the NextGen Assessment Grid (NAG) AI Question Generation Pipeline and supporting infrastructure services.

---

## 1. LiteLLM Multi-Provider Gateway Overview

The platform routes all AI question generation and refinement requests through a unified **LiteLLM Gateway** (running on port `4000`). LiteLLM exposes a standardized OpenAI-compatible REST API while delegating downstream requests to:

1. **Google Gemini (AI Studio & Google Cloud Vertex AI)**: High-performance multimodal reasoning models (Gemini 2.5 Flash-Lite, Flash, and Pro).
2. **AWS Bedrock**: Enterprise cloud models (Amazon Nova Micro and Nova Lite).
3. **Local Ollama**: Zero-cost, air-gapped local models (`llama3.2:1b`, `qwen2.5:1.5b`, `qwen2-math:1.5b`, and embeddings via `all-minilm`) for offline local development and CI/CD pipelines.

```
┌────────────────────────────────────────────────────────┐
│  Question Bank Service / Content Macro-Service         │
│  (Spring Boot @Component ModelRouter)                  │
└──────────────────────────┬─────────────────────────────┘
                           │ OpenAI-compatible HTTP POST
                           ▼
┌────────────────────────────────────────────────────────┐
│  LiteLLM Gateway (http://litellm:4000)                 │
│  litellm-config.yaml                                   │
└──────┬───────────────────┼────────────────────┬────────┘
       │                   │                    │
       ▼                   ▼                    ▼
┌──────────────┐   ┌───────────────┐   ┌─────────────────┐
│ Google Gemini│   │  AWS Bedrock  │   │  Local Ollama   │
│ AI Studio /  │   │  Nova Micro / │   │  qwen2.5:1.5b   │
│ Vertex AI    │   │  Nova Lite    │   │  llama3.2:1b    │
└──────────────┘   └───────────────┘   └─────────────────┘
```

### High Availability & Automatic Fallbacks

The gateway is configured with automatic multi-tier fallback routes in `litellm-config.yaml`:
- If Google Gemini experiences a rate limit (HTTP 429) or transient outage (HTTP 5xx), requests automatically fail over to AWS Bedrock (`nova-lite` or `nova-micro`), and subsequently to local Ollama (`qwen2.5:1.5b` or `llama3.2:1b`).
- If AWS Bedrock encounters an outage, requests fall back to Google Gemini and local Ollama.

---

## 2. Active Provider & Model Tier Configuration

The platform selects models based on difficulty tiers (`EASY`, `MEDIUM`, `HARD`) and the configured `AI_PROVIDER`.

### Supported Providers (`AI_PROVIDER`)

| `AI_PROVIDER` | Description | Typical Use Case |
|---|---|---|
| `gemini` | Google Gemini 2.5 models via AI Studio or Vertex AI | Production / High Reasoning / Multilingual |
| `bedrock` | AWS Bedrock Amazon Nova models | Production / AWS GovCloud / AWS Enterprise |
| `local` (or `ollama`) | Ollama local models | Local development / Offline / Zero-cost CI |

### Difficulty Tier Mappings

| Difficulty Tier | Bedrock (`AI_PROVIDER=bedrock`) | Gemini (`AI_PROVIDER=gemini`) | Local (`AI_PROVIDER=local`) |
|---|---|---|---|
| **EASY** | `nova-micro` (`amazon.nova-micro-v1:0`) | `gemini-flash-lite` (`gemini-2.5-flash-lite`) | `llama3.2:1b` |
| **MEDIUM** | `nova-lite` (`amazon.nova-lite-v1:0`) | `gemini-flash` (`gemini-2.5-flash`) | `qwen2.5:1.5b` |
| **HARD** | `nova-lite` (`amazon.nova-lite-v1:0`) | `gemini-pro` (`gemini-2.5-pro`) | `qwen2.5:1.5b` / `qwen2-math:1.5b` |

### Environment Variables

Configure these settings in `infrastructure/docker-compose/.env`:

```bash
# Active AI Provider: bedrock | gemini | local
AI_PROVIDER=gemini

# Default Bedrock Model Tiers
AI_MODEL_EASY=nova-micro
AI_MODEL_MEDIUM=nova-lite
AI_MODEL_HARD=nova-lite

# Gemini Model Tiers (used when AI_PROVIDER=gemini)
AI_GEMINI_MODEL_EASY=gemini-flash-lite
AI_GEMINI_MODEL_MEDIUM=gemini-flash
AI_GEMINI_MODEL_HARD=gemini-pro
```

---

## 3. Configuring Google Gemini Credentials

### Option A: Google AI Studio (`GEMINI_API_KEY`) — Recommended for Quick Setup

1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. Open `infrastructure/docker-compose/.env` and paste your key:
   ```bash
   AI_PROVIDER=gemini
   GEMINI_API_KEY=AIzaSy...your-gemini-api-key-here
   ```
3. LiteLLM automatically injects `GEMINI_API_KEY` into requests sent to `gemini/gemini-2.5-*` models.

### Option B: Google Cloud Vertex AI — Enterprise IAM Path

For enterprise deployments requiring OAuth2 service accounts or VPC Service Controls:

1. Enable the **Vertex AI API** in the Google Cloud Console.
2. Create a service account with the **Vertex AI User** (`roles/aiplatform.user`) role and download the JSON key.
3. In `infrastructure/docker-compose/.env`, configure:
   ```bash
   AI_PROVIDER=gemini
   VERTEX_PROJECT=your-gcp-project-id
   VERTEX_LOCATION=us-central1
   GOOGLE_APPLICATION_CREDENTIALS_FILE=/path/to/application_default_credentials.json
   ```
4. LiteLLM routes `vertex-gemini-*` models through Vertex AI using these credentials.

---

## 4. Configuring AWS Bedrock Credentials

When running with `AI_PROVIDER=bedrock` or using Bedrock as a fallback:

```bash
AI_PROVIDER=bedrock
AWS_REGION_NAME=us-east-1
AWS_ACCESS_KEY_ID=AKIA...your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AI_BEDROCK_ROLE_ARN=arn:aws:iam::123456789012:role/service-role/nag-bedrock-execution-role
AI_BEDROCK_S3_BUCKET=nag-bedrock
```

---

## 5. AI Guardrails, Resilience & OWASP Top 10 for LLM

The platform implements comprehensive resilience, rate-limiting, and validation controls for LLM calls:

```bash
# -----------------------------------------------------------------------------
# AI Agent Guardrails & Resilience Configuration (OWASP Top 10 for LLM)
# -----------------------------------------------------------------------------
AI_GUARDRAILS_ENABLED=true
AI_GUARDRAIL_STRICT_MODE=true

# Circuit Breaker & Upstream Resilience
AI_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50       # Percentage of failures before opening circuit
AI_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS=30       # Seconds to wait in OPEN state before HALF_OPEN probe
AI_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=10         # Number of calls evaluated for failure rate
AI_CALL_TIMEOUT_SECONDS=45                        # Maximum latency per LLM inference call
AI_AGENT_MAX_REFINEMENT_CYCLES=2                  # Maximum iterative self-correction cycles

# Token & Rate Limits per request
AI_MAX_PROMPT_INPUT_LENGTH=4000                   # Max character length of prompt context
AI_MAX_OUTPUT_TOKENS=2048                         # Max completion tokens generated
```

### Key Guardrail Mechanisms:
- **Prompt Injection Defense**: Sanitizes inputs and validates generated question structures against JSON schemas.
- **Circuit Breaker Protection**: Isolates failing upstream AI providers to preserve platform responsiveness.
- **Deterministic Math & Format Validation**: Ensures all mathematical expressions adhere to `$$ ... $$` LaTeX formatting rules before questions are persisted.

---

## 6. Starting and Verifying the AI Stack

### Start AI Services

To spin up the minimal AI pipeline stack (LiteLLM, Ollama, Redis, PostgreSQL, Kafka, Keycloak):

```bash
cd infrastructure/docker-compose

# Option 1: Using the helper script
./start-ai-pipeline.sh

# Option 2: Using Docker Compose directly
docker compose --profile ai up -d litellm ollama
```

### Health Check

Verify LiteLLM gateway status:

```bash
curl -i http://localhost:4000/health
```

Expected response: `HTTP/1.1 200 OK`

### Test Model Completions via LiteLLM

#### Test Google Gemini (AI Studio):
```bash
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -d '{
    "model": "gemini-flash",
    "messages": [
      {"role": "user", "content": "Generate a multiple-choice question on Newton'\''s Second Law of Motion in JSON."}
    ],
    "temperature": 0.2
  }'
```

#### Test AWS Bedrock Nova Micro:
```bash
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -d '{
    "model": "nova-micro",
    "messages": [
      {"role": "user", "content": "Ping"}
    ]
  }'
```

#### Test Local Ollama:
```bash
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -d '{
    "model": "qwen2.5:1.5b",
    "messages": [
      {"role": "user", "content": "Ping"}
    ]
  }'
```

---

## 7. Troubleshooting

| Symptom | Cause | Solution |
|---|---|---|
| `AuthenticationError: API key not valid` for Gemini | `GEMINI_API_KEY` is missing, malformed, or inactive | Verify the key in Google AI Studio and ensure it is set in `.env` without surrounding quotes. |
| `404 Not Found` for model route | Route not defined in `litellm-config.yaml` | Verify model name against `litellm-config.yaml` (`gemini-flash`, `gemini-pro`, `nova-micro`, etc.). |
| Upstream timeout or circuit breaker open | Provider latency exceeded `AI_CALL_TIMEOUT_SECONDS` | Check network connectivity to Google / AWS APIs or increase `AI_CALL_TIMEOUT_SECONDS`. |
| Connection refused on `http://localhost:4000` | LiteLLM container failed to start | Check container logs with `docker compose logs litellm`. Ensure port 4000 is not blocked. |
