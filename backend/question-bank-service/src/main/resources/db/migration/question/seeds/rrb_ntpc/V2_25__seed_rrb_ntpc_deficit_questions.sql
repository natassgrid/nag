-- ============================================================
-- Seed Questions: RRB NTPC CBT-1 Blueprint Deficit Fulfillment (45 Questions)
-- Examination: RRB NTPC (Non-Technical Popular Categories) CBT-1 2026
-- Deficits Resolved:
--   Rule 1: General Awareness > Everyday Science (EASY, UNDERSTAND) - 3 Qs
--   Rule 2: General Awareness > Economic Scene (EASY, UNDERSTAND) - 4 Qs
--   Rule 3: General Awareness > India and Neighbouring Countries (MEDIUM, REMEMBER) - 4 Qs
--   Rule 4: Mathematics > Arithmetic (MEDIUM, APPLY) - 12 Qs
--   Rule 5: Mathematics > Algebra (MEDIUM, APPLY) - 6 Qs
--   Rule 6: Mathematics > Geometry (HARD, ANALYZE) - 4 Qs
--   Rule 7: Mathematics > Trigonometry (MEDIUM, APPLY) - 4 Qs
--   Rule 8: Mathematics > Statistics (EASY, UNDERSTAND) - 4 Qs
--   Rule 9: General Intelligence and Reasoning > Coding and Decoding (EASY, APPLY) - 4 Qs
-- Format Standard: Valid UUIDs, JSONB options, LaTeX math ($$..$$), state='APPROVED'
-- UUID Range: a1250000-0000-0000-0000-000000000001 to a1250000-0000-0000-0000-000000000045
-- ============================================================

-- Step 1: Ensure Subjects exist
INSERT INTO question_service.subject (tenant_id, name, code, description)
VALUES
    ('default', 'General Awareness', 'GA', 'General awareness covering everyday science, economic scene, polity, history, and international relations'),
    ('default', 'Mathematics', 'MATH', 'Mathematics covering arithmetic, algebra, geometry, trigonometry, and statistics'),
    ('default', 'General Intelligence and Reasoning', 'GIR', 'General intelligence and reasoning covering coding-decoding, analogies, and analytical reasoning')
ON CONFLICT (name, tenant_id) DO NOTHING;

