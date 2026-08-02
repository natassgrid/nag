# Module Specification: Question Bank Service

## 1. Overview & Purpose

The **Question Bank Service** provides authoring, tagging, review, and repository management for single choice (MCQ), multiple choice (MSQ), numerical, and subjective questions.

---

## 2. Core Responsibilities

- Question item creation with support for LaTeX mathematical formulas and image attachments.
- Subject, topic, and difficulty level classification (`EASY`, `MEDIUM`, `HARD`).
- Multi-stage peer review and approval workflow (`DRAFT` $\rightarrow$ `IN_REVIEW` $\rightarrow$ `APPROVED` / `REJECTED`).
- Automated encrypted storage of question body and correct option keys.

---

## 3. Data Model

### Question Item Entity
```sql
CREATE TABLE question_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    subject_id UUID NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    question_type VARCHAR(30) DEFAULT 'MCQ',
    difficulty_level VARCHAR(20) DEFAULT 'MEDIUM',
    encrypted_content TEXT NOT NULL, -- AES-256 encrypted question stem + options
    marks NUMERIC(4,2) NOT NULL,
    negative_marks NUMERIC(4,2) DEFAULT 0.00,
    status VARCHAR(30) DEFAULT 'DRAFT',
    created_by VARCHAR(128) NOT NULL,
    reviewed_by VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_questions_subject_diff ON question_items(subject_id, difficulty_level, status);
```

---

## 4. REST API Reference

Base Path: `/api/v1/questions`

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/` | QUESTION_AUTHOR | Draft new question item |
| `GET` | `/` | QUESTION_AUTHOR, REVIEWER | List questions |
| `PUT` | `/{id}/review` | REVIEWER, APPROVER | Approve or reject question item |
| `GET` | `/subjects` | QUESTION_AUTHOR, EXAM_CONTROLLER | List active subjects & topics |
