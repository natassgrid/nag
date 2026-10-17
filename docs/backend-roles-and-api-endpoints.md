# Consolidated Backend Roles and API Endpoints Inventory

> **Platform**: NextGen Assessment Grid (NAG) - Digital Public Infrastructure (DPI)
> **Generated Date**: 2026-10-17
> **Source Files Scanned**: 54 Controllers across 15 Microservices (`/home/sheel/IdeaProjects/nag/backend`)
> **Target Output File**: `docs/backend-roles-and-api-endpoints.md`

---

## 1. Executive Summary

A comprehensive static audit was performed across all Spring REST controllers in the NAG backend repository to:
1. **Inventory all ROLE names** referenced in `@PreAuthorize`, cross-reference them against the canonical `UserRole` enum and `TenantBootstrapService` in `identity-service`, and provide an elimination/consolidation plan for non-standard (ghost) roles.
2. **Inventory all API Endpoints** (HTTP Method, Base Path, Path, Method Name, Security Requirements, and Purpose).
3. **Identify all Route Aliases** (class-level array mappings, method-level array mappings, and inconsistent path versioning) to facilitate their deprecation and consolidation.

### High-Level Statistics

| Metric | Count | Details |
| :--- | :--- | :--- |
| **Total Microservices** | **15** | `admin-service`, `analytics-service`, `asset-service`, `audit-service`, `candidate-service`, `delivery-service`, `evaluation-service`, `examination-service`, `identity-service`, `notification-service`, `paper-generator`, `practice-service`, `question-bank-service`, `recommendation-service`, `result-service` |
| **Total Controllers** | **54** | All `@RestController` and `@Controller` classes (excluding test classes and exception handlers) |
| **Total Endpoints** | **299** | Consolidated HTTP handler methods |
| **Endpoints with Path Aliases** | **50** | Endpoints that bind multiple route paths via `Ellipsis` array annotations |
| **Total Roles Referenced** | **17** | Unique role strings detected across all controller annotations |
| **Canonical Identity Roles** | **11** | Defined in `com.examplatform.identity.domain.enums.UserRole` |
| **Non-Standard (Ghost) Roles** | **6** | Roles used in controllers that **do not exist** in identity-service |

---

## 2. Consolidated Role Analysis & Identity Gap Report

### 2.1 Canonical Identity Roles (`UserRole` in `identity-service`)

These 11 roles are formally defined in `identity-service` and provisioned by `TenantBootstrapService`:

| Role Name | Title | Purpose & Functional Scope | Base Permissions | Controllers Using | Endpoint Count |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`SUPER_ADMIN`** | Super Admin | Full platform access across all tenants, systems, and administrative functions. | `*` | 28 controllers | **117** |
| **`SECURITY_ADMIN`** | Security Admin | Manages tenant admin invitations, security policies, MFA lifecycle, and audit logs. | `IDENTITY:*`, `ADMIN:*` | 11 controllers | **26** |
| **`EXAM_CONTROLLER`** | Exam Controller | Configures and manages examination blueprints, schedules, centers, paper generation, paper approval, and live session delivery oversight. | `EXAM:*`, `DELIVERY:*`, `EVALUATION:READ` | 19 controllers | **87** |
| **`QUESTION_AUTHOR`** | Question Author | Drafts, authors, and edits questions, passages, subtopics, and AI batch question generation. | `QUESTION:READ`, `QUESTION:CREATE`, `QUESTION:EDIT` | 9 controllers | **43** |
| **`REVIEWER`** | Reviewer | Reviews questions, verifies rubrics, validates taxonomy, and reviews question translations. | `QUESTION:READ`, `QUESTION:REVIEW` | 8 controllers | **36** |
| **`SUBJECT_MATTER_EXPERT`** | Subject Matter Expert (SME) | Domain expert authorized for end-to-end question authoring, review, blueprint matching, and syllabus curation. | `QUESTION:READ`, `QUESTION:CREATE`, `QUESTION:EDIT`, `QUESTION:REVIEW`, `QUESTION:APPROVE` | QuestionController | **1** |
| **`APPROVER`** | Approver | Grants final publication approval for reviewed questions, blueprints, and examination papers. | `QUESTION:READ`, `QUESTION:APPROVE`, `EXAM:READ` | 4 controllers | **19** |
| **`TRANSLATOR`** | Translator | Translates questions, answers, and explanations into regional languages and tracks translation jobs. | `QUESTION:READ`, `QUESTION:TRANSLATE` | QuestionController, TranslationController | **10** |
| **`EVALUATOR`** | Evaluator | Accesses candidate response sheets, scores subjective items, and reviews evaluation grades. | `EVALUATION:*` | AdminDashboardController, EvaluationController, ResponseController | **5** |
| **`AUDITOR`** | Auditor | Inspects compliance reports, immutable audit logs, tamper verifications, and user activity trails. | `ADMIN:AUDIT_READ`, `IDENTITY:USER_READ`, `EXAM:READ` | AuditQueryController, ResponseController | **2** |
| **`CANDIDATE`** | Candidate | Authenticates, takes examinations, practices questions, submits responses, and accesses published scorecards. | `DELIVERY:ATTEND`, `EXAM:READ` | 13 controllers | **42** |

### 2.2 Ghost & Non-Standard Roles (For Elimination & Consolidation)

The following **6 roles** are referenced in controller security expressions but **do not exist** in `com.examplatform.identity.domain.enums.UserRole` or the tenant permission seed. These must be consolidated or officially added to prevent authorization failures:

| Non-Standard Role | Endpoints | Controllers Using | Why It Does Not Exist / Gap Analysis | Recommended Elimination / Consolidation Action |
| :--- | :--- | :--- | :--- | :--- |
| **`ADMIN`** | **69** | `AdminConfigController`<br>`AdminDashboardController`<br>`AdminPracticeSetController`<br>`AdminSecuritySettingsController`<br>`AssetController`<br>`AuditQueryController`<br>`EvaluationController`<br>`ExaminationController`<br>`PaperController`<br>`QuestionController`<br>`ResultController`<br>`ReviewController`<br>`ScorecardController`<br>`SubjectTopicController`<br>`TranslationController` | Generic administrative role used across 15 controllers and 69 endpoints. NAG identity service uses granular administrative roles (`SUPER_ADMIN`, `SECURITY_ADMIN`, `EXAM_CONTROLLER`). `ADMIN` is not in `UserRole` enum or `TenantBootstrapService`. | **Consolidate into `SUPER_ADMIN` or `SECURITY_ADMIN` (for admin settings) and `EXAM_CONTROLLER` (for exam/paper management).** |
| **`TENANT_ADMIN`** | **1** | `AdminInvitationController` | Referenced in `AdminInvitationController.inviteAdmin` to permit tenant admin onboarding, but is missing from `UserRole` enum and `TenantBootstrapService`. | **Either formally add `TENANT_ADMIN` to `UserRole` enum if tenant isolation requires a sub-super-admin, or consolidate with `SECURITY_ADMIN`.** |
| **`CONTENT_MANAGER`** | **18** | `AdminPracticeSetController`<br>`AssetController` | Referenced in `AdminPracticeSetController` and `AssetController`. In the NAG identity domain, content lifecycle is governed by `QUESTION_AUTHOR`, `REVIEWER`, `APPROVER`, and `SUBJECT_MATTER_EXPERT`. | **Eliminate `CONTENT_MANAGER` and replace with `QUESTION_AUTHOR` / `APPROVER` or `EXAM_CONTROLLER`.** |
| **`CENTER_ADMIN`** | **2** | `OfflineDeliveryController` | Referenced in `OfflineDeliveryController` (`/api/v1/sessions/{sessionId}/offline/*`) for authorizing test center offline deliveries. Missing from `UserRole` enum. | **Consolidate into `EXAM_CONTROLLER` or officially define `CENTER_ADMIN` in `UserRole` enum with explicit permissions (`DELIVERY:OFFLINE_SYNC`).** |
| **`FACULTY`** | **6** | `ExaminationController`<br>`SubjectTopicController` | Referenced in `ExaminationController` and `SubjectTopicController` as a legacy academic role. Not defined in `identity-service`. | **Eliminate `FACULTY` and consolidate into `QUESTION_AUTHOR` or `EXAM_CONTROLLER`.** |
| **`DELIVERY_SERVICE`** | **1** | `TranslationController` | Referenced in `TranslationController.getQuestionTranslation` as an end-user role (`hasAnyRole(..., 'DELIVERY_SERVICE', ...)`), confusing a service identity/client with an end-user role. | **Replace role check with OAuth2 client authority/scope (e.g., `hasAuthority('SCOPE_internal:delivery')`) or internal mTLS header check.** |

### 2.3 Role Usage Cross-Reference Matrix

| Controller | Module | Roles Referenced |
| :--- | :--- | :--- |
| `AdminConfigController` | `admin-service` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `AdminDashboardController` | `admin-service` | `ADMIN`, `EVALUATOR`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `AdminSecuritySettingsController` | `admin-service` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `AnalyticsController` | `analytics-service` | `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `AssetController` | `asset-service` | `ADMIN`, `APPROVER`, `CANDIDATE`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` |
| `AuditAnalyticsController` | `audit-service` | *None (Public / Authenticated)* |
| `AuditEventController` | `audit-service` | *None (Public / Authenticated)* |
| `AuditQueryController` | `audit-service` | `ADMIN`, `AUDITOR`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `CandidateAnalyticsController` | `candidate-service` | *None (Public / Authenticated)* |
| `CandidateProfileController` | `candidate-service` | `CANDIDATE`, `SUPER_ADMIN` |
| `NavigationController` | `delivery-service` | `CANDIDATE` |
| `OfflineDeliveryController` | `delivery-service` | `CENTER_ADMIN`, `EXAM_CONTROLLER` |
| `ProctoringController` | `delivery-service` | `CANDIDATE` |
| `SessionController` | `delivery-service` | `CANDIDATE` |
| `SessionStatusController` | `delivery-service` | `CANDIDATE` |
| `EvaluationAnalyticsController` | `evaluation-service` | *None (Public / Authenticated)* |
| `EvaluationController` | `evaluation-service` | `ADMIN`, `CANDIDATE`, `EVALUATOR`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `ExaminationAnalyticsController` | `examination-service` | *None (Public / Authenticated)* |
| `ExaminationCentreController` | `examination-service` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `ExaminationController` | `examination-service` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` |
| `ExaminationScheduleController` | `examination-service` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `GeoLocationController` | `examination-service` | *None (Public / Authenticated)* |
| `PublicExaminationController` | `examination-service` | `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `AdminInvitationController` | `identity-service` | `SECURITY_ADMIN`, `SUPER_ADMIN`, `TENANT_ADMIN` |
| `AdminProfileController` | `identity-service` | *None (Public / Authenticated)* |
| `IdentityController` | `identity-service` | `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `RoleController` | `identity-service` | `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `UserManagementController` | `identity-service` | `SECURITY_ADMIN`, `SUPER_ADMIN` |
| `VaultAdminController` | `identity-service` | `SECURITY_ADMIN` |
| `InAppNotificationController` | `notification-service` | *None (Public / Authenticated)* |
| `NotificationController` | `notification-service` | *None (Public / Authenticated)* |
| `BlueprintTemplateController` | `paper-generator` | `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `PaperController` | `paper-generator` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `AdminPracticeSetController` | `practice-service` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `PracticeExplanationController` | `practice-service` | *None (Public / Authenticated)* |
| `PracticeSessionController` | `practice-service` | *None (Public / Authenticated)* |
| `PracticeSetController` | `practice-service` | *None (Public / Authenticated)* |
| `BatchGenerationController` | `question-bank-service` | `QUESTION_AUTHOR`, `SUPER_ADMIN` |
| `PassageController` | `question-bank-service` | `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER` |
| `QuestionAiController` | `question-bank-service` | `QUESTION_AUTHOR`, `SUPER_ADMIN` |
| `QuestionBankAnalyticsController` | `question-bank-service` | *None (Public / Authenticated)* |
| `QuestionController` | `question-bank-service` | `ADMIN`, `APPROVER`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUBJECT_MATTER_EXPERT`, `SUPER_ADMIN`, `TRANSLATOR` |
| `QuestionIOController` | `question-bank-service` | `APPROVER`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` |
| `SubjectTopicController` | `question-bank-service` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` |
| `AdminMicroLessonController` | `question-bank-service` | *None (Public / Authenticated)* |
| `MicroLessonController` | `question-bank-service` | *None (Public / Authenticated)* |
| `TranslationController` | `question-bank-service` | `ADMIN`, `CANDIDATE`, `DELIVERY_SERVICE`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` |
| `LearningPathController` | `recommendation-service` | *None (Public / Authenticated)* |
| `RecommendationController` | `recommendation-service` | *None (Public / Authenticated)* |
| `ResponseController` | `response-service` | `AUDITOR`, `CANDIDATE`, `EVALUATOR` |
| `AnalyticsResultController` | `result-service` | `EXAM_CONTROLLER` |
| `ResultController` | `result-service` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `ReviewController` | `result-service` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |
| `ScorecardController` | `result-service` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` |

