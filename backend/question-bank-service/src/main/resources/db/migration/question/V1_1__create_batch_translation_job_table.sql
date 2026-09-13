-- ============================================================
-- Migration: V1_1__create_batch_translation_job_table.sql
-- Creates batch_translation_job table for asynchronous question translation
-- ============================================================

CREATE TABLE IF NOT EXISTS question_service.batch_translation_job (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    source_language         VARCHAR(10)   NOT NULL DEFAULT 'en',
    target_language         VARCHAR(10)   NOT NULL DEFAULT 'hi',
    target_status           VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    subject_filter          VARCHAR(100),
    overwrite_existing      BOOLEAN       NOT NULL DEFAULT TRUE,
    total_questions         INTEGER       NOT NULL DEFAULT 0,
    processed_questions     INTEGER       NOT NULL DEFAULT 0,
    successful_questions    INTEGER       NOT NULL DEFAULT 0,
    failed_questions        INTEGER       NOT NULL DEFAULT 0,
    failed_question_ids     JSONB         DEFAULT '[]'::jsonb,
    batch_size              INTEGER       NOT NULL DEFAULT 50,
    throttle_delay_ms       INTEGER       NOT NULL DEFAULT 50,
    max_concurrency         INTEGER       NOT NULL DEFAULT 2,
    initiated_by            UUID          NOT NULL,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                 BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_batch_trans_job_tenant_created
    ON question_service.batch_translation_job (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_batch_trans_job_status
    ON question_service.batch_translation_job (status, created_at ASC);
