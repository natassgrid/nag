# 0009: Unified Exam Spine Architecture

- **Status**: Accepted
- **Date**: 2026-10-18
- **Deciders**: Core Architecture Team, Examination Engineering Team

---

## 1. Context

In the initial implementation of the NextGen Assessment Grid (NAG), taxonomy, question classification, examination structures, and candidate learning were loosely coupled across services through free-form strings:
1. **Taxonomy Divergence**: Subjects, topics, and subtopics were owned solely by `question-bank-service`. Different examination seeds generated duplicate, slightly differing taxonomy nodes (e.g., "Mathematics" vs "Quantitative Aptitude", "General Intelligence" vs "Reasoning", "General Awareness" vs "General Studies").
2. **String-Matched Blueprints**: Blueprint rules matched questions via string queries (`UPPER(TRIM(subject)) = ...`) without subtopic granularity or numeric foreign keys.
3. **Flat Papers**: Generated papers serialized questions into a flat JSON list without persistent section boundaries, making section-wise scoring and granular analytics difficult.
4. **Learning Disconnection**: Practice sets, recommendations, and micro-lessons operated on string names (`subtopic_name`, `subject_slug`, `exam_target`) and were duplicated per target exam. Micro-lessons allowed only a single lesson per `(subtopic_id, target_exam)`.
5. **Missing Learner Anchor**: Candidates had no persistent concept of an active enrollment binding them to a specific target examination, preventing an exam-adaptive learning flow from guiding them from onboarding to readiness.

Because the platform is in pre-production, there is no legacy production data to migrate or preserve. We have the mandate for a clean architectural break to unify the entire platform around a canonical spine.

---

## 2. Decision

We establish the **Unified Exam Spine**:
$$\text{Examination} \longrightarrow \text{Subject} \longrightarrow \text{Topic} \longrightarrow \text{SubTopic} \longrightarrow \text{Question}$$

All learning content (micro-lessons, practice sets, dynamic learning paths, diagnostics, and spaced review) hangs directly off this same spine.

### 2.1 Core Architectural Tenets

1. **Platform-Default Taxonomy with Tenant Overrides**:
   - Canonical taxonomy nodes (`subject`, `topic`, `subtopic`) with `tenant_id IS NULL` serve as the platform default.
   - Tenants inherit platform nodes and may hide, rename, or reorder them via `taxonomy_tenant_override` without duplicating rows. Tenants may also add custom nodes under platform or tenant parents.
   - The effective tree for any tenant is resolved dynamically via `TaxonomyClient`.
2. **Mandatory SubTopic Classification**:
   - Every question strictly belongs to exactly one `subtopic_id` (`NOT NULL`).
   - Denormalized string columns (`subject`, `topic`, `subtopic`) and `used_in_exam_ids_json` are removed.
3. **Independent Blueprints with Numeric IDs**:
   - Blueprints are first-class, reusable entities independent of individual exams.
   - Blueprint rules target numeric IDs (`subject_id`, `topic_id`, `subtopic_id`).
   - `BlueprintMatchGrpcRequest` in `question_bank.proto` is updated to numeric fields (`subject_id`, `topic_id`, `subtopic_id`).
   - Each examination references exactly one blueprint (`examination.blueprint_id NOT NULL`).
4. **Relational Exam Syllabus**:
   - An exam's scope is explicitly defined by relational records in `exam_syllabus_node` (`SUBJECT`, `TOPIC`, `SUBTOPIC`), driving blueprint rule validation and candidate learning scope.
5. **Learner Profile & Enrollment Backbone**:
   - Onboarding requires a completed `learner_profile` (language, education level, study habits).
   - `learner_enrollment` is the sole canonical anchor tying a candidate to an examination (`candidate_id`, `examination_id`, `status`).
   - The lightweight reference `EnrollmentRef(candidateId, enrollmentId, examId)` flows across all telemetry and adaptive learning events.
6. **Exam-Agnostic, Multi-Lesson Subtopics**:
   - Micro-lessons are keyed by `subtopic_id` and are exam-agnostic.
   - A single subtopic supports multiple lessons varying by `lesson_type` (`CONCEPT`, `WORKED_EXAMPLES`, `TRAPS_AND_PITFALLS`, `CHEAT_SHEET`, `VIDEO`, etc.) and `difficulty_level` (`FOUNDATION`, `CORE`, `ADVANCED`).
7. **Tri-Mode Compatibility via `TaxonomyClient`**:
   - In microservices mode, `TaxonomyClient` queries `question-bank-service` via gRPC/REST with near-caching invalidated by `TaxonomyChanged` domain events.
   - In macro-services and monolith modes, `TaxonomyClient` delegates to an in-process bean with direct repository access.
   - Cross-schema consistency is validated logically using `TaxonomyClient.validateChain(subjectId, topicId, subtopicId)`.

---

## 3. Shared Library Contracts (`backend/shared-lib`)

The shared foundation introduces:
- `com.examplatform.shared.taxonomy.TaxonomyNodeRef`: Immutable record `(subjectId, topicId, subtopicId)` with completeness and narrowest-node helpers.
- `com.examplatform.shared.taxonomy.TaxonomyClient`: Cross-service interface offering:
  - `getEffectiveTree(tenantId, examId)`
  - `findNode(nodeType, nodeId, tenantId)` and `isValidNode(nodeType, nodeId, tenantId)`
  - `resolveName(nodeType, nodeId, tenantId)` and bulk `resolveNames()`
  - `validateChain(subjectId, topicId, subtopicId, tenantId)`
- `com.examplatform.shared.taxonomy.TaxonomyChanged`: Near-cache invalidation event for create, update, delete, merge, and override actions on topic exchange `MessagingConstants.TAXONOMY_EVENTS_TOPIC` (`exam.taxonomy.events`).
- `com.examplatform.shared.enrollment.EnrollmentRef`: Immutable record `(candidateId, enrollmentId, examId)`.
- Protobuf Contract: `BlueprintMatchGrpcRequest` updated to `int64 subject_id`, `int64 topic_id`, `int64 subtopic_id`.
- Telemetry DTOs: `SubtopicPerformanceDto` equipped with numeric ID constructors and lazy display name resolution (`withDisplayNames`).

---

## 4. Consequences

### Positive
- **Zero Divergence**: Subjects and topics are deduplicated across exams; national curriculum standards can be updated once and reflected everywhere.
- **Relational Integrity**: Eliminates brittle string-matching queries; questions, blueprints, syllabus nodes, and papers share exact foreign key semantics.
- **Continuous Adaptive Learning**: The candidate app now has a clear linear path: Login → Profile → Pick Target Exam → Enrollment → Diagnostic → Next-Best-Activity (Lessons, Practice, Section Mocks, Revision).
- **Multi-Modal Content Delivery**: Candidates who struggle with conceptual explanations can immediately switch to worked examples or visual lessons for the same subtopic without leaving the learning loop.
- **Performance**: Compact 8-byte numeric IDs (`BIGINT`) replace large string columns across 100M+ row fact tables (`question`, `response`, `paper_question`).

### Considerations
- **Downstream Breakage During Multi-Wave Transition**: Changes to `question_bank.proto` (`BlueprintMatchGrpcRequest`) and string removal break compilation in downstream services (`question-bank-service`, `paper-generator`) until subsequent wave subagents update them.
- **Cache Invalidation**: Microservice deployments must ensure reliable broadcast of `TaxonomyChanged` events to maintain near-cache coherence across service pods.
