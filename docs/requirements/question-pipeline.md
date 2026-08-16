# Ollama-Based Question Generation for Competitive Exam Preparation

## 1. Objective

The goal is to build an open-source platform that helps students prepare for competitive examinations by using a collection of PDF resources containing:

- Questions
- Answers
- Explanations
- Subjects
- Topics
- Sub-topics
- Previous examination questions

The platform should use locally hosted AI models through **Ollama** (via **LiteLLM** gateway) to generate new questions based on the knowledge extracted from these PDFs.

---

## 2. Current Question Table Schema

The `question_service.question` table stores all question data:

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (v7) | Primary key, app-generated |
| `tenant_id` | VARCHAR(255) | Multi-tenant isolation |
| `subject` | VARCHAR(100) | Subject classification (e.g., Physics, Mathematics) |
| `topic` | VARCHAR(200) | Topic within subject (e.g., Electromagnetism) |
| `subtopic` | VARCHAR(200) | Optional subtopic (e.g., Faraday's Law) |
| `chapter` | VARCHAR(200) | Optional chapter reference |
| `difficulty` | VARCHAR(20) | EASY, MEDIUM, HARD, EXPERT |
| `cognitive_level` | VARCHAR(20) | REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE |
| `question_type` | VARCHAR(30) | MCQ, MSQ, NUMERICAL, DESCRIPTIVE, TRUE_FALSE, FILL_BLANK |
| `content` | TEXT (encrypted) | Question body — HTML rich text (supports inline images via asset URLs) |
| `answer_key` | TEXT (encrypted) | Correct answer / answer key |
| `explanation` | TEXT | Detailed explanation of the correct answer (shown post-evaluation) |
| `references` | TEXT | Source citations: textbook, chapter, page, URL, PDF source |
| `embedding_vector` | JSONB | 768-dim vector from nomic-embed-text for similarity detection |
| `state` | VARCHAR(20) | Lifecycle: DRAFT → REVIEW → APPROVED → PUBLISHED → RETIRED |
| `encryption_key_id` | VARCHAR(255) | Vault DEK reference for content/answerKey encryption |
| `usage_count` | INTEGER | Times used in exams |
| `last_used_at` | TIMESTAMP | Last exam usage date |
| `used_in_exam_ids_json` | JSONB | Array of exam IDs where used |
| `used_in_shift_ids_json` | JSONB | Array of shift IDs where used |
| `author_id` | UUID | Question author (user or system for AI-generated) |
| `reviewer_id` | UUID | Assigned reviewer |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last modification timestamp |
| `version` | BIGINT | Optimistic locking version |

### Supporting Tables

- **`question_service.subject`** — Subject master (id, name, code, description, tenant)
- **`question_service.topic`** — Topic linked to subject (cascading FK)
- **`question_service.subtopic`** — Subtopic linked to topic (cascading FK)
- **`question_service.question_version`** — Append-only version history with diff/snapshot JSON

### Question Options (MCQ/MSQ)

Options are stored as JSON within the question record and represented in the DTO:

```java
public class QuestionOption {
    private String id;        // A, B, C, D, E, F
    private String text;      // Option text (can include HTML with <img> tags)
    private boolean correct;  // Is this the correct answer?
}
```

---

## 3. Images in Questions and Options

### 3.1 Architecture: Asset Service Integration

The platform has a dedicated **asset-service** that stores multimedia files (images, audio, video) with metadata:

```text
┌──────────────────┐        ┌──────────────────┐
│ question-bank-   │        │ asset-service     │
│ service          │        │                   │
│                  │        │ Upload → validate │
│ content: HTML    │───────>│ → virus scan     │
│ (references      │  asset │ → store binary   │
│  asset UUIDs)    │  UUIDs │ → generate thumb │
│                  │        │                   │
│ options[].text:  │        │ GET /api/v1/assets│
│  HTML            │        │ /{id}/download    │
└──────────────────┘        └──────────────────┘
```

### 3.2 How Images Work in Question Content

The `content` field stores **HTML** (via the Quill rich text editor). Images are embedded as:

```html
<p>Consider the following circuit diagram:</p>
<img src="/api/v1/assets/{asset-uuid}/download" alt="Circuit diagram" />
<p>What is the total resistance?</p>
```

The asset UUID references a file stored in the asset-service. During exam delivery, the frontend resolves these URLs to render images inline.

### 3.3 How Images Work in Options

Options support HTML in the `text` field, enabling image-based options:

```json
{
  "options": [
    { "id": "A", "text": "<img src=\"/api/v1/assets/uuid-1/download\" alt=\"Option A diagram\" />", "isCorrect": false },
    { "id": "B", "text": "<img src=\"/api/v1/assets/uuid-2/download\" alt=\"Option B diagram\" />", "isCorrect": true },
    { "id": "C", "text": "None of the above", "isCorrect": false },
    { "id": "D", "text": "<img src=\"/api/v1/assets/uuid-3/download\" alt=\"Option D diagram\" />", "isCorrect": false }
  ]
}
```

Options can also mix text and images:

```json
{ "id": "A", "text": "Waveform X: <img src=\"/api/v1/assets/uuid-1/download\" alt=\"Waveform X\" />", "isCorrect": true }
```

### 3.4 Image Handling Strategy

| Concern | Approach |
|---------|----------|
| Storage | Binary stored in asset-service (local filesystem or S3-compatible) |
| Reference | Asset UUID embedded in HTML `<img src>` tags |
| Security | Asset download requires valid JWT; per-tenant isolation |
| Accessibility | `alt` text mandatory for all images (WCAG 2.2 AA) |
| Encryption | Images in asset-service can be encrypted at rest via Vault |
| PDF Import | Images extracted from PDFs are uploaded to asset-service, UUIDs inserted into content HTML |
| AI Generation | LLM generates text-only questions; images must be added manually or via separate diagram generation |
| Delivery | Frontend resolves asset URLs; CDN-cacheable with signed URLs in production |
| Offline | Delivery service pre-fetches and bundles all referenced assets for offline exam packages |

### 3.5 Frontend Implementation

The Quill editor in the Create Question form supports image insertion via:
1. **Asset Library picker** — browse existing assets, select, insert `<img>` tag
2. **Direct upload** — upload from toolbar, auto-uploads to asset-service, inserts UUID reference

For options, the frontend provides a toggle to switch between text-only and rich-text (HTML) mode per option.

---

## 4. Recommended Architecture

The recommended approach is **RAG + LLM-based question generation**, rather than immediately fine-tuning an LLM.

```text
                    ┌──────────────────┐
                    │    Exam PDFs     │
                    │ Questions/Answers│
                    └────────┬─────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │ PDF Parser / OCR    │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │ Content Structuring │
                  │                     │
                  │ Subject             │
                  │ Topic               │
                  │ Sub-topic           │
                  │ Question            │
                  │ Answer              │
                  │ Explanation         │
                  │ References          │
                  └──────────┬──────────┘
                             │
                 ┌───────────┴───────────┐
                 ▼                       ▼
          ┌─────────────┐        ┌──────────────┐
          │ Embeddings  │        │ Question DB  │
          │nomic-embed  │        │ (PostgreSQL) │
          └──────┬──────┘        └──────────────┘
                 │
                 ▼
          ┌───────────────┐
          │ pgvector      │
          │ (768-dim)     │
          └───────┬───────┘
                  │
        Topic + difficulty +
        question type
                  │
                  ▼
          ┌────────────────┐
          │ LiteLLM Gateway│
          │ (port 4000)    │
          └───────┬────────┘
                  │
                  ▼
          ┌────────────────┐
          │ Ollama         │
          │ Qwen3 8B       │
          └───────┬────────┘
                  │
                  ▼
          ┌────────────────┐
          │ Question       │
          │ Generator      │
          └───────┬────────┘
                  │
                  ▼
          ┌────────────────┐
          │ Validator      │
          │ + Deduplication│
          └───────┬────────┘
                  │
                  ▼
          ┌────────────────┐
          │ Question Bank  │
          │ (state=DRAFT)  │
          └────────────────┘
```

---

## 5. Recommended Ollama Models

### 5.1 Generation Models

| Model | Primary Use | Recommendation |
|---|---|---|
| Qwen3 8B | Question generation, classification, explanations | Primary |
| Qwen3 14B | Higher-quality question generation | When resources allow |
| Llama 3.x 8B | General generation | Alternative |
| Gemma 3 12B | Reasoning and generation | Alternative |

### 5.2 Embedding Model

| Model | Dimensions | Use |
|---|---|---|
| nomic-embed-text (v1.5) | 768 | Question similarity, RAG retrieval |

### 5.3 Local Setup

```bash
# Models are auto-pulled via docker compose (ollama-pull service)
# Or manually:
docker exec exam-ollama ollama pull qwen3:8b
docker exec exam-ollama ollama pull nomic-embed-text
```

### 5.4 API Access via LiteLLM (port 4000)

```bash
# Generate embeddings
curl http://localhost:4000/v1/embeddings \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -H "Content-Type: application/json" \
  -d '{"model": "nomic-embed-text", "input": "What is Faraday'\''s Law?"}'

# Generate questions via chat completion
curl http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3-8b",
    "messages": [{"role": "user", "content": "Generate 5 MCQ questions on Faraday'\''s Law..."}]
  }'
```

---

## 6. Why RAG Instead of Fine-Tuning?

The initial version should **not require model fine-tuning**.

Instead:

```text
PDF → Extract knowledge → Store structured content → Generate embeddings
→ Vector search → Retrieve relevant content → Qwen3 → Generate question
```

Advantages:

- No expensive model training
- Easier to update the knowledge base
- New PDFs can be added dynamically
- Source attribution via `references` field
- Different examination domains can coexist
- Users can run the system locally
- The LLM can be replaced via LiteLLM config

Fine-tuning can be considered after the platform accumulates a large, validated question dataset.

---

## 7. PDF Processing Pipeline

```text
PDF
 │
 ▼
PDF Parser (Apache PDFBox / Tika)
 │
 ├── Text extraction
 ├── Image extraction → upload to asset-service → get asset UUIDs
 ├── Table extraction
 └── OCR when required (Tesseract)
 │
 ▼
Content segmentation
 │
 ▼
Question extraction (LLM-assisted)
 │
 ▼
Answer + Explanation extraction
 │
 ▼
Topic classification (LLM or embedding-based)
 │
 ▼
Store in question table (state=DRAFT, references=source PDF + page)
```

A normalized question maps directly to the database schema:

```json
{
  "subject": "Physics",
  "topic": "Electromagnetism",
  "subtopic": "Faraday's Law",
  "difficulty": "MEDIUM",
  "cognitiveLevel": "APPLY",
  "questionType": "MCQ",
  "content": "<p>A coil of 100 turns...</p>",
  "options": [
    { "id": "A", "text": "0.5 V", "isCorrect": false },
    { "id": "B", "text": "1.0 V", "isCorrect": true },
    { "id": "C", "text": "1.5 V", "isCorrect": false },
    { "id": "D", "text": "2.0 V", "isCorrect": false }
  ],
  "answerKey": "B",
  "explanation": "Using Faraday's Law: EMF = -N × dΦ/dt = -100 × 0.01 = 1.0 V",
  "references": "physics-question-bank.pdf, page 42; NCERT Class 12, Ch. 6"
}
```

---

## 8. Topic-Based Retrieval

Questions are organized in a hierarchy (stored in subject/topic/subtopic tables):

```text
Subject
   └── Topic
        └── Subtopic
             └── Concept (via embeddings)
```

Example:

```text
Physics
 └── Electromagnetism
      ├── Faraday's Law
      ├── Lenz's Law
      ├── Electromagnetic Induction
      └── Transformers
```

This allows generation requests like:

```json
{
  "subject": "Physics",
  "topic": "Electromagnetism",
  "subtopic": "Faraday's Law",
  "questionType": "MCQ",
  "difficulty": "MEDIUM",
  "cognitiveLevel": "APPLY",
  "count": 10,
  "avoidDuplicate": true,
  "sourceBased": true
}
```

---

## 9. Question Generation Prompt

Retrieved source material is supplied to the LLM with structured instructions:

```text
Topic: Faraday's Law
Difficulty: Medium
Cognitive Level: Apply
Question Type: MCQ (4 options, exactly one correct)

Source Material:
[Retrieved content from vector search]

Existing Questions (to avoid duplication):
[Embeddings-matched existing questions]

Generate 5 NEW questions. Requirements:
- Do not copy source questions
- Test the same underlying concepts with different scenarios/values
- Provide exactly 4 options (A-D)
- Have exactly one correct answer
- Include a detailed explanation referencing the source concept
- Include source reference (topic, textbook if known)
- Maintain the requested difficulty and cognitive level
- Return structured JSON matching the question schema
```

---

## 10. Question Validation Pipeline

Generated questions enter the existing lifecycle FSM:

```text
AI Generated Question
       │
       ▼
Schema Validation (JSON structure, required fields)
       │
       ▼
Answer Validation (correct answer exists in options)
       │
       ▼
Duplicate Detection (embedding similarity > threshold → reject)
       │
       ▼
Difficulty Validation (LLM cross-check)
       │
       ▼
Source Grounding Validation (explanation cites source)
       │
       ▼
Quality Score
       │
       ├── Reject (auto)
       │
       ├── state=DRAFT (needs human review)
       │
       └── state=REVIEW (high confidence → fast-track to reviewer)
```

Human reviewers then move questions through REVIEW → APPROVED → PUBLISHED via the existing lifecycle transitions.

---

## 11. Duplicate Detection via Embeddings

```text
Generated Question content
        │
        ▼
nomic-embed-text (768-dim vector)
        │
        ▼
pgvector cosine similarity search
        │
        ▼
If similarity > 0.92 → reject as near-duplicate
If similarity 0.85-0.92 → flag for human review
If similarity < 0.85 → pass
```

The `embedding_vector` JSONB column on the question table stores pre-computed vectors for all existing questions, enabling sub-second similarity lookups.

---

## 12. Spring Boot Integration

### Service Interface (LLM-agnostic)

```java
public interface QuestionGenerationService {
    List<GeneratedQuestion> generate(QuestionGenerationRequest request);
}

public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

### Implementation via LiteLLM (OpenAI-compatible)

```text
QuestionGenerationService
        │
        └── LiteLLMQuestionGenerationService
                 └── POST http://litellm:4000/v1/chat/completions
                          model: "qwen3-8b"

EmbeddingService
        │
        └── LiteLLMEmbeddingService
                 └── POST http://litellm:4000/v1/embeddings
                          model: "nomic-embed-text"
```

This allows swapping to OpenAI, Anthropic, or AWS Bedrock by changing only the LiteLLM config — no code changes needed.

---

## 13. Infrastructure (Docker Compose)

All AI services are included in the local dev stack:

| Service | Port | Purpose |
|---------|------|---------|
| Ollama | 11434 | Model runtime (Qwen3 8B, nomic-embed-text) |
| LiteLLM | 4000 | Unified OpenAI-compatible gateway |
| PostgreSQL + pgvector | 5432 | Question storage + vector similarity |
| Asset Service | 9005 | Image/media storage for question content |

---

## 14. Suggested MVP Phases

### Phase 1: PDF Ingestion
```text
PDF → Extract text + images → Store in asset-service
→ LLM-assisted question extraction → Store in question table (state=DRAFT)
→ Set references field to source PDF + page
```

### Phase 2: Embedding & Similarity
```text
All questions → nomic-embed-text → Store embedding_vector
→ Enable duplicate detection on new question creation
```

### Phase 3: AI Question Generation
```text
Topic + Difficulty + Cognitive Level
→ Vector search (retrieve relevant existing questions/content)
→ Qwen3 8B via LiteLLM
→ Generate new questions
→ Validate + deduplicate
→ Store as DRAFT for human review
```

### Phase 4: Quality & Automation
```text
→ Auto-difficulty classification
→ Auto-cognitive-level classification
→ Explanation generation for questions lacking explanations
→ Multi-language question generation (22 scheduled languages)
→ Batch generation with exam blueprint constraints
```

### Phase 5: Advanced
```text
→ Student performance-based adaptive question generation
→ Weak-topic detection → targeted question generation
→ Image/diagram generation (future — specialized models)
→ Question quality scoring via LLM-as-judge
→ Fine-tuning on validated question corpus
```
