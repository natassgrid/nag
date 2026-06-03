CREATE SCHEMA IF NOT EXISTS examination_service;
SET search_path TO examination_service;

CREATE TABLE examination (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 VARCHAR(100) NOT NULL,
    name                      VARCHAR(500) NOT NULL,
    duration_minutes          INTEGER NOT NULL,
    total_marks               INTEGER NOT NULL,
    negative_marking_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    negative_marking_value    DOUBLE PRECISION DEFAULT 0.0,
    navigation_policy         VARCHAR(20) NOT NULL DEFAULT 'FLEXIBLE',
    calculator_policy         VARCHAR(20) NOT NULL DEFAULT 'NONE',
    review_flag_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    sections_json             JSONB,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                   BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_examination_status_tenant ON examination(status, tenant_id);
CREATE INDEX idx_examination_tenant ON examination(tenant_id);
