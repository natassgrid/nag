# Sequence Diagrams — NextGen Assessment Grid

## 1. Overview

This document illustrates the core end-to-end sequence flows across NAG microservices for key workflows:
1. Schedule Creation & Approval Workflow Transition
2. Cryptographic Paper Generation & Package Locking
3. Candidate Answer Submission & Signature Verification
4. AI-Assisted Question Generation (SpringAiGenerationService: Fast Path & Multi-Agent Committee)

---

## 2. Schedule Creation & Workflow Transition Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Controller as Exam Controller
    participant UI as Angular SPA
    participant GW as API Gateway
    participant SchedSvc as Scheduling Service
    participant DB as PostgreSQL DB
    participant Kafka as Kafka Event Bus

    Controller->>UI: Fill Schedule Form (Name, Exam Date, TimeZone)
    UI->>GW: POST /api/v1/examinations/{examId}/schedules
    GW->>SchedSvc: Forward with X-Tenant-Id & Authorization
    SchedSvc->>SchedSvc: Validate DTO & User Permissions
    SchedSvc->>DB: INSERT INTO schedules (status='DRAFT', version=1)
    DB-->>SchedSvc: Schedule Object Saved
    SchedSvc-->>GW: Return ScheduleResponse (201 Created)
    GW-->>UI: Display DRAFT Schedule

    Controller->>UI: Click "Transition" -> Select targetStatus="SCHEDULER_REVIEW"
    UI->>GW: PUT /api/v1/examinations/{examId}/schedules/{scheduleId}/transition
    GW->>SchedSvc: Forward Request DTO
    SchedSvc->>SchedSvc: ScheduleWorkflowEngine.validateStateTransition("DRAFT", "SCHEDULER_REVIEW")
    SchedSvc->>DB: UPDATE schedules SET status='SCHEDULER_REVIEW'
    SchedSvc->>Kafka: Publish ScheduleTransitionEvent
    SchedSvc-->>GW: Return Updated ScheduleResponse
    GW-->>UI: Update Status Chip to SCHEDULER_REVIEW
```

---

## 3. Cryptographic Paper Generation & Distribution

```mermaid
sequenceDiagram
    autonumber
    actor Controller as Exam Controller
    participant UI as Angular SPA
    participant GW as API Gateway
    participant PaperSvc as Paper Generation Service
    participant QuestSvc as Question Bank Service
    participant HSM as KMS / HSM
    participant DB as PostgreSQL DB

    Controller->>UI: Click "Generate & Lock Exam Paper Package"
    UI->>GW: POST /api/v1/examinations/{examId}/papers/generate
    GW->>PaperSvc: Forward Request
    PaperSvc->>QuestSvc: GET /api/v1/questions?topic=&difficulty=
    QuestSvc-->>PaperSvc: Return Question Pool Metadata & Encrypted Bodies
    PaperSvc->>PaperSvc: Assemble Question Sets (Set A, Set B, Set C)
    PaperSvc->>HSM: Request Ephemeral AES-256 Package Key
    HSM-->>PaperSvc: Return Key & Split Shamir Shares (3-of-5)
    PaperSvc->>PaperSvc: Encrypt Package (AES-GCM-256)
    PaperSvc->>DB: Store Encrypted Package & Share Hashes
    PaperSvc-->>GW: Paper Generation Completed (Status: SEALED)
    GW-->>UI: Display Sealed Package Manifest
```

---

## 4. Candidate Answer Submission & Audit Signing

```mermaid
sequenceDiagram
    autonumber
    actor Candidate as Candidate
    participant Terminal as Exam Terminal / App
    participant GW as API Gateway
    participant DeliverySvc as Exam Delivery Service
    participant Redis as Redis Cache
    participant DB as PostgreSQL DB
    participant Kafka as Kafka Event Bus

    Candidate->>Terminal: Click "Submit Exam Response"
    Terminal->>Terminal: Compute Payload Hash: SHA-256(Responses + Timestamp)
    Terminal->>GW: POST /api/v1/examinations/{examId}/delivery/submit
    GW->>DeliverySvc: Forward Answer Bundle & Signature Header
    DeliverySvc->>DeliverySvc: Verify Candidate Session & Payload Signature
    DeliverySvc->>Redis: Cache Submission Receipt
    DeliverySvc->>DB: INSERT INTO candidate_responses (Status='SUBMITTED')
    DeliverySvc->>Kafka: Publish AnswerSubmittedEvent (Audit Stream)
    DeliverySvc-->>GW: Return Submission Acknowledgment (HTTP 200)
    GW-->>Terminal: Show Confirmation Screen & Hash Receipt