---

## 3. Consolidated API Route Aliases Inventory

In several controllers, endpoints specify arrays of URLs in `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc., creating multiple route aliases for identical logic. Furthermore, prefix inconsistencies (e.g., `/api/...` vs `/api/v1/...`) exist across certain services.

### 3.1 Method-Level & Class-Level Path Aliases

| Microservice | Controller | Method Name | Current Alias Routes | Recommended Canonical Route | Aliases To Remove |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `candidate-service` | `CandidateProfileController` | `create` | `/api/v1/candidates`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate` | **`/api/v1/candidates`** | `/api/v1/candidate/profile`<br>`/api/v1/candidate` |
| `candidate-service` | `CandidateProfileController` | `getByUserId` | `/api/v1/candidates/{userId}`<br>`/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` | **`/api/v1/candidates/{userId}`** | `/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` |
| `candidate-service` | `CandidateProfileController` | `update` | `/api/v1/candidates/{userId}`<br>`/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` | **`/api/v1/candidates/{userId}`** | `/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` |
| `candidate-service` | `CandidateProfileController` | `erasePii` | `/api/v1/candidates/{userId}/pii`<br>`/api/v1/candidate/profile/{userId}/pii`<br>`/api/v1/candidate/{userId}/pii` | **`/api/v1/candidates/{userId}/pii`** | `/api/v1/candidate/profile/{userId}/pii`<br>`/api/v1/candidate/{userId}/pii` |
| `candidate-service` | `CandidateProfileController` | `getEducation` | `/api/v1/candidates/{userId}/education`<br>`/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` | **`/api/v1/candidates/{userId}/education`** | `/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` |
| `candidate-service` | `CandidateProfileController` | `addEducation` | `/api/v1/candidates/{userId}/education`<br>`/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` | **`/api/v1/candidates/{userId}/education`** | `/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` |
| `candidate-service` | `CandidateProfileController` | `updateEducation` | `/api/v1/candidates/{userId}/education/{educationId}`<br>`/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` | **`/api/v1/candidates/{userId}/education/{educationId}`** | `/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` |
| `candidate-service` | `CandidateProfileController` | `deleteEducation` | `/api/v1/candidates/{userId}/education/{educationId}`<br>`/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` | **`/api/v1/candidates/{userId}/education/{educationId}`** | `/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` |
| `candidate-service` | `CandidateProfileController` | `recordConsent` | `/api/v1/candidates/{userId}/consent`<br>`/api/v1/candidate/profile/{userId}/consent`<br>`/api/v1/candidate/{userId}/consent` | **`/api/v1/candidates/{userId}/consent`** | `/api/v1/candidate/profile/{userId}/consent`<br>`/api/v1/candidate/{userId}/consent` |
| `candidate-service` | `CandidateProfileController` | `initiateDigiLockerAuth` | `/api/v1/candidates/digilocker/auth`<br>`/api/v1/candidates/{userId}/digilocker/auth`<br>`/api/v1/candidate/profile/digilocker/auth`<br>`/api/v1/candidate/profile/{userId}/digilocker/auth`<br>`/api/v1/candidate/digilocker/auth`<br>`/api/v1/candidate/{userId}/digilocker/auth` | **`/api/v1/candidates/digilocker/auth`** | `/api/v1/candidates/{userId}/digilocker/auth`<br>`/api/v1/candidate/profile/digilocker/auth`<br>`/api/v1/candidate/profile/{userId}/digilocker/auth`<br>`/api/v1/candidate/digilocker/auth`<br>`/api/v1/candidate/{userId}/digilocker/auth` |
| `candidate-service` | `CandidateProfileController` | `handleDigiLockerCallbackGet` | `/api/v1/candidates/digilocker/callback`<br>`/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` | **`/api/v1/candidates/digilocker/callback`** | `/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` |
| `candidate-service` | `CandidateProfileController` | `handleDigiLockerCallbackPost` | `/api/v1/candidates/digilocker/callback`<br>`/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` | **`/api/v1/candidates/digilocker/callback`** | `/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` |
| `candidate-service` | `CandidateProfileController` | `verifyDigiLocker` | `/api/v1/candidates/{userId}/digilocker/verify`<br>`/api/v1/candidate/profile/{userId}/digilocker/verify`<br>`/api/v1/candidate/{userId}/digilocker/verify` | **`/api/v1/candidates/{userId}/digilocker/verify`** | `/api/v1/candidate/profile/{userId}/digilocker/verify`<br>`/api/v1/candidate/{userId}/digilocker/verify` |
| `candidate-service` | `CandidateProfileController` | `verifyFace` | `/api/v1/candidates/{userId}/face/verify`<br>`/api/v1/candidate/profile/{userId}/face/verify`<br>`/api/v1/candidate/{userId}/face/verify` | **`/api/v1/candidates/{userId}/face/verify`** | `/api/v1/candidate/profile/{userId}/face/verify`<br>`/api/v1/candidate/{userId}/face/verify` |
| `identity-service` | `AdminInvitationController` | `inviteAdmin` | `/api/v1/identity/admin/invite`<br>`/api/v1/identity/invitations` | **`/api/v1/identity/admin/invite`** | `/api/v1/identity/invitations` |
| `identity-service` | `AdminInvitationController` | `validateInvite` | `/api/v1/identity/admin/invite/validate`<br>`/api/v1/identity/invitations/validate` | **`/api/v1/identity/admin/invite/validate`** | `/api/v1/identity/invitations/validate` |
| `identity-service` | `AdminInvitationController` | `acceptInvite` | `/api/v1/identity/admin/invite/accept`<br>`/api/v1/identity/invitations/accept` | **`/api/v1/identity/admin/invite/accept`** | `/api/v1/identity/invitations/accept` |
| `identity-service` | `AdminProfileController` | `getProfile` | `/api/v1/admin/me/profile`<br>`/api/v1/identity/admin/me/profile` | **`/api/v1/admin/me/profile`** | `/api/v1/identity/admin/me/profile` |
| `identity-service` | `AdminProfileController` | `updateProfile` | `/api/v1/admin/me/profile`<br>`/api/v1/identity/admin/me/profile` | **`/api/v1/admin/me/profile`** | `/api/v1/identity/admin/me/profile` |
| `identity-service` | `AdminProfileController` | `changePassword` | `/api/v1/admin/me/security/password`<br>`/api/v1/identity/admin/me/security/password` | **`/api/v1/admin/me/security/password`** | `/api/v1/identity/admin/me/security/password` |
| `identity-service` | `AdminProfileController` | `setupMfa` | `/api/v1/admin/me/security/mfa/setup`<br>`/api/v1/identity/admin/me/security/mfa/setup` | **`/api/v1/admin/me/security/mfa/setup`** | `/api/v1/identity/admin/me/security/mfa/setup` |
| `identity-service` | `AdminProfileController` | `verifyMfa` | `/api/v1/admin/me/security/mfa/verify`<br>`/api/v1/admin/me/security/mfa/verify-setup`<br>`/api/v1/identity/admin/me/security/mfa/verify`<br>`/api/v1/identity/admin/me/security/mfa/verify-setup` | **`/api/v1/admin/me/security/mfa/verify`** | `/api/v1/admin/me/security/mfa/verify-setup`<br>`/api/v1/identity/admin/me/security/mfa/verify`<br>`/api/v1/identity/admin/me/security/mfa/verify-setup` |
| `identity-service` | `AdminProfileController` | `getMfaStatus` | `/api/v1/admin/me/security/mfa/status`<br>`/api/v1/identity/admin/me/security/mfa/status` | **`/api/v1/admin/me/security/mfa/status`** | `/api/v1/identity/admin/me/security/mfa/status` |
| `identity-service` | `AdminProfileController` | `disableMfa` | `/api/v1/admin/me/security/mfa/disable`<br>`/api/v1/identity/admin/me/security/mfa/disable` | **`/api/v1/admin/me/security/mfa/disable`** | `/api/v1/identity/admin/me/security/mfa/disable` |
| `identity-service` | `AdminProfileController` | `getSessions` | `/api/v1/admin/me/sessions`<br>`/api/v1/identity/admin/me/sessions` | **`/api/v1/admin/me/sessions`** | `/api/v1/identity/admin/me/sessions` |
| `identity-service` | `AdminProfileController` | `revokeSession` | `/api/v1/admin/me/sessions/{sessionId}`<br>`/api/v1/identity/admin/me/sessions/{sessionId}` | **`/api/v1/admin/me/sessions/{sessionId}`** | `/api/v1/identity/admin/me/sessions/{sessionId}` |
| `identity-service` | `AdminProfileController` | `revokeOtherSessions` | `/api/v1/admin/me/sessions/other`<br>`/api/v1/identity/admin/me/sessions/other` | **`/api/v1/admin/me/sessions/other`** | `/api/v1/identity/admin/me/sessions/other` |
| `identity-service` | `AdminProfileController` | `listTokens` | `/api/v1/admin/me/tokens`<br>`/api/v1/identity/admin/me/tokens` | **`/api/v1/admin/me/tokens`** | `/api/v1/identity/admin/me/tokens` |
| `identity-service` | `AdminProfileController` | `createToken` | `/api/v1/admin/me/tokens`<br>`/api/v1/identity/admin/me/tokens` | **`/api/v1/admin/me/tokens`** | `/api/v1/identity/admin/me/tokens` |
| `identity-service` | `AdminProfileController` | `revokeToken` | `/api/v1/admin/me/tokens/{tokenId}`<br>`/api/v1/identity/admin/me/tokens/{tokenId}` | **`/api/v1/admin/me/tokens/{tokenId}`** | `/api/v1/identity/admin/me/tokens/{tokenId}` |
| `identity-service` | `AdminProfileController` | `getActivity` | `/api/v1/admin/me/activity`<br>`/api/v1/identity/admin/me/activity` | **`/api/v1/admin/me/activity`** | `/api/v1/identity/admin/me/activity` |
| `identity-service` | `AdminProfileController` | `exportActivity` | `/api/v1/admin/me/activity/export`<br>`/api/v1/identity/admin/me/activity/export` | **`/api/v1/admin/me/activity/export`** | `/api/v1/identity/admin/me/activity/export` |
| `identity-service` | `AdminProfileController` | `getPermissions` | `/api/v1/admin/me/permissions`<br>`/api/v1/identity/admin/me/permissions` | **`/api/v1/admin/me/permissions`** | `/api/v1/identity/admin/me/permissions` |
| `identity-service` | `IdentityController` | `verifyEmail` | `/api/v1/identity/verify/email`<br>`/api/v1/identity/auth/verify/email` | **`/api/v1/identity/verify/email`** | `/api/v1/identity/auth/verify/email` |
| `identity-service` | `IdentityController` | `verifyMobile` | `/api/v1/identity/verify/mobile`<br>`/api/v1/identity/auth/verify/mobile` | **`/api/v1/identity/verify/mobile`** | `/api/v1/identity/auth/verify/mobile` |
| `identity-service` | `IdentityController` | `resendEmailOtp` | `/api/v1/identity/resend/email-otp`<br>`/api/v1/identity/otp/resend/email` | **`/api/v1/identity/resend/email-otp`** | `/api/v1/identity/otp/resend/email` |
| `identity-service` | `IdentityController` | `verifyOtp` | `/api/v1/identity/otp/verify`<br>`/api/v1/identity/verify-otp` | **`/api/v1/identity/otp/verify`** | `/api/v1/identity/verify-otp` |
| `identity-service` | `IdentityController` | `token` | `/api/v1/identity/auth/token`<br>`/api/v1/identity/auth/login` | **`/api/v1/identity/auth/token`** | `/api/v1/identity/auth/login` |
| `identity-service` | `IdentityController` | `refreshToken` | `/api/v1/identity/auth/token/refresh`<br>`/api/v1/identity/auth/refresh` | **`/api/v1/identity/auth/token/refresh`** | `/api/v1/identity/auth/refresh` |
| `identity-service` | `IdentityController` | `setup2fa` | `/api/v1/identity/auth/2fa/setup`<br>`/api/v1/identity/auth/mfa/totp/setup` | **`/api/v1/identity/auth/2fa/setup`** | `/api/v1/identity/auth/mfa/totp/setup` |
| `identity-service` | `IdentityController` | `get2faStatus` | `/api/v1/identity/auth/2fa/status`<br>`/api/v1/identity/auth/mfa/totp/status` | **`/api/v1/identity/auth/2fa/status`** | `/api/v1/identity/auth/mfa/totp/status` |
| `identity-service` | `IdentityController` | `verify2faSetup` | `/api/v1/identity/auth/2fa/verify-setup`<br>`/api/v1/identity/auth/mfa/totp/verify-setup` | **`/api/v1/identity/auth/2fa/verify-setup`** | `/api/v1/identity/auth/mfa/totp/verify-setup` |
| `identity-service` | `IdentityController` | `disable2fa` | `/api/v1/identity/auth/2fa/disable`<br>`/api/v1/identity/auth/mfa/totp/disable` | **`/api/v1/identity/auth/2fa/disable`** | `/api/v1/identity/auth/mfa/totp/disable` |
| `paper-generator` | `PaperController` | `getPaperTranslationStatus` | `/api/v1/papers/{paperId}/translate/{jobId}`<br>`/api/v1/papers/{paperId}/translation-status/{jobId}` | **`/api/v1/papers/{paperId}/translate/{jobId}`** | `/api/v1/papers/{paperId}/translation-status/{jobId}` |
| `paper-generator` | `PaperController` | `getLatestPaperTranslationStatus` | `/api/v1/papers/{paperId}/translation-status`<br>`/api/v1/papers/{paperId}/translate/status` | **`/api/v1/papers/{paperId}/translation-status`** | `/api/v1/papers/{paperId}/translate/status` |
| `practice-service` | `PracticeExplanationController` | `getExplanation` | `/api/practice/questions/{questionId}/explanation`<br>`/api/v1/practice/questions/{questionId}/explanation` | **`/api/v1/practice/questions/{questionId}/explanation`** | `/api/practice/questions/{questionId}/explanation` |
| `question-bank-service` | `QuestionController` | `matchBlueprint` | `/api/v1/questions/match-blueprint`<br>`/api/v1/questions/blueprint-match` | **`/api/v1/questions/match-blueprint`** | `/api/v1/questions/blueprint-match` |
| `question-bank-service` | `QuestionController` | `getQuestionsByIds` | `/api/v1/questions/by-ids`<br>`/api/v1/questions/batch-find` | **`/api/v1/questions/by-ids`** | `/api/v1/questions/batch-find` |
| `question-bank-service` | `MicroLessonController` | `getPublishedMicroLesson` | `/api/micro-lessons/subtopic/{subtopicId}`<br>`/api/v1/micro-lessons/subtopic/{subtopicId}` | **`/api/v1/micro-lessons/subtopic/{subtopicId}`** | `/api/micro-lessons/subtopic/{subtopicId}` |
| `question-bank-service` | `TranslationController` | `getBatchJobStatus` | `/api/v1/translations/batch/{jobId}`<br>`/api/v1/translations/batch/{jobId}/status` | **`/api/v1/translations/batch/{jobId}`** | `/api/v1/translations/batch/{jobId}/status` |

