# Implementation Plan: Open Source Government Examination Platform

## Overview

This plan implements the Open Source Government Examination Platform as a Java 21 / Spring Boot 3.x microservices monorepo.
Tasks are organized by bounded context and follow the architecture described in the design document.
All services are stateless Spring Boot applications; persistent state lives in PostgreSQL 16, session/cache state in Redis Cluster, and async domain events flow over Apache Kafka.

## Tasks

- [x] 1. Repository scaffold and shared infrastructure
  - [x] 1.1 Initialize monorepo structure with top-level directories `/frontend`, `/backend`, `/infrastructure`, `/docs` and root files `LICENSE` (Apache 2.0), `CONTRIBUTING.md`, `SECURITY.md`
    - Create Maven/Gradle multi-module root `pom.xml` / `settings.gradle` declaring all backend service modules
    - Add `.github/workflows/ci.yml` skeleton with Build, Unit Test, Integration Test, SAST, DAST, Container Build, Deploy stages
    - _Requirements: 27.1, 27.2, 27.3, 24.1_
  - [x] 1.2 Create `backend/shared-lib` module with common classes: `ApiResponse<T>` envelope, `ProblemDetail` RFC 7807 error builder, `AuditEventType` enum, `LifecycleState` enums, `TenantContext` thread-local, and `BaseEntity` JPA mapped superclass
    - _Requirements: 23.1, 15.1, 3.1_
  - [x] 1.3 Write Docker Compose configuration (`infrastructure/docker-compose/docker-compose.yml`) starting PostgreSQL 16, Kafka, Redis Cluster, Keycloak, HashiCorp Vault (dev mode), Prometheus, Grafana, Jaeger, and all backend services
    - _Requirements: 24.4_


- [x] 2. Identity Service — authentication, MFA, RBAC/ABAC, rate limiting
  - [x] 2.1 Scaffold `backend/identity-service` Spring Boot project: configure Spring Security OAuth2 Resource Server, Keycloak adapter, Spring Data JPA with `identity_service` schema, Redis for session state, Actuator, and OpenTelemetry Java agent
    - _Requirements: 2.1, 3.1, 16.3, 23.4_
  - [x] 2.2 Implement candidate registration endpoint `POST /api/v1/identity/register`: validate identity document type (Aadhaar/PAN/Passport/VoterID/DL), persist pending account, enforce duplicate-identity check (SHA-256 hash comparison), return acknowledgement within 2 seconds
    - _Requirements: 1.1, 1.5_
  - [x] 2.3 Implement OTP verification `POST /api/v1/identity/otp/verify`: activate pending account, issue JWT access + refresh tokens via Keycloak token endpoint
    - _Requirements: 1.2_
  - [x] 2.4 Implement password+MFA authentication `POST /api/v1/identity/auth/token`: validate credentials, enforce MFA OTP/hardware-token step when MFA is enabled, enforce device binding (device fingerprint claim in JWT), enforce single concurrent active session per candidate during shift
    - _Requirements: 2.1, 2.2, 2.5, 2.7_
  - [x] 2.5 Implement WebAuthn / FIDO2 authentication `POST /api/v1/identity/auth/webauthn` using Spring Security WebAuthn support; verify authenticator assertion and issue JWT
    - _Requirements: 2.3_
  - [x] 2.6 Implement account lockout: after 5 consecutive failed authentication attempts within 10 minutes, lock account and trigger notification event on Kafka `exam.notifications.outbound`; implement step-up authentication on risk signal (new device/geo/time)
    - _Requirements: 2.4, 2.6_
  - [x] 2.7 Implement role assignment/revocation `POST /api/v1/identity/roles/{userId}` (Super_Admin only); enforce all 10 named roles (Super_Admin, Security_Admin, Question_Author, Reviewer, Approver, Exam_Controller, Translator, Evaluator, Auditor, Candidate); enforce least-privilege RBAC on all API endpoints; return HTTP 403 on unauthorized access
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [x] 2.8 Implement rate limiting on `/api/v1/identity/auth/*`: Redis token-bucket limiting 10 auth attempts per IP per minute; return HTTP 429 on excess; implement idle-session timeout invalidation
    - _Requirements: 17.1, 2.8_
  - [ ]* 2.9 Write property test for authentication rate limiting
    - **Property 13: Authentication Rate Limiting**
    - **Validates: Requirements 17.1**
    - Use `@Property(tries = 1000)` with jqwik generator `authRequestBursts()` that produces IP addresses and request sequences; assert requests 1–10 receive non-429, requests 11–N receive HTTP 429 within any 60-second window
  - [x] 2.10 Implement HSM/Vault integration: configure Spring Vault `TransitTemplate`; expose `VaultCryptoService` with `encrypt`, `decrypt`, `sign`, `verify`, `rotateKey`, `revokeKey`; ensure private keys never leave Vault boundary; implement 60-second revocation on Security_Admin trigger
    - _Requirements: 16.3, 16.4, 16.5_
  - [x] 2.11 Publish audit events to Kafka `exam.audit.events` for: login, logout, role-change, denied-access, account-lock, key-revocation; include actor, role, resource, IP, device fingerprint, timestamp
    - _Requirements: 2.9, 3.3, 3.5, 16.5_


