CREATE SCHEMA IF NOT EXISTS question_service;

-- Enable pgvector extension for halfvec type (requires pgvector/pgvector Docker image)
CREATE EXTENSION IF NOT EXISTS vector SCHEMA public;

-- ============================================================
-- Subject -> Topic -> Subtopic hierarchy tables
-- ------------------------------------------------------------
-- Canonical platform taxonomy (tenant_id IS NULL) with tenant overrides
-- and extensions (tenant_id IS NOT NULL, base_node_id optional).
-- Numeric BIGINT identity primary keys provide compact 8-byte foreign keys.
-- ============================================================

CREATE TABLE question_service.subject (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(255),
    code          VARCHAR(50),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    base_node_id  BIGINT REFERENCES question_service.subject(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_subject_platform_code ON question_service.subject (code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_subject_tenant_code ON question_service.subject (tenant_id, code);
CREATE UNIQUE INDEX uq_subject_platform_name ON question_service.subject (name) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_subject_tenant_name ON question_service.subject (tenant_id, name);
CREATE INDEX idx_subject_tenant_id ON question_service.subject(tenant_id);

CREATE TABLE question_service.topic (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(255),
    subject_id    BIGINT NOT NULL REFERENCES question_service.subject(id) ON DELETE CASCADE,
    code          VARCHAR(50),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    base_node_id  BIGINT REFERENCES question_service.topic(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_topic_platform_code ON question_service.topic (subject_id, code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_topic_tenant_code ON question_service.topic (tenant_id, subject_id, code);
CREATE UNIQUE INDEX uq_topic_platform_name ON question_service.topic (subject_id, name) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_topic_tenant_name ON question_service.topic (tenant_id, subject_id, name);
CREATE INDEX idx_topic_tenant_id ON question_service.topic(tenant_id);
CREATE INDEX idx_topic_subject_id ON question_service.topic(subject_id);

CREATE TABLE question_service.subtopic (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     VARCHAR(255),
    topic_id      BIGINT NOT NULL REFERENCES question_service.topic(id) ON DELETE CASCADE,
    code          VARCHAR(50),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    base_node_id  BIGINT REFERENCES question_service.subtopic(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_subtopic_platform_code ON question_service.subtopic (topic_id, code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_subtopic_tenant_code ON question_service.subtopic (tenant_id, topic_id, code);
CREATE UNIQUE INDEX uq_subtopic_platform_name ON question_service.subtopic (topic_id, name) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_subtopic_tenant_name ON question_service.subtopic (tenant_id, topic_id, name);
CREATE INDEX idx_subtopic_tenant_id ON question_service.subtopic(tenant_id);
CREATE INDEX idx_subtopic_topic_id ON question_service.subtopic(topic_id);

-- ============================================================
-- Table: taxonomy_tenant_override (Tenant overrides on platform taxonomy)
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.taxonomy_tenant_override (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    node_type       VARCHAR(20) NOT NULL,
    node_id         BIGINT NOT NULL,
    hidden          BOOLEAN NOT NULL DEFAULT FALSE,
    name_override   VARCHAR(200),
    order_override  INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_taxonomy_tenant_override UNIQUE (tenant_id, node_type, node_id)
);

CREATE INDEX IF NOT EXISTS idx_taxonomy_tenant_override_tenant ON question_service.taxonomy_tenant_override(tenant_id);
CREATE INDEX IF NOT EXISTS idx_taxonomy_tenant_override_node ON question_service.taxonomy_tenant_override(node_type, node_id);

-- ============================================================
-- Table: passage (Comprehension Passages / Case Studies - Issue #135)
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.passage (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    title               VARCHAR(500),
    content             TEXT,
    content_format      VARCHAR(20) NOT NULL DEFAULT 'MIXED',
    subject_id          BIGINT NOT NULL REFERENCES question_service.subject(id),
    topic_id            BIGINT REFERENCES question_service.topic(id),
    subtopic_id         BIGINT REFERENCES question_service.subtopic(id),
    subject             VARCHAR(100),
    topic               VARCHAR(200),
    subtopic            VARCHAR(200),
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
CREATE INDEX IF NOT EXISTS idx_passage_subtopic_id ON question_service.passage(subtopic_id);
CREATE INDEX IF NOT EXISTS idx_passage_state ON question_service.passage(state);
CREATE INDEX IF NOT EXISTS idx_passage_embedding ON question_service.passage USING ivfflat (embedding halfvec_cosine_ops) WITH (lists = 50);

-- ============================================================
-- Table: question (hash-partitioned by subject_id for scalability)
-- ============================================================
-- SubTopic classification is mandatory (subtopic_id NOT NULL).
-- String denormalized columns (subject, topic, subtopic) and used_in_exam_ids_json are dropped.
-- The numeric FKs (subject_id, topic_id, subtopic_id) form the relational taxonomy spine.

CREATE TABLE question_service.question (
    id                      UUID NOT NULL,
    tenant_id               VARCHAR(255) NOT NULL,
    passage_id              UUID REFERENCES question_service.passage(id),
    passage_order_index     INTEGER,
    subject_id              BIGINT NOT NULL,
    topic_id                BIGINT NOT NULL,
    subtopic_id             BIGINT NOT NULL,
    chapter                 VARCHAR(200),
    difficulty              VARCHAR(20) NOT NULL,
    cognitive_level         VARCHAR(20) NOT NULL,
    question_type           VARCHAR(30) NOT NULL,
    content                 TEXT,
    options                 JSONB,
    answer_key              TEXT,
    explanation             TEXT,
    source_references       TEXT,
    embedding               halfvec(384),
    benchmark_time_seconds  INTEGER NOT NULL DEFAULT 90,
    has_images              BOOLEAN NOT NULL DEFAULT FALSE,
    state                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    encryption_key_id       VARCHAR(255),
    usage_count             INTEGER NOT NULL DEFAULT 0,
    last_used_at            TIMESTAMP,
    used_in_shift_ids_json  JSONB,
    author_id               UUID NOT NULL,
    reviewer_id             UUID,
    review_comments         TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, subject_id)
) PARTITION BY HASH (subject_id);

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
CREATE INDEX idx_question_passage_id ON question_service.question(passage_id);
CREATE INDEX idx_question_subject_id ON question_service.question(subject_id);
CREATE INDEX idx_question_topic_id ON question_service.question(topic_id);
CREATE INDEX idx_question_subtopic_id ON question_service.question(subtopic_id);
CREATE INDEX idx_question_difficulty ON question_service.question(difficulty);
CREATE INDEX idx_question_state ON question_service.question(state);
CREATE INDEX idx_question_author_id ON question_service.question(author_id);
CREATE INDEX idx_question_type ON question_service.question(question_type);

CREATE INDEX IF NOT EXISTS idx_question_has_images
    ON question_service.question(has_images)
    WHERE has_images = TRUE;

COMMENT ON COLUMN question_service.question.has_images
    IS 'Set TRUE when content, explanation, or any option contains image/SVG media.';

COMMENT ON COLUMN question_service.question.review_comments
    IS 'Reviewer feedback comments set when a question is rejected (REVIEW -> DRAFT). '
       'Visible to the author for revision guidance.';

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
-- Table: batch_generation_job
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.batch_generation_job (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              VARCHAR(100)  NOT NULL,
    status                 VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    items                  JSONB         NOT NULL DEFAULT '[]'::jsonb,
    total_requested        INTEGER       NOT NULL,
    total_generated        INTEGER       NOT NULL DEFAULT 0,
    total_failed           INTEGER       NOT NULL DEFAULT 0,
    total_duplicates       INTEGER       NOT NULL DEFAULT 0,
    model_used             VARCHAR(100),
    avoid_duplicates       BOOLEAN       NOT NULL DEFAULT TRUE,
    initiated_by           UUID          NOT NULL,
    started_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    error_message          TEXT,
    bedrock_job_arn        VARCHAR(500),
    generated_question_ids JSONB         DEFAULT '[]'::jsonb,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_batch_job_tenant_created
    ON question_service.batch_generation_job (tenant_id, created_at DESC);

CREATE INDEX idx_batch_job_status
    ON question_service.batch_generation_job (status, created_at ASC);

CREATE INDEX idx_batch_job_user
    ON question_service.batch_generation_job (initiated_by, tenant_id, created_at DESC);

-- ============================================================
-- Table: translation
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.translation (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    question_id         UUID,
    passage_id          UUID REFERENCES question_service.passage(id),
    language_code       VARCHAR(10) NOT NULL,
    translated_payload  TEXT,
    payload_encrypted   BOOLEAN NOT NULL DEFAULT FALSE,
    source_version      BIGINT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL,
    translator_id       UUID NOT NULL,
    reviewer_id         UUID,
    review_comments     TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_translation_question_lang_tenant
        UNIQUE (question_id, language_code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_translation_tenant_id
    ON question_service.translation (tenant_id);
CREATE INDEX IF NOT EXISTS idx_translation_question_id
    ON question_service.translation (question_id);
CREATE INDEX IF NOT EXISTS idx_translation_passage_id
    ON question_service.translation (passage_id);
CREATE INDEX IF NOT EXISTS idx_translation_language_code
    ON question_service.translation (language_code);
CREATE INDEX IF NOT EXISTS idx_translation_status
    ON question_service.translation (status);

-- ============================================================
-- Table: batch_translation_job
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.batch_translation_job (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    paper_id                UUID,
    question_ids            JSONB         DEFAULT '[]'::jsonb,
    source_language         VARCHAR(10)   NOT NULL DEFAULT 'en',
    target_language         VARCHAR(10)   NOT NULL DEFAULT 'hi',
    target_status           VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    subject_filter          VARCHAR(100),
    overwrite_existing      BOOLEAN       NOT NULL DEFAULT TRUE,
    total_questions         INTEGER       NOT NULL DEFAULT 0,
    processed_questions     INTEGER       NOT NULL DEFAULT 0,
    successful_questions    INTEGER       NOT NULL DEFAULT 0,
    failed_questions        INTEGER       NOT NULL DEFAULT 0,
    failed_question_ids     JSONB         DEFAULT '[]'::jsonb,
    batch_size              INTEGER       NOT NULL DEFAULT 50,
    throttle_delay_ms       INTEGER       NOT NULL DEFAULT 50,
    max_concurrency         INTEGER       NOT NULL DEFAULT 2,
    initiated_by            UUID          NOT NULL,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                 BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_batch_trans_job_tenant_created
    ON question_service.batch_translation_job (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_batch_trans_job_status
    ON question_service.batch_translation_job (status, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_batch_trans_job_paper_id
    ON question_service.batch_translation_job (paper_id) WHERE paper_id IS NOT NULL;

-- ============================================================
-- Table: subtopic_micro_lesson (Micro-Lessons v2 - Multi-lesson per subtopic)
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.subtopic_micro_lesson (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(255),
    subject_id              BIGINT NOT NULL,
    topic_id                BIGINT NOT NULL,
    subtopic_id             BIGINT NOT NULL REFERENCES question_service.subtopic(id) ON DELETE CASCADE,
    lesson_type             VARCHAR(30) NOT NULL DEFAULT 'CONCEPT',
    difficulty_level        VARCHAR(20) NOT NULL DEFAULT 'CORE',
    sequence_no             INTEGER NOT NULL DEFAULT 1,
    prerequisite_lesson_id  UUID REFERENCES question_service.subtopic_micro_lesson(id),
    estimated_minutes       INTEGER NOT NULL DEFAULT 5,
    language                VARCHAR(10) NOT NULL DEFAULT 'en',
    core_concept            TEXT NOT NULL,
    trap                    TEXT NOT NULL,
    cheat_sheet             JSONB NOT NULL,
    step_by_step_breakdown  JSONB NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'AI_GENERATED',
    reviewed_by             UUID,
    reviewed_at             TIMESTAMP WITH TIME ZONE,
    review_comments         TEXT,
    quality_score           DOUBLE PRECISION,
    content_version         INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subtopic_lesson_variant_platform ON question_service.subtopic_micro_lesson
    (subtopic_id, lesson_type, difficulty_level, sequence_no, content_version)
    WHERE tenant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subtopic_lesson_variant_tenant ON question_service.subtopic_micro_lesson
    (tenant_id, subtopic_id, lesson_type, difficulty_level, sequence_no, content_version)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_micro_lesson_lookup ON question_service.subtopic_micro_lesson(subtopic_id, status);
CREATE INDEX IF NOT EXISTS idx_micro_lesson_review ON question_service.subtopic_micro_lesson(status, subject_id);

-- ============================================================
-- Table: lesson_effectiveness
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.lesson_effectiveness (
    lesson_id                   UUID PRIMARY KEY REFERENCES question_service.subtopic_micro_lesson(id) ON DELETE CASCADE,
    views                       BIGINT NOT NULL DEFAULT 0,
    completions                 BIGINT NOT NULL DEFAULT 0,
    post_lesson_accuracy_delta  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Table: microlesson_translation
-- ============================================================
CREATE TABLE IF NOT EXISTS question_service.microlesson_translation (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    microlesson_id              UUID NOT NULL REFERENCES question_service.subtopic_micro_lesson(id) ON DELETE CASCADE,
    tenant_id                   VARCHAR(100) NOT NULL DEFAULT 'default',
    language_code               VARCHAR(10)  NOT NULL,
    core_concept                TEXT         NOT NULL,
    trap                        TEXT         NOT NULL,
    cheat_sheet                 JSONB        NOT NULL,
    step_by_step_breakdown      JSONB        NOT NULL,
    source_version              INTEGER      NOT NULL DEFAULT 1,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    translator_id               UUID,
    reviewer_id                 UUID,
    review_comments             TEXT,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_microlesson_trans_lang_tenant UNIQUE (microlesson_id, language_code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_microlesson_trans_lesson_id ON question_service.microlesson_translation(microlesson_id);
CREATE INDEX IF NOT EXISTS idx_microlesson_trans_lang_status ON question_service.microlesson_translation(microlesson_id, language_code, status);

CREATE TABLE IF NOT EXISTS question_service.microlesson_batch_translation_job (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL DEFAULT 'default',
    status                  VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    source_language         VARCHAR(10)   NOT NULL DEFAULT 'en',
    target_language         VARCHAR(10)   NOT NULL DEFAULT 'hi',
    target_status           VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    subject_filter          VARCHAR(100),
    topic_filter            VARCHAR(200),
    target_exam_filter      VARCHAR(50),
    lesson_ids              JSONB         DEFAULT '[]'::jsonb,
    overwrite_existing      BOOLEAN       NOT NULL DEFAULT TRUE,
    total_lessons           INTEGER       NOT NULL DEFAULT 0,
    processed_lessons       INTEGER       NOT NULL DEFAULT 0,
    successful_lessons      INTEGER       NOT NULL DEFAULT 0,
    failed_lessons          INTEGER       NOT NULL DEFAULT 0,
    failed_lesson_ids       JSONB         DEFAULT '[]'::jsonb,
    batch_size              INTEGER       NOT NULL DEFAULT 20,
    throttle_delay_ms       INTEGER       NOT NULL DEFAULT 50,
    max_concurrency         INTEGER       NOT NULL DEFAULT 2,
    initiated_by            UUID          NOT NULL,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                 BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ml_batch_trans_job_tenant_created ON question_service.microlesson_batch_translation_job (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_batch_trans_job_status ON question_service.microlesson_batch_translation_job (status, created_at ASC);

-- ============================================================
-- Canonical Platform Taxonomy Seeds (tenant_id IS NULL)
-- ============================================================

-- 1. Canonical Subjects
INSERT INTO question_service.subject (tenant_id, code, name, description, display_order, active) VALUES
(NULL, 'MATH', 'Mathematics', 'Mathematics including arithmetic, algebra, geometry, mensuration, trigonometry, statistics and probability', 1, TRUE),
(NULL, 'REAS', 'Reasoning', 'General intelligence and reasoning covering verbal, non-verbal, analytical and logical reasoning', 2, TRUE),
(NULL, 'GS', 'General Studies', 'General Studies and General Awareness covering history, geography, polity, economy, science and current affairs', 3, TRUE),
(NULL, 'ENG', 'English', 'English language and comprehension covering vocabulary, grammar, sentence structure, error detection, cloze passages and comprehension passages', 4, TRUE),
(NULL, 'CS', 'Computer Science', 'Computer science covering computer basics, hardware, software, networking, databases, programming and cyber security', 5, TRUE),
(NULL, 'GSCI', 'General Science', 'General science covering physics, chemistry and biology', 6, TRUE),
(NULL, 'STATS', 'Statistics', 'Statistics covering collection and presentation of data, measures of central tendency and dispersion, probability, sampling, inference and index numbers', 7, TRUE),
(NULL, 'BFA', 'Banking and Financial Awareness', 'Banking principles, financial systems, RBI regulations, monetary policy, financial inclusion, digital banking, and government economic schemes', 8, TRUE),
(NULL, 'DILA', 'Data Interpretation and Logical Analysis', 'Data interpretation across tabular data, line and bar charts, pie charts, caselets, radar charts, data sufficiency, and analytical reasoning', 9, TRUE),
(NULL, 'HND', 'General Hindi', 'General Hindi covering grammar, sandhi, samas, synonyms, antonyms, idioms, proverbs, error detection, sentence correction, and comprehension passages', 10, TRUE),
(NULL, 'PAG', 'Public Administration and Governance', 'Administrative theories, Indian administrative structure, constitutional and statutory bodies, public policy, e-governance, and civil services ethics', 11, TRUE),
(NULL, 'EEB', 'Environmental Ecology and Biodiversity', 'Ecology, biodiversity conservation, climate change, international conventions, wildlife protection, and pollution management', 12, TRUE),
(NULL, 'CDP', 'Child Development and Pedagogy', 'Child development, learning theories, inclusive education, pedagogy of school subjects, curriculum design, continuous and comprehensive evaluation (CCE)', 13, TRUE)
ON CONFLICT DO NOTHING;

-- 2. Canonical Topics
INSERT INTO question_service.topic (tenant_id, subject_id, code, name, description, display_order, active)
SELECT NULL, s.id, v.code, v.name, v.description, v.display_order, TRUE
FROM (VALUES
    ('MATH', 'NUM_SYSTEMS', 'Number Systems', 'Number systems, properties, divisibility, decimals, fractions and factors', 1),
    ('MATH', 'ARITH_OPS', 'Fundamental Arithmetical Operations', 'Arithmetic operations, percentages, ratio, profit/loss, interest and time/distance', 2),
    ('MATH', 'ALGEBRA', 'Algebra', 'Algebraic expressions, identities, linear and quadratic equations and surds', 3),
    ('MATH', 'GEOMETRY', 'Geometry', 'Geometric shapes, properties, triangles, circles, chords, tangents and measurements', 4),
    ('MATH', 'MENSURATION', 'Mensuration', '2D and 3D perimeter, area, volume, prisms, pyramids, cylinders, cones and spheres', 5),
    ('MATH', 'TRIGONOMETRY', 'Trigonometry', 'Trigonometric ratios, identities, heights and distances', 6),
    ('MATH', 'STATS_PROB', 'Statistics and Probability', 'Central tendency, dispersion, frequency distributions, probability distributions', 7),
    ('MATH', 'ARITHMETIC', 'Arithmetic', 'Basic arithmetic operations, percentages, ratios, averages and profit/loss', 8),
    ('MATH', 'STATISTICS', 'Statistics', 'Statistical measures, mean, median, mode and data analysis', 9),
    ('REAS', 'ANALOGY', 'Analogy', 'Semantic, symbolic, number and figural analogies', 1),
    ('REAS', 'CLASSIFICATION', 'Classification', 'Semantic, symbolic, number and figural classification', 2),
    ('REAS', 'SERIES', 'Series', 'Semantic, number, letter and figural series', 3),
    ('REAS', 'PROBLEM_SOLVING', 'Problem Solving', 'Problem solving, puzzles, direction sense, blood relations and arrangements', 4),
    ('REAS', 'CODING_DECODING', 'Coding and Decoding', 'Coding and decoding by letter shifts, numbers and substitution', 5),
    ('REAS', 'NUMERICAL_OPS', 'Numerical Operations', 'Mathematical operators substitution and arithmetic equations', 6),
    ('REAS', 'SYMBOLIC_OPS', 'Symbolic Operations', 'Symbolic operations and mathematical inequalities', 7),
    ('REAS', 'TRENDS', 'Trends', 'Figural and symbolic trends', 8),
    ('REAS', 'SPACE_ORIENTATION', 'Space Orientation', 'Spatial orientation and direction sense', 9),
    ('REAS', 'SPACE_VISUALIZATION', 'Space Visualization', 'Spatial visualization, cube and dice folding', 10),
    ('REAS', 'VENN_DIAGRAMS', 'Venn Diagrams', 'Logical Venn diagrams and class relationships', 11),
    ('REAS', 'DRAWING_INFERENCES', 'Drawing Inferences', 'Drawing inferences and logical conclusions', 12),
    ('REAS', 'PATTERN_FOLDING', 'Pattern Folding and Unfolding', 'Punched hole pattern folding and unfolding', 13),
    ('REAS', 'FIGURAL_PATTERN', 'Figural Pattern Folding and Completion', 'Figural pattern completion and folding', 14),
    ('REAS', 'INDEXING', 'Indexing', 'Alphabetical and numerical indexing', 15),
    ('REAS', 'ADDRESS_MATCHING', 'Address Matching', 'Address and data verification', 16),
    ('REAS', 'DATE_CITY_MATCHING', 'Date and City Matching', 'Date and city matching verification', 17),
    ('REAS', 'EMBEDDED_FIGURES', 'Embedded Figures', 'Hidden and embedded figures', 18),
    ('REAS', 'CRITICAL_THINKING', 'Critical Thinking', 'Critical thinking, statement and argument, cause and effect', 19),
    ('REAS', 'EMOTIONAL_INTELLIGENCE', 'Emotional Intelligence', 'Emotional intelligence and social awareness', 20),
    ('REAS', 'SOCIAL_INTELLIGENCE', 'Social Intelligence', 'Social intelligence and ethical dilemmas', 21),
    ('REAS', 'WORD_BUILDING', 'Word Building', 'Word formation and dictionary order', 22),
    ('REAS', 'STATEMENT_CONCLUSION', 'Statement and Conclusion', 'Deductive conclusions derived strictly from given statements', 23),
    ('REAS', 'SYLLOGISTIC_REASONING', 'Syllogistic Reasoning', 'Two and three premise categorical syllogisms', 24),
    ('REAS', 'LOGICAL_REASONING', 'Logical Reasoning', 'Logic-based problem solving and deductive reasoning', 25),
    ('REAS', 'ANALYTICAL_REASONING', 'Analytical Reasoning', 'Complex analytical puzzles and matrix arrangements', 26),
    ('REAS', 'VERBAL_REASONING', 'Verbal Reasoning', 'Verbal logic, classification and inferences', 27),
    ('REAS', 'NON_VERBAL_REASONING', 'Non-Verbal Reasoning', 'Mirror images, paper folding, pattern completion and diagrammatic series', 28),
    ('REAS', 'SEATING_ARRANGEMENT', 'Linear Seating Arrangement', 'Unidirectional and bidirectional linear seating arrangements', 29),
    ('REAS', 'MACHINE_INPUT_OUTPUT', 'Machine Input-Output', 'Step-by-step rearrangement of word and number sequences', 30),
    ('REAS', 'INEQUALITIES', 'Inequalities', 'Direct and coded inequalities in deductive reasoning', 31),
    ('REAS', 'PUZZLES_CRITICAL', 'Puzzles & Critical Reasoning', 'Complex floor, box, flat puzzles and critical reasoning deductions', 32),
    ('REAS', 'NUMBER_SERIES_REAS', 'Number Series', 'Missing and wrong number series patterns', 33),
    ('REAS', 'SYLLOGISM', 'Syllogism', 'Standard, only-a-few, and negative deduction syllogisms', 34),
    ('GS', 'CURRENT_EVENTS', 'Current Events', 'National and international current affairs, summits, sports, awards', 1),
    ('GS', 'HISTORY', 'History', 'Ancient, medieval and modern history', 2),
    ('GS', 'CULTURE', 'Culture', 'Indian culture, classical dances, music, art and architecture', 3),
    ('GS', 'GEOGRAPHY', 'Geography', 'Physical, economic and human geography, climate and rivers', 4),
    ('GS', 'ECONOMIC_SCENE', 'Economic Scene', 'Indian economy, fiscal policy, inflation, banking and trade', 5),
    ('GS', 'GENERAL_POLICY', 'General Policy', 'Indian constitution, fundamental rights, governance and public policy', 6),
    ('GS', 'SCIENTIFIC_RESEARCH', 'Scientific Research', 'Scientific developments, ISRO, DRDO and space exploration', 7),
    ('GS', 'EVERYDAY_SCIENCE', 'Everyday Science', 'Scientific principles in everyday physics, chemistry and biology', 8),
    ('GS', 'INDIA_NEIGHBOURS', 'India and Neighbouring Countries', 'Borders, treaties, capitals, bilateral relations and regional forums', 9),
    ('GS', 'INDIAN_HISTORY', 'Indian History', 'History of India from ancient to modern times and freedom struggle', 10),
    ('GS', 'INDIAN_GEOGRAPHY', 'Indian Geography', 'Physical, economic and human geography of India, drainage and climate', 11),
    ('GS', 'INDIAN_POLITY', 'Indian Polity', 'Indian constitution, governance, executive, legislature, judiciary and political system', 12),
    ('GS', 'INDIAN_ECONOMY', 'Indian Economy', 'Economic planning, NITI Aayog, banking, budget and taxation', 13),
    ('GS', 'SCIENCE_TECH', 'Science & Technology', 'Scientific developments, biotechnology, nanotechnology and IT', 14),
    ('GS', 'ENVIRONMENT', 'Environment', 'Environmental science, ecosystems, biodiversity, climate change and conservation', 15),
    ('GS', 'CURRENT_AFFAIRS', 'Current Affairs', 'National and international events of significance', 16),
    ('GS', 'INDIAN_ART_CULTURE', 'Indian Art & Culture', 'Classical dance, music, temple architecture, literature, festivals and heritage', 17),
    ('GS', 'INTL_RELATIONS', 'International Relations & Global Bodies', 'United Nations, G20, BRICS, SCO, ASEAN, WTO, IMF, World Bank and bilateral ties', 18),
    ('GS', 'INTERNAL_SECURITY', 'Internal Security & Disaster Management', 'Border management, cyber warfare, money laundering, NDMA guidelines and disaster mitigation', 19),
    ('GS', 'ETHICS_INTEGRITY', 'Ethics, Integrity & Aptitude', 'Moral philosophy, public service values, emotional intelligence, attitude and administrative ethics', 20),
    ('GS', 'PHYSICAL_GEOGRAPHY', 'Physical Geography', 'Topographical contours, plate tectonics, geomorphology and earth structure', 21),
    ('ENG', 'VOCABULARY', 'Vocabulary', 'Word knowledge, usage, contextual meanings, synonyms and antonyms', 1),
    ('ENG', 'GRAMMAR', 'Grammar', 'English grammar rules, syntax, parts of speech and usage', 2),
    ('ENG', 'SENTENCE_STRUCTURE', 'Sentence Structure', 'Sentence structure, ordering, synthesis and parallelism', 3),
    ('ENG', 'SPOT_ERROR', 'Spot the Error', 'Grammar, usage, tense, preposition and agreement errors', 4),
    ('ENG', 'FILL_BLANKS', 'Fill in the Blanks', 'Single, double, prepositional and phrasal fillers', 5),
    ('ENG', 'SYNONYMS_HOMONYMS', 'Synonyms and Homonyms', 'Synonyms and homonyms disambiguation', 6),
    ('ENG', 'ANTONYMS', 'Antonyms', 'Antonyms and opposite contextual meanings', 7),
    ('ENG', 'SPELLINGS', 'Spellings', 'Spelling rules, detecting misspelled words and correct forms', 8),
    ('ENG', 'IDIOMS_PHRASES', 'Idioms and Phrases', 'Idiomatic expressions, phrasal verbs and contextual usage', 9),
    ('ENG', 'ONE_WORD_SUB', 'One Word Substitution', 'Single word substitutes for descriptive phrases and concepts', 10),
    ('ENG', 'SENTENCE_IMPROVEMENT', 'Improvement of Sentences', 'Sentence improvement, clause correction and phrase replacement', 11),
    ('ENG', 'VOICE_ACTIVE_PASSIVE', 'Active and Passive Voice', 'Rules and conversion between active and passive voice', 12),
    ('ENG', 'NARRATION_DIRECT_INDIRECT', 'Direct and Indirect Narration', 'Reported speech rules and direct-to-indirect conversions', 13),
    ('ENG', 'SENTENCE_SHUFFLING', 'Sentence Shuffling', 'Para-jumbles and sentence rearrangement for coherence', 14),
    ('ENG', 'CLOZE_PASSAGE', 'Cloze Passage', 'Cloze passages testing vocabulary and grammar in context', 15),
    ('ENG', 'COMPREHENSION_PASSAGE', 'Comprehension Passage', 'Reading comprehension across literary, current affairs and technical passages', 16),
    ('ENG', 'COMPREHENSION', 'Comprehension', 'Reading comprehension, factual recall and inferential analysis', 17),
    ('ENG', 'ERROR_DETECTION', 'Error Detection and Spotting', 'Advanced error detection in complex sentences', 18),
    ('ENG', 'IDIOMS_VOCAB', 'Idioms, Phrasal Verbs & Vocabulary', 'Financial, legal and formal idioms and phrasal expressions', 19),
    ('ENG', 'PARA_JUMBLES', 'Para Jumbles & Sentence Rearrangement', 'Coherent paragraph construction from shuffled sentences', 20),
    ('ENG', 'READING_COMP', 'Reading Comprehension', 'Financial, economic and analytical reading comprehension', 21),
    ('ENG', 'SENTENCE_CONNECTORS', 'Sentence Connectors and Starters', 'Discourse connectors, conjunctions and sentence starters', 22),
    ('ENG', 'SENTENCE_IMPROVE_CORR', 'Sentence Improvement and Correction', 'Grammatical correction and phrase replacement in banking/SSC contexts', 23),
    ('ENG', 'WORD_SWAP', 'Word Swap & Sentence Reordering', 'Word swap within sentences to restore semantic coherence', 24),
    ('ENG', 'MATCH_COLUMNS', 'Match the Columns', 'Connecting clause fragments and sentence halves across two columns', 25),
    ('GSCI', 'PHYSICS', 'Physics', 'Mechanics, optics, thermodynamics, electricity, magnetism and waves', 1),
    ('GSCI', 'CHEMISTRY', 'Chemistry', 'Physical, inorganic, organic chemistry, periodic table, acids and bases', 2),
    ('GSCI', 'BIOLOGY', 'Biology', 'Cell biology, human physiology, genetics, plant physiology and diseases', 3),
    ('CS', 'COMPUTER_BASICS', 'Computer Basics', 'Computer organization, CPU, input/output devices and memory', 1),
    ('CS', 'SOFTWARE', 'Software', 'Operating systems, Microsoft Office and application software', 2),
    ('CS', 'INTERNET_EMAIL', 'Internet and E-mail', 'Web browsing, searching, downloading, email and e-banking', 3),
    ('CS', 'NETWORKING_CYBER_SECURITY', 'Networking and Cyber Security', 'Network devices, protocols, malware, viruses and security measures', 4),
    ('CS', 'PROGRAMMING', 'Programming', 'Programming paradigms, data structures and algorithms', 5),
    ('CS', 'DATABASES', 'Databases', 'Relational databases, SQL queries and normalization', 6),
    ('CS', 'NETWORKING', 'Networking', 'Networking models, topologies and communications', 7),
    ('STATS', 'DATA_COLLECTION_PRESENTATION', 'Collection, Classification and Presentation of Statistical Data', 'Primary/secondary data, classification, tabulation, graphs and charts', 1),
    ('STATS', 'CENTRAL_TENDENCY', 'Measures of Central Tendency', 'Arithmetic mean, median, mode, geometric mean and harmonic mean', 2),
    ('STATS', 'DISPERSION', 'Measures of Dispersion', 'Range, quartile deviation, mean deviation, variance and standard deviation', 3),
    ('STATS', 'MOMENTS_SKEWNESS', 'Moments, Skewness and Kurtosis', 'Central moments, Karl Pearson and Bowley skewness, kurtosis', 4),
    ('STATS', 'CORRELATION_REGRESSION', 'Correlation and Regression', 'Scatter diagram, Pearson correlation, Spearman rank correlation, regression lines', 5),
    ('STATS', 'PROBABILITY_THEORY', 'Probability Theory', 'Classical/axiomatic probability, conditional probability, Bayes theorem', 6),
    ('STATS', 'PROBABILITY_DISTRIBUTIONS', 'Random Variable and Probability Distributions', 'Binomial, Poisson, normal, exponential distributions and expectations', 7),
    ('STATS', 'SAMPLING_THEORY', 'Sampling Theory', 'Population, sample, sampling errors, random and stratified sampling', 8),
    ('STATS', 'STATISTICAL_INFERENCE', 'Statistical Inference', 'Point/interval estimation, hypothesis testing, z-test, t-test, chi-square, ANOVA', 9),
    ('STATS', 'ANOVA', 'Analysis of Variance', 'One-way and two-way analysis of variance', 10),
    ('STATS', 'TIME_SERIES', 'Time Series Analysis', 'Components of time series, trend, seasonal variation and cyclical movements', 11),
    ('STATS', 'INDEX_NUMBERS', 'Index Numbers', 'Laspeyres, Paasche, Fisher index numbers, cost of living index', 12),
    ('BFA', 'BFA_INDIAN_BANKING', 'Indian Banking System & RBI Functions', 'Structure of commercial/cooperative banks, RBI constitution, currency management', 1),
    ('BFA', 'BFA_MONETARY_POLICY', 'Monetary Policy & Inflation Dynamics', 'Repo rate, reverse repo, CRR, SLR, policy transmission, CPI/WPI dynamics', 2),
    ('BFA', 'BFA_FINANCIAL_MARKETS', 'Financial Markets & Capital Instruments', 'Money market, capital market, T-bills, CPs, bonds, SEBI regulations', 3),
    ('BFA', 'BFA_DIGITAL_BANKING', 'Digital Banking & Payment Systems', 'NPCI, UPI, RTGS, NEFT, IMPS, CBDC / Digital Rupee, cybersecurity in payments', 4),
    ('BFA', 'BFA_GOVT_SCHEMES', 'Government Financial Schemes & Inclusions', 'PM Jan Dhan, PMJJBY, PMSBY, APY, Mudra, Stand-Up India, PSL norms', 5),
    ('BFA', 'BFA_NPA_MANAGEMENT', 'Non-Performing Assets & Risk Management', 'NPA classification, SARFAESI, IBC, Basel III norms, PCA framework', 6),
    ('BFA', 'BFA_MONEY_CAPITAL', 'Money Market & Capital Market Instruments', 'Treasury bills, commercial papers, certificates of deposit and capital markets', 7),
    ('BFA', 'BFA_NPA_RESOLUTION', 'NPA Management & Resolution Framework', 'Stressed asset resolution, SARFAESI, IBC and DRT processes', 8),
    ('BFA', 'BFA_NEGOTIABLE_INST', 'Negotiable Instruments & Banking Regulations', 'Cheques, promissory notes, bills of exchange, NI Act 1881 and Ombudsman', 9),
    ('BFA', 'BFA_PSL_CREDIT', 'Priority Sector Lending & Agriculture Credit', 'PSL targets, sub-targets, RIDF and agricultural credit delivery', 10),
    ('DILA', 'DILA_TABULAR', 'Tabular Data Interpretation', 'Multi-variable and missing data tables, percentages, ratios and growth trends', 1),
    ('DILA', 'DILA_BAR_GRAPHS', 'Bar Graphs and Histograms', 'Simple, grouped, stacked bar diagrams and histograms', 2),
    ('DILA', 'DILA_LINE_GRAPHS', 'Line Graphs and Radar Charts', 'Time series line plots, comparative lines and polygonal radar charts', 3),
    ('DILA', 'DILA_PIE_CHARTS', 'Pie Charts and Mixed Graphs', 'Single/dual pie charts, degree-percentage conversions, mixed table-graph sets', 4),
    ('DILA', 'DILA_CASELET_ARITH', 'Caselet and Arithmetic DI', 'Paragraph-based DI, Venn diagram caselets, profit-loss and SI/CI caselets', 5),
    ('DILA', 'DILA_DATA_SUFFICIENCY', 'Data Sufficiency', 'Determining sufficiency of statements to answer algebraic/arithmetic queries', 6),
    ('DILA', 'DILA_QUADRATIC', 'Quadratic Equations & Inequalities', 'Quadratic comparisons, root relationships and algebraic inequalities', 7),
    ('HND', 'HND_VARN_VICHAR', 'वर्ण विचार एवं वर्तनी शुद्धिकरण', 'स्वर, व्यंजन, उच्चारण स्थान और वर्तनी शुद्धि', 1),
    ('HND', 'HND_SANDHI', 'संधि एवं संधि विच्छेद', 'स्वर, व्यंजन और विसर्ग संधि के नियम एवं विच्छेद', 2),
    ('HND', 'HND_SAMAS', 'समास एवं समास विग्रह', 'तत्पुरुष, कर्मधारय, द्विगु, द्वन्द्व, बहुव्रीहि एवं अव्ययीभाव समास', 3),
    ('HND', 'HND_VILOM_PARYAY', 'विलोम एवं पर्यायवाची शब्द', 'समानार्थी एवं विपरीतार्थक शब्द और उनका वाक्यों में प्रयोग', 4),
    ('HND', 'HND_MUHAVARE', 'मुहावरे एवं लोकोक्तियां', 'प्रचलित मुहावरे, कहावतें और संदर्भानुसार प्रयोग', 5),
    ('HND', 'HND_ONE_WORD', 'अनेक शब्दों के लिए एक शब्द', 'संक्षिप्त अभिव्यक्ति एवं एकल शब्द चयन', 6),
    ('HND', 'HND_VAKYA_SHUDDHI', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'लिंग, वचन, कारक, काल और पदक्रम संबंधी त्रुटि सुधार', 7),
    ('HND', 'HND_GADHYANSH', 'अपठित गद्यांश', 'गद्यांश आधारित बोधगम्यता, शीर्षक चयन एवं व्याख्या', 8),
    ('PAG', 'PAG_THEORIES', 'Administrative Theories & Evolution', 'Scientific management, bureaucracy, human relations, NPM, good governance', 1),
    ('PAG', 'PAG_MACHINERY', 'Union and State Administrative Machinery', 'Cabinet Secretariat, PMO, Ministries, Chief Secretariat, District Administration', 2),
    ('PAG', 'PAG_LOCAL_GOVT', 'Panchayati Raj & Local Self-Government', '73rd and 74th Amendments, PESA Act, urban and rural local bodies', 3),
    ('PAG', 'PAG_POLICY', 'Public Policy & Citizen Centricity', 'Policy formulation, Citizen Charters, RTI Act, Lokpal and Lokayukta', 4),
    ('PAG', 'PAG_EGOV_DPI', 'E-Governance & Digital Public Infrastructure', 'Digital India, direct benefit transfer (DBT), UMANG, DigiLocker and DPI architecture', 5),
    ('EEB', 'EEB_ECOSYSTEM', 'Ecosystem Dynamics & Biomes', 'Food chains, trophic levels, energy flow, ecological pyramids and biogeochemical cycles', 1),
    ('EEB', 'EEB_BIODIVERSITY', 'Biodiversity & Wildlife Conservation', 'Protected Area Network, National Parks, Sanctuaries, Biosphere Reserves, Ramsar sites', 2),
    ('EEB', 'EEB_CLIMATE', 'Climate Change & Global Agreements', 'UNFCCC, Kyoto Protocol, Paris Agreement, COP summits and carbon markets', 3),
    ('EEB', 'EEB_POLLUTION', 'Environmental Pollution & Waste Management', 'Air, water, soil, plastic and e-waste rules, NGT frameworks', 4),
    ('CDP', 'CDP_DEVELOPMENT', 'Child Development & Learning Principles', 'Piaget, Vygotsky, Kohlberg theories of cognitive and moral development', 1),
    ('CDP', 'CDP_INCLUSIVE', 'Inclusive Education & Special Needs', 'Addressing diverse learners, learning disabilities (Dyslexia, ADHD) and gifted children', 2),
    ('CDP', 'CDP_STRATEGIES', 'Teaching-Learning Strategies & Pedagogy', 'Constructivist approach, problem solving, inquiry learning and motivation', 3),
    ('CDP', 'CDP_ASSESSMENT', 'Assessment, Evaluation & CCE', 'Formative/summative assessment, Continuous and Comprehensive Evaluation, rubrics', 4)
) AS v(subject_code, code, name, description, display_order)
JOIN question_service.subject s ON s.code = v.subject_code AND s.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- 3. Canonical Subtopics
INSERT INTO question_service.subtopic (tenant_id, topic_id, code, name, description, display_order, active)
SELECT NULL, t.id, v.code, v.name, v.description, v.display_order, TRUE
FROM (VALUES
    ('NUM_SYSTEMS', 'Whole Numbers', 'NUM_WHOLE', 'Whole numbers and integers', 1),
    ('NUM_SYSTEMS', 'Decimals', 'NUM_DECIMALS', 'Decimal representations and recurring decimals', 2),
    ('NUM_SYSTEMS', 'Fractions', 'NUM_FRACTIONS', 'Fractions, vulgar fractions and operations', 3),
    ('NUM_SYSTEMS', 'Relationship Between Numbers', 'NUM_RELATIONSHIPS', 'Divisibility, factors, multiples and modular arithmetic', 4),
    ('NUM_SYSTEMS', 'Divisibility and Factors', 'NUM_DIVISIBILITY', 'LCM, HCF and prime factorization', 5),
    ('ARITH_OPS', 'Percentages', 'ARITH_PERCENTAGES', 'Percentage calculations and rate of change', 1),
    ('ARITH_OPS', 'Ratio and Proportion', 'ARITH_RATIO_PROP', 'Ratios, direct/inverse proportion and variation', 2),
    ('ARITH_OPS', 'Square Roots', 'ARITH_SQ_ROOTS', 'Square roots, cube roots and powers', 3),
    ('ARITH_OPS', 'Averages', 'ARITH_AVERAGES', 'Simple and weighted averages', 4),
    ('ARITH_OPS', 'Simple Interest', 'ARITH_SI', 'Simple interest formulas and calculations', 5),
    ('ARITH_OPS', 'Compound Interest', 'ARITH_CI', 'Compound interest, compounding frequency and growth', 6),
    ('ARITH_OPS', 'Profit and Loss', 'ARITH_PROFIT_LOSS', 'Cost price, selling price, marked price and profit/loss margins', 7),
    ('ARITH_OPS', 'Discount', 'ARITH_DISCOUNT', 'Trade discount, cash discount and successive discounts', 8),
    ('ARITH_OPS', 'Partnership Business', 'ARITH_PARTNERSHIP', 'Capital investments, time periods and profit sharing', 9),
    ('ARITH_OPS', 'Mixture and Alligation', 'ARITH_MIXTURE', 'Rule of alligation and concentration mixtures', 10),
    ('ARITH_OPS', 'Time and Distance', 'ARITH_TIME_DIST', 'Speed, distance, relative velocity and train/boat problems', 11),
    ('ARITH_OPS', 'Time and Work', 'ARITH_TIME_WORK', 'Work rates, efficiency, pipes and cisterns', 12),
    ('ALGEBRA', 'Algebraic Identities', 'ALG_IDENTITIES', 'Standard identities and polynomial factorizations', 1),
    ('ALGEBRA', 'Elementary Surds', 'ALG_SURDS', 'Radicals, surds, rationalization and indices', 2),
    ('ALGEBRA', 'Graphs of Linear Equations', 'ALG_LINEAR_EQS', 'Coordinate representations and linear slopes', 3),
    ('ALGEBRA', 'Linear Equations', 'ALG_LINEAR', 'Single and simultaneous linear systems', 4),
    ('ALGEBRA', 'Quadratic Equations', 'ALG_QUADRATIC', 'Roots, discriminant and nature of quadratic roots', 5),
    ('ALGEBRA', 'Polynomials', 'ALG_POLYNOMIALS', 'Polynomial division, remainder theorem and zeroes', 6),
    ('GEOMETRY', 'Triangles', 'GEOM_TRIANGLES', 'Properties of triangles, angles and bisectors', 1),
    ('GEOMETRY', 'Congruence and Similarity', 'GEOM_CONGRUENCE', 'Criteria for congruence and similarity of triangles', 2),
    ('GEOMETRY', 'Circles', 'GEOM_CIRCLES', 'Circle geometry, arcs, sectors and cyclic quadrilaterals', 3),
    ('GEOMETRY', 'Chords and Tangents', 'GEOM_CHORDS_TANGENTS', 'Secants, tangents and chord intersection theorems', 4),
    ('GEOMETRY', 'Right Triangles', 'GEOM_RIGHT_TRIANGLES', 'Pythagorean theorem and altitude to hypotenuse', 5),
    ('GEOMETRY', 'Coordinate Geometry', 'GEOM_COORDINATE', 'Cartesian coordinates, distance formula and section formula', 6),
    ('MENSURATION', 'Triangle', 'MENS_TRIANGLE', 'Area of triangles and Heron formula', 1),
    ('MENSURATION', 'Quadrilaterals', 'MENS_QUADRILATERALS', 'Parallelograms, trapeziums, rhombuses and rectangles', 2),
    ('MENSURATION', 'Regular Polygons', 'MENS_POLYGONS', 'Interior/exterior angles and regular polygon areas', 3),
    ('MENSURATION', 'Circle', 'MENS_CIRCLE', 'Circumference, circular rings and area of circles', 4),
    ('MENSURATION', 'Right Prism', 'MENS_RIGHT_PRISM', 'Surface area and volume of right prisms', 5),
    ('MENSURATION', 'Right Circular Cone', 'MENS_CONE', 'Slant height, lateral area and cone volume', 6),
    ('MENSURATION', 'Right Circular Cylinder', 'MENS_CYLINDER', 'Curved surface area, total area and cylinder volume', 7),
    ('MENSURATION', 'Sphere', 'MENS_SPHERE', 'Surface area and volume of solid/hollow spheres', 8),
    ('MENSURATION', 'Hemisphere', 'MENS_HEMISPHERE', 'Hemisphere surface areas and capacity', 9),
    ('MENSURATION', 'Rectangular Parallelepiped', 'MENS_PARALLELEPIPED', 'Cuboids, cubes and diagonal calculations', 10),
    ('MENSURATION', 'Regular Right Pyramid', 'MENS_PYRAMID', 'Pyramid slant heights, lateral surfaces and volume', 11),
    ('MENSURATION', 'Perimeter and Area', 'MENS_PERIMETER_AREA', '2D geometric perimeters and combined surface areas', 12),
    ('MENSURATION', 'Surface Area and Volume', 'MENS_SURFACE_VOLUME', 'Composite 3D solids and shape transformations', 13),
    ('TRIGONOMETRY', 'Trigonometric Ratios', 'TRIG_RATIOS', 'Sine, cosine, tangent and reciprocal ratios', 1),
    ('TRIGONOMETRY', 'Complementary Angles', 'TRIG_COMPLEMENTARY', 'Ratios of complementary angles', 2),
    ('TRIGONOMETRY', 'Heights and Distances', 'TRIG_HEIGHTS_DIST', 'Angles of elevation, depression and distance calculations', 3),
    ('TRIGONOMETRY', 'Standard Identities', 'TRIG_IDENTITIES', 'Pythagorean trigonometric identities and angle transformations', 4),
    ('STATS_PROB', 'Histogram', 'SP_HISTOGRAM', 'Data distribution histograms and bins', 1),
    ('STATS_PROB', 'Frequency Polygon', 'SP_FREQ_POLYGON', 'Continuous frequency polygons', 2),
    ('STATS_PROB', 'Bar Diagram', 'SP_BAR_DIAGRAM', 'Simple, multiple and stacked bar charts', 3),
    ('STATS_PROB', 'Pie Chart', 'SP_PIE_CHART', 'Angular allocation and percentage sector charts', 4),
    ('STATS_PROB', 'Mean', 'SP_MEAN', 'Arithmetic, geometric and harmonic averages', 5),
    ('STATS_PROB', 'Median', 'SP_MEDIAN', 'Median calculation for grouped and ungrouped series', 6),
    ('STATS_PROB', 'Mode', 'SP_MODE', 'Modal value identification and empirical relationships', 7),
    ('STATS_PROB', 'Standard Deviation', 'SP_STD_DEV', 'Variance, standard deviation and coefficient of variation', 8),
    ('STATS_PROB', 'Simple Probability', 'SP_PROBABILITY', 'Event spaces, classical probability and odds', 9),
    ('STATS_PROB', 'Permutations and Combinations', 'SP_PERM_COMB', 'Factorials, arrangements and selections', 10),
    ('STATS_PROB', 'Bayes Theorem', 'SP_BAYES', 'Conditional probability, total probability and Bayes theorem', 11),
    ('STATS_PROB', 'Conditional Probability', 'SP_CONDITIONAL', 'Independent events and conditional dependencies', 12),
    ('ARITHMETIC', 'Basic Arithmetic Operations', 'ARITH_BASIC', 'Fundamental operations and numerical evaluation', 1),
    ('ARITHMETIC', 'BODMAS Rule Simplification', 'ARITH_SIMPLIFICATION', 'Order of operations, bracket removal and fractions', 2),
    ('ARITHMETIC', 'Approximation Techniques', 'ARITH_APPROX', 'Rounding, estimations and fast arithmetic methods', 3),
    ('ARITHMETIC', 'Number System', 'ARITH_NUM_SYS', 'Number classification and core numerical properties', 4),
    ('ARITHMETIC', 'Percentage', 'ARITH_PERCENT', 'Percentage change and base conversions', 5),
    ('STATISTICS', 'Statistical Measures and Analysis', 'STATS_MEASURES', 'Descriptive statistical analysis and distributions', 1),
    ('ANALOGY', 'Semantic Analogy', 'ANA_SEMANTIC', 'Word meaning relationships and semantic analogies', 1),
    ('ANALOGY', 'Symbolic / Number Analogy', 'ANA_SYMBOLIC', 'Mathematical pattern and number analogies', 2),
    ('ANALOGY', 'Figural Analogy', 'ANA_FIGURAL', 'Shape, rotation and geometric pattern analogies', 3),
    ('CLASSIFICATION', 'Semantic Classification', 'CLASS_SEMANTIC', 'Odd-one-out word categorization', 1),
    ('CLASSIFICATION', 'Symbolic / Number Classification', 'CLASS_SYMBOLIC', 'Numerical property odd-one-out', 2),
    ('CLASSIFICATION', 'Figural Classification', 'CLASS_FIGURAL', 'Visual pattern odd-one-out', 3),
    ('SERIES', 'Semantic Series', 'SER_SEMANTIC', 'Sequence of words and logical event progressions', 1),
    ('SERIES', 'Number Series', 'SER_NUMBER', 'Mathematical progression, arithmetic, geometric and Fibonacci series', 2),
    ('SERIES', 'Figural Series', 'SER_FIGURAL', 'Rotation, reflection and visual shape series', 3),
    ('SERIES', 'Letter Series', 'SER_LETTER', 'Alphabetical pattern and character shift sequences', 4),
    ('SERIES', 'Alpha-Numeric Series', 'SER_ALPHANUM', 'Combined letter, symbol and number series', 5),
    ('PROBLEM_SOLVING', 'Direction and Distance', 'PS_DIRECTION', 'Cardinal directions, shadow angles and displacement', 1),
    ('PROBLEM_SOLVING', 'Blood Relations', 'PS_BLOOD_RELATIONS', 'Family trees, coded relations and generational links', 2),
    ('PROBLEM_SOLVING', 'Complex Puzzles and Arrangements', 'PS_ARRANGEMENTS', 'Circular, rectangular and priority order arrangements', 3),
    ('PROBLEM_SOLVING', 'Floor & Box Puzzles', 'PS_FLOOR_FLAT', 'Multi-floor, box stack and flat seating arrangements', 4),
    ('PROBLEM_SOLVING', 'Scheduling Matrix', 'PS_SCHEDULING', 'Day, month, department and attribute matrix puzzles', 5),
    ('CODING_DECODING', 'Letter Coding', 'CD_LETTER', 'Letter shifting and alphabetical offset encoding', 1),
    ('CODING_DECODING', 'Number Coding', 'CD_NUMBER', 'Letter-to-digit and numerical mapping', 2),
    ('CODING_DECODING', 'Coding', 'CD_CODING', 'Rule-based sentence and word encoding', 3),
    ('CODING_DECODING', 'Decoding', 'CD_DECODING', 'Reversing cryptograms and cipher rules', 4),
    ('CODING_DECODING', 'Chinese & Substitution Coding', 'CD_SUBSTITUTION', 'Contextual word-phrase substitution and message extraction', 5),
    ('CODING_DECODING', 'Positional Shifts', 'CD_POSITIONAL', 'Alphabet rank values and reverse positional ranks', 6),
    ('NUMERICAL_OPS', 'Operator Substitution', 'NUM_OPS_SUBSTITUTION', 'Symbol substitution in arithmetic formulas', 1),
    ('NUMERICAL_OPS', 'Interchange of Signs and Numbers', 'NUM_OPS_SIGNS', 'Balancing equations through operator swapping', 2),
    ('SYMBOLIC_OPS', 'Symbol Substitution', 'SYM_OPS_SUBSTITUTION', 'Symbol-based logical relationships', 1),
    ('SYMBOLIC_OPS', 'Symbolic Inequalities', 'SYM_OPS_INEQUALITIES', 'Comparative inequalities with symbols', 2),
    ('TRENDS', 'Figural and Symbolic Trends', 'TRENDS_FIGURAL', 'Pattern trends across progressive steps', 1),
    ('SPACE_ORIENTATION', 'Spatial Orientation and Cardinal Directions', 'SPACE_ORIENT_DIR', '3D rotation and coordinate perspectives', 1),
    ('SPACE_VISUALIZATION', 'Cube and Dice Folding', 'SPACE_VIS_CUBES', 'Unfolded dice faces, opposite surfaces and 3D folding', 1),
    ('VENN_DIAGRAMS', 'Geometric Classes Venn Diagram', 'VENN_GEOMETRIC', 'Intersection of disjoint, overlapping and universal sets', 1),
    ('VENN_DIAGRAMS', 'Two and Three Group Venn Diagrams', 'VENN_LOGICAL', 'Numerical set problems involving Venn partitions', 2),
    ('DRAWING_INFERENCES', 'Statement and Assumption', 'INF_ASSUMPTIONS', 'Validating implicit assumptions behind statements', 1),
    ('DRAWING_INFERENCES', 'Critical Reasoning Inferences', 'INF_CRITICAL', 'Necessary implications and evidentiary deductions', 2),
    ('PATTERN_FOLDING', 'Punched Hole Pattern Folding and Unfolding', 'PAT_PUNCHED_HOLE', 'Hole punching symmetry across paper folds', 1),
    ('PATTERN_FOLDING', 'Paper Folding and Cutting', 'PAT_PAPER_FOLDING', 'Transparent sheet folding and perforation outcomes', 2),
    ('FIGURAL_PATTERN', 'Figure Pattern Completion', 'FIG_COMPLETION', 'Missing quadrant geometric completion', 1),
    ('INDEXING', 'Alphabetical and Numerical Indexing', 'INDEX_ALPHA_NUM', 'Record indexing and database key sorting', 1),
    ('ADDRESS_MATCHING', 'Address and Data Matching', 'ADDR_MATCH_VERIFY', 'Detecting typographical discrepancies in address records', 1),
    ('DATE_CITY_MATCHING', 'Date and City Matching Verification', 'DATE_CITY_VERIFY', 'Verifying tabular date and location entries', 1),
    ('EMBEDDED_FIGURES', 'Embedded Figures', 'EMB_FIG_HIDDEN', 'Locating hidden sub-patterns inside complex figures', 1),
    ('CRITICAL_THINKING', 'Cause and Effect', 'CRIT_CAUSE_EFFECT', 'Distinguishing root causes from incidental effects', 1),
    ('CRITICAL_THINKING', 'Statement and Argument', 'CRIT_STATEMENT_ARG', 'Differentiating strong versus weak arguments', 2),
    ('CRITICAL_THINKING', 'Course of Action', 'CRIT_COURSE_ACTION', 'Pragmatic and administrative courses of action', 3),
    ('EMOTIONAL_INTELLIGENCE', 'Situational Judgment and Emotional Intelligence', 'EMOT_SITUATIONAL', 'Handling interpersonal workplace dilemmas', 1),
    ('SOCIAL_INTELLIGENCE', 'Interpersonal Scenarios and Social Intelligence', 'SOC_INTERPERSONAL', 'Social dynamics, empathy and consensus building', 1),
    ('WORD_BUILDING', 'Dictionary Order', 'WORD_DICTIONARY', 'Alphabetical collation and dictionary sequence', 1),
    ('WORD_BUILDING', 'Word Formation and Meaningful Words', 'WORD_FORMATION', 'Forming meaningful words from letter combinations', 2),
    ('STATEMENT_CONCLUSION', 'Direct & Indirect Inferences', 'SC_INFERENCES', 'Definite deductions, transitive relationships, and strict implication', 1),
    ('STATEMENT_CONCLUSION', 'Logical Fallacies & Conditional Deductions', 'SC_FALLACIES', 'Contrapositive reasoning and avoiding formal fallacies', 2),
    ('STATEMENT_CONCLUSION', 'Cause, Effect and Assertion-Reasoning', 'SC_CAUSE_EFFECT', 'Validating causal linkages and sufficiency of premises', 3),
    ('STATEMENT_CONCLUSION', 'Multi-Statement Analytical Conclusions', 'SC_MULTI_STATEMENT', 'Multi-premise universal and existential deductions', 4),
    ('STATEMENT_CONCLUSION', 'Statement and Conclusion Evaluation', 'SC_EVALUATION', 'Standard statement and conclusion verification', 5),
    ('SYLLOGISTIC_REASONING', 'Two Premise Syllogisms', 'SYLL_TWO_PREMISE', 'Standard universal and particular categorical logic', 1),
    ('SYLLOGISTIC_REASONING', 'Three Premise Syllogisms', 'SYLL_THREE_PREMISE', 'Complex multi-premise Venn syllogisms', 2),
    ('SYLLOGISTIC_REASONING', 'Possibility & Negative Deductions', 'SYLL_POSSIBILITY', 'Possibility cases, no-conclusion and complementary pairs', 3),
    ('SYLLOGISTIC_REASONING', 'Standard & Only-A-Few Syllogism', 'SYLL_STANDARD', 'Modern banking syllogisms including only-a-few qualifiers', 4),
    ('LOGICAL_REASONING', 'Deductive and Inductive Logic', 'LOG_DEDUCTIVE', 'Formal logical consistency and truth-table evaluation', 1),
    ('ANALYTICAL_REASONING', 'Matrix and Seating Arrangements', 'ANA_MATRIX', 'Analytical matrix matching and relationship networks', 1),
    ('VERBAL_REASONING', 'Assertion and Reason', 'VRB_ASSERTION_REASON', 'Validating factual assertions and causal explanations', 1),
    ('NON_VERBAL_REASONING', 'Mirror Images', 'NVR_MIRROR_IMAGES', 'Lateral inversion and reflection across vertical/horizontal planes', 1),
    ('NON_VERBAL_REASONING', 'Paper Folding & Pattern Completion', 'NVR_PAPER_FOLDING', 'Paper folding, hole punching and pattern completion', 2),
    ('NON_VERBAL_REASONING', 'Diagrammatic Series', 'NVR_DIAGRAMMATIC', 'Abstract diagram series and topological shape evolution', 3),
    ('SEATING_ARRANGEMENT', 'Parallel Row Arrangements', 'SEAT_PARALLEL', 'Facing north-south parallel row seating puzzles', 1),
    ('SEATING_ARRANGEMENT', 'Unidirectional & Bidirectional Lines', 'SEAT_UNIDIRECTIONAL', 'Single row seating with directional orientations', 2),
    ('MACHINE_INPUT_OUTPUT', 'Mathematical Step Operations', 'MIO_STEP_OPS', 'Mathematical logic transformations per step', 1),
    ('MACHINE_INPUT_OUTPUT', 'Word & Number Rearrangement', 'MIO_WORD_NUMBER', 'Alternating word alphabetical and number sorting', 2),
    ('INEQUALITIES', 'Direct Inequalities', 'INEQ_DIRECT', 'Evaluating chain inequalities involving greater/lesser relations', 1),
    ('INEQUALITIES', 'Coded Inequalities', 'INEQ_CODED', 'Symbol-substituted comparative inequality deductions', 2),
    ('PUZZLES_CRITICAL', 'Floor & Box Puzzles', 'PUZ_FLOOR_BOX', 'Multi-attribute vertical and sequential stack puzzles', 1),
    ('PUZZLES_CRITICAL', 'Critical Reasoning', 'PUZ_CRITICAL', 'Strengthening, weakening and assumption deductions', 2),
    ('NUMBER_SERIES_REAS', 'Missing Number Series', 'NUM_SER_MISSING', 'Determining missing terms in complex numerical sequences', 1),
    ('NUMBER_SERIES_REAS', 'Wrong Number Series', 'NUM_SER_WRONG', 'Detecting the erroneous term in arithmetic/polynomial series', 2),
    ('SYLLOGISM', 'Possibility & Negative Deductions', 'SYLL_POSS_NEG', 'Possibility statements and negative categorical boundaries', 1),
    ('SYLLOGISM', 'Standard & Only-A-Few Syllogism', 'SYLL_STD_FEW', 'Exclusivity qualifiers and only-a-few syllogisms', 2),
    ('CURRENT_EVENTS', 'National Current Events', 'CE_NATIONAL', 'Government initiatives, national infrastructure, sports and honours', 1),
    ('CURRENT_EVENTS', 'International Current Events', 'CE_INTERNATIONAL', 'Global treaties, geopolitical conflicts, summits and international leadership', 2),
    ('HISTORY', 'Ancient History', 'HIST_ANCIENT', 'Indus Valley civilization, Vedic period, Mauryas and Guptas', 1),
    ('HISTORY', 'Medieval History', 'HIST_MEDIEVAL', 'Delhi Sultanate, Mughal Empire, Vijayanagara and Marathas', 2),
    ('HISTORY', 'Modern History', 'HIST_MODERN', 'European advent, British administration and Indian national movement', 3),
    ('CULTURE', 'Indian Art and Culture', 'CULT_ART', 'Classical arts, temple styles, paintings and handicrafts', 1),
    ('CULTURE', 'Fairs, Festivals and Heritage Sites', 'CULT_HERITAGE', 'UNESCO World Heritage and regional cultural traditions', 2),
    ('GEOGRAPHY', 'Physical Geography', 'GEOG_PHYSICAL', 'Landforms, plate tectonics, volcanoes and weathering', 1),
    ('GEOGRAPHY', 'River Systems and Drainage', 'GEOG_RIVERS', 'Himalayan and Peninsular river basins and deltas', 2),
    ('GEOGRAPHY', 'Climate and Monsoons', 'GEOG_CLIMATE', 'Monsoon dynamics, Western disturbances and cyclones', 3),
    ('ECONOMIC_SCENE', 'Indian Economy Overview', 'ECON_INDIAN_OVERVIEW', 'GDP growth, sectors of the economy, monetary and fiscal policies', 1),
    ('ECONOMIC_SCENE', 'Banking and Financial Institutions', 'ECON_BANKING', 'RBI, banking regulation and public sector reforms', 2),
    ('GENERAL_POLICY', 'Constitution of India', 'GP_CONSTITUTION', 'Framing, Articles, Schedules and Constitutional architecture', 1),
    ('GENERAL_POLICY', 'Fundamental Rights and Duties', 'GP_FUNDAMENTAL_RIGHTS', 'Articles 12-35, writs and Directive Principles', 2),
    ('SCIENTIFIC_RESEARCH', 'Space Missions and ISRO', 'SR_SPACE_ISRO', 'Launch vehicles, lunar/solar missions and satellite programs', 1),
    ('SCIENTIFIC_RESEARCH', 'Defence Technology and DRDO', 'SR_DEFENCE_DRDO', 'Missile systems, indigenous defense platforms and radars', 2),
    ('EVERYDAY_SCIENCE', 'Basic Physics in Daily Life', 'ES_PHYSICS_DAILY', 'Mechanics, optics, household electricity and sound', 1),
    ('EVERYDAY_SCIENCE', 'Chemistry in Everyday Life', 'ES_CHEMISTRY_DAILY', 'Chemical compounds, cleaning agents, acids and bases', 2),
    ('EVERYDAY_SCIENCE', 'Human Biology and Health', 'ES_BIOLOGY_HEALTH', 'Human physiology, infectious diseases and public hygiene', 3),
    ('INDIA_NEIGHBOURS', 'International Borders and Treaties', 'IN_BORDERS', 'Boundary lines, agreements and bilateral engagements', 1),
    ('INDIAN_HISTORY', 'Ancient India', 'IH_ANCIENT', 'Ancient civilizations, dynasties and philosophical schools', 1),
    ('INDIAN_HISTORY', 'Medieval India', 'IH_MEDIEVAL', 'Sultanate, Mughal empire and regional kingdoms', 2),
    ('INDIAN_HISTORY', 'Modern India', 'IH_MODERN', 'Freedom struggle, constitutional milestones and independence', 3),
    ('INDIAN_GEOGRAPHY', 'Physical and Regional Geography', 'IG_PHYSICAL', 'Physiographic divisions of India and mountain passes', 1),
    ('INDIAN_GEOGRAPHY', 'Drainage and Climate', 'IG_DRAINAGE', 'Major river basins, multipurpose river valley projects', 2),
    ('INDIAN_POLITY', 'Constitutional Framework & Historical Framing', 'IP_FRAMEWORK', 'Making of Constitution and constitutional conventions', 1),
    ('INDIAN_POLITY', 'Preamble, Union & Citizenship', 'IP_PREAMBLE', 'Preamble ideals, Union territory and citizenship laws', 2),
    ('INDIAN_POLITY', 'Fundamental Rights & Writs', 'IP_FUNDAMENTAL_RIGHTS', 'Rights to equality, liberty, religious freedom and judicial writs', 3),
    ('INDIAN_POLITY', 'Directive Principles & Fundamental Duties', 'IP_DPSP_DUTIES', 'Socio-economic directives and constitutional duties', 4),
    ('INDIAN_POLITY', 'Union Executive', 'IP_UNION_EXEC', 'President, Prime Minister, Union cabinet and executive prerogatives', 5),
    ('INDIAN_POLITY', 'Union Legislature & Parliament', 'IP_UNION_LEGIS', 'Lok Sabha, Rajya Sabha and parliamentary procedures', 6),
    ('INDIAN_POLITY', 'State Executive & Legislature', 'IP_STATE_EXEC_LEGIS', 'Governors, Chief Ministers and state legislative assemblies', 7),
    ('INDIAN_POLITY', 'Judiciary', 'IP_JUDICIARY', 'Supreme Court, High Courts, judicial review and basic structure doctrine', 8),
    ('INDIAN_POLITY', 'Local Self-Government & Panchayati Raj', 'IP_LOCAL_GOVT', '73rd and 74th Amendments, PRIs and urban civic governance', 9),
    ('INDIAN_POLITY', 'Constitutional Bodies & Amendments', 'IP_BODIES_AMEND', 'Election Commission, CAG, Finance Commission and Amendment procedures', 10),
    ('INDIAN_ECONOMY', 'Planning and NITI Aayog', 'IE_PLANNING', 'Five-Year Plans, economic reforms and NITI Aayog policy', 1),
    ('INDIAN_ECONOMY', 'Fiscal Policy and Taxation', 'IE_FISCAL', 'GST, direct/indirect taxes, public debt and deficits', 2),
    ('SCIENCE_TECH', 'Scientific Developments and Innovations', 'ST_DEVELOPMENTS', 'Biotechnology, renewable energy, AI and digital infrastructure', 1),
    ('ENVIRONMENT', 'Ecology and Conservation', 'ENV_CONSERVATION', 'Protected areas, biosphere reserves and wildlife preservation', 1),
    ('CURRENT_AFFAIRS', 'National and International Events', 'CA_EVENTS', 'Contemporary socio-economic and political developments', 1),
    ('INDIAN_ART_CULTURE', 'Art, Architecture & Literature', 'IAC_HERITAGE', 'Temple architecture, classical arts and literary traditions', 1),
    ('INTL_RELATIONS', 'International Forums and Treaties', 'IR_BODIES', 'Multilateral conventions, global governance and international organizations', 1),
    ('INTERNAL_SECURITY', 'Security Challenges and Management', 'IS_FRAMEWORK', 'Border management, counter-terrorism, NDMA disaster guidelines', 1),
    ('ETHICS_INTEGRITY', 'Ethics and Public Administration', 'ETH_VALUES', 'Probity in governance, code of conduct and civil service values', 1),
    ('PHYSICAL_GEOGRAPHY', 'Subduction Zone Tectonic Plate Boundary Cross-Section', 'PG_TECTONICS', 'Plate boundaries, convergent margins, subduction zones and volcanism', 1),
    ('PHYSICAL_GEOGRAPHY', 'Topographical Contour Line Elevation Map', 'PG_CONTOURS', 'Contour lines, relief features, gradient and elevation mapping', 2),
    ('VOCABULARY', 'Synonyms', 'VOC_SYNONYMS', 'Contextual word equivalents and nuance', 1),
    ('VOCABULARY', 'Antonyms', 'VOC_ANTONYMS', 'Opposite word meanings in discourse', 2),
    ('VOCABULARY', 'Homonyms', 'VOC_HOMONYMS', 'Homophones and confusable words', 3),
    ('VOCABULARY', 'Vocabulary Usage', 'VOC_USAGE', 'Accurate lexical selection in sentences', 4),
    ('GRAMMAR', 'Grammar Rules', 'GRM_RULES', 'Parts of speech, syntax and concord', 1),
    ('GRAMMAR', 'Verb Usage', 'GRM_VERB_USAGE', 'Tenses, modal auxiliaries and non-finite verbs', 2),
    ('SENTENCE_STRUCTURE', 'Sentence Ordering and Synthesis', 'SS_SYNTHESIS', 'Simple, compound and complex sentence structures', 1),
    ('SPOT_ERROR', 'Grammatical Errors in Sentences', 'SE_SYNTAX_ERRORS', 'Identification of grammatical anomalies in clauses', 1),
    ('FILL_BLANKS', 'Contextual Word Selection', 'FB_CONTEXTUAL', 'Contextual cloze sentence fillers', 1),
    ('FILL_BLANKS', 'Double Fillers in Banking', 'FB_DOUBLE_BANKING', 'Dual-blank contextual completion in financial prose', 2),
    ('SYNONYMS_HOMONYMS', 'Synonym Identification', 'SH_DISAMBIGUATION', 'Exact synonym matching across varied registers', 1),
    ('ANTONYMS', 'Antonym Identification', 'ANT_OPPOSITE', 'Antonym selection in formal prose', 1),
    ('SPELLINGS', 'Misspelt Words Detection', 'SPEL_ACCURACY', 'Spotting orthographic errors in vocabulary', 1),
    ('IDIOMS_PHRASES', 'Idiomatic Expressions and Phrasal Verbs', 'IP_MEANINGS', 'Figurative expressions and phrasal verb idioms', 1),
    ('ONE_WORD_SUB', 'One Word Substitution', 'OWS_TERMS', 'Lexical substitutes for descriptive clauses', 1),
    ('SENTENCE_IMPROVEMENT', 'Sentence Correction and Improvement', 'SI_REPLACEMENT', 'Grammatical optimization of underlined clauses', 1),
    ('VOICE_ACTIVE_PASSIVE', 'Voice Conversion Rules', 'VAP_TRANSFORMATION', 'Transforming sentences between active and passive voice', 1),
    ('NARRATION_DIRECT_INDIRECT', 'Reported Speech Rules', 'NDI_SPEECH', 'Direct and indirect speech conversion principles', 1),
    ('SENTENCE_SHUFFLING', 'Sentence Parts', 'SSH_PARTS', 'Reconstructing sentence fragments (PQRS)', 1),
    ('SENTENCE_SHUFFLING', 'Sentence Ordering', 'SSH_ORDERING', 'Paragraph-level logical sentence sequencing', 2),
    ('CLOZE_PASSAGE', 'Standard Cloze Test', 'CP_PASSAGE_FILL', 'Contextual paragraph filler selection', 1),
    ('COMPREHENSION_PASSAGE', 'Literary Passage', 'COMP_LITERARY', 'Literary comprehension and stylistic inferences', 1),
    ('COMPREHENSION_PASSAGE', 'Current Affairs Passage', 'COMP_CURRENT_AFFAIRS', 'Editorial and socio-economic reading comprehension', 2),
    ('COMPREHENSION_PASSAGE', 'Report or Editorial Passage', 'COMP_REPORT', 'Analytical policy and technical report comprehension', 3),
    ('COMPREHENSION', 'Reading Comprehension', 'COMP_GENERAL', 'Passage main idea, theme and tone analysis', 1),
    ('ERROR_DETECTION', 'Subject-Verb & Modifier Errors', 'ED_MODIFIERS', 'Dangling modifiers and agreement defects', 1),
    ('ERROR_DETECTION', 'Tense, Conditionals & Prepositions', 'ED_TENSES', 'Subjunctive mood, conditionals and prepositional idiom', 2),
    ('IDIOMS_VOCAB', 'Financial Idioms & Expressions', 'IV_FINANCIAL_EXPR', 'Banking and financial idioms in context', 1),
    ('IDIOMS_VOCAB', 'Phrasal Verbs & Usage', 'IV_PHRASALS', 'Polysemous phrasal verbs and particle semantics', 2),
    ('PARA_JUMBLES', 'Editorial Sentence Sequences', 'PJ_EDITORIAL', 'Rearranging five to six sentence editorial paragraphs', 1),
    ('PARA_JUMBLES', 'Sentence Boundary Identification', 'PJ_BOUNDARIES', 'Identifying introductory and concluding sentences', 2),
    ('READING_COMP', 'Financial & Economic Passages', 'RC_FINANCIAL_PASSAGES', 'Banking and macroeconomic case studies', 1),
    ('READING_COMP', 'Inference & Author Intent', 'RC_INFERENCES', 'Deducing implicit tone, intent and thematic conclusions', 2),
    ('SENTENCE_CONNECTORS', 'Discourse Connectors', 'SC_DISCOURSE_CONN', 'Connectors showing concession, cause and result', 1),
    ('SENTENCE_CONNECTORS', 'Sentence Starters', 'SC_STARTER_PHRASES', 'Synthesizing two sentences with specific starting clauses', 2),
    ('SENTENCE_IMPROVE_CORR', 'Phrase Replacement', 'SIC_PHRASE_REPLACE', 'Substituting highlighted fragments with standard idioms', 1),
    ('SENTENCE_IMPROVE_CORR', 'Structural Parallelism', 'SIC_PARALLEL', 'Maintaining coordinate and correlative conjunction symmetry', 2),
    ('WORD_SWAP', 'Four-Word Swap', 'WS_FOUR_SWAP', 'Exchanging bolded words across positions A, B, C, D', 1),
    ('WORD_SWAP', 'Vocabulary Alignment', 'WS_ALIGN', 'Semantic contextual fit of swapped terms', 2),
    ('MATCH_COLUMNS', 'Economic Discourse Matching', 'MC_ECON_DISCOURSE', 'Matching sentence clauses across column 1 and column 2', 1),
    ('MATCH_COLUMNS', 'Grammatical Fragment Matching', 'MC_GRAMMAR_FRAG', 'Syntactic pairing of clauses across columns', 2),
    ('PHYSICS', 'Mechanics', 'PHY_MECHANICS', 'Kinematics, dynamics, circular motion, Newton laws', 1),
    ('PHYSICS', 'Optics', 'PHY_OPTICS', 'Reflection, refraction, mirrors, lenses, dispersion', 2),
    ('PHYSICS', 'Units and Measurements', 'PHY_UNITS', 'SI units, dimensions, errors and precision', 3),
    ('PHYSICS', 'Kinematics and Motion', 'PHY_MOTION', 'Velocity, acceleration, projectile motion', 4),
    ('PHYSICS', 'Laws of Motion and Force', 'PHY_FORCE', 'Friction, momentum, impulse and equilibrium', 5),
    ('PHYSICS', 'Work, Energy and Power', 'PHY_WORK_ENERGY', 'Kinetic energy, potential energy, conservation of energy', 6),
    ('PHYSICS', 'Gravitation', 'PHY_GRAVITATION', 'Kepler laws, gravitational acceleration and escape velocity', 7),
    ('PHYSICS', 'Fluids and Properties of Matter', 'PHY_FLUIDS', 'Pressure, Pascal law, Archimedes principle, surface tension, viscosity', 8),
    ('PHYSICS', 'Heat and Thermodynamics', 'PHY_HEAT', 'Thermal expansion, calorimetry, thermodynamic laws', 9),
    ('PHYSICS', 'Waves and Sound', 'PHY_WAVES', 'Longitudinal/transverse waves, Doppler effect, resonance', 10),
    ('PHYSICS', 'Electricity and Magnetism', 'PHY_ELECTRICITY', 'Ohm law, circuits, magnetic field, electromagnetic induction', 11),
    ('PHYSICS', 'Optics and Light', 'PHY_LIGHT', 'Interference, diffraction, polarization and optical instruments', 12),
    ('CHEMISTRY', 'Organic', 'CHEM_ORGANIC', 'Hydrocarbons, functional groups, isomerism and nomenclature', 1),
    ('CHEMISTRY', 'Atomic Structure and Mole Concept', 'CHEM_ATOMIC', 'Subatomic particles, Bohr model, electronic configuration, Avogadro number', 2),
    ('CHEMISTRY', 'Periodic Table and Classification of Elements', 'CHEM_PERIODIC', 'Periodic trends, electronegativity, ionization enthalpy', 3),
    ('CHEMISTRY', 'Chemical Reactions and Equations', 'CHEM_REACTIONS', 'Types of chemical reactions, stoichiometry and oxidation-reduction', 4),
    ('CHEMISTRY', 'Acids, Bases, Salts and pH Scale', 'CHEM_ACIDS_BASES', 'Arrhenius/Bronsted theories, indicators and pH calculations', 5),
    ('CHEMISTRY', 'Metals, Non-Metals and Metallurgy', 'CHEM_METALS', 'Extraction of metals, ores, alloys and reactivity series', 6),
    ('CHEMISTRY', 'Carbon and its Compounds', 'CHEM_CARBON', 'Allotropes of carbon, alkanes, alkenes, alkynes, functional groups', 7),
    ('CHEMISTRY', 'Environmental Chemistry and Pollution', 'CHEM_ENVIRONMENTAL', 'Atmospheric pollution, greenhouse effect, acid rain and ozone depletion', 8),
    ('CHEMISTRY', 'Chemistry in Everyday Life and Polymers', 'CHEM_DAILY', 'Soaps, detergents, medicines, polymers and synthetic fibers', 9),
    ('BIOLOGY', 'Cell Biology and Genetics', 'BIO_CELL', 'Prokaryotic/eukaryotic cells, cell division, DNA and Mendelian genetics', 1),
    ('BIOLOGY', 'Human Physiology and Health', 'BIO_PHYSIOLOGY', 'Digestive, circulatory, respiratory, nervous systems and endocrinology', 2),
    ('COMPUTER_BASICS', 'Computer Organization & CPU', 'CB_CPU', 'ALU, control unit, registers, clock speed and bus architectures', 1),
    ('COMPUTER_BASICS', 'Input and Output Devices', 'CB_DEVICES', 'Keyboards, mice, scanners, monitors, printers and display technologies', 2),
    ('COMPUTER_BASICS', 'Computer Memory & Backup', 'CB_MEMORY', 'RAM, ROM, cache hierarchy, SSDs, HDDs and secondary storage', 3),
    ('SOFTWARE', 'Windows Operating System', 'SW_OS', 'Process scheduling, file systems, device drivers and GUI navigation', 1),
    ('SOFTWARE', 'Microsoft Office Suite', 'SW_OFFICE', 'Word processing, spreadsheets, presentations and shortcut commands', 2),
    ('INTERNET_EMAIL', 'Web Browsing and Searching', 'IE_BROWSING', 'HTTP, HTTPS, URL structure, search engine indexing and caching', 1),
    ('INTERNET_EMAIL', 'E-mail Management and Security', 'IE_EMAIL', 'SMTP, IMAP, POP3 protocols, spam filtering and encryption', 2),
    ('NETWORKING_CYBER_SECURITY', 'Networking Devices and Protocols', 'NCS_DEVICES', 'Routers, switches, hubs, IP addressing, subnetting and DNS', 1),
    ('NETWORKING_CYBER_SECURITY', 'Cybersecurity and Malware', 'NCS_SECURITY', 'Viruses, worms, trojans, ransomware, phishing and firewalls', 2),
    ('PROGRAMMING', 'Data Structures', 'PROG_DS', 'Arrays, linked lists, stacks, queues, trees and graphs', 1),
    ('PROGRAMMING', 'Algorithms', 'PROG_ALGO', 'Sorting, searching, algorithmic complexity and dynamic programming', 2),
    ('DATABASES', 'Relational Databases and SQL', 'DB_SQL', 'DDL, DML queries, joins, indexes, ACID properties and normalization', 1),
    ('NETWORKING', 'OSI and TCP/IP Models', 'NET_MODELS', 'Seven-layer OSI stack, TCP vs UDP and network topologies', 1),
    ('DATA_COLLECTION_PRESENTATION', 'Primary and Secondary Data Collection', 'STATS_PRIMARY', 'Questionnaires, sampling methods, tabulations and distributions', 1),
    ('CENTRAL_TENDENCY', 'Mean, Median and Mode', 'STATS_MEAN', 'Calculations of averages, weighted means and partition values', 1),
    ('DISPERSION', 'Range, Variance and Standard Deviation', 'STATS_RANGE_STD', 'Absolute and relative measures of dispersion', 1),
    ('MOMENTS_SKEWNESS', 'Moments, Skewness and Kurtosis', 'STATS_MOMENTS', 'Calculations of central moments, skewness and kurtosis coefficients', 1),
    ('CORRELATION_REGRESSION', 'Correlation and Regression Lines', 'STATS_CORR_REG', 'Bivariate correlation, coefficient of determination and regression slopes', 1),
    ('PROBABILITY_THEORY', 'Probability Theory and Bayes Theorem', 'STATS_PROB_THEORY', 'Probability axioms, independent events and Bayes theorem', 1),
    ('PROBABILITY_DISTRIBUTIONS', 'Random Variables and Probability Distributions', 'STATS_DISTRIBUTIONS', 'Discrete and continuous distributions, PDF, CDF, expectation', 1),
    ('SAMPLING_THEORY', 'Sampling Theory and Estimators', 'STATS_SAMPLING', 'Sample distributions, central limit theorem and sampling errors', 1),
    ('STATISTICAL_INFERENCE', 'Hypothesis Testing and Confidence Intervals', 'STATS_HYPOTHESIS', 'Type I/II errors, significance levels, p-values, z and t tests', 1),
    ('ANOVA', 'One-Way and Two-Way ANOVA', 'STATS_ANOVA_TEST', 'F-distribution, sum of squares and variance ratios', 1),
    ('TIME_SERIES', 'Time Series Analysis and Trends', 'STATS_TIME_SERIES', 'Moving averages, least-squares trend fitting, seasonal indices', 1),
    ('INDEX_NUMBERS', 'Index Numbers and Formulae', 'STATS_INDEX_NUMS', 'Price and quantity index formulas, base shifting and inflation adjustment', 1),
    ('BFA_INDIAN_BANKING', 'Commercial Banks & Types', 'BFA_COMMERCIAL', 'Public, private, foreign banks, SFBs and payments banks', 1),
    ('BFA_INDIAN_BANKING', 'RBI Constitution & Currency Management', 'BFA_RBI', 'RBI Act 1934, subsidiary institutions, currency issuance and oversight', 2),
    ('BFA_MONETARY_POLICY', 'Monetary Policy Framework', 'BFA_FRAMEWORK', 'MPC mandate, headline inflation targeting, liquidity adjustment facility (LAF)', 1),
    ('BFA_MONETARY_POLICY', 'Policy Rates & Liquidity Ratios', 'BFA_RATES', 'Cash Reserve Ratio (CRR), Statutory Liquidity Ratio (SLR), MSF and bank rate', 2),
    ('BFA_FINANCIAL_MARKETS', 'Money and Capital Market Instruments', 'BFA_MARKET_INST', 'Treasury bills, certificates of deposit, commercial papers, equity and debt markets', 1),
    ('BFA_DIGITAL_BANKING', 'Unified Payments Interface (UPI) & IMPS', 'BFA_UPI', 'Real-time retail payment infrastructure, UPI 123Pay and IMPS architecture', 1),
    ('BFA_DIGITAL_BANKING', 'Central Bank Digital Currency (CBDC)', 'BFA_CBDC', 'Digital Rupee wholesale (e₹-W) and retail (e₹-R) frameworks and tokenization', 2),
    ('BFA_GOVT_SCHEMES', 'Financial Inclusion Schemes', 'BFA_INCLUSION', 'PMJDY zero-balance accounts, overdrafts and direct benefit transfer mechanisms', 1),
    ('BFA_GOVT_SCHEMES', 'Social Security & Microfinance Schemes', 'BFA_SOCIAL_SEC', 'Life/disability insurance schemes, APY pension rules and MUDRA sub-categories', 2),
    ('BFA_NPA_MANAGEMENT', 'Asset Classification & Provisioning', 'BFA_CLASSIFICATION', 'Standard, sub-standard, doubtful and loss assets, SMA-0/1/2 buckets', 1),
    ('BFA_NPA_MANAGEMENT', 'Insolvency & Recovery Mechanisms', 'BFA_RECOVERY', 'SARFAESI Act 2002, DRT procedures, Insolvency and Bankruptcy Code (IBC) 2016', 2),
    ('BFA_NPA_MANAGEMENT', 'Capital Adequacy & Tier Structure', 'BFA_CAPITAL_ADEQUACY', 'Basel III capital ratios, Tier-I/II capital, capital conservation buffers', 3),
    ('BFA_MONEY_CAPITAL', 'Capital Markets & Bond Dynamics', 'BFA_BONDS', 'G-Secs, yield curve, sovereign green bonds and mutual fund regulations', 1),
    ('BFA_MONEY_CAPITAL', 'Money Market Instruments', 'BFA_INSTRUMENTS', 'Call/notice money, repo, reverse repo, commercial bills and T-bills', 2),
    ('BFA_NPA_RESOLUTION', 'Asset Classification & Provisioning', 'BFA_SARFAESI', 'Non-performing asset classification and provisioning requirements', 1),
    ('BFA_NPA_RESOLUTION', 'Insolvency & Recovery Mechanisms', 'BFA_IBC_RECOVERY', 'Insolvency resolution professionals, committee of creditors and liquidation', 2),
    ('BFA_NEGOTIABLE_INST', 'Banking Ombudsman & Customer Rights', 'BFA_OMBUDSMAN', 'Reserve Bank - Integrated Ombudsman Scheme (RB-IOS) and consumer protection', 1),
    ('BFA_NEGOTIABLE_INST', 'Negotiable Instruments Act 1881', 'BFA_NI_ACT', 'Sections 138 bouncing of cheques, holder in due course, endorsements', 2),
    ('BFA_PSL_CREDIT', 'PSL Targets & Classification', 'BFA_PSL_TARGETS', 'Agriculture, MSME, education, housing targets and PSLC certificates', 1),
    ('BFA_PSL_CREDIT', 'Rural Infrastructure & Shortfall Mechanics', 'BFA_RURAL_INFRA', 'NABARD RIDF contributions for domestic and foreign bank PSL shortfalls', 2),
    ('DILA_TABULAR', 'Multi-Variable Tables', 'DILA_MULTI_VAR', 'Extracting trends, computing totals and ratios across structured tables', 1),
    ('DILA_TABULAR', 'Missing Data Tables', 'DILA_MISSING', 'Solving algebraic and arithmetic conditions in incomplete tables', 2),
    ('DILA_BAR_GRAPHS', 'Bar Graphs and Histograms', 'DILA_HISTOGRAMS', 'Grouped and stacked bar comparisons across sectors and years', 1),
    ('DILA_LINE_GRAPHS', 'Radar Charts & Polygonal DI', 'DILA_RADAR', 'Spider-web multivariable comparisons across dimensions', 1),
    ('DILA_LINE_GRAPHS', 'Time Series Line Trends', 'DILA_TIME_SERIES', 'Analyzing multi-year growth curves and intersection points', 2),
    ('DILA_PIE_CHARTS', 'Degree & Percentage Pie Charts', 'DILA_DEG_PCT', 'Angle-to-percentage transformations and sector proportional distribution', 1),
    ('DILA_PIE_CHARTS', 'Mixed Graph Sets', 'DILA_MIXED_SETS', 'Coupled table-pie, bar-line and composite data analysis sets', 2),
    ('DILA_CASELET_ARITH', 'Arithmetic Caselets', 'DILA_ARITH_CASELETS', 'Passage-based calculations involving mixtures, time/work and ratios', 1),
    ('DILA_CASELET_ARITH', 'Profit & Loss and SI/CI Caselets', 'DILA_SI_CI_CASELETS', 'Commercial arithmetic data sets and banking caselets', 2),
    ('DILA_DATA_SUFFICIENCY', 'Data Sufficiency', 'DILA_SUFFICIENCY_ALGEBRA', 'Two and three statement sufficiency evaluations in mathematics', 1),
    ('DILA_QUADRATIC', 'Higher Degree & Root Equations', 'DILA_ROOT_EQUATIONS', 'Comparing signs and magnitudes of paired polynomial roots', 1),
    ('DILA_QUADRATIC', 'Quadratic Comparisons', 'DILA_QUAD_COMP', 'Symbolic comparisons of root intervals X vs Y', 2),
    ('HND_VARN_VICHAR', 'स्वर एवं व्यंजन विचार', 'HND_SWAR_VJAN', 'स्वर, व्यंजन भेद, प्रयत्न एवं उच्चारण स्थान', 1),
    ('HND_SANDHI', 'स्वर संधि भेद (दीर्घ, गुण, वृद्धि, यण, अयादि)', 'HND_SWAR_SANDHI', 'स्वर संधि के भेद एवं विच्छेद नियम', 1),
    ('HND_SANDHI', 'व्यंजन संधि एवं विसर्ग संधि', 'HND_VYANJAN_SANDHI', 'व्यंजन एवं विसर्ग संधि के प्रमुख नियम', 2),
    ('HND_SAMAS', 'तत्पुरुष एवं कर्मधारय समास', 'HND_TATPURUSH', 'उत्तरपद प्रधान समास और विशेषण-विशेष्य संबंध', 1),
    ('HND_SAMAS', 'द्विगु, द्वन्द्व एवं बहुव्रीहि समास', 'HND_DVIGU', 'संख्यापूर्व द्विगु, उभयपद प्रधान द्वन्द्व एवं अन्यपद प्रधान बहुव्रीहि', 2),
    ('HND_VILOM_PARYAY', 'समानार्थी एवं विपरीतार्थक शब्द', 'HND_VILOM_WORDS', 'समानार्थी, विलोम एवं अनेकार्थी शब्द प्रयोग', 1),
    ('HND_MUHAVARE', 'प्रचलित मुहावरे एवं लोकोक्तियां', 'HND_MUHAVARE_USE', 'मुहावरे एवं लोकोक्तियों के सटीक भावार्थ एवं प्रयोग', 1),
    ('HND_ONE_WORD', 'संक्षिप्त अभिव्यक्ति एवं एकल शब्द', 'HND_EKAL_SHABD', 'वाक्यांश के लिए एक शब्द का सटीक चयन', 1),
    ('HND_VAKYA_SHUDDHI', 'कारक एवं वचन संबंधी अशुद्धियां', 'HND_KARAK_SHUDDHI', 'कारक, वचन एवं लिंग संबंधी त्रुटियों का शोधन', 1),
    ('HND_VAKYA_SHUDDHI', 'पदक्रम एवं मुहावरे संबंधी वाक्य दोष', 'HND_PADKRAM', 'वाक्य में शब्दों का उपयुक्त क्रम एवं दोष निवारण', 2),
    ('HND_GADHYANSH', 'अपठित गद्यांश एवं बोधगम्यता', 'HND_BODHGAMYATA', 'गद्यांश का केंद्रीय भाव, बोध प्रश्न एवं निष्कर्ष', 1),
    ('PAG_THEORIES', 'Scientific Management and Bureaucracy', 'PAG_CLASSICAL', 'Taylor, Fayol, Weber and Simon decision-making theory', 1),
    ('PAG_MACHINERY', 'Cabinet Secretariat and Ministries', 'PAG_CABINET', 'Policy coordination, secretariat administration and civil services structure', 1),
    ('PAG_LOCAL_GOVT', 'Panchayati Raj and 73rd Amendment', 'PAG_PANCHAYAT', 'Gram Sabha, three-tier PRI system and financial devolution', 1),
    ('PAG_POLICY', 'Public Policy and Citizen Charters', 'PAG_RTI', 'Transparency, Right to Information and anti-corruption machinery', 1),
    ('PAG_EGOV_DPI', 'Digital India and DPI Architecture', 'PAG_DIGITAL_INDIA', 'Open digital protocols, Aadhaar, DigiLocker, UPI and paperless governance', 1),
    ('EEB_ECOSYSTEM', 'Food Chains and Trophic Levels', 'EEB_FOOD_CHAINS', 'Energy pyramid, trophic efficiency and nutrient cycles', 1),
    ('EEB_BIODIVERSITY', 'National Parks & Wildlife Sanctuaries of India', 'EEB_PARKS', 'In-situ conservation, Project Tiger, Project Elephant and wildlife corridors', 1),
    ('EEB_BIODIVERSITY', 'Biosphere Reserves & Ramsar Sites in India', 'EEB_RAMSAR', 'Wetland conservation, Ramsar criteria and biosphere zoning', 2),
    ('EEB_CLIMATE', 'Paris Agreement & Nationally Determined Contributions (NDCs)', 'EEB_PARIS', 'Global climate targets, net zero commitments and emission reductions', 1),
    ('EEB_CLIMATE', 'Renewable Energy Targets & National Solar Mission', 'EEB_SOLAR', 'Clean energy transitions, solar parks and green hydrogen mission', 2),
    ('EEB_POLLUTION', 'Air, Water and Waste Management Rules', 'EEB_WASTE_RULES', 'Pollution abatement standards, NGT decisions and circular economy frameworks', 1),
    ('CDP_DEVELOPMENT', 'Cognitive Development Theory (Jean Piaget)', 'CDP_PIAGET', 'Sensorimotor, preoperational, concrete and formal operational stages', 1),
    ('CDP_DEVELOPMENT', 'Socio-Cultural Learning Theory (Lev Vygotsky)', 'CDP_VYGOTSKY', 'Zone of proximal development (ZPD), scaffolding and social mediation', 2),
    ('CDP_INCLUSIVE', 'Diverse Learners and Inclusive Education', 'CDP_SPECIAL_NEEDS', 'Universal design for learning, accommodations and RTE Act provisions', 1),
    ('CDP_STRATEGIES', 'Constructivist Teaching and Problem Solving', 'CDP_CONSTRUCTIVIST', 'Active student inquiry, experiential learning and concept mapping', 1),
    ('CDP_ASSESSMENT', 'Formative & Summative Evaluation Techniques', 'CDP_FORMATIVE', 'Diagnostic evaluation, rubrics, portfolios and constructive feedback', 1),
    ('CDP_ASSESSMENT', 'School-Based Assessment (SBA) & Holistic Report Cards', 'CDP_SBA', '360-degree holistic assessment and competency-based evaluation', 2)
) AS v(topic_code, name, code, description, display_order)
JOIN question_service.topic t ON t.code = v.topic_code AND t.tenant_id IS NULL
ON CONFLICT DO NOTHING;