### 3.2 Service Route Prefix Inconsistencies

| Service / Controller | Current Base Path | Standardized Canonical Path | Reason for Anomaly |
| :--- | :--- | :--- | :--- |
| `practice-service` (`AdminPracticeSetController`) | `/api/admin/practice/sets` | `/api/v1/practice/admin/sets` or `/api/v1/admin/practice/sets` | Missing `/v1/` version prefix |
| `practice-service` (`PracticeSessionController`) | `/api/practice` | `/api/v1/practice` | Missing `/v1/` version prefix |
| `practice-service` (`PracticeSetController`) | `/api/practice/sets` | `/api/v1/practice/sets` | Missing `/v1/` version prefix |
| `practice-service` (`PracticeExplanationController`) | `{"/api/practice/questions", "/api/v1/practice/questions"}` | `/api/v1/practice/questions` | Contains dual versioned & unversioned base alias |
| `question-bank-service` (`AdminMicroLessonController`) | `/api/admin/micro-lessons` | `/api/v1/admin/micro-lessons` | Missing `/v1/` version prefix |
| `question-bank-service` (`MicroLessonController`) | `{"/api/micro-lessons", "/api/v1/micro-lessons"}` | `/api/v1/micro-lessons` | Contains dual versioned & unversioned base alias |
| `recommendation-service` (`LearningPathController`) | `/api/learning-path` | `/api/v1/learning-path` | Missing `/v1/` version prefix |
| `recommendation-service` (`RecommendationController`) | `/api/recommendations` | `/api/v1/recommendations` | Missing `/v1/` version prefix |
| `evaluation-service` (`EvaluationAnalyticsController`) | `/api/v1/evaluation/analytics/summary` | `/api/v1/evaluations/analytics/summary` | Singular `evaluation` vs plural `evaluations` standard |

---

## 4. Master Consolidated List of All API Endpoints

Below is the complete, exhaustive catalog of all 299 API endpoints grouped by microservice and controller.

### 4.1 Module: `admin-service`

#### Controller: `AdminConfigController`
- **File**: `admin-service/src/main/java/com/examplatform/admin/controller/AdminConfigController.java`
- **Class Base Path(s)**: `/api/v1/admin/config`
- **Class `@PreAuthorize`**: `"hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN')"`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/admin/config` | `getConfig` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Retrieves all system configuration parameters for the given or current tenant. |
| **`GET`** | `/api/v1/admin/config/map` | `getConfigMap` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Retrieves all system configuration parameters as a key-value map. |
| **`PUT`** | `/api/v1/admin/config` | `updateConfig` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Updates a single system configuration parameter via JSON body. |
| **`PUT`** | `/api/v1/admin/config/bulk` | `updateBulkConfig` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Bulk updates multiple system configuration parameters in a single transaction. |
| **`POST`** | `/api/v1/admin/config/reset` | `resetConfigToDefaults` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Resets all configurations to platform defaults for the tenant. |

#### Controller: `AdminDashboardController`
- **File**: `admin-service/src/main/java/com/examplatform/admin/controller/AdminDashboardController.java`
- **Class Base Path(s)**: `/api/v1/admin/dashboard`
- **Class `@PreAuthorize`**: `"hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR', 'REVIEWER', 'EVALUATOR')"`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/admin/dashboard/summary` | `getDashboardSummary` | `ADMIN`, `EVALUATOR`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Retrieve aggregated dashboard summary metrics and status feeds for the active tenant. GET /api/v1/admin/dashboard/summary |

#### Controller: `AdminSecuritySettingsController`
- **File**: `admin-service/src/main/java/com/examplatform/admin/controller/AdminSecuritySettingsController.java`
- **Class Base Path(s)**: `/api/v1/admin/settings/security/mfa`
- **Class `@PreAuthorize`**: `"hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN')"`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/admin/settings/security/mfa` | `getMfaSecurityPolicy` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Retrieves current cluster MFA security policies. |
| **`PUT`** | `/api/v1/admin/settings/security/mfa` | `updateMfaSecurityPolicy` | `ADMIN`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Updates cluster MFA security policies. |

### 4.2 Module: `analytics-service`

#### Controller: `AnalyticsController`
- **File**: `analytics-service/src/main/java/com/examplatform/analytics/controller/AnalyticsController.java`
- **Class Base Path(s)**: `/api/v1/analytics/exams`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/analytics/exams/{id}` | `getExamAnalytics` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Returns computed analytics for a given exam. |
| **`POST`** | `/api/v1/analytics/exams/{id}/compute` | `computeExamAnalytics` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Triggers batch computation of analytics for a given exam from persisted candidate records. |
| **`GET`** | `/api/v1/analytics/exams/{id}/export` | `exportExamAnalytics` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Exports analytics data in the specified format (csv or pdf). |

### 4.3 Module: `asset-service`

#### Controller: `AssetController`
- **File**: `asset-service/src/main/java/com/examplatform/asset/controller/AssetController.java`
- **Class Base Path(s)**: `/api/v1/assets`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/assets` | `uploadAsset` | `ADMIN`, `CANDIDATE`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Upload a new media asset (multipart/form-data). |
| **`POST`** | `/api/v1/assets/{id}/content` | `replaceContent` | `ADMIN`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Replace the binary content of an existing asset. |
| **`GET`** | `/api/v1/assets/{id}` | `getAsset` | `ADMIN`, `APPROVER`, `CANDIDATE`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `REVIEWER` | No | Retrieve asset metadata by ID. |
| **`GET`** | `/api/v1/assets` | `listAssets` | `ADMIN`, `APPROVER`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `REVIEWER` | No | List assets for tenant (paginated). |
| **`GET`** | `/api/v1/assets/search` | `searchAssets` | `ADMIN`, `APPROVER`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `REVIEWER` | No | Search and list assets with filters and pagination. |
| **`PUT`** | `/api/v1/assets/{id}/metadata` | `updateMetadata` | `ADMIN`, `CONTENT_MANAGER`, `QUESTION_AUTHOR` | No | Update user-supplied metadata for an asset. |
| **`POST`** | `/api/v1/assets/references` | `addReference` | `ADMIN`, `CONTENT_MANAGER`, `QUESTION_AUTHOR` | No | Create a reference between an asset and a platform entity. |
| **`GET`** | `/api/v1/assets/{id}/references` | `getReferences` | `ADMIN`, `APPROVER`, `CONTENT_MANAGER`, `QUESTION_AUTHOR`, `REVIEWER` | No | List all references for an asset. |
| **`DELETE`** | `/api/v1/assets/references/{refId}` | `removeReference` | `ADMIN`, `CONTENT_MANAGER` | No | Delete a specific reference. |
| **`DELETE`** | `/api/v1/assets/{id}` | `deleteAsset` | `ADMIN`, `CONTENT_MANAGER` | No | Soft-delete an asset. Rejected if the asset has active references. |
| **`GET`** | `/api/v1/assets/{id}/download` | `downloadAsset` | *Public / None* | No | Download the binary content of an asset. Public access permitted so standard HTML <img>, <audio>, and <video> elements can stream media without custom auth headers. |
| **`GET`** | `/api/v1/assets/{id}/url` | `getPublicUrl` | *Public / None* | No | Get accessible public or direct URL for an asset. |
| **`PUT`** | `/api/v1/assets/{id}/archive` | `archiveAsset` | `ADMIN`, `CONTENT_MANAGER` | No | Archive an active asset. |
| **`PUT`** | `/api/v1/assets/{id}/restore` | `restoreAsset` | `ADMIN`, `CONTENT_MANAGER` | No | Restore an archived asset to ACTIVE. |

### 4.4 Module: `audit-service`

#### Controller: `AuditAnalyticsController`
- **File**: `audit-service/src/main/java/com/examplatform/audit/controller/AuditAnalyticsController.java`
- **Class Base Path(s)**: *None*

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/audit/events/recent` | `getRecentEvents` | *Public / None* | No | REST backup endpoint for recent ledger events. GET /api/v1/audit/events/recent |

#### Controller: `AuditEventController`
- **File**: `audit-service/src/main/java/com/examplatform/audit/controller/AuditEventController.java`
- **Class Base Path(s)**: `/api/v1/audit/events`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`PUT`** | `/api/v1/audit/events/{id}` | `rejectUpdate` | *Public / None* | No | Reject any UPDATE attempt on an existing audit event. Returns HTTP 403 and writes a TAMPER_ATTEMPT audit event. |
| **`DELETE`** | `/api/v1/audit/events/{id}` | `rejectDelete` | *Public / None* | No | Reject any DELETE attempt on an existing audit event. Returns HTTP 403 and writes a TAMPER_ATTEMPT audit event. |

#### Controller: `AuditQueryController`
- **File**: `audit-service/src/main/java/com/examplatform/audit/controller/AuditQueryController.java`
- **Class Base Path(s)**: `/api/v1/audit/events`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/audit/events` | `queryAuditEvents` | `ADMIN`, `AUDITOR`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Query audit events with optional filters. |

### 4.5 Module: `candidate-service`

#### Controller: `CandidateAnalyticsController`
- **File**: `candidate-service/src/main/java/com/examplatform/candidate/controller/CandidateAnalyticsController.java`
- **Class Base Path(s)**: `/api/v1/candidates/analytics`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/candidates/analytics/summary` | `getSummary` | *Public / None* | No | Handler method `getSummary` |

