-- ============================================================
-- Seed Questions: General Awareness (SSC & RRB)
-- Format Standard: All numbers/scientific units in $$...$$, hex UUIDs, JSONB escaped
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
    -- 1. Indian Polity (Constitution / Fundamental Rights)
    (
        'a1030000-0000-0000-0000-000000000001'::uuid,
        'General Policy',
        'EASY',
        'REMEMBER',
        'Which Article of the Constitution of India guarantees the Right to Constitutional Remedies, referred to by Dr. B.R. Ambedkar as the "Heart and Soul of the Constitution"?',
        '[{"id":"A","text":"Article $$32$$","isCorrect":true},{"id":"B","text":"Article $$21$$","isCorrect":false},{"id":"C","text":"Article $$14$$","isCorrect":false},{"id":"D","text":"Article $$19$$","isCorrect":false}]',
        'A',
        'Article $$32$$ gives citizens the right to move the Supreme Court directly for the enforcement of Fundamental Rights via writs like Habeas Corpus, Mandamus, Prohibition, Quo-Warranto, and Certiorari.'
    ),
    -- 2. Physical Geography (River Systems)
    (
        'a1030000-0000-0000-0000-000000000002'::uuid,
        'Geography',
        'EASY',
        'REMEMBER',
        'Which of the following peninsular rivers in India flows westwards through a rift valley and drains into the Arabian Sea?',
        '[{"id":"A","text":"Narmada","isCorrect":true},{"id":"B","text":"Godavari","isCorrect":false},{"id":"C","text":"Krishna","isCorrect":false},{"id":"D","text":"Mahanadi","isCorrect":false}]',
        'A',
        'The Narmada and Tapi rivers flow westwards through tectonic rift valleys between the Vindhya and Satpura ranges and empty into the Arabian Sea.'
    ),
    -- 3. Ancient History (Indus Valley / Buddhism)
    (
        'a1030000-0000-0000-0000-000000000003'::uuid,
        'History',
        'MEDIUM',
        'UNDERSTAND',
        'At which ancient Harappan site was the famous dockyard and tidal port discovered by archaeologist S.R. Rao in $$1954$$?',
        '[{"id":"A","text":"Lothal","isCorrect":true},{"id":"B","text":"Kalibangan","isCorrect":false},{"id":"C","text":"Dholavira","isCorrect":false},{"id":"D","text":"Rakhigarhi","isCorrect":false}]',
        'A',
        'Lothal, located in the Bhal region of Gujarat on the Bhogava river, contained a massive brick dockyard connected to an old channel of the Sabarmati river.'
    ),
    -- 4. Everyday Science (Physics - Optics)
    (
        'a1030000-0000-0000-0000-000000000004'::uuid,
        'Everyday Science',
        'EASY',
        'UNDERSTAND',
        'Optical fibers transmit light signals over long distances with minimal loss primarily using which optical phenomenon?',
        '[{"id":"A","text":"Total Internal Reflection","isCorrect":true},{"id":"B","text":"Diffraction of Light","isCorrect":false},{"id":"C","text":"Refraction through Prism","isCorrect":false},{"id":"D","text":"Polarization","isCorrect":false}]',
        'A',
        'Optical fibers operate on Total Internal Reflection (TIR), where light entering the core at an angle greater than the critical angle is completely reflected within the core cladding.'
    ),
    -- 5. Indian Economy (Monetary Policy)
    (
        'a1030000-0000-0000-0000-000000000005'::uuid,
        'Economic Scene',
        'MEDIUM',
        'APPLY',
        'When the Reserve Bank of India (RBI) increases the Cash Reserve Ratio (CRR), what is the direct impact on the commercial banking system?',
        '[{"id":"A","text":"It decreases the lendable reserves and credit creation capacity of commercial banks","isCorrect":true},{"id":"B","text":"It increases liquidity in the market","isCorrect":false},{"id":"C","text":"It decreases interest rates immediately","isCorrect":false},{"id":"D","text":"It increases bank profitability without affecting money supply","isCorrect":false}]',
        'A',
        'Higher CRR requires commercial banks to hold a larger fraction of their Net Demand and Time Liabilities (NDTL) as liquid cash with the RBI, thereby reducing the available funds for lending.'
    ),
    -- 6. Everyday Science (Chemistry - Metallurgy)
    (
        'a1030000-0000-0000-0000-000000000006'::uuid,
        'Everyday Science',
        'EASY',
        'REMEMBER',
        'Galvanization is a metallurgical process used to prevent the rusting of iron by applying a protective coating of which metal?',
        '[{"id":"A","text":"Zinc ($$\\text{Zn}$$)","isCorrect":true},{"id":"B","text":"Copper ($$\\text{Cu}$$)","isCorrect":false},{"id":"C","text":"Aluminium ($$\\text{Al}$$)","isCorrect":false},{"id":"D","text":"Tin ($$\\text{Sn}$$)","isCorrect":false}]',
        'A',
        'Galvanization deposits a thin protective layer of zinc over iron/steel. Zinc acts as a sacrificial anode because it has a higher oxidation potential than iron.'
    ),
    -- 7. Modern History (Freedom Struggle)
    (
        'a1030000-0000-0000-0000-000000000007'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'Under whose Viceroyalty was the Vernacular Press Act of $$1878$$ enacted to curtail the freedom of the Indian-language press?',
        '[{"id":"A","text":"Lord Lytton","isCorrect":true},{"id":"B","text":"Lord Ripon","isCorrect":false},{"id":"C","text":"Lord Curzon","isCorrect":false},{"id":"D","text":"Lord Dalhousie","isCorrect":false}]',
        'A',
        'Lord Lytton enacted the Vernacular Press Act in $$1878$$ (nicknamed the "Gagging Act"), which was later repealed in $$1882$$ by Lord Ripon.'
    ),
    -- 8. Everyday Science (Biology - Endocrine System)
    (
        'a1030000-0000-0000-0000-000000000008'::uuid,
        'Everyday Science',
        'EASY',
        'REMEMBER',
        'Which endocrine gland in the human body is situated in the sella turcica at the base of the brain and is known as the "Master Gland"?',
        '[{"id":"A","text":"Pituitary Gland","isCorrect":true},{"id":"B","text":"Thyroid Gland","isCorrect":false},{"id":"C","text":"Adrenal Gland","isCorrect":false},{"id":"D","text":"Pancreas","isCorrect":false}]',
        'A',
        'The pituitary gland regulates and coordinates hormone secretion across multiple peripheral endocrine organs, earning it the title Master Gland.'
    ),
    -- 9. Geography (Atmosphere Layers)
    (
        'a1030000-0000-0000-0000-000000000009'::uuid,
        'Geography',
        'EASY',
        'UNDERSTAND',
        'In which layer of Earth atmosphere is the ozone layer ($$\\text{O}_3$$) primarily concentrated, absorbing harmful ultraviolet ($$\\text{UV}$$) radiation?',
        '[{"id":"A","text":"Stratosphere","isCorrect":true},{"id":"B","text":"Troposphere","isCorrect":false},{"id":"C","text":"Mesosphere","isCorrect":false},{"id":"D","text":"Thermosphere","isCorrect":false}]',
        'A',
        'The ozone layer is located in the stratosphere, between roughly $$15\\text{ km}$$ and $$35\\text{ km}$$ above the Earth surface.'
    ),
    -- 10. Indian Polity (Parliament / Bills)
    (
        'a1030000-0000-0000-0000-000000000010'::uuid,
        'General Policy',
        'MEDIUM',
        'ANALYZE',
        'Who among the following has the sole constitutional authority to decide whether a legislative bill is a Money Bill or not under Article $$110(3)$$?',
        '[{"id":"A","text":"Speaker of the Lok Sabha","isCorrect":true},{"id":"B","text":"President of India","isCorrect":false},{"id":"C","text":"Chairman of the Rajya Sabha","isCorrect":false},{"id":"D","text":"Finance Minister","isCorrect":false}]',
        'A',
        'Under Article $$110(3)$$, the decision of the Speaker of the Lok Sabha as to whether a bill is a Money Bill is final and cannot be questioned in court.'
    ),
    -- 11. Culture & Arts (Classical Dances)
    (
        'a1030000-0000-0000-0000-000000000011'::uuid,
        'Culture',
        'EASY',
        'REMEMBER',
        'Sattriya, an Indian classical dance tradition established by the medieval saint-reformer Sankaradeva in the 15th century, originates from which Indian state?',
        '[{"id":"A","text":"Assam","isCorrect":true},{"id":"B","text":"Odisha","isCorrect":false},{"id":"C","text":"Manipur","isCorrect":false},{"id":"D","text":"Kerala","isCorrect":false}]',
        'A',
        'Sattriya was originated in the monasteries (Sattras) of Assam by Mahapurusha Srimanta Sankaradeva.'
    ),
    -- 12. Neighbouring Countries (Geography / Borders)
    (
        'a1030000-0000-0000-0000-000000000012'::uuid,
        'India and Neighbouring Countries',
        'EASY',
        'REMEMBER',
        'With which of its neighbouring countries does India share its longest international land border measuring approximately $$4,096.7\\text{ km}$$?',
        '[{"id":"A","text":"Bangladesh","isCorrect":true},{"id":"B","text":"China","isCorrect":false},{"id":"C","text":"Pakistan","isCorrect":false},{"id":"D","text":"Nepal","isCorrect":false}]',
        'A',
        'India shares its longest land border with Bangladesh ($$4,096.7\\text{ km}$$), followed by China ($$3,488\\text{ km}$$) and Pakistan ($$3,323\\text{ km}$$).'
    ),
    -- 13. Scientific Research (Space / ISRO)
    (
        'a1030000-0000-0000-0000-000000000013'::uuid,
        'Scientific Research',
        'EASY',
        'REMEMBER',
        'What is the designated name of the lunar landing site near the South Pole where the Chandrayaan-3 Vikram lander touched down on August 23, 2023?',
        '[{"id":"A","text":"Shiv Shakti Point","isCorrect":true},{"id":"B","text":"Tiranga Point","isCorrect":false},{"id":"C","text":"Jawahar Point","isCorrect":false},{"id":"D","text":"Atal Point","isCorrect":false}]',
        'A',
        'The Chandrayaan-3 touchdown spot was named Shiv Shakti Point, while the Chandrayaan-2 impact spot is known as Tiranga Point.'
    ),
    -- 14. Everyday Science (Physics - Mechanics)
    (
        'a1030000-0000-0000-0000-000000000014'::uuid,
        'Everyday Science',
        'MEDIUM',
        'UNDERSTAND',
        'If the radius of the Earth were to shrink by $$1\\%$$ with its mass remaining unchanged, the acceleration due to gravity ($$g$$) on its surface would:',
        '[{"id":"A","text":"Increase by approximately $$2\\%$$","isCorrect":true},{"id":"B","text":"Decrease by $$2\\%$$","isCorrect":false},{"id":"C","text":"Increase by $$1\\%$$","isCorrect":false},{"id":"D","text":"Remain unchanged","isCorrect":false}]',
        'A',
        '$$g = \\frac{GM}{R^2}$$. Differentiating gives $$\\frac{\\Delta g}{g} \\approx -2\\frac{\\Delta R}{R}$$. A $$1\\%$$ decrease in $$R$$ leads to a $$-2(-1\\%) = +2\\%$$ increase in $$g$$.'
    ),
    -- 15. Indian Economy (National Income)
    (
        'a1030000-0000-0000-0000-000000000015'::uuid,
        'Economic Scene',
        'HARD',
        'ANALYZE',
        'Which macroeconomic metric is officially considered the truest measure of National Income in economic accounting?',
        '[{"id":"A","text":"Net National Product at Factor Cost ($$\\text{NNP}_{\\text{FC}}$$)","isCorrect":true},{"id":"B","text":"Gross Domestic Product at Market Price ($$\\text{GDP}_{\\text{MP}}$$)","isCorrect":false},{"id":"C","text":"Gross National Product at Factor Cost ($$\\text{GNP}_{\\text{FC}}$$)","isCorrect":false},{"id":"D","text":"Personal Disposable Income ($$\\text{PDI}$$)","isCorrect":false}]',
        'A',
        'National Income ($$\\text{NI}$$) is formally defined as Net National Product at Factor Cost: $$\\text{NNP}_{\\text{FC}} = \\text{GNP}_{\\text{FC}} - \\text{Depreciation}$$.'
    ),
    -- 16. Medieval History (Delhi Sultanate / Architecture)
    (
        'a1030000-0000-0000-0000-000000000016'::uuid,
        'History',
        'MEDIUM',
        'REMEMBER',
        'Who built the Alai Darwaza, the southern gateway of the Quwwat-ul-Islam Mosque in the Qutb complex in Delhi in $$1311\\text{ CE}$$?',
        '[{"id":"A","text":"Alauddin Khalji","isCorrect":true},{"id":"B","text":"Iltutmish","isCorrect":false},{"id":"C","text":"Qutb ud-Din Aibak","isCorrect":false},{"id":"D","text":"Ghiyas ud-Din Balban","isCorrect":false}]',
        'A',
        'The Alai Darwaza was commissioned by Sultan Alauddin Khalji in $$1311\\text{ CE}$$ and is celebrated as the first true arch and dome in Indian architecture.'
    ),
    -- 17. Environment & Ecology
    (
        'a1030000-0000-0000-0000-000000000017'::uuid,
        'Scientific Research',
        'MEDIUM',
        'UNDERSTAND',
        'The phenomenon of biomagnification refers to:',
        '[{"id":"A","text":"The progressive increase in concentration of non-biodegradable toxic pollutants at successive trophic levels in a food chain","isCorrect":true},{"id":"B","text":"The rapid growth of phytoplankton in eutrophic water bodies","isCorrect":false},{"id":"C","text":"The increase in biodiversity within tropical rainforests","isCorrect":false},{"id":"D","text":"The genetic adaptation of microorganisms against antibiotics","isCorrect":false}]',
        'A',
        'Biomagnification occurs when non-metabolizable toxins (like DDT, methylmercury) accumulate in fatty tissues and multiply in concentration up the food pyramid.'
    ),
    -- 18. Everyday Science (Vitamins / Deficiency)
    (
        'a1030000-0000-0000-0000-000000000018'::uuid,
        'Everyday Science',
        'EASY',
        'REMEMBER',
        'Pernicious anemia in human beings is caused by the chronic deficiency or malabsorption of which water-soluble vitamin containing cobalt?',
        '[{"id":"A","text":"Vitamin $$\\text{B}_{12}$$ (Cobalamin)","isCorrect":true},{"id":"B","text":"Vitamin $$\\text{B}_1$$ (Thiamine)","isCorrect":false},{"id":"C","text":"Vitamin $$\\text{B}_3$$ (Niacin)","isCorrect":false},{"id":"D","text":"Vitamin $$\\text{C}$$ (Ascorbic Acid)","isCorrect":false}]',
        'A',
        'Vitamin $$\\text{B}_{12}$$ (cyanocobalamin) contains cobalt and requires intrinsic factor for absorption in the ileum; its deficiency impairs RBC maturation and leads to pernicious anemia.'
    ),
    -- 19. Indian Polity (Constitutional Amendments)
    (
        'a1030000-0000-0000-0000-000000000019'::uuid,
        'General Policy',
        'MEDIUM',
        'REMEMBER',
        'By which Constitutional Amendment Act were the words "Socialist", "Secular", and "Integrity" added to the Preamble of the Indian Constitution?',
        '[{"id":"A","text":"$$42^{\\text{nd}}$$ Amendment Act, $$1976$$","isCorrect":true},{"id":"B","text":"$$44^{\\text{th}}$$ Amendment Act, $$1978$$","isCorrect":false},{"id":"C","text":"$$73^{\\text{rd}}$$ Amendment Act, $$1992$$","isCorrect":false},{"id":"D","text":"$$86^{\\text{th}}$$ Amendment Act, $$2002$$","isCorrect":false}]',
        'A',
        'The $$42^{\\text{nd}}$$ Constitutional Amendment Act of $$1976$$ (Mini-Constitution) amended the Preamble to insert the three words.'
    ),
    -- 20. Geography (Ocean Currents)
    (
        'a1030000-0000-0000-0000-000000000020'::uuid,
        'Geography',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following ocean currents is a cold current located in the South Atlantic Ocean?',
        '[{"id":"A","text":"Benguela Current","isCorrect":true},{"id":"B","text":"Gulf Stream","isCorrect":false},{"id":"C","text":"Kuroshio Current","isCorrect":false},{"id":"D","text":"Brazil Current","isCorrect":false}]',
        'A',
        'The Benguela Current is a cold ocean current that flows northward along the south-western coast of Africa in the South Atlantic Ocean.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
