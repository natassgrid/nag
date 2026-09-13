-- ============================================================
-- Seed Questions: Banking PO / Clerk Prelims Blueprint Deficit Fulfillment (52 Questions)
-- Examination: IBPS / SBI Probationary Officer (PO) Preliminary Examination 2026
-- Deficits Resolved:
--   Rule 1: English Language and Comprehension > Comprehension Passage (HARD, ANALYZE) - 10 Qs
--   Rule 2: English Language and Comprehension > Cloze Passage (MEDIUM, UNDERSTAND) - 5 Qs
--   Rule 3: English Language and Comprehension > Sentence Shuffling (MEDIUM, APPLY) - 5 Qs
--   Rule 4: English Language and Comprehension > Vocabulary (EASY, REMEMBER) - 5 Qs
--   Rule 5: Quantitative Aptitude / Mathematical Abilities > Statistics and Probability (HARD, ANALYZE) - 9 Qs
--   Rule 6: General Intelligence and Reasoning > Coding and Decoding (EASY, APPLY) - 4 Qs
--   Rule 7: General Intelligence and Reasoning > Problem Solving (HARD, ANALYZE) - 9 Qs
--   Rule 8: General Intelligence and Reasoning > Drawing Inferences (MEDIUM, ANALYZE) - 5 Qs
-- Format Standard: Valid UUIDs, JSONB options, LaTeX math ($$..$$), state='APPROVED'
-- UUID Range: a1260000-0000-0000-0000-000000000001 to a1260000-0000-0000-0000-000000000052
-- ============================================================

-- Step 1: Ensure Subjects exist
INSERT INTO question_service.subject (tenant_id, name, code, description)
VALUES
    ('default', 'English Language and Comprehension', 'ELC', 'English language proficiency covering comprehension, vocabulary, cloze tests, and grammar'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'QAMA', 'Quantitative aptitude, data interpretation, probability, arithmetic, and advanced mathematics'),
    ('default', 'General Intelligence and Reasoning', 'GIR', 'Logical, analytical, and critical reasoning, problem-solving puzzles, and coding')
ON CONFLICT (name, tenant_id) DO NOTHING;