#### Controller: `CandidateProfileController`
- **File**: `candidate-service/src/main/java/com/examplatform/candidate/controller/CandidateProfileController.java`
- **Class Base Path(s)**: `/api/v1/candidates`, `/api/v1/candidate/profile`, `/api/v1/candidate`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/candidates`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate` | `create` | `CANDIDATE` | **YES** | Create a new candidate profile. Requires CANDIDATE role. |
| **`GET`** | `/api/v1/candidates/{userId}`<br>`/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` | `getByUserId` | `CANDIDATE`, `SUPER_ADMIN` | **YES** | Get candidate profile. CANDIDATE can get own profile, SUPER_ADMIN can get any. |
| **`PUT`** | `/api/v1/candidates/{userId}`<br>`/api/v1/candidates`<br>`/api/v1/candidate/profile/{userId}`<br>`/api/v1/candidate/profile`<br>`/api/v1/candidate/{userId}`<br>`/api/v1/candidate` | `update` | `CANDIDATE` | **YES** | Update candidate profile. CANDIDATE can update own profile only. |
| **`DELETE`** | `/api/v1/candidates/{userId}/pii`<br>`/api/v1/candidate/profile/{userId}/pii`<br>`/api/v1/candidate/{userId}/pii` | `erasePii` | `CANDIDATE`, `SUPER_ADMIN` | **YES** | DPDP erasure endpoint. CANDIDATE can erase own data, SUPER_ADMIN can erase any. |
| **`GET`** | `/api/v1/candidates/{userId}/education`<br>`/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` | `getEducation` | `CANDIDATE`, `SUPER_ADMIN` | **YES** | List all educational details for a candidate. |
| **`POST`** | `/api/v1/candidates/{userId}/education`<br>`/api/v1/candidate/profile/{userId}/education`<br>`/api/v1/candidate/{userId}/education` | `addEducation` | `CANDIDATE` | **YES** | Add a new educational qualification record. Requires CANDIDATE role. |
| **`PUT`** | `/api/v1/candidates/{userId}/education/{educationId}`<br>`/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` | `updateEducation` | `CANDIDATE` | **YES** | Update an educational qualification record. Requires CANDIDATE role. |
| **`DELETE`** | `/api/v1/candidates/{userId}/education/{educationId}`<br>`/api/v1/candidate/profile/{userId}/education/{educationId}`<br>`/api/v1/candidate/{userId}/education/{educationId}` | `deleteEducation` | `CANDIDATE` | **YES** | Delete an educational qualification record. Requires CANDIDATE role. |
| **`POST`** | `/api/v1/candidates/{userId}/consent`<br>`/api/v1/candidate/profile/{userId}/consent`<br>`/api/v1/candidate/{userId}/consent` | `recordConsent` | `CANDIDATE` | **YES** | Record explicit consent before biometric data collection. Requires CANDIDATE role. Validates: Requirements 25.3 |
| **`GET`** | `/api/v1/candidates/digilocker/auth`<br>`/api/v1/candidates/{userId}/digilocker/auth`<br>`/api/v1/candidate/profile/digilocker/auth`<br>`/api/v1/candidate/profile/{userId}/digilocker/auth`<br>`/api/v1/candidate/digilocker/auth`<br>`/api/v1/candidate/{userId}/digilocker/auth` | `initiateDigiLockerAuth` | `CANDIDATE` | **YES** | Initiates DigiLocker OAuth2 authorization flow. Returns authorization URL and state token. Validates: Requirements 1.3 |
| **`GET`** | `/api/v1/candidates/digilocker/callback`<br>`/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` | `handleDigiLockerCallbackGet` | *Public / None* | **YES** | Handles DigiLocker OAuth2 callback with authorization code. Validates: Requirements 1.3 |
| **`POST`** | `/api/v1/candidates/digilocker/callback`<br>`/api/v1/candidate/profile/digilocker/callback`<br>`/api/v1/candidate/digilocker/callback` | `handleDigiLockerCallbackPost` | *Public / None* | **YES** | Handles DigiLocker OAuth2 callback with authorization code via POST. Validates: Requirements 1.3 |
| **`POST`** | `/api/v1/candidates/{userId}/digilocker/verify`<br>`/api/v1/candidate/profile/{userId}/digilocker/verify`<br>`/api/v1/candidate/{userId}/digilocker/verify` | `verifyDigiLocker` | `CANDIDATE` | **YES** | Verify candidate identity document via DigiLocker. Requires CANDIDATE role. Validates: Requirements 1.3 |
| **`POST`** | `/api/v1/candidates/{userId}/face/verify`<br>`/api/v1/candidate/profile/{userId}/face/verify`<br>`/api/v1/candidate/{userId}/face/verify` | `verifyFace` | `CANDIDATE` | **YES** | Verify candidate face against identity document photograph. Requires CANDIDATE role. Validates: Requirements 1.4 |

### 4.6 Module: `delivery-service`

#### Controller: `NavigationController`
- **File**: `delivery-service/src/main/java/com/examplatform/delivery/controller/NavigationController.java`
- **Class Base Path(s)**: `/api/v1/sessions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/sessions/{sessionId}/navigate` | `navigate` | `CANDIDATE` | No | Navigate within an active exam session. Validates the navigation against the session's policy (Sequential, Flexible, Restricted). Returns 422 if the navigation violates the policy. |

#### Controller: `OfflineDeliveryController`
- **File**: `delivery-service/src/main/java/com/examplatform/delivery/controller/OfflineDeliveryController.java`
- **Class Base Path(s)**: `/api/v1/sessions/{sessionId}/offline`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/sessions/{sessionId}/offline/preload` | `preloadExamPackage` | `CENTER_ADMIN`, `EXAM_CONTROLLER` | No | Pre-loads an exam package for offline delivery at a specific center. |
| **`POST`** | `/api/v1/sessions/{sessionId}/offline/reconcile` | `reconcileOnReconnect` | `CENTER_ADMIN`, `EXAM_CONTROLLER` | No | Reconciles offline data when connectivity is restored. |

#### Controller: `ProctoringController`
- **File**: `delivery-service/src/main/java/com/examplatform/delivery/controller/ProctoringController.java`
- **Class Base Path(s)**: `/api/v1/sessions/{sessionId}/proctoring`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/sessions/{sessionId}/proctoring/snapshot` | `captureSnapshot` | `CANDIDATE` | No | Captures a webcam snapshot for AI proctoring analysis. |
| **`POST`** | `/api/v1/sessions/{sessionId}/proctoring/fullscreen-exit` | `recordFullScreenExit` | `CANDIDATE` | No | Records a full-screen exit event for the given session. The session is flagged after 3 exits. |

#### Controller: `SessionController`
- **File**: `delivery-service/src/main/java/com/examplatform/delivery/controller/SessionController.java`
- **Class Base Path(s)**: `/api/v1/sessions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/sessions/start` | `startSession` | `CANDIDATE` | No | Start a new exam session or seamlessly resume an existing active session for the candidate. Extracts candidateId from the JWT subject claim and tenantId from the JWT tenant claim. |
| **`GET`** | `/api/v1/sessions/paper/{paperId}/questions` | `getPaperQuestions` | *Public / None* | No | Retrieve question list for a specific paper (used for practice mock test delivery). |
| **`POST`** | `/api/v1/sessions/terminate-active` | `terminateActiveSessions` | `CANDIDATE` | No | Terminate all active exam sessions for the authenticated candidate. Allows candidate to clear stale or concurrent sessions when switching exams. |
| **`POST`** | `/api/v1/sessions/{sessionId}/resume` | `resumeSession` | `CANDIDATE` | No | Handler method `resumeSession` |
| **`GET`** | `/api/v1/sessions/{sessionId}/questions` | `getSessionQuestions` | `CANDIDATE` | No | Retrieve question list for an ongoing active exam session. |
| **`GET`** | `/api/v1/sessions/{sessionId}/questions/{sequenceNumber}` | `getSessionQuestion` | `CANDIDATE` | No | Retrieve a specific question by 1-based sequence number for an ongoing active exam session. |

#### Controller: `SessionStatusController`
- **File**: `delivery-service/src/main/java/com/examplatform/delivery/controller/SessionStatusController.java`
- **Class Base Path(s)**: `/api/v1/sessions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/sessions/{sessionId}/status` | `getSessionStatus` | `CANDIDATE` | No | Returns the current session status, remaining time, and fullscreen lock state. |

### 4.7 Module: `evaluation-service`

#### Controller: `EvaluationAnalyticsController`
- **File**: `evaluation-service/src/main/java/com/examplatform/evaluation/controller/EvaluationAnalyticsController.java`
- **Class Base Path(s)**: *None*

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/evaluation/analytics/summary` | `getAnalyticsSummary` | *Public / None* | No | REST backup endpoint for analytics summary. GET /api/v1/evaluation/analytics/summary |

#### Controller: `EvaluationController`
- **File**: `evaluation-service/src/main/java/com/examplatform/evaluation/controller/EvaluationController.java`
- **Class Base Path(s)**: `/api/v1/evaluations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/evaluations/{id}/score` | `recordScore` | `EVALUATOR`, `EXAM_CONTROLLER` | No | Record a manual evaluator's score for an evaluation. POST /api/v1/evaluations/{id}/score |
| **`POST`** | `/api/v1/evaluations/aggregate` | `aggregateScores` | `EVALUATOR`, `EXAM_CONTROLLER` | No | Aggregate scores for a candidate's session. POST /api/v1/evaluations/aggregate |
| **`GET`** | `/api/v1/evaluations/candidate/{candidateId}` | `getCandidateEvaluations` | `ADMIN`, `CANDIDATE`, `EVALUATOR`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Retrieve all evaluations for a candidate. GET /api/v1/evaluations/candidate/{candidateId} |

### 4.8 Module: `examination-service`

#### Controller: `ExaminationAnalyticsController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/ExaminationAnalyticsController.java`
- **Class Base Path(s)**: *None*

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/examinations/analytics/summary` | `getAnalyticsSummary` | *Public / None* | No | REST backup endpoint for analytics summary. GET /api/v1/examinations/analytics/summary |

#### Controller: `ExaminationCentreController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/ExaminationCentreController.java`
- **Class Base Path(s)**: *None*

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/examinations/centres` | `createCentre` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `createCentre` |
| **`GET`** | `/api/v1/examinations/centres` | `listCentres` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `listCentres` |
| **`GET`** | `/api/v1/examinations/centres/{centreId}` | `getCentre` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `getCentre` |
| **`PUT`** | `/api/v1/examinations/centres/{centreId}/deactivate` | `deactivateCentre` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `deactivateCentre` |
| **`POST`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | `upsertAllocation` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `upsertAllocation` |
| **`GET`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | `listAllocations` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `listAllocations` |

#### Controller: `ExaminationController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/ExaminationController.java`
- **Class Base Path(s)**: `/api/v1/examinations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/examinations` | `list` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | List all examinations for the current tenant (paginated). Requires appropriate staff/admin roles. |
| **`POST`** | `/api/v1/examinations` | `create` | `EXAM_CONTROLLER` | No | Create a new examination. Requires EXAM_CONTROLLER role. |
| **`PUT`** | `/api/v1/examinations/{examId}` | `update` | `EXAM_CONTROLLER` | No | Update an existing examination. Requires EXAM_CONTROLLER role. |
| **`ALL`** | `/api/v1/examinations/{examId}/publish` | `publish` | `EXAM_CONTROLLER` | No | Publish an examination (transition from DRAFT to PUBLISHED). Requires EXAM_CONTROLLER role. Supports both PUT and POST for client flexibility. |
| **`GET`** | `/api/v1/examinations/{examId}` | `getById` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Retrieve an examination by ID. Requires staff or admin role. |

#### Controller: `ExaminationScheduleController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/ExaminationScheduleController.java`
- **Class Base Path(s)**: `/api/v1/examinations/{examId}/schedules`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/examinations/{examId}/schedules` | `create` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `create` |
| **`GET`** | `/api/v1/examinations/{examId}/schedules` | `list` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `list` |
| **`GET`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}` | `getById` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `getById` |
| **`PUT`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/transition` | `transition` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Transition through the approval workflow (Req 7b.7). Actor roles are extracted from the JWT realm_access.roles claim. |
| **`PUT`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/amend` | `amend` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Amend a published schedule — creates a new version row (Req 7b.8). |
| **`POST`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts` | `addShift` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `addShift` |
| **`PUT`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}` | `updateShift` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `updateShift` |
| **`GET`** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts` | `listShifts` | `EXAM_CONTROLLER`, `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Handler method `listShifts` |

#### Controller: `GeoLocationController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/GeoLocationController.java`
- **Class Base Path(s)**: `/api/v1/geo`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/geo/countries` | `getCountries` | *Public / None* | No | Handler method `getCountries` |
| **`GET`** | `/api/v1/geo/countries/{countryId}/states` | `getStates` | *Public / None* | No | Handler method `getStates` |
| **`GET`** | `/api/v1/geo/states/{stateId}/cities` | `getCities` | *Public / None* | No | Handler method `getCities` |

