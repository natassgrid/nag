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
-- Table: batch_generation_job
-- Batch generation job tracking table.
-- A single job aggregates multiple generation items into one Bedrock batch inference call.
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
-- Seed Data
-- ------------------------------------------------------------
-- Numeric ids are assigned by the BIGINT IDENTITY columns. Child rows
-- (topic/subtopic) resolve their parent id via a natural-key subquery on
-- (name, tenant_id), so the seed never hard-codes surrogate ids.
-- tenant_id = 'default'
-- ============================================================

-- ============================================================
-- SUBJECTS
-- ============================================================
INSERT INTO question_service.subject (tenant_id, name, code, description) VALUES
('default', 'General Intelligence and Reasoning', 'GIR', 'General Intelligence and Reasoning covering verbal and non-verbal reasoning, analogy, classification, series, coding-decoding, problem solving, spatial visualization, critical thinking, emotional intelligence and social intelligence'),
('default', 'General Awareness', 'GA', 'General awareness covering current events, history, culture, geography, economic scene, general policy, scientific research and India and neighbouring countries'),
('default', 'Quantitative Aptitude / Mathematical Abilities', 'QA-MA', 'Quantitative aptitude and mathematical abilities covering number systems, arithmetic, algebra, geometry, mensuration, trigonometry, statistics and probability'),
('default', 'English Language and Comprehension', 'ENG', 'English language and comprehension covering vocabulary, grammar, sentence structure, synonyms, antonyms, error detection, sentence improvement, voice, narration, cloze passages and comprehension passages'),
('default', 'Computer Knowledge', 'CK', 'Computer knowledge covering computer basics, hardware, memory, operating systems, Microsoft Office, internet, email, networking and cyber security'),
('default', 'Statistics', 'STATS', 'Statistics covering collection and presentation of data, measures of central tendency and dispersion, moments, skewness, kurtosis, correlation, regression, probability, sampling, statistical inference, analysis of variance, time series and index numbers'),
('default', 'General Studies', 'GS', 'General Studies covering history, geography, polity, economy, science and current affairs'),
('default', 'Mathematics', 'MATH', 'Mathematics including arithmetic, algebra, geometry, statistics and trigonometry'),
('default', 'English', 'ENG-GEN', 'English language including grammar, comprehension and vocabulary'),
('default', 'Reasoning', 'REAS', 'Logical, analytical and verbal reasoning'),
('default', 'General Science', 'GSCI', 'General science covering physics, chemistry and biology'),
('default', 'Computer Science', 'CS', 'Computer science including programming, databases and networking'),
('default', 'Banking and Financial Awareness', 'BFA', 'Banking principles, financial systems, RBI regulations, monetary policy, financial inclusion, digital banking, and government economic schemes'),
('default', 'Data Interpretation and Logical Analysis', 'DILA', 'Data interpretation across tabular data, line and bar charts, pie charts, caselets, radar charts, data sufficiency, and analytical reasoning'),
('default', 'General Hindi', 'HND', 'General Hindi covering grammar, sandhi, samas, synonyms, antonyms, idioms, proverbs, error detection, sentence correction, and comprehension passages'),
('default', 'Public Administration and Governance', 'PAG', 'Administrative theories, Indian administrative structure, constitutional and statutory bodies, public policy, e-governance, and civil services ethics'),
('default', 'Environmental Ecology and Biodiversity', 'EEB', 'Ecology, biodiversity conservation, climate change, international conventions, wildlife protection, and pollution management'),
('default', 'Child Development and Pedagogy', 'CDP', 'Child development, learning theories, inclusive education, pedagogy of school subjects, curriculum design, continuous and comprehensive evaluation (CCE)')
ON CONFLICT (name, tenant_id) DO NOTHING;