-- Step 2: Ensure Topics exist under respective Subjects
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Comprehension Passage', 'Reading comprehension passages analyzing financial, socio-economic, and technological themes'),
    ('Cloze Passage', 'Cloze test passages evaluating contextual vocabulary and structural grammar coherence'),
    ('Sentence Shuffling', 'Para-jumbles and sentence rearrangement for paragraph coherence and logical flow'),
    ('Vocabulary', 'Synonyms, antonyms, idioms, phrases, and word contextual usage')
) AS v(name, description)
WHERE s.name = 'English Language and Comprehension' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Statistics and Probability', 'Bayes theorem, conditional probability, permutations, combinations, dispersion, and distributions')
) AS v(name, description)
WHERE s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Coding and Decoding', 'Positional shift coding, reverse letter coding, symbol and number coding'),
    ('Problem Solving', 'Complex multi-variable floor-flat arrangements, circular puzzles, and scheduling matrix problems'),
    ('Drawing Inferences', 'Statement and assumption, critical reasoning deductions, and macro-policy logical inferences')
) AS v(name, description)
WHERE s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- Step 3: Insert the 52 Assessment Questions
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
    NULL,
    v.subject_name,
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
    -- =========================================================================
    -- RULE 1: English Language and Comprehension > Comprehension Passage (HARD, ANALYZE) - 10 Qs
    -- =========================================================================
    -- Passage 1: Central Bank Digital Currencies (CBDCs) and Monetary Transmission (Q1 - Q5)
    (
        'a1260000-0000-0000-0000-000000000001'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Financial Reading Comprehension',
        'HARD',
        'ANALYZE',
        'Read the following passage carefully and answer the question:

"The advent of retail Central Bank Digital Currencies (CBDCs) represents a profound paradigm shift in modern monetary architecture. While proponents champion CBDCs as instruments for universal financial inclusion and frictionless cross-border settlement, macroprudential authorities warn of latent structural perils. Paramount among these is the risk of commercial bank disintermediation. In times of systemic turbulence, depositors, motivated by flight-to-safety incentives, may rapidly migrate liquidity from commercial bank deposit accounts into risk-free central bank liabilities. Unlike physical cash—whose physical storage, handling friction, and logistical constraints act as a natural governor against precipitous bank runs—a digital sovereign token allows instantaneous and frictionless capital flight at the tap of a screen. Consequently, to mitigate these flight-to-safety dynamics, central banks are exploring hard holding caps and tiered remuneration schedules to deter large-scale corporate deposit substitution."

According to the passage, what is the primary structural vulnerability created by retail CBDCs compared to physical fiat currency during systemic financial distress?',
        '[{"id": "A", "text": "Elimination of transaction fees which reduces commercial bank operating revenues", "isCorrect": false}, {"id": "B", "text": "Absence of physical storage and logistical constraints, enabling frictionless instantaneous capital flight to central bank liabilities", "isCorrect": true}, {"id": "C", "text": "Total obsolescence of cross-border clearing houses and correspondent banking rails", "isCorrect": false}, {"id": "D", "text": "Inability of macroprudential authorities to enforce tiered interest rate structures on fiat currency", "isCorrect": false}]',
        'B',
        'The passage explicitly highlights that unlike physical cash—whose logistical and storage friction acts as a natural governor—digital tokens permit instantaneous, frictionless migration of liquidity into central bank liabilities during crises.'
    ),
    (
        'a1260000-0000-0000-0000-000000000002'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Financial Reading Comprehension',
        'HARD',
        'ANALYZE',
        'Based on the passage on Central Bank Digital Currencies (CBDCs), what is the primary regulatory rationale for instituting "holding caps and tiered remuneration schedules"?',
        '[{"id": "A", "text": "To curb retail user adoption and discourage cross-border payment integration", "isCorrect": false}, {"id": "B", "text": "To prevent commercial bank deposit drainage and limit wholesale disintermediation", "isCorrect": true}, {"id": "C", "text": "To maximize central bank seigniorage profits by penalizing small retail savings accounts", "isCorrect": false}, {"id": "D", "text": "To mandate complete replacement of physical fiat banknotes by sovereign digital tokens", "isCorrect": false}]',
        'B',
        'Holding caps and tiered remuneration schedules are designed to discourage large-scale corporate deposit migration into central bank digital balances, thereby preserving commercial banks'' deposit bases and lending capacity.'
    ),
    (
        'a1260000-0000-0000-0000-000000000003'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Financial Reading Comprehension',
        'HARD',
        'ANALYZE',
        'Which of the following best describes the author''s overarching tone toward retail CBDC implementation in the passage?',
        '[{"id": "A", "text": "Blindly celebratory and unreservedly promotional", "isCorrect": false}, {"id": "B", "text": "Analytical, circumspect, and cognizant of macroeconomic trade-offs", "isCorrect": true}, {"id": "C", "text": "Dismissive and vehemently opposed to all digital banking innovations", "isCorrect": false}, {"id": "D", "text": "Ambivalent, indifferent, and purely descriptive", "isCorrect": false}]',
        'B',
        'The author objectively weighs the proclaimed benefits (financial inclusion, frictionless settlement) against structural stability risks (disintermediation, rapid bank runs) and discusses regulatory design remedies, demonstrating an analytical and circumspect tone.'
    ),
    (
        'a1260000-0000-0000-0000-000000000004'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Financial Reading Comprehension',
        'HARD',
        'ANALYZE',
        'What can be logically inferred from the passage regarding the relationship between commercial banks and central bank liabilities?',
        '[{"id": "A", "text": "Commercial bank deposits carry identical credit and default risk profiles as direct central bank liabilities", "isCorrect": false}, {"id": "B", "text": "Central bank liabilities are perceived as default-free safe havens relative to commercial bank deposits in distress periods", "isCorrect": true}, {"id": "C", "text": "Commercial banks prefer depositors to hold central bank digital currency to lower reserve requirements", "isCorrect": false}, {"id": "D", "text": "Central banks cannot issue direct liabilities without prior authorization from private commercial lenders", "isCorrect": false}]',
        'B',
        'The concept of "flight-to-safety" driving depositors to migrate funds into "risk-free central bank liabilities" confirms that central bank obligations are seen as inherently credit-risk-free compared to fractional-reserve commercial deposits.'
    ),
    (
        'a1260000-0000-0000-0000-000000000005'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Financial Reading Comprehension',
        'HARD',
        'ANALYZE',
        'As used in the passage, which of the following is closest in meaning to the word "governor" in the phrase "natural governor against precipitous bank runs"?',
        '[{"id": "A", "text": "Political administrator", "isCorrect": false}, {"id": "B", "text": "Restraining or regulating mechanism", "isCorrect": true}, {"id": "C", "text": "Stimulating catalyst", "isCorrect": false}, {"id": "D", "text": "Legal arbitrator", "isCorrect": false}]',
        'B',
        'In mechanical and systems engineering contexts, a "governor" is a regulatory device that limits speed or moderates flow. Here it contextually refers to a natural restraining mechanism that dampens the velocity of bank runs.'
    ),

    -- Passage 2: Generative AI in Credit Underwriting and Model Risk (Q6 - Q10)
    (
        'a1260000-0000-0000-0000-000000000006'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'AI in Banking Passage',
        'HARD',
        'ANALYZE',
        'Read the following passage carefully and answer the question:

"The integration of Generative AI and deep neural networks into automated credit underwriting has promised unprecedented predictive acuity. By assimilating non-traditional alternative data streams—including unstructured telecommunication telemetry and granular transactional metadata—algorithmic models routinely outperform legacy logistic regressions in isolating creditworthiness among unbanked segments. Nonetheless, this algorithmic paradigm introduces severe systemic vulnerabilities: model opacity, colloquially termed the ''black box'' dilemma, severely compromises adverse action explanation compliance under fair lending mandates. More perilously, when non-stationary macroeconomic stress hits financial markets—such as sudden stagflationary shocks or liquidity freezes—machine learning models trained purely on benign historical epochs exhibit severe epistemic degradation, hallucinating false correlation signals and exacerbating procyclical credit rationing."

According to the passage, why do autonomous AI underwriting models experience "severe epistemic degradation" during macroeconomic crises?',
        '[{"id": "A", "text": "They operate exclusively on structured balance sheet ratios rather than alternative data", "isCorrect": false}, {"id": "B", "text": "Their training datasets predominantly encompass benign historical periods, rendering them incapable of navigating unprecedented non-stationary shifts", "isCorrect": true}, {"id": "C", "text": "They are prohibited from processing transactional metadata under consumer privacy regulations", "isCorrect": false}, {"id": "D", "text": "Legacy logistic regression scorecards override their neural parameters during stress scenarios", "isCorrect": false}]',
        'B',
        'The passage explicitly mentions that when non-stationary macroeconomic shocks hit, models trained purely on benign historical epochs fail to generalize, leading to epistemic degradation and hallucinated correlations.'
    ),
    (
        'a1260000-0000-0000-0000-000000000007'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'AI in Banking Passage',
        'HARD',
        'ANALYZE',
        'Which regulatory mandate is directly threatened by the "black box" nature of deep neural credit scoring models?',
        '[{"id": "A", "text": "Capital Adequacy Ratio maintenance under Basel III", "isCorrect": false}, {"id": "B", "text": "Adverse action notice explanation compliance under fair lending frameworks", "isCorrect": true}, {"id": "C", "text": "Foreign exchange net open position limits", "isCorrect": false}, {"id": "D", "text": "Statutory Liquidity Ratio investment allocations", "isCorrect": false}]',
        'B',
        'The passage states that the ''black box'' dilemma severely compromises "adverse action explanation compliance under fair lending mandates," where rejected borrowers must be provided transparent reasons for loan denial.'
    ),
    (
        'a1260000-0000-0000-0000-000000000008'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'AI in Banking Passage',
        'HARD',
        'ANALYZE',
        'Which of the following scenarios, if true, would most effectively mitigate the "procyclical credit rationing" risk described in the passage?',
        '[{"id": "A", "text": "Eliminating human risk underwriters from the loan approval workflow entirely", "isCorrect": false}, {"id": "B", "text": "Subjecting algorithmic models to adversarial stress-testing simulating extreme counter-cyclical shock scenarios and macroeconomic regime shifts", "isCorrect": true}, {"id": "C", "text": "Restricting credit evaluation exclusively to applicant social media telemetry data", "isCorrect": false}, {"id": "D", "text": "Increasing the learning rate parameter in deep neural network optimization", "isCorrect": false}]',
        'B',
        'Since model fragility stems from training solely on benign historical regimes, conducting adversarial stress-testing simulating counter-cyclical shocks directly strengthens model robustness against procyclical collapse.'
    ),
    (
        'a1260000-0000-0000-0000-000000000009'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'AI in Banking Passage',
        'HARD',
        'ANALYZE',
        'What is the primary advantage of alternative data ingestion highlighted in the text?',
        '[{"id": "A", "text": "Guaranteed immunity from algorithmic bias and societal discrimination", "isCorrect": false}, {"id": "B", "text": "Enhanced predictive capability in assessing creditworthiness for previously unbanked populations", "isCorrect": true}, {"id": "C", "text": "Complete avoidance of adverse macroeconomic stagflationary shocks", "isCorrect": false}, {"id": "D", "text": "Reduction of capital reserve requirements to zero for digital lenders", "isCorrect": false}]',
        'B',
        'The text states that assimilating non-traditional alternative data enables models to "routinely outperform legacy logistic regressions in isolating creditworthiness among unbanked segments."'
    ),
    (
        'a1260000-0000-0000-0000-000000000010'::uuid,
        'English Language and Comprehension',
        'Comprehension Passage',
        'AI in Banking Passage',
        'HARD',
        'ANALYZE',
        'Which of the following titles best encapsulates the central thesis of the second passage?',
        '[{"id": "A", "text": "The Unchecked Triumph of Machine Learning in Retail Banking", "isCorrect": false}, {"id": "B", "text": "Algorithmic Underwriting: Predictive Power vs. Opacity and Systemic Fragility", "isCorrect": true}, {"id": "C", "text": "Why Traditional Credit Bureaus Are Obsolete in Modern Finance", "isCorrect": false}, {"id": "D", "text": "Eliminating Financial Risk through Deep Neural Network Models", "isCorrect": false}]',
        'B',
        'The passage balances the predictive acuity and inclusion gains of AI underwriting against systemic risks such as model opacity, regulatory compliance hurdles, and epistemic failures under macroeconomic stress.'
    ),

    -- =========================================================================
    -- RULE 2: English Language and Comprehension > Cloze Passage (MEDIUM, UNDERSTAND) - 5 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000011'::uuid,
        'English Language and Comprehension',
        'Cloze Passage',
        'Banking Operations Cloze',
        'MEDIUM',
        'UNDERSTAND',
        'In the following banking passage, there are blanks numbered (1) to (5). Choose the most appropriate word to fill blank (1):

