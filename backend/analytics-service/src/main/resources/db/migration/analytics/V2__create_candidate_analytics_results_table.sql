-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Table: candidate_analytics_results
-- Stores individual candidate evaluation result records for analytics
-- aggregation and on-demand recomputations.
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_service.candidate_analytics_results (
    id                          UUID PRIMARY KEY,
    exam_id                     UUID NOT NULL,
    candidate_id                UUID NOT NULL,
    session_id                  UUID,
    total_raw_score             NUMERIC(10,2) NOT NULL,
    section_scores_json         JSONB,
    question_level_scores_json  JSONB,
    tenant_id                   VARCHAR(100),
    evaluated_at                TIMESTAMP NOT NULL,
    created_at                  TIMESTAMP NOT NULL,
    CONSTRAINT uq_candidate_analytics_exam UNIQUE (exam_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_candidate_analytics_exam_id ON analytics_service.candidate_analytics_results(exam_id);
