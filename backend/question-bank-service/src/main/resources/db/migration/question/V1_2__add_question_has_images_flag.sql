-- Issue #100: Add has_images flag for efficient visual-question identification
ALTER TABLE question_service.question
    ADD COLUMN IF NOT EXISTS has_images BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_question_has_images
    ON question_service.question(has_images)
    WHERE has_images = TRUE;

COMMENT ON COLUMN question_service.question.has_images
    IS 'Set TRUE when content, explanation, or any option contains image/SVG media.';
