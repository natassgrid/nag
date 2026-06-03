CREATE SCHEMA IF NOT EXISTS evaluation_service;
SET search_path TO evaluation_service;

CREATE TABLE evaluation (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         VARCHAR(255) NOT NULL,
    session_id        UUID NOT NULL,
    question_id       UUID NOT NULL,
    candidate_id      UUID NOT NULL,
    evaluation_type   VARCHAR(10) NOT NULL,
    evaluator_id      UUID,
    score             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    max_marks         NUMERIC(10, 2) NOT NULL,
    negative_marks    NUMERIC(10, 2) NOT NULL DEFAULT 0,
    comments          TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_evaluation_type CHECK (evaluation_type IN ('AUTO', 'MANUAL')),
    CONSTRAINT chk_evaluation_status CHECK (status IN ('PENDING', 'AUTO_EVALUATED', 'MANUAL_EVALUATED', 'ARBITRATION', 'FINALIZED'))
);

CREATE INDEX idx_evaluation_tenant ON evaluation(tenant_id);
CREATE INDEX idx_evaluation_session_tenant ON evaluation(session_id, tenant_id);
CREATE INDEX idx_evaluation_candidate_tenant ON evaluation(candidate_id, tenant_id);
CREATE INDEX idx_evaluation_status_tenant ON evaluation(status, tenant_id);
