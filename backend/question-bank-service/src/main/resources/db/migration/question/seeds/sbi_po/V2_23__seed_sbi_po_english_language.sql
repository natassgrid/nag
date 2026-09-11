-- SPDX-License-Identifier: AGPL-3.0-only
-- Flyway Migration: V2_23
-- Seed Data: SBI PO / Bank PO English Language (100 Questions)

SET search_path TO question_service, public;

-- Step 1: Ensure Subject, Topics, and Subtopics exist
DO $$
DECLARE
    v_tenant_id VARCHAR := 'default';
    v_subj_id BIGINT;
    v_top_id BIGINT;
BEGIN
    SELECT id INTO v_subj_id FROM question_service.subject WHERE name = 'English Language and Comprehension' AND tenant_id = v_tenant_id LIMIT 1;
    IF v_subj_id IS NULL THEN
        INSERT INTO question_service.subject (tenant_id, name, code, description)
        VALUES (v_tenant_id, 'English Language and Comprehension', UPPER(SUBSTRING('English Language and Comprehension', 1, 6)), 'English Language and Comprehension for Bank PO Examinations')
        RETURNING id INTO v_subj_id;
    END IF;

    -- Topic: Reading Comprehension
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Reading Comprehension' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Reading Comprehension', 'Discursive, financial, and analytical prose passages with inference, vocabulary, and theme analysis')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Financial & Economic Passages', 'Macroeconomic trends, banking digital transformation, and monetary policy analysis')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Inference & Author''s Tone', 'Identifying implicit assumptions, central themes, tone, and contextual synonyms/antonyms')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Cloze Test
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Cloze Test' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Cloze Test', 'Multi-blank thematic passages assessing contextual vocabulary, collocations, and prepositions')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Economic Cloze Passages', 'Passages on global financial stability, capital markets, and fiscal deficits')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Grammatical & Collocation Cloze', 'Targeting phrasal verbs, transition words, and idiomatic syntax')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Error Detection and Spotting
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Error Detection and Spotting' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Error Detection and Spotting', 'Identifying grammatical errors across sentence segments based on advanced syntactical rules')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Subject-Verb & Modifier Errors', 'Inversion, proximity errors, split infinitives, and dangling participles')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Tense, Conditionals & Prepositions', 'Subjunctive mood, third conditionals, and nuanced prepositional usage')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Sentence Improvement and Correction
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Sentence Improvement and Correction' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Sentence Improvement and Correction', 'Replacing awkward or erroneous clauses with concise and grammatically superior alternatives')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Phrase Replacement', 'Correcting idiomatic and syntactical flaws in highlighted clauses')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Structural Parallelism', 'Enforcing parallel construction in coordinate and correlative clauses')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Para Jumbles & Sentence Rearrangement
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Para Jumbles & Sentence Rearrangement' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Para Jumbles & Sentence Rearrangement', 'Sequencing scrambled sentences into coherent analytical editorials and narrative paragraphs')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Editorial Sentence Sequences', 'Reordering 5-to-6 sentence sets discussing public finance and governance')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Sentence Boundary Identification', 'Pinpointing introductory, bridging, and concluding statements')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Fill in the Blanks
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Fill in the Blanks' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Fill in the Blanks', 'Single and double fillers requiring precise semantic and grammatical concordance')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Double Fillers in Banking', 'Sentences requiring pairs of complementary economic vocabulary')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Contextual Word Selection', 'Discerning between near-synonyms based on contextual connotations')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Word Swap & Sentence Reordering
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Word Swap & Sentence Reordering' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Word Swap & Sentence Reordering', 'Exchanging misallocated words across designated sentence positions to restore coherence')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Four-Word Swap', 'Transposing bolded words (A-B-C-D) within a single sentence structure')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Vocabulary Alignment', 'Ensuring part-of-speech and semantic consistency across swapped slots')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Idioms, Phrasal Verbs & Vocabulary
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Idioms, Phrasal Verbs & Vocabulary' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Idioms, Phrasal Verbs & Vocabulary', 'High-frequency banking idioms, financial jargon, and phrasal verb distinctions')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Financial Idioms & Expressions', 'Idiomatic phrases like ''in the red'', ''bear market'', ''quantitative easing''')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Phrasal Verbs & Usage', 'Differentiating ''bail out'', ''write off'', ''call off'', ''phase out''')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Sentence Connectors and Starters
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Sentence Connectors and Starters' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Sentence Connectors and Starters', 'Synthesizing separate clauses into single logical statements using transitional conjunctions')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Sentence Starters', 'Beginning combined sentences with ''Notwithstanding'', ''In spite of'', ''Hardly had''')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Discourse Connectors', 'Connecting cause-and-effect, contrast, and concession propositions')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Match the Columns
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Match the Columns' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Match the Columns', 'Connecting complementary sentence fragments between Column 1 and Column 2')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Grammatical Fragment Matching', 'Pairing clauses respecting subject-verb concord and semantic completeness')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Economic Discourse Matching', 'Constructing policy statements from split sentence halves')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

END $$;

-- Step 2: Insert 100 Bank PO English Language Questions
INSERT INTO question_service.question (
    id, tenant_id, subject_id, topic_id, subtopic_id,
    subject, topic, subtopic, difficulty, cognitive_level,
    question_type, content, options, answer_key, explanation,
    state, author_id
)
SELECT
    v.id,
    'default',
    s.id,
    t.id,
    st.id,
    'English Language and Comprehension',
    v.topic_name,
    v.subtopic_name,
    v.difficulty,
    v.cognitive_level,
    'SINGLE_MCQ',
    v.content,
    v.options::jsonb,
    v.answer_key,
    v.explanation,
    'APPROVED',
    '00000000-0000-0000-0000-000000000001'::uuid
FROM (VALUES
    (
        'a1170000-0001-0000-0000-000000000001'::uuid,
        'Reading Comprehension',
        'Financial & Economic Passages',
        'MEDIUM',
        'UNDERSTAND',
        '**Read the excerpt**:
''Central banks globally are navigating an unprecedented dilemma: balancing growth while combating persistent inflation. The rapid succession of interest rate hikes has tightened credit conditions, raising debt servicing costs for emerging market economies. Furthermore, the advent of Central Bank Digital Currencies (CBDCs) represents a structural paradigm shift, promising greater financial inclusion yet necessitating stringent data privacy safeguards.''

What is the primary dilemma faced by global central banks according to the passage?',
        '[{"id": "A", "text": "Balancing economic growth while concurrently controlling persistent inflation", "isCorrect": true}, {"id": "B", "text": "Deciding whether to eliminate paper currency completely", "isCorrect": false}, {"id": "C", "text": "Managing public sector bank mergers", "isCorrect": false}, {"id": "D", "text": "Regulating international maritime trade shipping tariffs", "isCorrect": false}]',
        'A',
        'The passage explicitly states: ''navigating an unprecedented dilemma: balancing growth while combating persistent inflation''.'
    ),
    (
        'a1170000-0002-0000-0000-000000000002'::uuid,
        'Reading Comprehension',
        'Financial & Economic Passages',
        'EASY',
        'REMEMBER',
        'According to the excerpt, what has been a direct adverse consequence of rapid interest rate hikes?',
        '[{"id": "A", "text": "Tightened credit conditions and increased debt servicing burdens for emerging markets", "isCorrect": true}, {"id": "B", "text": "A total collapse of digital payment gateways", "isCorrect": false}, {"id": "C", "text": "Widespread deflation across consumer goods", "isCorrect": false}, {"id": "D", "text": "An immediate cessation of cross-border remittances", "isCorrect": false}]',
        'A',
        'The passage mentions: ''The rapid succession of interest rate hikes has tightened credit conditions, raising debt servicing costs for emerging market economies''.'
    ),
    (
        'a1170000-0003-0000-0000-000000000003'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'MEDIUM',
        'ANALYZE',
        'What can be inferred about Central Bank Digital Currencies ($$\text{CBDCs}$$) from the text?',
        '[{"id": "A", "text": "While they offer significant financial inclusion potential, they mandate robust privacy and security safeguards", "isCorrect": true}, {"id": "B", "text": "They are intended to replace all commercial banks immediately", "isCorrect": false}, {"id": "C", "text": "They have increased inflation in developing nations", "isCorrect": false}, {"id": "D", "text": "They are solely designed for speculative trading", "isCorrect": false}]',
        'A',
        'The passage highlights that CBDCs promise ''greater financial inclusion yet necessitating stringent data privacy safeguards''.'
    ),
    (
        'a1170000-0004-0000-0000-000000000004'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'HARD',
        'ANALYZE',
        'What is the prevailing tone of the author in the provided excerpt?',
        '[{"id": "A", "text": "Analytical and Objective", "isCorrect": true}, {"id": "B", "text": "Cynical and Derogatory", "isCorrect": false}, {"id": "C", "text": "Laudatory and Euphoric", "isCorrect": false}, {"id": "D", "text": "Satirical and Sarcastic", "isCorrect": false}]',
        'A',
        'The author examines macroeconomic challenges, policy trade-offs, and technological developments in a balanced, sober, and analytical tone.'
    ),
    (
        'a1170000-0005-0000-0000-000000000005'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following is most nearly SIMILAR in meaning to the word **''PARADIGM''** as used in the passage?',
        '[{"id": "A", "text": "Framework / Standard Model", "isCorrect": true}, {"id": "B", "text": "Aberration", "isCorrect": false}, {"id": "C", "text": "Minority", "isCorrect": false}, {"id": "D", "text": "Impediment", "isCorrect": false}]',
        'A',
        '''Paradigm'' refers to a distinct fundamental framework, model, or conceptual pattern of thought.'
    ),
    (
        'a1170000-0006-0000-0000-000000000006'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'EASY',
        'UNDERSTAND',
        'Which of the following is most nearly OPPOSITE in meaning to the word **''PERSISTENT''** as used in the passage?',
        '[{"id": "A", "text": "Ephemeral / Fleeting", "isCorrect": true}, {"id": "B", "text": "Relentless", "isCorrect": false}, {"id": "C", "text": "Tenacious", "isCorrect": false}, {"id": "D", "text": "Unyielding", "isCorrect": false}]',
        'A',
        '''Persistent'' means enduring or continuous over time; its opposite is fleeting, transitory, or ephemeral.'
    ),
    (
        'a1170000-0007-0000-0000-000000000007'::uuid,
        'Reading Comprehension',
        'Financial & Economic Passages',
        'HARD',
        'ANALYZE',
        'Why do interest rate hikes by advanced economies pose special vulnerability to emerging markets?',
        '[{"id": "A", "text": "Capital flight occurs towards higher foreign yields, depreciating local currencies and escalating foreign debt repayment obligations", "isCorrect": true}, {"id": "B", "text": "They force emerging nations to cease domestic tax collection", "isCorrect": false}, {"id": "C", "text": "They automatically freeze all domestic bank accounts", "isCorrect": false}, {"id": "D", "text": "They cause domestic interest rates to plummet to zero", "isCorrect": false}]',
        'A',
        'Higher foreign interest rates attract global capital flows away from emerging economies, weakening their exchange rates and inflating dollar-denominated debt service costs.'
    ),
    (
        'a1170000-0008-0000-0000-000000000008'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following would serve as the most appropriate title for the excerpt?',
        '[{"id": "A", "text": "Central Banking Challenges: Monetary Tightening and the Digital Horizon", "isCorrect": true}, {"id": "B", "text": "The Imminent Death of Physical Cash", "isCorrect": false}, {"id": "C", "text": "Why Developing Economies Should Avoid Technology", "isCorrect": false}, {"id": "D", "text": "A History of Global Stock Markets", "isCorrect": false}]',
        'A',
        'The title accurately encapsulates the dual themes addressed: rate hikes/monetary dilemmas and digital central banking.'
    ),
    (
        'a1170000-0009-0000-0000-000000000009'::uuid,
        'Reading Comprehension',
        'Inference & Author''s Tone',
        'EASY',
        'REMEMBER',
        'What does the word **''STRINGENT''** mean in the phrase ''stringent data privacy safeguards''?',
        '[{"id": "A", "text": "Rigorous, strict, and binding", "isCorrect": true}, {"id": "B", "text": "Lenient and flexible", "isCorrect": false}, {"id": "C", "text": "Casual and optional", "isCorrect": false}, {"id": "D", "text": "Obsolete and outdated", "isCorrect": false}]',
        'A',
        '''Stringent'' means strictly enforced, rigorous, and demanding tight compliance.'
    ),
    (
        'a1170000-000a-0000-0000-00000000000a'::uuid,
        'Reading Comprehension',
        'Financial & Economic Passages',
        'HARD',
        'EVALUATE',
        'Which of the following statements is **NOT TRUE** according to the passage?',
        '[{"id": "A", "text": "Central banks face no friction between fostering growth and controlling inflation", "isCorrect": true}, {"id": "B", "text": "Interest rate increases have tightened overall credit accessibility", "isCorrect": false}, {"id": "C", "text": "CBDCs are considered a structural paradigm shift", "isCorrect": false}, {"id": "D", "text": "Emerging economies face increased debt servicing costs", "isCorrect": false}]',
        'A',
        'Statement A is false because the central premise of the excerpt is that central banks face a difficult dilemma in reconciling growth with inflation control.'
    ),
    (
        'a1170000-000b-0000-0000-00000000000b'::uuid,
        'Cloze Test',
        'Economic Cloze Passages',
        'EASY',
        'APPLY',
        '**Cloze Text**:
''Financial inclusion is essential for sustainable economic development. By extending banking services to the unbanked, governments can __(1)__ poverty and stimulate grassroots entrepreneurship.''

Which of the following words best fits blank **(1)**?',
        '[{"id": "A", "text": "alleviate", "isCorrect": true}, {"id": "B", "text": "aggravate", "isCorrect": false}, {"id": "C", "text": "perpetuate", "isCorrect": false}, {"id": "D", "text": "fabricate", "isCorrect": false}]',
        'A',
        '''Alleviate'' means to lessen, relieve, or mitigate distress/poverty, which fits the positive context of financial inclusion.'
    ),
    (
        'a1170000-000c-0000-0000-00000000000c'::uuid,
        'Cloze Test',
        'Economic Cloze Passages',
        'MEDIUM',
        'APPLY',
        '**Cloze Text**:
''However, technological adoption must be accompanied by digital literacy to __(2)__ vulnerable citizens from predatory fraud.''

Which of the following words best fits blank **(2)**?',
        '[{"id": "A", "text": "shield", "isCorrect": true}, {"id": "B", "text": "expose", "isCorrect": false}, {"id": "C", "text": "subject", "isCorrect": false}, {"id": "D", "text": "entice", "isCorrect": false}]',
        'A',
        '''Shield'' (meaning protect or guard against harm/fraud) correctly completes the clause.'
    ),
    (
        'a1170000-000d-0000-0000-00000000000d'::uuid,
        'Cloze Test',
        'Economic Cloze Passages',
        'MEDIUM',
        'APPLY',
        '**Cloze Text**:
''Commercial lenders must also ensure that credit is disbursed __(3)__, without fostering unsustainable household debt.''

Which of the following words best fits blank **(3)**?',
        '[{"id": "A", "text": "prudently", "isCorrect": true}, {"id": "B", "text": "recklessly", "isCorrect": false}, {"id": "C", "text": "haphazardly", "isCorrect": false}, {"id": "D", "text": "ostentatiously", "isCorrect": false}]',
        'A',
        '''Prudently'' means acting with care, wisdom, and foresight, which prevents unsustainable debt accumulation.'
    ),
    (
        'a1170000-000e-0000-0000-00000000000e'::uuid,
        'Cloze Test',
        'Economic Cloze Passages',
        'HARD',
        'APPLY',
        '**Cloze Text**:
''Sound macroeconomic governance demands that fiscal deficits be kept within __(4)__ thresholds to prevent sovereign rating downgrades.''

Which word best fits blank **(4)**?',
        '[{"id": "A", "text": "sustainable", "isCorrect": true}, {"id": "B", "text": "astronomical", "isCorrect": false}, {"id": "C", "text": "arbitrary", "isCorrect": false}, {"id": "D", "text": "negligible", "isCorrect": false}]',
        'A',
        '''Sustainable'' thresholds are fiscal boundaries that can be maintained without triggering sovereign credit defaults or rating cuts.'
    ),
    (
        'a1170000-000f-0000-0000-00000000000f'::uuid,
        'Cloze Test',
        'Economic Cloze Passages',
        'MEDIUM',
        'APPLY',
        '**Cloze Text**:
''The central bank decided to __(5)__ excess liquidity from the banking system via open market sales.''

Which word best fits blank **(5)**?',
        '[{"id": "A", "text": "absorb", "isCorrect": true}, {"id": "B", "text": "infuse", "isCorrect": false}, {"id": "C", "text": "propel", "isCorrect": false}, {"id": "D", "text": "disperse", "isCorrect": false}]',
        'A',
        '''Absorb'' is the standard monetary term for removing or mopping up surplus liquidity from the inter-bank market.'
    ),
    (
        'a1170000-0010-0000-0000-000000000010'::uuid,
        'Cloze Test',
        'Grammatical & Collocation Cloze',
        'EASY',
        'APPLY',
        '**Cloze Text**:
''The committee was tasked with reviewing the bank''s operational resilience __(6)__ cyber security breaches.''

Which preposition best fits blank **(6)**?',
        '[{"id": "A", "text": "against", "isCorrect": true}, {"id": "B", "text": "towards", "isCorrect": false}, {"id": "C", "text": "upon", "isCorrect": false}, {"id": "D", "text": "into", "isCorrect": false}]',
        'A',
        'One builds resilience ''against'' threats or attacks.'
    ),
    (
        'a1170000-0011-0000-0000-000000000011'::uuid,
        'Cloze Test',
        'Grammatical & Collocation Cloze',
        'MEDIUM',
        'APPLY',
        '**Cloze Text**:
''The merger was finalized in order to __(7)__ synergy and optimize branch operating overheads.''

Which verb best fits blank **(7)**?',
        '[{"id": "A", "text": "harness", "isCorrect": true}, {"id": "B", "text": "curtail", "isCorrect": false}, {"id": "C", "text": "sabotage", "isCorrect": false}, {"id": "D", "text": "fragment", "isCorrect": false}]',
        'A',
        'To ''harness'' synergy means to utilize or leverage mutual operational advantages effectively.'
    ),
    (
        'a1170000-0012-0000-0000-000000000012'::uuid,
        'Cloze Test',
        'Grammatical & Collocation Cloze',
        'HARD',
        'APPLY',
        '**Cloze Text**:
''Failure to comply with statutory anti-money laundering protocols may __(8)__ severe penal sanctions from the regulatory authority.''

Which word best fits blank **(8)**?',
        '[{"id": "A", "text": "invite", "isCorrect": true}, {"id": "B", "text": "avert", "isCorrect": false}, {"id": "C", "text": "preclude", "isCorrect": false}, {"id": "D", "text": "defuse", "isCorrect": false}]',
        'A',
        'To ''invite'' penal sanctions means to cause or make oneself liable to statutory fines and punishments.'
    ),
    (
        'a1170000-0013-0000-0000-000000000013'::uuid,
        'Cloze Test',
        'Grammatical & Collocation Cloze',
        'MEDIUM',
        'APPLY',
        '**Cloze Text**:
''The board of directors unanimously resolved to __(9)__ the dividend payout to conserve regulatory capital.''

Which word best fits blank **(9)**?',
        '[{"id": "A", "text": "withhold", "isCorrect": true}, {"id": "B", "text": "amplify", "isCorrect": false}, {"id": "C", "text": "squander", "isCorrect": false}, {"id": "D", "text": "extravagate", "isCorrect": false}]',
        'A',
        'To ''withhold'' dividends means to retain earnings rather than distributing them to equity holders.'
    ),
    (
        'a1170000-0014-0000-0000-000000000014'::uuid,
        'Cloze Test',
        'Grammatical & Collocation Cloze',
        'EASY',
        'APPLY',
        '**Cloze Text**:
''The startup succeeded in securing venture capital by demonstrating a clear path __(10)__ profitability.''

Which preposition best fits blank **(10)**?',
        '[{"id": "A", "text": "to", "isCorrect": true}, {"id": "B", "text": "at", "isCorrect": false}, {"id": "C", "text": "for", "isCorrect": false}, {"id": "D", "text": "with", "isCorrect": false}]',
        'A',
        'The standard idiom is ''path to profitability''.'
    ),
    (
        'a1170000-0015-0000-0000-000000000015'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'MEDIUM',
        'ANALYZE',
        '**Spot the Error**: In the following sentence, find the part that contains a grammatical error:

''Neither the Chief Executive Officer (A) / nor the board directors (B) / was present at the annual (C) / general shareholder meeting. (D)''',
        '[{"id": "A", "text": "Part (C) - ''was present'' should be ''were present''", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (B)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'Rule of Proximity for ''neither... nor'': When subjects differ in number, the verb agrees with the nearer subject. Since ''board directors'' is plural, the verb must be plural ''were present''.'
    ),
    (
        'a1170000-0016-0000-0000-000000000016'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'EASY',
        'ANALYZE',
        '**Spot the Error**:
''The quality of these (A) / newly minted silver coins (B) / are significantly superior (C) / to the older batch. (D)''',
        '[{"id": "A", "text": "Part (C) - ''are'' should be ''is''", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (B)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'The true subject of the sentence is the singular noun ''The quality'', not ''coins''. Therefore, the singular verb ''is'' must be used.'
    ),
    (
        'a1170000-0017-0000-0000-000000000017'::uuid,
        'Error Detection and Spotting',
        'Tense, Conditionals & Prepositions',
        'MEDIUM',
        'ANALYZE',
        '**Spot the Error**:
''If the bank would have (A) / implemented two-factor authentication earlier, (B) / the cyber heist (C) / could have been averted. (D)''',
        '[{"id": "A", "text": "Part (A) - ''would have'' should be ''had''", "isCorrect": true}, {"id": "B", "text": "Part (B)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'Third Conditional rule: The ''if''-clause takes the past perfect tense (''If the bank had implemented''), never ''would have''.'
    ),
    (
        'a1170000-0018-0000-0000-000000000018'::uuid,
        'Error Detection and Spotting',
        'Tense, Conditionals & Prepositions',
        'HARD',
        'ANALYZE',
        '**Spot the Error**:
''Scarcely had the stock exchange opened (A) / than panic selling triggered (B) / the lower circuit breaker, (C) / halting all trading. (D)''',
        '[{"id": "A", "text": "Part (B) - ''than'' should be ''when'' or ''before''", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'Correlative conjunction rule: ''Scarcely / Hardly'' is always followed by ''when'' or ''before'', whereas ''No sooner'' is followed by ''than''.'
    ),
    (
        'a1170000-0019-0000-0000-000000000019'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'MEDIUM',
        'ANALYZE',
        '**Spot the Error**:
''One of the primary reason (A) / for the depreciation of the currency (B) / was the widening (C) / current account deficit. (D)''',
        '[{"id": "A", "text": "Part (A) - ''reason'' should be ''reasons''", "isCorrect": true}, {"id": "B", "text": "Part (B)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'The construct ''One of the + [plural noun]'' requires a plural noun: ''One of the primary reasons''.'
    ),
    (
        'a1170000-001a-0000-0000-00000000001a'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'HARD',
        'ANALYZE',
        '**Spot the Error**:
''The auditor demanded that (A) / the branch manager provides (B) / all relevant loan sanction (C) / files by tomorrow noon. (D)''',
        '[{"id": "A", "text": "Part (B) - ''provides'' should be ''provide'' (subjunctive mood)", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'Mandative Subjunctive rule: Verbs expressing demands, requests, or insistence (''demanded that...'') take the base form of the verb (''provide'', not ''provides'').'
    ),
    (
        'a1170000-001b-0000-0000-00000000001b'::uuid,
        'Error Detection and Spotting',
        'Tense, Conditionals & Prepositions',
        'EASY',
        'ANALYZE',
        '**Spot the Error**:
''She has been working (A) / in this foreign exchange department (B) / since five years (C) / with exemplary dedication. (D)''',
        '[{"id": "A", "text": "Part (C) - ''since'' should be ''for''", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (B)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        '''For'' is used to denote a period/duration of time (''for five years''), whereas ''since'' denotes a specific starting point in time.'
    ),
    (
        'a1170000-001c-0000-0000-00000000001c'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'HARD',
        'ANALYZE',
        '**Spot the Error**:
''Walking down the trading floor, (A) / the sudden siren (B) / startled the young treasury intern (C) / working at the terminal. (D)''',
        '[{"id": "A", "text": "Part (A) - Dangling modifier error; the siren was not walking down the floor", "isCorrect": true}, {"id": "B", "text": "Part (B)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        'Dangling Participle: The participial phrase ''Walking down the trading floor'' incorrectly modifies ''the sudden siren'' rather than the intern. Correct: ''As the young treasury intern was walking down the trading floor, the sudden siren startled him''.'
    ),
    (
        'a1170000-001d-0000-0000-00000000001d'::uuid,
        'Error Detection and Spotting',
        'Tense, Conditionals & Prepositions',
        'MEDIUM',
        'ANALYZE',
        '**Spot the Error**:
''Despite of the steep increase (A) / in repo rates by the central bank, (B) / retail housing demand (C) / remained exceptionally resilient. (D)''',
        '[{"id": "A", "text": "Part (A) - ''Despite of'' should be ''Despite'' or ''In spite of''", "isCorrect": true}, {"id": "B", "text": "Part (B)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        '''Despite'' is never followed by the preposition ''of''. One says either ''Despite the steep increase'' or ''In spite of the steep increase''.'
    ),
    (
        'a1170000-001e-0000-0000-00000000001e'::uuid,
        'Error Detection and Spotting',
        'Subject-Verb & Modifier Errors',
        'EASY',
        'ANALYZE',
        '**Spot the Error**:
''Each of the loan applicants (A) / have submitted their (B) / certified identity proof (C) / to the verification officer. (D)''',
        '[{"id": "A", "text": "Part (B) - ''have'' should be ''has''", "isCorrect": true}, {"id": "B", "text": "Part (A)", "isCorrect": false}, {"id": "C", "text": "Part (C)", "isCorrect": false}, {"id": "D", "text": "Part (D)", "isCorrect": false}]',
        'A',
        '''Each'' is an indefinite singular pronoun requiring a singular verb (''has submitted'') and singular pronoun (''his or her'').'
    ),
    (
        'a1170000-001f-0000-0000-00000000001f'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'EASY',
        'APPLY',
        'Improve the highlighted phrase:
''The financial committee has decided to **call off** the merger talks due to valuation mismatches.''',
        '[{"id": "A", "text": "No improvement required (the phrase is correct)", "isCorrect": true}, {"id": "B", "text": "call out", "isCorrect": false}, {"id": "C", "text": "call upon", "isCorrect": false}, {"id": "D", "text": "call in", "isCorrect": false}]',
        'A',
        '''Call off'' means to cancel or abandon an event or discussion, which is grammatically and idiomatically correct in this context.'
    ),
    (
        'a1170000-0020-0000-0000-000000000020'::uuid,
        'Sentence Improvement and Correction',
        'Structural Parallelism',
        'MEDIUM',
        'APPLY',
        'Improve the underlined portion:
''The new banking regulation aims **at promoting transparency, to ensure consumer safety, and minimizing operational fraud**.''',
        '[{"id": "A", "text": "at promoting transparency, ensuring consumer safety, and minimizing operational fraud", "isCorrect": true}, {"id": "B", "text": "to promote transparency, ensuring consumer safety, and minimizing operational fraud", "isCorrect": false}, {"id": "C", "text": "promoting transparency, to ensure consumer safety, and to minimize fraud", "isCorrect": false}, {"id": "D", "text": "for promotion of transparency, to ensure consumer safety, and minimize fraud", "isCorrect": false}]',
        'A',
        'Law of Parallelism: All elements in the series must follow the same grammatical structure (gerund form: ''promoting..., ensuring..., and minimizing...'').'
    ),
    (
        'a1170000-0021-0000-0000-000000000021'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'MEDIUM',
        'APPLY',
        'Improve the underlined portion:
''The branch manager is senior **than** all the probationary officers appointed this year.''',
        '[{"id": "A", "text": "senior to", "isCorrect": true}, {"id": "B", "text": "senior from", "isCorrect": false}, {"id": "C", "text": "more senior than", "isCorrect": false}, {"id": "D", "text": "senior against", "isCorrect": false}]',
        'A',
        'Comparative adjectives of Latin origin ending in ''-ior'' (senior, junior, prior, superior, inferior) take the preposition ''to'', not ''than''.'
    ),
    (
        'a1170000-0022-0000-0000-000000000022'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'HARD',
        'APPLY',
        'Improve the underlined portion:
''Seldom **we have seen** such unprecedented volatility in the foreign exchange sovereign debt market.''',
        '[{"id": "A", "text": "have we seen", "isCorrect": true}, {"id": "B", "text": "we saw", "isCorrect": false}, {"id": "C", "text": "had we saw", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        'Rule of Inversion: When a negative or restrictive adverbial (''Seldom'', ''Rarely'', ''Never'') begins a sentence, subject-auxiliary inversion is mandatory (''Seldom have we seen...'').'
    ),
    (
        'a1170000-0023-0000-0000-000000000023'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'EASY',
        'APPLY',
        'Improve the underlined portion:
''He works hardly, yet he fails to meet his quarterly retail insurance targets.''',
        '[{"id": "A", "text": "He works hard", "isCorrect": true}, {"id": "B", "text": "He is working hardly", "isCorrect": false}, {"id": "C", "text": "Hard he works", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        '''Hardly'' means barely or scarcely. The adverb meaning diligently or with great effort is ''hard''.'
    ),
    (
        'a1170000-0024-0000-0000-000000000024'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'MEDIUM',
        'APPLY',
        'Improve the underlined portion:
''The managing director alongside his board members **were attending** the global banking symposium.''',
        '[{"id": "A", "text": "was attending", "isCorrect": true}, {"id": "B", "text": "are attending", "isCorrect": false}, {"id": "C", "text": "have been attending", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        'When subjects are joined by ''alongside'', ''as well as'', ''together with'', or ''with'', the verb agrees with the primary first subject (''The managing director'' = singular, so ''was attending'').'
    ),
    (
        'a1170000-0025-0000-0000-000000000025'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'HARD',
        'APPLY',
        'Improve the underlined portion:
''The economic crisis was **so severe as** no commercial enterprise could remain completely unscathed.''',
        '[{"id": "A", "text": "so severe that", "isCorrect": true}, {"id": "B", "text": "as severe that", "isCorrect": false}, {"id": "C", "text": "too severe that", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        'The correlative conjunction denoting cause and consequence is ''so + adjective + that'' (''so severe that no commercial enterprise...'').'
    ),
    (
        'a1170000-0026-0000-0000-000000000026'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'EASY',
        'APPLY',
        'Improve the underlined portion:
''She prefers equity investment **than** fixed deposit receipts.''',
        '[{"id": "A", "text": "to fixed deposit receipts", "isCorrect": true}, {"id": "B", "text": "against fixed deposit receipts", "isCorrect": false}, {"id": "C", "text": "over than fixed deposit receipts", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        'The verb ''prefer'' takes the preposition ''to'' (''prefers X to Y''), never ''than''.'
    ),
    (
        'a1170000-0027-0000-0000-000000000027'::uuid,
        'Sentence Improvement and Correction',
        'Phrase Replacement',
        'MEDIUM',
        'APPLY',
        'Improve the underlined portion:
''The customer asked the teller **that where was the manager sitting**.''',
        '[{"id": "A", "text": "where the manager was sitting", "isCorrect": true}, {"id": "B", "text": "that where the manager was sitting", "isCorrect": false}, {"id": "C", "text": "where was the manager sitting", "isCorrect": false}, {"id": "D", "text": "No improvement required", "isCorrect": false}]',
        'A',
        'Indirect Speech rule: Interrogative words (where, what, why) act as conjunctions; adding ''that'' is redundant, and the subject precedes the auxiliary verb (''where the manager was sitting'').'
    ),
    (
        'a1170000-0028-0000-0000-000000000028'::uuid,
        'Sentence Improvement and Correction',
        'Structural Parallelism',
        'HARD',
        'APPLY',
        'Improve the underlined portion:
''Not only **did the central bank cut** interest rates, but it also injected massive liquidity.''',
        '[{"id": "A", "text": "No improvement required (the sentence is correct)", "isCorrect": true}, {"id": "B", "text": "the central bank not only cut", "isCorrect": false}, {"id": "C", "text": "not only the central bank did cut", "isCorrect": false}, {"id": "D", "text": "did not only the central bank cut", "isCorrect": false}]',
        'A',
        'The sentence correctly employs negative inversion with ''Not only did the central bank cut...'', balanced parallelly by ''but it also injected''.'
    ),
    (
        'a1170000-0029-0000-0000-000000000029'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Editorial Sentence Sequences',
        'HARD',
        'ANALYZE',
        '**Rearrange the following sentences (A, B, C, D, E) to form a coherent paragraph**:
- **A**: This unprecedented expansion of digital financial infrastructure has transformed retail commerce.
- **B**: Over the past decade, India has witnessed a monumental surge in electronic transactions.
- **C**: Consequently, traditional cash dependency has receded significantly across urban centers.
- **D**: Chief among these technological drivers is the Unified Payments Interface (UPI).
- **E**: However, this digital acceleration brings critical imperatives for robust fraud prevention.

Which of the following represents the correct logical sequence?',
        '[{"id": "A", "text": "B - D - A - C - E", "isCorrect": true}, {"id": "B", "text": "B - A - D - E - C", "isCorrect": false}, {"id": "C", "text": "D - B - A - C - E", "isCorrect": false}, {"id": "D", "text": "A - B - D - C - E", "isCorrect": false}]',
        'A',
        'Logical order: B introduces the macro phenomenon (surge in transactions); D specifies the key technological driver (UPI); A describes the systemic transformation; C presents the direct consequence (decline in cash); E introduces the concluding caveat/challenge (fraud prevention). Sequence is B - D - A - C - E.'
    ),
    (
        'a1170000-002a-0000-0000-00000000002a'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'EASY',
        'UNDERSTAND',
        'Based on the paragraph (B - D - A - C - E), which sentence serves as the **FIRST (introductory)** sentence?',
        '[{"id": "A", "text": "B", "isCorrect": true}, {"id": "B", "text": "A", "isCorrect": false}, {"id": "C", "text": "D", "isCorrect": false}, {"id": "D", "text": "C", "isCorrect": false}]',
        'A',
        'Sentence B is independent and establishes the broad historical context, serving as the opening sentence.'
    ),
    (
        'a1170000-002b-0000-0000-00000000002b'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'EASY',
        'UNDERSTAND',
        'Based on the paragraph (B - D - A - C - E), which sentence serves as the **FIFTH (concluding)** sentence?',
        '[{"id": "A", "text": "E", "isCorrect": true}, {"id": "B", "text": "C", "isCorrect": false}, {"id": "C", "text": "A", "isCorrect": false}, {"id": "D", "text": "D", "isCorrect": false}]',
        'A',
        'Sentence E provides the critical counterpoint and forward-looking policy challenge, acting as the conclusion.'
    ),
    (
        'a1170000-002c-0000-0000-00000000002c'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'MEDIUM',
        'APPLY',
        'Based on the sequence (B - D - A - C - E), which sentence immediately follows **Sentence D**?',
        '[{"id": "A", "text": "Sentence A", "isCorrect": true}, {"id": "B", "text": "Sentence C", "isCorrect": false}, {"id": "C", "text": "Sentence E", "isCorrect": false}, {"id": "D", "text": "Sentence B", "isCorrect": false}]',
        'A',
        'Sentence A (''This unprecedented expansion...'') directly follows Sentence D''s introduction of UPI.'
    ),
    (
        'a1170000-002d-0000-0000-00000000002d'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'MEDIUM',
        'APPLY',
        'Based on the sequence (B - D - A - C - E), which sentence is the **THIRD** sentence?',
        '[{"id": "A", "text": "Sentence A", "isCorrect": true}, {"id": "B", "text": "Sentence D", "isCorrect": false}, {"id": "C", "text": "Sentence C", "isCorrect": false}, {"id": "D", "text": "Sentence E", "isCorrect": false}]',
        'A',
        'The third sentence in the order B-D-A-C-E is Sentence A.'
    ),
    (
        'a1170000-002e-0000-0000-00000000002e'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Editorial Sentence Sequences',
        'HARD',
        'ANALYZE',
        '**Rearrange the sentences**:
- **P**: Global commodity prices began to surge rapidly following geopolitical supply shocks.
- **Q**: In response, central bankers abandoned their transitory inflation stance.
- **R**: This ignited a synchronized cycle of monetary policy rate hikes worldwide.
- **S**: Emerging markets subsequently suffered from domestic currency depreciations.
What is the logical sequence?',
        '[{"id": "A", "text": "P - Q - R - S", "isCorrect": true}, {"id": "B", "text": "Q - P - R - S", "isCorrect": false}, {"id": "C", "text": "P - R - Q - S", "isCorrect": false}, {"id": "D", "text": "S - P - Q - R", "isCorrect": false}]',
        'A',
        'Sequence of causality: Supply shocks spike commodity prices (P) -> Central banks shift stance (Q) -> Rate hike cycle begins (R) -> Emerging market currencies depreciate (S). Order is P - Q - R - S.'
    ),
    (
        'a1170000-002f-0000-0000-00000000002f'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'EASY',
        'APPLY',
        'In the sequence P - Q - R - S, which sentence is the **SECOND** sentence?',
        '[{"id": "A", "text": "Sentence Q", "isCorrect": true}, {"id": "B", "text": "Sentence P", "isCorrect": false}, {"id": "C", "text": "Sentence R", "isCorrect": false}, {"id": "D", "text": "Sentence S", "isCorrect": false}]',
        'A',
        'Sentence Q is the second step in the chronological causal chain.'
    ),
    (
        'a1170000-0030-0000-0000-000000000030'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Sentence Boundary Identification',
        'MEDIUM',
        'APPLY',
        'In the sequence P - Q - R - S, which sentence describes the **final macroeconomic outcome**?',
        '[{"id": "A", "text": "Sentence S", "isCorrect": true}, {"id": "B", "text": "Sentence R", "isCorrect": false}, {"id": "C", "text": "Sentence Q", "isCorrect": false}, {"id": "D", "text": "Sentence P", "isCorrect": false}]',
        'A',
        'Sentence S outlines the downstream impact on emerging market currencies as the final consequence.'
    ),
    (
        'a1170000-0031-0000-0000-000000000031'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Editorial Sentence Sequences',
        'HARD',
        'ANALYZE',
        'Which of the following connectors in Sentence C (''Consequently, traditional cash dependency has receded...'') reveals its role in paragraph coherence?',
        '[{"id": "A", "text": "''Consequently'' functions as a cause-and-effect transition marker indicating an outcome of prior points", "isCorrect": true}, {"id": "B", "text": "It introduces an adverse contradiction", "isCorrect": false}, {"id": "C", "text": "It serves as a hypothetical conditional", "isCorrect": false}, {"id": "D", "text": "It functions as an opening greeting", "isCorrect": false}]',
        'A',
        '''Consequently'' signals that the statement describes a direct result or outcome of the preceding developments.'
    ),
    (
        'a1170000-0032-0000-0000-000000000032'::uuid,
        'Para Jumbles & Sentence Rearrangement',
        'Editorial Sentence Sequences',
        'MEDIUM',
        'UNDERSTAND',
        'What linguistic clue in Sentence A (''This unprecedented expansion...'') links it to the preceding sentence?',
        '[{"id": "A", "text": "The demonstrative pronoun ''This'' referring back to the UPI system described in Sentence D", "isCorrect": true}, {"id": "B", "text": "The use of the future continuous tense", "isCorrect": false}, {"id": "C", "text": "The presence of numerical percentages", "isCorrect": false}, {"id": "D", "text": "The Latin prefix ''anti-''", "isCorrect": false}]',
        'A',
        'Demonstrative determiners (''This'', ''These'') create anaphoric cohesion by directly referring to the concept introduced in the immediately prior sentence.'
    ),
    (
        'a1170000-0033-0000-0000-000000000033'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'MEDIUM',
        'APPLY',
        'The monetary policy committee decided to keep the repo rate _______ in its latest meeting, citing the need to _______ ongoing economic recovery.',
        '[{"id": "A", "text": "unchanged, nurture", "isCorrect": true}, {"id": "B", "text": "elevated, dismantle", "isCorrect": false}, {"id": "C", "text": "volatile, discourage", "isCorrect": false}, {"id": "D", "text": "stagnant, jeopardize", "isCorrect": false}]',
        'A',
        'Keeping rates ''unchanged'' to ''nurture'' (foster/support) economic recovery provides positive semantic harmony.'
    ),
    (
        'a1170000-0034-0000-0000-000000000034'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'HARD',
        'APPLY',
        'Despite the initial _______ from traditional bankers, the fintech platform managed to _______ a substantial market share in rural credit.',
        '[{"id": "A", "text": "skepticism, capture", "isCorrect": true}, {"id": "B", "text": "applause, relinquish", "isCorrect": false}, {"id": "C", "text": "approval, squander", "isCorrect": false}, {"id": "D", "text": "enthusiasm, forfeit", "isCorrect": false}]',
        'A',
        '''Despite'' introduces contrast: initial ''skepticism'' (doubt) contrasted with managing to ''capture'' (acquire) substantial market share.'
    ),
    (
        'a1170000-0035-0000-0000-000000000035'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'MEDIUM',
        'APPLY',
        'The auditor pointed out several _______ in the loan documentation, which could potentially _______ the bank''s legal claims during recovery.',
        '[{"id": "A", "text": "irregularities, jeopardize", "isCorrect": true}, {"id": "B", "text": "strengths, bolster", "isCorrect": false}, {"id": "C", "text": "amenities, facilitate", "isCorrect": false}, {"id": "D", "text": "accuracies, undermine", "isCorrect": false}]',
        'A',
        'Documentation ''irregularities'' (defects) would ''jeopardize'' (endanger/weaken) legal recovery claims.'
    ),
    (
        'a1170000-0036-0000-0000-000000000036'::uuid,
        'Fill in the Blanks',
        'Contextual Word Selection',
        'EASY',
        'APPLY',
        'The central bank issued a stern _______ to commercial banks regarding the _______ of customer grievance redressal norms.',
        '[{"id": "A", "text": "warning, violation", "isCorrect": true}, {"id": "B", "text": "praise, infringement", "isCorrect": false}, {"id": "C", "text": "reward, flouting", "isCorrect": false}, {"id": "D", "text": "discount, adherence", "isCorrect": false}]',
        'A',
        'A stern ''warning'' is issued regarding the ''violation'' of regulatory norms.'
    ),
    (
        'a1170000-0037-0000-0000-000000000037'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'HARD',
        'APPLY',
        'The finance minister emphasized that fiscal discipline is _______ to maintaining macroeconomic stability and _______ foreign investor confidence.',
        '[{"id": "A", "text": "paramount, bolstering", "isCorrect": true}, {"id": "B", "text": "detrimental, eroding", "isCorrect": false}, {"id": "C", "text": "superfluous, undermining", "isCorrect": false}, {"id": "D", "text": "peripheral, destroying", "isCorrect": false}]',
        'A',
        'Fiscal discipline is ''paramount'' (of vital importance) for ''bolstering'' (strengthening) investor confidence.'
    ),
    (
        'a1170000-0038-0000-0000-000000000038'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'MEDIUM',
        'APPLY',
        'The board was forced to _______ the project after incurring _______ losses over consecutive quarters.',
        '[{"id": "A", "text": "abandon, colossal", "isCorrect": true}, {"id": "B", "text": "inaugurate, severe", "isCorrect": false}, {"id": "C", "text": "accelerate, meager", "isCorrect": false}, {"id": "D", "text": "perpetuate, nominal", "isCorrect": false}]',
        'A',
        'To ''abandon'' a venture after suffering ''colossal'' (massive) losses matches the logical sequence.'
    ),
    (
        'a1170000-0039-0000-0000-000000000039'::uuid,
        'Fill in the Blanks',
        'Contextual Word Selection',
        'EASY',
        'APPLY',
        'The new mobile banking application is designed to be _______, allowing even novice smartphone users to navigate with _______.',
        '[{"id": "A", "text": "intuitive, ease", "isCorrect": true}, {"id": "B", "text": "convoluted, difficulty", "isCorrect": false}, {"id": "C", "text": "opaque, comfort", "isCorrect": false}, {"id": "D", "text": "cumbersome, speed", "isCorrect": false}]',
        'A',
        'An ''intuitive'' (user-friendly) app allows users to navigate with ''ease''.'
    ),
    (
        'a1170000-003a-0000-0000-00000000003a'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'HARD',
        'APPLY',
        'The rapid _______ of artificial intelligence in credit underwriting has raised serious questions about algorithmic _______ and bias.',
        '[{"id": "A", "text": "proliferation, transparency", "isCorrect": true}, {"id": "B", "text": "reduction, clarity", "isCorrect": false}, {"id": "C", "text": "stagnation, fairness", "isCorrect": false}, {"id": "D", "text": "demise, accuracy", "isCorrect": false}]',
        'A',
        'The ''proliferation'' (rapid spread) of AI highlights concerns regarding algorithmic ''transparency''.'
    ),
    (
        'a1170000-003b-0000-0000-00000000003b'::uuid,
        'Fill in the Blanks',
        'Double Fillers in Banking',
        'MEDIUM',
        'APPLY',
        'To _______ the liquidity crunch, the central bank conducted an emergency auction to _______ funds into the money market.',
        '[{"id": "A", "text": "mitigate, inject", "isCorrect": true}, {"id": "B", "text": "exacerbate, withdraw", "isCorrect": false}, {"id": "C", "text": "prolong, siphon", "isCorrect": false}, {"id": "D", "text": "intensify, pump", "isCorrect": false}]',
        'A',
        'To ''mitigate'' (relieve) a cash crunch, central banks ''inject'' (infuse) liquidity.'
    ),
    (
        'a1170000-003c-0000-0000-00000000003c'::uuid,
        'Fill in the Blanks',
        'Contextual Word Selection',
        'EASY',
        'APPLY',
        'The bank''s customer base grew _______ following the launch of zero-fee digital savings accounts, exceeding all initial _______.',
        '[{"id": "A", "text": "exponentially, expectations", "isCorrect": true}, {"id": "B", "text": "marginally, obstacles", "isCorrect": false}, {"id": "C", "text": "sluggishly, targets", "isCorrect": false}, {"id": "D", "text": "scarcely, forecasts", "isCorrect": false}]',
        'A',
        'Growing ''exponentially'' to exceed all ''expectations'' represents a natural positive collocation.'
    ),
    (
        'a1170000-003d-0000-0000-00000000003d'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'MEDIUM',
        'APPLY',
        'In the sentence below, four words are highlighted in bold (A, B, C, D). Which pair of words should be swapped to make the sentence grammatically correct and contextually meaningful?

''The central bank decided to **curtail** (A) the interest rates in order to **stimulate** (B) economic growth, while ensuring that inflation does not **breach** (C) the upper **tolerance** (D) limit.''',
        '[{"id": "A", "text": "No swap required (the sentence is already correct)", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "B - C", "isCorrect": false}, {"id": "D", "text": "C - D", "isCorrect": false}]',
        'A',
        'All words are in their correct contextual positions: curtail rates to stimulate growth without breaching the tolerance limit.'
    ),
    (
        'a1170000-003e-0000-0000-00000000003e'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'HARD',
        'APPLY',
        'Identify the necessary word swap:

''The commercial bank suffered heavy **penalties** (A) due to **systemic** (B) failures in its compliance architecture, prompting regulatory **scrutiny** (C) and severe **losses** (D).'' (Wait: ''suffered heavy losses due to systemic failures... and severe penalties''). Which swap resolves this?',
        '[{"id": "A", "text": "A - D (suffered heavy losses ... severe penalties)", "isCorrect": true}, {"id": "B", "text": "B - C", "isCorrect": false}, {"id": "C", "text": "A - C", "isCorrect": false}, {"id": "D", "text": "B - D", "isCorrect": false}]',
        'A',
        'Swapping A and D produces: ''The commercial bank suffered heavy losses (D) due to systemic failures..., prompting regulatory scrutiny and severe penalties (A)''.'
    ),
    (
        'a1170000-003f-0000-0000-00000000003f'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'MEDIUM',
        'APPLY',
        'Find the correct swap:

''The company''s failure to **repay** (A) its debts led to the **appointment** (B) of an insolvency professional to **manage** (C) its daily corporate **operations** (D).''',
        '[{"id": "A", "text": "No swap required", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "B - C", "isCorrect": false}, {"id": "D", "text": "C - D", "isCorrect": false}]',
        'A',
        'The sentence is fully coherent and grammatically correct as written.'
    ),
    (
        'a1170000-0040-0000-0000-000000000040'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'HARD',
        'APPLY',
        'Find the required swap:

''The government announced a series of **recovery** (A) incentives to accelerate the **economic** (B) of micro enterprises.'' (Swap ''recovery'' with ''economic'' -> economic incentives to accelerate the recovery)',
        '[{"id": "A", "text": "A - B", "isCorrect": true}, {"id": "B", "text": "No swap required", "isCorrect": false}, {"id": "C", "text": "A - C", "isCorrect": false}, {"id": "D", "text": "B - D", "isCorrect": false}]',
        'A',
        'Swapping A and B yields: ''a series of economic incentives to accelerate the recovery of micro enterprises''.'
    ),
    (
        'a1170000-0041-0000-0000-000000000041'::uuid,
        'Word Swap & Sentence Reordering',
        'Vocabulary Alignment',
        'EASY',
        'APPLY',
        'Find the correct swap:

''Customers are advised to **confidential** (A) their passwords **keep** (B) at all times.''',
        '[{"id": "A", "text": "A - B (keep their passwords confidential)", "isCorrect": true}, {"id": "B", "text": "No swap required", "isCorrect": false}, {"id": "C", "text": "A with subject", "isCorrect": false}, {"id": "D", "text": "B with end", "isCorrect": false}]',
        'A',
        'Swapping A and B restores proper verb-adjective order: ''advised to keep (B) their passwords confidential (A)''.'
    ),
    (
        'a1170000-0042-0000-0000-000000000042'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'MEDIUM',
        'APPLY',
        'Identify the necessary swap:

''The committee expressed **unanimous** (A) satisfaction with the **performance** (B) of the branch manager in **expanding** (C) retail deposits.''',
        '[{"id": "A", "text": "No swap required", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "B - C", "isCorrect": false}, {"id": "D", "text": "A - C", "isCorrect": false}]',
        'A',
        'The sentence is grammatically sound and meaningful without any word swap.'
    ),
    (
        'a1170000-0043-0000-0000-000000000043'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'HARD',
        'APPLY',
        'Find the required swap:

''The **volatility** (A) of foreign institutional investors caused severe **exodus** (B) in the domestic stock index.''',
        '[{"id": "A", "text": "A - B (The exodus of foreign investors caused severe volatility)", "isCorrect": true}, {"id": "B", "text": "No swap required", "isCorrect": false}, {"id": "C", "text": "A with end", "isCorrect": false}, {"id": "D", "text": "B with index", "isCorrect": false}]',
        'A',
        'Investors do an ''exodus'' (departure), which creates ''volatility'' (fluctuation) in the market.'
    ),
    (
        'a1170000-0044-0000-0000-000000000044'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'MEDIUM',
        'APPLY',
        'Find the required swap:

''High inflation tends to **erode** (A) the purchasing **power** (B) of consumers, thereby dampening overall economic **sentiment** (C).''',
        '[{"id": "A", "text": "No swap required", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "B - C", "isCorrect": false}, {"id": "D", "text": "A - C", "isCorrect": false}]',
        'A',
        'The sentence is completely coherent as phrased.'
    ),
    (
        'a1170000-0045-0000-0000-000000000045'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'EASY',
        'APPLY',
        'Identify the swap:

''The bank introduced a new **scheme** (A) to attract fixed **deposits** (B) from senior citizens.''',
        '[{"id": "A", "text": "No swap required", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "A with deposits", "isCorrect": false}, {"id": "D", "text": "B with citizens", "isCorrect": false}]',
        'A',
        'The phrase ''introduced a new scheme to attract fixed deposits'' is grammatically flawless.'
    ),
    (
        'a1170000-0046-0000-0000-000000000046'::uuid,
        'Word Swap & Sentence Reordering',
        'Four-Word Swap',
        'HARD',
        'APPLY',
        'Find the required swap:

''To **cushion** (A) the financial impact of loan defaults, commercial banks must maintain adequate capital **reserves** (B).'' ',
        '[{"id": "A", "text": "No swap required", "isCorrect": true}, {"id": "B", "text": "A - B", "isCorrect": false}, {"id": "C", "text": "A with defaults", "isCorrect": false}, {"id": "D", "text": "B with impact", "isCorrect": false}]',
        'A',
        'The sentence correctly pairs the verb ''cushion'' with maintaining ''capital reserves''.'
    ),
    (
        'a1170000-0047-0000-0000-000000000047'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'EASY',
        'REMEMBER',
        'What does the financial idiom **''in the red''** mean?',
        '[{"id": "A", "text": "Operating at a financial loss or in debt", "isCorrect": true}, {"id": "B", "text": "Making exceptionally high profits", "isCorrect": false}, {"id": "C", "text": "Undergoing a tax audit", "isCorrect": false}, {"id": "D", "text": "Investing in government securities", "isCorrect": false}]',
        'A',
        'In traditional accounting, negative balances were entered in red ink, signifying that a company is running at a financial loss or in debt.'
    ),
    (
        'a1170000-0048-0000-0000-000000000048'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'EASY',
        'REMEMBER',
        'What does the term **''bear market''** signify in financial terminology?',
        '[{"id": "A", "text": "A prolonged period of declining stock prices and widespread investor pessimism", "isCorrect": true}, {"id": "B", "text": "A market with rapidly escalating share prices", "isCorrect": false}, {"id": "C", "text": "A market exclusively trading agricultural commodities", "isCorrect": false}, {"id": "D", "text": "A regulatory trading halt", "isCorrect": false}]',
        'A',
        'A bear market describes a market condition characterized by falling prices (typically a 20% decline from recent peaks) and prevailing investor pessimism.'
    ),
    (
        'a1170000-0049-0000-0000-000000000049'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Phrasal Verbs & Usage',
        'MEDIUM',
        'UNDERSTAND',
        'What does the phrasal verb **''write off''** mean in banking parlance?',
        '[{"id": "A", "text": "Recognizing an uncollectible bad loan as an accounting loss and removing it from the active asset balance sheet", "isCorrect": true}, {"id": "B", "text": "Forgiving the borrower''s debt legally and returning all collateral", "isCorrect": false}, {"id": "C", "text": "Submitting an official resignation letter", "isCorrect": false}, {"id": "D", "text": "Increasing the interest rate on overdue credit", "isCorrect": false}]',
        'A',
        'Writing off a loan is an accounting entry where a bank recognizes an uncollectible debt as a loss, moving it to off-balance sheet recovery while legal recovery efforts continue.'
    ),
    (
        'a1170000-004a-0000-0000-00000000004a'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Phrasal Verbs & Usage',
        'MEDIUM',
        'UNDERSTAND',
        'What is a **''bailout''** in corporate and macroeconomic contexts?',
        '[{"id": "A", "text": "Financial assistance extended to a failing corporation or economy to prevent insolvency and systemic collapse", "isCorrect": true}, {"id": "B", "text": "A hostile takeover by corporate competitors", "isCorrect": false}, {"id": "C", "text": "A legal arrest warrant issued against fraudulent directors", "isCorrect": false}, {"id": "D", "text": "A dividend distribution to preferred shareholders", "isCorrect": false}]',
        'A',
        'A bailout is emergency capital assistance (usually provided by the government or multilateral institutions) to rescue an entity from financial collapse.'
    ),
    (
        'a1170000-004b-0000-0000-00000000004b'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'HARD',
        'ANALYZE',
        'What does the term **''moral hazard''** signify in banking and economics?',
        '[{"id": "A", "text": "A situation where an entity takes excessive risks because it knows that another party (like the government) will bear the financial cost of failure", "isCorrect": true}, {"id": "B", "text": "Ethical misconduct by bank tellers", "isCorrect": false}, {"id": "C", "text": "Refusing loans to hazardous industrial manufacturers", "isCorrect": false}, {"id": "D", "text": "The risk of counterfeit banknotes in currency chests", "isCorrect": false}]',
        'A',
        'Moral hazard arises when an entity has an incentive to increase its exposure to risk because it is protected from the negative consequences (e.g. ''too big to fail'').'
    ),
    (
        'a1170000-004c-0000-0000-00000000004c'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'MEDIUM',
        'REMEMBER',
        'What does the idiom **''cook the books''** mean?',
        '[{"id": "A", "text": "Fraudulently manipulating accounting records to falsify a company''s true financial condition", "isCorrect": true}, {"id": "B", "text": "Preparing annual budgets meticulously", "isCorrect": false}, {"id": "C", "text": "Publishing textbooks on monetary policy", "isCorrect": false}, {"id": "D", "text": "Digitizing paper bank ledgers", "isCorrect": false}]',
        'A',
        '''Cooking the books'' is an informal idiom meaning to falsify financial statements and conceal losses through accounting fraud.'
    ),
    (
        'a1170000-004d-0000-0000-00000000004d'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Phrasal Verbs & Usage',
        'EASY',
        'APPLY',
        'The government decided to **phase out** the old subsidy scheme. What does **''phase out''** mean?',
        '[{"id": "A", "text": "Discontinue or eliminate gradually in stages", "isCorrect": true}, {"id": "B", "text": "Expand rapidly nationwide", "isCorrect": false}, {"id": "C", "text": "Renegotiate with international partners", "isCorrect": false}, {"id": "D", "text": "Halt abruptly within 24 hours", "isCorrect": false}]',
        'A',
        'To ''phase out'' means to withdraw or terminate something in planned, incremental stages.'
    ),
    (
        'a1170000-004e-0000-0000-00000000004e'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'HARD',
        'UNDERSTAND',
        'What is a **''haircut''** in loan restructuring and debt resolution?',
        '[{"id": "A", "text": "The percentage reduction or write-down in the value of an asset or loan accepted by a lender as part of a debt settlement", "isCorrect": true}, {"id": "B", "text": "An administrative deduction from an employee''s salary", "isCorrect": false}, {"id": "C", "text": "The commission charged by stock brokers for executing equity trades", "isCorrect": false}, {"id": "D", "text": "A statutory audit penalty imposed by the central bank", "isCorrect": false}]',
        'A',
        'In debt restructuring, a ''haircut'' is the loss or concession accepted by creditors on their outstanding claims to reach a viable resolution.'
    ),
    (
        'a1170000-004f-0000-0000-00000000004f'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'MEDIUM',
        'REMEMBER',
        'What does the phrase **''helicopter money''** refer to in unconventional monetary economics?',
        '[{"id": "A", "text": "A direct, unbacked transfer of newly minted money from the central bank to the public to stimulate spending during severe deflation", "isCorrect": true}, {"id": "B", "text": "High-altitude air transport of cash to remote border posts", "isCorrect": false}, {"id": "C", "text": "Subsidized corporate aircraft leasing loans", "isCorrect": false}, {"id": "D", "text": "Foreign exchange earned through tourism aviation", "isCorrect": false}]',
        'A',
        'Coined by Milton Friedman, ''helicopter money'' refers to unconventional monetary policy involving direct distribution of cash to citizens to boost demand.'
    ),
    (
        'a1170000-0050-0000-0000-000000000050'::uuid,
        'Idioms, Phrasal Verbs & Vocabulary',
        'Financial Idioms & Expressions',
        'EASY',
        'REMEMBER',
        'What is a **''unicorn''** in venture capital and startup terminology?',
        '[{"id": "A", "text": "A privately held startup company valued at over $$1\\text{ billion US dollars}$$", "isCorrect": true}, {"id": "B", "text": "A listed corporation with zero debt", "isCorrect": false}, {"id": "C", "text": "A bank branch operating exclusively through solar power", "isCorrect": false}, {"id": "D", "text": "A company that has been nationalized by the state", "isCorrect": false}]',
        'A',
        'A unicorn is a privately held startup company that attains a valuation exceeding $$1\text{ billion}$$ dollars.'
    ),
    (
        'a1170000-0051-0000-0000-000000000051'::uuid,
        'Sentence Connectors and Starters',
        'Sentence Starters',
        'MEDIUM',
        'APPLY',
        'Select the connector that best combines the two sentences into a single grammatically correct statement:
$$I.$$ The government introduced comprehensive fiscal stimulus packages.
$$II.$$ Consumer demand remained subdued in rural markets.',
        '[{"id": "A", "text": "Although the government introduced comprehensive fiscal stimulus packages, consumer demand remained subdued in rural markets.", "isCorrect": true}, {"id": "B", "text": "Because the government introduced comprehensive stimulus packages, consumer demand remained subdued.", "isCorrect": false}, {"id": "C", "text": "Unless the government introduced comprehensive stimulus packages, consumer demand remained subdued.", "isCorrect": false}, {"id": "D", "text": "Since consumer demand was subdued, the government did not introduce stimulus.", "isCorrect": false}]',
        'A',
        '''Although'' expresses concession and contrast between the policy stimulus and the persistent sluggishness of rural demand.'
    ),
    (
        'a1170000-0052-0000-0000-000000000052'::uuid,
        'Sentence Connectors and Starters',
        'Discourse Connectors',
        'EASY',
        'APPLY',
        'Combine the two clauses: ''The bank upgraded its cybersecurity infrastructure. Customer credential phishing dropped by $$70\%$.''',
        '[{"id": "A", "text": "As a result of upgrading its cybersecurity infrastructure, customer credential phishing dropped by $$70\\%$$.", "isCorrect": true}, {"id": "B", "text": "Despite upgrading its cybersecurity infrastructure, phishing dropped.", "isCorrect": false}, {"id": "C", "text": "Nevertheless the bank upgraded its cybersecurity, phishing dropped.", "isCorrect": false}, {"id": "D", "text": "While phishing dropped by 70%, the bank upgraded nothing.", "isCorrect": false}]',
        'A',
        '''As a result of'' directly conveys the causal link between the infrastructure upgrade and the reduction in phishing.'
    ),
    (
        'a1170000-0053-0000-0000-000000000053'::uuid,
        'Sentence Connectors and Starters',
        'Sentence Starters',
        'HARD',
        'ANALYZE',
        'Which starter can properly begin the combined sentence?
''The monetary committee was aware of mounting geopolitical tensions; they chose to keep rates unchanged.''',
        '[{"id": "A", "text": "Notwithstanding the mounting geopolitical tensions, the monetary committee chose to keep rates unchanged.", "isCorrect": true}, {"id": "B", "text": "Owing to the mounting geopolitical tensions, the committee changed rates.", "isCorrect": false}, {"id": "C", "text": "Provided that geopolitical tensions mounted, rates were unchanged.", "isCorrect": false}, {"id": "D", "text": "In order that geopolitical tensions mount, rates were kept.", "isCorrect": false}]',
        'A',
        '''Notwithstanding'' (meaning in spite of / regardless of) correctly conveys that rate policy remained steady despite the presence of geopolitical tensions.'
    ),
    (
        'a1170000-0054-0000-0000-000000000054'::uuid,
        'Sentence Connectors and Starters',
        'Discourse Connectors',
        'MEDIUM',
        'APPLY',
        'Select the appropriate connector:
''The borrower failed to submit audited financial statements. The credit department rejected his term loan application.''',
        '[{"id": "A", "text": "Since the borrower failed to submit audited financial statements, the credit department rejected his term loan application.", "isCorrect": true}, {"id": "B", "text": "Although the borrower failed to submit statements, the loan was rejected.", "isCorrect": false}, {"id": "C", "text": "Even though the statements were missing, the application was rejected.", "isCorrect": false}, {"id": "D", "text": "Unless the borrower submitted statements, the application was approved.", "isCorrect": false}]',
        'A',
        '''Since'' articulates the clear cause-and-effect reason behind the loan rejection.'
    ),
    (
        'a1170000-0055-0000-0000-000000000055'::uuid,
        'Sentence Connectors and Starters',
        'Sentence Starters',
        'HARD',
        'ANALYZE',
        'Select the starter that correctly joins the sentences with negative inversion:
''The auditor barely completed the forensic audit when new accounting discrepancies came to light.''',
        '[{"id": "A", "text": "Hardly had the auditor completed the forensic audit when new accounting discrepancies came to light.", "isCorrect": true}, {"id": "B", "text": "No sooner had the auditor completed the audit when discrepancies appeared.", "isCorrect": false}, {"id": "C", "text": "Scarcely did the auditor completed the audit than discrepancies appeared.", "isCorrect": false}, {"id": "D", "text": "Barely the auditor had completed the audit then discrepancies appeared.", "isCorrect": false}]',
        'A',
        'The inverted correlative construction ''Hardly had + subject + V3 ... when'' correctly coordinates the two immediate actions.'
    ),
    (
        'a1170000-0056-0000-0000-000000000056'::uuid,
        'Sentence Connectors and Starters',
        'Discourse Connectors',
        'EASY',
        'APPLY',
        'Choose the best connector:
''The company suffered severe supply chain bottlenecks; _______, its quarterly earnings plunged.''',
        '[{"id": "A", "text": "consequently", "isCorrect": true}, {"id": "B", "text": "nevertheless", "isCorrect": false}, {"id": "C", "text": "on the contrary", "isCorrect": false}, {"id": "D", "text": "in spite of", "isCorrect": false}]',
        'A',
        '''Consequently'' properly denotes the financial fallout resulting from the supply bottlenecks.'
    ),
    (
        'a1170000-0057-0000-0000-000000000057'::uuid,
        'Sentence Connectors and Starters',
        'Sentence Starters',
        'MEDIUM',
        'APPLY',
        'Combine using ''In spite of'':
''The inflation rate was high. The central bank paused its rate hike cycle.''',
        '[{"id": "A", "text": "In spite of the high inflation rate, the central bank paused its rate hike cycle.", "isCorrect": true}, {"id": "B", "text": "In spite of the central bank paused, inflation was high.", "isCorrect": false}, {"id": "C", "text": "In spite that inflation was high, the bank paused.", "isCorrect": false}, {"id": "D", "text": "In spite of pausing rates, inflation was high.", "isCorrect": false}]',
        'A',
        '''In spite of'' is followed by a noun phrase (''In spite of the high inflation rate,...'') to express contrast.'
    ),
    (
        'a1170000-0058-0000-0000-000000000058'::uuid,
        'Sentence Connectors and Starters',
        'Discourse Connectors',
        'HARD',
        'ANALYZE',
        'Combine the two statements:
$$I.$$ The treasury division did not hedge its dollar exposure.
$$II.$$ The sudden rupee depreciation inflicted heavy trading losses.',
        '[{"id": "A", "text": "Because the treasury division had not hedged its dollar exposure, the sudden rupee depreciation inflicted heavy trading losses.", "isCorrect": true}, {"id": "B", "text": "Although the treasury had hedged exposure, losses occurred.", "isCorrect": false}, {"id": "C", "text": "Unless the treasury hedged exposure, no losses would occur.", "isCorrect": false}, {"id": "D", "text": "Despite not hedging exposure, heavy losses occurred.", "isCorrect": false}]',
        'A',
        'The unhedged exposure was the causal reason why the currency depreciation inflicted losses; ''Because...'' correctly captures this.'
    ),
    (
        'a1170000-0059-0000-0000-000000000059'::uuid,
        'Sentence Connectors and Starters',
        'Discourse Connectors',
        'EASY',
        'APPLY',
        'Choose the correct connector:
''You must complete your full KYC verification; _______, your account will be frozen.''',
        '[{"id": "A", "text": "otherwise", "isCorrect": true}, {"id": "B", "text": "likewise", "isCorrect": false}, {"id": "C", "text": "furthermore", "isCorrect": false}, {"id": "D", "text": "nevertheless", "isCorrect": false}]',
        'A',
        '''Otherwise'' expresses the adverse conditional consequence of failing to complete KYC.'
    ),
    (
        'a1170000-005a-0000-0000-00000000005a'::uuid,
        'Sentence Connectors and Starters',
        'Sentence Starters',
        'MEDIUM',
        'APPLY',
        'Combine the two clauses:
''The stock plunged. Rumors of executive fraud began circulating.''',
        '[{"id": "A", "text": "No sooner had rumors of executive fraud begun circulating than the stock plunged.", "isCorrect": true}, {"id": "B", "text": "No sooner rumors began circulating when the stock plunged.", "isCorrect": false}, {"id": "C", "text": "Hardly had rumors begun circulating than the stock plunged.", "isCorrect": false}, {"id": "D", "text": "Scarcely had rumors begun circulating then the stock plunged.", "isCorrect": false}]',
        'A',
        'The correlative pair ''No sooner had... than...'' correctly coordinates the prompt occurrence.'
    ),
    (
        'a1170000-005b-0000-0000-00000000005b'::uuid,
        'Match the Columns',
        'Grammatical Fragment Matching',
        'MEDIUM',
        'APPLY',
        '**Match Column 1 with Column 2 to form grammatically and contextually correct sentences**:

**Column 1**:
- **(A)** The central bank raised the cash reserve ratio
- **(B)** Several commercial lenders faced liquidity crunches
- **(C)** The government introduced targeted subsidies

**Column 2**:
- **(D)** to alleviate acute distress among smallholder farmers.
- **(E)** to mop up excess speculative liquidity from the system.
- **(F)** following the sudden withdrawal of bulk institutional deposits.

Which combination is correct?',
        '[{"id": "A", "text": "A-E, B-F, C-D", "isCorrect": true}, {"id": "B", "text": "A-D, B-E, C-F", "isCorrect": false}, {"id": "C", "text": "A-F, B-D, C-E", "isCorrect": false}, {"id": "D", "text": "A-E, B-D, C-F", "isCorrect": false}]',
        'A',
        'A pairs with E (CRR raised to mop up liquidity); B pairs with F (liquidity crunch following bulk deposit withdrawal); C pairs with D (subsidies to alleviate distress).'
    ),
    (
        'a1170000-005c-0000-0000-00000000005c'::uuid,
        'Match the Columns',
        'Economic Discourse Matching',
        'EASY',
        'APPLY',
        '**Match the Columns**:
**Column 1**:
- **(A)** A hike in the policy repo rate
- **(B)** Digital tokenisation safeguards customer cards

**Column 2**:
- **(C)** by replacing sensitive PAN digits with unique algorithmic strings.
- **(D)** increases borrowing costs across retail and corporate loans.

Which pairs match?',
        '[{"id": "A", "text": "A-D and B-C", "isCorrect": true}, {"id": "B", "text": "A-C and B-D", "isCorrect": false}, {"id": "C", "text": "A-D only", "isCorrect": false}, {"id": "D", "text": "B-C only", "isCorrect": false}]',
        'A',
        'Repo rate hikes raise borrowing costs (A-D), and tokenisation replaces card digits with unique strings (B-C).'
    ),
    (
        'a1170000-005d-0000-0000-00000000005d'::uuid,
        'Match the Columns',
        'Grammatical Fragment Matching',
        'HARD',
        'ANALYZE',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** Had the regulatory framework been enforced stringently,
- **(B)** The monetary easing stance will continue,

**Column 2**:
- **(C)** the massive capital accounting fraud could have been prevented.
- **(D)** provided headline inflation remains within the statutory tolerance band.

Which match is valid?',
        '[{"id": "A", "text": "Both A-C and B-D are grammatically and logically valid", "isCorrect": true}, {"id": "B", "text": "A-D and B-C", "isCorrect": false}, {"id": "C", "text": "A-C only", "isCorrect": false}, {"id": "D", "text": "B-D only", "isCorrect": false}]',
        'A',
        'A-C forms a valid inverted third conditional (''Had it been enforced... could have been prevented''). B-D forms a valid first conditional with ''provided''.'
    ),
    (
        'a1170000-005e-0000-0000-00000000005e'::uuid,
        'Match the Columns',
        'Economic Discourse Matching',
        'MEDIUM',
        'APPLY',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** Priority sector lending guidelines mandate banks
- **(B)** Sovereign Gold Bonds offer capital appreciation

**Column 2**:
- **(C)** along with an annual fixed interest rate of 2.50%.
- **(D)** to channel at least 40% of net bank credit to designated sectors.

Which combination matches?',
        '[{"id": "A", "text": "A-D and B-C", "isCorrect": true}, {"id": "B", "text": "A-C and B-D", "isCorrect": false}, {"id": "C", "text": "A-D only", "isCorrect": false}, {"id": "D", "text": "B-C only", "isCorrect": false}]',
        'A',
        'PSL targets require 40% lending to priority sectors (A-D); SGBs provide gold capital appreciation plus 2.50% annual interest (B-C).'
    ),
    (
        'a1170000-005f-0000-0000-00000000005f'::uuid,
        'Match the Columns',
        'Grammatical Fragment Matching',
        'EASY',
        'APPLY',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** The branch manager instructed the clerk
- **(B)** The auditor verified all transaction logs

**Column 2**:
- **(C)** to reconcile cash ledger balances before closing the counter.
- **(D)** to check for unauthorized administrative system overrides.

Which pair is correct?',
        '[{"id": "A", "text": "Both A-C and B-D are correct", "isCorrect": true}, {"id": "B", "text": "A-D and B-C", "isCorrect": false}, {"id": "C", "text": "A-C only", "isCorrect": false}, {"id": "D", "text": "B-D only", "isCorrect": false}]',
        'A',
        'Both pairs are syntactically and semantically harmonious.'
    ),
    (
        'a1170000-0060-0000-0000-000000000060'::uuid,
        'Match the Columns',
        'Economic Discourse Matching',
        'HARD',
        'ANALYZE',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** The Insolvency and Bankruptcy Code prioritizes
- **(B)** The Prompt Corrective Action framework intervenes

**Column 2**:
- **(C)** when a bank''s capital adequacy ratio deteriorates below threshold.
- **(D)** time-bound corporate debt reorganization over asset liquidation.

Which matches correctly?',
        '[{"id": "A", "text": "A-D and B-C", "isCorrect": true}, {"id": "B", "text": "A-C and B-D", "isCorrect": false}, {"id": "C", "text": "A-D only", "isCorrect": false}, {"id": "D", "text": "B-C only", "isCorrect": false}]',
        'A',
        'IBC emphasizes time-bound debt restructuring over liquidation (A-D); PCA framework triggers upon capital deterioration (B-C).'
    ),
    (
        'a1170000-0061-0000-0000-000000000061'::uuid,
        'Match the Columns',
        'Grammatical Fragment Matching',
        'MEDIUM',
        'APPLY',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** Neither the board chairman
- **(B)** Either the internal auditor

**Column 2**:
- **(C)** nor the managing directors have approved the dividend payout.
- **(D)** or the compliance officer was responsible for submitting the filing.

Which matches correctly?',
        '[{"id": "A", "text": "Both A-C and B-D are grammatically concordant", "isCorrect": true}, {"id": "B", "text": "A-D and B-C", "isCorrect": false}, {"id": "C", "text": "A-C only", "isCorrect": false}, {"id": "D", "text": "B-D only", "isCorrect": false}]',
        'A',
        'A-C follows ''neither... nor'' with plural agreement (''managing directors have approved''); B-D follows ''either... or'' with singular agreement (''compliance officer was responsible'').'
    ),
    (
        'a1170000-0062-0000-0000-000000000062'::uuid,
        'Match the Columns',
        'Economic Discourse Matching',
        'EASY',
        'APPLY',
        '**Match the Columns**:
**Column 1**:
- **(A)** The Standing Deposit Facility absorbs funds
- **(B)** The Marginal Standing Facility provides emergency funds

**Column 2**:
- **(C)** overnight without requiring government securities collateral.
- **(D)** against the pledge of approved securities dipping into SLR.

Which pairs match?',
        '[{"id": "A", "text": "A-C and B-D", "isCorrect": true}, {"id": "B", "text": "A-D and B-C", "isCorrect": false}, {"id": "C", "text": "A-C only", "isCorrect": false}, {"id": "D", "text": "B-D only", "isCorrect": false}]',
        'A',
        'SDF absorbs uncollateralized liquidity (A-C); MSF lends overnight dipping into SLR (B-D).'
    ),
    (
        'a1170000-0063-0000-0000-000000000063'::uuid,
        'Match the Columns',
        'Grammatical Fragment Matching',
        'HARD',
        'ANALYZE',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** Not only did the treasury earn windfall bond profits,
- **(B)** No sooner had the market opened,

**Column 2**:
- **(C)** than the benchmark index skyrocketed by 500 points.
- **(D)** but it also curtailed operating expenses drastically.

Which matches correctly?',
        '[{"id": "A", "text": "A-D and B-C", "isCorrect": true}, {"id": "B", "text": "A-C and B-D", "isCorrect": false}, {"id": "C", "text": "A-D only", "isCorrect": false}, {"id": "D", "text": "B-C only", "isCorrect": false}]',
        'A',
        '''Not only did... but it also...'' connects A with D; ''No sooner had... than...'' connects B with C.'
    ),
    (
        'a1170000-0064-0000-0000-000000000064'::uuid,
        'Match the Columns',
        'Economic Discourse Matching',
        'MEDIUM',
        'APPLY',
        '**Match Column 1 with Column 2**:
**Column 1**:
- **(A)** Automated clearing house systems facilitate
- **(B)** Point-of-Sale terminals allow retail consumers

**Column 2**:
- **(C)** bulk recurring payments like salaries, pensions, and utility debits.
- **(D)** to make instantaneous card and contactless payments at merchant stores.

Which pairs match correctly?',
        '[{"id": "A", "text": "Both A-C and B-D are correct", "isCorrect": true}, {"id": "B", "text": "A-D and B-C", "isCorrect": false}, {"id": "C", "text": "A-C only", "isCorrect": false}, {"id": "D", "text": "B-D only", "isCorrect": false}]',
        'A',
        'NACH manages bulk recurring payments (A-C); POS terminals handle retail payments at merchant checkouts (B-D).'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = 'English Language and Comprehension' AND s.tenant_id = 'default'
JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
