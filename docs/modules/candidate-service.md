# Module Specification: Candidate Service

## 1. Overview & Purpose

The **Candidate Service** manages candidate profile registrations, identity verification, roll number generation, biometrics metadata logging, and candidate exam portal session tokens.

---

## 2. Core Responsibilities

- Candidate profile registration, updates, and document verification status.
- Unique Roll Number and Registration ID assignment per examination cycle.
- Biometric template metadata registration (fingerprint, photo hash).
- Candidate portal dashboard data aggregation (active exams, hall tickets, results).

---

## 3. Data Model

### Candidate Entity
```sql
CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    national_id_hash VARCHAR(64) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    pwd_status BOOLEAN DEFAULT FALSE,
    profile_status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_candidates_tenant_user ON candidates(tenant_id, user_id);
```

---

## 4. REST API Reference

Base Path: `/api/v1/candidates`

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/register` | Public / CANDIDATE | Register new candidate profile |
| `GET` | `/me` | CANDIDATE | Get current logged-in candidate profile |
| `PUT` | `/me` | CANDIDATE | Update editable profile fields |
| `GET` | `/{candidateId}/hall-ticket/{examId}` | CANDIDATE, SUPER_ADMIN | Download hall ticket manifest |

---

## 5. Event Specifications

- **Published Topic**: `candidate.events`
  - Event: `CandidateRegisteredEvent` `{ candidateId, tenantId, email, timestamp }`
  - Event: `HallTicketIssuedEvent` `{ candidateId, examId, rollNumber, centerId, shiftId }`
