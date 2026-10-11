-- SPDX-License-Identifier: AGPL-3.0-only
-- NAG Contributors 2026
-- Creates learner_subtopic_performance table and adds subtopic_accuracy_map to learner_profile

CREATE TABLE IF NOT EXISTS recommendation_service.learner_subtopic_performance (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(255) NOT NULL,
    candidate_id            UUID NOT NULL,
    subject_id              BIGINT,
    topic_id                BIGINT,
    subtopic_id             BIGINT,
    subject_name            VARCHAR(100) NOT NULL,
    topic_name              VARCHAR(200) NOT NULL,
    subtopic_name           VARCHAR(200) NOT NULL,
    total_questions         INTEGER NOT NULL DEFAULT 0,
    total_attempts          INTEGER NOT NULL DEFAULT 0,
    total_correct           INTEGER NOT NULL DEFAULT 0,
    total_skipped           INTEGER NOT NULL DEFAULT 0,
    avg_accuracy_percent    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    avg_pace_ratio          DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    avg_skip_rate_percent   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    last_tested_at          TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_learner_subtopic UNIQUE (candidate_id, subtopic_name)
);

CREATE INDEX IF NOT EXISTS idx_learner_subtopic_perf ON recommendation_service.learner_subtopic_performance(candidate_id, avg_accuracy_percent);

ALTER TABLE recommendation_service.learner_profile
    ADD COLUMN IF NOT EXISTS subtopic_accuracy_map JSONB;
