# Question Generation Pipeline — Design

#[[file:docs/requirements/question-pipeline.md]]

## Technology Choices

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| AI Framework | Spring AI | 2.0.0 | Native Spring Boot integration, OpenAI-compatible client, structured output |
| LLM Gateway | LiteLLM | latest | Unified API, model routing, swap providers without code changes |
| Generation Model | Qwen3 8B via Ollama | - | Best quality/speed for local, open-source friendly |
| Embedding Model | nomic-embed-text v1.5 | 768-dim | High quality, fast, runs on CPU |
| Vector Store | PostgreSQL + pgvector | 16 | Already in stack, no new infra needed |
| PDF Processing | Apache PDFBox | 3.x | Pure Java, no native deps, handles text + image extraction |

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                question-bank-service                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Question     │  │ Embedding    │  │ PDF Import   │  │
│  │ Generation   │  │ Service      │  │ Service      │  │
│  │ Service      │  │              │  │              │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         └──────────────────┼──────────────────┘          │
│                            │                             │
│                    ┌───────▼───────┐                     │
│                    │ Spring AI     │                     │
│                    │ ChatClient +  │                     │
│                    │ EmbeddingModel│                     │
│                    └───────┬───────┘                     │
│                            │                             │
└────────────────────────────┼─────────────────────────────┘
                             │ HTTP (OpenAI-compatible)
                             ▼
                    ┌─────────────────┐
                    │ LiteLLM Gateway │
                    │ (port 4000)     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Ollama          │
                    │ (port 11434)    │
                    │ • qwen3:8b      │
                    │ • nomic-embed   │
                    └─────────────────┘
```

## Package Structure

```
com.examplatform.questionbank
├── ai/
│   ├── config/
│   │   └── SpringAiConfig.java              # ChatClient + EmbeddingModel beans
│   ├── embedding/
│   │   ├── EmbeddingService.java            # Interface
│   │   └── SpringAiEmbeddingService.java    # Spring AI EmbeddingModel impl
│   ├── generation/
│   │   ├── QuestionGenerationService.java   # Interface
│   │   ├── SpringAiGenerationService.java   # ChatClient-based impl
│   │   ├── QuestionGenerationRequest.java   # Request DTO
│   │   └── QuestionGenerationResponse.java  # Response DTO with validation results
│   ├── similarity/
│   │   └── SimilarityDetectionService.java  # Cosine similarity via pgvector
│   └── pdf/
│       ├── PdfImportService.java            # PDF parsing + LLM structuring
│       └── PdfQuestionExtractor.java        # PDFBox text/image extraction
├── controller/
│   └── QuestionAiController.java            # /generate, /import/pdf, /embeddings/backfill
└── ...
```

## Key Design Decisions

### 1. Spring AI Configuration

```java
@Configuration
public class SpringAiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

`application.yml`:
```yaml
spring:
  ai:
    openai:
      base-url: http://litellm:4000
      api-key: ${LITELLM_API_KEY:sk-litellm-dev-key}
      chat:
        options:
          model: qwen3-8b
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text
```

Spring AI 2.0.0's OpenAI client works with any OpenAI-compatible endpoint (LiteLLM).

### 2. Embedding Storage

The `embedding_vector` column is currently JSONB. For pgvector cosine similarity:

```sql
-- Migration to add pgvector support (when extension is available):
-- ALTER TABLE question_service.question
--   ADD COLUMN embedding vector(768);
-- CREATE INDEX idx_question_embedding ON question_service.question
--   USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

Until pgvector extension is enabled, similarity is computed in-application using the JSONB array.

### 3. Question Generation Prompt Template

```text
You are an expert examination question author for Indian competitive exams.

Generate {count} {questionType} questions for:
- Subject: {subject}
- Topic: {topic}
- Subtopic: {subtopic}
- Difficulty: {difficulty}
- Cognitive Level: {cognitiveLevel}

Reference material (existing questions on this topic):
{retrievedContext}

Requirements:
- Each question MUST be original (do not copy the reference material)
- MCQ: exactly 4 options (A-D), exactly one correct answer
- Include a detailed explanation citing the underlying concept
- Include difficulty-appropriate distractors
- Return valid JSON array matching this schema:
[{
  "content": "HTML question text",
  "questionType": "MCQ",
  "difficulty": "{difficulty}",
  "cognitiveLevel": "{cognitiveLevel}",
  "options": [{"id":"A","text":"...","isCorrect":false}, ...],
  "answerKey": "B",
  "explanation": "...",
  "references": "AI-generated based on: {topic}/{subtopic}"
}]
```

### 4. Duplicate Detection Flow

```text
1. New question content → embed via EmbeddingService
2. Query: SELECT id, 1 - (embedding <=> :newEmbedding) AS similarity
          FROM question WHERE tenant_id = :tenantId AND subject = :subject
          ORDER BY similarity DESC LIMIT 5
3. If max similarity > 0.92 → DuplicateException
4. If max similarity > 0.85 → return warning in response
5. Store embedding on question record
```

### 5. PDF Import Flow

```text
1. Upload PDF → store temporarily
2. Extract text per page (PDFBox)
3. Extract images per page → upload to asset-service → get UUIDs
4. For each page/section:
   a. Prompt LLM to identify questions, answers, explanations
   b. Prompt LLM to classify subject/topic/subtopic/difficulty
   c. Replace image refs with <img src="/api/v1/assets/{uuid}/download" />
5. Create Question entities (state=DRAFT)
6. Generate embeddings for each
7. Return import summary (count, duplicates skipped, etc.)
```

### 6. Error Handling

| Scenario | Behavior |
|----------|----------|
| LLM timeout | Retry 2x, then return partial results with error |
| LLM unavailable | Question creation succeeds without embedding; log warning |
| Invalid LLM response (unparseable JSON) | Retry with stricter prompt; if fails, skip that question |
| Duplicate detected | Return 409 Conflict with duplicate question ID |
| PDF too large (>50MB) | Return 413 with size limit message |
| PDF has no extractable text | Return 422 with "no text content found" |

## Dependencies (Gradle)

```groovy
// Spring AI 2.0.0
implementation platform('org.springframework.ai:spring-ai-bom:2.0.0')
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'

// PDF processing
implementation 'org.apache.pdfbox:pdfbox:3.0.3'
```
