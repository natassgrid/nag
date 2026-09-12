# Platform Gaps & Technical Debt Analysis

> **Document Version:** 1.0.3  
> **Last Updated:** 2026-09-11  
> **Repository:** `natassgrid/nag`  
> **Scope:** Entire platform (Backend microservices, Macroservices, Monolith, Frontend SPAs, Infrastructure, CI/CD, and Security)

---

## Executive Summary

A comprehensive architectural and code-level audit was conducted across the National Assessment Grid (NAG) platform. The platform demonstrates strong architectural foundations:
- **Tri-Mode Architecture** supporting Microservices, Macro-services, and Single-JVM Monolith.
- **REST Controller Integration Test Suite** using Testcontainers across all 14 backend microservices.
- **Unified Multi-Schema Database Migrations** managed via Flyway.
- **Accessible UI Topologies** for administrators/controllers (Angular 21) and candidates (React 19).

However, key architectural and operational gaps exist that must be addressed prior to enterprise or nation-scale production deployment. These findings are prioritized below by severity.

---

## Gap Matrix Summary

| Category | Total Gaps | Critical | High | Medium | Low | Resolved |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| 1. Security & Authentication | 4 | 0 (was 1) | 1 (was 3) | 0 | 0 | 3 |
| 2. Gateway & Microservice Decoupling | 3 | 0 | 1 | 2 | 0 | 0 |
| 3. Business Logic & Consumer Stubs | 6 | 0 | 0 | 5 | 1 | 0 |
| 4. Frontend & Testing Automation | 3 | 0 | 2 | 0 | 1 | 0 |
| 5. Infrastructure, CI/CD & Documentation | 4 | 0 | 1 | 2 | 1 | 0 |
| **Total** | **20** | **0** | **5** | **9** | **3** | **3** |

---

## 1. Security & Authentication Gaps

### 1.1 `.env` File Not Ignored in `.gitignore` `[RESOLVED]`
- **Severity:** `CRITICAL`
- **Status:** **RESOLVED**
- **Component:** Repository Root ([`.gitignore`](../.gitignore))
- **File Reference:** [`.gitignore`](../.gitignore), [`infrastructure/docker-compose/.env.example`](../infrastructure/docker-compose/.env.example), [`.env.example`](../.env.example)
- **Description:**  
  Local environment configuration files (such as `infrastructure/docker-compose/.env`), which contain AWS credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) and Bedrock execution IAM roles, risked accidental git staging and commit.
- **Remediation Completed:**
  - Enhanced root [`.gitignore`](../.gitignore) with explicit glob patterns covering all root and nested `.env` variants: `.env`, `.env.*`, `.env.local`, `*.env`, `**/.env`, `**/.env.*`, `**/*.env`, while preserving `!.env.example` and `!**/.env.example`.
  - Verified git history: confirmed AWS credentials were never committed in any prior git revision.
  - Provided sanitized template [`infrastructure/docker-compose/.env.example`](../infrastructure/docker-compose/.env.example) with placeholder values.
  - Updated root [`.env.example`](../.env.example) with complete configuration options for AWS Bedrock and IndicTrans2.
  - Sanitized local [`infrastructure/docker-compose/.env`](../infrastructure/docker-compose/.env) with placeholder values.
  - Recommended AWS credential rotation in AWS IAM console as a security best practice.

### 1.2 WebAuthn Signature Verification Stub `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `identity-service`
- **File Reference:** [`WebAuthnService.java`](../backend/identity-service/src/main/java/com/examplatform/identity/service/WebAuthnService.java), [`WebAuthnServiceTest.java`](../backend/identity-service/src/test/java/com/examplatform/identity/service/WebAuthnServiceTest.java), [`build.gradle`](../backend/identity-service/build.gradle)
- **Description:**  
  The method `verifyAssertionSignature()` validated only that input strings were non-empty and Base64URL-encoded (`return authData.length > 0 && signature.length > 0 && clientDataJSON.length > 0;`). It did not decode COSE public keys or verify cryptographic ECDSA/RSA signatures.
- **Remediation Completed:**
  - Added dependency `com.webauthn4j:webauthn4j-core:0.24.1.RELEASE` to `backend/identity-service/build.gradle`.
  - Updated `verifyAssertionSignature()` in `WebAuthnService.java` to parse stored CBOR `publicKeyCose` using WebAuthn4J's `ObjectConverter` into a `COSEKey` and extract the `java.security.PublicKey`.
  - Dynamically resolved the JCA algorithm identifier (supporting ECDSA / `SHA256withECDSA`, RSA, and EdDSA) via `coseKey.getAlgorithm().toSignatureAlgorithm().getJcaName()`.
  - Constructed the authenticated payload `authenticatorData || SHA-256(clientDataJSON)` and verified the signature cryptographically via `java.security.Signature`.
  - Updated `WebAuthnServiceTest.java` to dynamically generate cryptographic P-256 keypairs, valid assertions, and signature verification failure checks (including tampered signatures and empty payloads).
  - Verified 100% test pass rate across `identity-service`.