- [x] 3. Candidate Service — PII management, DigiLocker, face verification
  - [x] 3.1 Scaffold `backend/candidate-service` Spring Boot project: configure JPA with `candidate_service` schema, `VaultCryptoService` dependency, AES-256 column encryption via JPA `AttributeConverter` for all PII fields (name, DOB, gender, nationality, category, mobile, email, address, reservationCategory, identityDocNumber)
    - _Requirements: 1.6, 16.1, 25.1_
  - [x] 3.2 Implement candidate profile CRUD: store per-candidate DEK reference in `encryption_key_id`, store SHA-256 hash of mobile for uniqueness check, store SHA-256 + HMAC of identity document for duplicate detection; implement DPDP erasure endpoint that zeroes PII columns and deletes DEK reference
    - _Requirements: 1.6, 25.2_
  - [x] 3.3 Implement DigiLocker verification: call DigiLocker API with OAuth2 token, validate returned document data, update `digiLockerVerified` status to `VERIFIED` or `FAILED`
    - _Requirements: 1.3_
  - [x] 3.4 Implement face verification: compare submitted photograph embedding against identity document photograph; reject and set `faceVerificationStatus=FAILED` when similarity score is below configured threshold
    - _Requirements: 1.4_
  - [x] 3.5 Implement consent recording: Angular form presents plain-language consent notice before biometric data collection; persist `consentRecorded=true` and `consentTimestamp` on explicit acceptance
    - _Requirements: 25.3_
  - [x] 3.6 Publish audit event to Kafka `exam.audit.events` on candidate profile creation
    - _Requirements: 1.7_
  - [ ]* 3.7 Write property test for candidate PII encryption at rest
    - **Property 12: Candidate PII Encryption at Rest**
    - **Validates: Requirements 1.6, 16.1**
    - Use jqwik generator `candidatePIIRecords()` producing arbitrary PII strings; assert `storedBytes ≠ UTF8(piiValue)` and `decrypt(candidateDEK, storedBytes) == piiValue`


- [x] 4. Question Bank Service — CRUD, versioning, lifecycle FSM, similarity, exposure tracking
  - [x] 4.1 Scaffold `backend/question-bank-service` Spring Boot project: configure JPA with `question_service` schema including Hash-partitioned `question` table (16 partitions), pgvector extension, OpenSearch client, `VaultCryptoService` for per-question AES-256 encryption
    - _Requirements: 4.1, 4.5, 19.6_
  - [x] 4.2 Implement question creation `POST /api/v1/questions`: validate required metadata (subject, topic, subtopic, chapter, difficulty, cognitiveLevel, questionType); enforce supported question types (Single_MCQ, Multi_MCQ, Numerical, Descriptive, Matrix_Match, Assertion_Reason, Coding, Case_Study); persist in Draft state; encrypt content fields with per-question DEK; accept rich content types (HTML5, SVG, PNG, JPEG, WEBP, Audio, Video, LaTeX, MathML)
    - _Requirements: 4.1, 4.2, 4.3, 4.5_
  - [x] 4.3 Implement question versioning: on every update to question content/metadata, create a `QuestionVersion` record with `authorId`, `changedAt`, JSON diff of modified fields, and encrypted full snapshot; expose `GET /api/v1/questions/{id}/versions`
    - _Requirements: 4.4_
  - [x] 4.4 Implement question lifecycle FSM: enforce valid transitions (DRAFT→REVIEW, REVIEW→APPROVED, REVIEW→DRAFT, APPROVED→PUBLISHED, PUBLISHED→ARCHIVED) via `POST /api/v1/questions/{id}/transition`; reject any transition not in `VALID_TRANSITIONS` with HTTP 422; enforce four-eyes principle (reviewer ≠ approver)
    - _Requirements: 4.6, 5.5_
  - [ ]* 4.5 Write property test for question lifecycle state machine
    - **Property 8: Question Lifecycle State Machine**
    - **Validates: Requirements 4.6**
    - Use jqwik generator `lifecycleStatePairs()` producing all (currentState, targetState) combinations; assert `transition()` succeeds iff pair is in `VALID_TRANSITIONS`
  - [x] 4.6 Implement review/approval workflow: on transition to REVIEW, assign to available Reviewer by subject specialization and publish Kafka event to `exam.question.lifecycle`; on Reviewer approval → APPROVED + notify Author; on return → DRAFT with comments + notify Author; on Approver approval → PUBLISHED; notify via `exam.notifications.outbound`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6_
  - [x] 4.7 Implement similarity detection: on question save, compute embedding vector (1536-dim), store in `embedding_vector` (pgvector); before creating Draft, cosine-similarity search against all published questions; reject with HTTP 422 + `similarQuestionId` if any cosine similarity > 0.80 threshold
    - _Requirements: 4.7_
  - [ ]* 4.8 Write property test for question similarity rejection
    - **Property 14: Question Similarity Rejection**
    - **Validates: Requirements 4.7**
    - Use jqwik generator `questionEmbeddingPairs()` producing (submittedEmbedding, publishedEmbedding, cosineSim) tuples; assert `submit(q) = REJECTED(similarId)` when `cosineSim > 0.80`, `ACCEPTED` otherwise
  - [x] 4.9 Implement exposure tracking: increment `usageCount`, update `lastUsedAt`, append to `usedInExamIds`/`usedInShiftIds` when a question is selected into a paper; implement reuse policy enforcement (NEVER / 1_Year / 2_Years / Custom)
    - _Requirements: 4.8, 4.9_
  - [x] 4.10 Implement full-text search `GET /api/v1/questions/search` via OpenSearch index (100M question capacity); return results within 2 seconds at p95; expose `GET /api/v1/questions/{id}/analytics` with difficulty index, discrimination index, usage count
    - _Requirements: 19.3, 26.5_
  - [x] 4.11 Publish audit events to Kafka `exam.audit.events` on question creation, modification, and each state transition
    - _Requirements: 4.10, 5.6, 15.1_


- [x] 5. Translation Service — multilingual content workflow
  - [x] 5.1 Scaffold `backend/translation-service` Spring Boot project: configure JPA with `translation_service` schema; configure per-translation AES-256 column encryption via `VaultCryptoService`; enforce UTF-8 storage
    - _Requirements: 6.3, 6.6_
  - [x] 5.2 Implement translation workflow: on request for a Published question, initiate Author→Translator→Reviewer→Approver pipeline; store translation in DRAFT status linked to source question and language code (ISO 639-1/BCP47); support all 22 Eighth Schedule languages
    - _Requirements: 6.1, 6.2, 6.3_
  - [x] 5.3 Implement translation review: on Reviewer approval → mark APPROVED, make available for paper generation; on rejection → attach reviewer comments, notify Translator; detect stale translations when source question is modified after approval (publish `STALE` status + Kafka event)
    - _Requirements: 6.4, 6.5, 6.7_

