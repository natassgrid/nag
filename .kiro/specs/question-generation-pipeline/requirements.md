# Question Generation Pipeline — Requirements

#[[file:docs/requirements/question-pipeline.md]]

## Functional Requirements

### FR-1: Embedding Generation
- The system MUST generate 384-dimensional embeddings for every question using `all-minilm` model via Ollama (through LiteLLM gateway, OpenAI-compatible `/v1/embeddings` endpoint).
- Embeddings MUST be stored in the `embedding` column as `halfvec(384)` (pgvector half-precision vector type).
- Embeddings MUST be generated on question creation and on content update.

### FR-2: Duplicate Detection
- On question creation, the system MUST compute cosine similarity between the new question's embedding and existing questions in the same subject+tenant using pgvector's `<=>` operator on `halfvec(384)`.
- If similarity > 0.92, the question MUST be rejected with an error message identifying the near-duplicate.
- If similarity is between 0.85–0.92, the question MUST be flagged for human review (added to response metadata).
- The IVFFlat index on `halfvec_cosine_ops` MUST be used for sub-200ms lookups.

### FR-3: AI Question Generation
- The system MUST provide a REST endpoint `POST /api/v1/questions/generate` that accepts: subject, topic, subtopic, difficulty, cognitiveLevel, questionType, count, contentFormat, and optional avoidDuplicate flag.
- The system MUST select the appropriate model based on subject:
  - **Math/Science subjects** → `qwen2-math:1.5b` (via LiteLLM model name: `qwen2-math-1.5b`)
  - **History/Geography/Sports/Current Affairs** → `llama3.2:1b` (via LiteLLM model name: `llama3.2-1b`)
  - **All other subjects** (balanced/structured) → `qwen2.5:1.5b` (via LiteLLM model name: `qwen2.5-1.5b`)
- The system MUST use RAG: retrieve top-K relevant existing questions via halfvec similarity, then prompt the selected model to generate new questions based on retrieved context.
- Generated questions MUST be returned as structured JSON matching the `CreateQuestionRequest` schema.
- Generated questions MUST have `state=DRAFT` and `references` set to "AI-generated via {model_name}".
- Generated questions MUST pass duplicate detection before being stored.

### FR-4: Content Format Support
- Question `content`, `answer_key`, `explanation`, and `options[].text` MUST support multiple formats:
  - **TEXT** — plain text
  - **LATEX** — LaTeX math expressions (e.g., `$$x = \frac{-b \pm \sqrt{b^2-4ac}}{2a}$$`)
  - **SVG** — inline SVG diagrams (e.g., `<svg>...</svg>`)
  - **MIXED** — combination of text, LaTeX, and SVG in a single field
- The `content_format` column MUST indicate the primary format: `TEXT`, `HTML`, `LATEX`, `SVG`, or `MIXED`.
- The frontend MUST render each format appropriately (KaTeX for LaTeX, inline SVG, HTML sanitized).

### FR-5: Options Storage
- MCQ/MSQ options MUST be stored as a JSONB array in the `options` column on the question table.
- Each option object: `{ "id": "A", "text": "...", "isCorrect": true/false }`
- Option `text` supports the same formats as content (text, LaTeX, SVG).

### FR-6: PDF Ingestion (Optional)
- The system MAY provide a REST endpoint `POST /api/v1/questions/import/pdf` that accepts a PDF file upload.
- PDF import is an optional feature — not required for core pipeline operation.
- If implemented: extract text, use LLM to structure into questions, store with `references` pointing to source PDF + page.

### FR-7: LLM Abstraction via Spring AI
- The system MUST use Spring AI 2.0.0 with the OpenAI-compatible client pointing to LiteLLM (http://litellm:4000).
- The system MUST define a `ChatClient` bean configured with the LiteLLM endpoint and API key.
- The system MUST define an `EmbeddingModel` bean for all-minilm via the same LiteLLM endpoint.
- The LLM provider MUST be swappable by changing only configuration (no code changes).

### FR-8: Question Validation
- AI-generated questions MUST pass schema validation (all required fields present, correct option count for MCQ).
- AI-generated questions MUST pass answer validation (correct answer exists in options for MCQ/MSQ).
- Validation failures MUST be returned in the generation response with failure reasons.

### FR-9: Batch Embedding Backfill
- The system MUST provide an admin endpoint `POST /api/v1/questions/embeddings/backfill` that generates embeddings for all questions that have a null `embedding` column.
- The endpoint MUST process in batches of 50 to avoid overwhelming the embedding service.

## Non-Functional Requirements

### NFR-1: Performance
- Embedding generation MUST complete within 200ms per question (all-minilm is ~23MB, very fast).
- Question generation (single question) MUST complete within 30 seconds.
- Duplicate detection MUST complete within 100ms using pgvector IVFFlat index on halfvec(384).

### NFR-2: Resilience
- LLM calls MUST have a 120-second timeout with 2 retries.
- If the LLM service is unavailable, question creation MUST still succeed (without embedding/duplicate detection) with a warning logged.

### NFR-3: Security
- LiteLLM API key MUST be configured via environment variable, not hardcoded.
- Generated question content MUST pass through the same encryption pipeline as manually created questions.

### NFR-4: Observability
- All LLM calls MUST be traced via OpenTelemetry (span: `ai.generate`, `ai.embed`).
- Token usage MUST be logged for cost tracking.

### NFR-5: Partitioning
- The question table MUST be hash-partitioned by `subject` (8 partitions) for horizontal scalability.
- All queries MUST include `subject` in WHERE clause to enable partition pruning.
