SET search_path TO question_service;

CREATE TABLE question_version (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    question_id     UUID NOT NULL,
    author_id       UUID NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    diff_json       JSONB NOT NULL,
    snapshot_json   TEXT,           -- encrypted
    version_number  INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_question_version_number UNIQUE (question_id, version_number)
);

CREATE INDEX idx_question_version_question ON question_version(question_id);