- [x] 6. Examination Service — exam config, sections, marking schemes, navigation, scheduling
  - [x] 6.1 Scaffold `backend/examination-service` Spring Boot project: configure JPA with `examination_service` schema; implement `Examination`, `Section`, and `SubjectTopicRule` JPA entities with JSONB sections column
    - _Requirements: 7.1, 7.2_
  - [x] 6.2 Implement exam creation/update API: persist exam with name, duration, total marks, negative marking flag/value, navigation policy (Sequential/Flexible/Restricted), calculator policy (None/Basic/Scientific), review-flag policy, and section list; validate that Σ(marksPerQuestion × questionCount) over all sections == totalMarks; reject with descriptive error on mismatch
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  - [ ]* 6.3 Write property test for examination section marks validation
    - **Property 11: Examination Section Marks Validation**
    - **Validates: Requirements 7.6**
    - Use jqwik generator `examSectionConfigs()` producing arbitrary section lists; assert `isValid(exam) ⟺ Σ(marksPerQuestion × questionCount) == exam.totalMarks`
  - [x] 6.4 Implement disability time extension: before session starts, apply `extraTimeMinutes` from candidate profile to session duration
    - _Requirements: 22.6_
  - [x] 6.5 Publish audit event to Kafka `exam.audit.events` on exam publication
    - _Requirements: 7.7_
  - [ ] 6.6 Extend `examination_service` DB schema with scheduling tables: `examination_schedule`, `exam_shift`, `examination_centre`, `shift_seat_allocation`; add `code`, `conducting_authority`, `category`, `examination_type`, `academic_year`, `examination_mode` columns to `examination` table; run Flyway migration V2
    - _Requirements: 7b.1, 7b.2, 7b.5, 7b.6_
  - [ ] 6.7 Implement examination schedule CRUD: `POST /api/v1/examinations/{id}/schedules` persists schedule with name, version=1, notification number, exam date, reserve date, time zone, status=DRAFT; `GET` list and single-record endpoints; `PUT` amend endpoint (requires published status + mandatory change reason + increments version); store complete version history via `previous_version_id` chain
    - _Requirements: 7b.1, 7b.4, 7b.8, 7b.9_
  - [ ] 6.8 Implement shift management: `POST/PUT /api/v1/examinations/{id}/schedules/{sid}/shifts`; enforce all five timing ordering constraints (reportingTime < gateClosingTime < loginStartTime < examStartTime < examEndTime) and duration equality; reject overlapping shifts on the same date; return descriptive RFC 7807 error identifying the violated constraint
    - _Requirements: 7b.2, 7b.3_
  - [ ]* 6.9 Write property test for examination shift timing invariants
    - **Property 15: Examination Shift Timing Invariants**
    - **Validates: Requirements 7b.3**
    - Use jqwik generator `shiftTimingConfigs()` producing arbitrary (reportingTime, gateClosingTime, loginStartTime, examStartTime, examEndTime, durationMinutes) tuples; assert service accepts iff all ordering constraints hold and durationMinutes == examEndTime − examStartTime
  - [ ] 6.10 Implement examination centre management: `POST/GET /api/v1/examinations/centres`; persist region, state, district, city, centre name, building, floor, laboratory, capacity, active flag; enforce no overlapping centre-shift allocation across different examinations on the same date
    - _Requirements: 7b.5, 7b.11_
  - [ ] 6.11 Implement seat allocation: `POST /api/v1/examinations/{id}/schedules/{sid}/shifts/{shiftId}/allocations`; persist totalSeats, availableSeats, reservedSeats, pwdSeats, emergencyBufferSeats, femaleReservedSeats, specialCategorySeats per centre-shift; reject allocations that would cause availableSeats < 0; validate reserve date conflicts across tenant schedules
    - _Requirements: 7b.6, 7b.10_
  - [ ] 6.12 Implement schedule approval workflow: enforce state machine Draft → Scheduler_Review → Controller_Approved → Security_Review → Chairman_Approved → Published via `PUT /api/v1/examinations/{id}/schedules/{sid}/transition`; require authenticated actor in the appropriate role at each step; record approver identifier and timestamp; on reach Published, publish candidate notification event to Kafka `exam.notifications.outbound`
    - _Requirements: 7b.7, 7b.12_
  - [ ] 6.13 Publish audit events to Kafka `exam.audit.events` on every scheduling action (create, approve, amend, publish, cancel); include schedule identifier, version number, actor identifier, action type, previous value, new value, and timestamp
    - _Requirements: 7b.13_


