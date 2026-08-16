-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

CREATE SCHEMA IF NOT EXISTS question_service;

-- Enable pgvector extension for halfvec type (requires pgvector/pgvector Docker image)
CREATE EXTENSION IF NOT EXISTS vector SCHEMA public;

-- ============================================================
-- Table: question (hash-partitioned by subject for scalability)
-- ============================================================
-- Content supports: plain text, HTML, LaTeX ($$..$$), SVG (<svg>...</svg>)
-- content_format indicates how to render: TEXT, HTML, LATEX, SVG, MIXED
-- Options stored as JSONB array: [{id, text, isCorrect}]
-- Embedding: halfvec(384) from all-minilm model for similarity detection

CREATE TABLE question_service.question (
    id                      UUID NOT NULL,
    tenant_id               VARCHAR(255) NOT NULL,
    subject                 VARCHAR(100) NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    subtopic                VARCHAR(200),
    chapter                 VARCHAR(200),
    difficulty              VARCHAR(20) NOT NULL,
    cognitive_level         VARCHAR(20) NOT NULL,
    question_type           VARCHAR(30) NOT NULL,
    content                 TEXT,
    options                 JSONB,
    answer_key              TEXT,
    explanation             TEXT,
    "references"            TEXT,
    embedding               halfvec(384),
    state                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    encryption_key_id       VARCHAR(255),
    usage_count             INTEGER NOT NULL DEFAULT 0,
    last_used_at            TIMESTAMP,
    used_in_exam_ids_json   JSONB,
    used_in_shift_ids_json  JSONB,
    author_id               UUID NOT NULL,
    reviewer_id             UUID,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, subject)
) PARTITION BY HASH (subject);

-- Create 8 hash partitions
CREATE TABLE question_service.question_p0 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 0);
CREATE TABLE question_service.question_p1 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 1);
CREATE TABLE question_service.question_p2 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 2);
CREATE TABLE question_service.question_p3 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 3);
CREATE TABLE question_service.question_p4 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 4);
CREATE TABLE question_service.question_p5 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 5);
CREATE TABLE question_service.question_p6 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 6);
CREATE TABLE question_service.question_p7 PARTITION OF question_service.question FOR VALUES WITH (MODULUS 8, REMAINDER 7);

-- Indexes (created on parent; propagated to partitions)
CREATE INDEX idx_question_tenant_id ON question_service.question(tenant_id);
CREATE INDEX idx_question_subject ON question_service.question(subject);
CREATE INDEX idx_question_topic ON question_service.question(topic);
CREATE INDEX idx_question_difficulty ON question_service.question(difficulty);
CREATE INDEX idx_question_state ON question_service.question(state);
CREATE INDEX idx_question_author_id ON question_service.question(author_id);
CREATE INDEX idx_question_type ON question_service.question(question_type);

-- halfvec cosine similarity index (IVFFlat) for duplicate detection
-- Uses cosine distance operator <=> on halfvec(384)
CREATE INDEX idx_question_embedding ON question_service.question
    USING ivfflat (embedding halfvec_cosine_ops) WITH (lists = 50);

