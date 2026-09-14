-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- Add diagnostic and analytics fields to result table (SPEC-RS1)
ALTER TABLE result_service.result
    ADD COLUMN IF NOT EXISTS accuracy_rate            NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS category_rank            INT,
    ADD COLUMN IF NOT EXISTS sectional_status_json    JSONB,
    ADD COLUMN IF NOT EXISTS cognitive_breakdown_json JSONB,
    ADD COLUMN IF NOT EXISTS topic_breakdown_json     JSONB,
    ADD COLUMN IF NOT EXISTS time_analysis_json       JSONB,
    ADD COLUMN IF NOT EXISTS qr_verification_code     VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_result_qr_code
    ON result_service.result(qr_verification_code)
    WHERE qr_verification_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_result_exam_rank
    ON result_service.result(exam_id, overall_rank);

COMMENT ON COLUMN result_service.result.accuracy_rate IS 'Accuracy: correct / attempted questions';
COMMENT ON COLUMN result_service.result.category_rank IS 'Rank within candidate category (OBC/SC/ST/GEN)';
COMMENT ON COLUMN result_service.result.cognitive_breakdown_json IS 'Bloom taxonomy breakdown: {REMEMBER:85, UNDERSTAND:70, APPLY:55, ANALYZE:40}';
COMMENT ON COLUMN result_service.result.topic_breakdown_json IS 'Topic-wise score breakdown: {Thermodynamics:{score:12,maxScore:16}}';
COMMENT ON COLUMN result_service.result.time_analysis_json IS 'Time diagnostics: {avgTimePerQuestion:65000, timeOnCorrect:72000, timeOnIncorrect:45000}';
COMMENT ON COLUMN result_service.result.qr_verification_code IS 'UUID token for QR-based scorecard verification at /api/v1/results/verify';