- [x] 7. Paper Generator — blueprint-driven assembly, encryption, serialization
  - [x] 7.1 Scaffold `backend/paper-generator` Spring Boot project: configure JPA with `paper_generator` schema; configure `VaultCryptoService` for per-shift AES-256 paper encryption; configure async Kafka consumer for paper generation request jobs
    - _Requirements: 8.7_
  - [x] 7.2 Implement blueprint-driven paper assembly: `POST /api/v1/papers/generate` submits async job; worker selects questions from Question Bank satisfying subject/topic/difficulty/cognitive ratios; enforces all active reuse policies; attaches difficulty score, topic distribution report, and similarity report
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - [ ]* 7.3 Write property test for blueprint constraint satisfaction
    - **Property 1: Blueprint Constraint Satisfaction**
    - **Validates: Requirements 8.1, 8.2**
    - Use jqwik generator `validBlueprints()` + `questionBankMock()` producing arbitrary valid blueprints; assert every generated paper's actual ratio distribution matches blueprint within integer rounding tolerance
  - [x] 7.4 Implement shift comparability enforcement: for any pair of shift papers in the same exam, assert `|difficultyScore(pi) - difficultyScore(pj)| / totalMarks ≤ 0.02`; return error and block paper approval if violated
    - _Requirements: 8.9_
  - [ ]* 7.5 Write property test for shift paper statistical comparability
    - **Property 2: Shift Paper Statistical Comparability**
    - **Validates: Requirements 8.9**
    - Use jqwik generator producing pairs of shift paper difficulty scores and total marks; assert `|diff| / totalMarks ≤ 0.02` for all generated pairs within the same exam
  - [x] 7.6 Implement gap report: when blueprint cannot be satisfied due to insufficient questions, return RFC 7807 error with gap details per subject-topic-difficulty combination; do NOT generate a partial paper
    - _Requirements: 8.5_
  - [x] 7.7 Implement paper JSON serialization/deserialization: `PaperSerializer.format(Paper) → String` and `PaperSerializer.parse(String) → Paper`; store only question identifiers (not content) in paper definition; implement Paper Schema versioning field; implement `POST /api/v1/papers/validate` schema validation endpoint
    - _Requirements: 8.7, 28.1, 28.2, 28.3, 28.5_
  - [ ]* 7.8 Write property test for paper serialization round-trip
    - **Property 3: Paper Serialization Round-Trip**
    - **Validates: Requirements 28.2, 28.3, 28.4**
    - Use jqwik `@Property(tries=1000)` with generator `validPapers()` producing arbitrary Paper objects; assert `parse(format(paper)) == paper` and `format(parse(json)) ≡ json`
  - [ ]* 7.9 Write property test for paper schema validation rejecting invalid documents
    - **Property 4: Paper Schema Validation Rejects Invalid Documents**
    - **Validates: Requirements 28.5**
    - Use jqwik generator `invalidPaperDocuments()` producing Papers with at least one invalid field (bad enum, negative marks, missing required field); assert `validate(doc).error.field ∈ fields(doc)` and error value matches violating value
  - [x] 7.10 Implement paper approval and HSM encryption: `POST /api/v1/papers/{paperId}/approve` transitions paper to APPROVED, then ENCRYPTED; encrypt paper package with shift-specific AES-256 key via `VaultCryptoService`; store only object-store reference (not content inline); complete within 5 minutes of request
    - _Requirements: 8.6, 8.7_
  - [x] 7.11 Publish audit events to Kafka `exam.audit.events` on paper generation and paper approval
    - _Requirements: 8.8_

- [x] 8. Checkpoint — core domain services
  - Ensure all unit and property tests pass for services 2–7 before proceeding. Ask the user if questions arise.


- [x] 9. Delivery Service — session start, question serving, navigation, proctoring, offline
  - [x] 9.1 Scaffold `backend/delivery-service` Spring Boot project: configure JPA with `delivery_service` schema; configure Redis for hot exam session state; configure `VaultCryptoService` for shift-key decryption; configure HPA metrics `active_exam_sessions` (target 5,000/pod)
    - _Requirements: 9.1, 19.2_
  - [x] 9.2 Implement session start: authenticate candidate JWT, look up shift assignment, decrypt shift paper package using HSM-managed shift key (in-memory only), serve first question within 500ms; enforce single concurrent session (Req 2.7)
    - _Requirements: 9.1, 9.3_
  - [x] 9.3 Implement navigation policy enforcement: apply Sequential/Flexible/Restricted rules from exam configuration on every navigation request; reject policy-violating navigation with HTTP 422; support rendering modes One_Question, Section_Mode, Batch_Mode
    - _Requirements: 9.2, 9.5_
  - [x] 9.4 Implement session timer: schedule session expiry event at `scheduledEndAt` + disability extension; serve question with language selection from approved translation variants; enforce full-screen lock mode signal via API (Angular enforces locally)
    - _Requirements: 9.3, 9.6, 9.8, 22.6_
  - [x] 9.5 Implement proctoring capture: record webcam snapshots at configurable interval (min 30s) and screen activity recordings; publish to Kafka `exam.proctoring.alerts`; track `fullScreenExitCount` and flag session after 3 exits; enforce configurable retention window; check consent before storing biometric data
    - _Requirements: 11.1, 11.2, 11.6, 11.7_
  - [x] 9.6 Implement AI proctoring analysis event handling: consume proctoring frames; publish `exam.audit.events` with event type `no-face-detected`, `multiple-faces-detected`, or `prohibited-object-detected`
    - _Requirements: 11.3, 11.4, 11.5_
  - [x] 9.7 Implement offline delivery: pre-load and locally decrypt exam package using center-specific time-limited key; serve questions offline; reconcile on reconnect
    - _Requirements: 9.7_
  - [x] 9.8 Publish session start and session submission events to Kafka `exam.session.events`; publish security/proctoring alerts to `exam.audit.events`
    - _Requirements: 15.1_


- [x] 10. Response Service — persistence, auto-save, revision history, offline sync
  - [x] 10.1 Scaffold `backend/response-service` Spring Boot project: configure JPA with `response_service` schema including Range-partitioned `response` table (monthly partitions), composite index on `(session_id, question_id, revision_sequence DESC)`, and index on `candidate_id WHERE is_final=TRUE`; configure Redis for hot session state; configure HPA metric `response_save_rate` (target 50,000 saves/min/pod)
    - _Requirements: 10.1, 19.4, 19.6_
  - [x] 10.2 Implement response save `POST /api/v1/responses/{sessionId}/save`: persist `Response` record with questionId, selectedOptionIds or enteredValue, timestamp, cumulativeTimeSpentMs, revisionSequence, saveSource within 200ms at p99; enforce Kafka `acks=all` before ACK
    - _Requirements: 10.1, 20.3_
  - [ ]* 10.3 Write property test for response persistence round-trip
    - **Property 5: Response Persistence Round-Trip**
    - **Validates: Requirements 10.1**
    - Use jqwik generator `validResponses()` producing arbitrary Response objects across all responseTypes including Unicode content; assert `readResponse(saveResponse(r).id) == r` (all fields bitwise-identical)
  - [x] 10.4 Implement auto-save pipeline: server-side heartbeat triggers auto-save every 30 seconds per active session; also trigger on every navigation event; consume from Kafka `exam.session.events` for navigation signals
    - _Requirements: 10.2, 10.3_
  - [x] 10.5 Implement offline buffer reconciliation `POST /api/v1/responses/{sessionId}/bulk-save`: accept ordered list of buffered responses; reconcile with server-side state using `revisionSequence`; guarantee zero response data loss
    - _Requirements: 10.4_
  - [x] 10.6 Implement revision history: store every response update as new row with monotonically increasing `revisionSequence`; expose full history via `GET /api/v1/responses/{sessionId}/responses` (Evaluator/Auditor role)
    - _Requirements: 10.5_
  - [ ]* 10.7 Write property test for response revision history preservation
    - **Property 6: Response Revision History Preservation**
    - **Validates: Requirements 10.5**
    - Use jqwik generator `responseUpdateSequences()` producing sequences of N≥2 updates to the same (sessionId, questionId); assert history returns exactly N records with `revisionSequence` 1..N and each record's value matches corresponding submitted update
  - [x] 10.8 Implement session finalization `POST /api/v1/responses/{sessionId}/submit`: set `isFinal=true` on all responses for session, lock response set against further modification; trigger Kafka event on `exam.session.events`
    - _Requirements: 10.6_
  - [x] 10.9 Publish sampled audit events to Kafka `exam.audit.events` on response save (max once per candidate per 60 seconds)
    - _Requirements: 10.7_


