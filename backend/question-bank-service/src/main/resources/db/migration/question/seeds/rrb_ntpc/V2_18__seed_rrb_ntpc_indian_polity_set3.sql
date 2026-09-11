-- ============================================================
-- Seed Questions: RRB NTPC - Indian Polity and Governance (Set 3) (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Awareness / General Studies -> Topic: Indian Polity
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX ($$..$$)
-- UUID Range: a1120000-0000-0000-0000-000000000001 to a1120000-0000-0000-0000-000000000100
-- ============================================================

-- Step 1: Ensure topic and subtopics exist under 'General Awareness'
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, 'Indian Polity', 'Indian constitution, governance and political system'
FROM question_service.subject s
WHERE s.name IN ('General Awareness', 'General Studies') AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

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

-- Step 2: Insert 100 RRB NTPC Indian Polity Questions (Set 3)
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
    (
        'a1120000-0000-0000-0000-000000000001'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'On which historic date did the Constituent Assembly adopt the Constitution of India?',
        '[{"id": "A", "text": "November $$26, 1949$$", "isCorrect": true}, {"id": "B", "text": "January $$26, 1950$$", "isCorrect": false}, {"id": "C", "text": "August $$15, 1947$$", "isCorrect": false}, {"id": "D", "text": "December $$9, 1946$$", "isCorrect": false}]',
        'A',
        'The Constitution was adopted on November $$26, 1949$$ (celebrated as Constitution Day / Samvidhan Divas) and came into full effect on January $$26, 1950$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000002'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Who moved the historic ''Objectives Resolution'' in the Constituent Assembly on December 13, 1946?',
        '[{"id": "A", "text": "Pandit Jawaharlal Nehru", "isCorrect": true}, {"id": "B", "text": "Dr. B.R. Ambedkar", "isCorrect": false}, {"id": "C", "text": "Dr. Rajendra Prasad", "isCorrect": false}, {"id": "D", "text": "Sardar Vallabhbhai Patel", "isCorrect": false}]',
        'A',
        'Jawaharlal Nehru moved the Objectives Resolution on December $$13, 1946$$, outlining the philosophy and guiding principles of the Constitution.'
    ),
    (
        'a1120000-0000-0000-0000-000000000003'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Who was the Chairman of the Advisory Committee on Fundamental Rights, Minorities, and Tribal Areas in the Constituent Assembly?',
        '[{"id": "A", "text": "Sardar Vallabhbhai Patel", "isCorrect": true}, {"id": "B", "text": "J.B. Kripalani", "isCorrect": false}, {"id": "C", "text": "H.C. Mookerjee", "isCorrect": false}, {"id": "D", "text": "Gopinath Bardoloi", "isCorrect": false}]',
        'A',
        'Sardar Vallabhbhai Patel headed the Advisory Committee on Fundamental Rights, Minorities and Tribal and Excluded Areas.'
    ),
    (
        'a1120000-0000-0000-0000-000000000004'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The concept of ''Concurrent List'' in the Seventh Schedule was borrowed from the constitution of which country?',
        '[{"id": "A", "text": "Australia", "isCorrect": true}, {"id": "B", "text": "Canada", "isCorrect": false}, {"id": "C", "text": "Ireland", "isCorrect": false}, {"id": "D", "text": "South Africa", "isCorrect": false}]',
        'A',
        'The Concurrent List, joint sitting of Parliament, and freedom of trade and commerce were borrowed from the Australian Constitution.'
    ),
    (
        'a1120000-0000-0000-0000-000000000005'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The procedure for amending the Constitution of India (Article 368) was borrowed from:',
        '[{"id": "A", "text": "South Africa", "isCorrect": true}, {"id": "B", "text": "United Kingdom", "isCorrect": false}, {"id": "C", "text": "France", "isCorrect": false}, {"id": "D", "text": "Germany (Weimar Constitution)", "isCorrect": false}]',
        'A',
        'The procedure for amendment of the Constitution and election of members of Rajya Sabha were borrowed from South Africa.'
    ),
    (
        'a1120000-0000-0000-0000-000000000006'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'The ideals of ''Liberty, Equality, and Fraternity'' in our Preamble were inspired by which revolution?',
        '[{"id": "A", "text": "The French Revolution ($$1789$$)", "isCorrect": true}, {"id": "B", "text": "The Russian Revolution ($$1917$$)", "isCorrect": false}, {"id": "C", "text": "The American Revolution ($$1776$$)", "isCorrect": false}, {"id": "D", "text": "The Industrial Revolution", "isCorrect": false}]',
        'A',
        'The ideals of Liberty, Equality, and Fraternity were borrowed from the French Revolution ($$1789-1799$$).'
    ),
    (
        'a1120000-0000-0000-0000-000000000007'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'Who was appointed as the temporary interim President of the Constituent Assembly in its first session on December 9, 1946?',
        '[{"id": "A", "text": "Dr. Sachchidananda Sinha (oldest member, following French practice)", "isCorrect": true}, {"id": "B", "text": "Dr. Rajendra Prasad", "isCorrect": false}, {"id": "C", "text": "Dr. B.R. Ambedkar", "isCorrect": false}, {"id": "D", "text": "H.C. Mookerjee", "isCorrect": false}]',
        'A',
        'Following French practice of electing the oldest member, Dr. Sachchidananda Sinha was elected temporary Chairman on December 9, $$1946$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000008'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'How much total time did the Constituent Assembly take to complete the drafting of the Constitution?',
        '[{"id": "A", "text": "$$2\\text{ years, } 11\\text{ months, and } 18\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$3\\text{ years, } 1\\text{ month, and } 15\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$2\\text{ years, } 6\\text{ months, and } 10\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$3\\text{ years, } 5\\text{ months, and } 12\\text{ days}$$", "isCorrect": false}]',
        'A',
        'The Constituent Assembly held 11 sessions over $$2\text{ years, } 11\text{ months, and } 18\text{ days}$$ to finalize the Constitution.'
    ),
    (
        'a1120000-0000-0000-0000-000000000009'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'Which British proposal in $$1942$$ offered Dominion status to India after World War II, famously termed by Mahatma Gandhi as ''a post-dated cheque on a crashing bank''?',
        '[{"id": "A", "text": "Cripps Mission ($$1942$$)", "isCorrect": true}, {"id": "B", "text": "Cabinet Mission ($$1946$$)", "isCorrect": false}, {"id": "C", "text": "Wavell Plan ($$1945$$)", "isCorrect": false}, {"id": "D", "text": "August Offer ($$1940$$)", "isCorrect": false}]',
        'A',
        'Mahatma Gandhi criticized the proposals brought by Sir Stafford Cripps in $$1942$$ as a ''post-dated cheque on a failing bank''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000010'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The federal scheme, office of Governor, judiciary, public service commissions, and emergency provisions in our Constitution were majorly drawn from:',
        '[{"id": "A", "text": "Government of India Act $$1935$$", "isCorrect": true}, {"id": "B", "text": "Government of India Act $$1919$$", "isCorrect": false}, {"id": "C", "text": "US Constitution", "isCorrect": false}, {"id": "D", "text": "British Unwritten Constitution", "isCorrect": false}]',
        'A',
        'More than half of the provisions in the Indian Constitution are structurally derived from the Government of India Act $$1935$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000011'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'What is the exact opening phrase of the Preamble to the Constitution of India?',
        '[{"id": "A", "text": "''WE, THE PEOPLE OF INDIA...''", "isCorrect": true}, {"id": "B", "text": "''WE, THE CITIZENS OF BHARAT...''", "isCorrect": false}, {"id": "C", "text": "''THE GOVERNMENT OF INDIA...''", "isCorrect": false}, {"id": "D", "text": "''IN THE NAME OF GOD AND THE PEOPLE...''", "isCorrect": false}]',
        'A',
        'The Preamble begins with ''WE, THE PEOPLE OF INDIA, having solemnly resolved to constitute India into a SOVEREIGN SOCIALIST SECULAR DEMOCRATIC REPUBLIC...''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000012'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which date is explicitly mentioned in the text of the Preamble as the date of adoption of the Constitution?',
        '[{"id": "A", "text": "$$26\\text{th day of November, } 1949$$", "isCorrect": true}, {"id": "B", "text": "$$26\\text{th day of January, } 1950$$", "isCorrect": false}, {"id": "C", "text": "$$15\\text{th day of August, } 1947$$", "isCorrect": false}, {"id": "D", "text": "$$9\\text{th day of December, } 1946$$", "isCorrect": false}]',
        'A',
        'The concluding line of the Preamble states: ''...this twenty-sixth day of November, 1949, do HEREBY ADOPT, ENACT AND GIVE TO OURSELVES THIS CONSTITUTION''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000013'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'In the Berubari Union case ($$1960$$), what was the initial view of the Supreme Court regarding the Preamble?',
        '[{"id": "A", "text": "The Preamble is NOT a part of the Constitution", "isCorrect": true}, {"id": "B", "text": "The Preamble is supreme over Fundamental Rights", "isCorrect": false}, {"id": "C", "text": "The Preamble cannot be amended under any circumstance", "isCorrect": false}, {"id": "D", "text": "The Preamble is enforceable in courts of law", "isCorrect": false}]',
        'A',
        'In $$1960$$, the Supreme Court in the Berubari reference opined that the Preamble is not a part of the Constitution; this was later overruled in $$1973$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000014'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$2$$ of the Constitution, who is empowered to admit new States into the Union or establish new States?',
        '[{"id": "A", "text": "The Parliament of India by law", "isCorrect": true}, {"id": "B", "text": "The President of India by executive decree", "isCorrect": false}, {"id": "C", "text": "The Supreme Court of India", "isCorrect": false}, {"id": "D", "text": "The Inter-State Council", "isCorrect": false}]',
        'A',
        'Article $$2$$ empowers the Parliament by law to admit into the Union, or establish, new States on such terms and conditions as it thinks fit.'
    ),
    (
        'a1120000-0000-0000-0000-000000000015'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Which Constitutional Amendment converted Sikkim from an ''Associate State'' into a full-fledged 22nd State of the Indian Union in $$1975$$?',
        '[{"id": "A", "text": "$$36\\text{th}$$ Constitutional Amendment Act $$1975$$", "isCorrect": true}, {"id": "B", "text": "$$35\\text{th}$$ Constitutional Amendment Act $$1974$$", "isCorrect": false}, {"id": "C", "text": "$$42\\text{nd}$$ Constitutional Amendment Act $$1976$$", "isCorrect": false}, {"id": "D", "text": "$$44\\text{th}$$ Constitutional Amendment Act $$1978$$", "isCorrect": false}]',
        'A',
        'The $$35\text{th}$$ Amendment ($$1974$$) introduced Sikkim as an associate state, and the $$36\text{th}$$ Amendment ($$1975$$) admitted Sikkim as the $$22\text{nd}$$ full State.'
    ),
    (
        'a1120000-0000-0000-0000-000000000016'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which Union Territory was granted special status and designated as the ''National Capital Territory'' ($$\text{NCT}$$) of Delhi by the $$69\text{th}$$ Amendment in $$1991$$?',
        '[{"id": "A", "text": "Delhi (Article $$239\\text{AA}$$)", "isCorrect": true}, {"id": "B", "text": "Puducherry", "isCorrect": false}, {"id": "C", "text": "Chandigarh", "isCorrect": false}, {"id": "D", "text": "Dadra and Nagar Haveli", "isCorrect": false}]',
        'A',
        'The $$69\text{th}$$ Constitutional Amendment Act of $$1991$$ designated Union Territory of Delhi as the National Capital Territory of Delhi with a Legislative Assembly.'
    ),
    (
        'a1120000-0000-0000-0000-000000000017'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'REMEMBER',
        'Which Article deals with citizenship rights of certain migrants to Pakistan who returned back to India?',
        '[{"id": "A", "text": "Article $$7$$", "isCorrect": true}, {"id": "B", "text": "Article $$6$$", "isCorrect": false}, {"id": "C", "text": "Article $$8$$", "isCorrect": false}, {"id": "D", "text": "Article $$9$$", "isCorrect": false}]',
        'A',
        'Article $$7$$ deals with rights of citizenship of certain migrants to Pakistan who returned under a permit for resettlement.'
    ),
    (
        'a1120000-0000-0000-0000-000000000018'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'What are the three ways by which a person can lose Indian citizenship under the Citizenship Act of $$1955$$?',
        '[{"id": "A", "text": "Renunciation, Termination, and Deprivation", "isCorrect": true}, {"id": "B", "text": "Imprisonment, Default, and Deportation", "isCorrect": false}, {"id": "C", "text": "Exile, Disqualification, and Extradition", "isCorrect": false}, {"id": "D", "text": "Bankruptcy, Treason, and Incarceration", "isCorrect": false}]',
        'A',
        'The Citizenship Act of $$1955$$ prescribes three modes of loss of citizenship: Renunciation (voluntary), Termination (by operation of law), and Deprivation (compulsory termination by Government).'
    ),
    (
        'a1120000-0000-0000-0000-000000000019'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which Union Ministry is the nodal agency for citizenship and immigration matters in India?',
        '[{"id": "A", "text": "Ministry of Home Affairs", "isCorrect": true}, {"id": "B", "text": "Ministry of External Affairs", "isCorrect": false}, {"id": "C", "text": "Ministry of Law and Justice", "isCorrect": false}, {"id": "D", "text": "Prime Minister''s Office", "isCorrect": false}]',
        'A',
        'The Ministry of Home Affairs (MHA) administers the Citizenship Act of $$1955$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000020'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Which Committee was formed in $$1948$$ by the President of the Constituent Assembly to examine the linguistic reorganization of states, and recommended administration convenience over language?',
        '[{"id": "A", "text": "S.K. Dhar Commission", "isCorrect": true}, {"id": "B", "text": "JVP Committee", "isCorrect": false}, {"id": "C", "text": "Fazl Ali Commission", "isCorrect": false}, {"id": "D", "text": "Sarkaria Commission", "isCorrect": false}]',
        'A',
        'The Linguistic Provinces Commission headed by Justice S.K. Dhar ($$1948$$) recommended reorganization of states on administrative convenience rather than linguistic basis.'
    ),
    (
        'a1120000-0000-0000-0000-000000000021'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution defines the term ''State'' for the purposes of Part III (Fundamental Rights)?',
        '[{"id": "A", "text": "Article $$12$$", "isCorrect": true}, {"id": "B", "text": "Article $$13$$", "isCorrect": false}, {"id": "C", "text": "Article $$14$$", "isCorrect": false}, {"id": "D", "text": "Article $$36$$", "isCorrect": false}]',
        'A',
        'Article $$12$$ defines the ''State'' to include the Government and Parliament of India, Government and Legislatures of States, and all local and other authorities.'
    ),
    (
        'a1120000-0000-0000-0000-000000000022'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Which Article declares that any law inconsistent with or in derogation of Fundamental Rights shall be void (Doctrine of Judicial Review)?',
        '[{"id": "A", "text": "Article $$13$$", "isCorrect": true}, {"id": "B", "text": "Article $$14$$", "isCorrect": false}, {"id": "C", "text": "Article $$32$$", "isCorrect": false}, {"id": "D", "text": "Article $$226$$", "isCorrect": false}]',
        'A',
        'Article $$13$$ provides teeth to Fundamental Rights by declaring laws violating them void, establishing the foundation of judicial review in India.'
    ),
    (
        'a1120000-0000-0000-0000-000000000023'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Equality before the law and equal protection of the laws is guaranteed to all persons under:',
        '[{"id": "A", "text": "Article $$14$$", "isCorrect": true}, {"id": "B", "text": "Article $$15$$", "isCorrect": false}, {"id": "C", "text": "Article $$16$$", "isCorrect": false}, {"id": "D", "text": "Article $$19$$", "isCorrect": false}]',
        'A',
        'Article $$14$$ states that the State shall not deny to any person equality before the law or the equal protection of the laws within India.'
    ),
    (
        'a1120000-0000-0000-0000-000000000024'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'On which five specific grounds does Article $$15(1)$$ prohibit discrimination by the State against citizens?',
        '[{"id": "A", "text": "Religion, Race, Caste, Sex, or Place of birth", "isCorrect": true}, {"id": "B", "text": "Religion, Language, Residence, Caste, or Wealth", "isCorrect": false}, {"id": "C", "text": "Race, Caste, Profession, Domicile, or Creed", "isCorrect": false}, {"id": "D", "text": "Sex, Income, Education, Descent, or Religion", "isCorrect": false}]',
        'A',
        'Article $$15(1)$$ prohibits discrimination against any citizen on grounds only of religion, race, caste, sex, place of birth or any of them.'
    ),
    (
        'a1120000-0000-0000-0000-000000000025'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Equality of opportunity in matters of public employment is guaranteed under:',
        '[{"id": "A", "text": "Article $$16$$", "isCorrect": true}, {"id": "B", "text": "Article $$14$$", "isCorrect": false}, {"id": "C", "text": "Article $$19$$", "isCorrect": false}, {"id": "D", "text": "Article $$21$$", "isCorrect": false}]',
        'A',
        'Article $$16$$ guarantees equality of opportunity for all citizens in matters relating to employment or appointment to any office under the State.'
    ),
    (
        'a1120000-0000-0000-0000-000000000026'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'In which landmark $$9\text{-judge}$$ bench decision did the Supreme Court cap total caste reservations at $$50\%$$?',
        '[{"id": "A", "text": "Indra Sawhney v. Union of India ($$1992$$)", "isCorrect": true}, {"id": "B", "text": "Champakam Dorairajan Case ($$1951$$)", "isCorrect": false}, {"id": "C", "text": "M. Nagaraj v. Union of India ($$2006$$)", "isCorrect": false}, {"id": "D", "text": "Ashoka Kumar Thakur v. Union of India ($$2008$$)", "isCorrect": false}]',
        'A',
        'In the historic Mandal case (Indra Sawhney, $$1992$$), the Supreme Court upheld 27% OBC reservation while imposing a strict 50% upper ceiling.'
    ),
    (
        'a1120000-0000-0000-0000-000000000027'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'The ''Right to Privacy'' was declared an intrinsic part of the Right to Life and Personal Liberty (Article 21) in:',
        '[{"id": "A", "text": "Justice K.S. Puttaswamy v. Union of India ($$2017$$)", "isCorrect": true}, {"id": "B", "text": "Maneka Gandhi v. Union of India ($$1978$$)", "isCorrect": false}, {"id": "C", "text": "A.K. Gopalan v. State of Madras ($$1950$$)", "isCorrect": false}, {"id": "D", "text": "Navtej Singh Johar v. Union of India ($$2018$$)", "isCorrect": false}]',
        'A',
        'A unanimous 9-judge bench in Puttaswamy ($$2017$$) held that privacy is a fundamental right protected under Article $$21$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000028'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Freedom of conscience and free profession, practice, and propagation of religion is guaranteed under:',
        '[{"id": "A", "text": "Article $$25$$", "isCorrect": true}, {"id": "B", "text": "Article $$26$$", "isCorrect": false}, {"id": "C", "text": "Article $$27$$", "isCorrect": false}, {"id": "D", "text": "Article $$28$$", "isCorrect": false}]',
        'A',
        'Article $$25$$ guarantees to all persons freedom of conscience and the right to freely profess, practice and propagate religion.'
    ),
    (
        'a1120000-0000-0000-0000-000000000029'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which writ is termed the ''Bulwark of Individual Liberty'' against arbitrary and illegal detention?',
        '[{"id": "A", "text": "Habeas Corpus", "isCorrect": true}, {"id": "B", "text": "Mandamus", "isCorrect": false}, {"id": "C", "text": "Certiorari", "isCorrect": false}, {"id": "D", "text": "Quo-Warranto", "isCorrect": false}]',
        'A',
        'Habeas Corpus (literally ''to have the body of'') protects citizens from unlawful detention by demanding the physical production of the person before the court.'
    ),
    (
        'a1120000-0000-0000-0000-000000000030'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$226$$, the writ jurisdiction of a High Court is:',
        '[{"id": "A", "text": "Broader than that of the Supreme Court, as it covers both Fundamental Rights and ordinary legal rights", "isCorrect": true}, {"id": "B", "text": "Narrower than the Supreme Court, restricted only to statutory violations", "isCorrect": false}, {"id": "C", "text": "Identical in every respect to Article 32", "isCorrect": false}, {"id": "D", "text": "Advisory and non-binding on the executive", "isCorrect": false}]',
        'A',
        'Article 32 can only be invoked for Fundamental Rights, whereas Article 226 can be invoked for Fundamental Rights and ''for any other purpose'' (ordinary legal rights).'
    ),
    (
        'a1120000-0000-0000-0000-000000000031'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Which Article directs the State to secure equal justice and free legal aid to poor and weaker sections?',
        '[{"id": "A", "text": "Article $$39\\text{A}$$", "isCorrect": true}, {"id": "B", "text": "Article $$38$$", "isCorrect": false}, {"id": "C", "text": "Article $$41$$", "isCorrect": false}, {"id": "D", "text": "Article $$43$$", "isCorrect": false}]',
        'A',
        'Article $$39\text{A}$$ was inserted by the $$42\text{nd}$$ Amendment ($$1976$$) to provide free legal aid to the poor, resulting in the Legal Services Authorities Act (NALSA).'
    ),
    (
        'a1120000-0000-0000-0000-000000000032'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'REMEMBER',
        'Provision for just and humane conditions of work and maternity relief is enshrined in:',
        '[{"id": "A", "text": "Article $$42$$", "isCorrect": true}, {"id": "B", "text": "Article $$41$$", "isCorrect": false}, {"id": "C", "text": "Article $$43$$", "isCorrect": false}, {"id": "D", "text": "Article $$44$$", "isCorrect": false}]',
        'A',
        'Article $$42$$ directs the State to make provision for securing just and humane conditions of work and for maternity relief.'
    ),
    (
        'a1120000-0000-0000-0000-000000000033'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Article $$43\text{A}$$, inserted by the $$42\text{nd}$$ Amendment, provides for:',
        '[{"id": "A", "text": "Participation of workers in the management of industries", "isCorrect": true}, {"id": "B", "text": "Promotion of cooperative societies", "isCorrect": false}, {"id": "C", "text": "Living wages for cottage weavers", "isCorrect": false}, {"id": "D", "text": "Universal employment guarantee", "isCorrect": false}]',
        'A',
        'Article $$43\text{A}$$ directs the State to take steps to secure the participation of workers in the management of undertakings and establishments.'
    ),
    (
        'a1120000-0000-0000-0000-000000000034'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Which Article directs the State to protect and improve the environment and safeguard forests and wildlife?',
        '[{"id": "A", "text": "Article $$48\\text{A}$$", "isCorrect": true}, {"id": "B", "text": "Article $$47$$", "isCorrect": false}, {"id": "C", "text": "Article $$49$$", "isCorrect": false}, {"id": "D", "text": "Article $$50$$", "isCorrect": false}]',
        'A',
        'Article $$48\text{A}$$ was added by the $$42\text{nd}$$ Amendment ($$1976$$) directing the State to protect the environment, forests, and wildlife.'
    ),
    (
        'a1120000-0000-0000-0000-000000000035'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'Article $$43\text{B}$$ relating to the promotion of Cooperative Societies was inserted by which Constitutional Amendment?',
        '[{"id": "A", "text": "$$97\\text{th}$$ Constitutional Amendment Act $$2011$$", "isCorrect": true}, {"id": "B", "text": "$$86\\text{th}$$ Constitutional Amendment Act $$2002$$", "isCorrect": false}, {"id": "C", "text": "$$91\\text{st}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}, {"id": "D", "text": "$$73\\text{rd}$$ Constitutional Amendment Act $$1992$$", "isCorrect": false}]',
        'A',
        'The $$97\text{th}$$ Amendment Act of $$2011$$ added Article $$43\text{B}$$ directing the State to promote voluntary formation and autonomous functioning of cooperative societies.'
    ),
    (
        'a1120000-0000-0000-0000-000000000036'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Which Article obligates the State to protect monuments, places, and objects of artistic or historic interest declared to be of national importance?',
        '[{"id": "A", "text": "Article $$49$$", "isCorrect": true}, {"id": "B", "text": "Article $$48$$", "isCorrect": false}, {"id": "C", "text": "Article $$50$$", "isCorrect": false}, {"id": "D", "text": "Article $$51$$", "isCorrect": false}]',
        'A',
        'Article $$49$$ mandates that the State shall protect monuments, places and objects of national historic importance from spoliation, destruction or removal.'
    ),
    (
        'a1120000-0000-0000-0000-000000000037'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution contains the Fundamental Duties?',
        '[{"id": "A", "text": "Article $$51\\text{A}$$ (Part IV-A)", "isCorrect": true}, {"id": "B", "text": "Article $$35\\text{A}$$ (Part III)", "isCorrect": false}, {"id": "C", "text": "Article $$21\\text{A}$$ (Part III)", "isCorrect": false}, {"id": "D", "text": "Article $$31\\text{C}$$ (Part III)", "isCorrect": false}]',
        'A',
        'Part IV-A consists of a single article, Article $$51\text{A}$$, specifying the fundamental duties of every Indian citizen.'
    ),
    (
        'a1120000-0000-0000-0000-000000000038'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'To abide by the Constitution and respect its ideals and institutions, the National Flag, and the National Anthem is listed under which clause?',
        '[{"id": "A", "text": "Article $$51\\text{A}(a)$$", "isCorrect": true}, {"id": "B", "text": "Article $$51\\text{A}(c)$$", "isCorrect": false}, {"id": "C", "text": "Article $$51\\text{A}(e)$$", "isCorrect": false}, {"id": "D", "text": "Article $$51\\text{A}(g)$$", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(a)$$ is the very first Fundamental Duty: to abide by the Constitution and respect the National Flag and National Anthem.'
    ),
    (
        'a1120000-0000-0000-0000-000000000039'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'In which landmark judgment did the Supreme Court rule that Indian Constitution is founded on the bedrock of the balance between Part III (Fundamental Rights) and Part IV (DPSPs)?',
        '[{"id": "A", "text": "Minerva Mills v. Union of India ($$1980$$)", "isCorrect": true}, {"id": "B", "text": "Golaknath v. State of Punjab ($$1967$$)", "isCorrect": false}, {"id": "C", "text": "Maneka Gandhi v. Union of India ($$1978$$)", "isCorrect": false}, {"id": "D", "text": "Shankari Prasad v. Union of India ($$1951$$)", "isCorrect": false}]',
        'A',
        'In Minerva Mills ($$1980$$), the Supreme Court observed that Part III and Part IV together constitute the core of commitment to social revolution and give each other balance.'
    ),
    (
        'a1120000-0000-0000-0000-000000000040'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Under Article $$47$$, what is one of the primary duties of the State regarding public health?',
        '[{"id": "A", "text": "Prohibition of consumption of intoxicating drinks and drugs injurious to health", "isCorrect": true}, {"id": "B", "text": "Free health insurance for all central employees", "isCorrect": false}, {"id": "C", "text": "Building private multi-specialty hospitals", "isCorrect": false}, {"id": "D", "text": "Abolition of traditional medicine systems", "isCorrect": false}]',
        'A',
        'Article $$47$$ directs the State to raise nutrition levels, improve public health, and prohibit intoxicating drinks and drugs except for medicinal purposes.'
    ),
    (
        'a1120000-0000-0000-0000-000000000041'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who was the only President of India elected unopposed in $$1977$$?',
        '[{"id": "A", "text": "Neelam Sanjiva Reddy", "isCorrect": true}, {"id": "B", "text": "Dr. Rajendra Prasad", "isCorrect": false}, {"id": "C", "text": "Dr. Zakir Husain", "isCorrect": false}, {"id": "D", "text": "Fakhruddin Ali Ahmed", "isCorrect": false}]',
        'A',
        'Neelam Sanjiva Reddy was unanimously elected unopposed as the sixth President of India in $$1977$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000042'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who is the supreme commander of the defence forces of the Union of India?',
        '[{"id": "A", "text": "The President of India (Article $$53(2)$$)", "isCorrect": true}, {"id": "B", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "C", "text": "The Chief of Defence Staff (CDS)", "isCorrect": false}, {"id": "D", "text": "The Union Minister of Defence", "isCorrect": false}]',
        'A',
        'Article $$53(2)$$ vests supreme command of the Defence Forces of the Union in the President, exercised in accordance with law.'
    ),
    (
        'a1120000-0000-0000-0000-000000000043'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'In case both the offices of President and Vice-President fall vacant simultaneously, who discharges the functions of the President?',
        '[{"id": "A", "text": "The Chief Justice of India (or senior-most Supreme Court Judge)", "isCorrect": true}, {"id": "B", "text": "The Speaker of the Lok Sabha", "isCorrect": false}, {"id": "C", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "D", "text": "The Attorney General for India", "isCorrect": false}]',
        'A',
        'Under the President (Discharge of Functions) Act $$1969$$, the Chief Justice of India (first executed by Justice M. Hidayatullah in 1969) discharges Presidential duties.'
    ),
    (
        'a1120000-0000-0000-0000-000000000044'::uuid,
        'Union Executive',
        'MEDIUM',
        'REMEMBER',
        'What is the security deposit required to be deposited by a candidate contesting the election for the President of India?',
        '[{"id": "A", "text": "$$\\text{Rs. } 15,000$$ in the Reserve Bank of India", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 25,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 10,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 50,000$$", "isCorrect": false}]',
        'A',
        'A presidential candidate must deposit Rs. $$15,000$$ with the Reserve Bank of India, forfeited if the candidate fails to secure 1/6th of total valid votes.'
    ),
    (
        'a1120000-0000-0000-0000-000000000045'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'Who investigates and decides all doubts and disputes arising out of the election of the President or Vice-President?',
        '[{"id": "A", "text": "The Supreme Court of India, whose decision is final (Article $$71$$)", "isCorrect": true}, {"id": "B", "text": "The Election Commission of India", "isCorrect": false}, {"id": "C", "text": "A special joint parliamentary committee", "isCorrect": false}, {"id": "D", "text": "The High Court of Delhi", "isCorrect": false}]',
        'A',
        'Under Article $$71(1)$$, all election disputes concerning President or Vice-President are inquired into and decided exclusively by the Supreme Court.'
    ),
    (
        'a1120000-0000-0000-0000-000000000046'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Can the President return a Money Bill to Parliament for reconsideration?',
        '[{"id": "A", "text": "No, the President can only give assent or withhold assent, but cannot return it", "isCorrect": true}, {"id": "B", "text": "Yes, once with recommendations", "isCorrect": false}, {"id": "C", "text": "Yes, if recommended by the Finance Minister", "isCorrect": false}, {"id": "D", "text": "Yes, if passed without a quorum", "isCorrect": false}]',
        'A',
        'Under Article $$111$$, the President cannot return a Money Bill for reconsideration, since it is introduced in the Lok Sabha with his prior recommendation.'
    ),
    (
        'a1120000-0000-0000-0000-000000000047'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who was the first Vice-President of independent India?',
        '[{"id": "A", "text": "Dr. Sarvepalli Radhakrishnan", "isCorrect": true}, {"id": "B", "text": "Dr. Zakir Husain", "isCorrect": false}, {"id": "C", "text": "V.V. Giri", "isCorrect": false}, {"id": "D", "text": "Gopal Swarup Pathak", "isCorrect": false}]',
        'A',
        'Dr. Sarvepalli Radhakrishnan served as the first Vice-President from $$1952$$ to $$1962$$ for two consecutive terms.'
    ),
    (
        'a1120000-0000-0000-0000-000000000048'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'To remove the Vice-President of India, where must the resolution be initiated?',
        '[{"id": "A", "text": "Only in the Rajya Sabha (Council of States)", "isCorrect": true}, {"id": "B", "text": "In either House of Parliament", "isCorrect": false}, {"id": "C", "text": "Only in the Lok Sabha", "isCorrect": false}, {"id": "D", "text": "In a Joint Sitting of both Houses", "isCorrect": false}]',
        'A',
        'Under Article $$67(b)$$, a resolution to remove the Vice-President can only be introduced in the Rajya Sabha with 14 days advance notice.'
    ),
    (
        'a1120000-0000-0000-0000-000000000049'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'What happens to the Union Council of Ministers if the Prime Minister resigns or passes away?',
        '[{"id": "A", "text": "The entire Council of Ministers automatically dissolves", "isCorrect": true}, {"id": "B", "text": "The senior-most cabinet minister automatically becomes Prime Minister", "isCorrect": false}, {"id": "C", "text": "The Council of Ministers continues under the Home Minister", "isCorrect": false}, {"id": "D", "text": "The President runs the government directly without ministers", "isCorrect": false}]',
        'A',
        'Since the Prime Minister is the keystone of the Cabinet arch, his death or resignation automatically brings about the dissolution of the Council of Ministers.'
    ),
    (
        'a1120000-0000-0000-0000-000000000050'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'What is the maximum limit on the total number of ministers, including the Prime Minister, in the Union Council of Ministers?',
        '[{"id": "A", "text": "$$15\\%$$ of the total number of members of the Lok Sabha", "isCorrect": true}, {"id": "B", "text": "$$10\\%$$ of the total number of members of the Lok Sabha", "isCorrect": false}, {"id": "C", "text": "$$20\\%$$ of the total number of members of Parliament", "isCorrect": false}, {"id": "D", "text": "$$50\\text{ ministers}$$ maximum in absolute count", "isCorrect": false}]',
        'A',
        'Article $$75(1\text{A})$$, inserted by the $$91\text{st}$$ Amendment Act $$2003$$, caps the Union Council of Ministers at $$15\%$$ of the total strength of the Lok Sabha.'
    ),
    (
        'a1120000-0000-0000-0000-000000000051'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'The nomination of Anglo-Indian members to the Lok Sabha and State Assemblies was discontinued by which Constitutional Amendment?',
        '[{"id": "A", "text": "$$104\\text{th}$$ Constitutional Amendment Act $$2019$$", "isCorrect": true}, {"id": "B", "text": "$$101\\text{st}$$ Constitutional Amendment Act $$2016$$", "isCorrect": false}, {"id": "C", "text": "$$103\\text{rd}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}, {"id": "D", "text": "$$99\\text{th}$$ Constitutional Amendment Act $$2014$$", "isCorrect": false}]',
        'A',
        'The $$104\text{th}$$ Amendment Act ($$2019$$) extended reservations for SCs/STs in legislatures for another 10 years but did not extend the nomination of Anglo-Indians.'
    ),
    (
        'a1120000-0000-0000-0000-000000000052'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'Who was the first Speaker of the Lok Sabha in independent India?',
        '[{"id": "A", "text": "G.V. Mavalankar (Ganesh Vasudev Mavalankar)", "isCorrect": true}, {"id": "B", "text": "M. Ananthasayanam Ayyangar", "isCorrect": false}, {"id": "C", "text": "Hukam Singh", "isCorrect": false}, {"id": "D", "text": "K.S. Hegde", "isCorrect": false}]',
        'A',
        'G.V. Mavalankar served as the first Speaker of the Lok Sabha from $$1952$$ until his death in $$1956$$, affectionately called ''Father of the Lok Sabha''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000053'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'Who was the first woman Speaker of the Lok Sabha?',
        '[{"id": "A", "text": "Meira Kumar ($$2009-2014$$)", "isCorrect": true}, {"id": "B", "text": "Sumitra Mahajan", "isCorrect": false}, {"id": "C", "text": "Najma Heptulla", "isCorrect": false}, {"id": "D", "text": "Pratibha Patil", "isCorrect": false}]',
        'A',
        'Meira Kumar was elected the first woman Speaker of the Lok Sabha in June $$2009$$ during the 15th Lok Sabha.'
    ),
    (
        'a1120000-0000-0000-0000-000000000054'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'What is ''Zero Hour'' in Indian parliamentary parlance?',
        '[{"id": "A", "text": "An informal period immediately following Question Hour where MPs raise matters of urgent public importance without prior notice", "isCorrect": true}, {"id": "B", "text": "The hour when budget proposals are voted upon", "isCorrect": false}, {"id": "C", "text": "The midnight session called on Independence Day", "isCorrect": false}, {"id": "D", "text": "The recess period between parliamentary sessions", "isCorrect": false}]',
        'A',
        'Zero Hour (started in 1962) is an Indian parliamentary innovation starting at 12:00 noon where members raise pressing matters without prior 10-day notice.'
    ),
    (
        'a1120000-0000-0000-0000-000000000055'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'Who is the Chairman of the Public Accounts Committee (PAC) by parliamentary convention since $$1967$$?',
        '[{"id": "A", "text": "A leader from the Opposition in the Lok Sabha", "isCorrect": true}, {"id": "B", "text": "The Union Finance Minister", "isCorrect": false}, {"id": "C", "text": "The Speaker of the Lok Sabha", "isCorrect": false}, {"id": "D", "text": "The senior-most member of the ruling party", "isCorrect": false}]',
        'A',
        'Since $$1967$$, by established convention, the Chairman of the PAC is selected from the Opposition by the Speaker.'
    ),
    (
        'a1120000-0000-0000-0000-000000000056'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'How many total members are there in the Public Accounts Committee (PAC)?',
        '[{"id": "A", "text": "$$22\\text{ members (15 from Lok Sabha and 7 from Rajya Sabha)}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ members (all from Lok Sabha)}$$", "isCorrect": false}, {"id": "C", "text": "$$15\\text{ members (10 from Lok Sabha and 5 from Rajya Sabha)}$$", "isCorrect": false}, {"id": "D", "text": "$$25\\text{ members}$$", "isCorrect": false}]',
        'A',
        'The PAC consists of $$22$$ members: $$15$$ elected from Lok Sabha and $$7$$ from Rajya Sabha for a term of one year.'
    ),
    (
        'a1120000-0000-0000-0000-000000000057'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'Under Article $$112$$, what is the constitutional term used for the ''Union Budget''?',
        '[{"id": "A", "text": "Annual Financial Statement", "isCorrect": true}, {"id": "B", "text": "National Income Statement", "isCorrect": false}, {"id": "C", "text": "Consolidated Fund Expenditure Statement", "isCorrect": false}, {"id": "D", "text": "Appropriation Bill", "isCorrect": false}]',
        'A',
        'The word ''Budget'' is not used in the Constitution; Article $$112$$ refers to it as the ''Annual Financial Statement''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000058'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'What is the maximum permissible gap between two consecutive sessions of Parliament under Article $$85(1)$$?',
        '[{"id": "A", "text": "$$6\\text{ months}$$", "isCorrect": true}, {"id": "B", "text": "$$3\\text{ months}$$", "isCorrect": false}, {"id": "C", "text": "$$4\\text{ months}$$", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ year}$$", "isCorrect": false}]',
        'A',
        'Article $$85(1)$$ mandates that six months shall not intervene between the last sitting in one session and the date appointed for its first sitting in the next session.'
    ),
    (
        'a1120000-0000-0000-0000-000000000059'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'The Consolidated Fund of India is established under which Article?',
        '[{"id": "A", "text": "Article $$266(1)$$", "isCorrect": true}, {"id": "B", "text": "Article $$267$$", "isCorrect": false}, {"id": "C", "text": "Article $$280$$", "isCorrect": false}, {"id": "D", "text": "Article $$110$$", "isCorrect": false}]',
        'A',
        'Article $$266(1)$$ creates the Consolidated Fund of India into which all revenues, loans, and receipts of the Government are credited.'
    ),
    (
        'a1120000-0000-0000-0000-000000000060'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'The Contingency Fund of India, placed at the disposal of the President for unforeseen expenditures, is established under:',
        '[{"id": "A", "text": "Article $$267(1)$$", "isCorrect": true}, {"id": "B", "text": "Article $$266(2)$$", "isCorrect": false}, {"id": "C", "text": "Article $$280$$", "isCorrect": false}, {"id": "D", "text": "Article $$112$$", "isCorrect": false}]',
        'A',
        'Article $$267$$ establishes the Contingency Fund of India, operated by the Finance Secretary on behalf of the President.'
    ),
    (
        'a1120000-0000-0000-0000-000000000061'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'How many Indian States currently have a bicameral legislature (Vidhan Sabha and Vidhan Parishad)?',
        '[{"id": "A", "text": "$$6\\text{ States (Andhra Pradesh, Bihar, Karnataka, Maharashtra, Telangana, Uttar Pradesh)}$$", "isCorrect": true}, {"id": "B", "text": "$$7\\text{ States}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ States}$$", "isCorrect": false}, {"id": "D", "text": "$$9\\text{ States}$$", "isCorrect": false}]',
        'A',
        'Currently, 6 States possess bicameral legislatures: Uttar Pradesh, Bihar, Maharashtra, Karnataka, Andhra Pradesh, and Telangana.'
    ),
    (
        'a1120000-0000-0000-0000-000000000062'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'What is the maximum limit on the membership of a State Legislative Council (Vidhan Parishad)?',
        '[{"id": "A", "text": "One-third ($$\\frac{1}{3}\\text{rd}$$) of the total membership of the State Legislative Assembly", "isCorrect": true}, {"id": "B", "text": "Half ($$\\frac{1}{2}$$) of the State Legislative Assembly", "isCorrect": false}, {"id": "C", "text": "One-fourth ($$\\frac{1}{4}\\text{th}$$) of the State Legislative Assembly", "isCorrect": false}, {"id": "D", "text": "Fixed at $$100\\text{ members}$$ for all States", "isCorrect": false}]',
        'A',
        'Under Article $$171(1)$$, the total membership of the Legislative Council shall not exceed one-third of the total assembly strength, but cannot be less than 40.'
    ),
    (
        'a1120000-0000-0000-0000-000000000063'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'How many members does the Governor nominate to the State Legislative Council from fields of literature, science, art, cooperative movement, and social service?',
        '[{"id": "A", "text": "One-sixth ($$\\frac{1}{6}\\text{th}$$) of the total members", "isCorrect": true}, {"id": "B", "text": "One-twelfth ($$\\frac{1}{12}\\text{th}$$) of the total members", "isCorrect": false}, {"id": "C", "text": "One-third ($$\\frac{1}{3}\\text{rd}$$) of the total members", "isCorrect": false}, {"id": "D", "text": "Ten members in absolute count", "isCorrect": false}]',
        'A',
        'Under Article $$171(3)(e)$$, 1/6th of the members of the Legislative Council are nominated by the Governor.'
    ),
    (
        'a1120000-0000-0000-0000-000000000064'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Who was the first woman Governor of a State in independent India?',
        '[{"id": "A", "text": "Sarojini Naidu (Governor of Uttar Pradesh)", "isCorrect": true}, {"id": "B", "text": "Sucheta Kripalani", "isCorrect": false}, {"id": "C", "text": "Vijayalakshmi Pandit", "isCorrect": false}, {"id": "D", "text": "Padmaja Naidu", "isCorrect": false}]',
        'A',
        'Sarojini Naidu served as the Governor of the United Provinces (Uttar Pradesh) from August $$15, 1947$$ until her death in March $$1949$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000065'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Who was the first woman Chief Minister of an Indian State?',
        '[{"id": "A", "text": "Sucheta Kripalani (Chief Minister of Uttar Pradesh, $$1963$$)", "isCorrect": true}, {"id": "B", "text": "Nandini Satpathy", "isCorrect": false}, {"id": "C", "text": "Jayalalithaa", "isCorrect": false}, {"id": "D", "text": "Mayawati", "isCorrect": false}]',
        'A',
        'Sucheta Kripalani became the Chief Minister of Uttar Pradesh in October $$1963$$, making her the first woman CM in India.'
    ),
    (
        'a1120000-0000-0000-0000-000000000066'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'Under Article $$163(1)$$, in which matters is the Governor NOT required to act on the aid and advice of the Council of Ministers?',
        '[{"id": "A", "text": "Matters in which he is by or under the Constitution required to act in his discretion", "isCorrect": true}, {"id": "B", "text": "All fiscal and budgetary decisions", "isCorrect": false}, {"id": "C", "text": "All civil service transfers and postings", "isCorrect": false}, {"id": "D", "text": "Pardoning convicted felons", "isCorrect": false}]',
        'A',
        'Article $$163(1)$$ provides that the Council of Ministers aids and advises the Governor, except where he is required to exercise his functions in his discretion.'
    ),
    (
        'a1120000-0000-0000-0000-000000000067'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'For how long can the State Legislative Council delay an ordinary bill passed by the Legislative Assembly in the first instance?',
        '[{"id": "A", "text": "$$3\\text{ months}$$ in the first instance", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ months}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ month}$$", "isCorrect": false}]',
        'A',
        'The Legislative Council can delay an ordinary bill for 3 months in the first instance, and 1 month in the second instance (total maximum delay of 4 months).'
    ),
    (
        'a1120000-0000-0000-0000-000000000068'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$167$$, what is the constitutional duty of the Chief Minister toward the Governor?',
        '[{"id": "A", "text": "To communicate all decisions of the Council of Ministers and furnish information relating to administration", "isCorrect": true}, {"id": "B", "text": "To obtain prior sanction from the Governor before tabling any bill", "isCorrect": false}, {"id": "C", "text": "To submit daily administrative police reports", "isCorrect": false}, {"id": "D", "text": "To present the state budget in person", "isCorrect": false}]',
        'A',
        'Article $$167$$ defines the duties of the Chief Minister in furnishing information relating to the administration of the affairs of the State to the Governor.'
    ),
    (
        'a1120000-0000-0000-0000-000000000069'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'Under the $$7\text{th}$$ Constitutional Amendment Act of $$1956$$, can the same person be appointed as Governor of two or more States?',
        '[{"id": "A", "text": "Yes, under the proviso added to Article $$153$$", "isCorrect": true}, {"id": "B", "text": "No, each State must have a separate and distinct Governor", "isCorrect": false}, {"id": "C", "text": "Yes, but only for a maximum period of 6 months", "isCorrect": false}, {"id": "D", "text": "Only if approved by a 2/3rd majority in both State assemblies", "isCorrect": false}]',
        'A',
        'The 7th Amendment ($$1956$$) amended Article $$153$$, permitting the appointment of the same person as Governor for two or more States.'
    ),
    (
        'a1120000-0000-0000-0000-000000000070'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'The salary and allowances of the Governor of a State are charged upon:',
        '[{"id": "A", "text": "The Consolidated Fund of the concerned State", "isCorrect": true}, {"id": "B", "text": "The Consolidated Fund of India", "isCorrect": false}, {"id": "C", "text": "The Contingency Fund of India", "isCorrect": false}, {"id": "D", "text": "The Ministry of Home Affairs annual budget", "isCorrect": false}]',
        'A',
        'Under Article $$202(3)(a)$$, the emoluments and allowances of the Governor are charged on the Consolidated Fund of the State (non-votable).'
    ),
    (
        'a1120000-0000-0000-0000-000000000071'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'What is the present sanctioned strength of judges in the Supreme Court of India (including the Chief Justice of India)?',
        '[{"id": "A", "text": "$$34\\text{ judges}$$ ($$33\\text{ judges} + 1\\text{ CJI}$)", "isCorrect": true}, {"id": "B", "text": "$$31\\text{ judges}$$", "isCorrect": false}, {"id": "C", "text": "$$26\\text{ judges}$$", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ judges}$$", "isCorrect": false}]',
        'A',
        'The Supreme Court (Number of Judges) Amendment Act $$2019$$ increased the sanctioned strength from 31 to 34 (33 Puisne Judges + Chief Justice of India).'
    ),
    (
        'a1120000-0000-0000-0000-000000000072'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$127$$, who appoints an ''Ad-hoc Judge'' in the Supreme Court when there is a lack of quorum?',
        '[{"id": "A", "text": "The Chief Justice of India with prior consent of the President", "isCorrect": true}, {"id": "B", "text": "The President of India directly", "isCorrect": false}, {"id": "C", "text": "The Union Minister of Law", "isCorrect": false}, {"id": "D", "text": "The Collegium of Supreme Court Judges", "isCorrect": false}]',
        'A',
        'Under Article $$127$$, the CJI may, with prior consent of the President and consultation with the Chief Justice of the High Court concerned, appoint a High Court judge as an ad-hoc SC judge.'
    ),
    (
        'a1120000-0000-0000-0000-000000000073'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Which Article provides that the law declared by the Supreme Court shall be binding on all courts within the territory of India?',
        '[{"id": "A", "text": "Article $$141$$", "isCorrect": true}, {"id": "B", "text": "Article $$142$$", "isCorrect": false}, {"id": "C", "text": "Article $$144$$", "isCorrect": false}, {"id": "D", "text": "Article $$137$$", "isCorrect": false}]',
        'A',
        'Article $$141$$ enshrines the doctrine of precedent (stare decisis), making law declared by the Supreme Court binding on all courts in India.'
    ),
    (
        'a1120000-0000-0000-0000-000000000074'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'Article $$142$$ of the Constitution empowers the Supreme Court to:',
        '[{"id": "A", "text": "Pass any decree or order necessary for doing ''complete justice'' in any cause or matter", "isCorrect": true}, {"id": "B", "text": "Dissolve the Lok Sabha during political deadlocks", "isCorrect": false}, {"id": "C", "text": "Veto constitutional amendments approved by Parliament", "isCorrect": false}, {"id": "D", "text": "Assume direct administrative control of State High Courts", "isCorrect": false}]',
        'A',
        'Article $$142(1)$$ provides unique inherent power to the Supreme Court to pass any decree or order to do ''complete justice'' between parties.'
    ),
    (
        'a1120000-0000-0000-0000-000000000075'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Which Article empowers the Supreme Court to review its own judgments or orders?',
        '[{"id": "A", "text": "Article $$137$$", "isCorrect": true}, {"id": "B", "text": "Article $$139$$", "isCorrect": false}, {"id": "C", "text": "Article $$140$$", "isCorrect": false}, {"id": "D", "text": "Article $$145$$", "isCorrect": false}]',
        'A',
        'Under Article $$137$$, the Supreme Court has the power to review any judgment pronounced or order made by it (Review Petition and Curative Petition).'
    ),
    (
        'a1120000-0000-0000-0000-000000000076'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'How many High Courts are there currently in India?',
        '[{"id": "A", "text": "$$25\\text{ High Courts}$$", "isCorrect": true}, {"id": "B", "text": "$$24\\text{ High Courts}$$", "isCorrect": false}, {"id": "C", "text": "$$28\\text{ High Courts}$$", "isCorrect": false}, {"id": "D", "text": "$$21\\text{ High Courts}$$", "isCorrect": false}]',
        'A',
        'There are currently $$25$$ High Courts in India, with the Andhra Pradesh High Court at Amaravati ($$2019$$) being the 25th.'
    ),
    (
        'a1120000-0000-0000-0000-000000000077'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Which High Court has the largest territorial jurisdiction covering four northeastern states?',
        '[{"id": "A", "text": "Gauhati High Court (Assam, Nagaland, Mizoram, Arunachal Pradesh)", "isCorrect": true}, {"id": "B", "text": "Calcutta High Court", "isCorrect": false}, {"id": "C", "text": "Bombay High Court", "isCorrect": false}, {"id": "D", "text": "Kerala High Court", "isCorrect": false}]',
        'A',
        'The Gauhati High Court has jurisdiction over four states: Assam, Nagaland, Mizoram, and Arunachal Pradesh.'
    ),
    (
        'a1120000-0000-0000-0000-000000000078'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'Under Article $$227$$, every High Court has power of superintendence over:',
        '[{"id": "A", "text": "All subordinate courts and tribunals throughout the territories in relation to which it exercises jurisdiction", "isCorrect": true}, {"id": "B", "text": "Only District Civil Courts, excluding revenue tribunals", "isCorrect": false}, {"id": "C", "text": "Military Courts and Courts Martial", "isCorrect": false}, {"id": "D", "text": "Neighboring High Courts in the judicial zone", "isCorrect": false}]',
        'A',
        'Article $$227$$ confers on every High Court supervisory jurisdiction (administrative and judicial) over all subordinate courts and tribunals (except armed forces tribunals).'
    ),
    (
        'a1120000-0000-0000-0000-000000000079'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'District Judges in a State are appointed by:',
        '[{"id": "A", "text": "The Governor of the State in consultation with the High Court (Article $$233$$)", "isCorrect": true}, {"id": "B", "text": "The President of India", "isCorrect": false}, {"id": "C", "text": "The Chief Minister of the State", "isCorrect": false}, {"id": "D", "text": "The State Public Service Commission unilaterally", "isCorrect": false}]',
        'A',
        'Under Article $$233(1)$$, appointments and postings of District Judges are made by the Governor in consultation with the State High Court.'
    ),
    (
        'a1120000-0000-0000-0000-000000000080'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Who was the first woman judge appointed to the Supreme Court of India in $$1989$$?',
        '[{"id": "A", "text": "Justice M. Fathima Beevi", "isCorrect": true}, {"id": "B", "text": "Justice Sujata Manohar", "isCorrect": false}, {"id": "C", "text": "Justice Ruma Pal", "isCorrect": false}, {"id": "D", "text": "Justice Leila Seth", "isCorrect": false}]',
        'A',
        'Justice M. Fathima Beevi became the first woman Judge of the Supreme Court of India in October $$1989$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000081'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Under Article $$243\text{E}$$, what is the normal duration of every Panchayat in India?',
        '[{"id": "A", "text": "$$5\\text{ years}$$ from the date appointed for its first meeting", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$4\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$3\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Article $$243\text{E}$$ provides that every Panchayat shall continue for 5 years from the date appointed for its first meeting, unless dissolved sooner.'
    ),
    (
        'a1120000-0000-0000-0000-000000000082'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'If a Panchayat is dissolved prematurely before its 5-year term, elections must be completed within:',
        '[{"id": "A", "text": "$$6\\text{ months}$$ from the date of dissolution", "isCorrect": true}, {"id": "B", "text": "$$3\\text{ months}$$", "isCorrect": false}, {"id": "C", "text": "$$1\\text{ year}$$", "isCorrect": false}, {"id": "D", "text": "$$45\\text{ days}$$", "isCorrect": false}]',
        'A',
        'Under Article $$243\text{E}(3)(b)$$, an election to constitute a dissolved Panchayat must be completed within six months of its dissolution.'
    ),
    (
        'a1120000-0000-0000-0000-000000000083'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'What is the minimum age prescribed for contesting an election to a Panchayat or Municipality?',
        '[{"id": "A", "text": "$$21\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$25\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Under Article $$243\text{F}(1)(b)$$, no person shall be disqualified for election to a Panchayat if he has attained the age of $$21$$ years.'
    ),
    (
        'a1120000-0000-0000-0000-000000000084'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'Which Committee appointed in $$1977$$ recommended replacing the 3-tier system with a 2-tier Panchayati Raj system (Mandal Panchayat and Zila Parishad)?',
        '[{"id": "A", "text": "Ashok Mehta Committee", "isCorrect": true}, {"id": "B", "text": "Balwant Rai Mehta Committee", "isCorrect": false}, {"id": "C", "text": "Dhar Commission", "isCorrect": false}, {"id": "D", "text": "Sarkaria Commission", "isCorrect": false}]',
        'A',
        'The Ashok Mehta Committee appointed by the Janata Government in $$1977$$ recommended a 2-tier Panchayati Raj system.'
    ),
    (
        'a1120000-0000-0000-0000-000000000085'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Which Committee in $$1986$$ first recommended constitutional recognition for Panchayati Raj institutions?',
        '[{"id": "A", "text": "L.M. Singhvi Committee", "isCorrect": true}, {"id": "B", "text": "G.V.K. Rao Committee", "isCorrect": false}, {"id": "C", "text": "Thungon Committee", "isCorrect": false}, {"id": "D", "text": "Gadgil Committee", "isCorrect": false}]',
        'A',
        'The L.M. Singhvi Committee ($$1986$$) appointed by Rajiv Gandhi recommended that local self-government institutions should be constitutionally recognized and protected.'
    ),
    (
        'a1120000-0000-0000-0000-000000000086'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'What is the ''Gram Sabha'' defined as under Article $$243(b)$$ of the Constitution?',
        '[{"id": "A", "text": "A body consisting of all persons registered in the electoral rolls of a village within the Panchayat area", "isCorrect": true}, {"id": "B", "text": "The elected executive council of ward members", "isCorrect": false}, {"id": "C", "text": "A council of elder villagers appointed by the Sarpanch", "isCorrect": false}, {"id": "D", "text": "The assembly of all land-owning resident heads", "isCorrect": false}]',
        'A',
        'Article $$243(b)$$ defines the Gram Sabha as a body consisting of persons registered in the electoral rolls relating to a village comprised within the area of Panchayat at the village level.'
    ),
    (
        'a1120000-0000-0000-0000-000000000087'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which was the first Municipal Corporation established in India in the year $$1688$$?',
        '[{"id": "A", "text": "Madras Municipal Corporation", "isCorrect": true}, {"id": "B", "text": "Calcutta Municipal Corporation", "isCorrect": false}, {"id": "C", "text": "Bombay Municipal Corporation", "isCorrect": false}, {"id": "D", "text": "Delhi Municipal Corporation", "isCorrect": false}]',
        'A',
        'The first municipal corporation in India was set up at Madras in $$1688$$, followed by Bombay and Calcutta in $$1726$$.'
    ),
    (
        'a1120000-0000-0000-0000-000000000088'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Which Constitutional Article provides for the constitution of a District Planning Committee ($$\text{DPC}$$) to consolidate plans prepared by Panchayats and Municipalities?',
        '[{"id": "A", "text": "Article $$243\\text{ZD}$$", "isCorrect": true}, {"id": "B", "text": "Article $$243\\text{ZE}$$", "isCorrect": false}, {"id": "C", "text": "Article $$243\\text{I}$$", "isCorrect": false}, {"id": "D", "text": "Article $$243\\text{Q}$$", "isCorrect": false}]',
        'A',
        'Article $$243\text{ZD}$$ mandates the constitution of a District Planning Committee (DPC) at the district level to consolidate plans prepared by Panchayats and Municipalities.'
    ),
    (
        'a1120000-0000-0000-0000-000000000089'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following states is exempted from the application of Part IX (Panchayati Raj) of the Constitution?',
        '[{"id": "A", "text": "Nagaland, Meghalaya, and Mizoram (Article $$243\\text{M}$$)", "isCorrect": true}, {"id": "B", "text": "Assam and Tripura", "isCorrect": false}, {"id": "C", "text": "Goa and Sikkim", "isCorrect": false}, {"id": "D", "text": "Himachal Pradesh and Uttarakhand", "isCorrect": false}]',
        'A',
        'Under Article $$243\text{M}$$, Part IX does not apply to the States of Nagaland, Meghalaya, and Mizoram, and certain tribal hill areas.'
    ),
    (
        'a1120000-0000-0000-0000-000000000090'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Who is known as the ''Father of Local Self-Government in India'' for his historic resolution of $$1882$$?',
        '[{"id": "A", "text": "Lord Ripon", "isCorrect": true}, {"id": "B", "text": "Lord Mayo", "isCorrect": false}, {"id": "C", "text": "Lord Curzon", "isCorrect": false}, {"id": "D", "text": "Lord Dalhousie", "isCorrect": false}]',
        'A',
        'Viceroy Lord Ripon''s Resolution of $$1882$$ laid the foundation of local self-government in urban India, earning him the title ''Father of Local Self-Government''.'
    ),
    (
        'a1120000-0000-0000-0000-000000000091'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Union Public Service Commission (UPSC) and State Public Service Commissions are established under which Article?',
        '[{"id": "A", "text": "Article $$315$$", "isCorrect": true}, {"id": "B", "text": "Article $$324$$", "isCorrect": false}, {"id": "C", "text": "Article $$280$$", "isCorrect": false}, {"id": "D", "text": "Article $$338$$", "isCorrect": false}]',
        'A',
        'Article $$315$$ provides for a Public Service Commission for the Union and a Public Service Commission for each State.'
    ),
    (
        'a1120000-0000-0000-0000-000000000092'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'What is the tenure of the Chairman and members of the Union Public Service Commission (UPSC)?',
        '[{"id": "A", "text": "$$6\\text{ years}$$ or until attaining the age of $$65\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$5\\text{ years}$$ or until attaining the age of $$62\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$6\\text{ years}$$ or until attaining the age of $$62\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$5\\text{ years}$$ with no age limit", "isCorrect": false}]',
        'A',
        'Under Article $$316(2)$$, a member of UPSC holds office for a term of 6 years or until attaining 65 years (62 years for State PSC).'
    ),
    (
        'a1120000-0000-0000-0000-000000000093'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'The National Commission for Scheduled Castes ($$\text{NCSC}$$) is established under which Article?',
        '[{"id": "A", "text": "Article $$338$$", "isCorrect": true}, {"id": "B", "text": "Article $$338\\text{A}$$", "isCorrect": false}, {"id": "C", "text": "Article $$338\\text{B}$$", "isCorrect": false}, {"id": "D", "text": "Article $$340$$", "isCorrect": false}]',
        'A',
        'Article $$338$$ establishes the National Commission for Scheduled Castes. Article $$338\text{A}$$ provides for NCST, and Article $$338\text{B}$$ for NCBC.'
    ),
    (
        'a1120000-0000-0000-0000-000000000094'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment bifurcated the National Commission for SCs and STs into two separate bodies (NCSC and NCST)?',
        '[{"id": "A", "text": "$$89\\text{th}$$ Constitutional Amendment Act $$2003$$", "isCorrect": true}, {"id": "B", "text": "$$65\\text{th}$$ Constitutional Amendment Act $$1990$$", "isCorrect": false}, {"id": "C", "text": "$$102\\text{nd}$$ Constitutional Amendment Act $$2018$$", "isCorrect": false}, {"id": "D", "text": "$$91\\text{st}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}]',
        'A',
        'The $$89\text{th}$$ Amendment ($$2003$$) bifurcated the joint Commission into NCSC (Article 338) and NCST (Article 338A).'
    ),
    (
        'a1120000-0000-0000-0000-000000000095'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'Which Constitutional Amendment accorded constitutional status to the National Commission for Backward Classes ($$\text{NCBC}$$)?',
        '[{"id": "A", "text": "$$102\\text{nd}$$ Constitutional Amendment Act $$2018$$", "isCorrect": true}, {"id": "B", "text": "$$103\\text{rd}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}, {"id": "C", "text": "$$100\\text{th}$$ Constitutional Amendment Act $$2015$$", "isCorrect": false}, {"id": "D", "text": "$$101\\text{st}$$ Constitutional Amendment Act $$2016$$", "isCorrect": false}]',
        'A',
        'The $$102\text{nd}$$ Amendment ($$2018$$) inserted Article $$338\text{B}$$, giving constitutional status to the National Commission for Backward Classes.'
    ),
    (
        'a1120000-0000-0000-0000-000000000096'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'Goods and Services Tax ($$\text{GST}$$) was introduced in India by which Constitutional Amendment Act?',
        '[{"id": "A", "text": "$$101\\text{st}$$ Constitutional Amendment Act $$2016$$", "isCorrect": true}, {"id": "B", "text": "$$100\\text{th}$$ Constitutional Amendment Act $$2015$$", "isCorrect": false}, {"id": "C", "text": "$$103\\text{rd}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}, {"id": "D", "text": "$$99\\text{th}$$ Constitutional Amendment Act $$2014$$", "isCorrect": false}]',
        'A',
        'The $$101\text{st}$$ Amendment Act ($$2016$$) introduced the GST regime from July 1, $$2017$$, inserting Article $$246\text{A}$$ and Article $$279\text{A}$$ (GST Council).'
    ),
    (
        'a1120000-0000-0000-0000-000000000097'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment provided a $$10\%$$ reservation for Economically Weaker Sections ($$\text{EWS}$$) in admissions and public jobs?',
        '[{"id": "A", "text": "$$103\\text{rd}$$ Constitutional Amendment Act $$2019$$", "isCorrect": true}, {"id": "B", "text": "$$104\\text{th}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}, {"id": "C", "text": "$$105\\text{th}$$ Constitutional Amendment Act $$2021$$", "isCorrect": false}, {"id": "D", "text": "$$102\\text{nd}$$ Constitutional Amendment Act $$2018$$", "isCorrect": false}]',
        'A',
        'The $$103\text{rd}$$ Amendment ($$2019$$) amended Articles $$15$$ and $$16$$ to provide up to $$10\%$$ reservation for EWS.'
    ),
    (
        'a1120000-0000-0000-0000-000000000098'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'UNDERSTAND',
        'Is the Planning Commission / NITI Aayog a constitutional body?',
        '[{"id": "A", "text": "No, it is a non-constitutional, extra-constitutional body created by an executive cabinet resolution", "isCorrect": true}, {"id": "B", "text": "Yes, created under Article $$280$$", "isCorrect": false}, {"id": "C", "text": "Yes, created under Article $$263$$", "isCorrect": false}, {"id": "D", "text": "It is a statutory body established by an Act of Parliament", "isCorrect": false}]',
        'A',
        'NITI Aayog (established January 1, $$2015$$) is an extra-constitutional, non-statutory think tank created by an executive resolution of the Union Cabinet.'
    ),
    (
        'a1120000-0000-0000-0000-000000000099'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'The Inter-State Council to foster center-state coordination is established by the President under which Article?',
        '[{"id": "A", "text": "Article $$263$$", "isCorrect": true}, {"id": "B", "text": "Article $$280$$", "isCorrect": false}, {"id": "C", "text": "Article $$312$$", "isCorrect": false}, {"id": "D", "text": "Article $$300$$", "isCorrect": false}]',
        'A',
        'Article $$263$$ empowers the President to establish an Inter-State Council (first established in $$1990$$ following Sarkaria Commission recommendations).'
    ),
    (
        'a1120000-0000-0000-0000-000000000100'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'What is the requirement for creating a new All India Service under Article $$312$$ of the Constitution?',
        '[{"id": "A", "text": "Rajya Sabha must pass a resolution supported by not less than two-thirds of members present and voting", "isCorrect": true}, {"id": "B", "text": "Lok Sabha must pass a resolution by simple majority", "isCorrect": false}, {"id": "C", "text": "The President must issue an emergency ordinance", "isCorrect": false}, {"id": "D", "text": "A majority of all State Legislative Assemblies must request it", "isCorrect": false}]',
        'A',
        'Article $$312$$ gives special power to the Rajya Sabha to authorize Parliament to create new All India Services by a resolution passed with a 2/3rd majority.'
    )
) AS v(id, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Indian Polity' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
