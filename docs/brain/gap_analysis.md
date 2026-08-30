# Candidate Frontend ↔ Backend Gap Analysis

> Generated: 2026-08-30  
> Scope: `candidate-frontend/` (React 18 + Vite + Tailwind) ↔ Java Spring Boot microservices

---

## Executive Summary

The candidate frontend is a **fully mock skeleton** — every API call is simulated with `setTimeout` in `AuthContext.tsx`, and all question/exam data is hard-coded arrays. The backend microservices have **real controller endpoints** for auth, candidate profile, exam sessions, response capture, and results — but they are either incomplete (stub services) or gated behind JWT/role checks that the frontend does not yet send.

**Critical path to production:**
1. JWT auth layer (frontend must obtain + send bearer tokens)
2. API client / service layer (no HTTP calls exist today)
3. Candidate-facing exam listing endpoint (missing from backend)
4. Exam application flow (missing from backend)
5. Real question delivery wiring (TakeExam uses hardcoded MOCK_QUESTIONS)

---

## 1. Backend API Inventory

### 1.1 Identity Service — `POST /api/v1/identity/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/register` | POST | Public | ✅ Implemented | Register |
| `/otp/verify` | POST | Public | ✅ Implemented | VerifyOtp |
| `/auth/token` | POST | Public | ✅ Implemented | Login |
| `/auth/webauthn` | POST | Public | ✅ Implemented | Login (not wired) |
| `/users` | GET | SUPER_ADMIN | ✅ Implemented | — |

**Request shapes needed by frontend:**

```json
// POST /api/v1/identity/register → RegistrationRequest
{
  "fullName": "string",
  "email": "string",
  "mobile": "string",
  "password": "string",
  "identityDocType": "AADHAAR | PAN | PASSPORT",
  "identityDocNumber": "string"
}

// POST /api/v1/identity/auth/token → AuthTokenRequest
{
  "username": "string",   // email or mobile
  "password": "string",
  "otp": "string?",       // optional MFA
  "deviceFingerprint": "string?"
}

// Response: AuthTokenResponse
{
  "accessToken": "string",
  "refreshToken": "string",
  "expiresIn": 3600
}
```

> [!IMPORTANT]
> The frontend currently uses plain string comparison (`email === "candidate@nag.gov.in"`) for auth.  
> It must instead POST to `/api/v1/identity/auth/token`, store the JWT, and attach it as `Authorization: Bearer <token>` on every subsequent request.

---

### 1.2 Candidate Service — `GET|POST|PUT /api/v1/candidates/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/` | POST | CANDIDATE | ✅ Implemented | Post-registration flow |
| `/{userId}` | GET | CANDIDATE / SUPER_ADMIN | ✅ Implemented | Profile |
| `/{userId}` | PUT | CANDIDATE | ✅ Implemented | Profile |
| `/{userId}/pii` | DELETE | CANDIDATE | ✅ Implemented | — |
| `/{userId}/consent` | POST | CANDIDATE | ✅ Implemented | Profile (biometrics tab) |
| `/{userId}/digilocker/verify` | POST | CANDIDATE | ✅ Implemented | Profile (docs tab) |
| `/{userId}/face/verify` | POST | CANDIDATE | ✅ Implemented | Profile (biometrics) |

**Missing in backend:**
- ❌ No `PATCH /candidates/{userId}/password` endpoint — `IdentityController` has no change-password route for candidates  
- ❌ No `POST /candidates/{userId}/photo`, `/{userId}/signature` — photo/signature upload must go through **asset-service** (`POST /api/v1/assets`), but `AssetController` requires role `QUESTION_AUTHOR | ADMIN | CONTENT_MANAGER` — candidates are excluded

> [!WARNING]
> **`AssetController` does not allow `CANDIDATE` role to upload files.**  
> The Profile page's "Upload Photo / Signature / ID Proof" buttons will always get `403 Forbidden`.  
> The asset-service `@PreAuthorize` must be updated to include `CANDIDATE` for own-resource uploads, or a dedicated `/candidates/{id}/documents` endpoint must be added to candidate-service.

---

### 1.3 Examination Service — `GET|POST|PUT /api/v1/examinations/*`

| Endpoint | Method | Role | Status | Notes |
|----------|--------|------|--------|-------|
| `/` | GET | **EXAM_CONTROLLER only** | ✅ Implemented | ❌ Candidates cannot list exams |
| `/` | POST | EXAM_CONTROLLER | ✅ Implemented | Admin only |
| `/{examId}` | GET | EXAM_CONTROLLER / SUPER_ADMIN | ✅ Implemented | ❌ Candidates cannot get exam detail |
| `/{examId}/publish` | PUT | EXAM_CONTROLLER | ✅ Implemented | Admin only |