"In an era defined by rapid digital transformation, banking institutions must proactively __(1)__ cyber vulnerabilities while delivering seamless customer experiences. Modern fraud detection architectures deploy behavioral biometrics to monitor transactional anomalies in real-time. By implementing automated risk controls, lenders can __(2)__ fraudulent transfers without introducing excessive friction into genuine user workflows. However, maintaining such resilient ecosystems demands continuous capital expenditure and rigorous __(3)__ audits to ensure complete compliance with sovereign regulatory standards. Ultimately, institutions that successfully balance security with operational agility will __(4)__ long-term trust across retail and corporate client segments, thereby cementing their position as __(5)__ pillars of the modern financial economy."

Which word best fits blank (1)?',
        '[{"id": "A", "text": "mitigate", "isCorrect": true}, {"id": "B", "text": "aggravate", "isCorrect": false}, {"id": "C", "text": "exacerbate", "isCorrect": false}, {"id": "D", "text": "procrastinate", "isCorrect": false}]',
        'A',
        '"Mitigate" means to make less severe, serious, or painful, which is the exact positive action banks must take regarding "cyber vulnerabilities". Words like aggravate/exacerbate mean the opposite.'
    ),
    (
        'a1260000-0000-0000-0000-000000000012'::uuid,
        'English Language and Comprehension',
        'Cloze Passage',
        'Banking Operations Cloze',
        'MEDIUM',
        'UNDERSTAND',
        'Refer to the passage above. Which word best fits blank (2): "lenders can __(2)__ fraudulent transfers without introducing excessive friction"?',
        '[{"id": "A", "text": "intercept", "isCorrect": true}, {"id": "B", "text": "encourage", "isCorrect": false}, {"id": "C", "text": "fabricate", "isCorrect": false}, {"id": "D", "text": "overlook", "isCorrect": false}]',
        'A',
        '"Intercept" means to prevent something from reaching its destination or stop something in transit, which precisely describes blocking fraudulent financial transfers in real-time.'
    ),
    (
        'a1260000-0000-0000-0000-000000000013'::uuid,
        'English Language and Comprehension',
        'Cloze Passage',
        'Banking Operations Cloze',
        'MEDIUM',
        'UNDERSTAND',
        'Refer to the passage above. Which word best fits blank (3): "demands continuous capital expenditure and rigorous __(3)__ audits to ensure complete compliance"?',
        '[{"id": "A", "text": "surveillance", "isCorrect": true}, {"id": "B", "text": "superficial", "isCorrect": false}, {"id": "C", "text": "complacent", "isCorrect": false}, {"id": "D", "text": "negligent", "isCorrect": false}]',
        'A',
        '"Surveillance audits" (or rigorous security surveillance) logically pairs with ensuring regulatory and institutional compliance, whereas superficial, complacent, or negligent denote flawed oversight.'
    ),
    (
        'a1260000-0000-0000-0000-000000000014'::uuid,
        'English Language and Comprehension',
        'Cloze Passage',
        'Banking Operations Cloze',
        'MEDIUM',
        'UNDERSTAND',
        'Refer to the passage above. Which word best fits blank (4): "institutions that successfully balance security with operational agility will __(4)__ long-term trust"?',
        '[{"id": "A", "text": "bolster", "isCorrect": true}, {"id": "B", "text": "undermine", "isCorrect": false}, {"id": "C", "text": "relinquish", "isCorrect": false}, {"id": "D", "text": "jeopardize", "isCorrect": false}]',
        'A',
        '"Bolster" means to support, strengthen, or reinforce. Succeeding in balancing security and convenience strengthens ("bolsters") customer trust.'
    ),
    (
        'a1260000-0000-0000-0000-000000000015'::uuid,
        'English Language and Comprehension',
        'Cloze Passage',
        'Banking Operations Cloze',
        'MEDIUM',
        'UNDERSTAND',
        'Refer to the passage above. Which word best fits blank (5): "cementing their position as __(5)__ pillars of the modern financial economy"?',
        '[{"id": "A", "text": "indispensable", "isCorrect": true}, {"id": "B", "text": "redundant", "isCorrect": false}, {"id": "C", "text": "ephemeral", "isCorrect": false}, {"id": "D", "text": "superfluous", "isCorrect": false}]',
        'A',
        '"Indispensable" means absolutely necessary or essential. Cementing a position as "indispensable pillars" reflects central, essential status in the financial economy, whereas redundant/ephemeral/superfluous indicate uselessness or impermanence.'
    ),

    -- =========================================================================
    -- RULE 3: English Language and Comprehension > Sentence Shuffling (MEDIUM, APPLY) - 5 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000016'::uuid,
        'English Language and Comprehension',
        'Sentence Shuffling',
        'Para Jumble Rearrangement',
        'MEDIUM',
        'APPLY',
        'Rearrange the following five sentences (A, B, C, D, E) to form a logically coherent paragraph, then answer the question:

(A) This exponential surge in digital liquidity has substantially reduced cash handling costs for commercial banks.
(B) In recent years, India''s retail payments architecture has witnessed an unprecedented digital revolution spearheaded by UPI.
(C) Consequently, formal credit delivery to micro-enterprises has expanded exponentially on the back of transparent digital transaction trails.
(D) Furthermore, small merchants who previously operated outside the formal banking periphery are now integrated into the financial mainstream.
(E) As smartphone penetration deepened across tier-2 and tier-3 geographies, even rural micro-transactions shifted from paper currency to instant QR-based settlements.

