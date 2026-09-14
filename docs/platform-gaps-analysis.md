# Platform Gaps Analysis & Remediation Tracking

> **National Assessment Grid (NAG)** — Open Digital Public Infrastructure (DPI) Platform  
> **Document Status:** Active Remediation Log  
> **Last Updated:** September 2026

---

## Executive Summary

This document catalogues identified architectural, structural, and implementation gaps across the National Assessment Grid backend services and records the concrete remediation applied to each.

---

## 1. Security & Configuration Gaps

### 1.1 Insecure Fallback Secrets in Production Profiles `[RESOLVED]`
- **Severity:** `CRITICAL`
- **Status:** **RESOLVED**
- **Component:** `auth-service`, `api-gateway`, `delivery-service`, `question-bank-service`, `examination-service`, `paper-generator`, `evaluation-service`, `candidate-service`
- **File References:**  
  - [`auth-service/application.yml`](../backend/auth-service/src/main/resources/application.yml)
  - [`api-gateway/application.yml`](../backend/api-gateway/src/main/resources/application.yml)
  - [`delivery-service/application.yml`](../backend/delivery-service/src/main/resources/application.yml)
  - [`question-bank-service/application.yml`](../backend/question-bank-service/src/main/resources/application.yml)
  - [`examination-service/application.yml`](../backend/examination-service/src/main/resources/application.yml)
  - [`paper-generator/application.yml`](../backend/paper-generator/src/main/resources/application.yml)
  - [`evaluation-service/application.yml`](../backend/evaluation-service/src/main/resources/application.yml)
  - [`candidate-service/application.yml`](../backend/candidate-service/src/main/resources/application.yml)
- **Description:**  
  Hardcoded fallback values for JWT secrets, database credentials, Redis passwords, and asymmetric encryption keys were embedded in base `application.yml` files, creating a vulnerability if deployed with missing environment variables.
- **Remediation Completed:**
  - Removed all hardcoded secrets from base configuration files.
  - Implemented strict environment variable requirements (`${VAR_NAME}`) without fallback in production profiles (`application-prod.yml`).
  - Added startup validation checks (`SecurityConfigValidator`) to abort application boot if critical secrets are absent.

### 1.2 Missing HMAC Verification on Delivery Response Packets `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `delivery-service`
- **File References:**  
  - [`ResponseSubmissionService.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/service/ResponseSubmissionService.java)
  - [`HmacVerificationFilter.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/security/HmacVerificationFilter.java)
- **Description:**  
  Response submission packets from candidate terminals lacked tamper-evident digital signatures, allowing hypothetical payload tampering in transit if TLS termination was bypassed.
- **Remediation Completed:**
  - Implemented `HmacVerificationFilter` validating SHA-256 HMAC headers against candidate session tokens before processing responses.
  - Enforced monotonic client sequence numbers to mitigate packet replay attacks.

### 1.3 Tenant Isolation Weaknesses in Dynamic Query Specifications `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `question-bank-service`, `examination-service`
- **File References:**  
  - [`QuestionSpecification.java`](../backend/question-bank-service/src/main/java/com/examplatform/questionbank/repository/QuestionSpecification.java)
  - [`ExamSpecification.java`](../backend/examination-service/src/main/java/com/examplatform/examination/repository/ExamSpecification.java)
- **Description:**  
  Dynamic JPA `Specification` builders omitted tenant predicates when filter parameters were empty, risking cross-tenant data leakage if controller tenant extraction failed.
- **Remediation Completed:**
  - Mandated `tenantId` as the root predicate (`builder.equal(root.get("tenantId"), tenantId)`) across all dynamic specifications, independent of supplied search criteria.
  - Added unit test suites verifying non-empty tenant filters under all permutations.

### 1.4 API Gateway Route Coverage & Wildcard Fallback Gaps `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `api-gateway`
- **File References:**  
  - [`routes.yaml`](../backend/api-gateway/src/main/resources/routes.yaml)
  - [`SecurityConfiguration.java`](../backend/api-gateway/src/main/java/com/examplatform/gateway/config/SecurityConfiguration.java)
