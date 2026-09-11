-- ============================================================
-- Seed Questions: RRB NTPC - Indian Polity and Governance (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Awareness / General Studies
-- Topic: Indian Polity
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX numbers/articles ($$..$$)
-- Blueprint Distribution:
--   - 35 EASY (Remember / Understand)
--   - 45 MEDIUM (Understand / Apply)
--   - 20 HARD (Apply / Analyze)
-- UUID Range: a10d0000-0000-0000-0000-000000000001 to a10d0000-0000-0000-0000-000000000100
-- ============================================================

-- Step 1: Ensure 'Indian Polity' topic exists under 'General Awareness' and 'General Studies'
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, 'Indian Polity', 'Indian constitution, governance and political system'
FROM question_service.subject s
WHERE s.name IN ('General Awareness', 'General Studies') AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- Step 2: Ensure all specific Indian Polity subtopics exist under 'Indian Polity'
INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
SELECT 'default', t.id, sub.name, sub.description
FROM question_service.subject s
JOIN question_service.topic t ON t.subject_id = s.id AND t.name = 'Indian Polity' AND t.tenant_id = 'default'
CROSS JOIN (VALUES
    ('Constitutional Framework & Historical Framing', 'Making of the Constitution, Constituent Assembly, Drafting Committee, and Sources borrowed'),
    ('Preamble, Union & Citizenship', 'Preamble philosophy, Union and its Territory (Articles 1-4), and Citizenship provisions (Articles 5-11)'),
    ('Fundamental Rights & Writs', 'Fundamental Rights (Articles 12-35), Constitutional remedies (Article 32, 226), and judicial writs'),
    ('Directive Principles & Fundamental Duties', 'Directive Principles of State Policy (Articles 36-51) and Fundamental Duties (Article 51A)'),
    ('Union Executive', 'President of India, Vice-President, Prime Minister, Council of Ministers, and Attorney General'),
    ('Union Legislature & Parliament', 'Lok Sabha, Rajya Sabha, Parliamentary proceedings, legislative procedure, and Parliamentary Committees'),
    ('State Executive & Legislature', 'Governor, Chief Minister, State Council of Ministers, and State Legislative Assemblies/Councils'),
    ('Judiciary', 'Supreme Court of India, High Courts, Subordinate Courts, and judicial doctrines'),
    ('Local Self-Government & Panchayati Raj', 'Panchayati Raj Institutions (73rd Amendment) and Municipalities (74th Amendment)'),
    ('Constitutional Bodies & Amendments', 'Election Commission, CAG, UPSC, Finance Commission, Emergency Provisions, and Constitutional Amendments')
) AS sub(name, description)
WHERE s.name IN ('General Awareness', 'General Studies') AND s.tenant_id = 'default'
ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

-- Step 3: Insert 100 RRB NTPC Indian Polity Questions
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
    'General Awareness',
    'Indian Polity',
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
    -- ========================================================
    -- SECTION 1: Constitutional Framework & Historical Framing (Q1 - Q10)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000001'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Who was elected as the permanent President of the Constituent Assembly of India on December $$11$$, $$1946$$?',
        '[{"id":"A","text":"Dr. Rajendra Prasad","isCorrect":true},{"id":"B","text":"Dr. B.R. Ambedkar","isCorrect":false},{"id":"C","text":"Dr. Sachchidananda Sinha","isCorrect":false},{"id":"D","text":"Jawaharlal Nehru","isCorrect":false}]',
        'A',
        'Dr. Rajendra Prasad was unanimously elected as the permanent President of the Constituent Assembly on December $$11$$, $$1946$$, succeeding the temporary president Dr. Sachchidananda Sinha.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000002'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Under which plan was the Constituent Assembly of India formulated in $$1946$$?',
        '[{"id":"A","text":"Cabinet Mission Plan","isCorrect":true},{"id":"B","text":"Cripps Mission Plan","isCorrect":false},{"id":"C","text":"Wavell Plan","isCorrect":false},{"id":"D","text":"Mountbatten Plan","isCorrect":false}]',
        'A',
        'The Constituent Assembly was constituted in November $$1946$$ under the scheme formulated by the Cabinet Mission Plan of $$1946$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000003'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'How much total time did the Constituent Assembly take to draft and finalize the Constitution of India?',
        '[{"id":"A","text":"$$2$$ years, $$11$$ months, and $$18$$ days","isCorrect":true},{"id":"B","text":"$$3$$ years, $$2$$ months, and $$15$$ days","isCorrect":false},{"id":"C","text":"$$2$$ years, $$9$$ months, and $$11$$ days","isCorrect":false},{"id":"D","text":"$$1$$ year, $$11$$ months, and $$28$$ days","isCorrect":false}]',
        'A',
        'The Constituent Assembly held $$11$$ sessions over a total period of $$2$$ years, $$11$$ months, and $$18$$ days before adopting the Constitution on November $$26$$, $$1949$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000004'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Who served as the Constitutional Advisor to the Constituent Assembly during the framing of the Constitution?',
        '[{"id":"A","text":"Sir B.N. Rau","isCorrect":true},{"id":"B","text":"Alladi Krishnaswamy Iyer","isCorrect":false},{"id":"C","text":"K.M. Munshi","isCorrect":false},{"id":"D","text":"H.N. Kunzru","isCorrect":false}]',
        'A',
        'Sir Benegal Narsing Rau (B.N. Rau) was appointed as the Constitutional Advisor to the Constituent Assembly and prepared the initial draft of the Indian Constitution.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000005'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The concept of a ''Concurrent List'' in the Seventh Schedule of the Indian Constitution was borrowed from the constitution of which country?',
        '[{"id":"A","text":"Australia","isCorrect":true},{"id":"B","text":"Canada","isCorrect":false},{"id":"C","text":"Ireland","isCorrect":false},{"id":"D","text":"United States of America","isCorrect":false}]',
        'A',
        'The feature of the Concurrent List along with freedom of trade, commerce and intercourse, and joint sitting of the two Houses of Parliament was borrowed from Australia.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000006'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The ideal of ''Rule of Law'' and the parliamentary form of executive were adopted into the Indian Constitution from which constitutional tradition?',
        '[{"id":"A","text":"British Constitution","isCorrect":true},{"id":"B","text":"French Constitution","isCorrect":false},{"id":"C","text":"German Weimar Constitution","isCorrect":false},{"id":"D","text":"Russian Constitution","isCorrect":false}]',
        'A',
        'The parliamentary government, Rule of Law, legislative procedure, single citizenship, cabinet system, prerogative writs, and bicameralism were drawn from the British Constitution.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000007'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'Which historical British statute served as the primary administrative blueprint for the Constitution of India, contributing more than half of its institutional provisions?',
        '[{"id":"A","text":"Government of India Act, $$1935$$","isCorrect":true},{"id":"B","text":"Government of India Act, $$1919$$","isCorrect":false},{"id":"C","text":"Indian Independence Act, $$1947$$","isCorrect":false},{"id":"D","text":"Indian Councils Act, $$1909$$","isCorrect":false}]',
        'A',
        'The Government of India Act of $$1935$$ provided the federal scheme, office of Governor, judiciary, Public Service Commissions, emergency provisions, and administrative details.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000008'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'On which date is ''Constitution Day'' (Samvidhan Divas) celebrated annually across India?',
        '[{"id":"A","text":"$$26^{\\text{th}}$$ November","isCorrect":true},{"id":"B","text":"$$26^{\\text{th}}$$ January","isCorrect":false},{"id":"C","text":"$$15^{\\text{th}}$$ August","isCorrect":false},{"id":"D","text":"$$9^{\\text{th}}$$ December","isCorrect":false}]',
        'A',
        'Constitution Day is celebrated on $$26^{\\text{th}}$$ November to commemorate the formal adoption of the Constitution of India by the Constituent Assembly in $$1949$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000009'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The procedure for amending the Constitution of India under Article $$368$$ was borrowed from which country?',
        '[{"id":"A","text":"South Africa","isCorrect":true},{"id":"B","text":"United States of America","isCorrect":false},{"id":"C","text":"Japan","isCorrect":false},{"id":"D","text":"Switzerland","isCorrect":false}]',
        'A',
        'The procedure for amendment of the Constitution and election of members of Rajya Sabha were borrowed from the Constitution of South Africa.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000010'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'The suspension of Fundamental Rights during an Emergency under the Indian Constitution was adopted from which source?',
        '[{"id":"A","text":"Weimar Constitution of Germany","isCorrect":true},{"id":"B","text":"Canadian Constitution","isCorrect":false},{"id":"C","text":"Constitution of the USSR","isCorrect":false},{"id":"D","text":"Irish Constitution","isCorrect":false}]',
        'A',
        'The suspension of Fundamental Rights during a National Emergency was borrowed from the Weimar Constitution of Germany.'
    ),

    -- ========================================================
    -- SECTION 2: Preamble, Union & Territory, Citizenship (Q11 - Q20)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000011'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'The Preamble to the Constitution of India is based on the historic ''Objectives Resolution'' introduced by whom in the Constituent Assembly?',
        '[{"id":"A","text":"Jawaharlal Nehru","isCorrect":true},{"id":"B","text":"Dr. B.R. Ambedkar","isCorrect":false},{"id":"C","text":"Sardar Vallabhbhai Patel","isCorrect":false},{"id":"D","text":"Mahatma Gandhi","isCorrect":false}]',
        'A',
        'Jawaharlal Nehru moved the historic ''Objectives Resolution'' on December $$13$$, $$1946$$, which outlined the constitutional ideals and ultimately shaped the Preamble.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000012'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'REMEMBER',
        'Which three words were added to the Preamble of the Indian Constitution by the $$42^{\\text{nd}}$$ Constitutional Amendment Act of $$1976$$?',
        '[{"id":"A","text":"Socialist, Secular, Integrity","isCorrect":true},{"id":"B","text":"Sovereign, Democratic, Republic","isCorrect":false},{"id":"C","text":"Justice, Liberty, Equality","isCorrect":false},{"id":"D","text":"Fraternity, Dignity, Unity","isCorrect":false}]',
        'A',
        'The $$42^{\\text{nd}}$$ Amendment Act of $$1976$$ amended the Preamble by adding the words ''Socialist'', ''Secular'', and ''Integrity''.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000013'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'In which landmark judicial judgment did the Supreme Court of India rule that the Preamble is an integral part of the Constitution and can be amended without altering the Basic Structure?',
        '[{"id":"A","text":"Kesavananda Bharati v. State of Kerala ($$1973$$)","isCorrect":true},{"id":"B","text":"Berubari Union case ($$1960$$)","isCorrect":false},{"id":"C","text":"Golaknath v. State of Punjab ($$1967$$)","isCorrect":false},{"id":"D","text":"Minerva Mills v. Union of India ($$1980$$)","isCorrect":false}]',
        'A',
        'In the Kesavananda Bharati case ($$1973$$), a $$13$$-judge bench held that the Preamble is an integral part of the Constitution and subject to amendment under Article $$368$$ without violating the basic structure.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000014'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'According to Article $$1$$ of the Constitution, India is declared as what?',
        '[{"id":"A","text":"A Union of States","isCorrect":true},{"id":"B","text":"A Federation of States","isCorrect":false},{"id":"C","text":"A Confederation of States","isCorrect":false},{"id":"D","text":"A Unitary Republic","isCorrect":false}]',
        'A',
        'Article $$1(1)$$ states: ''India, that is Bharat, shall be a Union of States'', indicating indestructible nature of the Union.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000015'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'APPLY',
        'Under Article $$3$$ of the Indian Constitution, who possesses the exclusive authority to form a new State or alter the boundaries and names of existing States?',
        '[{"id":"A","text":"Parliament of India by simple majority","isCorrect":true},{"id":"B","text":"President of India by executive decree","isCorrect":false},{"id":"C","text":"State Legislative Assembly concerned","isCorrect":false},{"id":"D","text":"Inter-State Council","isCorrect":false}]',
        'A',
        'Article $$3$$ empowers the Parliament of India to form new States and alter boundaries or names of existing States by enacting a law by a simple majority.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000016'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which was the first state in independent India created on a linguistic basis in October $$1953$$?',
        '[{"id":"A","text":"Andhra State","isCorrect":true},{"id":"B","text":"Gujarat","isCorrect":false},{"id":"C","text":"Kerala","isCorrect":false},{"id":"D","text":"Maharashtra","isCorrect":false}]',
        'A',
        'Andhra State was created on October $$1$$, $$1953$$ for Telugu-speaking people following the martyrdom of Potti Sreeramulu.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000017'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'REMEMBER',
        'Which constitutional commission recommended the reorganization of Indian states based on linguistic and administrative considerations in $$1955$$?',
        '[{"id":"A","text":"Fazal Ali Commission","isCorrect":true},{"id":"B","text":"Dhar Commission","isCorrect":false},{"id":"C","text":"JVP Committee","isCorrect":false},{"id":"D","text":"Sarkaria Commission","isCorrect":false}]',
        'A',
        'The States Reorganisation Commission headed by Justice Fazal Ali (with K.M. Panikkar and H.N. Kunzru) submitted its report in $$1955$$, leading to the States Reorganisation Act of $$1956$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000018'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'In which Part and Articles of the Indian Constitution are provisions relating to Citizenship contained?',
        '[{"id":"A","text":"Part II, Articles $$5$$ to $$11$$","isCorrect":true},{"id":"B","text":"Part I, Articles $$1$$ to $$4$$","isCorrect":false},{"id":"C","text":"Part III, Articles $$12$$ to $$35$$","isCorrect":false},{"id":"D","text":"Part IV, Articles $$36$$ to $$51$$","isCorrect":false}]',
        'A',
        'Citizenship is governed by Articles $$5$$ to $$11$$ under Part II of the Constitution of India.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000019'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'Which principle describes the nature of citizenship provided under the Indian Constitution across all States and Union Territories?',
        '[{"id":"A","text":"Single Citizenship for the entire Union","isCorrect":true},{"id":"B","text":"Dual Citizenship for State and Union","isCorrect":false},{"id":"C","text":"Multiple Citizenship based on domicile","isCorrect":false},{"id":"D","text":"Federal Dual Nationality","isCorrect":false}]',
        'A',
        'India provides for single uniform citizenship throughout the country, unlike federal systems such as the USA where dual citizenship exists.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000020'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Under Article $$11$$ of the Constitution, which institution is empowered to regulate the acquisition and termination of citizenship by law?',
        '[{"id":"A","text":"Parliament of India","isCorrect":true},{"id":"B","text":"President of India","isCorrect":false},{"id":"C","text":"Supreme Court of India","isCorrect":false},{"id":"D","text":"Union Ministry of Home Affairs","isCorrect":false}]',
        'A',
        'Article $$11$$ explicitly vests Parliament with the power to make provisions with respect to the acquisition and termination of citizenship and all matters relating thereto (e.g., Citizenship Act, $$1955$$).'
    ),

    -- ========================================================
    -- SECTION 3: Fundamental Rights & Writs (Q21 - Q34)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000021'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Part of the Indian Constitution is widely acclaimed by jurists as the ''Magna Carta of India''?',
        '[{"id":"A","text":"Part III","isCorrect":true},{"id":"B","text":"Part IV","isCorrect":false},{"id":"C","text":"Part IVA","isCorrect":false},{"id":"D","text":"Part IX","isCorrect":false}]',
        'A',
        'Part III (Articles $$12$$ to $$35$$) guarantees Fundamental Rights and is described as the Magna Carta of India.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000022'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution abolishes ''Untouchability'' and forbids its practice in any form?',
        '[{"id":"A","text":"Article $$17$$","isCorrect":true},{"id":"B","text":"Article $$14$$","isCorrect":false},{"id":"C","text":"Article $$19$$","isCorrect":false},{"id":"D","text":"Article $$21$$","isCorrect":false}]',
        'A',
        'Article $$17$$ abolishes untouchability and makes its practice in any form a punishable offence in accordance with law.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000023'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$18$$ of the Constitution, what restriction is placed on titles granted by the State?',
        '[{"id":"A","text":"No title, not being a military or academic distinction, shall be conferred by the State","isCorrect":true},{"id":"B","text":"All Bharat Ratna awards are completely prohibited","isCorrect":false},{"id":"C","text":"Foreign titles can be accepted without Presidential assent","isCorrect":false},{"id":"D","text":"Only royal titles of princely states are retained","isCorrect":false}]',
        'A',
        'Article $$18(1)$$ prohibits the State from conferring any title other than military or academic distinctions.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000024'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'REMEMBER',
        'How many democratic freedoms are currently guaranteed to Indian citizens under Article $$19(1)$$ of the Constitution?',
        '[{"id":"A","text":"$$6$$ freedoms","isCorrect":true},{"id":"B","text":"$$7$$ freedoms","isCorrect":false},{"id":"C","text":"$$8$$ freedoms","isCorrect":false},{"id":"D","text":"$$5$$ freedoms","isCorrect":false}]',
        'A',
        'Originally there were $$7$$ freedoms, but the Right to acquire, hold and dispose of property under Article $$19(1)(f)$$ was deleted by the $$44^{\\text{th}}$$ Amendment ($$1978$$), leaving $$6$$ freedoms.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000025'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which two Fundamental Rights under Part III cannot be suspended even during the operation of a National Emergency declared under Article $$352$$?',
        '[{"id":"A","text":"Article $$20$$ and Article $$21$$","isCorrect":true},{"id":"B","text":"Article $$14$$ and Article $$19$$","isCorrect":false},{"id":"C","text":"Article $$19$$ and Article $$22$$","isCorrect":false},{"id":"D","text":"Article $$25$$ and Article $$32$$","isCorrect":false}]',
        'A',
        'By virtue of the $$44^{\\text{th}}$$ Constitutional Amendment Act of $$1978$$, Articles $$20$$ (protection in respect of conviction for offences) and $$21$$ (protection of life and personal liberty) cannot be suspended during an Emergency.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000026'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment inserted Article $$21\\text{A}$$, making the Right to Free and Compulsory Education for children aged $$6$$ to $$14$$ years a Fundamental Right?',
        '[{"id":"A","text":"$$86^{\\text{th}}$$ Constitutional Amendment Act, $$2002$$","isCorrect":true},{"id":"B","text":"$$44^{\\text{th}}$$ Constitutional Amendment Act, $$1978$$","isCorrect":false},{"id":"C","text":"$$91^{\\text{st}}$$ Constitutional Amendment Act, $$2003$$","isCorrect":false},{"id":"D","text":"$$73^{\\text{rd}}$$ Constitutional Amendment Act, $$1992$$","isCorrect":false}]',
        'A',
        'The $$86^{\\text{th}}$$ Amendment Act ($$2002$$) inserted Article $$21\\text{A}$$ into Part III, enacted as the Right of Children to Free and Compulsory Education (RTE) Act, $$2009$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000027'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Which Article of the Constitution prohibits the employment of children below the age of $$14$$ years in hazardous occupations, factories, or mines?',
        '[{"id":"A","text":"Article $$24$$","isCorrect":true},{"id":"B","text":"Article $$23$$","isCorrect":false},{"id":"C","text":"Article $$21$$","isCorrect":false},{"id":"D","text":"Article $$22$$","isCorrect":false}]',
        'A',
        'Article $$24$$ prohibits the employment of children below $$14$$ years of age in any factory, mine, or other hazardous employment.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000028'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'What legal status was assigned to the ''Right to Property'' after its removal from Fundamental Rights by the $$44^{\\text{th}}$$ Constitutional Amendment in $$1978$$?',
        '[{"id":"A","text":"A Constitutional / Legal Right under Article $$300\\text{A}$$","isCorrect":true},{"id":"B","text":"A Directive Principle of State Policy under Article $$39$$","isCorrect":false},{"id":"C","text":"A Fundamental Duty under Article $$51\\text{A}$$","isCorrect":false},{"id":"D","text":"An unwritten common-law convention","isCorrect":false}]',
        'A',
        'The $$44^{\\text{th}}$$ Amendment shifted the Right to Property from Article $$31$$ to Article $$300\\text{A}$$ in Part XII as a constitutional/legal right.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000029'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'APPLY',
        'Which prerogative writ is issued by superior courts to secure the release of a person unlawfully or illegally detained in custody?',
        '[{"id":"A","text":"Habeas Corpus","isCorrect":true},{"id":"B","text":"Mandamus","isCorrect":false},{"id":"C","text":"Quo-Warranto","isCorrect":false},{"id":"D","text":"Certiorari","isCorrect":false}]',
        'A',
        'Habeas Corpus (Latin for ''to have the body'') is an order issued to produce a detained person before the court and examine the legality of detention.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000030'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'APPLY',
        'Which writ is issued by a court commanding a public official or public authority to perform a mandatory statutory duty that they have refused to perform?',
        '[{"id":"A","text":"Mandamus","isCorrect":true},{"id":"B","text":"Habeas Corpus","isCorrect":false},{"id":"C","text":"Prohibition","isCorrect":false},{"id":"D","text":"Quo-Warranto","isCorrect":false}]',
        'A',
        'Mandamus (meaning ''we command'') is issued by a court to a public authority or subordinate official to enforce the performance of a public or statutory duty.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000031'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which writ is issued to prevent an usurper from wrongfully holding a public office and to inquire into the legality of their claim to that office?',
        '[{"id":"A","text":"Quo-Warranto","isCorrect":true},{"id":"B","text":"Certiorari","isCorrect":false},{"id":"C","text":"Mandamus","isCorrect":false},{"id":"D","text":"Habeas Corpus","isCorrect":false}]',
        'A',
        'Quo-Warranto (meaning ''by what authority'') prevents illegal usurpation of a public office created by statute or constitution.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000032'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'How does the writ jurisdiction of a High Court under Article $$226$$ compare to that of the Supreme Court under Article $$32$$?',
        '[{"id":"A","text":"The High Court writ jurisdiction is broader, covering Fundamental Rights and any other legal right","isCorrect":true},{"id":"B","text":"The Supreme Court writ jurisdiction is broader in all aspects","isCorrect":false},{"id":"C","text":"Both have identical territorial and substantive jurisdiction","isCorrect":false},{"id":"D","text":"High Courts can only issue writs against lower trial courts","isCorrect":false}]',
        'A',
        'Article $$32$$ can only be invoked for Fundamental Rights, whereas Article $$226$$ empowers High Courts to issue writs for Fundamental Rights as well as ordinary legal rights.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000033'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution guarantees the right to freedom of conscience and free profession, practice, and propagation of religion?',
        '[{"id":"A","text":"Article $$25$$","isCorrect":true},{"id":"B","text":"Article $$28$$","isCorrect":false},{"id":"C","text":"Article $$29$$","isCorrect":false},{"id":"D","text":"Article $$30$$","isCorrect":false}]',
        'A',
        'Article $$25$$ guarantees to all persons equal entitlement to freedom of conscience and the right to freely profess, practice, and propagate religion.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000034'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Article $$20(2)$$ states that ''No person shall be prosecuted and punished for the same offence more than once''. Which legal doctrine is this?',
        '[{"id":"A","text":"Doctrine of Double Jeopardy","isCorrect":true},{"id":"B","text":"Rule against Ex-Post Facto Law","isCorrect":false},{"id":"C","text":"Right against Self-Incrimination","isCorrect":false},{"id":"D","text":"Doctrine of Severability","isCorrect":false}]',
        'A',
        'Article $$20(2)$$ incorporates the principle of ''Double Jeopardy'' (nemo debet bis vexari pro una et eadem causa).'
    ),

    -- ========================================================
    -- SECTION 4: Directive Principles & Fundamental Duties (Q35 - Q44)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000035'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'In which Part of the Indian Constitution are the Directive Principles of State Policy (DPSP) enshrined?',
        '[{"id":"A","text":"Part IV (Articles $$36$$ to $$51$$)","isCorrect":true},{"id":"B","text":"Part III (Articles $$12$$ to $$35$$)","isCorrect":false},{"id":"C","text":"Part IVA (Article $$51\\text{A}$$)","isCorrect":false},{"id":"D","text":"Part V (Articles $$52$$ to $$151$$)","isCorrect":false}]',
        'A',
        'Part IV of the Constitution contains the Directive Principles of State Policy across Articles $$36$$ to $$51$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000036'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'What is the legal enforceability of Directive Principles of State Policy as stated in Article $$37$$?',
        '[{"id":"A","text":"They are non-justiciable and cannot be enforced by any court","isCorrect":true},{"id":"B","text":"They are directly enforceable by writs under Article $$32$$","isCorrect":false},{"id":"C","text":"They take absolute precedence over Fundamental Rights in all cases","isCorrect":false},{"id":"D","text":"They are legally binding upon all private corporations","isCorrect":false}]',
        'A',
        'Article $$37$$ states that DPSPs shall not be enforceable by any court, but they are fundamental in the governance of the country.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000037'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Which Article under Part IV directs the State to organize Village Panchayats as units of local self-government?',
        '[{"id":"A","text":"Article $$40$$","isCorrect":true},{"id":"B","text":"Article $$44$$","isCorrect":false},{"id":"C","text":"Article $$48$$","isCorrect":false},{"id":"D","text":"Article $$50$$","isCorrect":false}]',
        'A',
        'Article $$40$$ reflects the Gandhian principle directing the State to take steps to organize village panchayats.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000038'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'REMEMBER',
        'Which Article of the Constitution of India provides for a Uniform Civil Code (UCC) for all citizens throughout the territory of India?',
        '[{"id":"A","text":"Article $$44$$","isCorrect":true},{"id":"B","text":"Article $$41$$","isCorrect":false},{"id":"C","text":"Article $$45$$","isCorrect":false},{"id":"D","text":"Article $$49$$","isCorrect":false}]',
        'A',
        'Article $$44$$ directs that the State shall endeavour to secure for the citizens a Uniform Civil Code throughout the territory of India.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000039'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Article $$50$$ of the Constitution directs the State to take steps to achieve what institutional objective?',
        '[{"id":"A","text":"Separation of the judiciary from the executive in the public services","isCorrect":true},{"id":"B","text":"Promotion of international peace and security","isCorrect":false},{"id":"C","text":"Protection of monuments and places of national importance","isCorrect":false},{"id":"D","text":"Organization of agriculture and animal husbandry","isCorrect":false}]',
        'A',
        'Article $$50$$ provides for the separation of the judiciary from the executive in the public services of the State.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000040'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'APPLY',
        'Which Article was inserted into Part IV by the $$42^{\\text{nd}}$$ Amendment to provide for ''Equal justice and free legal aid'' to the poor and weaker sections?',
        '[{"id":"A","text":"Article $$39\\text{A}$$","isCorrect":true},{"id":"B","text":"Article $$43\\text{A}$$","isCorrect":false},{"id":"C","text":"Article $$48\\text{A}$$","isCorrect":false},{"id":"D","text":"Article $$43\\text{B}$$","isCorrect":false}]',
        'A',
        'Article $$39\\text{A}$$ was added by the $$42^{\\text{nd}}$$ Amendment ($$1976$$) to provide free legal aid to ensure justice is not denied due to economic or other disabilities (led to Legal Services Authorities Act, $$1987$$).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000041'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'On the recommendation of which committee were Fundamental Duties incorporated into the Indian Constitution in $$1976$$?',
        '[{"id":"A","text":"Swaran Singh Committee","isCorrect":true},{"id":"B","text":"Verma Committee","isCorrect":false},{"id":"C","text":"Sarkaria Commission","isCorrect":false},{"id":"D","text":"Kothari Commission","isCorrect":false}]',
        'A',
        'The Swaran Singh Committee recommended incorporating Fundamental Duties, leading to the enactment of the $$42^{\\text{nd}}$$ Constitutional Amendment Act of $$1976$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000042'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'REMEMBER',
        'How many total Fundamental Duties are currently listed under Article $$51\\text{A}$$ of the Indian Constitution?',
        '[{"id":"A","text":"$$11$$ duties","isCorrect":true},{"id":"B","text":"$$10$$ duties","isCorrect":false},{"id":"C","text":"$$12$$ duties","isCorrect":false},{"id":"D","text":"$$9$$ duties","isCorrect":false}]',
        'A',
        'Originally $$10$$ duties were added by the $$42^{\\text{nd}}$$ Amendment ($$1976$$). The $$11^{\\text{th}}$$ duty was added by the $$86^{\\text{th}}$$ Amendment Act ($$2002$$).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000043'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'What was the subject of the $$11^{\\text{th}}$$ Fundamental Duty added under Article $$51\\text{A}(k)$$ by the $$86^{\\text{th}}$$ Constitutional Amendment Act, $$2002$$?',
        '[{"id":"A","text":"Duty of a parent or guardian to provide opportunities for education to their child aged $$6$$ to $$14$$ years","isCorrect":true},{"id":"B","text":"Duty to cast vote in all general elections","isCorrect":false},{"id":"C","text":"Duty to pay taxes promptly","isCorrect":false},{"id":"D","text":"Duty to protect and improve wildlife reserves","isCorrect":false}]',
        'A',
        'Article $$51\\text{A}(k)$$ places a duty on parents/guardians to provide education opportunities to their children between the ages of $$6$$ and $$14$$ years.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000044'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'Which Article under Part IV directs the State to take steps to separate the promotion of international peace and security and foster respect for international treaties?',
        '[{"id":"A","text":"Article $$51$$","isCorrect":true},{"id":"B","text":"Article $$48\\text{A}$$","isCorrect":false},{"id":"C","text":"Article $$46$$","isCorrect":false},{"id":"D","text":"Article $$47$$","isCorrect":false}]',
        'A',
        'Article $$51$$ deals with the promotion of international peace, security, just and honourable relations between nations, and respect for international law and treaties.'
    ),

    -- ========================================================
    -- SECTION 5: Union Executive (Q45 - Q56)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000045'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'What is the minimum age required for a citizen to be eligible for election as the President of India under Article $$58$$?',
        '[{"id":"A","text":"$$35$$ years","isCorrect":true},{"id":"B","text":"$$30$$ years","isCorrect":false},{"id":"C","text":"$$25$$ years","isCorrect":false},{"id":"D","text":"$$40$$ years","isCorrect":false}]',
        'A',
        'Article $$58(1)(b)$$ prescribes that a candidate for President of India must have completed the age of $$35$$ years.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000046'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Who constitutes the Electoral College for the election of the President of India under Article $$54$$?',
        '[{"id":"A","text":"Elected members of both Houses of Parliament and elected members of State Legislative Assemblies including Delhi and Puducherry","isCorrect":true},{"id":"B","text":"All members (elected and nominated) of both Houses of Parliament only","isCorrect":false},{"id":"C","text":"Elected members of Lok Sabha and State Legislative Assemblies only","isCorrect":false},{"id":"D","text":"All members of Parliament and State Legislative Councils","isCorrect":false}]',
        'A',
        'Under Article $$54$$, only elected members of Lok Sabha, Rajya Sabha, and State Legislative Assemblies (including UTs of Delhi and Puducherry) can vote in Presidential elections.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000047'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'Under Article $$61$$, what majority is required in each House of Parliament to pass a resolution for the impeachment of the President of India?',
        '[{"id":"A","text":"A majority of not less than two-thirds of the total membership of the House","isCorrect":true},{"id":"B","text":"A majority of two-thirds of members present and voting","isCorrect":false},{"id":"C","text":"A simple majority of total membership","isCorrect":false},{"id":"D","text":"Three-fourths majority of members present and voting","isCorrect":false}]',
        'A',
        'Article $$61(4)$$ mandates a special majority of not less than two-thirds of the total membership of each House to pass an impeachment resolution.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000048'::uuid,
        'Union Executive',
        'MEDIUM',
        'APPLY',
        'Which Article of the Constitution grants the President of India the power to grant pardons, reprieves, respites or remissions of punishment?',
        '[{"id":"A","text":"Article $$72$$","isCorrect":true},{"id":"B","text":"Article $$61$$","isCorrect":false},{"id":"C","text":"Article $$123$$","isCorrect":false},{"id":"D","text":"Article $$161$$","isCorrect":false}]',
        'A',
        'Article $$72$$ empowers the President to grant pardons, reprieves, respites, or remissions of punishment or to suspend, remit, or commute the sentence of any person convicted of an offence.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000049'::uuid,
        'Union Executive',
        'MEDIUM',
        'APPLY',
        'Under Article $$123$$, what is the maximum duration an Ordinance promulgated by the President remains valid without approval from Parliament?',
        '[{"id":"A","text":"$$6$$ weeks from the reassembly of Parliament","isCorrect":true},{"id":"B","text":"$$6$$ months from the date of promulgation","isCorrect":false},{"id":"C","text":"$$3$$ months from the reassembly of Parliament","isCorrect":false},{"id":"D","text":"$$1$$ year unconditionally","isCorrect":false}]',
        'A',
        'An ordinance must be laid before both Houses of Parliament and ceases to operate after $$6$$ weeks from the reassembly of Parliament unless approved earlier.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000050'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who serves as the ex-officio Chairman of the Rajya Sabha (Council of States)?',
        '[{"id":"A","text":"Vice-President of India","isCorrect":true},{"id":"B","text":"Prime Minister of India","isCorrect":false},{"id":"C","text":"Speaker of Lok Sabha","isCorrect":false},{"id":"D","text":"Leader of the Opposition","isCorrect":false}]',
        'A',
        'Under Articles $$64$$ and $$89(1)$$, the Vice-President of India is ex-officio Chairman of the Council of States (Rajya Sabha).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000051'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Who participates in the Electoral College for electing the Vice-President of India under Article $$66$$?',
        '[{"id":"A","text":"All members (both elected and nominated) of both Houses of Parliament","isCorrect":true},{"id":"B","text":"Only elected members of both Houses of Parliament","isCorrect":false},{"id":"C","text":"Elected members of Parliament and State Legislative Assemblies","isCorrect":false},{"id":"D","text":"Only Lok Sabha members","isCorrect":false}]',
        'A',
        'Under Article $$66$$, the Vice-President is elected by an electoral college consisting of all members (elected and nominated) of both Houses of Parliament without state assembly participation.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000052'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'According to Article $$75(3)$$, to which body is the Union Council of Ministers collectively responsible?',
        '[{"id":"A","text":"House of the People (Lok Sabha)","isCorrect":true},{"id":"B","text":"Council of States (Rajya Sabha)","isCorrect":false},{"id":"C","text":"President of India","isCorrect":false},{"id":"D","text":"Parliament as a whole","isCorrect":false}]',
        'A',
        'Article $$75(3)$$ provides that the Council of Ministers shall be collectively responsible to the House of the People (Lok Sabha).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000053'::uuid,
        'Union Executive',
        'HARD',
        'REMEMBER',
        'By which Constitutional Amendment was the maximum size of the Union Council of Ministers restricted to $$15\\%$$ of the total number of members of the Lok Sabha?',
        '[{"id":"A","text":"$$91^{\\text{st}}$$ Constitutional Amendment Act, $$2003$$","isCorrect":true},{"id":"B","text":"$$86^{\\text{th}}$$ Constitutional Amendment Act, $$2002$$","isCorrect":false},{"id":"C","text":"$$44^{\\text{th}}$$ Constitutional Amendment Act, $$1978$$","isCorrect":false},{"id":"D","text":"$$97^{\\text{th}}$$ Constitutional Amendment Act, $$2011$$","isCorrect":false}]',
        'A',
        'The $$91^{\\text{st}}$$ Amendment ($$2003$$) amended Article $$75(1\\text{A})$$, restricting the total number of ministers including the Prime Minister to not exceed $$15\\%$$ of Lok Sabha membership.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000054'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who is designated under Article $$76$$ as the highest law officer in the Government of India?',
        '[{"id":"A","text":"Attorney General for India","isCorrect":true},{"id":"B","text":"Solicitor General of India","isCorrect":false},{"id":"C","text":"Chief Justice of India","isCorrect":false},{"id":"D","text":"Union Law Minister","isCorrect":false}]',
        'A',
        'The Attorney General for India, appointed under Article $$76$$, is the highest law officer and primary legal adviser to the Union Government.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000055'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'Under Article $$88$$, which special privilege does the Attorney General for India possess with respect to parliamentary proceedings?',
        '[{"id":"A","text":"Right to speak and participate in either House of Parliament or joint sittings, but without the right to vote","isCorrect":true},{"id":"B","text":"Right to vote in the Rajya Sabha on constitutional amendment bills","isCorrect":false},{"id":"C","text":"Power to introduce Money Bills directly","isCorrect":false},{"id":"D","text":"Presiding authority over parliamentary standing committees","isCorrect":false}]',
        'A',
        'Article $$88$$ gives the Attorney General the right to speak and take part in proceedings of both Houses or any parliamentary committee, but without the right to vote.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000056'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'If the offices of both the President and the Vice-President become vacant simultaneously, who acts as the President of India?',
        '[{"id":"A","text":"Chief Justice of India","isCorrect":true},{"id":"B","text":"Speaker of Lok Sabha","isCorrect":false},{"id":"C","text":"Senior-most Governor","isCorrect":false},{"id":"D","text":"Attorney General of India","isCorrect":false}]',
        'A',
        'Under the President (Discharge of Functions) Act, $$1969$$, the Chief Justice of India (or in their absence, senior-most SC judge) discharges the functions of the President (e.g., Justice M. Hidayatullah in $$1969$$).'
    ),

    -- ========================================================
    -- SECTION 6: Union Legislature & Parliament (Q57 - Q68)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000057'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'According to Article $$79$$, what three components constitute the Parliament of India?',
        '[{"id":"A","text":"The President, Council of States (Rajya Sabha), and House of the People (Lok Sabha)","isCorrect":true},{"id":"B","text":"Lok Sabha, Rajya Sabha, and Prime Minister","isCorrect":false},{"id":"C","text":"Lok Sabha, Rajya Sabha, and Supreme Court","isCorrect":false},{"id":"D","text":"Lok Sabha and Rajya Sabha only","isCorrect":false}]',
        'A',
        'Article $$79$$ defines Parliament as comprising the President and the two Houses: the Council of States and the House of the People.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000058'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the maximum constitutional strength of the Rajya Sabha as laid down under Article $$80$$?',
        '[{"id":"A","text":"$$250$$ members","isCorrect":true},{"id":"B","text":"$$245$$ members","isCorrect":false},{"id":"C","text":"$$252$$ members","isCorrect":false},{"id":"D","text":"$$238$$ members","isCorrect":false}]',
        'A',
        'Article $$80$$ prescribes a maximum strength of $$250$$ members ($$238$$ representatives of States and UTs, plus $$12$$ members nominated by the President).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000059'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'Why is the Rajya Sabha described as a permanent legislative body?',
        '[{"id":"A","text":"It is not subject to dissolution; one-third of its members retire every second year","isCorrect":true},{"id":"B","text":"Its members hold lifetime appointments","isCorrect":false},{"id":"C","text":"It can never be adjourned by the Chairman","isCorrect":false},{"id":"D","text":"Its term is fixed for ten years","isCorrect":false}]',
        'A',
        'Article $$83(1)$$ provides that Rajya Sabha is not subject to dissolution, but one-third of its members retire every second year, each member serving a $$6$$-year term.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000060'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the minimum age required to become a member of the Lok Sabha and Rajya Sabha respectively?',
        '[{"id":"A","text":"$$25$$ years for Lok Sabha and $$30$$ years for Rajya Sabha","isCorrect":true},{"id":"B","text":"$$21$$ years for Lok Sabha and $$25$$ years for Rajya Sabha","isCorrect":false},{"id":"C","text":"$$30$$ years for Lok Sabha and $$35$$ years for Rajya Sabha","isCorrect":false},{"id":"D","text":"$$25$$ years for both Houses","isCorrect":false}]',
        'A',
        'Article $$84(b)$$ stipulates a minimum age of $$25$$ years for Lok Sabha and $$30$$ years for Rajya Sabha.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000061'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'APPLY',
        'Under Article $$100(3)$$, what is the quorum required to constitute a valid meeting of either House of Parliament?',
        '[{"id":"A","text":"One-tenth ($$10\\%$$) of the total number of members of the House","isCorrect":true},{"id":"B","text":"One-fifth of the total number of members of the House","isCorrect":false},{"id":"C","text":"One-third of the total number of members of the House","isCorrect":false},{"id":"D","text":"Fifty members in Lok Sabha and twenty-five in Rajya Sabha","isCorrect":false}]',
        'A',
        'Article $$100(3)$$ defines quorum as one-tenth of the total number of members of the House ($$55$$ in Lok Sabha and $$25$$ in Rajya Sabha).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000062'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'Who holds the final and conclusive authority under Article $$110(3)$$ to decide whether a bill is a Money Bill or not?',
        '[{"id":"A","text":"Speaker of the Lok Sabha","isCorrect":true},{"id":"B","text":"President of India","isCorrect":false},{"id":"C","text":"Chairman of the Rajya Sabha","isCorrect":false},{"id":"D","text":"Union Finance Minister","isCorrect":false}]',
        'A',
        'Article $$110(3)$$ provides that if any question arises whether a Bill is a Money Bill or not, the decision of the Speaker of the Lok Sabha shall be final and conclusive.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000063'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'What is the maximum period for which the Rajya Sabha can delay or detain a Money Bill passed by the Lok Sabha?',
        '[{"id":"A","text":"$$14$$ days","isCorrect":true},{"id":"B","text":"$$1$$ month","isCorrect":false},{"id":"C","text":"$$3$$ months","isCorrect":false},{"id":"D","text":"$$6$$ months","isCorrect":false}]',
        'A',
        'Under Article $$109(5)$$, if a Money Bill is not returned by Rajya Sabha to Lok Sabha within $$14$$ days, it is deemed to have been passed by both Houses.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000064'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'APPLY',
        'Under Article $$108$$, who summons a joint sitting of both Houses of Parliament in case of a legislative deadlock, and who presides over it?',
        '[{"id":"A","text":"Summoned by the President; presided over by the Speaker of Lok Sabha","isCorrect":true},{"id":"B","text":"Summoned by the Prime Minister; presided over by the Vice-President","isCorrect":false},{"id":"C","text":"Summoned and presided over by the Chairman of Rajya Sabha","isCorrect":false},{"id":"D","text":"Summoned and presided over by the President of India","isCorrect":false}]',
        'A',
        'The President summons a joint sitting under Article $$108$$, and under Article $$118(4)$$, the Speaker of the Lok Sabha presides over it.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000065'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'A Joint Sitting of Parliament under Article $$108$$ CANNOT be summoned for which of the following categories of bills?',
        '[{"id":"A","text":"Money Bills and Constitutional Amendment Bills","isCorrect":true},{"id":"B","text":"Ordinary legislative bills","isCorrect":false},{"id":"C","text":"Financial Bills Category I","isCorrect":false},{"id":"D","text":"Financial Bills Category II","isCorrect":false}]',
        'A',
        'Joint sittings apply only to ordinary and financial bills; there is no provision for a joint sitting for Money Bills (Lok Sabha overriding power) or Constitutional Amendment Bills (Article 368 requires separate passage in each House).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000066'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the maximum permissible time gap between two consecutive sessions of Parliament under Article $$85(1)$$?',
        '[{"id":"A","text":"$$6$$ months","isCorrect":true},{"id":"B","text":"$$3$$ months","isCorrect":false},{"id":"C","text":"$$4$$ months","isCorrect":false},{"id":"D","text":"$$9$$ months","isCorrect":false}]',
        'A',
        'Article $$85(1)$$ mandates that six months shall not intervene between the last sitting in one session and the date appointed for its first sitting in the next session.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000067'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'Which is the largest Parliamentary Committee of the Indian Parliament in terms of membership, comprising $$30$$ members entirely from the Lok Sabha?',
        '[{"id":"A","text":"Estimates Committee","isCorrect":true},{"id":"B","text":"Public Accounts Committee","isCorrect":false},{"id":"C","text":"Committee on Public Undertakings","isCorrect":false},{"id":"D","text":"Business Advisory Committee","isCorrect":false}]',
        'A',
        'The Estimates Committee has $$30$$ members, all elected from the Lok Sabha alone; Rajya Sabha has no representation on this committee.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000068'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'APPLY',
        'Which Parliamentary Committee is tasked with examining the annual audit reports submitted by the Comptroller and Auditor General (CAG) of India?',
        '[{"id":"A","text":"Public Accounts Committee (PAC)","isCorrect":true},{"id":"B","text":"Estimates Committee","isCorrect":false},{"id":"C","text":"Committee on Subordinate Legislation","isCorrect":false},{"id":"D","text":"Privileges Committee","isCorrect":false}]',
        'A',
        'The Public Accounts Committee ($$15$$ LS + $$7$$ RS members) examines the appropriation and audit accounts of the Government of India and the CAG reports.'
    ),

    -- ========================================================
    -- SECTION 7: State Executive & Legislature (Q69 - Q78)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000069'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Under Article $$155$$ of the Indian Constitution, who appoints the Governor of a State?',
        '[{"id":"A","text":"President of India","isCorrect":true},{"id":"B","text":"Chief Minister of the State","isCorrect":false},{"id":"C","text":"Chief Justice of the High Court","isCorrect":false},{"id":"D","text":"Prime Minister of India","isCorrect":false}]',
        'A',
        'The Governor of a State is appointed by the President of India by warrant under his hand and seal.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000070'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'What is the tenure of office of a State Governor according to Article $$156(1)$$?',
        '[{"id":"A","text":"Holds office during the pleasure of the President","isCorrect":true},{"id":"B","text":"Fixed non-renewable term of $$5$$ years","isCorrect":false},{"id":"C","text":"Until the age of $$65$$ years","isCorrect":false},{"id":"D","text":"Subject to confirmation by the State Assembly","isCorrect":false}]',
        'A',
        'Under Article $$156(1)$$, the Governor holds office during the pleasure of the President, though the normal term is $$5$$ years.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000071'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'APPLY',
        'Which Article empowers the Governor of a State to promulgate Ordinances during the recess of the State Legislature?',
        '[{"id":"A","text":"Article $$213$$","isCorrect":true},{"id":"B","text":"Article $$123$$","isCorrect":false},{"id":"C","text":"Article $$161$$","isCorrect":false},{"id":"D","text":"Article $$200$$","isCorrect":false}]',
        'A',
        'Article $$213$$ empowers the Governor to promulgate ordinances when the State Legislative Assembly (or both houses in bicameral states) is not in session.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000072'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Under Article $$165$$, who is appointed by the Governor as the highest legal adviser to the State Government?',
        '[{"id":"A","text":"Advocate General for the State","isCorrect":true},{"id":"B","text":"Attorney General for India","isCorrect":false},{"id":"C","text":"Chief Justice of High Court","isCorrect":false},{"id":"D","text":"State Law Secretary","isCorrect":false}]',
        'A',
        'The Advocate General for the State is appointed under Article $$165$$ and corresponds at the state level to the Attorney General of India.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000073'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'Which Article provides the procedure for the creation or abolition of Legislative Councils (Vidhan Parishad) in States by Parliament?',
        '[{"id":"A","text":"Article $$169$$","isCorrect":true},{"id":"B","text":"Article $$170$$","isCorrect":false},{"id":"C","text":"Article $$171$$","isCorrect":false},{"id":"D","text":"Article $$168$$","isCorrect":false}]',
        'A',
        'Article $$169$$ empowers Parliament to abolish or create a Legislative Council if the Legislative Assembly of the State passes a resolution by a special majority.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000074'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Which of the following Indian states possesses a bicameral legislature (having both Vidhan Sabha and Vidhan Parishad)?',
        '[{"id":"A","text":"Bihar","isCorrect":true},{"id":"B","text":"West Bengal","isCorrect":false},{"id":"C","text":"Tamil Nadu","isCorrect":false},{"id":"D","text":"Odisha","isCorrect":false}]',
        'A',
        'Currently, only $$6$$ Indian states have bicameral legislatures: Uttar Pradesh, Bihar, Maharashtra, Karnataka, Andhra Pradesh, and Telangana.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000075'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'What is the maximum constitutional limit on the number of members in a State Legislative Assembly (Vidhan Sabha) under Article $$170$$?',
        '[{"id":"A","text":"$$500$$ members","isCorrect":true},{"id":"B","text":"$$450$$ members","isCorrect":false},{"id":"C","text":"$$550$$ members","isCorrect":false},{"id":"D","text":"$$400$$ members","isCorrect":false}]',
        'A',
        'Article $$170(1)$$ stipulates that the Legislative Assembly of each State shall consist of not more than $$500$$ and not less than $$60$$ members (with specific exceptions like Goa, Sikkim, Mizoram).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000076'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'What is the maximum strength of a State Legislative Council (Vidhan Parishad) as prescribed under Article $$171$$?',
        '[{"id":"A","text":"One-third of the total membership of the State Legislative Assembly","isCorrect":true},{"id":"B","text":"One-half of the total membership of the State Legislative Assembly","isCorrect":false},{"id":"C","text":"Fixed at $$100$$ members in all states","isCorrect":false},{"id":"D","text":"One-fourth of the total membership of the State Legislative Assembly","isCorrect":false}]',
        'A',
        'Article $$171(1)$$ provides that the total number of members in the Legislative Council shall not exceed one-third of the total members in the Assembly, and in no case less than $$40$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000077'::uuid,
        'State Executive & Legislature',
        'HARD',
        'APPLY',
        'When a bill passed by the State Legislature is presented to the Governor under Article $$200$$, which special option does the Governor have regarding the President?',
        '[{"id":"A","text":"Reserve the bill for the consideration of the President","isCorrect":true},{"id":"B","text":"Refer the bill directly to the Supreme Court for advisory review","isCorrect":false},{"id":"C","text":"Amend the bill without legislative approval","isCorrect":false},{"id":"D","text":"Dismiss the State Cabinet immediately","isCorrect":false}]',
        'A',
        'Under Article $$200$$, the Governor may reserve a bill for the consideration of the President, which is mandatory if the bill derogates from the powers of the High Court.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000078'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$164(2)$$, to which institution is the State Council of Ministers collectively responsible?',
        '[{"id":"A","text":"Legislative Assembly of the State (Vidhan Sabha)","isCorrect":true},{"id":"B","text":"Legislative Council of the State (Vidhan Parishad)","isCorrect":false},{"id":"C","text":"Governor of the State","isCorrect":false},{"id":"D","text":"High Court of the State","isCorrect":false}]',
        'A',
        'Article $$164(2)$$ states that the Council of Ministers shall be collectively responsible to the Legislative Assembly of the State.'
    ),

    -- ========================================================
    -- SECTION 8: Judiciary (Supreme Court & High Courts) (Q79 - Q88)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000079'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'On which date was the Supreme Court of India officially inaugurated in New Delhi?',
        '[{"id":"A","text":"$$28^{\\text{th}}$$ January, $$1950$$","isCorrect":true},{"id":"B","text":"$$26^{\\text{th}}$$ January, $$1950$$","isCorrect":false},{"id":"C","text":"$$15^{\\text{th}}$$ August, $$1947$$","isCorrect":false},{"id":"D","text":"$$26^{\\text{th}}$$ November, $$1949$$","isCorrect":false}]',
        'A',
        'The Supreme Court of India was inaugurated on January $$28$$, $$1950$$, succeeding the Federal Court of India established under the Government of India Act, $$1935$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000080'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'What is the current sanctioned judicial strength of the Supreme Court of India including the Chief Justice of India?',
        '[{"id":"A","text":"$$34$$ Judges ($$1$$ CJI + $$33$$ Judges)","isCorrect":true},{"id":"B","text":"$$31$$ Judges ($$1$$ CJI + $$30$$ Judges)","isCorrect":false},{"id":"C","text":"$$26$$ Judges ($$1$$ CJI + $$25$$ Judges)","isCorrect":false},{"id":"D","text":"$$35$$ Judges ($$1$$ CJI + $$34$$ Judges)","isCorrect":false}]',
        'A',
        'The Supreme Court (Number of Judges) Amendment Act, $$2019$$ increased the sanctioned strength from $$31$$ to $$34$$ judges including the Chief Justice of India.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000081'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Up to what age does a Judge of the Supreme Court of India hold office under Article $$124(2)$$?',
        '[{"id":"A","text":"$$65$$ years","isCorrect":true},{"id":"B","text":"$$62$$ years","isCorrect":false},{"id":"C","text":"$$60$$ years","isCorrect":false},{"id":"D","text":"$$70$$ years","isCorrect":false}]',
        'A',
        'Under Article $$124(2)$$, a Supreme Court judge holds office until attaining the age of $$65$$ years (High Court judges retire at $$62$$ years).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000082'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'Under Article $$131$$, disputes between the Government of India and one or more States fall under which jurisdiction of the Supreme Court?',
        '[{"id":"A","text":"Original Jurisdiction","isCorrect":true},{"id":"B","text":"Appellate Jurisdiction","isCorrect":false},{"id":"C","text":"Advisory Jurisdiction","isCorrect":false},{"id":"D","text":"Review Jurisdiction","isCorrect":false}]',
        'A',
        'Article $$131$$ confers exclusive Original Jurisdiction upon the Supreme Court in federal disputes between the Union and States or between two or more States.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000083'::uuid,
        'Judiciary',
        'MEDIUM',
        'APPLY',
        'Which Article empowers the President of India to seek the advisory opinion of the Supreme Court on questions of law or public fact?',
        '[{"id":"A","text":"Article $$143$$","isCorrect":true},{"id":"B","text":"Article $$136$$","isCorrect":false},{"id":"C","text":"Article $$141$$","isCorrect":false},{"id":"D","text":"Article $$137$$","isCorrect":false}]',
        'A',
        'Article $$143$$ provides for the Advisory Jurisdiction of the Supreme Court, enabling the President to refer matters of public importance for judicial opinion.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000084'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'What does Article $$129$$ establish regarding the nature and authority of the Supreme Court of India?',
        '[{"id":"A","text":"It shall be a Court of Record and have power to punish for contempt of itself","isCorrect":true},{"id":"B","text":"It is subordinate to the Union Ministry of Law","isCorrect":false},{"id":"C","text":"Its decisions can be overruled by an executive decree","isCorrect":false},{"id":"D","text":"Its registry is subject to parliamentary audit","isCorrect":false}]',
        'A',
        'Article $$129$$ declares that the Supreme Court shall be a Court of Record and have all the powers of such a court, including the power to punish for contempt of itself.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000085'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'How many High Courts are currently functioning across India?',
        '[{"id":"A","text":"$$25$$ High Courts","isCorrect":true},{"id":"B","text":"$$24$$ High Courts","isCorrect":false},{"id":"C","text":"$$28$$ High Courts","isCorrect":false},{"id":"D","text":"$$21$$ High Courts","isCorrect":false}]',
        'A',
        'India has $$25$$ High Courts; the $$25^{\\text{th}}$$ is the High Court of Andhra Pradesh established at Amaravati in $$2019$$.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000086'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'What is the retirement age of a Judge of a High Court in India under Article $$217(1)$$?',
        '[{"id":"A","text":"$$62$$ years","isCorrect":true},{"id":"B","text":"$$65$$ years","isCorrect":false},{"id":"C","text":"$$60$$ years","isCorrect":false},{"id":"D","text":"$$58$$ years","isCorrect":false}]',
        'A',
        'The retirement age of a High Court judge is $$62$$ years (raised from $$60$$ by the $$15^{\\text{th}}$$ Constitutional Amendment Act of $$1963$$).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000087'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'Which Constitutional Amendment Act permitted the establishment of a common High Court for two or more States or Union Territories?',
        '[{"id":"A","text":"$$7^{\\text{th}}$$ Constitutional Amendment Act, $$1956$$","isCorrect":true},{"id":"B","text":"$$1^{\\text{st}}$$ Constitutional Amendment Act, $$1951$$","isCorrect":false},{"id":"C","text":"$$42^{\\text{nd}}$$ Constitutional Amendment Act, $$1976$$","isCorrect":false},{"id":"D","text":"$$44^{\\text{th}}$$ Constitutional Amendment Act, $$1978$$","isCorrect":false}]',
        'A',
        'The $$7^{\\text{th}}$$ Amendment ($$1956$$) amended Article $$231$$, empowering Parliament to establish a common High Court for two or more States (e.g., Punjab and Haryana High Court).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000088'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Who is regarded as the pioneer of ''Public Interest Litigation'' (PIL) in the Indian judicial system in the late $$1970$$s and early $$1980$$s?',
        '[{"id":"A","text":"Justice P.N. Bhagwati and Justice V.R. Krishna Iyer","isCorrect":true},{"id":"B","text":"Justice M. Hidayatullah","isCorrect":false},{"id":"C","text":"Justice H.J. Kania","isCorrect":false},{"id":"D","text":"Justice Ranjan Gogoi","isCorrect":false}]',
        'A',
        'Justices P.N. Bhagwati and V.R. Krishna Iyer pioneered Public Interest Litigation (PIL), relaxing locus standi to democratize access to justice.'
    ),

    -- ========================================================
    -- SECTION 9: Local Self-Government & Panchayati Raj (Q89 - Q94)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000089'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which committee recommended the introduction of a three-tier Panchayati Raj system in India in $$1957$$?',
        '[{"id":"A","text":"Balwant Rai Mehta Committee","isCorrect":true},{"id":"B","text":"Ashok Mehta Committee","isCorrect":false},{"id":"C","text":"L.M. Singhvi Committee","isCorrect":false},{"id":"D","text":"G.V.K. Rao Committee","isCorrect":false}]',
        'A',
        'The Balwant Rai Mehta Committee ($$1957$$) recommended a $$3$$-tier system: Gram Panchayat at village, Panchayat Samiti at block, and Zilla Parishad at district level.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000090'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'In which district and state was the Panchayati Raj system first inaugurated in independent India on October $$2$$, $$1959$$ by Jawaharlal Nehru?',
        '[{"id":"A","text":"Nagaur district, Rajasthan","isCorrect":true},{"id":"B","text":"Belgaum district, Karnataka","isCorrect":false},{"id":"C","text":"Guntur district, Andhra Pradesh","isCorrect":false},{"id":"D","text":"Wardha district, Maharashtra","isCorrect":false}]',
        'A',
        'The Panchayati Raj system was first inaugurated in Nagaur district of Rajasthan on October $$2$$, $$1959$$, followed by Andhra Pradesh.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000091'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'Which Part and Schedule were inserted into the Constitution by the $$73^{\\text{rd}}$$ Constitutional Amendment Act of $$1992$$?',
        '[{"id":"A","text":"Part IX and Eleventh Schedule","isCorrect":true},{"id":"B","text":"Part IXA and Twelfth Schedule","isCorrect":false},{"id":"C","text":"Part VIII and Tenth Schedule","isCorrect":false},{"id":"D","text":"Part X and Ninth Schedule","isCorrect":false}]',
        'A',
        'The $$73^{\\text{rd}}$$ Amendment added Part IX (Articles $$243$$ to $$243\\text{O}$$) and the Eleventh Schedule containing $$29$$ functional subjects for Panchayats.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000092'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'What minimum reservation of seats is constitutionally mandated for women in Panchayati Raj institutions under Article $$243\\text{D}(3)$$?',
        '[{"id":"A","text":"Not less than one-third ($$33.33\\%$$\\!) of total seats","isCorrect":true},{"id":"B","text":"Not less than fifty percent ($$50\\%$$\\!) in all states","isCorrect":false},{"id":"C","text":"Not less than twenty-five percent ($$25\\%$$\\!)","isCorrect":false},{"id":"D","text":"Ten percent ($$10\\%$$\\!) reservation","isCorrect":false}]',
        'A',
        'Article $$243\\text{D}(3)$$ mandates that not less than one-third of the total number of seats to be filled by direct election in every Panchayat shall be reserved for women.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000093'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'How many functional items are listed under the Twelfth Schedule added by the $$74^{\\text{th}}$$ Constitutional Amendment Act, $$1992$$ for Municipalities?',
        '[{"id":"A","text":"$$18$$ functional items","isCorrect":true},{"id":"B","text":"$$29$$ functional items","isCorrect":false},{"id":"C","text":"$$21$$ functional items","isCorrect":false},{"id":"D","text":"$$12$$ functional items","isCorrect":false}]',
        'A',
        'The Twelfth Schedule added by the $$74^{\\text{th}}$$ Amendment contains $$18$$ functional matters for Municipalities (Urban Local Bodies).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000094'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'APPLY',
        'Under Articles $$243\\text{K}$$ and $$243\\text{ZA}$$, who conducts and supervises all elections to Panchayats and Municipalities?',
        '[{"id":"A","text":"State Election Commission","isCorrect":true},{"id":"B","text":"Election Commission of India","isCorrect":false},{"id":"C","text":"District Collector","isCorrect":false},{"id":"D","text":"State Legislative Assembly Secretariat","isCorrect":false}]',
        'A',
        'Panchayat and municipal elections are conducted by the State Election Commission headed by a State Election Commissioner appointed by the Governor.'
    ),

    -- ========================================================
    -- SECTION 10: Constitutional Bodies, Amendments & Emergencies (Q95 - Q100)
    -- ========================================================
    (
        'a10d0000-0000-0000-0000-000000000095'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution establishes the Election Commission of India for superintendence, direction, and control of elections?',
        '[{"id":"A","text":"Article $$324$$","isCorrect":true},{"id":"B","text":"Article $$315$$","isCorrect":false},{"id":"C","text":"Article $$280$$","isCorrect":false},{"id":"D","text":"Article $$356$$","isCorrect":false}]',
        'A',
        'Article $$324$$ vests the superintendence, direction, and control of the preparation of electoral rolls and the conduct of all elections to Parliament, State Legislatures, and the offices of President and Vice-President in the Election Commission.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000096'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$148$$, who is designated as the ''Guardian of the Public Purse'' and head of the Indian Audit and Accounts Department?',
        '[{"id":"A","text":"Comptroller and Auditor General of India (CAG)","isCorrect":true},{"id":"B","text":"Union Finance Secretary","isCorrect":false},{"id":"C","text":"Governor of the Reserve Bank of India","isCorrect":false},{"id":"D","text":"Chairman of the Finance Commission","isCorrect":false}]',
        'A',
        'The CAG of India, appointed under Article $$148$$, controls the entire financial system of the country at both Union and State levels.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000097'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$280$$, the Finance Commission of India is constituted by the President at the expiration of every how many years?',
        '[{"id":"A","text":"Every fifth year or earlier","isCorrect":true},{"id":"B","text":"Every six years","isCorrect":false},{"id":"C","text":"Every three years","isCorrect":false},{"id":"D","text":"Every ten years coinciding with census","isCorrect":false}]',
        'A',
        'Article $$280(1)$$ requires the President to constitute a Finance Commission at the expiration of every fifth year or at such earlier time as considered necessary.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000098'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment Act reduced the voting age for elections to the Lok Sabha and State Legislative Assemblies from $$21$$ years to $$18$$ years?',
        '[{"id":"A","text":"$$61^{\\text{st}}$$ Constitutional Amendment Act, $$1988$$","isCorrect":true},{"id":"B","text":"$$42^{\\text{nd}}$$ Constitutional Amendment Act, $$1976$$","isCorrect":false},{"id":"C","text":"$$73^{\\text{rd}}$$ Constitutional Amendment Act, $$1992$$","isCorrect":false},{"id":"D","text":"$$86^{\\text{th}}$$ Constitutional Amendment Act, $$2002$$","isCorrect":false}]',
        'A',
        'The $$61^{\\text{st}}$$ Amendment Act ($$1988$$) amended Article $$326$$, lowering the voting age from $$21$$ to $$18$$ years.'
    ),
    (
        'a10d0000-0000-0000-0000-000000000099'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'Under Article $$352$$, on which three grounds can the President of India proclaim a National Emergency?',
        '[{"id":"A","text":"War, external aggression, or armed rebellion","isCorrect":true},{"id":"B","text":"War, external aggression, or internal disturbance","isCorrect":false},{"id":"C","text":"Financial breakdown, civil riot, or strike","isCorrect":false},{"id":"D","text":"Breakdown of constitutional machinery in all States","isCorrect":false}]',
        'A',
        'Article $$352$$ permits a National Emergency on grounds of ''War'', ''External aggression'', or ''Armed rebellion''. (The phrase ''internal disturbance'' was replaced with ''armed rebellion'' by the $$44^{\\text{th}}$$ Amendment in $$1978$$).'
    ),
    (
        'a10d0000-0000-0000-0000-000000000100'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'UNDERSTAND',
        'How many times has a Financial Emergency under Article $$360$$ of the Constitution been proclaimed in India since independence?',
        '[{"id":"A","text":"Never (Zero times)","isCorrect":true},{"id":"B","text":"Once during the $$1991$$ economic crisis","isCorrect":false},{"id":"C","text":"Twice ($$1965$$ and $$1971$$)","isCorrect":false},{"id":"D","text":"Three times","isCorrect":false}]',
        'A',
        'A Financial Emergency under Article $$360$$ has never been declared in India since the Constitution came into force in $$1950$$.'
    )
) AS v(id, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Indian Polity' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
