# Question Generation Pipeline — Requirements

#[[file:docs/requirements/question-pipeline.md]]

## Functional Requirements

### FR-1: Embedding Generation
- The system MUST generate 768-dimensional embeddings for every question using nomic-embed-text via LiteLLM gateway (OpenAI-compatible `/v1/embeddings` endpoint).
- Embeddings MUST be stored in the `embedding_vector` JSONB column on the question table.
- Embeddings MUST be generated on question creation and on content update.

### FR-2: Duplicate Detection
- On question creation, the system MUST compute cosine similarity between the new question's embedding and all existing questions in the same subject+tenant.
- If similarity > 0.92, the question MUST be rejected with an error message identifying the near-duplicate.
- If similarity is between 0.85–0.92, the question MUST be flagged for human review (added to response metadata).

### FR-3: AI Question Generation
- The system MUST provide a REST endpoint `POST /api/v1/questions/generate` that accepts: subject, topic, subtopic, difficulty, cognitiveLevel, questionType, count, and optional avoidDuplicate flag.
- The system MUST use RAG: retrieve top-K relevant existing questions via embedding similarity, then prompt Qwen3 8B (via LiteLLM) to generate new questions based on retrieved context.
- Generated questions MUST be returned as structured JSON matching the `CreateQuestionRequest` schema.
- Generated questions MUST have `state=DRAFT` and `references` set to "AI-generated via Qwen3 8B".
- Generated questions MUST pass duplicate detection before being stored.

### FR-4: PDF Ingestion
- The system MUST provide a REST endpoint `POST /api/v1/questions/import/pdf` that accepts a PDF file upload.
- The system MUST extract text content from the PDF (using Apache PDFBox or Tika).
- The system MUST extract images from the PDF, upload them to the asset-service, and replace with asset UUID references.
- The system MUST use the LLM to structure extracted content into question/answer/explanation/subject/topic format.
- Imported questions MUST be stored with `state=DRAFT` and `references` pointing to the source PDF filename + page.

### FR-5: LLM Abstraction via Spring AI
- The system MUST use Spring AI 2.0.0 with the OpenAI-compatible client pointing to LiteLLM (http://litellm:4000).
- The system MUST define a `ChatClient` bean configured with the LiteLLM endpoint and API key.
- The system MUST define an `EmbeddingModel` bean for nomic-embed-text via the same LiteLLM endpoint.
- The LLM provider MUST be swappable by changing only configuration (no code changes).

### FR-6: Question Validation
- AI-generated questions MUST pass schema validation (all required fields present, correct option count for MCQ).
- AI-generated questions MUST pass answer validation (correct answer exists in options for MCQ/MSQ).
- Validation failures MUST be returned in the generation response with failure reasons.

### FR-7: Batch Embedding Backfill
- The system MUST provide an admin endpoint `POST /api/v1/questions/embeddings/backfill` that generates embeddings for all questions that have a null `embedding_vector`.
- The endpoint MUST process in batches of 50 to avoid overwhelming the embedding service.

## Non-Functional Requirements

### NFR-1: Performance
- Embedding generation MUST complete within 500ms per question.
- Question generation (single question) MUST complete within 30 seconds.
- Duplicate detection MUST complete within 200ms using pgvector index.

### NFR-2: Resilience
- LLM calls MUST have a 120-second timeout with 2 retries.
- If the LLM service is unavailable, question creation MUST still succeed (without embedding/duplicate detection) with a warning logged.

### NFR-3: Security
- LiteLLM API key MUST be configured via environment variable, not hardcoded.
- Generated question content MUST pass through the same encryption pipeline as manually created questions.

### NFR-4: Observability
- All LLM calls MUST be traced via OpenTelemetry (span: `ai.generate`, `ai.embed`).
- Token usage MUST be logged for cost tracking.
