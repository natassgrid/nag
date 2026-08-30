CREATE SCHEMA IF NOT EXISTS question_service;

-- Enable pgvector extension for halfvec type (requires pgvector/pgvector Docker image)
CREATE EXTENSION IF NOT EXISTS vector SCHEMA public;

-- ============================================================
-- Subject -> Topic -> Subtopic hierarchy tables
-- ------------------------------------------------------------
-- These are small, slowly-changing reference tables referenced
-- by the very large `question` fact table. They use compact
-- numeric BIGINT identity primary keys so the foreign keys on
-- `question` are 8 bytes (vs 16 for a UUID), halving index/row
-- storage on the referencing side and speeding up joins.
-- ============================================================

CREATE TABLE question_service.subject (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    subject_id  BIGINT NOT NULL REFERENCES question_service.subject(id) ON DELETE CASCADE,
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
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    topic_id    BIGINT NOT NULL REFERENCES question_service.topic(id) ON DELETE CASCADE,
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
-- Table: question (hash-partitioned by subject_id for scalability)
-- ============================================================
-- Content supports: plain text, HTML, LaTeX ($$..$$), SVG (<svg>...</svg>)
-- content_format indicates how to render: TEXT, HTML, LATEX, SVG, MIXED
-- Options stored as JSONB array: [{id, text, isCorrect}]
-- Embedding: halfvec(384) from all-minilm model for similarity detection
--
-- The hierarchy link is by numeric FK (subject_id/topic_id/subtopic_id).
-- The subject/topic/subtopic NAME columns are denormalized copies kept in
-- sync at write time; they back reviewer routing, search, similarity, version
-- diffs and human-readable export.

CREATE TABLE question_service.question (
    id                      UUID NOT NULL,
    tenant_id               VARCHAR(255) NOT NULL,
    subject_id              BIGINT NOT NULL,
    topic_id                BIGINT NOT NULL,
    subtopic_id             BIGINT,
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
CREATE INDEX idx_question_subject_id ON question_service.question(subject_id);
CREATE INDEX idx_question_topic_id ON question_service.question(topic_id);
CREATE INDEX idx_question_subtopic_id ON question_service.question(subtopic_id);
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
-- Seed Data
-- ------------------------------------------------------------
-- Numeric ids are assigned by the BIGINT IDENTITY columns. Child rows
-- (topic/subtopic) resolve their parent id via a natural-key subquery on
-- (name, tenant_id), so the seed never hard-codes surrogate ids.
-- tenant_id = 'exam-authority-1'
-- ============================================================

-- ============================================================
-- SUBJECTS
-- ============================================================
INSERT INTO question_service.subject (tenant_id, name, code, description) VALUES
('exam-authority-1', 'General Intelligence and Reasoning', 'GIR', 'General Intelligence and Reasoning covering verbal and non-verbal reasoning, analogy, classification, series, coding-decoding, problem solving, spatial visualization, critical thinking, emotional intelligence and social intelligence'),
('exam-authority-1', 'General Awareness', 'GA', 'General awareness covering current events, history, culture, geography, economic scene, general policy, scientific research and India and neighbouring countries'),
('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'QA-MA', 'Quantitative aptitude and mathematical abilities covering number systems, arithmetic, algebra, geometry, mensuration, trigonometry, statistics and probability'),
('exam-authority-1', 'English Language and Comprehension', 'ENG', 'English language and comprehension covering vocabulary, grammar, sentence structure, synonyms, antonyms, error detection, sentence improvement, voice, narration, cloze passages and comprehension passages'),
('exam-authority-1', 'Computer Knowledge', 'CK', 'Computer knowledge covering computer basics, hardware, memory, operating systems, Microsoft Office, internet, email, networking and cyber security'),
('exam-authority-1', 'Statistics', 'STATS', 'Statistics covering collection and presentation of data, measures of central tendency and dispersion, moments, skewness, kurtosis, correlation, regression, probability, sampling, statistical inference, analysis of variance, time series and index numbers'),
('exam-authority-1', 'General Studies', 'GS', 'General Studies covering history, geography, polity, economy, science and current affairs'),
('exam-authority-1', 'Mathematics', 'MATH', 'Mathematics including arithmetic, algebra, geometry, statistics and trigonometry'),
('exam-authority-1', 'English', 'ENG-GEN', 'English language including grammar, comprehension and vocabulary'),
('exam-authority-1', 'Reasoning', 'REAS', 'Logical, analytical and verbal reasoning'),
('exam-authority-1', 'General Science', 'GSCI', 'General science covering physics, chemistry and biology'),
('exam-authority-1', 'Computer Science', 'CS', 'Computer science including programming, databases and networking');

-- ============================================================
-- TOPICS (parent subject resolved by name)
-- ============================================================
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT v.tenant_id, s.id, v.name, v.description
FROM (VALUES
    -- General Intelligence and Reasoning
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Analogy', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Classification', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Series', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Problem Solving', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Coding and Decoding', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Numerical Operations', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Symbolic Operations', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Trends', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Space Orientation', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Space Visualization', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Venn Diagrams', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Drawing Inferences', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Pattern Folding and Unfolding', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Figural Pattern Folding and Completion', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Indexing', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Address Matching', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Date and City Matching', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Embedded Figures', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Critical Thinking', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Emotional Intelligence', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Social Intelligence', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Word Building', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Statement and Conclusion', NULL),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Syllogistic Reasoning', NULL),
    -- General Awareness
    ('exam-authority-1', 'General Awareness', 'Current Events', NULL),
    ('exam-authority-1', 'General Awareness', 'History', NULL),
    ('exam-authority-1', 'General Awareness', 'Culture', NULL),
    ('exam-authority-1', 'General Awareness', 'Geography', NULL),
    ('exam-authority-1', 'General Awareness', 'Economic Scene', NULL),
    ('exam-authority-1', 'General Awareness', 'General Policy', NULL),
    ('exam-authority-1', 'General Awareness', 'Scientific Research', NULL),
    ('exam-authority-1', 'General Awareness', 'Everyday Science', NULL),
    ('exam-authority-1', 'General Awareness', 'India and Neighbouring Countries', NULL),
    -- Quantitative Aptitude / Mathematical Abilities
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', NULL),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', NULL),
    -- English Language and Comprehension
    ('exam-authority-1', 'English Language and Comprehension', 'Vocabulary', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Grammar', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Sentence Structure', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Spot the Error', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Fill in the Blanks', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Synonyms and Homonyms', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Antonyms', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Spellings', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Idioms and Phrases', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'One Word Substitution', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Improvement of Sentences', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Active and Passive Voice', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Direct and Indirect Narration', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Sentence Shuffling', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Cloze Passage', NULL),
    ('exam-authority-1', 'English Language and Comprehension', 'Comprehension Passage', NULL),
    -- Computer Knowledge
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', NULL),
    ('exam-authority-1', 'Computer Knowledge', 'Software', NULL),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', NULL),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', NULL),
    -- Statistics
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', NULL),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', NULL),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', NULL),
    ('exam-authority-1', 'Statistics', 'Moments, Skewness and Kurtosis', NULL),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', NULL),
    ('exam-authority-1', 'Statistics', 'Probability Theory', NULL),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', NULL),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', NULL),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', NULL),
    ('exam-authority-1', 'Statistics', 'Analysis of Variance', NULL),
    ('exam-authority-1', 'Statistics', 'Time Series Analysis', NULL),
    ('exam-authority-1', 'Statistics', 'Index Numbers', NULL),
    -- General Studies
    ('exam-authority-1', 'General Studies', 'Indian History', 'History of India from ancient to modern times'),
    ('exam-authority-1', 'General Studies', 'Indian Geography', 'Physical, economic and human geography of India'),
    ('exam-authority-1', 'General Studies', 'Indian Polity', 'Indian constitution, governance and political system'),
    ('exam-authority-1', 'General Studies', 'Indian Economy', 'Economic planning, banking, trade and fiscal policy'),
    ('exam-authority-1', 'General Studies', 'Science & Technology', 'Scientific developments and technological advancements'),
    ('exam-authority-1', 'General Studies', 'Environment', 'Environmental science, ecology and conservation'),
    ('exam-authority-1', 'General Studies', 'Current Affairs', 'National and international current events'),
    -- Mathematics
    ('exam-authority-1', 'Mathematics', 'Arithmetic', 'Basic arithmetic operations and concepts'),
    ('exam-authority-1', 'Mathematics', 'Algebra', 'Algebraic expressions, equations and operations'),
    ('exam-authority-1', 'Mathematics', 'Geometry', 'Geometric shapes, properties and measurements'),
    ('exam-authority-1', 'Mathematics', 'Statistics', 'Statistical measures and data analysis'),
    ('exam-authority-1', 'Mathematics', 'Trigonometry', 'Trigonometric ratios, identities and applications'),
    -- English (general)
    ('exam-authority-1', 'English', 'Grammar', 'English grammar rules and usage'),
    ('exam-authority-1', 'English', 'Comprehension', 'Reading comprehension and passage analysis'),
    ('exam-authority-1', 'English', 'Vocabulary', 'Word knowledge and usage'),
    -- Reasoning (general)
    ('exam-authority-1', 'Reasoning', 'Logical Reasoning', 'Logic-based problem solving'),
    ('exam-authority-1', 'Reasoning', 'Analytical Reasoning', 'Analysis and pattern recognition'),
    ('exam-authority-1', 'Reasoning', 'Verbal Reasoning', 'Verbal logic and inference'),
    -- General Science
    ('exam-authority-1', 'General Science', 'Physics', 'Fundamental physics concepts'),
    ('exam-authority-1', 'General Science', 'Chemistry', 'Chemistry concepts and applications'),
    ('exam-authority-1', 'General Science', 'Biology', 'Biological sciences'),
    -- Computer Science (general)
    ('exam-authority-1', 'Computer Science', 'Programming', 'Programming concepts and paradigms'),
    ('exam-authority-1', 'Computer Science', 'Databases', 'Database management concepts'),
    ('exam-authority-1', 'Computer Science', 'Networking', 'Computer networking fundamentals')
) AS v(tenant_id, subject_name, name, description)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id;

