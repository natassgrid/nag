# Question Generation Pipeline — Tasks

## Phase 1: Spring AI Setup & Embedding Service

- [x] 1.1: Add Spring AI 2.0.0 BOM and OpenAI starter + pgvector dependency to `question-bank-service/build.gradle`
- [x] 1.2: Create `SpringAiConfig.java` with `ChatClient` and `EmbeddingModel` beans pointing to LiteLLM (http://litellm:4000)
- [x] 1.3: Add Spring AI config to `application.yml` and `application-docker.yml` (base-url, api-key, model: all-minilm for embedding, qwen2.5-1.5b for chat)
- [x] 1.4: Create `EmbeddingService` interface with `float[] embed(String text)` and `List<float[]> embedBatch(List<String> texts)`
- [x] 1.5: Implement `SpringAiEmbeddingService` using Spring AI's `EmbeddingModel` to call all-minilm (384-dim) via LiteLLM
- [x] 1.6: Write unit test for `SpringAiEmbeddingService` with mocked `EmbeddingModel`

## Phase 2: Entity & Repository Updates for halfvec

- [x] 2.1: Update `Question` entity — add `embedding` field (mapped to `halfvec(384)` column), `options` JSONB field
- [x] 2.2: Remove old `embedding_vector` JSONB field from entity
- [x] 2.3: Add pgvector JPA type support (custom `@Type` or `AttributeConverter` for halfvec)
- [x] 2.4: Update `QuestionRepository` — add native query for cosine similarity search using `<=>` operator on halfvec
- [x] 2.5: Update `QuestionResponse` and `CreateQuestionRequest` DTOs — update `options` handling
- [x] 2.6: Verify partitioned table works with JPA (composite PK: id + subject)

## Phase 3: Similarity Detection & Duplicate Checking

- [x] 3.1: Create `SimilarityDetectionService` using native pgvector `<=>` cosine distance on halfvec(384)
- [x] 3.2: Integrate embedding generation into `QuestionService.createQuestion()` — generate and store embedding after save
- [x] 3.3: Integrate duplicate detection into `QuestionService.createQuestion()` — check similarity before save, reject > 0.92, warn > 0.85
- [x] 3.4: Add `POST /api/v1/questions/embeddings/backfill` endpoint for admin batch embedding generation (batches of 50)
- [x] 3.5: Write integration test for duplicate detection flow

## Phase 4: AI Question Generation with Model Routing

- [x] 4.1: Create `ModelRouter.java` — maps subject to model name (math→qwen2-math-1.5b, trivia→llama3.2-1b, general→qwen2.5-1.5b)
- [x] 4.2: Create `QuestionGenerationRequest` DTO (subject, topic, subtopic, difficulty, cognitiveLevel, questionType, count, avoidDuplicate)
- [x] 4.3: Create `QuestionGenerationResponse` DTO (list of generated questions, validation results, duplicates detected, model used)
- [x] 4.4: Create `QuestionGenerationService` interface
- [x] 4.5: Implement `SpringAiGenerationService`:
  - Select model via ModelRouter based on subject
  - Build prompt from template with retrieved context (RAG)
  - Call selected model via LiteLLM ChatClient
  - Parse structured JSON response into question DTOs
  - Validate each generated question (schema + answer check)
  - Run duplicate detection on each
  - Return results with validation status per question
- [x] 4.6: Implement RAG retrieval: query top-5 similar existing questions via halfvec embedding for context injection
- [x] 4.7: Add `POST /api/v1/questions/generate` endpoint in `QuestionAiController`
- [x] 4.8: Add option to auto-save generated questions as DRAFT (vs. return-only preview mode)
- [x] 4.9: Write integration test for question generation with mocked ChatClient

## Phase 5: Frontend Rendering (Text + LaTeX + SVG)

- [x] 5.1: Frontend: integrate KaTeX library for rendering `$$...$$` blocks in question content and options
- [x] 5.2: Frontend: support inline SVG rendering in question content and options
- [x] 5.3: Frontend: update Create Question form to support mixed content input (text + LaTeX + SVG preview)

## Phase 6: PDF Import (Optional)

- [ ] 6.1: Add Apache PDFBox 3.x dependency to `build.gradle`
- [ ] 6.2: Create `PdfQuestionExtractor` — extracts text per page, extracts embedded images
- [ ] 6.3: Create `PdfImportService` — accepts PDF, extracts content, prompts LLM to structure, stores as DRAFT
- [ ] 6.4: Add `POST /api/v1/questions/import/pdf` endpoint (multipart upload, max 50MB)
- [ ] 6.5: Write unit test for `PdfQuestionExtractor`

## Phase 7: Observability & Resilience

- [ ] 7.1: Add OpenTelemetry span instrumentation to embedding and generation service calls
- [ ] 7.2: Add token usage logging from LLM response metadata
- [ ] 7.3: Configure retry logic (2 retries, 120s timeout) via Spring AI client configuration
- [ ] 7.4: Add circuit breaker — if Ollama/LiteLLM is down, question creation proceeds without AI features
- [ ] 7.5: Add Prometheus metrics: `ai_embedding_duration_seconds`, `ai_generation_duration_seconds`, `ai_generation_questions_total`