- **Description:**  
  Gateway routes lacked explicit mappings for newly introduced endpoints (`/api/v1/papers/blueprints/**`, `/api/v1/translations/**`), causing requests to hit fallback handlers without proper CORS headers or role enforcement.
- **Remediation Completed:**
  - Updated `routes.yaml` with comprehensive route definitions for all microservices.
  - Added rate-limiting policies (`RedisRateLimiter`) and JWT claim validation filters for every routed path.

---

## 2. Architecture & Inter-Service Communication Gaps

### 2.1 Outbox Table Schema & Kafka Transaction Coordination `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `delivery-service`, `examination-service`, `question-bank-service`, `paper-generator`
- **File References:**  
  - [`V1_1__create_outbox_table.sql`](../backend/delivery-service/src/main/resources/db/migration/delivery/V1_1__create_outbox_table.sql)
  - [`TransactionalOutboxPublisher.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/outbox/TransactionalOutboxPublisher.java)
  - [`OutboxPoller.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/outbox/OutboxPoller.java)
- **Description:**  
  Microservices publishing domain events used direct `KafkaTemplate.send()` calls within database transactions, creating dual-write inconsistency risks during database rollbacks.
- **Remediation Completed:**
  - Applied Flyway migrations creating `outbox` tables across all stateful services.
  - Implemented transactional outbox pattern: domain events are inserted into the outbox within the local database transaction.
  - Deployed scheduled poller (`OutboxPoller`) with pessimistic locking (`SELECT FOR UPDATE SKIP LOCKED`) and exponential backoff retry.

### 2.2 Shift Window Boundary Enforcement Race Conditions `[RESOLVED]`
- **Severity:** `MEDIUM`
- **Status:** **RESOLVED**
- **Component:** `delivery-service`, `examination-service`
- **File References:**  
  - [`ShiftWindowValidator.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/service/ShiftWindowValidator.java)
  - [`DeliverySessionService.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/service/DeliverySessionService.java)
- **Description:**  
  Candidate session starts were validated against shift start/end timestamps without grace period tolerance, resulting in spurious rejections during network jitter near window boundaries.
- **Remediation Completed:**
  - Introduced configurable pre-shift and post-shift grace windows (`delivery.shift.grace-period-seconds=300`).
  - Added Redis distributed locks around session initiation to prevent concurrent multi-device starts for the same candidate.

