# Spec: Full-Stack End-to-End (E2E) Integration Test Suites
**Issue:** [#171](https://github.com/natassgrid/nag/issues/171)  
**Branch:** `test/issue-171-e2e-integration-suites`  
**Date:** 2026-09-27  
**Author:** Antigravity AI — Spec-Driven Development Plan

---

## 1. Problem Statement

The NAG platform spans Angular frontends (`admin-portal`, `candidate-delivery`, `public-verifier`), an API Gateway, 14+ Spring Boot microservices, PostgreSQL, Redis, Kafka, and Keycloak. No automated E2E test suite currently validates cross-service lifecycle workflows, leaving critical regression paths untested and making CI/CD deployments risky.

This spec defines the architecture, tooling, data strategy, and concrete test scenarios required to implement deterministic, fully isolated, and CI/CD-compatible full-stack E2E integration test suites.

---

## 2. Current State Analysis

### 2.1 What Exists

| Layer | What's There | Quality |
|---|---|---|
| Backend unit tests | JUnit 5 tests in individual services | ⚠️ Sparse — mostly happy-path |
| Backend integration tests | None | 🔴 Missing |
| Frontend unit tests | Karma/Jest specs in `nag-frontend-workspace` | ⚠️ Sparse |
| Frontend E2E | No Playwright/Cypress suite configured | 🔴 Missing |
| Docker Compose stack | `infrastructure/docker-compose/docker-compose.yml` — full infra stack | ✅ Solid baseline |
| Seed data | `infrastructure/docker-compose/seed-data.sql` | ⚠️ Needs E2E-scoped seed extensions |
| CI/CD | GitHub Actions workflows in `.github/` | ⚠️ No E2E stage |

### 2.2 Identified Gaps

| ID | Gap | Location | Severity |
|---|---|---|---|
| G1 | No `docker-compose.e2e.yml` overlay for test environment isolation | `infrastructure/docker-compose/` | 🔴 Critical |
| G2 | No WireMock/mock stubs for DigiLocker, SMS, and Email gateways | `e2e-tests/` | 🔴 Critical |
| G3 | No REST Assured / Spring Boot `@SpringBootTest` integration test harness | `backend/*/src/test/` | 🔴 Critical |
| G4 | No Playwright test suite configured for Angular apps | `nag-frontend-workspace/e2e/` | 🔴 Critical |
| G5 | No E2E-scoped seed SQL with deterministic UUIDs | `infrastructure/docker-compose/` | 🔴 Critical |
| G6 | No database reset / teardown strategy between test runs | `e2e-tests/` | 🔴 Critical |
| G7 | No DPDP Act PII erasure verification test | `e2e-tests/` | 🟠 High |
| G8 | No test reporting pipeline with trace correlation | `.github/workflows/` | 🟠 High |
| G9 | No CI/CD E2E stage in GitHub Actions | `.github/workflows/` | 🔴 Critical |

---

## 3. Target Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    E2E Test Execution Layer                        │
│                                                                    │
│  ┌─────────────────────┐   ┌──────────────────────────────────┐   │
│  │  Playwright Suite   │   │  REST Assured / Spring IT Suite  │   │
│  │  (Frontend E2E)     │   │  (Backend Integration Tests)     │   │
│  │  nag-frontend-      │   │  e2e-tests/ (standalone module)  │   │
│  │  workspace/e2e/     │   │  + per-service @SpringBootTest   │   │
│  └──────────┬──────────┘   └──────────────┬───────────────────┘   │
│             │                              │                       │
│             └──────────┬───────────────────┘                       │
│                        ▼                                           │
│         ┌──────────────────────────────┐                           │
│         │   API Gateway (:8080)        │                           │
│         │   (via docker-compose.e2e)   │                           │
│         └──────────────┬───────────────┘                           │
└────────────────────────┼───────────────────────────────────────────┘
                         ▼
┌────────────────────────────────────────────────────────────────────┐
│                 E2E Docker Compose Environment                     │
│                                                                    │
│  postgres:5432     redis:6379       keycloak:8081                  │
│  kafka:9092        wiremock:8089    mailhog:8025 (SMTP mock)       │
│                                                                    │
│  All NAG microservices (monolith OR micro profile)                 │
│  Pre-loaded with deterministic E2E seed data                       │
└────────────────────────────────────────────────────────────────────┘
```

### 3.1 Test Module Layout

```
nag/
├── e2e-tests/                              # NEW: Standalone E2E test module
│   ├── build.gradle                        # Gradle sub-project
│   ├── src/
│   │   └── test/
│   │       ├── java/com/examplatform/e2e/
│   │       │   ├── config/
│   │       │   │   ├── E2ETestConfig.java          # Base config, RestAssured setup
│   │       │   │   └── E2ETestExtension.java       # JUnit 5 extension, DB reset
│   │       │   ├── fixtures/
│   │       │   │   ├── CandidateFixtures.java      # Factory methods for E2E data
│   │       │   │   ├── ExamFixtures.java
│   │       │   │   └── KeycloakFixtures.java       # Token provisioning helpers
│   │       │   ├── lifecycle/
│   │       │   │   ├── CandidateOnboardingE2ETest.java
│   │       │   │   ├── ExaminationLifecycleE2ETest.java
│   │       │   │   ├── EvaluationAndResultsE2ETest.java
│   │       │   │   └── DpdpPiiErasureE2ETest.java
│   │       │   └── util/
│   │       │       ├── ApiClient.java              # Typed REST client wrapper
│   │       │       ├── DbResetUtil.java            # Flyway / truncate helpers
│   │       │       └── TraceCorrelator.java        # Log + span correlation helper
│   │       └── resources/
│   │           ├── e2e-application.properties
│   │           └── wiremock/
│   │               ├── digilocker/                 # WireMock stubs for DigiLocker
│   │               └── sms-email/                  # WireMock stubs for SMS/Email
│
├── nag-frontend-workspace/
│   └── e2e/                                # NEW: Playwright E2E suite
│       ├── playwright.config.ts
│       ├── fixtures/
│       │   ├── auth.fixture.ts             # Keycloak login helper
│       │   └── exam.fixture.ts
│       ├── tests/
│       │   ├── candidate-onboarding.spec.ts
│       │   ├── exam-authoring.spec.ts
│       │   ├── exam-taking.spec.ts
│       │   └── result-verification.spec.ts
│       └── page-objects/
│           ├── LoginPage.ts
│           ├── CandidateDashboardPage.ts
│           ├── ExamPage.ts
│           └── AdminPortalPage.ts
│
└── infrastructure/
    └── docker-compose/
        ├── docker-compose.e2e.yml          # NEW: E2E overlay — adds WireMock, MailHog
        └── e2e-seed-data.sql               # NEW: Deterministic E2E seed with fixed UUIDs
```

---

## 4. Environment Orchestration

### 4.1 `docker-compose.e2e.yml` — E2E Overlay

**File:** `infrastructure/docker-compose/docker-compose.e2e.yml`

**Purpose:** Compose overlay that adds mock third-party gateways and configures services to use them. Run alongside `docker-compose.monolith.yml` or `docker-compose.services.yml`.

**Key additions over base stack:**

| Service | Image | Port | Purpose |
|---|---|---|---|
| `wiremock` | `wiremock/wiremock:3.x` | `8089` | Mock DigiLocker, NSDL, external APIs |
| `mailhog` | `mailhog/mailhog:latest` | `8025` (UI), `1025` (SMTP) | Intercept and assert OTP/notification emails |
| `fake-sms` | Custom Spring Boot stub | `8090` | Capture SMS OTPs for assertion |

**Environment overrides on NAG services:**
- `DIGILOCKER_BASE_URL=http://wiremock:8089/digilocker`
- `SMS_GATEWAY_URL=http://fake-sms:8090`
- `SPRING_MAIL_HOST=mailhog`, `SPRING_MAIL_PORT=1025`
- `E2E_MODE=true` — activates test-only API endpoints for data setup/teardown

### 4.2 E2E Seed Data Strategy

**File:** `infrastructure/docker-compose/e2e-seed-data.sql`

**Principles:**
- All UUIDs are hardcoded and deterministic (e.g., `e2e-candidate-001`, `e2e-exam-001`) so tests can reference them without lookups.
- Seed creates: 1 Admin user, 2 Candidate users (verified + unverified), 1 published Exam, 1 Question Paper with 10 questions, Keycloak realm users matching DB records.
- The `e2e_seed_marker` table tracks seed version for idempotent re-seeding.

**Pre-seeded entities:**

```
Keycloak Realm: nag-exam
  └── Users:
       ├── admin@e2e.test        (ROLE_ADMIN)
       ├── candidate1@e2e.test   (ROLE_CANDIDATE, KYC verified)
       └── candidate2@e2e.test   (ROLE_CANDIDATE, pending verification)

PostgreSQL: exam_platform DB
  ├── candidates:      2 rows (linked to Keycloak users)
  ├── examinations:    1 row  (status=PUBLISHED, schedule=now+1h)
  ├── question_papers: 1 row  (10 questions, randomized=false for determinism)
  └── exam_registrations: 1 row (candidate1 registered)
```

### 4.3 Database Reset Between Runs

**Strategy:** Truncate-and-reseed (not full drop/recreate) for speed.

**`DbResetUtil.java`** calls a `POST /internal/e2e/reset` endpoint (only active when `E2E_MODE=true`) that:
1. Executes `TRUNCATE ... CASCADE` on all transactional tables (responses, sessions, results, audit_logs).
2. Re-runs `e2e-seed-data.sql` to restore baseline state.
3. Flushes Redis keys matching `e2e:*`.
4. Resets WireMock scenario states to `Started`.

JUnit 5 `E2ETestExtension` annotates each test class with `@ExtendWith(E2ETestExtension.class)` and calls `DbResetUtil.reset()` in `beforeEach()`.

---

## 5. Backend E2E Test Specifications

All backend E2E tests reside in the new `e2e-tests/` Gradle module. They use **REST Assured** against the running Docker stack (not `@SpringBootTest` embedded server), ensuring real network, real auth, real Redis/Postgres behaviour.

### 5.1 Lifecycle Scenario 1 — Candidate Onboarding

**File:** `e2e-tests/src/test/java/com/examplatform/e2e/lifecycle/CandidateOnboardingE2ETest.java`

**Steps & Assertions:**

| Step | API Call | Expected Outcome |
|---|---|---|
| 1. Register candidate | `POST /api/v1/candidates/register` `{name, email, phone, password}` | HTTP 201, `candidateId` in body |
| 2. Assert email OTP sent | Query MailHog `GET http://mailhog:8025/api/v2/messages` | 1 message with subject `"Verify your email"`, extract OTP |
| 3. Verify email OTP | `POST /api/v1/candidates/verify-email` `{candidateId, otp}` | HTTP 200, `status=EMAIL_VERIFIED` |
| 4. Complete profile | `PUT /api/v1/candidates/{id}/profile` `{dob, address, photo, ...}` | HTTP 200 |
| 5. Upload KYC document | `POST /api/v1/identity/{id}/kyc-upload` multipart `aadhaar.pdf` | HTTP 202, `kycStatus=PENDING` |
| 6. Mock DigiLocker claim | WireMock stub returns signed XML claim; call `POST /api/v1/identity/{id}/digilocker-verify` | HTTP 200, `kycStatus=VERIFIED` |
| 7. Assert DB state | Direct JDBC: `candidates` row has `kyc_status=VERIFIED`, `email_verified=true` | Passes |

**WireMock stub:** `wiremock/digilocker/kyc-verify-success.json`
```json
{
  "request": { "method": "GET", "urlPattern": "/digilocker/v2/documents/.*" },
  "response": { "status": 200, "bodyFileName": "digilocker-kyc-success.xml" }
}
```

---

### 5.2 Lifecycle Scenario 2 — Examination Lifecycle

**File:** `e2e-tests/src/test/java/com/examplatform/e2e/lifecycle/ExaminationLifecycleE2ETest.java`

**Substeps:**

#### Phase A — Admin Authors and Publishes Exam
| Step | API Call | Expected Outcome |
|---|---|---|
| A1. Create exam | `POST /api/v1/examinations` `{title, duration, sections}` | HTTP 201, `examId` |
| A2. Add questions | `POST /api/v1/question-bank/questions` ×10 (pre-seeded via fixture) | HTTP 201 per question |
| A3. Generate paper | `POST /api/v1/paper-generator/generate` `{examId, questionSetIds}` | HTTP 202, Kafka event emitted |
| A4. Publish schedule | `POST /api/v1/examinations/{examId}/schedule` `{startTime, endTime, centreIds}` | HTTP 200, `status=PUBLISHED` |
| A5. Assert notification | MailHog shows bulk candidate notification email | Passes |

#### Phase B — Candidate Registers and Downloads Admit Card
| Step | API Call | Expected Outcome |
|---|---|---|
| B1. Authenticate candidate | `POST /auth/realms/nag-exam/protocol/openid-connect/token` | `access_token` |
| B2. Register for exam | `POST /api/v1/examinations/{examId}/register` | HTTP 201, `registrationId` |
| B3. Download admit card | `GET /api/v1/examinations/{examId}/admit-card` | HTTP 200, PDF binary response |
| B4. Assert PDF headers | Content-Type `application/pdf`, Content-Disposition includes `admit-card` | Passes |

#### Phase C — Candidate Takes Exam
| Step | API Call | Expected Outcome |
|---|---|---|
| C1. Start session | `POST /api/v1/delivery/sessions/start` `{examId, registrationId}` | HTTP 201, `sessionId`, `sessionToken` |
| C2. Fetch question paper | `GET /api/v1/delivery/sessions/{sessionId}/paper` | HTTP 200, 10 questions (deterministic order) |
| C3. Auto-save response | `POST /api/v1/responses/auto-save` `{sessionId, questionId, answer}` ×5 | HTTP 200 per save |
| C4. Submit final answers | `POST /api/v1/responses/submit` `{sessionId, responses[]}` | HTTP 200, `submittedAt` timestamp |
| C5. Assert idempotency | Re-submit same `sessionId` | HTTP 409 Conflict |
| C6. Assert DB state | JDBC: `responses` table has 10 rows, `sessions.status=SUBMITTED` | Passes |

---

### 5.3 Lifecycle Scenario 3 — Evaluation & Results

**File:** `e2e-tests/src/test/java/com/examplatform/e2e/lifecycle/EvaluationAndResultsE2ETest.java`

**Pre-condition:** Candidate session from Scenario 2 is in `SUBMITTED` state.

| Step | API Call / Event | Expected Outcome |
|---|---|---|
| E1. Trigger evaluation | `POST /api/v1/evaluation/trigger` `{sessionId}` (admin call) | HTTP 202, Kafka event `exam.evaluation.events` emitted |
| E2. Poll evaluation status | `GET /api/v1/evaluation/status/{sessionId}` (poll max 30s) | `status=COMPLETED` |
| E3. Compute result | Kafka consumer triggers `result-service`; poll `GET /api/v1/results/{candidateId}/{examId}` | HTTP 200, `score`, `rank`, `status=PUBLISHED` |
| E4. Assert scorecard fields | Response body has `accuracyRate`, `cognitiveBreakdown`, `timeSpentAnalysis`, `sectionScores` | All fields present |
| E5. Download signed scorecard | `GET /api/v1/results/{resultId}/scorecard` | HTTP 200, PDF with `X-Signature-Hash` header |
| E6. Verify signature | `POST /api/v1/results/verify-signature` `{scorecardId, hash}` | HTTP 200, `valid=true` |
| E7. Public QR verification | `GET /api/v1/public/verify/{scorecardId}` (unauthenticated) | HTTP 200, candidate name + score summary |

---

### 5.4 Lifecycle Scenario 4 — DPDP Act PII Erasure Compliance

**File:** `e2e-tests/src/test/java/com/examplatform/e2e/lifecycle/DpdpPiiErasureE2ETest.java`

**Purpose:** Validate that a candidate's right-to-erasure request anonymizes PII across all services while preserving audit integrity.

| Step | API Call | Expected Outcome |
|---|---|---|
| D1. Submit erasure request | `POST /api/v1/candidates/{id}/erasure-request` | HTTP 202, `requestId`, `estimatedCompletionTime` |
| D2. Poll erasure status | `GET /api/v1/candidates/{id}/erasure-request/{requestId}` | `status=COMPLETED` within 60s |
| D3. Assert candidate table | JDBC: `candidates` row has `name=ANONYMIZED`, `email=NULL`, `phone=NULL`, `photo_url=NULL` | Passes |
| D4. Assert identity table | JDBC: `identity_documents` row has `aadhaar_number=NULL`, `document_url=NULL` | Passes |
| D5. Assert audit log integrity | JDBC: `audit_logs` rows for this candidate exist but `actor_name`, `actor_email` columns are anonymized | Passes (non-deletion of log entries) |
| D6. Assert Keycloak user deleted | Keycloak Admin REST API: `GET /admin/realms/nag-exam/users/{keycloakId}` | HTTP 404 Not Found |
| D7. Assert no PII in Redis | Redis `KEYS candidate:{id}:*` | Empty result set |
| D8. Assert public verifier | `GET /api/v1/public/verify/{scorecardId}` | HTTP 200 — scorecard still verifiable, candidate name shows `ANONYMIZED` |

---

## 6. Frontend E2E Test Specifications (Playwright)

**Location:** `nag-frontend-workspace/e2e/`  
**Tool:** Playwright v1.45+ with TypeScript  
**Base URL:** `http://localhost:4200` (admin-portal) / `http://localhost:4201` (candidate-delivery)

### 6.1 Configuration

**File:** `nag-frontend-workspace/e2e/playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  retries: 1,
  reporter: [['html', { outputFolder: 'e2e-report' }], ['junit', { outputFile: 'e2e-results.xml' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
  ],
  globalSetup: './fixtures/global-setup.ts',
});
```

### 6.2 Auth Fixture — Keycloak OIDC Login

**File:** `nag-frontend-workspace/e2e/fixtures/auth.fixture.ts`

```typescript
import { test as base, Page } from '@playwright/test';

export const test = base.extend<{ adminPage: Page; candidatePage: Page }>({
  adminPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'e2e/.auth/admin.json' });
    await use(await ctx.newPage());
    await ctx.close();
  },
  candidatePage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: 'e2e/.auth/candidate.json' });
    await use(await ctx.newPage());
    await ctx.close();
  },
});
```

**`global-setup.ts`** pre-authenticates admin and candidate via Keycloak OIDC and saves `storageState` JSON. This avoids re-login on every test.

### 6.3 Frontend Test Scenarios

#### Spec: `candidate-onboarding.spec.ts`
- Navigate to `/register`, fill form, submit → Assert redirect to `/verify-email`
- Assert MailHog received OTP email (via API call to `http://mailhog:8025/api/v2/messages`)
- Enter OTP → Assert redirect to `/dashboard`, toast "Email verified"

#### Spec: `exam-authoring.spec.ts`
- Login as admin → Navigate to `/exams/new`
- Fill exam title, duration, section config → Submit
- Navigate to `/question-bank`, search for pre-seeded questions, attach to exam
- Trigger paper generation → Assert status chip shows `GENERATED`
- Publish exam schedule → Assert exam appears in candidate-facing list

#### Spec: `exam-taking.spec.ts`
- Login as candidate1 → Navigate to `/exams`, assert seeded exam visible
- Click "Start Exam" → Assert lockdown prompt / full-screen request
- Answer 5 questions using `ExamPage.selectOption()` helper
- Trigger auto-save by waiting 30s → Assert `PUT /responses/auto-save` network call made (via `page.route`)
- Click "Submit Exam" → Assert confirmation modal → Confirm → Assert redirect to `/results`

#### Spec: `result-verification.spec.ts`
- Login as candidate1 → Navigate to `/results/{examId}`
- Assert score, rank, section breakdown are displayed
- Click "Download Scorecard" → Assert file download triggered (PDF)
- Navigate to `/verify/{scorecardId}` (public route, no auth) → Assert candidate name and pass/fail badge

---

## 7. Implementation Plan

### Phase 1 — Infrastructure & Environment Setup (Week 1)

| Task | Files | Owner Hint |
|---|---|---|
| 1.1 | Create `infrastructure/docker-compose/docker-compose.e2e.yml` with WireMock + MailHog services | Backend |
| 1.2 | Write `infrastructure/docker-compose/e2e-seed-data.sql` with deterministic UUIDs | Backend |
| 1.3 | Add `E2E_MODE` flag to API Gateway and monolith config; implement `POST /internal/e2e/reset` endpoint | Backend |
| 1.4 | Add WireMock stub JSON files under `e2e-tests/src/test/resources/wiremock/` | Backend |
| 1.5 | Create `e2e-tests/` Gradle sub-project and wire into `settings.gradle` | Backend |
| 1.6 | Write `E2ETestConfig.java`, `E2ETestExtension.java`, `DbResetUtil.java` | Backend |

### Phase 2 — Backend Lifecycle Test Implementation (Week 2–3)

| Task | Files | Owner Hint |
|---|---|---|
| 2.1 | `CandidateOnboardingE2ETest.java` — 7 steps including DigiLocker mock | Backend |
| 2.2 | `ExaminationLifecycleE2ETest.java` — Phase A (authoring), Phase B (registration), Phase C (exam taking) | Backend |
| 2.3 | `EvaluationAndResultsE2ETest.java` — evaluation trigger, result polling, scorecard, signature verification | Backend |
| 2.4 | `DpdpPiiErasureE2ETest.java` — full PII erasure verification across all stores | Backend |
| 2.5 | `CandidateFixtures.java`, `ExamFixtures.java`, `KeycloakFixtures.java` factory classes | Backend |
| 2.6 | `ApiClient.java` typed REST wrapper (wraps REST Assured, handles JWT refresh) | Backend |

### Phase 3 — Frontend Playwright Suite (Week 3–4)

| Task | Files | Owner Hint |
|---|---|---|
| 3.1 | Install Playwright: `npm install -D @playwright/test` in `nag-frontend-workspace/` | Frontend |
| 3.2 | `playwright.config.ts` with multi-browser projects and JUnit reporter | Frontend |
| 3.3 | `global-setup.ts` — Keycloak OIDC auth state generation | Frontend |
| 3.4 | `auth.fixture.ts` — Playwright fixture for pre-authenticated contexts | Frontend |
| 3.5 | Page Object Models: `LoginPage.ts`, `ExamPage.ts`, `AdminPortalPage.ts`, `CandidateDashboardPage.ts` | Frontend |
| 3.6 | Test specs: `candidate-onboarding.spec.ts`, `exam-authoring.spec.ts`, `exam-taking.spec.ts`, `result-verification.spec.ts` | Frontend |

### Phase 4 — CI/CD Integration & Reporting (Week 4)

| Task | Files | Owner Hint |
|---|---|---|
| 4.1 | Add `.github/workflows/e2e.yml` GitHub Actions workflow | DevOps |
| 4.2 | Configure `TraceCorrelator.java` to extract Micrometer/OpenTelemetry trace IDs from response headers | Backend |
| 4.3 | Configure Playwright HTML + JUnit XML report upload as CI artifacts | DevOps |
| 4.4 | Configure Gradle `test` task in `e2e-tests/` to produce Surefire XML reports | Backend |
| 4.5 | Add `nx run-many -t e2e` target to Nx workspace | Frontend |

---

## 8. GitHub Actions E2E Workflow

**File:** `.github/workflows/e2e.yml`

```yaml
name: E2E Integration Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  e2e-backend:
    name: Backend E2E (REST Assured)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Start E2E Docker stack
        run: |
          docker compose \
            -f infrastructure/docker-compose/docker-compose.monolith.yml \
            -f infrastructure/docker-compose/docker-compose.e2e.yml \
            up -d --wait
        env:
          COMPOSE_FILE: infrastructure/docker-compose/docker-compose.monolith.yml
      - name: Seed E2E data
        run: |
          docker exec exam-postgres psql -U exam_admin -d exam_platform \
            -f /docker-entrypoint-initdb.d/e2e-seed-data.sql
      - name: Run backend E2E tests
        run: ./gradlew :e2e-tests:test --info
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-e2e-report
          path: e2e-tests/build/reports/tests/

  e2e-frontend:
    name: Frontend E2E (Playwright)
    runs-on: ubuntu-latest
    needs: e2e-backend
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node 22
        uses: actions/setup-node@v4
        with: { node-version: '22' }
      - name: Install dependencies
        run: npm ci
        working-directory: nag-frontend-workspace
      - name: Install Playwright browsers
        run: npx playwright install --with-deps
        working-directory: nag-frontend-workspace
      - name: Build Angular apps
        run: npx nx run-many -t build --skip-nx-cache
        working-directory: nag-frontend-workspace
      - name: Run Playwright E2E
        run: npx playwright test
        working-directory: nag-frontend-workspace
        env:
          E2E_BASE_URL: http://localhost:4200
      - name: Upload Playwright report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: nag-frontend-workspace/e2e-report/
```

---

## 9. Test Data Isolation Strategy

| Layer | Isolation Mechanism |
|---|---|
| **PostgreSQL** | Per-run TRUNCATE on transactional tables; seed data has stable UUIDs |
| **Redis** | Flush keys matching `e2e:*` namespace before each test class |
| **Keycloak** | Pre-created users in dedicated `nag-exam-e2e` realm; reset passwords before test class |
| **Kafka** | Use dedicated `e2e.*` topic prefix; consumer groups reset to `earliest` before tests |
| **WireMock** | Reset scenario state to `Started` via `POST /__admin/scenarios/reset` before each test |
| **MailHog** | Delete all messages via `DELETE http://mailhog:8025/api/v1/messages` before each test |
| **Files/Assets** | Use `asset-service` mock mode; uploaded files stored in `/tmp/e2e-assets/` and wiped on reset |

---

## 10. Acceptance Criteria Mapping

| Issue Criterion | How This Spec Satisfies It |
|---|---|
| ✅ Deterministic E2E suites executable locally and in CI/CD | Docker Compose E2E stack runs locally with `docker compose -f ... up`; GitHub Actions workflow in §8 |
| ✅ Clean test data isolation with automated teardown / database reset between runs | `DbResetUtil`, `E2ETestExtension.beforeEach()`, WireMock/MailHog reset — §9 |
| ✅ Test reporting with execution logs and service trace correlation | JUnit XML + Playwright HTML reports; `TraceCorrelator.java` extracts OpenTelemetry trace IDs — §7 Phase 4 |

---

## 11. Dependencies & New Libraries

### Backend (`e2e-tests/build.gradle`)
```groovy
dependencies {
    testImplementation 'io.rest-assured:rest-assured:5.4.0'
    testImplementation 'io.rest-assured:json-path:5.4.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
    testImplementation 'com.github.tomakehurst:wiremock-standalone:3.6.0'
    testImplementation 'org.testcontainers:testcontainers:1.20.0'   // for programmatic WireMock container if needed
    testImplementation 'org.postgresql:postgresql:42.7.3'
    testImplementation 'org.keycloak:keycloak-admin-client:24.0.4'
    testImplementation 'io.lettuce:lettuce-core:6.3.2.RELEASE'      // Redis reset
}
```

### Frontend (`nag-frontend-workspace/package.json` devDependencies)
```json
{
  "@playwright/test": "^1.45.0",
  "@types/node": "^22.0.0"
}
```

---

## 12. Local Developer Quickstart

```bash
# 1. Start E2E environment (monolith + mocks)
docker compose \
  -f infrastructure/docker-compose/docker-compose.monolith.yml \
  -f infrastructure/docker-compose/docker-compose.e2e.yml \
  up -d --wait

# 2. Seed E2E data
docker exec exam-postgres psql -U exam_admin -d exam_platform \
  -f /docker-entrypoint-initdb.d/e2e-seed-data.sql

# 3. Run backend E2E tests
./gradlew :e2e-tests:test --info

# 4. Open backend test report
open e2e-tests/build/reports/tests/test/index.html

# 5. Run Playwright frontend E2E
cd nag-frontend-workspace
npx playwright test

# 6. Open Playwright HTML report
npx playwright show-report e2e-report/

# 7. Tear down
docker compose \
  -f infrastructure/docker-compose/docker-compose.monolith.yml \
  -f infrastructure/docker-compose/docker-compose.e2e.yml \
  down -v
```

---

## 13. Open Questions

| # | Question | Stakeholder |
|---|---|---|
| Q1 | Should E2E tests run against `docker-compose.monolith.yml` (simpler) or `docker-compose.services.yml` (full microservices)? Monolith recommended for initial phase. | Architecture |
| Q2 | Should `POST /internal/e2e/reset` be protected by a shared secret header (`X-E2E-Reset-Token`) to prevent accidental invocation in staging? | Security |
| Q3 | Keycloak realm for E2E — use the same `nag-exam` realm with an `e2e-` user prefix, or a dedicated `nag-exam-e2e` realm? Dedicated realm recommended for isolation. | DevOps |
| Q4 | Should the Playwright suite also cover the `public-verifier` Angular app? Scorecard QR verification is a key user journey. | Product |
| Q5 | For the lockdown/proctoring exam-taking test, should Playwright be configured with a mock webcam/screen-share permission, or should that scenario be skipped in CI? | Frontend |