-- Step 2: Ensure Topics exist under respective Subjects
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Everyday Science', 'Scientific principles in daily life, physics, chemistry, biology'),
    ('Economic Scene', 'Indian economy, banking, monetary and fiscal policies, inflation'),
    ('India and Neighbouring Countries', 'Borders, geographical features, capitals, treaties, and bilateral relations')
) AS v(name, description)
WHERE s.name = 'General Awareness' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Arithmetic', 'Percentages, profit and loss, ratios, time and work, speed and distance, simple and compound interest'),
    ('Algebra', 'Algebraic identities, linear and quadratic equations, polynomials, indices and surds'),
    ('Geometry', 'Circles, tangents, triangles, polygons, coordinate geometry'),
    ('Trigonometry', 'Trigonometric identities, values, heights and distances'),
    ('Statistics', 'Measures of central tendency, dispersion, mean, median, mode, standard deviation')
) AS v(name, description)
WHERE s.name = 'Mathematics' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Coding and Decoding', 'Letter coding, number coding, substitution, positional shifts')
) AS v(name, description)
WHERE s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- Step 3: Insert the 45 Assessment Questions
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
    -- =========================================================================
    -- RULE 1: General Awareness > Everyday Science (EASY, UNDERSTAND) - 3 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000001'::uuid,
        'General Awareness',
        'Everyday Science',
        'Optics in Daily Life',
        'EASY',
        'UNDERSTAND',
        'Why are convex mirrors commonly used as rear-view mirrors in motor vehicles?',
        '[{"id": "A", "text": "They always produce real and inverted images", "isCorrect": false}, {"id": "B", "text": "They produce erect, diminished images providing a wider field of view", "isCorrect": true}, {"id": "C", "text": "They produce magnified virtual images", "isCorrect": false}, {"id": "D", "text": "They absorb glare from trailing headlights completely", "isCorrect": false}]',
        'B',
        'Convex mirrors always form virtual, erect, and diminished images, which allows the driver to view a much larger area behind the vehicle than would be possible with a plane mirror.'
    ),
    (
        'a1250000-0000-0000-0000-000000000002'::uuid,
        'General Awareness',
        'Everyday Science',
        'Household Chemistry',
        'EASY',
        'UNDERSTAND',
        'What causes bread and cakes to rise and become spongy during baking when baking powder is added?',
        '[{"id": "A", "text": "Release of carbon dioxide ($$\\text{CO}_2$$) gas upon heating", "isCorrect": true}, {"id": "B", "text": "Absorption of atmospheric nitrogen gas", "isCorrect": false}, {"id": "C", "text": "Evaporation of water molecules from gluten", "isCorrect": false}, {"id": "D", "text": "Release of hydrogen gas during sugar fermentation", "isCorrect": false}]',
        'A',
        'Baking powder contains sodium bicarbonate and a mild edible acid. When heated with moisture, it decomposes to release carbon dioxide ($$\\text{CO}_2$$) gas, creating bubbles that expand the dough and make the baked item porous and spongy.'
    ),
    (
        'a1250000-0000-0000-0000-000000000003'::uuid,
        'General Awareness',
        'Everyday Science',
        'Atmospheric Physics',
        'EASY',
        'UNDERSTAND',
        'Why does the clear sky appear blue during daytime to an observer on Earth?',
        '[{"id": "A", "text": "Blue light is reflected by ocean surfaces into the atmosphere", "isCorrect": false}, {"id": "B", "text": "Blue light has a shorter wavelength and is scattered more by air molecules (Rayleigh scattering)", "isCorrect": true}, {"id": "C", "text": "Air molecules absorb all colours of sunlight except blue", "isCorrect": false}, {"id": "D", "text": "Atmospheric ozone radiates blue wavelengths upon solar excitation", "isCorrect": false}]',
        'B',
        'According to Rayleigh scattering, the scattering intensity is inversely proportional to the fourth power of wavelength ($$I \\propto 1/\\lambda^4$$). Because blue light has a relatively short wavelength, it is scattered much more strongly by atmospheric molecules than red light.'
    ),

    -- =========================================================================
    -- RULE 2: General Awareness > Economic Scene (EASY, UNDERSTAND) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000004'::uuid,
        'General Awareness',
        'Economic Scene',
        'Monetary Policy',
        'EASY',
        'UNDERSTAND',
        'What happens when the Reserve Bank of India (RBI) increases the Repo Rate?',
        '[{"id": "A", "text": "Commercial bank borrowing becomes cheaper, increasing money supply", "isCorrect": false}, {"id": "B", "text": "Commercial bank borrowing becomes costlier, helping to curb inflationary pressures", "isCorrect": true}, {"id": "C", "text": "Government fiscal deficit decreases immediately", "isCorrect": false}, {"id": "D", "text": "Cash Reserve Ratio (CRR) automatically drops to zero", "isCorrect": false}]',
        'B',
        'The Repo Rate is the rate at which the RBI lends money to commercial banks. Increasing the repo rate makes commercial borrowings costlier, discouraging excessive lending and consumer spending, which helps control inflation.'
    ),
    (
        'a1250000-0000-0000-0000-000000000005'::uuid,
        'General Awareness',
        'Economic Scene',
        'National Income Accounting',
        'EASY',
        'UNDERSTAND',
        'Gross Domestic Product (GDP) represents the total monetary value of:',
        '[{"id": "A", "text": "All intermediate goods produced within a country in a year", "isCorrect": false}, {"id": "B", "text": "All final goods and services produced within the domestic territory of a country during a specified time period", "isCorrect": true}, {"id": "C", "text": "Net exports plus foreign remittances received by citizens", "isCorrect": false}, {"id": "D", "text": "Total assets owned by citizens both inside and outside the country", "isCorrect": false}]',
        'B',
        'GDP is defined as the total market value of all final goods and services produced within the geographic boundaries of a country during a given financial period (usually one year).'
    ),
    (
        'a1250000-0000-0000-0000-000000000006'::uuid,
        'General Awareness',
        'Economic Scene',
        'Inflation Concepts',
        'EASY',
        'UNDERSTAND',
        'What is meant by the economic phenomenon termed "Cost-Push Inflation"?',
        '[{"id": "A", "text": "Inflation caused by excessive growth in aggregate consumer demand", "isCorrect": false}, {"id": "B", "text": "Inflation driven by an increase in production costs such as wages, fuel, and raw materials", "isCorrect": true}, {"id": "C", "text": "A continuous fall in the general price level of consumer goods", "isCorrect": false}, {"id": "D", "text": "Inflation resulting exclusively from printing excess paper currency", "isCorrect": false}]',
        'B',
        'Cost-push inflation occurs when aggregate supply decreases due to higher costs of raw materials, energy, or labour, forcing manufacturers to raise the selling prices of finished goods.'
    ),
    (
        'a1250000-0000-0000-0000-000000000007'::uuid,
        'General Awareness',
        'Economic Scene',
        'Fiscal Policy',
        'EASY',
        'UNDERSTAND',
        'Which of the following defines a "Fiscal Deficit" in the context of the Union Budget?',
        '[{"id": "A", "text": "Total government expenditure exceeding total revenue (excluding borrowings)", "isCorrect": true}, {"id": "B", "text": "Total revenue receipts minus total capital receipts", "isCorrect": false}, {"id": "C", "text": "Total interest payments minus external loans", "isCorrect": false}, {"id": "D", "text": "Total tax collections minus non-tax revenues", "isCorrect": false}]',
        'A',
        'Fiscal Deficit is the excess of total government expenditure over total receipts excluding borrowings ($$\\text{Fiscal Deficit} = \\text{Total Expenditure} - (\\text{Revenue Receipts} + \\text{Non-debt Capital Receipts})$$).'
    ),

    -- =========================================================================
    -- RULE 3: General Awareness > India and Neighbouring Countries (MEDIUM, REMEMBER) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000008'::uuid,
        'General Awareness',
        'India and Neighbouring Countries',
        'International Boundaries',
        'MEDIUM',
        'REMEMBER',
        'Which international boundary line demarcates the border between India and China in the Eastern sector?',
        '[{"id": "A", "text": "Radcliffe Line", "isCorrect": false}, {"id": "B", "text": "McMahon Line", "isCorrect": true}, {"id": "C", "text": "Durand Line", "isCorrect": false}, {"id": "D", "text": "Maginot Line", "isCorrect": false}]',
        'B',
        'The McMahon Line is the boundary line negotiated between the British representative Sir Henry McMahon and Tibetan authorities at the 1914 Simla Convention, separating northeast India and the Tibet region of China.'
    ),
    (
        'a1250000-0000-0000-0000-000000000009'::uuid,
        'General Awareness',
        'India and Neighbouring Countries',
        'Maritime Boundaries',
        'MEDIUM',
        'REMEMBER',
        'Which water body separates the southern peninsula of India from the island nation of Sri Lanka?',
        '[{"id": "A", "text": "Ten Degree Channel", "isCorrect": false}, {"id": "B", "text": "Palk Strait and Gulf of Mannar", "isCorrect": true}, {"id": "C", "text": "Malacca Strait", "isCorrect": false}, {"id": "D", "text": "Duncan Passage", "isCorrect": false}]',
        'B',
        'The Palk Strait and Gulf of Mannar separate Tamil Nadu state in India from the Jaffna District of the Northern Province of Sri Lanka.'
    ),
    (
        'a1250000-0000-0000-0000-000000000010'::uuid,
        'General Awareness',
        'India and Neighbouring Countries',
        'Regional Organizations',
        'MEDIUM',
        'REMEMBER',
        'Where is the permanent Secretariat of the South Asian Association for Regional Cooperation (SAARC) located?',
        '[{"id": "A", "text": "New Delhi, India", "isCorrect": false}, {"id": "B", "text": "Kathmandu, Nepal", "isCorrect": true}, {"id": "C", "text": "Dhaka, Bangladesh", "isCorrect": false}, {"id": "D", "text": "Colombo, Sri Lanka", "isCorrect": false}]',
        'B',
        'The SAARC Secretariat was established in Kathmandu, Nepal, on 16 January 1987 to coordinate and monitor the implementation of SAARC activities.'
    ),
    (
        'a1250000-0000-0000-0000-000000000011'::uuid,
        'General Awareness',
        'India and Neighbouring Countries',
        'Geography & Borders',
        'MEDIUM',
        'REMEMBER',
        'With which neighbouring country does India share its longest international land border?',
        '[{"id": "A", "text": "China", "isCorrect": false}, {"id": "B", "text": "Bangladesh", "isCorrect": true}, {"id": "C", "text": "Pakistan", "isCorrect": false}, {"id": "D", "text": "Myanmar", "isCorrect": false}]',
        'B',
        'India shares its longest land boundary with Bangladesh, spanning approximately $$4,096.7\\text{ km}$$, running across five Indian states (West Bengal, Assam, Meghalaya, Tripura, and Mizoram).'
    ),

    -- =========================================================================
    -- RULE 4: Mathematics > Arithmetic (MEDIUM, APPLY) - 12 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000012'::uuid,
        'Mathematics',
        'Arithmetic',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'If the price of sugar is increased by $$25\\%$$, by what percentage must a household reduce its consumption so that the total expenditure on sugar remains unchanged?',
        '[{"id": "A", "text": "$$20\\%$$", "isCorrect": true}, {"id": "B", "text": "$$25\\%$$", "isCorrect": false}, {"id": "C", "text": "$$16\\frac{2}{3}\\%$$", "isCorrect": false}, {"id": "D", "text": "$$15\\%$$", "isCorrect": false}]',
        'A',
        'Reduction percentage $$= \\left(\\frac{r}{100 + r}\\right) \\times 100 = \\left(\\frac{25}{125}\\right) \\times 100 = \\frac{1}{5} \\times 100 = 20\\%$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000013'::uuid,
        'Mathematics',
        'Arithmetic',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'A shopkeeper sells an article at a profit of $$15\\%$$. If he had bought it at $$10\\%$$ less and sold it for ₹$$4$$ less, he would have gained $$25\\%$$. What is the cost price (CP) of the article?',
        '[{"id": "A", "text": "₹$$160$$", "isCorrect": true}, {"id": "B", "text": "₹$$180$$", "isCorrect": false}, {"id": "C", "text": "₹$$200$$", "isCorrect": false}, {"id": "D", "text": "₹$$140$$", "isCorrect": false}]',
        'A',
        'Let CP $$= x$$. Initial SP $$= 1.15x$$. New CP $$= 0.90x$$. New SP $$= 0.90x \\times 1.25 = 1.125x$$. Difference $$= 1.15x - 1.125x = 0.025x = 4 \\implies x = \\frac{4}{0.025} = ₹160$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000014'::uuid,
        'Mathematics',
        'Arithmetic',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'The ratio of income of A and B is $$4:3$$ and the ratio of their expenditures is $$3:2$$. If each saves ₹$$6,000$$ per month, find the monthly income of A.',
        '[{"id": "A", "text": "₹$$24,000$$", "isCorrect": true}, {"id": "B", "text": "₹$$18,000$$", "isCorrect": false}, {"id": "C", "text": "₹$$30,000$$", "isCorrect": false}, {"id": "D", "text": "₹$$20,000$$", "isCorrect": false}]',
        'A',
        'Let incomes be $$4x$$ and $$3x$$. Incomes - Savings = Expenditures: $$\\frac{4x - 6000}{3x - 6000} = \\frac{3}{2} \\implies 2(4x - 6000) = 3(3x - 6000) \\implies 8x - 12000 = 9x - 18000 \\implies x = 6000$$. Monthly income of A $$= 4 \\times 6000 = ₹24,000$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000015'::uuid,
        'Mathematics',
        'Arithmetic',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'A can complete a piece of work in $$12$$ days and B in $$18$$ days. They work together for $$4$$ days, after which A leaves. In how many more days will B finish the remaining work?',
        '[{"id": "A", "text": "$$8$$ days", "isCorrect": true}, {"id": "B", "text": "$$6$$ days", "isCorrect": false}, {"id": "C", "text": "$$10$$ days", "isCorrect": false}, {"id": "D", "text": "$$7$$ days", "isCorrect": false}]',
        'A',
        'Total work $$= \\text{LCM}(12, 18) = 36$$ units. Efficiency of A $$= 3$$ units/day, B $$= 2$$ units/day. Work done in $$4$$ days by (A+B) $$= 4 \\times (3+2) = 20$$ units. Remaining work $$= 36 - 20 = 16$$ units. Time taken by B $$= \\frac{16}{2} = 8$$ days.'
    ),
    (
        'a1250000-0000-0000-0000-000000000016'::uuid,
        'Mathematics',
        'Arithmetic',
        'Pipes and Cisterns',
        'MEDIUM',
        'APPLY',
        'Two pipes A and B can fill a tank in $$15$$ hours and $$20$$ hours respectively, while a waste pipe C can empty it in $$30$$ hours. If all three pipes are opened simultaneously, in how many hours will the empty tank be filled?',
        '[{"id": "A", "text": "$$12$$ hours", "isCorrect": true}, {"id": "B", "text": "$$10$$ hours", "isCorrect": false}, {"id": "C", "text": "$$15$$ hours", "isCorrect": false}, {"id": "D", "text": "$$14$$ hours", "isCorrect": false}]',
        'A',
        'Capacity of tank $$= \\text{LCM}(15, 20, 30) = 60$$ units. Efficiency of A $$= +4$$, B $$= +3$$, C $$= -2$$. Net filling rate $$= 4 + 3 - 2 = 5$$ units/hr. Time taken $$= \\frac{60}{5} = 12$$ hours.'
    ),
    (
        'a1250000-0000-0000-0000-000000000017'::uuid,
        'Mathematics',
        'Arithmetic',
        'Time, Speed and Distance',
        'MEDIUM',
        'APPLY',
        'A train $$240\\text{ m}$$ long passes a pole in $$16\\text{ seconds}$$. How long will it take to pass a platform of length $$360\\text{ m}$$ at the same speed?',
        '[{"id": "A", "text": "$$40\\text{ seconds}$$", "isCorrect": true}, {"id": "B", "text": "$$36\\text{ seconds}$$", "isCorrect": false}, {"id": "C", "text": "$$45\\text{ seconds}$$", "isCorrect": false}, {"id": "D", "text": "$$32\\text{ seconds}$$", "isCorrect": false}]',
        'A',
        'Speed of train $$= \\frac{240}{16} = 15\\text{ m/s}$$. Total distance to cross platform $$= 240 + 360 = 600\\text{ m}$$. Time required $$= \\frac{600}{15} = 40\\text{ seconds}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000018'::uuid,
        'Mathematics',
        'Arithmetic',
        'Boats and Streams',
        'MEDIUM',
        'APPLY',
        'A boat goes $$24\\text{ km}$$ downstream in $$2\\text{ hours}$$ and $$16\\text{ km}$$ upstream in $$4\\text{ hours}$$. What is the speed of the current (stream)?',
        '[{"id": "A", "text": "$$4\\text{ km/h}$$", "isCorrect": true}, {"id": "B", "text": "$$3\\text{ km/h}$$", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ km/h}$$", "isCorrect": false}, {"id": "D", "text": "$$2\\text{ km/h}$$", "isCorrect": false}]',
        'A',
        'Downstream speed $$u = \\frac{24}{2} = 12\\text{ km/h}$$. Upstream speed $$v = \\frac{16}{4} = 4\\text{ km/h}$$. Speed of stream $$= \\frac{u - v}{2} = \\frac{12 - 4}{2} = 4\\text{ km/h}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000019'::uuid,
        'Mathematics',
        'Arithmetic',
        'Simple Interest',
        'MEDIUM',
        'APPLY',
        'A sum of money invested at simple interest amounts to ₹$$8,400$$ in $$3$$ years and to ₹$$9,600$$ in $$5$$ years. Find the principal sum.',
        '[{"id": "A", "text": "₹$$6,600$$", "isCorrect": true}, {"id": "B", "text": "₹$$6,000$$", "isCorrect": false}, {"id": "C", "text": "₹$$7,200$$", "isCorrect": false}, {"id": "D", "text": "₹$$6,400$$", "isCorrect": false}]',
        'A',
        'SI for $$2$$ years $$= 9600 - 8400 = ₹1200 \\implies \\text{SI per year} = ₹600$$. SI for $$3$$ years $$= 3 \\times 600 = ₹1800$$. Principal $$P = 8400 - 1800 = ₹6,600$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000020'::uuid,
        'Mathematics',
        'Arithmetic',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'What is the difference between compound interest (compounded annually) and simple interest on a principal of ₹$$15,000$$ at $$10\\%$$ per annum for $$2$$ years?',
        '[{"id": "A", "text": "₹$$150$$", "isCorrect": true}, {"id": "B", "text": "₹$$120$$", "isCorrect": false}, {"id": "C", "text": "₹$$180$$", "isCorrect": false}, {"id": "D", "text": "₹$$200$$", "isCorrect": false}]',
        'A',
        'Difference for $$2$$ years $$= P\\left(\\frac{R}{100}\\right)^2 = 15000 \\times \\left(\\frac{10}{100}\\right)^2 = 15000 \\times \\frac{1}{100} = ₹150$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000021'::uuid,
        'Mathematics',
        'Arithmetic',
        'Averages',
        'MEDIUM',
        'APPLY',
        'The average weight of $$24$$ students in a class is $$45\\text{ kg}$$. If the weight of the teacher is included, the average increases by $$500\\text{ grams}$$. What is the weight of the teacher?',
        '[{"id": "A", "text": "$$57.5\\text{ kg}$$", "isCorrect": true}, {"id": "B", "text": "$$55.0\\text{ kg}$$", "isCorrect": false}, {"id": "C", "text": "$$58.0\\text{ kg}$$", "isCorrect": false}, {"id": "D", "text": "$$60.0\\text{ kg}$$", "isCorrect": false}]',
        'A',
        'Weight of teacher $$= \\text{New Average} + (\\text{Old Count} \\times \\text{Increase}) = 45.5 + (24 \\times 0.5) = 45.5 + 12 = 57.5\\text{ kg}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000022'::uuid,
        'Mathematics',
        'Arithmetic',
        'Alligation and Mixtures',
        'MEDIUM',
        'APPLY',
        'In what ratio must a grocer mix two varieties of tea costing ₹$$180\\text{ per kg}$$ and ₹$$240\\text{ per kg}$$ respectively, so that selling the mixture at ₹$$231\\text{ per kg}$$ yields a profit of $$10\\%$$?',
        '[{"id": "A", "text": "$$1:1$$", "isCorrect": true}, {"id": "B", "text": "$$2:3$$", "isCorrect": false}, {"id": "C", "text": "$$3:4$$", "isCorrect": false}, {"id": "D", "text": "$$1:2$$", "isCorrect": false}]',
        'A',
        'Mean cost price $$= \\frac{231}{1.10} = ₹210\\text{ per kg}$$. By rule of alligation: $$\\frac{\\text{Quantity 1}}{\\text{Quantity 2}} = \\frac{240 - 210}{210 - 180} = \\frac{30}{30} = 1:1$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000023'::uuid,
        'Mathematics',
        'Arithmetic',
        'LCM and HCF',
        'MEDIUM',
        'APPLY',
        'The HCF of two numbers is $$16$$ and their LCM is $$480$$. If one of the numbers is $$96$$, find the other number.',
        '[{"id": "A", "text": "$$80$$", "isCorrect": true}, {"id": "B", "text": "$$72$$", "isCorrect": false}, {"id": "C", "text": "$$84$$", "isCorrect": false}, {"id": "D", "text": "$$90$$", "isCorrect": false}]',
        'A',
        '$$\\text{Product of numbers} = \\text{HCF} \\times \\text{LCM} \\implies 96 \\times x = 16 \\times 480 \\implies x = \\frac{16 \\times 480}{96} = \\frac{7680}{96} = 80$$.'
    ),

    -- =========================================================================
    -- RULE 5: Mathematics > Algebra (MEDIUM, APPLY) - 6 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000024'::uuid,
        'Mathematics',
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x + \\frac{1}{x} = 5$$, find the value of $$x^3 + \\frac{1}{x^3}$$.',
        '[{"id": "A", "text": "$$110$$", "isCorrect": true}, {"id": "B", "text": "$$125$$", "isCorrect": false}, {"id": "C", "text": "$$140$$", "isCorrect": false}, {"id": "D", "text": "$$115$$", "isCorrect": false}]',
        'A',
        '$$x^3 + \\frac{1}{x^3} = \\left(x + \\frac{1}{x}\\right)^3 - 3\\left(x + \\frac{1}{x}\\right) = 5^3 - 3(5) = 125 - 15 = 110$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000025'::uuid,
        'Mathematics',
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x + \\frac{1}{x} = 4$$, find the value of $$x^4 + \\frac{1}{x^4}$$.',
        '[{"id": "A", "text": "$$194$$", "isCorrect": true}, {"id": "B", "text": "$$196$$", "isCorrect": false}, {"id": "C", "text": "$$192$$", "isCorrect": false}, {"id": "D", "text": "$$188$$", "isCorrect": false}]',
        'A',
        '$$x^2 + \\frac{1}{x^2} = 4^2 - 2 = 14$$. Then $$x^4 + \\frac{1}{x^4} = 14^2 - 2 = 196 - 2 = 194$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000026'::uuid,
        'Mathematics',
        'Algebra',
        'Quadratic Equations',
        'MEDIUM',
        'APPLY',
        'If $$\\alpha$$ and $$\\beta$$ are the roots of the quadratic equation $$2x^2 - 7x + 5 = 0$$, evaluate $$\\frac{1}{\\alpha} + \\frac{1}{\\beta}$$.',
        '[{"id": "A", "text": "$$\\frac{7}{5}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{5}{7}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{7}{2}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{5}{2}$$", "isCorrect": false}]',
        'A',
        'Sum of roots $$\\alpha + \\beta = -\\frac{-7}{2} = \\frac{7}{2}$$, product of roots $$\\alpha\\beta = \\frac{5}{2}$$. Therefore, $$\\frac{1}{\\alpha} + \\frac{1}{\\beta} = \\frac{\\alpha + \\beta}{\\alpha\\beta} = \\frac{7/2}{5/2} = \\frac{7}{5}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000027'::uuid,
        'Mathematics',
        'Algebra',
        'Conditional Identities',
        'MEDIUM',
        'APPLY',
        'If $$a + b + c = 0$$, what is the simplified value of $$\\frac{a^2}{bc} + \\frac{b^2}{ca} + \\frac{c^2}{ab}$$?',
        '[{"id": "A", "text": "$$3$$", "isCorrect": true}, {"id": "B", "text": "$$0$$", "isCorrect": false}, {"id": "C", "text": "$$1$$", "isCorrect": false}, {"id": "D", "text": "$$-3$$", "isCorrect": false}]',
        'A',
        '$$\\frac{a^2}{bc} + \\frac{b^2}{ca} + \\frac{c^2}{ab} = \\frac{a^3 + b^3 + c^3}{abc}$$. When $$a + b + c = 0$$, $$a^3 + b^3 + c^3 = 3abc$$. Thus, $$\\frac{3abc}{abc} = 3$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000028'::uuid,
        'Mathematics',
        'Algebra',
        'Linear Systems',
        'MEDIUM',
        'APPLY',
        'For what value of $$k$$ does the system of equations $$2x + 3y = 7$$ and $$6x + ky = 21$$ have infinitely many solutions?',
        '[{"id": "A", "text": "$$9$$", "isCorrect": true}, {"id": "B", "text": "$$6$$", "isCorrect": false}, {"id": "C", "text": "$$12$$", "isCorrect": false}, {"id": "D", "text": "$$3$$", "isCorrect": false}]',
        'A',
        'For infinitely many solutions: $$\\frac{a_1}{a_2} = \\frac{b_1}{b_2} = \\frac{c_1}{c_2} \\implies \\frac{2}{6} = \\frac{3}{k} = \\frac{7}{21} \\implies \\frac{1}{3} = \\frac{3}{k} \\implies k = 9$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000029'::uuid,
        'Mathematics',
        'Algebra',
        'Indices and Surds',
        'MEDIUM',
        'APPLY',
        'If $$2^{x+3} = 32$$, what is the value of $$3^{x+1}$$?',
        '[{"id": "A", "text": "$$27$$", "isCorrect": true}, {"id": "B", "text": "$$9$$", "isCorrect": false}, {"id": "C", "text": "$$81$$", "isCorrect": false}, {"id": "D", "text": "$$18$$", "isCorrect": false}]',
        'A',
        '$$2^{x+3} = 32 = 2^5 \\implies x + 3 = 5 \\implies x = 2$$. Then $$3^{x+1} = 3^{2+1} = 3^3 = 27$$.'
    ),

    -- =========================================================================
    -- RULE 6: Mathematics > Geometry (HARD, ANALYZE) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000030'::uuid,
        'Mathematics',
        'Geometry',
        'Circle Theorems',
        'HARD',
        'ANALYZE',
        'From an external point $$P$$, a tangent $$PT$$ of length $$12\\text{ cm}$$ is drawn to a circle. A secant $$PAB$$ intersects the circle at $$A$$ and $$B$$. If $$PA = 8\\text{ cm}$$, what is the length of chord $$AB$$?',
        '[{"id": "A", "text": "$$10\\text{ cm}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$6\\text{ cm}$$", "isCorrect": false}]',
        'A',
        'By the Tangent-Secant Theorem: $$PT^2 = PA \\cdot PB \\implies 12^2 = 8 \\cdot PB \\implies 144 = 8 \\cdot PB \\implies PB = 18\\text{ cm}$$. Since $$PB = PA + AB$$, $$AB = 18 - 8 = 10\\text{ cm}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000031'::uuid,
        'Mathematics',
        'Geometry',
        'Triangle Centers',
        'HARD',
        'ANALYZE',
        'In $$\\triangle ABC$$, $$I$$ is the incenter. If $$\\angle BAC = 70^\\circ$$, what is the measure of $$\\angle BIC$$?',
        '[{"id": "A", "text": "$$125^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$110^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$135^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$140^\\circ$$", "isCorrect": false}]',
        'A',
        'The angle formed at the incenter is $$\\angle BIC = 90^\\circ + \\frac{\\angle A}{2} = 90^\\circ + \\frac{70^\\circ}{2} = 90^\\circ + 35^\\circ = 125^\\circ$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000032'::uuid,
        'Mathematics',
        'Geometry',
        'Cyclic Quadrilaterals',
        'HARD',
        'ANALYZE',
        'In a cyclic quadrilateral $$ABCD$$, the side $$AB$$ is extended to point $$E$$. If $$\\angle CBE = 82^\\circ$$ and $$\\angle BAC = 38^\\circ$$, determine the measure of $$\\angle CAD$$.',
        '[{"id": "A", "text": "$$44^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$48^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$52^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$42^\\circ$$", "isCorrect": false}]',
        'A',
        'In a cyclic quadrilateral, exterior angle equals interior opposite angle: $$\\angle ADC = \\angle CBE = 82^\\circ$$. Also $$\\angle BDC = \\angle BAC = 38^\\circ$$ (angles in same segment subtended by arc $$BC$$). Thus, $$\\angle CAD = \\angle CBD = \\angle ADC - \\angle BAC = 82^\\circ - 38^\\circ = 44^\\circ$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000033'::uuid,
        'Mathematics',
        'Geometry',
        'Intersecting Chords',
        'HARD',
        'ANALYZE',
        'Two chords $$AB$$ and $$CD$$ of a circle intersect internally at point $$P$$. If $$AP = 6\\text{ cm}$$, $$PB = 8\\text{ cm}$$, and $$CP = 4\\text{ cm}$$, calculate the total length of chord $$CD$$.',
        '[{"id": "A", "text": "$$16\\text{ cm}$$", "isCorrect": true}, {"id": "B", "text": "$$12\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$18\\text{ cm}$$", "isCorrect": false}]',
        'A',
        'By the Intersecting Chords Theorem: $$AP \\cdot PB = CP \\cdot PD \\implies 6 \\times 8 = 4 \\times PD \\implies 48 = 4 \\cdot PD \\implies PD = 12\\text{ cm}$$. Total length $$CD = CP + PD = 4 + 12 = 16\\text{ cm}$$.'
    ),

    -- =========================================================================
    -- RULE 7: Mathematics > Trigonometry (MEDIUM, APPLY) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000034'::uuid,
        'Mathematics',
        'Trigonometry',
        'Heights and Distances',
        'MEDIUM',
        'APPLY',
        'The angle of elevation of the top of a vertical tower from a point on level ground $$30\\text{ m}$$ away from its foot is $$30^\\circ$$. Find the height of the tower.',
        '[{"id": "A", "text": "$$10\\sqrt{3}\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$15\\sqrt{3}\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$30\\sqrt{3}\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$20\\text{ m}$$", "isCorrect": false}]',
        'A',
        '$$\\tan 30^\\circ = \\frac{h}{30} \\implies \\frac{1}{\\sqrt{3}} = \\frac{h}{30} \\implies h = \\frac{30}{\\sqrt{3}} = 10\\sqrt{3}\\text{ m}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000035'::uuid,
        'Mathematics',
        'Trigonometry',
        'Trigonometric Identities',
        'MEDIUM',
        'APPLY',
        'If $$\\sec \\theta + \\tan \\theta = 3$$, what is the value of $$\\sin \\theta$$ (where $$0^\\circ < \\theta < 90^\\circ$$)?',
        '[{"id": "A", "text": "$$\\frac{4}{5}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{3}{5}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{1}{3}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{5}{13}$$", "isCorrect": false}]',
        'A',
        'Since $$\\sec^2\\theta - \\tan^2\\theta = 1$$, $$\\sec\\theta - \\tan\\theta = \\frac{1}{3}$$. Adding gives $$2\\sec\\theta = 3 + \\frac{1}{3} = \\frac{10}{3} \\implies \\sec\\theta = \\frac{5}{3} \\implies \\cos\\theta = \\frac{3}{5}$$. Thus $$\\sin\\theta = \\sqrt{1 - (3/5)^2} = \\frac{4}{5}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000036'::uuid,
        'Mathematics',
        'Trigonometry',
        'Trigonometric Simplification',
        'MEDIUM',
        'APPLY',
        'Simplify the expression: $$\\frac{\\cos 20^\\circ}{\\sin 70^\\circ} + \\frac{\\tan 35^\\circ}{\\cot 55^\\circ} - 2\\sin 30^\\circ$$.',
        '[{"id": "A", "text": "$$1$$", "isCorrect": true}, {"id": "B", "text": "$$0$$", "isCorrect": false}, {"id": "C", "text": "$$2$$", "isCorrect": false}, {"id": "D", "text": "$$-1$$", "isCorrect": false}]',
        'A',
        'Since $$\\cos 20^\\circ = \\sin(90^\\circ - 20^\\circ) = \\sin 70^\\circ$$, $$\\frac{\\cos 20^\\circ}{\\sin 70^\\circ} = 1$$. Similarly, $$\\frac{\\tan 35^\\circ}{\\cot 55^\\circ} = 1$$. And $$2\\sin 30^\\circ = 2\\left(\\frac{1}{2}\\right) = 1$$. Result $$= 1 + 1 - 1 = 1$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000037'::uuid,
        'Mathematics',
        'Trigonometry',
        'Trigonometric Equations',
        'MEDIUM',
        'APPLY',
        'If $$2\\sin^2 \\theta + 3\\cos \\theta = 3$$ (where $$0^\\circ \\le \\theta \\le 90^\\circ$$), find the value of $$\\theta$$.',
        '[{"id": "A", "text": "$$60^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$30^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$45^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$0^\\circ$$", "isCorrect": false}]',
        'A',
        '$$2(1 - \\cos^2\\theta) + 3\\cos\\theta = 3 \\implies 2 - 2\\cos^2\\theta + 3\\cos\\theta = 3 \\implies 2\\cos^2\\theta - 3\\cos\\theta + 1 = 0 \\implies (2\\cos\\theta - 1)(\\cos\\theta - 1) = 0$$. So $$\\cos\\theta = \\frac{1}{2} \\implies \\theta = 60^\\circ$$ (or $$\\cos\\theta = 1 \\implies \\theta = 0^\\circ$$, with $$60^\\circ$$ being the non-trivial acute angle).'
    ),

    -- =========================================================================
    -- RULE 8: Mathematics > Statistics (EASY, UNDERSTAND) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000038'::uuid,
        'Mathematics',
        'Statistics',
        'Empirical Relationship',
        'EASY',
        'UNDERSTAND',
        'For a moderately skewed frequency distribution, what is the empirical relationship connecting Mean, Median, and Mode?',
        '[{"id": "A", "text": "$$\\text{Mode} = 3\\text{Median} - 2\\text{Mean}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Mode} = 2\\text{Median} - 3\\text{Mean}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Mean} = 3\\text{Mode} - 2\\text{Median}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Median} = 3\\text{Mean} - 2\\text{Mode}$$", "isCorrect": false}]',
        'A',
        'Karl Pearson''s empirical formula for moderately asymmetrical distributions states that $$\\text{Mode} = 3\\text{Median} - 2\\text{Mean}$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000039'::uuid,
        'Mathematics',
        'Statistics',
        'Measures of Dispersion',
        'EASY',
        'UNDERSTAND',
        'What is the range of the given dataset: $$14, 28, 9, 36, 17, 45, 21, 12$$?',
        '[{"id": "A", "text": "$$36$$", "isCorrect": true}, {"id": "B", "text": "$$34$$", "isCorrect": false}, {"id": "C", "text": "$$32$$", "isCorrect": false}, {"id": "D", "text": "$$38$$", "isCorrect": false}]',
        'A',
        '$$\\text{Range} = \\text{Maximum value} - \\text{Minimum value} = 45 - 9 = 36$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000040'::uuid,
        'Mathematics',
        'Statistics',
        'Median Calculation',
        'EASY',
        'UNDERSTAND',
        'Find the median of the numbers: $$7, 12, 15, 6, 9, 18, 14$$.',
        '[{"id": "A", "text": "$$12$$", "isCorrect": true}, {"id": "B", "text": "$$13$$", "isCorrect": false}, {"id": "C", "text": "$$14$$", "isCorrect": false}, {"id": "D", "text": "$$10$$", "isCorrect": false}]',
        'A',
        'Arranging the $$n=7$$ data points in ascending order: $$6, 7, 9, 12, 14, 15, 18$$. The middle (4th) element is $$12$$.'
    ),
    (
        'a1250000-0000-0000-0000-000000000041'::uuid,
        'Mathematics',
        'Statistics',
        'Mode Concept',
        'EASY',
        'UNDERSTAND',
        'Which measure of central tendency corresponds to the value that occurs with the greatest frequency in a dataset?',
        '[{"id": "A", "text": "Mode", "isCorrect": true}, {"id": "B", "text": "Mean", "isCorrect": false}, {"id": "C", "text": "Median", "isCorrect": false}, {"id": "D", "text": "Geometric Mean", "isCorrect": false}]',
        'A',
        'The mode is defined as the observation that appears most frequently (has the highest frequency) in a dataset.'
    ),

    -- =========================================================================
    -- RULE 9: General Intelligence and Reasoning > Coding and Decoding (EASY, APPLY) - 4 Qs
    -- =========================================================================
    (
        'a1250000-0000-0000-0000-000000000042'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Letter Shift Coding',
        'EASY',
        'APPLY',
        'In a certain code language, if "TRAIN" is coded as "WUDLQ", how will "TRACK" be coded in that same language?',
        '[{"id": "A", "text": "WUDFN", "isCorrect": true}, {"id": "B", "text": "WUDDM", "isCorrect": false}, {"id": "C", "text": "WUCFN", "isCorrect": false}, {"id": "D", "text": "VTCEL", "isCorrect": false}]',
        'A',
        'Each letter is shifted forward by $$+3$$ positions in the alphabet: T(+3)=W, R(+3)=U, A(+3)=D, C(+3)=F, K(+3)=N. Hence, "TRACK" is coded as "WUDFN".'
    ),
    (
        'a1250000-0000-0000-0000-000000000043'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Reverse Alphabet Coding',
        'EASY',
        'APPLY',
        'If in a code language, "CAT" is coded as "XZG", how will "DOG" be coded in the same system?',
        '[{"id": "A", "text": "WLT", "isCorrect": true}, {"id": "B", "text": "WLS", "isCorrect": false}, {"id": "C", "text": "VLT", "isCorrect": false}, {"id": "D", "text": "XMT", "isCorrect": false}]',
        'A',
        'Each letter is replaced by its opposite/complementary letter (sum of alphabetical positions $$= 27$$): D(4) -> W(23), O(15) -> L(12), G(7) -> T(20).'
    ),
    (
        'a1250000-0000-0000-0000-000000000044'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Number Value Coding',
        'EASY',
        'APPLY',
        'If $$A = 1$$, $$AND = 19$$ (where $$1 + 14 + 4 = 19$$), what is the numerical code for $$BAT$$?',
        '[{"id": "A", "text": "$$23$$", "isCorrect": true}, {"id": "B", "text": "$$22$$", "isCorrect": false}, {"id": "C", "text": "$$24$$", "isCorrect": false}, {"id": "D", "text": "$$20$$", "isCorrect": false}]',
        'A',
        'The code is the sum of alphabetical positions: B(2) + A(1) + T(20) = 2 + 1 + 20 = 23.'
    ),
    (
        'a1250000-0000-0000-0000-000000000045'::uuid,
        'General Intelligence and Reasoning',
        'Coding and Decoding',
        'Direct Substitution Coding',
        'EASY',
        'APPLY',
        'In a certain code language, if "EARTH" is coded as "81537" and "PEAR" is coded as "2815", how will "PEARL" be coded if "L" is coded as "9"?',
        '[{"id": "A", "text": "28159", "isCorrect": true}, {"id": "B", "text": "28157", "isCorrect": false}, {"id": "C", "text": "28153", "isCorrect": false}, {"id": "D", "text": "28195", "isCorrect": false}]',
        'A',
        'By direct substitution: P=2, E=8, A=1, R=5, and L=9. Therefore, "PEARL" is coded as 28159.'
    )
) AS v(id, subject_name, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = v.subject_name AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