### 2.3 Inter-Service gRPC Integration for Real-Time Shift Verification & Lookups `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `delivery-service`, `examination-service`, `candidate-service`, `question-bank-service`, `shared-lib`
- **File References:**  
  - [`examination.proto`](../backend/shared-lib/src/main/proto/examination.proto)
  - [`candidate.proto`](../backend/shared-lib/src/main/proto/candidate.proto)
  - [`question_bank.proto`](../backend/shared-lib/src/main/proto/question_bank.proto)
  - [`GrpcServerRunner.java`](../backend/shared-lib/src/main/java/com/examplatform/shared/grpc/GrpcServerRunner.java)
  - [`GrpcChannelFactory.java`](../backend/shared-lib/src/main/java/com/examplatform/shared/grpc/GrpcChannelFactory.java)
  - [`ShiftAssignmentGrpcServiceImpl.java`](../backend/examination-service/src/main/java/com/examplatform/examination/grpc/ShiftAssignmentGrpcServiceImpl.java)
  - [`CandidateProfileGrpcServiceImpl.java`](../backend/candidate-service/src/main/java/com/examplatform/candidate/grpc/CandidateProfileGrpcServiceImpl.java)
  - [`QuestionBankGrpcServiceImpl.java`](../backend/question-bank-service/src/main/java/com/examplatform/questionbank/grpc/QuestionBankGrpcServiceImpl.java)
  - [`ShiftAssignmentClientImpl.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/client/ShiftAssignmentClientImpl.java)
  - [`CandidateProfileClientImpl.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/client/CandidateProfileClientImpl.java)
  - [`QuestionCacheService.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/service/QuestionCacheService.java)
- **Description:**  
  Inter-service client implementations in `delivery-service` contained stub implementations returning mocked responses rather than querying upstream authoritative services (`examination-service`, `candidate-service`, and `question-bank-service`).
- **Remediation Completed:**
  - Configured Protobuf compiler plugin and gRPC dependencies (`io.grpc:grpc-*`) across the multi-project build.
  - Defined high-performance Protobuf contracts in `shared-lib`:
    - `ShiftAssignmentGrpcService` (`examination.proto`) for real-time candidate shift assignment verification and duration lookups.
    - `CandidateProfileGrpcService` (`candidate.proto`) for candidate profile and PwD disability extra-time extension resolution.
    - `QuestionBankGrpcService` (`question_bank.proto`) for paper question batch lookups.
  - Built Spring gRPC server runner (`GrpcServerRunner`) with automatic service discovery and graceful server lifecycle management, plus `GrpcChannelFactory` for channel pooling and caching.
  - Implemented authoritative gRPC server endpoints:
    - `ShiftAssignmentGrpcServiceImpl` in `examination-service` on port `9085`.
    - `CandidateProfileGrpcServiceImpl` in `candidate-service` on port `9082`.
    - `QuestionBankGrpcServiceImpl` in `question-bank-service` on port `9083`.
  - Implemented non-blocking gRPC client integrations in `delivery-service`:
    - `ShiftAssignmentClientImpl` connecting to `examination-service` with fallback.
    - `CandidateProfileClientImpl` connecting to `candidate-service` with fallback.
    - `QuestionCacheService` connecting to `question-bank-service` with Resilience4j circuit breakers and Redis caching.
  - Added comprehensive test suites across all 5 affected modules with 100% pass rate.

### 2.4 Blueprint Feasibility Verification & Question Deficit Admin Alerting `[RESOLVED]`
- **Severity:** `MEDIUM`
- **Status:** **RESOLVED**
- **Component:** `paper-generator`
- **File References:**  
  - [`PaperAssemblyService.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/service/PaperAssemblyService.java)
  - [`BlueprintTemplateService.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/service/BlueprintTemplateService.java)
  - [`PaperController.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/controller/PaperController.java)
  - [`BlueprintTemplateController.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/controller/BlueprintTemplateController.java)
  - [`BlueprintFeasibilityRequest.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/dto/BlueprintFeasibilityRequest.java)
  - [`BlueprintFeasibilityResponse.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/dto/BlueprintFeasibilityResponse.java)
  - [`RuleFeasibilityDetail.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/dto/RuleFeasibilityDetail.java)
- **Description:**  
  Paper generation previously performed question availability checks only during actual generation runs, failing with `InsufficientQuestionsException` without providing pre-flight feasibility validation endpoints or notifying administrators about question bank shortages.
- **Remediation Completed:**
  - Implemented `PaperAssemblyService.checkBlueprintSufficiency(...)` evaluating live question availability, reuse windows, surplus/deficit per rule, and overall feasibility without persisting papers.
  - Integrated administrative notifications publishing `BLUEPRINT_INSUFFICIENT_QUESTIONS_ALERT` events to `exam.notifications.outbound` (targeting `ADMIN` / `EXAM_CONTROLLER` roles) and audit logs to `exam.audit.events` whenever question bank shortages occur (both during pre-flight checks and generation failures).
  - Added REST endpoint `POST /api/v1/papers/blueprints/check-sufficiency` in `PaperController` for pre-flight validation of ad-hoc blueprint rules.
  - Added REST endpoint `POST /api/v1/papers/blueprint-templates/{id}/check-sufficiency` in `BlueprintTemplateController` for live auditing of stored blueprint templates.
  - Added complete unit and integration test coverage (`PaperAssemblyServiceTest`, `PaperControllerIntegrationTest`, `BlueprintTemplateControllerIntegrationTest`).

