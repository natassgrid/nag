# Technical Specification: Auto Async Batch Translation (ENG to Hindi)

## 1. Overview & Objectives

In large-scale multilingual government and enterprise examinations, question banks originally authored in English (`en` / `eng_Latn`) need to be localized into Hindi (`hi` / `hin_Deva`) across thousands of questions.

This specification details the architecture, design, and implementation of an **asynchronous, fault-tolerant batch auto-translation system** that translates English questions to Hindi using IndicTrans2 AI, performs clean **upserts** without unique constraint collisions, marks the translations directly as `PUBLISHED`, and guarantees **system stability** ("without killing the system") via bounded concurrency, streaming pagination, error isolation, and rate-limiting safeguards.

---

## 2. Core Requirements

1. **Async Execution & Non-Blocking Workflow**:
   - Triggering batch translation returns a `202 Accepted` response with a tracking `jobId` immediately.
   - Background execution is decoupled from HTTP request threads using Spring `@Async` and managed thread pools.

2. **System Safeguards ("Without Killing the System")**:
   - **Streaming Pagination**: Process questions in chunks (configurable `batchSize`, default `50`) to prevent Out-Of-Memory (OOM) errors.
   - **Bounded Concurrency & Rate Limiting**: Enforce a strict concurrency limit (e.g., `Semaphore` or worker pool of 2–4 workers) for calling the IndicTrans2 AI model container to prevent CPU/GPU saturation, connection pool exhaustion, or service timeouts.
   - **Pacing / Throttle Delay**: Configurable inter-batch delay (default `50ms`) to yield resources to other platform workloads.
   - **Fault Isolation**: Each question is processed in an isolated transaction context with `try-catch`. A failure in a single question (e.g. malformed LaTeX, transient timeout) is logged, recorded in failed question IDs, and does not terminate the batch job.

3. **Upsert Semantics**:
   - If a translation for `(questionId, 'hi', tenantId)` already exists, update its `translated_payload`, `source_version`, `status`, `reviewer_id`, and `review_comments`.
   - If no translation exists, insert a new `Translation` entity.
   - Prevents violations of the `uq_translation_question_lang_tenant` unique constraint.

4. **Published Status & Lifecycle Integration**:
   - Set status to `PUBLISHED` upon successful translation so questions are immediately ready for test assembly and candidate delivery.
   - Extended `TranslationStatus` and `TranslationState` enums to include `PUBLISHED`.
   - Query layer (`TranslationQueryService`) supports retrieving translations with status `PUBLISHED` or `APPROVED`.

5. **Job Monitoring & Control**:
   - Persist job state in `question_service.batch_translation_job` table.
   - Track `totalQuestions`, `processedQuestions`, `successfulQuestions`, `failedQuestions`, `startedAt`, `completedAt`, `status`, and error logs.
   - Provide APIs for querying job progress and canceling running jobs.

---

## 3. Architecture & Data Flow

```
[ Admin / Exam Controller ]
            |
            | POST /api/v1/translations/batch/auto-translate
            v
[ TranslationController ]
            |
            | (1) Creates BatchTranslationJob (PENDING)
            | (2) Returns 202 Accepted with jobId
            v
[ AsyncBatchTranslationWorker ] (@Async background execution)
            |
            +--> Page through questions from QuestionRepository
            |
            +--> For each question / chunk:
            |      |
            |      +--> Concurrency Limiter (Semaphore acquire)
            |      +--> IndicTrans2Service (/translate/batch)
            |      +--> Upsert Translation entity (status: PUBLISHED)
            |      +--> Concurrency Limiter (Semaphore release)
            |      +--> Update BatchTranslationJob progress
            |
            +--> Mark Job as COMPLETED (or FAILED if unrecoverable)
```

---

## 4. Database Schema & Entity Design

