-- ============================================================
-- Seed Questions: RRB NTPC - Indian Polity and Governance (Set 2) (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Awareness / General Studies -> Topic: Indian Polity
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX ($$..$$)
-- UUID Range: a1110000-0000-0000-0000-000000000001 to a1110000-0000-0000-0000-000000000100
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

-- Step 2: Insert 100 RRB NTPC Indian Polity Questions (Set 2)
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
        'a1110000-0000-0000-0000-000000000001'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Which British enactment first designated the Governor of Bengal as the ''Governor-General of Bengal''?',
        '[{"id": "A", "text": "Regulating Act of $$1773$$", "isCorrect": true}, {"id": "B", "text": "Pitt''s India Act of $$1784$$", "isCorrect": false}, {"id": "C", "text": "Charter Act of $$1833$$", "isCorrect": false}, {"id": "D", "text": "Government of India Act $$1858$$", "isCorrect": false}]',
        'A',
        'The Regulating Act of $$1773$$ designated the Governor of Bengal as the Governor-General of Bengal, with Lord Warren Hastings being the first.'
    ),
    (
        'a1110000-0000-0000-0000-000000000002'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Under the Regulating Act of $$1773$$, a Supreme Court of Judicature was established in $$1774$$ at:',
        '[{"id": "A", "text": "Calcutta (Fort William)", "isCorrect": true}, {"id": "B", "text": "Bombay", "isCorrect": false}, {"id": "C", "text": "Madras", "isCorrect": false}, {"id": "D", "text": "Allahabad", "isCorrect": false}]',
        'A',
        'The Supreme Court was established at Calcutta in $$1774$$ with Sir Elijah Impey as its first Chief Justice.'
    ),
    (
        'a1110000-0000-0000-0000-000000000003'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'Which Act of the British Parliament ended the commercial monopoly of the East India Company, except for trade in tea and trade with China?',
        '[{"id": "A", "text": "Charter Act of $$1813$$", "isCorrect": true}, {"id": "B", "text": "Charter Act of $$1833$$", "isCorrect": false}, {"id": "C", "text": "Charter Act of $$1853$$", "isCorrect": false}, {"id": "D", "text": "Pitt''s India Act of $$1784$$", "isCorrect": false}]',
        'A',
        'The Charter Act of $$1813$$ ended the Company''s trade monopoly in India, opening it to all British merchants except for tea and China trade.'
    ),
    (
        'a1110000-0000-0000-0000-000000000004'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Which Act made the Governor-General of Bengal the ''Governor-General of India'' and vested in him all civil and military powers?',
        '[{"id": "A", "text": "Charter Act of $$1833$$", "isCorrect": true}, {"id": "B", "text": "Charter Act of $$1853$$", "isCorrect": false}, {"id": "C", "text": "Government of India Act $$1858$$", "isCorrect": false}, {"id": "D", "text": "Indian Councils Act $$1861$$", "isCorrect": false}]',
        'A',
        'The Charter Act of $$1833$$ made Lord William Bentinck the first Governor-General of India.'
    ),
    (
        'a1110000-0000-0000-0000-000000000005'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'The rule of the British East India Company was abolished and the administration of India was transferred directly to the British Crown by:',
        '[{"id": "A", "text": "Government of India Act $$1858$$", "isCorrect": true}, {"id": "B", "text": "Indian Councils Act $$1861$$", "isCorrect": false}, {"id": "C", "text": "Charter Act of $$1853$$", "isCorrect": false}, {"id": "D", "text": "Queen''s Proclamation of $$1877$$", "isCorrect": false}]',
        'A',
        'Following the Revolt of $$1857$$, the Government of India Act $$1858$$ (Act for the Better Government of India) transferred power to the British Crown.'
    ),
    (
        'a1110000-0000-0000-0000-000000000006'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Portfolio system in the Governor-General''s Executive Council was introduced by Lord Canning through which Act?',
        '[{"id": "A", "text": "Indian Councils Act $$1861$$", "isCorrect": true}, {"id": "B", "text": "Indian Councils Act $$1892$$", "isCorrect": false}, {"id": "C", "text": "Charter Act of $$1853$$", "isCorrect": false}, {"id": "D", "text": "Government of India Act $$1858$$", "isCorrect": false}]',
        'A',
        'Lord Canning introduced the portfolio system in $$1859$$, which was granted statutory recognition by the Indian Councils Act $$1861$$.'
    ),
    (
        'a1110000-0000-0000-0000-000000000007'::uuid,
        'Constitutional Framework & Historical Framing',
        'EASY',
        'REMEMBER',
        'Separate electorates for Muslims were introduced in British India for the first time by which legislative reform?',
        '[{"id": "A", "text": "Morley-Minto Reforms (Indian Councils Act $$1909$$)", "isCorrect": true}, {"id": "B", "text": "Montagu-Chelmsford Reforms ($$1919$$)", "isCorrect": false}, {"id": "C", "text": "Government of India Act $$1935$$", "isCorrect": false}, {"id": "D", "text": "Indian Councils Act $$1892$$", "isCorrect": false}]',
        'A',
        'The Indian Councils Act $$1909$$ introduced communal representation with separate electorates for Muslims; Lord Minto is called the Father of Communal Electorate.'
    ),
    (
        'a1110000-0000-0000-0000-000000000008'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'UNDERSTAND',
        'Dyarchy (dual governance) was introduced in the Provinces by which Act?',
        '[{"id": "A", "text": "Government of India Act $$1919$$ (Mont-Ford Reforms)", "isCorrect": true}, {"id": "B", "text": "Government of India Act $$1935$$", "isCorrect": false}, {"id": "C", "text": "Indian Councils Act $$1909$$", "isCorrect": false}, {"id": "D", "text": "Charter Act of $$1853$$", "isCorrect": false}]',
        'A',
        'The Government of India Act $$1919$$ introduced Dyarchy in provinces, dividing subjects into ''Transferred'' and ''Reserved''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000009'::uuid,
        'Constitutional Framework & Historical Framing',
        'HARD',
        'ANALYZE',
        'Which Act abolished dyarchy in the provinces and introduced ''Provincial Autonomy''?',
        '[{"id": "A", "text": "Government of India Act $$1935$$", "isCorrect": true}, {"id": "B", "text": "Government of India Act $$1919$$", "isCorrect": false}, {"id": "C", "text": "Indian Independence Act $$1947$$", "isCorrect": false}, {"id": "D", "text": "Cabinet Mission Plan of $$1946$$", "isCorrect": false}]',
        'A',
        'The Government of India Act $$1935$$ abolished provincial dyarchy, granted provincial autonomy, and provided for dyarchy at the Centre.'
    ),
    (
        'a1110000-0000-0000-0000-000000000010'::uuid,
        'Constitutional Framework & Historical Framing',
        'MEDIUM',
        'REMEMBER',
        'Who was the Constitutional Advisor to the Constituent Assembly of India during the drafting of the Constitution?',
        '[{"id": "A", "text": "Sir B.N. Rau", "isCorrect": true}, {"id": "B", "text": "Dr. B.R. Ambedkar", "isCorrect": false}, {"id": "C", "text": "K.M. Munshi", "isCorrect": false}, {"id": "D", "text": "Alladi Krishnaswamy Iyer", "isCorrect": false}]',
        'A',
        'Sir Benegal Narsing Rau (B.N. Rau) was appointed the Constitutional Advisor to the Constituent Assembly.'
    ),
    (
        'a1110000-0000-0000-0000-000000000011'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which words were added to the Preamble of the Indian Constitution by the $$42\text{nd}$$ Constitutional Amendment Act of $$1976$$?',
        '[{"id": "A", "text": "Socialist, Secular, and Integrity", "isCorrect": true}, {"id": "B", "text": "Sovereign, Democratic, and Republic", "isCorrect": false}, {"id": "C", "text": "Liberty, Equality, and Fraternity", "isCorrect": false}, {"id": "D", "text": "Justice, Liberty, and Dignity", "isCorrect": false}]',
        'A',
        'The $$42\text{nd}$$ Amendment ($$1976$$) amended the Preamble once in Indian history, adding ''Socialist'', ''Secular'', and ''and Integrity''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000012'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'In which landmark case did the Supreme Court explicitly rule that the Preamble is an integral part of the Constitution?',
        '[{"id": "A", "text": "Kesavananda Bharati v. State of Kerala ($$1973$$)", "isCorrect": true}, {"id": "B", "text": "Berubari Union Case ($$1960$$)", "isCorrect": false}, {"id": "C", "text": "Golaknath v. State of Punjab ($$1967$$)", "isCorrect": false}, {"id": "D", "text": "Minerva Mills v. Union of India ($$1980$$)", "isCorrect": false}]',
        'A',
        'In Kesavananda Bharati ($$1973$$), the Supreme Court overturned the Berubari opinion and held that the Preamble is an integral part of the Constitution.'
    ),
    (
        'a1110000-0000-0000-0000-000000000013'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Article $$1$$ of the Constitution describes India as:',
        '[{"id": "A", "text": "A Union of States", "isCorrect": true}, {"id": "B", "text": "A Federation of States", "isCorrect": false}, {"id": "C", "text": "A Confederation of States", "isCorrect": false}, {"id": "D", "text": "A Unitary State with autonomous provinces", "isCorrect": false}]',
        'A',
        'Article $$1(1)$$ declares: ''India, that is Bharat, shall be a Union of States''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000014'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$3$$ of the Indian Constitution, the Parliament can form new states or alter state boundaries by:',
        '[{"id": "A", "text": "A simple majority in both Houses of Parliament", "isCorrect": true}, {"id": "B", "text": "A special majority of two-thirds members present and voting", "isCorrect": false}, {"id": "C", "text": "A special majority ratified by half of state legislatures", "isCorrect": false}, {"id": "D", "text": "Unanimous consent of the affected State Legislative Assembly", "isCorrect": false}]',
        'A',
        'Article $$3$$ laws can be passed by a simple majority under Article $$4(2)$$, which specifies that such laws are not considered amendments under Article $$368$$.'
    ),
    (
        'a1110000-0000-0000-0000-000000000015'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Which Commission was appointed in $$1953$$ to re-examine the reorganization of Indian states on linguistic lines?',
        '[{"id": "A", "text": "Fazl Ali Commission (States Reorganisation Commission)", "isCorrect": true}, {"id": "B", "text": "Dhar Commission", "isCorrect": false}, {"id": "C", "text": "JVP Committee", "isCorrect": false}, {"id": "D", "text": "Sarkaria Commission", "isCorrect": false}]',
        'A',
        'The Fazl Ali Commission (comprising Justice Fazl Ali, K.M. Panikkar, and H.N. Kunzru) submitted its report in $$1955$$, leading to the States Reorganisation Act $$1956$$.'
    ),
    (
        'a1110000-0000-0000-0000-000000000016'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which was the first state created on a linguistic basis in independent India in $$1953$$?',
        '[{"id": "A", "text": "Andhra State (for Telugu-speaking population)", "isCorrect": true}, {"id": "B", "text": "Gujarat", "isCorrect": false}, {"id": "C", "text": "Maharashtra", "isCorrect": false}, {"id": "D", "text": "Kerala", "isCorrect": false}]',
        'A',
        'Andhra State was created on October 1, $$1953$$, carved out of Madras State after the hunger strike and death of Potti Sreeramulu.'
    ),
    (
        'a1110000-0000-0000-0000-000000000017'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'REMEMBER',
        'Which Articles in the Indian Constitution deal with Citizenship?',
        '[{"id": "A", "text": "Articles $$5$$ to $$11$$ (Part II)", "isCorrect": true}, {"id": "B", "text": "Articles $$1$$ to $$4$$ (Part I)", "isCorrect": false}, {"id": "C", "text": "Articles $$12$$ to $$35$$ (Part III)", "isCorrect": false}, {"id": "D", "text": "Articles $$36$$ to $$51$$ (Part IV)", "isCorrect": false}]',
        'A',
        'Part II of the Constitution contains Articles $$5$$ to $$11$$, dealing with Citizenship at the commencement of the Constitution.'
    ),
    (
        'a1110000-0000-0000-0000-000000000018'::uuid,
        'Preamble, Union & Citizenship',
        'MEDIUM',
        'UNDERSTAND',
        'Under the Citizenship Act of $$1955$$, Indian citizenship can NOT be acquired through:',
        '[{"id": "A", "text": "Acquiring foreign real estate or property", "isCorrect": true}, {"id": "B", "text": "Birth", "isCorrect": false}, {"id": "C", "text": "Descent", "isCorrect": false}, {"id": "D", "text": "Naturalisation or Registration", "isCorrect": false}]',
        'A',
        'Indian citizenship is acquired via Birth, Descent, Registration, Naturalisation, and Incorporation of Territory. Property acquisition confers no citizenship rights.'
    ),
    (
        'a1110000-0000-0000-0000-000000000019'::uuid,
        'Preamble, Union & Citizenship',
        'EASY',
        'UNDERSTAND',
        'Does the Indian Constitution allow dual citizenship with other foreign sovereign countries?',
        '[{"id": "A", "text": "No, India provides for single citizenship only", "isCorrect": true}, {"id": "B", "text": "Yes, for all Commonwealth nations", "isCorrect": false}, {"id": "C", "text": "Yes, through Overseas Citizen of India (OCI) full citizenship", "isCorrect": false}, {"id": "D", "text": "Yes, but only for members of SAARC", "isCorrect": false}]',
        'A',
        'Under Article $$9$$, a person voluntarily acquiring foreign citizenship automatically loses Indian citizenship. OI/OCI is a card-holder status, not dual citizenship.'
    ),
    (
        'a1110000-0000-0000-0000-000000000020'::uuid,
        'Preamble, Union & Citizenship',
        'HARD',
        'ANALYZE',
        'Which Constitutional Article empowers the Parliament to regulate the right of citizenship by law?',
        '[{"id": "A", "text": "Article $$11$$", "isCorrect": true}, {"id": "B", "text": "Article $$10$$", "isCorrect": false}, {"id": "C", "text": "Article $$8$$", "isCorrect": false}, {"id": "D", "text": "Article $$9$$", "isCorrect": false}]',
        'A',
        'Article $$11$$ expressly grants Parliament the plenary power to make any provision with respect to the acquisition and termination of citizenship.'
    ),
    (
        'a1110000-0000-0000-0000-000000000021'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution abolishes all titles, prohibiting the State from conferring any title except military and academic distinctions?',
        '[{"id": "A", "text": "Article $$18$$", "isCorrect": true}, {"id": "B", "text": "Article $$17$$", "isCorrect": false}, {"id": "C", "text": "Article $$19$$", "isCorrect": false}, {"id": "D", "text": "Article $$14$$", "isCorrect": false}]',
        'A',
        'Article $$18(1)$$ abolishes all titles and prevents the State from conferring any title except military or academic distinctions.'
    ),
    (
        'a1110000-0000-0000-0000-000000000022'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Abolition of titles (except military and academic distinctions) is provided under which Article?',
        '[{"id": "A", "text": "Article $$18$$", "isCorrect": true}, {"id": "B", "text": "Article $$16$$", "isCorrect": false}, {"id": "C", "text": "Article $$15$$", "isCorrect": false}, {"id": "D", "text": "Article $$20$$", "isCorrect": false}]',
        'A',
        'Article $$18(1)$$ states that no title, not being a military or academic distinction, shall be conferred by the State.'
    ),
    (
        'a1110000-0000-0000-0000-000000000023'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'How many basic freedoms are currently guaranteed under Article $$19(1)$$ of the Indian Constitution?',
        '[{"id": "A", "text": "$$6\\text{ freedoms}$$", "isCorrect": true}, {"id": "B", "text": "$$7\\text{ freedoms}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ freedoms}$$", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ freedoms}$$", "isCorrect": false}]',
        'A',
        'Originally there were 7 freedoms. The right to acquire, hold and dispose of property (Article $$19(1)(f)$$) was deleted by the $$44\text{th}$$ Amendment ($$1978$$), leaving 6 freedoms.'
    ),
    (
        'a1110000-0000-0000-0000-000000000024'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Article $$20(2)$$ of the Constitution states that no person shall be prosecuted and punished for the same offence more than once. This principle is known as:',
        '[{"id": "A", "text": "Rule against Double Jeopardy", "isCorrect": true}, {"id": "B", "text": "Ex-post facto law", "isCorrect": false}, {"id": "C", "text": "Prohibition against self-incrimination", "isCorrect": false}, {"id": "D", "text": "Habeas corpus doctrine", "isCorrect": false}]',
        'A',
        'Article $$20(2)$$ incorporates the common law principle of ''nemo debet bis vexari'' (no one should be put in double jeopardy for the same offence).'
    ),
    (
        'a1110000-0000-0000-0000-000000000025'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment inserted Article $$21\text{A}$$, making free and compulsory education for children aged 6 to 14 a Fundamental Right?',
        '[{"id": "A", "text": "$$86\\text{th}$$ Constitutional Amendment Act $$2002$$", "isCorrect": true}, {"id": "B", "text": "$$44\\text{th}$$ Constitutional Amendment Act $$1978$$", "isCorrect": false}, {"id": "C", "text": "$$91\\text{st}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}, {"id": "D", "text": "$$73\\text{rd}$$ Constitutional Amendment Act $$1992$$", "isCorrect": false}]',
        'A',
        'The $$86\text{th}$$ Amendment Act of $$2002$$ inserted Article $$21\text{A}$$, giving effect to the Right to Education (RTE Act $$2009$$).'
    ),
    (
        'a1110000-0000-0000-0000-000000000026'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'REMEMBER',
        'Which Article prohibits trafficking in human beings and forced labor (begar)?',
        '[{"id": "A", "text": "Article $$23$$", "isCorrect": true}, {"id": "B", "text": "Article $$24$$", "isCorrect": false}, {"id": "C", "text": "Article $$25$$", "isCorrect": false}, {"id": "D", "text": "Article $$22$$", "isCorrect": false}]',
        'A',
        'Article $$23$$ prohibits traffic in human beings, begar, and other similar forms of forced labor.'
    ),
    (
        'a1110000-0000-0000-0000-000000000027'::uuid,
        'Fundamental Rights & Writs',
        'EASY',
        'REMEMBER',
        'Article $$24$$ of the Constitution prohibits the employment of children below what age in factories, mines, and hazardous jobs?',
        '[{"id": "A", "text": "$$14\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$16\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$18\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$12\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Article $$24$$ prohibits the employment of children below the age of $$14$$ years in any factory, mine, or hazardous occupation.'
    ),
    (
        'a1110000-0000-0000-0000-000000000028'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which writ literally translates to ''We Command'', issued to compel a public official or authority to perform their mandatory statutory duty?',
        '[{"id": "A", "text": "Mandamus", "isCorrect": true}, {"id": "B", "text": "Habeas Corpus", "isCorrect": false}, {"id": "C", "text": "Quo-Warranto", "isCorrect": false}, {"id": "D", "text": "Certiorari", "isCorrect": false}]',
        'A',
        'Mandamus means ''We Command''. It is an order issued by a superior court commanding a public officer or body to perform a public duty.'
    ),
    (
        'a1110000-0000-0000-0000-000000000029'::uuid,
        'Fundamental Rights & Writs',
        'MEDIUM',
        'UNDERSTAND',
        'Which writ is issued by a higher court to quash an order already passed by a lower court or tribunal that exceeded its jurisdiction?',
        '[{"id": "A", "text": "Certiorari", "isCorrect": true}, {"id": "B", "text": "Prohibition", "isCorrect": false}, {"id": "C", "text": "Mandamus", "isCorrect": false}, {"id": "D", "text": "Quo-Warranto", "isCorrect": false}]',
        'A',
        'Certiorari is curative: it quashes illegal orders passed by lower courts. Prohibition is preventive: it stops ongoing proceedings.'
    ),
    (
        'a1110000-0000-0000-0000-000000000030'::uuid,
        'Fundamental Rights & Writs',
        'HARD',
        'ANALYZE',
        'Which writ is issued to inquire into the legality of a person''s claim to a public office to prevent illegal usurpation?',
        '[{"id": "A", "text": "Quo-Warranto", "isCorrect": true}, {"id": "B", "text": "Mandamus", "isCorrect": false}, {"id": "C", "text": "Certiorari", "isCorrect": false}, {"id": "D", "text": "Habeas Corpus", "isCorrect": false}]',
        'A',
        'Quo-Warranto literally means ''by what authority or warrant''. It prevents illegal usurpation of a public office by an unqualified individual.'
    ),
    (
        'a1110000-0000-0000-0000-000000000031'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'The Directive Principles of State Policy (Part IV) in the Indian Constitution were borrowed from which country''s constitution?',
        '[{"id": "A", "text": "Ireland (Irish Constitution of $$1937$$)", "isCorrect": true}, {"id": "B", "text": "United States of America", "isCorrect": false}, {"id": "C", "text": "United Kingdom", "isCorrect": false}, {"id": "D", "text": "Australia", "isCorrect": false}]',
        'A',
        'The makers of the Indian Constitution borrowed the Directive Principles of State Policy from the Irish Constitution (which borrowed them from Spain).'
    ),
    (
        'a1110000-0000-0000-0000-000000000032'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution directs the State to secure a ''Uniform Civil Code'' (UCC) for citizens throughout the territory of India?',
        '[{"id": "A", "text": "Article $$44$$", "isCorrect": true}, {"id": "B", "text": "Article $$40$$", "isCorrect": false}, {"id": "C", "text": "Article $$48$$", "isCorrect": false}, {"id": "D", "text": "Article $$50$$", "isCorrect": false}]',
        'A',
        'Article $$44$$ states that ''The State shall endeavour to secure for the citizens a Uniform Civil Code throughout the territory of India''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000033'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Article $$40$$ of the Constitution directs the State to organize which local self-governing institutions?',
        '[{"id": "A", "text": "Village Panchayats", "isCorrect": true}, {"id": "B", "text": "Municipal Corporations", "isCorrect": false}, {"id": "C", "text": "Cooperative Societies", "isCorrect": false}, {"id": "D", "text": "Cottage industries boards", "isCorrect": false}]',
        'A',
        'Article $$40$$ embodies Gandhian principles: ''The State shall take steps to organise village panchayats and endow them with such powers as may be necessary''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000034'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Separation of the Judiciary from the Executive in public services of the State is mandated under which Article?',
        '[{"id": "A", "text": "Article $$50$$", "isCorrect": true}, {"id": "B", "text": "Article $$45$$", "isCorrect": false}, {"id": "C", "text": "Article $$51$$", "isCorrect": false}, {"id": "D", "text": "Article $$47$$", "isCorrect": false}]',
        'A',
        'Article $$50$$ provides for the separation of judiciary from the executive in the public services of the State.'
    ),
    (
        'a1110000-0000-0000-0000-000000000035'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'REMEMBER',
        'Promotion of international peace and security and maintaining just and honorable relations between nations is enshrined in:',
        '[{"id": "A", "text": "Article $$51$$", "isCorrect": true}, {"id": "B", "text": "Article $$49$$", "isCorrect": false}, {"id": "C", "text": "Article $$51\\text{A}$$", "isCorrect": false}, {"id": "D", "text": "Preamble only", "isCorrect": false}]',
        'A',
        'Article $$51$$ is the constitutional directive on foreign policy, urging the State to promote international peace, security, and treaty obligations.'
    ),
    (
        'a1110000-0000-0000-0000-000000000036'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Can Directive Principles of State Policy be enforced directly by citizens through courts of law in case of violation?',
        '[{"id": "A", "text": "No, they are non-justiciable under Article $$37$$", "isCorrect": true}, {"id": "B", "text": "Yes, through writ petitions under Article $$32$$", "isCorrect": false}, {"id": "C", "text": "Yes, but only in High Courts under Article $$226$$", "isCorrect": false}, {"id": "D", "text": "Yes, if passed by Parliament as a statutory resolution", "isCorrect": false}]',
        'A',
        'Article $$37$$ states that DPSPs are non-justiciable (not enforceable by any court), but nevertheless fundamental in the governance of the country.'
    ),
    (
        'a1110000-0000-0000-0000-000000000037'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'Fundamental Duties were incorporated into Part IV-A of the Constitution on the recommendations of which Committee?',
        '[{"id": "A", "text": "Swaran Singh Committee ($$1976$$)", "isCorrect": true}, {"id": "B", "text": "Verma Committee", "isCorrect": false}, {"id": "C", "text": "Sarkaria Commission", "isCorrect": false}, {"id": "D", "text": "Kothari Commission", "isCorrect": false}]',
        'A',
        'The Swaran Singh Committee recommended incorporating Fundamental Duties in $$1976$$, resulting in the $$42\text{nd}$$ Constitutional Amendment.'
    ),
    (
        'a1110000-0000-0000-0000-000000000038'::uuid,
        'Directive Principles & Fundamental Duties',
        'EASY',
        'REMEMBER',
        'How many Fundamental Duties were initially added by the $$42\text{nd}$$ Constitutional Amendment in $$1976$$, and how many are there today?',
        '[{"id": "A", "text": "$$10\\text{ originally, and } 11\\text{ today}$$", "isCorrect": true}, {"id": "B", "text": "$$8\\text{ originally, and } 10\\text{ today}$$", "isCorrect": false}, {"id": "C", "text": "$$11\\text{ originally, and } 11\\text{ today}$$", "isCorrect": false}, {"id": "D", "text": "$$9\\text{ originally, and } 12\\text{ today}$$", "isCorrect": false}]',
        'A',
        'The $$42\text{nd}$$ Amendment added 10 duties in Article $$51\text{A}$$. The $$11\text{th}$$ duty (duty of parents to provide education) was added by the $$86\text{th}$$ Amendment ($$2002$$).'
    ),
    (
        'a1110000-0000-0000-0000-000000000039'::uuid,
        'Directive Principles & Fundamental Duties',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following is an explicit Fundamental Duty under Article $$51\text{A}$$?',
        '[{"id": "A", "text": "To develop the scientific temper, humanism, and the spirit of inquiry and reform", "isCorrect": true}, {"id": "B", "text": "To pay direct taxes honestly and on time", "isCorrect": false}, {"id": "C", "text": "To vote compulsorily in general elections", "isCorrect": false}, {"id": "D", "text": "To practice family planning and population control", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(h)$$ explicitly mentions developing scientific temper, humanism, and the spirit of inquiry and reform. Voting and paying taxes are not listed duties.'
    ),
    (
        'a1110000-0000-0000-0000-000000000040'::uuid,
        'Directive Principles & Fundamental Duties',
        'HARD',
        'ANALYZE',
        'Under Article $$51\text{A}(k)$$, whose duty is it to provide educational opportunities to a child between the ages of 6 and 14?',
        '[{"id": "A", "text": "The parent or legal guardian of the child", "isCorrect": true}, {"id": "B", "text": "The village Panchayat head only", "isCorrect": false}, {"id": "C", "text": "The State Government civil servants", "isCorrect": false}, {"id": "D", "text": "Non-governmental social welfare organizations", "isCorrect": false}]',
        'A',
        'Article $$51\text{A}(k)$$ mandates that a parent or guardian must provide opportunities for education to their child or ward between the ages of six and fourteen.'
    ),
    (
        'a1110000-0000-0000-0000-000000000041'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'What is the minimum age required for a candidate to contest the election for the President of India?',
        '[{"id": "A", "text": "$$35\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$25\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$21\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Article $$58$$ stipulates that to be eligible for election as President, a person must be a citizen of India and have completed the age of $$35$$ years.'
    ),
    (
        'a1110000-0000-0000-0000-000000000042'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Who among the following does NOT participate in the election of the President of India?',
        '[{"id": "A", "text": "Nominated members of both Houses of Parliament and State Legislative Assemblies", "isCorrect": true}, {"id": "B", "text": "Elected members of the Lok Sabha", "isCorrect": false}, {"id": "C", "text": "Elected members of the Rajya Sabha", "isCorrect": false}, {"id": "D", "text": "Elected members of the Legislative Assemblies of States and Union Territories", "isCorrect": false}]',
        'A',
        'Under Article $$54$$, the electoral college consists only of elected members of Parliament and State Legislative Assemblies (including Delhi and Puducherry). Nominated members cannot vote.'
    ),
    (
        'a1110000-0000-0000-0000-000000000043'::uuid,
        'Union Executive',
        'HARD',
        'ANALYZE',
        'Under Article $$61$$, on what sole constitutional ground can the President of India be impeached?',
        '[{"id": "A", "text": "Violation of the Constitution", "isCorrect": true}, {"id": "B", "text": "Proven misbehavior or incapacity", "isCorrect": false}, {"id": "C", "text": "Criminal conspiracy or bribery", "isCorrect": false}, {"id": "D", "text": "Insolvency or bankruptcy", "isCorrect": false}]',
        'A',
        'Article $$61(1)$$ states that the President can be impeached only for ''violation of the Constitution''.'
    ),
    (
        'a1110000-0000-0000-0000-000000000044'::uuid,
        'Union Executive',
        'MEDIUM',
        'REMEMBER',
        'Under which Article can the President promulgate an Ordinance when both Houses of Parliament are not in session?',
        '[{"id": "A", "text": "Article $$123$$", "isCorrect": true}, {"id": "B", "text": "Article $$213$$", "isCorrect": false}, {"id": "C", "text": "Article $$143$$", "isCorrect": false}, {"id": "D", "text": "Article $$72$$", "isCorrect": false}]',
        'A',
        'Article $$123$$ empowers the President to promulgate Ordinances during recess of Parliament. Article $$213$$ grants similar power to State Governors.'
    ),
    (
        'a1110000-0000-0000-0000-000000000045'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'What is the maximum life of an Ordinance promulgated by the President if Parliament is reassembled and takes no action?',
        '[{"id": "A", "text": "$$6\\text{ weeks}$$ from the date of reassembly of Parliament", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ months}$$ from the date of promulgation", "isCorrect": false}, {"id": "C", "text": "$$30\\text{ days}$$ from reassembly", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ year}$$", "isCorrect": false}]',
        'A',
        'An ordinance ceases to operate $$6$$ weeks after the reassembly of Parliament, unless approved earlier. Maximum gap between sessions is 6 months, giving theoretical max life of 6 months and 6 weeks.'
    ),
    (
        'a1110000-0000-0000-0000-000000000046'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'The pardoning powers of the President of India (Pardon, Commutation, Remission, Respite, Reprieve) are governed by:',
        '[{"id": "A", "text": "Article $$72$$", "isCorrect": true}, {"id": "B", "text": "Article $$161$$", "isCorrect": false}, {"id": "C", "text": "Article $$74$$", "isCorrect": false}, {"id": "D", "text": "Article $$76$$", "isCorrect": false}]',
        'A',
        'Article $$72$$ empowers the President to grant pardons, reprieves, respites or remissions of punishment or to suspend, remit or commute sentences in all cases including court-martial and death penalties.'
    ),
    (
        'a1110000-0000-0000-0000-000000000047'::uuid,
        'Union Executive',
        'EASY',
        'REMEMBER',
        'Who is the ex-officio Chairman of the Rajya Sabha (Council of States)?',
        '[{"id": "A", "text": "The Vice-President of India", "isCorrect": true}, {"id": "B", "text": "The Prime Minister", "isCorrect": false}, {"id": "C", "text": "The Chief Justice of India", "isCorrect": false}, {"id": "D", "text": "The Speaker of Lok Sabha", "isCorrect": false}]',
        'A',
        'Under Article $$64$$ and Article $$89(1)$$, the Vice-President of India is the ex-officio Chairman of the Rajya Sabha.'
    ),
    (
        'a1110000-0000-0000-0000-000000000048'::uuid,
        'Union Executive',
        'MEDIUM',
        'REMEMBER',
        'Who is the first law officer of the Government of India, appointed under Article $$76$$?',
        '[{"id": "A", "text": "Attorney General for India", "isCorrect": true}, {"id": "B", "text": "Solicitor General of India", "isCorrect": false}, {"id": "C", "text": "Union Minister of Law and Justice", "isCorrect": false}, {"id": "D", "text": "Chief Justice of India", "isCorrect": false}]',
        'A',
        'Article $$76$$ provides for the Attorney General for India, who advises the Government upon legal matters and has the right of audience in all Indian courts.'
    ),
    (
        'a1110000-0000-0000-0000-000000000049'::uuid,
        'Union Executive',
        'HARD',
        'UNDERSTAND',
        'Does the Attorney General for India have the right to speak and take part in the proceedings of both Houses of Parliament?',
        '[{"id": "A", "text": "Yes, but without the right to vote", "isCorrect": true}, {"id": "B", "text": "Yes, including full voting rights in joint sittings", "isCorrect": false}, {"id": "C", "text": "No, he can only appear in courts", "isCorrect": false}, {"id": "D", "text": "Yes, but only in the Rajya Sabha", "isCorrect": false}]',
        'A',
        'Under Article $$88$$, the Attorney General has the right to speak and participate in Parliament and Parliamentary Committees, but has no right to vote.'
    ),
    (
        'a1110000-0000-0000-0000-000000000050'::uuid,
        'Union Executive',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$75(3)$$, the Union Council of Ministers is collectively responsible to:',
        '[{"id": "A", "text": "The Lok Sabha (House of the People)", "isCorrect": true}, {"id": "B", "text": "The President of India", "isCorrect": false}, {"id": "C", "text": "Both Houses of Parliament equally", "isCorrect": false}, {"id": "D", "text": "The Prime Minister", "isCorrect": false}]',
        'A',
        'Article $$75(3)$$ specifies collective responsibility to the Lok Sabha. If the Lok Sabha passes a no-confidence motion, all ministers must resign.'
    ),
    (
        'a1110000-0000-0000-0000-000000000051'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the maximum sanctioned strength of the Rajya Sabha as per the Constitution?',
        '[{"id": "A", "text": "$$250\\text{ members}$$", "isCorrect": true}, {"id": "B", "text": "$$245\\text{ members}$$", "isCorrect": false}, {"id": "C", "text": "$$550\\text{ members}$$", "isCorrect": false}, {"id": "D", "text": "$$260\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Under Article $$80$$, the maximum strength of Rajya Sabha is $$250$$ ($$238$$ representing States/UTs and $$12$$ nominated by the President).'
    ),
    (
        'a1110000-0000-0000-0000-000000000052'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'How many members of the Rajya Sabha retire every two years?',
        '[{"id": "A", "text": "One-third ($$\\frac{1}{3}\\text{rd}$$) of its members", "isCorrect": true}, {"id": "B", "text": "One-fourth ($$\\frac{1}{4}\\text{th}$$) of its members", "isCorrect": false}, {"id": "C", "text": "Half ($$\\frac{1}{2}$$) of its members", "isCorrect": false}, {"id": "D", "text": "One-sixth ($$\\frac{1}{6}\\text{th}$$) of its members", "isCorrect": false}]',
        'A',
        'Rajya Sabha is a permanent body not subject to dissolution. One-third of its members retire every second year under Article $$83(1)$$, each serving a 6-year term.'
    ),
    (
        'a1110000-0000-0000-0000-000000000053'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'Which type of Cut Motion in the Lok Sabha seeks that the amount of the budget demand be reduced to Re. 1 to represent disapproval of policy?',
        '[{"id": "A", "text": "Disapproval of Policy Cut Motion", "isCorrect": true}, {"id": "B", "text": "Economy Cut Motion", "isCorrect": false}, {"id": "C", "text": "Token Cut Motion", "isCorrect": false}, {"id": "D", "text": "Adjournment Motion", "isCorrect": false}]',
        'A',
        'A Disapproval of Policy Cut Motion demands that the amount of the demand be reduced to Re. 1, representing disapproval of the policy underlying the demand.'
    ),
    (
        'a1110000-0000-0000-0000-000000000054'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'What is the constitutional quorum required to transact business in either House of Parliament?',
        '[{"id": "A", "text": "One-tenth ($$\\frac{1}{10}\\text{th}$$) of the total membership of the House", "isCorrect": true}, {"id": "B", "text": "One-sixth ($$\\frac{1}{6}\\text{th}$$) of the total membership", "isCorrect": false}, {"id": "C", "text": "One-fifth ($$\\frac{1}{5}\\text{th}$$) of the total membership", "isCorrect": false}, {"id": "D", "text": "One-third ($$\\frac{1}{3}\\text{rd}$$) of the total membership", "isCorrect": false}]',
        'A',
        'Article $$100(3)$$ mandates a quorum of 1/10th of the total members of either House to constitute a valid meeting.'
    ),
    (
        'a1110000-0000-0000-0000-000000000055'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'Who decides whether a particular legislative bill is a Money Bill or not?',
        '[{"id": "A", "text": "Speaker of the Lok Sabha", "isCorrect": true}, {"id": "B", "text": "President of India", "isCorrect": false}, {"id": "C", "text": "Chairman of the Rajya Sabha", "isCorrect": false}, {"id": "D", "text": "Union Finance Minister", "isCorrect": false}]',
        'A',
        'Under Article $$110(3)$$, the decision of the Speaker of the Lok Sabha on whether a bill is a Money Bill is final and conclusive.'
    ),
    (
        'a1110000-0000-0000-0000-000000000056'::uuid,
        'Union Legislature & Parliament',
        'EASY',
        'REMEMBER',
        'For how long can the Rajya Sabha detain a Money Bill passed by the Lok Sabha before it is deemed passed?',
        '[{"id": "A", "text": "$$14\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$6\\text{ months}$$", "isCorrect": false}, {"id": "D", "text": "$$3\\text{ months}$$", "isCorrect": false}]',
        'A',
        'Article $$109(5)$$ specifies that if a Money Bill is not returned by the Rajya Sabha within $$14$$ days, it is deemed to have been passed by both Houses in the form passed by Lok Sabha.'
    ),
    (
        'a1110000-0000-0000-0000-000000000057'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'UNDERSTAND',
        'Who presides over a Joint Sitting of both Houses of Parliament convened under Article $$108$$?',
        '[{"id": "A", "text": "Speaker of the Lok Sabha", "isCorrect": true}, {"id": "B", "text": "Chairman of the Rajya Sabha", "isCorrect": false}, {"id": "C", "text": "President of India", "isCorrect": false}, {"id": "D", "text": "Prime Minister of India", "isCorrect": false}]',
        'A',
        'Under Article $$118(4)$$, the Speaker of the Lok Sabha presides over a joint sitting (or Deputy Speaker in his absence). The Chairman of Rajya Sabha never presides.'
    ),
    (
        'a1110000-0000-0000-0000-000000000058'::uuid,
        'Union Legislature & Parliament',
        'HARD',
        'ANALYZE',
        'Can a Joint Sitting of Parliament be summoned to resolve a deadlock over a Constitution Amendment Bill?',
        '[{"id": "A", "text": "No, each House must pass a Constitution Amendment Bill separately by special majority", "isCorrect": true}, {"id": "B", "text": "Yes, if the deadlock persists for over six months", "isCorrect": false}, {"id": "C", "text": "Yes, but only on the advice of the Chief Justice of India", "isCorrect": false}, {"id": "D", "text": "Yes, if half the States petition the President", "isCorrect": false}]',
        'A',
        'Under Article $$368$$, there is no provision for a joint sitting on Constitution Amendment Bills or Money Bills. Both Houses must pass amendment bills independently.'
    ),
    (
        'a1110000-0000-0000-0000-000000000059'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'The first hour of every parliamentary sitting is reserved for:',
        '[{"id": "A", "text": "Question Hour", "isCorrect": true}, {"id": "B", "text": "Zero Hour", "isCorrect": false}, {"id": "C", "text": "Calling Attention Motion", "isCorrect": false}, {"id": "D", "text": "Adjournment debate", "isCorrect": false}]',
        'A',
        'The first hour (11:00 AM to 12:00 noon) is Question Hour, during which MPs pose questions to Ministers.'
    ),
    (
        'a1110000-0000-0000-0000-000000000060'::uuid,
        'Union Legislature & Parliament',
        'MEDIUM',
        'REMEMBER',
        'Which parliamentary committee is the largest in terms of membership, consisting exclusively of $$30$$ members from the Lok Sabha?',
        '[{"id": "A", "text": "Estimates Committee", "isCorrect": true}, {"id": "B", "text": "Public Accounts Committee (PAC)", "isCorrect": false}, {"id": "C", "text": "Committee on Public Undertakings (COPU)", "isCorrect": false}, {"id": "D", "text": "Business Advisory Committee", "isCorrect": false}]',
        'A',
        'The Estimates Committee has $$30$$ members, all elected from Lok Sabha. Rajya Sabha has no representation on this committee.'
    ),
    (
        'a1110000-0000-0000-0000-000000000061'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'Who appoints the Governor of an Indian State under Article $$155$$?',
        '[{"id": "A", "text": "The President of India by warrant under his hand and seal", "isCorrect": true}, {"id": "B", "text": "The Prime Minister of India", "isCorrect": false}, {"id": "C", "text": "The Chief Minister of the concerned State", "isCorrect": false}, {"id": "D", "text": "The Chief Justice of the State High Court", "isCorrect": false}]',
        'A',
        'Article $$155$$ provides that the Governor of a State shall be appointed by the President by warrant under his hand and seal.'
    ),
    (
        'a1110000-0000-0000-0000-000000000062'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'What is the normal tenure of a State Governor, and at whose pleasure does he hold office?',
        '[{"id": "A", "text": "$$5\\text{ years}$$, holding office during the pleasure of the President", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ years}$$, removable only by impeachment in the State Assembly", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ years}$$, holding office during the pleasure of the Chief Minister", "isCorrect": false}, {"id": "D", "text": "$$4\\text{ years}$$, holding office during good behaviour", "isCorrect": false}]',
        'A',
        'Under Article $$156$$, the Governor holds office during the pleasure of the President, with a normal term of 5 years.'
    ),
    (
        'a1110000-0000-0000-0000-000000000063'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Which Article of the Constitution provides for the creation or abolition of Legislative Councils (Vidhan Parishad) in States?',
        '[{"id": "A", "text": "Article $$169$$", "isCorrect": true}, {"id": "B", "text": "Article $$168$$", "isCorrect": false}, {"id": "C", "text": "Article $$170$$", "isCorrect": false}, {"id": "D", "text": "Article $$171$$", "isCorrect": false}]',
        'A',
        'Article $$169$$ empowers Parliament to create or abolish a State Legislative Council if the State Assembly passes a special resolution to that effect.'
    ),
    (
        'a1110000-0000-0000-0000-000000000064'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'What is the minimum age prescribed to become a member of the State Legislative Assembly (MLA) and State Legislative Council (MLC)?',
        '[{"id": "A", "text": "$$25\\text{ years for Assembly (MLA), and } 30\\text{ years for Council (MLC)}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ years for MLA, and } 25\\text{ years for MLC}$$", "isCorrect": false}, {"id": "C", "text": "$$21\\text{ years for MLA, and } 25\\text{ years for MLC}$$", "isCorrect": false}, {"id": "D", "text": "$$25\\text{ years for both}$$", "isCorrect": false}]',
        'A',
        'Article $$173(b)$$ stipulates minimum age of $$25$$ years for Legislative Assembly and $$30$$ years for Legislative Council.'
    ),
    (
        'a1110000-0000-0000-0000-000000000065'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$213$$, an Ordinance promulgated by a Governor must be approved by the State Legislature within:',
        '[{"id": "A", "text": "$$6\\text{ weeks}$$ from the reassembly of the State Legislature", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ months}$$ from promulgation", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ days}$$ from reassembly", "isCorrect": false}, {"id": "D", "text": "$$3\\text{ months}$$", "isCorrect": false}]',
        'A',
        'Like Presidential ordinances, a Governor''s ordinance lapses after $$6$$ weeks from the reassembly of the state legislature unless approved earlier.'
    ),
    (
        'a1110000-0000-0000-0000-000000000066'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'Under Article $$200$$, when a Bill is passed by the State Legislature, the Governor may reserve it for the consideration of:',
        '[{"id": "A", "text": "The President of India", "isCorrect": true}, {"id": "B", "text": "The Chief Justice of India", "isCorrect": false}, {"id": "C", "text": "The Union Law Ministry", "isCorrect": false}, {"id": "D", "text": "The Inter-State Council", "isCorrect": false}]',
        'A',
        'Under Article $$200$$, the Governor may reserve a bill for the consideration of the President, especially if it derogates from the powers of the High Court.'
    ),
    (
        'a1110000-0000-0000-0000-000000000067'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'REMEMBER',
        'Who administers the oath of office to the Governor of a State?',
        '[{"id": "A", "text": "Chief Justice of the High Court of the concerned State", "isCorrect": true}, {"id": "B", "text": "President of India", "isCorrect": false}, {"id": "C", "text": "Chief Justice of India", "isCorrect": false}, {"id": "D", "text": "Chief Minister of the State", "isCorrect": false}]',
        'A',
        'Under Article $$159$$, the oath of office to a Governor is administered by the Chief Justice of the High Court exercising jurisdiction in that State.'
    ),
    (
        'a1110000-0000-0000-0000-000000000068'::uuid,
        'State Executive & Legislature',
        'EASY',
        'REMEMBER',
        'What is the maximum and minimum strength of a State Legislative Assembly as per Article $$170$$ (except for smaller states with special exemptions)?',
        '[{"id": "A", "text": "Maximum $$500$$ and minimum $$60$$ members", "isCorrect": true}, {"id": "B", "text": "Maximum $$450$$ and minimum $$50$$ members", "isCorrect": false}, {"id": "C", "text": "Maximum $$600$$ and minimum $$100$$ members", "isCorrect": false}, {"id": "D", "text": "Maximum $$400$$ and minimum $$40$$ members", "isCorrect": false}]',
        'A',
        'Article $$170(1)$$ specifies that a State Legislative Assembly shall consist of not more than 500 and not less than 60 members.'
    ),
    (
        'a1110000-0000-0000-0000-000000000069'::uuid,
        'State Executive & Legislature',
        'MEDIUM',
        'UNDERSTAND',
        'The Advocate-General for a State is appointed under which Article and holds office during the pleasure of:',
        '[{"id": "A", "text": "Article $$165$$, holding office during the pleasure of the Governor", "isCorrect": true}, {"id": "B", "text": "Article $$76$$, holding office during the pleasure of the President", "isCorrect": false}, {"id": "C", "text": "Article $$177$$, holding office during the pleasure of the Chief Minister", "isCorrect": false}, {"id": "D", "text": "Article $$168$$, holding office for a fixed term of 5 years", "isCorrect": false}]',
        'A',
        'Article $$165$$ provides for the Advocate-General for the State, appointed by the Governor and holding office during the Governor''s pleasure.'
    ),
    (
        'a1110000-0000-0000-0000-000000000070'::uuid,
        'State Executive & Legislature',
        'HARD',
        'ANALYZE',
        'The maximum size of the Council of Ministers in a State, including the Chief Minister, is restricted to what percentage of the total assembly strength by the $$91\text{st}$$ Amendment?',
        '[{"id": "A", "text": "$$15\\%$$ (and not less than $$12$$ ministers)", "isCorrect": true}, {"id": "B", "text": "$$10\\%$$ (and not less than $$10$$ ministers)", "isCorrect": false}, {"id": "C", "text": "$$20\\%$$ (and not less than $$15$$ ministers)", "isCorrect": false}, {"id": "D", "text": "$$12\\%$$ (and not less than $$8$$ ministers)", "isCorrect": false}]',
        'A',
        'Article $$164(1\text{A})$$, inserted by the $$91\text{st}$$ Amendment ($$2003$$), caps ministers at $$15\%$$ of assembly strength, with a minimum floor of $$12$$ ministers.'
    ),
    (
        'a1110000-0000-0000-0000-000000000071'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'At what age does a Judge of the Supreme Court of India retire?',
        '[{"id": "A", "text": "$$65\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$62\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$60\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$70\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Under Article $$124(2)$$, a Supreme Court judge holds office until attaining the age of $$65$$ years. High Court judges retire at $$62$$ years.'
    ),
    (
        'a1110000-0000-0000-0000-000000000072'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'At what age does a Judge of a High Court in India retire?',
        '[{"id": "A", "text": "$$62\\text{ years}$$", "isCorrect": true}, {"id": "B", "text": "$$65\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$60\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$58\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Under Article $$217(1)$$, a judge of a High Court holds office until the age of $$62$$ years (increased from 60 to 62 by the 15th Amendment in 1963).'
    ),
    (
        'a1110000-0000-0000-0000-000000000073'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$143$$, who is empowered to seek the Advisory Opinion of the Supreme Court on a question of law or fact?',
        '[{"id": "A", "text": "The President of India", "isCorrect": true}, {"id": "B", "text": "The Prime Minister", "isCorrect": false}, {"id": "C", "text": "The Speaker of Lok Sabha", "isCorrect": false}, {"id": "D", "text": "The Attorney General for India", "isCorrect": false}]',
        'A',
        'Article $$143$$ grants Advisory Jurisdiction to the Supreme Court, allowing the President to refer matters of public importance for opinion.'
    ),
    (
        'a1110000-0000-0000-0000-000000000074'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Which Article of the Constitution declares the Supreme Court to be a ''Court of Record'' with power to punish for contempt of itself?',
        '[{"id": "A", "text": "Article $$129$$", "isCorrect": true}, {"id": "B", "text": "Article $$131$$", "isCorrect": false}, {"id": "C", "text": "Article $$136$$", "isCorrect": false}, {"id": "D", "text": "Article $$141$$", "isCorrect": false}]',
        'A',
        'Article $$129$$ establishes the Supreme Court as a Court of Record. Article $$215$$ confers the same status upon High Courts.'
    ),
    (
        'a1110000-0000-0000-0000-000000000075'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'What constitutes the ''Original Jurisdiction'' of the Supreme Court under Article $$131$$?',
        '[{"id": "A", "text": "Disputes between the Government of India and one or more States, or between two or more States", "isCorrect": true}, {"id": "B", "text": "Civil appeals from High Court judgments", "isCorrect": false}, {"id": "C", "text": "Inter-State commercial contract arbitration", "isCorrect": false}, {"id": "D", "text": "Tax disputes between municipal corporations", "isCorrect": false}]',
        'A',
        'Article $$131$$ confers exclusive original jurisdiction on federal disputes: Center vs States, or State vs State.'
    ),
    (
        'a1110000-0000-0000-0000-000000000076'::uuid,
        'Judiciary',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$136$$, Special Leave Petitions ($$\text{SLP}$$) to appeal against any judgment or order in India may be granted by:',
        '[{"id": "A", "text": "The Supreme Court of India at its discretion", "isCorrect": true}, {"id": "B", "text": "The President of India", "isCorrect": false}, {"id": "C", "text": "The Union Law Commission", "isCorrect": false}, {"id": "D", "text": "The Chief Justice of the concerned High Court", "isCorrect": false}]',
        'A',
        'Article $$136$$ empowers the Supreme Court to grant special leave to appeal from any judgment, decree, sentence or order passed by any court/tribunal (except military tribunals).'
    ),
    (
        'a1110000-0000-0000-0000-000000000077'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Which is the oldest High Court in India, established in July $$1862$$?',
        '[{"id": "A", "text": "Calcutta High Court", "isCorrect": true}, {"id": "B", "text": "Bombay High Court", "isCorrect": false}, {"id": "C", "text": "Madras High Court", "isCorrect": false}, {"id": "D", "text": "Allahabad High Court", "isCorrect": false}]',
        'A',
        'The Calcutta High Court was established on July 1, $$1862$$ under the High Courts Act $$1861$$, followed by Bombay and Madras.'
    ),
    (
        'a1110000-0000-0000-0000-000000000078'::uuid,
        'Judiciary',
        'MEDIUM',
        'REMEMBER',
        'Under Article $$222$$, who has the authority to transfer a Judge from one High Court to another?',
        '[{"id": "A", "text": "The President of India, after consultation with the Chief Justice of India", "isCorrect": true}, {"id": "B", "text": "The Chief Justice of India unilaterally", "isCorrect": false}, {"id": "C", "text": "The Governor of the recipient State", "isCorrect": false}, {"id": "D", "text": "The collegium of High Court Chief Justices", "isCorrect": false}]',
        'A',
        'Under Article $$222$$, the President may, after consultation with the Chief Justice of India (via Supreme Court collegium), transfer a judge from one High Court to any other.'
    ),
    (
        'a1110000-0000-0000-0000-000000000079'::uuid,
        'Judiciary',
        'HARD',
        'ANALYZE',
        'The concept of Public Interest Litigation ($$\text{PIL}$$) was introduced to the Indian judicial system primarily by:',
        '[{"id": "A", "text": "Justice P.N. Bhagwati and Justice V.R. Krishna Iyer", "isCorrect": true}, {"id": "B", "text": "Justice H.J. Kania", "isCorrect": false}, {"id": "C", "text": "Justice M. Hidayatullah", "isCorrect": false}, {"id": "D", "text": "Justice Y.V. Chandrachud", "isCorrect": false}]',
        'A',
        'Justices P.N. Bhagwati and V.R. Krishna Iyer pioneered Public Interest Litigation (PIL) in the early 1980s, relaxing the traditional rule of locus standi.'
    ),
    (
        'a1110000-0000-0000-0000-000000000080'::uuid,
        'Judiciary',
        'EASY',
        'REMEMBER',
        'Who was the first Chief Justice of independent India''s Supreme Court?',
        '[{"id": "A", "text": "H.J. Kania (Harilal Jekisundas Kania)", "isCorrect": true}, {"id": "B", "text": "M. Patanjali Sastri", "isCorrect": false}, {"id": "C", "text": "Mehr Chand Mahajan", "isCorrect": false}, {"id": "D", "text": "Sudhi Ranjan Das", "isCorrect": false}]',
        'A',
        'Justice H.J. Kania served as the first Chief Justice of India from January 26, $$1950$$ until his death in November $$1951$$.'
    ),
    (
        'a1110000-0000-0000-0000-000000000081'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment granted constitutional status to Panchayati Raj Institutions in $$1992$$?',
        '[{"id": "A", "text": "$$73\\text{rd}$$ Constitutional Amendment Act", "isCorrect": true}, {"id": "B", "text": "$$74\\text{th}$$ Constitutional Amendment Act", "isCorrect": false}, {"id": "C", "text": "$$61\\text{st}$$ Constitutional Amendment Act", "isCorrect": false}, {"id": "D", "text": "$$44\\text{th}$$ Constitutional Amendment Act", "isCorrect": false}]',
        'A',
        'The $$73\text{rd}$$ Constitutional Amendment Act $$1992$$ added Part IX and the Eleventh Schedule, constitutionalizing Panchayati Raj.'
    ),
    (
        'a1110000-0000-0000-0000-000000000082'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which Schedule of the Constitution lists the $$29$$ functional items within the purview of Panchayats?',
        '[{"id": "A", "text": "Eleventh Schedule ($$11\\text{th}$$ Schedule)", "isCorrect": true}, {"id": "B", "text": "Twelfth Schedule ($$12\\text{th}$$ Schedule)", "isCorrect": false}, {"id": "C", "text": "Tenth Schedule ($$10\\text{th}$$ Schedule)", "isCorrect": false}, {"id": "D", "text": "Ninth Schedule ($$9\\text{th}$$ Schedule)", "isCorrect": false}]',
        'A',
        'The $$11\text{th}$$ Schedule (Article $$243\text{G}$$) contains 29 subjects for Panchayats. The $$12\text{th}$$ Schedule contains 18 subjects for Municipalities.'
    ),
    (
        'a1110000-0000-0000-0000-000000000083'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'What is National Panchayati Raj Day celebrated on every year in India?',
        '[{"id": "A", "text": "April $$24$$", "isCorrect": true}, {"id": "B", "text": "October $$2$$", "isCorrect": false}, {"id": "C", "text": "January $$26$$", "isCorrect": false}, {"id": "D", "text": "November $$26$$", "isCorrect": false}]',
        'A',
        'National Panchayati Raj Day is observed on April 24, commemorating the day the $$73\text{rd}$$ Amendment came into force on April 24, $$1993$$.'
    ),
    (
        'a1110000-0000-0000-0000-000000000084'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'Which committee first recommended the establishment of a 3-tier Panchayati Raj system in India in $$1957$$?',
        '[{"id": "A", "text": "Balwant Rai Mehta Committee", "isCorrect": true}, {"id": "B", "text": "Ashok Mehta Committee", "isCorrect": false}, {"id": "C", "text": "L.M. Singhvi Committee", "isCorrect": false}, {"id": "D", "text": "G.V.K. Rao Committee", "isCorrect": false}]',
        'A',
        'The Balwant Rai Mehta Committee ($$1957$$) recommended democratic decentralization through a 3-tier structure (Gram, Block, and District).'
    ),
    (
        'a1110000-0000-0000-0000-000000000085'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'Which Indian state was the first to establish Panchayati Raj on October 2, $$1959$$ in Nagaur district?',
        '[{"id": "A", "text": "Rajasthan", "isCorrect": true}, {"id": "B", "text": "Andhra Pradesh", "isCorrect": false}, {"id": "C", "text": "Gujarat", "isCorrect": false}, {"id": "D", "text": "Punjab", "isCorrect": false}]',
        'A',
        'Panchayati Raj was first inaugurated by Prime Minister Jawaharlal Nehru on October 2, $$1959$$ in Nagaur district, Rajasthan.'
    ),
    (
        'a1110000-0000-0000-0000-000000000086'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'UNDERSTAND',
        'What is the mandatory minimum reservation for women in Panchayati Raj institutions under Article $$243\text{D}$$?',
        '[{"id": "A", "text": "Not less than one-third ($$\\frac{1}{3}\\text{rd}$$ or $$33\\%$$) of all seats", "isCorrect": true}, {"id": "B", "text": "Fifty percent ($$50\\%$$) across all States uniformly", "isCorrect": false}, {"id": "C", "text": "Twenty-five percent ($$25\\%$$)", "isCorrect": false}, {"id": "D", "text": "Ten percent ($$10\\%$$)", "isCorrect": false}]',
        'A',
        'Article $$243\text{D}(3)$$ provides that not less than one-third of seats must be reserved for women (though many states have voluntarily increased it to $$50\%$$).'
    ),
    (
        'a1110000-0000-0000-0000-000000000087'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'ANALYZE',
        'Who conducts and supervises elections to Panchayats and Municipalities in a State?',
        '[{"id": "A", "text": "The State Election Commission (Article $$243\\text{K}$$)", "isCorrect": true}, {"id": "B", "text": "The Election Commission of India (Article $$324$$)", "isCorrect": false}, {"id": "C", "text": "The District Magistrate / Collector exclusively", "isCorrect": false}, {"id": "D", "text": "The State Legislative Assembly Secretariat", "isCorrect": false}]',
        'A',
        'Under Article $$243\text{K}$$, superintendence, direction, and control of elections to Panchayats is vested in the State Election Commission.'
    ),
    (
        'a1110000-0000-0000-0000-000000000088'::uuid,
        'Local Self-Government & Panchayati Raj',
        'MEDIUM',
        'REMEMBER',
        'The State Finance Commission is constituted every five years by the Governor under which Article?',
        '[{"id": "A", "text": "Article $$243\\text{I}$$", "isCorrect": true}, {"id": "B", "text": "Article $$280$$", "isCorrect": false}, {"id": "C", "text": "Article $$243\\text{Z}$$", "isCorrect": false}, {"id": "D", "text": "Article $$360$$", "isCorrect": false}]',
        'A',
        'Article $$243\text{I}$$ requires the Governor to constitute a State Finance Commission every 5 years to review the financial position of Panchayats.'
    ),
    (
        'a1110000-0000-0000-0000-000000000089'::uuid,
        'Local Self-Government & Panchayati Raj',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment Act accorded constitutional status to Urban Local Bodies (Municipalities)?',
        '[{"id": "A", "text": "$$74\\text{th}$$ Constitutional Amendment Act $$1992$$", "isCorrect": true}, {"id": "B", "text": "$$73\\text{rd}$$ Constitutional Amendment Act $$1992$$", "isCorrect": false}, {"id": "C", "text": "$$65\\text{th}$$ Constitutional Amendment Act", "isCorrect": false}, {"id": "D", "text": "$$86\\text{th}$$ Constitutional Amendment Act", "isCorrect": false}]',
        'A',
        'The $$74\text{th}$$ Amendment Act of $$1992$$ inserted Part IX-A and the Twelfth Schedule to constitutionalize Municipalities.'
    ),
    (
        'a1110000-0000-0000-0000-000000000090'::uuid,
        'Local Self-Government & Panchayati Raj',
        'HARD',
        'UNDERSTAND',
        'The Provisions of the Panchayats (Extension to the Scheduled Areas) Act, $$1996$$ ($$\text{PESA}$$) applies to which Schedule areas?',
        '[{"id": "A", "text": "Fifth Schedule Areas", "isCorrect": true}, {"id": "B", "text": "Sixth Schedule Areas", "isCorrect": false}, {"id": "C", "text": "Eighth Schedule Areas", "isCorrect": false}, {"id": "D", "text": "Seventh Schedule Areas", "isCorrect": false}]',
        'A',
        'PESA ($$1996$$) extends the provisions of Part IX to the tribal areas covered under the Fifth Schedule across 10 states.'
    ),
    (
        'a1110000-0000-0000-0000-000000000091'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'The Election Commission of India is established under which Article of the Constitution?',
        '[{"id": "A", "text": "Article $$324$$", "isCorrect": true}, {"id": "B", "text": "Article $$326$$", "isCorrect": false}, {"id": "C", "text": "Article $$280$$", "isCorrect": false}, {"id": "D", "text": "Article $$315$$", "isCorrect": false}]',
        'A',
        'Article $$324$$ vests the superintendence, direction, and control of elections to Parliament, State Legislatures, and the offices of President/Vice-President in the Election Commission.'
    ),
    (
        'a1110000-0000-0000-0000-000000000092'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution establishes Universal Adult Suffrage as the basis for elections to the Lok Sabha and State Legislative Assemblies?',
        '[{"id": "A", "text": "Article $$326$$", "isCorrect": true}, {"id": "B", "text": "Article $$324$$", "isCorrect": false}, {"id": "C", "text": "Article $$325$$", "isCorrect": false}, {"id": "D", "text": "Article $$327$$", "isCorrect": false}]',
        'A',
        'Article $$326$$ guarantees universal adult suffrage to every Indian citizen who is not less than 18 years of age.'
    ),
    (
        'a1110000-0000-0000-0000-000000000093'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'Which Constitutional Amendment lowered the voting age in India from $$21\text{ years}$$ to $$18\text{ years}$$?',
        '[{"id": "A", "text": "$$61\\text{st}$$ Constitutional Amendment Act $$1988$$", "isCorrect": true}, {"id": "B", "text": "$$42\\text{nd}$$ Constitutional Amendment Act $$1976$$", "isCorrect": false}, {"id": "C", "text": "$$44\\text{th}$$ Constitutional Amendment Act $$1978$$", "isCorrect": false}, {"id": "D", "text": "$$52\\text{nd}$$ Constitutional Amendment Act $$1985$$", "isCorrect": false}]',
        'A',
        'The $$61\text{st}$$ Amendment Act $$1988$$ (effective March $$1989$$) amended Article $$326$$ to reduce the voting age from 21 to 18.'
    ),
    (
        'a1110000-0000-0000-0000-000000000094'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'The Comptroller and Auditor General ($$\text{CAG}$$) of India is appointed under which Article?',
        '[{"id": "A", "text": "Article $$148$$", "isCorrect": true}, {"id": "B", "text": "Article $$280$$", "isCorrect": false}, {"id": "C", "text": "Article $$110$$", "isCorrect": false}, {"id": "D", "text": "Article $$76$$", "isCorrect": false}]',
        'A',
        'Article $$148$$ provides for the office of CAG, described by Dr. B.R. Ambedkar as the most important officer under the Constitution of India.'
    ),
    (
        'a1110000-0000-0000-0000-000000000095'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'What is the tenure of the Comptroller and Auditor General ($$\text{CAG}$$) of India?',
        '[{"id": "A", "text": "$$6\\text{ years}$$ or up to the age of $$65\\text{ years}$$, whichever is earlier", "isCorrect": true}, {"id": "B", "text": "$$5\\text{ years}$$ or up to the age of $$62\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ years}$$ or up to the age of $$65\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "$$6\\text{ years}$$ with no upper age limit", "isCorrect": false}]',
        'A',
        'The CAG holds office for a term of 6 years or until attaining the age of 65 years, whichever occurs earlier.'
    ),
    (
        'a1110000-0000-0000-0000-000000000096'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'The Finance Commission of India is constituted by the President every five years under:',
        '[{"id": "A", "text": "Article $$280$$", "isCorrect": true}, {"id": "B", "text": "Article $$266$$", "isCorrect": false}, {"id": "C", "text": "Article $$267$$", "isCorrect": false}, {"id": "D", "text": "Article $$300\\text{A}$$", "isCorrect": false}]',
        'A',
        'Article $$280$$ provides for a Finance Commission consisting of a Chairman and 4 other members to recommend net tax distribution between Center and States.'
    ),
    (
        'a1110000-0000-0000-0000-000000000097'::uuid,
        'Constitutional Bodies & Amendments',
        'EASY',
        'REMEMBER',
        'National Emergency across India or any part thereof is declared by the President under which Article?',
        '[{"id": "A", "text": "Article $$352$$", "isCorrect": true}, {"id": "B", "text": "Article $$356$$", "isCorrect": false}, {"id": "C", "text": "Article $$360$$", "isCorrect": false}, {"id": "D", "text": "Article $$365$$", "isCorrect": false}]',
        'A',
        'Article $$352$$ deals with National Emergency on grounds of War, External Aggression, or Armed Rebellion (substituted for internal disturbance by the 44th Amendment).'
    ),
    (
        'a1110000-0000-0000-0000-000000000098'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'UNDERSTAND',
        'President''s Rule (State Emergency) on failure of constitutional machinery in a State is imposed under:',
        '[{"id": "A", "text": "Article $$356$$", "isCorrect": true}, {"id": "B", "text": "Article $$352$$", "isCorrect": false}, {"id": "C", "text": "Article $$360$$", "isCorrect": false}, {"id": "D", "text": "Article $$370$$", "isCorrect": false}]',
        'A',
        'Article $$356$$ allows the President to assume all or any functions of the government of a State if its government cannot be carried on in accordance with the Constitution.'
    ),
    (
        'a1110000-0000-0000-0000-000000000099'::uuid,
        'Constitutional Bodies & Amendments',
        'MEDIUM',
        'REMEMBER',
        'Has Financial Emergency under Article $$360$$ ever been declared in India?',
        '[{"id": "A", "text": "No, it has never been invoked so far", "isCorrect": true}, {"id": "B", "text": "Yes, once during the $$1991$$ balance of payments crisis", "isCorrect": false}, {"id": "C", "text": "Yes, twice during wars with neighbors", "isCorrect": false}, {"id": "D", "text": "Yes, during the Covid-19 pandemic in $$2020$$", "isCorrect": false}]',
        'A',
        'Financial Emergency under Article $$360$$ has never been declared in India since the adoption of the Constitution.'
    ),
    (
        'a1110000-0000-0000-0000-000000000100'::uuid,
        'Constitutional Bodies & Amendments',
        'HARD',
        'ANALYZE',
        'The Anti-Defection Law was added to the Constitution as the Tenth Schedule by which Amendment Act?',
        '[{"id": "A", "text": "$$52\\text{nd}$$ Constitutional Amendment Act $$1985$$", "isCorrect": true}, {"id": "B", "text": "$$42\\text{nd}$$ Constitutional Amendment Act $$1976$$", "isCorrect": false}, {"id": "C", "text": "$$91\\text{st}$$ Constitutional Amendment Act $$2003$$", "isCorrect": false}, {"id": "D", "text": "$$44\\text{th}$$ Constitutional Amendment Act $$1978$$", "isCorrect": false}]',
        'A',
        'The $$52\text{nd}$$ Amendment Act of $$1985$$ added the Tenth Schedule, laying down disqualifications on grounds of defection for members of Parliament and State Legislatures.'
    )
) AS v(id, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Indian Polity' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
