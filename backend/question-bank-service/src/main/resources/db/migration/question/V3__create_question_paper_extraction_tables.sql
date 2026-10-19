-- ============================================================
-- V3__create_question_paper_extraction_tables.sql
-- Question Paper Metadata & Staging Extracted Questions Tables
-- ============================================================

CREATE TABLE IF NOT EXISTS question_service.question_paper_metadata (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(255) NOT NULL DEFAULT 'default',
    examination_name         VARCHAR(255) NOT NULL,
    exam_year                INTEGER      NOT NULL,
    department               VARCHAR(255),
    paper_code               VARCHAR(100),
    booklet_serial_no        VARCHAR(100),
    subject_name             VARCHAR(255),
    default_subject_id       BIGINT REFERENCES question_service.subject(id),
    default_topic_id         BIGINT REFERENCES question_service.topic(id),
    default_subtopic_id      BIGINT REFERENCES question_service.subtopic(id),
    languages_json           JSONB        NOT NULL DEFAULT '["en", "hi"]'::jsonb,
    total_pages              INTEGER      NOT NULL DEFAULT 0,
    total_questions_expected INTEGER      NOT NULL DEFAULT 0,
    duration_minutes         INTEGER,
    max_marks                DOUBLE PRECISION,
    negative_marking_ratio   DOUBLE PRECISION,
    sections_metadata_json   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    file_asset_id            UUID,
    original_filename        VARCHAR(500),
    file_storage_path        VARCHAR(1000),
    file_sha256              VARCHAR(128),
    extraction_status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    model_used               VARCHAR(100),
    pages_processed          INTEGER      NOT NULL DEFAULT 0,
    questions_extracted      INTEGER      NOT NULL DEFAULT 0,
    questions_approved       INTEGER      NOT NULL DEFAULT 0,
    questions_promoted       INTEGER      NOT NULL DEFAULT 0,
    failed_pages_json        JSONB        NOT NULL DEFAULT '[]'::jsonb,
    error_message            TEXT,
    uploaded_by              UUID         NOT NULL,
    started_at               TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                  BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_qpm_tenant_created
    ON question_service.question_paper_metadata (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_qpm_exam_year_dept
    ON question_service.question_paper_metadata (tenant_id, examination_name, exam_year, department);

CREATE INDEX IF NOT EXISTS idx_qpm_status
    ON question_service.question_paper_metadata (extraction_status, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_qpm_file_sha256
    ON question_service.question_paper_metadata (tenant_id, file_sha256)
    WHERE file_sha256 IS NOT NULL;

-- ============================================================
-- Table: extracted_question
-- Staging table for questions extracted from uploaded PDF papers
-- before review/approval and promotion to question_service.question
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.extracted_question (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(255) NOT NULL DEFAULT 'default',
    paper_metadata_id        UUID         NOT NULL REFERENCES question_service.question_paper_metadata(id) ON DELETE CASCADE,
    question_number          INTEGER      NOT NULL,
    source_page_number       INTEGER      NOT NULL,
    section_name             VARCHAR(255),
    question_type            VARCHAR(30)  NOT NULL DEFAULT 'SINGLE_MCQ',
    content                  TEXT         NOT NULL,
    options                  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    answer_key               TEXT,
    explanation              TEXT,
    passage_text             TEXT,
    translations_json        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    has_images               BOOLEAN      NOT NULL DEFAULT FALSE,
    has_tables               BOOLEAN      NOT NULL DEFAULT FALSE,
    has_formulas             BOOLEAN      NOT NULL DEFAULT FALSE,
    media_urls_json          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    suggested_subject_id     BIGINT REFERENCES question_service.subject(id),
    suggested_topic_id       BIGINT REFERENCES question_service.topic(id),
    suggested_subtopic_id    BIGINT REFERENCES question_service.subtopic(id),
    suggested_difficulty     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    suggested_cognitive_level VARCHAR(20) NOT NULL DEFAULT 'UNDERSTAND',
    extraction_confidence    DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    parsing_tier             VARCHAR(40)  NOT NULL DEFAULT 'TIER_0_LOCAL_TEXT',
    raw_llm_payload_json     JSONB,
    status                   VARCHAR(30)  NOT NULL DEFAULT 'EXTRACTED',
    duplicate_of_question_id UUID,
    promoted_question_id     UUID,
    reviewer_id              UUID,
    review_comments          TEXT,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_extracted_question_paper_qnum
        UNIQUE (paper_metadata_id, question_number)
);

CREATE INDEX IF NOT EXISTS idx_eq_paper_metadata_id
    ON question_service.extracted_question (paper_metadata_id, question_number ASC);

CREATE INDEX IF NOT EXISTS idx_eq_tenant_status
    ON question_service.extracted_question (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_eq_paper_status
    ON question_service.extracted_question (paper_metadata_id, status);

CREATE INDEX IF NOT EXISTS idx_eq_promoted_question_id
    ON question_service.extracted_question (promoted_question_id)
    WHERE promoted_question_id IS NOT NULL;