Which of the following should be the FIRST sentence after rearrangement?',
        '[{"id": "A", "text": "B", "isCorrect": true}, {"id": "B", "text": "E", "isCorrect": false}, {"id": "C", "text": "A", "isCorrect": false}, {"id": "D", "text": "D", "isCorrect": false}]',
        'A',
        'Sentence (B) introduces the overarching subject—the digital payments revolution spearheaded by UPI—making it the ideal opening sentence of the paragraph.'
    ),
    (
        'a1260000-0000-0000-0000-000000000017'::uuid,
        'English Language and Comprehension',
        'Sentence Shuffling',
        'Para Jumble Rearrangement',
        'MEDIUM',
        'APPLY',
        'Refer to the paragraph rearrangement above (sentences A, B, C, D, E). Which of the following should be the SECOND sentence after rearrangement?',
        '[{"id": "A", "text": "E", "isCorrect": true}, {"id": "B", "text": "A", "isCorrect": false}, {"id": "C", "text": "C", "isCorrect": false}, {"id": "D", "text": "D", "isCorrect": false}]',
        'A',
        'Sentence (E) elaborates on how the digital revolution described in (B) expanded geographically into tier-2/3 regions and rural micro-transactions via QR settlements.'
    ),
    (
        'a1260000-0000-0000-0000-000000000018'::uuid,
        'English Language and Comprehension',
        'Sentence Shuffling',
        'Para Jumble Rearrangement',
        'MEDIUM',
        'APPLY',
        'Refer to the paragraph rearrangement above (sentences A, B, C, D, E). Which of the following should be the THIRD sentence after rearrangement?',
        '[{"id": "A", "text": "A", "isCorrect": true}, {"id": "B", "text": "D", "isCorrect": false}, {"id": "C", "text": "C", "isCorrect": false}, {"id": "D", "text": "B", "isCorrect": false}]',
        'A',
        'Sentence (A) connects directly with (E) through "This exponential surge in digital liquidity", highlighting the immediate efficiency dividend for commercial banks.'
    ),
    (
        'a1260000-0000-0000-0000-000000000019'::uuid,
        'English Language and Comprehension',
        'Sentence Shuffling',
        'Para Jumble Rearrangement',
        'MEDIUM',
        'APPLY',
        'Refer to the paragraph rearrangement above (sentences A, B, C, D, E). Which of the following should be the FOURTH sentence after rearrangement?',
        '[{"id": "A", "text": "D", "isCorrect": true}, {"id": "B", "text": "C", "isCorrect": false}, {"id": "C", "text": "A", "isCorrect": false}, {"id": "D", "text": "E", "isCorrect": false}]',
        'A',
        'Sentence (D) transitions from bank efficiencies to merchant onboarding ("Furthermore, small merchants... are now integrated").'
    ),
    (
        'a1260000-0000-0000-0000-000000000020'::uuid,
        'English Language and Comprehension',
        'Sentence Shuffling',
        'Para Jumble Rearrangement',
        'MEDIUM',
        'APPLY',
        'Refer to the paragraph rearrangement above (sentences A, B, C, D, E). Which of the following should be the FIFTH (Concluding) sentence after rearrangement?',
        '[{"id": "A", "text": "C", "isCorrect": true}, {"id": "B", "text": "D", "isCorrect": false}, {"id": "C", "text": "B", "isCorrect": false}, {"id": "D", "text": "A", "isCorrect": false}]',
        'A',
        'Sentence (C) provides the logical culmination ("Consequently, formal credit delivery to micro-enterprises has expanded exponentially on the back of transparent digital transaction trails"). Full sequence: B-E-A-D-C.'
    ),

    -- =========================================================================
    -- RULE 4: English Language and Comprehension > Vocabulary (EASY, REMEMBER) - 5 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000021'::uuid,
        'English Language and Comprehension',
        'Vocabulary',
        'Synonyms',
        'EASY',
        'REMEMBER',
        'Select the word that is most nearly SIMILAR in meaning to the given word: **AMELIORATE**',
        '[{"id": "A", "text": "Improve", "isCorrect": true}, {"id": "B", "text": "Worsen", "isCorrect": false}, {"id": "C", "text": "Depreciate", "isCorrect": false}, {"id": "D", "text": "Stagnate", "isCorrect": false}]',
        'A',
        '"Ameliorate" means to make something bad or unsatisfactory better; to improve.'
    ),
    (
        'a1260000-0000-0000-0000-000000000022'::uuid,
        'English Language and Comprehension',
        'Vocabulary',
        'Antonyms',
        'EASY',
        'REMEMBER',
        'Select the word that is most OPPOSITE in meaning to the given word: **STRINGENT**',
        '[{"id": "A", "text": "Lenient", "isCorrect": true}, {"id": "B", "text": "Rigid", "isCorrect": false}, {"id": "C", "text": "Stern", "isCorrect": false}, {"id": "D", "text": "Austere", "isCorrect": false}]',
        'A',
        '"Stringent" means strict, precise, and exacting. Its opposite is "Lenient" (permissive, flexible).'
    ),
    (
        'a1260000-0000-0000-0000-000000000023'::uuid,
        'English Language and Comprehension',
        'Vocabulary',
        'Contextual Financial Vocabulary',
        'EASY',
        'REMEMBER',
        'What does the financial term **SOLVENCY** denote regarding a commercial enterprise?',
        '[{"id": "A", "text": "The ability of a business to meet its long-term financial obligations", "isCorrect": true}, {"id": "B", "text": "The daily cash turnover of a branch counter", "isCorrect": false}, {"id": "C", "text": "The total dividend paid out to equity shareholders", "isCorrect": false}, {"id": "D", "text": "The rate at which a currency is devalued against gold", "isCorrect": false}]',
        'A',
        'Solvency refers to an organization''s capacity to meet its long-term debts and financial obligations as they mature.'
    ),
    (
        'a1260000-0000-0000-0000-000000000024'::uuid,
        'English Language and Comprehension',
        'Vocabulary',
        'Antonyms',
        'EASY',
        'REMEMBER',
        'Select the word that is most OPPOSITE in meaning to the given word: **PERILOUS**',
        '[{"id": "A", "text": "Safe", "isCorrect": true}, {"id": "B", "text": "Hazardous", "isCorrect": false}, {"id": "C", "text": "Precarious", "isCorrect": false}, {"id": "D", "text": "Treacherous", "isCorrect": false}]',
        'A',
        '"Perilous" means full of danger or risk. The direct antonym is "Safe" (secure, free from harm).'
    ),
    (
        'a1260000-0000-0000-0000-000000000025'::uuid,
        'English Language and Comprehension',
        'Vocabulary',
        'Idioms and Phrases',
        'EASY',
        'REMEMBER',
        'What is the meaning of the common financial idiom **"IN THE RED"**?',
        '[{"id": "A", "text": "Operating at a financial loss or in debt", "isCorrect": true}, {"id": "B", "text": "Earning surplus foreign exchange reserves", "isCorrect": false}, {"id": "C", "text": "Having high credit ratings across rating agencies", "isCorrect": false}, {"id": "D", "text": "Undergoing immediate corporate restructuring", "isCorrect": false}]',
        'A',
        'To be "in the red" originates from accounting practices where negative balances or losses were written in red ink.'
    ),

    -- =========================================================================
    -- RULE 5: Quantitative Aptitude / Mathematical Abilities > Statistics and Probability (HARD, ANALYZE) - 9 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000026'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Bayes Theorem & Conditional Probability',
        'HARD',
        'ANALYZE',
        'A bank''s automated fraud detection system flags fraudulent transactions. Historically, $$1\\%$$ of all digital transactions are fraudulent. The system accurately identifies a fraudulent transaction with probability $$0.95$$ (sensitivity), but has a false positive rate of $$0.02$$ on legitimate transactions. If a transaction is flagged by the system as fraudulent, what is the probability that it is genuinely fraudulent?',
        '[{"id": "A", "text": "$$\\frac{95}{293}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{95}{100}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{19}{200}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{5}{147}$$", "isCorrect": false}]',
        'A',
        'By Bayes'' Theorem: $$P(F) = 0.01$$, $$P(L) = 0.99$$. $$P(\\text{Flag}|F) = 0.95$$, $$P(\\text{Flag}|L) = 0.02$$. Total probability of flag $$P(\\text{Flag}) = (0.01 \\times 0.95) + (0.99 \\times 0.02) = 0.0095 + 0.0198 = 0.0293$$. Posterior probability $$P(F|\\text{Flag}) = \\frac{0.0095}{0.0293} = \\frac{95}{293} \\approx 32.42\\%$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000027'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Multi-Urn Probability',
        'HARD',
        'ANALYZE',
        'Urn A contains $$4$$ red and $$6$$ blue tokens. Urn B contains $$5$$ red and $$3$$ blue tokens. A token is randomly transferred from Urn A to Urn B, and then one token is drawn at random from Urn B. What is the probability that the token drawn from Urn B is red?',
        '[{"id": "A", "text": "$$\\frac{3}{5}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{4}{9}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{1}{2}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{2}{3}$$", "isCorrect": false}]',
        'A',
        'Case 1: Transferred token is Red ($$P = 4/10 = 2/5$$). Urn B now has $$6$$ Red, $$3$$ Blue (Total $$9$$). $$P(\\text{Red}|\\text{Transferred Red}) = 6/9 = 2/3$$.