#### Controller: `PublicExaminationController`
- **File**: `examination-service/src/main/java/com/examplatform/examination/controller/PublicExaminationController.java`
- **Class Base Path(s)**: `/api/v1/examinations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/examinations/public` | `listPublished` | *Public / None* | No | List all PUBLISHED examinations. This endpoint is publicly accessible — no authentication required. |
| **`GET`** | `/api/v1/examinations/public/{examId}` | `getPublishedById` | *Public / None* | No | Get a single published examination by ID. Publicly accessible. |
| **`GET`** | `/api/v1/examinations/centres/public` | `listPublicCentres` | *Public / None* | No | List active examination centres available across India for candidate preference selection. Publicly accessible. |
| **`POST`** | `/api/v1/examinations/{examId}/apply` | `apply` | `CANDIDATE` | No | Apply the authenticated candidate for a specific examination with preferences. Returns 409 Conflict if already applied. |
| **`GET`** | `/api/v1/examinations/my-exams` | `getMyApplications` | `CANDIDATE` | No | List all examinations the authenticated candidate has applied for. |
| **`GET`** | `/api/v1/examinations/{examId}/my-application` | `getMyApplication` | `CANDIDATE` | No | Check application status for a specific exam for the authenticated candidate. |
| **`GET`** | `/api/v1/examinations/{examId}/admit-card` | `getAdmitCard` | `CANDIDATE` | No | Retrieve digital hall ticket / admit card for a registered examination. |
| **`GET`** | `/api/v1/examinations/applications/{applicationId}/admit-card` | `getAdmitCardByApplicationId` | `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Retrieve digital hall ticket / admit card by application ID. |

### 4.9 Module: `identity-service`

#### Controller: `AdminInvitationController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/AdminInvitationController.java`
- **Class Base Path(s)**: `/api/v1/identity/admin/invite`, `/api/v1/identity/invitations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/identity/admin/invite`<br>`/api/v1/identity/invitations` | `inviteAdmin` | `SECURITY_ADMIN`, `SUPER_ADMIN`, `TENANT_ADMIN` | **YES** | Send email invitation to a new admin / staff user. Restricted to SUPER_ADMIN, SECURITY_ADMIN, or TENANT_ADMIN. |
| **`GET`** | `/api/v1/identity/admin/invite/validate`<br>`/api/v1/identity/invitations/validate` | `validateInvite` | *Public / None* | **YES** | Validate an invitation token before displaying onboarding 2FA setup form. |
| **`POST`** | `/api/v1/identity/admin/invite/accept`<br>`/api/v1/identity/invitations/accept` | `acceptInvite` | *Public / None* | **YES** | Accept invitation: set password, configure 2FA TOTP, activate account. |

#### Controller: `AdminProfileController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/AdminProfileController.java`
- **Class Base Path(s)**: `/api/v1/admin/me`, `/api/v1/identity/admin/me`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/admin/me/profile`<br>`/api/v1/identity/admin/me/profile` | `getProfile` | `isAuthenticated()` | **YES** | 1. Retrieve Current Admin Profile & Permissions |
| **`PUT`** | `/api/v1/admin/me/profile`<br>`/api/v1/identity/admin/me/profile` | `updateProfile` | `isAuthenticated()` | **YES** | 2. Update Personal Profile & Localization Preferences |
| **`ALL`** | `/api/v1/admin/me/security/password`<br>`/api/v1/identity/admin/me/security/password` | `changePassword` | `isAuthenticated()` | **YES** | 3. Change Account Password |
| **`POST`** | `/api/v1/admin/me/security/mfa/setup`<br>`/api/v1/identity/admin/me/security/mfa/setup` | `setupMfa` | `isAuthenticated()` | **YES** | 4. Multi-Factor Authentication: Setup TOTP |
| **`POST`** | `/api/v1/admin/me/security/mfa/verify`<br>`/api/v1/admin/me/security/mfa/verify-setup`<br>`/api/v1/identity/admin/me/security/mfa/verify`<br>`/api/v1/identity/admin/me/security/mfa/verify-setup` | `verifyMfa` | `isAuthenticated()` | **YES** | 5. Multi-Factor Authentication: Verify and Enable TOTP |
| **`GET`** | `/api/v1/admin/me/security/mfa/status`<br>`/api/v1/identity/admin/me/security/mfa/status` | `getMfaStatus` | `isAuthenticated()` | **YES** | 6. Multi-Factor Authentication: Check Status |
| **`POST`** | `/api/v1/admin/me/security/mfa/disable`<br>`/api/v1/identity/admin/me/security/mfa/disable` | `disableMfa` | `isAuthenticated()` | **YES** | 7. Multi-Factor Authentication: Disable MFA |
| **`GET`** | `/api/v1/admin/me/sessions`<br>`/api/v1/identity/admin/me/sessions` | `getSessions` | `isAuthenticated()` | **YES** | 8. Active Sessions: List |
| **`DELETE`** | `/api/v1/admin/me/sessions/{sessionId}`<br>`/api/v1/identity/admin/me/sessions/{sessionId}` | `revokeSession` | `isAuthenticated()` | **YES** | 9. Active Sessions: Terminate Single Session |
| **`DELETE`** | `/api/v1/admin/me/sessions/other`<br>`/api/v1/identity/admin/me/sessions/other` | `revokeOtherSessions` | `isAuthenticated()` | **YES** | 10. Active Sessions: Revoke All Other Sessions |
| **`GET`** | `/api/v1/admin/me/tokens`<br>`/api/v1/identity/admin/me/tokens` | `listTokens` | `isAuthenticated()` | **YES** | 11. Personal Access Tokens: List |
| **`POST`** | `/api/v1/admin/me/tokens`<br>`/api/v1/identity/admin/me/tokens` | `createToken` | `isAuthenticated()` | **YES** | 12. Personal Access Tokens: Create |
| **`DELETE`** | `/api/v1/admin/me/tokens/{tokenId}`<br>`/api/v1/identity/admin/me/tokens/{tokenId}` | `revokeToken` | `isAuthenticated()` | **YES** | 13. Personal Access Tokens: Revoke |
| **`GET`** | `/api/v1/admin/me/activity`<br>`/api/v1/identity/admin/me/activity` | `getActivity` | `isAuthenticated()` | **YES** | 14. Activity History & Audit Trail: List |
| **`GET`** | `/api/v1/admin/me/activity/export`<br>`/api/v1/identity/admin/me/activity/export` | `exportActivity` | `isAuthenticated()` | **YES** | 15. Activity History & Audit Trail: Export (CSV or JSON) |
| **`GET`** | `/api/v1/admin/me/permissions`<br>`/api/v1/identity/admin/me/permissions` | `getPermissions` | `isAuthenticated()` | **YES** | 16. Granular RBAC & Effective Permissions Inspector |

#### Controller: `IdentityController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/IdentityController.java`
- **Class Base Path(s)**: `/api/v1/identity`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/identity/users` | `listUsers` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | List all user accounts for the given tenant (admin only). |
| **`GET`** | `/api/v1/identity/users/me` | `getCurrentUserProfile` | `isAuthenticated()` | No | Get the profile of the currently authenticated user. |
| **`PUT`** | `/api/v1/identity/users/me` | `updateCurrentUserProfile` | `isAuthenticated()` | No | Update editable profile details for the authenticated user. |
| **`GET`** | `/api/v1/identity/users/sessions` | `getActiveSessions` | `isAuthenticated()` | No | Get active sessions for the authenticated user. |
| **`DELETE`** | `/api/v1/identity/users/sessions/other` | `revokeOtherSessions` | `isAuthenticated()` | No | Invalidate other active sessions for the authenticated user. |
| **`GET`** | `/api/v1/identity/reviewers` | `getReviewers` | *Public / None* | No | List reviewers and SMEs matching a given subject and tenant. |
| **`POST`** | `/api/v1/identity/register` | `register` | *Public / None* | No | Initiate candidate registration. |
| **`POST`** | `/api/v1/identity/verify/email`<br>`/api/v1/identity/auth/verify/email` | `verifyEmail` | *Public / None* | **YES** | Verify Email OTP for candidate. |
| **`POST`** | `/api/v1/identity/verify/mobile`<br>`/api/v1/identity/auth/verify/mobile` | `verifyMobile` | *Public / None* | **YES** | Verify Mobile OTP for candidate. |
| **`GET`** | `/api/v1/identity/verification-status` | `getVerificationStatus` | *Public / None* | No | Get candidate verification status and remaining SMS count. |
| **`POST`** | `/api/v1/identity/resend/email-otp`<br>`/api/v1/identity/otp/resend/email` | `resendEmailOtp` | *Public / None* | **YES** | Resend candidate Email OTP (supports userId and/or email). |
| **`POST`** | `/api/v1/identity/resend/sms-otp` | `resendSmsOtp` | *Public / None* | No | Resend candidate SMS OTP. |
| **`POST`** | `/api/v1/identity/otp/verify`<br>`/api/v1/identity/verify-otp` | `verifyOtp` | *Public / None* | **YES** | Verify OTP and activate the pending account (backward-compatible). |
| **`POST`** | `/api/v1/identity/otp/resend` | `resendOtp` | *Public / None* | No | Resend OTP (backward-compatible). |
| **`POST`** | `/api/v1/identity/auth/token`<br>`/api/v1/identity/auth/login` | `token` | *Public / None* | **YES** | Handler method `token` |
| **`POST`** | `/api/v1/identity/auth/token/refresh`<br>`/api/v1/identity/auth/refresh` | `refreshToken` | *Public / None* | **YES** | Refresh JWT access token using a valid refresh token. |
| **`POST`** | `/api/v1/identity/auth/webauthn` | `webAuthn` | *Public / None* | No | Authenticate using WebAuthn / FIDO2 assertion. |
| **`POST`** | `/api/v1/identity/auth/2fa/setup`<br>`/api/v1/identity/auth/mfa/totp/setup` | `setup2fa` | *Public / None* | **YES** | Initiate TOTP 2FA setup (returns secret, otpauth URI, backup codes). |
| **`GET`** | `/api/v1/identity/auth/2fa/status`<br>`/api/v1/identity/auth/mfa/totp/status` | `get2faStatus` | *Public / None* | **YES** | Check TOTP 2FA configuration status for a user. |
| **`POST`** | `/api/v1/identity/auth/2fa/verify-setup`<br>`/api/v1/identity/auth/mfa/totp/verify-setup` | `verify2faSetup` | *Public / None* | **YES** | Verify and activate TOTP 2FA setup for user. |
| **`POST`** | `/api/v1/identity/auth/2fa/disable`<br>`/api/v1/identity/auth/mfa/totp/disable` | `disable2fa` | *Public / None* | **YES** | Disable TOTP 2FA for authenticated user. |
| **`ALL`** | `/api/v1/identity/auth/change-password` | `changePassword` | `isAuthenticated()` | No | Change password for the authenticated user. |
| **`DELETE`** | `/api/v1/identity/auth/logout` | `logout` | `isAuthenticated()` | No | Logout: revoke active sessions and refresh tokens. |

#### Controller: `RoleController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/RoleController.java`
- **Class Base Path(s)**: `/api/v1/identity/roles`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/identity/roles/definitions` | `listRoles` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | List all role definitions with pagination and search. |
| **`GET`** | `/api/v1/identity/roles/definitions/{roleId}` | `getRole` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Get a specific role definition by ID. |
| **`POST`** | `/api/v1/identity/roles/definitions` | `createRole` | `SUPER_ADMIN` | No | Create a new role definition. |
| **`PUT`** | `/api/v1/identity/roles/definitions/{roleId}` | `updateRole` | `SUPER_ADMIN` | No | Update an existing role definition. |
| **`DELETE`** | `/api/v1/identity/roles/definitions/{roleId}` | `deleteRole` | `SUPER_ADMIN` | No | Delete a role definition. System roles cannot be deleted. |
| **`GET`** | `/api/v1/identity/roles/permissions` | `listPermissions` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | List all available permissions with pagination and search. |
| **`POST`** | `/api/v1/identity/roles/assignments/{userId}` | `manageRole` | `SUPER_ADMIN` | No | Assign or revoke a role for a user (Super_Admin only). |
| **`GET`** | `/api/v1/identity/roles/assignments/{userId}` | `getUserRoles` | `SUPER_ADMIN` | No | List roles for a user (Super_Admin or the user themselves). |

