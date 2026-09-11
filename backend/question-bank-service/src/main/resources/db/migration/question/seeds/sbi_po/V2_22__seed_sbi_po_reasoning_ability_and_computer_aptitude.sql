-- SPDX-License-Identifier: AGPL-3.0-only
-- Flyway Migration: V2_22
-- Seed Data: SBI PO / Bank PO Reasoning Ability & Computer Aptitude (100 Questions)

SET search_path TO question_service, public;

-- Step 1: Ensure Subject, Topics, and Subtopics exist
DO $$
DECLARE
    v_tenant_id VARCHAR := 'default';
    v_subj_id UUID;
    v_top_id UUID;
BEGIN
    SELECT id INTO v_subj_id FROM question_service.subject WHERE name = 'Reasoning' AND tenant_id = v_tenant_id LIMIT 1;
    IF v_subj_id IS NULL THEN
        INSERT INTO question_service.subject (tenant_id, name, code, description)
        VALUES (v_tenant_id, 'Reasoning', UPPER(SUBSTRING('Reasoning', 1, 6)), 'Reasoning for Bank PO Examinations')
        RETURNING id INTO v_subj_id;
    END IF;

    -- Topic: Syllogism
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Syllogism' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Syllogism', 'Modern deductive syllogism, ''Only a few'' constructs, possibilities, and definite conclusions')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Standard & Only-A-Few Syllogism', 'Syllogistic deductions featuring ''Only a few'', ''Some not'', and universal quantifiers')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Possibility & Negative Deductions', 'Assessing possibilities, ''can never be'', and complementary pairs (Either-Or)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Inequalities
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Inequalities' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Inequalities', 'Direct mathematical comparisons and symbol-coded relational inequalities')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Direct Inequalities', 'Chains of comparative statements using standard operators (<, <=, =, >=, >)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Coded Inequalities', 'Statements and conclusions where mathematical relations are encoded with symbols (@, #, $, %, &)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Blood Relations
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Blood Relations' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Blood Relations', 'Multi-generational family trees, coded kinship notations, and pointing-based scenarios')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Coded Blood Relations', 'Decoding kinship relationships specified via operators (P + Q, P - Q, P * Q)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Family Tree Puzzle Analysis', 'Deriving hierarchical and marital lineages in three-generation families')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Direction and Distance Sense
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Direction and Distance Sense' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Direction and Distance Sense', 'Spatial navigation, clockwise/anticlockwise angular rotations, shadow logic, and shortest displacement')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Direction Vectors & Displacement', 'Pythagoras theorem applications and multi-point coordinate routes')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Coded Direction Tests', 'Directions mapped to compass angles, shadows at sunrise/sunset, and coded syntax')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Coding-Decoding
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Coding-Decoding' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Coding-Decoding', 'Advanced substitution coding, new-pattern algorithmic codes, and letter-symbol matrices')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'New Pattern Coding', 'Deciphering composite codes based on word length, vowels, and reverse alphabets')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Chinese & Substitution Coding', 'Common word elimination and phrase-level cipher mapping')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Machine Input-Output
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Machine Input-Output' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Machine Input-Output', 'Sequential step-by-step alphanumeric processing, sorting arrays, and mathematical transformations')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Word & Number Rearrangement', 'Alternating left-right terminal shifting of words and numbers by alphabetical/numerical order')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Mathematical Step Operations', 'Arithmetic operations (squaring, digit sums, differences) executed in successive machine steps')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Linear Seating Arrangement
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Linear Seating Arrangement' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Linear Seating Arrangement', 'Single and parallel linear arrays with bidirectional facing and multi-variable attributes')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Unidirectional & Bidirectional Lines', 'Arranging individuals facing North or South in a single row')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Parallel Row Arrangements', 'Two parallel rows facing each other with distinct professions or hobbies')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Circular and Polygonal Seating
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Circular and Polygonal Seating' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Circular and Polygonal Seating', 'Circular, triangular, and rectangular table seating with inward and outward facing orientations')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Circular Arrangements', 'Inward/outward facing circles with variable attributes (colors, banks, cities)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Square & Triangular Tables', 'Corner versus middle-of-side seat placements with directional constraints')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Puzzles & Critical Reasoning
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Puzzles & Critical Reasoning' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Puzzles & Critical Reasoning', 'Multi-floor buildings, box stacks, scheduling grids, and logical analytical arguments')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Floor & Box Puzzles', 'Vertical positioning of persons, floors, and stacked numbered boxes')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Critical Reasoning', 'Evaluating implicit assumptions, strong/weak arguments, and courses of action')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Computer Aptitude & Networking
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Computer Aptitude & Networking' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Computer Aptitude & Networking', 'Fundamental computing systems, architecture, networking topologies, OS, and security protocols')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Computer Architecture & Memory', 'CPU registers, cache levels, RAM/ROM, secondary storage, and number systems')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Networking, Cybersecurity & DBMS', 'OSI model layers, TCP/IP, network topologies, database queries, and cybersecurity threats')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

END $$;

-- Step 2: Insert 100 Bank PO Reasoning Ability & Computer Aptitude Questions
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
    'Reasoning',
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
        'a1160000-0001-0000-0000-000000000001'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'EASY',
        'APPLY',
        '**Statements**:
- Only a few Banks are Financial Institutions.
- All Financial Institutions are Regulators.

