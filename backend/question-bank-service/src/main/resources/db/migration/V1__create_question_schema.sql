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


-- ============================================================
-- SUBJECTS
-- Continue from previous seed:
-- Previous last subject ID: 10000000-...-000000000006
-- ============================================================

INSERT INTO question_service.subject (id, tenant_id, name, code, description) VALUES

('10000000-0000-0000-0000-000000000007', 'exam-authority-1', 'General Studies', 'GS', 'General Studies covering history, geography, polity, economy, science and current affairs'),

('10000000-0000-0000-0000-000000000008', 'exam-authority-1', 'Mathematics', 'MATH', 'Mathematics including arithmetic, algebra, geometry, statistics and trigonometry'),

('10000000-0000-0000-0000-000000000009', 'exam-authority-1', 'English', 'ENG', 'English language including grammar, comprehension and vocabulary'),

('10000000-0000-0000-0000-000000000010', 'exam-authority-1', 'Reasoning', 'REAS', 'Logical, analytical and verbal reasoning'),

('10000000-0000-0000-0000-000000000011', 'exam-authority-1', 'General Science', 'GSCI', 'General science covering physics, chemistry and biology'),

('10000000-0000-0000-0000-000000000012', 'exam-authority-1', 'Computer Science', 'CS', 'Computer science including programming, databases and networking');


-- ============================================================
-- TOPICS
-- Continue from previous last topic:
-- 20000000-0000-0000-0000-000000000024
-- ============================================================

INSERT INTO question_service.topic (id, tenant_id, subject_id, name, description) VALUES