> [!CAUTION]
> **Candidates have NO endpoint to browse or apply for exams.**  
> `ExaminationController` is admin-only (`EXAM_CONTROLLER` role required). There is no public/candidate-facing exam catalogue.  
> **Required new backend endpoint:**
> ```
> GET /api/v1/examinations/public          → list PUBLISHED exams (no auth required)
> POST /api/v1/examinations/{examId}/apply → candidate applies (CANDIDATE role)
> GET /api/v1/examinations/my-exams        → candidate's registered exams (CANDIDATE role)
> ```

---

### 1.4 Delivery Service — `POST /api/v1/sessions/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/start` | POST | CANDIDATE | ✅ Implemented | TakeExam |
| `/{sessionId}/navigate` | POST | CANDIDATE | ✅ Implemented | TakeExam |
| `/{sessionId}/proctoring/snapshot` | POST | CANDIDATE | ✅ Implemented | TakeExam |
| `/{sessionId}/proctoring/fullscreen-exit` | POST | CANDIDATE | ✅ Implemented | TakeExam |

**Missing in backend:**
- ❌ No `GET /sessions/{sessionId}/question/{index}` — TakeExam must fetch questions from the server, not from `MOCK_QUESTIONS`
- ❌ No `GET /sessions/{sessionId}/status` — frontend cannot poll for session state (time remaining, submission status)
- ❌ No explicit session end/timeout endpoint (covered by `/responses/{sessionId}/submit`)

---

### 1.5 Response Service — `POST /api/v1/responses/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/{sessionId}/save` | POST | CANDIDATE | ✅ Implemented | TakeExam (per-question autosave) |
| `/{sessionId}/bulk-save` | POST | CANDIDATE | ✅ Implemented | TakeExam (offline reconnect) |
| `/{sessionId}/submit` | POST | CANDIDATE | ✅ Implemented | TakeExam (submit button) |
| `/{sessionId}/responses` | GET | EVALUATOR / AUDITOR | ✅ Implemented | Admin only |

**Frontend currently:** Calls `submitExamResult()` in `AuthContext` which just mutates local state with a random rank. The real flow must be: `POST /responses/{sessionId}/submit` → wait for result computation.

---

### 1.6 Result Service — `GET|POST /api/v1/results/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/{candidateId}?examId=` | GET | CANDIDATE / SUPER_ADMIN | ✅ Implemented | Results |
| `/{candidateId}/scorecard` | GET | CANDIDATE / ADMIN | ✅ Implemented | Results (PDF download) |
| `/compute` | POST | EXAM_CONTROLLER | ✅ Implemented | Admin only |
| `/{candidateId}/publish` | POST | EXAM_CONTROLLER | ✅ Implemented | Admin only |

> [!NOTE]
> Result display on frontend uses mock data (fixed `percentile`, `rank`).  
> `GET /api/v1/results/{candidateId}?examId=` must be called with the JWT token to get the real score, rank, percentile.

---

### 1.7 Notification Service — `GET|PATCH /api/v1/notifications/*`

| Endpoint | Method | Role | Status | Frontend Page |
|----------|--------|------|--------|---------------|
| `/` | GET | Authenticated | ✅ Implemented | Layout (bell icon) |
| `/stream` | GET (SSE) | Authenticated | ✅ Implemented | Layout (real-time bell) |
| `/{id}/read` | PATCH | Authenticated | ✅ Implemented | Layout |

**Frontend currently:** The bell icon in `Layout.tsx` shows a static badge count. No SSE connection is established.

---

## 2. Frontend Gap Analysis

### 2.1 Auth Layer — `src/context/AuthContext.tsx`

| Gap | Severity | Fix |
|-----|----------|-----|
| All functions use `setTimeout` mock — no real HTTP | 🔴 Critical | Replace with real `fetch`/`axios` calls |
| No JWT token storage (no `accessToken`, `refreshToken`) | 🔴 Critical | Store tokens in `localStorage` or `httpOnly` cookie |
| No `Authorization: Bearer` header on any request | 🔴 Critical | Add Axios interceptor to attach token |
| No token expiry / refresh logic | 🔴 Critical | Add silent refresh (`/auth/refresh`) before expiry |
| `UserProfile` shape does not match backend DTO | 🟠 High | Add `userId: UUID`, `tenantId`, `kycVerified`, `profileCompleteness` |
| No `X-Tenant-Id` header sent | 🟠 High | Every request needs this header; default `"default"` |
| OTP verification does not return JWT tokens | 🔴 Critical | `/otp/verify` returns `AuthTokenResponse`; frontend ignores it |
| `changePassword` is a no-op returning `true` | 🟠 High | Backend endpoint missing — needs to be added |

