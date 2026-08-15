# Question Generation Pipeline — Tasks

## Phase 1: Spring AI Setup & Embedding Service

- [ ] 1.1: Add Spring AI 2.0.0 BOM and OpenAI starter dependency to `question-bank-service/build.gradle`
- [ ] 1.2: Create `SpringAiConfig.java` with `ChatClient` and `EmbeddingModel` beans, configured via `application.yml` to point to LiteLLM (http://litellm:4000)
- [ ] 1.3: Add Spring AI config to `application.yml` and `application-docker.yml` (base-url, api-key, model names)
- [ ] 1.4: Create `EmbeddingService` interface with `float[] embed(String text)` and `List<float[]> embedBatch(List<String> texts)`
- [ ] 1.5: Implement `SpringAiEmbeddingService` using Spring AI's `EmbeddingModel` to call nomic-embed-text via LiteLLM
- [ ] 1.6: Write unit test for `SpringAiEmbeddingService` with mocked `EmbeddingModel`

## Phase 2: Similarity Detection & Duplicate Checking

- [ ] 2.1: Create `SimilarityDetectionService` that computes cosine similarity between a new embedding and existing question embeddings (in-app via JSONB arrays initially)
- [ ] 2.2: Add helper method to `QuestionRepository` to fetch embedding vectors for a given tenant + subject
- [ ] 2.3: Integrate embedding generation into `QuestionService.createQuestion()` — generate and store embedding after save
- [ ] 2.4: Integrate duplicate detection into `QuestionService.createQuestion()` — check similarity before save, reject if > 0.92, warn if > 0.85
- [ ] 2.5: Add `POST /api/v1/questions/embeddings/backfill` endpoint in a new `QuestionAiController` for admin batch embedding generation
- [ ] 2.6: Write integration test for duplicate detection flow (mock LiteLLM response)

## Phase 3: AI Question Generation

- [ ] 3.1: Create `QuestionGenerationRequest` DTO (subject, topic, subtopic, difficulty, cognitiveLevel, questionType, count, avoidDuplicate)
- [ ] 3.2: Create `QuestionGenerationResponse` DTO (list of generated questions, validation results, duplicates detected)
- [ ] 3.3: Create `QuestionGenerationService` interface with `QuestionGenerationResponse generate(QuestionGenerationRequest request)`
- [ ] 3.4: Implement `SpringAiGenerationService` using Spring AI `ChatClient` with structured output:
  - Build prompt from template with retrieved context
  - Call Qwen3 8B via LiteLLM
  - Parse structured JSON response into question DTOs
  - Validate each generated question (schema + answer check)
  - Run duplicate detection on each
  - Return results with validation status per question
- [ ] 3.5: Implement RAG retrieval: query top-5 similar existing questions by topic+embedding for context injection
- [ ] 3.6: Add `POST /api/v1/questions/generate` endpoint in `QuestionAiController`
- [ ] 3.7: Add option to auto-save generated questions as DRAFT (vs. return-only preview mode)
- [ ] 3.8: Write integration test for question generation (mock ChatClient response with valid JSON)

## Phase 4: PDF Import

- [ ] 4.1: Add Apache PDFBox 3.x dependency to `build.gradle`
- [ ] 4.2: Create `PdfQuestionExtractor` — extracts text per page, extracts embedded images
- [ ] 4.3: Create `PdfImportService`:
  - Accept uploaded PDF
  - Extract text + images via `PdfQuestionExtractor`
  - Upload images to asset-service (via REST client), get asset UUIDs
  - Prompt LLM to structure content into questions (subject, topic, options, answer, explanation)
  - Create Question entities with state=DRAFT, references=source PDF + page
  - Generate embeddings + run duplicate detection
  - Return import summary
- [ ] 4.4: Add `POST /api/v1/questions/import/pdf` endpoint in `QuestionAiController` (multipart upload)
- [ ] 4.5: Add file size validation (max 50MB) and content-type validation (application/pdf only)
- [ ] 4.6: Write unit test for `PdfQuestionExtractor` with a sample PDF

## Phase 5: Observability & Resilience

- [ ] 5.1: Add OpenTelemetry span instrumentation to `EmbeddingService` and `QuestionGenerationService` calls
- [ ] 5.2: Add token usage logging (input/output tokens from LLM response metadata)
- [ ] 5.3: Configure retry logic (2 retries, 120s timeout) via Spring AI client configuration
- [ ] 5.4: Add circuit breaker for LLM calls — if Ollama/LiteLLM is down, question creation proceeds without AI features (graceful degradation)
- [ ] 5.5: Add Prometheus metrics: `ai_embedding_duration_seconds`, `ai_generation_duration_seconds`, `ai_generation_questions_total`

## Phase 6: pgvector Migration (Optional — when extension available)

- [ ] 6.1: Create Flyway migration to add `vector(768)` column and IVFFlat index (conditional on pgvector extension)
- [ ] 6.2: Update `SimilarityDetectionService` to use native pgvector `<=>` operator for cosine distance
- [ ] 6.3: Add migration to copy JSONB embeddings to native vector column
- [ ] 6.4: Benchmark: in-app similarity vs. pgvector native (target: <200ms for 100K questions)