('20000000-0000-0000-0000-000000000025', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Indian History', 'History of India from ancient to modern times'),

('20000000-0000-0000-0000-000000000026', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Indian Geography', 'Physical, economic and human geography of India'),

('20000000-0000-0000-0000-000000000027', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Indian Polity', 'Indian constitution, governance and political system'),

('20000000-0000-0000-0000-000000000028', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Indian Economy', 'Economic planning, banking, trade and fiscal policy'),

('20000000-0000-0000-0000-000000000029', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Science & Technology', 'Scientific developments and technological advancements'),

('20000000-0000-0000-0000-000000000030', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Environment', 'Environmental science, ecology and conservation'),

('20000000-0000-0000-0000-000000000031', 'exam-authority-1', '10000000-0000-0000-0000-000000000007', 'Current Affairs', 'National and international current events'),

('20000000-0000-0000-0000-000000000032', 'exam-authority-1', '10000000-0000-0000-0000-000000000008', 'Arithmetic', 'Basic arithmetic operations and concepts'),

('20000000-0000-0000-0000-000000000033', 'exam-authority-1', '10000000-0000-0000-0000-000000000008', 'Algebra', 'Algebraic expressions, equations and operations'),

('20000000-0000-0000-0000-000000000034', 'exam-authority-1', '10000000-0000-0000-0000-000000000008', 'Geometry', 'Geometric shapes, properties and measurements'),

('20000000-0000-0000-0000-000000000035', 'exam-authority-1', '10000000-0000-0000-0000-000000000008', 'Statistics', 'Statistical measures and data analysis'),

('20000000-0000-0000-0000-000000000036', 'exam-authority-1', '10000000-0000-0000-0000-000000000008', 'Trigonometry', 'Trigonometric ratios, identities and applications'),

('20000000-0000-0000-0000-000000000037', 'exam-authority-1', '10000000-0000-0000-0000-000000000009', 'Grammar', 'English grammar rules and usage'),

('20000000-0000-0000-0000-000000000038', 'exam-authority-1', '10000000-0000-0000-0000-000000000009', 'Comprehension', 'Reading comprehension and passage analysis'),

('20000000-0000-0000-0000-000000000039', 'exam-authority-1', '10000000-0000-0000-0000-000000000009', 'Vocabulary', 'Word knowledge and usage'),

('20000000-0000-0000-0000-000000000040', 'exam-authority-1', '10000000-0000-0000-0000-000000000010', 'Logical Reasoning', 'Logic-based problem solving'),

('20000000-0000-0000-0000-000000000041', 'exam-authority-1', '10000000-0000-0000-0000-000000000010', 'Analytical Reasoning', 'Analysis and pattern recognition'),

('20000000-0000-0000-0000-000000000042', 'exam-authority-1', '10000000-0000-0000-0000-000000000010', 'Verbal Reasoning', 'Verbal logic and inference'),

('20000000-0000-0000-0000-000000000043', 'exam-authority-1', '10000000-0000-0000-0000-000000000011', 'Physics', 'Fundamental physics concepts'),

('20000000-0000-0000-0000-000000000044', 'exam-authority-1', '10000000-0000-0000-0000-000000000011', 'Chemistry', 'Chemistry concepts and applications'),

('20000000-0000-0000-0000-000000000045', 'exam-authority-1', '10000000-0000-0000-0000-000000000011', 'Biology', 'Biological sciences'),

('20000000-0000-0000-0000-000000000046', 'exam-authority-1', '10000000-0000-0000-0000-000000000012', 'Programming', 'Programming concepts and paradigms'),

('20000000-0000-0000-0000-000000000047', 'exam-authority-1', '10000000-0000-0000-0000-000000000012', 'Databases', 'Database management concepts'),

('20000000-0000-0000-0000-000000000048', 'exam-authority-1', '10000000-0000-0000-0000-000000000012', 'Networking', 'Computer networking fundamentals');


-- ============================================================
-- SUBTOPICS
-- Continue from previous last subtopic:
-- 30000000-0000-0000-0000-000000000073
-- ============================================================

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name, description) VALUES

('30000000-0000-0000-0000-000000000074', 'exam-authority-1', '20000000-0000-0000-0000-000000000025', 'Ancient India', 'Indus Valley, Vedic period, Mauryas, Guptas'),

('30000000-0000-0000-0000-000000000075', 'exam-authority-1', '20000000-0000-0000-0000-000000000025', 'Medieval India', 'Delhi Sultanate, Mughal Empire'),

('30000000-0000-0000-0000-000000000076', 'exam-authority-1', '20000000-0000-0000-0000-000000000025', 'Modern India', 'British rule, freedom movement'),

('30000000-0000-0000-0000-000000000077', 'exam-authority-1', '20000000-0000-0000-0000-000000000027', 'Constitution', 'Constitutional framework and amendments'),

('30000000-0000-0000-0000-000000000078', 'exam-authority-1', '20000000-0000-0000-0000-000000000027', 'Fundamental Rights', 'Constitutional rights and duties'),

('30000000-0000-0000-0000-000000000079', 'exam-authority-1', '20000000-0000-0000-0000-000000000032', 'Number System', 'Natural, whole, integers, rational numbers'),

('30000000-0000-0000-0000-000000000080', 'exam-authority-1', '20000000-0000-0000-0000-000000000032', 'Percentage', 'Percentage calculations and applications'),

('30000000-0000-0000-0000-000000000081', 'exam-authority-1', '20000000-0000-0000-0000-000000000033', 'Linear Equations', 'Linear equations in one and two variables'),

('30000000-0000-0000-0000-000000000082', 'exam-authority-1', '20000000-0000-0000-0000-000000000033', 'Quadratic Equations', 'Quadratic equations and solutions'),

('30000000-0000-0000-0000-000000000083', 'exam-authority-1', '20000000-0000-0000-0000-000000000043', 'Mechanics', 'Force, motion, energy and work'),

('30000000-0000-0000-0000-000000000084', 'exam-authority-1', '20000000-0000-0000-0000-000000000043', 'Optics', 'Light, reflection, refraction'),

('30000000-0000-0000-0000-000000000085', 'exam-authority-1', '20000000-0000-0000-0000-000000000044', 'Organic', 'Carbon compounds and hydrocarbons'),

('30000000-0000-0000-0000-000000000086', 'exam-authority-1', '20000000-0000-0000-0000-000000000046', 'Data Structures', 'Arrays, linked lists, trees, graphs'),

('30000000-0000-0000-0000-000000000087', 'exam-authority-1', '20000000-0000-0000-0000-000000000046', 'Algorithms', 'Sorting, searching, dynamic programming');


-- ============================================================
-- SSC CGL 2024 - Subject / Topic / Subtopic Seed Data
-- tenant_id = 'exam-authority-1'
-- ============================================================


-- ============================================================
-- SUBJECTS
-- ============================================================

INSERT INTO question_service.subject (id, tenant_id, name, code, description)
VALUES

('10000000-0000-0000-0000-000000000001',
 'exam-authority-1',
 'General Intelligence and Reasoning',
 'GIR',
 'General Intelligence and Reasoning covering verbal and non-verbal reasoning, analogy, classification, series, coding-decoding, problem solving, spatial visualization, critical thinking, emotional intelligence and social intelligence'),

('10000000-0000-0000-0000-000000000002',
 'exam-authority-1',
 'General Awareness',
 'GA',
 'General awareness covering current events, history, culture, geography, economic scene, general policy, scientific research and India and neighbouring countries'),

('10000000-0000-0000-0000-000000000003',
 'exam-authority-1',
 'Quantitative Aptitude / Mathematical Abilities',
 'QA-MA',
 'Quantitative aptitude and mathematical abilities covering number systems, arithmetic, algebra, geometry, mensuration, trigonometry, statistics and probability'),

('10000000-0000-0000-0000-000000000004',
 'exam-authority-1',
 'English Language and Comprehension',
 'ENG',
 'English language and comprehension covering vocabulary, grammar, sentence structure, synonyms, antonyms, error detection, sentence improvement, voice, narration, cloze passages and comprehension passages'),

('10000000-0000-0000-0000-000000000005',
 'exam-authority-1',
 'Computer Knowledge',
 'CK',
 'Computer knowledge covering computer basics, hardware, memory, operating systems, Microsoft Office, internet, email, networking and cyber security'),

('10000000-0000-0000-0000-000000000006',
 'exam-authority-1',
 'Statistics',
 'STATS',
 'Statistics covering collection and presentation of data, measures of central tendency and dispersion, moments, skewness, kurtosis, correlation, regression, probability, sampling, statistical inference, analysis of variance, time series and index numbers');
-- ============================================================
-- TOPICS
-- ============================================================

-- ------------------------------------------------------------
-- General Intelligence and Reasoning
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('20000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Analogy'),

('20000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Classification'),

('20000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Series'),

('20000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Problem Solving'),

('20000000-0000-0000-0000-000000000005','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Coding and Decoding'),

('20000000-0000-0000-0000-000000000006','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Numerical Operations'),

('20000000-0000-0000-0000-000000000007','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Symbolic Operations'),

('20000000-0000-0000-0000-000000000008','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Trends'),

('20000000-0000-0000-0000-000000000009','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Space Orientation'),

('20000000-0000-0000-0000-000000000010','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Space Visualization'),

('20000000-0000-0000-0000-000000000011','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Venn Diagrams'),

('20000000-0000-0000-0000-000000000012','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Drawing Inferences'),

('20000000-0000-0000-0000-000000000013','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Pattern Folding and Unfolding'),

('20000000-0000-0000-0000-000000000014','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Figural Pattern Folding and Completion'),

('20000000-0000-0000-0000-000000000015','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Indexing'),

('20000000-0000-0000-0000-000000000016','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Address Matching'),

('20000000-0000-0000-0000-000000000017','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Date and City Matching'),

('20000000-0000-0000-0000-000000000018','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Embedded Figures'),

('20000000-0000-0000-0000-000000000019','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Critical Thinking'),

('20000000-0000-0000-0000-000000000020','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Emotional Intelligence'),

('20000000-0000-0000-0000-000000000021','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Social Intelligence'),

('20000000-0000-0000-0000-000000000022','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Word Building'),

('20000000-0000-0000-0000-000000000023','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Statement and Conclusion'),

('20000000-0000-0000-0000-000000000024','exam-authority-1',
 '10000000-0000-0000-0000-000000000001','Syllogistic Reasoning');


-- ------------------------------------------------------------
-- General Awareness
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('21000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Current Events'),

('21000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','History'),

('21000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Culture'),

('21000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Geography'),

('21000000-0000-0000-0000-000000000005','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Economic Scene'),

('21000000-0000-0000-0000-000000000006','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','General Policy'),

('21000000-0000-0000-0000-000000000007','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Scientific Research'),

('21000000-0000-0000-0000-000000000008','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','Everyday Science'),

('21000000-0000-0000-0000-000000000009','exam-authority-1',
 '10000000-0000-0000-0000-000000000002','India and Neighbouring Countries');


-- ------------------------------------------------------------
-- Quantitative Aptitude / Mathematical Abilities
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('22000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Number Systems'),

('22000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Fundamental Arithmetical Operations'),

('22000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Algebra'),

('22000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Geometry'),

('22000000-0000-0000-0000-000000000005','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Mensuration'),

('22000000-0000-0000-0000-000000000006','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Trigonometry'),

('22000000-0000-0000-0000-000000000007','exam-authority-1',
 '10000000-0000-0000-0000-000000000003','Statistics and Probability');


-- ------------------------------------------------------------
-- English Language and Comprehension
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('23000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Vocabulary'),

('23000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Grammar'),

('23000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Sentence Structure'),

('23000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Spot the Error'),

('23000000-0000-0000-0000-000000000005','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Fill in the Blanks'),

('23000000-0000-0000-0000-000000000006','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Synonyms and Homonyms'),

('23000000-0000-0000-0000-000000000007','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Antonyms'),

('23000000-0000-0000-0000-000000000008','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Spellings'),

('23000000-0000-0000-0000-000000000009','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Idioms and Phrases'),

('23000000-0000-0000-0000-000000000010','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','One Word Substitution'),

('23000000-0000-0000-0000-000000000011','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Improvement of Sentences'),

('23000000-0000-0000-0000-000000000012','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Active and Passive Voice'),

('23000000-0000-0000-0000-000000000013','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Direct and Indirect Narration'),

('23000000-0000-0000-0000-000000000014','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Sentence Shuffling'),

('23000000-0000-0000-0000-000000000015','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Cloze Passage'),

('23000000-0000-0000-0000-000000000016','exam-authority-1',
 '10000000-0000-0000-0000-000000000004','Comprehension Passage');


-- ------------------------------------------------------------
-- Computer Knowledge
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('24000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000005','Computer Basics'),

('24000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000005','Software'),

('24000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000005','Internet and E-mail'),

('24000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000005','Networking and Cyber Security');


-- ------------------------------------------------------------
-- Statistics
-- ------------------------------------------------------------

INSERT INTO question_service.topic (id, tenant_id, subject_id, name)
VALUES
('25000000-0000-0000-0000-000000000001','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Collection, Classification and Presentation of Statistical Data'),

('25000000-0000-0000-0000-000000000002','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Measures of Central Tendency'),

('25000000-0000-0000-0000-000000000003','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Measures of Dispersion'),

('25000000-0000-0000-0000-000000000004','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Moments, Skewness and Kurtosis'),

('25000000-0000-0000-0000-000000000005','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Correlation and Regression'),

('25000000-0000-0000-0000-000000000006','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Probability Theory'),

('25000000-0000-0000-0000-000000000007','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Random Variable and Probability Distributions'),

('25000000-0000-0000-0000-000000000008','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Sampling Theory'),

('25000000-0000-0000-0000-000000000009','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Statistical Inference'),

('25000000-0000-0000-0000-000000000010','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Analysis of Variance'),

('25000000-0000-0000-0000-000000000011','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Time Series Analysis'),

('25000000-0000-0000-0000-000000000012','exam-authority-1',
 '10000000-0000-0000-0000-000000000006','Index Numbers');


-- ============================================================
-- SUBTOPICS
-- ============================================================

-- ------------------------------------------------------------
-- Reasoning
-- ------------------------------------------------------------

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name)
VALUES
('30000000-0000-0000-0000-000000000001','exam-authority-1','20000000-0000-0000-0000-000000000001','Semantic Analogy'),
('30000000-0000-0000-0000-000000000002','exam-authority-1','20000000-0000-0000-0000-000000000001','Symbolic / Number Analogy'),
('30000000-0000-0000-0000-000000000003','exam-authority-1','20000000-0000-0000-0000-000000000001','Figural Analogy'),

('30000000-0000-0000-0000-000000000004','exam-authority-1','20000000-0000-0000-0000-000000000002','Semantic Classification'),
('30000000-0000-0000-0000-000000000005','exam-authority-1','20000000-0000-0000-0000-000000000002','Symbolic / Number Classification'),
('30000000-0000-0000-0000-000000000006','exam-authority-1','20000000-0000-0000-0000-000000000002','Figural Classification'),

('30000000-0000-0000-0000-000000000007','exam-authority-1','20000000-0000-0000-0000-000000000003','Semantic Series'),
('30000000-0000-0000-0000-000000000008','exam-authority-1','20000000-0000-0000-0000-000000000003','Number Series'),
('30000000-0000-0000-0000-000000000009','exam-authority-1','20000000-0000-0000-0000-000000000003','Figural Series'),

('30000000-0000-0000-0000-000000000010','exam-authority-1','20000000-0000-0000-0000-000000000005','Coding'),
('30000000-0000-0000-0000-000000000011','exam-authority-1','20000000-0000-0000-0000-000000000005','Decoding'),

('30000000-0000-0000-0000-000000000012','exam-authority-1','20000000-0000-0000-0000-000000000013','Punched Hole Pattern Folding and Unfolding'),

('30000000-0000-0000-0000-000000000013','exam-authority-1','20000000-0000-0000-0000-000000000018','Embedded Figures');


-- ------------------------------------------------------------
-- Quantitative Aptitude / Mathematical Abilities
-- ------------------------------------------------------------

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name)
VALUES

-- Number Systems
('31000000-0000-0000-0000-000000000001','exam-authority-1','22000000-0000-0000-0000-000000000001','Whole Numbers'),
('31000000-0000-0000-0000-000000000002','exam-authority-1','22000000-0000-0000-0000-000000000001','Decimals'),
('31000000-0000-0000-0000-000000000003','exam-authority-1','22000000-0000-0000-0000-000000000001','Fractions'),
('31000000-0000-0000-0000-000000000004','exam-authority-1','22000000-0000-0000-0000-000000000001','Relationship Between Numbers'),

-- Fundamental Arithmetic
('31000000-0000-0000-0000-000000000005','exam-authority-1','22000000-0000-0000-0000-000000000002','Percentages'),
('31000000-0000-0000-0000-000000000006','exam-authority-1','22000000-0000-0000-0000-000000000002','Ratio and Proportion'),
('31000000-0000-0000-0000-000000000007','exam-authority-1','22000000-0000-0000-0000-000000000002','Square Roots'),
('31000000-0000-0000-0000-000000000008','exam-authority-1','22000000-0000-0000-0000-000000000002','Averages'),
('31000000-0000-0000-0000-000000000009','exam-authority-1','22000000-0000-0000-0000-000000000002','Simple Interest'),
('31000000-0000-0000-0000-000000000010','exam-authority-1','22000000-0000-0000-0000-000000000002','Compound Interest'),
('31000000-0000-0000-0000-000000000011','exam-authority-1','22000000-0000-0000-0000-000000000002','Profit and Loss'),
('31000000-0000-0000-0000-000000000012','exam-authority-1','22000000-0000-0000-0000-000000000002','Discount'),
('31000000-0000-0000-0000-000000000013','exam-authority-1','22000000-0000-0000-0000-000000000002','Partnership Business'),
('31000000-0000-0000-0000-000000000014','exam-authority-1','22000000-0000-0000-0000-000000000002','Mixture and Alligation'),
('31000000-0000-0000-0000-000000000015','exam-authority-1','22000000-0000-0000-0000-000000000002','Time and Distance'),
('31000000-0000-0000-0000-000000000016','exam-authority-1','22000000-0000-0000-0000-000000000002','Time and Work'),

-- Algebra
('31000000-0000-0000-0000-000000000017','exam-authority-1','22000000-0000-0000-0000-000000000003','Algebraic Identities'),
('31000000-0000-0000-0000-000000000018','exam-authority-1','22000000-0000-0000-0000-000000000003','Elementary Surds'),
('31000000-0000-0000-0000-000000000019','exam-authority-1','22000000-0000-0000-0000-000000000003','Graphs of Linear Equations'),

-- Geometry
('31000000-0000-0000-0000-000000000020','exam-authority-1','22000000-0000-0000-0000-000000000004','Triangles'),
('31000000-0000-0000-0000-000000000021','exam-authority-1','22000000-0000-0000-0000-000000000004','Congruence and Similarity'),
('31000000-0000-0000-0000-000000000022','exam-authority-1','22000000-0000-0000-0000-000000000004','Circles'),
('31000000-0000-0000-0000-000000000023','exam-authority-1','22000000-0000-0000-0000-000000000004','Chords and Tangents'),

-- Mensuration
('31000000-0000-0000-0000-000000000024','exam-authority-1','22000000-0000-0000-0000-000000000005','Triangle'),
('31000000-0000-0000-0000-000000000025','exam-authority-1','22000000-0000-0000-0000-000000000005','Quadrilaterals'),
('31000000-0000-0000-0000-000000000026','exam-authority-1','22000000-0000-0000-0000-000000000005','Regular Polygons'),
('31000000-0000-0000-0000-000000000027','exam-authority-1','22000000-0000-0000-0000-000000000005','Circle'),
('31000000-0000-0000-0000-000000000028','exam-authority-1','22000000-0000-0000-0000-000000000005','Right Prism'),
('31000000-0000-0000-0000-000000000029','exam-authority-1','22000000-0000-0000-0000-000000000005','Right Circular Cone'),
('31000000-0000-0000-0000-000000000030','exam-authority-1','22000000-0000-0000-0000-000000000005','Right Circular Cylinder'),
('31000000-0000-0000-0000-000000000031','exam-authority-1','22000000-0000-0000-0000-000000000005','Sphere'),
('31000000-0000-0000-0000-000000000032','exam-authority-1','22000000-0000-0000-0000-000000000005','Hemisphere'),
('31000000-0000-0000-0000-000000000033','exam-authority-1','22000000-0000-0000-0000-000000000005','Rectangular Parallelepiped'),
('31000000-0000-0000-0000-000000000034','exam-authority-1','22000000-0000-0000-0000-000000000005','Regular Right Pyramid'),

-- Trigonometry
('31000000-0000-0000-0000-000000000035','exam-authority-1','22000000-0000-0000-0000-000000000006','Trigonometric Ratios'),
('31000000-0000-0000-0000-000000000036','exam-authority-1','22000000-0000-0000-0000-000000000006','Complementary Angles'),
('31000000-0000-0000-0000-000000000037','exam-authority-1','22000000-0000-0000-0000-000000000006','Heights and Distances'),
('31000000-0000-0000-0000-000000000038','exam-authority-1','22000000-0000-0000-0000-000000000006','Standard Identities'),

-- Statistics
('31000000-0000-0000-0000-000000000039','exam-authority-1','22000000-0000-0000-0000-000000000007','Histogram'),
('31000000-0000-0000-0000-000000000040','exam-authority-1','22000000-0000-0000-0000-000000000007','Frequency Polygon'),
('31000000-0000-0000-0000-000000000041','exam-authority-1','22000000-0000-0000-0000-000000000007','Bar Diagram'),
('31000000-0000-0000-0000-000000000042','exam-authority-1','22000000-0000-0000-0000-000000000007','Pie Chart'),
('31000000-0000-0000-0000-000000000043','exam-authority-1','22000000-0000-0000-0000-000000000007','Mean'),
('31000000-0000-0000-0000-000000000044','exam-authority-1','22000000-0000-0000-0000-000000000007','Median'),
('31000000-0000-0000-0000-000000000045','exam-authority-1','22000000-0000-0000-0000-000000000007','Mode'),
('31000000-0000-0000-0000-000000000046','exam-authority-1','22000000-0000-0000-0000-000000000007','Standard Deviation'),
('31000000-0000-0000-0000-000000000047','exam-authority-1','22000000-0000-0000-0000-000000000007','Simple Probability');


-- ------------------------------------------------------------
-- English
-- ------------------------------------------------------------

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name)
VALUES
('32000000-0000-0000-0000-000000000001','exam-authority-1','23000000-0000-0000-0000-000000000001','Synonyms'),
('32000000-0000-0000-0000-000000000002','exam-authority-1','23000000-0000-0000-0000-000000000001','Antonyms'),
('32000000-0000-0000-0000-000000000003','exam-authority-1','23000000-0000-0000-0000-000000000001','Homonyms'),
('32000000-0000-0000-0000-000000000004','exam-authority-1','23000000-0000-0000-0000-000000000001','Vocabulary Usage'),

('32000000-0000-0000-0000-000000000005','exam-authority-1','23000000-0000-0000-0000-000000000002','Grammar Rules'),
('32000000-0000-0000-0000-000000000006','exam-authority-1','23000000-0000-0000-0000-000000000002','Verb Usage'),

('32000000-0000-0000-0000-000000000007','exam-authority-1','23000000-0000-0000-0000-000000000014','Sentence Parts'),
('32000000-0000-0000-0000-000000000008','exam-authority-1','23000000-0000-0000-0000-000000000014','Sentence Ordering'),

('32000000-0000-0000-0000-000000000009','exam-authority-1','23000000-0000-0000-0000-000000000016','Literary Passage'),
('32000000-0000-0000-0000-000000000010','exam-authority-1','23000000-0000-0000-0000-000000000016','Current Affairs Passage'),
('32000000-0000-0000-0000-000000000011','exam-authority-1','23000000-0000-0000-0000-000000000016','Report or Editorial Passage');


-- ------------------------------------------------------------
-- Computer Knowledge
-- ------------------------------------------------------------

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name)
VALUES
('33000000-0000-0000-0000-000000000001','exam-authority-1','24000000-0000-0000-0000-000000000001','Computer Organization'),
('33000000-0000-0000-0000-000000000002','exam-authority-1','24000000-0000-0000-0000-000000000001','Central Processing Unit'),
('33000000-0000-0000-0000-000000000003','exam-authority-1','24000000-0000-0000-0000-000000000001','Input and Output Devices'),
('33000000-0000-0000-0000-000000000004','exam-authority-1','24000000-0000-0000-0000-000000000001','Computer Memory'),
('33000000-0000-0000-0000-000000000005','exam-authority-1','24000000-0000-0000-0000-000000000001','Memory Organization'),
('33000000-0000-0000-0000-000000000006','exam-authority-1','24000000-0000-0000-0000-000000000001','Backup Devices'),
('33000000-0000-0000-0000-000000000007','exam-authority-1','24000000-0000-0000-0000-000000000001','Ports'),
('33000000-0000-0000-0000-000000000008','exam-authority-1','24000000-0000-0000-0000-000000000001','Windows Explorer'),
('33000000-0000-0000-0000-000000000009','exam-authority-1','24000000-0000-0000-0000-000000000001','Keyboard Shortcuts'),

('33000000-0000-0000-0000-000000000010','exam-authority-1','24000000-0000-0000-0000-000000000002','Windows Operating System'),
('33000000-0000-0000-0000-000000000011','exam-authority-1','24000000-0000-0000-0000-000000000002','Microsoft Word'),
('33000000-0000-0000-0000-000000000012','exam-authority-1','24000000-0000-0000-0000-000000000002','Microsoft Excel'),
('33000000-0000-0000-0000-000000000013','exam-authority-1','24000000-0000-0000-0000-000000000002','Microsoft PowerPoint'),

('33000000-0000-0000-0000-000000000014','exam-authority-1','24000000-0000-0000-0000-000000000003','Web Browsing'),
('33000000-0000-0000-0000-000000000015','exam-authority-1','24000000-0000-0000-0000-000000000003','Web Searching'),
('33000000-0000-0000-0000-000000000016','exam-authority-1','24000000-0000-0000-0000-000000000003','Downloading and Uploading'),
('33000000-0000-0000-0000-000000000017','exam-authority-1','24000000-0000-0000-0000-000000000003','E-mail Management'),
('33000000-0000-0000-0000-000000000018','exam-authority-1','24000000-0000-0000-0000-000000000003','E-banking'),

('33000000-0000-0000-0000-000000000019','exam-authority-1','24000000-0000-0000-0000-000000000004','Networking Devices'),
('33000000-0000-0000-0000-000000000020','exam-authority-1','24000000-0000-0000-0000-000000000004','Network Protocols'),
('33000000-0000-0000-0000-000000000021','exam-authority-1','24000000-0000-0000-0000-000000000004','Hacking'),
('33000000-0000-0000-0000-000000000022','exam-authority-1','24000000-0000-0000-0000-000000000004','Computer Viruses'),
('33000000-0000-0000-0000-000000000023','exam-authority-1','24000000-0000-0000-0000-000000000004','Worms'),
('33000000-0000-0000-0000-000000000024','exam-authority-1','24000000-0000-0000-0000-000000000004','Trojan'),
('33000000-0000-0000-0000-000000000025','exam-authority-1','24000000-0000-0000-0000-000000000004','Preventive Security Measures');


-- ------------------------------------------------------------
-- Statistics
-- ------------------------------------------------------------

INSERT INTO question_service.subtopic (id, tenant_id, topic_id, name)
VALUES
('34000000-0000-0000-0000-000000000001','exam-authority-1','25000000-0000-0000-0000-000000000001','Primary Data'),
('34000000-0000-0000-0000-000000000002','exam-authority-1','25000000-0000-0000-0000-000000000001','Secondary Data'),
('34000000-0000-0000-0000-000000000003','exam-authority-1','25000000-0000-0000-0000-000000000001','Methods of Data Collection'),
('34000000-0000-0000-0000-000000000004','exam-authority-1','25000000-0000-0000-0000-000000000001','Tabulation'),
('34000000-0000-0000-0000-000000000005','exam-authority-1','25000000-0000-0000-0000-000000000001','Graphs and Charts'),
('34000000-0000-0000-0000-000000000006','exam-authority-1','25000000-0000-0000-0000-000000000001','Frequency Distributions'),

('34000000-0000-0000-0000-000000000007','exam-authority-1','25000000-0000-0000-0000-000000000002','Mean'),
('34000000-0000-0000-0000-000000000008','exam-authority-1','25000000-0000-0000-0000-000000000002','Median'),
('34000000-0000-0000-0000-000000000009','exam-authority-1','25000000-0000-0000-0000-000000000002','Mode'),
('34000000-0000-0000-0000-000000000010','exam-authority-1','25000000-0000-0000-0000-000000000002','Quartiles'),
('34000000-0000-0000-0000-000000000011','exam-authority-1','25000000-0000-0000-0000-000000000002','Deciles'),
('34000000-0000-0000-0000-000000000012','exam-authority-1','25000000-0000-0000-0000-000000000002','Percentiles'),

('34000000-0000-0000-0000-000000000013','exam-authority-1','25000000-0000-0000-0000-000000000003','Range'),
('34000000-0000-0000-0000-000000000014','exam-authority-1','25000000-0000-0000-0000-000000000003','Quartile Deviation'),
('34000000-0000-0000-0000-000000000015','exam-authority-1','25000000-0000-0000-0000-000000000003','Mean Deviation'),
('34000000-0000-0000-0000-000000000016','exam-authority-1','25000000-0000-0000-0000-000000000003','Standard Deviation'),
('34000000-0000-0000-0000-000000000017','exam-authority-1','25000000-0000-0000-0000-000000000003','Relative Dispersion'),

('34000000-0000-0000-0000-000000000018','exam-authority-1','25000000-0000-0000-0000-000000000004','Moments'),
('34000000-0000-0000-0000-000000000019','exam-authority-1','25000000-0000-0000-0000-000000000004','Skewness'),
('34000000-0000-0000-0000-000000000020','exam-authority-1','25000000-0000-0000-0000-000000000004','Kurtosis'),

('34000000-0000-0000-0000-000000000021','exam-authority-1','25000000-0000-0000-0000-000000000005','Scatter Diagram'),
('34000000-0000-0000-0000-000000000022','exam-authority-1','25000000-0000-0000-0000-000000000005','Simple Correlation Coefficient'),
('34000000-0000-0000-0000-000000000023','exam-authority-1','25000000-0000-0000-0000-000000000005','Regression Lines'),
('34000000-0000-0000-0000-000000000024','exam-authority-1','25000000-0000-0000-0000-000000000005','Spearman Rank Correlation'),
('34000000-0000-0000-0000-000000000025','exam-authority-1','25000000-0000-0000-0000-000000000005','Association of Attributes'),
('34000000-0000-0000-0000-000000000026','exam-authority-1','25000000-0000-0000-0000-000000000005','Multiple Regression'),
('34000000-0000-0000-0000-000000000027','exam-authority-1','25000000-0000-0000-0000-000000000005','Multiple and Partial Correlation'),

('34000000-0000-0000-0000-000000000028','exam-authority-1','25000000-0000-0000-0000-000000000006','Meaning of Probability'),
('34000000-0000-0000-0000-000000000029','exam-authority-1','25000000-0000-0000-0000-000000000006','Definitions of Probability'),
('34000000-0000-0000-0000-000000000030','exam-authority-1','25000000-0000-0000-0000-000000000006','Conditional Probability'),
('34000000-0000-0000-0000-000000000031','exam-authority-1','25000000-0000-0000-0000-000000000006','Compound Probability'),
('34000000-0000-0000-0000-000000000032','exam-authority-1','25000000-0000-0000-0000-000000000006','Independent Events'),
('34000000-0000-0000-0000-000000000033','exam-authority-1','25000000-0000-0000-0000-000000000006','Bayes Theorem'),

('34000000-0000-0000-0000-000000000034','exam-authority-1','25000000-0000-0000-0000-000000000007','Random Variables'),
('34000000-0000-0000-0000-000000000035','exam-authority-1','25000000-0000-0000-0000-000000000007','Probability Functions'),
('34000000-0000-0000-0000-000000000036','exam-authority-1','25000000-0000-0000-0000-000000000007','Expectation and Variance'),
('34000000-0000-0000-0000-000000000037','exam-authority-1','25000000-0000-0000-0000-000000000007','Higher Moments'),
('34000000-0000-0000-0000-000000000038','exam-authority-1','25000000-0000-0000-0000-000000000007','Binomial Distribution'),
('34000000-0000-0000-0000-000000000039','exam-authority-1','25000000-0000-0000-0000-000000000007','Poisson Distribution'),
('34000000-0000-0000-0000-000000000040','exam-authority-1','25000000-0000-0000-0000-000000000007','Normal Distribution'),
('34000000-0000-0000-0000-000000000041','exam-authority-1','25000000-0000-0000-0000-000000000007','Exponential Distribution'),
('34000000-0000-0000-0000-000000000042','exam-authority-1','25000000-0000-0000-0000-000000000007','Joint Distribution'),

('34000000-0000-0000-0000-000000000043','exam-authority-1','25000000-0000-0000-0000-000000000008','Population and Sample'),
('34000000-0000-0000-0000-000000000044','exam-authority-1','25000000-0000-0000-0000-000000000008','Parameter and Statistic'),
('34000000-0000-0000-0000-000000000045','exam-authority-1','25000000-0000-0000-0000-000000000008','Sampling Errors'),
('34000000-0000-0000-0000-000000000046','exam-authority-1','25000000-0000-0000-0000-000000000008','Probability Sampling'),
('34000000-0000-0000-0000-000000000047','exam-authority-1','25000000-0000-0000-0000-000000000008','Non-Probability Sampling'),
('34000000-0000-0000-0000-000000000048','exam-authority-1','25000000-0000-0000-0000-000000000008','Sampling Distribution'),
('34000000-0000-0000-0000-000000000049','exam-authority-1','25000000-0000-0000-0000-000000000008','Sample Size Decisions'),

('34000000-0000-0000-0000-000000000050','exam-authority-1','25000000-0000-0000-0000-000000000009','Point Estimation'),
('34000000-0000-0000-0000-000000000051','exam-authority-1','25000000-0000-0000-0000-000000000009','Interval Estimation'),
('34000000-0000-0000-0000-000000000052','exam-authority-1','25000000-0000-0000-0000-000000000009','Properties of Good Estimator'),
('34000000-0000-0000-0000-000000000053','exam-authority-1','25000000-0000-0000-0000-000000000009','Method of Moments'),
('34000000-0000-0000-0000-000000000054','exam-authority-1','25000000-0000-0000-0000-000000000009','Maximum Likelihood'),
('34000000-0000-0000-0000-000000000055','exam-authority-1','25000000-0000-0000-0000-000000000009','Least Squares'),
('34000000-0000-0000-0000-000000000056','exam-authority-1','25000000-0000-0000-0000-000000000009','Hypothesis Testing'),
('34000000-0000-0000-0000-000000000057','exam-authority-1','25000000-0000-0000-0000-000000000009','Z Test'),
('34000000-0000-0000-0000-000000000058','exam-authority-1','25000000-0000-0000-0000-000000000009','t Test'),
('34000000-0000-0000-0000-000000000059','exam-authority-1','25000000-0000-0000-0000-000000000009','Chi-Square Test'),
('34000000-0000-0000-0000-000000000060','exam-authority-1','25000000-0000-0000-0000-000000000009','F Test'),
('34000000-0000-0000-0000-000000000061','exam-authority-1','25000000-0000-0000-0000-000000000009','Confidence Intervals'),

('34000000-0000-0000-0000-000000000062','exam-authority-1','25000000-0000-0000-0000-000000000010','One-Way ANOVA'),
('34000000-0000-0000-0000-000000000063','exam-authority-1','25000000-0000-0000-0000-000000000010','Two-Way ANOVA'),

('34000000-0000-0000-0000-000000000064','exam-authority-1','25000000-0000-0000-0000-000000000011','Components of Time Series'),
('34000000-0000-0000-0000-000000000065','exam-authority-1','25000000-0000-0000-0000-000000000011','Trend Component'),
('34000000-0000-0000-0000-000000000066','exam-authority-1','25000000-0000-0000-0000-000000000011','Seasonal Variation'),

('34000000-0000-0000-0000-000000000067','exam-authority-1','25000000-0000-0000-0000-000000000012','Meaning of Index Numbers'),
('34000000-0000-0000-0000-000000000068','exam-authority-1','25000000-0000-0000-0000-000000000012','Construction of Index Numbers'),
('34000000-0000-0000-0000-000000000069','exam-authority-1','25000000-0000-0000-0000-000000000012','Types of Index Numbers'),
('34000000-0000-0000-0000-000000000070','exam-authority-1','25000000-0000-0000-0000-000000000012','Index Number Formulae'),
('34000000-0000-0000-0000-000000000071','exam-authority-1','25000000-0000-0000-0000-000000000012','Base Shifting and Splicing'),
('34000000-0000-0000-0000-000000000072','exam-authority-1','25000000-0000-0000-0000-000000000012','Cost of Living Index Numbers'),
('34000000-0000-0000-0000-000000000073','exam-authority-1','25000000-0000-0000-0000-000000000012','Uses of Index Numbers');