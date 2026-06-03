CREATE SCHEMA IF NOT EXISTS question_service;
SET search_path TO question_service;

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Hash-partitioned question table (16 partitions)
CREATE TABLE question (
    id                  UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    subject             VARCHAR(100) NOT NULL,
    topic               VARCHAR(200) NOT NULL,
    subtopic            VARCHAR(200),
    chapter             VARCHAR(200),
    difficulty          VARCHAR(20) NOT NULL,
    cognitive_level     VARCHAR(20) NOT NULL,
    question_type       VARCHAR(30) NOT NULL,
    content             TEXT,           -- encrypted
    answer_key          TEXT,           -- encrypted
    embedding_vector    vector(1536),
    state               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    encryption_key_id   VARCHAR(255),
    usage_count         INTEGER NOT NULL DEFAULT 0,
    last_used_at        TIMESTAMPTZ,
    author_id           UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, tenant_id)
) PARTITION BY HASH (id);

-- Create 16 hash partitions
CREATE TABLE question_p0 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 0);
CREATE TABLE question_p1 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 1);
CREATE TABLE question_p2 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 2);
CREATE TABLE question_p3 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 3);
CREATE TABLE question_p4 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 4);
CREATE TABLE question_p5 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 5);
CREATE TABLE question_p6 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 6);
CREATE TABLE question_p7 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 7);
CREATE TABLE question_p8 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 8);
CREATE TABLE question_p9 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 9);
CREATE TABLE question_p10 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 10);
CREATE TABLE question_p11 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 11);
CREATE TABLE question_p12 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 12);
CREATE TABLE question_p13 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 13);
CREATE TABLE question_p14 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 14);
CREATE TABLE question_p15 PARTITION OF question FOR VALUES WITH (MODULUS 16, REMAINDER 15);

-- Indexes
CREATE INDEX idx_question_subject_state ON question(subject, state, tenant_id);
CREATE INDEX idx_question_author ON question(author_id, tenant_id);
CREATE INDEX idx_question_state ON question(state, tenant_id);
