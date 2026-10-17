-- ============================================================
-- Flyway migration V3: Micro-Lesson Multilingual Translation Tables
-- Parity with question translation (IndicTrans2)
-- ============================================================

CREATE TABLE IF NOT EXISTS question_service.microlesson_translation (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    microlesson_id              UUID NOT NULL REFERENCES question_service.subtopic_micro_lesson(id) ON DELETE CASCADE,
    tenant_id                   VARCHAR(100) NOT NULL DEFAULT 'default',
    language_code               VARCHAR(10)  NOT NULL,
    core_concept                TEXT         NOT NULL,
    trap                        TEXT         NOT NULL,
    cheat_sheet                 JSONB        NOT NULL,
    step_by_step_breakdown      JSONB        NOT NULL,
    source_version              INTEGER      NOT NULL DEFAULT 1,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    translator_id               UUID,
    reviewer_id                 UUID,
    review_comments             TEXT,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_microlesson_trans_lang_tenant UNIQUE (microlesson_id, language_code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_microlesson_trans_lesson_id ON question_service.microlesson_translation(microlesson_id);
CREATE INDEX IF NOT EXISTS idx_microlesson_trans_lang_status ON question_service.microlesson_translation(microlesson_id, language_code, status);

CREATE TABLE IF NOT EXISTS question_service.microlesson_batch_translation_job (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL DEFAULT 'default',
    status                  VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    source_language         VARCHAR(10)   NOT NULL DEFAULT 'en',
    target_language         VARCHAR(10)   NOT NULL DEFAULT 'hi',
    target_status           VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    subject_filter          VARCHAR(100),
    topic_filter            VARCHAR(200),
    target_exam_filter      VARCHAR(50),
    lesson_ids              JSONB         DEFAULT '[]'::jsonb,
    overwrite_existing      BOOLEAN       NOT NULL DEFAULT TRUE,
    total_lessons           INTEGER       NOT NULL DEFAULT 0,
    processed_lessons       INTEGER       NOT NULL DEFAULT 0,
    successful_lessons      INTEGER       NOT NULL DEFAULT 0,
    failed_lessons          INTEGER       NOT NULL DEFAULT 0,
    failed_lesson_ids       JSONB         DEFAULT '[]'::jsonb,
    batch_size              INTEGER       NOT NULL DEFAULT 20,
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

CREATE INDEX IF NOT EXISTS idx_ml_batch_trans_job_tenant_created ON question_service.microlesson_batch_translation_job (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_batch_trans_job_status ON question_service.microlesson_batch_translation_job (status, created_at ASC);