- [x] 11. Evaluation Service — auto-evaluation, partial marking, dual-evaluator workflow
  - [x] 11.1 Scaffold `backend/evaluation-service` Spring Boot project: configure JPA with `evaluation_service` schema; consume `exam.session.events` (session-submitted) from Kafka to trigger auto-evaluation pipeline
    - _Requirements: 12.1_
  - [x] 11.2 Implement auto-evaluation: for finalized sessions, evaluate all Single_MCQ, Multi_MCQ, and Numerical responses against answer key; apply positive marks, configurable negative marks for wrong answers, zero for unattempted; store `Evaluation` records with `evaluationType=AUTO`
    - _Requirements: 12.1, 12.2_
  - [x] 11.3 Implement partial marking for Multiple_Correct_MCQ: award `(|selection ∩ answerKey| / |answerKey|) × marksPerQuestion` when `selection ⊆ answerKey`; award zero marks when selection contains any incorrect option
    - _Requirements: 12.3_
  - [ ]* 11.4 Write property test for partial marking arithmetic correctness
    - **Property 9: Partial Marking Arithmetic Correctness**
    - **Validates: Requirements 12.2, 12.3**
    - Use jqwik generator `multiCorrectMCQScenarios()` producing arbitrary (selection, answerKey, marksPerQuestion) tuples; assert score formula holds for all subset/non-subset cases
  - [x] 11.5 Implement manual evaluation workflow: notify Evaluators via Kafka `exam.notifications.outbound` after auto-eval completes; record Evaluator score with evaluator ID, timestamp, comments; implement dual-evaluator routing and flag responses where score divergence exceeds tolerance for arbitration
    - _Requirements: 12.4, 12.5, 12.6_
  - [x] 11.6 Compute and store candidate total raw score and section-wise scores after all evaluations are complete; publish event to Kafka `exam.evaluation.events`
    - _Requirements: 12.7_
  - [x] 11.7 Publish audit events to Kafka `exam.audit.events` on each evaluation record creation
    - _Requirements: 12.8_

- [x] 12. Result Service — score aggregation, normalization, PDF scorecard, DigiLocker
  - [x] 12.1 Scaffold `backend/result-service` Spring Boot project: configure JPA with `result_service` schema; integrate iText/PDFBox for PDF generation; configure DigiLocker OAuth2 client
    - _Requirements: 13.4, 13.8_
  - [x] 12.2 Implement result computation: for each candidate, compute totalScore, section-wise scores, overallRank, overallPercentile; apply shift normalization formula when configured; store `Result` record
    - _Requirements: 13.1, 13.2_
  - [ ]* 12.3 Write property test for result score decomposition invariant
    - **Property 10: Result Score Decomposition Invariant**
    - **Validates: Requirements 13.1**
    - Use jqwik generator `candidateScoreScenarios()` producing arbitrary candidate sets with section scores; assert `totalScore == Σ sectionScores` and `rank(A) < rank(B) ⟺ totalScore(A) > totalScore(B)` (excluding ties)
  - [x] 12.4 Implement PDF scorecard generation with password derived from candidate's registered date of birth + candidate identifier; include subject-wise and topic-wise performance breakdown; store PDF reference in object store
    - _Requirements: 13.3, 13.4_
  - [x] 12.5 Implement result publication: expose `GET` scorecard endpoint via Angular portal; expose versioned REST API `GET /api/v1/results/{candidateId}` with OAuth2 access token for third-party integrators; push scorecard to DigiLocker when integration is enabled; publish `exam.notifications.outbound` event for result notification
    - _Requirements: 13.3, 13.5, 13.6, 13.8_
  - [x] 12.6 Compute per-question analytics on exam finalization: difficulty index, discrimination index, response distribution across options; expose via analytics dashboard API
    - _Requirements: 26.1, 26.5_
  - [x] 12.7 Publish audit event to Kafka `exam.audit.events` on result publication
    - _Requirements: 13.7_


- [x] 13. Audit Service — immutable append-only event log, tamper evidence, query API
  - [x] 13.1 Scaffold `backend/audit-service` Spring Boot project: configure JPA with `audit_service` schema; Range-partition `audit_event` table by `occurred_at` (quarterly, 28 partitions for 7-year retention); enforce `REVOKE UPDATE, DELETE ON audit_event FROM audit_writer_role` at DB level; configure Kafka consumer for `exam.audit.events`
    - _Requirements: 15.1, 15.5, 15.6, 19.6_
  - [x] 13.2 Implement audit event ingestion: on Kafka consumption, compute `payloadHash = SHA-256(eventPayload)`, sign with HSM ECDSA P-256 key via `VaultCryptoService`, persist `AuditEvent` with `hsmSignature` and `signingKeyId`; reject any UPDATE/DELETE request on existing records with HTTP 403 and write a new tamper-attempt audit event
    - _Requirements: 15.2, 15.4_
  - [ ]* 13.3 Write property test for audit event tamper detection
    - **Property 7: Audit Event Tamper Detection**
    - **Validates: Requirements 15.2**
    - Use jqwik generator `randomAuditEvents()` + `byteModifications()` producing unmodified and byte-modified payloads; assert `verify(signingKeyId, SHA256(payload), hsmSignature) = true` for originals, `false` for modified variants
  - [x] 13.4 Implement read-only query API `GET /api/v1/audit/events`: filter by userId, examId, actionType, time range; accessible to Auditor role only; return paginated results
    - _Requirements: 15.3_
  - [x] 13.5 Implement local WAL buffer for audit events when Kafka is unavailable; replay on Kafka reconnection to ensure exam operations continue without blocking on audit writes
    - _Requirements: design error-handling table_