### 1.3 Unimplemented Identity Endpoints (Change Password, Resend OTP, Logout) `[RESOLVED]`
- **Severity:** `HIGH`
- **Status:** **RESOLVED**
- **Component:** `identity-service`
- **File Reference:** [`IdentityController.java`](../backend/identity-service/src/main/java/com/examplatform/identity/controller/IdentityController.java), [`AuthenticationService.java`](../backend/identity-service/src/main/java/com/examplatform/identity/service/AuthenticationService.java), [`RegistrationService.java`](../backend/identity-service/src/main/java/com/examplatform/identity/service/RegistrationService.java), [`KeycloakService.java`](../backend/identity-service/src/main/java/com/examplatform/identity/service/KeycloakService.java), [`DevKeycloakService.java`](../backend/identity-service/src/main/java/com/examplatform/identity/service/DevKeycloakService.java)
- **Description:**  
  The endpoints `PUT/POST /api/v1/identity/auth/change-password` & `/api/v1/identity/users/change-password`, `POST /api/v1/identity/otp/resend`, and `DELETE /api/v1/identity/auth/logout` returned HTTP 200 OK responses with hardcoded success messages, but contained commented-out TODO calls to business services.
- **Remediation Completed:**
  - **Change Password**: Implemented `AuthenticationService.changePassword(userIdOrSubject, currentPassword, newPassword, tenantId)` and `KeycloakService.changePassword(username, currentPassword, newPassword, keycloakUserId)`. Verifies the user's current password via Keycloak token endpoint, resets password via Keycloak Admin REST API (`PUT /admin/realms/{realm}/users/{id}/reset-password`), checks account status (`ACTIVE`), and publishes asynchronous audit events. Added HTTP mapping aliases in `IdentityController` supporting both POST and PUT methods at `/auth/change-password` and `/users/change-password`.
  - **Resend OTP**: Implemented `RegistrationService.resendOtp(userId, tenantId)`. Validates tenant affinity and account status (`PENDING_VERIFICATION`), re-dispatches 6-digit OTP via `OtpService.sendOtp()`, and emits asynchronous audit events.
  - **Logout**: Implemented `AuthenticationService.logout(userIdOrSubject, tenantId)` and `KeycloakService.revokeUserSessions(keycloakUserId)`. Invalidates concurrent active sessions from `ActiveSessionRepository`, revokes active sessions in Keycloak via Admin API (`POST /admin/realms/{realm}/users/{id}/logout`), and publishes `AuditEventType.LOGOUT` audit events.
  - **Unit & Integration Tests**: Added comprehensive test suites in `RegistrationServiceTest.java`, `AuthenticationServiceTest.java`, and `IdentityControllerIntegrationTest.java` verifying both positive workflows and negative edge cases (account not found, tenant mismatch, locked/deactivated account, already verified account, wrong current password, invalid payloads). Verified 100% test pass rate across `identity-service` (141/141 tests passing).

### 1.4 Stubbed Keycloak User Deactivation
- **Severity:** `HIGH`
- **Component:** `admin-service`
- **File Reference:** [`KeycloakAdminClientImpl.java`](../backend/admin-service/src/main/java/com/examplatform/admin/client/KeycloakAdminClientImpl.java#L28-L41)
- **Description:**  
  `KeycloakAdminClientImpl.disableUser()` logs a stub message without invoking the Keycloak Admin REST API (`PUT /admin/realms/{realm}/users/{id}`).
- **Impact:** Disabling or banning an administrator or tenant in `admin-service` does not prevent them from authenticating via Keycloak.
- **Mitigation:**
  - Implement Keycloak Admin Client with service account client-credentials grant.

---

## 2. Gateway & Microservice Decoupling Gaps

### 2.1 API Gateway Port Configuration Offsets
- **Severity:** `HIGH`
- **Component:** `api-gateway`
- **File Reference:** [`application.yml`](../backend/api-gateway/src/main/resources/application.yml#L80-L165)
- **Description:**  
  In local development mode without Docker container DNS overrides, fallback service ports are offset by one starting from port 8085:
  - `paper-generator` defaults to `http://localhost:8085` (actual port: `8086`)
  - `delivery-service` defaults to `http://localhost:8086` (actual port: `8087`)
  - `response-service` defaults to `http://localhost:8087` (actual port: `8088`)
  - `evaluation-service` defaults to `http://localhost:8088` (actual port: `8089`)
  - `result-service` defaults to `http://localhost:8089` (actual port: `8090`)
  - `audit-service` defaults to `http://localhost:8090` (actual port: `8091`)
  - `notification-service` defaults to `http://localhost:8091` (actual port: `8092`)
- **Impact:** Running services standalone locally causes `api-gateway` to route requests to the wrong services or fail with connection refused.
- **Mitigation:**
  - Correct default fallback URLs in `api-gateway/src/main/resources/application.yml` to match the actual microservice ports defined in each service's `application.yml`.

### 2.2 Direct Cross-Schema Database Coupling in Paper Generator
- **Severity:** `MEDIUM`
- **Component:** `paper-generator`
- **File Reference:** [`QuestionBankClientImpl.java`](../backend/paper-generator/src/main/java/com/examplatform/papergenerator/client/QuestionBankClientImpl.java#L52-L95)
- **Description:**  
  `paper-generator` directly executes SQL against the `question_service.question` table via `JdbcTemplate` instead of calling `question-bank-service` over REST or gRPC.
- **Impact:** Violates database-per-service isolation; breaks if `question-bank-service` moves to an independent database instance or cluster.
- **Mitigation:**
  - Expose a batch search endpoint in `question-bank-service` (e.g. `POST /api/v1/questions/blueprint-match`).
  - Update `paper-generator` to query this endpoint via `RestClient` / `WebClient`.

### 2.3 Stubbed Inter-Service Clients in Delivery Service
- **Severity:** `MEDIUM`
- **Component:** `delivery-service`
- **File References:**  
  - [`ShiftAssignmentClientImpl.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/client/ShiftAssignmentClientImpl.java)
  - [`CandidateProfileClientImpl.java`](../backend/delivery-service/src/main/java/com/examplatform/delivery/client/CandidateProfileClientImpl.java)