#### Controller: `UserManagementController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/UserManagementController.java`
- **Class Base Path(s)**: `/api/v1/identity/users`
- **Class `@PreAuthorize`**: `"hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')"`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/identity/users` | `createUser` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Create a new user account (admin-initiated). Creates the account in ACTIVE status, bypassing OTP verification. |
| **`PUT`** | `/api/v1/identity/users/{userId}` | `updateUser` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Update an existing user account (admin-initiated). |
| **`DELETE`** | `/api/v1/identity/users/{userId}` | `deactivateUser` | `SECURITY_ADMIN`, `SUPER_ADMIN` | No | Deactivate a user account (admin-initiated). |

#### Controller: `VaultAdminController`
- **File**: `identity-service/src/main/java/com/examplatform/identity/controller/VaultAdminController.java`
- **Class Base Path(s)**: `/api/v1/identity/admin/vault`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/identity/admin/vault/keys/{keyName}/revoke` | `scheduleKeyRevocation` | `SECURITY_ADMIN` | No | Schedule a Vault Transit key for revocation within 60 seconds. Only accessible by Security_Admin role. |
| **`POST`** | `/api/v1/identity/admin/vault/keys/{keyName}/rotate` | `rotateKey` | `SECURITY_ADMIN` | No | Rotate a Vault Transit key immediately. Only accessible by Security_Admin role. |

### 4.10 Module: `notification-service`

#### Controller: `InAppNotificationController`
- **File**: `notification-service/src/main/java/com/examplatform/notification/controller/InAppNotificationController.java`
- **Class Base Path(s)**: `/api/v1/notifications`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/notifications` | `getNotifications` | `isAuthenticated()` | No | Returns paginated notifications for the authenticated user, filtered by role + userId. |
| **`GET`** | `/api/v1/notifications/stream` | `streamNotifications` | `isAuthenticated()` | No | SSE endpoint for real-time push notifications. The client connects and receives notifications as they arrive. |
| **`PATCH`** | `/api/v1/notifications/{id}/read` | `markAsRead` | `isAuthenticated()` | No | Marks a notification as read. |

#### Controller: `NotificationController`
- **File**: `notification-service/src/main/java/com/examplatform/notification/controller/NotificationController.java`
- **Class Base Path(s)**: `/api/v1/notifications`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/notifications/send` | `sendNotification` | `isAuthenticated()` | No | Dispatches a notification across multi-channel endpoints (Email, SMS, WhatsApp, Push, In-App). |
| **`POST`** | `/api/v1/notifications/push/register-token` | `registerDeviceToken` | `isAuthenticated()` | No | Registers a Web PWA or mobile push notification device token. |
| **`GET`** | `/api/v1/notifications/preferences/{userId}` | `getPreferences` | `isAuthenticated()` | No | Retrieves communication channel preferences for a candidate. |
| **`PUT`** | `/api/v1/notifications/preferences/{userId}` | `updatePreferences` | `isAuthenticated()` | No | Updates communication channel preferences for a candidate. |

### 4.11 Module: `paper-generator`

#### Controller: `BlueprintTemplateController`
- **File**: `paper-generator/src/main/java/com/examplatform/papergenerator/controller/BlueprintTemplateController.java`
- **Class Base Path(s)**: `/api/v1/papers/blueprint-templates`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/papers/blueprint-templates` | `list` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `list` |
| **`GET`** | `/api/v1/papers/blueprint-templates/{id}` | `getById` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `getById` |
| **`POST`** | `/api/v1/papers/blueprint-templates/{id}/check-sufficiency` | `checkSufficiency` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Real-time feasibility check: audits a blueprint template against the live question bank. |
| **`POST`** | `/api/v1/papers/blueprint-templates` | `create` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `create` |
| **`PUT`** | `/api/v1/papers/blueprint-templates/{id}` | `update` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `update` |
| **`DELETE`** | `/api/v1/papers/blueprint-templates/{id}` | `delete` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `delete` |

#### Controller: `PaperController`
- **File**: `paper-generator/src/main/java/com/examplatform/papergenerator/controller/PaperController.java`
- **Class Base Path(s)**: `/api/v1/papers`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/papers` | `listPapers` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Lists generated papers with optional filters and pagination. |
| **`GET`** | `/api/v1/papers/{paperId}` | `getPaper` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Retrieves detailed information and summary for a single paper. |
| **`POST`** | `/api/v1/papers/blueprints/check-sufficiency` | `checkBlueprintSufficiency` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Checks blueprint feasibility against the current question bank without generating a paper. Optionally alerts administrators if any rule deficit is detected. |
| **`POST`** | `/api/v1/papers/generate` | `generatePaper` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Submits an async paper generation job. Requires the EXAM_CONTROLLER role. |
| **`POST`** | `/api/v1/papers/validate` | `validatePaper` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Validates a paper JSON document against the expected schema. Returns 200 OK if valid, or 422 Unprocessable Entity with validation errors. |
| **`POST`** | `/api/v1/papers/{paperId}/approve` | `approvePaper` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Approves a paper, encrypts it with a shift-specific key, and anchors to public ledger. Transitions through DRAFT → APPROVED → ENCRYPTED. |
| **`GET`** | `/api/v1/papers/{paperId}/tamper-check` | `checkPaperTampering` | `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Performs a live cryptographic tamper-check on an examination paper. |
| **`GET`** | `/api/v1/papers/public/verify` | `verifyPaperPublic` | *Public / None* | No | Open-access public API endpoint to independently verify an examination paper's cryptographic seal. Validates: Issue #156 |
| **`POST`** | `/api/v1/papers/public/verify/merkle-leaf` | `verifyMerkleLeaf` | *Public / None* | No | Public endpoint to verify that a specific question leaf is cryptographically included in the paper Merkle tree. |
| **`GET`** | `/api/v1/papers/public/{paperId}/merkle-proof/{questionId}` | `getQuestionProof` | *Public / None* | No | Public endpoint to retrieve the Merkle proof steps for a question in a paper. |
| **`POST`** | `/api/v1/papers/{paperId}/translate` | `translatePaper` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Triggers asynchronous batch translation for all questions in an examination paper. Accepts overwrite configuration and publishes translated questions directly for exam delivery. |
| **`GET`** | `/api/v1/papers/{paperId}/translate/{jobId}`<br>`/api/v1/papers/{paperId}/translation-status/{jobId}` | `getPaperTranslationStatus` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | **YES** | Retrieves the current status and progress of a paper translation job. |
| **`GET`** | `/api/v1/papers/{paperId}/translation-status`<br>`/api/v1/papers/{paperId}/translate/status` | `getLatestPaperTranslationStatus` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | **YES** | Retrieves the latest translation status of a paper. |
| **`GET`** | `/api/v1/papers/public/practice` | `getPracticePapers` | *Public / None* | No | Public endpoint for candidates to list approved practice papers for an examination. |

### 4.12 Module: `practice-service`

#### Controller: `AdminPracticeSetController`
- **File**: `practice-service/src/main/java/com/examplatform/practice/controller/AdminPracticeSetController.java`
- **Class Base Path(s)**: `/api/admin/practice/sets`
- **Class `@PreAuthorize`**: `"hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN', 'EXAM_CONTROLLER')"`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/admin/practice/sets` | `listAll` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `listAll` |
| **`POST`** | `/api/admin/practice/sets` | `create` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `create` |
| **`PUT`** | `/api/admin/practice/sets/{id}` | `update` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `update` |
| **`DELETE`** | `/api/admin/practice/sets/{id}` | `delete` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `delete` |
| **`POST`** | `/api/admin/practice/sets/{id}/publish` | `publish` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `publish` |
| **`POST`** | `/api/admin/practice/sets/{id}/unpublish` | `unpublish` | `ADMIN`, `CONTENT_MANAGER`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Handler method `unpublish` |

#### Controller: `PracticeExplanationController`
- **File**: `practice-service/src/main/java/com/examplatform/practice/controller/PracticeExplanationController.java`
- **Class Base Path(s)**: `/api/practice/questions`, `/api/v1/practice/questions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/practice/questions/{questionId}/explanation`<br>`/api/v1/practice/questions/{questionId}/explanation` | `getExplanation` | `isAuthenticated()` | **YES** | Handler method `getExplanation` |

#### Controller: `PracticeSessionController`
- **File**: `practice-service/src/main/java/com/examplatform/practice/controller/PracticeSessionController.java`
- **Class Base Path(s)**: `/api/practice`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/practice/adaptive-sets/generate` | `generateAdaptiveSet` | `isAuthenticated()` | No | Handler method `generateAdaptiveSet` |
| **`POST`** | `/api/practice/sessions` | `startSession` | `isAuthenticated()` | No | Handler method `startSession` |
| **`GET`** | `/api/practice/sessions/{sessionId}` | `getSession` | `isAuthenticated()` | No | Handler method `getSession` |
| **`GET`** | `/api/practice/sessions/{sessionId}/questions` | `getSessionQuestions` | `isAuthenticated()` | No | Handler method `getSessionQuestions` |
| **`PUT`** | `/api/practice/sessions/{sessionId}/response` | `saveResponse` | `isAuthenticated()` | No | Handler method `saveResponse` |
| **`POST`** | `/api/practice/sessions/{sessionId}/submit` | `submitSession` | `isAuthenticated()` | No | Handler method `submitSession` |
| **`GET`** | `/api/practice/sessions/{sessionId}/result` | `getResult` | `isAuthenticated()` | No | Handler method `getResult` |
| **`GET`** | `/api/practice/history` | `getHistory` | `isAuthenticated()` | No | Handler method `getHistory` |

#### Controller: `PracticeSetController`
- **File**: `practice-service/src/main/java/com/examplatform/practice/controller/PracticeSetController.java`
- **Class Base Path(s)**: `/api/practice/sets`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/practice/sets` | `listSets` | `isAuthenticated()` | No | Handler method `listSets` |
| **`GET`** | `/api/practice/sets/{setId}` | `getSet` | `isAuthenticated()` | No | Handler method `getSet` |

### 4.13 Module: `question-bank-service`

