-- SPDX-License-Identifier: AGPL-3.0-only
-- Flyway Migration: V2_20
-- Seed Data: SBI PO / Bank PO Banking & Financial Awareness (100 Questions)

SET search_path TO question_service, public;

-- Step 1: Ensure Subject, Topics, and Subtopics exist
DO $$
DECLARE
    v_tenant_id VARCHAR := 'default';
    v_subj_id UUID;
    v_top_id UUID;
BEGIN
    SELECT id INTO v_subj_id FROM question_service.subject WHERE name = 'Banking and Financial Awareness' AND tenant_id = v_tenant_id LIMIT 1;
    IF v_subj_id IS NULL THEN
        INSERT INTO question_service.subject (tenant_id, name, code, description)
        VALUES (v_tenant_id, 'Banking and Financial Awareness', UPPER(SUBSTRING('Banking and Financial Awareness', 1, 6)), 'Banking and Financial Awareness for Bank PO Examinations')
        RETURNING id INTO v_subj_id;
    END IF;

    -- Topic: Indian Banking System & RBI Functions
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Indian Banking System & RBI Functions' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Indian Banking System & RBI Functions', 'Structure of Indian commercial banks, RBI origin, Preamble, functions, and currency management')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'RBI Constitution & Currency Management', 'Reserve Bank of India Act 1934, clean note policy, currency chests')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Commercial Banks & Types', 'Public sector banks, private banks, foreign banks, mergers, and consolidation')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Monetary Policy & Inflation Dynamics
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Monetary Policy & Inflation Dynamics' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Monetary Policy & Inflation Dynamics', 'Monetary Policy Committee, quantitative and qualitative monetary instruments, CPI and WPI')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Policy Rates & Liquidity Ratios', 'Repo rate, reverse repo rate, Standing Deposit Facility (SDF), Marginal Standing Facility (MSF), CRR, SLR')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Monetary Policy Framework', 'Flexible Inflation Targeting, Section 45ZB of RBI Act, headline inflation, core inflation')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: NPA Management & Resolution Framework
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'NPA Management & Resolution Framework' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'NPA Management & Resolution Framework', 'Asset classification, prudential provisioning, IBC 2016, SARFAESI, and NARCL')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Asset Classification & Provisioning', 'Standard, Sub-standard, Doubtful, Loss assets, SMA-0, SMA-1, SMA-2 categories')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Insolvency & Recovery Mechanisms', 'SARFAESI Act 2002, Insolvency and Bankruptcy Code 2016, NCLT, Bad Bank (NARCL-IDRCL)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Digital Banking & Payment Systems
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Digital Banking & Payment Systems' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Digital Banking & Payment Systems', 'Payment and settlement infrastructure, NPCI, digital currencies, and payment security')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Retail & Wholesale Payment Systems', 'NEFT, RTGS, IMPS, Unified Payments Interface (UPI), NACH, Cheque Truncation System (CTS)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Emerging Digital Payment Ecosystems', 'Central Bank Digital Currency (CBDC / e-Rupee), Tokenisation, Prepaid Payment Instruments (PPIs)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Money Market & Capital Market Instruments
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Money Market & Capital Market Instruments' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Money Market & Capital Market Instruments', 'Short-term and long-term financial instruments, trading platforms, and debt markets')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Money Market Instruments', 'Call Money, Notice Money, Term Money, Treasury Bills, Commercial Paper, Certificates of Deposit')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Capital Markets & Bond Dynamics', 'Government Securities (G-Secs), Sovereign Gold Bonds, yield curve, zero-coupon bonds')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Government Financial Schemes & Inclusions
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Government Financial Schemes & Inclusions' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Government Financial Schemes & Inclusions', 'Flagship financial inclusion programs, social security initiatives, and micro-credit')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Financial Inclusion Schemes', 'Pradhan Mantri Jan Dhan Yojana (PMJDY), overdraft facilities, RuPay debit cards')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Social Security & Microfinance Schemes', 'PMJJBY, PMSBY, Atal Pension Yojana (APY), PM MUDRA Yojana, PM SVANidhi')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Priority Sector Lending & Agriculture Credit
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Priority Sector Lending & Agriculture Credit' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Priority Sector Lending & Agriculture Credit', 'Mandatory sectoral targets, rural development funding, and sub-target classifications')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'PSL Targets & Classification', 'Overall PSL target (40% for domestic banks, 75% for RRBs/SFBs), Agriculture, MSME, Export Credit, Education')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Rural Infrastructure & Shortfall Mechanics', 'Rural Infrastructure Development Fund (RIDF), Priority Sector Lending Certificates (PSLCs)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Negotiable Instruments & Banking Regulations
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Negotiable Instruments & Banking Regulations' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Negotiable Instruments & Banking Regulations', 'Statutory provisions governing cheques, bills of exchange, crossing, and consumer protection')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Negotiable Instruments Act 1881', 'Promissory notes, bills of exchange, cheques, Section 138 dishonour, crossing types, endorsements')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Banking Ombudsman & Customer Rights', 'Integrated Ombudsman Scheme 2021, Banking Regulation Act 1949, KYC/AML norms')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Basel III Norms & Capital Adequacy
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Basel III Norms & Capital Adequacy' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Basel III Norms & Capital Adequacy', 'Global banking supervisory standards, capital adequacy, risk weighting, and buffers')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Capital Adequacy & Tier Structure', 'Capital to Risk-Weighted Assets Ratio (CRAR), Common Equity Tier 1 (CET1), Additional Tier 1, Tier 2 capital')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Prudential Buffers & Liquidity Norms', 'Capital Conservation Buffer (CCB), Liquidity Coverage Ratio (LCR), Net Stable Funding Ratio (NSFR)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    -- Topic: Development Financial Institutions & Global Bodies
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Development Financial Institutions & Global Bodies' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Development Financial Institutions & Global Bodies', 'Specialized developmental institutions, export-import finance, and international multilateral lenders')
        RETURNING id INTO v_top_id;
    END IF;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Domestic Development Institutions', 'NABARD, SIDBI, EXIM Bank of India, National Housing Bank (NHB), NaBFID')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'International Financial Institutions', 'International Monetary Fund (IMF), World Bank Group (IBRD, IDA, IFC), Asian Development Bank (ADB), New Development Bank (NDB)')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

END $$;

