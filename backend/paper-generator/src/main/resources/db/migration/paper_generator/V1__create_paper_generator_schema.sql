-- SPDX-License-Identifier: AGPL-3.0-only
--
-- NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2026 NAG Contributors

CREATE SCHEMA IF NOT EXISTS paper_generator;

-- ============================================================
-- Table: blueprint
-- Reusable, canonical blueprint definition independent of individual exams.
-- tenant_id IS NULL indicates a platform-default blueprint available to all tenants.
-- ============================================================
CREATE TABLE paper_generator.blueprint (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255),                  -- NULL = platform default blueprint
    code                    VARCHAR(100),                  -- unique code per tenant / platform
    name                    VARCHAR(255) NOT NULL,
    description             VARCHAR(1000),
    version                 INTEGER      NOT NULL DEFAULT 1,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / DRAFT / ARCHIVED
    total_questions         INTEGER      NOT NULL DEFAULT 0,
    total_marks             DOUBLE PRECISION,
    created_by              UUID,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version_opt             BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_blueprint_tenant_code
    ON paper_generator.blueprint(tenant_id, code)
    WHERE code IS NOT NULL;

CREATE INDEX idx_blueprint_tenant_id ON paper_generator.blueprint(tenant_id);
CREATE INDEX idx_blueprint_status    ON paper_generator.blueprint(status);

-- ============================================================
-- Table: blueprint_section
-- First-class sections within a blueprint, targeting canonical taxonomy subjects.
-- ============================================================
CREATE TABLE paper_generator.blueprint_section (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255),
    blueprint_id            UUID         NOT NULL
                                REFERENCES paper_generator.blueprint(id) ON DELETE CASCADE,
    name                    VARCHAR(255) NOT NULL,
    order_index             INTEGER      NOT NULL DEFAULT 0,
    subject_id              BIGINT       NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version_opt             BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_blueprint_section_blueprint_id ON paper_generator.blueprint_section(blueprint_id);
CREATE INDEX idx_blueprint_section_subject_id   ON paper_generator.blueprint_section(subject_id);

-- ============================================================
-- Table: blueprint_rule
-- Relational rules targeting subject, optional topic, and optional subtopic.
-- Granularity is the narrowest non-null node identifier.
-- ============================================================
CREATE TABLE paper_generator.blueprint_rule (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255),
    blueprint_section_id    UUID         NOT NULL
                                REFERENCES paper_generator.blueprint_section(id) ON DELETE CASCADE,
    subject_id              BIGINT       NOT NULL,
    topic_id                BIGINT,
    subtopic_id             BIGINT,
    difficulty              VARCHAR(20),                   -- EASY / MEDIUM / HARD / NULL
    cognitive_level         VARCHAR(30),                   -- REMEMBER / UNDERSTAND / APPLY / ANALYZE / EVALUATE / CREATE / NULL
    question_count          INTEGER      NOT NULL,
    marks_override          DOUBLE PRECISION,
    order_index             INTEGER      NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version_opt             BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_blueprint_rule_section_id  ON paper_generator.blueprint_rule(blueprint_section_id);
CREATE INDEX idx_blueprint_rule_subject_id  ON paper_generator.blueprint_rule(subject_id);
CREATE INDEX idx_blueprint_rule_topic_id    ON paper_generator.blueprint_rule(topic_id);
CREATE INDEX idx_blueprint_rule_subtopic_id ON paper_generator.blueprint_rule(subtopic_id);

