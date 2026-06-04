CREATE SCHEMA IF NOT EXISTS paper_generator;

-- ============================================================
-- Table: paper
-- ============================================================
CREATE TABLE paper_generator.paper (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(255) NOT NULL,
    exam_id                 UUID NOT NULL,
    shift_id                VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    paper_definition_json   JSONB,
    difficulty_score        DOUBLE PRECISION,
    topic_distribution_json JSONB,
    encrypted_package_ref   VARCHAR(255),
    encryption_key_id       VARCHAR(255),
    generated_by            UUID,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_paper_tenant_id ON paper_generator.paper(tenant_id);
CREATE INDEX idx_paper_exam_id ON paper_generator.paper(exam_id);
CREATE INDEX idx_paper_shift_id ON paper_generator.paper(shift_id);
CREATE INDEX idx_paper_status ON paper_generator.paper(status);
