# Spec: Examination Response Collection, Integrity Validation & Result Analytics
**Issue:** [#101](https://github.com/natassgrid/nag/issues/101)  
**Branch:** `feat/issue-101-response-collection-evaluation-results`  
**Date:** 2026-09-14  
**Author:** Antigravity AI — Spec-Driven Development Plan

---

## 1. Problem Statement

The NAG platform needs a complete, end-to-end pipeline for:

1. **High-throughput, tamper-evident response ingestion** during live examinations.
2. **Automated post-exam evaluation** with psychometric scoring.
3. **Diagnostic result analytics** (per-question, per-section, cognitive-level, time-telemetry).
4. **Downloadable, verifiable scorecards** with digital signatures and QR verification codes.
5. **Interactive post-exam review UI** with LaTeX step-by-step solutions and peer benchmarks.

---

## 2. Current State Analysis

### 2.1 What Already Exists

| Service | What's There | Quality |
|---|---|---|
| `response-service` | `ResponseController`, `ResponseSaveService`, `BulkSaveService`, `SessionFinalizationService`, `AutoSaveService`, `ResponseHistoryService` | ✅ Production-ready skeleton |
| `evaluation-service` | `AutoEvaluationService`, `ManualEvaluationService`, `ScoreAggregationService`, `EvaluationController` | ✅ Core logic present |
| `result-service` | `ResultComputationService`, `ScorecardPdfService`, `QuestionAnalyticsService`, `ResultPublicationService` | ⚠️ Partial — see gaps below |
| `candidate-frontend` | `Results.tsx` — shows score, section breakdown, PDF download button | ⚠️ Missing: diagnostic analytics, question review, cognitive breakdown |

### 2.2 Identified Gaps

| ID | Gap | Service | Severity |
|---|---|---|---|
| G1 | **Integrity validation**: No check that `questionId` belongs to the candidate's randomized paper | `response-service` | 🔴 Critical |
| G2 | **Session-timing enforcement**: `submitSession()` doesn't verify the session hasn't expired | `response-service` | 🔴 Critical |
| G3 | **Double-submission prevention**: Redis idempotency key on `/submit` is missing | `response-service` | 🔴 Critical |
| G4 | **Anti-tamper telemetry fields**: No `clientIp`, `userAgent`, `focusLossCount` fields on `Response` entity | `response-service` | 🟠 High |
| G5 | **`QuestionAnalyticsService` uses mock data**: Must be replaced with real cross-service aggregation | `result-service` | 🔴 Critical |
| G6 | **Missing diagnostic fields on `Result`**: `accuracyRate`, `cognitiveBreakdown`, `timeSpentAnalysis`, `topicBreakdown` | `result-service` | 🔴 Critical |
| G7 | **Category rank missing**: `Result` domain only has `overallRank` — no `categoryRank` | `result-service` | 🟠 High |
| G8 | **Scorecard PDF lacks QR verification + digital signature**: Only password-protected PDF, no QR/signature | `result-service` | 🟠 High |
| G9 | **Post-exam review UI missing entirely**: No `ReviewExam.tsx` page with question-level review + solutions | `candidate-frontend` | 🔴 Critical |
| G10 | **Cognitive-level + topic breakdown missing from Results page** | `candidate-frontend` | 🟠 High |
| G11 | **Section cutoff qualification status not surfaced in API** | `result-service` | 🟡 Medium |
| G12 | **`ScoreAggregationService` doesn't emit event to trigger result-service** | `evaluation-service` | 🟠 High |

---

## 3. Functional Specifications

### 3.1 Response Collection Pipeline (response-service)

#### SPEC-R1 — Integrity Validation on Save

**File:** `backend/response-service/src/main/java/com/examplatform/response/service/ResponseSaveService.java`

**Behavior:**
- Before persisting, call delivery-service (gRPC) to verify:
  - Session is `ACTIVE` and not expired.
  - `questionId` is part of the candidate's randomized paper.
  - `selectedOptionId` (if MCQ) is a valid option for that question.
- On failure, return `HTTP 422 Unprocessable Entity` with error code `INTEGRITY_VIOLATION`.

**gRPC Contract:**
```
ValidateResponseRequest {
  sessionId:         UUID
  candidateId:       UUID
  questionId:        UUID
  selectedOptionIds: List<String>
}
ValidateResponseResponse {
  valid:  boolean
  reason: String   // "QUESTION_NOT_IN_PAPER" | "SESSION_EXPIRED" | "INVALID_OPTION"
}
```

**Test Cases (TDD):**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-R1-T1 | Valid question ID, active session | HTTP 200, response saved |
| SPEC-R1-T2 | Question ID not in candidate's paper | HTTP 422, code=INTEGRITY_VIOLATION |
| SPEC-R1-T3 | Session expired (gRPC says SESSION_EXPIRED) | HTTP 422, code=SESSION_EXPIRED |
| SPEC-R1-T4 | Invalid option ID for question | HTTP 422, code=INVALID_OPTION |
| SPEC-R1-T5 | delivery-service timeout | Save with INTEGRITY_UNKNOWN flag + alert audit |

---

#### SPEC-R2 — Anti-Tamper Telemetry Fields

**File:** `backend/response-service/src/main/java/com/examplatform/response/domain/Response.java`

**New fields:**
```java
@Column(name = "client_ip", length = 45)              // IPv6 max length
private String clientIp;

@Column(name = "user_agent", columnDefinition = "text")
private String userAgent;

@Column(name = "focus_loss_count", nullable = false)
private int focusLossCount;                            // Tab switch / focus-loss events

@Column(name = "integrity_checksum", length = 64)     // SHA-256 of (sessionId+questionId+answer+timestamp)
private String integrityChecksum;
```

**`SaveResponseRequest.java` additions:**
```java
@NotNull
private int focusLossCount;

private String integrityChecksum;   // HMAC-SHA256, verified server-side
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-R2-T1 | Request has X-Forwarded-For header | `clientIp` persisted from that header |
| SPEC-R2-T2 | focusLossCount = 3 in request | `focusLossCount = 3` persisted |
| SPEC-R2-T3 | integrityChecksum mismatch | HTTP 422, code=CHECKSUM_MISMATCH |
| SPEC-R2-T4 | integrityChecksum null/blank | Saved with UNCHECKED flag |

---

#### SPEC-R3 — Idempotent Session Submission (Double-Submit Prevention)

**File:** `backend/response-service/src/main/java/com/examplatform/response/service/SessionFinalizationService.java`

**Redis key design:**
```
Key:   submit:lock:{sessionId}
Value: {candidateId}:{submittedAt}
TTL:   30 seconds
NX:    true  (SET if Not eXists)
```

**Behavior:**
- On `/submit`, acquire the Redis lock (SETNX).
- If lock already held → return `HTTP 409 Conflict`, code=`ALREADY_SUBMITTED`.
- Lock released only after all responses are `isFinal=true` and event published.

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-R3-T1 | First submission | HTTP 200, Redis key set |
| SPEC-R3-T2 | Second submission (lock exists) | HTTP 409, ALREADY_SUBMITTED |
| SPEC-R3-T3 | Redis unavailable | Submission proceeds (fail-open) + alert |
| SPEC-R3-T4 | Responses already have isFinal=true | HTTP 409 without Redis lookup |

---

#### SPEC-R4 — Session-Timing Enforcement

**File:** `backend/response-service/src/main/java/com/examplatform/response/service/SessionFinalizationService.java`

**Behavior:**
- Before marking final: call delivery-service to get `sessionEndTime`.
- If `Instant.now() > sessionEndTime + gracePeriod(default=5min)` → reject.
- Grace period configurable via `DynamicConfigService` key: `response.submit.grace.seconds`.

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-R4-T1 | Within session time | Submission accepted |
| SPEC-R4-T2 | Exactly at session end boundary | Accepted |
| SPEC-R4-T3 | 4 minutes after end (within 5min grace) | Accepted |
| SPEC-R4-T4 | 6 minutes after end (past grace) | HTTP 422, SESSION_EXPIRED |

---

### 3.2 Psychometric Evaluation Engine (evaluation-service)

#### SPEC-E1 — Flexible Marking Scheme

**File:** `backend/evaluation-service/src/main/java/com/examplatform/evaluation/service/AutoEvaluationService.java`

**New `MarkingScheme` enum:**
```java
public enum MarkingScheme {
    STANDARD,       // +ve for correct, -ve for wrong (existing)
    ZERO_NEGATIVE,  // +ve for correct, 0 for wrong
    PARTIAL_CREDIT  // proportional for MULTI_MCQ (existing)
}
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-E1-T1 | STANDARD + correct | +marksPerQuestion |
| SPEC-E1-T2 | STANDARD + wrong | -negativeMarks |
| SPEC-E1-T3 | ZERO_NEGATIVE + wrong | 0.0 |
| SPEC-E1-T4 | Any scheme + unattempted | 0.0 |

---

#### SPEC-E2 — EVALUATION_COMPLETED Kafka Event

**File:** `backend/evaluation-service/src/main/java/com/examplatform/evaluation/service/ScoreAggregationService.java`

**Kafka event payload:**
```json
Topic: exam.evaluation.completed
Key:   {sessionId}
{
  "eventType": "EVALUATION_COMPLETED",
  "sessionId": "uuid",
  "candidateId": "uuid",
  "examId": "uuid",
  "totalRawScore": 145.0,
  "sectionScores": { "Physics": 55.0, "Chemistry": 42.0, "Math": 48.0 },
  "questionLevelScores": [
    { "questionId": "uuid", "score": 4.0, "timeSpentMs": 45000 }
  ],
  "tenantId": "default",
  "evaluatedAt": "2026-09-14T07:53:22Z"
}
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-E2-T1 | After successful aggregation | Kafka event published with correct payload |
| SPEC-E2-T2 | Kafka failure | Score saved, error logged, alert on audit topic |
| SPEC-E2-T3 | Event includes timeSpentMs | Sourced from response-service telemetry |

---

### 3.3 Diagnostic Result Analytics (result-service)

#### SPEC-RS1 — Enhanced Result Domain Model

**File:** `backend/result-service/src/main/java/com/examplatform/result/domain/Result.java`

**New columns:**
```java
@Column(name = "accuracy_rate", precision = 5, scale = 2)
private BigDecimal accuracyRate;              // correct / attempted

@Column(name = "category_rank")
private Integer categoryRank;                 // OBC/SC/ST/GEN rank

@Column(name = "sectional_status_json", columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private String sectionalStatusJson;           // { "Physics": "QUALIFIED", "Chemistry": "NOT_QUALIFIED" }

@Column(name = "cognitive_breakdown_json", columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private String cognitiveBreakdownJson;        // { "REMEMBER": 85, "UNDERSTAND": 70, "APPLY": 55, "ANALYZE": 40 }

@Column(name = "topic_breakdown_json", columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private String topicBreakdownJson;            // { "Thermodynamics": {"score": 12, "maxScore": 16} }

@Column(name = "time_analysis_json", columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private String timeAnalysisJson;              // { "avgTimePerQuestion": 65000, "timeOnCorrect": 72000, "timeOnIncorrect": 45000 }

@Column(name = "qr_verification_code", length = 255)
private String qrVerificationCode;            // UUID-based verifiable QR token
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-RS1-T1 | 72 correct, 90 attempted | accuracyRate = 80.00 |
| SPEC-RS1-T2 | 0 attempts | accuracyRate = 0.00 |
| SPEC-RS1-T3 | cognitiveBreakdownJson serialized | Valid JSON parseable to Map<String,Integer> |
| SPEC-RS1-T4 | topicBreakdownJson | Contains all topics from exam blueprint |

---

#### SPEC-RS2 — Real QuestionAnalyticsService

**File:** `backend/result-service/src/main/java/com/examplatform/result/service/QuestionAnalyticsService.java`

**Gap G5 fix:** Replace mock data with real computation.

**Algorithm:**
1. Fetch all `Evaluation` records for the exam from evaluation-service (REST/gRPC).
2. Fetch answer keys + metadata from question-bank-service.
3. Per question compute:
   - `difficultyIndex = correctCount / totalAttempted`
   - `discriminationIndex = top27CorrectRate − bottom27CorrectRate`
   - `responseDistribution = count per option selected`
   - `avgTimeSpentMs = average(timeSpentMs across all candidates)`

**New return fields in `QuestionAnalyticsResult`:**
```java
private double avgTimeSpentMs;
private int totalAttempted;
private int totalCorrect;
private String questionType;
private String solutionExplanation;
private String correctAnswer;
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-RS2-T1 | 100 candidates, 75 correct | difficultyIndex = 0.75 |
| SPEC-RS2-T2 | Sorted cohort groups | discriminationIndex computed correctly |
| SPEC-RS2-T3 | timeSpentMs data available | avgTimeSpentMs = sum / count |
| SPEC-RS2-T4 | Zero attempts on question | difficultyIndex = 0, discriminationIndex = 0 |
| SPEC-RS2-T5 | evaluation-service unavailable | Returns INCOMPLETE flag, cached last result if available |

---

#### SPEC-RS3 — EVALUATION_COMPLETED Consumer

**File:** `[NEW]` `backend/result-service/src/main/java/com/examplatform/result/consumer/EvaluationCompletedConsumer.java`

**Behavior:**
- Consume Kafka topic `exam.evaluation.completed`.
- For each event: call `ResultComputationService.computeCandidateResult(...)`.
- Compute and persist: `accuracyRate`, `timeAnalysis`, `cognitiveBreakdown`, `topicBreakdown`.

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-RS3-T1 | Valid EVALUATION_COMPLETED event | Result record created with all diagnostic fields |
| SPEC-RS3-T2 | Duplicate event (same sessionId) | Idempotent — no duplicate Result |
| SPEC-RS3-T3 | Missing exam blueprint for cognitive mapping | Result saved with partial data + alert |

---

#### SPEC-RS4 — Verifiable Scorecard PDF with QR Code

**File:** `backend/result-service/src/main/java/com/examplatform/result/service/ScorecardPdfService.java`

**Enhancements:**
1. Generate `qrVerificationCode` (UUID token) stored in `Result.qrVerificationCode`.
2. Encode QR payload: `https://nag.gov.in/verify?code={qrVerificationCode}`.
3. Embed QR image in PDF using ZXing.
4. New dependency: `com.google.zxing:core:3.5.3` + `com.google.zxing:javase:3.5.3`.
5. Digital signature: HMAC-SHA256 over `{candidateId}:{examId}:{totalScore}:{qrCode}`.

**New endpoint:**
```
GET /api/v1/results/verify?code={qrCode}
→ 200: { valid, examTitle, candidateId, totalScore, rank, issueDate }
→ 404: code not found
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-RS4-T1 | Scorecard generated | qrVerificationCode stored in Result |
| SPEC-RS4-T2 | Known QR code queried | Returns valid redacted result |
| SPEC-RS4-T3 | Unknown QR code | HTTP 404 |
| SPEC-RS4-T4 | PDF content | QR image embedded bottom-right corner |
| SPEC-RS4-T5 | Digital signature | Matches HMAC-SHA256 derivation |

---

### 3.4 Interactive Post-Exam Review UI (candidate-frontend)

#### SPEC-UI1 — ReviewExam Page

**File:** `[NEW]` `candidate-frontend/src/pages/ReviewExam.tsx`  
**Route:** `/results/:examId/review`

**Wireframe:**
```
┌──────────────────────────────────────────────────────┐
│  [← Back to Results]    JEE Main 2026 — Q. Review   │
├──────────────────────────────────────────────────────┤
│  Filter: [All ▾]  [Correct ▾]  [Incorrect ▾]        │
│  Sort:   [By Subject ▾]  [By Time Spent ▾]          │
├──────────────────────────────────────────────────────┤
│  Question 5 of 90               ◀ Prev  Next ▶       │
│  [Subject: Physics] [Thermodynamics] [Medium] [Apply]│
│                                                      │
│  A gas expands isothermally such that $$PV = nRT$$   │
│                                                      │
│  ○ A. $$W = nRT\ln(V_f/V_i)$$  ← Your answer (red) │
│  ● B. $$W = P\Delta V$$        ← Correct (green)    │
│  ○ C. ...                                            │
│  ○ D. ...                                            │
│                                                      │
│  ⏱ Time spent: 1m 23s   |   Peer accuracy: 42%      │
│  ─────────────────────────────────────────────────── │
│  📖 Solution  [Show ▾]                               │
│  $$W = \int_{V_i}^{V_f} P\,dV = nRT\ln(V_f/V_i)$$ │
└──────────────────────────────────────────────────────┘
```

**Test Cases (Cypress E2E):**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-UI1-T1 | Page loads for 90-question exam | All 90 questions navigable |
| SPEC-UI1-T2 | Filter "Incorrect" | Only wrong answers shown |
| SPEC-UI1-T3 | LaTeX in question content | Renders via MathRenderer component |
| SPEC-UI1-T4 | Candidate's wrong answer | Highlighted red; correct answer green |
| SPEC-UI1-T5 | Time spent per question | Displayed in human-readable format |
| SPEC-UI1-T6 | Peer accuracy | Shown as percentage |
| SPEC-UI1-T7 | Solution section | Expandable/collapsible with LaTeX rendered |

---

#### SPEC-UI2 — Enhanced Results Page Diagnostics

**File:** `candidate-frontend/src/pages/Results.tsx`

**Additions:**

1. **Cognitive Level Breakdown** bar chart from `cognitiveBreakdownJson`:
   ```
   Remember:   ████████░░  80%
   Understand: ██████░░░░  60%
   Apply:      ████░░░░░░  40%
   Analyze:    ███░░░░░░░  30%
   ```

2. **Topic Breakdown Table** from `topicBreakdownJson`:
   ```
   | Topic            | Score | Max | Accuracy |
   |---|---|---|---|
   | Thermodynamics   | 12    | 16  | 75%      |
   | Electrostatics   | 8     | 12  | 67%      |
   ```

3. **Time Analysis** from `timeAnalysisJson`:
   ```
   Avg time/question:  1m 12s
   Time on correct:    1m 32s
   Time on incorrect:  45s
   ```

4. **[Review Answers →]** button linking to `/results/{examId}/review`.

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-UI2-T1 | cognitiveBreakdown in API response | Bar chart renders with correct values |
| SPEC-UI2-T2 | topicBreakdownJson present | Sorted by accuracy ascending |
| SPEC-UI2-T3 | Result status = PUBLISHED | "Review Answers" button visible |
| SPEC-UI2-T4 | timeAnalysisJson present | Avg/correct/incorrect times shown |

---

#### SPEC-UI3 — Review API Endpoint

**File:** `[NEW]` `backend/result-service/src/main/java/com/examplatform/result/controller/ReviewController.java`

```
GET /api/v1/results/{candidateId}/review?examId={examId}
Authorization: Bearer <JWT> (CANDIDATE role, own data only)
```

**Response schema:**
```json
{
  "examId": "uuid",
  "candidateId": "uuid",
  "questions": [
    {
      "questionId": "uuid",
      "questionNumber": 1,
      "content": "A gas expands isothermally...",
      "subject": "Physics",
      "topic": "Thermodynamics",
      "difficulty": "MEDIUM",
      "bloomsLevel": "APPLY",
      "options": [
        { "id": "opt-a", "text": "$$W = nRT\\ln(V_f/V_i)$$", "isCorrect": true }
      ],
      "candidateSelectedOptionIds": ["opt-b"],
      "isCorrect": false,
      "marksAwarded": -0.25,
      "timeSpentMs": 83000,
      "peerAccuracyPct": 42.3,
      "explanation": "The isothermal work formula is $$W = \\int_{V_i}^{V_f} P\\,dV$$..."
    }
  ]
}
```

**Test Cases:**

| ID | Scenario | Expected |
|---|---|---|
| SPEC-UI3-T1 | Valid candidateId + examId | Returns all questions with candidate selections |
| SPEC-UI3-T2 | Each question | Includes LaTeX explanation |
| SPEC-UI3-T3 | peerAccuracyPct | Sourced from QuestionAnalyticsService |
| SPEC-UI3-T4 | Candidate accessing another's review | HTTP 403 |
| SPEC-UI3-T5 | Result not PUBLISHED | HTTP 404 or 403 |

---

## 4. Database Schema Changes

### 4.1 response_service.response — New Columns
```sql
-- Migration: V9__add_response_antitamper_fields.sql
ALTER TABLE response_service.response
    ADD COLUMN client_ip          VARCHAR(45),
    ADD COLUMN user_agent         TEXT,
    ADD COLUMN focus_loss_count   INT NOT NULL DEFAULT 0,
    ADD COLUMN integrity_checksum VARCHAR(64);

CREATE INDEX idx_response_session_is_final
    ON response_service.response(session_id, is_final)
    WHERE is_final = TRUE;
```

### 4.2 result_service.result — New Columns
```sql
-- Migration: V8__add_result_diagnostic_fields.sql
ALTER TABLE result_service.result
    ADD COLUMN accuracy_rate            NUMERIC(5, 2),
    ADD COLUMN category_rank            INT,
    ADD COLUMN sectional_status_json    JSONB,
    ADD COLUMN cognitive_breakdown_json JSONB,
    ADD COLUMN topic_breakdown_json     JSONB,
    ADD COLUMN time_analysis_json       JSONB,
    ADD COLUMN qr_verification_code     VARCHAR(255) UNIQUE;

CREATE UNIQUE INDEX idx_result_qr_code
    ON result_service.result(qr_verification_code)
    WHERE qr_verification_code IS NOT NULL;

CREATE INDEX idx_result_exam_rank
    ON result_service.result(exam_id, overall_rank);
```

---

## 5. Kafka Topic Design

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `exam.response.saved` | response-service | audit-service | Per-response audit event (throttled 60s) |
| `exam.session.events` (SESSION_SUBMITTED) | response-service | evaluation-service | Triggers auto-evaluation pipeline |
| `exam.evaluation.completed` | evaluation-service | result-service | Triggers result computation + diagnostics |
| `exam.audit.events` | all services | audit-service | Tamper/integrity violation alerts |

---

## 6. API Surface Summary

| Method | Path | Service | Change |
|---|---|---|---|
| `POST` | `/api/v1/responses/{sessionId}/save` | response-service | 🔧 Add integrity validation + telemetry |
| `POST` | `/api/v1/responses/{sessionId}/submit` | response-service | 🔧 Add idempotency lock + timing check |
| `GET` | `/api/v1/results/{candidateId}` | result-service | 🔧 Add diagnostic fields to response DTO |
| `GET` | `/api/v1/results/{candidateId}/review` | result-service | 🆕 New — question review endpoint |
| `GET` | `/api/v1/results/verify` | result-service | 🆕 New — QR code verification (public) |
| `GET` | `/api/v1/results/{examId}/analytics` | result-service | 🔧 Replace mock with real data |

---

## 7. Non-Functional Requirements

### 7.1 Performance
- `POST /save` p99 latency: ≤ 200ms (existing SLA, preserve)
- Integrity gRPC call: must complete in ≤ 50ms (circuit breaker at 100ms)
- `GET /review` p95 latency: ≤ 500ms (pre-computed + cached on result publication)
- `QuestionAnalyticsService`: pre-compute on `EVALUATION_COMPLETED`, not on-demand

### 7.2 Security
- All `/responses` endpoints: `CANDIDATE` role, JWT sub must match `candidateId`
- `/verify` QR endpoint: public, rate-limited 60 req/min per IP via API gateway
- Integrity checksum: HMAC-SHA256, shared secret from `DynamicConfigService`
- `clientIp` from `X-Forwarded-For` header (trusted proxy header, not directly from socket)

### 7.3 Scalability
- Redis Sorted Set for percentile computation (O(N log N) vs in-memory O(N²))
- `response` table: monthly range partition on `created_at` (pre-planned)
- `result` table: composite index on `(exam_id, overall_rank)` for leaderboard

---

## 8. Implementation Phases & Story Breakdown

### Phase 1 — Response Integrity & Security [Sprint 1, ~3 days]

| Story ID | Description | File(s) | Est. |
|---|---|---|---|
| S1.1 | Add telemetry fields to `Response` entity + Flyway migration | `Response.java`, `SaveResponseRequest.java`, `V9__*.sql` | 0.5d |
| S1.2 | gRPC delivery-service call for integrity validation | `ResponseSaveService.java`, proto | 1d |
| S1.3 | Redis idempotency lock on `/submit` | `SessionFinalizationService.java` | 0.5d |
| S1.4 | Session timing enforcement | `SessionFinalizationService.java` | 0.5d |
| S1.5 | TDD unit + integration tests for S1.1–S1.4 | `*Test.java`, `*IntegrationTest.java` | 0.5d |

---

### Phase 2 — Evaluation Engine Enhancements [Sprint 1, ~1.5 days]

| Story ID | Description | File(s) | Est. |
|---|---|---|---|
| S2.1 | `MarkingScheme` enum + ZERO_NEGATIVE support | `AutoEvaluationService.java`, `AnswerKey.java` | 0.5d |
| S2.2 | Emit `EVALUATION_COMPLETED` Kafka event | `ScoreAggregationService.java` | 0.5d |
| S2.3 | Include `questionLevelScores` with `timeSpentMs` | `ScoreAggregationService.java` | 0.5d |

---

### Phase 3 — Diagnostic Results (result-service) [Sprint 2, ~5.5 days]

| Story ID | Description | File(s) | Est. |
|---|---|---|---|
| S3.1 | Extend `Result` domain + Flyway migration | `Result.java`, `V8__*.sql` | 0.5d |
| S3.2 | `EvaluationCompletedConsumer` — consume event + compute diagnostics | `EvaluationCompletedConsumer.java` | 1.5d |
| S3.3 | Real `QuestionAnalyticsService` | `QuestionAnalyticsService.java` | 1.5d |
| S3.4 | QR code generation + digital signature in scorecard | `ScorecardPdfService.java`, `build.gradle` | 1d |
| S3.5 | `ReviewController` + review API | `ReviewController.java` | 1d |

---

### Phase 4 — Frontend Review UI [Sprint 2, ~5 days]

| Story ID | Description | File(s) | Est. |
|---|---|---|---|
| S4.1 | `ReviewExam.tsx` — full question review page | `ReviewExam.tsx` | 2d |
| S4.2 | Cognitive breakdown chart in `Results.tsx` | `Results.tsx` | 0.5d |
| S4.3 | Topic breakdown table + time analysis | `Results.tsx` | 0.5d |
| S4.4 | "Review Answers" button + routing | `Results.tsx`, `App.tsx` | 0.5d |
| S4.5 | Cypress E2E tests | `cypress/e2e/review.cy.ts` | 1.5d |

---

## 9. Test Strategy

### 9.1 Unit Tests (JUnit 5 + Mockito + AssertJ)
- `ResponseSaveServiceTest` — extend with SPEC-R1, SPEC-R2 integrity cases
- `SessionFinalizationServiceTest` — SPEC-R3 idempotency, SPEC-R4 timing
- `AutoEvaluationServiceTest` — SPEC-E1 MarkingScheme cases
- `ResultComputationServiceTest` — SPEC-RS1 accuracyRate, cognitive fields
- `QuestionAnalyticsServiceTest` — SPEC-RS2 real computation (delete mock)
- `ScorecardPdfServiceTest` — SPEC-RS4 QR generation + signature

### 9.2 Integration Tests (Testcontainers — PostgreSQL + Redis + Kafka)
- `ResponseControllerIntegrationTest` — `/save` + `/submit` full pipeline
- `EvaluationControllerIntegrationTest` — evaluation pipeline
- `ResultControllerIntegrationTest` — `/review` + `/verify` endpoints

### 9.3 Property-Based Tests (jqwik)
- Integrity checksum validation: ∀ random inputs, checksum mismatch → reject
- Partial marking invariant: score ∈ `[-negativeMarks, maxMarks]` always

### 9.4 E2E Tests (Cypress)
- `review.cy.ts` — full post-exam review flow including LaTeX render
- `results-diagnostics.cy.ts` — cognitive/topic/time UI sections

### 9.5 Performance Tests (k6)
- `POST /save` under 1000 concurrent candidates → p99 ≤ 200ms
- `GET /review` after publication → p95 ≤ 500ms

---

## 10. Acceptance Criteria Traceability

| Issue Acceptance Criterion | Spec ID | Implementation Target |
|---|---|---|
| Scalable, idempotent response collection | SPEC-R3, SPEC-R4 | `SessionFinalizationService` + Redis |
| Integrity validation (late/tampered/invalid) | SPEC-R1, SPEC-R2 | gRPC delivery validation + checksum |
| Automated grading per blueprint rules | SPEC-E1 | `MarkingScheme` + `AutoEvaluationService` |
| Detailed diagnostic result breakdown | SPEC-RS1, SPEC-RS2, SPEC-RS3 | `Result` entity + `EvaluationCompletedConsumer` |
| Interactive post-exam review with solutions | SPEC-UI1, SPEC-UI2, SPEC-UI3 | `ReviewExam.tsx` + `ReviewController` |

---

## 11. Open Questions

> [!IMPORTANT]
> **Q1 — Checksum Strategy:** Should the integrity checksum (HMAC-SHA256) be verified server-side by re-deriving it from the request fields, or just stored as received? If server-side verification is required, the HMAC shared secret rotation strategy needs to be defined.

> [!IMPORTANT]
> **Q2 — Bloom's Level Source:** What is the source of `bloomsLevel` mapping per question? Is it already a column in `question-bank-service`? If not, a schema migration for that service is required before SPEC-RS2 and SPEC-UI1 can be completed.

> [!IMPORTANT]
> **Q3 — Category Rank Basis:** Is `categoryRank` derived from the candidate's self-declared category on registration (OBC/SC/ST/GEN), or from exam-application-specific declarations? This affects what data the result-service needs to fetch.

> [!NOTE]
> **Q4 — QuestionAnalyticsService gRPC:** Confirm the gRPC interface for fetching evaluation records from `evaluation-service`. Is `EvaluationController` already gRPC-enabled, or does it need a gRPC facade added?

> [!NOTE]
> **Q5 — DigiLocker + QR:** Should the DigiLocker scorecard push (in `ResultPublicationService`) include the new QR verification URL in the document metadata? This may require changes to the DigiLocker API integration contract.

---

*Generated from analysis of branch `feat/issue-101-response-collection-evaluation-results` on 2026-09-14.*  
*Total estimated effort: ~15 developer-days across 2 sprints.*
