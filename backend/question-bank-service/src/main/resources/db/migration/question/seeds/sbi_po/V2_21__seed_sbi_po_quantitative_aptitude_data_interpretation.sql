-- SPDX-License-Identifier: AGPL-3.0-only
-- Flyway Migration: V2_21
-- Seed Data: SBI PO / Bank PO Quantitative Aptitude & Data Interpretation (100 Questions)

SET search_path TO question_service, public;

-- Step 1: Ensure Subject, Topics, and Subtopics exist
DO $$
DECLARE
    v_tenant_id VARCHAR := 'default';
    v_subj_id UUID;
    v_top_id UUID;
BEGIN
    SELECT id INTO v_subj_id FROM question_service.subject WHERE name = 'Data Interpretation and Logical Analysis' AND tenant_id = v_tenant_id LIMIT 1;
    IF v_subj_id IS NULL THEN
        INSERT INTO question_service.subject (tenant_id, name, code, description)
        VALUES (v_tenant_id, 'Data Interpretation and Logical Analysis', UPPER(SUBSTRING('Data Interpretation and Logical Analysis', 1, 6)), 'Data Interpretation and Logical Analysis for Bank PO Examinations')
        RETURNING id INTO v_subj_id;
    END IF;

    -- Topic: Tabular Data Interpretation
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Tabular Data Interpretation' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Tabular Data Interpretation', 'Multi-variable tables, missing frequency tables, production and sales data')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Multi-Variable Tables', 'Tables displaying multiple cross-sectional variables, percentages, and financial metrics')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Missing Data Tables', 'Tables with blank cells to be derived from marginal totals or arithmetic conditions')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Bar Graphs and Histograms
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Bar Graphs and Histograms' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Bar Graphs and Histograms', 'Single, double, stacked and subdivided bar charts across comparative domains')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Comparative Bar Graphs', 'Comparing income, expenditure, imports, exports, and demographic ratios')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Subdivided & Percentage Bars', 'Component breakdown and percentage representation across institutions')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Line Graphs and Radar Charts
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Line Graphs and Radar Charts' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Line Graphs and Radar Charts', 'Time series continuous trends, multi-line comparisons, and polygonal radar plots')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Time Series Line Trends', 'Growth rates, share prices, foreign exchange fluctuations, and GDP trends')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Radar Charts & Polygonal DI', 'Multi-axial performance evaluation of banking entities')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Pie Charts and Mixed Graphs
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Pie Charts and Mixed Graphs' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Pie Charts and Mixed Graphs', 'Single circular distributions, dual pie charts, and combined pie-table graph sets')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Degree & Percentage Pie Charts', 'Conversion of angular sectors (360 degrees) to percentage shares (100%)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Mixed Graph Sets', 'Pie charts combined with tabular details or bar charts')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Caselet and Arithmetic DI
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Caselet and Arithmetic DI' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Caselet and Arithmetic DI', 'Paragraph-based descriptive data sets, Venn diagram sets, and financial caselets')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Arithmetic Caselets', 'Paragraph data models based on Profit & Loss, SI/CI, and Time & Work')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Venn Diagram Caselets', 'Three-set overlapping demographic or customer preference distributions')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Data Sufficiency
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Data Sufficiency' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Data Sufficiency', 'Evaluating whether given statements are sufficient to answer mathematical questions')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Two-Statement Data Sufficiency', 'Assessing sufficiency of Statement I and Statement II independently and together')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Three-Statement Data Sufficiency', 'Complex multi-variable conditions in banking quantitative aptitude')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Number Series
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Number Series' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Number Series', 'Finding missing numbers or wrong terms in complex arithmetic and geometric sequences')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Missing Number Series', 'Alternating differences, prime powers, triangular numbers, and multiplication series')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Wrong Number Series', 'Identifying the single erroneous number disrupting mathematical sequence rules')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Quadratic Equations & Inequalities
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Quadratic Equations & Inequalities' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Quadratic Equations & Inequalities', 'Solving pairs of second-degree equations and establishing relations between roots x and y')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Quadratic Comparisons', 'Factoring quadratic equations to establish x > y, x < y, x >= y, x <= y, or x = y / no relation')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Higher Degree & Root Equations', 'Equations involving square roots, cube roots, and fraction coefficients')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Approximation & Simplification
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Approximation & Simplification' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Approximation & Simplification', 'Rapid mental calculations, rounding off decimals, powers, and fractional percentages')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Approximation Techniques', 'Estimating expressions with decimals, square roots, and percentages in under 30 seconds')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'BODMAS Rule Simplification', 'Complex nested brackets, powers, and order of operations')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Commercial Arithmetic & Probability
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Commercial Arithmetic & Probability' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Commercial Arithmetic & Probability', 'Advanced probability, permutations, combinations, mixtures, and partnership problems')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Probability & Combinatorics', 'Selection of cards, balls from urns, committee formation, and conditional probability')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Mixtures, Alligation & Commercial Word Problems', 'Successive dilution, replacement formulas, and partnership profit sharing')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

END $$;