### 4.1 `batch_translation_job` Table
```sql
CREATE TABLE IF NOT EXISTS question_service.batch_translation_job (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100) NOT NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    source_language         VARCHAR(10)  NOT NULL DEFAULT 'en',
    target_language         VARCHAR(10)  NOT NULL DEFAULT 'hi',
    target_status           VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    overwrite_existing      BOOLEAN      NOT NULL DEFAULT TRUE,
    total_questions         INTEGER      NOT NULL DEFAULT 0,
    processed_questions     INTEGER      NOT NULL DEFAULT 0,
    successful_questions    INTEGER      NOT NULL DEFAULT 0,
    failed_questions        INTEGER      NOT NULL DEFAULT 0,
    failed_question_ids     JSONB        DEFAULT '[]'::jsonb,
    batch_size              INTEGER      NOT NULL DEFAULT 50,
    throttle_delay_ms       INTEGER      NOT NULL DEFAULT 50,
    max_concurrency         INTEGER      NOT NULL DEFAULT 2,
    initiated_by            UUID         NOT NULL,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_batch_trans_job_tenant_created
    ON question_service.batch_translation_job (tenant_id, created_at DESC);
CREATE INDEX idx_batch_trans_job_status
    ON question_service.batch_translation_job (status, created_at ASC);
```

### 4.2 TranslationStatus Enum Update
```java
public enum TranslationStatus {
    DRAFT,
    APPROVED,
    PUBLISHED,
    STALE
}
```

---

## 5. API Design

### 5.1 Trigger Async Batch Translation
- **Endpoint**: `POST /api/v1/translations/batch/auto-translate`
- **Security**: `@PreAuthorize("hasAnyRole('ADMIN', 'EXAM_CONTROLLER')")`
- **Request Body**:
```json
{
  "sourceLanguage": "en",
  "targetLanguage": "hi",
  "targetStatus": "PUBLISHED",
  "overwriteExisting": true,
  "batchSize": 50,
  "throttleDelayMs": 50,
  "maxConcurrency": 2,
  "subject": "General Awareness" // Optional filter
}
```
- **Response**: `202 Accepted`
```json
{
  "jobId": "0191e456-789a-7b3c-9012-3456789abcde",
  "status": "PENDING",
  "message": "Batch translation job started successfully"
}
```

### 5.2 Get Batch Job Status
- **Endpoint**: `GET /api/v1/translations/batch/{jobId}`
- **Security**: `@PreAuthorize("hasAnyRole('ADMIN', 'EXAM_CONTROLLER', 'TRANSLATOR', 'REVIEWER')")`
- **Response**: `200 OK`
```json
{
  "id": "0191e456-789a-7b3c-9012-3456789abcde",
  "tenantId": "default",
  "status": "IN_PROGRESS",
  "sourceLanguage": "en",
  "targetLanguage": "hi",
  "totalQuestions": 1500,
  "processedQuestions": 450,
  "successfulQuestions": 448,
  "failedQuestions": 2,
  "progressPercentage": 30.0,
  "startedAt": "2026-09-13T17:30:00Z",
  "completedAt": null,
  "errorMessage": null
}
```

### 5.3 List Batch Jobs
- **Endpoint**: `GET /api/v1/translations/batch`
- **Security**: `@PreAuthorize("hasAnyRole('ADMIN', 'EXAM_CONTROLLER')")`
- **Response**: `200 OK` (list of recent jobs)

### 5.4 Cancel Batch Job
- **Endpoint**: `POST /api/v1/translations/batch/{jobId}/cancel`
- **Security**: `@PreAuthorize("hasAnyRole('ADMIN', 'EXAM_CONTROLLER')")`
- **Response**: `200 OK`

---

## 6. Stability & Error Handling Strategy

1. **Backpressure and Semaphore Limits**:
   - `maxConcurrency` defaults to `2` parallel threads against the IndicTrans2 endpoint.
   - Semaphore prevents concurrent overload while other requests remain responsive.
2. **Transaction Scoping**:
   - Database writes for individual translations are isolated (`Propagation.REQUIRES_NEW` or distinct helper), preventing single translation failures from rolling back other translated questions.
3. **Graceful Cancellation**:
   - Workers check `job.getStatus() == CANCELLED` between chunks and exit cleanly without leaving corrupted states.
4. **Retry Mechanism**:
   - Transient network issues to IndicTrans2 use short exponential backoff before recording an item as failed.

---

## 7. Verification & Acceptance Criteria

1. Async invocation returns `202 Accepted` with `jobId`.
2. All target questions have Hindi translation records created or updated.
3. All successfully processed translations have `status = 'PUBLISHED'`.
4. Existing translations are cleanly upserted without duplicate key errors.
5. Large question sets process stably without OOM, thread starvation, or AI crash.
6. Progress endpoint reflects accurate counts and completion status.
7. Full suite of automated unit and integration tests passing.
