-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors
--
-- Migration: V3__add_passage_support.sql
-- Supports Paragraph-Based (Comprehension / Case Study) Question Sets (Issue #135)

CREATE TABLE IF NOT EXISTS question_service.passage (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    title               VARCHAR(500),
    content             TEXT, -- AES-256 encrypted (EncryptedFieldConverter)
    content_format      VARCHAR(20) NOT NULL DEFAULT 'MIXED',
    subject_id          BIGINT NOT NULL REFERENCES question_service.subject(id),
    topic_id            BIGINT REFERENCES question_service.topic(id),
    subject             VARCHAR(100) NOT NULL, -- denormalized
    topic               VARCHAR(200), -- denormalized
    has_images          BOOLEAN NOT NULL DEFAULT FALSE,
    state               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    encryption_key_id   VARCHAR(255),
    author_id           UUID NOT NULL,
    reviewer_id         UUID,
    embedding           halfvec(384),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_passage_tenant_id ON question_service.passage(tenant_id);
CREATE INDEX IF NOT EXISTS idx_passage_subject_id ON question_service.passage(subject_id);
CREATE INDEX IF NOT EXISTS idx_passage_topic_id ON question_service.passage(topic_id);
CREATE INDEX IF NOT EXISTS idx_passage_state ON question_service.passage(state);
CREATE INDEX IF NOT EXISTS idx_passage_embedding ON question_service.passage USING ivfflat (embedding halfvec_cosine_ops) WITH (lists = 50);

-- Add passage columns to question table
ALTER TABLE question_service.question ADD COLUMN IF NOT EXISTS passage_id UUID REFERENCES question_service.passage(id);
ALTER TABLE question_service.question ADD COLUMN IF NOT EXISTS passage_order_index INTEGER;
CREATE INDEX IF NOT EXISTS idx_question_passage_id ON question_service.question(passage_id);

-- Support passage translation in translation table
ALTER TABLE question_service.translation ADD COLUMN IF NOT EXISTS passage_id UUID REFERENCES question_service.passage(id);
ALTER TABLE question_service.translation ALTER COLUMN question_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_translation_passage_id ON question_service.translation(passage_id);