#### Controller: `AdminMicroLessonController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/microlesson/controller/AdminMicroLessonController.java`
- **Class Base Path(s)**: `/api/admin/micro-lessons`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/admin/micro-lessons` | `listForReview` | `isAuthenticated()` | No | Lists micro-lessons awaiting review with pagination, optional search, and taxonomy/status filters. |
| **`GET`** | `/api/admin/micro-lessons/stats` | `getStats` | `isAuthenticated()` | No | Aggregated statistics for the Micro-Lesson Moderation Desk header ribbon. |
| **`GET`** | `/api/admin/micro-lessons/filter-options` | `getFilterOptions` | `isAuthenticated()` | No | Retrieves unique subjects, topics, and target exams present in micro-lessons for dynamic dropdown filters. |
| **`GET`** | `/api/admin/micro-lessons/{id}` | `getMicroLessonById` | `isAuthenticated()` | No | Retrieves a single micro-lesson by ID for viewing or editing in the Authoring Studio. |
| **`POST`** | `/api/admin/micro-lessons` | `createMicroLesson` | `isAuthenticated()` | No | Creates a manually authored micro-lesson. |
| **`PUT`** | `/api/admin/micro-lessons/{id}` | `updateMicroLesson` | `isAuthenticated()` | No | Updates an existing micro-lesson by ID and increments its content version. |
| **`DELETE`** | `/api/admin/micro-lessons/{id}` | `deleteMicroLesson` | `isAuthenticated()` | No | Deletes a single micro-lesson by ID. |
| **`POST`** | `/api/admin/micro-lessons/bulk-delete` | `bulkDeleteMicroLessons` | `isAuthenticated()` | No | Bulk deletes multiple micro-lessons by IDs. |
| **`PUT`** | `/api/admin/micro-lessons/{id}/review` | `reviewMicroLesson` | `isAuthenticated()` | No | Approves, rejects, or requests revision for a micro-lesson. |
| **`POST`** | `/api/admin/micro-lessons/generate` | `generateMicroLesson` | `isAuthenticated()` | No | Admin trigger for asynchronous or syllabus subtopic batch generation. |
| **`POST`** | `/api/admin/micro-lessons/batch-generate` | `triggerBatchGenerate` | `isAuthenticated()` | No | Handler method `triggerBatchGenerate` |
| **`GET`** | `/api/admin/micro-lessons/batch-jobs/{batchId}` | `getBatchJobStatus` | `isAuthenticated()` | No | Handler method `getBatchJobStatus` |
| **`GET`** | `/api/admin/micro-lessons/batch-jobs/active` | `getActiveBatchJob` | `isAuthenticated()` | No | Handler method `getActiveBatchJob` |
| **`POST`** | `/api/admin/micro-lessons/batch-jobs/{batchId}/cancel` | `cancelBatchJob` | `isAuthenticated()` | No | Handler method `cancelBatchJob` |

#### Controller: `BatchGenerationController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/BatchGenerationController.java`
- **Class Base Path(s)**: `/api/v1/questions/batch`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/questions/batch` | `submitBatchJob` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Submits a new batch generation job. Returns immediately with the job metadata. The job is processed asynchronously in the background. |
| **`GET`** | `/api/v1/questions/batch/{jobId}` | `getJobStatus` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Gets the status of a specific batch job. |
| **`GET`** | `/api/v1/questions/batch` | `listJobs` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Lists batch jobs for the current tenant with pagination. |
| **`POST`** | `/api/v1/questions/batch/{jobId}/cancel` | `cancelJob` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Cancels a pending or processing batch job. |

#### Controller: `MicroLessonController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/microlesson/controller/MicroLessonController.java`
- **Class Base Path(s)**: `/api/micro-lessons`, `/api/v1/micro-lessons`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/micro-lessons/subtopic/{subtopicId}`<br>`/api/v1/micro-lessons/subtopic/{subtopicId}` | `getPublishedMicroLesson` | `isAuthenticated()` | **YES** | Candidate-facing read-only endpoint returning verified PUBLISHED micro-lesson. Backed by Redis cache (TTL 7 days) for &lt;20ms retrieval latency. |

#### Controller: `PassageController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/PassageController.java`
- **Class Base Path(s)**: `/api/v1/passages`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/passages` | `createPassage` | `QUESTION_AUTHOR` | No | Handler method `createPassage` |
| **`GET`** | `/api/v1/passages` | `listPassages` | `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER` | No | Handler method `listPassages` |
| **`GET`** | `/api/v1/passages/{id}` | `getPassage` | `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER` | No | Handler method `getPassage` |
| **`PUT`** | `/api/v1/passages/{id}` | `updatePassage` | `QUESTION_AUTHOR` | No | Handler method `updatePassage` |
| **`DELETE`** | `/api/v1/passages/{id}` | `deletePassage` | `QUESTION_AUTHOR` | No | Handler method `deletePassage` |
| **`PUT`** | `/api/v1/passages/{id}/submit` | `submitForReview` | `QUESTION_AUTHOR` | No | Handler method `submitForReview` |
| **`PUT`** | `/api/v1/passages/{id}/approve` | `approve` | `APPROVER`, `REVIEWER` | No | Handler method `approve` |
| **`PUT`** | `/api/v1/passages/{id}/reject` | `reject` | `APPROVER`, `REVIEWER` | No | Handler method `reject` |
| **`POST`** | `/api/v1/passages/{id}/transition` | `transition` | `APPROVER`, `REVIEWER` | No | Handler method `transition` |

#### Controller: `QuestionAiController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/QuestionAiController.java`
- **Class Base Path(s)**: `/api/v1/questions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/questions/generate` | `generateQuestions` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Generates questions using AI based on the provided parameters. |
| **`POST`** | `/api/v1/questions/generate/with-samples` | `generateWithSamplesMultipart` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Generates questions with sample document upload (multipart/form-data). Parses PDF/images using tiered ladder and executes conditional complexity triage. |
| **`POST`** | `/api/v1/questions/generate/with-samples` | `generateWithSamplesJson` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Generates questions with sample questions payload in JSON format. |
| **`POST`** | `/api/v1/questions/generate/clarify` | `clarifyRequirements` | `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Interactive requirement elicitation and refinement endpoint. |
| **`POST`** | `/api/v1/questions/embeddings/backfill` | `backfillEmbeddings` | `SUPER_ADMIN` | No | Generates embeddings for all questions that have a null embedding column. |

#### Controller: `QuestionBankAnalyticsController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/QuestionBankAnalyticsController.java`
- **Class Base Path(s)**: *None*

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/questions/analytics/summary` | `getAnalyticsSummary` | *Public / None* | No | REST backup endpoint for analytics summary. GET /api/v1/questions/analytics/summary |

#### Controller: `QuestionController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/QuestionController.java`
- **Class Base Path(s)**: `/api/v1/questions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/questions` | `createQuestion` | `QUESTION_AUTHOR` | No | Create a new question in DRAFT state. Requires QUESTION_AUTHOR role. Content and answerKey are encrypted using envelope encryption. Validates: Requirements 4.1, 4.2, 4.3, 4.5 |
| **`GET`** | `/api/v1/questions` | `listQuestions` | `ADMIN`, `APPROVER`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `TRANSLATOR` | No | List questions for a tenant with optional filtering, sorting, and pagination. Requires QUESTION_AUTHOR, REVIEWER, APPROVER, TRANSLATOR, EXAM_CONTROLLER, or ADMIN role. |
| **`GET`** | `/api/v1/questions/{id}` | `getQuestion` | `ADMIN`, `APPROVER`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `TRANSLATOR` | No | Retrieve a question by ID. Requires QUESTION_AUTHOR, REVIEWER, APPROVER, TRANSLATOR, EXAM_CONTROLLER, or ADMIN role. |
| **`PUT`** | `/api/v1/questions/{id}/submit` | `submitForReview` | `QUESTION_AUTHOR` | No | Submit a DRAFT question for review — transitions to REVIEW state. Requires QUESTION_AUTHOR role. |
| **`POST`** | `/api/v1/questions/{id}/transition` | `transition` | `APPROVER`, `REVIEWER` | No | Generic transition endpoint driven by the transition request payload. Requires REVIEWER or APPROVER role. |
| **`POST`** | `/api/v1/questions/bulk-transition` | `bulkTransition` | `ADMIN`, `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Bulk transition questions to a target state. Enforces FSM and 4-eyes principle per question, processing up to 100 items per batch. |
| **`PUT`** | `/api/v1/questions/{id}/approve` | `approve` | `APPROVER`, `REVIEWER` | No | Approve a question — transitions to APPROVED state. Enforces the Four-Eyes Principle: approver cannot be the author. Requires REVIEWER or APPROVER role. Validates: Requirements 5.2, 5.5 |
| **`PUT`** | `/api/v1/questions/{id}/publish` | `publish` | `ADMIN`, `APPROVER`, `SUPER_ADMIN` | No | Publish an APPROVED question — transitions to PUBLISHED state. Enforces the Four-Eyes Principle: publisher cannot be the reviewer who approved it. Requires APPROVER or ADMIN role. Validates: Requirements 5.4, 5.5 |
| **`PUT`** | `/api/v1/questions/{id}/reject` | `reject` | `APPROVER`, `REVIEWER`, `SUPER_ADMIN` | No | Reject a question with required comments — transitions back to DRAFT state. Requires REVIEWER or APPROVER role. Validates: Requirement 5.3 |
| **`PUT`** | `/api/v1/questions/{id}` | `updateQuestion` | `QUESTION_AUTHOR` | No | Update an existing question. If modified, creates a new QuestionVersion record and clears embedding. Requires QUESTION_AUTHOR role. Validates: Requirements 4.4, 4.6 |
| **`DELETE`** | `/api/v1/questions/{id}` | `deleteQuestion` | `ADMIN`, `QUESTION_AUTHOR`, `SUBJECT_MATTER_EXPERT`, `SUPER_ADMIN` | No | Delete an existing question. Removes the question and cascades cleanup to associated versions and translations. Questions that are in PUBLISHED state or actively referenced by examinations cannot be deleted. Accessible to QUESTION_AUTHOR, SUBJECT_MATTER_EXPERT, ADMIN, or SUPER_ADMIN role. |
| **`GET`** | `/api/v1/questions/{id}/versions` | `getVersions` | `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Retrieve the version history of a question. Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role. Validates: Requirement 4.6 |
| **`GET`** | `/api/v1/questions/search` | `search` | `APPROVER`, `QUESTION_AUTHOR`, `REVIEWER` | No | Search questions using full-text search with optional filters. Requires QUESTION_AUTHOR, REVIEWER, or APPROVER role. Validates: Requirement 19.3 |
| **`GET`** | `/api/v1/questions/{id}/analytics` | `getAnalytics` | `ADMIN`, `EXAM_CONTROLLER` | No | Retrieve analytics and exposure metrics for a question. Requires EXAM_CONTROLLER or ADMIN role. Validates: Requirement 26.5 |
| **`POST`** | `/api/v1/questions/match-blueprint`<br>`/api/v1/questions/blueprint-match` | `matchBlueprint` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | **YES** | Find published questions matching blueprint criteria for Paper Generator. Only PUBLISHED questions (final approver sign-off complete) are returned. Supports both /match-blueprint and /blueprint-match for inter-service clients. Accessible to EXAM_CONTROLLER, ADMIN, SUPER_ADMIN, or unauthenticated internal calls. |
| **`POST`** | `/api/v1/questions/by-ids`<br>`/api/v1/questions/batch-find` | `getQuestionsByIds` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | **YES** | Find questions by a list of UUIDs for Paper Generator review and delivery. Supports both /by-ids and /batch-find for inter-service clients. Accessible to EXAM_CONTROLLER, ADMIN, SUPER_ADMIN, or unauthenticated internal calls. |

#### Controller: `QuestionIOController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/QuestionIOController.java`
- **Class Base Path(s)**: `/api/v1/questions`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/questions/export` | `exportQuestions` | `APPROVER`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Export questions matching the given filters as a downloadable ZIP archive. Large result sets are split into multiple batch files inside the archive. |
| **`POST`** | `/api/v1/questions/import` | `importQuestions` | `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `SUPER_ADMIN` | No | Import questions from a ZIP archive of JSON/CSV batch files. |

#### Controller: `SubjectTopicController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/controller/SubjectTopicController.java`
- **Class Base Path(s)**: `/api/v1/subjects`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/subjects` | `listSubjects` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | List all subjects for the given tenant. |
| **`POST`** | `/api/v1/subjects` | `createSubject` | `ADMIN`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Create a new subject. |
| **`GET`** | `/api/v1/subjects/{subjectId}/topics` | `listTopics` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | List all topics for a given subject. |
| **`POST`** | `/api/v1/subjects/{subjectId}/topics` | `createTopic` | `ADMIN`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Create a new topic under a subject. |
| **`GET`** | `/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics` | `listSubtopics` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | List all subtopics for a given topic. |
| **`POST`** | `/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics` | `createSubtopic` | `ADMIN`, `EXAM_CONTROLLER`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Create a new subtopic under a topic. |
| **`GET`** | `/api/v1/subjects/hierarchy` | `getHierarchy` | `ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `QUESTION_AUTHOR`, `REVIEWER`, `SUPER_ADMIN` | No | Returns the full Subject → Topic → Subtopic tree for populating cascading dropdowns. |