- [x] 14. Notification Service — email/push delivery, retry, in-app notifications
  - [x] 14.1 Scaffold `backend/notification-service` Spring Boot project: configure Kafka consumer for `exam.notifications.outbound`; configure email client (SMTP/SendGrid); implement in-app notification entity in `notification_service` schema
    - _Requirements: 14.1_
  - [x] 14.2 Implement notification delivery: send email within 60 seconds of triggering event; retry up to 3 times on failure; mark as UNDELIVERED and log on third failure; ensure no PII or question content in message bodies — identifiers and action links only
    - _Requirements: 14.1, 14.2, 14.4_
  - [x] 14.3 Implement in-app notifications API: persist in-app events in notification store; serve to authenticated users via `GET /api/v1/notifications` filtered by role and userId; publish to Angular via SSE or WebSocket
    - _Requirements: 14.3_


- [x] 15. Admin Service and Analytics Service
  - [x] 15.1 Scaffold `backend/admin-service` Spring Boot project: implement Super_Admin console APIs — create/update/deactivate/list users for all roles; multi-tenancy isolation by `tenantId`; configuration API (`/api/v1/admin/config`) for session timeout, rate limits, auto-save interval, paper-gen concurrency — accessible to Super_Admin and Security_Admin only
    - _Requirements: 29.1, 29.2, 29.3, 29.4_
  - [x] 15.2 Implement deactivation: immediately invalidate all active sessions via Redis; prevent new authentication via Keycloak; publish audit event on config-parameter change (paramName, oldValue, newValue, actorId, timestamp)
    - _Requirements: 29.3, 29.5_
  - [x] 15.3 Scaffold `backend/analytics-service` Spring Boot project: consume finalized result data; implement analytics dashboard API `GET /api/v1/analytics/exams/{id}` — total registered/appeared candidates, score distribution histogram, section-wise averages, top/bottom 10th percentile thresholds; implement CSV and PDF export
    - _Requirements: 26.2, 26.3, 26.4_

- [x] 16. API Gateway and cross-cutting concerns
  - [x] 16.1 Configure Spring Cloud Gateway: OAuth2 token validation via Keycloak introspection before routing; Redis token-bucket rate limiting (1,000 req/min standard, configurable for partners); WAF/ModSecurity integration; request sanitization (strip dangerous headers, validate Content-Type); TLS 1.3 termination at ingress; common response envelope middleware; `X-Request-Id` / `X-Tenant-Id` header propagation
    - _Requirements: 17.2, 17.5, 23.3, 23.5, 23.6_
  - [x] 16.2 Configure Spring Cloud Gateway DDoS mitigation: alert and absorb when inbound request rate exceeds 10,000 req/s from single origin; publish security alert to Kafka `exam.audit.events` and `exam.notifications.outbound` within 60 seconds
    - _Requirements: 17.3, 17.7_
  - [x] 16.3 Implement Zero Trust inter-service authentication: all service-to-service calls authenticated with short-lived service-account JWTs signed by Keycloak; verify via JWKS endpoint; no shared secrets between services; configure Istio mTLS or Spring Security mTLS as fallback
    - _Requirements: 17.6, 2.5_
  - [x] 16.4 Add Resilience4j circuit breakers to all synchronous inter-service calls: OPEN after 5 failures in 10s, HALF_OPEN probe, CLOSED on recovery; configure Delivery Service to serve pre-cached questions from Redis when Question Bank is unavailable
    - _Requirements: design error-handling_


- [x] 17. Angular SPA — candidate-facing UI, accessibility, in-app notifications
  - [x] 17.1 Scaffold `frontend/` Angular 17+ project: configure Angular Material, WCAG 2.2 AA baseline, Angular Router with role-based guards, HTTP interceptor for JWT + `X-Request-Id` + `Accept-Language` headers, axe-core integration for automated accessibility scanning
    - _Requirements: 22.1, 22.2, 22.3_
  - [x] 17.2 Implement candidate registration, OTP verification, and login pages: form validation, WebAuthn option, MFA OTP input, error display; high-contrast mode toggle (WCAG AA 4.5:1 contrast); keyboard navigation (Tab/Shift+Tab/Enter/Space/Arrow); screen reader ARIA labels for all dynamic content
    - _Requirements: 22.1, 22.2, 22.3, 22.4, 22.5_
  - [x] 17.3 Implement exam delivery UI: question rendering for all types (HTML5, SVG, LaTeX/MathML via MathJax/KaTeX, PNG/JPEG/WEBP, Audio/Video) without plugins; full-screen locked mode (disable clipboard, devtools, print); 5-minute warning countdown + auto-submit on timer zero; offline response buffering in IndexedDB + reconciliation on reconnect
    - _Requirements: 9.4, 9.6, 9.8, 10.4_
  - [x] 17.4 Implement navigation UI: enforce navigation policy (Sequential/Flexible/Restricted); question palette with status indicators (answered/unanswered/flagged); review-flag toggle; section switcher where policy allows; all interactions keyboard-accessible with ARIA live regions for state changes
    - _Requirements: 9.2, 9.5, 22.2, 22.3_
  - [x] 17.5 Implement candidate result page: scorecard display with subject/topic breakdown; PDF download link (password: DOB + candidateId); in-app notification panel (SSE/WebSocket); all pages responsive at minimum 320px width without horizontal scroll
    - _Requirements: 13.3, 14.3, 22.5_
  - [x] 17.6 Implement Super_Admin console pages: user management (create/update/deactivate/list); exam analytics dashboard (histogram, section averages, percentile thresholds); CSV/PDF report export
    - _Requirements: 29.1, 26.2, 26.3_
  - [ ]* 17.7 Write axe-core automated WCAG 2.2 AA scan tests for all exam-facing Angular routes (login, profile, exam instructions, question delivery, result)
    - _Requirements: 22.1_