---

### 2.2 Missing Infrastructure Files

| File / Directory | What's Needed |
|-----------------|---------------|
| `src/services/api.ts` | Axios instance with base URL, interceptors, `Authorization` + `X-Tenant-Id` headers |
| `src/services/authService.ts` | `login()`, `register()`, `verifyOtp()`, `refreshToken()` wrappers |
| `src/services/candidateService.ts` | `getProfile()`, `updateProfile()`, `uploadDocument()` |
| `src/services/examService.ts` | `listPublishedExams()`, `applyForExam()`, `getMyExams()` |
| `src/services/sessionService.ts` | `startSession()`, `navigate()`, `captureSnapshot()` |
| `src/services/responseService.ts` | `saveResponse()`, `bulkSave()`, `submitSession()` |
| `src/services/resultService.ts` | `getResult()`, `downloadScorecard()` |
| `src/services/notificationService.ts` | `getNotifications()`, `markAsRead()`, SSE setup |
| `src/hooks/useAuth.ts` | Wrap context with React Query for loading/error states |
| `src/hooks/useNotifications.ts` | SSE event source management |
| `src/types/api.ts` | All request/response TypeScript interfaces matching backend DTOs |
| `src/utils/tokenManager.ts` | `getToken()`, `setToken()`, `clearToken()`, decode + check expiry |

---

### 2.3 Page-by-Page Gaps

#### `Login.tsx`
| Current | Needed |
|---------|--------|
| Hardcoded credential check | `POST /api/v1/identity/auth/token` |
| No token storage | Store `accessToken` + `refreshToken` |
| Forgot password = no-op | Needs `POST /identity/otp/initiate?type=FORGOT_PASSWORD` (backend missing) |
| No loading spinner during API call | Add `isLoading` state |
| No error message from server | Display `ApiResponse.message` on failure |

---

#### `Register.tsx`
| Current | Needed |
|---------|--------|
| Creates local profile object | `POST /api/v1/identity/register` |
| Identity document fields not present | Add `identityDocType` + `identityDocNumber` fields |
| No server-side duplicate check | Backend returns 409 if email/mobile exists |
| Password strength rules not enforced | Add zod/yup validation schema |

---

#### `VerifyOtp.tsx`
| Current | Needed |
|---------|--------|
| Any 6-digit code passes | `POST /api/v1/identity/otp/verify` with real OTP |
| Returns `boolean` only | Real response returns `AuthTokenResponse` (tokens must be stored) |
| Resend OTP is a no-op | Needs `POST /identity/otp/resend` (backend endpoint missing) |
| Countdown is cosmetic | Server enforces OTP expiry (5 min) — show real expiry |

---

#### `Dashboard.tsx`
| Current | Needed |
|---------|--------|
| Registered exams from `user.registeredExams[]` (IDs only) | `GET /api/v1/examinations/my-exams` (missing) |
| Completed exams from `user.completedExams[]` (mock) | `GET /api/v1/results/{candidateId}` |
| Announcements are hardcoded strings | `GET /api/v1/notifications` |
| Profile completeness % calculated locally | Should come from `CandidateProfileResponse.completionPercentage` |

---

#### `Profile.tsx`
| Current | Needed |
|---------|--------|
| `updateProfile()` mutates local state | `PUT /api/v1/candidates/{userId}` |
| Initial data from context mock `DEFAULT_USER` | `GET /api/v1/candidates/{userId}` on mount |
| File upload is a fake progress bar | `POST /api/v1/assets` (needs CANDIDATE role added) |
| DigiLocker tab is UI-only | `POST /api/v1/candidates/{userId}/digilocker/verify` |
| Face verification tab is UI-only | `POST /api/v1/candidates/{userId}/face/verify` |
| Consent checkbox does nothing | `POST /api/v1/candidates/{userId}/consent` |
| No field-level validation | Add Zod or React Hook Form schema |

---

#### `PasswordMgmt.tsx`
| Current | Needed |
|---------|--------|
| Returns `true` if both fields non-empty | Backend endpoint is **completely missing** |
| No current-password verification | Needs `POST /api/v1/identity/auth/change-password` |