Case 2: Transferred token is Blue ($$P = 6/10 = 3/5$$). Urn B now has $$5$$ Red, $$4$$ Blue (Total $$9$$). $$P(\\text{Red}|\\text{Transferred Blue}) = 5/9$$.
Total probability $$= \\left(\\frac{2}{5} \\times \\frac{2}{3}\\right) + \\left(\\frac{3}{5} \\times \\frac{5}{9}\\right) = \\frac{4}{15} + \\frac{5}{15} = \\frac{9}{15} = \\frac{3}{5}$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000028'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Compound Probability & Dice',
        'HARD',
        'ANALYZE',
        'Two unbiased standard 6-sided dice are rolled simultaneously. What is the conditional probability that the sum of the two numbers is greater than or equal to $$8$$, given that at least one die shows a prime number ($$2, 3,\\text{ or } 5$$)?',
        '[{"id": "A", "text": "$$\\frac{13}{27}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{15}{36}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{11}{27}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{7}{18}$$", "isCorrect": false}]',
        'A',
        'Prime outcomes per die: {2, 3, 5} (3 outcomes), non-prime {1, 4, 6} (3 outcomes).
Event B (at least one prime): Total outcomes $$= 36 - 3^2 = 36 - 9 = 27$$.
Event A (sum $$\\ge 8$$): Total outcomes with sum $$\\ge 8$$ is $$15$$: (2,6), (3,5),(3,6), (4,4),(4,5),(4,6), (5,3),(5,4),(5,5),(5,6), (6,2),(6,3),(6,4),(6,5),(6,6).
Outcomes in A with NO prime numbers: Only (4,4), (4,6), (6,4), (6,6) (4 outcomes).
Therefore, outcomes in $$A \\cap B = 15 - 4 = 13$$.
Conditional probability $$P(A|B) = \\frac{|A \\cap B|}{|B|} = \\frac{13}{27}$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000029'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Combined Standard Deviation',
        'HARD',
        'ANALYZE',
        'Branch A has $$n_1 = 50$$ employees with a mean performance score $$\\bar{x}_1 = 80$$ and variance $$\\sigma_1^2 = 16$$. Branch B has $$n_2 = 50$$ employees with mean score $$\\bar{x}_2 = 70$$ and variance $$\\sigma_2^2 = 9$$. Find the standard deviation of the combined group of $$100$$ employees.',
        '[{"id": "A", "text": "$$\\sqrt{37.5}$$", "isCorrect": true}, {"id": "B", "text": "$$\\sqrt{25}$$", "isCorrect": false}, {"id": "C", "text": "$$\\sqrt{12.5}$$", "isCorrect": false}, {"id": "D", "text": "$$\\sqrt{50}$$", "isCorrect": false}]',
        'A',
        'Combined mean $$\\bar{x} = \\frac{50(80) + 50(70)}{100} = 75$$. Deviations: $$d_1 = 80 - 75 = 5$$, $$d_2 = 70 - 75 = -5$$.
Combined variance $$\\sigma^2 = \\frac{n_1(\\sigma_1^2 + d_1^2) + n_2(\\sigma_2^2 + d_2^2)}{n_1 + n_2} = \\frac{50(16 + 25) + 50(9 + 25)}{100} = \\frac{50(41) + 50(34)}{100} = \\frac{2050 + 1700}{100} = 37.5$$.
Combined standard deviation $$= \\sqrt{37.5}$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000030'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Combinatorics in Risk Committees',
        'HARD',
        'ANALYZE',
        'A credit appraisal committee of $$5$$ members is to be formed from a pool of $$6$$ Risk Analysts and $$4$$ Chartered Accountants (CAs). In how many distinct ways can the committee be formed such that it contains at least $$2$$ Chartered Accountants?',
        '[{"id": "A", "text": "$$186$$", "isCorrect": true}, {"id": "B", "text": "$$120$$", "isCorrect": false}, {"id": "C", "text": "$$210$$", "isCorrect": false}, {"id": "D", "text": "$$156$$", "isCorrect": false}]',
        'A',
        'Total ways without restriction $$= \\binom{10}{5} = \\frac{10 \\times 9 \\times 8 \\times 7 \\times 6}{120} = 252$$.
Case 0 CAs: $$\\binom{6}{5} = 6$$.
Case 1 CA: $$\\binom{4}{1} \\times \\binom{6}{4} = 4 \\times 15 = 60$$.
Ways with at least 2 CAs $$= 252 - (6 + 60) = 252 - 66 = 186$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000031'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Geometric / Sequential Probability',
        'HARD',
        'ANALYZE',
        'Three loan recovery agents A, B, and C attempt to resolve a defaulted high-value NPA account. Their individual probabilities of successfully resolving the account are $$\\frac{2}{3}$$, $$\\frac{3}{4}$$, and $$\\frac{4}{5}$$ respectively. What is the probability that the NPA is resolved by exactly two of them?',
        '[{"id": "A", "text": "$$\\frac{13}{30}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{17}{30}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{7}{15}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{11}{30}$$", "isCorrect": false}]',
        'A',
        'Let $$P(A)=2/3, P(B)=3/4, P(C)=4/5$$, so $$P(A'')=1/3, P(B'')=1/4, P(C'')=1/5$$.
