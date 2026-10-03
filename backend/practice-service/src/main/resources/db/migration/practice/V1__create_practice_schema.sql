-- SPDX-License-Identifier: AGPL-3.0-only
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

CREATE SCHEMA IF NOT EXISTS practice_service;

CREATE TABLE practice_service.practice_set (
    id                UUID PRIMARY KEY,
    tenant_id         VARCHAR(255) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    created_by        UUID NOT NULL,
    source            VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    question_ids      JSONB,
    duration_minutes  INTEGER NOT NULL DEFAULT 60,
    subject_slug      VARCHAR(100),
    published         BOOLEAN NOT NULL DEFAULT FALSE,
    total_questions   INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE practice_service.practice_session (
    id                     UUID PRIMARY KEY,
    tenant_id              VARCHAR(255) NOT NULL,
    candidate_id           UUID NOT NULL,
    practice_set_id        UUID NOT NULL REFERENCES practice_service.practice_set(id),
    mode                   VARCHAR(30) NOT NULL DEFAULT 'TIMED',
    status                 VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    started_at             TIMESTAMP,
    submitted_at           TIMESTAMP,
    total_questions        INTEGER NOT NULL DEFAULT 0,
    duration_minutes       INTEGER NOT NULL DEFAULT 0,
    correct_count          INTEGER NOT NULL DEFAULT 0,
    incorrect_count        INTEGER NOT NULL DEFAULT 0,
    skipped_count          INTEGER NOT NULL DEFAULT 0,
    total_marks            INTEGER NOT NULL DEFAULT 0,
    obtained_marks         INTEGER NOT NULL DEFAULT 0,
    topic_wise_breakdown   JSONB,
    difficulty_breakdown   JSONB,
    timing_breakdown       JSONB,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE practice_service.practice_response (
    id                    UUID PRIMARY KEY,
    tenant_id             VARCHAR(255) NOT NULL,
    practice_session_id   UUID NOT NULL REFERENCES practice_service.practice_session(id),
    question_id           UUID NOT NULL,
    selected_option_ids   JSONB,
    entered_value         TEXT,
    correct               BOOLEAN NOT NULL DEFAULT FALSE,
    marks_awarded         INTEGER NOT NULL DEFAULT 0,
    time_spent_ms         BIGINT NOT NULL DEFAULT 0,
    marked_for_review     BOOLEAN NOT NULL DEFAULT FALSE,
    revision_sequence     INTEGER NOT NULL DEFAULT 1,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_practice_session_candidate ON practice_service.practice_session(candidate_id);
CREATE INDEX idx_practice_response_session ON practice_service.practice_response(practice_session_id);

-- Seed default published practice sets
INSERT INTO practice_service.practice_set (
    id, tenant_id, name, description, created_by, source, question_ids, duration_minutes, subject_slug, published, total_questions, created_at, updated_at, version
) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'default',
    'General Intelligence & Reasoning Mock Test',
    'Comprehensive reasoning practice covering syllogisms, analogies, coding-decoding, and logical puzzles.',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL',
    '[]'::jsonb,
    45,
    'reasoning',
    TRUE,
    25,
    NOW(),
    NOW(),
    0
),
(
    '22222222-2222-2222-2222-222222222222',
    'default',
    'Quantitative Aptitude Speed Practice',
    'High-yield arithmetic, algebra, data interpretation, and speed math practice set.',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL',
    '[]'::jsonb,
    60,
    'quantitative-aptitude',
    TRUE,
    30,
    NOW(),
    NOW(),
    0
),
(
    '33333333-3333-3333-3333-333333333333',
    'default',
    'General Awareness & Science Capsule',
    'Curated practice questions spanning general science, polity, economics, and contemporary awareness.',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL',
    '[]'::jsonb,
    30,
    'general-awareness',
    TRUE,
    20,
    NOW(),
    NOW(),
    0
)
ON CONFLICT (id) DO NOTHING;
