-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 2: General Awareness (Set 2: 25 New Qs)
-- UUID Range: a10a0000-0000-0000-0000-000000000001 to a10a0000-0000-0000-0000-000000000025
-- ============================================================

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
    'General Awareness',
    v.topic_name,
    NULL,
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
    -- 1. Current Events (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000001'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which country joined the BRICS grouping as new member on 1 January 2024 alongside Egypt, Ethiopia, Iran, and the United Arab Emirates?',
        '[{"id":"A","text":"Saudi Arabia","isCorrect":true},{"id":"B","text":"Argentina","isCorrect":false},{"id":"C","text":"Nigeria","isCorrect":false},{"id":"D","text":"Indonesia","isCorrect":false}]',
        'A',
        'Saudi Arabia, Egypt, Ethiopia, Iran, and the UAE joined the BRICS coalition expanding the original 5-member bloc.'
    ),
    -- 2. Current Events (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000002'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Who was awarded the prestigious Nobel Peace Prize for 2023 for her tireless fight against the oppression of women in Iran?',
        '[{"id":"A","text":"Narges Mohammadi","isCorrect":true},{"id":"B","text":"Malala Yousafzai","isCorrect":false},{"id":"C","text":"Maria Ressa","isCorrect":false},{"id":"D","text":"Tawakkol Karman","isCorrect":false}]',
        'A',
        'Narges Mohammadi was awarded the Nobel Peace Prize 2023 while imprisoned in Tehran.'
    ),
    -- 3. Current Events (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000003'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which Indian state emerged on top of the medal tally at the 37th National Games held in Goa in October-November 2023?',
        '[{"id":"A","text":"Maharashtra","isCorrect":true},{"id":"B","text":"Services Sports Control Board","isCorrect":false},{"id":"C","text":"Haryana","isCorrect":false},{"id":"D","text":"Karnataka","isCorrect":false}]',
        'A',
        'Maharashtra won the Raja Bhalindra Singh Trophy with a total of 228 medals (80 gold).'
    ),
    -- 4. Current Events (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000004'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'What is the designated name of the landing site where ISRO Chandrayaan-3 lander Vikram touched down near the Lunar South Pole?',
        '[{"id":"A","text":"Shiv Shakti Point","isCorrect":true},{"id":"B","text":"Tiranga Point","isCorrect":false},{"id":"C","text":"Jawahar Point","isCorrect":false},{"id":"D","text":"Atal Point","isCorrect":false}]',
        'A',
        'Prime Minister Narendra Modi named the Chandrayaan-3 landing site "Shiv Shakti Point" and designated August 23 as National Space Day.'
    ),
    -- 5. Current Events (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000005'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which city was officially declared India first AI City with a dedicated artificial intelligence technological hub?',
        '[{"id":"A","text":"Lucknow","isCorrect":true},{"id":"B","text":"Bengaluru","isCorrect":false},{"id":"C","text":"Hyderabad","isCorrect":false},{"id":"D","text":"Pune","isCorrect":false}]',
        'A',
        'The Government of Uttar Pradesh initiated development of India first AI City in Nadarganj, Lucknow.'
    ),
    -- 6. History (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000006'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'Which Mauryan ruler embraced Buddhism after witnessing the devastation of the Kalinga War in $$261\\text{ BCE}$$?',
        '[{"id":"A","text":"Ashoka","isCorrect":true},{"id":"B","text":"Chandragupta Maurya","isCorrect":false},{"id":"C","text":"Bindusara","isCorrect":false},{"id":"D","text":"Brihadratha","isCorrect":false}]',
        'A',
        'Emperor Ashoka converted to Buddhism under the guidance of monk Upagupta, replacing Bherighosha with Dhammaghosha.'
    ),
    -- 7. History (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000007'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'The Permanent Settlement of Bengal was introduced in $$1793$$ by which British Governor-General?',
        '[{"id":"A","text":"Lord Cornwallis","isCorrect":true},{"id":"B","text":"Warren Hastings","isCorrect":false},{"id":"C","text":"Lord Wellesley","isCorrect":false},{"id":"D","text":"Lord William Bentinck","isCorrect":false}]',
        'A',
        'Lord Cornwallis introduced the Zamindari system / Permanent Settlement in Bengal, Bihar, and Orissa, fixing revenues permanently.'
    ),
    -- 8. History (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000008'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'In which year did Mahatma Gandhi launch the historic Salt Satyagraha (Dandi March) from Sabarmati Ashram to Dandi?',
        '[{"id":"A","text":"$$1930$$","isCorrect":true},{"id":"B","text":"$$1920$$","isCorrect":false},{"id":"C","text":"$$1942$$","isCorrect":false},{"id":"D","text":"$$1919$$","isCorrect":false}]',
        'A',
        'The Dandi March was undertaken from 12 March 1930 to 6 April 1930, inaugurating the Civil Disobedience Movement.'
    ),
    -- 9. History (MEDIUM, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000009'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'The famous Iron Pillar of Delhi located in the Qutb complex belongs to the reign of which monarch?',
        '[{"id":"A","text":"Chandragupta II (Vikramaditya)","isCorrect":true},{"id":"B","text":"Samudragupta","isCorrect":false},{"id":"C","text":"Kanishka","isCorrect":false},{"id":"D","text":"Harshavardhana","isCorrect":false}]',
        'A',
        'The rust-resistant Iron Pillar bears a Brahmi inscription mentioning a powerful king named "Chandra", identified as Chandragupta II.'
    ),
    -- 10. Geography (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000010'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'The Tropic of Cancer ($$23^\\circ 30''\\text{ N}$$) passes through how many Indian states?',
        '[{"id":"A","text":"$$8$$","isCorrect":true},{"id":"B","text":"$$7$$","isCorrect":false},{"id":"C","text":"$$9$$","isCorrect":false},{"id":"D","text":"$$6$$","isCorrect":false}]',
        'A',
        'It passes through Gujarat, Rajasthan, Madhya Pradesh, Chhattisgarh, Jharkhand, West Bengal, Tripura, and Mizoram.'
    ),
    -- 11. Geography (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000011'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'Which is the highest peak in the Western Ghats and the highest point in South India?',
        '[{"id":"A","text":"Anamudi","isCorrect":true},{"id":"B","text":"Doddabetta","isCorrect":false},{"id":"C","text":"Kalsubai","isCorrect":false},{"id":"D","text":"Mahendragiri","isCorrect":false}]',
        'A',
        'Anamudi ($$2,695\\text{ m}$$) is located in the Anaimalai Hills of Kerala inside Eravikulam National Park.'
    ),
    -- 12. Geography (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000012'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'Which river is known as the "Sorrow of Bengal" due to its devastating historic monsoon inundations?',
        '[{"id":"A","text":"Damodar River","isCorrect":true},{"id":"B","text":"Kosi River","isCorrect":false},{"id":"C","text":"Hooghly River","isCorrect":false},{"id":"D","text":"Teesta River","isCorrect":false}]',
        'A',
        'The Damodar River was called the Sorrow of Bengal before the construction of dams by the Damodar Valley Corporation (DVC).'
    ),
    -- 13. Geography (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000013'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'The Rohtang Pass, now bypassed by the Atal Tunnel, connects the Kullu Valley with which valley in Himachal Pradesh?',
        '[{"id":"A","text":"Lahaul and Spiti Valley","isCorrect":true},{"id":"B","text":"Kangra Valley","isCorrect":false},{"id":"C","text":"Kinnaur Valley","isCorrect":false},{"id":"D","text":"Chamba Valley","isCorrect":false}]',
        'A',
        'Rohtang Pass on the Pir Panjal Range connects Kullu with Lahaul and Spiti.'
    ),
    -- 14. Culture (EASY, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000014'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'The classical dance form "Bharatanatyam" originated and flourished in which Indian state?',
        '[{"id":"A","text":"Tamil Nadu","isCorrect":true},{"id":"B","text":"Andhra Pradesh","isCorrect":false},{"id":"C","text":"Odisha","isCorrect":false},{"id":"D","text":"Assam","isCorrect":false}]',
        'A',
        'Bharatanatyam traces its lineage to the ancient Natya Shastra and flourished through the temple Devadasi tradition in Tamil Nadu.'
    ),
    -- 15. Culture (EASY, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000015'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'The famous Khajuraho Group of Monuments in Madhya Pradesh were constructed by which medieval dynasty?',
        '[{"id":"A","text":"Chandela Dynasty","isCorrect":true},{"id":"B","text":"Paramara Dynasty","isCorrect":false},{"id":"C","text":"Solanki Dynasty","isCorrect":false},{"id":"D","text":"Rashtrakuta Dynasty","isCorrect":false}]',
        'A',
        'The Chandelas built the UNESCO World Heritage Nagara-style temples at Khajuraho between $$950\\text{ and }1050\\text{ CE}$$.'
    ),
    -- 16. Culture (EASY, REMEMBER)
    (
        'a10a0000-0000-0000-0000-000000000016'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'In which Indian state is the harvest festival "Bihu" celebrated across three seasonal intervals (Bohag, Kati, Magh)?',
        '[{"id":"A","text":"Assam","isCorrect":true},{"id":"B","text":"Manipur","isCorrect":false},{"id":"C","text":"Tripura","isCorrect":false},{"id":"D","text":"West Bengal","isCorrect":false}]',
        'A',
        'Bihu is the prime festival of Assam, signifying different phases of agricultural paddy cultivation.'
    ),
    -- 17. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000017'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'What is the rate at which commercial banks borrow short-term liquidity from the Reserve Bank of India against government securities called?',
        '[{"id":"A","text":"Repo Rate","isCorrect":true},{"id":"B","text":"Reverse Repo Rate","isCorrect":false},{"id":"C","text":"Bank Rate","isCorrect":false},{"id":"D","text":"Cash Reserve Ratio","isCorrect":false}]',
        'A',
        'Repo (Repurchase Option) Rate is the rate at which RBI lends funds to commercial banks against pledged government collateral.'
    ),
    -- 18. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000018'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'Gross Domestic Product (GDP) calculated at constant base-year market prices is referred to as:',
        '[{"id":"A","text":"Real GDP","isCorrect":true},{"id":"B","text":"Nominal GDP","isCorrect":false},{"id":"C","text":"Net National Product","isCorrect":false},{"id":"D","text":"Gross National Income","isCorrect":false}]',
        'A',
        'Real GDP measures output at constant prices, adjusting for price changes and inflation.'
    ),
    -- 19. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000019'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'Which statutory regulatory institution oversees and regulates commodity derivative and equities capital markets in India?',
        '[{"id":"A","text":"Securities and Exchange Board of India (SEBI)","isCorrect":true},{"id":"B","text":"Reserve Bank of India (RBI)","isCorrect":false},{"id":"C","text":"IRDAI","isCorrect":false},{"id":"D","text":"Competition Commission of India","isCorrect":false}]',
        'A',
        'SEBI was established as a statutory body in 1992 under the SEBI Act to protect investor interests and regulate securities markets.'
    ),
    -- 20. General Policy (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000020'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'Which Constitutional Amendment Act reduced the voting age for Lok Sabha and State Legislative Assembly elections from $$21\\text{ to }18\\text{ years}$$?',
        '[{"id":"A","text":"$$61^{\\text{st}}$$ Constitutional Amendment Act, $$1988$$","isCorrect":true},{"id":"B","text":"$$42^{\\text{nd}}$$ Constitutional Amendment Act, $$1976$$","isCorrect":false},{"id":"C","text":"$$44^{\\text{th}}$$ Constitutional Amendment Act, $$1978$$","isCorrect":false},{"id":"D","text":"$$73^{\\text{rd}}$$ Constitutional Amendment Act, $$1992$$","isCorrect":false}]',
        'A',
        'The 61st Amendment amended Article 326 to lower the voting age from 21 to 18 years.'
    ),
    -- 21. General Policy (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000021'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$76$$ of the Constitution of India, who is appointed by the President as the highest legal officer in the country?',
        '[{"id":"A","text":"Attorney General for India","isCorrect":true},{"id":"B","text":"Solicitor General of India","isCorrect":false},{"id":"C","text":"Advocate General","isCorrect":false},{"id":"D","text":"Chief Justice of India","isCorrect":false}]',
        'A',
        'The Attorney General for India is appointed under Article 76 and holds office during the pleasure of the President.'
    ),
    -- 22. General Policy (MEDIUM, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000022'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'Which writ is issued by High Courts or the Supreme Court commanding a public official or statutory body to perform a public duty that they have failed to perform?',
        '[{"id":"A","text":"Mandamus","isCorrect":true},{"id":"B","text":"Habeas Corpus","isCorrect":false},{"id":"C","text":"Quo-Warranto","isCorrect":false},{"id":"D","text":"Certiorari","isCorrect":false}]',
        'A',
        'Mandamus literally means "We Command" and compels public officials or lower tribunals to perform their lawful duties.'
    ),
    -- 23. Everyday Science (EASY, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000023'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Which gas is responsible for the effervescence and fizz when opening a carbonated soft drink bottle?',
        '[{"id":"A","text":"Carbon Dioxide ($$\\text{CO}_2$$)","isCorrect":true},{"id":"B","text":"Nitrous Oxide ($$\\text{N}_2\\text{O}$$)","isCorrect":false},{"id":"C","text":"Oxygen ($$\\text{O}_2$$)","isCorrect":false},{"id":"D","text":"Hydrogen ($$\\text{H}_2$$)","isCorrect":false}]',
        'A',
        'Carbon dioxide dissolved under high pressure forms dilute carbonic acid; releasing pressure releases $$\\text{CO}_2$$ gas bubbles.'
    ),
    -- 24. Everyday Science (EASY, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000024'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Which chemical element is the primary constituent of natural gas and kitchen biogas?',
        '[{"id":"A","text":"Methane ($$\\text{CH}_4$$)","isCorrect":true},{"id":"B","text":"Propane ($$\\text{C}_3\\text{H}_8$$)","isCorrect":false},{"id":"C","text":"Butane ($$\\text{C}_4\\text{H}_{10}$$)","isCorrect":false},{"id":"D","text":"Ethane ($$\\text{C}_2\\text{H}_6$$)","isCorrect":false}]',
        'A',
        'Methane ($$\\text{CH}_4$$) constitutes over $$70-90\\%$$ of natural gas and $$55-70\\%$$ of biogas.'
    ),
    -- 25. Everyday Science (EASY, UNDERSTAND)
    (
        'a10a0000-0000-0000-0000-000000000025'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Myopia (short-sightedness) is an ocular condition corrected by using which type of lens?',
        '[{"id":"A","text":"Concave lens","isCorrect":true},{"id":"B","text":"Convex lens","isCorrect":false},{"id":"C","text":"Cylindrical lens","isCorrect":false},{"id":"D","text":"Bifocal lens","isCorrect":false}]',
        'A',
        'In myopia, the image of distant objects forms in front of the retina. A diverging (concave) lens moves the image onto the retina.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