Exactly two resolve:
$$P(A \\cap B \\cap C'') = \\frac{2}{3} \\times \\frac{3}{4} \\times \\frac{1}{5} = \\frac{6}{60}$$
$$P(A \\cap B'' \\cap C) = \\frac{2}{3} \\times \\frac{1}{4} \\times \\frac{4}{5} = \\frac{8}{60}$$
$$P(A'' \\cap B \\cap C) = \\frac{1}{3} \\times \\frac{3}{4} \\times \\frac{4}{5} = \\frac{12}{60}$$
Total probability $$= \\frac{6 + 8 + 12}{60} = \\frac{26}{60} = \\frac{13}{30}$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000032'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Binomial Distribution',
        'HARD',
        'ANALYZE',
        'A digital payment gateway exhibits a transaction failure rate of $$p = 0.1$$. If a sample of $$5$$ independent transactions is processed, what is the probability that at least $$2$$ transactions fail?',
        '[{"id": "A", "text": "$$0.08146$$", "isCorrect": true}, {"id": "B", "text": "$$0.12450$$", "isCorrect": false}, {"id": "C", "text": "$$0.05230$$", "isCorrect": false}, {"id": "D", "text": "$$0.09150$$", "isCorrect": false}]',
        'A',
        '$$P(X \\ge 2) = 1 - [P(X=0) + P(X=1)]$$.
$$P(X=0) = (0.9)^5 = 0.59049$$.
$$P(X=1) = \\binom{5}{1} (0.1)^1 (0.9)^4 = 5 \\times 0.1 \\times 0.6561 = 0.32805$$.
$$P(X=0) + P(X=1) = 0.59049 + 0.32805 = 0.91854$$.
$$P(X \\ge 2) = 1 - 0.91854 = 0.08146$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000033'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Coefficient of Variation & Moments',
        'HARD',
        'ANALYZE',
        'A distribution of $$n = 20$$ observation values has $$\\sum x_i = 200$$ and $$\\sum x_i^2 = 2420$$. What is the Coefficient of Variation ($$\\text{CV}$$) of this dataset?',
        '[{"id": "A", "text": "$$45.83\\%$$", "isCorrect": true}, {"id": "B", "text": "$$21.00\\%$$", "isCorrect": false}, {"id": "C", "text": "$$35.50\\%$$", "isCorrect": false}, {"id": "D", "text": "$$52.40\\%$$", "isCorrect": false}]',
        'A',
        'Mean $$\\bar{x} = \\frac{200}{20} = 10$$.
Variance $$\\sigma^2 = \\frac{\\sum x_i^2}{n} - (\\bar{x})^2 = \\frac{2420}{20} - 10^2 = 121 - 100 = 21$$.
Standard deviation $$\\sigma = \\sqrt{21} \\approx 4.582576$$.
$$\\text{CV} = \\frac{\\sigma}{\\bar{x}} \\times 100 = \\frac{4.582576}{10} \\times 100 = 45.83\\%$$.'
    ),
    (
        'a1260000-0000-0000-0000-000000000034'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Statistics and Probability',
        'Poisson Arrival Rates',
        'HARD',
        'ANALYZE',
        'Customer arrivals at an ATM follow a Poisson distribution with an average arrival rate of $$\\lambda = 2$$ customers per minute. What is the probability that exactly $$3$$ customers arrive during a given $$2$$-minute interval?',
        '[{"id": "A", "text": "$$\\frac{32}{3} e^{-4}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{16}{3} e^{-2}$$", "isCorrect": false}, {"id": "C", "text": "$$8 e^{-4}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{4}{3} e^{-2}$$", "isCorrect": false}]',
        'A',
        'For a $$t = 2$$ minute interval, mean arrival parameter $$\\mu = \\lambda t = 2 \\times 2 = 4$$.
Poisson formula: $$P(X = k) = \\frac{\\mu^k e^{-\\mu}}{k!}$$.
For $$k = 3$$: $$P(X=3) = \\frac{4^3 e^{-4}}{3!} = \\frac{64 e^{-4}}{6} = \\frac{32}{3} e^{-4}$$.'
    ),

    -- =========================================================================
    -- RULE 6: General Intelligence and Reasoning > Coding and Decoding (EASY, APPLY) - 4 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000035'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Positional Shift Coding',
        'EASY',
        'APPLY',
        'In a certain code language, if **BANKING** is coded as **EDQNLQJ**, how will **CREDIT** be coded in that same language?',
        '[{"id": "A", "text": "FUHGLW", "isCorrect": true}, {"id": "B", "text": "FUGHLW", "isCorrect": false}, {"id": "C", "text": "EVHGLW", "isCorrect": false}, {"id": "D", "text": "FTHGKV", "isCorrect": false}]',
        'A',
        'Each letter is shifted forward by $$+3$$ alphabetical positions:
