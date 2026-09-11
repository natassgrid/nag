-- ============================================================
-- Seed Questions: RRB NTPC - Indian Polity and Governance (Set 4) (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Awareness / General Studies -> Topic: Indian Polity
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX ($$..$$)
-- UUID Range: a1130000-0000-0000-0000-000000000001 to a1130000-0000-0000-0000-000000000100
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

-- Step 2: Insert 100 RRB NTPC Indian Polity Questions (Set 4)
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
        'a1130000-0000-0000-0000-000000000001'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'How many members were there in the Drafting Committee of the Constituent Assembly, chaired by Dr. B.R. Ambedkar?',
        '[{"id": "A", "text": "$$7\\text{ members}$$", "isCorrect": true}, {"id": "B", "text": "$$9\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ members}$$", "isCorrect": false}, {"id": "D", "text": "$$11\\text{ members}$$", "isCorrect": false}]',
        'A',
        'The Drafting Committee, set up on August 29, $$1947$$, consisted of 7 members: Dr. B.R. Ambedkar (Chairman), N. Gopalaswamy Ayyangar, Alladi Krishnaswamy Iyer, Dr. K.M. Munshi, Syed Mohammad Saadulla, N. Madhava Rau, and T.T. Krishnamachari.'
    ),
    (
        'a1130000-0000-0000-0000-000000000002'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Who among the following was the only Congress member originally appointed to the Drafting Committee?',
        '[{"id": "A", "text": "Dr. K.M. Munshi", "isCorrect": true}, {"id": "B", "text": "Jawaharlal Nehru", "isCorrect": false}, {"id": "C", "text": "Sardar Patel", "isCorrect": false}, {"id": "D", "text": "Dr. Rajendra Prasad", "isCorrect": false}]',
        'A',
        'Dr. K.M. Munshi was the only member of the Drafting Committee who was originally an active member of the Indian National Congress.'
    ),
    (
        'a1130000-0000-0000-0000-000000000003'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'Under which mission plan was the Constituent Assembly of India formed in $$1946$$?',
        '[{"id": "A", "text": "Cabinet Mission Plan of $$1946$$", "isCorrect": true}, {"id": "B", "text": "Cripps Mission of $$1942$$", "isCorrect": false}, {"id": "C", "text": "Wavell Plan of $$1945$$", "isCorrect": false}, {"id": "D", "text": "Mountbatten Plan of $$1947$$", "isCorrect": false}]',
        'A',
        'The Constituent Assembly was constituted in November $$1946$$ under the scheme formulated by the Cabinet Mission (Lord Pethick-Lawrence, Sir Stafford Cripps, and A.V. Alexander).'
    ),
    (
        'a1130000-0000-0000-0000-000000000004'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Who calligraphed the original handwritten Constitution of India in flowing italic style?',
        '[{"id": "A", "text": "Prem Behari Narain Raizada", "isCorrect": true}, {"id": "B", "text": "Nandalal Bose", "isCorrect": false}, {"id": "C", "text": "Beohar Rammanohar Sinha", "isCorrect": false}, {"id": "D", "text": "Vasant Krishan Vaidya", "isCorrect": false}]',
        'A',
        'The original Constitution of India was handwritten by Prem Behari Narain Raizada in flowing italic style, while the pages were decorated by Shantiniketan artists led by Nandalal Bose.'
    ),
    (
        'a1130000-0000-0000-0000-000000000005'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'The ''Rule of Law'' and ''Parliamentary privileges'' in the Indian Constitution were borrowed from which country?',
        '[{"id": "A", "text": "United Kingdom (British Constitution)", "isCorrect": true}, {"id": "B", "text": "United States of America", "isCorrect": false}, {"id": "C", "text": "Canada", "isCorrect": false}, {"id": "D", "text": "France", "isCorrect": false}]',
        'A',
        'Parliamentary government, the rule of law, legislative procedure, single citizenship, cabinet system, prerogative writs, and parliamentary privileges were drawn from Britain.'
    ),
    (
        'a1130000-0000-0000-0000-000000000006'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'The suspension of Fundamental Rights during National Emergency was borrowed from the constitution of:',
        '[{"id": "A", "text": "Germany (Weimar Constitution)", "isCorrect": true}, {"id": "B", "text": "Soviet Union (USSR)", "isCorrect": false}, {"id": "C", "text": "Japan", "isCorrect": false}, {"id": "D", "text": "Canada", "isCorrect": false}]',
        'A',
        'Suspension of Fundamental Rights during National Emergency was borrowed from the Weimar Constitution of Germany.'
    ),
    (
        'a1130000-0000-0000-0000-000000000007'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Who was the Chairman of the Union Powers Committee of the Constituent Assembly?',
        '[{"id": "A", "text": "Pandit Jawaharlal Nehru", "isCorrect": true}, {"id": "B", "text": "Sardar Patel", "isCorrect": false}, {"id": "C", "text": "Dr. B.R. Ambedkar", "isCorrect": false}, {"id": "D", "text": "Dr. Rajendra Prasad", "isCorrect": false}]',
        'A',
        'Jawaharlal Nehru chaired the Union Powers Committee, Union Constitution Committee, and States Committee.'
    ),
    (
        'a1130000-0000-0000-0000-000000000008'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'What was the total membership of the Constituent Assembly originally allotted before partition, and how many remained after partition?',
        '[{"id": "A", "text": "$$389\\text{ originally, reducing to } 299\\text{ after partition}$$", "isCorrect": true}, {"id": "B", "text": "$$350\\text{ originally, reducing to } 250\\text{ after partition}$$", "isCorrect": false}, {"id": "C", "text": "$$400\\text{ originally, reducing to } 300\\text{ after partition}$$", "isCorrect": false}, {"id": "D", "text": "$$389\\text{ originally, reducing to } 325\\text{ after partition}$$", "isCorrect": false}]',
        'A',
        'The total strength was originally 389 members. Following the Mountbatten Plan ($$1947$$) and creation of Pakistan, the Indian Constituent Assembly strength dropped to 299.'
    ),
    (
        'a1130000-0000-0000-0000-000000000009'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Which was the animal adopted as the official seal (symbol) of the Constituent Assembly?',
        '[{"id": "A", "text": "Elephant", "isCorrect": true}, {"id": "B", "text": "Tiger", "isCorrect": false}, {"id": "C", "text": "Lion", "isCorrect": false}, {"id": "D", "text": "Peacock", "isCorrect": false}]',
        'A',
        'The elephant was adopted as the official emblem (symbol/seal) of the Constituent Assembly.'
    ),
    (
        'a1130000-0000-0000-0000-000000000010'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'UNDERSTAND',
        'The concept of ''Procedure Established by Law'' in Article 21 was borrowed from which country''s constitution?',
        '[{"id": "A", "text": "Japan", "isCorrect": true}, {"id": "B", "text": "USA", "isCorrect": false}, {"id": "C", "text": "UK", "isCorrect": false}, {"id": "D", "text": "Australia", "isCorrect": false}]',
        'A',
        '''Procedure Established by Law'' was borrowed from the Japanese Constitution (Article 31), though later expanded to include ''Due Process of Law'' by the Supreme Court in Maneka Gandhi ($$1978$$).'
    ),
    (
        'a1130000-0000-0000-0000-000000000011'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which form of justice is NOT explicitly mentioned in the Preamble of the Indian Constitution?',
        '[{"id": "A", "text": "Religious justice", "isCorrect": true}, {"id": "B", "text": "Social justice", "isCorrect": false}, {"id": "C", "text": "Economic justice", "isCorrect": false}, {"id": "D", "text": "Political justice", "isCorrect": false}]',
        'A',
        'The Preamble secures to all its citizens: ''JUSTICE, social, economic and political''. Religious justice is not mentioned.'
    ),
    (
        'a1130000-0000-0000-0000-000000000012'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'What is the meaning of ''Sovereignty'' as stated in the Preamble?',
        '[{"id": "A", "text": "India is completely independent externally and supreme internally, without subordination to any foreign power", "isCorrect": true}, {"id": "B", "text": "The President is the absolute constitutional monarch", "isCorrect": false}, {"id": "C", "text": "The Union government can alter fundamental rights unconditionally", "isCorrect": false}, {"id": "D", "text": "States possess sovereign rights to secede from the Union", "isCorrect": false}]',
        'A',
        'Sovereignty implies that India is neither a dependency nor a dominion of any other nation, but an independent sovereign state.'
    ),
    (
        'a1130000-0000-0000-0000-000000000013'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Why is the Indian Federation described by K.C. Wheare as ''Quasi-Federal''?',
        '[{"id": "A", "text": "Because it has a strong unitary bias with central supremacy in emergencies and key appointments", "isCorrect": true}, {"id": "B", "text": "Because States possess separate constitutions and citizenship", "isCorrect": false}, {"id": "C", "text": "Because it was created by voluntary treaty between sovereign princely states", "isCorrect": false}, {"id": "D", "text": "Because local bodies have equal constitutional status to the Union", "isCorrect": false}]',
        'A',
        'Prof. K.C. Wheare described the Indian Constitution as ''quasi-federal'' because it is federal in form during peace, but unitary in substance and operations during emergencies.'
    ),
    (
        'a1130000-0000-0000-0000-000000000014'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'REMEMBER',
        'Which was the last princely state / territory integrated into India from Portuguese rule in December $$1961$$?',
        '[{"id": "A", "text": "Goa, Daman and Diu (Operation Vijay)", "isCorrect": true}, {"id": "B", "text": "Puducherry", "isCorrect": false}, {"id": "C", "text": "Hyderabad (Operation Polo)", "isCorrect": false}, {"id": "D", "text": "Junagadh", "isCorrect": false}]',
        'A',
        'Goa, Daman and Diu were liberated from Portuguese rule in December $$1961$$ via military ''Operation Vijay'' and made a Union Territory by the 12th Amendment ($$1962$$).'
    ),
    (
        'a1130000-0000-0000-0000-000000000015'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which constitutional amendment transferred the Berubari Union to Pakistan, requiring an amendment under Article 368?',
        '[{"id": "A", "text": "$$9\\text{th}$$ Constitutional Amendment Act $$1960$$", "isCorrect": true}, {"id": "B", "text": "$$7\\text{th}$$ Constitutional Amendment Act $$1956$$", "isCorrect": false}, {"id": "C", "text": "$$1\\text{st}$$ Constitutional Amendment Act $$1951$$", "isCorrect": false}, {"id": "D", "text": "$$24\\text{th}$$ Constitutional Amendment Act $$1971$$", "isCorrect": false}]',
        'A',
        'The Supreme Court held that cession of Indian territory to a foreign country cannot be done under Article 3, necessitating the 9th Constitutional Amendment Act $$1960$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000016'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Which Article allows Parliament to create a new state or alter boundaries of an existing state without the consent of the concerned state legislature?',
        '[{"id": "A", "text": "Article $$3$$", "isCorrect": true}, {"id": "B", "text": "Article $$2$$", "isCorrect": false}, {"id": "C", "text": "Article $$368$$", "isCorrect": false}, {"id": "D", "text": "Article $$356$$", "isCorrect": false}]',
        'A',
        'Under Article 3, the bill is referred to the state legislature for expressing views within a specified period, but Parliament is NOT bound by the state legislature''s views.'
    ),
    (
        'a1130000-0000-0000-0000-000000000017'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'What is the legal concept behind ''An indestructible Union of destructible States''?',
        '[{"id": "A", "text": "The territorial integrity of States is not guaranteed, but the Union of India is permanent and cannot be broken", "isCorrect": true}, {"id": "B", "text": "States have the constitutional right to secede whenever their borders change", "isCorrect": false}, {"id": "C", "text": "The Union can be dissolved by a referendum of all States", "isCorrect": false}, {"id": "D", "text": "State borders can only be altered by amending the basic structure", "isCorrect": false}]',
        'A',
        'Parliament can redraw the political map of India by creating or altering states under Article 3; hence states are destructible, but the Union is indestructible.'
    ),
    (
        'a1130000-0000-0000-0000-000000000018'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment ratified the Land Boundary Agreement ($$\text{LBA}$$) between India and Bangladesh in $$2015$$?',
        '[{"id": "A", "text": "$$100\\text{th}$$ Constitutional Amendment Act $$2015$$", "isCorrect": true}, {"id": "B", "text": "$$99\\text{th}$$ Constitutional Amendment Act $$2014$$", "isCorrect": false}, {"id": "C", "text": "$$101\\text{st}$$ Constitutional Amendment Act $$2016$$", "isCorrect": false}, {"id": "D", "text": "$$102\\text{nd}$$ Constitutional Amendment Act $$2018$$", "isCorrect": false}]',
        'A',
        'The $$100\text{th}$$ Amendment Act ($$2015$$) amended the First Schedule to give effect to the acquiring and transfer of enclaves between India and Bangladesh.'
    ),
    (
        'a1130000-0000-0000-0000-000000000019'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$6$$, what was the cutoff date for migrants from Pakistan to acquire citizenship of India automatically?',
        '[{"id": "A", "text": "July $$19, 1948$$", "isCorrect": true}, {"id": "B", "text": "August $$15, 1947$$", "isCorrect": false}, {"id": "C", "text": "January $$26, 1950$$", "isCorrect": false}, {"id": "D", "text": "November $$26, 1949$$", "isCorrect": false}]',
        'A',
        'Article $$6(b)$$ established July $$19, 1948$$ (the date when the permit system for migration was introduced) as the cutoff date.'
    ),
    (
        'a1130000-0000-0000-0000-000000000020'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Can an Overseas Citizen of India ($$\text{OCI}$$) cardholder vote in Indian legislative elections or hold constitutional offices?',
        '[{"id": "A", "text": "No, OCI cardholders have no voting rights and cannot hold constitutional posts", "isCorrect": true}, {"id": "B", "text": "Yes, they can vote in Lok Sabha elections but not Rajya Sabha", "isCorrect": false}, {"id": "C", "text": "Yes, they have all citizen rights except appointment to the Armed Forces", "isCorrect": false}, {"id": "D", "text": "Yes, provided they reside in India for at least six months a year", "isCorrect": false}]',
        'A',
        'OCI status is a visa facility conferring economic, financial, and educational benefits. Under Section 7B of the Citizenship Act, they cannot vote, contest elections, or hold public office.'
    ),
    (
        'a1130000-0000-0000-0000-000000000021'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Fundamental Rights CANNOT be suspended even during a National Emergency declared under Article 352?',
        '[{"id": "A", "text": "Articles $$20$$ and $$21$$ (Protection in respect of conviction and Right to Life)", "isCorrect": true}, {"id": "B", "text": "Article $$19$$ and Article $$14$$", "isCorrect": false}, {"id": "C", "text": "Articles $$25$$ to $$28$$", "isCorrect": false}, {"id": "D", "text": "Article $$32$$ and Article $$226$$", "isCorrect": false}]',
        'A',
        'The $$44\text{th}$$ Constitutional Amendment ($$1978$$) barred the suspension of enforcement of Articles $$20$$ and $$21$$ during a National Emergency under Article $$359$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000022'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$358$$, the Fundamental Rights guaranteed under Article $$19$$ are automatically suspended ONLY when an emergency is declared on grounds of:',
        '[{"id": "A", "text": "War or External Aggression (External Emergency)", "isCorrect": true}, {"id": "B", "text": "Armed Rebellion (Internal Emergency)", "isCorrect": false}, {"id": "C", "text": "Financial crisis or stock market collapse", "isCorrect": false}, {"id": "D", "text": "Breakdown of constitutional machinery in a State", "isCorrect": false}]',
        'A',
        'The $$44\text{th}$$ Amendment amended Article $$358$$ so that Article $$19$$ is suspended automatically ONLY when emergency is proclaimed due to War or External Aggression, NOT Armed Rebellion.'
    ),
    (
        'a1130000-0000-0000-0000-000000000023'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Article gives protection against arrest and detention in certain cases and provides production before a magistrate within 24 hours?',
        '[{"id": "A", "text": "Article $$22$$", "isCorrect": true}, {"id": "B", "text": "Article $$20$$", "isCorrect": false}, {"id": "C", "text": "Article $$21$$", "isCorrect": false}, {"id": "D", "text": "Article $$19$$", "isCorrect": false}]',
        'A',
        'Article $$22(2)$$ mandates that every person arrested must be produced before the nearest magistrate within 24 hours of arrest (excluding travel time).'
    ),
    (
        'a1130000-0000-0000-0000-000000000024'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'What is the maximum period for which a person can be detained under a Preventive Detention law without obtaining the opinion of an Advisory Board?',
        '[{"id": "A", "text": "$$3\\text{ months}$$", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ months}$$", "isCorrect": false}, {"id": "C", "text": "$$1\\text{ month}$$", "isCorrect": false}, {"id": "D", "text": "$$14\\text{ days}$$", "isCorrect": false}]',
        'A',
        'Under Article $$22(4)$$, no law for preventive detention can authorize detention for longer than 3 months unless an Advisory Board finds sufficient cause for extension.'
    ),
    (
        'a1130000-0000-0000-0000-000000000025'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'REMEMBER',
        'Which Article guarantees the right of minorities to establish and administer educational institutions of their choice?',
        '[{"id": "A", "text": "Article $$30$$", "isCorrect": true}, {"id": "B", "text": "Article $$29$$", "isCorrect": false}, {"id": "C", "text": "Article $$28$$", "isCorrect": false}, {"id": "D", "text": "Article $$25$$", "isCorrect": false}]',
        'A',
        'Article $$30(1)$$ provides that all minorities, whether based on religion or language, shall have the right to establish and administer educational institutions of their choice.'
    ),
    (
        'a1130000-0000-0000-0000-000000000026'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Protection of language, script, and culture of minorities is safeguarded under:',
        '[{"id": "A", "text": "Article $$29$$", "isCorrect": true}, {"id": "B", "text": "Article $$30$$", "isCorrect": false}, {"id": "C", "text": "Article $$343$$", "isCorrect": false}, {"id": "D", "text": "Article $$350\\text{A}$$", "isCorrect": false}]',
        'A',
        'Article $$29(1)$$ protects the right of any section of citizens having a distinct language, script or culture of its own to conserve the same.'
    ),
    (
        'a1130000-0000-0000-0000-000000000027'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which Article empowers Parliament to modify or restrict the application of Fundamental Rights to members of the Armed Forces and Police forces?',
        '[{"id": "A", "text": "Article $$33$$", "isCorrect": true}, {"id": "B", "text": "Article $$34$$", "isCorrect": false}, {"id": "C", "text": "Article $$35$$", "isCorrect": false}, {"id": "D", "text": "Article $$31$$", "isCorrect": false}]',
        'A',
        'Article $$33$$ empowers Parliament to determine to what extent Fundamental Rights apply to members of the Armed Forces, Paramilitary, and Police forces to ensure proper discipline.'
    ),
    (
        'a1130000-0000-0000-0000-000000000028'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Article $$34$$ relates to the restriction on Fundamental Rights while:',
        '[{"id": "A", "text": "Martial Law (military rule) is in force in any area", "isCorrect": true}, {"id": "B", "text": "A National Emergency is in force", "isCorrect": false}, {"id": "C", "text": "Elections are underway", "isCorrect": false}, {"id": "D", "text": "A disaster management protocol is activated", "isCorrect": false}]',
        'A',
        'Article $$34$$ empowers Parliament to indemnify any person in respect of acts done during Martial Law and validates sentences passed under martial law.'
    ),
    (
        'a1130000-0000-0000-0000-000000000029'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'REMEMBER',
        'Which Article provides that only Parliament, and not State Legislatures, has the power to make laws giving effect to certain Fundamental Rights (e.g. Articles 16(3), 32(3), 33, 34)?',
        '[{"id": "A", "text": "Article $$35$$", "isCorrect": true}, {"id": "B", "text": "Article $$36$$", "isCorrect": false}, {"id": "C", "text": "Article $$13$$", "isCorrect": false}, {"id": "D", "text": "Article $$246$$", "isCorrect": false}]',
        'A',
        'Article $$35$$ ensures uniformity throughout India by reserving exclusive legislative power to Parliament to prescribe punishments for violating fundamental rights.'
    ),
    (
        'a1130000-0000-0000-0000-000000000030'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which writ is issued to prevent a judicial or quasi-judicial authority from continuing proceedings when it lacks jurisdiction?',
        '[{"id": "A", "text": "Prohibition", "isCorrect": true}, {"id": "B", "text": "Certiorari", "isCorrect": false}, {"id": "C", "text": "Mandamus", "isCorrect": false}, {"id": "D", "text": "Quo-Warranto", "isCorrect": false}]',
        'A',
        'Prohibition is a preventive writ issued while proceedings are pending in a lower court to stop it from exceeding its jurisdiction. Certiorari is curative, quashing an order already delivered.'
    ),
    (
        'a1130000-0000-0000-0000-000000000031'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Directive Principles of State Policy are contained in which Part of the Indian Constitution?',
        '[{"id": "A", "text": "Part IV (Articles $$36$$ to $$51$$)", "isCorrect": true}, {"id": "B", "text": "Part III (Articles $$12$$ to $$35$$)", "isCorrect": false}, {"id": "C", "text": "Part IV-A (Article $$51\\text{A}$$)", "isCorrect": false}, {"id": "D", "text": "Part V (Articles $$52$$ to $$151$$)", "isCorrect": false}]',
        'A',
        'Part IV of the Constitution contains the Directive Principles of State Policy (Articles $$36$$ to $$51$$).'
    ),
    (
        'a1130000-0000-0000-0000-000000000032'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following is categorized as a ''Gandhian Principle'' among the Directive Principles of State Policy?',
        '[{"id": "A", "text": "Promotion of cottage industries on an individual or co-operative basis (Article $$43$$)", "isCorrect": true}, {"id": "B", "text": "Securing a Uniform Civil Code (Article $$44$$)", "isCorrect": false}, {"id": "C", "text": "Separation of judiciary from executive (Article $$50$$)", "isCorrect": false}, {"id": "D", "text": "Promotion of international peace and security (Article $$51$$)", "isCorrect": false}]',
        'A',
        'Gandhian principles reflect Gandhi''s reconstruction program: Articles 40 (panchayats), 43 (cottage industries), 43B (cooperatives), 46 (weaker sections), 47 (prohibition), 48 (cattle protection).'
    ),
    (
        'a1130000-0000-0000-0000-000000000033'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'Which Constitutional Article gives protection to laws giving effect to DPSPs under Article $$39(b)$$ and $$39(c)$$ against challenges under Articles $$14$$ and $$19$$?',
        '[{"id": "A", "text": "Article $$31\\text{C}$$", "isCorrect": true}, {"id": "B", "text": "Article $$31\\text{A}$$", "isCorrect": false}, {"id": "C", "text": "Article $$31\\text{B}$$", "isCorrect": false}, {"id": "D", "text": "Article $$38$$", "isCorrect": false}]',
        'A',
        'Inserted by the 25th Amendment ($$1971$$), Article $$31\text{C}$$ states that laws securing Article 39(b) and 39(c) cannot be declared void on ground of violating Articles 14 or 19.'
    ),
    (
        'a1130000-0000-0000-0000-000000000034'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Prohibition of slaughter of cows, calves, and other milch and draught cattle is mentioned under which Article?',
        '[{"id": "A", "text": "Article $$48$$", "isCorrect": true}, {"id": "B", "text": "Article $$47$$", "isCorrect": false}, {"id": "C", "text": "Article $$46$$", "isCorrect": false}, {"id": "D", "text": "Article $$49$$", "isCorrect": false}]',
        'A',
        'Article $$48$$ directs the State to organize agriculture and animal husbandry on modern lines and prohibit slaughter of cows and calves.'
    ),
    (
        'a1130000-0000-0000-0000-000000000035'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'REMEMBER',
        'Promotion of educational and economic interests of Scheduled Castes, Scheduled Tribes, and other weaker sections is guided by:',
        '[{"id": "A", "text": "Article $$46$$", "isCorrect": true}, {"id": "B", "text": "Article $$45$$", "isCorrect": false}, {"id": "C", "text": "Article $$44$$", "isCorrect": false}, {"id": "D", "text": "Article $$43$$", "isCorrect": false}]',
        'A',
        'Article $$46$$ directs the State to promote with special care the educational and economic interests of the weaker sections, particularly SCs and STs.'
    ),
    (
        'a1130000-0000-0000-0000-000000000036'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'The Justice J.S. Verma Committee ($$1999$$) on Fundamental Duties identified the existence of legal provisions for the enforcement of:',
        '[{"id": "A", "text": "Several Fundamental Duties under existing statutes (e.g. Prevention of Insults to National Honour Act, Wildlife Protection Act)", "isCorrect": true}, {"id": "B", "text": "Mandatory imprisonment for not respecting the National Anthem", "isCorrect": false}, {"id": "C", "text": "Constitutional disqualification from contesting polls for not planting trees", "isCorrect": false}, {"id": "D", "text": "Mandatory military training for all youth under Article 51A", "isCorrect": false}]',
        'A',
        'The Verma Committee ($$1999$$) documented that many Fundamental Duties are already backed by statutory penal laws (such as IPC, RPA 1951, Wildlife Protection Act 1972).'
    ),
    (
        'a1130000-0000-0000-0000-000000000037'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'To renounce practices derogatory to the dignity of women is an explicit Fundamental Duty under which clause?',
        '[{"id": "A", "text": "Article $$51\\text{A}(e)$$", "isCorrect": true}, {"id": "B", "text": "Article $$51\\text{A}(b)$$", "isCorrect": false}, {"id": "C", "text": "Article $$51\\text{A}(f)$$", "isCorrect": false}, {"id": "D", "text": "Article $$51\\text{A}(h)$$", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(e)$$ mandates citizens to promote harmony and brotherhood and to renounce practices derogatory to the dignity of women.'
    ),
    (
        'a1130000-0000-0000-0000-000000000038'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'To value and preserve the rich heritage of the country''s composite culture is stated in:',
        '[{"id": "A", "text": "Article $$51\\text{A}(f)$$", "isCorrect": true}, {"id": "B", "text": "Article $$51\\text{A}(c)$$", "isCorrect": false}, {"id": "C", "text": "Article $$51\\text{A}(d)$$", "isCorrect": false}, {"id": "D", "text": "Article $$51\\text{A}(g)$$", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(f)$$ mandates every citizen to value and preserve the rich heritage of our composite culture.'
    ),
    (
        'a1130000-0000-0000-0000-000000000039'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'To safeguard public property and abjure violence is a Fundamental Duty under:',
        '[{"id": "A", "text": "Article $$51\\text{A}(i)$$", "isCorrect": true}, {"id": "B", "text": "Article $$51\\text{A}(j)$$", "isCorrect": false}, {"id": "C", "text": "Article $$51\\text{A}(k)$$", "isCorrect": false}, {"id": "D", "text": "Article $$51\\text{A}(a)$$", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(i)$$ directs citizens to safeguard public property and to abjure violence.'
    ),
    (
        'a1130000-0000-0000-0000-000000000040'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'Which Directive Principle was substantially amended by the $$86\text{th}$$ Constitutional Amendment Act of $$2002$$?',
        '[{"id": "A", "text": "Article $$45$$ (changed to early childhood care and education below age six)", "isCorrect": true}, {"id": "B", "text": "Article $$41$$ (right to work)", "isCorrect": false}, {"id": "C", "text": "Article $$43$$ (living wages)", "isCorrect": false}, {"id": "D", "text": "Article $$39$$ (distribution of resources)", "isCorrect": false}]',
        'A',
        'When free education was made a Fundamental Right under Article 21A, the 86th Amendment substituted Article 45 to focus on early childhood care and education for all children until age six.'
    ),
    (
        'a1130000-0000-0000-0000-000000000041'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'In the election of the President of India, how is the vote value of an elected Member of a State Legislative Assembly (MLA) calculated?',
        '[{"id": "A", "text": "$$\\frac{\\text{Total Population of State}}{\\text{Total Elected MLAs of State} \\times 1000}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{\\text{Total Population of State}}{\\text{Total Elected MLAs of State}}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{\\text{Total Valid Electors}}{\\text{Number of Districts}}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{\\text{Total MPs}}{\\text{Total MLAs} \\times 100}$$", "isCorrect": false}]',
        'A',
        'Under Article $$55(2)(a)$$, the vote value of an elected MLA is: Total Population of State / (Total elected members of Assembly * 1000).'
    ),
    (
        'a1130000-0000-0000-0000-000000000042'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Which census population is currently used to determine the value of votes of MLAs and MPs in Presidential elections until after the year 2026?',
        '[{"id": "A", "text": "$$1971\\text{ Census}$$ (fixed by the $$84\\text{th}$$ Amendment Act)", "isCorrect": true}, {"id": "B", "text": "$$2001\\text{ Census}$$", "isCorrect": false}, {"id": "C", "text": "$$2011\\text{ Census}$$", "isCorrect": false}, {"id": "D", "text": "$$1991\\text{ Census}$$", "isCorrect": false}]',
        'A',
        'The $$84\text{th}$$ Amendment Act ($$2001$$) froze the census population for calculating presidential vote values to the $$1971$$ Census until the first census taken after $$2026$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000043'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who is the longest serving Prime Minister of India?',
        '[{"id": "A", "text": "Jawaharlal Nehru ($$1947 - 1964$$)", "isCorrect": true}, {"id": "B", "text": "Indira Gandhi", "isCorrect": false}, {"id": "C", "text": "Narendra Modi", "isCorrect": false}, {"id": "D", "text": "Manmohan Singh", "isCorrect": false}]',
        'A',
        'Jawaharlal Nehru was Prime Minister for 16 years and 286 days from August 15, $$1947$$ until his demise on May 27, $$1964$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000044'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$74(1)$$, as amended by the $$44\text{th}$$ Constitutional Amendment Act ($$1978$$), what power does the President possess regarding advice given by the Council of Ministers?',
        '[{"id": "A", "text": "The President may require the Council of Ministers to reconsider such advice once, but is bound by the reconsidered advice", "isCorrect": true}, {"id": "B", "text": "The President may veto the advice indefinitely", "isCorrect": false}, {"id": "C", "text": "The President can reject ministerial advice twice before accepting", "isCorrect": false}, {"id": "D", "text": "The President has absolute power to disregard advice without explanation", "isCorrect": false}]',
        'A',
        'The 44th Amendment inserted a proviso to Article $$74(1)$$ allowing the President to send back advice for reconsideration once; but after reconsideration, the President must act in accordance with it.'
    ),
    (
        'a1130000-0000-0000-0000-000000000045'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who administers the oath of office to the President of India under Article $$60$$?',
        '[{"id": "A", "text": "The Chief Justice of India (or senior-most Supreme Court Judge)", "isCorrect": true}, {"id": "B", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "C", "text": "The Vice-President of India", "isCorrect": false}, {"id": "D", "text": "The outgoing President", "isCorrect": false}]',
        'A',
        'Under Article $$60$$, the oath of office to the President is administered by the Chief Justice of India, or in his absence, the senior-most judge of the Supreme Court.'
    ),
    (
        'a1130000-0000-0000-0000-000000000046'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'To whom does the President of India address his letter of resignation?',
        '[{"id": "A", "text": "The Vice-President of India", "isCorrect": true}, {"id": "B", "text": "The Chief Justice of India", "isCorrect": false}, {"id": "C", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "D", "text": "The Speaker of the Lok Sabha", "isCorrect": false}]',
        'A',
        'Under Article $$56(1)(a)$$, the President may resign his office by writing under his hand addressed to the Vice-President (who forthwith communicates it to the Speaker of Lok Sabha).'
    ),
    (
        'a1130000-0000-0000-0000-000000000047'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'Can a person who is not a member of either House of Parliament be appointed as a Minister or Prime Minister?',
        '[{"id": "A", "text": "Yes, but he must get elected to either House of Parliament within $$6\\text{ consecutive months}$$", "isCorrect": true}, {"id": "B", "text": "No, prior membership of Parliament is mandatory", "isCorrect": false}, {"id": "C", "text": "Yes, but only as a Minister of State without portfolio", "isCorrect": false}, {"id": "D", "text": "Yes, provided the Supreme Court issues an executive waiver", "isCorrect": false}]',
        'A',
        'Under Article $$75(5)$$, a minister who for any period of 6 consecutive months is not a member of either House of Parliament ceases to be a minister.'
    ),
    (
        'a1130000-0000-0000-0000-000000000048'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who was the first acting President of India following the death of President Dr. Zakir Husain in $$1969$$?',
        '[{"id": "A", "text": "V.V. Giri (Varahagiri Venkatagiri)", "isCorrect": true}, {"id": "B", "text": "Justice M. Hidayatullah", "isCorrect": false}, {"id": "C", "text": "B.D. Jatti", "isCorrect": false}, {"id": "D", "text": "Gopal Swarup Pathak", "isCorrect": false}]',
        'A',
        'Vice-President V.V. Giri became acting President upon Zakir Husain''s death in May $$1969$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000049'::uuid,
        'Union Executive',
        'MEDIUM',
        'REMEMBER',
        'Article $$77$$ prescribes that all executive action of the Government of India shall be expressed to be taken in the name of:',
        '[{"id": "A", "text": "The President of India", "isCorrect": true}, {"id": "B", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "C", "text": "The Cabinet Secretary", "isCorrect": false}, {"id": "D", "text": "The Parliament of India", "isCorrect": false}]',
        'A',
        'Article $$77(1)$$ states: ''All executive action of the Government of India shall be expressed to be taken in the name of the President''.'
    ),
    (
        'a1130000-0000-0000-0000-000000000050'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$78$$, which constitutional functionary serves as the principal channel of communication between the President and the Council of Ministers?',
        '[{"id": "A", "text": "The Prime Minister", "isCorrect": true}, {"id": "B", "text": "The Cabinet Secretary", "isCorrect": false}, {"id": "C", "text": "The Home Minister", "isCorrect": false}, {"id": "D", "text": "The Speaker of Lok Sabha", "isCorrect": false}]',
        'A',
        'Article $$78$$ makes it the duty of the Prime Minister to communicate to the President all decisions of the Council of Ministers.'
    ),
    (
        'a1130000-0000-0000-0000-000000000051'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the maximum life of the Lok Sabha during normal times?',
        '[{"id": "A", "text": "$$5\\text{ years}$$ from the date appointed for its first meeting", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$4\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "Indefinite until dissolved", "isCorrect": false}]',
        'A',
        'Under Article $$83(2)$$, the Lok Sabha continues for 5 years from the date appointed for its first meeting, unless dissolved earlier.'
    ),
    (
        'a1130000-0000-0000-0000-000000000052'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'During a National Emergency under Article 352, by what period can the life of the Lok Sabha be extended by Parliament by law?',
        '[{"id": "A", "text": "For a period not exceeding one year at a time", "isCorrect": true}, {"id": "B", "text": "For up to two years at a time", "isCorrect": false}, {"id": "C", "text": "For six months at a time", "isCorrect": false}, {"id": "D", "text": "Indefinitely without limit", "isCorrect": false}]',
        'A',
        'Under the proviso to Article $$83(2)$$, Parliament can extend the term of Lok Sabha for a period not exceeding 1 year at a time, and not extending beyond 6 months after emergency ceases.'
    ),
    (
        'a1130000-0000-0000-0000-000000000053'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'What is a ''Guillotine'' in Indian parliamentary legislative procedure?',
        '[{"id": "A", "text": "Putting all outstanding budgetary demands to vote without discussion due to lack of time", "isCorrect": true}, {"id": "B", "text": "Suspension of an unruly MP for the remainder of the session", "isCorrect": false}, {"id": "C", "text": "Expunging unparliamentary remarks from the official Hansard record", "isCorrect": false}, {"id": "D", "text": "Adjourning the House sine die before the scheduled date", "isCorrect": false}]',
        'A',
        'On the last day allotted for the discussion on demands for grants, the Speaker applies the ''guillotine'', putting all remaining demands to vote without further debate.'
    ),
    (
        'a1130000-0000-0000-0000-000000000054'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'Which type of question in Parliament requires an oral answer on the floor of the House and can be followed by supplementary questions?',
        '[{"id": "A", "text": "Starred Question (marked with an asterisk)", "isCorrect": true}, {"id": "B", "text": "Unstarred Question", "isCorrect": false}, {"id": "C", "text": "Short Notice Question", "isCorrect": false}, {"id": "D", "text": "Private Member Question", "isCorrect": false}]',
        'A',
        'A Starred Question is marked with an asterisk and requires an oral answer from the minister, allowing supplementary questions. Unstarred questions require written answers.'
    ),
    (
        'a1130000-0000-0000-0000-000000000055'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'Parliamentary privileges enjoyed by the Houses of Parliament and their members are governed by which Article?',
        '[{"id": "A", "text": "Article $$105$$", "isCorrect": true}, {"id": "B", "text": "Article $$194$$", "isCorrect": false}, {"id": "C", "text": "Article $$110$$", "isCorrect": false}, {"id": "D", "text": "Article $$120$$", "isCorrect": false}]',
        'A',
        'Article $$105$$ defines the powers, privileges, and immunities of Parliament and its members. Article $$194$$ provides the corresponding privileges for State Legislatures.'
    ),
    (
        'a1130000-0000-0000-0000-000000000056'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'Can a No-Confidence Motion be introduced in the Rajya Sabha?',
        '[{"id": "A", "text": "No, it can be moved only in the Lok Sabha", "isCorrect": true}, {"id": "B", "text": "Yes, if supported by 50 Rajya Sabha members", "isCorrect": false}, {"id": "C", "text": "Yes, but only against individual Ministers", "isCorrect": false}, {"id": "D", "text": "Yes, during a joint session", "isCorrect": false}]',
        'A',
        'Under Article $$75(3)$$, the Council of Ministers is collectively responsible solely to the Lok Sabha; hence a No-Confidence Motion can only be introduced and voted upon in the Lok Sabha.'
    ),
    (
        'a1130000-0000-0000-0000-000000000057'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'How many members must support a No-Confidence Motion in the Lok Sabha for it to be admitted for debate?',
        '[{"id": "A", "text": "At least $$50\\text{ members}$$ of the House", "isCorrect": true}, {"id": "B", "text": "At least $$25\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "One-tenth ($$\\frac{1}{10}\\text{th}$$) of the members", "isCorrect": false}, {"id": "D", "text": "At least $$100\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Under Rule 198 of Lok Sabha Rules of Procedure, a motion of no-confidence requires the support of at least $$50$$ members to be granted leave.'
    ),
    (
        'a1130000-0000-0000-0000-000000000058'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the term of the Committee on Public Undertakings ($$\text{COPU}$$), and how many members does it have?',
        '[{"id": "A", "text": "One year term, with $$22\\text{ members (15 Lok Sabha and 7 Rajya Sabha)}$$", "isCorrect": true}, {"id": "B", "text": "Two year term, with $$30\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "One year term, with $$15\\text{ members}$$", "isCorrect": false}, {"id": "D", "text": "Five year term, with $$25\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Created in $$1964$$ on the recommendation of the Krishna Menon Committee, COPU consists of $$22$$ members ($$15$$ from LS, $$7$$ from RS) serving a 1-year term.'
    ),
    (
        'a1130000-0000-0000-0000-000000000059'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'What is an ''Appropriation Bill'' passed under Article $$114$$ of the Constitution?',
        '[{"id": "A", "text": "A statutory bill that legally authorizes the government to withdraw money from the Consolidated Fund of India", "isCorrect": true}, {"id": "B", "text": "A bill that alters direct tax rates", "isCorrect": false}, {"id": "C", "text": "A bill sanctioning loans from foreign multilateral agencies", "isCorrect": false}, {"id": "D", "text": "A resolution dividing tax proceeds with the states", "isCorrect": false}]',
        'A',
        'Article $$114(3)$$ provides that no money shall be withdrawn from the Consolidated Fund of India except under appropriation made by law (the Appropriation Bill).'
    ),
    (
        'a1130000-0000-0000-0000-000000000060'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'What is a ''Lame Duck'' session of Parliament?',
        '[{"id": "A", "text": "The final session of the existing Lok Sabha held after the election of a new Lok Sabha", "isCorrect": true}, {"id": "B", "text": "A session disrupted continuously by protests without any business conducted", "isCorrect": false}, {"id": "C", "text": "A special emergency session summoned during war", "isCorrect": false}, {"id": "D", "text": "A joint sitting of Parliament that fails to achieve a quorum", "isCorrect": false}]',
        'A',
        'A Lame Duck session refers to the last session of the old Lok Sabha after elections for a new Lok Sabha have concluded. Those old members who could not get re-elected are called ''lame ducks''.'
    ),
    (
        'a1130000-0000-0000-0000-000000000061'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'Which Article of the Constitution contains special provisions with respect to the State of Nagaland, including customary law protection?',
        '[{"id": "A", "text": "Article $$371\\text{A}$$", "isCorrect": true}, {"id": "B", "text": "Article $$371\\text{B}$$", "isCorrect": false}, {"id": "C", "text": "Article $$371\\text{C}$$", "isCorrect": false}, {"id": "D", "text": "Article $$371\\text{D}$$", "isCorrect": false}]',
        'A',
        'Article $$371\text{A}$$ provides special provisions for Nagaland, protecting religious and social practices, customary law, and land ownership.'
    ),
    (
        'a1130000-0000-0000-0000-000000000062'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Special provisions regarding the Hyderabad-Karnataka region were inserted as Article $$371\text{J}$$ by which Amendment Act?',
        '[{"id": "A", "text": "$$98\\text{th}$$ Constitutional Amendment Act $$2012$$", "isCorrect": true}, {"id": "B", "text": "$$91\\text{st}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}, {"id": "C", "text": "$$95\\text{th}$$ Constitutional Amendment Act $$2009$$", "isCorrect": false}, {"id": "D", "text": "$$89\\text{th}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}]',
        'A',
        'The $$98\text{th}$$ Constitutional Amendment Act of $$2012$$ inserted Article $$371\text{J}$$ providing special provisions for the Hyderabad-Karnataka region.'
    ),
    (
        'a1130000-0000-0000-0000-000000000063'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Who is the executive head of a State in India?',
        '[{"id": "A", "text": "The Governor", "isCorrect": true}, {"id": "B", "text": "The Chief Minister", "isCorrect": false}, {"id": "C", "text": "The Chief Secretary", "isCorrect": false}, {"id": "D", "text": "The President", "isCorrect": false}]',
        'A',
        'Under Article $$154(1)$$, the executive power of the State is vested in the Governor, who is the de jure (constitutional) head, while the Chief Minister is de facto (real) executive head.'
    ),
    (
        'a1130000-0000-0000-0000-000000000064'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'Who determines the qualifications required for appointment as a Member of the State Legislative Assembly?',
        '[{"id": "A", "text": "The Constitution of India and Parliament by law (Representation of the People Act)", "isCorrect": true}, {"id": "B", "text": "The State Legislative Assembly unilaterally", "isCorrect": false}, {"id": "C", "text": "The Governor in his sole discretion", "isCorrect": false}, {"id": "D", "text": "The Chief Minister in cabinet", "isCorrect": false}]',
        'A',
        'Qualifications are prescribed by Article $$173$$ and statutory disqualifications by the Representation of the People Act, $$1951$$, enacted by Parliament.'
    ),
    (
        'a1130000-0000-0000-0000-000000000065'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'If any question arises as to whether an MLA has become subject to disqualification under Article 191(1) (other than defection), whose decision is final?',
        '[{"id": "A", "text": "The Governor, acting according to the opinion of the Election Commission of India (Article $$192$$)", "isCorrect": true}, {"id": "B", "text": "The Speaker of the Legislative Assembly unilaterally", "isCorrect": false}, {"id": "C", "text": "The High Court of the State", "isCorrect": false}, {"id": "D", "text": "The Chief Minister", "isCorrect": false}]',
        'A',
        'Under Article $$192$$, questions regarding disqualifications (office of profit, unsound mind, citizenship) are referred to the Governor, who must obtain and act on the opinion of the Election Commission.'
    ),
    (
        'a1130000-0000-0000-0000-000000000066'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'Under the Tenth Schedule (Anti-Defection Law), who decides the disqualification of a member of a State Legislative Assembly?',
        '[{"id": "A", "text": "The Speaker of the State Legislative Assembly", "isCorrect": true}, {"id": "B", "text": "The Governor of the State", "isCorrect": false}, {"id": "C", "text": "The State Election Commission", "isCorrect": false}, {"id": "D", "text": "The High Court directly", "isCorrect": false}]',
        'A',
        'Paragraph 6 of the Tenth Schedule provides that questions of defection are decided by the Chairman or Speaker of the respective House, subject to judicial review (Kihoto Hollohan case).'
    ),
    (
        'a1130000-0000-0000-0000-000000000067'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Which is the minimum strength of a State Legislative Council as per Article $$171$$?',
        '[{"id": "A", "text": "$$40\\text{ members}$$", "isCorrect": true}, {"id": "B", "text": "$$50\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "$$60\\text{ members}$$", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Article $$171(1)$$ provides that the total number of members in the Legislative Council shall not be less than 40.'
    ),
    (
        'a1130000-0000-0000-0000-000000000068'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'In the landmark judgment of S.R. Bommai v. Union of India ($$1994$$), the Supreme Court ruled that:',
        '[{"id": "A", "text": "Presidential proclamation under Article 356 is subject to judicial review, and the Assembly cannot be dissolved until Parliament approves it", "isCorrect": true}, {"id": "B", "text": "The Governor can dismiss any Chief Minister without a floor test", "isCorrect": false}, {"id": "C", "text": "State autonomy is absolute and emergency cannot be declared in border states", "isCorrect": false}, {"id": "D", "text": "Secularism is not a part of the basic structure", "isCorrect": false}]',
        'A',
        'In S.R. Bommai ($$1994$$), the Supreme Court held that Article 356 is not immune from judicial review, and that floor tests on the assembly floor are mandatory to test majority.'
    ),
    (
        'a1130000-0000-0000-0000-000000000069'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Who summons and prorogues the sessions of the State Legislature?',
        '[{"id": "A", "text": "The Governor of the State (Article $$174$$)", "isCorrect": true}, {"id": "B", "text": "The Speaker of the State Legislative Assembly", "isCorrect": false}, {"id": "C", "text": "The Chief Minister", "isCorrect": false}, {"id": "D", "text": "The President of India", "isCorrect": false}]',
        'A',
        'Under Article $$174(1)$$, the Governor summons the House or each House of the Legislature of the State from time to time.'
    ),
    (
        'a1130000-0000-0000-0000-000000000070'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'The contingency fund of a State is placed at the disposal of:',
        '[{"id": "A", "text": "The Governor of the State", "isCorrect": true}, {"id": "B", "text": "The Chief Minister of the State", "isCorrect": false}, {"id": "C", "text": "The State Finance Minister", "isCorrect": false}, {"id": "D", "text": "The Comptroller and Auditor General", "isCorrect": false}]',
        'A',
        'Under Article $$267(2)$$, the Contingency Fund of the State is placed at the disposal of the Governor to enable advances to be made for unforeseen expenditure.'
    ),
    (
        'a1130000-0000-0000-0000-000000000071'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'The National Judicial Appointments Commission ($$\text{NJAC}$$) created by the $$99\text{th}$$ Amendment was struck down as unconstitutional in $$2015$$ by the Supreme Court on the ground that it violated:',
        '[{"id": "A", "text": "Judicial independence, which is an inviolable basic structure of the Constitution", "isCorrect": true}, {"id": "B", "text": "Federalism and rights of State Bar Councils", "isCorrect": false}, {"id": "C", "text": "The Preamble''s socialist commitments", "isCorrect": false}, {"id": "D", "text": "The financial autonomy of the Consolidated Fund of India", "isCorrect": false}]',
        'A',
        'In October $$2015$$, a 5-judge Constitution Bench in Supreme Court Advocates-on-Record Association (Fourth Judges Case) struck down the 99th Amendment and NJAC Act, upholding judicial independence.'
    ),
    (
        'a1130000-0000-0000-0000-000000000072'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$130$$, what is the permanent seat of the Supreme Court of India, and who can appoint other places as seats?',
        '[{"id": "A", "text": "Delhi, and the Chief Justice of India with the approval of the President", "isCorrect": true}, {"id": "B", "text": "New Delhi, and Parliament by law alone", "isCorrect": false}, {"id": "C", "text": "Kolkata, and the President of India", "isCorrect": false}, {"id": "D", "text": "Any city designated by a simple majority in Lok Sabha", "isCorrect": false}]',
        'A',
        'Article $$130$$ declares Delhi as the seat of the Supreme Court, but authorizes the Chief Justice of India, with the approval of the President, to appoint other seats.'
    ),
    (
        'a1130000-0000-0000-0000-000000000073'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'The concept of ''Curative Petition'' in Indian judicial procedure was evolved by the Supreme Court in which landmark case?',
        '[{"id": "A", "text": "Rupa Ashok Hurra v. Ashok Hurra ($$2002$$)", "isCorrect": true}, {"id": "B", "text": "Kesavananda Bharati v. State of Kerala ($$1973$$)", "isCorrect": false}, {"id": "C", "text": "Maneka Gandhi v. Union of India ($$1978$$)", "isCorrect": false}, {"id": "D", "text": "Indra Sawhney v. Union of India ($$1992$$)", "isCorrect": false}]',
        'A',
        'The Supreme Court in Rupa Ashok Hurra ($$2002$$) evolved the concept of Curative Petition to cure gross miscarriage of justice after a review petition has been dismissed.'
    ),
    (
        'a1130000-0000-0000-0000-000000000074'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Administrative Tribunals for adjudicating service disputes of central government employees are established under:',
        '[{"id": "A", "text": "Article $$323\\text{A}$$ (Central Administrative Tribunal - CAT)", "isCorrect": true}, {"id": "B", "text": "Article $$323\\text{B}$$", "isCorrect": false}, {"id": "C", "text": "Article $$262$$", "isCorrect": false}, {"id": "D", "text": "Article $$280$$", "isCorrect": false}]',
        'A',
        'Article $$323\text{A}$$, added by the $$42\text{nd}$$ Amendment ($$1976$$), empowers Parliament to establish Administrative Tribunals (resulting in the Administrative Tribunals Act $$1985$$).'
    ),
    (
        'a1130000-0000-0000-0000-000000000075'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Are the decisions of the Central Administrative Tribunal ($$\text{CAT}$$) subject to judicial review by High Courts?',
        '[{"id": "A", "text": "Yes, before a Division Bench of the High Court under Articles 226/227 (L. Chandra Kumar case)", "isCorrect": true}, {"id": "B", "text": "No, CAT orders can only be appealed directly to the Supreme Court", "isCorrect": false}, {"id": "C", "text": "No, CAT is completely immune from all judicial review", "isCorrect": false}, {"id": "D", "text": "Yes, but only with prior permission from the Law Ministry", "isCorrect": false}]',
        'A',
        'In L. Chandra Kumar ($$1997$$), a 7-judge bench ruled that orders of administrative tribunals are subject to examination by a Division Bench of the respective High Court.'
    ),
    (
        'a1130000-0000-0000-0000-000000000076'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Lok Adalats in India have been given statutory status under which legislation?',
        '[{"id": "A", "text": "Legal Services Authorities Act, $$1987$$", "isCorrect": true}, {"id": "B", "text": "Arbitration and Conciliation Act, $$1996$$", "isCorrect": false}, {"id": "C", "text": "Code of Civil Procedure, $$1908$$", "isCorrect": false}, {"id": "D", "text": "Consumer Protection Act, $$1986$$", "isCorrect": false}]',
        'A',
        'The Legal Services Authorities Act, $$1987$$ gave statutory status to Lok Adalats, making their awards final, binding, and non-appealable.'
    ),
    (
        'a1130000-0000-0000-0000-000000000077'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Which is the only Union Territory in India that has had its own independent High Court since $$1966$$?',
        '[{"id": "A", "text": "Delhi", "isCorrect": true}, {"id": "B", "text": "Puducherry", "isCorrect": false}, {"id": "C", "text": "Chandigarh", "isCorrect": false}, {"id": "D", "text": "Andaman and Nicobar Islands", "isCorrect": false}]',
        'A',
        'Delhi is the only Union Territory with its own separate High Court, established in $$1966$$ (Jammu & Kashmir is also a UT with a High Court shared with Ladakh since 2019).'
    ),
    (
        'a1130000-0000-0000-0000-000000000078'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'Under Article $$214$$, can Parliament establish a common High Court for two or more States by law?',
        '[{"id": "A", "text": "Yes, under Article $$231$$", "isCorrect": true}, {"id": "B", "text": "No, each State must have its own High Court", "isCorrect": false}, {"id": "C", "text": "Only during a National Emergency", "isCorrect": false}, {"id": "D", "text": "Only if approved by the Governors of both States", "isCorrect": false}]',
        'A',
        'Article $$231$$ empowers Parliament by law to establish a common High Court for two or more States or two or more States and a Union Territory (e.g. Punjab and Haryana High Court).'
    ),
    (
        'a1130000-0000-0000-0000-000000000079'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Can the salaries and allowances of Supreme Court and High Court judges be reduced by Parliament during their tenure?',
        '[{"id": "A", "text": "No, except during a Proclamation of Financial Emergency under Article $$360$$", "isCorrect": true}, {"id": "B", "text": "Yes, by an ordinary financial bill passed by simple majority", "isCorrect": false}, {"id": "C", "text": "Yes, if recommended by the Finance Commission", "isCorrect": false}, {"id": "D", "text": "Never under any circumstance, even during Financial Emergency", "isCorrect": false}]',
        'A',
        'Judges'' salaries cannot be varied to their disadvantage after appointment, except when a Financial Emergency under Article $$360(4)(b)$$ is in operation.'
    ),
    (
        'a1130000-0000-0000-0000-000000000080'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Who was the first woman Chief Justice of a State High Court in India?',
        '[{"id": "A", "text": "Justice Leila Seth (Himachal Pradesh High Court, $$1991$$)", "isCorrect": true}, {"id": "B", "text": "Justice Fathima Beevi", "isCorrect": false}, {"id": "C", "text": "Justice Anna Chandy", "isCorrect": false}, {"id": "D", "text": "Justice Ruma Pal", "isCorrect": false}]',
        'A',
        'Justice Leila Seth was the first woman to become Chief Justice of a State High Court, serving at the Himachal Pradesh High Court in $$1991$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000081'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which Schedule was added to the Constitution by the $$74\text{th}$$ Constitutional Amendment Act $$1992$$?',
        '[{"id": "A", "text": "Twelfth Schedule ($$12\\text{th}$$ Schedule, containing $$18\\text{ functional items}$$)", "isCorrect": true}, {"id": "B", "text": "Eleventh Schedule", "isCorrect": false}, {"id": "C", "text": "Tenth Schedule", "isCorrect": false}, {"id": "D", "text": "Ninth Schedule", "isCorrect": false}]',
        'A',
        'The $$74\text{th}$$ Amendment Act added Part IX-A and the Twelfth Schedule containing $$18$$ functional items for Municipalities.'
    ),
    (
        'a1130000-0000-0000-0000-000000000082'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$243\text{Q}$$, what are the three types of Municipalities constituted in every State?',
        '[{"id": "A", "text": "Nagar Panchayat (transitional area), Municipal Council (smaller urban area), and Municipal Corporation (larger urban area)", "isCorrect": true}, {"id": "B", "text": "Zila Parishad, Panchayat Samiti, and Gram Panchayat", "isCorrect": false}, {"id": "C", "text": "Notified Area Committee, Town Area Committee, and Cantonment Board", "isCorrect": false}, {"id": "D", "text": "Metropolitan Council, District Council, and City Council", "isCorrect": false}]',
        'A',
        'Article $$243\text{Q}$$ provides for 3 tiers of urban local bodies: Nagar Panchayat for transitional areas, Municipal Council for smaller urban areas, and Municipal Corporation for larger urban areas.'
    ),
    (
        'a1130000-0000-0000-0000-000000000083'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Cantonment Boards in military stations are set up and administered under which Union Ministry?',
        '[{"id": "A", "text": "Ministry of Defence (under Cantonments Act, $$2006$$)", "isCorrect": true}, {"id": "B", "text": "Ministry of Housing and Urban Affairs", "isCorrect": false}, {"id": "C", "text": "Ministry of Home Affairs", "isCorrect": false}, {"id": "D", "text": "Concerned State Government Urban Development Department", "isCorrect": false}]',
        'A',
        'Cantonment boards are established under central legislation (Cantonments Act $$2006$$) and function under the administrative control of the Union Ministry of Defence.'
    ),
    (
        'a1130000-0000-0000-0000-000000000084'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'A Metropolitan Planning Committee ($$\text{MPC}$$) is constituted under which Article for every metropolitan area with a population of 10 lakhs or more?',
        '[{"id": "A", "text": "Article $$243\\text{ZE}$$", "isCorrect": true}, {"id": "B", "text": "Article $$243\\text{ZD}$$", "isCorrect": false}, {"id": "C", "text": "Article $$243\\text{S}$$", "isCorrect": false}, {"id": "D", "text": "Article $$243\\text{T}$$", "isCorrect": false}]',
        'A',
        'Article $$243\text{ZE}$$ provides for the constitution of a Metropolitan Planning Committee to prepare a draft development plan for metropolitan areas.'
    ),
    (
        'a1130000-0000-0000-0000-000000000085'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Wards Committees are required to be constituted in municipalities having a population of:',
        '[{"id": "A", "text": "$$3\\text{ lakhs}$$ or more (Article $$243\\text{S}$$)", "isCorrect": true}, {"id": "B", "text": "$$1\\text{ lakh}$$ or more", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ lakhs}$$ or more", "isCorrect": false}, {"id": "D", "text": "$$10\\text{ lakhs}$$ or more", "isCorrect": false}]',
        'A',
        'Under Article $$243\text{S}$$, Wards Committees consisting of one or more wards must be constituted in all municipalities having a population of 3 lakhs or more.'
    ),
    (
        'a1130000-0000-0000-0000-000000000086'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'Can courts interfere in electoral matters concerning elections to Panchayats under Article $$243\text{O}$$?',
        '[{"id": "A", "text": "No, the validity of any law relating to delimitation or allotment of seats cannot be questioned in any court except via election petition", "isCorrect": true}, {"id": "B", "text": "Yes, the High Court has plenary writ power to cancel any local election", "isCorrect": false}, {"id": "C", "text": "Yes, civil courts can grant stay orders on candidate nominations", "isCorrect": false}, {"id": "D", "text": "Yes, through interim orders of the District Court", "isCorrect": false}]',
        'A',
        'Article $$243\text{O}$$ bars interference by courts in electoral matters of Panchayats, specifying that elections can only be questioned by an election petition.'
    ),
    (
        'a1130000-0000-0000-0000-000000000087'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Which of the following is a compulsory (mandatory) provision under the $$73\text{rd}$$ Constitutional Amendment Act?',
        '[{"id": "A", "text": "Establishment of a State Election Commission and State Finance Commission", "isCorrect": true}, {"id": "B", "text": "Giving representation to MPs and MLAs in Panchayats", "isCorrect": false}, {"id": "C", "text": "Providing reservation for Backward Classes (OBCs)", "isCorrect": false}, {"id": "D", "text": "Devolving financial powers to levy and collect octroi", "isCorrect": false}]',
        'A',
        'Establishing a State Election Commission, State Finance Commission, Gram Sabha, 3-tier structure, and reservations for SCs/STs/women are mandatory provisions. OBC reservations and tax levies are voluntary.'
    ),
    (
        'a1130000-0000-0000-0000-000000000088'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which was the second State in India to adopt the Panchayati Raj system after Rajasthan in $$1959$$?',
        '[{"id": "A", "text": "Andhra Pradesh", "isCorrect": true}, {"id": "B", "text": "Tamil Nadu", "isCorrect": false}, {"id": "C", "text": "Karnataka", "isCorrect": false}, {"id": "D", "text": "Maharashtra", "isCorrect": false}]',
        'A',
        'Andhra Pradesh was the second state to introduce the Panchayati Raj system in November $$1959$$.'
    ),
    (
        'a1130000-0000-0000-0000-000000000089'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'The G.V.K. Rao Committee was appointed by the Planning Commission in $$1985$$ to examine:',
        '[{"id": "A", "text": "Administrative Arrangements for Rural Development and Poverty Alleviation Programmes", "isCorrect": true}, {"id": "B", "text": "Urban infrastructure funding", "isCorrect": false}, {"id": "C", "text": "Center-State financial relations", "isCorrect": false}, {"id": "D", "text": "Judicial appointments to tribunals", "isCorrect": false}]',
        'A',
        'The G.V.K. Rao Committee ($$1985$$) examined rural development, famously observing that development administration was becoming bureaucratized and divorced from Panchayati Raj (''grass without roots'').'
    ),
    (
        'a1130000-0000-0000-0000-000000000090'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Under PESA ($$1996$$), which institution has mandatory ownership of minor forest produce and prior consultation rights for land acquisition in Scheduled Areas?',
        '[{"id": "A", "text": "The Gram Sabha", "isCorrect": true}, {"id": "B", "text": "The State Forest Corporation", "isCorrect": false}, {"id": "C", "text": "The District Collector exclusively", "isCorrect": false}, {"id": "D", "text": "The Union Ministry of Environment", "isCorrect": false}]',
        'A',
        'Under PESA $$1996$$, the Gram Sabha is endowed specifically with ownership of minor forest produce and mandatory prior consultation before land acquisition.'
    ),
    (
        'a1130000-0000-0000-0000-000000000091'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'In which historic case did the Supreme Court propound the ''Basic Structure Doctrine'' limiting Parliament''s power under Article 368?',
        '[{"id": "A", "text": "Kesavananda Bharati v. State of Kerala ($$1973$$)", "isCorrect": true}, {"id": "B", "text": "Golaknath v. State of Punjab ($$1967$$)", "isCorrect": false}, {"id": "C", "text": "Sankari Prasad v. Union of India ($$1951$$)", "isCorrect": false}, {"id": "D", "text": "Sajjan Singh v. State of Rajasthan ($$1965$$)", "isCorrect": false}]',
        'A',
        'By a 7:6 majority in the historic 13-judge bench in Kesavananda Bharati (April 24, $$1973$$), the Supreme Court ruled that Parliament cannot alter the ''Basic Structure'' of the Constitution.'
    ),
    (
        'a1130000-0000-0000-0000-000000000092'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following is NOT an element of the ''Basic Structure'' of the Indian Constitution?',
        '[{"id": "A", "text": "Uncontrolled power of Parliament to amend the Constitution without judicial review", "isCorrect": true}, {"id": "B", "text": "Secular character of the Constitution", "isCorrect": false}, {"id": "C", "text": "Separation of powers between legislature, executive, and judiciary", "isCorrect": false}, {"id": "D", "text": "Free and fair democratic elections", "isCorrect": false}]',
        'A',
        'Unlimited amending power was explicitly rejected as unconstitutional in Minerva Mills ($$1980$$), since limited amending power is itself a basic feature.'
    ),
    (
        'a1130000-0000-0000-0000-000000000093'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment added the Ninth Schedule to shield land reform laws from judicial review?',
        '[{"id": "A", "text": "$$1\\text{st}$$ Constitutional Amendment Act $$1951$$", "isCorrect": true}, {"id": "B", "text": "$$7\\text{th}$$ Constitutional Amendment Act $$1956$$", "isCorrect": false}, {"id": "C", "text": "$$24\\text{th}$$ Constitutional Amendment Act $$1971$$", "isCorrect": false}, {"id": "D", "text": "$$42\\text{nd}$$ Constitutional Amendment Act $$1976$$", "isCorrect": false}]',
        'A',
        'The $$1\text{st}$$ Amendment Act ($$1951$$) added the Ninth Schedule and Article $$31\text{B}$$ to protect land reform legislation from judicial review.'
    ),
    (
        'a1130000-0000-0000-0000-000000000094'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'In I.R. Coelho v. State of Tamil Nadu ($$2007$$), the Supreme Court held that laws inserted into the Ninth Schedule are subject to judicial review if placed after:',
        '[{"id": "A", "text": "April $$24, 1973$$ (the date of the Kesavananda Bharati judgment)", "isCorrect": true}, {"id": "B", "text": "January $$26, 1950$$", "isCorrect": false}, {"id": "C", "text": "November $$26, 1949$$", "isCorrect": false}, {"id": "D", "text": "June $$25, 1975$$", "isCorrect": false}]',
        'A',
        'A 9-judge bench in I.R. Coelho ($$2007$$) held that laws included in the 9th Schedule after April 24, $$1973$$ do not enjoy blanket immunity and can be challenged for violating the basic structure.'
    ),
    (
        'a1130000-0000-0000-0000-000000000095'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which is the official language of the Union of India as declared under Article $$343(1)$$?',
        '[{"id": "A", "text": "Hindi in Devanagari script", "isCorrect": true}, {"id": "B", "text": "Hindi and English jointly", "isCorrect": false}, {"id": "C", "text": "Sanskrit in Devanagari script", "isCorrect": false}, {"id": "D", "text": "All 22 languages listed in Eighth Schedule equally", "isCorrect": false}]',
        'A',
        'Article $$343(1)$$ declares that the official language of the Union shall be Hindi in Devanagari script, with international form of Indian numerals.'
    ),
    (
        'a1130000-0000-0000-0000-000000000096'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'How many official languages are currently recognized in the Eighth Schedule of the Constitution?',
        '[{"id": "A", "text": "$$22\\text{ languages}$$", "isCorrect": true}, {"id": "B", "text": "$$14\\text{ languages}$$", "isCorrect": false}, {"id": "C", "text": "$$18\\text{ languages}$$", "isCorrect": false}, {"id": "D", "text": "$$25\\text{ languages}$$", "isCorrect": false}]',
        'A',
        'Originally there were 14 languages in the 8th Schedule. With subsequent amendments (21st, 71st, 92nd Amendments), the count reached 22 languages.'
    ),
    (
        'a1130000-0000-0000-0000-000000000097'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'Which four languages were added to the Eighth Schedule by the $$92\text{nd}$$ Constitutional Amendment Act $$2003$$?',
        '[{"id": "A", "text": "Bodo, Dogri, Maithili, and Santhali (BDMS)", "isCorrect": true}, {"id": "B", "text": "Konkani, Manipuri, Nepali, and Sindhi", "isCorrect": false}, {"id": "C", "text": "Sindhi, Sanskrit, Urdu, and Kashmiri", "isCorrect": false}, {"id": "D", "text": "Assamese, Odia, Marathi, and Gujarati", "isCorrect": false}]',
        'A',
        'The $$92\text{nd}$$ Amendment ($$2003$$) added Bodo, Dogri, Maithili, and Santhali, raising the total count to 22.'
    ),
    (
        'a1130000-0000-0000-0000-000000000098'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment provided $$33\%$$ reservation for women in the Lok Sabha and State Legislative Assemblies (Nari Shakti Vandan Adhiniyam)?',
        '[{"id": "A", "text": "$$106\\text{th}$$ Constitutional Amendment Act $$2023$$", "isCorrect": true}, {"id": "B", "text": "$$105\\text{th}$$ Constitutional Amendment Act $$2021$$", "isCorrect": false}, {"id": "C", "text": "$$104\\text{th}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}, {"id": "D", "text": "$$103\\text{rd}$$ Constitutional Amendment Act $$2019$$", "isCorrect": false}]',
        'A',
        'The $$106\text{th}$$ Constitutional Amendment Act ($$2023$$) reserves one-third of all seats for women in Lok Sabha, State Legislative Assemblies, and Delhi Assembly.'
    ),
    (
        'a1130000-0000-0000-0000-000000000099'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$368$$, an amendment that affects federal provisions (such as election of President or Seventh Schedule lists) requires:',
        '[{"id": "A", "text": "A special majority in each House of Parliament plus ratification by legislatures of not less than half of the States", "isCorrect": true}, {"id": "B", "text": "A simple majority in Parliament and all State Assemblies", "isCorrect": false}, {"id": "C", "text": "Unanimous consent of all 28 State Assemblies", "isCorrect": false}, {"id": "D", "text": "Approval through a nationwide popular referendum", "isCorrect": false}]',
        'A',
        'Article $$368(2)$$ proviso requires special majority in Parliament AND ratification by resolutions passed by the legislatures of not less than one-half of the States.'
    ),
    (
        'a1130000-0000-0000-0000-000000000100'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'Which Constitutional Article provides for the appointment of a Special Officer for Linguistic Minorities by the President?',
        '[{"id": "A", "text": "Article $$350\\text{B}$$", "isCorrect": true}, {"id": "B", "text": "Article $$350\\text{A}$$", "isCorrect": false}, {"id": "C", "text": "Article $$351$$", "isCorrect": false}, {"id": "D", "text": "Article $$347$$", "isCorrect": false}]',
        'A',
        'Article $$350\text{B}$$ was inserted by the 7th Amendment ($$1956$$), providing for a Special Officer for Linguistic Minorities appointed by the President to investigate safeguards.'
    )
) AS v(id, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Indian Polity' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
