CREATE SCHEMA IF NOT EXISTS result_service;
SET search_path TO result_service;

CREATE TABLE result (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             VARCHAR(255) NOT NULL,
    candidate_id          UUID NOT NULL,
    exam_id               UUID NOT NULL,
    total_score           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    section_scores_json   JSONB,
    overall_rank          INTEGER,
    overall_percentile    NUMERIC(6, 3),
    scorecard_pdf_ref     VARCHAR(500),
    digi_locker_pushed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_result_tenant ON result(tenant_id);
CREATE INDEX idx_result_candidate_tenant ON result(candidate_id, tenant_id);
CREATE INDEX idx_result_exam_tenant ON result(exam_id, tenant_id);
CREATE UNIQUE INDEX idx_result_candidate_exam_tenant ON result(candidate_id, exam_id, tenant_id);
