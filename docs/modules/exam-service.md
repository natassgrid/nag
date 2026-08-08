# Module Specification: Exam Management Service

## 1. Overview & Purpose

The **Exam Management Service** defines core examination metadata, test duration, marking schemes (positive/negative marking), navigation policies, calculator policies, and exam lifecycle publishing.

---

## 2. Core Responsibilities

- Creation and maintenance of examination definitions.
- Configuration of section structure, question count, and marks allocation.
- Enforcement of navigation policies (`LINEAR_ONLY`, `FREE_NAVIGATION`, `SECTION_LOCKED`).
- Examination lifecycle state management (`DRAFT`, `PUBLISHED`, `ARCHIVED`).

---

## 3. Data Model

### Examination Entity
```sql
CREATE TABLE examinations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    total_marks INT NOT NULL CHECK (total_marks > 0),
    negative_marking_enabled BOOLEAN DEFAULT FALSE,
    negative_marking_value NUMERIC(4,2) DEFAULT 0.00,
    navigation_policy VARCHAR(50) DEFAULT 'FREE_NAVIGATION',
    calculator_policy VARCHAR(50) DEFAULT 'DISALLOWED',
    review_flag_enabled BOOLEAN DEFAULT TRUE,
    sections JSONB NOT NULL, -- Array of { name, questionCount, marksPerQuestion }
    status VARCHAR(30) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_examinations_tenant ON examinations(tenant_id);
```

---

## 4. REST API Reference

Base Path: `/api/v1/examinations`

| Method | Path | Roles | Description |
|---|---|---|---|
| `GET` | `/` | EXAM_CONTROLLER, SUPER_ADMIN | List all examinations |
| `POST` | `/` | EXAM_CONTROLLER | Create new examination draft |
| `GET` | `/{id}` | EXAM_CONTROLLER, SUPER_ADMIN, CANDIDATE | Get exam definition by ID |
| `PUT` | `/{id}` | EXAM_CONTROLLER | Update exam configuration |
| `PUT` | `/{id}/publish` | EXAM_CONTROLLER, SUPER_ADMIN | Publish examination |

---

## 5. Event Specifications

- **Published Topic**: `exam.events`
  - Event: `ExamCreatedEvent` `{ examId, name, tenantId }`
  - Event: `ExamPublishedEvent` `{ examId, durationMinutes, totalMarks }`
