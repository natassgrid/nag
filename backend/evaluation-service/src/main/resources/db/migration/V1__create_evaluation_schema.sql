CREATE SCHEMA IF NOT EXISTS evaluation_service;

-- ============================================================
-- Table: evaluation
-- ============================================================
CREATE TABLE evaluation_service.evaluation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    session_id      UUID NOT NULL,
    question_id     UUID NOT NULL,
    candidate_id    UUID NOT NULL,
    evaluation_type VARCHAR(10) NOT NULL,
    evaluator_id    UUID,
    score           NUMERIC(10,2) NOT NULL,
    max_marks       NUMERIC(10,2) NOT NULL,
    negative_marks  NUMERIC(10,2) NOT NULL,
    comments        TEXT,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_evaluation_tenant_id ON evaluation_service.evaluation(tenant_id);
CREATE INDEX idx_evaluation_session_id ON evaluation_service.evaluation(session_id);
CREATE INDEX idx_evaluation_candidate_id ON evaluation_service.evaluation(candidate_id);
CREATE INDEX idx_evaluation_status ON evaluation_service.evaluation(status);
