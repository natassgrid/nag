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

The platform uses locally hosted AI models through **Ollama** (via **LiteLLM** gateway) and **Spring AI** to generate new questions based on the knowledge extracted from these resources and blueprints.

---

## 2. Current Question Table Schema

The `question_service.question` table stores all question data:

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (v7) | Primary key, app-generated |
| `tenant_id` | VARCHAR(255) | Multi-tenant isolation |
| `subject_id` | BIGINT | Numeric foreign key to subject table |
| `topic_id` | BIGINT | Numeric foreign key to topic table |
| `subtopic_id` | BIGINT | Optional numeric foreign key to subtopic |
| `subject` | VARCHAR(100) | Subject classification (e.g., Physics, Mathematics) |
| `topic` | VARCHAR(200) | Topic within subject (e.g., Electromagnetism) |
| `subtopic` | VARCHAR(200) | Optional subtopic (e.g., Faraday's Law) |
| `chapter` | VARCHAR(200) | Optional chapter reference |
| `difficulty` | VARCHAR(20) | EASY, MEDIUM, HARD, EXPERT |
| `cognitive_level` | VARCHAR(20) | REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE |
| `question_type` | VARCHAR(30) | SINGLE_MCQ, MULTI_MCQ, NUMERICAL, DESCRIPTIVE, ASSERTION_REASON, PARAGRAPH_SET |
| `content` | TEXT (encrypted) | Question body — HTML rich text with LaTeX ($$...$$) and inline images/SVGs |
| `answer_key` | TEXT (encrypted) | Correct answer / answer key |
| `explanation` | TEXT | Detailed explanation of the correct answer (shown post-evaluation) |
| `references` | TEXT | Source citations: textbook, chapter, page, URL, or model routing reference |
| `embedding_vector` | vector(768) | 768-dim vector from nomic-embed-text for pgvector similarity |
| `has_images` | BOOLEAN | Flag indicating presence of embedded images or figures |
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
    private String id;        // A, B, C, D
    private String text;      // Option text (LaTeX $$...$$ and optional HTML/<img>)
    private boolean isCorrect;// Is this the correct answer?
}
```

---

## 3. Images in Questions and Options

### 3.1 Architecture: Asset Service Integration

The platform has a dedicated **asset-service** that stores multimedia files (images, audio, video) with metadata:

```text
┌──────────────────┐        ┌──────────────────┐
│ question-bank-   │        │ asset-service    │
│ service          │        │                  │
│                  │        │ Upload → validate│
│ content: HTML    │───────>│ → virus scan     │
│ (references      │  asset │ → store binary   │
│  asset UUIDs)    │  UUIDs │ → generate thumb │
│                  │        │                  │
│ options[].text:  │        │ GET /api/v1/assets
│  HTML            │        │ /{id}/download   │
└──────────────────┘        └──────────────────┘
```

### 3.2 How Images Work in Question Content

The `content` field stores HTML with LaTeX formulas and image references:

```html
<p>Consider the following circuit diagram:</p>
<img src="/api/v1/assets/{asset-uuid}/download" alt="Circuit diagram" />
<p>What is the total resistance $$R_{\text{total}}$$?</p>
```

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

### 3.4 Image Handling Strategy

| Concern | Approach |
|---------|----------|
| Storage | Binary stored in asset-service (local filesystem or S3-compatible) |
| Reference | Asset UUID embedded in HTML `<img>` tags |
| Security | Asset download requires valid JWT; per-tenant isolation |
| Accessibility | `alt` text mandatory for all images (WCAG 2.2 AA) |
| Encryption | Images in asset-service can be encrypted at rest via Vault |
| PDF Import | Images extracted from PDFs via `SampleDocumentParserService` (Tier 1) |
| AI Generation | Multimodal vision models ingest downsampled figures; generated stems support SVG |
| Delivery | Frontend resolves asset URLs; CDN-cacheable with signed URLs in production |
| Offline | Delivery service pre-fetches and bundles all referenced assets for offline exam packages |

---

## 4. Production AI Generation Architecture (`SpringAiGenerationService`)

The generation engine uses **Spring AI** backed by **LiteLLM / Ollama** and implements an intelligent **"Agent Only When Needed" dual-path architecture**:

```text
               ┌───────────────────────────────────────────────┐
               │    Author Request + Sample Documents (PDF)    │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │ SampleDocumentParserService (Tier 0/1/2 Ladder│
               │ Tier 0: PDFBox Text  Tier 1: PDImageXObject   │
               │ Tier 2: 150 DPI Fallback Rasterization        │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │ Pre-Generation RAG Vector Search (pgvector)   │
               │ Retrieve Top-5 Similar Questions as Negatives │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │    ComplexityEvaluator (Conditional Triage)   │
               └───────────────┬───────────────────────┬───────┘
                               │                       │
                (Low Complexity: ~90%)   (High Complexity / Overrides)
                               ▼                       ▼
    ┌──────────────────────────────────┐   ┌──────────────────────────────────┐
    │     Single-Model Fast Path       │   │  Multi-Agent Collaborative Team  │
    │  - Single LLM call via ChatClient│   │  1. Requirement Analyst Agent    │
    │  - ModelRouter (Nova Micro/Lite) │   │  2. Question Author Agent        │
    │  - Sub-2-second latency          │   │  3. Psychometric Critic Agent    │
    │                                  │   │  4. Refinement Agent (1 cycle)   │
    │                                  │   │  5. Similarity Auditor Agent     │
    └──────────────────┬───────────────┘   └──────────────────┬───────────────┘
                       │                                      │
                       └───────────────────┬──────────────────┘
                                           │
                                           ▼
               ┌───────────────────────────────────────────────┐
               │ LaTeX Normalizer & Option / Schema Validator  │
               │ Delimiter normalization: \( \) / \[ \] → $$   │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │ Taxonomy Resolution (SubjectTopicService) &   │
               │ Auto-Save as DRAFT + Embedding Computation    │
               └───────────────────────────────────────────────┘
```

---

## 5. Recommended Models

### 5.1 Generation Models

| Model | Primary Use | Routing Strategy |
|---|---|---|
| Amazon Nova Micro / Qwen3 8B | Fast Path, low-complexity MCQs (`EASY`) | `ModelRouter.selectModel("EASY")` |
| Amazon Nova Lite / Qwen3 14B | Medium & hard questions, multi-agent committee | `ModelRouter.selectModel("MEDIUM" / "HARD")` |
| Llama 3.x 8B / Gemma 3 12B | General alternative models via LiteLLM | Configurable in `application.yml` |

### 5.2 Embedding Model

| Model | Dimensions | Use |
|---|---|---|
| nomic-embed-text (v1.5) | 768 | pgvector cosine similarity, RAG pre-generation retrieval |

### 5.3 Local Setup

```bash
docker exec exam-ollama ollama pull qwen3:8b
docker exec exam-ollama ollama pull nomic-embed-text
```

### 5.4 API Access via LiteLLM (port 4000)

```bash
curl http://localhost:4000/v1/embeddings \
  -H "Authorization: Bearer sk-litellm-dev-key" \
  -H "Content-Type: application/json" \
  -d '{"model": "nomic-embed-text", "input": "What is Faraday'\''s Law?"}'
```

---

## 6. Why RAG Instead of Fine-Tuning?

The platform utilizes **RAG + LLM-based question generation**, rather than fine-tuning an LLM:

- **Dynamically Updated Knowledge Base**: New syllabus PDFs and question pools can be indexed instantly without retraining.
- **Syllabus & Negative Grounding**: Top-5 similar questions are retrieved before generation and supplied to the prompt to explicitly prevent duplicate generation.
- **Cost & Speed**: Lightweight local or cloud models generate questions in <2s on the fast path.
- **Swappable Providers**: Models are decoupled through LiteLLM and Spring AI `ChatClient`.

---

## 7. PDF Processing Pipeline (`SampleDocumentParserService`)

The parser implements a cost-optimized 3-tier extraction ladder:

1. **Tier 0 (Local Text Stream Extraction)**:
   - Uses Apache PDFBox `PDFTextStripper` to extract digital text streams directly.
   - If digital text is extracted and no formulas/diagrams require visual inspection, vision models are completely bypassed ($0.00 vision token spend).
2. **Tier 1 (Selective Diagram Isolation)**:
   - Traverses PDF resource dictionaries (`PDResources`, `PDImageXObject`).
   - Extracts and downsamples only the embedded figure (max 1024px dimension) instead of whole-page rendering.
3. **Tier 2 (Scanned Fallback)**:
   - When digital text streams are empty (scanned paper), renders pages at 150 DPI (`SCANNED_RENDER_DPI`).
   - Reduces image byte size by 75% compared to standard 300 DPI scans.

---

## 8. Topic-Based Retrieval & Taxonomy

Questions are organized in a strict relational hierarchy:

```text
Subject (e.g. Physics)
   └── Topic (e.g. Electromagnetism)
        └── Subtopic (e.g. Faraday's Law)
```

In [`SpringAiGenerationService`](file:///F:/code/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/ai/generation/SpringAiGenerationService.java), taxonomy persistence verifies:
- If `subjectId` and `topicId` are supplied in the request, [`SubjectTopicService.resolveByIds()`](file:///F:/code/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/service/SubjectTopicService.java) validates their presence and assigns IDs. It never creates arbitrary new categories from typos.
- If IDs are omitted, legacy fallback `resolveOrCreateByName()` handles backwards compatibility.

---

## 9. Generation Prompt Structure

The system prompt enforces LaTeX mathematics, KaTeX formatting, and option rules:

```text
System Prompt Rules:
- ALL mathematical and physical expressions MUST be enclosed in $$...$$ delimiters.
- Chemical reactions MUST use valid LaTeX mhchem syntax ($$\ce{2H2 + O2 -> 2H2O}$$).
- Percentage signs in math mode must be escaped as \% ($$99.9\%$$).
- For SINGLE_MCQ: exactly 4 options (A, B, C, D) with exactly ONE "isCorrect": true.
- Output ONLY a JSON array of question objects without markdown wrapping.
```

---

## 10. Multi-Agent Question Validation Pipeline

Generated questions are validated through a multi-stage quality pipeline:

```text
Generated Question
       │
       ▼
Schema & Option Validation (4 options for MCQ, valid answerKey)
       │
       ▼
Psychometric Critic Agent Review
  • Unambiguous sole correct answer
  • Plausible distractors (no throwaway "All/None of the above")
  • LaTeX KaTeX & mhchem delimiter compliance
       │
       ├─────────────────────────────────┐
       │ (Issues detected)               │ (Approved)
       ▼                                 │
Refinement Agent                         │
  • Replaces weak distractors            │
  • Fixes broken LaTeX syntax             │
  • Bounded to 1 retry loop              │
       │                                 │
       └────────────────┬────────────────┘
                        │
                        ▼
Similarity Auditor Agent (<0.85 Cosine Similarity Audit)
  • Matches pgvector cosine distance against existing tenant items
  • Reject if similarity >= 0.85
       │
       ▼
Taxonomy Resolution & Auto-Save
  • Persists to question_service.question (state=DRAFT)
  • Generates and stores 768-dim embedding_vector
```

---

## 11. Duplicate Detection via Embeddings

```text
Question content
        │
        ▼
nomic-embed-text (768-dim vector)
        │
        ▼
pgvector Cosine Similarity Search
        │
        ├─ Similarity >= 0.85 ──► Flagged as Duplicate / Rejected by SimilarityAuditorAgent
        └─ Similarity <  0.85 ──► Unique Item Accepted
```

The `embedding_vector` vector(768) column with `ivfflat` index enables sub-10ms similarity searches across hundreds of thousands of questions.

---

## 12. Spring Boot Integration

### Service Interface

The core service interface is [`QuestionGenerationService`](file:///F:/code/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/ai/generation/QuestionGenerationService.java):

```java
public interface QuestionGenerationService {

    /**
     * Generates questions based on provided request parameters.
     */
    QuestionGenerationResponse generate(QuestionGenerationRequest request, String tenantId, UUID authorId);

    /**
     * Generates questions with normalized sample questions guiding style and structure.
     * Evaluates complexity to route between Fast Path and Multi-Agent pipeline.
     */
    QuestionGenerationResponse generateWithSamples(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions,
            String tenantId,
            UUID authorId);

    /**
     * Interactively clarifies generation requirements when author parameters are ambiguous.
     */
    ClarifyRequirementsResponse clarifyRequirements(ClarifyRequirementsRequest request);
}
```

### Implementation Architecture

[`SpringAiGenerationService`](file:///F:/code/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/ai/generation/SpringAiGenerationService.java) implements the pipeline using:
- **`org.springframework.ai.chat.client.ChatClient`**: Interacts with the LiteLLM gateway.
- **`ModelRouter`**: Maps difficulty levels (`EASY`, `MEDIUM`, `HARD`) to configured models (e.g., Nova Micro, Nova Lite).
- **`ComplexityEvaluator`**: Decides between Single-Model Fast Path and Multi-Agent Committee.
- **`SampleDocumentParserService`**: Parses uploaded PDF/image sample questions.
- **Agent Committee Components**: `RequirementAnalystAgent`, `QuestionAuthorAgent`, `PsychometricCriticAgent`, `SimilarityAuditorAgent`, `RefinementAgent`.
- **`EmbeddingService` / `SpringAiEmbeddingService`**: Generates 768-dimensional embeddings via `nomic-embed-text`.

---

## 13. Infrastructure (Docker Compose)

All AI components are included in the local dev stack:

| Service | Port | Purpose |
|---------|------|---------|
| Ollama | 11434 | Model runtime (Qwen3 8B, nomic-embed-text) |
| LiteLLM | 4000 | Unified OpenAI-compatible gateway |
| PostgreSQL + pgvector | 5432 | Question storage + vector similarity |
| Asset Service | 9005 | Image/media storage for question content |

---

## 14. Implementation Phases

- **Phase 1: PDF Ingestion & Tiered Parser Ladder** (`SampleDocumentParserService`) — Completed
- **Phase 2: Embedding & pgvector Similarity Detection** (`SpringAiEmbeddingService`) — Completed
- **Phase 3: Fast Path & Multi-Agent Committee Pipeline** (`SpringAiGenerationService`) — Completed
- **Phase 4: Requirement Clarification & Multimodal Upload Endpoints** (`QuestionAiController`) — Completed
- **Phase 5: Asynchronous Bedrock Batch Generation** (`BedrockBatchService`) — Completed
