# Module Specification: Evaluation Service

## 1. Overview & Purpose

The **Evaluation Service** manages automated grading for objective question types (MCQ, MSQ, Numerical) and anonymized grading workflows for subjective answer scripts.

---

## 2. Core Responsibilities

- Automated grading engine calculating positive scores, negative marking subtractions, and net section totals.
- Evaluation anonymization: generating fictitious 128-bit evaluation tokens to mask candidate PII from evaluators.
- Single and double-evaluation rubrics for subjective questions.
- Result scorecard generation and rank aggregation.

---

## 3. Evaluation Schema

```sql
CREATE TABLE evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    candidate_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    total_raw_marks NUMERIC(6,2) NOT NULL,
    negative_marks NUMERIC(6,2) DEFAULT 0.00,
    final_score NUMERIC(6,2) NOT NULL,
    percentile NUMERIC(5,2),
    status VARCHAR(30) DEFAULT 'COMPUTED',
    evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_eval_candidate_exam ON evaluation_results(candidate_id, exam_id);
```

---

## 4. REST API Reference

Base Path: `/api/v1/evaluations`

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/trigger/{examId}` | EXAM_CONTROLLER | Trigger automated grading for exam |
| `GET` | `/subjective/queue` | EVALUATOR | Fetch anonymized subjective answer queue |
| `POST` | `/subjective/grade` | EVALUATOR | Submit grade score for answer script |
| `GET` | `/results/{candidateId}` | CANDIDATE, SUPER_ADMIN | Fetch candidate result scorecard |