C(+3) = F, R(+3) = U, E(+3) = H, D(+3) = G, I(+3) = L, T(+3) = W.
Thus, CREDIT becomes FUHGLW.'
    ),
    (
        'a1260000-0000-0000-0000-000000000036'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Opposite Letter Coding',
        'EASY',
        'APPLY',
        'If in a code language, **LOAN** is coded as **OLZM**, what will be the code for **DEBT** in that code system?',
        '[{"id": "A", "text": "WVYG", "isCorrect": true}, {"id": "B", "text": "WVYH", "isCorrect": false}, {"id": "C", "text": "XUZG", "isCorrect": false}, {"id": "D", "text": "WWXG", "isCorrect": false}]',
        'A',
        'Each letter is replaced by its opposite letter in the alphabet (sum of positions $$= 27$$):
D(4) -> W(23), E(5) -> V(22), B(2) -> Y(25), T(20) -> G(7).
Code: WVYG.'
    ),
    (
        'a1260000-0000-0000-0000-000000000037'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Alphabet Numerical Value Addition',
        'EASY',
        'APPLY',
        'If **RBI** is coded as $$39$$ ($$18 + 2 + 9 = 29$$? No, $$18+2+9 = 29 + 10 = 39$$), and **SBI** is coded as $$40$$ ($$19 + 2 + 9 + 10 = 40$$), what is the code for **IBPS** in this coding logic?',
        '[{"id": "A", "text": "$$61$$", "isCorrect": true}, {"id": "B", "text": "$$51$$", "isCorrect": false}, {"id": "C", "text": "$$59$$", "isCorrect": false}, {"id": "D", "text": "$$65$$", "isCorrect": false}]',
        'A',
        'The logic adds $$10$$ to the sum of alphabetical position values:
I(9) + B(2) + P(16) + S(19) = 46 + 5 = 51 + 10 = 61.'
    ),
    (
        'a1260000-0000-0000-0000-000000000038'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Direct Substitution Coding',
        'EASY',
        'APPLY',
        'In a code language:
"safe digital banking" is coded as "7 3 9",
"banking made easy" is coded as "3 5 2",
"easy digital payment" is coded as "5 9 8".
What digit represents the word **"safe"**?',
        '[{"id": "A", "text": "7", "isCorrect": true}, {"id": "B", "text": "3", "isCorrect": false}, {"id": "C", "text": "9", "isCorrect": false}, {"id": "D", "text": "5", "isCorrect": false}]',
        'A',
        'From (1) & (2): "banking" = 3.
From (1) & (3): "digital" = 9.
From (1): "safe digital banking" = 7 3 9, hence "safe" = 7.'
    ),

    -- =========================================================================
    -- RULE 7: General Intelligence and Reasoning > Problem Solving (HARD, ANALYZE) - 9 Qs
    -- =========================================================================
    -- Puzzle Set 1: Multi-variable 4-Floor 2-Flat Building with 8 Bank Executives (Q39 - Q42)
    (
        'a1260000-0000-0000-0000-000000000039'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Floor and Flat Puzzle',
        'HARD',
        'ANALYZE',
        'Eight executives P, Q, R, S, T, U, V, and W live in a 4-storey building (Floors 1 to 4 from bottom to top). Each floor has two flats: Flat 1 (West) and Flat 2 (East). Each executive heads a distinct department: Forex, Risk, Retail, Treasury, Audit, Compliance, IT, and HR.
- P lives on an even-numbered floor in Flat 1 and heads Treasury.
- The Risk manager lives immediately above P in the same flat type (Flat 1).
- Q lives on Floor 4 in Flat 2 and heads Forex.
- S lives in Flat 2 immediately below Q and heads Audit.
- U lives on Floor 4 in Flat 1 and heads IT.
- W lives on Floor 2 in Flat 2 and heads Compliance.
- V lives on Floor 1 in Flat 1 and heads Retail.
- R heads HR and lives on Floor 1 in Flat 2.

Who lives on Floor 3 in Flat 1, and which department does this person head?',
        '[{"id": "A", "text": "T, heading Risk", "isCorrect": true}, {"id": "B", "text": "S, heading Audit", "isCorrect": false}, {"id": "C", "text": "P, heading Treasury", "isCorrect": false}, {"id": "D", "text": "U, heading IT", "isCorrect": false}]',
        'A',
        'Since P is on Floor 2 Flat 1 (Treasury), the Risk manager immediately above P in Flat 1 is T on Floor 3 Flat 1.'
    ),
    (
        'a1260000-0000-0000-0000-000000000040'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Floor and Flat Puzzle',
        'HARD',
        'ANALYZE',
        'Based on the 4-floor flat puzzle above, which of the following executives lives on the same floor as the executive heading Compliance (W)?',
        '[{"id": "A", "text": "P (Treasury)", "isCorrect": true}, {"id": "B", "text": "T (Risk)", "isCorrect": false}, {"id": "C", "text": "V (Retail)", "isCorrect": false}, {"id": "D", "text": "Q (Forex)", "isCorrect": false}]',
        'A',
        'W (Compliance) lives on Floor 2 Flat 2. On Floor 2 Flat 1 lives P (Treasury).'
    ),
    (
        'a1260000-0000-0000-0000-000000000041'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Floor and Flat Puzzle',
        'HARD',
        'ANALYZE',
        'Based on the 4-floor flat puzzle above, who lives in Flat 2 of Floor 1 and which department do they manage?',
        '[{"id": "A", "text": "R, heading HR", "isCorrect": true}, {"id": "B", "text": "V, heading Retail", "isCorrect": false}, {"id": "C", "text": "W, heading Compliance", "isCorrect": false}, {"id": "D", "text": "S, heading Audit", "isCorrect": false}]',
        'A',
        'R lives on Floor 1 in Flat 2 and heads the HR department.'
    ),
    (
        'a1260000-0000-0000-0000-000000000042'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Floor and Flat Puzzle',
        'HARD',
        'ANALYZE',
        'Based on the 4-floor flat puzzle above, which of the following statements is FALSE?',
        '[{"id": "A", "text": "V lives on Floor 2 and heads the Retail department", "isCorrect": true}, {"id": "B", "text": "S heads the Audit department and lives on Floor 3 in Flat 2", "isCorrect": false}, {"id": "C", "text": "U heads IT and lives on Floor 4 in Flat 1", "isCorrect": false}, {"id": "D", "text": "Q heads Forex and lives on Floor 4 in Flat 2", "isCorrect": false}]',
        'A',
        'Statement A is false because V lives on Floor 1 (Flat 1), not Floor 2.'
    ),

    -- Puzzle Set 2: Circular Seating Arrangement with Inward/Outward Facing (Q43 - Q47)
    (
        'a1260000-0000-0000-0000-000000000043'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Circular Seating Puzzle',
        'HARD',
        'ANALYZE',
        'Eight bank officers A, B, C, D, E, F, G, and H sit around a circular table. Some face the center, while others face outward away from the center.
- B faces the center. A sits third to the right of B.
- C sits second to the left of A, and A faces outward (opposite to B).
- D sits second to the right of C, and D faces outward.
- E sits third to the left of D and faces inward.
- F sits third to the right of E and faces outward.
- G is not an immediate neighbour of B; G faces inward.
- H sits second to the left of G.

Who sits exactly opposite to B in this circular seating arrangement?',
        '[{"id": "A", "text": "D", "isCorrect": true}, {"id": "B", "text": "E", "isCorrect": false}, {"id": "C", "text": "F", "isCorrect": false}, {"id": "D", "text": "H", "isCorrect": false}]',
        'A',
        'Tracing the 8 positions: B (Pos 1, Inward) -> Pos 4 is A (Outward). C sits 2nd left of A -> Pos 2 (C). D is 2nd right of C -> Pos 5 (D, Outward). Since Pos 1 is B and Pos 5 is D, D sits exactly diametrically opposite to B (4 positions away in an 8-person circle).'
    ),
    (
        'a1260000-0000-0000-0000-000000000044'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Circular Seating Puzzle',
        'HARD',
        'ANALYZE',
        'In the circular arrangement above, how many officers face outward away from the center?',
        '[{"id": "A", "text": "4", "isCorrect": true}, {"id": "B", "text": "3", "isCorrect": false}, {"id": "C", "text": "5", "isCorrect": false}, {"id": "D", "text": "2", "isCorrect": false}]',
        'A',
        'Four officers face outward (A, D, F, and H) and four officers face inward (B, C, E, and G).'
    ),
    (
        'a1260000-0000-0000-0000-000000000045'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Circular Seating Puzzle',
        'HARD',
        'ANALYZE',
        'Who sits to the immediate left of D?',
        '[{"id": "A", "text": "E", "isCorrect": true}, {"id": "B", "text": "A", "isCorrect": false}, {"id": "C", "text": "F", "isCorrect": false}, {"id": "D", "text": "H", "isCorrect": false}]',
        'A',
        'D sits at Pos 5 and faces outward. Looking outward from Pos 5, immediate left is Pos 6, which is occupied by E.'
    ),
    (
        'a1260000-0000-0000-0000-000000000046'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Circular Seating Puzzle',
        'HARD',
        'ANALYZE',
        'What is the position of F with respect to G in the circle?',
        '[{"id": "A", "text": "Second to the right", "isCorrect": true}, {"id": "B", "text": "Third to the left", "isCorrect": false}, {"id": "C", "text": "Immediate left", "isCorrect": false}, {"id": "D", "text": "Fourth to the right", "isCorrect": false}]',
        'A',
        'G faces inward at Pos 7. Moving to the right of G (counter-clockwise): Pos 8 is H, Pos 1 is B, Pos 2 is C, Pos 3 is F, which is second to the right from Pos 1.'
    ),
    (
        'a1260000-0000-0000-0000-000000000047'::uuid,
        'General Intelligence and Reasoning',
        'Problem Solving',
        'Circular Seating Puzzle',
        'HARD',
        'ANALYZE',
        'Which of the following pairs of officers represents immediate neighbours facing opposite directions to each other?',
        '[{"id": "A", "text": "B and H", "isCorrect": true}, {"id": "B", "text": "A and D", "isCorrect": false}, {"id": "C", "text": "E and G", "isCorrect": false}, {"id": "D", "text": "F and H", "isCorrect": false}]',
        'A',
        'B faces inward (center) while H faces outward. Since B and H sit adjacent to each other at Pos 1 and Pos 8, they are immediate neighbours facing opposite directions.'
    ),

    -- =========================================================================
    -- RULE 8: General Intelligence and Reasoning > Drawing Inferences (MEDIUM, ANALYZE) - 5 Qs
    -- =========================================================================
    (
        'a1260000-0000-0000-0000-000000000048'::uuid,
        'General Intelligence and Reasoning',
        'Drawing Inferences',
        'Critical Reasoning & Policy Inferences',
        'MEDIUM',
        'ANALYZE',
        'Read the statement and determine which of the given inferences logically follows:

**Statement:** "To incentivize eco-friendly technologies, the central bank mandated that commercial banks must offer a 50-basis-point interest concession on credit extended to electric commercial vehicle fleets while maintaining standard risk-weighted capital adequacy buffers."

**Inferences:**
I. Borrowers purchasing electric commercial vehicle fleets will face lower borrowing costs compared to conventional diesel fleet operators.
II. The central bank has completely eliminated all capital reserve requirements for commercial vehicle lending.',
        '[{"id": "A", "text": "Only inference I follows", "isCorrect": true}, {"id": "B", "text": "Only inference II follows", "isCorrect": false}, {"id": "C", "text": "Both inferences I and II follow", "isCorrect": false}, {"id": "D", "text": "Neither inference I nor II follows", "isCorrect": false}]',
        'A',
        'A 50-basis-point interest discount directly translates to lower borrowing costs for EV fleets (Inference I is valid). Inference II is contradicted by the statement which explicitly states "maintaining standard risk-weighted capital adequacy buffers".'
    ),
    (
        'a1260000-0000-0000-0000-000000000049'::uuid,
        'General Intelligence and Reasoning',
        'Drawing Inferences',
        'Critical Reasoning & Policy Inferences',
        'MEDIUM',
        'ANALYZE',
        'Read the statement and evaluate the inferences:

**Statement:** "Following recurrent core-banking outage incidents during peak business hours, the banking ombudsman issued a directive barring Bank X from onboarding new digital credit card customers until a third-party cybersecurity and IT architecture audit certifies its core infrastructure resiliency."

**Inferences:**
I. Bank X''s existing credit card customers will no longer be allowed to use their cards for digital payments.
II. Regulatory authorities prioritize technological resiliency and consumer protection over unconstrained institutional customer expansion.',
        '[{"id": "A", "text": "Only inference II follows", "isCorrect": true}, {"id": "B", "text": "Only inference I follows", "isCorrect": false}, {"id": "C", "text": "Both inferences I and II follow", "isCorrect": false}, {"id": "D", "text": "Neither inference I nor II follows", "isCorrect": false}]',
        'A',
        'The directive specifically restricts onboarding NEW customers, not existing customers (making I invalid). Halting expansion until stability audits pass shows that the regulator prioritizes resiliency and consumer protection over aggressive growth (II follows).'
    ),
    (
        'a1260000-0000-0000-0000-000000000050'::uuid,
        'General Intelligence and Reasoning',
        'Drawing Inferences',
        'Critical Reasoning & Policy Inferences',
        'MEDIUM',
        'ANALYZE',
        'Read the statement and evaluate the inferences:

**Statement:** "Despite aggressive policy rate hikes by the central bank to curb persistent retail inflation, credit growth across housing and personal auto loans continued to expand at double-digit annual percentages."

**Inferences:**
I. Consumer demand for housing and vehicle assets remained resilient despite elevated borrowing costs.
II. Commercial banks refused to pass on the central bank''s repo rate hikes to retail loan products.',
        '[{"id": "A", "text": "Only inference I follows", "isCorrect": true}, {"id": "B", "text": "Only inference II follows", "isCorrect": false}, {"id": "C", "text": "Both inferences I and II follow", "isCorrect": false}, {"id": "D", "text": "Neither inference I nor II follows", "isCorrect": false}]',
        'A',
        'Double-digit credit growth under high policy rates indicates strong underlying consumer demand (I follows). Inference II cannot be inferred because banks may have increased rates, yet consumer demand persisted.'
    ),
    (
        'a1260000-0000-0000-0000-000000000051'::uuid,
        'General Intelligence and Reasoning',
        'Drawing Inferences',
        'Critical Reasoning & Policy Inferences',
        'MEDIUM',
        'ANALYZE',
        'Read the statement and evaluate the inferences:

**Statement:** "A longitudinal survey of microfinance institutions revealed that self-help group borrowers who received financial literacy and digital bookkeeping training exhibited a 40% lower non-performing asset (NPA) default rate compared to untrained peer groups."

**Inferences:**
I. Financial literacy and digital bookkeeping capabilities significantly enhance loan repayment discipline and micro-enterprise sustainability.
II. All loan defaults in the microfinance sector are exclusively caused by mathematical calculation errors.',
        '[{"id": "A", "text": "Only inference I follows", "isCorrect": true}, {"id": "B", "text": "Only inference II follows", "isCorrect": false}, {"id": "C", "text": "Both inferences I and II follow", "isCorrect": false}, {"id": "D", "text": "Neither inference I nor II follows", "isCorrect": false}]',
        'A',
        'The 40% drop in NPAs among trained groups directly demonstrates that financial literacy aids repayment discipline (I follows). Inference II makes an extreme and unfounded claim ("exclusively caused by mathematical calculation errors").'
    ),
    (
        'a1260000-0000-0000-0000-000000000052'::uuid,
        'General Intelligence and Reasoning',
        'Drawing Inferences',
        'Critical Reasoning & Policy Inferences',
        'MEDIUM',
        'ANALYZE',
        'Read the statement and evaluate the inferences:

**Statement:** "The introduction of automated e-mandates and recurring UPI debits for utility bills has reduced municipal property tax payment delays by over 65% across metropolitan corporations."

**Inferences:**
I. Automated frictionless payment mechanisms mitigate inadvertent defaults and payment forgetfulness among urban taxpayers.
II. Municipal corporations have eliminated manual offline tax collection counters entirely.',
        '[{"id": "A", "text": "Only inference I follows", "isCorrect": true}, {"id": "B", "text": "Only inference II follows", "isCorrect": false}, {"id": "C", "text": "Both inferences I and II follow", "isCorrect": false}, {"id": "D", "text": "Neither inference I nor II follows", "isCorrect": false}]',
        'A',
        'Automated recurring payment schedules prevent oversight/delays, directly supporting Inference I. There is no evidence indicating complete elimination of offline collection desks (II is an overextension).'
    )
) AS v(id, subject_name, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
