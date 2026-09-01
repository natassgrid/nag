-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Additional Indian Government Examination Subjects, Topics & Subtopics
-- Covers: UPSC CSE, SSC (CGL/CHSL/MTS/CPO/GD), Banking (IBPS/SBI/RBI),
-- Railway (RRB NTPC/Group D/JE), State PSCs, Teaching (CTET/TET), Defense (NDA/CDS)
-- ============================================================

-- 1. Additional Subjects
INSERT INTO question_service.subject (tenant_id, name, code, description) VALUES
('default', 'Banking and Financial Awareness', 'BFA', 'Banking principles, financial systems, RBI regulations, monetary policy, financial inclusion, digital banking, and government economic schemes'),
('default', 'Data Interpretation and Logical Analysis', 'DILA', 'Data interpretation across tabular data, line and bar charts, pie charts, caselets, radar charts, data sufficiency, and analytical reasoning'),
('default', 'General Hindi', 'HND', 'General Hindi covering grammar, sandhi, samas, synonyms, antonyms, idioms, proverbs, error detection, sentence correction, and comprehension passages'),
('default', 'Public Administration and Governance', 'PAG', 'Administrative theories, Indian administrative structure, constitutional and statutory bodies, public policy, e-governance, and civil services ethics'),
('default', 'Environmental Ecology and Biodiversity', 'EEB', 'Ecology, biodiversity conservation, climate change, international conventions, wildlife protection, and pollution management'),
('default', 'Child Development and Pedagogy', 'CDP', 'Child development, learning theories, inclusive education, pedagogy of school subjects, curriculum design, continuous and comprehensive evaluation (CCE)')
ON CONFLICT (name, tenant_id) DO NOTHING;