-- ============================================================
-- SUBTOPICS (parent topic resolved by (subject_name, topic_name))
-- A subtopic's topic name is unique within its subject, so we join on
-- both to disambiguate topic names that repeat across subjects
-- (e.g. "Algebra", "Geometry", "Vocabulary", "Grammar").
-- ============================================================
INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
SELECT v.tenant_id, t.id, v.name, NULL
FROM (VALUES
    -- Reasoning subtopics
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Analogy', 'Semantic Analogy'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Analogy', 'Symbolic / Number Analogy'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Analogy', 'Figural Analogy'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Classification', 'Semantic Classification'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Classification', 'Symbolic / Number Classification'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Classification', 'Figural Classification'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Series', 'Semantic Series'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Series', 'Number Series'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Series', 'Figural Series'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Coding and Decoding', 'Coding'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Coding and Decoding', 'Decoding'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Pattern Folding and Unfolding', 'Punched Hole Pattern Folding and Unfolding'),
    ('exam-authority-1', 'General Intelligence and Reasoning', 'Embedded Figures', 'Embedded Figures'),
    -- Quantitative Aptitude subtopics
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Whole Numbers'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Decimals'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Fractions'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Relationship Between Numbers'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Percentages'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Ratio and Proportion'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Square Roots'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Averages'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Simple Interest'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Compound Interest'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Profit and Loss'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Discount'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Partnership Business'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Mixture and Alligation'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Time and Distance'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Time and Work'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Algebraic Identities'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Elementary Surds'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Graphs of Linear Equations'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Triangles'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Congruence and Similarity'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Circles'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Chords and Tangents'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Triangle'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Quadrilaterals'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Regular Polygons'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Circle'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Prism'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Circular Cone'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Circular Cylinder'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Sphere'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Hemisphere'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Rectangular Parallelepiped'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Regular Right Pyramid'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Trigonometric Ratios'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Complementary Angles'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Heights and Distances'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Standard Identities'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Histogram'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Frequency Polygon'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Bar Diagram'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Pie Chart'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Mean'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Median'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Mode'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Standard Deviation'),
    ('exam-authority-1', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Simple Probability'),
    -- English subtopics
    ('exam-authority-1', 'English Language and Comprehension', 'Vocabulary', 'Synonyms'),
    ('exam-authority-1', 'English Language and Comprehension', 'Vocabulary', 'Antonyms'),
    ('exam-authority-1', 'English Language and Comprehension', 'Vocabulary', 'Homonyms'),
    ('exam-authority-1', 'English Language and Comprehension', 'Vocabulary', 'Vocabulary Usage'),
    ('exam-authority-1', 'English Language and Comprehension', 'Grammar', 'Grammar Rules'),
    ('exam-authority-1', 'English Language and Comprehension', 'Grammar', 'Verb Usage'),
    ('exam-authority-1', 'English Language and Comprehension', 'Sentence Shuffling', 'Sentence Parts'),
    ('exam-authority-1', 'English Language and Comprehension', 'Sentence Shuffling', 'Sentence Ordering'),
    ('exam-authority-1', 'English Language and Comprehension', 'Comprehension Passage', 'Literary Passage'),
    ('exam-authority-1', 'English Language and Comprehension', 'Comprehension Passage', 'Current Affairs Passage'),
    ('exam-authority-1', 'English Language and Comprehension', 'Comprehension Passage', 'Report or Editorial Passage'),
    -- Computer Knowledge subtopics
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Computer Organization'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Central Processing Unit'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Input and Output Devices'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Computer Memory'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Memory Organization'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Backup Devices'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Ports'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Windows Explorer'),
    ('exam-authority-1', 'Computer Knowledge', 'Computer Basics', 'Keyboard Shortcuts'),
    ('exam-authority-1', 'Computer Knowledge', 'Software', 'Windows Operating System'),
    ('exam-authority-1', 'Computer Knowledge', 'Software', 'Microsoft Word'),
    ('exam-authority-1', 'Computer Knowledge', 'Software', 'Microsoft Excel'),
    ('exam-authority-1', 'Computer Knowledge', 'Software', 'Microsoft PowerPoint'),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', 'Web Browsing'),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', 'Web Searching'),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', 'Downloading and Uploading'),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', 'E-mail Management'),
    ('exam-authority-1', 'Computer Knowledge', 'Internet and E-mail', 'E-banking'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Networking Devices'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Network Protocols'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Hacking'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Computer Viruses'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Worms'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Trojan'),
    ('exam-authority-1', 'Computer Knowledge', 'Networking and Cyber Security', 'Preventive Security Measures'),
    -- Statistics subtopics
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Primary Data'),
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Secondary Data'),
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Methods of Data Collection'),
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Tabulation'),
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Graphs and Charts'),
    ('exam-authority-1', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Frequency Distributions'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Mean'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Median'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Mode'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Quartiles'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Deciles'),
    ('exam-authority-1', 'Statistics', 'Measures of Central Tendency', 'Percentiles'),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', 'Range'),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', 'Quartile Deviation'),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', 'Mean Deviation'),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', 'Standard Deviation'),
    ('exam-authority-1', 'Statistics', 'Measures of Dispersion', 'Relative Dispersion'),
    ('exam-authority-1', 'Statistics', 'Moments, Skewness and Kurtosis', 'Moments'),
    ('exam-authority-1', 'Statistics', 'Moments, Skewness and Kurtosis', 'Skewness'),
    ('exam-authority-1', 'Statistics', 'Moments, Skewness and Kurtosis', 'Kurtosis'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Scatter Diagram'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Simple Correlation Coefficient'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Regression Lines'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Spearman Rank Correlation'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Association of Attributes'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Multiple Regression'),
    ('exam-authority-1', 'Statistics', 'Correlation and Regression', 'Multiple and Partial Correlation'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Meaning of Probability'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Definitions of Probability'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Conditional Probability'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Compound Probability'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Independent Events'),
    ('exam-authority-1', 'Statistics', 'Probability Theory', 'Bayes Theorem'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Random Variables'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Probability Functions'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Expectation and Variance'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Higher Moments'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Binomial Distribution'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Poisson Distribution'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Normal Distribution'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Exponential Distribution'),
    ('exam-authority-1', 'Statistics', 'Random Variable and Probability Distributions', 'Joint Distribution'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Population and Sample'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Parameter and Statistic'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Sampling Errors'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Probability Sampling'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Non-Probability Sampling'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Sampling Distribution'),
    ('exam-authority-1', 'Statistics', 'Sampling Theory', 'Sample Size Decisions'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Point Estimation'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Interval Estimation'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Properties of Good Estimator'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Method of Moments'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Maximum Likelihood'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Least Squares'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Hypothesis Testing'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Z Test'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 't Test'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Chi-Square Test'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'F Test'),
    ('exam-authority-1', 'Statistics', 'Statistical Inference', 'Confidence Intervals'),
    ('exam-authority-1', 'Statistics', 'Analysis of Variance', 'One-Way ANOVA'),
    ('exam-authority-1', 'Statistics', 'Analysis of Variance', 'Two-Way ANOVA'),
    ('exam-authority-1', 'Statistics', 'Time Series Analysis', 'Components of Time Series'),
    ('exam-authority-1', 'Statistics', 'Time Series Analysis', 'Trend Component'),
    ('exam-authority-1', 'Statistics', 'Time Series Analysis', 'Seasonal Variation'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Meaning of Index Numbers'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Construction of Index Numbers'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Types of Index Numbers'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Index Number Formulae'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Base Shifting and Splicing'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Cost of Living Index Numbers'),
    ('exam-authority-1', 'Statistics', 'Index Numbers', 'Uses of Index Numbers'),
    -- General Studies subtopics
    ('exam-authority-1', 'General Studies', 'Indian History', 'Ancient India'),
    ('exam-authority-1', 'General Studies', 'Indian History', 'Medieval India'),
    ('exam-authority-1', 'General Studies', 'Indian History', 'Modern India'),
    ('exam-authority-1', 'General Studies', 'Indian Polity', 'Constitution'),
    ('exam-authority-1', 'General Studies', 'Indian Polity', 'Fundamental Rights'),
    -- Mathematics subtopics
    ('exam-authority-1', 'Mathematics', 'Arithmetic', 'Number System'),
    ('exam-authority-1', 'Mathematics', 'Arithmetic', 'Percentage'),
    ('exam-authority-1', 'Mathematics', 'Algebra', 'Linear Equations'),
    ('exam-authority-1', 'Mathematics', 'Algebra', 'Quadratic Equations'),
    -- General Science subtopics
    ('exam-authority-1', 'General Science', 'Physics', 'Mechanics'),
    ('exam-authority-1', 'General Science', 'Physics', 'Optics'),
    ('exam-authority-1', 'General Science', 'Chemistry', 'Organic'),
    -- Computer Science subtopics
    ('exam-authority-1', 'Computer Science', 'Programming', 'Data Structures'),
    ('exam-authority-1', 'Computer Science', 'Programming', 'Algorithms')
) AS v(tenant_id, subject_name, topic_name, name)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = v.tenant_id;
