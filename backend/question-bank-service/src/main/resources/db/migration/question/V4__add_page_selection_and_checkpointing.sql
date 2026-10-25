-- ============================================================
-- V4__add_page_selection_and_checkpointing.sql
-- Add page selection, last completed page checkpointing,
-- and ensure QUOTA_EXHAUSTED status support for question paper extraction.
-- ============================================================

ALTER TABLE question_service.question_paper_metadata
    ADD COLUMN IF NOT EXISTS page_selection VARCHAR(500);

ALTER TABLE question_service.question_paper_metadata
    ADD COLUMN IF NOT EXISTS last_completed_page INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_qpm_extraction_status'
    ) THEN
        ALTER TABLE question_service.question_paper_metadata
            DROP CONSTRAINT chk_qpm_extraction_status;
        ALTER TABLE question_service.question_paper_metadata
            ADD CONSTRAINT chk_qpm_extraction_status
            CHECK (extraction_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'PARTIAL_FAILED', 'FAILED', 'CANCELLED', 'QUOTA_EXHAUSTED'));
    END IF;
END $$;
