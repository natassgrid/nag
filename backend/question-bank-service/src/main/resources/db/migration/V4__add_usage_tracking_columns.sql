-- Add JSONB columns for tracking which exams/shifts a question has been used in
-- Validates: Requirements 4.8, 4.9

ALTER TABLE question_service.question
    ADD COLUMN IF NOT EXISTS used_in_exam_ids_json JSONB DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS used_in_shift_ids_json JSONB DEFAULT '[]'::jsonb;

-- Index for querying questions by exam usage
CREATE INDEX IF NOT EXISTS idx_question_used_in_exams
    ON question_service.question USING GIN (used_in_exam_ids_json);