> [!CAUTION]
> **No backend endpoint for candidate password change exists.**  
> Must be added to `IdentityController`:  
> `POST /api/v1/identity/auth/change-password` with `{currentPassword, newPassword}` + JWT auth

---

#### `BrowseExams.tsx`
| Current | Needed |
|---------|--------|
| Hardcoded `MOCK_EXAMS` array of 6 exams | `GET /api/v1/examinations/public` (missing from backend) |
| `applyForExam()` adds ID to local array | `POST /api/v1/examinations/{examId}/apply` (missing) |
| No eligibility check | Backend should validate candidate profile completeness before allowing apply |
| No pagination | Add `page` + `size` params |
| Search is client-side filter | Should send `?search=` to backend |

---

#### `TakeExam.tsx`
| Current | Needed |
|---------|--------|
| `MOCK_QUESTIONS` hardcoded (8 questions) | `POST /api/v1/sessions/start` → get `sessionId` + first batch of questions |
| Navigation is local state only | `POST /api/v1/sessions/{sessionId}/navigate` per question change |
| Answers stored in component state only | `POST /api/v1/responses/{sessionId}/save` on each answer |
| Submit writes to local context | `POST /api/v1/responses/{sessionId}/submit` |
| No proctoring | `POST /sessions/{sessionId}/proctoring/snapshot` on webcam tick |
| Fullscreen exit detection missing | `POST /sessions/{sessionId}/proctoring/fullscreen-exit` |
| Timer is cosmetic, client-side only | Server returns `durationSeconds`; enforce server-side remaining time |
| Offline mode: no retry/queue | Use `BulkSaveRequest` to replay on reconnect |

---

#### `Results.tsx`
| Current | Needed |
|---------|--------|
| Shows first entry from `user.completedExams[]` (mock) | `GET /api/v1/results/{candidateId}?examId={examId}` |
| Rank/percentile are random numbers | Come from server `Result` entity |
| "Download Certificate" opens a modal | `GET /api/v1/results/{candidateId}/scorecard` → stream PDF download |
| Section-wise bar chart is hard-coded | Backend result includes section breakdown |

---

## 3. Missing Backend Endpoints (Must Build)

These endpoints do not exist in any backend service and are required to complete the candidate flow:

| Priority | Endpoint | Service | Description |
|----------|----------|---------|-------------|
| 🔴 P0 | `GET /api/v1/examinations/public` | examination-service | List all PUBLISHED exams, paginated, no auth |
| 🔴 P0 | `POST /api/v1/examinations/{examId}/apply` | examination-service | Candidate applies; stores registration, validates eligibility |
| 🔴 P0 | `GET /api/v1/examinations/my-exams` | examination-service | Returns registered exams for authenticated candidate |
| 🔴 P0 | `POST /api/v1/identity/auth/change-password` | identity-service | Candidate changes own password with old+new verification |
| 🟠 P1 | `POST /api/v1/identity/otp/resend` | identity-service | Resend OTP to registered email + mobile |
| 🟠 P1 | `POST /api/v1/identity/auth/forgot-password` | identity-service | Initiate forgot-password flow (sends OTP) |
| 🟠 P1 | `POST /api/v1/identity/auth/reset-password` | identity-service | Complete password reset after OTP verification |
| 🟠 P1 | `PUT /api/v1/assets` — allow `CANDIDATE` role | asset-service | Candidates must upload photo/signature/id-proof |
| 🟡 P2 | `GET /api/v1/sessions/{sessionId}/status` | delivery-service | Poll session time remaining, submission state |
| 🟡 P2 | `GET /api/v1/sessions/{sessionId}/question/{index}` | delivery-service | Fetch specific question by index within session |
| 🟡 P2 | `POST /api/v1/identity/auth/refresh` | identity-service | Silent token refresh using `refreshToken` |
| 🟡 P2 | `DELETE /api/v1/identity/auth/logout` | identity-service | Revoke refresh token on logout |

---

## 4. Frontend Infrastructure Gaps

### 4.1 State Management
Currently all state lives in `AuthContext` (a single React Context). This will not scale.

**Recommended additions:**
- **React Query (TanStack Query)** — for server state (profiles, exams, results, notifications)
- **Zustand** — for client-only state (exam session UI state, question palette, timer)

### 4.2 API Client
No HTTP client exists. Every page directly calls mock functions.