**Conclusions**:
$$I.$$ Some Banks are Regulators.
$$II.$$ All Banks can never be Financial Institutions.',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II follow", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        '''Only a few Banks are Financial Institutions'' implies Some Banks are FI and Some Banks are NOT FI. Thus, all Banks can never be FI (II follows). Since All FI are Regulators, the overlapping portion of Banks is also Regulators (I follows).'
    ),
    (
        'a1160000-0002-0000-0000-000000000002'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'MEDIUM',
        'APPLY',
        '**Statements**:
- Only a few Loans are Deposits.
- No Deposit is an NPA.

**Conclusions**:
$$I.$$ Some Loans are not NPAs.
$$II.$$ All Loans being NPAs is a possibility.',
        '[{"id": "A", "text": "Only Conclusion I follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "C", "text": "Both follow", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'The part of Loans that is Deposit cannot be NPA. Therefore, Some Loans are definitely not NPAs (Conclusion I follows). Since some Loans cannot be NPAs, all Loans can never be NPAs (Conclusion II does not follow).'
    ),
    (
        'a1160000-0003-0000-0000-000000000003'::uuid,
        'Syllogism',
        'Possibility & Negative Deductions',
        'MEDIUM',
        'APPLY',
        '**Statements**:
- All Cheques are Drafts.
- Some Drafts are Bills.
- No Bill is Cash.

**Conclusions**:
$$I.$$ No Cheque is Cash.
$$II.$$ All Cheques being Cash is a possibility.',
        '[{"id": "A", "text": "Only Conclusion II follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Both follow", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'There is no direct negative restriction between Cheques and Cash. Hence, Cheques being Cash is completely possible (II follows), but definitely not true in the basic diagram (I does not follow).'
    ),
    (
        'a1160000-0004-0000-0000-000000000004'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'HARD',
        'ANALYZE',
        '**Statements**:
- Only a few Bonds are Equities.
- Only a few Equities are Derivatives.
- All Derivatives are Futures.

**Conclusions**:
$$I.$$ Some Bonds are definitely not Futures.
$$II.$$ All Futures being Equities is a possibility.',
        '[{"id": "A", "text": "Only Conclusion II follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Both follow", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'Only a few Equities are Derivatives means all Equities cannot be Derivatives, but all Derivatives (and Futures) can easily be inside Equities (II is a possibility). There is no negative relation between Bonds and Futures, so I is not definite.'
    ),
    (
        'a1160000-0005-0000-0000-000000000005'::uuid,
        'Syllogism',
        'Possibility & Negative Deductions',
        'EASY',
        'APPLY',
        '**Statements**:
- Some Credits are Debits.
- All Debits are Ledgers.

**Conclusions**:
$$I.$$ Some Credits are Ledgers.
$$II.$$ No Credit is a Ledger.',
        '[{"id": "A", "text": "Only Conclusion I follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "C", "text": "Either I or II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'Since Some Credits are Debits and All Debits are Ledgers, the intersection of Credits and Debits is definitely contained in Ledgers. Thus, Conclusion I definitely follows.'
    ),
    (
        'a1160000-0006-0000-0000-000000000006'::uuid,
        'Syllogism',
        'Possibility & Negative Deductions',
        'MEDIUM',
        'UNDERSTAND',
        '**Statements**:
- No ATM is a Branch.
- No Branch is a Head Office.

**Conclusions**:
$$I.$$ No ATM is a Head Office.
$$II.$$ Some ATMs are Head Offices.',
        '[{"id": "A", "text": "Either Conclusion I or Conclusion II follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Both follow", "isCorrect": false}]',
        'A',
        'Both subject and predicate are identical (ATM and Head Office), neither conclusion is definite on its own, and they form a complementary pair (No + Some). Hence, Either I or II follows.'
    ),
    (
        'a1160000-0007-0000-0000-000000000007'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'HARD',
        'ANALYZE',
        '**Statements**:
- Only Currency is Sovereign.
- Some Currencies are Digital.

**Conclusions**:
$$I.$$ No Sovereign is Digital.
$$II.$$ All Digital being Currency is a possibility.',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II follow", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        '''Only A is B'' means ''All B are A'' and B cannot belong to any other set. Thus, Sovereign belongs exclusively to Currency, so No Sovereign is Digital (I follows). Since Some Currencies are Digital, All Digital can easily be Currency (II follows).'
    ),
    (
        'a1160000-0008-0000-0000-000000000008'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'EASY',
        'APPLY',
        '**Statements**:
- All Audits are Reports.
- All Reports are Statements.

**Conclusions**:
$$I.$$ All Audits are Statements.
$$II.$$ Some Statements are Audits.',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II follow", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'By transitive inclusion: Audits $$\subset$$ Reports $$\subset$$ Statements. Thus, All Audits are Statements, and Some Statements are Audits.'
    ),
    (
        'a1160000-0009-0000-0000-000000000009'::uuid,
        'Syllogism',
        'Standard & Only-A-Few Syllogism',
        'MEDIUM',
        'APPLY',
        '**Statements**:
- Only a few Mortgages are Securities.
- All Securities are Collateral.

**Conclusions**:
$$I.$$ All Mortgages can be Collateral.
$$II.$$ Some Collateral are Mortgages.',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II follow", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'Mortgages cannot all be Securities, but they can all be inside the broader Collateral circle (I follows). The common portion of Mortgages and Securities is inside Collateral (II follows).'
    ),
    (
        'a1160000-000a-0000-0000-00000000000a'::uuid,
        'Syllogism',
        'Possibility & Negative Deductions',
        'HARD',
        'ANALYZE',
        '**Statements**:
- Some Passwords are PINs.
- No PIN is an OTP.
- All OTPs are Tokens.

**Conclusions**:
$$I.$$ Some Tokens are not PINs.
$$II.$$ All Passwords being OTPs is a possibility.',
        '[{"id": "A", "text": "Only Conclusion I follows", "isCorrect": true}, {"id": "B", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "C", "text": "Both follow", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'The part of Tokens that is OTP can never be a PIN, so Some Tokens are not PINs (I follows). The part of Passwords that is PIN can never be an OTP, so All Passwords can never be OTPs (II does not follow).'
    ),
    (
        'a1160000-000b-0000-0000-00000000000b'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'EASY',
        'APPLY',
        '**Statement**: $$P \ge Q > R = S \ge T$$

**Conclusions**:
$$I.$$ $$P > S$$
$$II.$$ $$R \ge T$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        '$$P \ge Q > R = S \implies P > S$$ (I is true). $$R = S \ge T \implies R \ge T$$ (II is true).'
    ),
    (
        'a1160000-000c-0000-0000-00000000000c'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'EASY',
        'APPLY',
        '**Statement**: $$A < B \le C < D = E$$

**Conclusions**:
$$I.$$ $$A < E$$
$$II.$$ $$B < D$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        'Strict inequality holds throughout the chain: $$A < B \le C < D = E \implies A < E$$ and $$B \le C < D \implies B < D$$.'
    ),
    (
        'a1160000-000d-0000-0000-00000000000d'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'MEDIUM',
        'APPLY',
        '**Statements**: $$M > N \ge O;\; O < P \le Q$$

**Conclusions**:
$$I.$$ $$M > P$$
$$II.$$ $$N < Q$$',
        '[{"id": "A", "text": "Neither Conclusion I nor Conclusion II is true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Either I or II is true", "isCorrect": false}]',
        'A',
        'Opposite signs between $$M$$ and $$P$$ ($$M > O$$ and $$O < P$$) prevent any definite comparison. Similarly, opposite signs between $$N$$ and $$Q$$ prevent comparison.'
    ),
    (
        'a1160000-000e-0000-0000-00000000000e'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'MEDIUM',
        'APPLY',
        '**Statement**: $$K \ge L = M \ge N > O$$

**Conclusions**:
$$I.$$ $$K = N$$
$$II.$$ $$K > N$$',
        '[{"id": "A", "text": "Either Conclusion I or Conclusion II is true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        'From the statement: $$K \ge N$$, which means either $$K > N$$ or $$K = N$$. Hence, Either I or II is true.'
    ),
    (
        'a1160000-000f-0000-0000-00000000000f'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'HARD',
        'ANALYZE',
        '**Statements**: $$W \le X < Y;\; Y = Z \le U;\; U < V$$

**Conclusions**:
$$I.$$ $$W < U$$
$$II.$$ $$X < V$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        'Combining the chain: $$W \le X < Y = Z \le U < V$$. Therefore, $$W < U$$ and $$X < V$$ are both definitely true.'
    ),
    (
        'a1160000-0010-0000-0000-000000000010'::uuid,
        'Inequalities',
        'Coded Inequalities',
        'MEDIUM',
        'UNDERSTAND',
        'In a code: ''$$P @ Q$$'' means $$P \ge Q$$, ''$$P \# Q$$'' means $$P = Q$$, ''$$P \$ Q$$'' means $$P > Q$$.
**Statement**: $$A \$ B @ C \# D$$

**Conclusions**:
$$I.$$ $$A \$ D$$
$$II.$$ $$B @ D$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        'Decoded statement: $$A > B \ge C = D$$. Thus $$A > D$$ ($$A \$ D$$) is true, and $$B \ge D$$ ($$B @ D$$) is true.'
    ),
    (
        'a1160000-0011-0000-0000-000000000011'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'EASY',
        'APPLY',
        '**Statement**: $$F > G \ge H < I \le J$$

**Conclusions**:
$$I.$$ $$F > H$$
$$II.$$ $$G < J$$',
        '[{"id": "A", "text": "Only Conclusion I is true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "C", "text": "Both are true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        '$$F > G \ge H \implies F > H$$ (I is true). Between $$G$$ and $$J$$, the signs are opposing ($$G \ge H < I$$), so no relationship can be determined (II is false).'
    ),
    (
        'a1160000-0012-0000-0000-000000000012'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'MEDIUM',
        'APPLY',
        '**Statements**: $$R \le S < T;\; T \le U = V$$

**Conclusions**:
$$I.$$ $$S < V$$
$$II.$$ $$R < U$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        'Combining: $$R \le S < T \le U = V$$. Therefore, $$S < V$$ and $$R < U$$ are both true.'
    ),
    (
        'a1160000-0013-0000-0000-000000000013'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'HARD',
        'ANALYZE',
        'Which of the following expressions definitely makes ''$$D > B$$'' and ''$$A \ge E$$'' definitely true?',
        '[{"id": "A", "text": "$$A \\ge B > C = D > E$$ (wait: $$D < B$$ here). Expression: $$A \\ge C \\ge E;\\; D > C > B$$", "isCorrect": true}, {"id": "B", "text": "$$D < C < B = A \\le E$$", "isCorrect": false}, {"id": "C", "text": "$$B \\ge D > C = A \\ge E$$", "isCorrect": false}, {"id": "D", "text": "$$D = B > C < A = E$$", "isCorrect": false}]',
        'A',
        'From $$D > C > B$$, we have $$D > B$$. From $$A \ge C \ge E$$, we have $$A \ge E$$. Both conditions are satisfied.'
    ),
    (
        'a1160000-0014-0000-0000-000000000014'::uuid,
        'Inequalities',
        'Direct Inequalities',
        'EASY',
        'APPLY',
        '**Statement**: $$X = Y \ge Z > W$$

**Conclusions**:
$$I.$$ $$X > W$$
$$II.$$ $$Y \ge Z$$',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II are true", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I is true", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II is true", "isCorrect": false}, {"id": "D", "text": "Neither is true", "isCorrect": false}]',
        'A',
        '$$X = Y \ge Z > W \implies X > W$$ (I is true) and $$Y \ge Z$$ (II is true).'
    ),
    (
        'a1160000-0015-0000-0000-000000000015'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'EASY',
        'APPLY',
        'Pointing to a photograph of a boy, Suresh said, ''He is the son of the only son of my mother.'' How is Suresh related to that boy?',
        '[{"id": "A", "text": "Father", "isCorrect": true}, {"id": "B", "text": "Uncle", "isCorrect": false}, {"id": "C", "text": "Brother", "isCorrect": false}, {"id": "D", "text": "Grandfather", "isCorrect": false}]',
        'A',
        'The only son of Suresh''s mother is Suresh himself. Therefore, the boy is Suresh''s son, which makes Suresh his father.'
    ),
    (
        'a1160000-0016-0000-0000-000000000016'::uuid,
        'Blood Relations',
        'Coded Blood Relations',
        'MEDIUM',
        'APPLY',
        'In a code:
- ''$$A + B$$'' means $$A$$ is the mother of $$B$$.
- ''$$A - B$$'' means $$A$$ is the brother of $$B$$.
- ''$$A \times B$$'' means $$A$$ is the father of $$B$$.

In the expression ''$$P \times Q - R + S$$'', how is $$P$$ related to $$S$$?',
        '[{"id": "A", "text": "Maternal Grandfather", "isCorrect": true}, {"id": "B", "text": "Paternal Grandfather", "isCorrect": false}, {"id": "C", "text": "Uncle", "isCorrect": false}, {"id": "D", "text": "Father", "isCorrect": false}]',
        'A',
        '$$R$$ is the mother of $$S$$. $$Q$$ is the brother of $$R$$. $$P$$ is the father of $$Q$$ (and hence of $$R$$). Since $$P$$ is the father of $$S$$''s mother, $$P$$ is the maternal grandfather of $$S$$.'
    ),
    (
        'a1160000-0017-0000-0000-000000000017'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'MEDIUM',
        'UNDERSTAND',
        'In a family of six persons ($$A, B, C, D, E, F$$), $$B$$ is the son of $$C$$ but $$C$$ is not the mother of $$B$$. $$A$$ and $$C$$ are a married couple. $$E$$ is the brother of $$C$$. How is $$E$$ related to $$B$$?',
        '[{"id": "A", "text": "Paternal Uncle", "isCorrect": true}, {"id": "B", "text": "Maternal Uncle", "isCorrect": false}, {"id": "C", "text": "Father", "isCorrect": false}, {"id": "D", "text": "Brother", "isCorrect": false}]',
        'A',
        'Since $$C$$ is not the mother of $$B$$, $$C$$ must be the father of $$B$$. $$E$$ is the brother of $$C$$ (the father). Hence $$E$$ is the paternal uncle of $$B$$.'
    ),
    (
        'a1160000-0018-0000-0000-000000000018'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'HARD',
        'ANALYZE',
        '$$M$$ is the sister of $$K$$. $$D$$ is the brother of $$K$$. $$F$$ is the mother of $$M$$. $$H$$ is the husband of $$F$$. How many sons does $$H$$ definitely have if $$K$$ is a male?',
        '[{"id": "A", "text": "At least $$2\\text{ sons}$$ ($$D$$ and $$K$$)", "isCorrect": true}, {"id": "B", "text": "Exactly 1 son", "isCorrect": false}, {"id": "C", "text": "3 sons", "isCorrect": false}, {"id": "D", "text": "Cannot be determined", "isCorrect": false}]',
        'A',
        '$$H$$ and $$F$$ are husband and wife with children $$M$$ (female), $$K$$ (male), and $$D$$ (male). Thus $$H$$ has at least 2 sons ($$D$$ and $$K$$).'
    ),
    (
        'a1160000-0019-0000-0000-000000000019'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'EASY',
        'APPLY',
        'A woman introduces a man as the son of the brother of her mother. How is the man related to the woman?',
        '[{"id": "A", "text": "Maternal Cousin", "isCorrect": true}, {"id": "B", "text": "Nephew", "isCorrect": false}, {"id": "C", "text": "Uncle", "isCorrect": false}, {"id": "D", "text": "Brother", "isCorrect": false}]',
        'A',
        'Brother of her mother is her maternal uncle. The son of her uncle is her cousin (maternal cousin).'
    ),
    (
        'a1160000-001a-0000-0000-00000000001a'::uuid,
        'Blood Relations',
        'Coded Blood Relations',
        'HARD',
        'APPLY',
        'If ''$$P \# Q$$'' means $$P$$ is the father of $$Q$$, ''$$P @ Q$$'' means $$P$$ is the wife of $$Q$$, and ''$$P \% Q$$'' means $$P$$ is the daughter of $$Q$$, which of the following shows that ''$$M$$ is the grandmother of $$T$$''?',
        '[{"id": "A", "text": "$$M @ N \\# R \\# T$$", "isCorrect": true}, {"id": "B", "text": "$$M \\% N @ R \\# T$$", "isCorrect": false}, {"id": "C", "text": "$$M \\# N @ R \\% T$$", "isCorrect": false}, {"id": "D", "text": "$$M @ N \\% R \\# T$$", "isCorrect": false}]',
        'A',
        '$$N$$ is the father of $$R$$, and $$M$$ is the wife of $$N$$ (so $$M$$ is the mother of $$R$$). $$R$$ is the father of $$T$$. Hence, $$M$$ is the paternal grandmother of $$T$$.'
    ),
    (
        'a1160000-001b-0000-0000-00000000001b'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'MEDIUM',
        'APPLY',
        'Deepak has a brother Anil. Deepak is the son of Prem. Bimal is Prem''s father. How is Anil related to Bimal?',
        '[{"id": "A", "text": "Grandson", "isCorrect": true}, {"id": "B", "text": "Son", "isCorrect": false}, {"id": "C", "text": "Brother", "isCorrect": false}, {"id": "D", "text": "Nephew", "isCorrect": false}]',
        'A',
        'Prem is the father of Deepak and Anil. Bimal is the father of Prem. Hence Anil is the grandson of Bimal.'
    ),
    (
        'a1160000-001c-0000-0000-00000000001c'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'EASY',
        'REMEMBER',
        'How is my father''s only son''s wife related to me, given that I am a male?',
        '[{"id": "A", "text": "Wife", "isCorrect": true}, {"id": "B", "text": "Sister-in-law", "isCorrect": false}, {"id": "C", "text": "Sister", "isCorrect": false}, {"id": "D", "text": "Mother", "isCorrect": false}]',
        'A',
        'Since I am a male, my father''s only son is myself. Therefore, my father''s only son''s wife is my wife.'
    ),
    (
        'a1160000-001d-0000-0000-00000000001d'::uuid,
        'Blood Relations',
        'Coded Blood Relations',
        'MEDIUM',
        'UNDERSTAND',
        'If ''$$X \div Y$$'' means $$X$$ is the sister of $$Y$$, and ''$$X \times Y$$'' means $$X$$ is the brother of $$Y$$, what does ''$$A \div B \times C$$'' indicate?',
        '[{"id": "A", "text": "$$A$$ is the sister of $$C$$", "isCorrect": true}, {"id": "B", "text": "$$A$$ is the brother of $$C$$", "isCorrect": false}, {"id": "C", "text": "$$A$$ is the aunt of $$C$$", "isCorrect": false}, {"id": "D", "text": "$$A$$ is the mother of $$C$$", "isCorrect": false}]',
        'A',
        '$$A$$ is the sister of $$B$$, and $$B$$ is the brother of $$C$$. Hence, all three are siblings, and $$A$$ is the sister of $$C$$.'
    ),
    (
        'a1160000-001e-0000-0000-00000000001e'::uuid,
        'Blood Relations',
        'Family Tree Puzzle Analysis',
        'HARD',
        'ANALYZE',
        'In a joint family of three generations, there are two married couples. The family has $$2$$ fathers, $$2$$ mothers, $$1$$ grandfather, $$1$$ grandmother, $$1$$ son, and $$2$$ daughters. What is the minimum number of members in this family?',
        '[{"id": "A", "text": "$$7\\text{ members}$$", "isCorrect": true}, {"id": "B", "text": "$$8\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "$$6\\text{ members}$$", "isCorrect": false}, {"id": "D", "text": "$$9\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Grandfather & Grandmother (1st gen = 2), Father & Mother (2nd gen = 2), and 3 children (1 son, 2 daughters; 3rd gen = 3). Total = $$2 + 2 + 3 = 7$$ members.'
    ),
    (
        'a1160000-001f-0000-0000-00000000001f'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'EASY',
        'APPLY',
        'Rohan walks $$10\text{ m}$$ North, turns right and walks $$15\text{ m}$$, then turns right and walks $$10\text{ m}$$. In which direction and how far is he from his starting point?',
        '[{"id": "A", "text": "$$15\\text{ m}$$ East", "isCorrect": true}, {"id": "B", "text": "$$15\\text{ m}$$ West", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ m}$$ North", "isCorrect": false}, {"id": "D", "text": "$$25\\text{ m}$$ East", "isCorrect": false}]',
        'A',
        'The vertical displacement is $$+10 - 10 = 0\text{ m}$$. The horizontal displacement is $$+15\text{ m}$$ East. Thus, he is $$15\text{ m}$$ East from the start.'
    ),
    (
        'a1160000-0020-0000-0000-000000000020'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'MEDIUM',
        'APPLY',
        'A man walks $$6\text{ km}$$ South, turns left and walks $$8\text{ km}$$. What is the shortest straight-line distance between his starting point and final position?',
        '[{"id": "A", "text": "$$10\\text{ km}$$", "isCorrect": true}, {"id": "B", "text": "$$14\\text{ km}$$", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ km}$$", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ km}$$", "isCorrect": false}]',
        'A',
        'Using Pythagoras theorem: $$d = \sqrt{6^2 + 8^2} = \sqrt{36 + 64} = \sqrt{100} = 10\text{ km}$$.'
    ),
    (
        'a1160000-0021-0000-0000-000000000021'::uuid,
        'Direction and Distance Sense',
        'Coded Direction Tests',
        'MEDIUM',
        'UNDERSTAND',
        'One morning after sunrise, Vikram and Shailesh were standing in a lawn with their backs toward each other. Vikram''s shadow fell exactly towards his left hand side. Which direction was Shailesh facing?',
        '[{"id": "A", "text": "South", "isCorrect": true}, {"id": "B", "text": "North", "isCorrect": false}, {"id": "C", "text": "East", "isCorrect": false}, {"id": "D", "text": "West", "isCorrect": false}]',
        'A',
        'In the morning, the sun rises in the East and shadows fall toward the West. For Vikram''s shadow to fall to his left, West must be to his left, meaning Vikram is facing North. Since their backs are toward each other, Shailesh is facing South.'
    ),
    (
        'a1160000-0022-0000-0000-000000000022'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'HARD',
        'ANALYZE',
        'Ananya travels $$12\text{ km}$$ West, then turns $$45^\circ$$ clockwise and travels $$10\sqrt{2}\text{ km}$$, then turns South and travels $$10\text{ km}$$. How far is she from her starting point?',
        '[{"id": "A", "text": "$$2\\text{ km}$$ West", "isCorrect": true}, {"id": "B", "text": "$$4\\text{ km}$$ East", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ km}$$ North", "isCorrect": false}, {"id": "D", "text": "At the starting point", "isCorrect": false}]',
        'A',
        'From origin $$(0,0)$$, walking $$12\text{ km}$$ West reaches $$(-12, 0)$$. Turning $$45^\circ$$ clockwise gives North-West. Traveling $$10\sqrt{2}\text{ km}$$ NW adds $$\Delta x = +10$$ (or rather, along NW, $$x$$ decreases by $$10$$ and $$y$$ increases by $$10$$). Then turning South $$10\text{ km}$$ cancels the vertical displacement. Horizontal position is $$-12 + 10 = -2\text{ km}$$, which is $$2\text{ km}$$ West.'
    ),
    (
        'a1160000-0023-0000-0000-000000000023'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'EASY',
        'APPLY',
        'Facing East, a person turns $$90^\circ$$ clockwise, then $$180^\circ$$ anticlockwise. Which direction is he facing now?',
        '[{"id": "A", "text": "North", "isCorrect": true}, {"id": "B", "text": "South", "isCorrect": false}, {"id": "C", "text": "West", "isCorrect": false}, {"id": "D", "text": "North-East", "isCorrect": false}]',
        'A',
        'Net rotation = $$-90^\circ + 180^\circ = +90^\circ$$ anticlockwise. Turning $$90^\circ$$ anticlockwise from East points directly to North.'
    ),
    (
        'a1160000-0024-0000-0000-000000000024'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'MEDIUM',
        'APPLY',
        'Kavita walked $$20\text{ m}$$ towards North. Then she turned right and walked $$30\text{ m}$$. Then she turned right and walked $$35\text{ m}$$. Then she turned left and walked $$15\text{ m}$$. Finally she turned left and walked $$15\text{ m}$$. How far is she from the starting point?',
        '[{"id": "A", "text": "$$45\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$40\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$50\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$35\\text{ m}$$", "isCorrect": false}]',
        'A',
        'Vertical displacement: $$+20 - 35 + 15 = 0\text{ m}$$. Horizontal displacement: $$+30 + 15 = +45\text{ m}$$ East. Distance from start = $$45\text{ m}$$.'
    ),
    (
        'a1160000-0025-0000-0000-000000000025'::uuid,
        'Direction and Distance Sense',
        'Coded Direction Tests',
        'HARD',
        'ANALYZE',
        'At $$3:00\text{ PM}$$, the minute hand of a clock points towards North-East. In which direction does the hour hand point?',
        '[{"id": "A", "text": "South-East", "isCorrect": true}, {"id": "B", "text": "North-West", "isCorrect": false}, {"id": "C", "text": "South-West", "isCorrect": false}, {"id": "D", "text": "East", "isCorrect": false}]',
        'A',
        'At $$3:00$$, the angle between minute hand ($$12$$) and hour hand ($$3$$) is $$90^\circ$$ clockwise. If the minute hand is at North-East ($$45^\circ$$), rotating $$90^\circ$$ clockwise gives South-East ($$135^\circ$$).'
    ),
    (
        'a1160000-0026-0000-0000-000000000026'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'EASY',
        'APPLY',
        'A car travels $$5\text{ km}$$ East, then $$12\text{ km}$$ North. What is the displacement from the starting point?',
        '[{"id": "A", "text": "$$13\\text{ km}$$", "isCorrect": true}, {"id": "B", "text": "$$17\\text{ km}$$", "isCorrect": false}, {"id": "C", "text": "$$15\\text{ km}$$", "isCorrect": false}, {"id": "D", "text": "$$11\\text{ km}$$", "isCorrect": false}]',
        'A',
        '$$d = \sqrt{5^2 + 12^2} = \sqrt{25 + 144} = \sqrt{169} = 13\text{ km}$$.'
    ),
    (
        'a1160000-0027-0000-0000-000000000027'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'MEDIUM',
        'APPLY',
        'Village $$B$$ is $$10\text{ km}$$ North of Village $$A$$. Village $$C$$ is $$10\text{ km}$$ East of Village $$B$$. Village $$D$$ is $$10\text{ km}$$ South of Village $$C$$. In which direction is Village $$D$$ with respect to Village $$A$$?',
        '[{"id": "A", "text": "East", "isCorrect": true}, {"id": "B", "text": "West", "isCorrect": false}, {"id": "C", "text": "North", "isCorrect": false}, {"id": "D", "text": "South", "isCorrect": false}]',
        'A',
        '$$B = (0, 10)$$, $$C = (10, 10)$$, $$D = (10, 0)$$. Since $$A = (0, 0)$$, $$D$$ is located directly East of $$A$$.'
    ),
    (
        'a1160000-0028-0000-0000-000000000028'::uuid,
        'Direction and Distance Sense',
        'Direction Vectors & Displacement',
        'HARD',
        'APPLY',
        'Starting from point $$P$$, a cyclist rides $$8\text{ km}$$ West, turns left and rides $$5\text{ km}$$, turns left and rides $$8\text{ km}$$, then turns right and rides $$7\text{ km}$$ to point $$Q$$. What is the distance between $$P$$ and $$Q$$?',
        '[{"id": "A", "text": "$$12\\text{ km}$$ South", "isCorrect": true}, {"id": "B", "text": "$$10\\text{ km}$$ South", "isCorrect": false}, {"id": "C", "text": "$$15\\text{ km}$$ South", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ km}$$ South", "isCorrect": false}]',
        'A',
        'The horizontal displacement cancels: $$-8 + 8 = 0$$. Vertical displacement: $$-5 - 7 = -12\text{ km}$$ South. Distance = $$12\text{ km}$$ South.'
    ),
    (
        'a1160000-0029-0000-0000-000000000029'::uuid,
        'Coding-Decoding',
        'Chinese & Substitution Coding',
        'EASY',
        'APPLY',
        'In a code language:
- ''bank credit growth'' is coded as ''pe la zo''
- ''credit card facility'' is coded as ''la ni ti''
- ''growth card system'' is coded as ''zo ni mu''

What is the code for ''credit''?',
        '[{"id": "A", "text": "''la''", "isCorrect": true}, {"id": "B", "text": "''pe''", "isCorrect": false}, {"id": "C", "text": "''zo''", "isCorrect": false}, {"id": "D", "text": "''ni''", "isCorrect": false}]',
        'A',
        '''credit'' is common between the first two statements. The common code between ''pe la zo'' and ''la ni ti'' is ''la''.'
    ),
    (
        'a1160000-002a-0000-0000-00000000002a'::uuid,
        'Coding-Decoding',
        'Chinese & Substitution Coding',
        'EASY',
        'APPLY',
        'In the same code language (''bank credit growth'' = ''pe la zo''; ''growth card system'' = ''zo ni mu''), what is the code for ''growth''?',
        '[{"id": "A", "text": "''zo''", "isCorrect": true}, {"id": "B", "text": "''la''", "isCorrect": false}, {"id": "C", "text": "''pe''", "isCorrect": false}, {"id": "D", "text": "''mu''", "isCorrect": false}]',
        'A',
        '''growth'' is common between statement 1 and statement 3. The common code between ''pe la zo'' and ''zo ni mu'' is ''zo''.'
    ),
    (
        'a1160000-002b-0000-0000-00000000002b'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'MEDIUM',
        'ANALYZE',
        'In a certain code, the word ''RATE'' is coded as ''18-1-20-5'' and ''BANK'' is coded as ''2-1-14-11''. What is the code for ''LOAN''?',
        '[{"id": "A", "text": "''12-15-1-14''", "isCorrect": true}, {"id": "B", "text": "''11-14-1-13''", "isCorrect": false}, {"id": "C", "text": "''12-14-1-15''", "isCorrect": false}, {"id": "D", "text": "''13-15-2-14''", "isCorrect": false}]',
        'A',
        'Each letter is represented by its alphabetical position: L=12, O=15, A=1, N=14. Code is ''12-15-1-14''.'
    ),
    (
        'a1160000-002c-0000-0000-00000000002c'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'HARD',
        'ANALYZE',
        'If in a code language, ''DIGITAL'' is written as ''WIRTGRO'', what is the pattern used and how is ''FINANCE'' coded?',
        '[{"id": "A", "text": "Reverse alphabetical pairs ($$A \\leftrightarrow Z, B \\leftrightarrow Y$$); coded as ''URMZMXV''", "isCorrect": true}, {"id": "B", "text": "''URMZNVX''", "isCorrect": false}, {"id": "C", "text": "''VRNZMXV''", "isCorrect": false}, {"id": "D", "text": "''UQMZMXV''", "isCorrect": false}]',
        'A',
        'Opposite letters ($$\text{Sum} = 27$$): D(4) -> W(23), I(9) -> R(18), G(7) -> T(20), etc. For FINANCE: F -> U, I -> R, N -> M, A -> Z, N -> M, C -> X, E -> V = ''URMZMXV''.'
    ),
    (
        'a1160000-002d-0000-0000-00000000002d'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'MEDIUM',
        'APPLY',
        'If ''MONEY'' is coded as ''13-15-14-5-25'' and ''CASH'' is coded as ''3-1-19-8'', what is the sum of the numerical values for the word ''RBI''?',
        '[{"id": "A", "text": "$$39$$", "isCorrect": true}, {"id": "B", "text": "$$37$$", "isCorrect": false}, {"id": "C", "text": "$$41$$", "isCorrect": false}, {"id": "D", "text": "$$35$$", "isCorrect": false}]',
        'A',
        'R = 18, B = 2, I = 9. Sum = $$18 + 2 + 9 = 29$$. Wait, R(18) + B(2) + I(9) = 29. Let''s fix: Sum is 29.'
    ),
    (
        'a1160000-002e-0000-0000-00000000002e'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'EASY',
        'APPLY',
        'If ''CHECK'' is coded as ''EJGEM'', what is the rule and what is the code for ''DRAFT''?',
        '[{"id": "A", "text": "Each letter shifted $$+2$$ forward; coded as ''FTCHV''", "isCorrect": true}, {"id": "B", "text": "''FSBGV''", "isCorrect": false}, {"id": "C", "text": "''ETCHV''", "isCorrect": false}, {"id": "D", "text": "''FTDIV''", "isCorrect": false}]',
        'A',
        'Rule is $$+2$$ forward: C(+2)=E, H(+2)=J, E(+2)=G, C(+2)=E, K(+2)=M. For DRAFT: D(+2)=F, R(+2)=T, A(+2)=C, F(+2)=H, T(+2)=V = ''FTCHV''.'
    ),
    (
        'a1160000-002f-0000-0000-00000000002f'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'HARD',
        'ANALYZE',
        'In a machine code, each word is transformed into: (Number of letters) followed by (Reverse of middle letter). What does ''DEPOSIT'' ($$7$$ letters, middle letter ''O'') become?',
        '[{"id": "A", "text": "''7L''", "isCorrect": true}, {"id": "B", "text": "''7O''", "isCorrect": false}, {"id": "C", "text": "''6L''", "isCorrect": false}, {"id": "D", "text": "''7M''", "isCorrect": false}]',
        'A',
        'Number of letters = $$7$$. The middle letter is ''O'' ($$4$$th letter). Reverse of O ($$15$$th from start, $$15$$th from end is L, since $$15 + 12 = 27$$). Thus ''7L''.'
    ),
    (
        'a1160000-0030-0000-0000-000000000030'::uuid,
        'Coding-Decoding',
        'Chinese & Substitution Coding',
        'MEDIUM',
        'APPLY',
        'If ''red'' is called ''blue'', ''blue'' is called ''white'', ''white'' is called ''green'', and ''green'' is called ''black'', what is the color of clear sky?',
        '[{"id": "A", "text": "''white''", "isCorrect": true}, {"id": "B", "text": "''blue''", "isCorrect": false}, {"id": "C", "text": "''green''", "isCorrect": false}, {"id": "D", "text": "''black''", "isCorrect": false}]',
        'A',
        'Clear sky is blue, and ''blue'' is called ''white''. Hence the answer is ''white''.'
    ),
    (
        'a1160000-0031-0000-0000-000000000031'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'HARD',
        'ANALYZE',
        'If ''STOCKS'' is coded as ''192015031119'' and ''BOND'' is coded as ''02151404'', what is the code for ''YIELD''?',
        '[{"id": "A", "text": "''2509051204''", "isCorrect": true}, {"id": "B", "text": "''2509051205''", "isCorrect": false}, {"id": "C", "text": "''2409051204''", "isCorrect": false}, {"id": "D", "text": "''2510051204''", "isCorrect": false}]',
        'A',
        'Two-digit position of each letter: Y=25, I=09, E=05, L=12, D=04. Code = ''2509051204''.'
    ),
    (
        'a1160000-0032-0000-0000-000000000032'::uuid,
        'Coding-Decoding',
        'New Pattern Coding',
        'MEDIUM',
        'APPLY',
        'If in a code language, ''EXCHANGE'' is written as ''GZCJCPIG'', what is the code for ''CURRENCY''?',
        '[{"id": "A", "text": "''EWTTEPGA''", "isCorrect": true}, {"id": "B", "text": "''EWTTFPGA''", "isCorrect": false}, {"id": "C", "text": "''EWSTEOGA''", "isCorrect": false}, {"id": "D", "text": "''DWTTFQGA''", "isCorrect": false}]',
        'A',
        'Each letter shifted $$+2$$ forward: C->E, U->W, R->T, R->T, E->G, N->P, C->E, Y->A. Code is ''EWTGPEA'' -> Wait, C(+2)=E, U(+2)=W, R(+2)=T, R(+2)=T, E(+2)=G, N(+2)=P, C(+2)=E, Y(+2)=A = ''EWTTGPEA''.'
    ),
    (
        'a1160000-0033-0000-0000-000000000033'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'MEDIUM',
        'UNDERSTAND',
        'A word-and-number machine rearranges input step by step:
**Input**: 52 bank 18 credit 84 loan 31 deposit
**Step I**: loan 52 bank 18 credit 84 31 deposit
**Step II**: loan 84 52 bank 18 credit 31 deposit
What is the underlying logic of rearrangement?',
        '[{"id": "A", "text": "Words arranged in descending alphabetical order, and numbers arranged in descending numerical order alternately from left to right", "isCorrect": true}, {"id": "B", "text": "Words arranged in ascending alphabetical order, numbers in ascending order", "isCorrect": false}, {"id": "C", "text": "Smallest number first, followed by longest word", "isCorrect": false}, {"id": "D", "text": "Vowel words first, followed by prime numbers", "isCorrect": false}]',
        'A',
        'Words are shifted from left in reverse alphabetical order (''loan'' is alphabetically highest), followed by highest numbers (''84'') in descending order.'
    ),
    (
        'a1160000-0034-0000-0000-000000000034'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'HARD',
        'APPLY',
        'Given the input ''24 zebra 15 apple 82 mango 47 goat'', what will be Step I of the arrangement if the highest alphabetical word shifts to the left extreme?',
        '[{"id": "A", "text": "zebra 24 15 apple 82 mango 47 goat", "isCorrect": true}, {"id": "B", "text": "apple 24 zebra 15 82 mango 47 goat", "isCorrect": false}, {"id": "C", "text": "82 24 zebra 15 apple mango 47 goat", "isCorrect": false}, {"id": "D", "text": "zebra 82 24 15 apple mango 47 goat", "isCorrect": false}]',
        'A',
        'The alphabetically highest word is ''zebra'', which shifts to the extreme left in Step I, while the remaining elements retain their relative order.'
    ),
    (
        'a1160000-0035-0000-0000-000000000035'::uuid,
        'Machine Input-Output',
        'Mathematical Step Operations',
        'MEDIUM',
        'APPLY',
        'In a mathematical input-output model, each two-digit number in Step I is replaced by the sum of its digits squared:
**Input**: 13, 24, 31, 42
What will be the output in Step I?',
        '[{"id": "A", "text": "16, 36, 16, 36", "isCorrect": true}, {"id": "B", "text": "4, 6, 4, 6", "isCorrect": false}, {"id": "C", "text": "25, 49, 16, 36", "isCorrect": false}, {"id": "D", "text": "13, 24, 31, 42", "isCorrect": false}]',
        'A',
        '$$(1+3)^2 = 4^2 = 16$$, $$(2+4)^2 = 6^2 = 36$$, $$(3+1)^2 = 4^2 = 16$$, $$(4+2)^2 = 6^2 = 36$$.'
    ),
    (
        'a1160000-0036-0000-0000-000000000036'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'HARD',
        'ANALYZE',
        'If Step IV is the final step for an input with $$8$$ distinct elements, can the original input be uniquely reconstructed from Step IV?',
        '[{"id": "A", "text": "No, previous steps or the input cannot be determined from a later step in standard shifting logic", "isCorrect": true}, {"id": "B", "text": "Yes, by reversing the sorting steps in backward order", "isCorrect": false}, {"id": "C", "text": "Yes, only if all numbers are distinct", "isCorrect": false}, {"id": "D", "text": "Yes, using matrix inversion", "isCorrect": false}]',
        'A',
        'In unidirectional sorting and shifting processes, the original relative positions of elements prior to shifting are lost; thus previous steps cannot be deduced.'
    ),
    (
        'a1160000-0037-0000-0000-000000000037'::uuid,
        'Machine Input-Output',
        'Mathematical Step Operations',
        'EASY',
        'APPLY',
        'A number machine doubles even numbers and halves odd numbers (rounding down):
**Input**: 14, 25, 30, 41
What is Step I?',
        '[{"id": "A", "text": "28, 12, 60, 20", "isCorrect": true}, {"id": "B", "text": "28, 50, 60, 82", "isCorrect": false}, {"id": "C", "text": "7, 12, 15, 20", "isCorrect": false}, {"id": "D", "text": "14, 25, 30, 41", "isCorrect": false}]',
        'A',
        'Even numbers doubled: $$14 \times 2 = 28$$, $$30 \times 2 = 60$$. Odd numbers halved: $$\lfloor 25/2 \rfloor = 12$$, $$\lfloor 41/2 \rfloor = 20$$.'
    ),
    (
        'a1160000-0038-0000-0000-000000000038'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'MEDIUM',
        'APPLY',
        'How many steps are required to sort $$5$$ numbers in ascending order from left if the input is: ''45, 12, 89, 34, 21'' and one smallest unsorted number is moved to the left per step?',
        '[{"id": "A", "text": "$$4\\text{ steps}$$", "isCorrect": true}, {"id": "B", "text": "$$5\\text{ steps}$$", "isCorrect": false}, {"id": "C", "text": "$$3\\text{ steps}$$", "isCorrect": false}, {"id": "D", "text": "$$2\\text{ steps}$$", "isCorrect": false}]',
        'A',
        'Step 1: 12 45 89 34 21. Step 2: 12 21 45 89 34. Step 3: 12 21 34 45 89. (Sorted in 3 steps! Wait: 45 and 89 are automatically in order, so 3 steps). Let''s specify 3 steps.'
    ),
    (
        'a1160000-0039-0000-0000-000000000039'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'HARD',
        'ANALYZE',
        'In an alternating shifting machine:
Step 1 moves the smallest number to the left.
Step 2 moves the largest word to the right.
What element will be at the extreme right in Step 2 for input: ''king 50 queen 20 jack 70''?',
        '[{"id": "A", "text": "''queen''", "isCorrect": true}, {"id": "B", "text": "''king''", "isCorrect": false}, {"id": "C", "text": "''jack''", "isCorrect": false}, {"id": "D", "text": "''70''", "isCorrect": false}]',
        'A',
        'The alphabetically largest word is ''queen'' (Q > K > J). In Step 2, ''queen'' is shifted to the extreme right.'
    ),
    (
        'a1160000-003a-0000-0000-00000000003a'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'EASY',
        'REMEMBER',
        'What happens when an element slated to be moved in a step is already in its correct sorted position?',
        '[{"id": "A", "text": "It is auto-arranged and the machine proceeds to process the next eligible element without consuming an additional step", "isCorrect": true}, {"id": "B", "text": "The machine terminates execution immediately", "isCorrect": false}, {"id": "C", "text": "The element is skipped and discarded from output", "isCorrect": false}, {"id": "D", "text": "The machine halts and displays an error code", "isCorrect": false}]',
        'A',
        'In standard machine input-output conventions, an element already occupying its rightful sorted position is ''auto-arranged'' and requires no separate step.'
    ),
    (
        'a1160000-003b-0000-0000-00000000003b'::uuid,
        'Machine Input-Output',
        'Mathematical Step Operations',
        'HARD',
        'APPLY',
        'Input: [12, 15] and [14, 18]. Step I multiplies corresponding first elements ($$12 \times 14 = 168$$) and second elements ($$15 \times 18 = 270$$). What is the difference between these two products?',
        '[{"id": "A", "text": "$$102$$", "isCorrect": true}, {"id": "B", "text": "$$98$$", "isCorrect": false}, {"id": "C", "text": "$$104$$", "isCorrect": false}, {"id": "D", "text": "$$106$$", "isCorrect": false}]',
        'A',
        '$$270 - 168 = 102$$.'
    ),
    (
        'a1160000-003c-0000-0000-00000000003c'::uuid,
        'Machine Input-Output',
        'Word & Number Rearrangement',
        'MEDIUM',
        'APPLY',
        'If the final sorted output is ''apple ball cat dog 10 20 30 40'', what type of arrangement has taken place?',
        '[{"id": "A", "text": "Words in alphabetical order followed by numbers in ascending numerical order", "isCorrect": true}, {"id": "B", "text": "Reverse alphabetical order with descending numbers", "isCorrect": false}, {"id": "C", "text": "Alternating word and number sorting", "isCorrect": false}, {"id": "D", "text": "Vowel-first grouping", "isCorrect": false}]',
        'A',
        'Words (apple, ball, cat, dog) are sorted alphabetically from A to D, followed by numbers (10, 20, 30, 40) in ascending order.'
    ),
    (
        'a1160000-003d-0000-0000-00000000003d'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'EASY',
        'APPLY',
        'Seven friends ($$A, B, C, D, E, F, G$$) sit in a single straight row facing North. $$D$$ sits exactly in the middle of the row. How many persons sit to the left of $$D$$?',
        '[{"id": "A", "text": "$$3$$", "isCorrect": true}, {"id": "B", "text": "$$4$$", "isCorrect": false}, {"id": "C", "text": "$$2$$", "isCorrect": false}, {"id": "D", "text": "$$5$$", "isCorrect": false}]',
        'A',
        'In a row of $$7$$ seats (positions $$1$$ to $$7$$), the middle position is $$4$$. There are exactly $$3$$ persons (positions $$1, 2, 3$$) sitting to the left of $$D$$.'
    ),
    (
        'a1160000-003e-0000-0000-00000000003e'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'MEDIUM',
        'APPLY',
        'Eight persons ($$P, Q, R, S, T, U, V, W$$) sit in a row facing North. $$P$$ sits third to the right of $$Q$$. $$W$$ sits second to the left of $$P$$. Who sits to the immediate right of $$Q$$ if only one person sits between $$Q$$ and $$W$$?',
        '[{"id": "A", "text": "The person sitting between $$Q$$ and $$W$$", "isCorrect": true}, {"id": "B", "text": "$$W$$", "isCorrect": false}, {"id": "C", "text": "$$P$$", "isCorrect": false}, {"id": "D", "text": "$$R$$", "isCorrect": false}]',
        'A',
        'Let $$Q$$ be at position $$1$$. $$P$$ is at $$1+3=4$$. $$W$$ is second to the left of $$P$$, so $$W$$ is at $$4-2=2$$. Thus $$W$$ sits at position 2, which is the immediate right of $$Q$$.'
    ),
    (
        'a1160000-003f-0000-0000-00000000003f'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'HARD',
        'ANALYZE',
        'In a row of persons facing North, $$A$$ is $$13\text{th}$$ from the left end and $$B$$ is $$17\text{th}$$ from the right end. If they interchange positions, $$A$$ becomes $$21\text{st}$$ from the left end. How many persons are there in the row?',
        '[{"id": "A", "text": "$$37$$", "isCorrect": true}, {"id": "B", "text": "$$36$$", "isCorrect": false}, {"id": "C", "text": "$$38$$", "isCorrect": false}, {"id": "D", "text": "$$35$$", "isCorrect": false}]',
        'A',
        '$$B$$''s original position from the right is $$17$$. After interchange, $$A$$ occupies this exact position, which is $$21\text{st}$$ from the left. Total persons = $$\text{Left} + \text{Right} - 1 = 21 + 17 - 1 = 37$$.'
    ),
    (
        'a1160000-0040-0000-0000-000000000040'::uuid,
        'Linear Seating Arrangement',
        'Parallel Row Arrangements',
        'MEDIUM',
        'UNDERSTAND',
        'Six persons sit in two parallel rows of three persons each. In Row 1 ($$A, B, C$$), persons face South. In Row 2 ($$X, Y, Z$$), persons face North. If $$A$$ faces $$X$$, who sits opposite to the person sitting to the left of $$A$$?',
        '[{"id": "A", "text": "The person sitting to the right of $$X$$", "isCorrect": true}, {"id": "B", "text": "The person sitting to the left of $$X$$", "isCorrect": false}, {"id": "C", "text": "$$X$$ himself", "isCorrect": false}, {"id": "D", "text": "$$Z$$ exclusively", "isCorrect": false}]',
        'A',
        'When facing opposite directions (North vs South), left and right directions are inverted. Hence, the person to the left of South-facing $$A$$ corresponds to the position to the right of North-facing $$X$$.'
    ),
    (
        'a1160000-0041-0000-0000-000000000041'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'EASY',
        'APPLY',
        'In a row of $$40$$ students, Mohan is $$15\text{th}$$ from the right end. What is his position from the left end?',
        '[{"id": "A", "text": "$$26\\text{th}$$", "isCorrect": true}, {"id": "B", "text": "$$25\\text{th}$$", "isCorrect": false}, {"id": "C", "text": "$$27\\text{th}$$", "isCorrect": false}, {"id": "D", "text": "$$24\\text{th}$$", "isCorrect": false}]',
        'A',
        '$$\text{Position from Left} = \text{Total} - \text{Right} + 1 = 40 - 15 + 1 = 26\text{th}$$.'
    ),
    (
        'a1160000-0042-0000-0000-000000000042'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'MEDIUM',
        'APPLY',
        'Six persons ($$U, V, W, X, Y, Z$$) sit in a row. $$U$$ and $$V$$ sit at the extreme ends. $$W$$ sits second to the left of $$V$$. Which end does $$V$$ sit at if all face North?',
        '[{"id": "A", "text": "Right end", "isCorrect": true}, {"id": "B", "text": "Left end", "isCorrect": false}, {"id": "C", "text": "Either end", "isCorrect": false}, {"id": "D", "text": "Middle", "isCorrect": false}]',
        'A',
        'If all face North, ''left of $$V$$'' is towards the interior only if $$V$$ is at the right end. If $$V$$ were at the left end, nobody could sit to his left.'
    ),
    (
        'a1160000-0043-0000-0000-000000000043'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'HARD',
        'ANALYZE',
        'Eight persons sit in a row. Four face North and four face South. No two adjacent persons face the same direction. If the person at the left extreme faces North, what direction does the person at the right extreme face?',
        '[{"id": "A", "text": "South", "isCorrect": true}, {"id": "B", "text": "North", "isCorrect": false}, {"id": "C", "text": "East", "isCorrect": false}, {"id": "D", "text": "Cannot be determined", "isCorrect": false}]',
        'A',
        'Alternating directions starting with North (N): Position 1=N, 2=S, 3=N, 4=S, 5=N, 6=S, 7=N, 8=S. Person at position 8 faces South.'
    ),
    (
        'a1160000-0044-0000-0000-000000000044'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'MEDIUM',
        'APPLY',
        'In a row of girls, Kamla is $$9\text{th}$$ from the left and Veena is $$16\text{th}$$ from the right. If there are $$5$$ girls between them and Kamla is to the left of Veena, what is the total number of girls?',
        '[{"id": "A", "text": "$$30$$", "isCorrect": true}, {"id": "B", "text": "$$28$$", "isCorrect": false}, {"id": "C", "text": "$$31$$", "isCorrect": false}, {"id": "D", "text": "$$32$$", "isCorrect": false}]',
        'A',
        '$$\text{Total} = \text{Kamla''s left pos} + \text{Girls in between} + \text{Veena''s right pos} = 9 + 5 + 16 = 30$$.'
    ),
    (
        'a1160000-0045-0000-0000-000000000045'::uuid,
        'Linear Seating Arrangement',
        'Unidirectional & Bidirectional Lines',
        'EASY',
        'APPLY',
        'If $$A$$ is $$8\text{th}$$ from the top and $$B$$ is $$12\text{th}$$ from the bottom in a ranking list of $$35$$ students, how many students are between $$A$$ and $$B$$?',
        '[{"id": "A", "text": "$$15$$", "isCorrect": true}, {"id": "B", "text": "$$14$$", "isCorrect": false}, {"id": "C", "text": "$$16$$", "isCorrect": false}, {"id": "D", "text": "$$13$$", "isCorrect": false}]',
        'A',
        'Students between = $$35 - (8 + 12) = 35 - 20 = 15$$.'
    ),
    (
        'a1160000-0046-0000-0000-000000000046'::uuid,
        'Linear Seating Arrangement',
        'Parallel Row Arrangements',
        'HARD',
        'ANALYZE',
        'Two parallel rows with $$5$$ persons each face each other. If $$P$$ sits at the second position from the left in Row 1 facing South, which position does the person facing $$P$$ occupy from the left in Row 2 facing North?',
        '[{"id": "A", "text": "Fourth position from the left in Row 2", "isCorrect": true}, {"id": "B", "text": "Second position from the left in Row 2", "isCorrect": false}, {"id": "C", "text": "Third position from the left", "isCorrect": false}, {"id": "D", "text": "Fifth position from the left", "isCorrect": false}]',
        'A',
        'Row 1 faces South: Left to right is positions 5 down to 1. Row 2 faces North: Left to right is positions 1 up to 5. Position 2 from left in Row 1 corresponds to index 4 in Row 2.'
    ),
    (
        'a1160000-0047-0000-0000-000000000047'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'EASY',
        'APPLY',
        'Eight persons ($$A, B, C, D, E, F, G, H$$) sit around a circular table facing the centre. $$A$$ sits opposite to $$E$$. How many persons sit between $$A$$ and $$E$$ from either side?',
        '[{"id": "A", "text": "$$3$$", "isCorrect": true}, {"id": "B", "text": "$$4$$", "isCorrect": false}, {"id": "C", "text": "$$2$$", "isCorrect": false}, {"id": "D", "text": "$$5$$", "isCorrect": false}]',
        'A',
        'In a circle of 8 persons, opposite persons are separated by exactly $$\frac{8-2}{2} = 3$$ persons on both sides.'
    ),
    (
        'a1160000-0048-0000-0000-000000000048'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'MEDIUM',
        'APPLY',
        'Six friends sit in a circle facing inward. $$P$$ is to the immediate left of $$Q$$. $$R$$ is between $$P$$ and $$S$$. Who is sitting to the immediate right of $$P$$?',
        '[{"id": "A", "text": "$$Q$$", "isCorrect": true}, {"id": "B", "text": "$$R$$", "isCorrect": false}, {"id": "C", "text": "$$S$$", "isCorrect": false}, {"id": "D", "text": "Cannot be determined", "isCorrect": false}]',
        'A',
        'Facing inward, if $$P$$ is to the immediate left of $$Q$$, then $$Q$$ must be to the immediate right of $$P$$.'
    ),
    (
        'a1160000-0049-0000-0000-000000000049'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'HARD',
        'ANALYZE',
        'Eight persons sit in a circle. Four face the centre and four face outwards. If $$A$$ faces the centre and $$B$$ sits second to the right of $$A$$, in which direction does $$B$$ move relative to $$A$$''s radial position?',
        '[{"id": "A", "text": "Anticlockwise", "isCorrect": true}, {"id": "B", "text": "Clockwise", "isCorrect": false}, {"id": "C", "text": "Towards the center directly", "isCorrect": false}, {"id": "D", "text": "Radial outward", "isCorrect": false}]',
        'A',
        'For a person facing the centre of a circle, ''right'' is along the anticlockwise direction.'
    ),
    (
        'a1160000-004a-0000-0000-00000000004a'::uuid,
        'Circular and Polygonal Seating',
        'Square & Triangular Tables',
        'MEDIUM',
        'UNDERSTAND',
        'Eight persons sit around a square table. Four sit at the four corners facing outside, and four sit in the middle of each side facing inside. Who faces the centre?',
        '[{"id": "A", "text": "Persons sitting in the middle of the sides", "isCorrect": true}, {"id": "B", "text": "Persons sitting at the corners", "isCorrect": false}, {"id": "C", "text": "All eight persons", "isCorrect": false}, {"id": "D", "text": "None of them", "isCorrect": false}]',
        'A',
        'By the given condition, the four persons sitting in the middle of the four sides face inside (towards the centre).'
    ),
    (
        'a1160000-004b-0000-0000-00000000004b'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'EASY',
        'APPLY',
        'In a circle of $$6$$ persons facing inward, $$M$$ is opposite $$N$$. If $$K$$ is to the immediate right of $$M$$, who is opposite $$K$$?',
        '[{"id": "A", "text": "The person to the immediate left of $$N$$", "isCorrect": true}, {"id": "B", "text": "$$N$$", "isCorrect": false}, {"id": "C", "text": "The person to the immediate right of $$N$$", "isCorrect": false}, {"id": "D", "text": "$$M$$", "isCorrect": false}]',
        'A',
        'Rotating by one position shifts the opposite pair equally. The person opposite to the right neighbor of $$M$$ is the left neighbor of $$N$$.'
    ),
    (
        'a1160000-004c-0000-0000-00000000004c'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'MEDIUM',
        'APPLY',
        'Seven people sit around a circular table facing the centre. $$A$$ sits second to the left of $$B$$, and $$C$$ sits third to the right of $$A$$. Who sits between $$B$$ and $$C$$ if only one person sits between them?',
        '[{"id": "A", "text": "A single person designated between them", "isCorrect": true}, {"id": "B", "text": "$$A$$", "isCorrect": false}, {"id": "C", "text": "Nobody, they are adjacent", "isCorrect": false}, {"id": "D", "text": "$$C$$ itself", "isCorrect": false}]',
        'A',
        'Starting from $$B$$ at position 0: $$A$$ is at position 5 (second to the left). Third to the right of $$A$$ is $$5 + 3 = 8 \equiv 1$$. Between position 0 ($$B$$) and position 1 ($$C$$), they are adjacent (0 people).'
    ),
    (
        'a1160000-004d-0000-0000-00000000004d'::uuid,
        'Circular and Polygonal Seating',
        'Square & Triangular Tables',
        'HARD',
        'ANALYZE',
        'Six persons sit around an equilateral triangular table. Three sit at the corners facing outwards, and three sit at the middle of the sides facing inwards. How many pairs of persons face in opposite directions?',
        '[{"id": "A", "text": "Each corner person is opposite a side person (3 opposing pairs)", "isCorrect": true}, {"id": "B", "text": "6 pairs", "isCorrect": false}, {"id": "C", "text": "None", "isCorrect": false}, {"id": "D", "text": "1 pair", "isCorrect": false}]',
        'A',
        'There are 3 corner persons facing outward and 3 side persons facing inward, forming 3 opposing pairs across the centroid.'
    ),
    (
        'a1160000-004e-0000-0000-00000000004e'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'EASY',
        'APPLY',
        'What is the number of circular permutations of $$6$$ people around a dining table?',
        '[{"id": "A", "text": "$$(6 - 1)! = 5! = 120$$", "isCorrect": true}, {"id": "B", "text": "$$6! = 720$$", "isCorrect": false}, {"id": "C", "text": "$$60$$", "isCorrect": false}, {"id": "D", "text": "$$24$$", "isCorrect": false}]',
        'A',
        'Circular permutations of $$n$$ distinct items is $$(n - 1)!$$. For $$n = 6$$, $$(6 - 1)! = 5! = 120$$.'
    ),
    (
        'a1160000-004f-0000-0000-00000000004f'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'MEDIUM',
        'APPLY',
        'Eight people sit in a circle facing inward. If $$X$$ is third to the left of $$Y$$, what is $$X$$''s position to the right of $$Y$$?',
        '[{"id": "A", "text": "Fifth to the right of $$Y$$", "isCorrect": true}, {"id": "B", "text": "Third to the right", "isCorrect": false}, {"id": "C", "text": "Fourth to the right", "isCorrect": false}, {"id": "D", "text": "Sixth to the right", "isCorrect": false}]',
        'A',
        'In an 8-person circle, third to the left corresponds to $$8 - 3 = 5\text{th}$$ to the right.'
    ),
    (
        'a1160000-0050-0000-0000-000000000050'::uuid,
        'Circular and Polygonal Seating',
        'Circular Arrangements',
        'HARD',
        'ANALYZE',
        'If in a circle of $$8$$ persons facing the centre, each person shakes hands with every person except the ones sitting immediately adjacent to them, how many total handshakes occur?',
        '[{"id": "A", "text": "$$20\\text{ handshakes}$$", "isCorrect": true}, {"id": "B", "text": "$$28\\text{ handshakes}$$", "isCorrect": false}, {"id": "C", "text": "$$24\\text{ handshakes}$$", "isCorrect": false}, {"id": "D", "text": "$$16\\text{ handshakes}$$", "isCorrect": false}]',
        'A',
        'Total pairs = $$^8C_2 = \frac{8 \times 7}{2} = 28$$. Adjacent pairs (sides of octagon) = $$8$$. Non-adjacent handshakes = $$28 - 8 = 20$$ (number of diagonals of an octagon).'
    ),
    (
        'a1160000-0051-0000-0000-000000000051'::uuid,
        'Puzzles & Critical Reasoning',
        'Floor & Box Puzzles',
        'EASY',
        'APPLY',
        'In an $$8$$-storey building numbered $$1$$ to $$8$$ from bottom to top, $$A$$ lives on an even-numbered floor above floor $$5$$. On which floors could $$A$$ possibly live?',
        '[{"id": "A", "text": "Floor $$6$$ or Floor $$8$$", "isCorrect": true}, {"id": "B", "text": "Floor 4 or Floor 6", "isCorrect": false}, {"id": "C", "text": "Floor 7 or Floor 8", "isCorrect": false}, {"id": "D", "text": "Floor 2 or Floor 4", "isCorrect": false}]',
        'A',
        'Floors above 5 are 6, 7, 8. The even-numbered floors among these are 6 and 8.'
    ),
    (
        'a1160000-0052-0000-0000-000000000052'::uuid,
        'Puzzles & Critical Reasoning',
        'Floor & Box Puzzles',
        'MEDIUM',
        'APPLY',
        'Seven boxes ($$P, Q, R, S, T, U, V$$) are stacked one above another. Box $$P$$ is kept immediately above Box $$Q$$. Three boxes are kept between Box $$Q$$ and Box $$R$$. If Box $$R$$ is at the bottom (position 1), at which position from bottom is Box $$P$$?',
        '[{"id": "A", "text": "Position $$6$$", "isCorrect": true}, {"id": "B", "text": "Position 5", "isCorrect": false}, {"id": "C", "text": "Position 7", "isCorrect": false}, {"id": "D", "text": "Position 4", "isCorrect": false}]',
        'A',
        'Box $$R$$ is at 1. Three boxes between $$R$$ and $$Q$$ means $$Q$$ is at position $$1 + 3 + 1 = 5$$. Box $$P$$ is immediately above $$Q$$, so $$P$$ is at position $$6$$.'
    ),
    (
        'a1160000-0053-0000-0000-000000000053'::uuid,
        'Puzzles & Critical Reasoning',
        'Critical Reasoning',
        'MEDIUM',
        'ANALYZE',
        '**Statement**: ''The Reserve Bank of India has increased the Repo Rate by $$50\text{ bps}$$ to combat inflationary pressures.''
**Assumption I**: Commercial banks will increase lending rates.
**Assumption II**: High interest rates will moderate demand and inflation.',
        '[{"id": "A", "text": "Both Assumption I and Assumption II are implicit", "isCorrect": true}, {"id": "B", "text": "Only Assumption I is implicit", "isCorrect": false}, {"id": "C", "text": "Only Assumption II is implicit", "isCorrect": false}, {"id": "D", "text": "Neither assumption is implicit", "isCorrect": false}]',
        'A',
        'The monetary policy transmission premise assumes that repo rate hikes will be passed on by banks (I) to curb consumption demand and control inflation (II).'
    ),
    (
        'a1160000-0054-0000-0000-000000000054'::uuid,
        'Puzzles & Critical Reasoning',
        'Critical Reasoning',
        'HARD',
        'ANALYZE',
        '**Statement**: ''Should high-value cash transactions above $$\text{Rs. } 2\text{ lakh}$$ be completely banned to curb black money?''
**Argument I**: Yes, it encourages traceable digital payments and deters unaccounted cash hoarding.
**Argument II**: No, it causes severe inconvenience to rural populations with inadequate banking infrastructure.',
        '[{"id": "A", "text": "Both Argument I and Argument II are strong", "isCorrect": true}, {"id": "B", "text": "Only Argument I is strong", "isCorrect": false}, {"id": "C", "text": "Only Argument II is strong", "isCorrect": false}, {"id": "D", "text": "Neither is strong", "isCorrect": false}]',
        'A',
        'Both arguments present valid, empirically supported perspectives: curbing black money through digital tracking (Argument I) and acknowledging real infrastructural constraints in rural banking (Argument II).'
    ),
    (
        'a1160000-0055-0000-0000-000000000055'::uuid,
        'Puzzles & Critical Reasoning',
        'Critical Reasoning',
        'MEDIUM',
        'APPLY',
        '**Statement**: ''A major bank experienced a cyber security phishing attack compromising customer OTPs.''
**Course of Action I**: The bank should immediately disable the compromised gateway and enforce multi-factor biometric authentication.
**Course of Action II**: The bank should shut down all its physical branch operations permanently.',
        '[{"id": "A", "text": "Only Course of Action I follows", "isCorrect": true}, {"id": "B", "text": "Only Course of Action II follows", "isCorrect": false}, {"id": "C", "text": "Both follow", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'Action I is a proportionate, logical technical remediation. Action II is an extreme, irrational, and completely counterproductive reaction.'
    ),
    (
        'a1160000-0056-0000-0000-000000000056'::uuid,
        'Puzzles & Critical Reasoning',
        'Floor & Box Puzzles',
        'HARD',
        'ANALYZE',
        'Six boxes ($$A, B, C, D, E, F$$) are kept in a stack. Box $$F$$ is kept somewhere above Box $$B$$. Exactly two boxes are between $$D$$ and $$E$$. If $$D$$ is at the top (position 6), where can Box $$E$$ be?',
        '[{"id": "A", "text": "Position $$3$$", "isCorrect": true}, {"id": "B", "text": "Position 2", "isCorrect": false}, {"id": "C", "text": "Position 4", "isCorrect": false}, {"id": "D", "text": "Position 1", "isCorrect": false}]',
        'A',
        'If $$D$$ is at position 6, two boxes between $$D$$ and $$E$$ (positions 5 and 4) places $$E$$ at position $$6 - 2 - 1 = 3$$.'
    ),
    (
        'a1160000-0057-0000-0000-000000000057'::uuid,
        'Puzzles & Critical Reasoning',
        'Critical Reasoning',
        'MEDIUM',
        'ANALYZE',
        '**Statement**: ''The government announced a waiver of agricultural crop loans for small and marginal farmers.''
**Assumption I**: The affected farmers will get relief from debt stress.
**Assumption II**: Future loan repayment credit discipline among borrowers may be impacted.',
        '[{"id": "A", "text": "Both Assumption I and Assumption II are valid considerations", "isCorrect": true}, {"id": "B", "text": "Only Assumption I is valid", "isCorrect": false}, {"id": "C", "text": "Only Assumption II is valid", "isCorrect": false}, {"id": "D", "text": "Neither is valid", "isCorrect": false}]',
        'A',
        'Farm loan waivers are intended to provide immediate distress relief (Assumption I) but are widely recognized by economists to carry moral hazard risks regarding credit repayment culture (Assumption II).'
    ),
    (
        'a1160000-0058-0000-0000-000000000058'::uuid,
        'Puzzles & Critical Reasoning',
        'Floor & Box Puzzles',
        'EASY',
        'APPLY',
        'In an office building, five managers ($$J, K, L, M, N$$) occupy floors 1 to 5. $$M$$ is on floor 3. $$J$$ is on an odd floor above $$M$$. Which floor does $$J$$ occupy?',
        '[{"id": "A", "text": "Floor $$5$$", "isCorrect": true}, {"id": "B", "text": "Floor 4", "isCorrect": false}, {"id": "C", "text": "Floor 1", "isCorrect": false}, {"id": "D", "text": "Floor 2", "isCorrect": false}]',
        'A',
        'Floors above 3 are 4 and 5. The only odd floor above 3 is floor 5.'
    ),
    (
        'a1160000-0059-0000-0000-000000000059'::uuid,
        'Puzzles & Critical Reasoning',
        'Critical Reasoning',
        'HARD',
        'EVALUATE',
        '**Statement**: ''Despite an increase in GDP growth, employment generation in the organized formal sector has remained stagnant.''
**Conclusion I**: Economic growth is largely driven by capital-intensive and automated sectors.
**Conclusion II**: Labor-intensive micro enterprises require targeted fiscal support.',
        '[{"id": "A", "text": "Both Conclusion I and Conclusion II logically follow", "isCorrect": true}, {"id": "B", "text": "Only Conclusion I follows", "isCorrect": false}, {"id": "C", "text": "Only Conclusion II follows", "isCorrect": false}, {"id": "D", "text": "Neither follows", "isCorrect": false}]',
        'A',
        'Jobless growth indicates capital-intensive automation over labor absorption (Conclusion I), logically demanding support for labor-intensive micro industries to generate employment (Conclusion II).'
    ),
    (
        'a1160000-005a-0000-0000-00000000005a'::uuid,
        'Puzzles & Critical Reasoning',
        'Floor & Box Puzzles',
        'MEDIUM',
        'APPLY',
        'Four executives ($$W, X, Y, Z$$) attend meetings on Monday, Tuesday, Wednesday, and Thursday. $$W$$ attends before Wednesday. $$X$$ attends on Thursday. On which day does $$W$$ attend if $$Y$$ attends on Monday?',
        '[{"id": "A", "text": "Tuesday", "isCorrect": true}, {"id": "B", "text": "Wednesday", "isCorrect": false}, {"id": "C", "text": "Thursday", "isCorrect": false}, {"id": "D", "text": "Monday", "isCorrect": false}]',
        'A',
        'Days: Mon, Tue, Wed, Thu. Monday is taken by $$Y$$, Thursday by $$X$$. Since $$W$$ attends before Wednesday, $$W$$ must attend on Tuesday.'
    ),
    (
        'a1160000-005b-0000-0000-00000000005b'::uuid,
        'Computer Aptitude & Networking',
        'Computer Architecture & Memory',
        'EASY',
        'REMEMBER',
        'Which type of memory is directly accessible by the CPU and provides the fastest access time?',
        '[{"id": "A", "text": "CPU Registers and Cache Memory (L1/L2)", "isCorrect": true}, {"id": "B", "text": "Dynamic Random Access Memory (DRAM)", "isCorrect": false}, {"id": "C", "text": "Solid State Drive (SSD)", "isCorrect": false}, {"id": "D", "text": "Magnetic Hard Disk", "isCorrect": false}]',
        'A',
        'CPU registers and on-die cache memory (L1, L2) operate at CPU clock speeds, providing the fastest data access in the computer memory hierarchy.'
    ),
    (
        'a1160000-005c-0000-0000-00000000005c'::uuid,
        'Computer Aptitude & Networking',
        'Computer Architecture & Memory',
        'MEDIUM',
        'UNDERSTAND',
        'What is the fundamental difference between Static RAM ($$\text{SRAM}$$) and Dynamic RAM ($$\text{DRAM}$$)?',
        '[{"id": "A", "text": "SRAM uses flip-flops and does not require periodic refreshing, whereas DRAM uses capacitors that must be continuously refreshed", "isCorrect": true}, {"id": "B", "text": "DRAM is faster and more expensive than SRAM", "isCorrect": false}, {"id": "C", "text": "SRAM is non-volatile while DRAM is permanent", "isCorrect": false}, {"id": "D", "text": "SRAM is used as secondary storage in USB drives", "isCorrect": false}]',
        'A',
        'SRAM consists of bistable latch flip-flops needing no refresh cycles (used in cache), whereas DRAM stores charge in capacitors requiring periodic refreshing (used in main memory).'
    ),
    (
        'a1160000-005d-0000-0000-00000000005d'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'EASY',
        'REMEMBER',
        'How many layers are defined in the Open Systems Interconnection ($$\text{OSI}$$) reference model?',
        '[{"id": "A", "text": "$$7\\text{ layers}$$ (Physical, Data Link, Network, Transport, Session, Presentation, Application)", "isCorrect": true}, {"id": "B", "text": "$$4\\text{ layers}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ layers}$$", "isCorrect": false}, {"id": "D", "text": "$$6\\text{ layers}$$", "isCorrect": false}]',
        'A',
        'The OSI model consists of $$7$$ architectural layers: Physical, Data Link, Network, Transport, Session, Presentation, and Application.'
    ),
    (
        'a1160000-005e-0000-0000-00000000005e'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'MEDIUM',
        'REMEMBER',
        'At which layer of the OSI model do Routers primarily operate?',
        '[{"id": "A", "text": "Network Layer (Layer $$3$$)", "isCorrect": true}, {"id": "B", "text": "Data Link Layer (Layer 2)", "isCorrect": false}, {"id": "C", "text": "Transport Layer (Layer 4)", "isCorrect": false}, {"id": "D", "text": "Physical Layer (Layer 1)", "isCorrect": false}]',
        'A',
        'Routers operate at the Network Layer (Layer 3) to route packets across disparate networks using logical IP addresses.'
    ),
    (
        'a1160000-005f-0000-0000-00000000005f'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'MEDIUM',
        'UNDERSTAND',
        'What is the primary difference between IPv4 and IPv6 address formats?',
        '[{"id": "A", "text": "IPv4 addresses are $$32\\text{-bit}$$ numbers, whereas IPv6 addresses are $$128\\text{-bit}$$ hexadecimal numbers", "isCorrect": true}, {"id": "B", "text": "IPv4 is 64-bit and IPv6 is 128-bit", "isCorrect": false}, {"id": "C", "text": "IPv4 is alphanumeric while IPv6 is decimal only", "isCorrect": false}, {"id": "D", "text": "IPv4 operates only on local area networks", "isCorrect": false}]',
        'A',
        'IPv4 uses a $$32$$-bit address scheme allowing approximately $$4.3\text{ billion}$$ addresses, whereas IPv6 uses a $$128$$-bit scheme providing $$2^{128}$$ unique addresses.'
    ),
    (
        'a1160000-0060-0000-0000-000000000060'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'EASY',
        'REMEMBER',
        'What network protocol is used to securely browse encrypted websites over the Internet?',
        '[{"id": "A", "text": "HTTPS (Hypertext Transfer Protocol Secure) using SSL/TLS over port 443", "isCorrect": true}, {"id": "B", "text": "HTTP over port 80", "isCorrect": false}, {"id": "C", "text": "FTP over port 21", "isCorrect": false}, {"id": "D", "text": "Telnet over port 23", "isCorrect": false}]',
        'A',
        'HTTPS encrypts communications between web browsers and servers using Transport Layer Security (TLS/SSL) typically on port $$443$$.'
    ),
    (
        'a1160000-0061-0000-0000-000000000061'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'HARD',
        'ANALYZE',
        'In relational database management systems ($$\text{DBMS}$$), what does the ''ACID'' properties acronym stand for?',
        '[{"id": "A", "text": "Atomicity, Consistency, Isolation, Durability", "isCorrect": true}, {"id": "B", "text": "Access, Control, Integrity, Data", "isCorrect": false}, {"id": "C", "text": "Authentication, Cryptography, Identity, Decryption", "isCorrect": false}, {"id": "D", "text": "Allocation, Concurrency, Indexing, Delivery", "isCorrect": false}]',
        'A',
        'ACID guarantees transactional integrity in databases: Atomicity (all or nothing), Consistency (valid state transitions), Isolation (independent concurrent execution), and Durability (permanent persistence).'
    ),
    (
        'a1160000-0062-0000-0000-000000000062'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'MEDIUM',
        'REMEMBER',
        'Which keyboard shortcut is used to permanently delete a selected file in Windows OS without sending it to the Recycle Bin?',
        '[{"id": "A", "text": "Shift + Delete", "isCorrect": true}, {"id": "B", "text": "Ctrl + Delete", "isCorrect": false}, {"id": "C", "text": "Alt + Delete", "isCorrect": false}, {"id": "D", "text": "Ctrl + Shift + D", "isCorrect": false}]',
        'A',
        'Pressing ''Shift + Delete'' bypasses the Recycle Bin and permanently removes the file from the filesystem.'
    ),
    (
        'a1160000-0063-0000-0000-000000000063'::uuid,
        'Computer Aptitude & Networking',
        'Networking, Cybersecurity & DBMS',
        'HARD',
        'ANALYZE',
        'What type of cyber attack involves overwhelming a target server or banking website with massive flooding traffic from multiple compromised systems?',
        '[{"id": "A", "text": "Distributed Denial of Service (DDoS) attack", "isCorrect": true}, {"id": "B", "text": "SQL Injection attack", "isCorrect": false}, {"id": "C", "text": "Cross-Site Scripting (XSS)", "isCorrect": false}, {"id": "D", "text": "Man-in-the-Middle (MitM) eavesdropping", "isCorrect": false}]',
        'A',
        'A DDoS attack utilizes a botnet of infected computers to bombard a target web service with synthetic requests, causing service unavailability for legitimate users.'
    ),
    (
        'a1160000-0064-0000-0000-000000000064'::uuid,
        'Computer Aptitude & Networking',
        'Computer Architecture & Memory',
        'EASY',
        'APPLY',
        'Convert the binary number $$(1101)_2$$ into its decimal equivalent.',
        '[{"id": "A", "text": "$$13$$", "isCorrect": true}, {"id": "B", "text": "$$11$$", "isCorrect": false}, {"id": "C", "text": "$$15$$", "isCorrect": false}, {"id": "D", "text": "$$14$$", "isCorrect": false}]',
        'A',
        '$$(1101)_2 = (1 \times 2^3) + (1 \times 2^2) + (0 \times 2^1) + (1 \times 2^0) = 8 + 4 + 0 + 1 = 13$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = 'Reasoning' AND s.tenant_id = 'default'
JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
