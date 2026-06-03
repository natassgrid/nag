-- Analytics Service Schema
CREATE SCHEMA IF NOT EXISTS analytics_service;

SET search_path TO analytics_service;

CREATE TABLE exam_analytics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID NOT NULL,
    total_registered BIGINT NOT NULL DEFAULT 0,
    total_appeared  BIGINT NOT NULL DEFAULT 0,
    score_distribution_json JSONB,
    section_averages_json   JSONB,
    top_10_percentile_threshold NUMERIC(10, 4),
    bottom_10_percentile_threshold NUMERIC(10, 4),
    computed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exam_analytics_exam_id ON exam_analytics(exam_id);
CREATE INDEX idx_exam_analytics_computed_at ON exam_analytics(computed_at DESC);