```

---

## 5. AI-Assisted Question Generation Sequence (`SpringAiGenerationService`)

```mermaid
sequenceDiagram
    autonumber
    actor Author as Question Author
    participant UI as Admin Portal
    participant Ctrl as QuestionAiController
    participant Svc as SpringAiGenerationService
    participant Parser as SampleDocumentParserService
    participant Embed as EmbeddingService
    participant DB as PostgreSQL (pgvector)
    participant Triage as ComplexityEvaluator
    participant Committee as Multi-Agent Committee
    participant LLM as Spring AI ChatClient (LiteLLM)

    Author->>UI: Upload sample PDF/Image + generation criteria
    UI->>Ctrl: POST /api/v1/questions/generate/with-samples (multipart)
    Ctrl->>Parser: parseUploadedFile(file)
    Parser-->>Ctrl: List<NormalizedSampleQuestion> (Tier 0/1/2 ladder)
    Ctrl->>Svc: generateWithSamples(request, sampleQuestions, tenantId, authorId)

    Note over Svc,Embed: Step 0: Pre-Generation RAG Retrieval
    Svc->>Embed: embed(compositeSearchQuery)
    Embed-->>Svc: 768-dim query embedding vector
    Svc->>DB: findTopSimilarQuestions(embeddingStr, subject, tenantId, topK=5)
    DB-->>Svc: Top-5 existing questions (used as negative duplicate constraints)

    Note over Svc,Triage: Step 1: Complexity Triage ("Agent Only When Needed")
    Svc->>Triage: evaluate(request, sampleQuestions)
    Triage-->>Svc: ComplexityEvaluationResult (useMultiAgent, targetModel, rationale)

    alt Fast Path (Low complexity, standard MCQs)
        Svc->>LLM: callLlm(targetModel, systemPrompt, userPromptWithSamples)
        LLM-->>Svc: Candidate JSON output (<2s latency)
    else Multi-Agent Committee (High stakes, complex question types, formulas)
        Svc->>Committee: RequirementAnalystAgent.formulateBlueprint(...)
        Committee-->>Svc: Synthesized Blueprint
        Svc->>Committee: QuestionAuthorAgent.buildPromptWithBlueprint(...)
        Committee-->>Svc: Few-shot demonstration prompt
        Svc->>LLM: callLlm(targetModel, systemPrompt, specializedPrompt)
        LLM-->>Svc: Raw Candidate Questions
        loop For each candidate question
            Svc->>Committee: PsychometricCriticAgent.reviewQuestion(...)
            alt Issues found
                Svc->>Committee: RefinementAgent.refine(candidate, criticIssues)
            end
            Svc->>Committee: SimilarityAuditorAgent.auditUniqueness(content, subject, tenantId)
        end
    end

    Note over Svc: Normalization & Persistence
    Svc->>Svc: normalizeLatexDelimiters (Convert \( \) and \[ \] to $$...$$)
    Svc->>Svc: validateQuestion (4 options, single key, Bloom level)

    opt autoSave is true and validation passed
        Svc->>DB: persistAsDraft(question, state='DRAFT')
        Svc->>Embed: embed(questionContent)
        Svc->>DB: updateEmbedding(savedId, embeddingStr)
    end

    Svc-->>Ctrl: QuestionGenerationResponse (questions, modelUsed, executionMode, counts)
    Ctrl-->>UI: Return HTTP 200 ApiResponse with generated questions
    UI-->>Author: Display generated cards with KaTeX rendering & critic feedback
```
