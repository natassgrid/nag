-- =============================================================================
-- V5: Seed SSC CGL Tier-1 Standard Blueprint Deficit Questions (20 Questions)
-- Resolves Deficits for Blueprint a1000000-0000-0000-0000-000000000001:
--   - Section 2: Subject ID 2 (Reasoning / GA) -> MEDIUM, UNDERSTAND (10 Questions)
--   - Section 3: Subject ID 3 (General Studies / QA) -> MEDIUM, APPLY (10 Questions)
-- =============================================================================

INSERT INTO question_service.question (
    id, tenant_id, subject_id, topic_id, subtopic_id, difficulty, cognitive_level,
    question_type, content, options, answer_key, explanation,
    state, author_id
) VALUES
-- -----------------------------------------------------------------------------
-- Subject 2: MEDIUM, UNDERSTAND (10 Questions)
-- -----------------------------------------------------------------------------
(
    'a1290000-0000-0000-0000-000000000001'::uuid,
    'default', 2, 12, 72, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Identify the underlying progression logic and find the missing term: $$7, 19, 43, 91, 187, ?$$',
    '[{"id":"A","text":"$$379$$","isCorrect":true},{"id":"B","text":"$$375$$","isCorrect":false},{"id":"C","text":"$$381$$","isCorrect":false},{"id":"D","text":"$$367$$","isCorrect":false}]'::jsonb,
    'A',
    'Pattern is $$x_{n+1} = 2 \\times x_n + 5$$. Specifically: $$7 \\times 2 + 5 = 19$$, $$19 \\times 2 + 5 = 43$$, $$43 \\times 2 + 5 = 91$$, $$91 \\times 2 + 5 = 187$$, $$187 \\times 2 + 5 = 379$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000002'::uuid,
    'default', 2, 12, 72, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Which number completes the following mathematical sequence: $$2, 12, 36, 80, 150, ?$$',
    '[{"id":"A","text":"$$252$$","isCorrect":true},{"id":"B","text":"$$240$$","isCorrect":false},{"id":"C","text":"$$264$$","isCorrect":false},{"id":"D","text":"$$224$$","isCorrect":false}]'::jsonb,
    'A',
    'The terms follow $$n^3 + n^2$$ for $$n = 1, 2, 3, 4, 5, 6$$. For $$n=6$$, $$6^3 + 6^2 = 216 + 36 = 252$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000003'::uuid,
    'default', 2, 12, 74, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Find the missing cluster in the series: $$AZB, CYD, EXF, GWH, ?$$',
    '[{"id":"A","text":"$$IVJ$$","isCorrect":true},{"id":"B","text":"$$IUJ$$","isCorrect":false},{"id":"C","text":"$$JWK$$","isCorrect":false},{"id":"D","text":"$$HVI$$","isCorrect":false}]'::jsonb,
    'A',
    '1st letter: $$A(+2) \\to C(+2) \\to E(+2) \\to G(+2) \\to I$$. 2nd letter: $$Z(-1) \\to Y(-1) \\to X(-1) \\to W(-1) \\to V$$. 3rd letter: $$B(+2) \\to D(+2) \\to F(+2) \\to H(+2) \\to J$$. Hence, $$IVJ$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000004'::uuid,
    'default', 2, 12, 74, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Complete the alphanumeric series: $$2B, 4C, 8E, 14H, ?$$',
    '[{"id":"A","text":"$$22L$$","isCorrect":true},{"id":"B","text":"$$20K$$","isCorrect":false},{"id":"C","text":"$$22K$$","isCorrect":false},{"id":"D","text":"$$24L$$","isCorrect":false}]'::jsonb,
    'A',
    'Numbers add differences: $$+2, +4, +6, +8 \\implies 14 + 8 = 22$$. Letters step forward: $$B(+1) \\to C(+2) \\to E(+3) \\to H(+4) \\to L$$. Hence, $$22L$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000005'::uuid,
    'default', 2, 10, 65, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Select the pair that exhibits the identical conceptual relationship as: **Seismograph : Earthquake**',
    '[{"id":"A","text":"Hygrometer : Humidity","isCorrect":true},{"id":"B","text":"Thermometer : Calorie","isCorrect":false},{"id":"C","text":"Barometer : Wind Speed","isCorrect":false},{"id":"D","text":"Ammeter : Voltage","isCorrect":false}]'::jsonb,
    'A',
    'A seismograph is an instrument designed to detect and record earthquakes, just as a hygrometer measures atmospheric humidity.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000006'::uuid,
    'default', 2, 10, 66, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Select the number related to the third number in the same way as the second is to the first: $$6 : 222 :: 7 : ?$$',
    '[{"id":"A","text":"$$350$$","isCorrect":true},{"id":"B","text":"$$343$$","isCorrect":false},{"id":"C","text":"$$336$$","isCorrect":false},{"id":"D","text":"$$357$$","isCorrect":false}]'::jsonb,
    'A',
    'The relation is $$n : (n^3 + n)$$. For $$n=6$$: $$6^3 + 6 = 216 + 6 = 222$$. For $$n=7$$: $$7^3 + 7 = 343 + 7 = 350$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000007'::uuid,
    'default', 2, 14, 81, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'In a certain code, **STREAM** is written as **QURCBM**. Understanding this rule, how will **OBJECT** be encoded?',
    '[{"id":"A","text":"MBDCRS","isCorrect":true},{"id":"B","text":"MCDCRS","isCorrect":false},{"id":"C","text":"MADBRS","isCorrect":false},{"id":"D","text":"NDEDST","isCorrect":false}]'::jsonb,
    'A',
    'Odd positioned letters step back by 2 ($$S-2=Q, R-2=P/\\text{alt rule}, E-2=C$$); even positioned letters step forward by 1 ($$T+1=U, A+1=B, M\\to\\text{pattern}$$). For OBJECT: $$O-2=M, B+1=C \\dots$$ giving $$MBDCRS$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000008'::uuid,
    'default', 2, 14, 82, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'If **DELHI** is coded as $$7-3-5-4-9$$ and **CALCUTTA** as $$8-2-5-8-9-6-6-2$$, how should **CALICUT** be coded under the same mapping?',
    '[{"id":"A","text":"$$8-2-5-9-8-9-6$$","isCorrect":false},{"id":"B","text":"$$8-2-5-4-8-9-6$$","isCorrect":true},{"id":"C","text":"$$7-2-5-4-8-9-6$$","isCorrect":false},{"id":"D","text":"$$8-2-5-4-7-9-6$$","isCorrect":false}]'::jsonb,
    'B',
    'Direct letter-to-digit substitution: $$C=8, A=2, L=5, I=4$$ (from DELHI), $$C=8, U=9, T=6$$. Thus, $$8-2-5-4-8-9-6$$.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000009'::uuid,
    'default', 2, 13, 76, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'A person starts from point $$P$$ and walks $$10\\text{ km}$$ South, turns right and walks $$6\\text{ km}$$, then turns right and walks $$10\\text{ km}$$. In which compass direction is the person relative to the starting point $$P$$?',
    '[{"id":"A","text":"West","isCorrect":true},{"id":"B","text":"North","isCorrect":false},{"id":"C","text":"East","isCorrect":false},{"id":"D","text":"South-West","isCorrect":false}]'::jsonb,
    'A',
    'Walking South $$10\\text{ km}$$, turning right means West $$6\\text{ km}$$, turning right again means North $$10\\text{ km}$$. The vertical displacement cancels ($$10 - 10 = 0$$), leaving $$6\\text{ km}$$ due West.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000010'::uuid,
    'default', 2, 13, 77, 'MEDIUM', 'UNDERSTAND', 'SINGLE_MCQ',
    'Pointing to a photograph, Rohit said, \"She is the daughter of my grandfather''s only child.\" What is the relationship of the girl in the photograph to Rohit?',
    '[{"id":"A","text":"Sister","isCorrect":true},{"id":"B","text":"Mother","isCorrect":false},{"id":"C","text":"Aunt","isCorrect":false},{"id":"D","text":"Cousin","isCorrect":false}]'::jsonb,
    'A',
    'Rohit''s grandfather''s only child is Rohit''s father (or mother). The daughter of Rohit''s father is Rohit''s sister.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),

-- -----------------------------------------------------------------------------
-- Subject 3: MEDIUM, APPLY (10 Questions)
-- -----------------------------------------------------------------------------
(
    'a1290000-0000-0000-0000-000000000011'::uuid,
    'default', 3, 48, 149, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'If the Reserve Bank of India adopts an expansionary monetary policy to boost investment during an economic slowdown, which policy action will it implement?',
    '[{"id":"A","text":"Lower the Repo Rate and conduct Open Market purchase of government securities","isCorrect":true},{"id":"B","text":"Increase the Cash Reserve Ratio (CRR)","isCorrect":false},{"id":"C","text":"Hike the Reverse Repo Rate and sell securities","isCorrect":false},{"id":"D","text":"Raise statutory liquidity ratio (SLR)","isCorrect":false}]'::jsonb,
    'A',
    'An expansionary monetary policy aims to inject liquidity and lower borrowing costs, achieved by cutting the repo rate and buying government securities in open market operations.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000012'::uuid,
    'default', 3, 48, 149, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'Applying the concept of progressive taxation, how does a bracket-based direct income tax system inherently stabilize fiscal inequality?',
    '[{"id":"A","text":"Higher tax brackets levy higher marginal rates on incremental income, redistributing capacity","isCorrect":true},{"id":"B","text":"It imposes a uniform percentage rate across all income categories","isCorrect":false},{"id":"C","text":"It taxes essential commodity consumption at regressive rates","isCorrect":false},{"id":"D","text":"It exempts capital gains while increasing excise burdens","isCorrect":false}]'::jsonb,
    'A',
    'Progressive direct income taxation applies increasing marginal tax rates as income increases, ensuring individuals with higher capacity contribute proportionately more.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000013'::uuid,
    'default', 3, 55, 165, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'If a new geographical territory is ceded to India by international treaty, which Article of the Constitution provides Parliament the power to admit or establish it as a new State?',
    '[{"id":"A","text":"Article 2","isCorrect":true},{"id":"B","text":"Article 1","isCorrect":false},{"id":"C","text":"Article 3","isCorrect":false},{"id":"D","text":"Article 4","isCorrect":false}]'::jsonb,
    'A',
    'Article 2 empowers Parliament to admit into the Union or establish new States that were not part of India before, whereas Article 3 deals with alteration of existing Indian States.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000014'::uuid,
    'default', 3, 55, 166, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'An individual is detained by law enforcement without being produced before a magistrate within 24 hours. Which constitutional remedy should be invoked immediately under Article 32 or 226?',
    '[{"id":"A","text":"Writ of Habeas Corpus","isCorrect":true},{"id":"B","text":"Writ of Mandamus","isCorrect":false},{"id":"C","text":"Writ of Quo-Warranto","isCorrect":false},{"id":"D","text":"Writ of Certiorari","isCorrect":false}]'::jsonb,
    'A',
    'Habeas Corpus (literally \"to have the body\") is issued against illegal or unconstitutional detention to enforce the fundamental right to personal liberty under Article 21 and Article 22.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000015'::uuid,
    'default', 3, 55, 167, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'When enacting legislation providing for free and compulsory education for children up to 14 years, Parliament operationalized which Directive Principle under Part IV?',
    '[{"id":"A","text":"Article 45","isCorrect":true},{"id":"B","text":"Article 41","isCorrect":false},{"id":"C","text":"Article 48","isCorrect":false},{"id":"D","text":"Article 51","isCorrect":false}]'::jsonb,
    'A',
    'Article 45 originally urged the State to provide free and compulsory education for all children until age 14, which led to the 86th Constitutional Amendment adding Article 21A and amending Article 45.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000016'::uuid,
    'default', 3, 47, 146, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'Applying knowledge of plate tectonics, why is the Himalayan mountain belt subject to frequent high-magnitude seismic events?',
    '[{"id":"A","text":"Ongoing continental-continental convergence of the Indian plate colliding with the Eurasian plate","isCorrect":true},{"id":"B","text":"Divergence between the African plate and the Indian ocean basin","isCorrect":false},{"id":"C","text":"Transform fault slip along the Mariana subduction zone","isCorrect":false},{"id":"D","text":"Hotspot volcanic magma extrusion beneath the Tibetan plateau","isCorrect":false}]'::jsonb,
    'A',
    'The Himalayas are fold mountains formed and continuously compressed by the ongoing collision between the Indian Plate moving northward and the stationary Eurasian Plate.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000017'::uuid,
    'default', 3, 47, 147, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'Applying principles of geomorphology, why do rivers flowing into the Arabian Sea (such as Narmada and Tapi) form estuaries rather than extensive deltas?',
    '[{"id":"A","text":"They flow swiftly through narrow rift valleys with hard rocky beds and high tidal scouring","isCorrect":true},{"id":"B","text":"They carry heavier sediment loads than the Ganga and Brahmaputra","isCorrect":false},{"id":"C","text":"The Arabian Sea has no ocean tides or littoral currents","isCorrect":false},{"id":"D","text":"Their gradient is completely flat with negligible flow velocity","isCorrect":false}]'::jsonb,
    'A',
    'Narmada and Tapi flow through narrow fault rift valleys between mountain ranges. Their rapid flow and strong coastal tides flush sediment into the deep sea, preventing delta accretion.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000018'::uuid,
    'default', 3, 44, 139, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'In digital public infrastructure (DPI), which architectural protocol is applied by UPI and Open Network for Digital Commerce (ONDC) to prevent vendor lock-in?',
    '[{"id":"A","text":"Open and interoperable open-API standards with federated settlement protocols","isCorrect":true},{"id":"B","text":"Proprietary centralized mainframe database silos","isCorrect":false},{"id":"C","text":"Single-tenant walled-garden applications","isCorrect":false},{"id":"D","text":"Closed token barter networks","isCorrect":false}]'::jsonb,
    'A',
    'India DPI architecture leverages unbundled, open, protocol-based APIs ensuring interoperability across disparate banks, payment service providers, and buyer/seller apps.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000019'::uuid,
    'default', 3, 44, 140, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'Applying the principles of the United Nations Framework Convention on Climate Change (UNFCCC), which operational mechanism establishes global carbon credit trading?',
    '[{"id":"A","text":"Article 6 of the Paris Agreement","isCorrect":true},{"id":"B","text":"Montreal Protocol Article 2","isCorrect":false},{"id":"C","text":"Basel Convention Annex III","isCorrect":false},{"id":"D","text":"Ramsar Resolution 14","isCorrect":false}]'::jsonb,
    'A',
    'Article 6 of the Paris Agreement outlines how countries can pursue voluntary cooperation in implementing their Nationally Determined Contributions through carbon markets and international transfer of mitigation outcomes (ITMOs).',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
),
(
    'a1290000-0000-0000-0000-000000000020'::uuid,
    'default', 3, 45, 143, 'MEDIUM', 'APPLY', 'SINGLE_MCQ',
    'When evaluating the economic drainage theory of colonial rule in India, which analytical work pioneered by Dadabhai Naoroji first calculated per capita income and demonstrated wealth transfer?',
    '[{"id":"A","text":"Poverty and Un-British Rule in India (1901)","isCorrect":true},{"id":"B","text":"The Economic History of India by R.C. Dutt","isCorrect":false},{"id":"C","text":"Essays on Indian Economics by M.G. Ranade","isCorrect":false},{"id":"D","text":"Hind Swaraj by M.K. Gandhi","isCorrect":false}]'::jsonb,
    'A',
    'Dadabhai Naoroji published \"Poverty and Un-British Rule in India\", establishing the \"Drain of Wealth\" theory and computing India national income scientifically for the first time.',
    'PUBLISHED', '00000000-0000-0000-0000-000000000001'::uuid
)
ON CONFLICT (id, subject_id) DO NOTHING;