#### Controller: `TranslationController`
- **File**: `question-bank-service/src/main/java/com/examplatform/questionbank/translation/controller/TranslationController.java`
- **Class Base Path(s)**: `/api/v1/translations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/translations/question/{questionId}/auto-translate/{lang}` | `autoTranslate` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | No | Auto-translate a single question into the specified target language using IndicTrans2. POST /api/v1/translations/question/{questionId}/auto-translate/{lang} |
| **`POST`** | `/api/v1/translations/batch/auto-translate` | `startBatchAutoTranslate` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Start an asynchronous batch translation job from English to target language (e.g., Hindi) with automatic upsert and published status. Supports whole-bank, subject filter, or paper questions. POST /api/v1/translations/batch/auto-translate |
| **`GET`** | `/api/v1/translations/batch/{jobId}`<br>`/api/v1/translations/batch/{jobId}/status` | `getBatchJobStatus` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | **YES** | Get the status, progress percentage, and statistics of a batch translation job. GET /api/v1/translations/batch/{jobId} GET /api/v1/translations/batch/{jobId}/status |
| **`GET`** | `/api/v1/translations/batch` | `listBatchJobs` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | List all batch translation jobs for the current tenant with pagination and recent-first sorting. GET /api/v1/translations/batch |
| **`GET`** | `/api/v1/translations/batch/paper/{paperId}` | `listBatchJobsByPaper` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | No | List all batch translation jobs for a specific paper. GET /api/v1/translations/batch/paper/{paperId} |
| **`POST`** | `/api/v1/translations/batch/{jobId}/cancel` | `cancelBatchJob` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Cancel an ongoing batch translation job. POST /api/v1/translations/batch/{jobId}/cancel |
| **`POST`** | `/api/v1/translations/batch/{jobId}/resume` | `resumeBatchJob` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Resume an interrupted, pending, or failed batch translation job. POST /api/v1/translations/batch/{jobId}/resume |
| **`POST`** | `/api/v1/translations` | `requestTranslation` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN`, `TRANSLATOR` | No | Submit a new translation (content + options + explanation) for a question. POST /api/v1/translations |
| **`PUT`** | `/api/v1/translations/{id}` | `resubmitTranslation` | `ADMIN`, `EXAM_CONTROLLER`, `SUPER_ADMIN`, `TRANSLATOR` | No | Resubmit a translation after rejection. PUT /api/v1/translations/{id} |
| **`POST`** | `/api/v1/translations/{id}/approve` | `approveTranslation` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN` | No | Approve a translation. POST /api/v1/translations/{id}/approve |
| **`POST`** | `/api/v1/translations/{id}/reject` | `rejectTranslation` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN` | No | Reject a translation with mandatory reviewer comments. POST /api/v1/translations/{id}/reject |
| **`GET`** | `/api/v1/translations/questions` | `listQuestionsForTranslation` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | No | List questions for translation with optional filtering by target language, translation status, subject, difficulty, etc. GET /api/v1/translations/questions |
| **`GET`** | `/api/v1/translations/question/{questionId}` | `listTranslations` | `ADMIN`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | No | List all translations for a question (all languages and statuses). Used by the admin / translator dashboard. GET /api/v1/translations/question/{questionId} |
| **`GET`** | `/api/v1/translations/question/{questionId}/language/{lang}` | `getApprovedTranslation` | `ADMIN`, `CANDIDATE`, `DELIVERY_SERVICE`, `EXAM_CONTROLLER`, `REVIEWER`, `SUPER_ADMIN`, `TRANSLATOR` | No | Fetch the approved or published translation for a specific question and language. Used by the delivery service when serving a localized exam to a candidate. Returns 404 if no approved/published translation exists yet. GET /api/v1/translations/question/{questionId}/language/{lang} |

### 4.14 Module: `recommendation-service`

#### Controller: `LearningPathController`
- **File**: `recommendation-service/src/main/java/com/examplatform/recommendation/controller/LearningPathController.java`
- **Class Base Path(s)**: `/api/learning-path`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/learning-path/current` | `getCurrentPath` | `isAuthenticated()` | No | Retrieves the candidate's currently active Dynamic Learning Path (DLP). |
| **`POST`** | `/api/learning-path/recalculate` | `recalculate` | `isAuthenticated()` | No | Manually triggers DLP recalculation after diagnostic mock or practice session. |
| **`GET`** | `/api/learning-path/summary` | `getSummary` | `isAuthenticated()` | No | Overview of mastery distribution across subjects and syllabus hierarchy. |
| **`POST`** | `/api/learning-path/milestones/{milestoneId}/complete` | `completeMilestone` | `isAuthenticated()` | No | Marks a Phase 1 remedial micro-lesson milestone as completed. |
| **`POST`** | `/api/learning-path/diagnostic-test/start` | `startDiagnosticTest` | `isAuthenticated()` | No | Synthesizes and launches a multi-topic Baseline Diagnostic Practice Set for cold-start candidates. |
| **`POST`** | `/api/learning-path/adaptive-quiz/start` | `startAdaptiveQuiz` | `isAuthenticated()` | No | Spawns Phase 2 adaptive mini-quiz once Phase 1 remediation is complete. |
| **`POST`** | `/api/learning-path/adaptive-quiz/submit` | `submitAdaptiveQuiz` | `isAuthenticated()` | No | Evaluates Phase 2 adaptive quiz score (promotes to Phase 3 if score >= 70%). |
| **`POST`** | `/api/learning-path/gatekeeper/start` | `startGatekeeperTest` | `isAuthenticated()` | No | Spawns Phase 3 Gatekeeper test once Phase 2 is complete. |
| **`POST`** | `/api/learning-path/gatekeeper/submit` | `submitGatekeeperTest` | `isAuthenticated()` | No | Evaluates Phase 3 Gatekeeper test score (unlocks full mock exam access if score >= 75%). |
| **`GET`** | `/api/learning-path/gatekeeper/status` | `getGatekeeperStatus` | `isAuthenticated()` | No | Checks if candidate is cleared for full mock exams or blocked by a pending Gatekeeper test. |
| **`GET`** | `/api/learning-path/gatekeeper/status/{candidateId}` | `getGatekeeperStatusByCandidateId` | `isAuthenticated()` | No | Internal/admin lookup of candidate Gatekeeper clearance status. |

#### Controller: `RecommendationController`
- **File**: `recommendation-service/src/main/java/com/examplatform/recommendation/controller/RecommendationController.java`
- **Class Base Path(s)**: `/api/recommendations`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/recommendations/latest` | `getLatest` | `isAuthenticated()` | No | Handler method `getLatest` |
| **`POST`** | `/api/recommendations/generate` | `generateRecommendations` | `isAuthenticated()` | No | Handler method `generateRecommendations` |
| **`GET`** | `/api/recommendations/history` | `getHistory` | `isAuthenticated()` | No | Handler method `getHistory` |
| **`POST`** | `/api/recommendations/{id}/dismiss` | `dismiss` | `isAuthenticated()` | No | Handler method `dismiss` |
| **`PATCH`** | `/api/recommendations/study-plan/days/{dayNumber}` | `updateStudyPlanDay` | `isAuthenticated()` | No | Handler method `updateStudyPlanDay` |
| **`GET`** | `/api/recommendations/learner-profile` | `getLearnerProfile` | `isAuthenticated()` | No | Handler method `getLearnerProfile` |
| **`GET`** | `/api/recommendations/learning-path/current` | `getCurrentLearningPath` | `isAuthenticated()` | No | Handler method `getCurrentLearningPath` |
| **`POST`** | `/api/recommendations/learning-path/recalculate` | `recalculateLearningPath` | `isAuthenticated()` | No | Handler method `recalculateLearningPath` |
| **`GET`** | `/api/recommendations/learning-path/summary` | `getLearningPathSummary` | `isAuthenticated()` | No | Handler method `getLearningPathSummary` |

### 4.15 Module: `response-service`

#### Controller: `ResponseController`
- **File**: `response-service/src/main/java/com/examplatform/response/controller/ResponseController.java`
- **Class Base Path(s)**: `/api/v1/responses`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`POST`** | `/api/v1/responses/{sessionId}/save` | `saveResponse` | `CANDIDATE` | No | Save a candidate's response to a question within an active exam session. Persists the response and waits for Kafka acks=all before returning 200 OK. Logs a warning if processing exceeds 150ms (approaching 200ms p99 SLA). |
| **`POST`** | `/api/v1/responses/{sessionId}/bulk-save` | `bulkSave` | `CANDIDATE` | No | Bulk-save offline-buffered responses with deduplication. Reconciles each response against server-side revision state: new responses are persisted, already-saved ones are skipped. |
| **`POST`** | `/api/v1/responses/{sessionId}/submit` | `submitSession` | `CANDIDATE` | No | Submit (finalize) an exam session: marks all responses as final and locks the response set against further modifications. |
| **`GET`** | `/api/v1/responses/{sessionId}/responses` | `getSessionResponses` | `AUDITOR`, `CANDIDATE`, `EVALUATOR` | No | Retrieves full revision history for all responses in an exam session. Accessible by candidates on session resume as well as evaluators and auditors. Ordered by questionId + revisionSequence for audit/evaluation purposes. |

### 4.16 Module: `result-service`

#### Controller: `AnalyticsResultController`
- **File**: `result-service/src/main/java/com/examplatform/result/controller/AnalyticsResultController.java`
- **Class Base Path(s)**: `/api/v1/results/analytics`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/results/analytics/exam/{examId}` | `getExamAnalytics` | `EXAM_CONTROLLER` | No | Retrieves per-question analytics for an exam. Accessible only by EXAM_CONTROLLER role. |

#### Controller: `ResultController`
- **File**: `result-service/src/main/java/com/examplatform/result/controller/ResultController.java`
- **Class Base Path(s)**: `/api/v1/results`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/results/my-results` | `getMyResults` | `ADMIN`, `CANDIDATE`, `SUPER_ADMIN` | No | Retrieves all results for the authenticated candidate. Accessible by CANDIDATE, SUPER_ADMIN, or ADMIN. |
| **`GET`** | `/api/v1/results/candidate/{candidateId}` | `getCandidateResults` | `ADMIN`, `CANDIDATE`, `SUPER_ADMIN` | No | Retrieves all results for a specific candidate. Accessible by CANDIDATE, SUPER_ADMIN, or ADMIN. |
| **`GET`** | `/api/v1/results/{candidateId}` | `getResult` | `CANDIDATE`, `SUPER_ADMIN` | No | Retrieves the result for a specific candidate in an exam. Accessible by the candidate themselves (CANDIDATE role) or SUPER_ADMIN. |
| **`POST`** | `/api/v1/results/compute` | `computeResults` | `EXAM_CONTROLLER` | No | Triggers result computation for an exam. Accessible only by EXAM_CONTROLLER role. |
| **`POST`** | `/api/v1/results/{candidateId}/publish` | `publishResult` | `EXAM_CONTROLLER` | No | Publishes a candidate's result, triggering DigiLocker push and notification. Accessible only by EXAM_CONTROLLER role. |
| **`POST`** | `/api/v1/results/{id}/digilocker/push` | `pushToDigiLocker` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Pushes a scorecard to DigiLocker for a specific result or candidate. Accessible by CANDIDATE, SUPER_ADMIN, ADMIN, or EXAM_CONTROLLER. |

#### Controller: `ReviewController`
- **File**: `result-service/src/main/java/com/examplatform/result/controller/ReviewController.java`
- **Class Base Path(s)**: `/api/v1/results`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/results/{candidateId}/review` | `getExamReview` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Returns the full post-exam review for a candidate in an exam. Includes each question, candidate's answer, correct answer, explanation, and peer statistics. Access: CANDIDATE role (own results only), EXAM_CONTROLLER, ADMIN, SUPER_ADMIN can view any. |
| **`GET`** | `/api/v1/results/verify` | `verifyScorecard` | *Public / None* | No | Public QR code verification endpoint. Returns redacted result information (name-less) for a given QR verification code. |

#### Controller: `ScorecardController`
- **File**: `result-service/src/main/java/com/examplatform/result/controller/ScorecardController.java`
- **Class Base Path(s)**: `/api/v1/results`

| HTTP Method | Effective Path(s) | Method Name | Required Roles / Security | Has Alias? | Purpose / Summary |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **`GET`** | `/api/v1/results/{id}/scorecard` | `getScorecard` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Retrieves scorecard presigned URL or direct binary stream for a candidate or result ID. <p>If the client requests {@code Accept: application/pdf} or provides {@code download=true}, the binary PDF stream is returned directly. Otherwise, returns a JSON response containing a secure, 15-minute presigned download URL. |
| **`GET`** | `/api/v1/results/{id}/scorecard/presigned` | `getPresignedUrl` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Explicit endpoint to retrieve a 15-minute presigned download URL for a candidate or result. |
| **`GET`** | `/api/v1/results/{id}/scorecard/download` | `downloadDirect` | `ADMIN`, `CANDIDATE`, `EXAM_CONTROLLER`, `SUPER_ADMIN` | No | Direct binary download endpoint for scorecard PDF. |
