CREATE SCHEMA IF NOT EXISTS response_service;
SET search_path TO response_service;

-- ============================================================================
-- Range-partitioned response table (monthly by created_at)
-- ============================================================================
CREATE TABLE response (
    id                       UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(255) NOT NULL,
    session_id               UUID NOT NULL,
    question_id              UUID NOT NULL,
    candidate_id             UUID NOT NULL,
    selected_option_ids      JSONB,
    entered_value            TEXT,
    timestamp                TIMESTAMPTZ NOT NULL,
    cumulative_time_spent_ms BIGINT NOT NULL DEFAULT 0,
    revision_sequence        INTEGER NOT NULL DEFAULT 1,
    save_source              VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    is_final                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_response PRIMARY KEY (id, created_at),
    CONSTRAINT chk_save_source CHECK (save_source IN ('AUTO', 'MANUAL', 'NAVIGATION', 'OFFLINE'))
) PARTITION BY RANGE (created_at);

-- ============================================================================
-- Monthly partitions for the current year (12 months)
-- ============================================================================
CREATE TABLE response_y2025_m01 PARTITION OF response
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE response_y2025_m02 PARTITION OF response
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE response_y2025_m03 PARTITION OF response
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE response_y2025_m04 PARTITION OF response
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE response_y2025_m05 PARTITION OF response
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE response_y2025_m06 PARTITION OF response
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE response_y2025_m07 PARTITION OF response
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE response_y2025_m08 PARTITION OF response
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE response_y2025_m09 PARTITION OF response
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE response_y2025_m10 PARTITION OF response
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE response_y2025_m11 PARTITION OF response
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE response_y2025_m12 PARTITION OF response
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- ============================================================================
-- Indexes
-- ============================================================================

-- Composite index for efficient "latest answer" lookups per question in a session
CREATE INDEX idx_response_session_question_rev
    ON response (session_id, question_id, revision_sequence DESC);

-- Partial index for final response aggregation queries
CREATE INDEX idx_response_candidate_final
    ON response (candidate_id)
    WHERE is_final = TRUE;

-- Tenant isolation index
CREATE INDEX idx_response_tenant ON response (tenant_id);

-- Session + tenant for tenant-scoped session queries
CREATE INDEX idx_response_session_tenant ON response (session_id, tenant_id);