- [x] 18. Checkpoint — full feature integration
  - Ensure all unit, property, and integration tests pass. Verify Kafka RPO=0 for response capture. Ask the user if questions arise.


- [x] 19. Observability, DevSecOps pipeline, and infrastructure configuration
  - [x] 19.1 Instrument all services with OpenTelemetry Java agent: emit structured JSON logs (service, traceId, spanId, method, path, status, responseTimeMs, masked userId); expose `/actuator/prometheus` with standard + domain-specific metrics; propagate W3C TraceContext through Kafka headers and HTTP calls; retain metrics ≥90 days, logs ≥365 days
    - _Requirements: 21.1, 21.2, 21.3, 21.6_
  - [x] 19.2 Create pre-built Grafana dashboards (6 dashboards) for: Exam Operations, Authentication, Question Bank, Paper Generation, Proctoring, Infrastructure; configure alerting rule triggering within 2 minutes when error rate > 1% over 5-minute window
    - _Requirements: 21.4, 21.5_
  - [x] 19.3 Configure PostgreSQL 16 partitioning: create all partition tables and indexes for `response`, `audit_event`, and `question` tables as defined in the schema design; configure Patroni HA with synchronous standby and PITR with WAL archival every 60 seconds; configure PgBouncer transaction-mode pooling per service
    - _Requirements: 19.6, 20.3, 20.5_
  - [x] 19.4 Configure Kafka cluster: 3+ brokers, replication factor 3, `min.insync.replicas=2`, `acks=all` on all producer configs; create all Kafka topics defined in the design; configure consumer groups per service
    - _Requirements: 20.3_
  - [x] 19.5 Configure Redis Cluster: 3 masters + 3 replicas across 3 AZs; configure Delivery Service and Response Service clients for hot session state; configure API Gateway token-bucket rate limiting via Redis
    - _Requirements: 19.2_
  - [x] 19.6 Write Helm chart under `infrastructure/helm/examination-platform/`: one directory per microservice with `deployment.yaml`, `service.yaml`, `hpa.yaml`, `configmap.yaml`; `values.yaml` with all defaults; `values-production.yaml`; HPA for Delivery/Response/Identity/Paper Generator with custom Prometheus metrics; inject all secrets via Sealed Secrets or External Secrets Operator — no plaintext secrets in values
    - _Requirements: 24.6_
  - [x] 19.7 Complete CI/CD pipeline `.github/workflows/ci.yml`: Build → Unit+Integration Tests → SpotBugs SAST → OWASP Dependency-Check → Semgrep → Docker build + Trivy image scan → OWASP ZAP DAST on staging → Helm upgrade --atomic; block PR merge on HIGH-severity SAST finding; create issue on DAST OWASP Top 10 finding; tag images with commit SHA + semver
    - _Requirements: 24.1, 24.2, 24.3, 24.5_
  - [x] 19.8 Publish OpenAPI 3.0 spec documents for all services at `/api/v1/docs`; enforce API versioning via URL prefix; document ISO 27001 evidence package (asset inventory, risk register, access control records, audit log exports) accessible to Auditor role
    - _Requirements: 23.2, 23.3, 25.5_

- [x] 20. Final checkpoint — all tests pass and all services wired
  - Ensure all unit tests, property tests, integration tests, Playwright E2E tests, and accessibility scans pass. Verify Docker Compose single-command startup. Ask the user if questions arise.


## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP build.
- Each task references specific requirements for traceability. Sub-requirements (e.g., 4.6) are cited where applicable.
- All Java code targets Java 21 with virtual threads (Project Loom); all Spring Boot services use version 3.x.
- Property-based tests use [jqwik](https://jqwik.net/) with `@Property(tries = 1000)` unless otherwise noted.
- Unit tests use JUnit 5; integration tests use Spring Boot Test + Testcontainers.
- The 14 correctness properties from the design document are each mapped to a dedicated property test sub-task.
- Checkpoints at tasks 8, 18, and 20 ensure incremental validation before proceeding to the next layer.
- All encrypted column fields must use JPA `AttributeConverter` backed by `VaultCryptoService`; no plaintext PII or question content may appear in logs.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1", "6.1", "7.1", "9.1", "10.1", "11.1", "12.1", "13.1", "14.1", "15.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.10", "3.2", "3.5", "4.2", "4.3", "6.2", "7.2", "9.2", "10.2", "11.2", "12.2", "13.2", "14.2", "15.3", "16.1"] },
    { "id": 3, "tasks": ["2.4", "2.5", "2.6", "3.3", "3.4", "4.4", "4.7", "6.4", "7.6", "7.7", "9.3", "9.4", "10.4", "11.3", "12.4", "13.4", "14.3", "15.2", "16.2", "16.3"] },
    { "id": 4, "tasks": ["2.7", "2.8", "3.6", "3.7", "4.5", "4.6", "4.8", "4.9", "4.10", "6.3", "7.3", "7.4", "7.8", "7.9", "9.5", "9.6", "10.3", "10.5", "10.6", "11.4", "12.3", "13.3", "16.4"] },
    { "id": 5, "tasks": ["2.9", "2.11", "4.11", "5.1", "6.5", "7.5", "7.10", "7.11", "9.7", "10.7", "10.8", "11.5", "11.6", "12.5", "12.6", "13.5", "17.1", "19.3", "19.4", "19.5"] },
    { "id": 6, "tasks": ["5.2", "5.3", "9.8", "10.9", "11.7", "12.7", "13.4", "17.2", "17.3", "17.4", "19.1", "19.6"] },
    { "id": 7, "tasks": ["17.5", "17.6", "19.2", "19.7", "19.8"] },
    { "id": 8, "tasks": ["17.7"] }
  ]
}
```

## Future Tasks

- [ ] 21. Implement actual inter-service REST clients (replace stubs)
  - [ ] 21.1 Implement `QuestionBankClientImpl` in paper-generator service — call question-bank-service REST API (`GET /api/questions/search`) with WebClient, passing subject, topic, difficulty, cognitiveLevel, tenantId as query params. Return `List<QuestionSummary>` from response.
    - File: `backend/paper-generator/src/main/java/com/examplatform/papergenerator/client/QuestionBankClientImpl.java`
    - _Requirements: 8.1, 8.3_
  - [ ] 21.2 Implement `KeycloakAdminClientImpl` in admin-service — call Keycloak Admin REST API (`PUT /admin/realms/{realm}/users/{userId}` with `{"enabled": false}`) using service account credentials. Handle token refresh.
    - File: `backend/admin-service/src/main/java/com/examplatform/admin/client/KeycloakAdminClientImpl.java`
    - _Requirements: 2.3, 17.1_
  - [ ] 21.3 Implement `ShiftAssignmentClientImpl` in delivery-service — call examination-service REST API (`GET /api/shifts/{shiftId}/assignments/{candidateId}`) to retrieve paper assignment, duration, and extra time.
    - File: `backend/delivery-service/src/main/java/com/examplatform/delivery/client/ShiftAssignmentClientImpl.java`
    - _Requirements: 9.1, 9.2_
  - [ ] 21.4 Implement `CandidateProfileClientImpl` in delivery-service — call candidate-service REST API (`GET /api/candidates/{candidateId}/extension`) to retrieve disability accommodations (extra time, scribe support).
    - File: `backend/delivery-service/src/main/java/com/examplatform/delivery/client/CandidateProfileClientImpl.java`
    - _Requirements: 9.1, 3.4_
  - [ ] 21.5 Implement `DigiLockerClientImpl` in candidate-service — call DigiLocker API with OAuth2 token (`GET /v3/file/uri`) to fetch and verify identity documents. Parse XML/JSON response into `DigiLockerResponse`.
    - File: `backend/candidate-service/src/main/java/com/examplatform/candidate/client/DigiLockerClientImpl.java`
    - _Requirements: 1.3_
  - [ ] 21.6 Implement `DigiLockerClientImpl` in result-service — call DigiLocker push API (`POST /v3/file/push-uri`) to push scorecard PDFs for candidate access.
    - File: `backend/result-service/src/main/java/com/examplatform/result/client/DigiLockerClientImpl.java`
    - _Requirements: 13.5_

- [ ] 22. Re-enable pgvector for similarity search
  - [ ] 22.1 Switch postgres image back to `pgvector/pgvector:pg16` in docker-compose.yml
  - [ ] 22.2 Change `embedding_vector` column from `JSONB` back to `vector(1536)` in `V1__create_question_schema.sql`
  - [ ] 22.3 Update `Question.java` entity field from `String` to `float[]` with `@Column(columnDefinition = "vector(1536)")`
  - [ ] 22.4 Restore native pgvector cosine distance query (`<=>` operator) in `QuestionRepository.findSimilarPublishedQuestion()`
  - [ ] 22.5 Add `CREATE EXTENSION IF NOT EXISTS vector;` back to `init-db.sql`
  - [ ] 22.6 Integrate real embedding API (OpenAI text-embedding-ada-002 or similar) in `SimilarityDetectionService.computeEmbedding()`
    - _Requirements: 4.7_

- [ ] 23. Frontend UI Implementation (Angular Material minimal theme)
  - [ ] 23.1 Setup global theme and layout — Material light theme, responsive sidenav shell with toolbar, role-based menu items, logout button
    - Fix APIs: None (UI only)
  - [ ] 23.2 Login page — Material card with username/password fields, error snackbar, redirect to dashboard on success
    - Fix APIs: `/api/v1/identity/auth/token` (already working)
  - [ ] 23.3 Admin Dashboard — Landing page after login showing role-based cards (Users, Questions, Exams, Results), navigation to sub-pages
    - Fix APIs: None (static UI, data loaded lazily)
  - [ ] 23.4 User Management (SUPER_ADMIN) — List users table with paginator, assign/revoke roles dialog
    - Fix APIs: Add `GET /api/v1/identity/users` endpoint, fix `GET /api/v1/identity/roles/{userId}`
  - [ ] 23.5 Question Bank (QUESTION_AUTHOR) — List questions table with filters (subject, topic, difficulty, state), create/edit question form with rich text, submit for review
    - Fix APIs: `GET /api/v1/questions` (add pagination), `POST /api/v1/questions`, `PUT /api/v1/questions/{id}`
  - [ ] 23.6 Question Review (REVIEWER) — List pending questions, approve/reject with comments
    - Fix APIs: Add `GET /api/v1/questions?state=REVIEW`, `POST /api/v1/questions/{id}/approve`, `POST /api/v1/questions/{id}/reject`
  - [ ] 23.7 Exam Management (EXAM_CONTROLLER) — Create exam form (name, duration, marks, sections JSON), list exams, publish exam
    - Fix APIs: `POST /api/v1/examinations`, `GET /api/v1/examinations`, `PUT /api/v1/examinations/{id}/publish`
  - [ ] 23.8 Candidate Registration & Profile — Self-registration form, OTP verification, view profile
    - Fix APIs: `/api/v1/identity/register`, `/api/v1/identity/otp/verify`, `GET /api/v1/candidates/{id}`
  - [ ] 23.9 Exam Delivery (CANDIDATE) — Timer, question display, option selection, navigation palette, review flag, submit
    - Fix APIs: `POST /api/v1/sessions/start`, `GET /api/v1/sessions/{id}/questions/{seq}`, `POST /api/v1/responses`
  - [ ] 23.10 Result View (CANDIDATE) — Score card display, section-wise breakdown, PDF download link
    - Fix APIs: `GET /api/v1/results/{candidateId}/{examId}`, scorecard PDF endpoint
  - [ ] 23.11 Analytics Dashboard (EXAM_CONTROLLER) — Charts (score distribution, section averages), CSV export
    - Fix APIs: `GET /api/v1/analytics/exams/{examId}`
  - [ ] 23.12 Notifications (all roles) — Bell icon with unread count, dropdown list, mark as read
    - Fix APIs: `GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read`
