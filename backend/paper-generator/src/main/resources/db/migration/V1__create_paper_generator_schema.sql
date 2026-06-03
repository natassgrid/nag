CREATE SCHEMA IF NOT EXISTS paper_generator;
SET search_path TO paper_generator;

CREATE TABLE paper (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(255) NOT NULL,
    exam_id                 UUID NOT NULL,
    shift_id                VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    paper_definition_json   JSONB,
    difficulty_score        DOUBLE PRECISION DEFAULT 0.0,
    topic_distribution_json JSONB,
    encrypted_package_ref   TEXT,
    encryption_key_id       VARCHAR(255),
    generated_by            UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_paper_status CHECK (status IN ('DRAFT', 'APPROVED', 'ENCRYPTED'))
);

CREATE INDEX idx_paper_exam_id ON paper(exam_id);
CREATE INDEX idx_paper_shift_id ON paper(shift_id);
CREATE INDEX idx_paper_tenant ON paper(tenant_id);
CREATE INDEX idx_paper_status ON paper(status);
CREATE INDEX idx_paper_exam_shift ON paper(exam_id, shift_id);