-- 2. Additional Topics for New & Existing Subjects
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT v.tenant_id, s.id, v.name, v.description
FROM (VALUES
    -- Banking and Financial Awareness
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Structure of Indian commercial and cooperative banks, RBI powers and monetary tools'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Repo rate, reverse repo, CRR, SLR, inflation indices (CPI, WPI) and monetary control'),
    ('default', 'Banking and Financial Awareness', 'Financial Markets & Capital Instruments', 'Money market, capital market, treasury bills, commercial papers, SEBI regulations and mutual funds'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'NPCI, UPI, RTGS, NEFT, IMPS, CBDC / Digital Rupee, cybersecurity in electronic transactions'),
    ('default', 'Banking and Financial Awareness', 'Government Financial Schemes & Inclusions', 'PM Jan Dhan Yojana, PMJJBY, PMSBY, APY, Mudra loans, Stand-Up India and priority sector lending'),
    ('default', 'Banking and Financial Awareness', 'Non-Performing Assets & Risk Management', 'NPA classification, SARFAESI Act, Insolvency and Bankruptcy Code (IBC), Basel III norms, and PCA framework'),

    -- Data Interpretation and Logical Analysis
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Data extraction, comparison, calculation and trend analysis from single/multi-column tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Bar Graphs and Histograms', 'Analysis and calculations based on simple, grouped, and stacked bar diagrams'),
    ('default', 'Data Interpretation and Logical Analysis', 'Line Graphs and Radar Charts', 'Time series line plots, multiple line comparisons, and radar spider charts'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Single and dual pie charts, degrees-to-percentage conversions, combined table-chart sets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Paragraph-based data sets, Venn diagram based sets, and arithmetic concept-based DI'),
    ('default', 'Data Interpretation and Logical Analysis', 'Data Sufficiency', 'Determining sufficiency of statements to answer algebraic, geometric and arithmetic queries'),

    -- General Hindi
    ('default', 'General Hindi', 'वर्ण विचार एवं वर्तनी शुद्धि', 'स्वर, व्यंजन, उच्चारण स्थान, और वर्तनी की अशुद्धियों का संशोधन'),
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'स्वर संधि, व्यंजन संधि और विसर्ग संधि के नियम एवं विच्छेद'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'तत्पुरुष, कर्मधारय, द्विगु, द्वन्द्व, बहुव्रीहि और अव्ययीभाव समास'),
    ('default', 'General Hindi', 'विलोम एवं पर्यायवाची शब्द', 'समानार्थी एवं विपरीतार्थक शब्द और उनका वाक्यों में उचित प्रयोग'),
    ('default', 'General Hindi', 'मुहावरे एवं लोकोक्तियाँ', 'प्रचलित मुहावरों, कहावतों के अर्थ और संदर्भानुसार प्रयोग'),
    ('default', 'General Hindi', 'अनेक शब्दों के लिए एक शब्द', 'संक्षिप्त अभिव्यक्ति एवं एकल शब्द चयन'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'लिंग, वचन, कारक, काल और पदक्रम संबंधी त्रुटि सुधार'),
    ('default', 'General Hindi', 'अपठित गद्यांश', 'गद्यांश आधारित बोधगम्यता, शीर्षक चयन एवं व्याख्यात्मक प्रश्न'),

    -- Public Administration and Governance
    ('default', 'Public Administration and Governance', 'Administrative Theories & Evolution', 'Scientific management, bureaucracy, human relations, new public management, and good governance'),
    ('default', 'Public Administration and Governance', 'Union and State Administrative Machinery', 'Cabinet Secretariat, PMO, Ministries, Chief Secretariat, and District Administration'),
    ('default', 'Public Administration and Governance', 'Panchayati Raj & Local Self-Government', '73rd and 74th Constitutional Amendment Acts, PESA Act, and urban/rural local body functions'),
    ('default', 'Public Administration and Governance', 'Public Policy & Citizen Centricity', 'Policy formulation, implementation evaluation, Citizen Charters, RTI Act, and Lokpal/Lokayukta'),
    ('default', 'Public Administration and Governance', 'E-Governance & Digital Public Infrastructure', 'Digital India initiatives, direct benefit transfer (DBT), UMANG, DigiLocker, and cybersecurity policy'),

    -- Environmental Ecology and Biodiversity
    ('default', 'Environmental Ecology and Biodiversity', 'Ecosystem Dynamics & Biomes', 'Food chains, trophic levels, energy flow, ecological pyramids, biomes, and biogeochemical cycles'),
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'Protected Area Network, National Parks, Wildlife Sanctuaries, Biosphere Reserves, Ramsar Wetlands, and IUCN Red List'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'UNFCCC, Kyoto Protocol, Paris Agreement, COP summits, IPCC reports, and carbon credits/markets'),
    ('default', 'Environmental Ecology and Biodiversity', 'Environmental Pollution & Waste Management', 'Air, water, soil, plastic and electronic waste management rules, and National Green Tribunal (NGT) acts'),

    -- Child Development and Pedagogy
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Piaget, Vygotsky, Kohlberg theories of cognitive and moral development, maturation, and individual differences'),
    ('default', 'Child Development and Pedagogy', 'Inclusive Education & Special Needs', 'Addressing learners from diverse backgrounds, learning disabilities (Dyslexia, ADHD), and gifted children'),
    ('default', 'Child Development and Pedagogy', 'Teaching-Learning Strategies & Pedagogy', 'Constructivist approach, problem solving, inquiry learning, motivation, and learning transfer'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'Formative and summative assessment, Continuous and Comprehensive Evaluation, rubrics, and diagnostic testing'),

    -- Additional Topics for General Studies (Enriching GS for UPSC / State PSC)
    ('default', 'General Studies', 'Indian Art & Culture', 'Classical dance, music, temple architecture, literature, festivals and UNESCO World Heritage Sites'),
    ('default', 'General Studies', 'International Relations & Global Bodies', 'United Nations, G20, BRICS, SCO, ASEAN, WTO, IMF, World Bank, and India bilateral relations'),
    ('default', 'General Studies', 'Internal Security & Disaster Management', 'Border management, cyber warfare, money laundering, NDMA guidelines, and disaster mitigation frameworks'),
    ('default', 'General Studies', 'Ethics, Integrity & Aptitude', 'Moral philosophy, public service values, emotional intelligence, attitude, case studies in administrative ethics')
) AS v(tenant_id, subject_name, name, description)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- 3. Subtopics for New Topics
INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
SELECT v.tenant_id, t.id, v.name, NULL
FROM (VALUES
    -- Banking subtopics
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Commercial Banks & Payments Banks'),
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Small Finance Banks & RRBs'),
    ('default', 'Banking and Financial Awareness', 'Indian Banking System & RBI Functions', 'Reserve Bank of India Constitution & Functions'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Quantitative Monetary Instruments'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'Qualitative Credit Controls'),
    ('default', 'Banking and Financial Awareness', 'Monetary Policy & Inflation Dynamics', 'CPI & WPI Trends'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'Unified Payments Interface (UPI) & IMPS'),
    ('default', 'Banking and Financial Awareness', 'Digital Banking & Payment Systems', 'Central Bank Digital Currency (CBDC)'),

    -- Data Interpretation subtopics
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Multi-Variable Tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Tabular Data Interpretation', 'Missing Data Tables'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Single & Dual Pie Charts'),
    ('default', 'Data Interpretation and Logical Analysis', 'Pie Charts and Mixed Graphs', 'Combined Table & Graph Sets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Profit & Loss and SI/CI Caselets'),
    ('default', 'Data Interpretation and Logical Analysis', 'Caselet and Arithmetic DI', 'Time & Work and Speed-Time Caselets'),

    -- General Hindi subtopics
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'स्वर संधि भेद (दीर्घ, गुण, वृद्धि, यण, अयादि)'),
    ('default', 'General Hindi', 'संधि एवं संधि विच्छेद', 'व्यंजन संधि एवं विसर्ग संधि'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'तत्पुरुष एवं कर्मधारय समास'),
    ('default', 'General Hindi', 'समास एवं समास विग्रह', 'द्विगु, द्वन्द्व एवं बहुव्रीहि समास'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'कारक एवं वचन संबंधी अशुद्धियाँ'),
    ('default', 'General Hindi', 'वाक्य शुद्धि एवं त्रुटि पहचान', 'पदक्रम एवं मुहावरे संबंधी वाक्य दोष'),

    -- Ecology & Environment subtopics
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'National Parks & Wildlife Sanctuaries of India'),
    ('default', 'Environmental Ecology and Biodiversity', 'Biodiversity & Wildlife Conservation', 'Biosphere Reserves & Ramsar Sites in India'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'Paris Agreement & Nationally Determined Contributions (NDCs)'),
    ('default', 'Environmental Ecology and Biodiversity', 'Climate Change & Global Agreements', 'Renewable Energy Targets & National Solar Mission'),

    -- Pedagogy subtopics
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Cognitive Development Theory (Jean Piaget)'),
    ('default', 'Child Development and Pedagogy', 'Child Development & Learning Principles', 'Socio-Cultural Learning Theory (Lev Vygotsky)'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'Formative & Summative Evaluation Techniques'),
    ('default', 'Child Development and Pedagogy', 'Assessment, Evaluation & CCE', 'School-Based Assessment (SBA) & Holistic Report Cards')
) AS v(tenant_id, subject_name, topic_name, name)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = v.tenant_id
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = v.tenant_id
ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
