# Backend Canonical Roles & Consolidated API Endpoints

This document provides the consolidated inventory of backend authentication roles and API endpoints across the NextGen Assessment Grid (NAG) platform, reflecting the complete cleanup of route aliases and non-canonical roles on branch `role-api-aliase-cleanup`.

---

## 1. Canonical Roles in `identity-service`

The authoritative source of truth for platform roles is defined in:
`com.examplatform.identity.domain.enums.UserRole`

| Canonical Role | Description / Purpose in NAG Platform | Status |
| :--- | :--- | :--- |
| **`SUPER_ADMIN`** | Global platform administrator with unrestricted superuser privileges across tenants. | **Active / Canonical** |
| **`SECURITY_ADMIN`** | Security operations, audit log access, MFA policy management, admin invitations, Keycloak & Vault administration. | **Active / Canonical** |
| **`EXAM_CONTROLLER`** | End-to-end examination management: creating exams, scheduling centers, publishing exams, triggering translations, paper generation. | **Active / Canonical** |
| **`QUESTION_AUTHOR`** | Authoring questions, passages, subject hierarchies, AI draft refinements, and media asset uploads. | **Active / Canonical** |
| **`REVIEWER`** | First-line peer review, technical validation, and linguistic inspection of authored questions and translations. | **Active / Canonical** |
| **`SUBJECT_MATTER_EXPERT`** | Domain-specific deep curriculum and factual verification for questions and blueprints. | **Active / Canonical** |
| **`APPROVER`** | Final sign-off authority approving reviewed questions and translations into the published item bank. | **Active / Canonical** |
| **`TRANSLATOR`** | Translating question items into regional and national languages, reviewing machine translations. | **Active / Canonical** |
| **`EVALUATOR`** | Scoring subjective and rubric-based candidate responses. | **Active / Canonical** |
| **`AUDITOR`** | Read-only compliance inspection of platform audit trails, cryptographic event logs, and forensic chains. | **Active / Canonical** |
| **`CANDIDATE`** | Test-taker role for authentication, profile management, examination sitting, response submission, and scorecard retrieval. | **Active / Canonical** |

---

## 2. Eliminated Non-Canonical Roles (Ghost Roles)

The following roles previously appeared in scattered controller `@PreAuthorize` annotations and have been consolidated into their canonical equivalents:

| Deprecated / Non-Existent Role | Canonical Replacement | Reason for Removal |
| :--- | :--- | :--- |
| `ADMIN` | `SUPER_ADMIN` / `SECURITY_ADMIN` | Ambiguous legacy role; does not exist in `UserRole` enum. Replaced with explicit `SUPER_ADMIN` or `SECURITY_ADMIN`. |
| `CONTENT_MANAGER` | `QUESTION_AUTHOR` / `APPROVER` / `SUPER_ADMIN` | Legacy role; content operations belong to Question Author and Approver workflows. |
| `CENTER_ADMIN` | `EXAM_CONTROLLER` / `SUPER_ADMIN` | Center operations are managed under `EXAM_CONTROLLER` purview in DPI model. |
| `FACULTY` | `QUESTION_AUTHOR` / `EXAM_CONTROLLER` | Academic authoring maps to `QUESTION_AUTHOR`; faculty administration maps to `EXAM_CONTROLLER`. |
| `TENANT_ADMIN` | `SECURITY_ADMIN` / `SUPER_ADMIN` | Tenant administration is handled by `SECURITY_ADMIN` within tenant boundaries. |
| `DELIVERY_SERVICE` | Machine client / mTLS / canonical roles | Service-to-service communication is not a user role; endpoints updated to canonical permissions. |

---

## 3. Eliminated Route Aliases

All duplicate/alias path mappings across controllers have been standardized to single, unambiguous canonical REST routes:

| Service | Controller | Previous Aliases | Canonical Route |
| :--- | :--- | :--- | :--- |
| **paper-generator** | `PaperController` | `/{paperId}/translate/{jobId}`, `/{paperId}/translation-status/{jobId}` | `/{paperId}/translate/{jobId}` |
| **paper-generator** | `PaperController` | `/{paperId}/translation-status`, `/{paperId}/translations` | `/{paperId}/translation-status` |
| **question-bank-service** | `QuestionController` | `/match-blueprint`, `/blueprint-match` | `/match-blueprint` |
| **question-bank-service** | `QuestionController` | `/by-ids`, `/batch-find` | `/by-ids` |
| **question-bank-service** | `TranslationController` | `/batch/{jobId}`, `/batch/{jobId}/status` | `/batch/{jobId}` |
| **question-bank-service** | `MicroLessonController` | `{"/api/micro-lessons", "/api/v1/micro-lessons"}` | `/api/micro-lessons` |
| **candidate-service** | `CandidateProfileController` | `{"/api/v1/candidates", "/api/v1/candidate/profile", "/api/v1/candidate"}` | `/api/v1/candidates` |
| **candidate-service** | `CandidateProfileController` | `/{userId}/digilocker/auth`, `/{userId}/digilocker/authorize` | `/{userId}/digilocker/auth` |
| **identity-service** | `AdminInvitationController` | `{"/api/v1/identity/invitations", "/api/v1/identity/admin/invitations"}` | `/api/v1/identity/invitations` |
| **identity-service** | `AdminProfileController` | `{"/api/v1/identity/admin/me", "/api/v1/identity/users/me/admin"}` | `/api/v1/identity/admin/me` |
| **identity-service** | `AdminProfileController` | `/security/mfa/verify`, `/security/mfa/confirm` | `/security/mfa/verify` |
| **identity-service** | `IdentityController` | `{"/auth/token", "/login"}` | `/auth/token` |
| **identity-service** | `IdentityController` | `{"/auth/token/refresh", "/token/refresh", "/refresh"}` | `/auth/token/refresh` |
| **identity-service** | `IdentityController` | `{"/verify/email", "/email/verify"}` | `/verify/email` |
| **identity-service** | `IdentityController` | `{"/verify/mobile", "/mobile/verify"}` | `/verify/mobile` |
| **identity-service** | `IdentityController` | `{"/resend/email-otp", "/email/resend-otp"}` | `/resend/email-otp` |
| **identity-service** | `IdentityController` | `{"/otp/verify", "/verify-otp"}` | `/otp/verify` |
| **identity-service** | `IdentityController` | `{"/auth/2fa/setup", "/2fa/setup"}` | `/auth/2fa/setup` |
| **identity-service** | `IdentityController` | `{"/auth/2fa/status", "/2fa/status"}` | `/auth/2fa/status` |
| **identity-service** | `IdentityController` | `{"/auth/2fa/verify-setup", "/2fa/verify-setup"}` | `/auth/2fa/verify-setup` |
| **identity-service** | `IdentityController` | `{"/auth/2fa/disable", "/2fa/disable"}` | `/auth/2fa/disable` |
| **practice-service** | `PracticeExplanationController` | `{"/api/practice/questions", "/api/v1/practice/questions"}` | `/api/practice/questions` |

---

## 4. Consolidated Endpoints Inventory

Total endpoints mapped: `267` across `54` controllers.