-- ============================================================
-- TOPICS (parent subject resolved by name)
-- ============================================================
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT v.tenant_id, s.id, v.name, v.description
FROM (VALUES
    -- General Intelligence and Reasoning
    ('default', 'General Intelligence and Reasoning', 'Analogy', NULL),
    ('default', 'General Intelligence and Reasoning', 'Classification', NULL),
    ('default', 'General Intelligence and Reasoning', 'Series', NULL),
    ('default', 'General Intelligence and Reasoning', 'Problem Solving', NULL),
    ('default', 'General Intelligence and Reasoning', 'Coding and Decoding', NULL),
    ('default', 'General Intelligence and Reasoning', 'Numerical Operations', NULL),
    ('default', 'General Intelligence and Reasoning', 'Symbolic Operations', NULL),
    ('default', 'General Intelligence and Reasoning', 'Trends', NULL),
    ('default', 'General Intelligence and Reasoning', 'Space Orientation', NULL),
    ('default', 'General Intelligence and Reasoning', 'Space Visualization', NULL),
    ('default', 'General Intelligence and Reasoning', 'Venn Diagrams', NULL),
    ('default', 'General Intelligence and Reasoning', 'Drawing Inferences', NULL),
    ('default', 'General Intelligence and Reasoning', 'Pattern Folding and Unfolding', NULL),
    ('default', 'General Intelligence and Reasoning', 'Figural Pattern Folding and Completion', NULL),
    ('default', 'General Intelligence and Reasoning', 'Indexing', NULL),
    ('default', 'General Intelligence and Reasoning', 'Address Matching', NULL),
    ('default', 'General Intelligence and Reasoning', 'Date and City Matching', NULL),
    ('default', 'General Intelligence and Reasoning', 'Embedded Figures', NULL),
    ('default', 'General Intelligence and Reasoning', 'Critical Thinking', NULL),
    ('default', 'General Intelligence and Reasoning', 'Emotional Intelligence', NULL),
    ('default', 'General Intelligence and Reasoning', 'Social Intelligence', NULL),
    ('default', 'General Intelligence and Reasoning', 'Word Building', NULL),
    ('default', 'General Intelligence and Reasoning', 'Statement and Conclusion', NULL),
    ('default', 'General Intelligence and Reasoning', 'Syllogistic Reasoning', NULL),
    -- General Awareness
    ('default', 'General Awareness', 'Current Events', NULL),
    ('default', 'General Awareness', 'History', NULL),
    ('default', 'General Awareness', 'Culture', NULL),
    ('default', 'General Awareness', 'Geography', NULL),
    ('default', 'General Awareness', 'Economic Scene', NULL),
    ('default', 'General Awareness', 'General Policy', NULL),
    ('default', 'General Awareness', 'Scientific Research', NULL),
    ('default', 'General Awareness', 'Everyday Science', NULL),
    ('default', 'General Awareness', 'India and Neighbouring Countries', NULL),
    -- Quantitative Aptitude / Mathematical Abilities
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', NULL),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', NULL),
    -- English Language and Comprehension
    ('default', 'English Language and Comprehension', 'Vocabulary', NULL),
    ('default', 'English Language and Comprehension', 'Grammar', NULL),
    ('default', 'English Language and Comprehension', 'Sentence Structure', NULL),
    ('default', 'English Language and Comprehension', 'Spot the Error', NULL),
    ('default', 'English Language and Comprehension', 'Fill in the Blanks', NULL),
    ('default', 'English Language and Comprehension', 'Synonyms and Homonyms', NULL),
    ('default', 'English Language and Comprehension', 'Antonyms', NULL),
    ('default', 'English Language and Comprehension', 'Spellings', NULL),
    ('default', 'English Language and Comprehension', 'Idioms and Phrases', NULL),
    ('default', 'English Language and Comprehension', 'One Word Substitution', NULL),
    ('default', 'English Language and Comprehension', 'Improvement of Sentences', NULL),
    ('default', 'English Language and Comprehension', 'Active and Passive Voice', NULL),
    ('default', 'English Language and Comprehension', 'Direct and Indirect Narration', NULL),
    ('default', 'English Language and Comprehension', 'Sentence Shuffling', NULL),
    ('default', 'English Language and Comprehension', 'Cloze Passage', NULL),
    ('default', 'English Language and Comprehension', 'Comprehension Passage', NULL),
    -- Computer Knowledge
    ('default', 'Computer Knowledge', 'Computer Basics', NULL),
    ('default', 'Computer Knowledge', 'Software', NULL),
    ('default', 'Computer Knowledge', 'Internet and E-mail', NULL),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', NULL),
    -- Statistics
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', NULL),
    ('default', 'Statistics', 'Measures of Central Tendency', NULL),
    ('default', 'Statistics', 'Measures of Dispersion', NULL),
    ('default', 'Statistics', 'Moments, Skewness and Kurtosis', NULL),
    ('default', 'Statistics', 'Correlation and Regression', NULL),
    ('default', 'Statistics', 'Probability Theory', NULL),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', NULL),
    ('default', 'Statistics', 'Sampling Theory', NULL),
    ('default', 'Statistics', 'Statistical Inference', NULL),
    ('default', 'Statistics', 'Analysis of Variance', NULL),
    ('default', 'Statistics', 'Time Series Analysis', NULL),
    ('default', 'Statistics', 'Index Numbers', NULL),
    -- General Studies
    ('default', 'General Studies', 'Indian History', 'History of India from ancient to modern times'),
    ('default', 'General Studies', 'Indian Geography', 'Physical, economic and human geography of India'),
    ('default', 'General Studies', 'Indian Polity', 'Indian constitution, governance and political system'),
    ('default', 'General Studies', 'Indian Economy', 'Economic planning, banking, trade and fiscal policy'),
    ('default', 'General Studies', 'Science & Technology', 'Scientific developments and technological advancements'),
    ('default', 'General Studies', 'Environment', 'Environmental science, ecology and conservation'),
    ('default', 'General Studies', 'Current Affairs', 'National and international current events'),
    -- Mathematics
    ('default', 'Mathematics', 'Arithmetic', 'Basic arithmetic operations and concepts'),
    ('default', 'Mathematics', 'Algebra', 'Algebraic expressions, equations and operations'),
    ('default', 'Mathematics', 'Geometry', 'Geometric shapes, properties and measurements'),
    ('default', 'Mathematics', 'Statistics', 'Statistical measures and data analysis'),
    ('default', 'Mathematics', 'Trigonometry', 'Trigonometric ratios, identities and applications'),
    -- English (general)
    ('default', 'English', 'Grammar', 'English grammar rules and usage'),
    ('default', 'English', 'Comprehension', 'Reading comprehension and passage analysis'),
    ('default', 'English', 'Vocabulary', 'Word knowledge and usage'),
    -- Reasoning (general)
    ('default', 'Reasoning', 'Logical Reasoning', 'Logic-based problem solving'),
    ('default', 'Reasoning', 'Analytical Reasoning', 'Analysis and pattern recognition'),
    ('default', 'Reasoning', 'Verbal Reasoning', 'Verbal logic and inference'),
    -- General Science
    ('default', 'General Science', 'Physics', 'Fundamental physics concepts'),
    ('default', 'General Science', 'Chemistry', 'Chemistry concepts and applications'),
    ('default', 'General Science', 'Biology', 'Biological sciences'),
    -- Computer Science (general)
    ('default', 'Computer Science', 'Programming', 'Programming concepts and paradigms'),
    ('default', 'Computer Science', 'Databases', 'Database management concepts'),
    ('default', 'Computer Science', 'Networking', 'Computer networking fundamentals'),

    -- Banking and Financial Awareness
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Structure of Indian commercial and cooperative banks, RBI powers and monetary tools'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Repo rate, reverse repo, CRR, SLR, inflation indices (CPI, WPI) and monetary control'),
    ('default', 'Banking and Financial Awareness', 'Financial Markets & Capital Instruments', 'Money market, capital market, treasury bills, commercial papers, SEBI regulations and mutual funds'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'NPCI, UPI, RTGS, NEFT, IMPS, CBDC / Digital Rupee, cybersecurity in electronic transactions'),
    ('default', 'Banking and Financial Awareness', 'Government Financial Schemes & Inclusions', 'PM Jan Dhan Yojana, PMJJBY, PMSBY, APY, Mudra loans, Stand-Up India and priority sector lending'),
    ('default', 'Banking and Financial Awareness', 'Non-Performing Assets & Risk Management', 'NPA classification, SARFAESI Act, Insolvency and Bankruptcy Code (IBC), Basel III norms, and PCA framework'),

    -- Data Interpretation and Logical Analysis
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Data extraction, comparison, calculation and trend analysis from single/multi-column tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Bar Graphs and Histograms', 'Analysis and calculations based on simple, grouped, and stacked bar diagrams'),
    ('default', 'Data Interpretation and Logical Analysis', 'Line Graphs and Radar Charts', 'Time series line plots, multiple line comparisons, and radar spider charts'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Single and dual pie charts, degrees-to-percentage conversions, combined table-chart sets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Paragraph-based data sets, Venn diagram based sets, and arithmetic concept-based DI'),
    ('default', 'Data Interpretation and Logical Analysis', 'Data Sufficiency', 'Determining sufficiency of statements to answer algebraic, geometric and arithmetic queries'),

    -- General Hindi
    ('default', 'General Hindi', 'वर्ण विचार एवं वर्तनी शुद्धि', 'स्वर, व्यंजन, उच्चारण स्थान, और वर्तनी की अशुद्धियों का संशोधन'),
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'स्वर संधि, व्यंजन संधि और विसर्ग संधि के नियम एवं विच्छेद'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'तत्पुरुष, कर्मधारय, द्विगु, द्वन्द्व, बहुव्रीहि और अव्ययीभाव समास'),
    ('default', 'General Hindi', 'विलोम एवं पर्यायवाची शब्द', 'समानार्थी एवं विपरीतार्थक शब्द और उनका वाक्यों में उचित प्रयोग'),
    ('default', 'General Hindi', 'मुहावरे एवं लोकोक्तियाँ', 'प्रचलित मुहावरों, कहावतों के अर्थ और संदर्भानुसार प्रयोग'),
    ('default', 'General Hindi', 'अनेक शब्दों के लिए एक शब्द', 'संक्षिप्त अभिव्यक्ति एवं एकल शब्द चयन'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'लिंग, वचन, कारक, काल और पदक्रम संबंधी त्रुटि सुधार'),
    ('default', 'General Hindi', 'अपठित गद्यांश', 'गद्यांश आधारित बोधगम्यता, शीर्षक चयन एवं व्याख्यात्मक प्रश्न'),

    -- Public Administration and Governance
    ('default', 'Public Administration and Governance', 'Administrative Theories & Evolution', 'Scientific management, bureaucracy, human relations, new public management, and good governance'),
    ('default', 'Public Administration and Governance', 'Union and State Administrative Machinery', 'Cabinet Secretariat, PMO, Ministries, Chief Secretariat, and District Administration'),
    ('default', 'Public Administration and Governance', 'Panchayati Raj & Local Self-Government', '73rd and 74th Constitutional Amendment Acts, PESA Act, and urban/rural local body functions'),
    ('default', 'Public Administration and Governance', 'Public Policy & Citizen Centricity', 'Policy formulation, implementation evaluation, Citizen Charters, RTI Act, and Lokpal/Lokayukta'),
    ('default', 'Public Administration and Governance', 'E-Governance & Digital Public Infrastructure', 'Digital India initiatives, direct benefit transfer (DBT), UMANG, DigiLocker, and cybersecurity policy'),

    -- Environmental Ecology and Biodiversity
    ('default', 'Environmental Ecology and Biodiversity', 'Ecosystem Dynamics & Biomes', 'Food chains, trophic levels, energy flow, ecological pyramids, biomes, and biogeochemical cycles'),
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'Protected Area Network, National Parks, Wildlife Sanctuaries, Biosphere Reserves, Ramsar Wetlands, and IUCN Red List'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'UNFCCC, Kyoto Protocol, Paris Agreement, COP summits, IPCC reports, and carbon credits/markets'),
    ('default', 'Environmental Ecology and Biodiversity', 'Environmental Pollution & Waste Management', 'Air, water, soil, plastic and electronic waste management rules, and National Green Tribunal (NGT) acts'),

    -- Child Development and Pedagogy
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Piaget, Vygotsky, Kohlberg theories of cognitive and moral development, maturation, and individual differences'),
    ('default', 'Child Development and Pedagogy', 'Inclusive Education & Special Needs', 'Addressing learners from diverse backgrounds, learning disabilities (Dyslexia, ADHD), and gifted children'),
    ('default', 'Child Development and Pedagogy', 'Teaching-Learning Strategies & Pedagogy', 'Constructivist approach, problem solving, inquiry learning, motivation, and learning transfer'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'Formative and summative assessment, Continuous and Comprehensive Evaluation, rubrics, and diagnostic testing'),

    -- Additional Topics for General Studies (Enriching GS for UPSC / State PSC)
    ('default', 'General Studies', 'Indian Art & Culture', 'Classical dance, music, temple architecture, literature, festivals and UNESCO World Heritage Sites'),
    ('default', 'General Studies', 'International Relations & Global Bodies', 'United Nations, G20, BRICS, SCO, ASEAN, WTO, IMF, World Bank, and India bilateral relations'),
    ('default', 'General Studies', 'Internal Security & Disaster Management', 'Border management, cyber warfare, money laundering, NDMA guidelines, and disaster mitigation frameworks'),
    ('default', 'General Studies', 'Ethics, Integrity & Aptitude', 'Moral philosophy, public service values, emotional intelligence, attitude, case studies in administrative ethics')
) AS v(tenant_id, subject_name, name, description)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

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
    ('default', 'General Intelligence and Reasoning', 'Analogy', 'Semantic Analogy'),
    ('default', 'General Intelligence and Reasoning', 'Analogy', 'Symbolic / Number Analogy'),
    ('default', 'General Intelligence and Reasoning', 'Analogy', 'Figural Analogy'),
    ('default', 'General Intelligence and Reasoning', 'Classification', 'Semantic Classification'),
    ('default', 'General Intelligence and Reasoning', 'Classification', 'Symbolic / Number Classification'),
    ('default', 'General Intelligence and Reasoning', 'Classification', 'Figural Classification'),
    ('default', 'General Intelligence and Reasoning', 'Series', 'Semantic Series'),
    ('default', 'General Intelligence and Reasoning', 'Series', 'Number Series'),
    ('default', 'General Intelligence and Reasoning', 'Series', 'Figural Series'),
    ('default', 'General Intelligence and Reasoning', 'Coding and Decoding', 'Coding'),
    ('default', 'General Intelligence and Reasoning', 'Coding and Decoding', 'Decoding'),
    ('default', 'General Intelligence and Reasoning', 'Pattern Folding and Unfolding', 'Punched Hole Pattern Folding and Unfolding'),
    ('default', 'General Intelligence and Reasoning', 'Embedded Figures', 'Embedded Figures'),
    -- Quantitative Aptitude subtopics
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Whole Numbers'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Decimals'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Fractions'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Number Systems', 'Relationship Between Numbers'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Percentages'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Ratio and Proportion'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Square Roots'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Averages'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Simple Interest'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Compound Interest'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Profit and Loss'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Discount'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Partnership Business'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Mixture and Alligation'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Time and Distance'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Fundamental Arithmetical Operations', 'Time and Work'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Algebraic Identities'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Elementary Surds'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Algebra', 'Graphs of Linear Equations'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Triangles'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Congruence and Similarity'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Circles'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Geometry', 'Chords and Tangents'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Triangle'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Quadrilaterals'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Regular Polygons'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Circle'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Prism'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Circular Cone'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Right Circular Cylinder'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Sphere'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Hemisphere'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Rectangular Parallelepiped'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Mensuration', 'Regular Right Pyramid'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Trigonometric Ratios'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Complementary Angles'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Heights and Distances'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Trigonometry', 'Standard Identities'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Histogram'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Frequency Polygon'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Bar Diagram'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Pie Chart'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Mean'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Median'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Mode'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Standard Deviation'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'Statistics and Probability', 'Simple Probability'),
    -- English subtopics
    ('default', 'English Language and Comprehension', 'Vocabulary', 'Synonyms'),
    ('default', 'English Language and Comprehension', 'Vocabulary', 'Antonyms'),
    ('default', 'English Language and Comprehension', 'Vocabulary', 'Homonyms'),
    ('default', 'English Language and Comprehension', 'Vocabulary', 'Vocabulary Usage'),
    ('default', 'English Language and Comprehension', 'Grammar', 'Grammar Rules'),
    ('default', 'English Language and Comprehension', 'Grammar', 'Verb Usage'),
    ('default', 'English Language and Comprehension', 'Sentence Shuffling', 'Sentence Parts'),
    ('default', 'English Language and Comprehension', 'Sentence Shuffling', 'Sentence Ordering'),
    ('default', 'English Language and Comprehension', 'Comprehension Passage', 'Literary Passage'),
    ('default', 'English Language and Comprehension', 'Comprehension Passage', 'Current Affairs Passage'),
    ('default', 'English Language and Comprehension', 'Comprehension Passage', 'Report or Editorial Passage'),
    -- Computer Knowledge subtopics
    ('default', 'Computer Knowledge', 'Computer Basics', 'Computer Organization'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Central Processing Unit'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Input and Output Devices'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Computer Memory'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Memory Organization'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Backup Devices'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Ports'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Windows Explorer'),
    ('default', 'Computer Knowledge', 'Computer Basics', 'Keyboard Shortcuts'),
    ('default', 'Computer Knowledge', 'Software', 'Windows Operating System'),
    ('default', 'Computer Knowledge', 'Software', 'Microsoft Word'),
    ('default', 'Computer Knowledge', 'Software', 'Microsoft Excel'),
    ('default', 'Computer Knowledge', 'Software', 'Microsoft PowerPoint'),
    ('default', 'Computer Knowledge', 'Internet and E-mail', 'Web Browsing'),
    ('default', 'Computer Knowledge', 'Internet and E-mail', 'Web Searching'),
    ('default', 'Computer Knowledge', 'Internet and E-mail', 'Downloading and Uploading'),
    ('default', 'Computer Knowledge', 'Internet and E-mail', 'E-mail Management'),
    ('default', 'Computer Knowledge', 'Internet and E-mail', 'E-banking'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Networking Devices'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Network Protocols'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Hacking'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Computer Viruses'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Worms'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Trojan'),
    ('default', 'Computer Knowledge', 'Networking and Cyber Security', 'Preventive Security Measures'),
    -- Statistics subtopics
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Primary Data'),
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Secondary Data'),
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Methods of Data Collection'),
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Tabulation'),
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Graphs and Charts'),
    ('default', 'Statistics', 'Collection, Classification and Presentation of Statistical Data', 'Frequency Distributions'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Mean'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Median'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Mode'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Quartiles'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Deciles'),
    ('default', 'Statistics', 'Measures of Central Tendency', 'Percentiles'),
    ('default', 'Statistics', 'Measures of Dispersion', 'Range'),
    ('default', 'Statistics', 'Measures of Dispersion', 'Quartile Deviation'),
    ('default', 'Statistics', 'Measures of Dispersion', 'Mean Deviation'),
    ('default', 'Statistics', 'Measures of Dispersion', 'Standard Deviation'),
    ('default', 'Statistics', 'Measures of Dispersion', 'Relative Dispersion'),
    ('default', 'Statistics', 'Moments, Skewness and Kurtosis', 'Moments'),
    ('default', 'Statistics', 'Moments, Skewness and Kurtosis', 'Skewness'),
    ('default', 'Statistics', 'Moments, Skewness and Kurtosis', 'Kurtosis'),
    ('default', 'Statistics', 'Correlation and Regression', 'Scatter Diagram'),
    ('default', 'Statistics', 'Correlation and Regression', 'Simple Correlation Coefficient'),
    ('default', 'Statistics', 'Correlation and Regression', 'Regression Lines'),
    ('default', 'Statistics', 'Correlation and Regression', 'Spearman Rank Correlation'),
    ('default', 'Statistics', 'Correlation and Regression', 'Association of Attributes'),
    ('default', 'Statistics', 'Correlation and Regression', 'Multiple Regression'),
    ('default', 'Statistics', 'Correlation and Regression', 'Multiple and Partial Correlation'),
    ('default', 'Statistics', 'Probability Theory', 'Meaning of Probability'),
    ('default', 'Statistics', 'Probability Theory', 'Definitions of Probability'),
    ('default', 'Statistics', 'Probability Theory', 'Conditional Probability'),
    ('default', 'Statistics', 'Probability Theory', 'Compound Probability'),
    ('default', 'Statistics', 'Probability Theory', 'Independent Events'),
    ('default', 'Statistics', 'Probability Theory', 'Bayes Theorem'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Random Variables'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Probability Functions'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Expectation and Variance'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Higher Moments'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Binomial Distribution'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Poisson Distribution'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Normal Distribution'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Exponential Distribution'),
    ('default', 'Statistics', 'Random Variable and Probability Distributions', 'Joint Distribution'),
    ('default', 'Statistics', 'Sampling Theory', 'Population and Sample'),
    ('default', 'Statistics', 'Sampling Theory', 'Parameter and Statistic'),
    ('default', 'Statistics', 'Sampling Theory', 'Sampling Errors'),
    ('default', 'Statistics', 'Sampling Theory', 'Probability Sampling'),
    ('default', 'Statistics', 'Sampling Theory', 'Non-Probability Sampling'),
    ('default', 'Statistics', 'Sampling Theory', 'Sampling Distribution'),
    ('default', 'Statistics', 'Sampling Theory', 'Sample Size Decisions'),
    ('default', 'Statistics', 'Statistical Inference', 'Point Estimation'),
    ('default', 'Statistics', 'Statistical Inference', 'Interval Estimation'),
    ('default', 'Statistics', 'Statistical Inference', 'Properties of Good Estimator'),
    ('default', 'Statistics', 'Statistical Inference', 'Method of Moments'),
    ('default', 'Statistics', 'Statistical Inference', 'Maximum Likelihood'),
    ('default', 'Statistics', 'Statistical Inference', 'Least Squares'),
    ('default', 'Statistics', 'Statistical Inference', 'Hypothesis Testing'),
    ('default', 'Statistics', 'Statistical Inference', 'Z Test'),
    ('default', 'Statistics', 'Statistical Inference', 't Test'),
    ('default', 'Statistics', 'Statistical Inference', 'Chi-Square Test'),
    ('default', 'Statistics', 'Statistical Inference', 'F Test'),
    ('default', 'Statistics', 'Statistical Inference', 'Confidence Intervals'),
    ('default', 'Statistics', 'Analysis of Variance', 'One-Way ANOVA'),
    ('default', 'Statistics', 'Analysis of Variance', 'Two-Way ANOVA'),
    ('default', 'Statistics', 'Time Series Analysis', 'Components of Time Series'),
    ('default', 'Statistics', 'Time Series Analysis', 'Trend Component'),
    ('default', 'Statistics', 'Time Series Analysis', 'Seasonal Variation'),
    ('default', 'Statistics', 'Index Numbers', 'Meaning of Index Numbers'),
    ('default', 'Statistics', 'Index Numbers', 'Construction of Index Numbers'),
    ('default', 'Statistics', 'Index Numbers', 'Types of Index Numbers'),
    ('default', 'Statistics', 'Index Numbers', 'Index Number Formulae'),
    ('default', 'Statistics', 'Index Numbers', 'Base Shifting and Splicing'),
    ('default', 'Statistics', 'Index Numbers', 'Cost of Living Index Numbers'),
    ('default', 'Statistics', 'Index Numbers', 'Uses of Index Numbers'),
    -- General Studies subtopics
    ('default', 'General Studies', 'Indian History', 'Ancient India'),
    ('default', 'General Studies', 'Indian History', 'Medieval India'),
    ('default', 'General Studies', 'Indian History', 'Modern India'),
    ('default', 'General Studies', 'Indian Polity', 'Constitution'),
    ('default', 'General Studies', 'Indian Polity', 'Fundamental Rights'),
    -- Mathematics subtopics
    ('default', 'Mathematics', 'Arithmetic', 'Number System'),
    ('default', 'Mathematics', 'Arithmetic', 'Percentage'),
    ('default', 'Mathematics', 'Algebra', 'Linear Equations'),
    ('default', 'Mathematics', 'Algebra', 'Quadratic Equations'),
    -- General Science subtopics
    ('default', 'General Science', 'Physics', 'Mechanics'),
    ('default', 'General Science', 'Physics', 'Optics'),
    ('default', 'General Science', 'Chemistry', 'Organic'),
    -- Computer Science subtopics
    ('default', 'Computer Science', 'Programming', 'Data Structures'),
    ('default', 'Computer Science', 'Programming', 'Algorithms'),

    -- Banking subtopics
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Commercial Banks & Payments Banks'),
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Small Finance Banks & RRBs'),
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Reserve Bank of India Constitution & Functions'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Quantitative Monetary Instruments'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Qualitative Credit Controls'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'CPI & WPI Trends'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'Unified Payments Interface (UPI) & IMPS'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'Central Bank Digital Currency (CBDC)'),

    -- Data Interpretation subtopics
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Multi-Variable Tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Missing Data Tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Single & Dual Pie Charts'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Combined Table & Graph Sets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Profit & Loss and SI/CI Caselets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Time & Work and Speed-Time Caselets'),

    -- General Hindi subtopics
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'स्वर संधि भेद (दीर्घ, गुण, वृद्धि, यण, अयादि)'),
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'व्यंजन संधि एवं विसर्ग संधि'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'तत्पुरुष एवं कर्मधारय समास'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'द्विगु, द्वन्द्व एवं बहुव्रीहि समास'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'कारक एवं वचन संबंधी अशुद्धियाँ'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'पदक्रम एवं मुहावरे संबंधी वाक्य दोष'),

    -- Ecology & Environment subtopics
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'National Parks & Wildlife Sanctuaries of India'),
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'Biosphere Reserves & Ramsar Sites in India'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'Paris Agreement & Nationally Determined Contributions (NDCs)'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'Renewable Energy Targets & National Solar Mission'),

    -- Pedagogy subtopics
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Cognitive Development Theory (Jean Piaget)'),
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Socio-Cultural Learning Theory (Lev Vygotsky)'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'Formative & Summative Evaluation Techniques'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'School-Based Assessment (SBA) & Holistic Report Cards')
) AS v(tenant_id, subject_name, topic_name, name)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = v.tenant_id
ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
