CREATE SCHEMA IF NOT EXISTS analytics_service;

-- ============================================================
-- Table: exam_analytics
-- Does NOT extend BaseEntity. Uses @GeneratedValue(UUID).
-- ============================================================
CREATE TABLE analytics_service.exam_analytics (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id                         UUID NOT NULL,
    total_registered                BIGINT NOT NULL,
    total_appeared                  BIGINT NOT NULL,
    score_distribution_json         JSONB,
    section_averages_json           JSONB,
    top_10_percentile_threshold     NUMERIC(10,4),
    bottom_10_percentile_threshold  NUMERIC(10,4),
    computed_at                     TIMESTAMP NOT NULL
);

CREATE INDEX idx_exam_analytics_exam_id ON analytics_service.exam_analytics(exam_id);