| Service | Controller | HTTP Verb | Path | Handler Method | Access Control (@PreAuthorize) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `admin-service` | `AdminConfigController.java` | **GET** | `/api/v1/admin/config/map` | `getConfigMap` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `admin-service` | `AdminConfigController.java` | **PUT** | `/api/v1/admin/config/bulk` | `updateBulkConfig` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `admin-service` | `AdminConfigController.java` | **POST** | `/api/v1/admin/config/reset` | `resetConfigToDefaults` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `admin-service` | `AdminDashboardController.java` | **GET** | `/api/v1/admin/dashboard/summary` | `getDashboardSummary` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR', 'REVIEWER', 'EVALUATOR')` |
| `analytics-service` | `AnalyticsController.java` | **GET** | `/api/v1/analytics/exams/{id}` | `getExamAnalytics` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `analytics-service` | `AnalyticsController.java` | **POST** | `/api/v1/analytics/exams/{id}/compute` | `computeExamAnalytics` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `analytics-service` | `AnalyticsController.java` | **GET** | `/api/v1/analytics/exams/{id}/export` | `exportExamAnalytics` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **POST** | `/api/v1/assets/{id}/content` | `replaceContent` | `hasAnyRole('QUESTION_AUTHOR', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **GET** | `/api/v1/assets/{id}` | `getAsset` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'CANDIDATE', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **GET** | `/api/v1/assets/search` | `searchAssets` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **PUT** | `/api/v1/assets/{id}/metadata` | `updateMetadata` | `hasAnyRole('QUESTION_AUTHOR', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **POST** | `/api/v1/assets/references` | `addReference` | `hasAnyRole('QUESTION_AUTHOR', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **GET** | `/api/v1/assets/{id}/references` | `getReferences` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **DELETE** | `/api/v1/assets/references/{refId}` | `removeReference` | `hasRole('SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **DELETE** | `/api/v1/assets/{id}` | `deleteAsset` | `hasRole('SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **GET** | `/api/v1/assets/{id}/download` | `downloadAsset` | `None / PermitAll` |
| `asset-service` | `AssetController.java` | **GET** | `/api/v1/assets/{id}/url` | `getPublicUrl` | `None / PermitAll` |
| `asset-service` | `AssetController.java` | **PUT** | `/api/v1/assets/{id}/archive` | `archiveAsset` | `hasRole('SUPER_ADMIN')` |
| `asset-service` | `AssetController.java` | **PUT** | `/api/v1/assets/{id}/restore` | `restoreAsset` | `hasRole('SUPER_ADMIN')` |
| `audit-service` | `AuditAnalyticsController.java` | **GET** | `/api/v1/audit/events/recent` | `getRecentEvents` | `None / PermitAll` |
| `audit-service` | `AuditEventController.java` | **PUT** | `/api/v1/audit/events/{id}` | `rejectUpdate` | `None / PermitAll` |
| `audit-service` | `AuditEventController.java` | **DELETE** | `/api/v1/audit/events/{id}` | `rejectDelete` | `None / PermitAll` |
| `candidate-service` | `CandidateAnalyticsController.java` | **GET** | `/api/v1/candidates/analytics/summary` | `getSummary` | `None / PermitAll` |
| `candidate-service` | `CandidateProfileController.java` | **GET** | `/api/v1/candidates/{userId}` | `getByUserId` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `candidate-service` | `CandidateProfileController.java` | **PUT** | `/api/v1/candidates/{userId}` | `update` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **DELETE** | `/api/v1/candidates/{userId}/pii` | `erasePii` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `candidate-service` | `CandidateProfileController.java` | **GET** | `/api/v1/candidates/{userId}/education` | `getEducation` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `candidate-service` | `CandidateProfileController.java` | **POST** | `/api/v1/candidates/{userId}/education` | `addEducation` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **PUT** | `/api/v1/candidates/{userId}/education/{educationId}` | `updateEducation` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **DELETE** | `/api/v1/candidates/{userId}/education/{educationId}` | `deleteEducation` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **POST** | `/api/v1/candidates/{userId}/consent` | `recordConsent` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **GET** | `/api/v1/candidates/{userId}/digilocker/auth` | `initiateDigiLockerAuth` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **GET** | `/api/v1/candidates/digilocker/callback` | `handleDigiLockerCallbackGet` | `None / PermitAll` |
| `candidate-service` | `CandidateProfileController.java` | **POST** | `/api/v1/candidates/digilocker/callback` | `handleDigiLockerCallbackPost` | `None / PermitAll` |
| `candidate-service` | `CandidateProfileController.java` | **POST** | `/api/v1/candidates/{userId}/digilocker/verify` | `verifyDigiLocker` | `hasRole('CANDIDATE')` |
| `candidate-service` | `CandidateProfileController.java` | **POST** | `/api/v1/candidates/{userId}/face/verify` | `verifyFace` | `hasRole('CANDIDATE')` |
| `delivery-service` | `NavigationController.java` | **POST** | `/api/v1/sessions/{sessionId}/navigate` | `navigate` | `hasRole('CANDIDATE')` |
| `delivery-service` | `OfflineDeliveryController.java` | **POST** | `/api/v1/sessions/{sessionId}/offline/preload` | `preloadExamPackage` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `delivery-service` | `OfflineDeliveryController.java` | **POST** | `/api/v1/sessions/{sessionId}/offline/reconcile` | `reconcileOnReconnect` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `delivery-service` | `ProctoringController.java` | **POST** | `/api/v1/sessions/{sessionId}/proctoring/snapshot` | `captureSnapshot` | `hasRole('CANDIDATE')` |
| `delivery-service` | `ProctoringController.java` | **POST** | `/api/v1/sessions/{sessionId}/proctoring/fullscreen-exit` | `recordFullScreenExit` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionController.java` | **POST** | `/api/v1/sessions/start` | `startSession` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionController.java` | **GET** | `/api/v1/sessions/paper/{paperId}/questions` | `getPaperQuestions` | `None / PermitAll` |
| `delivery-service` | `SessionController.java` | **POST** | `/api/v1/sessions/terminate-active` | `terminateActiveSessions` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionController.java` | **POST** | `/api/v1/sessions/{sessionId}/resume` | `resumeSession` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionController.java` | **GET** | `/api/v1/sessions/{sessionId}/questions` | `getSessionQuestions` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionController.java` | **GET** | `/api/v1/sessions/{sessionId}/questions/{sequenceNumber}` | `getSessionQuestion` | `hasRole('CANDIDATE')` |
| `delivery-service` | `SessionStatusController.java` | **GET** | `/api/v1/sessions/{sessionId}/status` | `getSessionStatus` | `hasRole('CANDIDATE')` |
| `evaluation-service` | `EvaluationAnalyticsController.java` | **GET** | `/api/v1/evaluation/analytics/summary` | `getAnalyticsSummary` | `None / PermitAll` |
| `evaluation-service` | `EvaluationController.java` | **POST** | `/api/v1/evaluations/{id}/score` | `recordScore` | `hasAnyRole('EVALUATOR', 'EXAM_CONTROLLER')` |
| `evaluation-service` | `EvaluationController.java` | **POST** | `/api/v1/evaluations/aggregate` | `aggregateScores` | `hasAnyRole('EVALUATOR', 'EXAM_CONTROLLER')` |
| `evaluation-service` | `EvaluationController.java` | **GET** | `/api/v1/evaluations/candidate/{candidateId}` | `getCandidateEvaluations` | `hasAnyRole('CANDIDATE', 'EVALUATOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `examination-service` | `ExaminationAnalyticsController.java` | **GET** | `/api/v1/examinations/analytics/summary` | `getAnalyticsSummary` | `None / PermitAll` |
| `examination-service` | `ExaminationCentreController.java` | **POST** | `/api/v1/examinations/centres` | `createCentre` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationCentreController.java` | **GET** | `/api/v1/examinations/centres` | `listCentres` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `ExaminationCentreController.java` | **GET** | `/api/v1/examinations/centres/{centreId}` | `getCentre` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `ExaminationCentreController.java` | **PUT** | `/api/v1/examinations/centres/{centreId}/deactivate` | `deactivateCentre` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationCentreController.java` | **POST** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | `upsertAllocation` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationCentreController.java` | **GET** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | `listAllocations` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `ExaminationController.java` | **PUT** | `/api/v1/examinations/{examId}` | `update` | `hasRole('EXAM_CONTROLLER')` |
| `examination-service` | `ExaminationController.java` | **POST/PUT** | `/api/v1/examinations/{examId}/publish` | `publish` | `hasRole('EXAM_CONTROLLER')` |
| `examination-service` | `ExaminationController.java` | **GET** | `/api/v1/examinations/{examId}` | `getById` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN', 'QUESTION_AUTHOR', 'REVIEWER')` |
| `examination-service` | `ExaminationScheduleController.java` | **GET** | `/api/v1/examinations/{examId}/schedules/{scheduleId}` | `getById` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `ExaminationScheduleController.java` | **PUT** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/transition` | `transition` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `ExaminationScheduleController.java` | **PUT** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/amend` | `amend` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationScheduleController.java` | **POST** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts` | `addShift` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationScheduleController.java` | **PUT** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}` | `updateShift` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `examination-service` | `ExaminationScheduleController.java` | **GET** | `/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts` | `listShifts` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN','SECURITY_ADMIN')` |
| `examination-service` | `GeoLocationController.java` | **GET** | `/api/v1/geo/countries` | `getCountries` | `None / PermitAll` |
| `examination-service` | `GeoLocationController.java` | **GET** | `/api/v1/geo/countries/{countryId}/states` | `getStates` | `None / PermitAll` |
| `examination-service` | `GeoLocationController.java` | **GET** | `/api/v1/geo/states/{stateId}/cities` | `getCities` | `None / PermitAll` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/public` | `listPublished` | `None / PermitAll` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/public/{examId}` | `getPublishedById` | `None / PermitAll` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/centres/public` | `listPublicCentres` | `None / PermitAll` |
| `examination-service` | `PublicExaminationController.java` | **POST** | `/api/v1/examinations/{examId}/apply` | `apply` | `hasRole('CANDIDATE')` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/my-exams` | `getMyApplications` | `hasRole('CANDIDATE')` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/{examId}/my-application` | `getMyApplication` | `hasRole('CANDIDATE')` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/{examId}/admit-card` | `getAdmitCard` | `hasRole('CANDIDATE')` |
| `examination-service` | `PublicExaminationController.java` | **GET** | `/api/v1/examinations/applications/{applicationId}/admit-card` | `getAdmitCardByApplicationId` | `hasRole('CANDIDATE') or hasRole('EXAM_CONTROLLER') or hasRole('SUPER_ADMIN')` |
| `identity-service` | `AdminInvitationController.java` | **GET** | `/api/v1/identity/invitations/validate` | `validateInvite` | `None / PermitAll` |
| `identity-service` | `AdminInvitationController.java` | **POST** | `/api/v1/identity/invitations/accept` | `acceptInvite` | `None / PermitAll` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/profile` | `getProfile` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **PUT** | `/api/v1/identity/admin/me/profile` | `updateProfile` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **POST/PUT** | `/api/v1/identity/admin/me/security/password` | `changePassword` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **POST** | `/api/v1/identity/admin/me/security/mfa/setup` | `setupMfa` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **POST** | `/api/v1/identity/admin/me/security/mfa/verify` | `verifyMfa` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/security/mfa/status` | `getMfaStatus` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **POST** | `/api/v1/identity/admin/me/security/mfa/disable` | `disableMfa` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/sessions` | `getSessions` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **DELETE** | `/api/v1/identity/admin/me/sessions/{sessionId}` | `revokeSession` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **DELETE** | `/api/v1/identity/admin/me/sessions/other` | `revokeOtherSessions` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/tokens` | `listTokens` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **POST** | `/api/v1/identity/admin/me/tokens` | `createToken` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **DELETE** | `/api/v1/identity/admin/me/tokens/{tokenId}` | `revokeToken` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/activity` | `getActivity` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/activity/export` | `exportActivity` | `isAuthenticated()` |
| `identity-service` | `AdminProfileController.java` | **GET** | `/api/v1/identity/admin/me/permissions` | `getPermissions` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/users` | `listUsers` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/users/me` | `getCurrentUserProfile` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **PUT** | `/api/v1/identity/users/me` | `updateCurrentUserProfile` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/users/sessions` | `getActiveSessions` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **DELETE** | `/api/v1/identity/users/sessions/other` | `revokeOtherSessions` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/reviewers` | `getReviewers` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/register` | `register` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/verify/email` | `verifyEmail` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/verify/mobile` | `verifyMobile` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/verification-status` | `getVerificationStatus` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/resend/email-otp` | `resendEmailOtp` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/resend/sms-otp` | `resendSmsOtp` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/otp/verify` | `verifyOtp` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/otp/resend` | `resendOtp` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/token` | `token` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/token/refresh` | `refreshToken` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/webauthn` | `webAuthn` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/2fa/setup` | `setup2fa` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **GET** | `/api/v1/identity/auth/2fa/status` | `get2faStatus` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/2fa/verify-setup` | `verify2faSetup` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST** | `/api/v1/identity/auth/2fa/disable` | `disable2fa` | `None / PermitAll` |
| `identity-service` | `IdentityController.java` | **POST/PUT** | `/api/v1/identity/auth/change-password` | `changePassword` | `isAuthenticated()` |
| `identity-service` | `IdentityController.java` | **DELETE** | `/api/v1/identity/auth/logout` | `logout` | `isAuthenticated()` |
| `identity-service` | `RoleController.java` | **GET** | `/api/v1/identity/roles/definitions` | `listRoles` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `RoleController.java` | **GET** | `/api/v1/identity/roles/definitions/{roleId}` | `getRole` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `RoleController.java` | **POST** | `/api/v1/identity/roles/definitions` | `createRole` | `hasRole('SUPER_ADMIN')` |
| `identity-service` | `RoleController.java` | **PUT** | `/api/v1/identity/roles/definitions/{roleId}` | `updateRole` | `hasRole('SUPER_ADMIN')` |
| `identity-service` | `RoleController.java` | **DELETE** | `/api/v1/identity/roles/definitions/{roleId}` | `deleteRole` | `hasRole('SUPER_ADMIN')` |
| `identity-service` | `RoleController.java` | **GET** | `/api/v1/identity/roles/permissions` | `listPermissions` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `RoleController.java` | **POST** | `/api/v1/identity/roles/assignments/{userId}` | `manageRole` | `hasRole('SUPER_ADMIN')` |
| `identity-service` | `RoleController.java` | **GET** | `/api/v1/identity/roles/assignments/{userId}` | `getUserRoles` | `hasRole('SUPER_ADMIN') or #userId.toString() == authentication.name` |
| `identity-service` | `UserManagementController.java` | **PUT** | `/api/v1/identity/users/{userId}` | `updateUser` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `UserManagementController.java` | **DELETE** | `/api/v1/identity/users/{userId}` | `deactivateUser` | `hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')` |
| `identity-service` | `VaultAdminController.java` | **POST** | `/api/v1/identity/admin/vault/keys/{keyName}/revoke` | `scheduleKeyRevocation` | `hasRole('SECURITY_ADMIN')` |
| `identity-service` | `VaultAdminController.java` | **POST** | `/api/v1/identity/admin/vault/keys/{keyName}/rotate` | `rotateKey` | `hasRole('SECURITY_ADMIN')` |
| `notification-service` | `InAppNotificationController.java` | **GET** | `/api/v1/notifications/stream` | `streamNotifications` | `isAuthenticated()` |
| `notification-service` | `InAppNotificationController.java` | **PATCH** | `/api/v1/notifications/{id}/read` | `markAsRead` | `isAuthenticated()` |
| `notification-service` | `NotificationController.java` | **POST** | `/api/v1/notifications/send` | `sendNotification` | `isAuthenticated()` |
| `notification-service` | `NotificationController.java` | **POST** | `/api/v1/notifications/push/register-token` | `registerDeviceToken` | `isAuthenticated()` |
| `notification-service` | `NotificationController.java` | **GET** | `/api/v1/notifications/preferences/{userId}` | `getPreferences` | `isAuthenticated()` |
| `notification-service` | `NotificationController.java` | **PUT** | `/api/v1/notifications/preferences/{userId}` | `updatePreferences` | `isAuthenticated()` |
| `paper-generator` | `BlueprintTemplateController.java` | **GET** | `/api/v1/papers/blueprint-templates/{id}` | `getById` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `BlueprintTemplateController.java` | **POST** | `/api/v1/papers/blueprint-templates/{id}/check-sufficiency` | `checkSufficiency` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `BlueprintTemplateController.java` | **PUT** | `/api/v1/papers/blueprint-templates/{id}` | `update` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `BlueprintTemplateController.java` | **DELETE** | `/api/v1/papers/blueprint-templates/{id}` | `delete` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/{paperId}` | `getPaper` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/blueprints/check-sufficiency` | `checkBlueprintSufficiency` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/generate` | `generatePaper` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/validate` | `validatePaper` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/{paperId}/approve` | `approvePaper` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/{paperId}/tamper-check` | `checkPaperTampering` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/public/verify` | `verifyPaperPublic` | `None / PermitAll` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/public/verify/merkle-leaf` | `verifyMerkleLeaf` | `None / PermitAll` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/public/{paperId}/merkle-proof/{questionId}` | `getQuestionProof` | `None / PermitAll` |
| `paper-generator` | `PaperController.java` | **POST** | `/api/v1/papers/{paperId}/translate` | `translatePaper` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/{paperId}/translate/{jobId}` | `getPaperTranslationStatus` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/{paperId}/translation-status` | `getLatestPaperTranslationStatus` | `hasAnyRole('EXAM_CONTROLLER','SUPER_ADMIN')` |
| `paper-generator` | `PaperController.java` | **GET** | `/api/v1/papers/public/practice` | `getPracticePapers` | `None / PermitAll` |
| `practice-service` | `AdminPracticeSetController.java` | **PUT** | `/api/admin/practice/sets/{id}` | `update` | `hasAnyRole('SUPER_ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR')` |
| `practice-service` | `AdminPracticeSetController.java` | **DELETE** | `/api/admin/practice/sets/{id}` | `delete` | `hasAnyRole('SUPER_ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR')` |
| `practice-service` | `AdminPracticeSetController.java` | **POST** | `/api/admin/practice/sets/{id}/publish` | `publish` | `hasAnyRole('SUPER_ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR')` |
| `practice-service` | `AdminPracticeSetController.java` | **POST** | `/api/admin/practice/sets/{id}/unpublish` | `unpublish` | `hasAnyRole('SUPER_ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR')` |
| `practice-service` | `PracticeExplanationController.java` | **GET** | `/api/practice/questions/{questionId}/explanation` | `getExplanation` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **POST** | `/api/practice/adaptive-sets/generate` | `generateAdaptiveSet` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **POST** | `/api/practice/sessions` | `startSession` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **GET** | `/api/practice/sessions/{sessionId}` | `getSession` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **GET** | `/api/practice/sessions/{sessionId}/questions` | `getSessionQuestions` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **PUT** | `/api/practice/sessions/{sessionId}/response` | `saveResponse` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **POST** | `/api/practice/sessions/{sessionId}/submit` | `submitSession` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **GET** | `/api/practice/sessions/{sessionId}/result` | `getResult` | `isAuthenticated()` |
| `practice-service` | `PracticeSessionController.java` | **GET** | `/api/practice/history` | `getHistory` | `isAuthenticated()` |
| `practice-service` | `PracticeSetController.java` | **GET** | `/api/practice/sets/{setId}` | `getSet` | `isAuthenticated()` |
| `question-bank-service` | `BatchGenerationController.java` | **GET** | `/api/v1/questions/batch/{jobId}` | `getJobStatus` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `BatchGenerationController.java` | **POST** | `/api/v1/questions/batch/{jobId}/cancel` | `cancelJob` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `PassageController.java` | **GET** | `/api/v1/passages/{id}` | `getPassage` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')` |
| `question-bank-service` | `PassageController.java` | **PUT** | `/api/v1/passages/{id}` | `updatePassage` | `hasRole('QUESTION_AUTHOR')` |
| `question-bank-service` | `PassageController.java` | **DELETE** | `/api/v1/passages/{id}` | `deletePassage` | `hasRole('QUESTION_AUTHOR')` |
| `question-bank-service` | `PassageController.java` | **PUT** | `/api/v1/passages/{id}/submit` | `submitForReview` | `hasRole('QUESTION_AUTHOR')` |
| `question-bank-service` | `PassageController.java` | **PUT** | `/api/v1/passages/{id}/approve` | `approve` | `hasAnyRole('REVIEWER', 'APPROVER')` |
| `question-bank-service` | `PassageController.java` | **PUT** | `/api/v1/passages/{id}/reject` | `reject` | `hasAnyRole('REVIEWER', 'APPROVER')` |
| `question-bank-service` | `PassageController.java` | **POST** | `/api/v1/passages/{id}/transition` | `transition` | `hasAnyRole('REVIEWER', 'APPROVER')` |
| `question-bank-service` | `QuestionAiController.java` | **POST** | `/api/v1/questions/generate` | `generateQuestions` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionAiController.java` | **POST** | `/api/v1/questions/generate/with-samples` | `generateWithSamplesMultipart` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionAiController.java` | **POST** | `/api/v1/questions/generate/with-samples` | `generateWithSamplesJson` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionAiController.java` | **POST** | `/api/v1/questions/generate/clarify` | `clarifyRequirements` | `hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionAiController.java` | **POST** | `/api/v1/questions/embeddings/backfill` | `backfillEmbeddings` | `hasRole('SUPER_ADMIN')` |
| `question-bank-service` | `QuestionBankAnalyticsController.java` | **GET** | `/api/v1/questions/analytics/summary` | `getAnalyticsSummary` | `None / PermitAll` |
| `question-bank-service` | `QuestionController.java` | **GET** | `/api/v1/questions/{id}` | `getQuestion` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'TRANSLATOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **PUT** | `/api/v1/questions/{id}/submit` | `submitForReview` | `hasRole('QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionController.java` | **POST** | `/api/v1/questions/{id}/transition` | `transition` | `hasAnyRole('REVIEWER', 'APPROVER')` |
| `question-bank-service` | `QuestionController.java` | **POST** | `/api/v1/questions/bulk-transition` | `bulkTransition` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **PUT** | `/api/v1/questions/{id}/approve` | `approve` | `hasAnyRole('REVIEWER', 'APPROVER')` |
| `question-bank-service` | `QuestionController.java` | **PUT** | `/api/v1/questions/{id}/publish` | `publish` | `hasAnyRole('APPROVER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **PUT** | `/api/v1/questions/{id}/reject` | `reject` | `hasAnyRole('REVIEWER', 'APPROVER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **PUT** | `/api/v1/questions/{id}` | `updateQuestion` | `hasRole('QUESTION_AUTHOR')` |
| `question-bank-service` | `QuestionController.java` | **DELETE** | `/api/v1/questions/{id}` | `deleteQuestion` | `hasAnyRole('QUESTION_AUTHOR', 'SUBJECT_MATTER_EXPERT', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **GET** | `/api/v1/questions/{id}/versions` | `getVersions` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **GET** | `/api/v1/questions/search` | `search` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER')` |
| `question-bank-service` | `QuestionController.java` | **GET** | `/api/v1/questions/{id}/analytics` | `getAnalytics` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **POST** | `/api/v1/questions/match-blueprint` | `matchBlueprint` | `permitAll() or hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionController.java` | **POST** | `/api/v1/questions/by-ids` | `getQuestionsByIds` | `permitAll() or hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionIOController.java` | **GET** | `/api/v1/questions/export` | `exportQuestions` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `QuestionIOController.java` | **POST** | `/api/v1/questions/import` | `importQuestions` | `hasAnyRole('QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `SubjectTopicController.java` | **GET** | `/api/v1/subjects/{subjectId}/topics` | `listTopics` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `SubjectTopicController.java` | **POST** | `/api/v1/subjects/{subjectId}/topics` | `createTopic` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `SubjectTopicController.java` | **GET** | `/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics` | `listSubtopics` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `SubjectTopicController.java` | **POST** | `/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics` | `createSubtopic` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `SubjectTopicController.java` | **GET** | `/api/v1/subjects/hierarchy` | `getHierarchy` | `hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `AdminMicroLessonController.java` | **GET** | `/api/admin/micro-lessons/stats` | `getStats` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **GET** | `/api/admin/micro-lessons/filter-options` | `getFilterOptions` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **GET** | `/api/admin/micro-lessons/{id}` | `getMicroLessonById` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **PUT** | `/api/admin/micro-lessons/{id}` | `updateMicroLesson` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **DELETE** | `/api/admin/micro-lessons/{id}` | `deleteMicroLesson` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **POST** | `/api/admin/micro-lessons/bulk-delete` | `bulkDeleteMicroLessons` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **PUT** | `/api/admin/micro-lessons/{id}/review` | `reviewMicroLesson` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **POST** | `/api/admin/micro-lessons/generate` | `generateMicroLesson` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **POST** | `/api/admin/micro-lessons/batch-generate` | `triggerBatchGenerate` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **GET** | `/api/admin/micro-lessons/batch-jobs/{batchId}` | `getBatchJobStatus` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **GET** | `/api/admin/micro-lessons/batch-jobs/active` | `getActiveBatchJob` | `isAuthenticated()` |
| `question-bank-service` | `AdminMicroLessonController.java` | **POST** | `/api/admin/micro-lessons/batch-jobs/{batchId}/cancel` | `cancelBatchJob` | `isAuthenticated()` |
| `question-bank-service` | `MicroLessonController.java` | **GET** | `/api/micro-lessons/subtopic/{subtopicId}` | `getPublishedMicroLesson` | `isAuthenticated()` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/question/{questionId}/auto-translate/{lang}` | `autoTranslate` | `hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/batch/auto-translate` | `startBatchAutoTranslate` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/batch/{jobId}` | `getBatchJobStatus` | `hasAnyRole('EXAM_CONTROLLER', 'TRANSLATOR', 'REVIEWER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/batch` | `listBatchJobs` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/batch/paper/{paperId}` | `listBatchJobsByPaper` | `hasAnyRole('EXAM_CONTROLLER', 'TRANSLATOR', 'REVIEWER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/batch/{jobId}/cancel` | `cancelBatchJob` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/batch/{jobId}/resume` | `resumeBatchJob` | `hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **PUT** | `/api/v1/translations/{id}` | `resubmitTranslation` | `hasAnyRole('TRANSLATOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/{id}/approve` | `approveTranslation` | `hasAnyRole('REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **POST** | `/api/v1/translations/{id}/reject` | `rejectTranslation` | `hasAnyRole('REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/questions` | `listQuestionsForTranslation` | `hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/question/{questionId}` | `listTranslations` | `hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `question-bank-service` | `TranslationController.java` | **GET** | `/api/v1/translations/question/{questionId}/language/{lang}` | `getApprovedTranslation` | `hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN', 'CANDIDATE')` |
| `recommendation-service` | `LearningPathController.java` | **GET** | `/api/learning-path/current` | `getCurrentPath` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/recalculate` | `recalculate` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **GET** | `/api/learning-path/summary` | `getSummary` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/milestones/{milestoneId}/complete` | `completeMilestone` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/diagnostic-test/start` | `startDiagnosticTest` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/adaptive-quiz/start` | `startAdaptiveQuiz` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/adaptive-quiz/submit` | `submitAdaptiveQuiz` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/gatekeeper/start` | `startGatekeeperTest` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **POST** | `/api/learning-path/gatekeeper/submit` | `submitGatekeeperTest` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **GET** | `/api/learning-path/gatekeeper/status` | `getGatekeeperStatus` | `isAuthenticated()` |
| `recommendation-service` | `LearningPathController.java` | **GET** | `/api/learning-path/gatekeeper/status/{candidateId}` | `getGatekeeperStatusByCandidateId` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **GET** | `/api/recommendations/latest` | `getLatest` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **POST** | `/api/recommendations/generate` | `generateRecommendations` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **GET** | `/api/recommendations/history` | `getHistory` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **POST** | `/api/recommendations/{id}/dismiss` | `dismiss` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **PATCH** | `/api/recommendations/study-plan/days/{dayNumber}` | `updateStudyPlanDay` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **GET** | `/api/recommendations/learner-profile` | `getLearnerProfile` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **GET** | `/api/recommendations/learning-path/current` | `getCurrentLearningPath` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **POST** | `/api/recommendations/learning-path/recalculate` | `recalculateLearningPath` | `isAuthenticated()` |
| `recommendation-service` | `RecommendationController.java` | **GET** | `/api/recommendations/learning-path/summary` | `getLearningPathSummary` | `isAuthenticated()` |
| `response-service` | `ResponseController.java` | **POST** | `/api/v1/responses/{sessionId}/save` | `saveResponse` | `hasRole('CANDIDATE')` |
| `response-service` | `ResponseController.java` | **POST** | `/api/v1/responses/{sessionId}/bulk-save` | `bulkSave` | `hasRole('CANDIDATE')` |
| `response-service` | `ResponseController.java` | **POST** | `/api/v1/responses/{sessionId}/submit` | `submitSession` | `hasRole('CANDIDATE')` |
| `response-service` | `ResponseController.java` | **GET** | `/api/v1/responses/{sessionId}/responses` | `getSessionResponses` | `hasAnyRole('EVALUATOR', 'AUDITOR', 'CANDIDATE')` |
| `result-service` | `AnalyticsResultController.java` | **GET** | `/api/v1/results/analytics/exam/{examId}` | `getExamAnalytics` | `hasRole('EXAM_CONTROLLER')` |
| `result-service` | `ResultController.java` | **GET** | `/api/v1/results/my-results` | `getMyResults` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `result-service` | `ResultController.java` | **GET** | `/api/v1/results/candidate/{candidateId}` | `getCandidateResults` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `result-service` | `ResultController.java` | **GET** | `/api/v1/results/{candidateId}` | `getResult` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN')` |
| `result-service` | `ResultController.java` | **POST** | `/api/v1/results/compute` | `computeResults` | `hasRole('EXAM_CONTROLLER')` |
| `result-service` | `ResultController.java` | **POST** | `/api/v1/results/{candidateId}/publish` | `publishResult` | `hasRole('EXAM_CONTROLLER')` |
| `result-service` | `ResultController.java` | **POST** | `/api/v1/results/{id}/digilocker/push` | `pushToDigiLocker` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN', 'EXAM_CONTROLLER')` |
| `result-service` | `ReviewController.java` | **GET** | `/api/v1/results/{candidateId}/review` | `getExamReview` | `hasAnyRole('CANDIDATE', 'EXAM_CONTROLLER', 'SUPER_ADMIN')` |
| `result-service` | `ReviewController.java` | **GET** | `/api/v1/results/verify` | `verifyScorecard` | `None / PermitAll` |
| `result-service` | `ScorecardController.java` | **GET** | `/api/v1/results/{id}/scorecard` | `getScorecard` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN', 'EXAM_CONTROLLER')` |
| `result-service` | `ScorecardController.java` | **GET** | `/api/v1/results/{id}/scorecard/presigned` | `getPresignedUrl` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN', 'EXAM_CONTROLLER')` |
| `result-service` | `ScorecardController.java` | **GET** | `/api/v1/results/{id}/scorecard/download` | `downloadDirect` | `hasAnyRole('CANDIDATE', 'SUPER_ADMIN', 'EXAM_CONTROLLER')` |
