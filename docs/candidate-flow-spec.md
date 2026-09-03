# Candidate Journey Specification: Self-Registration, Multi-step Exam Application, Hall Ticket Generation & CBT Delivery

## 1. Overview & Objective
This specification defines the end-to-end architecture and implementation details for candidate self-service within the **National Assessment Grid (NAG)**. It encompasses:
1. **Candidate Onboarding & Self-Registration**: Identity provisioning, OTP verification, DPDP consent, profile management with masked PII and HMAC deduplication.
2. **Exam Discovery & Multi-Step Application**: Browsing published government exams, selecting 3 examination centre preferences across India, shift preference, accommodation needs (PwD/Scribe), and instant confirmation.
3. **Admit Card / Hall Ticket Issuance**: Automated generation of tamper-evident admit cards containing unique hall ticket numbers (`HT-YYYY-XXXXXX`), assigned centre/venue details, shift schedule, gate timings, candidate metadata, and verification QR code.
4. **Computer-Based Test (CBT) Delivery Engine**: Secure, fullscreen lockdown test environment with multi-section tabs, NTA-standard question status palette, auto-save response pipeline with revision sequences, offline buffering in `IndexedDB`, and automatic submission safeguards.

---

## 2. Architecture & Data Flow

```mermaid
flowchart TD
    subgraph Candidate_Portal ["candidate-frontend (React 19 + Tailwind)"]
        REG[Self-Registration & OTP]
        PROF[Profile & DigiLocker / Aadhaar]
        BROWSE[Exam Discovery Catalog]
        APPLY[Multi-Step Application Modal]
        ADMIT[Admit Card Viewer & PDF Download]
        CBT[CBT Fullscreen Delivery Engine]
    end

    subgraph API_Gateway ["Spring Cloud Gateway (:8080)"]
        GW[JWT & Route Filters]
    end

    subgraph Microservices ["Backend Microservices"]
        IS[identity-service :8081]
        CS[candidate-service :8087]
        ES[examination-service :8085]
        DS[delivery-service :8088]
        RS[response-service :8089]
    end

    REG -->|POST /auth/register & verify| GW --> IS
    PROF -->|GET/PUT /candidates/profile| GW --> CS
    BROWSE -->|GET /examinations/public| GW --> ES
    APPLY -->|POST /examinations/{id}/apply| GW --> ES
    ADMIT -->|GET /examinations/{id}/admit-card| GW --> ES
    CBT -->|POST /sessions/start| GW --> DS
    CBT -->|POST /responses/save & submit| GW --> RS
```

---

## 3. Microservice Contracts & Data Models

### 3.1 `examination-service` Enhancements

#### A. Data Model Updates (`examination_service.exam_application`)
```sql
ALTER TABLE examination_service.exam_application
    ADD COLUMN IF NOT EXISTS first_choice_centre_id UUID REFERENCES examination_service.examination_centre(id),
    ADD COLUMN IF NOT EXISTS second_choice_centre_id UUID REFERENCES examination_service.examination_centre(id),
    ADD COLUMN IF NOT EXISTS third_choice_centre_id UUID REFERENCES examination_service.examination_centre(id),
    ADD COLUMN IF NOT EXISTS preferred_shift_id UUID REFERENCES examination_service.exam_shift(id),
    ADD COLUMN IF NOT EXISTS allocated_centre_id UUID REFERENCES examination_service.examination_centre(id),
    ADD COLUMN IF NOT EXISTS allocated_shift_id UUID REFERENCES examination_service.exam_shift(id),
    ADD COLUMN IF NOT EXISTS pwd_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scribe_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS qr_verification_hash VARCHAR(128);
```

#### B. API Endpoints
1. `GET /api/v1/examinations/centres/public`
   - Returns list of active examination centres (ID, name, city, state, address, capacity) for dropdowns.
2. `POST /api/v1/examinations/{examId}/apply`
   - Accepts `ExamApplicationRequest` (centre choices 1, 2, 3, preferred shift, PwD/scribe flags).
   - Generates Hall Ticket (`HT-2026-XXXXXX`) and allocates available centre/shift.
3. `GET /api/v1/examinations/{examId}/admit-card` & `GET /api/v1/examinations/applications/{appId}/admit-card`
   - Returns `AdmitCardResponse` with full candidate, venue, shift, instructions, and QR code payload.

---

## 4. Frontend UI/UX Specifications (`candidate-frontend`)

### 4.1 Multi-Step Application Modal (`ApplyExamModal.tsx`)
- **Step 1: Eligibility & Instructions**: Displays conducting authority, total marks, duration, syllabus, and requires candidate acknowledgement.
- **Step 2: Test Centre Preferences**: 3-level cascade allowing candidate to select 1st, 2nd, and 3rd preferred examination cities/centres across India with search and filter.
- **Step 3: Shift & Accommodations**: Shift preference, PwD category, and Scribe assistance options.
- **Step 4: Review & Submit**: Summary card and submission button; upon success, immediate transition to "Application Confirmed" with "View Admit Card" CTA.

### 4.2 Hall Ticket / Admit Card Viewer (`AdmitCardModal.tsx`)
- Standardized national format with:
  - Official Government Header ("National Assessment Grid - Admit Card / Hall Ticket")
  - Candidate Photograph & Signature placeholder
  - Candidate Name, Roll / Hall Ticket Number, Application ID
  - Exam Details: Name, Code, Exam Date, Shift, Reporting Time, Gate Closing Time, Duration
  - Examination Centre: Centre Name, Full Address, City, State, Landmark, Lab Number
  - Tamper-Evident QR Code encoding verification payload
  - Important Examination Day Guidelines (ID proofs, banned items, biometric verification)
  - Action buttons: `Print / Save as PDF` & `Close`.

### 4.3 Enhanced CBT Delivery Engine (`TakeExam.tsx`)
- **Section Tabs**: Tabbed navigation between test sections (e.g. Reasoning, General Awareness, Quantitative Aptitude, English) displaying remaining questions per section.
- **Color-Coded Question Palette (NTA/Standard CBT)**:
  - 🟩 **Green**: Answered
  - 🟥 **Red**: Not Answered
  - 🟨 **Amber**: Marked for Review
  - 🟪 **Purple**: Answered & Marked for Review
  - ⬜ **Gray**: Not Visited
- **Fullscreen Lockdown Pre-Check**: Pre-exam modal checking screen resolution, fullscreen permission, and candidate readiness.
- **Autosave & Offline Sync**: Visual heartbeat bar showing `Saved to Cloud` vs `Buffering in Offline Queue`.