-- Step 2: Insert 100 Bank PO Banking & Financial Awareness Questions
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
    'Banking and Financial Awareness',
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
        'a1140000-0001-0000-0000-000000000001'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'EASY',
        'REMEMBER',
        'On which committee''s recommendation was the Reserve Bank of India established in $$1935$$?',
        '[{"id": "A", "text": "Hilton Young Commission (Royal Commission on Indian Currency and Finance)", "isCorrect": true}, {"id": "B", "text": "Narasimham Committee", "isCorrect": false}, {"id": "C", "text": "Chakravarty Committee", "isCorrect": false}, {"id": "D", "text": "Urjit Patel Committee", "isCorrect": false}]',
        'A',
        'The Reserve Bank of India was established on April 1, $$1935$$ under the RBI Act $$1934$$ based on the recommendations of the Hilton Young Commission ($$1926$$).'
    ),
    (
        'a1140000-0002-0000-0000-000000000002'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'MEDIUM',
        'REMEMBER',
        'Under which section of the Reserve Bank of India Act, $$1934$$, does the RBI have the sole right to issue banknotes in India?',
        '[{"id": "A", "text": "Section $$22$$", "isCorrect": true}, {"id": "B", "text": "Section $$24$$", "isCorrect": false}, {"id": "C", "text": "Section $$7$$", "isCorrect": false}, {"id": "D", "text": "Section $$42$$", "isCorrect": false}]',
        'A',
        'Under Section $$22$$ of the RBI Act $$1934$$, the Reserve Bank of India has the sole authority to issue banknotes in India (excluding one-rupee notes and coins).'
    ),
    (
        'a1140000-0003-0000-0000-000000000003'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'EASY',
        'REMEMBER',
        'Who issues the one-rupee coin and one-rupee note in India, and whose signature does the one-rupee note bear?',
        '[{"id": "A", "text": "Ministry of Finance, Government of India; signed by the Finance Secretary", "isCorrect": true}, {"id": "B", "text": "Reserve Bank of India; signed by the RBI Governor", "isCorrect": false}, {"id": "C", "text": "Ministry of Corporate Affairs; signed by the Union Minister", "isCorrect": false}, {"id": "D", "text": "State Bank of India; signed by the SBI Chairman", "isCorrect": false}]',
        'A',
        'Under the Coinage Act $$2011$$, one-rupee notes and coins are issued by the Ministry of Finance, Government of India, and the note is signed by the Finance Secretary.'
    ),
    (
        'a1140000-0004-0000-0000-000000000004'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'HARD',
        'ANALYZE',
        'Under Section $$24$$ of the RBI Act $$1934$$, what is the maximum denomination of banknote that the Reserve Bank of India is authorized to issue?',
        '[{"id": "A", "text": "$$\\text{Rs. } 10,000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 5,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 2,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 20,000$$", "isCorrect": false}]',
        'A',
        'Section $$24(1)$$ of the RBI Act stipulates that banknotes shall be of denomination not exceeding ten thousand rupees ($$\text{Rs. } 10,000$$).'
    ),
    (
        'a1140000-0005-0000-0000-000000000005'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'MEDIUM',
        'UNDERSTAND',
        'What is the Minimum Reserve System ($$\text{MRS}$$) adopted by the RBI since $$1956$$ for the issuance of currency notes?',
        '[{"id": "A", "text": "Maintaining a minimum reserve of $$\\text{Rs. } 200\\text{ crore}$$, including at least $$\\text{Rs. } 115\\text{ crore}$$ in gold and $$\\text{Rs. } 85\\text{ crore}$$ in foreign currencies", "isCorrect": true}, {"id": "B", "text": "Maintaining $$\\text{Rs. } 500\\text{ crore}$$ in gold reserves exclusively", "isCorrect": false}, {"id": "C", "text": "Holding $$\\text{Rs. } 100\\text{ crore}$$ in Government securities and $$\\text{Rs. } 100\\text{ crore}$$ in bullion", "isCorrect": false}, {"id": "D", "text": "A fixed ratio of $$40\\%$$ gold against total currency issued", "isCorrect": false}]',
        'A',
        'Under the Minimum Reserve System ($$1956$$), the RBI must maintain a minimum reserve asset value of $$\text{Rs. } 200\text{ crore}$$ (minimum $$\text{Rs. } 115\text{ crore}$$ in gold and $$\text{Rs. } 85\text{ crore}$$ in foreign securities).'
    ),
    (
        'a1140000-0006-0000-0000-000000000006'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'MEDIUM',
        'REMEMBER',
        'Under Section $$7(1)$$ of the RBI Act $$1934$$, what power does the Central Government hold over the Reserve Bank?',
        '[{"id": "A", "text": "The power to give directions to the RBI in public interest after consultation with the RBI Governor", "isCorrect": true}, {"id": "B", "text": "The power to dismiss the Governor without judicial review", "isCorrect": false}, {"id": "C", "text": "The power to fix the daily exchange rate of the Indian Rupee", "isCorrect": false}, {"id": "D", "text": "The power to dissolve the Monetary Policy Committee", "isCorrect": false}]',
        'A',
        'Section $$7(1)$$ empowers the Central Government to give directions to the Bank as it may, after consultation with the Governor, consider necessary in the public interest.'
    ),
    (
        'a1140000-0007-0000-0000-000000000007'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'EASY',
        'REMEMBER',
        'In which city is the Central Office of the Reserve Bank of India located since its transfer from Kolkata in $$1937$$?',
        '[{"id": "A", "text": "Mumbai", "isCorrect": true}, {"id": "B", "text": "New Delhi", "isCorrect": false}, {"id": "C", "text": "Bengaluru", "isCorrect": false}, {"id": "D", "text": "Chennai", "isCorrect": false}]',
        'A',
        'The Central Office of the RBI was initially established in Calcutta (Kolkata) but was permanently moved to Mumbai in $$1937$$.'
    ),
    (
        'a1140000-0008-0000-0000-000000000008'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'MEDIUM',
        'UNDERSTAND',
        'Under the RBI''s Clean Note Policy, what is prohibited regarding currency notes?',
        '[{"id": "A", "text": "Writing on currency notes, stapling note packets, and excessive scribbling", "isCorrect": true}, {"id": "B", "text": "Accepting soiled notes for exchange at bank branches", "isCorrect": false}, {"id": "C", "text": "Using polymer currency notes for daily transactions", "isCorrect": false}, {"id": "D", "text": "Storing notes in currency chests for more than $$30\\text{ days}$$", "isCorrect": false}]',
        'A',
        'The Clean Note Policy prohibits bank branches and public from stapling currency notes or writing slogans/numbers on the watermarked surface.'
    ),
    (
        'a1140000-0009-0000-0000-000000000009'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'HARD',
        'ANALYZE',
        'What is a ''Currency Chest'' maintained by commercial banks on behalf of the Reserve Bank of India?',
        '[{"id": "A", "text": "A repository where the RBI stores banknotes and rupee coins to facilitate cash distribution across India", "isCorrect": true}, {"id": "B", "text": "A special locked safe for storing non-performing loan documents", "isCorrect": false}, {"id": "C", "text": "A vault exclusively dedicated to corporate foreign exchange transactions", "isCorrect": false}, {"id": "D", "text": "An escrow vault maintained for clearing house settlement failures", "isCorrect": false}]',
        'A',
        'Currency Chests are branches of selected commercial banks authorized by the RBI to hold currency notes and coins on its behalf for regional cash distribution.'
    ),
    (
        'a1140000-000a-0000-0000-00000000000a'::uuid,
        'Indian Banking System & RBI Functions',
        'RBI Constitution & Currency Management',
        'EASY',
        'REMEMBER',
        'What is the accounting year of the Reserve Bank of India currently aligned to?',
        '[{"id": "A", "text": "April $$1$$ to March $$31$$ (aligned with the Government Financial Year)", "isCorrect": true}, {"id": "B", "text": "July $$1$$ to June $$30$$", "isCorrect": false}, {"id": "C", "text": "January $$1$$ to December $$31$$ (Calendar Year)", "isCorrect": false}, {"id": "D", "text": "October $$1$$ to September $$30$$", "isCorrect": false}]',
        'A',
        'From the financial year $$2020-21$$, the RBI shifted its accounting year from July–June to April–March to align with the Government of India financial year.'
    ),
    (
        'a1140000-000b-0000-0000-00000000000b'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'EASY',
        'REMEMBER',
        'In which two years did major bank nationalizations take place in India, nationalizing $$14$$ banks and $$6$$ banks respectively?',
        '[{"id": "A", "text": "$$1969$$ and $$1980$$", "isCorrect": true}, {"id": "B", "text": "$$1955$$ and $$1975$$", "isCorrect": false}, {"id": "C", "text": "$$1971$$ and $$1991$$", "isCorrect": false}, {"id": "D", "text": "$$1949$$ and $$1965$$", "isCorrect": false}]',
        'A',
        'The first wave of nationalisation of 14 major commercial banks occurred on July 19, $$1969$$, followed by 6 more commercial banks on April 15, $$1980$$.'
    ),
    (
        'a1140000-000c-0000-0000-00000000000c'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'MEDIUM',
        'UNDERSTAND',
        'What is the maximum balance that a customer can hold in a Payments Bank account at the end of the day as per RBI guidelines?',
        '[{"id": "A", "text": "$$\\text{Rs. } 2,00,000$$ per individual customer", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1,00,000$$ per individual customer", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 5,00,000$$ per individual customer", "isCorrect": false}, {"id": "D", "text": "Unlimited subject to PAN verification", "isCorrect": false}]',
        'A',
        'In April $$2021$$, RBI doubled the maximum end-of-day balance limit for Payments Banks from $$\text{Rs. } 1,00,000$$ to $$\text{Rs. } 2,00,000$$.'
    ),
    (
        'a1140000-000d-0000-0000-00000000000d'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'EASY',
        'REMEMBER',
        'Can Payments Banks in India advance loans or issue credit cards to their customers?',
        '[{"id": "A", "text": "No, Payments Banks cannot grant loans or issue credit cards", "isCorrect": true}, {"id": "B", "text": "Yes, but only unsecured micro-loans up to Rs. 50,000", "isCorrect": false}, {"id": "C", "text": "Yes, but only against collateral of gold", "isCorrect": false}, {"id": "D", "text": "Yes, through tie-ups with NBFCs without RBI approval", "isCorrect": false}]',
        'A',
        'Payments Banks are differentiated banks that accept demand deposits and provide remittances, but are explicitly barred from undertaking lending activities or issuing credit cards.'
    ),
    (
        'a1140000-000e-0000-0000-00000000000e'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'HARD',
        'ANALYZE',
        'What is the minimum equity shareholding pattern mandated for Regional Rural Banks ($$\text{RRBs}$$) under the RRB Act $$1976$$?',
        '[{"id": "A", "text": "Central Government: $$50\\%$$, Sponsor Bank: $$35\\%$$, State Government: $$15\\%$$", "isCorrect": true}, {"id": "B", "text": "Central Government: $$51\\%$$, Sponsor Bank: $$49\\%$$, State Government: $$0\\%$$", "isCorrect": false}, {"id": "C", "text": "Central Government: $$33.33\\%$$, Sponsor Bank: $$33.33\\%$$, State Government: $$33.33\\%$$", "isCorrect": false}, {"id": "D", "text": "Central Government: $$60\\%$$, Sponsor Bank: $$20\\%$$, State Government: $$20\\%$$", "isCorrect": false}]',
        'A',
        'Under the Regional Rural Banks Act $$1976$$, the share capital is contributed by Central Government ($$50\%$$), Sponsor Bank ($$35\%$$), and State Government ($$15\%$$).'
    ),
    (
        'a1140000-000f-0000-0000-00000000000f'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'MEDIUM',
        'REMEMBER',
        'What is the minimum initial paid-up voting equity capital required to establish a new Small Finance Bank ($$\text{SFB}$$) under current on-tap licensing norms?',
        '[{"id": "A", "text": "$$\\text{Rs. } 200\\text{ crore}$$ (reduced to $$\\text{Rs. } 100\\text{ crore}$$ for transition of UCBs)", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 500\\text{ crore}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 100\\text{ crore}$$ universally", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 50\\text{ crore}$$", "isCorrect": false}]',
        'A',
        'Under the on-tap licensing guidelines for Small Finance Banks, the minimum paid-up voting equity capital is $$\text{Rs. } 200\text{ crore}$$ ($$\text{Rs. } 100\text{ crore}$$ for urban cooperative banks transitioning to SFBs).'
    ),
    (
        'a1140000-0010-0000-0000-000000000010'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'HARD',
        'UNDERSTAND',
        'What proportion of a Small Finance Bank''s loan portfolio must be comprised of loans and advances of up to $$\text{Rs. } 25\text{ lakh}$$?',
        '[{"id": "A", "text": "At least $$50\\%$$ of its loan portfolio", "isCorrect": true}, {"id": "B", "text": "At least $$75\\%$$ of its loan portfolio", "isCorrect": false}, {"id": "C", "text": "At least $$40\\%$$ of its loan portfolio", "isCorrect": false}, {"id": "D", "text": "At least $$25\\%$$ of its loan portfolio", "isCorrect": false}]',
        'A',
        'Small Finance Banks are mandated to ensure that at least $$50\%$$ of their loan portfolio constitutes loans and advances of up to $$\text{Rs. } 25\text{ lakh}$$ to foster micro-enterprise lending.'
    ),
    (
        'a1140000-0011-0000-0000-000000000011'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'EASY',
        'REMEMBER',
        'Which was the first Regional Rural Bank ($$\text{RRB}$$) established in India on October 2, $$1975$$?',
        '[{"id": "A", "text": "Prathama Bank (sponsored by Syndicate Bank in Moradabad, UP)", "isCorrect": true}, {"id": "B", "text": "Baroda UP Gramin Bank", "isCorrect": false}, {"id": "C", "text": "Aryavart Bank", "isCorrect": false}, {"id": "D", "text": "Kerala Gramin Bank", "isCorrect": false}]',
        'A',
        'Prathama Bank was the first RRB established on October 2, $$1975$$, headquartered at Moradabad, Uttar Pradesh, and sponsored by Syndicate Bank.'
    ),
    (
        'a1140000-0012-0000-0000-000000000012'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'MEDIUM',
        'UNDERSTAND',
        'What constitutes a ''Scheduled Bank'' in India under the banking legal framework?',
        '[{"id": "A", "text": "A bank included in the Second Schedule of the Reserve Bank of India Act, $$1934$$", "isCorrect": true}, {"id": "B", "text": "A bank listed on the National Stock Exchange (NSE)", "isCorrect": false}, {"id": "C", "text": "A bank registered under the Companies Act $$2013$$ with branches in all States", "isCorrect": false}, {"id": "D", "text": "Any bank authorized to operate automated teller machines (ATMs)", "isCorrect": false}]',
        'A',
        'A Scheduled Bank is a bank that is included in the Second Schedule of the RBI Act $$1934$$, satisfying capital adequacy and depositor protection criteria under Section $$42(6)$$.'
    ),
    (
        'a1140000-0013-0000-0000-000000000013'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'MEDIUM',
        'REMEMBER',
        'By what act was the Imperial Bank of India nationalized and transformed into the State Bank of India ($$\text{SBI}$$) in $$1955$$?',
        '[{"id": "A", "text": "State Bank of India Act, $$1955$$ (based on All India Rural Credit Survey Committee report)", "isCorrect": true}, {"id": "B", "text": "Banking Companies Act, $$1949$$", "isCorrect": false}, {"id": "C", "text": "Reserve Bank of India (Transfer to Public Ownership) Act, $$1948$$", "isCorrect": false}, {"id": "D", "text": "Companies Act, $$1956$$", "isCorrect": false}]',
        'A',
        'The State Bank of India was constituted on July 1, $$1955$$ under the SBI Act $$1955$$, taking over the undertaking of the Imperial Bank of India as recommended by the Gorwala Committee.'
    ),
    (
        'a1140000-0014-0000-0000-000000000014'::uuid,
        'Indian Banking System & RBI Functions',
        'Commercial Banks & Types',
        'HARD',
        'ANALYZE',
        'What is the regulatory limit on voting rights of a single shareholder in a private sector commercial bank in India?',
        '[{"id": "A", "text": "Capped at $$26\\%$$ of the total voting rights, regardless of excess shareholding", "isCorrect": true}, {"id": "B", "text": "Capped at $$10\\%$$ strictly", "isCorrect": false}, {"id": "C", "text": "Equal to the exact percentage of equity held without any statutory cap", "isCorrect": false}, {"id": "D", "text": "Capped at $$51\\%$$ with prior Central Government sanction", "isCorrect": false}]',
        'A',
        'Under Section $$12(2)$$ of the Banking Regulation Act $$1949$$ as amended, no shareholder in a banking company can exercise voting rights in excess of $$26\%$$.'
    ),
    (
        'a1140000-0015-0000-0000-000000000015'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'EASY',
        'REMEMBER',
        'What is the ''Repo Rate'' in the Indian banking and monetary system?',
        '[{"id": "A", "text": "The rate at which RBI lends short-term money to commercial banks against government securities", "isCorrect": true}, {"id": "B", "text": "The rate at which commercial banks lend to their most creditworthy corporate clients", "isCorrect": false}, {"id": "C", "text": "The interest rate offered on long-term fixed deposits by public sector banks", "isCorrect": false}, {"id": "D", "text": "The rate at which commercial banks park excess overnight funds with RBI without collateral", "isCorrect": false}]',
        'A',
        'Repo Rate (Repurchase Option) is the rate at which the RBI lends money to commercial banks against approved securities under the Liquidity Adjustment Facility (LAF).'
    ),
    (
        'a1140000-0016-0000-0000-000000000016'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'MEDIUM',
        'UNDERSTAND',
        'How does the Standing Deposit Facility ($$\text{SDF}$$), introduced in $$2022$$, differ from the traditional Reverse Repo Facility?',
        '[{"id": "A", "text": "SDF absorbs liquidity from banks without requiring the RBI to provide collateral of government securities", "isCorrect": true}, {"id": "B", "text": "SDF requires banks to pledge double the amount of G-Secs as collateral", "isCorrect": false}, {"id": "C", "text": "SDF carries a higher interest rate than the Marginal Standing Facility", "isCorrect": false}, {"id": "D", "text": "SDF is available only to foreign and regional rural banks", "isCorrect": false}]',
        'A',
        'Introduced in April $$2022$$ under Section $$17(1\text{A})$$ of the RBI Act, the Standing Deposit Facility (SDF) empowers RBI to absorb uncollateralized overnight liquidity from banks.'
    ),
    (
        'a1140000-0017-0000-0000-000000000017'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'HARD',
        'ANALYZE',
        'What are the floor and ceiling of the current Liquidity Adjustment Facility ($$\text{LAF}$$) corridor of the RBI?',
        '[{"id": "A", "text": "Floor is the Standing Deposit Facility (SDF) rate, and Ceiling is the Marginal Standing Facility (MSF) rate", "isCorrect": true}, {"id": "B", "text": "Floor is Reverse Repo Rate, and Ceiling is Bank Rate", "isCorrect": false}, {"id": "C", "text": "Floor is Cash Reserve Ratio, and Ceiling is Statutory Liquidity Ratio", "isCorrect": false}, {"id": "D", "text": "Floor is Call Money Rate, and Ceiling is MCLR", "isCorrect": false}]',
        'A',
        'The LAF corridor is symmetrically aligned around the policy Repo Rate: the Standing Deposit Facility (SDF) rate operates as the floor ($$\text{Repo} - 25\text{ bps}$$), and the MSF operates as the ceiling ($$\text{Repo} + 25\text{ bps}$$).'
    ),
    (
        'a1140000-0018-0000-0000-000000000018'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'MEDIUM',
        'UNDERSTAND',
        'Where is the Cash Reserve Ratio ($$\text{CRR}$$) maintained by commercial banks in India?',
        '[{"id": "A", "text": "As cash balances with the Reserve Bank of India, earning zero interest", "isCorrect": true}, {"id": "B", "text": "In the bank''s own vaults as unencumbered cash", "isCorrect": false}, {"id": "C", "text": "Invested in sovereign gold bonds yielding 2.5% interest", "isCorrect": false}, {"id": "D", "text": "Deposited in public accounts of the State Government", "isCorrect": false}]',
        'A',
        'Under Section $$42(1)$$ of the RBI Act $$1934$$, CRR is maintained as cash balances with the RBI, and the RBI pays no interest to banks on CRR balances.'
    ),
    (
        'a1140000-0019-0000-0000-000000000019'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'EASY',
        'REMEMBER',
        'Under Section $$24$$ of the Banking Regulation Act $$1949$$, in which assets can banks maintain their Statutory Liquidity Ratio ($$\text{SLR}$$)?',
        '[{"id": "A", "text": "Cash in hand, Gold, and unencumbered approved Government securities", "isCorrect": true}, {"id": "B", "text": "Only physical currency notes in the bank branch", "isCorrect": false}, {"id": "C", "text": "Corporate bonds and equity mutual funds", "isCorrect": false}, {"id": "D", "text": "Foreign currency balances held with offshore branches", "isCorrect": false}]',
        'A',
        'SLR is maintained by banks in liquid assets: cash, gold, and unencumbered government and other approved securities as a percentage of their Net Demand and Time Liabilities (NDTL).'
    ),
    (
        'a1140000-001a-0000-0000-00000000001a'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'HARD',
        'ANALYZE',
        'Under the Marginal Standing Facility ($$\text{MSF}$$), commercial banks can borrow overnight liquidity up to what maximum percentage of their NDTL?',
        '[{"id": "A", "text": "$$2\\%$$ of their Net Demand and Time Liabilities (NDTL) by dipping into SLR quota", "isCorrect": true}, {"id": "B", "text": "$$5\\%$$ of their NDTL", "isCorrect": false}, {"id": "C", "text": "$$10\\%$$ of their total capital adequacy reserve", "isCorrect": false}, {"id": "D", "text": "Unlimited subject to board approval", "isCorrect": false}]',
        'A',
        'Under the MSF window, scheduled commercial banks can borrow funds overnight against the pledge of government securities by dipping into their SLR portfolio up to a prescribed limit (normally $$2\%$$ of NDTL).'
    ),
    (
        'a1140000-001b-0000-0000-00000000001b'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'MEDIUM',
        'UNDERSTAND',
        'What is the ''Bank Rate'' under Section $$49$$ of the RBI Act $$1934$$?',
        '[{"id": "A", "text": "The standard rate at which RBI is prepared to buy or rediscount bills of exchange and commercial papers", "isCorrect": true}, {"id": "B", "text": "The interest rate commercial banks charge on savings accounts", "isCorrect": false}, {"id": "C", "text": "The rate at which banks lend to retail home loan borrowers", "isCorrect": false}, {"id": "D", "text": "The fixed commission rate charged for issuing bank guarantees", "isCorrect": false}]',
        'A',
        'Under Section $$49$$, the Bank Rate is the standard rate at which the RBI rediscounts eligible commercial paper and bills of exchange, currently aligned with the MSF rate.'
    ),
    (
        'a1140000-001c-0000-0000-00000000001c'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'MEDIUM',
        'APPLY',
        'When the RBI increases the Cash Reserve Ratio ($$\text{CRR}$$), what immediate impact does it have on the banking system?',
        '[{"id": "A", "text": "It contracts bank liquidity and reduces the credit-creation capacity of commercial banks", "isCorrect": true}, {"id": "B", "text": "It increases money supply and lowers commercial lending rates", "isCorrect": false}, {"id": "C", "text": "It stimulates immediate stock market rally", "isCorrect": false}, {"id": "D", "text": "It forces banks to liquidate their foreign currency assets", "isCorrect": false}]',
        'A',
        'An increase in CRR locks away a higher proportion of banks'' deposit liabilities with the RBI, sucking out liquidity and curbing credit expansion.'
    ),
    (
        'a1140000-001d-0000-0000-00000000001d'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'HARD',
        'UNDERSTAND',
        'What are Open Market Operations ($$\text{OMOs}$$) conducted by the Reserve Bank of India?',
        '[{"id": "A", "text": "Outright sale and purchase of government securities in the open market to regulate rupee liquidity", "isCorrect": true}, {"id": "B", "text": "Direct auction of gold bars to public retail consumers", "isCorrect": false}, {"id": "C", "text": "Setting the minimum margin requirement for commodity futures", "isCorrect": false}, {"id": "D", "text": "Licensing of overseas branches for public sector banks", "isCorrect": false}]',
        'A',
        'Open Market Operations involve outright purchase (infusing liquidity) or sale (absorbing liquidity) of Government Securities by the RBI in the secondary market.'
    ),
    (
        'a1140000-001e-0000-0000-00000000001e'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Policy Rates & Liquidity Ratios',
        'HARD',
        'ANALYZE',
        'What is ''Operation Twist'' occasionally executed by the Reserve Bank of India?',
        '[{"id": "A", "text": "Simultaneous sale of short-term government securities and purchase of long-term government bonds", "isCorrect": true}, {"id": "B", "text": "A complete replacement of soiled banknotes with polymer notes", "isCorrect": false}, {"id": "C", "text": "A sharp depreciation of the rupee against the US dollar", "isCorrect": false}, {"id": "D", "text": "Forced merger of regional cooperative banks with public sector banks", "isCorrect": false}]',
        'A',
        'Operation Twist involves simultaneously buying long-term securities (to depress long-term yields) and selling short-term securities (to absorb short-term liquidity) without changing total balance sheet size.'
    ),
    (
        'a1140000-001f-0000-0000-00000000001f'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'EASY',
        'REMEMBER',
        'How many total members constitute the Monetary Policy Committee ($$\text{MPC}$$) of India, and who is its ex-officio Chairperson?',
        '[{"id": "A", "text": "$$6\\text{ members}$$ ($$3$$ from RBI and $$3$$ appointed by Central Govt); chaired by the RBI Governor", "isCorrect": true}, {"id": "B", "text": "$$5\\text{ members}$$; chaired by the Union Finance Minister", "isCorrect": false}, {"id": "C", "text": "$$7\\text{ members}$$; chaired by the Chief Economic Adviser", "isCorrect": false}, {"id": "D", "text": "$$4\\text{ members}$$; chaired by the Deputy Governor in charge of monetary policy", "isCorrect": false}]',
        'A',
        'The MPC constituted under Section $$45\text{ZB}$$ of the RBI Act consists of $$6$$ members ($$3$$ internal from RBI, $$3$$ external experts appointed by the Central Government), chaired by the RBI Governor.'
    ),
    (
        'a1140000-0020-0000-0000-000000000020'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'MEDIUM',
        'REMEMBER',
        'What is the inflation target mandated for the Monetary Policy Committee under the Flexible Inflation Targeting framework?',
        '[{"id": "A", "text": "$$4\\%$$ Consumer Price Index (CPI) inflation with a tolerance band of $$\\pm 2\\%$$ ($$2\\% - 6\\%$$)", "isCorrect": true}, {"id": "B", "text": "$$5\\%$$ Wholesale Price Index (WPI) inflation with a tolerance band of $$\\pm 1\\%$$", "isCorrect": false}, {"id": "C", "text": "$$3\\%$$ GDP deflator with a tolerance band of $$\\pm 0.5\\%$$", "isCorrect": false}, {"id": "D", "text": "Zero percent headline inflation target", "isCorrect": false}]',
        'A',
        'Under the Monetary Policy Framework Agreement, the target is $$4\%$$ CPI headline inflation, with an upper tolerance limit of $$6\%$$ and lower tolerance limit of $$2\%$$.'
    ),
    (
        'a1140000-0021-0000-0000-000000000021'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'HARD',
        'ANALYZE',
        'Under Section $$45\text{ZN}$$ of the RBI Act, when is the RBI considered to have failed to meet its inflation target?',
        '[{"id": "A", "text": "When headline inflation is beyond the tolerance band ($$2\\% - 6\\%$$) for three consecutive quarters", "isCorrect": true}, {"id": "B", "text": "When inflation exceeds $$6\\%$$ for two consecutive months", "isCorrect": false}, {"id": "C", "text": "When food inflation crosses $$10\\%$$ in any single quarter", "isCorrect": false}, {"id": "D", "text": "When core inflation diverges from headline inflation by more than 300 basis points", "isCorrect": false}]',
        'A',
        'Failure to meet inflation target is defined as average inflation remaining outside the $$2\% - 6\%$$ band for three consecutive quarters, triggering a formal explanatory report to the Central Government.'
    ),
    (
        'a1140000-0022-0000-0000-000000000022'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'MEDIUM',
        'UNDERSTAND',
        'What is the difference between Headline Inflation and Core Inflation?',
        '[{"id": "A", "text": "Core inflation excludes volatile food and fuel items from the headline CPI basket", "isCorrect": true}, {"id": "B", "text": "Headline inflation is based on WPI, whereas Core inflation is based on CPI", "isCorrect": false}, {"id": "C", "text": "Core inflation measures only manufacturing sector goods", "isCorrect": false}, {"id": "D", "text": "Headline inflation includes imported commodity tariffs, while core inflation does not", "isCorrect": false}]',
        'A',
        'Headline inflation represents total CPI inflation including all components, whereas Core inflation excludes volatile food and fuel components to assess underlying inflationary trends.'
    ),
    (
        'a1140000-0023-0000-0000-000000000023'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'MEDIUM',
        'REMEMBER',
        'What is the quorum for a meeting of the Monetary Policy Committee ($$\text{MPC}$$)?',
        '[{"id": "A", "text": "$$4\\text{ members}$$, at least one of whom must be the Governor or in his absence the Deputy Governor", "isCorrect": true}, {"id": "B", "text": "All $$6\\text{ members}$$ unanimously", "isCorrect": false}, {"id": "C", "text": "$$3\\text{ members}$$ including the Finance Secretary", "isCorrect": false}, {"id": "D", "text": "$$5\\text{ members}$$", "isCorrect": false}]',
        'A',
        'Under Section $$45\text{ZI}$$ of the RBI Act $$1934$$, the quorum for an MPC meeting is $$4$$ members, with the Governor (or Deputy Governor) in attendance.'
    ),
    (
        'a1140000-0024-0000-0000-000000000024'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'HARD',
        'ANALYZE',
        'In case of an equality of votes (tie) during a Monetary Policy Committee meeting, who exercises the casting vote?',
        '[{"id": "A", "text": "The Governor of the Reserve Bank of India, as Chairperson", "isCorrect": true}, {"id": "B", "text": "The Union Finance Minister", "isCorrect": false}, {"id": "C", "text": "The senior-most external academic member", "isCorrect": false}, {"id": "D", "text": "The Chief Economic Adviser", "isCorrect": false}]',
        'A',
        'Under Section $$45\text{ZI}(3)$$, each member of the MPC has one vote, and in case of a tie, the Governor has a second or casting vote.'
    ),
    (
        'a1140000-0025-0000-0000-000000000025'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'MEDIUM',
        'REMEMBER',
        'What is the tenure of the external members appointed to the Monetary Policy Committee by the Central Government?',
        '[{"id": "A", "text": "$$4\\text{ years}$$ without eligibility for re-appointment", "isCorrect": true}, {"id": "B", "text": "$$3\\text{ years}$$ with option of one renewal", "isCorrect": false}, {"id": "C", "text": "$$5\\text{ years}$$ or up to age 65", "isCorrect": false}, {"id": "D", "text": "$$2\\text{ years}$$", "isCorrect": false}]',
        'A',
        'Under Section $$45\text{ZC}(3)$$, an external member holds office for a period of $$4$$ years and is not eligible for re-appointment.'
    ),
    (
        'a1140000-0026-0000-0000-000000000026'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'EASY',
        'REMEMBER',
        'How many times a year is the Monetary Policy Committee legally required to meet at a minimum?',
        '[{"id": "A", "text": "At least $$4\\text{ times}$$ a year", "isCorrect": true}, {"id": "B", "text": "At least $$6\\text{ times}$$ a year", "isCorrect": false}, {"id": "C", "text": "Every single month", "isCorrect": false}, {"id": "D", "text": "Twice a year", "isCorrect": false}]',
        'A',
        'Under Section $$45\text{ZI}(1)$$, the Reserve Bank organizes at least four meetings of the Monetary Policy Committee in a given year (normally held bi-monthly, 6 times).'
    ),
    (
        'a1140000-0027-0000-0000-000000000027'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'MEDIUM',
        'UNDERSTAND',
        'What does a ''Calibrated Tightening'' monetary stance signify?',
        '[{"id": "A", "text": "Interest rate hikes are possible in future meetings, but a rate cut is off the table", "isCorrect": true}, {"id": "B", "text": "Interest rates will definitely be reduced in consecutive meetings", "isCorrect": false}, {"id": "C", "text": "Liquidity will be kept in permanent deficit of Rs. 1 lakh crore", "isCorrect": false}, {"id": "D", "text": "Reserve requirements will be replaced with credit rationing", "isCorrect": false}]',
        'A',
        'A ''calibrated tightening'' stance signals that rate cuts are ruled out and policy rates may either remain on hold or be raised systematically in response to inflationary pressures.'
    ),
    (
        'a1140000-0028-0000-0000-000000000028'::uuid,
        'Monetary Policy & Inflation Dynamics',
        'Monetary Policy Framework',
        'HARD',
        'APPLY',
        'What does the term ''Accommodation'' or ''Accommodative Stance'' in monetary policy denote?',
        '[{"id": "A", "text": "The central bank is willing to expand money supply and cut interest rates to boost economic growth as long as inflation stays within target", "isCorrect": true}, {"id": "B", "text": "The central bank will absorb all excess liquidity and raise statutory reserve ratios", "isCorrect": false}, {"id": "C", "text": "The central bank guarantees foreign exchange to import oil", "isCorrect": false}, {"id": "D", "text": "Banks are ordered to reschedule all retail credit without interest", "isCorrect": false}]',
        'A',
        'An accommodative stance implies that the central bank is prepared to infuse liquidity and keep rates low to stimulate economic activity, provided inflation remains manageable.'
    ),
    (
        'a1140000-0029-0000-0000-000000000029'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'EASY',
        'REMEMBER',
        'After how many days of overdue interest or principal installment is a term loan account classified as a Non-Performing Asset ($$\text{NPA}$$)?',
        '[{"id": "A", "text": "$$90\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$60\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$30\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$180\\text{ days}$$", "isCorrect": false}]',
        'A',
        'As per RBI prudential guidelines, an asset becomes non-performing when it ceases to generate income for the bank, typically when interest or principal remains overdue for more than $$90$$ days.'
    ),
    (
        'a1140000-002a-0000-0000-00000000002a'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'MEDIUM',
        'UNDERSTAND',
        'Under RBI''s early stress recognition framework, what overdue period defines a Special Mention Account category $$2$$ ($$\text{SMA-2}$$)?',
        '[{"id": "A", "text": "$$61\\text{ to } 90\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$1\\text{ to } 30\\text{ days}$$ (SMA-0)", "isCorrect": false}, {"id": "C", "text": "$$31\\text{ to } 60\\text{ days}$$ (SMA-1)", "isCorrect": false}, {"id": "D", "text": "$$91\\text{ to } 120\\text{ days}$$", "isCorrect": false}]',
        'A',
        'The SMA framework classifies stress into: SMA-0 (1–30 days overdue), SMA-1 (31–60 days overdue), and SMA-2 (61–90 days overdue).'
    ),
    (
        'a1140000-002b-0000-0000-00000000002b'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'EASY',
        'REMEMBER',
        'How is an NPA classified as a ''Sub-standard Asset''?',
        '[{"id": "A", "text": "An asset that has remained an NPA for a period less than or equal to $$12\\text{ months}$$", "isCorrect": true}, {"id": "B", "text": "An asset that has remained an NPA for more than 3 years", "isCorrect": false}, {"id": "C", "text": "An asset where collateral value is zero", "isCorrect": false}, {"id": "D", "text": "A loan extended without personal guarantee", "isCorrect": false}]',
        'A',
        'A sub-standard asset is one which has remained an NPA for a period less than or equal to $$12$$ months.'
    ),
    (
        'a1140000-002c-0000-0000-00000000002c'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'MEDIUM',
        'REMEMBER',
        'What is the standard provisioning requirement mandated for secured sub-standard assets?',
        '[{"id": "A", "text": "$$15\\%$$ of the total outstanding balance", "isCorrect": true}, {"id": "B", "text": "$$25\\%$$ of the total outstanding balance", "isCorrect": false}, {"id": "C", "text": "$$10\\%$$ of the total outstanding balance", "isCorrect": false}, {"id": "D", "text": "$$40\\%$$ of the total outstanding balance", "isCorrect": false}]',
        'A',
        'A general provision of $$15\%$$ on the total outstanding balance is required for secured exposures classified under sub-standard category.'
    ),
    (
        'a1140000-002d-0000-0000-00000000002d'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'HARD',
        'ANALYZE',
        'What provisioning is required on the unsecured portion of a sub-standard loan exposure?',
        '[{"id": "A", "text": "$$25\\%$$ (or $$20\\%$$ for infrastructure loan accounts)", "isCorrect": true}, {"id": "B", "text": "$$15\\%$$ universally", "isCorrect": false}, {"id": "C", "text": "$$50\\%$$", "isCorrect": false}, {"id": "D", "text": "$$100\\%$$ immediately", "isCorrect": false}]',
        'A',
        'Unsecured exposures in the sub-standard category attract a higher provisioning requirement of $$25\%$$ ($$20\%$$ in certain infrastructure projects).'
    ),
    (
        'a1140000-002e-0000-0000-00000000002e'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'MEDIUM',
        'UNDERSTAND',
        'When does an asset graduate from ''Sub-standard'' to ''Doubtful Asset''?',
        '[{"id": "A", "text": "When it has remained in the sub-standard category for more than $$12\\text{ months}$$", "isCorrect": true}, {"id": "B", "text": "When it has remained in the sub-standard category for more than 36 months", "isCorrect": false}, {"id": "C", "text": "Immediately upon default of loan covenants", "isCorrect": false}, {"id": "D", "text": "Only when the borrower files for insolvency in court", "isCorrect": false}]',
        'A',
        'An asset is classified as Doubtful if it has remained in the sub-standard category for a period exceeding $$12$$ months.'
    ),
    (
        'a1140000-002f-0000-0000-00000000002f'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'HARD',
        'APPLY',
        'What are the provisioning requirements for the secured portion of Doubtful Assets across D1, D2, and D3 tiers?',
        '[{"id": "A", "text": "D1 (up to 1 yr): $$25\\%$$, D2 (1-3 yrs): $$40\\%$$, D3 (more than 3 yrs): $$100\\%$$", "isCorrect": true}, {"id": "B", "text": "D1: 10%, D2: 20%, D3: 50%", "isCorrect": false}, {"id": "C", "text": "100% across all doubtful stages irrespective of time", "isCorrect": false}, {"id": "D", "text": "D1: 15%, D2: 30%, D3: 75%", "isCorrect": false}]',
        'A',
        'For secured portions: Doubtful up to 1 year ($$\text{D1}$$) = $$25\%$$, Doubtful 1 to 3 years ($$\text{D2}$$) = $$40\%$$, Doubtful over 3 years ($$\text{D3}$$) = $$100\%$$.'
    ),
    (
        'a1140000-0030-0000-0000-000000000030'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'MEDIUM',
        'REMEMBER',
        'What is a ''Loss Asset'' in bank accounting?',
        '[{"id": "A", "text": "An asset identified by the bank, internal/external auditors, or RBI inspection as uncollectible, requiring $$100\\%$$ provisioning", "isCorrect": true}, {"id": "B", "text": "Any loan account where the borrower has closed their savings account", "isCorrect": false}, {"id": "C", "text": "A loan whose interest rate is below the RBI Repo rate", "isCorrect": false}, {"id": "D", "text": "A loan sanctioned to an individual residing overseas", "isCorrect": false}]',
        'A',
        'A loss asset is one where loss has been identified by the bank or internal/external auditors or RBI, but the amount has not been written off entirely; it requires $$100\%$$ provision.'
    ),
    (
        'a1140000-0031-0000-0000-000000000031'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'EASY',
        'REMEMBER',
        'For agricultural loans, after how many crop seasons of unpaid principal/interest is a short-duration crop loan classified as an NPA?',
        '[{"id": "A", "text": "$$2\\text{ crop seasons}$$", "isCorrect": true}, {"id": "B", "text": "$$1\\text{ crop season}$$", "isCorrect": false}, {"id": "C", "text": "$$3\\text{ crop seasons}$$", "isCorrect": false}, {"id": "D", "text": "$$4\\text{ crop seasons}$$", "isCorrect": false}]',
        'A',
        'For short duration crops, a loan is classified as NPA if the installment of principal or interest remains unpaid for $$2$$ crop seasons.'
    ),
    (
        'a1140000-0032-0000-0000-000000000032'::uuid,
        'NPA Management & Resolution Framework',
        'Asset Classification & Provisioning',
        'HARD',
        'UNDERSTAND',
        'For long-duration agricultural crops (crops with crop season longer than one year), when is the loan classified as an NPA?',
        '[{"id": "A", "text": "If the installment remains unpaid for $$1\\text{ crop season}$$", "isCorrect": true}, {"id": "B", "text": "If unpaid for 2 crop seasons", "isCorrect": false}, {"id": "C", "text": "If unpaid for 90 calendar days strictly", "isCorrect": false}, {"id": "D", "text": "If unpaid for 36 months", "isCorrect": false}]',
        'A',
        'For long-duration crops (where the crop season is longer than $$12$$ months), an installment overdue for $$1$$ crop season triggers NPA classification.'
    ),
    (
        'a1140000-0033-0000-0000-000000000033'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'EASY',
        'REMEMBER',
        'In which year was the Insolvency and Bankruptcy Code ($$\text{IBC}$$) enacted by the Parliament of India?',
        '[{"id": "A", "text": "$$2016$$", "isCorrect": true}, {"id": "B", "text": "$$2002$$", "isCorrect": false}, {"id": "C", "text": "$$2013$$", "isCorrect": false}, {"id": "D", "text": "$$2019$$", "isCorrect": false}]',
        'A',
        'The Insolvency and Bankruptcy Code ($$\text{IBC}$$) was enacted in May $$2016$$ to consolidate and amend laws relating to reorganization and insolvency resolution.'
    ),
    (
        'a1140000-0034-0000-0000-000000000034'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'MEDIUM',
        'UNDERSTAND',
        'What is the statutory timeline mandated under the IBC $$2016$$ for completing the Corporate Insolvency Resolution Process ($$\text{CIRP}$$)?',
        '[{"id": "A", "text": "$$180\\text{ days}$$, extendable by $$90\\text{ days}$$, with an overall cap of $$330\\text{ days}$$ including legal delays", "isCorrect": true}, {"id": "B", "text": "$$90\\text{ days}$$ strictly without extension", "isCorrect": false}, {"id": "C", "text": "$$365\\text{ days}$$ unconditionally", "isCorrect": false}, {"id": "D", "text": "$$2\\text{ years}$$ from admission", "isCorrect": false}]',
        'A',
        'The standard CIRP period is $$180$$ days, extendable by $$90$$ days, with a mandatory outer limit of $$330$$ days including litigation time under Section $$12$$.'
    ),
    (
        'a1140000-0035-0000-0000-000000000035'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'HARD',
        'REMEMBER',
        'What is the current minimum default threshold required to trigger corporate insolvency proceedings under Section $$4$$ of the IBC $$2016$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 1\\text{ crore}$$ (raised from $$\\text{Rs. } 1\\text{ lakh}$$ in March 2020)", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 50\\text{ lakh}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 10\\text{ crore}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 25\\text{ lakh}$$", "isCorrect": false}]',
        'A',
        'To prevent small MSMEs from being dragged into insolvency during COVID-19, the Ministry of Corporate Affairs raised the minimum default threshold from $$\text{Rs. } 1\text{ lakh}$$ to $$\text{Rs. } 1\text{ crore}$$.'
    ),
    (
        'a1140000-0036-0000-0000-000000000036'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'MEDIUM',
        'REMEMBER',
        'Which adjudicating authority handles corporate insolvency under the IBC $$2016$$?',
        '[{"id": "A", "text": "National Company Law Tribunal (NCLT)", "isCorrect": true}, {"id": "B", "text": "Debt Recovery Tribunal (DRT)", "isCorrect": false}, {"id": "C", "text": "High Court of the respective State", "isCorrect": false}, {"id": "D", "text": "Banking Ombudsman", "isCorrect": false}]',
        'A',
        'The National Company Law Tribunal (NCLT) is the Adjudicating Authority for corporate persons under the IBC, while the Debt Recovery Tribunal (DRT) adjudicates for individuals and partnership firms.'
    ),
    (
        'a1140000-0037-0000-0000-000000000037'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'MEDIUM',
        'UNDERSTAND',
        'What does Section $$13(2)$$ of the SARFAESI Act, $$2002$$ mandate for secured lenders?',
        '[{"id": "A", "text": "Serving a $$60\\text{-day}$$ demand notice to the defaulting borrower to discharge liabilities in full", "isCorrect": true}, {"id": "B", "text": "Taking immediate possession of assets without prior notice", "isCorrect": false}, {"id": "C", "text": "Filing an FIR with the economic offences wing within 7 days", "isCorrect": false}, {"id": "D", "text": "Auctioning the residential premises within 24 hours", "isCorrect": false}]',
        'A',
        'Under Section $$13(2)$$ of the SARFAESI Act, the secured creditor must give a $$60$$-day notice demanding payment before exercising enforcement rights without court intervention.'
    ),
    (
        'a1140000-0038-0000-0000-000000000038'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'HARD',
        'ANALYZE',
        'What types of assets are explicitly exempt from the application of the SARFAESI Act $$2002$$ under Section $$31$$?',
        '[{"id": "A", "text": "Agricultural land and accounts where remaining debt is less than 20% of principal and interest", "isCorrect": true}, {"id": "B", "text": "Commercial office complexes in metropolitan areas", "isCorrect": false}, {"id": "C", "text": "Industrial manufacturing plants with imported machinery", "isCorrect": false}, {"id": "D", "text": "Vehicles financed under hypothecation agreements", "isCorrect": false}]',
        'A',
        'Under Section $$31$$ of SARFAESI Act, security interest in agricultural land, liens, and claims where remaining debt is less than $$20\%$$ or under $$\text{Rs. } 1\text{ lakh}$$ cannot be enforced.'
    ),
    (
        'a1140000-0039-0000-0000-000000000039'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'MEDIUM',
        'REMEMBER',
        'What entity was set up as the ''Bad Bank'' of India to acquire stressed assets from commercial banks?',
        '[{"id": "A", "text": "National Asset Reconstruction Company Limited (NARCL) with IDRCL as operational manager", "isCorrect": true}, {"id": "B", "text": "Asset Reconstruction Company (India) Limited (ARCIL)", "isCorrect": false}, {"id": "C", "text": "Credit Guarantee Fund Trust for Micro and Small Enterprises (CGTMSE)", "isCorrect": false}, {"id": "D", "text": "National Financial Reporting Authority (NFRA)", "isCorrect": false}]',
        'A',
        'NARCL was incorporated in $$2021$$ as a government-backed bad bank to acquire stressed assets (of Rs. 500 crore and above), supported by the India Debt Resolution Company Ltd (IDRCL).'
    ),
    (
        'a1140000-003a-0000-0000-00000000003a'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'HARD',
        'UNDERSTAND',
        'In what proportion does NARCL pay consideration to banks when purchasing non-performing loans?',
        '[{"id": "A", "text": "$$15\\%$$ in cash and $$85\\%$$ in government-guaranteed Security Receipts (SRs)", "isCorrect": true}, {"id": "B", "text": "$$100\\%$$ upfront cash at written-down value", "isCorrect": false}, {"id": "C", "text": "$$50\\%$$ cash and $$50\\%$$ listed equity shares", "isCorrect": false}, {"id": "D", "text": "Entirely in redeemable preference shares maturing in 20 years", "isCorrect": false}]',
        'A',
        'Under the bad bank resolution model, NARCL pays banks $$15\%$$ upfront in cash and the balance $$85\%$$ in Security Receipts backed by a sovereign guarantee from the Government of India.'
    ),
    (
        'a1140000-003b-0000-0000-00000000003b'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'MEDIUM',
        'REMEMBER',
        'What is the minimum voting share required in the Committee of Creditors ($$\text{CoC}$$) to approve a corporate resolution plan under the IBC?',
        '[{"id": "A", "text": "$$66\\%$$ of the voting share of financial creditors", "isCorrect": true}, {"id": "B", "text": "$$51\\%$$ majority", "isCorrect": false}, {"id": "C", "text": "$$75\\%$$ super-majority", "isCorrect": false}, {"id": "D", "text": "$$90\\%$$ unanimous approval", "isCorrect": false}]',
        'A',
        'Section $$30(4)$$ of the IBC mandates that a resolution plan must be approved by a vote of not less than $$66\%$$ of the voting share of the financial creditors.'
    ),
    (
        'a1140000-003c-0000-0000-00000000003c'::uuid,
        'NPA Management & Resolution Framework',
        'Insolvency & Recovery Mechanisms',
        'HARD',
        'ANALYZE',
        'Under the Prompt Corrective Action ($$\text{PCA}$$) framework of the RBI, what three financial parameters are monitored for commercial banks?',
        '[{"id": "A", "text": "Capital Adequacy (CRAR/CET-1), Asset Quality (Net NPA ratio), and Leverage (Tier 1 Leverage Ratio)", "isCorrect": true}, {"id": "B", "text": "Net profit growth, branch expansion rate, and employee turnover", "isCorrect": false}, {"id": "C", "text": "Total deposits, ATM count, and foreign exchange reserves", "isCorrect": false}, {"id": "D", "text": "Credit card defaults, gold loan volume, and digital transaction percentage", "isCorrect": false}]',
        'A',
        'The revised PCA framework monitors Capital (CRAR/CET1), Asset Quality (Net NPA ratio), and Leverage (Tier 1 Leverage ratio) to enforce early corrective actions.'
    ),
    (
        'a1140000-003d-0000-0000-00000000003d'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'EASY',
        'REMEMBER',
        'Which organization serves as the umbrella organization for operating retail payments and settlement systems in India?',
        '[{"id": "A", "text": "National Payments Corporation of India (NPCI)", "isCorrect": true}, {"id": "B", "text": "Reserve Bank Information Technology Pvt. Ltd. (ReBIT)", "isCorrect": false}, {"id": "C", "text": "Indian Banks'' Association (IBA)", "isCorrect": false}, {"id": "D", "text": "Securities and Exchange Board of India (SEBI)", "isCorrect": false}]',
        'A',
        'NPCI was set up in $$2008$$ under the Payment and Settlement Systems Act $$2007$$ as an umbrella organization founded by RBI and IBA.'
    ),
    (
        'a1140000-003e-0000-0000-00000000003e'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'MEDIUM',
        'UNDERSTAND',
        'What is the primary difference between Real Time Gross Settlement ($$\text{RTGS}$$) and National Electronic Funds Transfer ($$\text{NEFT}$$)?',
        '[{"id": "A", "text": "RTGS settles transactions individually on a continuous gross basis, while NEFT settles in half-hourly batches", "isCorrect": true}, {"id": "B", "text": "NEFT has a minimum transaction limit of Rs. 2 lakh, while RTGS has no minimum limit", "isCorrect": false}, {"id": "C", "text": "RTGS operates only on bank working days, whereas NEFT operates 24x7", "isCorrect": false}, {"id": "D", "text": "RTGS operates exclusively via SMS messaging", "isCorrect": false}]',
        'A',
        'RTGS processes transactions continuously on an order-by-order gross settlement basis (minimum $$\text{Rs. } 2\text{ lakh}$$), whereas NEFT settles transactions in half-hourly net batches.'
    ),
    (
        'a1140000-003f-0000-0000-00000000003f'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'EASY',
        'REMEMBER',
        'What is the minimum transaction amount eligible for transfer through the Real Time Gross Settlement ($$\text{RTGS}$$) system?',
        '[{"id": "A", "text": "$$\\text{Rs. } 2,00,000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1,00,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 5,00,000$$", "isCorrect": false}, {"id": "D", "text": "No minimum limit", "isCorrect": false}]',
        'A',
        'RTGS is meant for large-value transactions and has a statutory floor of $$\text{Rs. } 2\text{ lakh}$$ with no upper ceiling.'
    ),
    (
        'a1140000-0040-0000-0000-000000000040'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'MEDIUM',
        'REMEMBER',
        'How many alphanumeric characters constitute an Indian Financial System Code ($$\text{IFSC}$$), and what is the fifth character?',
        '[{"id": "A", "text": "$$11\\text{ characters}$$; the fifth character is always $$0$$ (zero)", "isCorrect": true}, {"id": "B", "text": "$$10\\text{ characters}$$; the fifth character is an alphabet", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ characters}$$; the fifth character is a hyphen", "isCorrect": false}, {"id": "D", "text": "$$9\\text{ characters}$$; the fifth character is the branch code", "isCorrect": false}]',
        'A',
        'An IFSC code is an $$11$$-character code: first 4 alphabetic characters represent the bank, the 5th character is ''0'' (reserved for future use), and the last 6 characters represent the specific branch.'
    ),
    (
        'a1140000-0041-0000-0000-000000000041'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'EASY',
        'REMEMBER',
        'What is the transaction limit for UPI Lite per single transaction and the total balance limit in the on-device wallet?',
        '[{"id": "A", "text": "$$\\text{Rs. } 500$$ per transaction, and maximum wallet balance of $$\\text{Rs. } 2,000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1,000$$ per transaction, and maximum wallet balance of $$\\text{Rs. } 5,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 200$$ per transaction, and maximum wallet balance of $$\\text{Rs. } 1,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 2,000$$ per transaction, with unlimited wallet balance", "isCorrect": false}]',
        'A',
        'UPI Lite allows small-value near-instant offline wallet transactions of up to $$\text{Rs. } 500$$ per transaction, with a total wallet balance cap of $$\text{Rs. } 2,000$$.'
    ),
    (
        'a1140000-0042-0000-0000-000000000042'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'HARD',
        'ANALYZE',
        'What is the standard turnaround time ($$\text{TAT}$$) mandated by RBI for resolution of failed electronic transactions, under the ''Harmonisation of TAT and customer compensation'' circular?',
        '[{"id": "A", "text": "$$T + 1\\text{ day}$$, failing which banks must compensate the customer at $$\\text{Rs. } 100$$ per day of delay", "isCorrect": true}, {"id": "B", "text": "$$T + 7\\text{ days}$$ without financial penalty", "isCorrect": false}, {"id": "C", "text": "$$T + 3\\text{ days}$$ with interest at savings bank rate", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ days}$$ before initiating dispute redressal", "isCorrect": false}]',
        'A',
        'Under the RBI circular, the turnaround time for failed ATM/UPI/IMPS transactions is generally $$T + 1$$ day, beyond which the bank must pay compensation of $$\text{Rs. } 100$$ per day of delay.'
    ),
    (
        'a1140000-0043-0000-0000-000000000043'::uuid,
        'Digital Banking & Payment Systems',
        'Emerging Digital Payment Ecosystems',
        'MEDIUM',
        'UNDERSTAND',
        'What is the Central Bank Digital Currency ($$\text{CBDC}$$ / e-Rupee) launched by the RBI?',
        '[{"id": "A", "text": "A sovereign digital currency representing legal tender issued by the RBI on distributed ledger technology", "isCorrect": true}, {"id": "B", "text": "A decentralized cryptocurrency like Bitcoin with mining rewards", "isCorrect": false}, {"id": "C", "text": "A commercial bank prepaid gift voucher", "isCorrect": false}, {"id": "D", "text": "A loyalty reward coin issued by the National Payments Corporation of India", "isCorrect": false}]',
        'A',
        'The e-Rupee is the sovereign digital token issued by the RBI as direct liability of the central bank, functioning as digital legal tender in wholesale ($$\text{e}\mathbb{R}\text{-W}$$) and retail ($$\text{e}\mathbb{R}\text{-R}$$) segments.'
    ),
    (
        'a1140000-0044-0000-0000-000000000044'::uuid,
        'Digital Banking & Payment Systems',
        'Emerging Digital Payment Ecosystems',
        'EASY',
        'REMEMBER',
        'What does ''Card Tokenisation'' mean under RBI cybersecurity regulations?',
        '[{"id": "A", "text": "Replacing actual debit/credit card details with an alternate unique code called a ''token''", "isCorrect": true}, {"id": "B", "text": "Issuing physical brass tokens for branch counter queue management", "isCorrect": false}, {"id": "C", "text": "Encoding card magnetic strips with customer fingerprint data", "isCorrect": false}, {"id": "D", "text": "Converting card loyalty points into cryptocurrency", "isCorrect": false}]',
        'A',
        'Tokenisation replaces sensitive card details (16-digit PAN, expiry) with an algorithmically generated unique token to safeguard against merchant data breaches.'
    ),
    (
        'a1140000-0045-0000-0000-000000000045'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'HARD',
        'ANALYZE',
        'In the Cheque Truncation System ($$\text{CTS}$$), what is the ''Positive Pay System'' introduced by the RBI?',
        '[{"id": "A", "text": "Re-confirming key details (date, payee, amount) by the issuer for cheques of $$\\text{Rs. } 50,000$$ and above", "isCorrect": true}, {"id": "B", "text": "Compulsory biometric verification of the payee at the clearing house", "isCorrect": false}, {"id": "C", "text": "A monetary cashback reward paid for writing digital cheques", "isCorrect": false}, {"id": "D", "text": "Automatic electronic conversion of paper cheques into fixed deposit receipts", "isCorrect": false}]',
        'A',
        'Positive Pay is an anti-fraud mechanism whereby the drawer electronically reconfirms key cheque details (account number, cheque number, date, amount, payee) for amounts exceeding $$\text{Rs. } 50,000$$.'
    ),
    (
        'a1140000-0046-0000-0000-000000000046'::uuid,
        'Digital Banking & Payment Systems',
        'Retail & Wholesale Payment Systems',
        'MEDIUM',
        'REMEMBER',
        'What is the Bharat Bill Payment System ($$\text{BBPS}$$) operated by NPCI?',
        '[{"id": "A", "text": "An integrated interoperable bill payment system for recurring utility bills across digital channels", "isCorrect": true}, {"id": "B", "text": "A sovereign credit rating portal for small traders", "isCorrect": false}, {"id": "C", "text": "A platform for buying international travel insurance", "isCorrect": false}, {"id": "D", "text": "An income tax e-filing utility for corporate entities", "isCorrect": false}]',
        'A',
        'BBPS is an RBI-conceptualized, NPCI-operated integrated ecosystem offering interoperable, accessible bill payment services for electricity, water, gas, telecom, and education fees.'
    ),
    (
        'a1140000-0047-0000-0000-000000000047'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'EASY',
        'REMEMBER',
        'What is the maximum maturity period of instruments traded in the Indian Money Market?',
        '[{"id": "A", "text": "Up to $$1\\text{ year}$$ ($$365\\text{ days}$$)", "isCorrect": true}, {"id": "B", "text": "Up to $$3\\text{ years}$$", "isCorrect": false}, {"id": "C", "text": "Up to $$5\\text{ years}$$", "isCorrect": false}, {"id": "D", "text": "Up to $$10\\text{ years}$$", "isCorrect": false}]',
        'A',
        'The Money Market is a market for short-term financial assets with a maturity period of up to one year ($$365$$ days).'
    ),
    (
        'a1140000-0048-0000-0000-000000000048'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'MEDIUM',
        'UNDERSTAND',
        'What is the distinction between ''Call Money'', ''Notice Money'', and ''Term Money'' in the inter-bank money market?',
        '[{"id": "A", "text": "Call Money: $$1\\text{ day}$$ (overnight); Notice Money: $$2\\text{ to } 14\\text{ days}$$; Term Money: $$15\\text{ days to } 1\\text{ year}$$", "isCorrect": true}, {"id": "B", "text": "Call Money: 7 days; Notice Money: 30 days; Term Money: 90 days", "isCorrect": false}, {"id": "C", "text": "Call Money: under 1 hour; Notice Money: 24 hours; Term Money: 7 days", "isCorrect": false}, {"id": "D", "text": "Call Money is for public sector banks only, while Term Money is for foreign banks", "isCorrect": false}]',
        'A',
        'In the inter-bank market, borrowing/lending for 1 day is Call Money, for 2 to 14 days is Notice Money, and for 15 days up to 1 year is Term Money.'
    ),
    (
        'a1140000-0049-0000-0000-000000000049'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'EASY',
        'REMEMBER',
        'What are the standard tenors of Treasury Bills ($$\text{T-Bills}$$) currently issued by the Government of India?',
        '[{"id": "A", "text": "$$91\\text{ days}$$, $$182\\text{ days}$$, and $$364\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ days}$$, $$60\\text{ days}$$, and $$90\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ days}$$, $$28\\text{ days}$$, and $$56\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ year}$$, $$2\\text{ years}$$, and $$5\\text{ years}$$", "isCorrect": false}]',
        'A',
        'The Government of India currently issues three tenors of Treasury Bills: $$91$$-day, $$182$$-day, and $$364$$-day bills, auctioned by the RBI.'
    ),
    (
        'a1140000-004a-0000-0000-00000000004a'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'MEDIUM',
        'UNDERSTAND',
        'At what price are Treasury Bills issued and redeemed?',
        '[{"id": "A", "text": "Issued at a discount to the face value and redeemed at par (zero-coupon instruments)", "isCorrect": true}, {"id": "B", "text": "Issued at par with semi-annual coupon interest payments", "isCorrect": false}, {"id": "C", "text": "Issued at a premium and redeemed at market-linked floating NAV", "isCorrect": false}, {"id": "D", "text": "Issued at face value with inflation-indexed quarterly dividends", "isCorrect": false}]',
        'A',
        'Treasury Bills are zero-coupon securities that pay no periodic interest; they are issued at a discount and redeemed at full face value ($$\text{Par}$$) at maturity.'
    ),
    (
        'a1140000-004b-0000-0000-00000000004b'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'MEDIUM',
        'REMEMBER',
        'What is the minimum denomination and minimum maturity period of Commercial Paper ($$\text{CP}$$) in India?',
        '[{"id": "A", "text": "Minimum denomination: $$\\text{Rs. } 5\\text{ lakh}$$; minimum maturity: $$7\\text{ days}$$ (maximum $$1\\text{ year}$$)", "isCorrect": true}, {"id": "B", "text": "Minimum denomination: $$\\text{Rs. } 1\\text{ lakh}$$; minimum maturity: $$15\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "Minimum denomination: $$\\text{Rs. } 10\\text{ lakh}$$; minimum maturity: $$30\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "Minimum denomination: $$\\text{Rs. } 50,000$$; minimum maturity: $$1\\text{ day}$$", "isCorrect": false}]',
        'A',
        'Commercial Paper is an unsecured money market instrument issued in multiples of $$\text{Rs. } 5\text{ lakh}$$ for maturities between $$7$$ days and $$1$$ year by highly-rated corporates.'
    ),
    (
        'a1140000-004c-0000-0000-00000000004c'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'HARD',
        'ANALYZE',
        'What is a Certificate of Deposit ($$\text{CD}$$) issued by commercial banks?',
        '[{"id": "A", "text": "A negotiable, unsecured money market promissory note issued in dematerialised form for a minimum of $$\\text{Rs. } 1\\text{ lakh}$$", "isCorrect": true}, {"id": "B", "text": "A fixed deposit receipt that cannot be transferred under any circumstances", "isCorrect": false}, {"id": "C", "text": "A certificate issued by SEBI acknowledging payment of mutual fund fees", "isCorrect": false}, {"id": "D", "text": "A letter of credit issued to import agricultural fertilizers", "isCorrect": false}]',
        'A',
        'Certificates of Deposit are negotiable money market instruments issued by scheduled commercial banks for a minimum of $$\text{Rs. } 1\text{ lakh}$$ for periods between 7 days and 1 year.'
    ),
    (
        'a1140000-004d-0000-0000-00000000004d'::uuid,
        'Money Market & Capital Market Instruments',
        'Capital Markets & Bond Dynamics',
        'MEDIUM',
        'UNDERSTAND',
        'What happens to the price of an existing fixed-coupon bond when market interest rates rise?',
        '[{"id": "A", "text": "The bond price falls, because newer bonds offer higher yields (inverse price-yield relationship)", "isCorrect": true}, {"id": "B", "text": "The bond price rises proportionally", "isCorrect": false}, {"id": "C", "text": "The bond coupon rate increases automatically", "isCorrect": false}, {"id": "D", "text": "The bond maturity date is automatically extended", "isCorrect": false}]',
        'A',
        'Bond prices and market yields have an inverse relationship: when prevailing interest rates rise, existing fixed-coupon bonds become less attractive and their market prices fall.'
    ),
    (
        'a1140000-004e-0000-0000-00000000004e'::uuid,
        'Money Market & Capital Market Instruments',
        'Capital Markets & Bond Dynamics',
        'EASY',
        'REMEMBER',
        'What is the annual interest rate paid to investors holding Sovereign Gold Bonds ($$\text{SGBs}$$)?',
        '[{"id": "A", "text": "$$2.50\\%$$ per annum payable semi-annually on the nominal value", "isCorrect": true}, {"id": "B", "text": "$$4.00\\%$$ per annum", "isCorrect": false}, {"id": "C", "text": "$$1.50\\%$$ per annum", "isCorrect": false}, {"id": "D", "text": "Zero percent, returns are purely capital gains", "isCorrect": false}]',
        'A',
        'Sovereign Gold Bonds issued by the RBI on behalf of the Government of India pay a fixed interest of $$2.50\%$$ per annum payable semi-annually, plus capital appreciation on gold at redemption.'
    ),
    (
        'a1140000-004f-0000-0000-00000000004f'::uuid,
        'Money Market & Capital Market Instruments',
        'Capital Markets & Bond Dynamics',
        'HARD',
        'REMEMBER',
        'What is the total tenure of Sovereign Gold Bonds, and from which year is early redemption permitted?',
        '[{"id": "A", "text": "Total tenure: $$8\\text{ years}$$; early exit permitted after the $$5\\text{th year}$$", "isCorrect": true}, {"id": "B", "text": "Total tenure: 10 years; early exit after the 7th year", "isCorrect": false}, {"id": "C", "text": "Total tenure: 5 years; early exit after 3 years", "isCorrect": false}, {"id": "D", "text": "Total tenure: 15 years; early exit after 10 years", "isCorrect": false}]',
        'A',
        'The maturity period of SGBs is $$8$$ years, with an exit option available after the $$5$$th year on interest payment dates.'
    ),
    (
        'a1140000-0050-0000-0000-000000000050'::uuid,
        'Money Market & Capital Market Instruments',
        'Money Market Instruments',
        'HARD',
        'ANALYZE',
        'What is the Triparty Repo System ($$\text{TREPS}$$) introduced by CCIL in the Indian money market?',
        '[{"id": "A", "text": "A collateralized repo borrowing/lending market facilitated by an intermediary (CCIL) acting as third party between lender and borrower", "isCorrect": true}, {"id": "B", "text": "A three-way trade finance contract between an exporter, importer, and shipping line", "isCorrect": false}, {"id": "C", "text": "A tri-lateral agreement between RBI, SBI, and NABARD for rural financing", "isCorrect": false}, {"id": "D", "text": "An uncollateralized lending scheme for Tier-3 microfinance institutions", "isCorrect": false}]',
        'A',
        'TREPS is a tri-party repo mechanism managed by Clearing Corporation of India Ltd (CCIL), providing anonymous electronic order matching and central counterparty settlement.'
    ),
    (
        'a1140000-0051-0000-0000-000000000051'::uuid,
        'Government Financial Schemes & Inclusions',
        'Financial Inclusion Schemes',
        'EASY',
        'REMEMBER',
        'What is the maximum overdraft facility available under Pradhan Mantri Jan Dhan Yojana ($$\text{PMJDY}$$) accounts?',
        '[{"id": "A", "text": "$$\\text{Rs. } 10,000$$ (with up to $$\\text{Rs. } 2,000$$ without conditions)", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 5,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 25,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 50,000$$", "isCorrect": false}]',
        'A',
        'The overdraft facility under PMJDY was enhanced to $$\text{Rs. } 10,000$$ (from $$\text{Rs. } 5,000$$), with no conditions required for overdrafts up to $$\text{Rs. } 2,000$$.'
    ),
    (
        'a1140000-0052-0000-0000-000000000052'::uuid,
        'Government Financial Schemes & Inclusions',
        'Financial Inclusion Schemes',
        'MEDIUM',
        'REMEMBER',
        'What is the accidental insurance cover provided on RuPay debit cards issued with new PMJDY accounts opened after August $$28, 2018$$?',
        '[{"id": "A", "text": "$$\\text{Rs. } 2,00,000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1,00,000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 5,00,000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 50,000$$", "isCorrect": false}]',
        'A',
        'For RuPay debit cards issued to PMJDY account holders opened after August 28, $$2018$$, accidental insurance cover was doubled from $$\text{Rs. } 1\text{ lakh}$$ to $$\text{Rs. } 2\text{ lakh}$$.'
    ),
    (
        'a1140000-0053-0000-0000-000000000053'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'EASY',
        'REMEMBER',
        'What is the annual premium and life insurance coverage amount under Pradhan Mantri Jeevan Jyoti Bima Yojana ($$\text{PMJJBY}$$)?',
        '[{"id": "A", "text": "Annual premium: $$\\text{Rs. } 436$$; Life insurance coverage: $$\\text{Rs. } 2,00,000$$", "isCorrect": true}, {"id": "B", "text": "Annual premium: Rs. 330; Life insurance coverage: Rs. 1,00,000", "isCorrect": false}, {"id": "C", "text": "Annual premium: Rs. 12; Life insurance coverage: Rs. 2,00,000", "isCorrect": false}, {"id": "D", "text": "Annual premium: Rs. 500; Life insurance coverage: Rs. 5,00,000", "isCorrect": false}]',
        'A',
        'Revised in June $$2022$$, PMJJBY provides renewable one-year term life cover of $$\text{Rs. } 2,00,000$$ for death due to any cause at an annual premium of $$\text{Rs. } 436$$ (age group 18-50).'
    ),
    (
        'a1140000-0054-0000-0000-000000000054'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'EASY',
        'REMEMBER',
        'What is the annual premium and accidental death/disability coverage under Pradhan Mantri Suraksha Bima Yojana ($$\text{PMSBY}$$)?',
        '[{"id": "A", "text": "Annual premium: $$\\text{Rs. } 20$$; Accidental death cover: $$\\text{Rs. } 2,00,000$$", "isCorrect": true}, {"id": "B", "text": "Annual premium: Rs. 12; Accidental death cover: Rs. 1,00,000", "isCorrect": false}, {"id": "C", "text": "Annual premium: Rs. 100; Accidental death cover: Rs. 5,00,000", "isCorrect": false}, {"id": "D", "text": "Annual premium: Rs. 330; Accidental death cover: Rs. 2,00,000", "isCorrect": false}]',
        'A',
        'Revised in June $$2022$$, PMSBY provides accidental death and full disability cover of $$\text{Rs. } 2,00,000$$ (and $$\text{Rs. } 1\text{ lakh}$$ for partial disability) at a premium of $$\text{Rs. } 20$$ per annum (age group 18-70).'
    ),
    (
        'a1140000-0055-0000-0000-000000000055'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'MEDIUM',
        'UNDERSTAND',
        'What are the entry age limits and guaranteed monthly pension options available under Atal Pension Yojana ($$\text{APY}$$)?',
        '[{"id": "A", "text": "Entry age: $$18\\text{ to } 40\\text{ years}$$; monthly pension: $$\\text{Rs. } 1,000\\text{ to } \\text{Rs. } 5,000$$ starting at age $$60$$", "isCorrect": true}, {"id": "B", "text": "Entry age: 21 to 55 years; monthly pension: Rs. 2,000 to Rs. 10,000", "isCorrect": false}, {"id": "C", "text": "Entry age: 18 to 60 years; monthly pension: fixed at Rs. 3,000", "isCorrect": false}, {"id": "D", "text": "Entry age: 25 to 45 years; monthly pension: linked to inflation rate", "isCorrect": false}]',
        'A',
        'APY is open to all Indian citizens aged $$18-40$$ years with a bank account, providing fixed monthly pensions of $$\text{Rs. } 1,000, 2,000, 3,000, 4,000,\text{ or } 5,000$$ commencing at age $$60$$.'
    ),
    (
        'a1140000-0056-0000-0000-000000000056'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'MEDIUM',
        'REMEMBER',
        'What are the three loan categories under Pradhan Mantri MUDRA Yojana ($$\text{PMMY}$$) and their respective lending limits?',
        '[{"id": "A", "text": "Shishu (up to $$\\text{Rs. } 50,000$$), Kishore ($$\\text{Rs. } 50,001\\text{ to } 5\\text{ lakh}$$), Tarun ($$\\text{Rs. } 5,00,001\\text{ to } 10\\text{ lakh}$$)", "isCorrect": true}, {"id": "B", "text": "Bal (up to Rs. 25,000), Yuva (Rs. 25,001 to 1 lakh), Vriddha (Rs. 1 lakh to 5 lakh)", "isCorrect": false}, {"id": "C", "text": "Primary (up to Rs. 1 lakh), Secondary (Rs. 1 to 5 lakh), Tertiary (Rs. 5 to 20 lakh)", "isCorrect": false}, {"id": "D", "text": "Micro (up to Rs. 20,000), Small (Rs. 20,001 to 2 lakh), Medium (Rs. 2 to 10 lakh)", "isCorrect": false}]',
        'A',
        'PMMY covers three categories: Shishu (loans up to $$\text{Rs. } 50,000$$), Kishore (above $$\text{Rs. } 50,000$$ and up to $$\text{Rs. } 5\text{ lakh}$$), and Tarun (above $$\text{Rs. } 5\text{ lakh}$$ and up to $$\text{Rs. } 10\text{ lakh}$$, expandable to 20 lakh in Budget 2024).'
    ),
    (
        'a1140000-0057-0000-0000-000000000057'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'HARD',
        'REMEMBER',
        'What is the maximum loan limit provided under the Stand-Up India scheme for SC, ST, and women entrepreneurs?',
        '[{"id": "A", "text": "Bank loans between $$\\text{Rs. } 10\\text{ lakh}$$ and $$\\text{Rs. } 1\\text{ crore}$$ for greenfield enterprises", "isCorrect": true}, {"id": "B", "text": "Loans up to Rs. 10 lakh exclusively", "isCorrect": false}, {"id": "C", "text": "Loans between Rs. 1 crore and Rs. 5 crore", "isCorrect": false}, {"id": "D", "text": "Equity venture capital grants up to Rs. 50 lakh", "isCorrect": false}]',
        'A',
        'Stand-Up India facilitates bank loans between $$\text{Rs. } 10\text{ lakh}$$ and $$\text{Rs. } 1\text{ crore}$$ to at least one SC/ST borrower and at least one woman borrower per bank branch.'
    ),
    (
        'a1140000-0058-0000-0000-000000000058'::uuid,
        'Government Financial Schemes & Inclusions',
        'Social Security & Microfinance Schemes',
        'MEDIUM',
        'UNDERSTAND',
        'What is the collateral-free working capital loan amount offered to street vendors under the PM SVANidhi scheme?',
        '[{"id": "A", "text": "Initial working capital tranche of $$\\text{Rs. } 10,000$$, followed by $$\\text{Rs. } 20,000$$ and $$\\text{Rs. } 50,000$$ upon timely repayment", "isCorrect": true}, {"id": "B", "text": "Flat grant of Rs. 50,000 without repayment obligation", "isCorrect": false}, {"id": "C", "text": "Fixed Rs. 5,000 micro-loan with gold security", "isCorrect": false}, {"id": "D", "text": "Rs. 1,00,000 working capital loan with 15% interest", "isCorrect": false}]',
        'A',
        'PM SVANidhi provides an initial collateral-free loan of $$\text{Rs. } 10,000$$, followed by enhanced tranches of $$\text{Rs. } 20,000$$ and $$\text{Rs. } 50,000$$ with a $$7\%$$ interest subsidy on timely repayment.'
    ),
    (
        'a1140000-0059-0000-0000-000000000059'::uuid,
        'Government Financial Schemes & Inclusions',
        'Financial Inclusion Schemes',
        'HARD',
        'ANALYZE',
        'What is the maximum deposit permitted in a ''Basic Savings Bank Deposit Account'' ($$\text{BSBDA}$$) Small Account within a financial year?',
        '[{"id": "A", "text": "Total credits in a year must not exceed $$\\text{Rs. } 1,00,000$$ and balance must not exceed $$\\text{Rs. } 50,000$$ at any time", "isCorrect": true}, {"id": "B", "text": "Total credits must not exceed Rs. 5,00,000", "isCorrect": false}, {"id": "C", "text": "Balance must not exceed Rs. 10,000 at any point", "isCorrect": false}, {"id": "D", "text": "No cap exists as long as the account holder has Aadhaar", "isCorrect": false}]',
        'A',
        'For BSBDA-Small Accounts (opened without full KYC): total credits shall not exceed $$\text{Rs. } 1\text{ lakh}$$ in a year, maximum balance shall not exceed $$\text{Rs. } 50,000$$, and monthly withdrawals shall not exceed $$\text{Rs. } 10,000$$.'
    ),
    (
        'a1140000-005a-0000-0000-00000000005a'::uuid,
        'Government Financial Schemes & Inclusions',
        'Financial Inclusion Schemes',
        'EASY',
        'REMEMBER',
        'What is the minimum balance requirement for a Basic Savings Bank Deposit Account ($$\text{BSBDA}$$)?',
        '[{"id": "A", "text": "Zero balance (Zero Minimum Balance Requirement)", "isCorrect": true}, {"id": "B", "text": "Rs. 500 in rural areas and Rs. 1,000 in urban areas", "isCorrect": false}, {"id": "C", "text": "Rs. 2,000", "isCorrect": false}, {"id": "D", "text": "Rs. 100", "isCorrect": false}]',
        'A',
        'Under RBI guidelines, BSBDA accounts are zero-balance accounts with no minimum balance maintenance requirement or penalty for non-maintenance.'
    ),
    (
        'a1140000-005b-0000-0000-00000000005b'::uuid,
        'Priority Sector Lending & Agriculture Credit',
        'PSL Targets & Classification',
        'EASY',
        'REMEMBER',
        'What is the overall Priority Sector Lending ($$\text{PSL}$$) target mandated for domestic Scheduled Commercial Banks?',
        '[{"id": "A", "text": "$$40\\%$$ of Adjusted Net Bank Credit (ANBC) or Credit Equivalent Amount of Off-Balance Sheet Exposure (CEOBE)", "isCorrect": true}, {"id": "B", "text": "$$75\\%$$ of ANBC", "isCorrect": false}, {"id": "C", "text": "$$25\\%$$ of ANBC", "isCorrect": false}, {"id": "D", "text": "$$50\\%$$ of ANBC", "isCorrect": false}]',
        'A',
        'Domestic scheduled commercial banks and foreign banks with $$20$$ branches and above are mandated to achieve a total PSL target of $$40\%$$ of ANBC or CEOBE, whichever is higher.'
    ),
    (
        'a1140000-005c-0000-0000-00000000005c'::uuid,
        'Priority Sector Lending & Agriculture Credit',
        'PSL Targets & Classification',
        'MEDIUM',
        'REMEMBER',
        'What is the total PSL target mandated for Regional Rural Banks ($$\text{RRBs}$$) and Small Finance Banks ($$\text{SFBs}$$)?',
        '[{"id": "A", "text": "$$75\\%$$ of ANBC / CEOBE", "isCorrect": true}, {"id": "B", "text": "$$40\\%$$ of ANBC / CEOBE", "isCorrect": false}, {"id": "C", "text": "$$60\\%$$ of ANBC / CEOBE", "isCorrect": false}, {"id": "D", "text": "$$50\\%$$ of ANBC / CEOBE", "isCorrect": false}]',
        'A',
        'To accelerate rural credit and bottom-of-the-pyramid inclusion, RRBs and SFBs have a higher mandatory PSL target of $$75\%$$ of their ANBC.'
    ),
    (
        'a1140000-005d-0000-0000-00000000005d'::uuid,
        'Priority Sector Lending & Agriculture Credit',
        'PSL Targets & Classification',
        'MEDIUM',
        'UNDERSTAND',
        'What is the sub-target for lending to Agriculture within the overall PSL target for domestic commercial banks?',
        '[{"id": "A", "text": "$$18\\%$$ of ANBC (with $$10\\%$$ earmarked for Small and Marginal Farmers)", "isCorrect": true}, {"id": "B", "text": "12% of ANBC", "isCorrect": false}, {"id": "C", "text": "25% of ANBC", "isCorrect": false}, {"id": "D", "text": "15% of ANBC", "isCorrect": false}]',
        'A',
        'Within the 40% PSL quota, commercial banks must direct $$18\%$$ of ANBC to Agriculture, of which a sub-target of $$10\%$$ is dedicated to Small and Marginal Farmers (SMFs).'
    ),
    (
        'a1140000-005e-0000-0000-00000000005e'::uuid,
        'Priority Sector Lending & Agriculture Credit',
        'Rural Infrastructure & Shortfall Mechanics',
        'HARD',
        'ANALYZE',
        'Where must domestic commercial banks deposit funds if they fail to achieve their mandated Priority Sector Lending targets?',
        '[{"id": "A", "text": "Rural Infrastructure Development Fund (RIDF) maintained by NABARD and other funds with SIDBI/NHB", "isCorrect": true}, {"id": "B", "text": "RBI''s Consolidated Fund of India reserve", "isCorrect": false}, {"id": "C", "text": "Deposited in public accounts of the State Bank of India", "isCorrect": false}, {"id": "D", "text": "Transferred directly to the Prime Minister''s Relief Fund", "isCorrect": false}]',
        'A',
        'Any shortfall in achieving PSL targets must be contributed by banks to the Rural Infrastructure Development Fund (RIDF) established with NABARD, or other specified funds with SIDBI, MUDRA, or NHB.'
    ),
    (
        'a1140000-005f-0000-0000-00000000005f'::uuid,
        'Negotiable Instruments & Banking Regulations',
        'Negotiable Instruments Act 1881',
        'EASY',
        'REMEMBER',
        'Under Section $$138$$ of the Negotiable Instruments Act, $$1881$$, what is the offence of cheque bouncing categorized as?',
        '[{"id": "A", "text": "A criminal offence punishable with imprisonment up to $$2\\text{ years}$$ or fine up to twice the cheque amount, or both", "isCorrect": true}, {"id": "B", "text": "A non-cognizable civil breach with a maximum fine of Rs. 1,000", "isCorrect": false}, {"id": "C", "text": "A compoundable administrative default resolved by the bank manager", "isCorrect": false}, {"id": "D", "text": "A non-bailable felony with mandatory 7-year imprisonment", "isCorrect": false}]',
        'A',
        'Section $$138$$ of the NI Act $$1881$$ makes dishonour of a cheque for insufficiency of funds a criminal offence punishable with imprisonment up to $$2$$ years or a fine up to twice the cheque amount.'
    ),
    (
        'a1140000-0060-0000-0000-000000000060'::uuid,
        'Negotiable Instruments & Banking Regulations',
        'Negotiable Instruments Act 1881',
        'MEDIUM',
        'UNDERSTAND',
        'What is the statutory validity period of a bank cheque in India from its date of issue?',
        '[{"id": "A", "text": "$$3\\text{ months}$$ from the date of the cheque", "isCorrect": true}, {"id": "B", "text": "$$6\\text{ months}$$", "isCorrect": false}, {"id": "C", "text": "$$1\\text{ month}$$", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ year}$$", "isCorrect": false}]',
        'A',
        'Effective April 1, $$2012$$, the RBI reduced the validity period of cheques, drafts, and pay orders from $$6$$ months to $$3$$ months from the date of instrument.'
    ),
    (
        'a1140000-0061-0000-0000-000000000061'::uuid,
        'Negotiable Instruments & Banking Regulations',
        'Banking Ombudsman & Customer Rights',
        'HARD',
        'ANALYZE',
        'What is the maximum compensation the Banking Ombudsman can award for mental agony and harassment under the Reserve Bank - Integrated Ombudsman Scheme $$2021$$?',
        '[{"id": "A", "text": "Up to $$\\text{Rs. } 1,00,000$$ (in addition to actual loss compensation up to $$\\text{Rs. } 20\\text{ lakh}$$)", "isCorrect": true}, {"id": "B", "text": "Up to Rs. 50,000", "isCorrect": false}, {"id": "C", "text": "Up to Rs. 5,00,000", "isCorrect": false}, {"id": "D", "text": "Unlimited based on court discretion", "isCorrect": false}]',
        'A',
        'Under the Integrated Ombudsman Scheme $$2021$$, the Ombudsman can award compensation up to $$\text{Rs. } 20\text{ lakh}$$ for actual loss and up to $$\text{Rs. } 1\text{ lakh}$$ for loss of time, expenses, harassment, and mental anguish.'
    ),
    (
        'a1140000-0062-0000-0000-000000000062'::uuid,
        'Basel III Norms & Capital Adequacy',
        'Capital Adequacy & Tier Structure',
        'EASY',
        'REMEMBER',
        'What is the minimum Capital to Risk-Weighted Assets Ratio ($$\text{CRAR}$$) mandated by RBI for Indian Scheduled Commercial Banks under Basel III?',
        '[{"id": "A", "text": "$$9.0\\%$$ (higher than the Basel III global minimum of $$8.0\\%$$)", "isCorrect": true}, {"id": "B", "text": "$$8.0\\%$$", "isCorrect": false}, {"id": "C", "text": "$$11.5\\%$$", "isCorrect": false}, {"id": "D", "text": "$$6.5\\%$$", "isCorrect": false}]',
        'A',
        'The RBI mandates a minimum total CRAR of $$9.0\%$$ for Indian commercial banks ($$11.5\%$$ including the Capital Conservation Buffer), compared to the international Basel III norm of $$8.0\%$$.'
    ),
    (
        'a1140000-0063-0000-0000-000000000063'::uuid,
        'Basel III Norms & Capital Adequacy',
        'Prudential Buffers & Liquidity Norms',
        'HARD',
        'ANALYZE',
        'What is the Capital Conservation Buffer ($$\text{CCB}$$) percentage prescribed by RBI under Basel III norms?',
        '[{"id": "A", "text": "$$2.5\\%$$ of Risk-Weighted Assets, maintained entirely in Common Equity Tier 1 (CET1) capital", "isCorrect": true}, {"id": "B", "text": "$$1.5\\%$$ in Tier 2 subordinate debt", "isCorrect": false}, {"id": "C", "text": "$$3.0\\%$$ in gold bullion holdings", "isCorrect": false}, {"id": "D", "text": "$$0.5\\%$$ in short-term treasury bills", "isCorrect": false}]',
        'A',
        'The Capital Conservation Buffer is prescribed at $$2.5\%$$ of risk-weighted assets, composed entirely of Common Equity Tier 1 (CET1) capital, taking total regulatory capital to $$11.5\%$$.'
    ),
    (
        'a1140000-0064-0000-0000-000000000064'::uuid,
        'Development Financial Institutions & Global Bodies',
        'Domestic Development Institutions',
        'EASY',
        'REMEMBER',
        'On which committee''s recommendation was the National Bank for Agriculture and Rural Development ($$\text{NABARD}$$) established in $$1982$$?',
        '[{"id": "A", "text": "B. Sivaraman Committee (CRAFICARD)", "isCorrect": true}, {"id": "B", "text": "Narasimham Committee", "isCorrect": false}, {"id": "C", "text": "Rangarajan Committee", "isCorrect": false}, {"id": "D", "text": "A. C. Shah Committee", "isCorrect": false}]',
        'A',
        'NABARD was set up on July 12, $$1982$$ under the NABARD Act $$1981$$, implementing the recommendations of the Committee to Review Arrangements for Institutional Credit for Agriculture and Rural Development (CRAFICARD) chaired by B. Sivaraman.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = 'Banking and Financial Awareness' AND s.tenant_id = 'default'
JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
JOIN question_service.subtopic st ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
