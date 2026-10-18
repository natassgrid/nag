-- SPDX-License-Identifier: AGPL-3.0-only
-- V3__create_learner_tables.sql
-- Unified Exam Spine: Learner profile, enrollment, and diagnostic tables

CREATE TABLE candidate_service.learner_profile (
    candidate_id            UUID PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL DEFAULT 'default',
    display_name            VARCHAR(255),
    education_level         VARCHAR(100),
    preferred_language      VARCHAR(50),
    daily_study_minutes     INTEGER,
    preferred_lesson_types  JSONB,
    prior_experience        TEXT,
    target_date             DATE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP
);

CREATE INDEX idx_learner_profile_tenant ON candidate_service.learner_profile(tenant_id);

CREATE TABLE candidate_service.learner_enrollment (
    id                      UUID PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL DEFAULT 'default',
    candidate_id            UUID NOT NULL,
    examination_id          UUID NOT NULL,
    target_date             DATE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    diagnostic_session_id   UUID,
    diagnostic_status       VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    started_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_learner_enrollment_candidate ON candidate_service.learner_enrollment(candidate_id);
CREATE INDEX idx_learner_enrollment_candidate_status ON candidate_service.learner_enrollment(candidate_id, status);
CREATE INDEX idx_learner_enrollment_exam ON candidate_service.learner_enrollment(examination_id);

CREATE TABLE candidate_service.diagnostic_session (
    id                      UUID PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL DEFAULT 'default',
    enrollment_id           UUID NOT NULL REFERENCES candidate_service.learner_enrollment(id),
    candidate_id            UUID NOT NULL,
    examination_id          UUID NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    questions_json          JSONB,
    responses_json          JSONB,
    score                   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_questions         INTEGER NOT NULL DEFAULT 0,
    correct_count           INTEGER NOT NULL DEFAULT 0,
    subtopic_results_json   JSONB,
    started_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_diagnostic_session_enrollment ON candidate_service.diagnostic_session(enrollment_id);
CREATE INDEX idx_diagnostic_session_candidate ON candidate_service.diagnostic_session(candidate_id);