```ts
// src/services/api.ts — to be created
import axios from 'axios';
import { tokenManager } from '../utils/tokenManager';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'X-Tenant-Id': 'default' },
});

api.interceptors.request.use((config) => {
  const token = tokenManager.getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      await tokenManager.refresh();
      return api(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 4.3 Form Validation
No schema validation library is installed.

```bash
npm install react-hook-form zod @hookform/resolvers
```

Required for: `Register`, `Login`, `Profile`, `PasswordMgmt` pages.

### 4.4 Environment Configuration
No `.env` file exists. API base URL is hardcoded nowhere (because there are no API calls).

```dotenv
# candidate-frontend/.env.local
VITE_API_URL=http://localhost:9000
VITE_TENANT_ID=default
VITE_KEYCLOAK_URL=http://localhost:8080
```

### 4.5 Error Boundaries
No `ErrorBoundary` component exists. A JS error in any page crashes the entire app with a white screen.

### 4.6 Loading / Skeleton States
Every page loads instantly because all data is synchronous mock data. Real API calls require:
- Loading skeletons per page
- `Suspense` boundaries around React Query data

### 4.7 Toast / Alert System
Success/error states are shown inline (e.g., a green div). A shared `<Toast>` component is needed for:
- "Profile saved successfully"
- "OTP sent to xxxxxxx"
- "Session submission confirmed"

### 4.8 Offline / Connectivity
`TakeExam.tsx` has no offline strategy. When network drops during an exam:
1. Responses must be buffered locally (IndexedDB or `localStorage`)
2. On reconnect, `POST /responses/{sessionId}/bulk-save` replays buffered answers

### 4.9 Accessibility (a11y)
- No `aria-*` attributes on interactive elements
- Form inputs lack `aria-describedby` for errors
- Modal dialogs lack focus trap
- Color contrast: several gray tones may fail WCAG AA

### 4.10 Internationalisation (i18n)
All strings are English hard-coded. For a government exam platform serving multiple states:
- Install `react-i18next`
- Extract all strings to `src/locales/en.json`
- Add Hindi (`hi.json`) as a minimum second locale

---

## 5. Security Gaps

| Gap | Severity | Fix |
|-----|----------|-----|
| `DEFAULT_USER` with full profile always in `localStorage` | 🔴 Critical | Remove on logout; never seed production code with mock users |
| JWT stored in `localStorage` (XSS vulnerable) | 🟠 High | Use `httpOnly` cookie via BFF pattern, or at minimum use `sessionStorage` |
| No CSRF protection | 🟠 High | Add `X-Requested-With` header or same-site cookie policy |
| No Content Security Policy in nginx.conf | 🟠 High | Add `Content-Security-Policy` header |
| Exam questions include `correctOptionIndex` in mock data | 🔴 Critical | Server MUST NOT send correct answer to client — evaluation is server-side |
| Password visible during typing (no show/hide toggle) | 🟡 Low | UX best practice |

---

## 6. Delivery Roadmap

### Sprint 1 — Auth & API Foundation (Week 1–2)
- [ ] Create `src/services/api.ts` (Axios + interceptors)
- [ ] Create `src/utils/tokenManager.ts` (store / refresh JWT)
- [ ] Wire `Login.tsx` → `POST /auth/token`
- [ ] Wire `Register.tsx` → `POST /identity/register`
- [ ] Wire `VerifyOtp.tsx` → `POST /identity/otp/verify` (store returned tokens)
- [ ] Add `react-hook-form` + `zod` validation to all auth forms
- [ ] **Backend:** Add `POST /identity/auth/change-password`
- [ ] **Backend:** Add `POST /identity/otp/resend`
- [ ] **Backend:** Add `POST /identity/auth/refresh` (silent refresh)

### Sprint 2 — Profile & Documents (Week 3)
- [ ] Wire `Profile.tsx` → `GET /candidates/{userId}` on mount
- [ ] Wire `Profile.tsx` → `PUT /candidates/{userId}` on save
- [ ] **Backend:** Allow `CANDIDATE` role to call `POST /api/v1/assets`
- [ ] Wire photo / signature / ID proof upload → `POST /assets`
- [ ] Wire DigiLocker verify button → `POST /candidates/{userId}/digilocker/verify`
- [ ] Wire consent checkbox → `POST /candidates/{userId}/consent`
- [ ] Wire `PasswordMgmt.tsx` → new `POST /identity/auth/change-password`

### Sprint 3 — Exam Browse & Apply (Week 4)
- [ ] **Backend:** Add `GET /examinations/public` (no auth, list PUBLISHED exams)
- [ ] **Backend:** Add `POST /examinations/{examId}/apply` (CANDIDATE role)
- [ ] **Backend:** Add `GET /examinations/my-exams` (CANDIDATE role)
- [ ] Wire `BrowseExams.tsx` → real endpoint (remove `MOCK_EXAMS`)
- [ ] Wire "Apply Now" → `POST /examinations/{examId}/apply`
- [ ] Wire `Dashboard.tsx` registered exams → `GET /examinations/my-exams`

### Sprint 4 — Exam Delivery (Week 5–6)
- [ ] Wire `TakeExam.tsx` → `POST /sessions/start` (get `sessionId` + questions)
- [ ] Wire per-question autosave → `POST /responses/{sessionId}/save`
- [ ] Wire navigation → `POST /sessions/{sessionId}/navigate`
- [ ] Wire submit → `POST /responses/{sessionId}/submit`
- [ ] Add offline buffer (IndexedDB) → reconnect → `bulk-save`
- [ ] Add proctoring: webcam snapshot loop → `POST /proctoring/snapshot`
- [ ] Detect fullscreen exit → `POST /proctoring/fullscreen-exit`
- [ ] **Backend:** Remove `correctOptionIndex` from question payload to client

### Sprint 5 — Results & Notifications (Week 7)
- [ ] Wire `Results.tsx` → `GET /results/{candidateId}?examId=`
- [ ] Wire "Download Certificate" → `GET /results/{candidateId}/scorecard`
- [ ] Wire `Layout.tsx` bell icon → `GET /notifications`
- [ ] Establish SSE connection → `GET /notifications/stream`
- [ ] Wire mark-as-read → `PATCH /notifications/{id}/read`

### Sprint 6 — Quality & Hardening (Week 8)
- [ ] Add `ErrorBoundary` components
- [ ] Add skeleton loaders to all pages
- [ ] Add shared `<Toast>` notification system
- [ ] Implement `react-i18next` + Hindi locale
- [ ] Fix a11y: aria labels, focus traps, WCAG contrast
- [ ] Replace `localStorage` JWT with `httpOnly` cookie / BFF
- [ ] Add CSP header to `nginx.conf`
- [ ] Load / stress test exam session endpoints

---

## 7. File Change Summary

### Frontend Files to Create
```
src/services/
  api.ts
  authService.ts
  candidateService.ts
  examService.ts
  sessionService.ts
  responseService.ts
  resultService.ts
  notificationService.ts

