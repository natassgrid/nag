-- SPDX-License-Identifier: AGPL-3.0-only
-- NAG Contributors 2026

CREATE SCHEMA IF NOT EXISTS recommendation_service;

CREATE TABLE recommendation_service.learner_profile (
    id                      UUID PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    candidate_id            UUID NOT NULL UNIQUE,
    last_updated_at         TIMESTAMP,
    total_practice_sessions INTEGER NOT NULL DEFAULT 0,
    total_questions_attempted INTEGER NOT NULL DEFAULT 0,
    overall_accuracy        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    topic_accuracy_map      JSONB,
    subtopic_accuracy_map   JSONB,
    weak_topics             JSONB,
    strong_topics           JSONB,
    difficulty_accuracy_map JSONB,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE recommendation_service.recommendation (
    id                         UUID PRIMARY KEY,
    tenant_id                  VARCHAR(255) NOT NULL,
    candidate_id               UUID NOT NULL,
    trigger_session_id         UUID,
    status                     VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    generated_at               TIMESTAMP,
    weak_topic_recommendations JSONB,
    study_plan_items           JSONB,
    suggested_practice_set_ids JSONB,
    motivational_message       TEXT,
    model_used                 VARCHAR(100),
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    version                    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE recommendation_service.learner_subtopic_performance (
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

CREATE TABLE recommendation_service.dynamic_learning_path (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(255) NOT NULL DEFAULT 'default',
    candidate_id            UUID NOT NULL,
    source_session_id       UUID,
    exam_target             VARCHAR(50) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    conceptual_gaps_count   INTEGER NOT NULL DEFAULT 0,
    application_gaps_count  INTEGER NOT NULL DEFAULT 0,
    proficient_count        INTEGER NOT NULL DEFAULT 0,
    milestone_graph_json    JSONB NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_learner_profile_candidate ON recommendation_service.learner_profile(candidate_id);
CREATE INDEX idx_recommendation_candidate ON recommendation_service.recommendation(candidate_id);
CREATE INDEX idx_recommendation_status ON recommendation_service.recommendation(status);
CREATE INDEX idx_recommendation_created ON recommendation_service.recommendation(candidate_id, created_at DESC);
CREATE INDEX idx_learner_subtopic_perf ON recommendation_service.learner_subtopic_performance(candidate_id, avg_accuracy_percent);
CREATE INDEX idx_dlp_candidate ON recommendation_service.dynamic_learning_path(candidate_id, status);
CREATE INDEX idx_dlp_candidate_created ON recommendation_service.dynamic_learning_path(candidate_id, created_at DESC);
