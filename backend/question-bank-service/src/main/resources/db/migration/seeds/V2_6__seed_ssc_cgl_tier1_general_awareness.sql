-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 2: General Awareness (25 New Qs)
-- UUID Range: a1060000-0000-0000-0000-000000000001 to a1060000-0000-0000-0000-000000000025
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
        'a1060000-0000-0000-0000-000000000001'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which nation hosted the 18th G20 Leaders Summit in September 2023 under the theme "Vasudhaiva Kutumbakam" (One Earth, One Family, One Future)?',
        '[{"id":"A","text":"India","isCorrect":true},{"id":"B","text":"Brazil","isCorrect":false},{"id":"C","text":"Indonesia","isCorrect":false},{"id":"D","text":"South Africa","isCorrect":false}]',
        'A',
        'India held the G20 Presidency from Dec 1, 2022 to Nov 30, 2023, hosting the main summit at Bharat Mandapam, New Delhi.'
    ),
    -- 2. Current Events (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000002'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Who was crowned champion of the ICC Men Cricket World Cup 2023 held across India?',
        '[{"id":"A","text":"Australia","isCorrect":true},{"id":"B","text":"India","isCorrect":false},{"id":"C","text":"South Africa","isCorrect":false},{"id":"D","text":"England","isCorrect":false}]',
        'A',
        'Australia defeated India in the final at Narendra Modi Stadium, Ahmedabad to win their 6th Men Cricket World Cup title.'
    ),
    -- 3. Current Events (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000003'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which multilateral organization was formally admitted as a permanent 21st member of the G20 during the 2023 New Delhi Summit?',
        '[{"id":"A","text":"African Union","isCorrect":true},{"id":"B","text":"ASEAN","isCorrect":false},{"id":"C","text":"OPEC","isCorrect":false},{"id":"D","text":"Mercosur","isCorrect":false}]',
        'A',
        'The African Union (55 member states) was inducted as a permanent member under India G20 Presidency.'
    ),
    -- 4. Current Events (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000004'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'What was the official name of the solar observatory spacecraft launched by ISRO in September 2023 to study the Sun from the Sun-Earth L1 Lagrange point?',
        '[{"id":"A","text":"Aditya-L1","isCorrect":true},{"id":"B","text":"Surya-1","isCorrect":false},{"id":"C","text":"Bhaskara-L1","isCorrect":false},{"id":"D","text":"Helios-India","isCorrect":false}]',
        'A',
        'ISRO launched Aditya-L1 via PSLV-C57 to observe solar coronas, flares, and solar wind dynamics from a halo orbit around the L1 Lagrangian point.'
    ),
    -- 5. Current Events (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000005'::uuid,
        'Current Events',
        'MEDIUM',
        'REMEMBER',
        'Which Indian state launched the "Mukhyamantri Mahila Udyamita Abhiyan" to foster rural women micro-entrepreneurs?',
        '[{"id":"A","text":"Assam","isCorrect":true},{"id":"B","text":"Odisha","isCorrect":false},{"id":"C","text":"Rajasthan","isCorrect":false},{"id":"D","text":"Madhya Pradesh","isCorrect":false}]',
        'A',
        'Assam Government launched the scheme to assist women Self-Help Group (SHG) members in establishing micro-enterprises.'
    ),
    -- 6. History (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000006'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'Who was the founder of the famous Nalanda Mahavihara university in ancient Magadha during the 5th century CE?',
        '[{"id":"A","text":"Kumaragupta I","isCorrect":true},{"id":"B","text":"Chandragupta II","isCorrect":false},{"id":"C","text":"Samudragupta","isCorrect":false},{"id":"D","text":"Harshavardhana","isCorrect":false}]',
        'A',
        'Gupta monarch Kumaragupta I (Shakraditya) founded Nalanda University around $$427\\text{ CE}$$.'
    ),
    -- 7. History (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000007'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'The Battle of Khanwa was fought in $$1527\\text{ CE}$$ between Mughal Emperor Babur and which Rajput ruler?',
        '[{"id":"A","text":"Rana Sanga of Mewar","isCorrect":true},{"id":"B","text":"Rana Kumbha","isCorrect":false},{"id":"C","text":"Maharana Pratap","isCorrect":false},{"id":"D","text":"Raja Man Singh","isCorrect":false}]',
        'A',
        'The Battle of Khanwa ($$1527$$) cemented Mughal rule in Northern India after Babur defeated the confederacy led by Rana Sanga.'
    ),
    -- 8. History (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000008'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'Who presided over the historic Lahore Session of the Indian National Congress in December $$1929$$, where the resolution for "Purna Swaraj" (Complete Independence) was passed?',
        '[{"id":"A","text":"Jawaharlal Nehru","isCorrect":true},{"id":"B","text":"Mahatma Gandhi","isCorrect":false},{"id":"C","text":"Subhas Chandra Bose","isCorrect":false},{"id":"D","text":"Motilal Nehru","isCorrect":false}]',
        'A',
        'Jawaharlal Nehru presided over the $$1929$$ Lahore Session, declaring January $$26, 1930$$ as Independence Day.'
    ),
    -- 9. History (MEDIUM, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000009'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'The Mansabdari system, an integrated administrative and military ranking structure, was introduced by which Mughal Emperor?',
        '[{"id":"A","text":"Akbar","isCorrect":true},{"id":"B","text":"Humayun","isCorrect":false},{"id":"C","text":"Shah Jahan","isCorrect":false},{"id":"D","text":"Aurangzeb","isCorrect":false}]',
        'A',
        'Akbar introduced the Mansabdari system in $$1571$$, dividing ranks into Jat (personal rank/salary) and Sawar (number of cavalrymen maintained).'
    ),
    -- 10. Geography (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000010'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'The Ten Degree Channel ($$10^\\circ\\text{ N}$$ latitude) separates which two island groups in the Bay of Bengal?',
        '[{"id":"A","text":"Andaman Islands and Nicobar Islands","isCorrect":true},{"id":"B","text":"Lakshadweep and Maldives","isCorrect":false},{"id":"C","text":"Minicoy and Maldives","isCorrect":false},{"id":"D","text":"South Andaman and Little Andaman","isCorrect":false}]',
        'A',
        'The Ten Degree Channel separates Little Andaman in the north from Car Nicobar in the south.'
    ),
    -- 11. Geography (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000011'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'Which mountain pass connects Srinagar in Jammu and Kashmir with Leh in Ladakh across the Great Himalayan Range?',
        '[{"id":"A","text":"Zoji La Pass","isCorrect":true},{"id":"B","text":"Rohtang Pass","isCorrect":false},{"id":"C","text":"Shipki La Pass","isCorrect":false},{"id":"D","text":"Nathu La Pass","isCorrect":false}]',
        'A',
        'Zoji La Pass (altitude $$\\approx 3,528\\text{ m}$$) carries National Highway 1 between Srinagar and Leh.'
    ),
    -- 12. Geography (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000012'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'The famous Majuli river island, recognized as the world largest inhabited river island, is formed by which river system?',
        '[{"id":"A","text":"Brahmaputra River","isCorrect":true},{"id":"B","text":"Ganga River","isCorrect":false},{"id":"C","text":"Godavari River","isCorrect":false},{"id":"D","text":"Indus River","isCorrect":false}]',
        'A',
        'Majuli is an alluvial island formed by the Brahmaputra River and its anabranch the Kherkutia Xuti in Assam.'
    ),
    -- 13. Geography (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000013'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'Which soil type in India is self-ploughing, rich in montmorillonite clay minerals, and ideal for rain-fed cotton cultivation?',
        '[{"id":"A","text":"Black Soil (Regur)","isCorrect":true},{"id":"B","text":"Alluvial Soil","isCorrect":false},{"id":"C","text":"Laterite Soil","isCorrect":false},{"id":"D","text":"Red and Yellow Soil","isCorrect":false}]',
        'A',
        'Black soil (Regur) develops deep cracks during dry seasons (self-aerating) and expands into sticky clay when wet, retaining moisture excellently for cotton.'
    ),
    -- 14. Culture (EASY, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000014'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'The Sun Temple of Konark in Odisha, engineered in the shape of a colossal chariot with $$24$$ carved stone wheels, was built by which medieval monarch?',
        '[{"id":"A","text":"King Narasimhadeva I (Eastern Ganga Dynasty)","isCorrect":true},{"id":"B","text":"Anantavarman Chodaganga","isCorrect":false},{"id":"C","text":"Kapilendra Deva","isCorrect":false},{"id":"D","text":"Kharavela","isCorrect":false}]',
        'A',
        'King Narasimhadeva I built the Black Pagoda / Konark Sun Temple in the 13th century ($$1250\\text{ CE}$$).'
    ),
    -- 15. Culture (EASY, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000015'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'Hornbill Festival, known as the "Festival of Festivals", is celebrated annually in December in which north-eastern Indian state?',
        '[{"id":"A","text":"Nagaland","isCorrect":true},{"id":"B","text":"Meghalaya","isCorrect":false},{"id":"C","text":"Mizoram","isCorrect":false},{"id":"D","text":"Arunachal Pradesh","isCorrect":false}]',
        'A',
        'The Hornbill Festival is held at Naga Heritage Village Kisama near Kohima, Nagaland to showcase inter-tribal heritage.'
    ),
    -- 16. Culture (EASY, REMEMBER)
    (
        'a1060000-0000-0000-0000-000000000016'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'Kathakali, a classical dance-drama featuring elaborate facial makeup (Aharya) and dramatic costumes, belongs to which state?',
        '[{"id":"A","text":"Kerala","isCorrect":true},{"id":"B","text":"Tamil Nadu","isCorrect":false},{"id":"C","text":"Karnataka","isCorrect":false},{"id":"D","text":"Andhra Pradesh","isCorrect":false}]',
        'A',
        'Kathakali originated in the southwestern coastal state of Kerala, narrating stories from the Ramayana and Mahabharata.'
    ),
    -- 17. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000017'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'What type of inflation occurs when persistent aggregate demand exceeds the aggregate productive capacity of an economy at full employment?',
        '[{"id":"A","text":"Demand-Pull Inflation","isCorrect":true},{"id":"B","text":"Cost-Push Inflation","isCorrect":false},{"id":"C","text":"Stagflation","isCorrect":false},{"id":"D","text":"Deflationary Spiral","isCorrect":false}]',
        'A',
        'Demand-pull inflation happens when "too much money chases too few goods", pulling up prices due to elevated spending.'
    ),
    -- 18. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000018'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'The Lorenz Curve is a graphical representation of which economic parameter?',
        '[{"id":"A","text":"Income or wealth distribution inequality","isCorrect":true},{"id":"B","text":"Relationship between tax rates and tax revenue","isCorrect":false},{"id":"C","text":"Trade-off between unemployment and inflation","isCorrect":false},{"id":"D","text":"Foreign exchange volatility","isCorrect":false}]',
        'A',
        'The Lorenz curve plots cumulative percentage of national income against cumulative population, from which the Gini Coefficient is derived.'
    ),
    -- 19. Economic Scene (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000019'::uuid,
        'Economic Scene',
        'MEDIUM',
        'UNDERSTAND',
        'Under the Goods and Services Tax (GST) framework in India, who acts as the ex-officio Chairperson of the GST Council established under Article $$279\\text{A}$$?',
        '[{"id":"A","text":"Union Finance Minister","isCorrect":true},{"id":"B","text":"Prime Minister of India","isCorrect":false},{"id":"C","text":"Governor of RBI","isCorrect":false},{"id":"D","text":"Cabinet Secretary","isCorrect":false}]',
        'A',
        'The Union Finance Minister chairs the GST Council, comprising the Union Minister of State in charge of Revenue/Finance and state Finance Ministers.'
    ),
    -- 20. General Policy (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000020'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'Which Schedule of the Constitution of India contains provisions relating to the disqualification of members of Parliament and State Legislatures on ground of defection (Anti-Defection Law)?',
        '[{"id":"A","text":"Tenth Schedule","isCorrect":true},{"id":"B","text":"Seventh Schedule","isCorrect":false},{"id":"C","text":"Eighth Schedule","isCorrect":false},{"id":"D","text":"Eleventh Schedule","isCorrect":false}]',
        'A',
        'The Tenth Schedule was added by the $$52^{\\text{nd}}$$ Constitutional Amendment Act of $$1985$$ to curb political defections.'
    ),
    -- 21. General Policy (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000021'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'Under Article $$368$$ of the Constitution of India, which type of constitutional amendment requires ratification by legislatures of not less than one-half of the States?',
        '[{"id":"A","text":"Amendments affecting federal provisions such as distribution of legislative powers between Union and States","isCorrect":true},{"id":"B","text":"Creation of new states or alteration of boundaries","isCorrect":false},{"id":"C","text":"Abolition or creation of Legislative Councils in States","isCorrect":false},{"id":"D","text":"Amendments to Fundamental Duties","isCorrect":false}]',
        'A',
        'Amendments affecting federal structures (Article 54, 55, 73, 162, 241, Chapter IV of Part V, Seventh Schedule, or Article 368 itself) require special majority + state ratification.'
    ),
    -- 22. General Policy (MEDIUM, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000022'::uuid,
        'General Policy',
        'MEDIUM',
        'UNDERSTAND',
        'The Directive Principles of State Policy (DPSP) in Part IV of the Indian Constitution were borrowed from which country Constitution?',
        '[{"id":"A","text":"Irish Constitution","isCorrect":true},{"id":"B","text":"US Constitution","isCorrect":false},{"id":"C","text":"British Constitution","isCorrect":false},{"id":"D","text":"Australian Constitution","isCorrect":false}]',
        'A',
        'Articles $$36$$ to $$51$$ (DPSPs) were inspired by the Irish Constitution of $$1937$$, which had derived them from the Spanish Constitution.'
    ),
    -- 23. Everyday Science (EASY, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000023'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Why does a diamond sparkle with intense brilliance compared to an ordinary cut glass stone?',
        '[{"id":"A","text":"Due to a very high refractive index ($$2.42$$) and small critical angle ($$24.4^\\circ$$) causing multiple Total Internal Reflections","isCorrect":true},{"id":"B","text":"Because diamond naturally emits phosphorescent photons","isCorrect":false},{"id":"C","text":"Due to external mirror reflection from the polished surface alone","isCorrect":false},{"id":"D","text":"Because light speed increases inside the diamond lattice","isCorrect":false}]',
        'A',
        'Diamond has a high refractive index ($$\\approx 2.42$$) leading to a tiny critical angle ($$\\approx 24.4^\\circ$$); light trapped inside undergoes numerous Total Internal Reflections.'
    ),
    -- 24. Everyday Science (EASY, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000024'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Which organelle in eukaryotic cells contains its own circular DNA and $$70\\text{S}$$ ribosomes, functioning as the "Powerhouse of the Cell"?',
        '[{"id":"A","text":"Mitochondrion","isCorrect":true},{"id":"B","text":"Endoplasmic Reticulum","isCorrect":false},{"id":"C","text":"Golgi Complex","isCorrect":false},{"id":"D","text":"Lysosome","isCorrect":false}]',
        'A',
        'Mitochondria generate ATP via oxidative phosphorylation and possess semi-autonomous circular DNA (mtDNA) supported by endosymbiotic theory.'
    ),
    -- 25. Everyday Science (EASY, UNDERSTAND)
    (
        'a1060000-0000-0000-0000-000000000025'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Dry ice, widely used as a cooling and freezing agent, is the solid aggregate form of which compound?',
        '[{"id":"A","text":"Carbon Dioxide ($$\\text{CO}_2$$)","isCorrect":true},{"id":"B","text":"Carbon Monoxide ($$\\text{CO}$$)","isCorrect":false},{"id":"C","text":"Nitrogen Dioxide ($$\\text{NO}_2$$)","isCorrect":false},{"id":"D","text":"Sulfur Hexafluoride ($$\\text{SF}_6$$)","isCorrect":false}]',
        'A',
        'Dry ice is solid $$\\text{CO}_2$$ which sublimates directly from solid to gas at $$-78.5^\\circ\\text{C}$$ without leaving any liquid residue.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
