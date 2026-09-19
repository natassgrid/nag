-- ============================================================
-- Seed Data: Comprehension Passages & Passage-Based Questions (Issue #135)
-- Format Standard: Valid hex UUIDs, JSONB escaped, APPROVED state
-- ============================================================

-- ------------------------------------------------------------
-- 1. Insert Passages
-- ------------------------------------------------------------
INSERT INTO question_service.passage (
    id, tenant_id, title, content, content_format, subject_id, topic_id, subtopic_id,
    subject, topic, subtopic, state, author_id
)
SELECT
    v.id,
    'default',
    v.title,
    v.content,
    'MIXED',
    s.id,
    t.id,
    st.id,
    v.subject_name,
    v.topic_name,
    v.subtopic_name,
    'APPROVED',
    '00000000-0000-0000-0000-000000000001'::uuid
FROM (VALUES
    (
        'd1350000-0000-0000-0000-000000000001'::uuid,
        'The Quantum Frontier: Principles and Prospects of Superconducting Qubits',
        'Quantum computing represents a fundamental departure from the classical computation paradigm. While classical digital computers process information using discrete binary digits (bits) that represent either a zero or a one, quantum computers harness the counterintuitive principles of quantum mechanics—namely superposition and entanglement. In a quantum processor, superconducting circuits cooled to near absolute zero can sustain quantum bits, or "qubits", in a linear combination of states |0⟩ and |1⟩ simultaneously. This exponential state space enables quantum algorithms, such as Shor''s factoring and Grover''s database search, to solve specific mathematical formulations exponentially or quadratically faster than the most capable classical supercomputers. However, the fragile quantum states remain acutely susceptible to environmental decoherence, where thermal noise and electromagnetic fluctuations destroy quantum coherence before error-correcting surface codes can stabilize the computation. Consequently, transitioning from Noisy Intermediate-Scale Quantum (NISQ) systems to fault-tolerant universal quantum architectures demands breakthrough advances in cryogenic control lines and material purity.',
        'English Language and Comprehension',
        'Comprehension Passage',
        'Literary Passage'
    ),
    (
        'd1350000-0000-0000-0000-000000000002'::uuid,
        'Biodiversity Preservation and Mangrove Ecosystems of the Sundarbans',
        'The Sundarbans, spanning the deltaic confluence of the Ganges, Brahmaputra, and Meghna rivers across India and Bangladesh, constitutes the largest contiguous mangrove forest on Earth. Characterized by dense halophytic vegetation, intricate tidal waterways, and mudflats, this UNESCO World Heritage ecosystem provides an indispensable ecological bulwark against recurrent tropical cyclones and tidal surges originating in the Bay of Bengal. Mangrove root matrices act as natural sediment traps, attenuating shoreline erosion and sequestering immense reservoirs of blue carbon. Concurrently, the estuary nurtures apex biodiversity including the endangered Royal Bengal Tiger (Panthera tigris tigris) and the Irrawaddy dolphin. However, anthropogenic pressures including upstream freshwater diversion, shrimp aquaculture encroachment, and accelerated sea-level rise are precipitating hypersalinity and mangrove retreat. Sustaining the socio-ecological resilience of the Sundarbans necessitates community-driven afforestation alongside transboundary watershed governance.',
        'English Language and Comprehension',
        'Comprehension Passage',
        'Report or Editorial Passage'
    ),
    (
        'd1350000-0000-0000-0000-000000000003'::uuid,
        'Economic Survey Caselet: Renewable Energy Capacity Additions (2020–2025)',
        'According to national energy ministry reports between fiscal years 2020-21 and 2024-25, India expanded its cumulative non-fossil installed capacity from 140 GW to 210 GW. Within this 70 GW gross addition, utility-scale Solar Photovoltaic (PV) installations accounted for 60% of new capacity, Wind Energy contributed 25%, and Hybrid/Biomass/Small-Hydro systems comprised the remaining 15%. In FY 2023-24 alone, total renewable additions reached 18 GW, of which 12 GW came from solar, 4 GW from wind, and 2 GW from hybrid installations. Grid integration challenges emerged during peak solar generation hours (11:00 AM to 2:00 PM), requiring the commissioning of 5 GWh of Battery Energy Storage Systems (BESS) and pumped storage hydro to prevent renewable curtailment.',
        'Data Interpretation and Logical Analysis',
        'Caselet and Arithmetic DI',
        NULL
    )
) AS v(id, title, content, subject_name, topic_name, subtopic_name)
JOIN question_service.subject s ON s.name = v.subject_name AND s.tenant_id = 'default'
LEFT JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. Insert Passage Sub-Questions (10 Questions Linked to Passages)
-- ------------------------------------------------------------
INSERT INTO question_service.question (
    id, tenant_id, passage_id, passage_order_index, subject_id, topic_id, subtopic_id,
    subject, topic, subtopic, difficulty, cognitive_level,
    question_type, content, options, answer_key, explanation,
    state, author_id
)
SELECT
    v.id,
    'default',
    v.passage_id,
    v.passage_order_index,
    s.id,
    t.id,
    st.id,
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
    -- ============================================================
    -- Passage 1 Questions: Quantum Frontier
    -- ============================================================
    (
        'd1350000-0001-0000-0000-000000000001'::uuid,
        'd1350000-0000-0000-0000-000000000001'::uuid,
        1,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Literary Passage',
        'EASY',
        'COMPREHENSION',
        'According to the passage, what is the fundamental property of qubits that distinguishes them from classical bits?',
        '[{"id":"A","text":"They operate at high thermal energy without cooling","isCorrect":false},{"id":"B","text":"They can exist in a simultaneous linear combination of states 0 and 1","isCorrect":true},{"id":"C","text":"They are completely immune to environmental fluctuations","isCorrect":false},{"id":"D","text":"They only execute linear mathematical calculations","isCorrect":false}]',
        'B',
        'The passage explicitly mentions that qubits can sustain a linear combination of states |0⟩ and |1⟩ simultaneously due to the principle of quantum superposition.'
    ),
    (
        'd1350000-0001-0000-0000-000000000002'::uuid,
        'd1350000-0000-0000-0000-000000000001'::uuid,
        2,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Literary Passage',
        'MEDIUM',
        'ANALYSIS',
        'What is identified in the passage as the primary obstacle to stabilizing quantum computations in the NISQ era?',
        '[{"id":"A","text":"Environmental decoherence triggered by thermal noise and electromagnetic fluctuations","isCorrect":true},{"id":"B","text":"Lack of theoretical algorithms for database searching","isCorrect":false},{"id":"C","text":"Absence of superconducting circuits","isCorrect":false},{"id":"D","text":"Inability to simulate binary digits","isCorrect":false}]',
        'A',
        'The text states that fragile quantum states remain acutely susceptible to environmental decoherence caused by thermal noise and electromagnetic fluctuations.'
    ),
    (
        'd1350000-0001-0000-0000-000000000003'::uuid,
        'd1350000-0000-0000-0000-000000000001'::uuid,
        3,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Literary Passage',
        'EASY',
        'KNOWLEDGE',
        'In the context of the passage, what does the word "susceptible" most nearly mean?',
        '[{"id":"A","text":"Resistant","isCorrect":false},{"id":"B","text":"Vulnerable or prone","isCorrect":true},{"id":"C","text":"Indifferent","isCorrect":false},{"id":"D","text":"Superior","isCorrect":false}]',
        'B',
        'In this context, "susceptible" means vulnerable to or easily affected by environmental disturbances.'
    ),
    (
        'd1350000-0001-0000-0000-000000000004'::uuid,
        'd1350000-0000-0000-0000-000000000001'::uuid,
        4,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Literary Passage',
        'HARD',
        'EVALUATION',
        'What can be inferred about the transition from NISQ systems to fault-tolerant universal quantum architectures?',
        '[{"id":"A","text":"It requires solely software updates without hardware alterations","isCorrect":false},{"id":"B","text":"It depends heavily on engineering breakthroughs in material purity and cryogenic controls","isCorrect":true},{"id":"C","text":"It has already been fully completed by classical supercomputers","isCorrect":false},{"id":"D","text":"It is impeded by Shor''s factoring algorithm","isCorrect":false}]',
        'B',
        'The concluding sentence highlights that transitioning to fault-tolerant universal quantum architectures demands breakthrough advances in cryogenic control lines and material purity.'
    ),

    -- ============================================================
    -- Passage 2 Questions: Sundarbans Mangroves
    -- ============================================================
    (
        'd1350000-0002-0000-0000-000000000001'::uuid,
        'd1350000-0000-0000-0000-000000000002'::uuid,
        1,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Report or Editorial Passage',
        'EASY',
        'COMPREHENSION',
        'How do mangrove root matrices contribute to the physical defense of coastal areas?',
        '[{"id":"A","text":"By absorbing industrial pollutants into groundwater","isCorrect":false},{"id":"B","text":"By trapping sediment and attenuating shoreline erosion","isCorrect":true},{"id":"C","text":"By increasing oceanic salinity levels","isCorrect":false},{"id":"D","text":"By diverting river flow into inland canals","isCorrect":false}]',
        'B',
        'The passage highlights that mangrove root matrices act as natural sediment traps, attenuating shoreline erosion and protecting coastal zones.'
    ),
    (
        'd1350000-0002-0000-0000-000000000002'::uuid,
        'd1350000-0000-0000-0000-000000000002'::uuid,
        2,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Report or Editorial Passage',
        'MEDIUM',
        'ANALYSIS',
        'Which of the following is NOT mentioned in the text as a contributing factor to hypersalinity in the Sundarbans?',
        '[{"id":"A","text":"Upstream freshwater diversion","isCorrect":false},{"id":"B","text":"Accelerated sea-level rise","isCorrect":false},{"id":"C","text":"Shrimp aquaculture encroachment","isCorrect":false},{"id":"D","text":"Introduction of invasive predatory fish species","isCorrect":true}]',
        'D',
        'The passage specifically cites upstream freshwater diversion, shrimp aquaculture encroachment, and accelerated sea-level rise; invasive fish species are not mentioned.'
    ),
    (
        'd1350000-0002-0000-0000-000000000003'::uuid,
        'd1350000-0000-0000-0000-000000000002'::uuid,
        3,
        'English Language and Comprehension',
        'Comprehension Passage',
        'Report or Editorial Passage',
        'MEDIUM',
        'COMPREHENSION',
        'What does the term "blue carbon" refer to in the context of coastal mangrove ecosystems?',
        '[{"id":"A","text":"Carbon sequestered in coastal and marine ecosystems like mangroves","isCorrect":true},{"id":"B","text":"Industrial carbon captured from coal thermal power plants","isCorrect":false},{"id":"C","text":"Carbon monoxide dissolved in deep ocean water","isCorrect":false},{"id":"D","text":"Synthetic carbon fibers manufactured for boat construction","isCorrect":false}]',
        'A',
        'Blue carbon refers to biologically driven carbon storage in coastal and marine ecosystems such as mangroves, tidal marshes, and seagrasses.'
    ),

    -- ============================================================
    -- Passage 3 Questions: Renewable Energy Caselet (DI)
    -- ============================================================
    (
        'd1350000-0003-0000-0000-000000000001'::uuid,
        'd1350000-0000-0000-0000-000000000003'::uuid,
        1,
        'Data Interpretation and Logical Analysis',
        'Caselet and Arithmetic DI',
        NULL,
        'EASY',
        'APPLICATION',
        'What was the total capacity (in GW) contributed by Solar Photovoltaic (PV) installations in the 70 GW gross addition?',
        '[{"id":"A","text":"35 GW","isCorrect":false},{"id":"B","text":"42 GW","isCorrect":true},{"id":"C","text":"45 GW","isCorrect":false},{"id":"D","text":"50 GW","isCorrect":false}]',
        'B',
        '60% of 70 GW = 0.60 * 70 = 42 GW.'
    ),
    (
        'd1350000-0003-0000-0000-000000000002'::uuid,
        'd1350000-0000-0000-0000-000000000003'::uuid,
        2,
        'Data Interpretation and Logical Analysis',
        'Caselet and Arithmetic DI',
        NULL,
        'MEDIUM',
        'APPLICATION',
        'In FY 2023-24, what percentage of the 18 GW total additions was contributed by Wind Energy?',
        '[{"id":"A","text":"20.0%","isCorrect":false},{"id":"B","text":"22.22%","isCorrect":true},{"id":"C","text":"25.0%","isCorrect":false},{"id":"D","text":"28.5%","isCorrect":false}]',
        'B',
        'Wind addition in FY 2023-24 was 4 GW out of 18 GW total. (4 / 18) * 100 = 22.22%.'
    ),
    (
        'd1350000-0003-0000-0000-000000000003'::uuid,
        'd1350000-0000-0000-0000-000000000003'::uuid,
        3,
        'Data Interpretation and Logical Analysis',
        'Caselet and Arithmetic DI',
        NULL,
        'MEDIUM',
        'COMPREHENSION',
        'Why was 5 GWh of Battery Energy Storage Systems (BESS) and pumped hydro commissioned according to the text?',
        '[{"id":"A","text":"To replace all coal power plants immediately","isCorrect":false},{"id":"B","text":"To manage grid integration and prevent curtailment during peak solar generation hours","isCorrect":true},{"id":"C","text":"To export surplus power to neighbouring island nations","isCorrect":false},{"id":"D","text":"To reduce maintenance costs of transmission lines exclusively","isCorrect":false}]',
        'B',
        'The passage explains that grid integration challenges emerged during peak solar generation hours (11:00 AM to 2:00 PM), requiring BESS and pumped storage hydro to prevent renewable curtailment.'
    )
) AS v(id, passage_id, passage_order_index, subject_name, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = v.subject_name AND s.tenant_id = 'default'
LEFT JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