### 2.5 Batch Question Translation for Paper Generation with Overwrite & State Control `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `paper-generator`, `question-bank-service`, `frontend`
- **File References:**  
  - Database Migration: [`V1_3__add_paper_id_and_question_ids_to_batch_translation_job.sql`](../backend/question-bank-service/src/main/resources/db/migration/question/V1_3__add_paper_id_and_question_ids_to_batch_translation_job.sql)
  - Job Domain Entity: [`BatchTranslationJob.java`](../backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/domain/BatchTranslationJob.java)
  - Async Worker: [`AsyncBatchTranslationWorker.java`](../backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/service/AsyncBatchTranslationWorker.java)
  - Translation Controller: [`TranslationController.java`](../backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/controller/TranslationController.java)
  - Inter-service Orchestrator: [`PaperTranslationService.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/service/PaperTranslationService.java)
  - Paper Controller: [`PaperController.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/controller/PaperController.java)
  - Paper Service (FE): [`paper.service.ts`](../frontend/src/app/features/papers/paper.service.ts)
  - Summary Drawer (FE): [`paper-summary-drawer.component.ts`](../frontend/src/app/features/papers/paper-summary-drawer.component.ts), [`paper-summary-drawer.component.html`](../frontend/src/app/features/papers/paper-summary-drawer.component.html), [`paper-summary-drawer.component.scss`](../frontend/src/app/features/papers/paper-summary-drawer.component.scss)
  - Paper List (FE): [`paper-list.component.ts`](../frontend/src/app/features/papers/paper-list.component.ts), [`paper-list.component.html`](../frontend/src/app/features/papers/paper-list.component.html)
- **Description:**  
  Examination papers generated in English required batch translation across all constituent questions to support localized delivery in 22 8th Schedule Indian languages (Hindi, Tamil, Telugu, Bengali, Marathi, etc.) with custom overwrite policies (`overwriteExisting` toggle) and instant exam readiness (`PUBLISHED` state).
- **Remediation Completed:**
  - **Database Migration**: Added `paper_id UUID` and `question_ids JSONB` columns and indexing to `question_service.batch_translation_job` in `question-bank-service`.
  - **Asynchronous Translation Engine (`AsyncBatchTranslationWorker`)**:
    - Added chunked processing for paper-specific question ID sets (`questionRepository.findQuestionsByIdsIn`).
    - Implemented `overwriteExisting` toggle (default `true` to overwrite and upsert fresh IndicTrans2 translations; if `false`, skips questions already translated in target language).
    - Automatically marks translations as `PUBLISHED` (or configured `targetStatus`) so that translated questions are immediately available for exam delivery.
  - **Inter-service Paper Orchestration (`paper-generator`)**:
    - Implemented `PaperTranslationService.translatePaper(...)` extracting question UUIDs from `paperDefinitionJson` and delegating to `QuestionBankClient.triggerBatchTranslation(...)`.
    - Added endpoints `POST /api/v1/papers/{paperId}/translate` and `GET /api/v1/papers/{paperId}/translate/{jobId}` in `PaperController`.
  - **Frontend Integration**:
    - Updated `paper.service.ts` with `translatePaper` and `getPaperTranslationStatus` API methods.
    - Updated `paper-summary-drawer.component` with language selector (22 scheduled languages), overwrite toggle, target status picker, trigger button, and live polling progress tracker.
    - Added quick-translate action triggers in `paper-list.component`.
  - Added unit and integration tests across both microservices with 100% pass rate.

---

## 3. Business Logic & Consumer Stubs

### 3.1 Unimplemented AI Evaluation Provider
- **Severity:** `MEDIUM`
- **Status:** **TRACKED** (GitHub Issue [#89](https://github.com/natassgrid/nag/issues/89))
- **Component:** `evaluation-service`
- **File Reference:** [`AIEvaluationEngine.java`](../backend/evaluation-service/src/main/java/com/examplatform/evaluation/service/AIEvaluationEngine.java)
- **Description:**  
  Subjective answer evaluation contains stubbed scoring logic (`return EvaluationResult.builder().score(0.0)...`) without integration to AWS Bedrock or local LLM inference engines.
- **Impact:** Automated evaluation for descriptive/essay answers is non-functional.
- **Mitigation:**
  - Implement Bedrock Claude / Llama 3 client via AWS SDK with rubric-based prompt templates.
  - Add fallback rule-based heuristics when LLM quotas are exhausted.

---

## Conclusion & Action Plan

With Gaps **1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, and 2.5** fully resolved, the core security perimeter, API gateway routing topology, inter-service gRPC communication channels, blueprint feasibility auditing/alerting pipelines, and multilingual paper batch translation engines are enterprise-grade, localized for all 22 Indian scheduled languages, and production-ready.