-- ============================================================
-- Table: question_version (audit trail)
-- ============================================================
CREATE TABLE question_service.question_version (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    question_id     UUID NOT NULL,
    author_id       UUID NOT NULL,
    changed_at      TIMESTAMP NOT NULL,
    diff_json       JSONB NOT NULL,
    snapshot_json   TEXT,
    version_number  INTEGER NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_question_version_tenant_id ON question_service.question_version(tenant_id);
CREATE INDEX idx_question_version_question_id ON question_service.question_version(question_id);

-- ============================================================
-- Subject → Topic → Subtopic hierarchy tables
-- ============================================================

CREATE TABLE question_service.subject (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    code        VARCHAR(50),
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_subject_name_tenant UNIQUE (name, tenant_id)
);

CREATE INDEX idx_subject_tenant_id ON question_service.subject(tenant_id);
CREATE INDEX idx_subject_name ON question_service.subject(name);

CREATE TABLE question_service.topic (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    subject_id  UUID NOT NULL REFERENCES question_service.subject(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_topic_name_subject_tenant UNIQUE (name, subject_id, tenant_id)
);

CREATE INDEX idx_topic_tenant_id ON question_service.topic(tenant_id);
CREATE INDEX idx_topic_subject_id ON question_service.topic(subject_id);

CREATE TABLE question_service.subtopic (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    topic_id    UUID NOT NULL REFERENCES question_service.topic(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_subtopic_name_topic_tenant UNIQUE (name, topic_id, tenant_id)
);

CREATE INDEX idx_subtopic_tenant_id ON question_service.subtopic(tenant_id);
CREATE INDEX idx_subtopic_topic_id ON question_service.subtopic(topic_id);

-- ============================================================
-- Seed Data: Indian Examination Subjects for tenant 'exam-authority-1'
-- ============================================================

INSERT INTO question_service.subject (id, tenant_id, name, code, description) VALUES
('10000001-0000-0000-0000-000000000001', 'exam-authority-1', 'General Studies', 'GS', 'General Studies covering history, geography, polity, economy, science and current affairs'),
('10000001-0000-0000-0000-000000000002', 'exam-authority-1', 'Mathematics', 'MATH', 'Mathematics including arithmetic, algebra, geometry, statistics and trigonometry'),
('10000001-0000-0000-0000-000000000003', 'exam-authority-1', 'English', 'ENG', 'English language including grammar, comprehension and vocabulary'),
('10000001-0000-0000-0000-000000000004', 'exam-authority-1', 'Reasoning', 'REAS', 'Logical, analytical and verbal reasoning'),
('10000001-0000-0000-0000-000000000005', 'exam-authority-1', 'General Science', 'GSCI', 'General science covering physics, chemistry and biology'),
('10000001-0000-0000-0000-000000000006', 'exam-authority-1', 'Computer Science', 'CS', 'Computer science including programming, databases and networking');

-- Topics
INSERT INTO question_service.topic (id, tenant_id, subject_id, name, description) VALUES
('20000001-0000-0000-0000-000000000001', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Indian History', 'History of India from ancient to modern times'),
('20000001-0000-0000-0000-000000000002', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Indian Geography', 'Physical, economic and human geography of India'),
('20000001-0000-0000-0000-000000000003', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Indian Polity', 'Indian constitution, governance and political system'),
('20000001-0000-0000-0000-000000000004', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Indian Economy', 'Economic planning, banking, trade and fiscal policy'),
('20000001-0000-0000-0000-000000000005', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Science & Technology', 'Scientific developments and technological advancements'),
('20000001-0000-0000-0000-000000000006', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Environment', 'Environmental science, ecology and conservation'),
('20000001-0000-0000-0000-000000000007', 'exam-authority-1', '10000001-0000-0000-0000-000000000001', 'Current Affairs', 'National and international current events'),
('20000001-0000-0000-0000-000000000008', 'exam-authority-1', '10000001-0000-0000-0000-000000000002', 'Arithmetic', 'Basic arithmetic operations and concepts'),
('20000001-0000-0000-0000-000000000009', 'exam-authority-1', '10000001-0000-0000-0000-000000000002', 'Algebra', 'Algebraic expressions, equations and operations'),
('20000001-0000-0000-0000-000000000010', 'exam-authority-1', '10000001-0000-0000-0000-000000000002', 'Geometry', 'Geometric shapes, properties and measurements'),
('20000001-0000-0000-0000-000000000011', 'exam-authority-1', '10000001-0000-0000-0000-000000000002', 'Statistics', 'Statistical measures and data analysis'),
('20000001-0000-0000-0000-000000000012', 'exam-authority-1', '10000001-0000-0000-0000-000000000002', 'Trigonometry', 'Trigonometric ratios, identities and applications'),
('20000001-0000-0000-0000-000000000013', 'exam-authority-1', '10000001-0000-0000-0000-000000000003', 'Grammar', 'English grammar rules and usage'),
('20000001-0000-0000-0000-000000000014', 'exam-authority-1', '10000001-0000-0000-0000-000000000003', 'Comprehension', 'Reading comprehension and passage analysis'),
('20000001-0000-0000-0000-000000000015', 'exam-authority-1', '10000001-0000-0000-0000-000000000003', 'Vocabulary', 'Word knowledge and usage'),
('20000001-0000-0000-0000-000000000016', 'exam-authority-1', '10000001-0000-0000-0000-000000000004', 'Logical Reasoning', 'Logic-based problem solving'),
('20000001-0000-0000-0000-000000000017', 'exam-authority-1', '10000001-0000-0000-0000-000000000004', 'Analytical Reasoning', 'Analysis and pattern recognition'),
('20000001-0000-0000-0000-000000000018', 'exam-authority-1', '10000001-0000-0000-0000-000000000004', 'Verbal Reasoning', 'Verbal logic and inference'),
('20000001-0000-0000-0000-000000000019', 'exam-authority-1', '10000001-0000-0000-0000-000000000005', 'Physics', 'Fundamental physics concepts'),
('20000001-0000-0000-0000-000000000020', 'exam-authority-1', '10000001-0000-0000-0000-000000000005', 'Chemistry', 'Chemistry concepts and applications'),
('20000001-0000-0000-0000-000000000021', 'exam-authority-1', '10000001-0000-0000-0000-000000000005', 'Biology', 'Biological sciences'),
('20000001-0000-0000-0000-000000000022', 'exam-authority-1', '10000001-0000-0000-0000-000000000006', 'Programming', 'Programming concepts and paradigms'),
('20000001-0000-0000-0000-000000000023', 'exam-authority-1', '10000001-0000-0000-0000-000000000006', 'Databases', 'Database management concepts'),
('20000001-0000-0000-0000-000000000024', 'exam-authority-1', '10000001-0000-0000-0000-000000000006', 'Networking', 'Computer networking fundamentals');

-- Subtopics (abbreviated — key entries only for seed)
INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name, description) VALUES
('30000001-0000-0000-0000-000000000001', 'exam-authority-1', '20000001-0000-0000-0000-000000000001', 'Ancient India', 'Indus Valley, Vedic period, Mauryas, Guptas'),
('30000001-0000-0000-0000-000000000002', 'exam-authority-1', '20000001-0000-0000-0000-000000000001', 'Medieval India', 'Delhi Sultanate, Mughal Empire'),
('30000001-0000-0000-0000-000000000003', 'exam-authority-1', '20000001-0000-0000-0000-000000000001', 'Modern India', 'British rule, freedom movement'),
('30000001-0000-0000-0000-000000000004', 'exam-authority-1', '20000001-0000-0000-0000-000000000003', 'Constitution', 'Constitutional framework and amendments'),
('30000001-0000-0000-0000-000000000005', 'exam-authority-1', '20000001-0000-0000-0000-000000000003', 'Fundamental Rights', 'Constitutional rights and duties'),
('30000001-0000-0000-0000-000000000006', 'exam-authority-1', '20000001-0000-0000-0000-000000000008', 'Number System', 'Natural, whole, integers, rational numbers'),
('30000001-0000-0000-0000-000000000007', 'exam-authority-1', '20000001-0000-0000-0000-000000000008', 'Percentage', 'Percentage calculations and applications'),
('30000001-0000-0000-0000-000000000008', 'exam-authority-1', '20000001-0000-0000-0000-000000000009', 'Linear Equations', 'Linear equations in one and two variables'),
('30000001-0000-0000-0000-000000000009', 'exam-authority-1', '20000001-0000-0000-0000-000000000009', 'Quadratic Equations', 'Quadratic equations and solutions'),
('30000001-0000-0000-0000-000000000010', 'exam-authority-1', '20000001-0000-0000-0000-000000000019', 'Mechanics', 'Force, motion, energy and work'),
('30000001-0000-0000-0000-000000000011', 'exam-authority-1', '20000001-0000-0000-0000-000000000019', 'Optics', 'Light, reflection, refraction'),
('30000001-0000-0000-0000-000000000012', 'exam-authority-1', '20000001-0000-0000-0000-000000000020', 'Organic', 'Carbon compounds and hydrocarbons'),
('30000001-0000-0000-0000-000000000013', 'exam-authority-1', '20000001-0000-0000-0000-000000000022', 'Data Structures', 'Arrays, linked lists, trees, graphs'),
('30000001-0000-0000-0000-000000000014', 'exam-authority-1', '20000001-0000-0000-0000-000000000022', 'Algorithms', 'Sorting, searching, dynamic programming');
