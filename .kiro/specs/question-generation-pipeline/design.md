# Question Generation Pipeline — Design

#[[file:docs/requirements/question-pipeline.md]]

## Technology Choices

| Component | Technology | Details |
|-----------|-----------|---------|
| AI Framework | Spring AI 2.0.0 | OpenAI-compatible client → LiteLLM |
| LLM Gateway | LiteLLM (port 4000) | Unified API, model routing |
| Embedding Model | all-minilm via Ollama | 384-dim, ~23MB, fast on CPU |
| Generation (Math) | qwen2-math:1.5b via Ollama | Math/Science specialist |
| Generation (Trivia) | llama3.2:1b via Ollama | History, Sports, GK — fast |
| Generation (General) | qwen2.5:1.5b via Ollama | Balanced structured output |
| Vector Store | PostgreSQL 16 + pgvector | halfvec(384), IVFFlat index |
| Translation | IndicTrans2 (port 7860) | English → 22 Indian languages |
| PDF Processing | Apache PDFBox 3.x (optional) | Text + image extraction |

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                  question-bank-service                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Question     │  │ Embedding    │  │ PDF Import   │      │
│  │ Generation   │  │ Service      │  │ (optional)   │      │
│  │ Service      │  │              │  │              │      │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘      │
│         │                  │                                 │
│         │    ┌─────────────┴─────────────┐                  │
│         │    │ Model Router              │                  │
│         │    │ math → qwen2-math:1.5b    │                  │
│         │    │ trivia → llama3.2:1b      │                  │
│         │    │ general → qwen2.5:1.5b    │                  │
│         │    │ embed → all-minilm        │                  │
│         │    └─────────────┬─────────────┘                  │
│         │                  │                                 │
│         └──────────────────┼─────────────────────────────────┤
│                            │ HTTP (OpenAI-compatible)         │
└────────────────────────────┼─────────────────────────────────┘
                             ▼
                    ┌─────────────────┐
                    │ LiteLLM Gateway │
                    │ (port 4000)     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Ollama          │
                    │ (port 11434)    │
                    │ • all-minilm    │
                    │ • qwen2.5:1.5b  │
                    │ • qwen2-math    │
                    │ • llama3.2:1b   │
                    └─────────────────┘
```

## Database Schema (halfvec + partitioning)

```sql
-- question table: hash-partitioned by subject (8 partitions)
-- embedding: halfvec(384) with IVFFlat cosine index
-- options: JSONB array [{id, text, isCorrect}]
-- content is always mixed: text + $$LaTeX$$ + <svg>...</svg> inline

CREATE TABLE question_service.question (
    id UUID NOT NULL,
    subject VARCHAR(100) NOT NULL,
    ...
    content TEXT,              -- always mixed: text + $$LaTeX$$ + <svg>...</svg>
    options JSONB,            -- [{id:"A", text:"...", isCorrect:false}]
    answer_key TEXT,          -- same mixed format
    explanation TEXT,
    "references" TEXT,
    embedding halfvec(384),   -- all-minilm 384-dim half-precision vector
    ...
    PRIMARY KEY (id, subject)
) PARTITION BY HASH (subject);
```

## Content Format (always mixed)

All content fields (`content`, `answer_key`, `explanation`, `options[].text`) are always mixed format — no separate column needed. The frontend auto-detects and renders:
- `$$...$$` → KaTeX math rendering
- `<svg>...</svg>` → inline SVG
- Everything else → HTML/text

### Example: Math question with LaTeX options
```json
{
  "content": "Solve: $$x^2 - 5x + 6 = 0$$",
  "options": [
    {"id": "A", "text": "$$x = 2, 3$$", "isCorrect": true},
    {"id": "B", "text": "$$x = 1, 6$$", "isCorrect": false},
    {"id": "C", "text": "$$x = -2, -3$$", "isCorrect": false},
    {"id": "D", "text": "$$x = 2, -3$$", "isCorrect": false}
  ],
  "answerKey": "A",
  "explanation": "Factoring: $$(x-2)(x-3) = 0$$, so $$x = 2$$ or $$x = 3$$"
}
```

### Example: Diagram with SVG
```json
{
  "content": "Identify the shape:\n<svg width='100' height='100'><circle cx='50' cy='50' r='40' fill='none' stroke='black'/></svg>",
  "options": [
    {"id": "A", "text": "Circle", "isCorrect": true},
    {"id": "B", "text": "Square", "isCorrect": false},
    {"id": "C", "text": "Triangle", "isCorrect": false},
    {"id": "D", "text": "Ellipse", "isCorrect": false}
  ]
}
```

## Model Selection Logic

```java
public String selectModel(String subject) {
    return switch (subject.toLowerCase()) {
        case "mathematics", "general science", "physics", "chemistry" -> "qwen2-math-1.5b";
        case "general studies", "indian history", "indian geography",
             "current affairs", "sports" -> "llama3.2-1b";
        default -> "qwen2.5-1.5b";
    };
}
```

## Package Structure

```
com.examplatform.questionbank
├── ai/
│   ├── config/
│   │   └── SpringAiConfig.java              # ChatClient + EmbeddingModel beans
│   ├── embedding/
│   │   ├── EmbeddingService.java            # Interface
│   │   └── SpringAiEmbeddingService.java    # all-minilm via LiteLLM
│   ├── generation/
│   │   ├── QuestionGenerationService.java   # Interface
│   │   ├── SpringAiGenerationService.java   # Model routing + structured output
│   │   ├── ModelRouter.java                 # Subject → model mapping
│   │   ├── QuestionGenerationRequest.java   # Request DTO
│   │   └── QuestionGenerationResponse.java  # Response with validation results
│   ├── similarity/
│   │   └── SimilarityDetectionService.java  # halfvec cosine via pgvector
│   └── pdf/
│       ├── PdfImportService.java            # Optional PDF parsing
│       └── PdfQuestionExtractor.java        # PDFBox extraction
├── controller/
│   └── QuestionAiController.java            # /generate, /import/pdf, /embeddings/backfill
└── ...
```

## Spring AI Configuration

```yaml
spring:
  ai:
    openai:
      base-url: ${LITELLM_BASE_URL:http://localhost:4000}
      api-key: ${LITELLM_API_KEY:sk-litellm-dev-key}
      chat:
        options:
          model: qwen2.5-1.5b
          temperature: 0.7
      embedding:
        options:
          model: all-minilm
```

## Similarity Query (pgvector halfvec)

```sql
SELECT id, subject, content,
       1 - (embedding <=> :queryVec::halfvec(384)) AS similarity
FROM question_service.question
WHERE tenant_id = :tenantId
  AND subject = :subject
  AND embedding IS NOT NULL
ORDER BY embedding <=> :queryVec::halfvec(384)
LIMIT 5;
```

## Dependencies (Gradle)

```groovy
// Spring AI 2.0.0
implementation platform('org.springframework.ai:spring-ai-bom:2.0.0')
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'

// pgvector support
implementation 'org.postgresql:postgresql'
implementation 'com.pgvector:pgvector:0.1.6'

// PDF processing (optional)
implementation 'org.apache.pdfbox:pdfbox:3.0.3'
```
