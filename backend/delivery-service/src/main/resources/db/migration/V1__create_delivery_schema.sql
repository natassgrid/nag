CREATE SCHEMA IF NOT EXISTS delivery_service;
SET search_path TO delivery_service;

CREATE TABLE exam_session (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(255) NOT NULL,
    session_id               UUID NOT NULL UNIQUE,
    candidate_id             UUID NOT NULL,
    exam_id                  UUID NOT NULL,
    shift_id                 UUID NOT NULL,
    paper_id                 UUID NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at               TIMESTAMPTZ NOT NULL,
    scheduled_end_at         TIMESTAMPTZ NOT NULL,
    current_question_index   INTEGER NOT NULL DEFAULT 0,
    language_code            VARCHAR(10) NOT NULL DEFAULT 'en',
    full_screen_exit_count   INTEGER NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUBMITTED', 'EXPIRED'))
);

CREATE INDEX idx_session_tenant ON exam_session(tenant_id);
CREATE INDEX idx_session_candidate_tenant ON exam_session(candidate_id, tenant_id);
CREATE INDEX idx_session_status_tenant ON exam_session(status, tenant_id);
CREATE INDEX idx_session_shift ON exam_session(shift_id);
CREATE INDEX idx_session_exam ON exam_session(exam_id);