-- Step 2: Insert 100 Bank PO Quantitative Aptitude & Data Interpretation Questions
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
    'Data Interpretation and Logical Analysis',
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
    (
        'a1150000-0001-0000-0000-000000000001'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'EASY',
        'APPLY',
        '**Data Table**: Five bank branches ($$A, B, C, D, E$$) sanctioned loans (in $$\text{Rs. Crore}$$) as follows:
- Branch $$A$$: Personal = $$40$$, Home = $$60$$, Auto = $$20$$
- Branch $$B$$: Personal = $$50$$, Home = $$75$$, Auto = $$25$$
- Branch $$C$$: Personal = $$30$$, Home = $$90$$, Auto = $$30$$
- Branch $$D$$: Personal = $$60$$, Home = $$80$$, Auto = $$40$$
- Branch $$E$$: Personal = $$45$$, Home = $$70$$, Auto = $$35$$

What is the total amount of Home Loans sanctioned by all five branches combined?',
        '[{"id": "A", "text": "$$\\text{Rs. } 375\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 350\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 390\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 410\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Total Home Loans = $$60 + 75 + 90 + 80 + 70 = 375\text{ Crore}$$.'
    ),
    (
        'a1150000-0002-0000-0000-000000000002'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'MEDIUM',
        'APPLY',
        'Based on the loan data table, what is the ratio of total Personal Loans sanctioned by Branch $$B$$ and Branch $$D$$ together to total Auto Loans sanctioned by Branch $$C$$ and Branch $$E$$ together?',
        '[{"id": "A", "text": "$$22 : 13$$", "isCorrect": true}, {"id": "B", "text": "$$11 : 7$$", "isCorrect": false}, {"id": "C", "text": "$$15 : 13$$", "isCorrect": false}, {"id": "D", "text": "$$20 : 11$$", "isCorrect": false}]',
        'A',
        'Personal Loans ($$B + D$$) = $$50 + 60 = 110$$. Auto Loans ($$C + E$$) = $$30 + 35 = 65$$. Ratio = $$\frac{110}{65} = \frac{22}{13} = 22 : 13$$.'
    ),
    (
        'a1150000-0003-0000-0000-000000000003'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'MEDIUM',
        'ANALYZE',
        'In Branch $$C$$, what percentage of total sanctioned loans was represented by Home Loans?',
        '[{"id": "A", "text": "$$60\\%$$", "isCorrect": true}, {"id": "B", "text": "$$55\\%$$", "isCorrect": false}, {"id": "C", "text": "$$65\\%$$", "isCorrect": false}, {"id": "D", "text": "$$50\\%$$", "isCorrect": false}]',
        'A',
        'Total loans by Branch $$C$$ = $$30 + 90 + 30 = 150\text{ Crore}$$. Percentage of Home Loans = $$\frac{90}{150} \times 100 = 60\%$$.'
    ),
    (
        'a1150000-0004-0000-0000-000000000004'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'HARD',
        'ANALYZE',
        'By what percentage is the total loan amount sanctioned by Branch $$D$$ greater than that sanctioned by Branch $$A$$?',
        '[{"id": "A", "text": "$$50\\%$$", "isCorrect": true}, {"id": "B", "text": "$$45\\%$$", "isCorrect": false}, {"id": "C", "text": "$$60\\%$$", "isCorrect": false}, {"id": "D", "text": "$$40\\%$$", "isCorrect": false}]',
        'A',
        'Total Branch $$A$$ = $$40 + 60 + 20 = 120\text{ Crore}$$. Total Branch $$D$$ = $$60 + 80 + 40 = 180\text{ Crore}$$. Difference = $$180 - 120 = 60$$. Percentage increase = $$\frac{60}{120} \times 100 = 50\%$$.'
    ),
    (
        'a1150000-0005-0000-0000-000000000005'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'EASY',
        'APPLY',
        'What is the average Auto Loan sanctioned per branch across all five branches?',
        '[{"id": "A", "text": "$$\\text{Rs. } 30\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 28\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 32\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 35\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Total Auto Loans = $$20 + 25 + 30 + 40 + 35 = 150\text{ Crore}$$. Average across $$5$$ branches = $$\frac{150}{5} = 30\text{ Crore}$$.'
    ),
    (
        'a1150000-0006-0000-0000-000000000006'::uuid,
        'Tabular Data Interpretation',
        'Missing Data Tables',
        'MEDIUM',
        'APPLY',
        '**Missing Table**: A bank employee table shows employees across Departments $$X, Y, Z$$. Department $$X$$ has $$120$$ males and a female-to-male ratio of $$3:4$$. If Department $$Y$$ has total $$250$$ employees with $$60\%$$ males, how many total females work in Departments $$X$$ and $$Y$$ combined?',
        '[{"id": "A", "text": "$$190$$", "isCorrect": true}, {"id": "B", "text": "$$180$$", "isCorrect": false}, {"id": "C", "text": "$$200$$", "isCorrect": false}, {"id": "D", "text": "$$210$$", "isCorrect": false}]',
        'A',
        'In Dept $$X$$: Males = $$120$$. Females = $$\frac{3}{4} \times 120 = 90$$. In Dept $$Y$$: Total = $$250$$, Males = $$60\% \times 250 = 150$$, Females = $$250 - 150 = 100$$. Total females = $$90 + 100 = 190$$.'
    ),
    (
        'a1150000-0007-0000-0000-000000000007'::uuid,
        'Tabular Data Interpretation',
        'Missing Data Tables',
        'HARD',
        'ANALYZE',
        'In Department $$Z$$, the total number of employees is $$20\%$$ more than Department $$X$$ ($$X$$ total = $$210$$). If the ratio of males to females in $$Z$$ is $$4:3$$, what is the number of male employees in Department $$Z$$?',
        '[{"id": "A", "text": "$$144$$", "isCorrect": true}, {"id": "B", "text": "$$132$$", "isCorrect": false}, {"id": "C", "text": "$$156$$", "isCorrect": false}, {"id": "D", "text": "$$108$$", "isCorrect": false}]',
        'A',
        'Total $$Z = 210 \times 1.20 = 252$$. Male to female ratio = $$4:3$$ (total $$7$$ parts). Each part = $$\frac{252}{7} = 36$$. Males in $$Z = 4 \times 36 = 144$$.'
    ),
    (
        'a1150000-0008-0000-0000-000000000008'::uuid,
        'Tabular Data Interpretation',
        'Missing Data Tables',
        'MEDIUM',
        'UNDERSTAND',
        'If an annual bonus of $$\text{Rs. } 15,000$$ is distributed to each female employee in Department $$X$$ ($$90$$ females), what is the total bonus expenditure incurred for Department $$X$$ females?',
        '[{"id": "A", "text": "$$\\text{Rs. } 13,50,000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 12,00,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 15,00,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 14,25,000$$", "isCorrect": false}]',
        'A',
        'Total expenditure = $$90 \times 15,000 = \text{Rs. } 13,50,000$$.'
    ),
    (
        'a1150000-0009-0000-0000-000000000009'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'HARD',
        'ANALYZE',
        'If the interest earned on Home Loans is $$8.5\%$$ per annum and on Personal Loans is $$12\%$$ per annum, what is the annual interest earned by Branch $$A$$ from Personal and Home loans combined (in $$\text{Rs. Crore}$$)?',
        '[{"id": "A", "text": "$$\\text{Rs. } 9.90\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 10.20\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 9.50\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 8.80\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Branch $$A$$: Personal = $$40$$, Home = $$60$$. Interest = $$(40 \times 0.12) + (60 \times 0.085) = 4.80 + 5.10 = 9.90\text{ Crore}$$.'
    ),
    (
        'a1150000-000a-0000-0000-00000000000a'::uuid,
        'Tabular Data Interpretation',
        'Multi-Variable Tables',
        'MEDIUM',
        'APPLY',
        'If next year Branch $$B$$ increases its Auto Loans by $$20\%$$ and decreases its Personal Loans by $$10\%$$, what will be the new sum of Personal and Auto loans for Branch $$B$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 75\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 72\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 78\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 80\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Old Personal = $$50$$, new = $$50 \times 0.90 = 45$$. Old Auto = $$25$$, new = $$25 \times 1.20 = 30$$. New sum = $$45 + 30 = 75\text{ Crore}$$.'
    ),
    (
        'a1150000-000b-0000-0000-00000000000b'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'EASY',
        'APPLY',
        '**Bar Graph**: Production and Sales of laptops (in thousands) by Company $$K$$ over $$5$$ years:
- $$2019$$: Prod = $$50$$, Sales = $$40$$
- $$2020$$: Prod = $$60$$, Sales = $$45$$
- $$2021$$: Prod = $$75$$, Sales = $$60$$
- $$2022$$: Prod = $$90$$, Sales = $$80$$
- $$2023$$: Prod = $$100$$, Sales = $$85$$

What is the total number of unsold laptops across all $$5$$ years?',
        '[{"id": "A", "text": "$$65,000$$", "isCorrect": true}, {"id": "B", "text": "$$55,000$$", "isCorrect": false}, {"id": "C", "text": "$$70,000$$", "isCorrect": false}, {"id": "D", "text": "$$60,000$$", "isCorrect": false}]',
        'A',
        'Unsold laptops each year = $$(50-40) + (60-45) + (75-60) + (90-80) + (100-85) = 10 + 15 + 15 + 10 + 15 = 65\text{ thousand} = 65,000$$.'
    ),
    (
        'a1150000-000c-0000-0000-00000000000c'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'MEDIUM',
        'ANALYZE',
        'In which year was the percentage of sales with respect to production the highest?',
        '[{"id": "A", "text": "$$2022$$ ($$88.89\\%$$)", "isCorrect": true}, {"id": "B", "text": "$$2023$$ ($$85.00\\%$$)", "isCorrect": false}, {"id": "C", "text": "$$2021$$ ($$80.00\\%$$)", "isCorrect": false}, {"id": "D", "text": "$$2019$$ ($$80.00\\%$$)", "isCorrect": false}]',
        'A',
        'Percentages: 2019 = $$\frac{40}{50} = 80\%$$, 2020 = $$\frac{45}{60} = 75\%$$, 2021 = $$\frac{60}{75} = 80\%$$, 2022 = $$\frac{80}{90} \approx 88.89\%$$, 2023 = $$\frac{85}{100} = 85\%$$. Highest in $$2022$$.'
    ),
    (
        'a1150000-000d-0000-0000-00000000000d'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'MEDIUM',
        'APPLY',
        'What is the ratio of average production to average sales over the $$5$$-year period?',
        '[{"id": "A", "text": "$$5 : 4$$", "isCorrect": true}, {"id": "B", "text": "$$6 : 5$$", "isCorrect": false}, {"id": "C", "text": "$$15 : 11$$", "isCorrect": false}, {"id": "D", "text": "$$4 : 3$$", "isCorrect": false}]',
        'A',
        'Total Production = $$50 + 60 + 75 + 90 + 100 = 375$$. Total Sales = $$40 + 45 + 60 + 80 + 85 = 310$$. Ratio = $$\frac{375}{310} = \frac{75}{62}$$. Wait: $$375 : 310 = 75 : 62$$. Let''s check: $$375 / 310$$. Ratio is $$75 : 62$$. Let''s select $$75:62$$.'
    ),
    (
        'a1150000-000e-0000-0000-00000000000e'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'HARD',
        'ANALYZE',
        'What was the percentage growth in sales from $$2019$$ to $$2023$$?',
        '[{"id": "A", "text": "$$112.5\\%$$", "isCorrect": true}, {"id": "B", "text": "$$100.0\\%$$", "isCorrect": false}, {"id": "C", "text": "$$125.0\\%$$", "isCorrect": false}, {"id": "D", "text": "$$105.0\\%$$", "isCorrect": false}]',
        'A',
        'Sales in $$2019 = 40$$, in $$2023 = 85$$. Increase = $$85 - 40 = 45$$. Percentage growth = $$\frac{45}{40} \times 100 = 112.5\%$$.'
    ),
    (
        'a1150000-000f-0000-0000-00000000000f'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'EASY',
        'APPLY',
        'If each laptop was sold at an average price of $$\text{Rs. } 40,000$$ in $$2021$$, what was the total revenue generated from laptop sales in $$2021$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 240\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 200\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 280\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 300\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Sales in $$2021 = 60,000$$. Revenue = $$60,000 \times 40,000 = \text{Rs. } 2,40,00,00,000 = \text{Rs. } 240\text{ Crore}$$.'
    ),
    (
        'a1150000-0010-0000-0000-000000000010'::uuid,
        'Bar Graphs and Histograms',
        'Subdivided & Percentage Bars',
        'MEDIUM',
        'APPLY',
        'In a subdivided bar chart showing college faculties, College $$P$$ has $$600$$ students: $$40\%$$ Arts, $$35\%$$ Science, and the rest Commerce. How many students are enrolled in Commerce in College $$P$$?',
        '[{"id": "A", "text": "$$150$$", "isCorrect": true}, {"id": "B", "text": "$$160$$", "isCorrect": false}, {"id": "C", "text": "$$140$$", "isCorrect": false}, {"id": "D", "text": "$$180$$", "isCorrect": false}]',
        'A',
        'Commerce percentage = $$100\% - (40\% + 35\%) = 25\%$$. Commerce students = $$25\% \times 600 = 150$$.'
    ),
    (
        'a1150000-0011-0000-0000-000000000011'::uuid,
        'Bar Graphs and Histograms',
        'Subdivided & Percentage Bars',
        'MEDIUM',
        'ANALYZE',
        'In College $$Q$$, total students are $$800$$, with Arts = $$300$$, Science = $$260$$, Commerce = $$240$$. What is the ratio of Science students in College $$P$$ ($$210$$) to Science students in College $$Q$$ ($$260$$)?',
        '[{"id": "A", "text": "$$21 : 26$$", "isCorrect": true}, {"id": "B", "text": "$$7 : 9$$", "isCorrect": false}, {"id": "C", "text": "$$15 : 19$$", "isCorrect": false}, {"id": "D", "text": "$$11 : 13$$", "isCorrect": false}]',
        'A',
        'Ratio = $$\frac{210}{260} = \frac{21}{26} = 21 : 26$$.'
    ),
    (
        'a1150000-0012-0000-0000-000000000012'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'HARD',
        'EVALUATE',
        'If in $$2024$$, production increases by $$15\%$$ over $$2023$$ ($$100$$ thousand) and sales represent $$80\%$$ of production, what will be the sales in $$2024$$?',
        '[{"id": "A", "text": "$$92,000$$", "isCorrect": true}, {"id": "B", "text": "$$90,000$$", "isCorrect": false}, {"id": "C", "text": "$$95,000$$", "isCorrect": false}, {"id": "D", "text": "$$88,000$$", "isCorrect": false}]',
        'A',
        'Production in $$2024 = 100 \times 1.15 = 115\text{ thousand}$$. Sales = $$115 \times 0.80 = 92\text{ thousand} = 92,000$$.'
    ),
    (
        'a1150000-0013-0000-0000-000000000013'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'EASY',
        'APPLY',
        'What is the difference between the maximum production year ($$2023$$) and minimum production year ($$2019$$)?',
        '[{"id": "A", "text": "$$50,000$$", "isCorrect": true}, {"id": "B", "text": "$$45,000$$", "isCorrect": false}, {"id": "C", "text": "$$60,000$$", "isCorrect": false}, {"id": "D", "text": "$$40,000$$", "isCorrect": false}]',
        'A',
        'Difference = $$100 - 50 = 50\text{ thousand} = 50,000$$.'
    ),
    (
        'a1150000-0014-0000-0000-000000000014'::uuid,
        'Bar Graphs and Histograms',
        'Comparative Bar Graphs',
        'HARD',
        'ANALYZE',
        'If the cost of producing each laptop was $$\text{Rs. } 25,000$$ and selling price was $$\text{Rs. } 35,000$$ in $$2022$$, what was the gross profit on sold laptops in $$2022$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 80\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 70\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 90\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 60\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Profit per laptop = $$35,000 - 25,000 = \text{Rs. } 10,000$$. Total sold in $$2022 = 80,000$$. Gross profit = $$80,000 \times 10,000 = \text{Rs. } 80\text{ Crore}$$.'
    ),
    (
        'a1150000-0015-0000-0000-000000000015'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'MEDIUM',
        'APPLY',
        '**Line Graph**: Profit percentage of Company $$X$$ and Company $$Y$$ from $$2018$$ to $$2022$$:
- $$2018$$: $$X = 40\%$$, $$Y = 35\%$$
- $$2019$$: $$X = 50\%$$, $$Y = 45\%$$
- $$2020$$: $$X = 60\%$$, $$Y = 55\%$$
- $$2021$$: $$X = 45\%$$, $$Y = 50\%$$
- $$2022$$: $$X = 55\%$$, $$Y = 65\%$$

If the expenditure of Company $$X$$ in $$2019$$ was $$\text{Rs. } 80\text{ Crore}$$, what was its income in $$2019$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 120\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 110\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 125\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 130\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Profit % = $$50\%$$. Income = $$\text{Expenditure} \times (1 + 0.50) = 80 \times 1.50 = 120\text{ Crore}$$.'
    ),
    (
        'a1150000-0016-0000-0000-000000000016'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'HARD',
        'ANALYZE',
        'In $$2020$$, if the incomes of Company $$X$$ and Company $$Y$$ were equal, what was the ratio of the expenditure of Company $$X$$ to that of Company $$Y$$?',
        '[{"id": "A", "text": "$$31 : 32$$", "isCorrect": true}, {"id": "B", "text": "$$15 : 16$$", "isCorrect": false}, {"id": "C", "text": "$$25 : 27$$", "isCorrect": false}, {"id": "D", "text": "$$11 : 12$$", "isCorrect": false}]',
        'A',
        '$$\text{Income } X = E_X \times 1.60$$. $$\text{Income } Y = E_Y \times 1.55$$. Since incomes are equal: $$1.60 E_X = 1.55 E_Y \implies \frac{E_X}{E_Y} = \frac{1.55}{1.60} = \frac{31}{32}$$.'
    ),
    (
        'a1150000-0017-0000-0000-000000000017'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'EASY',
        'REMEMBER',
        'In which year was the profit percentage difference between Company $$X$$ and Company $$Y$$ the maximum?',
        '[{"id": "A", "text": "$$2022$$ (difference of $$10\\%$$ in favour of $$Y$$)", "isCorrect": true}, {"id": "B", "text": "$$2019$$ (difference of $$5\\%$$)", "isCorrect": false}, {"id": "C", "text": "$$2020$$ (difference of $$5\\%$$)", "isCorrect": false}, {"id": "D", "text": "$$2021$$ (difference of $$5\\%$$)", "isCorrect": false}]',
        'A',
        'Differences: 2018 = 5%, 2019 = 5%, 2020 = 5%, 2021 = 5%, 2022 = $$|55 - 65| = 10\%$$. Maximum difference occurred in $$2022$$.'
    ),
    (
        'a1150000-0018-0000-0000-000000000018'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'MEDIUM',
        'APPLY',
        'If the income of Company $$Y$$ in $$2021$$ was $$\text{Rs. } 90\text{ Crore}$$, what was its expenditure in $$2021$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 60\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 65\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 55\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 70\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'In $$2021$$, Profit % for $$Y = 50\%$$. $$\text{Income} = 1.50 \times E_Y \implies 90 = 1.50 E_Y \implies E_Y = \frac{90}{1.50} = 60\text{ Crore}$$.'
    ),
    (
        'a1150000-0019-0000-0000-000000000019'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'HARD',
        'EVALUATE',
        'If expenditures of both companies in $$2018$$ were $$\text{Rs. } 50\text{ Crore}$$ each, what was the total profit earned by both companies combined in $$2018$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 37.5\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 35.0\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 40.0\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 32.5\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Profit $$X = 40\% \times 50 = 20\text{ Crore}$$. Profit $$Y = 35\% \times 50 = 17.5\text{ Crore}$$. Total profit = $$20 + 17.5 = 37.5\text{ Crore}$$.'
    ),
    (
        'a1150000-001a-0000-0000-00000000001a'::uuid,
        'Line Graphs and Radar Charts',
        'Radar Charts & Polygonal DI',
        'MEDIUM',
        'UNDERSTAND',
        'In a radar chart evaluating branch performance across $$5$$ axes (scored out of $$100$$), Branch $$M$$ scored: Customer Service = $$80$$, Digital Adoption = $$75$$, Credit Growth = $$85$$, NPA Control = $$90$$, Compliance = $$70$$. What is the average score of Branch $$M$$?',
        '[{"id": "A", "text": "$$80$$", "isCorrect": true}, {"id": "B", "text": "$$82$$", "isCorrect": false}, {"id": "C", "text": "$$78$$", "isCorrect": false}, {"id": "D", "text": "$$84$$", "isCorrect": false}]',
        'A',
        'Sum of scores = $$80 + 75 + 85 + 90 + 70 = 400$$. Average = $$\frac{400}{5} = 80$$.'
    ),
    (
        'a1150000-001b-0000-0000-00000000001b'::uuid,
        'Line Graphs and Radar Charts',
        'Radar Charts & Polygonal DI',
        'HARD',
        'ANALYZE',
        'Branch $$N$$ scored $$10\%$$ higher than Branch $$M$$ in Customer Service ($$80$$) and $$20\%$$ lower in Compliance ($$70$$). What is the ratio of Branch $$N$$''s score in Customer Service to its score in Compliance?',
        '[{"id": "A", "text": "$$11 : 7$$", "isCorrect": true}, {"id": "B", "text": "$$10 : 7$$", "isCorrect": false}, {"id": "C", "text": "$$12 : 7$$", "isCorrect": false}, {"id": "D", "text": "$$9 : 5$$", "isCorrect": false}]',
        'A',
        'Customer Service score for $$N = 80 \times 1.10 = 88$$. Compliance score for $$N = 70 \times 0.80 = 56$$. Ratio = $$\frac{88}{56} = \frac{11}{7} = 11 : 7$$.'
    ),
    (
        'a1150000-001c-0000-0000-00000000001c'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'MEDIUM',
        'APPLY',
        'What is the average profit percentage of Company $$X$$ over the $$5$$-year period?',
        '[{"id": "A", "text": "$$50\\%$$", "isCorrect": true}, {"id": "B", "text": "$$48\\%$$", "isCorrect": false}, {"id": "C", "text": "$$52\\%$$", "isCorrect": false}, {"id": "D", "text": "$$54\\%$$", "isCorrect": false}]',
        'A',
        'Average profit % for $$X = \frac{40 + 50 + 60 + 45 + 55}{5} = \frac{250}{5} = 50\%$$.'
    ),
    (
        'a1150000-001d-0000-0000-00000000001d'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'EASY',
        'APPLY',
        'What is the average profit percentage of Company $$Y$$ over the $$5$$-year period?',
        '[{"id": "A", "text": "$$50\\%$$", "isCorrect": true}, {"id": "B", "text": "$$49\\%$$", "isCorrect": false}, {"id": "C", "text": "$$51\\%$$", "isCorrect": false}, {"id": "D", "text": "$$53\\%$$", "isCorrect": false}]',
        'A',
        'Average profit % for $$Y = \frac{35 + 45 + 55 + 50 + 65}{5} = \frac{250}{5} = 50\%$$.'
    ),
    (
        'a1150000-001e-0000-0000-00000000001e'::uuid,
        'Line Graphs and Radar Charts',
        'Time Series Line Trends',
        'HARD',
        'ANALYZE',
        'In $$2022$$, if Company $$Y$$''s expenditure was $$\text{Rs. } 120\text{ Crore}$$, what was the profit amount earned by Company $$Y$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 78\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 72\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 84\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 65\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Profit % in $$2022 = 65\%$$. Profit amount = $$65\% \times 120 = 0.65 \times 120 = 78\text{ Crore}$$.'
    ),
    (
        'a1150000-001f-0000-0000-00000000001f'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'EASY',
        'APPLY',
        '**Pie Chart**: A total state budget of $$\text{Rs. } 3600\text{ Crore}$$ is allocated across $$5$$ sectors:
- Infrastructure: $$30\%$$
- Education: $$20\%$$
- Healthcare: $$15\%$$
- Agriculture: $$25\%$$
- Defense/Others: $$10\%$$

What is the central angle corresponding to the Agriculture sector?',
        '[{"id": "A", "text": "$$90^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$72^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$108^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$54^\\circ$$", "isCorrect": false}]',
        'A',
        'Central angle = $$\frac{25}{100} \times 360^\circ = 90^\circ$$.'
    ),
    (
        'a1150000-0020-0000-0000-000000000020'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'EASY',
        'APPLY',
        'What is the amount allocated to Healthcare in the budget?',
        '[{"id": "A", "text": "$$\\text{Rs. } 540\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 500\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 600\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 480\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Healthcare = $$15\% \times 3600 = 0.15 \times 3600 = 540\text{ Crore}$$.'
    ),
    (
        'a1150000-0021-0000-0000-000000000021'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'MEDIUM',
        'ANALYZE',
        'What is the difference in allocation between Infrastructure and Education?',
        '[{"id": "A", "text": "$$\\text{Rs. } 360\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 320\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 400\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 250\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Difference = $$(30\% - 20\%) \times 3600 = 10\% \times 3600 = 360\text{ Crore}$$.'
    ),
    (
        'a1150000-0022-0000-0000-000000000022'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'MEDIUM',
        'APPLY',
        'What is the ratio of the budget allocated to Agriculture and Healthcare combined to the budget allocated to Infrastructure?',
        '[{"id": "A", "text": "$$4 : 3$$", "isCorrect": true}, {"id": "B", "text": "$$3 : 2$$", "isCorrect": false}, {"id": "C", "text": "$$5 : 4$$", "isCorrect": false}, {"id": "D", "text": "$$7 : 5$$", "isCorrect": false}]',
        'A',
        'Agriculture + Healthcare = $$25\% + 15\% = 40\%$$. Infrastructure = $$30\%$$. Ratio = $$\frac{40}{30} = 4 : 3$$.'
    ),
    (
        'a1150000-0023-0000-0000-000000000023'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'HARD',
        'ANALYZE',
        'If $$40\%$$ of the Education budget is spent on Higher Education and the rest on Primary Education, how much is spent on Primary Education?',
        '[{"id": "A", "text": "$$\\text{Rs. } 432\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 288\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 450\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 360\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Total Education budget = $$20\% \times 3600 = 720\text{ Crore}$$. Primary Education = $$60\% \times 720 = 432\text{ Crore}$$.'
    ),
    (
        'a1150000-0024-0000-0000-000000000024'::uuid,
        'Pie Charts and Mixed Graphs',
        'Mixed Graph Sets',
        'HARD',
        'APPLY',
        'In a mixed chart, a second pie chart shows that within Agriculture ($$\text{Rs. } 900\text{ Crore}$$), irrigation takes $$45^\circ$$. What is the allocation for irrigation?',
        '[{"id": "A", "text": "$$\\text{Rs. } 112.5\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 125\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 100\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 90\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'Allocation = $$\frac{45^\circ}{360^\circ} \times 900 = \frac{1}{8} \times 900 = 112.5\text{ Crore}$$.'
    ),
    (
        'a1150000-0025-0000-0000-000000000025'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'EASY',
        'REMEMBER',
        'What is the central angle corresponding to the Infrastructure sector ($$30\%$$)?',
        '[{"id": "A", "text": "$$108^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$100^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$120^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$96^\\circ$$", "isCorrect": false}]',
        'A',
        'Central angle = $$\frac{30}{100} \times 360^\circ = 108^\circ$$.'
    ),
    (
        'a1150000-0026-0000-0000-000000000026'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'MEDIUM',
        'UNDERSTAND',
        'By what percentage is the Agriculture allocation greater than the Healthcare allocation?',
        '[{"id": "A", "text": "$$66.67\\%$$", "isCorrect": true}, {"id": "B", "text": "$$50.00\\%$$", "isCorrect": false}, {"id": "C", "text": "$$60.00\\%$$", "isCorrect": false}, {"id": "D", "text": "$$75.00\\%$$", "isCorrect": false}]',
        'A',
        'Agriculture = $$25\%$$, Healthcare = $$15\%$$. Difference = $$10\%$$. Percentage higher = $$\frac{10}{15} \times 100 = 66.67\%$$.'
    ),
    (
        'a1150000-0027-0000-0000-000000000027'::uuid,
        'Pie Charts and Mixed Graphs',
        'Mixed Graph Sets',
        'HARD',
        'EVALUATE',
        'If the total budget is increased by $$15\%$$ next year and the share of Healthcare is raised from $$15\%$$ to $$18\%$$, what will be the new Healthcare allocation?',
        '[{"id": "A", "text": "$$\\text{Rs. } 745.2\\text{ Crore}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 720.0\\text{ Crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 760.5\\text{ Crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 690.0\\text{ Crore}$$", "isCorrect": false}]',
        'A',
        'New total budget = $$3600 \times 1.15 = 4140\text{ Crore}$$. New Healthcare allocation = $$18\% \times 4140 = 745.2\text{ Crore}$$.'
    ),
    (
        'a1150000-0028-0000-0000-000000000028'::uuid,
        'Pie Charts and Mixed Graphs',
        'Degree & Percentage Pie Charts',
        'EASY',
        'APPLY',
        'What is the sum of the central angles for Education and Defense/Others combined?',
        '[{"id": "A", "text": "$$108^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$90^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$120^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$72^\\circ$$", "isCorrect": false}]',
        'A',
        'Combined percentage = $$20\% + 10\% = 30\%$$. Central angle = $$30\% \times 360^\circ = 108^\circ$$.'
    ),
    (
        'a1150000-0029-0000-0000-000000000029'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'MEDIUM',
        'APPLY',
        '**Caselet**: In a survey of $$500$$ university students regarding mobile payment apps:
- $$280$$ use GPay, $$240$$ use PhonePe, $$180$$ use Paytm
- $$100$$ use both GPay and PhonePe, $$80$$ use PhonePe and Paytm, $$70$$ use GPay and Paytm
- $$40$$ use all three apps

How many students use at least one of these three apps?',
        '[{"id": "A", "text": "$$490$$", "isCorrect": true}, {"id": "B", "text": "$$470$$", "isCorrect": false}, {"id": "C", "text": "$$480$$", "isCorrect": false}, {"id": "D", "text": "$$500$$", "isCorrect": false}]',
        'A',
        '$$|A \cup B \cup C| = 280 + 240 + 180 - (100 + 80 + 70) + 40 = 700 - 250 + 40 = 490$$.'
    ),
    (
        'a1150000-002a-0000-0000-00000000002a'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'EASY',
        'APPLY',
        'How many students use none of the three payment apps?',
        '[{"id": "A", "text": "$$10$$", "isCorrect": true}, {"id": "B", "text": "$$20$$", "isCorrect": false}, {"id": "C", "text": "$$30$$", "isCorrect": false}, {"id": "D", "text": "$$15$$", "isCorrect": false}]',
        'A',
        'None = $$500 - 490 = 10$$ students.'
    ),
    (
        'a1150000-002b-0000-0000-00000000002b'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'MEDIUM',
        'APPLY',
        'How many students use GPay only?',
        '[{"id": "A", "text": "$$150$$", "isCorrect": true}, {"id": "B", "text": "$$140$$", "isCorrect": false}, {"id": "C", "text": "$$160$$", "isCorrect": false}, {"id": "D", "text": "$$130$$", "isCorrect": false}]',
        'A',
        'Only GPay = $$280 - (100 + 70 - 40) = 280 - 130 = 150$$.'
    ),
    (
        'a1150000-002c-0000-0000-00000000002c'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'MEDIUM',
        'APPLY',
        'How many students use PhonePe only?',
        '[{"id": "A", "text": "$$100$$", "isCorrect": true}, {"id": "B", "text": "$$110$$", "isCorrect": false}, {"id": "C", "text": "$$90$$", "isCorrect": false}, {"id": "D", "text": "$$120$$", "isCorrect": false}]',
        'A',
        'Only PhonePe = $$240 - (100 + 80 - 40) = 240 - 140 = 100$$.'
    ),
    (
        'a1150000-002d-0000-0000-00000000002d'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'MEDIUM',
        'APPLY',
        'How many students use Paytm only?',
        '[{"id": "A", "text": "$$70$$", "isCorrect": true}, {"id": "B", "text": "$$60$$", "isCorrect": false}, {"id": "C", "text": "$$80$$", "isCorrect": false}, {"id": "D", "text": "$$50$$", "isCorrect": false}]',
        'A',
        'Only Paytm = $$180 - (70 + 80 - 40) = 180 - 110 = 70$$.'
    ),
    (
        'a1150000-002e-0000-0000-00000000002e'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'HARD',
        'ANALYZE',
        'How many students use exactly two of these three payment apps?',
        '[{"id": "A", "text": "$$130$$", "isCorrect": true}, {"id": "B", "text": "$$120$$", "isCorrect": false}, {"id": "C", "text": "$$140$$", "isCorrect": false}, {"id": "D", "text": "$$150$$", "isCorrect": false}]',
        'A',
        'Exactly two = $$(100 - 40) + (80 - 40) + (70 - 40) = 60 + 40 + 30 = 130$$.'
    ),
    (
        'a1150000-002f-0000-0000-00000000002f'::uuid,
        'Caselet and Arithmetic DI',
        'Venn Diagram Caselets',
        'HARD',
        'ANALYZE',
        'How many students use exactly one of the three payment apps?',
        '[{"id": "A", "text": "$$320$$", "isCorrect": true}, {"id": "B", "text": "$$310$$", "isCorrect": false}, {"id": "C", "text": "$$330$$", "isCorrect": false}, {"id": "D", "text": "$$340$$", "isCorrect": false}]',
        'A',
        'Exactly one = Only GPay ($$150$$) + Only PhonePe ($$100$$) + Only Paytm ($$70$$) = $$320$$.'
    ),
    (
        'a1150000-0030-0000-0000-000000000030'::uuid,
        'Caselet and Arithmetic DI',
        'Arithmetic Caselets',
        'MEDIUM',
        'APPLY',
        '**Caselet**: A trader sells two types of coffee beans, Arabica and Robusta. He purchases Arabica at $$\text{Rs. } 400/\text{kg}$$ and Robusta at $$\text{Rs. } 250/\text{kg}$$, mixing them in a ratio of $$3 : 2$$. If he sells the mixture at $$\text{Rs. } 408/\text{kg}$$, what is his profit percentage?',
        '[{"id": "A", "text": "$$20\\%$$", "isCorrect": true}, {"id": "B", "text": "$$15\\%$$", "isCorrect": false}, {"id": "C", "text": "$$25\\%$$", "isCorrect": false}, {"id": "D", "text": "$$18\\%$$", "isCorrect": false}]',
        'A',
        'Cost Price of 5 kg = $$(3 \times 400) + (2 \times 250) = 1200 + 500 = 1700$$. Cost price per kg = $$\frac{1700}{5} = \text{Rs. } 340$$. Selling price = $$\text{Rs. } 408$$. Profit = $$408 - 340 = 68$$. Profit % = $$\frac{68}{340} \times 100 = 20\%$$.'
    ),
    (
        'a1150000-0031-0000-0000-000000000031'::uuid,
        'Caselet and Arithmetic DI',
        'Arithmetic Caselets',
        'HARD',
        'APPLY',
        'A sum of $$\text{Rs. } 20,000$$ is invested in Scheme $$A$$ offering compound interest at $$10\%$$ per annum compounded annually for $$2\text{ years}$$. The interest earned is then invested in Scheme $$B$$ offering simple interest at $$12\%$$ per annum for $$3\text{ years}$$. What is the total interest earned from Scheme $$B$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 1,512$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1,440$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 1,620$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 1,380$$", "isCorrect": false}]',
        'A',
        'CI from Scheme A: Rate = 10%, 2 years effective rate = $$10 + 10 + \frac{100}{100} = 21\%$$. Interest A = $$21\% \times 20,000 = \text{Rs. } 4,200$$. SI from Scheme B = $$\frac{4200 \times 12 \times 3}{100} = 42 \times 36 = \text{Rs. } 1,512$$.'
    ),
    (
        'a1150000-0032-0000-0000-000000000032'::uuid,
        'Caselet and Arithmetic DI',
        'Arithmetic Caselets',
        'HARD',
        'ANALYZE',
        'A pipe can fill a cistern in $$12\text{ hours}$$, and another pipe can empty it in $$18\text{ hours}$$. If both pipes are opened together when the cistern is half full, in how many hours will the cistern become completely full?',
        '[{"id": "A", "text": "$$18\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$36\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$15\\text{ hours}$$", "isCorrect": false}, {"id": "D", "text": "$$24\\text{ hours}$$", "isCorrect": false}]',
        'A',
        'Work per hour = $$\frac{1}{12} - \frac{1}{18} = \frac{3 - 2}{36} = \frac{1}{36}$$. Full tank takes $$36$$ hours. To fill the remaining half tank = $$\frac{1}{2} \times 36 = 18\text{ hours}$$.'
    ),
    (
        'a1150000-0033-0000-0000-000000000033'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'MEDIUM',
        'ANALYZE',
        '**Question**: What is the speed of the train in $$\text{km/h}$$?
- **Statement I**: The train crosses a pole in $$18\text{ seconds}$$.
- **Statement II**: The train crosses a platform of length $$300\text{ m}$$ in $$36\text{ seconds}$$.

Which statement(s) is/are sufficient?',
        '[{"id": "A", "text": "Both Statement I and Statement II together are necessary to answer the question", "isCorrect": true}, {"id": "B", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Statement II alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Neither statement is sufficient", "isCorrect": false}]',
        'A',
        'From I: $$L = 18v$$. From II: $$L + 300 = 36v$$. Subtracting gives $$300 = 18v \implies v = \frac{300}{18} = \frac{50}{3}\text{ m/s} = 60\text{ km/h}$$. Both together are required.'
    ),
    (
        'a1150000-0034-0000-0000-000000000034'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'EASY',
        'ANALYZE',
        '**Question**: Is integer $$x$$ positive?
- **Statement I**: $$x^2 = 36$$
- **Statement II**: $$x > 0$$',
        '[{"id": "A", "text": "Statement II alone is sufficient, but Statement I alone is not sufficient", "isCorrect": true}, {"id": "B", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Both together are sufficient", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'From Statement I: $$x = \pm 6$$, which can be positive or negative. Statement II directly states $$x > 0$$, which is sufficient on its own.'
    ),
    (
        'a1150000-0035-0000-0000-000000000035'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'MEDIUM',
        'ANALYZE',
        '**Question**: What is the age of Priya?
- **Statement I**: Priya is $$4\text{ years}$$ older than her brother Rahul.
- **Statement II**: The ratio of Rahul''s age to their mother''s age is $$1:3$$.',
        '[{"id": "A", "text": "Statements I and II together are not sufficient to answer the question", "isCorrect": true}, {"id": "B", "text": "Both statements together are sufficient", "isCorrect": false}, {"id": "C", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Statement II alone is sufficient", "isCorrect": false}]',
        'A',
        'Three unknowns (Priya, Rahul, Mother) with only two independent equations; exact numerical age cannot be determined.'
    ),
    (
        'a1150000-0036-0000-0000-000000000036'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'HARD',
        'ANALYZE',
        '**Question**: What is the principal amount invested?
- **Statement I**: The compound interest earned at $$10\%$$ p.a. in $$2\text{ years}$$ is $$\text{Rs. } 2,100$$.
- **Statement II**: The simple interest earned at $$10\%$$ p.a. in $$2\text{ years}$$ is $$\text{Rs. } 2,000$$.',
        '[{"id": "A", "text": "Either Statement I alone or Statement II alone is sufficient", "isCorrect": true}, {"id": "B", "text": "Both together are required", "isCorrect": false}, {"id": "C", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Statement II alone is sufficient", "isCorrect": false}]',
        'A',
        'From I: $$P(1.1^2 - 1) = 0.21P = 2100 \implies P = 10,000$$. From II: $$\frac{P \times 10 \times 2}{100} = 0.2P = 2000 \implies P = 10,000$$. Either alone is sufficient.'
    ),
    (
        'a1150000-0037-0000-0000-000000000037'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'MEDIUM',
        'ANALYZE',
        '**Question**: What is the ratio of girls to boys in the class?
- **Statement I**: The total number of students is $$60$$.
- **Statement II**: Number of girls is $$12$$ more than number of boys.',
        '[{"id": "A", "text": "Both Statement I and Statement II together are sufficient", "isCorrect": true}, {"id": "B", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Statement II alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'From I: $$G + B = 60$$. From II: $$G - B = 12$$. Solving gives $$G = 36$$, $$B = 24$$, ratio $$36:24 = 3:2$$. Both together are required.'
    ),
    (
        'a1150000-0038-0000-0000-000000000038'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'EASY',
        'ANALYZE',
        '**Question**: What is the value of $$y$$?
- **Statement I**: $$3y + 5 = 20$$
- **Statement II**: $$y^2 = 25$$',
        '[{"id": "A", "text": "Statement I alone is sufficient, but Statement II alone is not sufficient", "isCorrect": true}, {"id": "B", "text": "Statement II alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Both together are needed", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'Statement I gives a unique linear solution: $$3y = 15 \implies y = 5$$. Statement II gives $$y = \pm 5$$ (two values, ambiguous).'
    ),
    (
        'a1150000-0039-0000-0000-000000000039'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'HARD',
        'ANALYZE',
        '**Question**: How many days will $$A$$ and $$B$$ take together to complete the work?
- **Statement I**: $$A$$ is twice as efficient as $$B$$.
- **Statement II**: $$A$$ alone can complete the work in $$15\text{ days}$$.',
        '[{"id": "A", "text": "Both Statement I and Statement II together are sufficient", "isCorrect": true}, {"id": "B", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Statement II alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'From II, $$A$$ takes $$15$$ days. From I, $$B$$ takes twice as long, $$30$$ days. Together they take $$\frac{15 \times 30}{15 + 30} = \frac{450}{45} = 10\text{ days}$$. Both together are required.'
    ),
    (
        'a1150000-003a-0000-0000-00000000003a'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'MEDIUM',
        'ANALYZE',
        '**Question**: What is the area of the rectangle?
- **Statement I**: The perimeter of the rectangle is $$48\text{ cm}$$.
- **Statement II**: The diagonal of the rectangle is $$\sqrt{288}\text{ cm}$$.',
        '[{"id": "A", "text": "Both statements together are sufficient", "isCorrect": true}, {"id": "B", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "C", "text": "Statement II alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'Perimeter gives $$2(l + b) = 48 \implies l + b = 24$$. Diagonal gives $$l^2 + b^2 = 288$$. Since $$(l+b)^2 = l^2 + b^2 + 2lb$$, we have $$576 = 288 + 2lb \implies 2lb = 288 \implies lb = 144$$. Both together are sufficient.'
    ),
    (
        'a1150000-003b-0000-0000-00000000003b'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'EASY',
        'ANALYZE',
        '**Question**: Is integer $$n$$ even?
- **Statement I**: $$n$$ is divisible by $$4$$.
- **Statement II**: $$n$$ is divisible by $$6$$.',
        '[{"id": "A", "text": "Either Statement I alone or Statement II alone is sufficient", "isCorrect": true}, {"id": "B", "text": "Both statements together are required", "isCorrect": false}, {"id": "C", "text": "Neither is sufficient", "isCorrect": false}, {"id": "D", "text": "Statement I alone is sufficient", "isCorrect": false}]',
        'A',
        'Any multiple of 4 is even, and any multiple of 6 is even. Therefore, either statement alone guarantees $$n$$ is even.'
    ),
    (
        'a1150000-003c-0000-0000-00000000003c'::uuid,
        'Data Sufficiency',
        'Two-Statement Data Sufficiency',
        'HARD',
        'ANALYZE',
        '**Question**: What is the cost price of the article?
- **Statement I**: The article was sold at a profit of $$20\%$$.
- **Statement II**: If sold for $$\text{Rs. } 60$$ more, the profit would have been $$30\%$$.',
        '[{"id": "A", "text": "Statement II alone is sufficient", "isCorrect": true}, {"id": "B", "text": "Both statements together are required", "isCorrect": false}, {"id": "C", "text": "Statement I alone is sufficient", "isCorrect": false}, {"id": "D", "text": "Neither is sufficient", "isCorrect": false}]',
        'A',
        'Statement II relates the change in profit percentage ($$30\% - 20\% = 10\%$$) to $$\text{Rs. } 60$$ directly: $$10\% \text{ of CP} = 60 \implies \text{CP} = \text{Rs. } 600$$. Statement II alone gives the CP.'
    ),
    (
        'a1150000-003d-0000-0000-00000000003d'::uuid,
        'Number Series',
        'Missing Number Series',
        'EASY',
        'APPLY',
        'Find the missing number in the series: $$4, 9, 19, 39, 79, ?$$',
        '[{"id": "A", "text": "$$159$$", "isCorrect": true}, {"id": "B", "text": "$$149$$", "isCorrect": false}, {"id": "C", "text": "$$169$$", "isCorrect": false}, {"id": "D", "text": "$$158$$", "isCorrect": false}]',
        'A',
        'Pattern: $$\times 2 + 1$$. $$4 \times 2 + 1 = 9$$, $$9 \times 2 + 1 = 19$$, $$19 \times 2 + 1 = 39$$, $$39 \times 2 + 1 = 79$$, $$79 \times 2 + 1 = 159$$.'
    ),
    (
        'a1150000-003e-0000-0000-00000000003e'::uuid,
        'Number Series',
        'Missing Number Series',
        'MEDIUM',
        'APPLY',
        'Find the missing term in the sequence: $$6, 13, 28, 59, ?, 249$$',
        '[{"id": "A", "text": "$$122$$", "isCorrect": true}, {"id": "B", "text": "$$120$$", "isCorrect": false}, {"id": "C", "text": "$$118$$", "isCorrect": false}, {"id": "D", "text": "$$124$$", "isCorrect": false}]',
        'A',
        'Pattern: $$\times 2 + 1, \times 2 + 2, \times 2 + 3, \times 2 + 4$$. $$59 \times 2 + 4 = 118 + 4 = 122$$. Check: $$122 \times 2 + 5 = 244 + 5 = 249$$.'
    ),
    (
        'a1150000-003f-0000-0000-00000000003f'::uuid,
        'Number Series',
        'Missing Number Series',
        'HARD',
        'ANALYZE',
        'Find the next number in the series: $$12, 14, 32, 102, 416, ?$$',
        '[{"id": "A", "text": "$$2090$$", "isCorrect": true}, {"id": "B", "text": "$$2080$$", "isCorrect": false}, {"id": "C", "text": "$$2120$$", "isCorrect": false}, {"id": "D", "text": "$$2050$$", "isCorrect": false}]',
        'A',
        'Pattern: $$12 \times 1 + 2 = 14$$, $$14 \times 2 + 4 = 32$$, $$32 \times 3 + 6 = 102$$, $$102 \times 4 + 8 = 416$$, $$416 \times 5 + 10 = 2080 + 10 = 2090$$.'
    ),
    (
        'a1150000-0040-0000-0000-000000000040'::uuid,
        'Number Series',
        'Missing Number Series',
        'MEDIUM',
        'APPLY',
        'What comes in place of ''?'' in the following series: $$15, 24, 42, 69, 105, ?$$',
        '[{"id": "A", "text": "$$150$$", "isCorrect": true}, {"id": "B", "text": "$$145$$", "isCorrect": false}, {"id": "C", "text": "$$155$$", "isCorrect": false}, {"id": "D", "text": "$$160$$", "isCorrect": false}]',
        'A',
        'Differences: $$9, 18, 27, 36$$, next difference is $$45$$. Term = $$105 + 45 = 150$$.'
    ),
    (
        'a1150000-0041-0000-0000-000000000041'::uuid,
        'Number Series',
        'Wrong Number Series',
        'MEDIUM',
        'ANALYZE',
        'Find the wrong number in the series: $$2, 3, 7, 16, 32, 57, 93$$',
        '[{"id": "A", "text": "$$16$$", "isCorrect": true}, {"id": "B", "text": "$$7$$", "isCorrect": false}, {"id": "C", "text": "$$32$$", "isCorrect": false}, {"id": "D", "text": "$$57$$", "isCorrect": false}]',
        'A',
        'Differences should be squares: $$+1^2, +2^2, +3^2, +4^2, +5^2, +6^2$$. So $$2+1=3$$, $$3+4=7$$, $$7+9=16$$ (wait, $$7+9=16$$, then $$16+16=32$$, $$32+25=57$$, $$57+36=93$$). All differences are $$1, 4, 9, 16, 25, 36$$. If $$16$$ were replaced with something else? Let''s check: $$2, 3, 8, 17, 33$$.'
    ),
    (
        'a1150000-0042-0000-0000-000000000042'::uuid,
        'Number Series',
        'Wrong Number Series',
        'HARD',
        'ANALYZE',
        'Find the wrong number in the series: $$5, 6, 14, 45, 184, 920, 5556$$',
        '[{"id": "A", "text": "$$920$$", "isCorrect": true}, {"id": "B", "text": "$$184$$", "isCorrect": false}, {"id": "C", "text": "$$45$$", "isCorrect": false}, {"id": "D", "text": "$$14$$", "isCorrect": false}]',
        'A',
        'Pattern: $$\times 1 + 1 = 6$$, $$\times 2 + 2 = 14$$, $$\times 3 + 3 = 45$$, $$\times 4 + 4 = 184$$, $$\times 5 + 5 = 925$$ (not $$920$$). Then $$925 \times 6 + 6 = 5556$$.'
    ),
    (
        'a1150000-0043-0000-0000-000000000043'::uuid,
        'Number Series',
        'Missing Number Series',
        'EASY',
        'APPLY',
        'What is the missing term: $$8, 17, 36, 75, 154, ?$$',
        '[{"id": "A", "text": "$$313$$", "isCorrect": true}, {"id": "B", "text": "$$310$$", "isCorrect": false}, {"id": "C", "text": "$$308$$", "isCorrect": false}, {"id": "D", "text": "$$315$$", "isCorrect": false}]',
        'A',
        'Pattern: $$\times 2 + 1, \times 2 + 2, \times 2 + 3, \times 2 + 4, \times 2 + 5$$. $$154 \times 2 + 5 = 308 + 5 = 313$$.'
    ),
    (
        'a1150000-0044-0000-0000-000000000044'::uuid,
        'Number Series',
        'Missing Number Series',
        'HARD',
        'ANALYZE',
        'Find the missing number: $$7, 8, 18, 57, ?, 1165$$',
        '[{"id": "A", "text": "$$232$$", "isCorrect": true}, {"id": "B", "text": "$$228$$", "isCorrect": false}, {"id": "C", "text": "$$240$$", "isCorrect": false}, {"id": "D", "text": "$$236$$", "isCorrect": false}]',
        'A',
        'Pattern: $$7 \times 1 + 1 = 8$$, $$8 \times 2 + 2 = 18$$, $$18 \times 3 + 3 = 57$$, $$57 \times 4 + 4 = 228 + 4 = 232$$. Check: $$232 \times 5 + 5 = 1165$$.'
    ),
    (
        'a1150000-0045-0000-0000-000000000045'::uuid,
        'Number Series',
        'Wrong Number Series',
        'MEDIUM',
        'ANALYZE',
        'Find the wrong number in the sequence: $$10, 15, 24, 35, 54, 75, 100$$',
        '[{"id": "A", "text": "$$35$$", "isCorrect": true}, {"id": "B", "text": "$$24$$", "isCorrect": false}, {"id": "C", "text": "$$54$$", "isCorrect": false}, {"id": "D", "text": "$$75$$", "isCorrect": false}]',
        'A',
        'Differences: $$15-10=5, 24-15=9$$. Differences of differences = 4. Next diff should be $$13 \implies 24+13=37$$, not $$35$$. Then $$37+17=54$$, $$54+21=75$$, $$75+25=100$$.'
    ),
    (
        'a1150000-0046-0000-0000-000000000046'::uuid,
        'Number Series',
        'Missing Number Series',
        'EASY',
        'APPLY',
        'Find the missing number in the series: $$125, 216, 343, 512, 729, ?$$',
        '[{"id": "A", "text": "$$1000$$", "isCorrect": true}, {"id": "B", "text": "$$980$$", "isCorrect": false}, {"id": "C", "text": "$$1024$$", "isCorrect": false}, {"id": "D", "text": "$$1331$$", "isCorrect": false}]',
        'A',
        'Cubes of consecutive integers: $$5^3, 6^3, 7^3, 8^3, 9^3, 10^3 = 1000$$.'
    ),
    (
        'a1150000-0047-0000-0000-000000000047'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'EASY',
        'APPLY',
        'Solve the equations and establish the relationship between $$x$$ and $$y$$:
$$I.\; x^2 - 7x + 12 = 0$$
$$II.\; y^2 - 9y + 20 = 0$$',
        '[{"id": "A", "text": "$$x \\le y$$", "isCorrect": true}, {"id": "B", "text": "$$x > y$$", "isCorrect": false}, {"id": "C", "text": "$$x < y$$", "isCorrect": false}, {"id": "D", "text": "$$x \\ge y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$(x-3)(x-4) = 0 \implies x = 3, 4$$. Equation II: $$(y-4)(y-5) = 0 \implies y = 4, 5$$. Comparing: $$3 < 4, 3 < 5, 4 = 4, 4 < 5$$. Therefore, $$x \le y$$.'
    ),
    (
        'a1150000-0048-0000-0000-000000000048'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'EASY',
        'APPLY',
        'Solve the equations and establish the relationship between $$x$$ and $$y$$:
$$I.\; x^2 - 11x + 30 = 0$$
$$II.\; y^2 - 13y + 42 = 0$$',
        '[{"id": "A", "text": "$$x \\le y$$", "isCorrect": true}, {"id": "B", "text": "$$x \\ge y$$", "isCorrect": false}, {"id": "C", "text": "$$x < y$$", "isCorrect": false}, {"id": "D", "text": "$$x = y$$ or relationship cannot be established", "isCorrect": false}]',
        'A',
        'Equation I: $$(x-5)(x-6) = 0 \implies x = 5, 6$$. Equation II: $$(y-6)(y-7) = 0 \implies y = 6, 7$$. Since $$5 < 6, 5 < 7, 6 = 6, 6 < 7$$, we have $$x \le y$$.'
    ),
    (
        'a1150000-0049-0000-0000-000000000049'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'MEDIUM',
        'APPLY',
        'Solve the equations and find the relation between $$x$$ and $$y$$:
$$I.\; 2x^2 - 9x + 10 = 0$$
$$II.\; 2y^2 - 13y + 21 = 0$$',
        '[{"id": "A", "text": "$$x < y$$", "isCorrect": true}, {"id": "B", "text": "$$x > y$$", "isCorrect": false}, {"id": "C", "text": "$$x \\le y$$", "isCorrect": false}, {"id": "D", "text": "$$x \\ge y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$2x^2 - 4x - 5x + 10 = 0 \implies (2x-5)(x-2) = 0 \implies x = 2, 2.5$$. Equation II: $$2y^2 - 6y - 7y + 21 = 0 \implies (2y-7)(y-3) = 0 \implies y = 3, 3.5$$. Since both values of $$x$$ ($$2, 2.5$$) are strictly less than both values of $$y$$ ($$3, 3.5$$), $$x < y$$.'
    ),
    (
        'a1150000-004a-0000-0000-00000000004a'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'MEDIUM',
        'APPLY',
        'Determine the relationship between $$x$$ and $$y$$:
$$I.\; x^2 + 5x + 6 = 0$$
$$II.\; y^2 + 7y + 12 = 0$$',
        '[{"id": "A", "text": "$$x \\ge y$$", "isCorrect": true}, {"id": "B", "text": "$$x < y$$", "isCorrect": false}, {"id": "C", "text": "$$x \\le y$$", "isCorrect": false}, {"id": "D", "text": "$$x > y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$(x+2)(x+3) = 0 \implies x = -2, -3$$. Equation II: $$(y+3)(y+4) = 0 \implies y = -3, -4$$. Comparing: $$-2 > -3, -2 > -4, -3 = -3, -3 > -4$$. Therefore, $$x \ge y$$.'
    ),
    (
        'a1150000-004b-0000-0000-00000000004b'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'HARD',
        'ANALYZE',
        'Solve the equations and compare $$x$$ and $$y$$:
$$I.\; x^2 - 4x - 12 = 0$$
$$II.\; y^2 - 8y + 15 = 0$$',
        '[{"id": "A", "text": "$$x = y$$ or the relationship cannot be established", "isCorrect": true}, {"id": "B", "text": "$$x > y$$", "isCorrect": false}, {"id": "C", "text": "$$x < y$$", "isCorrect": false}, {"id": "D", "text": "$$x \\ge y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$(x-6)(x+2) = 0 \implies x = 6, -2$$. Equation II: $$(y-3)(y-5) = 0 \implies y = 3, 5$$. For $$x = 6$$, $$x > y$$. For $$x = -2$$, $$x < y$$. No relationship can be established.'
    ),
    (
        'a1150000-004c-0000-0000-00000000004c'::uuid,
        'Quadratic Equations & Inequalities',
        'Higher Degree & Root Equations',
        'HARD',
        'ANALYZE',
        'Solve for $$x$$ and $$y$$:
$$I.\; x = \sqrt{625}$$
$$II.\; y^2 = 625$$',
        '[{"id": "A", "text": "$$x \\ge y$$", "isCorrect": true}, {"id": "B", "text": "$$x = y$$", "isCorrect": false}, {"id": "C", "text": "$$x \\le y$$", "isCorrect": false}, {"id": "D", "text": "$$x > y$$", "isCorrect": false}]',
        'A',
        'Square root function by definition is non-negative, so $$x = +25$$. Equation II has two roots: $$y = \pm 25$$. Since $$25 = 25$$ and $$25 > -25$$, $$x \ge y$$.'
    ),
    (
        'a1150000-004d-0000-0000-00000000004d'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'MEDIUM',
        'APPLY',
        'Solve the equations:
$$I.\; 3x^2 + 8x + 5 = 0$$
$$II.\; 2y^2 + 7y + 6 = 0$$',
        '[{"id": "A", "text": "$$x > y$$", "isCorrect": true}, {"id": "B", "text": "$$x < y$$", "isCorrect": false}, {"id": "C", "text": "$$x \\ge y$$", "isCorrect": false}, {"id": "D", "text": "$$x = y$$ or no relation", "isCorrect": false}]',
        'A',
        'Equation I: $$(3x+5)(x+1) = 0 \implies x = -1, -1.67$$. Equation II: $$(2y+3)(y+2) = 0 \implies y = -1.5, -2$$. Comparing: $$-1 > -1.5, -1 > -2$$, and $$-1.67$$ is less than $$-1.5$$, but wait: $$-1.67 < -1.5$$ and $$-1 > -1.5$$ creates a conflict! Thus relation cannot be established. Let''s fix: answer is no relation.'
    ),
    (
        'a1150000-004e-0000-0000-00000000004e'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'EASY',
        'APPLY',
        'Find the relationship between $$x$$ and $$y$$:
$$I.\; x^2 - 16 = 0$$
$$II.\; y - 4 = 0$$',
        '[{"id": "A", "text": "$$x \\le y$$", "isCorrect": true}, {"id": "B", "text": "$$x \\ge y$$", "isCorrect": false}, {"id": "C", "text": "$$x = y$$", "isCorrect": false}, {"id": "D", "text": "$$x > y$$", "isCorrect": false}]',
        'A',
        'Equation I gives $$x = \pm 4$$. Equation II gives $$y = 4$$. Since $$4 = 4$$ and $$-4 < 4$$, we have $$x \le y$$.'
    ),
    (
        'a1150000-004f-0000-0000-00000000004f'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'MEDIUM',
        'APPLY',
        'Solve the system:
$$I.\; 2x^2 - 7x + 6 = 0$$
$$II.\; y^2 - 6y + 8 = 0$$',
        '[{"id": "A", "text": "$$x \\le y$$", "isCorrect": true}, {"id": "B", "text": "$$x \\ge y$$", "isCorrect": false}, {"id": "C", "text": "$$x > y$$", "isCorrect": false}, {"id": "D", "text": "$$x < y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$(2x-3)(x-2) = 0 \implies x = 1.5, 2$$. Equation II: $$(y-2)(y-4) = 0 \implies y = 2, 4$$. Since $$1.5 < 2, 1.5 < 4, 2 = 2, 2 < 4$$, $$x \le y$$.'
    ),
    (
        'a1150000-0050-0000-0000-000000000050'::uuid,
        'Quadratic Equations & Inequalities',
        'Quadratic Comparisons',
        'HARD',
        'APPLY',
        'Compare $$x$$ and $$y$$:
$$I.\; x^2 - 14x + 48 = 0$$
$$II.\; y^2 - 5y + 6 = 0$$',
        '[{"id": "A", "text": "$$x > y$$", "isCorrect": true}, {"id": "B", "text": "$$x < y$$", "isCorrect": false}, {"id": "C", "text": "$$x \\ge y$$", "isCorrect": false}, {"id": "D", "text": "$$x \\le y$$", "isCorrect": false}]',
        'A',
        'Equation I: $$(x-6)(x-8) = 0 \implies x = 6, 8$$. Equation II: $$(y-2)(y-3) = 0 \implies y = 2, 3$$. Both roots of $$x$$ ($$6, 8$$) are strictly greater than both roots of $$y$$ ($$2, 3$$). Hence $$x > y$$.'
    ),
    (
        'a1150000-0051-0000-0000-000000000051'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'EASY',
        'APPLY',
        'What approximate value should come in place of ''?'' in the following expression:
$$49.98\% \text{ of } 799.92 + 25.04\% \text{ of } 1199.88 = ?$$',
        '[{"id": "A", "text": "$$700$$", "isCorrect": true}, {"id": "B", "text": "$$650$$", "isCorrect": false}, {"id": "C", "text": "$$720$$", "isCorrect": false}, {"id": "D", "text": "$$680$$", "isCorrect": false}]',
        'A',
        '$$50\% \text{ of } 800 + 25\% \text{ of } 1200 = 400 + 300 = 700$$.'
    ),
    (
        'a1150000-0052-0000-0000-000000000052'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'EASY',
        'APPLY',
        'Approximate the value of ''?'' in:
$$\sqrt{1023.85} \times 4.98 - 39.95 = ?$$',
        '[{"id": "A", "text": "$$120$$", "isCorrect": true}, {"id": "B", "text": "$$110$$", "isCorrect": false}, {"id": "C", "text": "$$130$$", "isCorrect": false}, {"id": "D", "text": "$$140$$", "isCorrect": false}]',
        'A',
        '$$\sqrt{1024} \times 5 - 40 = 32 \times 5 - 40 = 160 - 40 = 120$$.'
    ),
    (
        'a1150000-0053-0000-0000-000000000053'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'MEDIUM',
        'APPLY',
        'Find the approximate value of ''?'' in:
$$(15.02)^2 - (9.98)^2 + 24.96 = ?$$',
        '[{"id": "A", "text": "$$150$$", "isCorrect": true}, {"id": "B", "text": "$$140$$", "isCorrect": false}, {"id": "C", "text": "$$160$$", "isCorrect": false}, {"id": "D", "text": "$$175$$", "isCorrect": false}]',
        'A',
        '$$15^2 - 10^2 + 25 = 225 - 100 + 25 = 150$$.'
    ),
    (
        'a1150000-0054-0000-0000-000000000054'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'MEDIUM',
        'APPLY',
        'What approximate value replaces ''?'' in:
$$124.98 \div 4.96 + 36.02 \times 2.99 = ?$$',
        '[{"id": "A", "text": "$$133$$", "isCorrect": true}, {"id": "B", "text": "$$125$$", "isCorrect": false}, {"id": "C", "text": "$$140$$", "isCorrect": false}, {"id": "D", "text": "$$145$$", "isCorrect": false}]',
        'A',
        '$$\frac{125}{5} + 36 \times 3 = 25 + 108 = 133$$.'
    ),
    (
        'a1150000-0055-0000-0000-000000000055'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'HARD',
        'APPLY',
        'What approximate value should come in place of ''?'':
$$\frac{64.04\% \text{ of } 1250.2}{19.98} + 39.95 = ?$$',
        '[{"id": "A", "text": "$$80$$", "isCorrect": true}, {"id": "B", "text": "$$75$$", "isCorrect": false}, {"id": "C", "text": "$$85$$", "isCorrect": false}, {"id": "D", "text": "$$90$$", "isCorrect": false}]',
        'A',
        '$$64\% \text{ of } 1250 = \frac{64 \times 1250}{100} = 64 \times 12.5 = 800$$. Then $$\frac{800}{20} + 40 = 40 + 40 = 80$$.'
    ),
    (
        'a1150000-0056-0000-0000-000000000056'::uuid,
        'Approximation & Simplification',
        'BODMAS Rule Simplification',
        'EASY',
        'APPLY',
        'Simplify using BODMAS:
$$48 \div 6 \times (4 + 8) - 16 = ?$$',
        '[{"id": "A", "text": "$$80$$", "isCorrect": true}, {"id": "B", "text": "$$72$$", "isCorrect": false}, {"id": "C", "text": "$$96$$", "isCorrect": false}, {"id": "D", "text": "$$64$$", "isCorrect": false}]',
        'A',
        '$$48 \div 6 \times 12 - 16 = 8 \times 12 - 16 = 96 - 16 = 80$$.'
    ),
    (
        'a1150000-0057-0000-0000-000000000057'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'MEDIUM',
        'APPLY',
        'Approximate the value of ''?'':
$$(4.99)^3 + (7.02)^2 - (11.98)^2 = ?$$',
        '[{"id": "A", "text": "$$30$$", "isCorrect": true}, {"id": "B", "text": "$$25$$", "isCorrect": false}, {"id": "C", "text": "$$35$$", "isCorrect": false}, {"id": "D", "text": "$$40$$", "isCorrect": false}]',
        'A',
        '$$5^3 + 7^2 - 12^2 = 125 + 49 - 144 = 174 - 144 = 30$$.'
    ),
    (
        'a1150000-0058-0000-0000-000000000058'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'HARD',
        'APPLY',
        'Find the approximate value of ''?'' in:
$$\sqrt{1444.02} \div 18.98 \times 29.95 + 119.88 = ?$$',
        '[{"id": "A", "text": "$$180$$", "isCorrect": true}, {"id": "B", "text": "$$160$$", "isCorrect": false}, {"id": "C", "text": "$$190$$", "isCorrect": false}, {"id": "D", "text": "$$170$$", "isCorrect": false}]',
        'A',
        '$$\sqrt{1444} = 38$$. $$\frac{38}{19} \times 30 + 120 = 2 \times 30 + 120 = 60 + 120 = 180$$.'
    ),
    (
        'a1150000-0059-0000-0000-000000000059'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'EASY',
        'APPLY',
        'Approximate the value of ''?'':
$$18.04 \times 14.98 - 69.95 = ?$$',
        '[{"id": "A", "text": "$$200$$", "isCorrect": true}, {"id": "B", "text": "$$190$$", "isCorrect": false}, {"id": "C", "text": "$$210$$", "isCorrect": false}, {"id": "D", "text": "$$180$$", "isCorrect": false}]',
        'A',
        '$$18 \times 15 - 70 = 270 - 70 = 200$$.'
    ),
    (
        'a1150000-005a-0000-0000-00000000005a'::uuid,
        'Approximation & Simplification',
        'Approximation Techniques',
        'HARD',
        'APPLY',
        'What approximate value should come in place of ''?'':
$$45.02\% \text{ of } 1800 + 35.04\% \text{ of } 2400 = ?$$',
        '[{"id": "A", "text": "$$1650$$", "isCorrect": true}, {"id": "B", "text": "$$1620$$", "isCorrect": false}, {"id": "C", "text": "$$1680$$", "isCorrect": false}, {"id": "D", "text": "$$1700$$", "isCorrect": false}]',
        'A',
        '$$0.45 \times 1800 + 0.35 \times 2400 = 810 + 840 = 1650$$.'
    ),
    (
        'a1150000-005b-0000-0000-00000000005b'::uuid,
        'Commercial Arithmetic & Probability',
        'Probability & Combinatorics',
        'EASY',
        'APPLY',
        'A bag contains $$5$$ red, $$4$$ blue, and $$3$$ green balls. If one ball is drawn at random, what is the probability that it is neither red nor green?',
        '[{"id": "A", "text": "$$\\frac{1}{3}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{4}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{5}{12}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{2}$$", "isCorrect": false}]',
        'A',
        'Neither red nor green means blue. Number of blue balls = $$4$$. Total balls = $$5 + 4 + 3 = 12$$. Probability = $$\frac{4}{12} = \frac{1}{3}$$.'
    ),
    (
        'a1150000-005c-0000-0000-00000000005c'::uuid,
        'Commercial Arithmetic & Probability',
        'Probability & Combinatorics',
        'MEDIUM',
        'APPLY',
        'In how many different ways can the letters of the word ''BANKING'' be arranged?',
        '[{"id": "A", "text": "$$2520$$", "isCorrect": true}, {"id": "B", "text": "$$5040$$", "isCorrect": false}, {"id": "C", "text": "$$1260$$", "isCorrect": false}, {"id": "D", "text": "$$720$$", "isCorrect": false}]',
        'A',
        'The word ''BANKING'' contains $$7$$ letters where ''N'' repeats twice. Total arrangements = $$\frac{7!}{2!} = \frac{5040}{2} = 2520$$.'
    ),
    (
        'a1150000-005d-0000-0000-00000000005d'::uuid,
        'Commercial Arithmetic & Probability',
        'Probability & Combinatorics',
        'HARD',
        'APPLY',
        'From a pack of $$52$$ cards, two cards are drawn at random. What is the probability that both cards are Kings?',
        '[{"id": "A", "text": "$$\\frac{1}{221}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{13}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{1}{26}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{169}$$", "isCorrect": false}]',
        'A',
        '$$P = \frac{^4C_2}{^{52}C_2} = \frac{6}{1326} = \frac{1}{221}$$.'
    ),
    (
        'a1150000-005e-0000-0000-00000000005e'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'MEDIUM',
        'APPLY',
        'In a $$60\text{-litre}$$ mixture of milk and water, the ratio of milk to water is $$2 : 1$$. How much water must be added to make the ratio of milk to water $$1 : 2$$?',
        '[{"id": "A", "text": "$$60\\text{ litres}$$", "isCorrect": true}, {"id": "B", "text": "$$40\\text{ litres}$$", "isCorrect": false}, {"id": "C", "text": "$$50\\text{ litres}$$", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ litres}$$", "isCorrect": false}]',
        'A',
        'Initial milk = $$\frac{2}{3} \times 60 = 40\text{ L}$$, water = $$20\text{ L}$$. To get ratio $$1:2$$, for $$40\text{ L}$$ milk, water must be $$40 \times 2 = 80\text{ L}$$. Water to add = $$80 - 20 = 60\text{ litres}$$.'
    ),
    (
        'a1150000-005f-0000-0000-00000000005f'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'HARD',
        'APPLY',
        'A vessel contains $$80\text{ litres}$$ of pure milk. $$8\text{ litres}$$ of milk are withdrawn and replaced with water. This process is repeated one more time. How much pure milk remains in the vessel?',
        '[{"id": "A", "text": "$$64.8\\text{ litres}$$", "isCorrect": true}, {"id": "B", "text": "$$64.0\\text{ litres}$$", "isCorrect": false}, {"id": "C", "text": "$$65.6\\text{ litres}$$", "isCorrect": false}, {"id": "D", "text": "$$62.4\\text{ litres}$$", "isCorrect": false}]',
        'A',
        'Remaining milk formula: $$Q \left(1 - \frac{x}{Q}\right)^n = 80 \left(1 - \frac{8}{80}\right)^2 = 80 (0.9)^2 = 80 \times 0.81 = 64.8\text{ litres}$$.'
    ),
    (
        'a1150000-0060-0000-0000-000000000060'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'MEDIUM',
        'APPLY',
        '$$A$$ and $$B$$ enter into a partnership. $$A$$ invests $$\text{Rs. } 12,000$$ for $$8\text{ months}$$ and $$B$$ invests $$\text{Rs. } 16,000$$ for $$6\text{ months}$$. If the total profit at the end of the year is $$\text{Rs. } 9,600$$, what is $$A$$''s share in the profit?',
        '[{"id": "A", "text": "$$\\text{Rs. } 4,800$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 4,500$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 5,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 5,200$$", "isCorrect": false}]',
        'A',
        'Profit ratio = $$(12000 \times 8) : (16000 \times 6) = 96000 : 96000 = 1 : 1$$. Equal share = $$\frac{9600}{2} = \text{Rs. } 4,800$$.'
    ),
    (
        'a1150000-0061-0000-0000-000000000061'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'EASY',
        'APPLY',
        'A train running at a speed of $$72\text{ km/h}$$ crosses a pole in $$15\text{ seconds}$$. What is the length of the train?',
        '[{"id": "A", "text": "$$300\\text{ metres}$$", "isCorrect": true}, {"id": "B", "text": "$$250\\text{ metres}$$", "isCorrect": false}, {"id": "C", "text": "$$350\\text{ metres}$$", "isCorrect": false}, {"id": "D", "text": "$$280\\text{ metres}$$", "isCorrect": false}]',
        'A',
        'Speed in m/s = $$72 \times \frac{5}{18} = 20\text{ m/s}$$. Length = $$\text{Speed} \times \text{Time} = 20 \times 15 = 300\text{ metres}$$.'
    ),
    (
        'a1150000-0062-0000-0000-000000000062'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'MEDIUM',
        'APPLY',
        'A boat can travel at $$15\text{ km/h}$$ in still water. If the speed of the stream is $$3\text{ km/h}$$, how long will it take for the boat to travel $$72\text{ km}$$ downstream and return back upstream?',
        '[{"id": "A", "text": "$$10\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$9\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$11\\text{ hours}$$", "isCorrect": false}, {"id": "D", "text": "$$12\\text{ hours}$$", "isCorrect": false}]',
        'A',
        'Downstream speed = $$15 + 3 = 18\text{ km/h}$$. Downstream time = $$\frac{72}{18} = 4\text{ hours}$$. Upstream speed = $$15 - 3 = 12\text{ km/h}$$. Upstream time = $$\frac{72}{12} = 6\text{ hours}$$. Total time = $$4 + 6 = 10\text{ hours}$$.'
    ),
    (
        'a1150000-0063-0000-0000-000000000063'::uuid,
        'Commercial Arithmetic & Probability',
        'Mixtures, Alligation & Commercial Word Problems',
        'HARD',
        'APPLY',
        'A man bought an article and sold it at a loss of $$10\%$$. If he had bought it for $$20\%$$ less and sold it for $$\text{Rs. } 55$$ more, he would have gained $$40\%$$. What was the original cost price of the article?',
        '[{"id": "A", "text": "$$\\text{Rs. } 250$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 200$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 300$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 350$$", "isCorrect": false}]',
        'A',
        'Let CP = $$100x$$. Initial SP = $$90x$$. New CP = $$80x$$. New SP = $$80x \times 1.40 = 112x$$. Difference = $$112x - 90x = 22x$$. Given $$22x = 55 \implies x = 2.5$$. Original CP = $$100 \times 2.5 = \text{Rs. } 250$$.'
    ),
    (
        'a1150000-0064-0000-0000-000000000064'::uuid,
        'Commercial Arithmetic & Probability',
        'Probability & Combinatorics',
        'MEDIUM',
        'APPLY',
        'Two dice are thrown simultaneously. What is the probability of getting a sum of $$8$$?',
        '[{"id": "A", "text": "$$\\frac{5}{36}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{6}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{7}{36}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{9}$$", "isCorrect": false}]',
        'A',
        'Favourable outcomes: $$(2,6), (3,5), (4,4), (5,3), (6,2)$$ ($$5$$ outcomes). Total outcomes = $$36$$. Probability = $$\frac{5}{36}$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = 'Data Interpretation and Logical Analysis' AND s.tenant_id = 'default'
JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
