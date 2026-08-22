-- Batch generation job tracking table.
-- A single job aggregates multiple generation items into one Bedrock batch inference call.

CREATE TABLE IF NOT EXISTS question_service.batch_generation_job (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              VARCHAR(100)  NOT NULL,
    status                 VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    items                  JSONB         NOT NULL DEFAULT '[]'::jsonb,
    total_requested        INTEGER       NOT NULL,
    total_generated        INTEGER       NOT NULL DEFAULT 0,
    total_failed           INTEGER       NOT NULL DEFAULT 0,
    total_duplicates       INTEGER       NOT NULL DEFAULT 0,
    model_used             VARCHAR(100),
    avoid_duplicates       BOOLEAN       NOT NULL DEFAULT TRUE,
    initiated_by           UUID          NOT NULL,
    started_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    error_message          TEXT,
    bedrock_job_arn        VARCHAR(500),
    generated_question_ids JSONB         DEFAULT '[]'::jsonb,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_batch_job_tenant_created
    ON question_service.batch_generation_job (tenant_id, created_at DESC);

CREATE INDEX idx_batch_job_status
    ON question_service.batch_generation_job (status, created_at ASC);

CREATE INDEX idx_batch_job_user
    ON question_service.batch_generation_job (initiated_by, tenant_id, created_at DESC);