src/utils/
  tokenManager.ts
  errorHandler.ts
  offlineQueue.ts       (IndexedDB wrapper for exam buffering)

src/hooks/
  useAuth.ts
  useProfile.ts
  useExams.ts
  useNotifications.ts   (SSE)
  useExamSession.ts     (TakeExam state machine)

src/types/
  api.ts               (all DTO interfaces)
  auth.ts
  candidate.ts
  exam.ts
  session.ts
  result.ts

src/components/
  Toast.tsx
  ErrorBoundary.tsx
  Skeleton.tsx
  ConfirmModal.tsx

src/.env.local          (new)
```

### Frontend Files to Substantially Modify
```
src/context/AuthContext.tsx    → strip all mocks, delegate to services
src/pages/Login.tsx            → add React Hook Form + real API
src/pages/Register.tsx         → add identity doc fields + real API
src/pages/VerifyOtp.tsx        → real OTP + token storage
src/pages/Dashboard.tsx        → React Query for exams + results + notifications
src/pages/Profile.tsx          → GET on mount, real uploads
src/pages/PasswordMgmt.tsx     → real password change endpoint
src/pages/BrowseExams.tsx      → real paginated exam listing
src/pages/TakeExam.tsx         → full delivery wiring (session, nav, responses, proctoring)
src/pages/Results.tsx          → real result + PDF download
src/components/Layout.tsx      → SSE notifications bell
```

### Backend Files to Create
```
identity-service:
  controller/IdentityController.java       → add changePassword(), forgotPassword(), resetPassword(), refreshToken(), logout()
  dto/ChangePasswordRequest.java
  dto/ForgotPasswordRequest.java
  dto/RefreshTokenRequest.java

examination-service:
  controller/PublicExaminationController.java  → GET /public (no auth), POST /{id}/apply, GET /my-exams
  dto/ExamApplicationRequest.java
  dto/ExamApplicationResponse.java
  service/ExamApplicationService.java
  repository/ExamApplicationRepository.java

asset-service:
  controller/AssetController.java             → add CANDIDATE to @PreAuthorize on upload endpoint
```