-- ============================================================
-- Table: paper
-- ============================================================
CREATE TABLE paper_generator.paper (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    name                    VARCHAR(255),
    exam_id                 UUID         NOT NULL,
    blueprint_id            UUID         REFERENCES paper_generator.blueprint(id),
    blueprint_version       INTEGER      NOT NULL DEFAULT 1,
    shift_id                VARCHAR(255) NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    is_practice             BOOLEAN      NOT NULL DEFAULT FALSE,
    paper_definition_json   JSONB,
    difficulty_score        DOUBLE PRECISION,
    topic_distribution_json JSONB,
    encrypted_package_ref   TEXT,
    encryption_key_id       VARCHAR(255),
    generated_by            UUID,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_paper_tenant_id    ON paper_generator.paper(tenant_id);
CREATE INDEX idx_paper_exam_id      ON paper_generator.paper(exam_id);
CREATE INDEX idx_paper_blueprint_id ON paper_generator.paper(blueprint_id);
CREATE INDEX idx_paper_shift_id     ON paper_generator.paper(shift_id);
CREATE INDEX idx_paper_status       ON paper_generator.paper(status);

-- ============================================================
-- Table: paper_section
-- Relational sections preserving section boundaries within a generated paper.
-- ============================================================
CREATE TABLE paper_generator.paper_section (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    paper_id                UUID         NOT NULL
                                REFERENCES paper_generator.paper(id) ON DELETE CASCADE,
    blueprint_section_id    UUID,
    examination_section_id  UUID,
    name                    VARCHAR(255) NOT NULL,
    order_index             INTEGER      NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_paper_section_paper_id ON paper_generator.paper_section(paper_id);

-- ============================================================
-- Table: paper_question
-- Individual question instances inside paper sections with ordering and subtopic snapshot.
-- ============================================================
CREATE TABLE paper_generator.paper_question (
    id                      UUID         PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    paper_section_id        UUID         NOT NULL
                                REFERENCES paper_generator.paper_section(id) ON DELETE CASCADE,
    question_id             UUID         NOT NULL,
    position                INTEGER      NOT NULL,
    subtopic_id             BIGINT,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_paper_section_position UNIQUE (paper_section_id, position)
);

CREATE INDEX idx_paper_question_section  ON paper_generator.paper_question(paper_section_id);
CREATE INDEX idx_paper_question_qid      ON paper_generator.paper_question(question_id);
CREATE INDEX idx_paper_question_subtopic ON paper_generator.paper_question(subtopic_id);

-- ============================================================
-- Backward-compatibility table for blueprint_template
-- ============================================================
CREATE TABLE paper_generator.blueprint_template (
    id           UUID         PRIMARY KEY,
    tenant_id    VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    exam_id      UUID,
    rules_json   JSONB        NOT NULL,
    created_by   UUID,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_blueprint_template_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_blueprint_template_tenant_id ON paper_generator.blueprint_template(tenant_id);
CREATE INDEX idx_blueprint_template_exam_id   ON paper_generator.blueprint_template(exam_id);

-- ============================================================
-- Seed Platform Blueprints
-- ============================================================
INSERT INTO paper_generator.blueprint (
    id, tenant_id, code, name, description, version, status, total_questions, total_marks, created_by, created_at, updated_at, version_opt
) VALUES
('a1000000-0000-0000-0000-000000000001', NULL, 'SSC-CGL-TIER1', 'SSC CGL Tier-1 (100 Questions - Standard)', 'Official blueprint structure for SSC CGL Tier-1 CBT: 25 Qs Reasoning, 25 Qs General Awareness, 25 Qs Quantitative Aptitude, 25 Qs English Comprehension.', 1, 'ACTIVE', 100, 200.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000002', NULL, 'BANKING-PO-PRE', 'Banking PO / Clerk Prelims (IBPS / SBI - 100 Qs)', 'Standard blueprint for IBPS PO, SBI PO: English Language (30 Qs), Quantitative Aptitude (35 Qs), and Reasoning Ability (35 Qs).', 1, 'ACTIVE', 100, 100.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000003', NULL, 'RRB-NTPC-CBT1', 'RRB NTPC CBT-1 (Railway Non-Technical - 100 Qs)', '100-question recruitment template for Railway Recruitment Board NTPC: General Awareness (40 Qs), Mathematics (30 Qs), Reasoning (30 Qs).', 1, 'ACTIVE', 100, 100.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000004', NULL, 'UPSC-CSE-PRE-GS1', 'UPSC CSE Prelims Paper-1 (General Studies - 100 Qs)', 'Standard 100-question blueprint for Union Public Service Commission Civil Services Preliminary Examination GS Paper-1.', 1, 'ACTIVE', 100, 200.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000005', NULL, 'UPSC-CSE-CSAT', 'UPSC CSE Prelims Paper-2 (CSAT Aptitude - 80 Qs)', 'Standard 80-question blueprint for UPSC CSE Paper-2: Reading Comprehension (28 Qs), Reasoning (26 Qs), Basic Numeracy (26 Qs).', 1, 'ACTIVE', 80, 200.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000006', NULL, 'SSC-CGL-TIER2', 'SSC CGL Tier-2 (Paper-1 Core - 150 Qs)', 'Comprehensive 150-question blueprint for SSC CGL Tier-2 CBT.', 1, 'ACTIVE', 150, 450.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000007', NULL, 'STATE-PSC-PRE', 'State PSC Combined Prelims (General Studies - 150 Qs)', '150-question blueprint for State Public Service Commission Combined Competitive Preliminary Examination.', 1, 'ACTIVE', 150, 150.0, NULL, NOW(), NOW(), 0),
('a1000000-0000-0000-0000-000000000008', NULL, 'CTET-PAPER1', 'CTET / TET Paper-1 (Primary Teacher - 150 Qs)', 'Standard 150-question blueprint for Central Teacher Eligibility Test Paper-1.', 1, 'ACTIVE', 150, 150.0, NULL, NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- Seed Blueprint Sections
INSERT INTO paper_generator.blueprint_section (
    id, tenant_id, blueprint_id, name, order_index, subject_id, created_at, updated_at, version_opt
) VALUES
-- SSC CGL Tier-1 (Sections 1..4)
('b1000000-0000-0000-0000-000000000001', NULL, 'a1000000-0000-0000-0000-000000000001', 'General Intelligence and Reasoning', 0, 1, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000002', NULL, 'a1000000-0000-0000-0000-000000000001', 'General Awareness',                  1, 2, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000003', NULL, 'a1000000-0000-0000-0000-000000000001', 'Quantitative Aptitude',              2, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000004', NULL, 'a1000000-0000-0000-0000-000000000001', 'English Comprehension',              3, 4, NOW(), NOW(), 0),

-- Banking PO Prelims (Sections 5..7)
('b1000000-0000-0000-0000-000000000005', NULL, 'a1000000-0000-0000-0000-000000000002', 'English Language',                  0, 4, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000006', NULL, 'a1000000-0000-0000-0000-000000000002', 'Quantitative Aptitude',              1, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000007', NULL, 'a1000000-0000-0000-0000-000000000002', 'Reasoning Ability',                  2, 1, NOW(), NOW(), 0),

-- RRB NTPC CBT-1 (Sections 8..10)
('b1000000-0000-0000-0000-000000000008', NULL, 'a1000000-0000-0000-0000-000000000003', 'General Awareness',                  0, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000009', NULL, 'a1000000-0000-0000-0000-000000000003', 'Mathematics',                        1, 1, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000010', NULL, 'a1000000-0000-0000-0000-000000000003', 'General Intelligence and Reasoning', 2, 2, NOW(), NOW(), 0),

-- UPSC CSE GS1 (Section 11)
('b1000000-0000-0000-0000-000000000011', NULL, 'a1000000-0000-0000-0000-000000000004', 'General Studies',                      0, 3, NOW(), NOW(), 0),

-- CTET Paper-1 (Sections 12..16)
('b1000000-0000-0000-0000-000000000012', NULL, 'a1000000-0000-0000-0000-000000000008', 'Child Development and Pedagogy',   0, 13, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000013', NULL, 'a1000000-0000-0000-0000-000000000008', 'Mathematics',                        1, 1,  NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000014', NULL, 'a1000000-0000-0000-0000-000000000008', 'Environmental Studies',               2, 12, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000015', NULL, 'a1000000-0000-0000-0000-000000000008', 'General Hindi',                         3, 10, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000016', NULL, 'a1000000-0000-0000-0000-000000000008', 'English Language',                  4, 4,  NOW(), NOW(), 0),

-- UPSC CSE CSAT Paper-2 (Sections 17..19)
('b1000000-0000-0000-0000-000000000017', NULL, 'a1000000-0000-0000-0000-000000000005', 'Reading Comprehension',                  0, 4, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000018', NULL, 'a1000000-0000-0000-0000-000000000005', 'Reasoning and Mental Ability',          1, 2, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000019', NULL, 'a1000000-0000-0000-0000-000000000005', 'Basic Numeracy and Data Interpretation', 2, 1, NOW(), NOW(), 0),

-- SSC CGL Tier-2 (Sections 20..24)
('b1000000-0000-0000-0000-000000000020', NULL, 'a1000000-0000-0000-0000-000000000006', 'Mathematical Abilities',              0, 1, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000021', NULL, 'a1000000-0000-0000-0000-000000000006', 'Reasoning and General Intelligence',  1, 2, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000022', NULL, 'a1000000-0000-0000-0000-000000000006', 'English Language and Comprehension',  2, 4, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000023', NULL, 'a1000000-0000-0000-0000-000000000006', 'General Awareness',                  3, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000024', NULL, 'a1000000-0000-0000-0000-000000000006', 'Computer Knowledge',                  4, 5, NOW(), NOW(), 0),

-- State PSC Prelims (Sections 25..29)
('b1000000-0000-0000-0000-000000000027', NULL, 'a1000000-0000-0000-0000-000000000007', 'Geography and Environment',            2, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000025', NULL, 'a1000000-0000-0000-0000-000000000007', 'General Science and Technology',        0, 6, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000026', NULL, 'a1000000-0000-0000-0000-000000000007', 'History and Indian National Movement',  1, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000028', NULL, 'a1000000-0000-0000-0000-000000000007', 'Indian Polity and Economy',            3, 3, NOW(), NOW(), 0),
('b1000000-0000-0000-0000-000000000029', NULL, 'a1000000-0000-0000-0000-000000000007', 'General Mental Ability and Aptitude',  4, 2, NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- Seed Representative Blueprint Rules
INSERT INTO paper_generator.blueprint_rule (
    id, tenant_id, blueprint_section_id, subject_id, topic_id, subtopic_id, difficulty, cognitive_level, question_count, marks_override, order_index, created_at, updated_at, version_opt
) VALUES
-- SSC CGL Tier-1: Section 1 (Reasoning, 25 Qs)
('c1000000-0000-0000-0000-000000000001', NULL, 'b1000000-0000-0000-0000-000000000001', 1, NULL, NULL, 'EASY',   'UNDERSTAND', 5, 2.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000002', NULL, 'b1000000-0000-0000-0000-000000000001', 1, NULL, NULL, 'MEDIUM', 'APPLY',      12, 2.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000003', NULL, 'b1000000-0000-0000-0000-000000000001', 1, NULL, NULL, 'HARD',   'ANALYZE',    8, 2.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-1: Section 2 (General Awareness, 25 Qs)
('c1000000-0000-0000-0000-000000000004', NULL, 'b1000000-0000-0000-0000-000000000002', 2, NULL, NULL, 'EASY',   'REMEMBER',   8, 2.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000005', NULL, 'b1000000-0000-0000-0000-000000000002', 2, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 12, 2.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000006', NULL, 'b1000000-0000-0000-0000-000000000002', 2, NULL, NULL, 'HARD',   'ANALYZE',    5, 2.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-1: Section 3 (Quantitative Aptitude, 25 Qs)
('c1000000-0000-0000-0000-000000000007', NULL, 'b1000000-0000-0000-0000-000000000003', 3, NULL, NULL, 'EASY',   'UNDERSTAND', 5, 2.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000008', NULL, 'b1000000-0000-0000-0000-000000000003', 3, NULL, NULL, 'MEDIUM', 'APPLY',      13, 2.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000009', NULL, 'b1000000-0000-0000-0000-000000000003', 3, NULL, NULL, 'HARD',   'ANALYZE',    7, 2.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-1: Section 4 (English Comprehension, 25 Qs)
('c1000000-0000-0000-0000-000000000010', NULL, 'b1000000-0000-0000-0000-000000000004', 4, NULL, NULL, 'EASY',   'UNDERSTAND', 7, 2.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000011', NULL, 'b1000000-0000-0000-0000-000000000004', 4, NULL, NULL, 'MEDIUM', 'APPLY',      12, 2.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000012', NULL, 'b1000000-0000-0000-0000-000000000004', 4, NULL, NULL, 'HARD',   'ANALYZE',    6, 2.0, 2, NOW(), NOW(), 0),

-- Banking PO: Section 5 (English, 30 Qs)
('c1000000-0000-0000-0000-000000000013', NULL, 'b1000000-0000-0000-0000-000000000005', 4, NULL, NULL, 'MEDIUM', 'ANALYZE',    20, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000014', NULL, 'b1000000-0000-0000-0000-000000000005', 4, NULL, NULL, 'HARD',   'EVALUATE',   10, 1.0, 1, NOW(), NOW(), 0),

-- Banking PO: Section 6 (Quantitative Aptitude, 35 Qs)
('c1000000-0000-0000-0000-000000000006', NULL, 'b1000000-0000-0000-0000-000000000006', 3, NULL, NULL, 'MEDIUM', 'APPLY',      20, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000016', NULL, 'b1000000-0000-0000-0000-000000000006', 3, NULL, NULL, 'HARD',   'ANALYZE',    15, 1.0, 1, NOW(), NOW(), 0),

-- Banking PO: Section 7 (Reasoning, 35 Qs)
('c1000000-0000-0000-0000-000000000017', NULL, 'b1000000-0000-0000-0000-000000000007', 1, NULL, NULL, 'MEDIUM', 'APPLY',      20, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000018', NULL, 'b1000000-0000-0000-0000-000000000007', 1, NULL, NULL, 'HARD',   'ANALYZE',    15, 1.0, 1, NOW(), NOW(), 0),

-- UPSC CSE GS1: Section 11 (General Studies, 100 Qs - 200 Marks)
('c1000000-0000-0000-0000-000000000019', NULL, 'b1000000-0000-0000-0000-000000000011', 3, NULL, NULL, 'EASY',   'UNDERSTAND', 20, 2.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000020', NULL, 'b1000000-0000-0000-0000-000000000011', 3, NULL, NULL, 'MEDIUM', 'APPLY',      50, 2.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000030', NULL, 'b1000000-0000-0000-0000-000000000011', 3, NULL, NULL, 'HARD',   'ANALYZE',    30, 2.0, 2, NOW(), NOW(), 0),

-- RRB NTPC CBT-1: Section 8 (General Awareness, 40 Qs - 40 Marks)
('c1000000-0000-0000-0000-000000000021', NULL, 'b1000000-0000-0000-0000-000000000008', 3, NULL, NULL, 'EASY',   'REMEMBER',   12, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000022', NULL, 'b1000000-0000-0000-0000-000000000008', 3, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 20, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000023', NULL, 'b1000000-0000-0000-0000-000000000008', 3, NULL, NULL, 'HARD',   'ANALYZE',     8, 1.0, 2, NOW(), NOW(), 0),

-- RRB NTPC CBT-1: Section 9 (Mathematics, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000024', NULL, 'b1000000-0000-0000-0000-000000000009', 1, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000025', NULL, 'b1000000-0000-0000-0000-000000000009', 1, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000026', NULL, 'b1000000-0000-0000-0000-000000000009', 1, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- RRB NTPC CBT-1: Section 10 (General Intelligence & Reasoning, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000027', NULL, 'b1000000-0000-0000-0000-000000000010', 2, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000028', NULL, 'b1000000-0000-0000-0000-000000000010', 2, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000029', NULL, 'b1000000-0000-0000-0000-000000000010', 2, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- UPSC CSE CSAT Paper-2: Section 17 (Reading Comprehension, 28 Qs - 70 Marks)
('c1000000-0000-0000-0000-000000000031', NULL, 'b1000000-0000-0000-0000-000000000017', 4, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 2.5, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000032', NULL, 'b1000000-0000-0000-0000-000000000017', 4, NULL, NULL, 'MEDIUM', 'APPLY',      14, 2.5, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000033', NULL, 'b1000000-0000-0000-0000-000000000017', 4, NULL, NULL, 'HARD',   'ANALYZE',     8, 2.5, 2, NOW(), NOW(), 0),

-- UPSC CSE CSAT Paper-2: Section 18 (Reasoning and Mental Ability, 26 Qs - 65 Marks)
('c1000000-0000-0000-0000-000000000034', NULL, 'b1000000-0000-0000-0000-000000000018', 2, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 2.5, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000035', NULL, 'b1000000-0000-0000-0000-000000000018', 2, NULL, NULL, 'MEDIUM', 'APPLY',      12, 2.5, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000036', NULL, 'b1000000-0000-0000-0000-000000000018', 2, NULL, NULL, 'HARD',   'ANALYZE',     8, 2.5, 2, NOW(), NOW(), 0),

-- UPSC CSE CSAT Paper-2: Section 19 (Basic Numeracy and Data Interpretation, 26 Qs - 65 Marks)
('c1000000-0000-0000-0000-000000000037', NULL, 'b1000000-0000-0000-0000-000000000019', 1, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 2.5, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000038', NULL, 'b1000000-0000-0000-0000-000000000019', 1, NULL, NULL, 'MEDIUM', 'APPLY',      12, 2.5, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000039', NULL, 'b1000000-0000-0000-0000-000000000019', 1, NULL, NULL, 'HARD',   'ANALYZE',     8, 2.5, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-2: Section 20 (Mathematical Abilities, 30 Qs - 90 Marks)
('c1000000-0000-0000-0000-000000000040', NULL, 'b1000000-0000-0000-0000-000000000020', 1, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 3.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000041', NULL, 'b1000000-0000-0000-0000-000000000020', 1, NULL, NULL, 'MEDIUM', 'APPLY',      16, 3.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000042', NULL, 'b1000000-0000-0000-0000-000000000020', 1, NULL, NULL, 'HARD',   'ANALYZE',     8, 3.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-2: Section 21 (Reasoning and General Intelligence, 30 Qs - 90 Marks)
('c1000000-0000-0000-0000-000000000043', NULL, 'b1000000-0000-0000-0000-000000000021', 2, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 3.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000044', NULL, 'b1000000-0000-0000-0000-000000000021', 2, NULL, NULL, 'MEDIUM', 'APPLY',      16, 3.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000045', NULL, 'b1000000-0000-0000-0000-000000000021', 2, NULL, NULL, 'HARD',   'ANALYZE',     8, 3.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-2: Section 22 (English Language and Comprehension, 45 Qs - 135 Marks)
('c1000000-0000-0000-0000-000000000046', NULL, 'b1000000-0000-0000-0000-000000000022', 4, NULL, NULL, 'EASY',   'UNDERSTAND', 10, 3.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000047', NULL, 'b1000000-0000-0000-0000-000000000022', 4, NULL, NULL, 'MEDIUM', 'APPLY',      23, 3.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000048', NULL, 'b1000000-0000-0000-0000-000000000022', 4, NULL, NULL, 'HARD',   'ANALYZE',    12, 3.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-2: Section 23 (General Awareness, 25 Qs - 75 Marks)
('c1000000-0000-0000-0000-000000000049', NULL, 'b1000000-0000-0000-0000-000000000023', 3, NULL, NULL, 'EASY',   'REMEMBER',    6, 3.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000050', NULL, 'b1000000-0000-0000-0000-000000000023', 3, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 13, 3.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000051', NULL, 'b1000000-0000-0000-0000-000000000023', 3, NULL, NULL, 'HARD',   'ANALYZE',     6, 3.0, 2, NOW(), NOW(), 0),

-- SSC CGL Tier-2: Section 24 (Computer Knowledge, 20 Qs - 60 Marks)
('c1000000-0000-0000-0000-000000000052', NULL, 'b1000000-0000-0000-0000-000000000024', 5, NULL, NULL, 'EASY',   'REMEMBER',    6, 3.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000053', NULL, 'b1000000-0000-0000-0000-000000000024', 5, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 10, 3.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000054', NULL, 'b1000000-0000-0000-0000-000000000024', 5, NULL, NULL, 'HARD',   'APPLY',       4, 3.0, 2, NOW(), NOW(), 0),

-- State PSC Prelims: Section 25 (General Science and Technology, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000055', NULL, 'b1000000-0000-0000-0000-000000000025', 6, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000056', NULL, 'b1000000-0000-0000-0000-000000000025', 6, NULL, NULL, 'MEDIUM', 'APPLY',      16, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000057', NULL, 'b1000000-0000-0000-0000-000000000025', 6, NULL, NULL, 'HARD',   'ANALYZE',     6, 1.0, 2, NOW(), NOW(), 0),

-- State PSC Prelims: Section 26 (History and Indian National Movement, 35 Qs - 35 Marks)
('c1000000-0000-0000-0000-000000000058', NULL, 'b1000000-0000-0000-0000-000000000026', 3, NULL, NULL, 'EASY',   'REMEMBER',   10, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000059', NULL, 'b1000000-0000-0000-0000-000000000026', 3, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 18, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000060', NULL, 'b1000000-0000-0000-0000-000000000026', 3, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- State PSC Prelims: Section 27 (Geography and Environment, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000061', NULL, 'b1000000-0000-0000-0000-000000000027', 3, NULL, NULL, 'EASY',   'REMEMBER',    8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000062', NULL, 'b1000000-0000-0000-0000-000000000027', 3, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000063', NULL, 'b1000000-0000-0000-0000-000000000027', 3, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- State PSC Prelims: Section 28 (Indian Polity and Economy, 35 Qs - 35 Marks)
('c1000000-0000-0000-0000-000000000064', NULL, 'b1000000-0000-0000-0000-000000000028', 3, NULL, NULL, 'EASY',   'UNDERSTAND', 10, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000065', NULL, 'b1000000-0000-0000-0000-000000000028', 3, NULL, NULL, 'MEDIUM', 'APPLY',      18, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000066', NULL, 'b1000000-0000-0000-0000-000000000028', 3, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- State PSC Prelims: Section 29 (General Mental Ability and Aptitude, 20 Qs - 20 Marks)
('c1000000-0000-0000-0000-000000000067', NULL, 'b1000000-0000-0000-0000-000000000029', 2, NULL, NULL, 'EASY',   'UNDERSTAND',  6, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000068', NULL, 'b1000000-0000-0000-0000-000000000029', 2, NULL, NULL, 'MEDIUM', 'APPLY',      10, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000069', NULL, 'b1000000-0000-0000-0000-000000000029', 2, NULL, NULL, 'HARD',   'ANALYZE',     4, 1.0, 2, NOW(), NOW(), 0),

-- CTET Paper-1: Section 12 (Child Development and Pedagogy, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000070', NULL, 'b1000000-0000-0000-0000-000000000012', 13, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000071', NULL, 'b1000000-0000-0000-0000-000000000012', 13, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000072', NULL, 'b1000000-0000-0000-0000-000000000012', 13, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- CTET Paper-1: Section 13 (Mathematics, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000073', NULL, 'b1000000-0000-0000-0000-000000000013', 1, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000074', NULL, 'b1000000-0000-0000-0000-000000000013', 1, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000075', NULL, 'b1000000-0000-0000-0000-000000000013', 1, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- CTET Paper-1: Section 14 (Environmental Studies, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000076', NULL, 'b1000000-0000-0000-0000-000000000014', 12, NULL, NULL, 'EASY',   'REMEMBER',    8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000077', NULL, 'b1000000-0000-0000-0000-000000000014', 12, NULL, NULL, 'MEDIUM', 'UNDERSTAND', 15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000078', NULL, 'b1000000-0000-0000-0000-000000000014', 12, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- CTET Paper-1: Section 15 (General Hindi, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000079', NULL, 'b1000000-0000-0000-0000-000000000015', 10, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000080', NULL, 'b1000000-0000-0000-0000-000000000015', 10, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000081', NULL, 'b1000000-0000-0000-0000-000000000015', 10, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0),

-- CTET Paper-1: Section 16 (English Language, 30 Qs - 30 Marks)
('c1000000-0000-0000-0000-000000000082', NULL, 'b1000000-0000-0000-0000-000000000016', 4, NULL, NULL, 'EASY',   'UNDERSTAND',  8, 1.0, 0, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000083', NULL, 'b1000000-0000-0000-0000-000000000016', 4, NULL, NULL, 'MEDIUM', 'APPLY',      15, 1.0, 1, NOW(), NOW(), 0),
('c1000000-0000-0000-0000-000000000084', NULL, 'b1000000-0000-0000-0000-000000000016', 4, NULL, NULL, 'HARD',   'ANALYZE',     7, 1.0, 2, NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;
