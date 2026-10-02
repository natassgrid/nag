-- SPDX-License-Identifier: AGPL-3.0-only
-- NAG Contributors 2025

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

CREATE INDEX idx_learner_profile_candidate ON recommendation_service.learner_profile(candidate_id);
CREATE INDEX idx_recommendation_candidate ON recommendation_service.recommendation(candidate_id);
CREATE INDEX idx_recommendation_status ON recommendation_service.recommendation(status);
CREATE INDEX idx_recommendation_created ON recommendation_service.recommendation(candidate_id, created_at DESC);
