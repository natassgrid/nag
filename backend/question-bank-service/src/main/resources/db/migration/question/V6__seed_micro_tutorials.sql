-- =============================================================================
-- V6: Seed Micro-Tutorials across Core Exam Disciplines
-- NextGen Assessment Grid (NAG) - Autonomous Learning & DPI Platform
-- Multi-Approach Pedagogical Seeds (v2) for Subtopic Mastery:
--   - CONCEPT: Foundational principles, intuition, and real-world analogies
--   - WORKED_EXAMPLE: Step-by-step problem deconstruction with LaTeX math
--   - TRAPS_AND_PITFALLS: Exam traps, boundary edge cases, and elimination tactics
--   - CHEAT_SHEET: High-yield formulas, identities, and rapid recall shortcuts
-- Disciplines Covered:
--   - Mathematics (Quantitative Aptitude): Arithmetic, Algebra, Geometry
--   - Reasoning (General Intelligence): Syllogisms, Number Series, Coding-Decoding, Inequalities
--   - General Science: Physics (Mechanics, Optics), Chemistry (Carbon, Acids/Bases), Biology (Cell)
--   - Banking & Financial Awareness: RBI Policy Rates, Capital Adequacy (Basel III)
--   - Indian Polity & Governance: Fundamental Rights & Writs, Preamble & Constitutional Architecture
--   - English Language & Comprehension: Subject-Verb Concord & Modifier Defects, Reported Speech
--   - Computer Science & Digital Tech: Networking Devices, OSI Stack, and Subnetting
-- Standards:
--   - AGENTS.md LaTeX standard: All math enclosed in $$ ... $$
--   - PostgreSQL JSONB serialization with verified double-backslash escaping
--   - Deterministic UUIDs: b1010000-0000-0000-0000-000000000001 to b1010000-0000-0000-0000-000000000036
--   - Fully resolves taxonomy via subject/topic/subtopic codes
-- =============================================================================

INSERT INTO question_service.subtopic_micro_lesson (
    id, tenant_id, subject_id, topic_id, subtopic_id,
    lesson_type, difficulty_level, sequence_no, prerequisite_lesson_id,
    estimated_minutes, language, core_concept, trap,
    cheat_sheet, step_by_step_breakdown,
    status, reviewed_by, reviewed_at, review_comments,
    quality_score, content_version
)
SELECT
    v.id,
    'default',
    s.id,
    t.id,
    st.id,
    v.lesson_type,
    v.difficulty_level,
    v.sequence_no,
    v.prerequisite_lesson_id,
    v.estimated_minutes,
    'en',
    v.core_concept,
    v.trap,
    v.cheat_sheet::jsonb,
    v.step_by_step_breakdown::jsonb,
    'PUBLISHED',
    '00000000-0000-0000-0000-000000000001'::uuid,
    NOW(),
    'Canonical seed micro-tutorial verified for pedagogy and LaTeX math standards.',
    0.98,
    1
FROM (VALUES
    (
        'b1010000-0000-0000-0000-000000000001'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PERCENTAGES',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'A percentage expresses a dimensionless ratio as a fraction of one hundred, providing a universal normalized scale to quantify relative change. Think of festive shopping at an Indian bazaar: when a merchant offers an initial discount followed by a clearance discount, the final effect is not a simple arithmetic sum, because each subsequent fraction applies to the intermediate discounted amount. The net interaction between successive percentage changes is governed by the net change formula $$\text{Net Change} = \left(a + b + \frac{ab}{100}\right)\%$$.',
        'The most prevalent exam trap is naively adding successive percentage changes: assuming a $$20\%$$ increase followed by a $$20\%$$ decrease returns to the original value, when it actually results in a net $$4\%$$ loss ($$\text{Net} = 20 - 20 - \frac{400}{100} = -4\%$$).',
        '["Successive Net Change: $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$", "Percentage Change: $$\\text{Percentage Change} = \\left(\\frac{\\text{Final} - \\text{Initial}}{\\text{Initial}}\\right) \\times 100\\%$$", "Multiplier Form: $$\\text{Multiplier} = 1 + \\frac{x}{100} \\quad (\\text{increase}), \\quad 1 - \\frac{x}{100} \\quad (\\text{decrease})$$"]',
        '{"questionStem": "The price of an article is first increased by $$25\\%$$ and subsequently decreased by $$20\\%$$. Find the net percentage change in the final price of the article.", "steps": ["Step 1: Identify the two successive percentage adjustments: $$a = +25\\%$$ and $$b = -20\\%$$.", "Step 2: Apply the successive net percentage formula: $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$.", "Step 3: Substitute parameters: $$\\text{Net Change} = \\left(25 - 20 + \\frac{25 \\times (-20)}{100}\\right)\\% = \\left(5 - \\frac{500}{100}\\right)\\% = (5 - 5)\\% = 0\\%$$. The final price remains unchanged."], "finalAnswer": "$$\\text{Net Change} = 0\\% \\quad (\\text{Price unchanged})$$", "structuredSteps": [{"stepNumber": 1, "title": "Identify Successive Rates", "explanation": "Designate the first increase as $$a = +25\\%$$ and the subsequent reduction as $$b = -20\\%$$.", "formula": "$$a = +25, \\quad b = -20$$", "tip": "Always retain algebraic signs for decreases."}, {"stepNumber": 2, "title": "Apply Successive Net Formula", "explanation": "Substitute $$a$$ and $$b$$ into the compound change identity.", "formula": "$$\\text{Net Change} = a + b + \\frac{ab}{100}$$", "tip": "The product term captures the compounding effect."}, {"stepNumber": 3, "title": "Evaluate Arithmetic Result", "explanation": "Compute algebraic sum and cross term: $$5 - 5 = 0\\%$$.", "formula": "$$\\text{Net Change} = 25 - 20 - 5 = 0\\%$$", "tip": "A $$25\\%$$ increase is precisely canceled by a $$20\\%$$ decrease because the base expands."}], "structuredTrapAlert": {"title": "Additive Percentage Fallacy", "description": "Assuming $$+25\\% - 20\\% = +5\\%$$.", "commonMistake": "Adding percentage figures directly without accounting for the expanded base.", "correctApproach": "Always use multiplier products $$1.25 \\times 0.80 = 1.00$$ or the net change formula."}, "structuredFormulas": [{"name": "Successive Net Percentage", "latex": "$$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$", "description": "Compound net change across two consecutive percentage variations.", "variables": ["a", "b"]}], "keyTakeaways": ["Percentage changes alter the operational base for all subsequent modifications.", "Equal percentage increase and decrease always result in an overall loss.", "Fractional multipliers simplify multi-step percentage calculations."], "quickCheckItems": [{"question": "If a quantity increases by $$50\\%$$ and then decreases by $$50\\%$$, what is the net percentage change?", "options": ["$$0\\%$$", "$$25\\%$$ increase", "$$25\\%$$ decrease", "$$5\\%$$ decrease"], "correctOptionIndex": 2, "explanation": "$$\\text{Net} = 50 - 50 + \\frac{50 \\times (-50)}{100} = -25\\%$$, so there is a $$25\\%$$ decrease."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000002'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PERCENTAGES',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000001'::uuid,
        6,
        'When solving multi-stage percentage problems involving population demographics or successive depreciation, converting percentage changes into fractional multipliers yields lightning-fast computation. If a population of $$P_0$$ grows by $$r_1\%$$ in year 1 and depreciates by $$r_2\%$$ in year 2, the final population is $$P = P_0 \left(1 + \frac{r_1}{100}\right)\left(1 - \frac{r_2}{100}\right)$$.',
        'Rounding intermediate decimal values in multi-stage percentage calculations introduces rounding errors that fail multiple-choice precision checks. Always maintain vulgar fractions until the final step.',
        '["Population Multiplier: $$P_t = P_0 \\prod_{i=1}^n \\left(1 \\pm \\frac{r_i}{100}\\right)$$", "Depreciation Value: $$V = V_0 \\left(1 - \\frac{r}{100}\\right)^n$$", "Annualized Rate: $$\\text{CAGR} = \\left(\\frac{P_t}{P_0}\\right)^{\\frac{1}{t}} - 1$$"]',
        '{"questionStem": "A town has an initial population of $$200,000$$. In the first year, it increases by $$10\\%$$, and in the second year, due to an epidemic, it decreases by $$10\\%$$. What is the population of the town at the end of the second year?", "steps": ["Step 1: Compute the population multiplier for Year 1 ($$+10\\%$$): $$M_1 = 1 + \\frac{10}{100} = \\frac{11}{10}$$.", "Step 2: Compute the population multiplier for Year 2 ($$-10\\%$$): $$M_2 = 1 - \\frac{10}{100} = \\frac{9}{10}$$.", "Step 3: Calculate the final population: $$P_2 = 200000 \\times \\frac{11}{10} \\times \\frac{9}{10} = 200000 \\times \\frac{99}{100} = 198,000$$. The net change is a $$1\\%$$ decrease ($$2000$$ people)."], "finalAnswer": "$$P = 198,000 \\quad (\\text{Net } 1\\% \\text{ reduction})$$", "structuredSteps": [{"stepNumber": 1, "title": "Year 1 Expansion", "explanation": "Determine Year 1 fractional multiplier.", "formula": "$$M_1 = \\frac{11}{10}$$", "tip": "Express $$110\\%$$ as $$11/10$$."}, {"stepNumber": 2, "title": "Year 2 Contraction", "explanation": "Determine Year 2 fractional multiplier.", "formula": "$$M_2 = \\frac{9}{10}$$", "tip": "Express $$90\\%$$ as $$9/10$$."}, {"stepNumber": 3, "title": "Compounded Outcome", "explanation": "Multiply initial population by composite multiplier $$99/100$$.", "formula": "$$P_2 = 200000 \\times \\frac{99}{100} = 198000$$", "tip": "Directly subtract $$1\\%$$ ($$2000$$) from $$200000$$."}], "structuredTrapAlert": {"title": "Naive Zero-Change Assumption", "description": "Assuming $$+10\\% - 10\\% = 0\\%$$ change gives $$200000$$.", "commonMistake": "Thinking the loss cancels the gain exactly.", "correctApproach": "Recognize that the $$10\\%$$ reduction applies to the higher figure ($$220000$$), losing $$22000$$."}, "keyTakeaways": ["Successive percentage changes of equal magnitude ($$+x\\%, -x\\%$$) always produce a net loss of $$\\frac{x^2}{100}\\%$$.", "Converting percentages into fractions enables rapid cancellation with large numbers."], "quickCheckItems": [{"question": "A salary is increased by $$20\\%$$ and then reduced by $$20\\%$$. What is the net percentage change in salary?", "options": ["$$0\\%$$", "$$4\\%$$ decrease", "$$4\\%$$ increase", "$$2\\%$$ decrease"], "correctOptionIndex": 1, "explanation": "$$\\text{Net} = -\\frac{20^2}{100} = -4\\%$$, so there is a $$4\\%$$ decrease."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000003'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PERCENTAGES',
        'TRAPS_AND_PITFALLS',
        'CORE',
        3,
        NULL::uuid,
        4,
        'Exam setters intentionally exploit cognitive illusions surrounding base shifts. In percentage comparison questions (e.g. ''A is $$25\%$$ more than B, by what percentage is B less than A?''), students repeatedly divide by the wrong base denominator. The inverse percentage relation is strictly given by $$\text{Decrease}\% = \left(\frac{x}{100 + x}\right) \times 100\%$$.',
        'Equating ''A is $$x\%$$ more than B'' with ''B is $$x\%$$ less than A''. The reference base shifts from B ($$100$$) to A ($$100 + x$$).',
        '["Inverse Base Reduction: $$\\text{Reduction}\\% = \\left(\\frac{x}{100 + x}\\right) \\times 100\\%$$", "Inverse Base Expansion: $$\\text{Expansion}\\% = \\left(\\frac{x}{100 - x}\\right) \\times 100\\%$$", "Constant Expenditure Invariant: $$\\text{Price} \\times \\text{Consumption} = \\text{Expenditure (Constant)}$$"]',
        '{"questionStem": "If the price of petrol increases by $$25\\%$$, by what percentage must a motorist reduce his consumption so that his expenditure on petrol remains unchanged?", "steps": ["Step 1: Set initial Price $$P_0 = 100$$ and initial Consumption $$C_0 = 100$$, giving constant Expenditure $$E = 100 \\times 100 = 10000$$.", "Step 2: The new Price is $$P_1 = 125$$. To maintain $$E = 10000$$, new Consumption must be $$C_1 = \\frac{10000}{125} = 80$$.", "Step 3: Reduction in consumption is $$100 - 80 = 20$$, which corresponds to a $$20\\%$$ decrease. Alternatively, apply $$\\frac{25}{100 + 25} \\times 100\\% = \\frac{25}{125} \\times 100\\% = 20\\%$$. "], "finalAnswer": "$$\\text{Consumption Reduction} = 20\\%$$", "structuredSteps": [{"stepNumber": 1, "title": "Model Expenditure Relation", "explanation": "Expenditure is the product of price and quantity consumed.", "formula": "$$P \\times C = E$$", "tip": "Assume $$100$$ for baseline values."}, {"stepNumber": 2, "title": "Compute Reciprocal Consumption", "explanation": "Substitute new price $$125$$ into constant expenditure equation.", "formula": "$$C_1 = \\frac{10000}{125} = 80$$", "tip": "Notice $$125 = \\frac{5}{4} \\times 100$$, so $$C_1 = \\frac{4}{5} \\times 100 = 80$$."}, {"stepNumber": 3, "title": "Derive Percentage Reduction", "explanation": "Calculate change relative to original consumption $$100$$.", "formula": "$$\\Delta C = 100 - 80 = 20\\%$$", "tip": "$$1/4$$ increase requires $$1/(4+1) = 1/5 = 20\\%$$ reduction."}], "structuredTrapAlert": {"title": "Reciprocal Percentage Fallacy", "description": "Choosing $$25\\%$$ reduction because price increased by $$25\\%$$.", "commonMistake": "Believing percentage changes are symmetrical in both directions.", "correctApproach": "Always shift the denominator from $$100$$ to $$100 + x$$."}, "keyTakeaways": ["If price increases by $$\\frac{1}{n}$$, consumption must decrease by $$\\frac{1}{n+1}$$ to keep expenditure constant.", "If price decreases by $$\\frac{1}{n}$$, consumption must increase by $$\\frac{1}{n-1}$$ to keep expenditure constant."], "quickCheckItems": [{"question": "If sugar price increases by $$33\\frac{1}{3}\\%$$, by what percentage must a household reduce consumption to maintain constant budget?", "options": ["$$33\\frac{1}{3}\\%$$", "$$25\\%$$", "$$20\\%$$", "$$50\\%$$"], "correctOptionIndex": 1, "explanation": "$$33\\frac{1}{3}\\% = \\frac{1}{3}$$. Reduction $$= \\frac{1}{3+1} = \\frac{1}{4} = 25\\%$$. "}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000004'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PERCENTAGES',
        'CHEAT_SHEET',
        'FOUNDATION',
        4,
        NULL::uuid,
        4,
        'Mastery of high-speed arithmetic depends on instant automaticity in converting common percentages to irreducible vulgar fractions. Converting $$14\frac{2}{7}\%$$ directly to $$\frac{1}{7}$$ or $$16\frac{2}{3}\%$$ to $$\frac{1}{6}$$ saves minutes during competitive exams.',
        'Mistaking recurring fractions for rounded integers, such as treating $$11\frac{1}{9}\%$$ as $$11\%$$ instead of the exact fraction $$\frac{1}{9}$$.',
        '["Halves & Thirds: $$\\frac{1}{2} = 50\\%, \\quad \\frac{1}{3} = 33.\\overline{3}\\% = 33\\frac{1}{3}\\%$$", "Fourths & Fifths: $$\\frac{1}{4} = 25\\%, \\quad \\frac{1}{5} = 20\\%, \\quad \\frac{1}{6} = 16\\frac{2}{3}\\%$$", "Sevenths & Eighths: $$\\frac{1}{7} = 14\\frac{2}{7}\\% \\approx 14.28\\%, \\quad \\frac{1}{8} = 12.5\\%, \\quad \\frac{3}{8} = 37.5\\%$$", "Ninths & Elevenths: $$\\frac{1}{9} = 11\\frac{1}{9}\\% \\approx 11.11\\%, \\quad \\frac{1}{11} = 9\\frac{1}{11}\\% \\approx 9.09\\%$$", "Twelfths & Sixteenths: $$\\frac{1}{12} = 8\\frac{1}{3}\\% \\approx 8.33\\%, \\quad \\frac{1}{16} = 6.25\\%, \\quad \\frac{1}{20} = 5\\%$$"]',
        '{"questionStem": "Evaluate the value of $$37.5\\% \\text{ of } 640 + 62.5\\% \\text{ of } 480$$ without long division or decimals.", "steps": ["Step 1: Convert $$37.5\\%$$ to vulgar fraction: $$37.5\\% = 3 \\times 12.5\\% = \\frac{3}{8}$$.", "Step 2: Convert $$62.5\\%$$ to vulgar fraction: $$62.5\\% = 5 \\times 12.5\\% = \\frac{5}{8}$$.", "Step 3: Compute the terms: $$\\frac{3}{8} \\times 640 = 3 \\times 80 = 240$$, and $$\\frac{5}{8} \\times 480 = 5 \\times 60 = 300$$. Total sum $$= 240 + 300 = 540$$."], "finalAnswer": "$$\\text{Sum} = 540$$", "structuredSteps": [{"stepNumber": 1, "title": "Fraction Conversion for 37.5%", "explanation": "Recognize $$37.5\\%$$ as $$3/8$$.", "formula": "$$37.5\\% = \\frac{3}{8}$$", "tip": "$$12.5\\% = 1/8$$, so $$3 \\times 1/8 = 3/8$$."}, {"stepNumber": 2, "title": "Fraction Conversion for 62.5%", "explanation": "Recognize $$62.5\\%$$ as $$5/8$$.", "formula": "$$62.5\\% = \\frac{5}{8}$$", "tip": "$$5 \\times 1/8 = 5/8$$."}, {"stepNumber": 3, "title": "Evaluate Fast Sum", "explanation": "Perform integer multiplications and add products.", "formula": "$$240 + 300 = 540$$", "tip": "Directly cancel 8 into 640 and 480."}], "keyTakeaways": ["Eighths ($$1/8, 3/8, 5/8, 7/8$$) appear constantly in banking and aptitude tests.", "Memorizing reciprocal fractions eliminates pencil-and-paper multi-digit multiplication."], "quickCheckItems": [{"question": "What is the vulgar fraction equivalent of $$87.5\\%$$?", "options": ["$$\\frac{7}{8}$$", "$$\\frac{5}{6}$$", "$$\\frac{9}{10}$$", "$$\\frac{11}{12}$$"], "correctOptionIndex": 0, "explanation": "$$87.5\\% = 100\\% - 12.5\\% = 1 - \\frac{1}{8} = \\frac{7}{8}$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000005'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PROFIT_LOSS',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Profit and loss calculations quantify commercial transactions relative to the base Cost Price ($$\text{CP}$$), whereas discounts are strictly computed against the Marked Price ($$\text{MP}$$). Imagine visiting a local Indian kirana or textile shop: the merchant marks up goods above purchase cost to accommodate bargaining, then applies a discount while ensuring a profit margin. This equilibrium is bridged by the golden equality $$\text{MP} \times \left(1 - \frac{d\%}{100}\right) = \text{CP} \times \left(1 + \frac{p\%}{100}\right)$$. Both expressions equal the Selling Price ($$\text{SP}$$).',
        'Computing discount percentage on Cost Price rather than Marked Price, or calculating profit percentage on Selling Price rather than Cost Price.',
        '["Profit/Loss Percentage: $$\\text{Profit}\\% = \\left(\\frac{\\text{SP} - \\text{CP}}{\\text{CP}}\\right) \\times 100\\%, \\quad \\text{Loss}\\% = \\left(\\frac{\\text{CP} - \\text{SP}}{\\text{CP}}\\right) \\times 100\\%$$", "Golden Equilibrium: $$\\text{MP} \\times \\left(1 - \\frac{d}{100}\\right) = \\text{CP} \\times \\left(1 + \\frac{p}{100}\\right) = \\text{SP}$$", "Markup Ratio: $$\\frac{\\text{MP}}{\\text{CP}} = \\frac{100 + p\\%}{100 - d\\%}$$"]',
        '{"questionStem": "A retailer purchases a watch for $$\\text{Rs. } 1200$$. At what marked price should he list it so that after allowing a discount of $$10\\%$$, he still earns a profit of $$20\\%$$?", "steps": ["Step 1: Compute required Selling Price to secure a $$20\\%$$ profit on Cost Price ($$\\text{CP} = 1200$$): $$\\text{SP} = 1200 \\times \\left(1 + \\frac{20}{100}\\right) = 1200 \\times 1.20 = \\text{Rs. } 1440$$.", "Step 2: Relate Selling Price to Marked Price under a $$10\\%$$ discount: $$\\text{SP} = \\text{MP} \\times (1 - 0.10) = 0.90 \\times \\text{MP}$$.", "Step 3: Solve for Marked Price: $$\\text{MP} = \\frac{1440}{0.90} = \\text{Rs. } 1600$$. Alternatively, use $$\\frac{\\text{MP}}{\\text{CP}} = \\frac{100 + 20}{100 - 10} = \\frac{120}{90} = \\frac{4}{3} \\implies \\text{MP} = 1200 \\times \\frac{4}{3} = 1600$$."], "finalAnswer": "$$\\text{Marked Price} = \\text{Rs. } 1600$$", "structuredSteps": [{"stepNumber": 1, "title": "Calculate Target Selling Price", "explanation": "Determine SP required for $$20\\%$$ profit on $$1200$$.", "formula": "$$\\text{SP} = 1200 \\times 1.2 = 1440$$", "tip": "Profit is always calculated on CP."}, {"stepNumber": 2, "title": "Incorporate Discount Condition", "explanation": "Express SP in terms of Marked Price and $$10\\%$$ discount.", "formula": "$$\\text{SP} = 0.90 \\times \\text{MP}$$", "tip": "Discount is always computed on MP."}, {"stepNumber": 3, "title": "Solve for Marked Price", "explanation": "Divide SP by $$0.90$$.", "formula": "$$\\text{MP} = \\frac{1440}{0.90} = 1600$$", "tip": "Use ratio $$120/90 = 4/3$$ for mental math."}], "structuredTrapAlert": {"title": "Base Confusion Trap", "description": "Subtracting $$10\\%$$ from $$1440$$ or adding $$30\\%$$ to $$1200$$.", "commonMistake": "Combining markup and discount percentages linearly.", "correctApproach": "Always maintain the triad CP, SP, MP with separate profit and discount bases."}, "keyTakeaways": ["$$\\text{CP} \\to \\text{SP}$$ depends on Profit/Loss %.", "$$\\text{MP} \\to \\text{SP}$$ depends on Discount %.", "The direct formula $$\\frac{\\text{MP}}{\\text{CP}} = \\frac{100+P\\%}{100-D\\%}$$ saves steps."], "quickCheckItems": [{"question": "If an item costing Rs. 800 is marked up by $$25\\%$$ and sold at a $$10\\%$$ discount, what is the selling price?", "options": ["Rs. 900", "Rs. 920", "Rs. 890", "Rs. 950"], "correctOptionIndex": 0, "explanation": "$$\\text{MP} = 800 \\times 1.25 = 1000$$. $$\\text{SP} = 1000 \\times 0.90 = 900$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000006'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_PROFIT_LOSS',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000005'::uuid,
        6,
        'In problems with dishonest shopkeepers who cheat using false weights, profit arises from two distinct mechanisms: selling price markup and false weight undersupply. When a merchant claims to sell at cost price but dispenses a weight of $$W_{\text{false}}$$ instead of $$W_{\text{true}}$$, the true cost incurred by the merchant corresponds to $$W_{\text{false}}$$, yielding an effective profit of $$\text{Gain}\% = \left(\frac{W_{\text{true}} - W_{\text{false}}}{W_{\text{false}}}\right) \times 100\%$$.',
        'Dividing the weight difference by the true weight ($$1000\text{ g}$$) rather than the false weight actually dispensed ($$W_{\text{false}}$$), vastly understating the actual profit percentage.',
        '["False Weight Gain: $$\\text{Gain}\\% = \\left(\\frac{\\text{Error}}{\\text{True Weight} - \\text{Error}}\\right) \\times 100\\%$$", "Composite False Weight Gain: $$\\text{Net Gain}\\% = \\left[\\left(1 + \\frac{m\\%}{100}\\right)\\left(\\frac{W_{\\text{true}}}{W_{\\text{false}}}\\right) - 1\\right] \\times 100\\%$$"]',
        '{"questionStem": "A dishonest grocer professes to sell his pulses at cost price, but he uses a false weight of $$950\\text{ g}$$ for a $$1\\text{ kg}$$ weight. Find his actual profit percentage.", "steps": ["Step 1: Identify the true weight $$W_{\\text{true}} = 1000\\text{ g}$$ and false weight dispensed $$W_{\\text{false}} = 950\\text{ g}$$.", "Step 2: The grocer charges the customer for $$1000\\text{ g}$$ while parting with goods costing him only $$950\\text{ g}$$. The profit is $$1000 - 950 = 50\\text{ g}$$.", "Step 3: Calculate gain percentage relative to grocer''s cost base ($$950\\text{ g}$$): $$\\text{Profit}\\% = \\frac{50}{950} \\times 100\\% = \\frac{1}{19} \\times 100\\% = 5\\frac{5}{19}\\% \\approx 5.26\\%$$. "], "finalAnswer": "$$\\text{Profit} = 5\\frac{5}{19}\\% \\approx 5.26\\%$$", "structuredSteps": [{"stepNumber": 1, "title": "Identify Weight Discrepancy", "explanation": "Determine true weight and weight delivered.", "formula": "$$W_{\\text{true}} = 1000\\text{ g}, \\quad W_{\\text{false}} = 950\\text{ g}$$", "tip": "The error is $$50\\text{ g}$$."}, {"stepNumber": 2, "title": "Establish Grocers Cost Base", "explanation": "The cost incurred by the merchant is the cost of the goods actually handed over.", "formula": "$$\\text{Base Cost} = 950\\text{ g}$$", "tip": "Never divide by $$1000\\text{ g}$$."}, {"stepNumber": 3, "title": "Calculate Percentage Return", "explanation": "Compute ratio of weight saved to weight delivered.", "formula": "$$\\text{Gain}\\% = \\frac{50}{950} \\times 100 = 5\\frac{5}{19}\\%$$", "tip": "$$1/19 \\approx 5.263\\%$$."}], "structuredTrapAlert": {"title": "Denominator Trap in False Weights", "description": "Calculating $$50/1000 = 5\\%$$.", "commonMistake": "Using the customer''s perceived weight ($$1000\\text{ g}$$) as the cost base.", "correctApproach": "Always divide by the actual physical weight dispensed by the seller."}, "keyTakeaways": ["The cost to the seller is always the quantity actually surrendered.", "False weights generate higher profit percentages than the nominal weight discount."], "quickCheckItems": [{"question": "If a dealer uses a $$900\\text{ g}$$ weight instead of $$1\\text{ kg}$$, what is his profit percentage?", "options": ["$$10\\%$$", "$$11\\frac{1}{9}\\%$$", "$$9\\frac{1}{11}\\%$$", "$$12\\%$$"], "correctOptionIndex": 1, "explanation": "$$\\text{Gain}\\% = \\frac{100}{900} \\times 100 = \\frac{1}{9} \\times 100 = 11\\frac{1}{9}\\%$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000007'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_SI',
        'CONCEPT',
        'FOUNDATION',
        1,
        NULL::uuid,
        4,
        'Simple Interest ($$\text{SI}$$) is linear growth where the principal remains strictly fixed across all elapsed periods. The interest accrued each year is constant: $$\text{SI} = \frac{P \times R \times T}{100}$$. If a sum doubles in $$T$$ years under simple interest, the interest accrued equals the principal ($$P$$), meaning the rate is $$R = \frac{100}{T}\%$$.',
        'Forgetting to subtract the original principal when a question asks for interest earned from a given total amount ($$A = P + \text{SI}$$).',
        '["Simple Interest: $$\\text{SI} = \\frac{P \\times R \\times T}{100}$$", "Total Maturity Amount: $$A = P + \\text{SI} = P\\left(1 + \\frac{RT}{100}\\right)$$", "Doubling Period Rule: $$R \\times T = 100 \\quad (\\text{when sum doubles})$$"]',
        '{"questionStem": "A certain sum of money doubles itself in $$8$$ years at simple interest. In how many years will it become $$4$$ times itself at the same rate?", "steps": ["Step 1: When a sum doubles ($$A = 2P$$), the simple interest earned is $$\\text{SI}_1 = 2P - P = P$$. Time taken is $$T_1 = 8\\text{ years}$$.", "Step 2: To become $$4$$ times itself ($$A = 4P$$), the required interest is $$\\text{SI}_2 = 4P - P = 3P$$.", "Step 3: Since simple interest grows linearly with time ($$\\text{SI} \\propto T$$): $$\\frac{T_2}{T_1} = \\frac{\\text{SI}_2}{\\text{SI}_1} = \\frac{3P}{P} = 3$$. Therefore, $$T_2 = 3 \\times 8 = 24\\text{ years}$$."], "finalAnswer": "$$T = 24\\text{ years}$$", "structuredSteps": [{"stepNumber": 1, "title": "Analyze First Condition", "explanation": "Doubling means interest earned equals $$1 \\times P$$ in 8 years.", "formula": "$$\\text{SI}_1 = P, \\quad T_1 = 8$$", "tip": "Interest is $$A - P$$."}, {"stepNumber": 2, "title": "Analyze Second Condition", "explanation": "Becoming 4 times means interest required is $$3 \\times P$$.", "formula": "$$\\text{SI}_2 = 3P$$", "tip": "Notice it is 3 times the interest, not 4 times."}, {"stepNumber": 3, "title": "Scale Proportionally", "explanation": "Multiply 8 years by 3.", "formula": "$$T_2 = 8 \\times 3 = 24\\text{ years}$$", "tip": "Linear scaling applies strictly to simple interest."}], "structuredTrapAlert": {"title": "Geometric Scaling Trap", "description": "Assuming 4 times takes $$8 \\times 2 = 16\\text{ years}$$.", "commonMistake": "Applying compound interest doubling logic ($$2^2 = 4$$) to simple interest.", "correctApproach": "Always subtract $$P$$ to find the interest multiple ($$n-1$$), then multiply."}, "keyTakeaways": ["Under Simple Interest, if a sum becomes $$n$$ times in $$T$$ years, it becomes $$m$$ times in $$\\frac{m-1}{n-1} \\times T$$ years.", "Annual interest remains fixed throughout the loan tenure."], "quickCheckItems": [{"question": "A sum triples itself in 5 years at simple interest. In how many years will it become 5 times itself?", "options": ["$$10\\text{ years}$$", "$$12.5\\text{ years}$$", "$$8\\text{ years}$$", "$$15\\text{ years}$$"], "correctOptionIndex": 0, "explanation": "$$T = \\frac{5-1}{3-1} \\times 5 = \\frac{4}{2} \\times 5 = 10\\text{ years}$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000008'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_CI',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Compound Interest ($$\text{CI}$$) embodies geometric exponential growth where accumulated interest is re-invested into the principal at each conversion epoch. The total amount after $$n$$ years compounded annually at rate $$r\%$$ is $$A = P \left(1 + \frac{r}{100}\right)^n$$. Over two years, the difference between Compound Interest and Simple Interest stems entirely from interest on first-year interest: $$\text{CI}_2 - \text{SI}_2 = P\left(\frac{r}{100}\right)^2$$.',
        'Forgetting to halve the interest rate and double the period when compounding semi-annually ($$r \to r/2$$, $$n \to 2n$$).',
        '["Compound Amount: $$A = P\\left(1 + \\frac{r}{100}\\right)^n$$", "2-Year CI-SI Delta: $$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2$$", "3-Year CI-SI Delta: $$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2\\left(3 + \\frac{r}{100}\\right)$$"]',
        '{"questionStem": "Find the difference between compound interest and simple interest on a principal of $$\\text{Rs. } 10,000$$ for $$2$$ years at $$10\\%$$ per annum compounded annually.", "steps": ["Step 1: Identify given parameters: $$P = 10000$$, $$r = 10\\%$$, and $$n = 2\\text{ years}$$.", "Step 2: Apply the 2-year difference formula: $$\\Delta = P \\left(\\frac{r}{100}\\right)^2$$.", "Step 3: Substitute values: $$\\Delta = 10000 \\times \\left(\\frac{10}{100}\\right)^2 = 10000 \\times \\frac{1}{100} = \\text{Rs. } 100$$. "], "finalAnswer": "$$\\text{CI} - \\text{SI} = \\text{Rs. } 100$$", "structuredSteps": [{"stepNumber": 1, "title": "Parameters Identification", "explanation": "Extract Principal and annual rate.", "formula": "$$P = 10000, \\quad r = 10\\%$$", "tip": "Ensure rate is annual."}, {"stepNumber": 2, "title": "Direct Delta Formula", "explanation": "Use the 2-year difference identity.", "formula": "$$\\Delta = P(r/100)^2$$", "tip": "Saves computing CI and SI separately."}, {"stepNumber": 3, "title": "Compute Difference", "explanation": "Evaluate fraction squared times principal.", "formula": "$$10000 \\times 0.01 = 100$$", "tip": "Corresponds to $$10\\%$$ of Year 1 interest ($$1000$$)."}], "structuredTrapAlert": {"title": "Separation Overhead Trap", "description": "Calculating $$A = 10000(1.1)^2 = 12100$$ then $$\\text{SI} = 2000$$, wasting 2 minutes.", "commonMistake": "Performing full arithmetic calculation of both CI and SI.", "correctApproach": "Directly evaluate $$P(r/100)^2$$ in under 5 seconds."}, "keyTakeaways": ["For Year 1, CI and SI are identical (when compounded annually).", "The 2-year difference is precisely the interest earned on Year 1 interest."], "quickCheckItems": [{"question": "What is the difference between CI and SI on Rs. 2500 for 2 years at 4% per annum?", "options": ["Rs. 4", "Rs. 2", "Rs. 5", "Rs. 8"], "correctOptionIndex": 0, "explanation": "$$\\Delta = 2500 \\times (4/100)^2 = 2500 \\times \\frac{16}{10000} = \\frac{2500}{10000} \\times 16 = \\frac{16}{4} = 4$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000009'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_CI',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000008'::uuid,
        6,
        'For 3-year investment horizons, the delta between Compound Interest and Simple Interest incorporates three identical 2-year interest components plus the interest on interest on interest: $$\text{CI}_3 - \text{SI}_3 = P\left(\frac{r}{100}\right)^2 \left(3 + \frac{r}{100}\right)$$. Memorizing this structure avoids solving cumbersome binomial cubes.',
        'Applying the 2-year difference formula to a 3-year problem, neglecting the third-year compounding cross-terms.',
        '["3-Year Delta: $$\\text{CI}_3 - \\text{SI}_3 = P\\left(\\frac{r}{100}\\right)^2 \\left(3 + \\frac{r}{100}\\right)$$", "Delta Ratio: $$\\frac{\\text{CI}_3 - \\text{SI}_3}{\\text{CI}_2 - \\text{SI}_2} = 3 + \\frac{r}{100}$$"]',
        '{"questionStem": "The difference between compound interest and simple interest on a certain sum of money for $$3$$ years at $$10\\%$$ per annum is $$\\text{Rs. } 155$$. Determine the sum of money.", "steps": ["Step 1: Set up the 3-year difference identity: $$\\Delta_3 = P\\left(\\frac{r}{100}\\right)^2 \\left(3 + \\frac{r}{100}\\right)$$.", "Step 2: Substitute $$r = 10\\%$$: $$\\left(\\frac{10}{100}\\right)^2 = \\frac{1}{100}$$, and $$\\left(3 + \\frac{10}{100}\\right) = \\frac{31}{10}$$.", "Step 3: Solve for $$P$$: $$155 = P \\times \\frac{1}{100} \\times \\frac{31}{10} = P \\times \\frac{31}{1000} \\implies P = \\frac{155 \\times 1000}{31} = 5 \\times 1000 = \\text{Rs. } 5000$$."], "finalAnswer": "$$P = \\text{Rs. } 5000$$", "structuredSteps": [{"stepNumber": 1, "title": "Recall 3-Year Delta Identity", "explanation": "Write down the 3-year CI-SI delta equation.", "formula": "$$\\Delta_3 = P(r/100)^2 (3 + r/100)$$", "tip": "Notice the factor $$(3 + r/100)$$."}, {"stepNumber": 2, "title": "Substitute Rate r=10%", "explanation": "Calculate the coefficient of P.", "formula": "$$\\frac{1}{100} \\times \\frac{31}{10} = \\frac{31}{1000}$$", "tip": "Notice that 31 divides 155 cleanly ($$31 \\times 5 = 155$$)."}, {"stepNumber": 3, "title": "Extract Principal", "explanation": "Multiply 5 by 1000.", "formula": "$$P = 5000$$", "tip": "Standard exam numbers always have clean factors."}], "keyTakeaways": ["The ratio $$\\frac{\\Delta_3}{\\Delta_2} = 3 + \\frac{r}{100}$$ allows finding rate $$r$$ directly when both deltas are given.", "Exam questions frequently feature $$10\\%$$ and $$5\\%$$ rates."], "quickCheckItems": [{"question": "If the difference between CI and SI for 2 years is Rs. 50 and for 3 years is Rs. 155, what is the interest rate?", "options": ["$$10\\%$$", "$$5\\%$$", "$$12\\%$$", "$$15\\%$$"], "correctOptionIndex": 0, "explanation": "$$\\frac{\\Delta_3}{\\Delta_2} = \\frac{155}{50} = 3.10 = 3 + \\frac{r}{100} \\implies r = 10\\%$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000010'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_TIME_WORK',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Time and Work problems are best solved using the LCM Work Unit Method rather than cumbersome fractions. Take the LCM of individual completion days as the Total Work units. The daily efficiency of each worker is simply $$\text{Efficiency} = \frac{\text{Total Work}}{\text{Days}}$$. Combined completion time is then $$\text{Time} = \frac{\text{Total Work}}{\sum \text{Efficiencies}}$$. Negative work occurs when pipes drain cisterns or obstacles destroy progress.',
        'Adding working days directly (e.g. if A takes 10 days and B takes 15 days, assuming together they take $$10+15 = 25\text{ days}$$ or $$(10+15)/2 = 12.5\text{ days}$$). More workers must take LESS time.',
        '["Work-Efficiency Law: $$\\text{Work} = \\text{Efficiency} \\times \\text{Time}$$", "Combined Rate: $$\\text{Time Together} = \\frac{\\text{LCM}(\\text{Days})}{\\sum \\text{Daily Efficiencies}}$$", "Chain Rule: $$M_1 D_1 H_1 E_1 W_2 = M_2 D_2 H_2 E_2 W_1$$"]',
        '{"questionStem": "A can complete a piece of work in $$12\\text{ days}$$, while B can complete the same work in $$18\\text{ days}$$. If they work together, in how many days will the work be completed?", "steps": ["Step 1: Compute the Total Work by taking the LCM of individual completion times: $$\\text{Total Work} = \\text{LCM}(12, 18) = 36\\text{ units}$$.", "Step 2: Calculate daily efficiencies: Efficiency of A is $$E_A = \\frac{36}{12} = 3\\text{ units/day}$$; Efficiency of B is $$E_B = \\frac{36}{18} = 2\\text{ units/day}$$.", "Step 3: Combined efficiency is $$E_A + E_B = 3 + 2 = 5\\text{ units/day}$$. Time taken together is $$T = \\frac{\\text{Total Work}}{\\text{Combined Efficiency}} = \\frac{36}{5} = 7.2\\text{ days} = 7\\frac{1}{5}\\text{ days}$$."], "finalAnswer": "$$T = 7.2\\text{ days} = 7\\frac{1}{5}\\text{ days}$$", "structuredSteps": [{"stepNumber": 1, "title": "Determine LCM Units", "explanation": "Find common multiple of 12 and 18.", "formula": "$$\\text{Total Work} = \\text{LCM}(12, 18) = 36\\text{ units}$$", "tip": "Choose 36 instead of fractions."}, {"stepNumber": 2, "title": "Calculate Daily Rates", "explanation": "Divide work by days for each worker.", "formula": "$$E_A = 3, \\quad E_B = 2$$", "tip": "A is $$1.5$$ times as efficient as B."}, {"stepNumber": 3, "title": "Calculate Completion Time", "explanation": "Divide 36 units by sum of efficiencies (5 units/day).", "formula": "$$T = \\frac{36}{5} = 7.2\\text{ days}$$", "tip": "Notice $$7.2 < 12$$, always check that combined time is less than fastest worker."}], "structuredTrapAlert": {"title": "Averaging Days Fallacy", "description": "Averaging 12 and 18 to get 15 days.", "commonMistake": "Averaging time rather than combining rates.", "correctApproach": "Always combine efficiencies: working together must be faster than the fastest worker."}, "keyTakeaways": ["The LCM method eliminates fractions entirely.", "Pipes and cisterns operate on the exact same principles, with drainage pipes having negative efficiency."], "quickCheckItems": [{"question": "If pipe A fills a tank in 10 hours and pipe B empties it in 15 hours, how long will it take to fill the tank if both are opened?", "options": ["$$30\\text{ hours}$$", "$$25\\text{ hours}$$", "$$12\\text{ hours}$$", "$$6\\text{ hours}$$"], "correctOptionIndex": 0, "explanation": "LCM = 30. Rate A = +3, Rate B = -2. Net Rate = +1. Time = 30/1 = 30 hours."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000011'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_TIME_WORK',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000010'::uuid,
        6,
        'In alternating day work schedules, workers do not work simultaneously; instead, each works for a complete 1-day shift in rotation. The solution requires bundling the alternating shifts into complete multi-day cycles, calculating cycle work output, and handling the remaining fractional units with the worker on duty.',
        'Dividing total work directly by combined efficiency without accounting for discrete shifts, leading to incorrect fractional days.',
        '["Cycle Efficiency: $$E_{\\text{cycle}} = \\sum_{i=1}^k E_i \\quad (k\\text{-day cycle})$$", "Remaining Work: $$W_{\\text{rem}} = W_{\\text{total}} - n \\times E_{\\text{cycle}}$$"]',
        '{"questionStem": "A can do a work in $$16\\text{ days}$$ and B in $$12\\text{ days}$$. Starting with A, they work on alternate days. In how many days will the entire work be finished?", "steps": ["Step 1: Total Work = $$\\text{LCM}(16, 12) = 48\\text{ units}$$. Daily efficiencies: $$E_A = 3\\text{ units/day}$$, $$E_B = 4\\text{ units/day}$$.", "Step 2: A 2-day cycle (Day 1: A, Day 2: B) delivers $$E_{\\text{cycle}} = 3 + 4 = 7\\text{ units}$$. In 6 complete cycles ($$12\\text{ days}$$), work done is $$6 \\times 7 = 42\\text{ units}$$.", "Step 3: Remaining work is $$48 - 42 = 6\\text{ units}$$. On Day 13, A works and completes $$3\\text{ units}$$, leaving $$6 - 3 = 3\\text{ units}$$. On Day 14, B takes over and requires $$\\frac{3}{4}\\text{ day}$$. Total time $$= 12 + 1 + \\frac{3}{4} = 13\\frac{3}{4}\\text{ days}$$."], "finalAnswer": "$$T = 13\\frac{3}{4}\\text{ days} = 13.75\\text{ days}$$", "structuredSteps": [{"stepNumber": 1, "title": "Determine LCM and Efficiencies", "explanation": "LCM(16, 12) is 48.", "formula": "$$E_A = 3, \\quad E_B = 4$$", "tip": "Keep units clean."}, {"stepNumber": 2, "title": "Compute Full 2-Day Cycles", "explanation": "Find maximum full cycles without exceeding 48.", "formula": "$$6 \\times 7 = 42\\text{ units in } 12\\text{ days}$$", "tip": "Next cycle would exceed 48."}, {"stepNumber": 3, "title": "Resolve Remaining Units", "explanation": "Allocate remaining 6 units sequentially to A then B.", "formula": "$$1\\text{ day (A, 3 units)} + \\frac{3}{4}\\text{ day (B, 3 units)}$$", "tip": "Total is $$12 + 1 + 0.75 = 13.75$$."}], "keyTakeaways": ["Never divide total work directly by combined efficiency for alternate work problems.", "Always identify which specific worker begins the remainder phase."], "quickCheckItems": [{"question": "If A completes 2 units/day and B completes 3 units/day, how many units are finished in 4 days of alternate work starting with A?", "options": ["10 units", "12 units", "8 units", "9 units"], "correctOptionIndex": 0, "explanation": "2 full 2-day cycles: $$2 \\times (2 + 3) = 10\\text{ units}$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000012'::uuid,
        'MATH',
        'ARITH_OPS',
        'ARITH_TIME_DIST',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Motion problems hinge on the invariance between speed, distance, and time ($$\text{Distance} = \text{Speed} \times \text{Time}$$). When converting units, remember that $$1\text{ km/h} = \frac{5}{18}\text{ m/s}$$. When two objects move relative to each other, their relative speed is $$u + v$$ if moving in opposite directions, and $$|u - v|$$ if moving in the same direction. When two equal distances are traversed at speeds $$u$$ and $$v$$, the harmonic average speed is $$\text{Average Speed} = \frac{2uv}{u + v}$$.',
        'Averaging speeds arithmetically (e.g. calculating $$\frac{u+v}{2}$$ instead of harmonic average $$\frac{2uv}{u+v}$$ when distances are identical).',
        '["Unit Conversion: $$1\\text{ km/h} = \\frac{5}{18}\\text{ m/s}, \\quad 1\\text{ m/s} = \\frac{18}{5}\\text{ km/h}$$", "Relative Speed: $$S_{\\text{rel}} = u + v \\quad (\\text{opposite}), \\quad S_{\\text{rel}} = |u - v| \\quad (\\text{same})$$", "Harmonic Average Speed: $$S_{\\text{avg}} = \\frac{2uv}{u + v}$$", "Train Crossing Station: $$\\text{Distance} = L_{\\text{train}} + L_{\\text{platform}}$$"]',
        '{"questionStem": "A train of length $$250\\text{ m}$$ traveling at a speed of $$72\\text{ km/h}$$ crosses a railway platform of length $$350\\text{ m}$$. Find the time taken by the train to cross the platform completely.", "steps": ["Step 1: Convert the train''s speed from $$\\text{km/h}$$ to $$\\text{m/s}$$: $$v = 72 \\times \\frac{5}{18} = 4 \\times 5 = 20\\text{ m/s}$$.", "Step 2: Determine total distance to be covered to clear the platform: $$D = L_{\\text{train}} + L_{\\text{platform}} = 250 + 350 = 600\\text{ m}$$.", "Step 3: Calculate crossing time: $$T = \\frac{\\text{Distance}}{\\text{Speed}} = \\frac{600}{20} = 30\\text{ seconds}$$."], "finalAnswer": "$$T = 30\\text{ seconds}$$", "structuredSteps": [{"stepNumber": 1, "title": "Speed Conversion to SI Units", "explanation": "Multiply 72 by 5/18.", "formula": "$$v = 72 \\times \\frac{5}{18} = 20\\text{ m/s}$$", "tip": "Always convert units before adding lengths."}, {"stepNumber": 2, "title": "Sum Effective Lengths", "explanation": "To completely clear the platform, the train must travel its own length plus platform length.", "formula": "$$D = 250 + 350 = 600\\text{ m}$$", "tip": "If crossing a pole or person, distance is only train length."}, {"stepNumber": 3, "title": "Calculate Duration", "explanation": "Divide 600m by 20m/s.", "formula": "$$T = \\frac{600}{20} = 30\\text{ s}$$", "tip": "Instant mental math once converted."}], "structuredTrapAlert": {"title": "Platform Neglect Trap", "description": "Dividing only train length 250m by 20m/s to get 12.5 seconds.", "commonMistake": "Ignoring the physical extent of the stationary object being crossed.", "correctApproach": "Always add the length of both train and platform."}, "keyTakeaways": ["Crossing stationary objects with length (platform, bridge, tunnel): $$D = L_{\\text{train}} + L_{\\text{object}}$$.", "Crossing stationary objects without length (pole, tree, standing person): $$D = L_{\\text{train}}$$."], "quickCheckItems": [{"question": "A motorist drives from city A to city B at 60 km/h and returns at 40 km/h. What is his average speed for the round trip?", "options": ["$$48\\text{ km/h}$$", "$$50\\text{ km/h}$$", "$$52\\text{ km/h}$$", "$$45\\text{ km/h}$$"], "correctOptionIndex": 0, "explanation": "$$S_{\\text{avg}} = \\frac{2 \\times 60 \\times 40}{60 + 40} = \\frac{4800}{100} = 48\\text{ km/h}$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000013'::uuid,
        'MATH',
        'ALGEBRA',
        'ALG_QUADRATIC',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'For any standard quadratic equation $$ax^2 + bx + c = 0$$ ($$a \neq 0$$), the roots are dictated by the discriminant $$\Delta = b^2 - 4ac$$. If $$\Delta > 0$$, roots are real and distinct; if $$\Delta = 0$$, roots are real and coincident; if $$\Delta < 0$$, roots form complex conjugate pairs. Vieta''s formulas link roots directly to coefficients: sum of roots $$\alpha + \beta = -\frac{b}{a}$$ and product of roots $$\alpha \beta = \frac{c}{a}$$.',
        'Sign errors in $$\alpha + \beta = -b/a$$ when $$b$$ is already negative, leading to wrong sign for root sum.',
        '["Roots Formula: $$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$", "Discriminant: $$\\Delta = b^2 - 4ac$$", "Vietas Relations: $$\\alpha + \\beta = -\\frac{b}{a}, \\quad \\alpha \\beta = \\frac{c}{a}$$", "Symmetric Identity: $$\\alpha^2 + \\beta^2 = (\\alpha + \\beta)^2 - 2\\alpha\\beta$$"]',
        '{"questionStem": "If $$\\alpha$$ and $$\\beta$$ are the roots of the quadratic equation $$2x^2 - 8x + 5 = 0$$, find the value of $$\\alpha^2 + \\beta^2$$.", "steps": ["Step 1: Identify coefficients from $$ax^2 + bx + c = 0$$: $$a = 2, b = -8, c = 5$$.", "Step 2: Apply Vieta''s formulas: $$\\alpha + \\beta = -\\frac{-8}{2} = 4$$, and $$\\alpha\\beta = \\frac{5}{2}$$.", "Step 3: Use the symmetric identity: $$\\alpha^2 + \\beta^2 = (\\alpha + \\beta)^2 - 2\\alpha\\beta = (4)^2 - 2\\left(\\frac{5}{2}\\right) = 16 - 5 = 11$$."], "finalAnswer": "$$\\alpha^2 + \\beta^2 = 11$$", "structuredSteps": [{"stepNumber": 1, "title": "Extract Equation Parameters", "explanation": "Determine coefficients a, b, and c.", "formula": "$$a=2, \\quad b=-8, \\quad c=5$$", "tip": "Watch the negative sign on b."}, {"stepNumber": 2, "title": "Compute Vieta Invariants", "explanation": "Evaluate sum and product of roots.", "formula": "$$\\alpha+\\beta = 4, \\quad \\alpha\\beta = 2.5$$", "tip": "Notice $$-(-8)/2 = +4$$."}, {"stepNumber": 3, "title": "Apply Algebraic Decomposition", "explanation": "Substitute into $$\\alpha^2+\\beta^2 = (\\alpha+\\beta)^2 - 2\\alpha\\beta$$.", "formula": "$$16 - 5 = 11$$", "tip": "Never solve for individual roots when evaluating symmetric forms."}], "structuredTrapAlert": {"title": "Individual Root Solving Trap", "description": "Attempting to find $$x = \\frac{8 \\pm \\sqrt{24}}{4}$$ and squaring messy surds.", "commonMistake": "Calculating irrational roots explicitly.", "correctApproach": "Always evaluate symmetric expressions using Vieta''s sum and product."}, "keyTakeaways": ["Symmetric expressions in roots can always be expressed purely in terms of $$\\alpha + \\beta$$ and $$\\alpha\\beta$$.", "$$\\alpha^3 + \\beta^3 = (\\alpha + \\beta)^3 - 3\\alpha\\beta(\\alpha + \\beta)$$."], "quickCheckItems": [{"question": "What is the nature of roots for $$3x^2 - 5x + 3 = 0$$?", "options": ["Real and distinct", "Real and equal", "Non-real complex conjugates", "Rational"], "correctOptionIndex": 2, "explanation": "$$\\Delta = (-5)^2 - 4(3)(3) = 25 - 36 = -11 < 0$$, so roots are complex conjugates."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000014'::uuid,
        'MATH',
        'ALGEBRA',
        'ALG_QUADRATIC',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000013'::uuid,
        6,
        'To construct a new quadratic equation whose roots are functions of the original roots (e.g. roots are $$\frac{\alpha}{\beta}$$ and $$\frac{\beta}{\alpha}$$), calculate the new sum $$S$$ and new product $$P$$, and apply the monic quadratic structure $$x^2 - Sx + P = 0$$.',
        'Mixing up the negative sign in the monic formation: writing $$x^2 + Sx + P = 0$$ instead of $$x^2 - Sx + P = 0$$.',
        '["Monic Equation Formation: $$x^2 - Sx + P = 0$$", "New Sum: $$S = \\frac{\\alpha}{\\beta} + \\frac{\\beta}{\\alpha} = \\frac{\\alpha^2 + \\beta^2}{\\alpha\\beta}$$", "New Product: $$P = \\frac{\\alpha}{\\beta} \\times \\frac{\\beta}{\\alpha} = 1$$"]',
        '{"questionStem": "If $$\\alpha, \\beta$$ are roots of $$x^2 - 4x + 2 = 0$$, construct the quadratic equation whose roots are $$\\frac{\\alpha}{\\beta}$$ and $$\\frac{\\beta}{\\alpha}$$.", "steps": ["Step 1: From the original equation $$x^2 - 4x + 2 = 0$$, we have $$\\alpha + \\beta = 4$$ and $$\\alpha\\beta = 2$$.", "Step 2: Find the new sum of roots: $$S = \\frac{\\alpha}{\\beta} + \\frac{\\beta}{\\alpha} = \\frac{\\alpha^2 + \\beta^2}{\\alpha\\beta} = \\frac{(\\alpha+\\beta)^2 - 2\\alpha\\beta}{\\alpha\\beta} = \\frac{4^2 - 2(2)}{2} = \\frac{16 - 4}{2} = 6$$.", "Step 3: Find the new product: $$P = \\frac{\\alpha}{\\beta} \\times \\frac{\\beta}{\\alpha} = 1$$. The required equation is $$x^2 - Sx + P = 0 \\implies x^2 - 6x + 1 = 0$$."], "finalAnswer": "$$x^2 - 6x + 1 = 0$$", "structuredSteps": [{"stepNumber": 1, "title": "Extract Base Vieta Terms", "explanation": "Get sum and product of alpha and beta.", "formula": "$$\\alpha+\\beta = 4, \\quad \\alpha\\beta = 2$$", "tip": "Keep original parameters ready."}, {"stepNumber": 2, "title": "Compute Transformed Sum", "explanation": "Express new sum algebraically.", "formula": "$$S = \\frac{12}{2} = 6$$", "tip": "Notice new product is identically 1."}, {"stepNumber": 3, "title": "Form Monic Equation", "explanation": "Substitute S and P into $$x^2 - Sx + P = 0$$.", "formula": "$$x^2 - 6x + 1 = 0$$", "tip": "Always check leading coefficient is positive."}], "keyTakeaways": ["Any quadratic equation is uniquely determined by $$x^2 - (\\text{Sum})x + (\\text{Product}) = 0$$.", "Reciprocal pairs $$\\alpha/\\beta$$ and $$\\beta/\\alpha$$ always have product $$P = 1$$."], "quickCheckItems": [{"question": "What is the quadratic equation whose roots are 3 and 5?", "options": ["$$x^2 - 8x + 15 = 0$$", "$$x^2 + 8x + 15 = 0$$", "$$x^2 - 8x - 15 = 0$$", "$$x^2 - 15x + 8 = 0$$"], "correctOptionIndex": 0, "explanation": "$$S = 3 + 5 = 8, \\quad P = 3 \\times 5 = 15 \\implies x^2 - 8x + 15 = 0$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000015'::uuid,
        'MATH',
        'ALGEBRA',
        'ALG_IDENTITIES',
        'CONCEPT',
        'FOUNDATION',
        1,
        NULL::uuid,
        4,
        'Standard polynomial identities are tools for non-computational factoring. The master 3-variable cubic identity is $$a^3 + b^3 + c^3 - 3abc = (a + b + c)(a^2 + b^2 + c^2 - ab - bc - ca)$$. A crucial exam corollary: if $$a + b + c = 0$$, then immediately $$a^3 + b^3 + c^3 = 3abc$$.',
        'Attempting to manually cube large numbers when their algebraic sum is zero.',
        '["Master Cubic Identity: $$a^3 + b^3 + c^3 - 3abc = \\frac{1}{2}(a+b+c)[(a-b)^2 + (b-c)^2 + (c-a)^2]$$", "Zero-Sum Corollary: $$\\text{If } a + b + c = 0 \\implies a^3 + b^3 + c^3 = 3abc$$", "Reciprocal Power Shift: $$x + \\frac{1}{x} = k \\implies x^2 + \\frac{1}{x^2} = k^2 - 2, \\quad x^3 + \\frac{1}{x^3} = k^3 - 3k$$"]',
        '{"questionStem": "Without calculating cubes, evaluate the numerical value of $$(28)^3 + (-15)^3 + (-13)^3$$.", "steps": ["Step 1: Let $$a = 28$$, $$b = -15$$, and $$c = -13$$.", "Step 2: Test the zero-sum condition: $$a + b + c = 28 + (-15) + (-13) = 28 - 28 = 0$$.", "Step 3: Since $$a + b + c = 0$$, apply $$a^3 + b^3 + c^3 = 3abc$$: $$\\text{Value} = 3(28)(-15)(-13) = 3 \\times 28 \\times 195 = 84 \\times 195 = 16,380$$."], "finalAnswer": "$$16,380$$", "structuredSteps": [{"stepNumber": 1, "title": "Check Zero Sum Condition", "explanation": "Add the three base numbers.", "formula": "$$28 - 15 - 13 = 0$$", "tip": "Condition $$a+b+c=0$$ is satisfied."}, {"stepNumber": 2, "title": "Invoke Corollary", "explanation": "Replace sum of cubes by 3abc.", "formula": "$$a^3 + b^3 + c^3 = 3abc$$", "tip": "Saves calculating $$28^3$$ ($$21952$$)."}, {"stepNumber": 3, "title": "Multiply Factors", "explanation": "Evaluate $$3 \\times 28 \\times (-15) \\times (-13)$$.", "formula": "$$84 \\times 195 = 16380$$", "tip": "Negative times negative yields positive."}], "keyTakeaways": ["Always test if base numbers sum to zero when evaluating a sum of three cubes.", "If $$x + 1/x = 3$$, then $$x^2 + 1/x^2 = 7$$ and $$x^3 + 1/x^3 = 18$$."], "quickCheckItems": [{"question": "If $$x + \\frac{1}{x} = 4$$, what is $$x^3 + \\frac{1}{x^3}$$?", "options": ["$$52$$", "$$64$$", "$$60$$", "$$48$$"], "correctOptionIndex": 0, "explanation": "$$x^3 + \\frac{1}{x^3} = k^3 - 3k = 4^3 - 3(4) = 64 - 12 = 52$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000016'::uuid,
        'MATH',
        'GEOMETRY',
        'GEOM_TRIANGLES',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'A triangle''s geometric centres possess distinct concurrency properties: the Centroid ($$G$$) divides medians in a $$2:1$$ ratio; the Incentre ($$I$$) is equidistant from all sides with angle $$\angle BIC = 90^\circ + \frac{\angle A}{2}$$; the Circumcentre ($$O$$) satisfies $$\angle BOC = 2\angle A$$; and the Orthocentre ($$H$$) satisfies $$\angle BHC = 180^\circ - \angle A$$. For right-angled triangles, the circumcentre lies precisely at the midpoint of the hypotenuse ($$R = \frac{c}{2}$$), while the orthocentre coincides with the vertex of the right angle.',
        'Confusing the incentre angle ($$90^\circ + A/2$$) with the circumcentre angle ($$2A$$).',
        '["Incentre Angle: $$\\angle BIC = 90^\\circ + \\frac{\\angle A}{2}$$", "Circumcentre Angle: $$\\angle BOC = 2\\angle A$$", "Orthocentre Angle: $$\\angle BHC = 180^\\circ - \\angle A$$", "Euler Line Relation: $$HG : GO = 2 : 1$$", "Centroid Coordinates: $$G = \\left(\\frac{x_1+x_2+x_3}{3}, \\frac{y_1+y_2+y_3}{3}\\right)$$"]',
        '{"questionStem": "In $$\\triangle ABC$$, the angle at vertex A is $$\\angle A = 70^\\circ$$. If $$I$$ is the incentre of $$\\triangle ABC$$, find the measure of $$\\angle BIC$$.", "steps": ["Step 1: Recall that the incentre is the intersection point of the interior angle bisectors.", "Step 2: In $$\\triangle BIC$$, the base angles are $$\\frac{\\angle B}{2}$$ and $$\\frac{\\angle C}{2}$$. Therefore, $$\\angle BIC = 180^\\circ - \\left(\\frac{\\angle B + \\angle C}{2}\\right)$$.", "Step 3: Since $$\\angle B + \\angle C = 180^\\circ - \\angle A$$, substitute to get $$\\angle BIC = 90^\\circ + \\frac{\\angle A}{2} = 90^\\circ + \\frac{70^\\circ}{2} = 90^\\circ + 35^\\circ = 125^\\circ$$."], "finalAnswer": "$$\\angle BIC = 125^\\circ$$", "structuredSteps": [{"stepNumber": 1, "title": "Identify Incentre Property", "explanation": "Incentre is angle bisector intersection.", "formula": "$$\\angle BIC = 90^\\circ + \\frac{\\angle A}{2}$$", "tip": "Standard geometry theorem."}, {"stepNumber": 2, "title": "Halve Vertex Angle", "explanation": "Divide $$70^\\circ$$ by 2 to get $$35^\\circ$$.", "formula": "$$\\frac{70^\\circ}{2} = 35^\\circ$$", "tip": "Always acute."}, {"stepNumber": 3, "title": "Add to 90 degrees", "explanation": "Sum $$90^\\circ + 35^\\circ = 125^\\circ$$.", "formula": "$$90^\\circ + 35^\\circ = 125^\\circ$$", "tip": "Always obtuse for acute-angled triangle."}], "keyTakeaways": ["Incentre angle is always $$90^\\circ + A/2$$.", "Excentre angle opposite A is $$90^\\circ - A/2$$."], "quickCheckItems": [{"question": "If circumcentre of triangle ABC is O and angle A is 50 degrees, what is angle BOC?", "options": ["$$100^\\circ$$", "$$115^\\circ$$", "$$130^\\circ$$", "$$65^\\circ$$"], "correctOptionIndex": 0, "explanation": "At circumcentre, angle subtended at centre is twice angle at vertex: $$\\angle BOC = 2\\angle A = 2 \\times 50^\\circ = 100^\\circ$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000017'::uuid,
        'REAS',
        'SYLLOGISTIC_REASONING',
        'SYLL_STANDARD',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Modern banking syllogisms introduce strict exclusivity qualifiers that differ from classical logic. ''Only a few A are B'' is a dual-clause compound statement: it means simultaneously (1) ''Some A are B'' AND (2) ''Some A are NOT B''. Therefore, ''All A can never be B'' is a 100% definite truth! Meanwhile, ''All B can be A'' remains a valid possibility unless contradicted by another constraint.',
        'Treating ''Only a few A are B'' as identical to simple ''Some A are B''. In standard ''Some A are B'', ''All A are B'' is a possibility; in ''Only a few A are B'', ''All A are B'' is strictly IMPOSSIBLE.',
        '["Only A Few Equivalence: $$\\text{Only a few A are B} \\equiv (\\text{Some A are B}) \\land (\\text{Some A are not B})$$", "Definite Negation: $$\\text{All A being B is a possibility} \\implies \\text{DEFINITELY FALSE}$$", "Exclusive Only: $$\\text{Only A is B} \\equiv \\text{All B are A (and B cannot intersect any other set)}$$", "Possibility Rule: \\text{If a basic conclusion is not definite, its possibility is true unless blocked by negative relation}"]',
        '{"questionStem": "Statements:\n1. Only a few metals are conductors.\n2. All conductors are solids.\n\nConclusions:\nI. All metals can never be conductors.\nII. All solids being metals is a possibility.", "steps": ["Step 1: Deconstruct Statement 1: ''Only a few metals are conductors'' implies that while some metals are conductors, some metals definitely CANNOT be conductors.", "Step 2: Evaluate Conclusion I: Since some metals can never enter the conductor set, ''All metals being conductors'' is impossible. Thus, ''All metals can never be conductors'' is a DEFINITE TRUE conclusion.", "Step 3: Evaluate Conclusion II: All conductors are inside solids. Can all solids fit inside metals? Since ''Only a few'' restricts metals from fully entering conductors, but does NOT prevent solids from being enclosed within metals, this possibility is valid. Both Conclusion I and II follow."], "finalAnswer": "$$\\text{Both Conclusions I and II follow}$$", "structuredSteps": [{"stepNumber": 1, "title": "Deconstruct Qualifier", "explanation": "Break ''only a few'' into two distinct Venn assertions.", "formula": "$$\\text{Metals} \\cap \\text{Conductors} \\neq \\emptyset, \\quad \\text{Metals} \\setminus \\text{Conductors} \\neq \\emptyset$$", "tip": "Metals have an exclusionary sector."}, {"stepNumber": 2, "title": "Verify Definitive Boundary", "explanation": "Assess universal positive possibility.", "formula": "$$\\text{All Metals } \\subset \\text{ Conductors} \\implies \\text{IMPOSSIBLE}$$", "tip": "Conclusion I is therefore true."}, {"stepNumber": 3, "title": "Test Reverse Possibility", "explanation": "Draw outer boundary allowing solids to be subset of metals.", "formula": "$$\\text{Solids } \\subseteq \\text{ Metals is possible}$$", "tip": "Conclusion II is a valid possibility."}], "structuredTrapAlert": {"title": "Reverse Qualifier Trap", "description": "Assuming ''All solids being metals'' is blocked.", "commonMistake": "Thinking ''Only a few'' applies symmetrically to both sets.", "correctApproach": "''Only a few A are B'' limits A''s movement into B, but does not limit B''s or solid''s movement into A."}, "keyTakeaways": ["''Only a few'' creates an asymmetric directional restriction.", "''Only A is B'' converts to ''All B are A'' with an exclusive lock on B."], "quickCheckItems": [{"question": "If ''Only a few pens are pencils'', which of the following is definitely true?", "options": ["All pens are pencils", "Some pens are not pencils", "No pen is a pencil", "All pencils are pens"], "correctOptionIndex": 1, "explanation": "''Only a few pens are pencils'' explicitly asserts that ''Some pens are not pencils''."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000018'::uuid,
        'REAS',
        'SYLLOGISTIC_REASONING',
        'SYLL_STANDARD',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000017'::uuid,
        6,
        'In 3-premise categorical syllogisms involving negative qualifiers (''No A is B'') and universal inclusions (''All B are C''), transitive negative boundaries must be tracked. If no A is B, and all B are C, then C has a portion (namely B) that can never intersect A, meaning ''Some C are not A'' is always definitely true.',
        'Assuming that because No A is B and All B are C, No A is C. Only the B-portion of C is blocked from A; the remainder of C can freely intersect A.',
        '["Restricted Intersection: $$\\text{No A is B} \\land \\text{All B are C} \\implies \\text{Some C are not A}$$", "Complementary Pair Condition: \\text{Either-Or requires: (1) Same subject-predicate, (2) Both false individually, (3) Pair is (Some + No) or (All + Some Not)}"]',
        '{"questionStem": "Statements:\n1. All rings are gold.\n2. No gold is silver.\n3. Some silver are diamonds.\n\nConclusions:\nI. No ring is silver.\nII. Some diamonds are not gold.", "steps": ["Step 1: Map the negative barrier: ''No gold is silver'' means the Gold circle and Silver circle are strictly disjoint ($$\\text{Gold} \\cap \\text{Silver} = \\emptyset$$).", "Step 2: Since all rings are inside gold, rings are completely enclosed within the gold circle. Because the entire gold circle cannot touch silver, no ring can ever touch silver. Conclusion I (''No ring is silver'') is DEFINITELY TRUE.", "Step 3: From statement 3, some diamonds are silver. Those particular diamonds that are silver belong to the silver set, which cannot touch gold. Therefore, those diamonds can never be gold. Conclusion II (''Some diamonds are not gold'') is DEFINITELY TRUE. Both conclusions follow."], "finalAnswer": "$$\\text{Both Conclusions I and II follow}$$", "structuredSteps": [{"stepNumber": 1, "title": "Disjoint Sets Isolation", "explanation": "Establish zero intersection between Gold and Silver.", "formula": "$$\\text{Gold} \\cap \\text{Silver} = \\emptyset$$", "tip": "Enclose Rings inside Gold."}, {"stepNumber": 2, "title": "Subset Inheritance", "explanation": "Rings inherit Gold''s negative barrier with Silver.", "formula": "$$\\text{Rings} \\subseteq \\text{Gold} \\implies \\text{Rings} \\cap \\text{Silver} = \\emptyset$$", "tip": "Conclusion I holds."}, {"stepNumber": 3, "title": "Intersecting Sub-Element Analysis", "explanation": "Identify diamond-silver overlap portion.", "formula": "$$\\text{Diamonds} \\cap \\text{Silver} \\subseteq \\text{Gold}^c$$", "tip": "Conclusion II holds."}], "keyTakeaways": ["A negative relationship between parent sets cascades to all subsets.", "Any element inside a disjoint circle inherits total exclusion."], "quickCheckItems": [{"question": "If All Apples are Fruits and No Fruit is Vegetable, can some Apples be Vegetables?", "options": ["Yes, it is possible", "No, definitely impossible", "Only if some Vegetables are Apples", "Cannot be determined"], "correctOptionIndex": 1, "explanation": "Since Apples are entirely inside Fruits, and Fruits cannot intersect Vegetables, no Apple can be a Vegetable."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000019'::uuid,
        'REAS',
        'SERIES',
        'SER_NUMBER',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Number series in competitive examinations fall into five canonical mathematical paradigms: (1) Polynomial differences (taking consecutive differences $$\Delta_1, \Delta_2$$ until constant), (2) Multiplicative with arithmetic offsets ($$x_{n+1} = a x_n \pm b$$), (3) Powers with offsets ($$n^3 \pm n$$, $$n^2 \pm n$$), (4) Alternating interwoven series, and (5) Triangular/Fibonacci sums. The first diagnostic check is always computing the tier-1 difference sequence.',
        'Assuming a single linear difference when differences grow rapidly — always test cubic offsets $$n^3 - n$$ or multiplicative factors when terms double or triple.',
        '["Tier-2 Constant Difference: $$\\Delta_2 = \\text{const} \\implies \\text{Quadratic } an^2 + bn + c$$", "Tier-3 Constant Difference: $$\\Delta_3 = \\text{const} \\implies \\text{Cubic } an^3 + bn^2 + cn + d$$", "Cubic Offset Signature: $$n^3 - n \\implies 0, 6, 24, 60, 120, 210, 336, 504, 720$$", "Sum of Squares Offset: $$n^3 + n^2 \\implies 2, 12, 36, 80, 150, 252, 392, 576$$"]',
        '{"questionStem": "Identify the underlying mathematical pattern and find the next number in the series: $$6, 24, 60, 120, 210, ?$$", "steps": ["Step 1: Compute first-order differences: $$24 - 6 = 18$$, $$60 - 24 = 36$$, $$120 - 60 = 60$$, $$210 - 120 = 90$$.", "Step 2: Compute second-order differences: $$36 - 18 = 18$$, $$60 - 36 = 24$$, $$90 - 60 = 30$$. The third differences are constant ($$24 - 18 = 6$$, $$30 - 24 = 6$$).", "Step 3: Alternatively, recognize the famous cubic offset pattern $$T_n = n^3 - n = n(n-1)(n+1)$$: for $$n=2$$, $$2^3 - 2 = 6$$; for $$n=3$$, $$3^3 - 3 = 24$$; for $$n=4$$, $$4^3 - 4 = 60$$; for $$n=5$$, $$5^3 - 5 = 120$$; for $$n=6$$, $$6^3 - 6 = 210$$. For $$n=7$$, the next term is $$7^3 - 7 = 343 - 7 = 336$$."], "finalAnswer": "$$\\text{Next Term} = 336$$", "structuredSteps": [{"stepNumber": 1, "title": "Difference Analysis", "explanation": "Extract differences between successive elements.", "formula": "$$\\Delta_1 = 18, 36, 60, 90$$", "tip": "Notice differences are multiples of 6."}, {"stepNumber": 2, "title": "Recognize n^3 - n Archetype", "explanation": "Map terms to $$n^3 - n$$.", "formula": "$$T_n = n^3 - n$$", "tip": "$$6 = 2^3-2, 24 = 3^3-3$$."}, {"stepNumber": 3, "title": "Compute for n=7", "explanation": "Evaluate $$7^3 - 7$$.", "formula": "$$343 - 7 = 336$$", "tip": "Instant answer once pattern is identified."}], "structuredTrapAlert": {"title": "Difference Arithmetic Exhaustion", "description": "Calculating 3 tiers of differences when memorizing $$n^3-n$$ gives the answer in 2 seconds.", "commonMistake": "Failing to memorize canonical power sequences.", "correctApproach": "Memorize the benchmark cubes and squares up to 25."}, "keyTakeaways": ["The sequence $$6, 24, 60, 120, 210, 336$$ is the product of three consecutive integers: $$(n-1)n(n+1)$$.", "Always test $$n^3 \\pm n$$ whenever numbers hover near perfect cubes ($$8, 27, 64, 125, 216, 343$$)."], "quickCheckItems": [{"question": "What is the next number in the series: $$2, 12, 36, 80, 150, ?$$", "options": ["$$252$$", "$$240$$", "$$264$$", "$$224$$"], "correctOptionIndex": 0, "explanation": "Pattern is $$n^3 + n^2$$. For $$n=6$$, $$6^3 + 6^2 = 216 + 36 = 252$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000020'::uuid,
        'REAS',
        'SERIES',
        'SER_NUMBER',
        'WORKED_EXAMPLE',
        'CORE',
        2,
        'b1010000-0000-0000-0000-000000000019'::uuid,
        5,
        'Multiplicative progressions with arithmetic offsets follow the recurrence $$x_{n+1} = a x_n \pm b$$. When successive terms roughly double or triple, determine the multiplier by testing ratios between consecutive terms, and establish the constant or escalating additive modifier.',
        'Attempting polynomial difference trees on multiplicative sequences, resulting in diverging difference rows.',
        '["Recurrence Form: $$x_{n+1} = a \\cdot x_n + b$$", "Escalating Offset: $$x_{n+1} = a \\cdot x_n + n$$", "Ratio Diagnostic: $$\\frac{x_{n+1}}{x_n} \\approx a \\implies \\text{Multiplicative recurrence}$$"]',
        '{"questionStem": "Identify the underlying progression logic and find the missing term: $$7, 19, 43, 91, 187, ?$$", "steps": ["Step 1: Check ratios of adjacent terms: $$19/7 \\approx 2.7$$, $$43/19 \\approx 2.2$$, $$91/43 \\approx 2.1$$, $$187/91 \\approx 2.05$$. Notice each term is roughly double the previous.", "Step 2: Test the $$2x + k$$ model: $$7 \\times 2 + 5 = 19$$, $$19 \\times 2 + 5 = 43$$, $$43 \\times 2 + 5 = 91$$, $$91 \\times 2 + 5 = 187$$. The constant recurrence is $$x_{n+1} = 2x_n + 5$$.", "Step 3: Compute the next term: $$187 \\times 2 + 5 = 374 + 5 = 379$$."], "finalAnswer": "$$\\text{Missing Term} = 379$$", "structuredSteps": [{"stepNumber": 1, "title": "Inspect Term Growth", "explanation": "Observe terms approximately double each step.", "formula": "$$x_{n+1} \\approx 2 x_n$$", "tip": "Suggests multiplier of 2."}, {"stepNumber": 2, "title": "Determine Offset Constant", "explanation": "Check $$19 - 14 = 5$$, $$43 - 38 = 5$$.", "formula": "$$x_{n+1} = 2x_n + 5$$", "tip": "Offset is fixed at +5."}, {"stepNumber": 3, "title": "Calculate Missing Term", "explanation": "Evaluate $$187 \\times 2 + 5$$.", "formula": "$$374 + 5 = 379$$", "tip": "Quick mental arithmetic."}], "keyTakeaways": ["When differences double ($$12, 24, 48, 96$$), the series is inherently $$\\times 2$$.", "Notice differences here are $$12, 24, 48, 96$$, each doubling: $$187 + 192 = 379$$."], "quickCheckItems": [{"question": "What is the next number in the series: $$3, 7, 15, 31, 63, ?$$", "options": ["$$127$$", "$$126$$", "$$128$$", "$$125$$"], "correctOptionIndex": 0, "explanation": "Pattern is $$2x + 1$$. Next term is $$63 \\times 2 + 1 = 127$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000021'::uuid,
        'REAS',
        'CODING_DECODING',
        'CD_LETTER',
        'CONCEPT',
        'FOUNDATION',
        1,
        NULL::uuid,
        4,
        'Letter coding is governed by English alphabet positional metrics ($$A=1, \dots, Z=26$$) and reverse complementary pairs whose sum is always $$27$$ ($$A \leftrightarrow Z, B \leftrightarrow Y, C \leftrightarrow X, M \leftrightarrow N$$). Mnemonics like EJOTY ($$5, 10, 15, 20, 25$$) and CFILORUX (multiples of 3) provide immediate anchors.',
        'Counting forward on fingers instead of calculating algebraic index offsets, which introduces one-off errors.',
        '["EJOTY Benchmark: E=5, J=10, O=15, T=20, Y=25", "Reverse Pair Sum: $$\\text{Forward Rank} + \\text{Reverse Rank} = 27$$", "Opposite Pairs Mnemonic: AZ (Amazon), BY (Boy), CX (Crux), DW (Dew), EV (Even), FU (Full), GT (GT Road), HS (High School), IR (Indian Railway), JQ (Jungle Queen), KP (Kanpur), LO (Love), MN (Man)"]',
        '{"questionStem": "If in a certain code language, `DELHI` is coded as `CCIDD`, how will `BOMBAY` be coded in that same pattern?", "steps": ["Step 1: Write down positional numerical ranks of `DELHI`: D=4, E=5, L=12, H=8, I=9.", "Step 2: Write down ranks of code `CCIDD`: C=3, C=3, I=9, D=4, D=4.", "Step 3: Analyze shifts: $$4-1=3 (-1)$$, $$5-2=3 (-2)$$, $$12-3=9 (-3)$$, $$8-4=4 (-4)$$, $$9-5=4 (-5)$$. The shift sequence is escalating reductions: $$-1, -2, -3, -4, -5$$. Apply to `BOMBAY` (2, 15, 13, 2, 1, 25): B(2-1)=1 (A), O(15-2)=13 (M), M(13-3)=10 (J), B(2-4 = 28-4)=24 (X), A(1-5 = 27-5)=22 (V), Y(25-6)=19 (S). The coded word is `AMJXVS`."], "finalAnswer": "`AMJXVS`", "structuredSteps": [{"stepNumber": 1, "title": "Translate to Number Ranks", "explanation": "Map letters to numerical values.", "formula": "$$\\text{DELHI} = (4, 5, 12, 8, 9)$$", "tip": "Use EJOTY anchors."}, {"stepNumber": 2, "title": "Identify Offset Pattern", "explanation": "Subtract code ranks from word ranks.", "formula": "$$\\text{Shift} = (-1, -2, -3, -4, -5)$$", "tip": "Notice sequential decrease."}, {"stepNumber": 3, "title": "Apply Modular Shift", "explanation": "Shift BOMBAY using cyclic alphabet (modulo 26).", "formula": "$$\\text{BOMBAY} \\to \\text{AMJXVS}$$", "tip": "When subtracting past A, wrap around using 26+rank."}], "keyTakeaways": ["Always convert letters to positional integers immediately.", "When shifting left past A, wrap modulo 26 ($$A-1 = Z, B-3 = Y$$)."], "quickCheckItems": [{"question": "What is the reverse opposite letter of ''K'' in the English alphabet?", "options": ["P", "Q", "O", "L"], "correctOptionIndex": 0, "explanation": "K is 11. Reverse letter is $$27 - 11 = 16$$, which is P (Kanpur pair)."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000022'::uuid,
        'REAS',
        'INEQUALITIES',
        'INEQ_DIRECT',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        4,
        'Deductive inequalities evaluate chains of directional symbols ($$>, \ge, =, \le, <$$). The Golden Rule of Inequalities: a definite relationship between two terms $$A$$ and $$B$$ exists if and only if ALL symbols in the path point in the SAME direction without any opposing gate symbols ($$>$$ facing $$<$$). If opposing signs appear (e.g. $$A > B < C$$), no definite relationship between $$A$$ and $$C$$ can ever be deduced.',
        'Attempting to draw conclusions between variables across opposing inequality gates ($$>$$ and $$<$$).',
        '["Priority Hierarchy: $$>$$ or $$<$$ (Priority 1) $$\\succ$$ $$\\ge$$ or $$\\le$$ (Priority 2) $$\\succ$$ $$=$$ (Priority 3)", "Gate Blocking Rule: $$A > B < C \\implies \\text{Definite relation between } A \\text{ and } C \\text{ is BLOCKED}$$", "Either-Or Triad: \\text{Same variables, both false individually, covering } >, <, ="]',
        '{"questionStem": "Statement: $$P > Q \\ge R = S < T$$\n\nConclusions:\nI. $$P > R$$\nII. $$P > T$$", "steps": ["Step 1: Check path from $$P$$ to $$R$$: $$P > Q \\ge R$$. All symbols point to the right. The highest priority symbol is $$>$$ (Priority 1). Therefore, $$P > R$$ is DEFINITELY TRUE.", "Step 2: Check path from $$P$$ to $$T$$: $$P > Q \\ge R = S < T$$. Notice the transition between $$S$$ and $$T$$ has a ''<'' symbol opposing the ''>'' symbols.", "Step 3: Because an opposing sign ($$<$$) blocks the path, no definite relation between $$P$$ and $$T$$ can be established. Conclusion II does NOT follow. Only Conclusion I follows."], "finalAnswer": "$$\\text{Only Conclusion I follows}$$", "structuredSteps": [{"stepNumber": 1, "title": "Trace Path for Conclusion I", "explanation": "Check signs from P to R: > and >=.", "formula": "$$P > Q \\ge R \\implies P > R$$", "tip": "Priority 1 sign dominates."}, {"stepNumber": 2, "title": "Inspect Gates for Conclusion II", "explanation": "Check signs from P to T.", "formula": "$$> \\dots < \\implies \\text{Opposing Gates}$$", "tip": "Path is broken at S < T."}, {"stepNumber": 3, "title": "Conclude Definitively", "explanation": "Only Conclusion I is valid.", "formula": "$$\\text{Result: Only I follows}$$", "tip": "Instant verification."}], "keyTakeaways": ["If any opposing gate exists in the path, all definite conclusions between endpoints are false.", "Priority 1 ($$>$$) always beats Priority 2 ($$\\ge$$)."], "quickCheckItems": [{"question": "If $$A \\ge B = C \\ge D$$, which conclusion is definitely true?", "options": ["$$A > D$$", "$$A \\ge D$$", "$$A = D$$", "$$A < D$$"], "correctOptionIndex": 1, "explanation": "The highest priority symbol in the chain is $$\\ge$$, so $$A \\ge D$$ is definitely true."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000023'::uuid,
        'GSCI',
        'PHYSICS',
        'PHY_MECHANICS',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Kinematics describes motion with uniform acceleration $$a$$ through Galileo''s canonical equations: (1) $$v = u + at$$, (2) $$s = ut + \frac{1}{2}at^2$$, and (3) $$v^2 = u^2 + 2as$$. For the $$n$$-th second specifically, displacement is given by $$s_n = u + \frac{a}{2}(2n - 1)$$. Under gravity ($$a = -g$$), upward motion decelerates until velocity momentarily reaches zero at maximum height $$H_{\text{max}} = \frac{u^2}{2g}$$, with total time of flight $$T = \frac{2u}{g}$$.',
        'Inverting sign conventions for gravity, such as taking $$g$$ as positive during upward flight, which results in calculating velocity increasing rather than decreasing.',
        '["Kinematic Triad: $$v = u + at, \\quad s = ut + \\frac{1}{2}at^2, \\quad v^2 = u^2 + 2as$$", "Displacement in n-th Second: $$s_n = u + \\frac{a}{2}(2n - 1)$$", "Freefall under Gravity: $$H_{\\text{max}} = \\frac{u^2}{2g}, \\quad T_{\\text{flight}} = \\frac{2u}{g}$$", "Momentum Conservation: $$m_1 u_1 + m_2 u_2 = m_1 v_1 + m_2 v_2$$"]',
        '{"questionStem": "A vehicle starts from rest and accelerates with a uniform acceleration of $$4\\text{ m/s}^2$$. Calculate the distance traveled by the vehicle during the $$5\\text{th second}$$ of its motion.", "steps": ["Step 1: Identify given parameters: initial velocity $$u = 0\\text{ m/s}$$, uniform acceleration $$a = 4\\text{ m/s}^2$$, and time index $$n = 5$$.", "Step 2: Recall the formula for displacement in the $$n$$-th second: $$s_n = u + \\frac{a}{2}(2n - 1)$$.", "Step 3: Substitute parameters: $$s_5 = 0 + \\frac{4}{2}(2 \\times 5 - 1) = 2 \\times (10 - 1) = 2 \\times 9 = 18\\text{ m}$$. Alternatively, compute $$s(5) - s(4) = \\frac{1}{2}(4)(25) - \\frac{1}{2}(4)(16) = 50 - 32 = 18\\text{ m}$$."], "finalAnswer": "$$s_5 = 18\\text{ m}$$", "structuredSteps": [{"stepNumber": 1, "title": "Extract Initial Conditions", "explanation": "Starting from rest implies u = 0.", "formula": "$$u = 0, \\quad a = 4\\text{ m/s}^2, \\quad n = 5$$", "tip": "Rest means zero initial velocity."}, {"stepNumber": 2, "title": "Apply n-th Second Formula", "explanation": "Use $$s_n = u + \\frac{a}{2}(2n-1)$$.", "formula": "$$s_n = u + \\frac{a}{2}(2n - 1)$$", "tip": "Saves calculating two full displacements."}, {"stepNumber": 3, "title": "Evaluate Distance", "explanation": "Calculate $$2 \\times 9 = 18\\text{ m}$$.", "formula": "$$s_5 = 18\\text{ m}$$", "tip": "Matches $$s(5) - s(4) = 50 - 32 = 18$$."}], "structuredTrapAlert": {"title": "Total Displacement Confusion", "description": "Calculating total distance in 5 seconds ($$s = \\frac{1}{2}(4)(25) = 50\\text{ m}$$) instead of distance in 5th second.", "commonMistake": "Ignoring the specific ordinal ''5th second''.", "correctApproach": "Always distinguish total displacement ($$s_t$$) from interval displacement ($$s_n$$)."}, "keyTakeaways": ["Displacement in consecutive seconds from rest follows odd ratios: $$1 : 3 : 5 : 7 : 9 : \\dots$$ (Galileo''s odd-number rule).", "For $$u=0, a=4$$, distance in 1st, 2nd, 3rd, 4th, 5th sec: $$2, 6, 10, 14, 18\\text{ m}$$."], "quickCheckItems": [{"question": "A body starts from rest with uniform acceleration. What is the ratio of distance traveled in the 1st second to the 3rd second?", "options": ["$$1 : 5$$", "$$1 : 3$$", "$$1 : 9$$", "$$1 : 4$$"], "correctOptionIndex": 0, "explanation": "Galileo odd-number law: $$s_1 : s_2 : s_3 = 1 : 3 : 5$$. Ratio is $$1 : 5$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000024'::uuid,
        'GSCI',
        'PHYSICS',
        'PHY_MECHANICS',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000023'::uuid,
        6,
        'When a projectile is launched from an elevated platform of height $$h$$, setting up a consistent vertical coordinate system (taking upward as positive, displacement as $$-h$$, and acceleration as $$-g$$) resolves the total flight time through a single quadratic equation: $$-h = ut - \frac{1}{2}gt^2$$.',
        'Breaking the motion into upward and downward phases without adjusting the velocity at the platform level, doubling calculation steps.',
        '["Elevated Projection: $$-h = ut - \\frac{1}{2}gt^2$$", "Landing Velocity: $$v^2 = u^2 + 2gh$$", "Standard Gravity: $$g = 9.8\\text{ m/s}^2 \\approx 10\\text{ m/s}^2$$"]',
        '{"questionStem": "A ball is thrown vertically upwards with a velocity of $$10\\text{ m/s}$$ from the top of a tower of height $$40\\text{ m}$$. Taking $$g = 10\\text{ m/s}^2$$, calculate the total time taken by the ball to reach the ground.", "steps": ["Step 1: Establish the sign convention taking upwards as positive: initial velocity $$u = +10\\text{ m/s}$$, acceleration $$a = -g = -10\\text{ m/s}^2$$, and net vertical displacement upon reaching ground $$s = -40\\text{ m}$$.", "Step 2: Substitute into $$s = ut + \\frac{1}{2}at^2$$: $$-40 = 10t - \\frac{1}{2}(10)t^2 \\implies -40 = 10t - 5t^2$$.", "Step 3: Rearrange into standard quadratic form: $$5t^2 - 10t - 40 = 0 \\implies t^2 - 2t - 8 = 0$$. Factorize: $$(t - 4)(t + 2) = 0$$. Discarding the negative root, total time of flight is $$t = 4\\text{ seconds}$$."], "finalAnswer": "$$t = 4\\text{ seconds}$$", "structuredSteps": [{"stepNumber": 1, "title": "Coordinate Sign Convention", "explanation": "Upward positive, displacement is negative because landing point is below starting point.", "formula": "$$s = -40\\text{ m}, \\quad u = +10\\text{ m/s}, \\quad a = -10\\text{ m/s}^2$$", "tip": "Crucial: s is -40, not +40."}, {"stepNumber": 2, "title": "Formulate Displacement Equation", "explanation": "Substitute into $$s = ut + \\frac{1}{2}at^2$$.", "formula": "$$-40 = 10t - 5t^2$$", "tip": "Divide across by 5 to simplify."}, {"stepNumber": 3, "title": "Solve Quadratic Equation", "explanation": "Factorize $$t^2 - 2t - 8 = 0$$.", "formula": "$$(t-4)(t+2) = 0 \\implies t = 4\\text{ s}$$", "tip": "Time must be strictly positive."}], "keyTakeaways": ["Setting $$s = -h$$ solves the entire ascent and descent in one clean calculation.", "Striking speed depends only on height and launch speed: $$v = \\sqrt{u^2 + 2gh} = \\sqrt{100 + 800} = 30\\text{ m/s}$$."], "quickCheckItems": [{"question": "What is the speed of the ball just before striking the ground when dropped from 40m tower (u=0, g=10)?", "options": ["$$20\\sqrt{2}\\text{ m/s}$$", "$$20\\text{ m/s}$$", "$$40\\text{ m/s}$$", "$$10\\sqrt{2}\\text{ m/s}$$"], "correctOptionIndex": 0, "explanation": "$$v = \\sqrt{2gh} = \\sqrt{2(10)(40)} = \\sqrt{800} = 20\\sqrt{2}\\text{ m/s}$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000025'::uuid,
        'GSCI',
        'PHYSICS',
        'PHY_OPTICS',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Light propagation across optical interfaces is governed by Snell''s Law: $$n_1 \sin \theta_1 = n_2 \sin \theta_2$$. When light travels from an optically denser medium ($$n_1$$) to a rarer medium ($$n_2 < n_1$$), the refracted ray bends away from the normal. At the critical angle $$\theta_c$$, the refraction angle reaches $$90^\circ$$, giving $$\sin \theta_c = \frac{n_2}{n_1}$$. Any incident angle exceeding $$\theta_c$$ produces Total Internal Reflection (TIR) with zero transmission loss, the operating foundation of optical fibers, mirages, and diamond brilliance.',
        'Assuming Total Internal Reflection can occur when light travels from a rarer medium to a denser medium (e.g. from air into glass). TIR strictly requires propagation from DENSER to RARER medium.',
        '["Snells Law: $$n_1 \\sin \\theta_1 = n_2 \\sin \\theta_2$$", "Critical Angle Condition: $$\\sin \\theta_c = \\frac{n_{\\text{rarer}}}{n_{\\text{denser}}}$$", "Lens Formula: $$\\frac{1}{f} = \\frac{1}{v} - \\frac{1}{u}, \\quad \\text{Power } P = \\frac{1}{f(\\text{m})}$$", "Mirror Formula: $$\\frac{1}{f} = \\frac{1}{v} + \\frac{1}{u}, \\quad m = -\\frac{v}{u}$$"]',
        '{"questionStem": "A ray of light traveling inside a glass block of refractive index $$n = 1.50$$ strikes the glass-air interface. Calculate the critical angle for total internal reflection (take refractive index of air $$n_{\\text{air}} = 1.00$$).", "steps": ["Step 1: Identify refractive indices: denser medium (glass) has $$n_1 = 1.50 = \\frac{3}{2}$$, and rarer medium (air) has $$n_2 = 1.00$$.", "Step 2: Apply the critical angle formula: $$\\sin \\theta_c = \\frac{n_{\\text{rarer}}}{n_{\\text{denser}}} = \\frac{1.00}{1.50} = \\frac{2}{3}$$.", "Step 3: Solve for $$\\theta_c$$: $$\\theta_c = \\arcsin\\left(\\frac{2}{3}\\right) \\approx \\arcsin(0.6667) \\approx 41.8^\\circ$$. Any angle of incidence inside glass exceeding $$41.8^\\circ$$ undergoes total internal reflection."], "finalAnswer": "$$\\theta_c = \\arcsin(2/3) \\approx 41.8^\\circ$$", "structuredSteps": [{"stepNumber": 1, "title": "Identify Boundary Media", "explanation": "Glass to air means denser to rarer.", "formula": "$$n_1 = 1.5, \\quad n_2 = 1.0$$", "tip": "Condition for TIR is satisfied."}, {"stepNumber": 2, "title": "Apply Critical Angle Ratio", "explanation": "Divide rarer index by denser index.", "formula": "$$\\sin \\theta_c = \\frac{1}{1.5} = \\frac{2}{3}$$", "tip": "Always less than or equal to 1."}, {"stepNumber": 3, "title": "Evaluate Angle", "explanation": "Invert sine function.", "formula": "$$\\theta_c \\approx 41.8^\\circ$$", "tip": "Water critical angle is $$48.6^\\circ$$, diamond is $$24.4^\\circ$$."}], "structuredTrapAlert": {"title": "Directional Precondition Trap", "description": "Attempting to compute critical angle for light entering glass from air.", "commonMistake": "Ignoring that light must travel from higher index to lower index.", "correctApproach": "TIR is physically impossible when entering a denser medium."}, "keyTakeaways": ["Two mandatory conditions for TIR: (1) Light travels from denser to rarer medium, (2) Angle of incidence exceeds critical angle.", "Optical fibers transmit signals with near zero attenuation using repeated TIR."], "quickCheckItems": [{"question": "Which phenomenon is primarily responsible for the sparkling brilliance of a cut diamond?", "options": ["Total internal reflection", "Diffraction", "Polarization", "Scattering"], "correctOptionIndex": 0, "explanation": "Diamond has a very high refractive index ($$2.42$$) and tiny critical angle ($$24.4^\\circ$$), entrapping light via multiple TIRs."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000026'::uuid,
        'GSCI',
        'CHEMISTRY',
        'CHEM_CARBON',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Carbon forms millions of stable covalent compounds due to its tetravalency ($$Z = 6$$, electronic configuration $$2, 4$$) and catenation power to form strong $$\ce{C-C}$$ single, double, and triple bonds. This self-linking stability arises because the small atomic radius of carbon ($$77\text{ pm}$$) enables its nucleus to hold shared electron pairs securely. Homologous series share the same general formula, identical functional groups, similar chemical properties, and a constant successive difference of $$\ce{-CH2-}$$ ($$\Delta M = 14\text{ u}$$).',
        'Students frequently confuse saturated alkanes ($$\ce{C_nH_{2n+2}}$$) with unsaturated alkenes ($$\ce{C_nH_{2n}}$$), mistakenly assuming multiple bonds increase stability, whereas double and triple bonds are actually much more reactive sites for addition reactions.',
        '["Homologous Formulas: $$\\text{Alkanes: } \\ce{C_nH_{2n+2}}, \\quad \\text{Alkenes: } \\ce{C_nH_{2n}}, \\quad \\text{Alkynes: } \\ce{C_nH_{2n-2}}$$", "Homologue Delta: $$\\Delta M = 14\\text{ u} \\quad (\\ce{-CH2-}\\text{ difference})$$", "Functional Groups: $$\\ce{-OH}\\text{ (alcohol)}, \\quad \\ce{-CHO}\\text{ (aldehyde)}, \\quad \\ce{-COOH}\\text{ (carboxylic acid)}$$"]',
        '{"questionStem": "Calculate the molecular mass of the fourth member of the alkane homologous series and determine the volume of $$\\ce{CO2}$$ gas produced at STP by the complete combustion of $$1\\text{ mol}$$ of methane ($$\\ce{CH4}$$).", "steps": ["Step 1: For the alkane series ($$\\ce{C_nH_{2n+2}}$$), the fourth member has $$n = 4$$, which gives butane ($$\\ce{C4H10}$$). Its molecular mass is $$4 \\times 12 + 10 \\times 1 = 58\\text{ u}$$.", "Step 2: Write the balanced combustion equation for methane: $$\\ce{CH4 + 2O2 -> CO2 + 2H2O}$$. One mole of $$\\ce{CH4}$$ produces exactly $$1\\text{ mol}$$ of $$\\ce{CO2}$$.", "Step 3: At standard temperature and pressure (STP), $$1\\text{ mol}$$ of any ideal gas occupies a molar volume of $$22.4\\text{ L}$$, so the volume of $$\\ce{CO2}$$ is $$22.4\\text{ L}$$."], "finalAnswer": "$$\\text{Mass} = 58\\text{ u}, \\quad V_{\\ce{CO2}} = 22.4\\text{ L}$$", "structuredSteps": [{"stepNumber": 1, "title": "Fourth Alkane Identification", "explanation": "Butane is C4H10 with mass 58 u.", "formula": "$$\\ce{C4H10}: 4(12) + 10(1) = 58\\text{ u}$$", "tip": "Methane(1), Ethane(2), Propane(3), Butane(4)."}, {"stepNumber": 2, "title": "Stoichiometry Balance", "explanation": "Combustion yields 1 mole of carbon dioxide.", "formula": "$$\\ce{CH4 + 2O2 -> CO2 + 2H2O}$$", "tip": "Carbon moles in fuel equals carbon moles in CO2."}, {"stepNumber": 3, "title": "STP Molar Volume Conversion", "explanation": "Apply Avogadro''s molar volume of 22.4 L.", "formula": "$$V = 1 \\times 22.4\\text{ L} = 22.4\\text{ L}$$", "tip": "STP is defined at 0C and 1 atm."}], "keyTakeaways": ["Successive homologues differ by one $$\\ce{-CH2-}$$ unit ($$14\\text{ u}$$).", "All hydrocarbons burn in excess oxygen to produce $$\\ce{CO2}$$ and $$\\ce{H2O}$$."], "quickCheckItems": [{"question": "What is the general formula for the alkyne homologous series?", "options": ["$$\\ce{C_nH_{2n-2}}$$", "$$\\ce{C_nH_{2n}}$$", "$$\\ce{C_nH_{2n+2}}$$", "$$\\ce{C_nH_{2n-4}}$$"], "correctOptionIndex": 0, "explanation": "Alkynes have a triple bond and follow general formula $$\\ce{C_nH_{2n-2}}$$ (e.g. ethyne $$\\ce{C2H2}$$)."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000027'::uuid,
        'GSCI',
        'CHEMISTRY',
        'CHEM_CARBON',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000026'::uuid,
        6,
        'Soaps are sodium or potassium salts of long-chain fatty acids prepared via alkaline hydrolysis (saponification) of esters/triglycerides: $$\text{Fat/Ester} + 3\ce{NaOH} \to 3\text{Soap} + \text{Glycerol}$$. In aqueous solution, soap molecules aggregate into spherical micelles with inward-facing hydrophobic tails and outward-facing hydrophilic heads.',
        'Assuming soaps clean effectively in hard water. In hard water, calcium and magnesium ions precipitate soap as insoluble curd/scum ($$\ce{(RCOO)2Ca}$$), requiring synthetic detergents.',
        '["Saponification Reaction: $$\\ce{RCOOCH2-CH(OOCR)-CH2OOCR + 3NaOH -> 3RCOONa + C3H5(OH)3}$$", "Micelle Architecture: \\text{Hydrophobic tail (hydrocarbon) inside, Hydrophilic head (carboxylate) outside}", "Hard Water Scum: $$\\ce{2RCOONa + Ca^{2+} -> (RCOO)2Ca v + 2Na+}$$"]',
        '{"questionStem": "Calculate the mass of glycerol ($$\\ce{C3H8O3}$$) obtained from the complete saponification of $$1\\text{ mol}$$ of glyceryl trioleate with excess $$\\ce{NaOH}$$.", "steps": ["Step 1: Write the stoichiometric saponification reaction: $$1\\text{ mol}$$ of triglyceride reacts with $$3\\text{ mol}$$ of $$\\ce{NaOH}$$ to yield $$3\\text{ mol}$$ of sodium soap and exactly $$1\\text{ mol}$$ of glycerol ($$\\ce{C3H8O3}$$).", "Step 2: Calculate the molar mass of glycerol ($$\\ce{C3H8O3}$$): Carbon ($$3 \\times 12 = 36$$), Hydrogen ($$8 \\times 1 = 8$$), Oxygen ($$3 \\times 16 = 48$$). Total molar mass $$= 36 + 8 + 48 = 92\\text{ g/mol}$$.", "Step 3: Since $$1\\text{ mol}$$ of triglyceride yields $$1\\text{ mol}$$ of glycerol, the final mass of glycerol produced is $$1 \\times 92 = 92\\text{ g}$$."], "finalAnswer": "$$\\text{Mass of glycerol} = 92\\text{ g}$$", "structuredSteps": [{"stepNumber": 1, "title": "Stoichiometric Ratio", "explanation": "1 mole of triester yields 1 mole of trihydric alcohol (glycerol).", "formula": "$$1\\text{ mol fat} \\to 1\\text{ mol glycerol}$$", "tip": "3 moles of ester bonds broken."}, {"stepNumber": 2, "title": "Glycerol Molar Mass", "explanation": "Sum atomic masses of C3H8O3.", "formula": "$$3(12) + 8(1) + 3(16) = 92\\text{ g/mol}$$", "tip": "Standard biochemical constant."}, {"stepNumber": 3, "title": "Final Product Yield", "explanation": "Multiply moles by molar mass.", "formula": "$$m = 1 \\times 92\\text{ g} = 92\\text{ g}$$", "tip": "Clean integer yield."}], "keyTakeaways": ["Saponification is the alkaline hydrolysis of an ester.", "Glycerol is the ubiquitous byproduct of traditional soap manufacturing."], "quickCheckItems": [{"question": "Which salt is responsible for the scum formed when soap is added to hard water?", "options": ["Calcium stearate", "Sodium chloride", "Potassium stearate", "Sodium carbonate"], "correctOptionIndex": 0, "explanation": "Soap forms insoluble calcium/magnesium stearate salts with hard water minerals."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000028'::uuid,
        'GSCI',
        'CHEMISTRY',
        'CHEM_ACIDS_BASES',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'According to the Bronsted-Lowry theory, an acid is a proton ($$\ce{H+}$$) donor and a base is a proton acceptor. The acidity or basicity of an aqueous solution is quantified on the logarithmic pH scale introduced by Sorensen: $$\text{pH} = -\log_{10}[\ce{H3O+}]$$ and $$\text{pOH} = -\log_{10}[\ce{OH-}]$$. At $$25^\circ\text{C}$$, the self-ionization product of water is $$K_w = [\ce{H+}][\ce{OH-}] = 1.0 \times 10^{-14}$$, establishing the invariant $$\text{pH} + \text{pOH} = 14$$.',
        'Assuming that pH can never be zero or negative, or forgetting that each unit drop on the logarithmic pH scale represents a tenfold ($$10\times$$) increase in hydronium ion concentration.',
        '["Logarithmic pH: $$\\text{pH} = -\\log_{10}[\\ce{H+}], \\quad \\text{pOH} = -\\log_{10}[\\ce{OH-}]$$", "Water Invariant at 25C: $$\\text{pH} + \\text{pOH} = 14, \\quad K_w = 1.0 \\times 10^{-14}$$", "Neutral Solution: $$[\\ce{H+}] = [\\ce{OH-}] = 10^{-7}\\text{ M} \\implies \\text{pH} = 7$$"]',
        '{"questionStem": "Calculate the pH of (a) a $$0.001\\text{ M}$$ aqueous solution of hydrochloric acid ($$\\ce{HCl}$$) and (b) a $$0.01\\text{ M}$$ aqueous solution of sodium hydroxide ($$\\ce{NaOH}$$) at $$25^\\circ\\text{C}$$.", "steps": ["Step 1: $$\\ce{HCl}$$ is a strong monoprotic acid that dissociates completely: $$[\\ce{H+}] = 0.001\\text{ M} = 10^{-3}\\text{ M}$$. Therefore, $$\\text{pH} = -\\log_{10}(10^{-3}) = 3$$.", "Step 2: $$\\ce{NaOH}$$ is a strong base that dissociates completely: $$[\\ce{OH-}] = 0.01\\text{ M} = 10^{-2}\\text{ M}$$. Therefore, $$\\text{pOH} = -\\log_{10}(10^{-2}) = 2$$.", "Step 3: Using the water equilibrium invariant $$\\text{pH} + \\text{pOH} = 14$$, calculate the pH of the $$\\ce{NaOH}$$ solution: $$\\text{pH} = 14 - \\text{pOH} = 14 - 2 = 12$$."], "finalAnswer": "$$\\text{pH}(\\ce{HCl}) = 3, \\quad \\text{pH}(\\ce{NaOH}) = 12$$", "structuredSteps": [{"stepNumber": 1, "title": "Acid Dissociation & pH", "explanation": "Strong acid dissociates completely.", "formula": "$$\\text{pH} = -\\log_{10}(10^{-3}) = 3$$", "tip": "$$10^{-n} \\implies \\text{pH} = n$$."}, {"stepNumber": 2, "title": "Base Dissociation & pOH", "explanation": "Compute hydroxide logarithmic pOH.", "formula": "$$\\text{pOH} = -\\log_{10}(10^{-2}) = 2$$", "tip": "Do not confuse pOH with pH."}, {"stepNumber": 3, "title": "Apply Water Invariant", "explanation": "Subtract pOH from 14.", "formula": "$$\\text{pH} = 14 - 2 = 12$$", "tip": "Basic solutions have pH > 7."}], "keyTakeaways": ["A pH drop from 5 to 3 means the solution is $$100\\times$$ more acidic.", "Strong bases require computing pOH first, then subtracting from 14."], "quickCheckItems": [{"question": "What is the pH of a 0.0001 M HNO3 solution?", "options": ["4", "3", "5", "10"], "correctOptionIndex": 0, "explanation": "$$0.0001\\text{ M} = 10^{-4}\\text{ M} \\implies \\text{pH} = -\\log_{10}(10^{-4}) = 4$$."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000029'::uuid,
        'GSCI',
        'BIOLOGY',
        'BIO_CELL',
        'CONCEPT',
        'FOUNDATION',
        1,
        NULL::uuid,
        4,
        'The cell is the fundamental structural and functional unit of all living organisms. Prokaryotic cells (bacteria, blue-green algae) lack membrane-bound organelles and a true nucleus, containing naked circular DNA and $$70\text{S}$$ ribosomes. Eukaryotic cells possess a compartmentalized cytoplasm with a double-membrane nucleus, mitochondria (site of ATP generation via oxidative phosphorylation), chloroplasts in plants, endoplasmic reticulum, Golgi apparatus, and $$80\text{S}$$ cytosolic ribosomes.',
        'Assuming ribosomes are membrane-bound organelles. Ribosomes are non-membrane-bound ribonucleoprotein granules found in BOTH prokaryotes and eukaryotes.',
        '["Ribosomal Subunits: $$\\text{Prokaryotes: } 70\\text{S} (50\\text{S} + 30\\text{S}), \\quad \\text{Eukaryotes: } 80\\text{S} (60\\text{S} + 40\\text{S})$$", "Semi-Autonomous Organelles: \\text{Mitochondria and Chloroplasts (contain circular DNA and 70S ribosomes)}", "Cell Theory Milestone: \\text{Schleiden & Schwann (1839) + Virchow (1855, Omnis cellula e cellula)}"]',
        '{"questionStem": "Which of the following cellular features is found in both prokaryotic bacteria and eukaryotic human liver cells?", "steps": ["Step 1: Compare organelle profiles: Mitochondria, Golgi apparatus, and endoplasmic reticulum are membrane-bound organelles found exclusively in eukaryotic cells.", "Step 2: Nuclear membrane: Prokaryotes lack a nuclear envelope; their genetic material is organized in a naked nucleoid.", "Step 3: Protein synthesis machinery: Ribosomes are universally present in all living cells. Although prokaryotes utilize $$70\\text{S}$$ ribosomes and eukaryotic cytoplasm utilizes $$80\\text{S}$$ ribosomes, ribosomes themselves perform translation in both cellular domains."], "finalAnswer": "$$\\text{Ribosomes (universal protein synthesis machinery)}$$", "structuredSteps": [{"stepNumber": 1, "title": "Survey Membrane Bound Structures", "explanation": "Exclude organelles with lipid bilayer membranes.", "formula": "$$\\text{Mitochondria, ER, Golgi} \\in \\text{Eukaryotes only}$$", "tip": "Prokaryotes lack internal membranes."}, {"stepNumber": 2, "title": "Check Nuclear Architecture", "explanation": "Prokaryotic DNA is non-enveloped.", "formula": "$$\\text{Nucleoid (Prokaryotes) vs True Nucleus (Eukaryotes)}$$", "tip": "Prokaryote means before nucleus."}, {"stepNumber": 3, "title": "Identify Universal Organelle", "explanation": "Ribosomes lack membranes and translate mRNA in both domains.", "formula": "$$\\text{Ribosomes} \\in \\text{All Cells}$$", "tip": "Essential for all protein synthesis."}], "keyTakeaways": ["Mitochondria and chloroplasts originated via endosymbiosis and retain their own circular DNA and $$70\\text{S}$$ ribosomes.", "Plant cells differ from animal cells by having a cellulose cell wall, large central vacuole, and plastids."], "quickCheckItems": [{"question": "Which organelle is universally known as the ''Powerhouse of the Cell'' due to ATP production?", "options": ["Mitochondria", "Ribosome", "Golgi apparatus", "Lysosome"], "correctOptionIndex": 0, "explanation": "Mitochondria synthesize cellular ATP through aerobic oxidative phosphorylation across their folded inner membrane cristae."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000030'::uuid,
        'BFA',
        'BFA_MONETARY_POLICY',
        'BFA_RATES',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'The Reserve Bank of India (RBI) operates its Monetary Policy Corridor to anchor the weighted average call rate (WACR) around the policy Repo Rate. The corridor ceiling is formed by the Marginal Standing Facility (MSF) rate and Bank Rate ($$\text{Repo} + 25\text{ bps}$$), while the floor is formed by the Standing Deposit Facility (SDF) rate ($$\text{Repo} - 25\text{ bps}$$). Quantitative reserve tools include the Cash Reserve Ratio (CRR, non-earning cash balances maintained with RBI as a percentage of NDTL) and Statutory Liquidity Ratio (SLR, investment in approved government securities and gold).',
        'Believing that the RBI pays interest on CRR balances. Under the RBI Act 1934, commercial banks earn strictly ZERO interest on statutory CRR deposits maintained with the RBI.',
        '["Policy Corridor: $$\\text{SDF (Floor)} = \\text{Repo} - 0.25\\%, \\quad \\text{MSF (Ceiling)} = \\text{Repo} + 0.25\\%$$", "Corridor Width: $$\\text{MSF} - \\text{SDF} = 50\\text{ bps} = 0.50\\%$$", "CRR Provision: \\text{Cash with RBI, 0% interest, fixed % of NDTL}", "SLR Provision: \\text{G-Secs, T-Bills, cash, gold, fixed % of NDTL}"]',
        '{"questionStem": "If the Monetary Policy Committee (MPC) of the RBI increases the policy Repo Rate by $$25\\text{ bps}$$ from $$6.50\\%$$ to $$6.75\\%$$, determine the corresponding Standing Deposit Facility (SDF) rate and Marginal Standing Facility (MSF) rate under the standard corridor framework.", "steps": ["Step 1: Identify the new policy Repo Rate: $$\\text{Repo} = 6.75\\%$$.", "Step 2: Calculate the Standing Deposit Facility (SDF) rate, which serves as the corridor floor positioned $$25\\text{ bps}$$ below the Repo Rate: $$\\text{SDF} = 6.75\\% - 0.25\\% = 6.50\\%$$.", "Step 3: Calculate the Marginal Standing Facility (MSF) rate, which serves as the corridor ceiling positioned $$25\\text{ bps}$$ above the Repo Rate: $$\\text{MSF} = 6.75\\% + 0.25\\% = 7.00\\%$$. The corridor maintains a total width of $$50\\text{ bps}$$."], "finalAnswer": "$$\\text{SDF} = 6.50\\%, \\quad \\text{MSF} = 7.00\\%$$", "structuredSteps": [{"stepNumber": 1, "title": "Identify Policy Rate Anchor", "explanation": "The Repo rate is the core operating target of the MPC.", "formula": "$$\\text{Repo} = 6.75\\%$$", "tip": "1 basis point = 0.01%."}, {"stepNumber": 2, "title": "Compute Corridor Floor (SDF)", "explanation": "SDF absorbs uncollateralized overnight liquidity at Repo - 25 bps.", "formula": "$$\\text{SDF} = 6.75 - 0.25 = 6.50\\%$$", "tip": "SDF requires no government securities collateral."}, {"stepNumber": 3, "title": "Compute Corridor Ceiling (MSF)", "explanation": "MSF provides penal emergency overnight borrowing at Repo + 25 bps.", "formula": "$$\\text{MSF} = 6.75 + 0.25 = 7.00\\%$$", "tip": "Corridor width is symmetrically 50 bps."}], "keyTakeaways": ["SDF was introduced in 2022 to absorb surplus liquidity without requiring government securities collateral.", "MSF allows banks to dip into their SLR quota up to an approved limit for emergency overnight funds."], "quickCheckItems": [{"question": "Does the RBI pay interest on the Cash Reserve Ratio (CRR) maintained by commercial banks?", "options": ["No, 0% interest", "Yes, at Repo Rate", "Yes, at SDF Rate", "Yes, at Savings Deposit Rate"], "correctOptionIndex": 0, "explanation": "RBI does not pay any interest on CRR balances maintained by banks under section 42(1) of the RBI Act."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000031'::uuid,
        'BFA',
        'BFA_MONETARY_POLICY',
        'BFA_RATES',
        'WORKED_EXAMPLE',
        'ADVANCED',
        2,
        'b1010000-0000-0000-0000-000000000030'::uuid,
        5,
        'Commercial banks must compute statutory reserve allocations on their Net Demand and Time Liabilities (NDTL) fortnightly. NDTL includes demand deposits (current/savings), time deposits (fixed/recurring), and interbank borrowing liabilities. If CRR is $$4.50\%$$ and SLR is $$18.00\%$$, the bank must park $$4.50\%$$ in cash with the RBI and invest $$18.00\%$$ in approved securities, leaving the remainder available for lending and credit creation.',
        'Calculating CRR and SLR on total gross deposits rather than Net Demand and Time Liabilities (NDTL), which excludes interbank assets.',
        '["Statutory Cash Reserve: $$\\text{CRR Amount} = \\text{NDTL} \\times \\frac{\\text{CRR}\\%}{100}$$", "Statutory Liquidity: $$\\text{SLR Amount} = \\text{NDTL} \\times \\frac{\\text{SLR}\\%}{100}$$", "Lendable Surplus Baseline: $$\\text{Available Funds} = \\text{NDTL} \\times \\left(1 - \\frac{\\text{CRR} + \\text{SLR}}{100}\\right)$$"]',
        '{"questionStem": "A scheduled commercial bank has Net Demand and Time Liabilities (NDTL) of $$\\text{Rs. } 50,000\\text{ crores}$$. Under current RBI regulations, the Cash Reserve Ratio (CRR) is $$4.50\\%$$ and the Statutory Liquidity Ratio (SLR) is $$18.00\\%$$. Calculate (1) the cash amount to be deposited with the RBI and (2) the minimum investment required in approved liquid securities.", "steps": ["Step 1: Compute required CRR deposit with RBI: $$\\text{CRR} = 50000 \\times \\frac{4.50}{100} = 500 \\times 4.50 = \\text{Rs. } 2,250\\text{ crores}$$. This must be maintained in pure cash with RBI.", "Step 2: Compute required SLR allocation in eligible assets: $$\\text{SLR} = 50000 \\times \\frac{18.00}{100} = 500 \\times 18.00 = \\text{Rs. } 9,000\\text{ crores}$$.", "Step 3: Total statutory pre-emptions amount to $$2250 + 9000 = \\text{Rs. } 11,250\\text{ crores}$$ ($$22.5\\%$$ of NDTL), leaving a maximum lendable surplus of $$\\text{Rs. } 38,750\\text{ crores}$$."], "finalAnswer": "$$\\text{CRR} = \\text{Rs. } 2,250\\text{ cr}, \\quad \\text{SLR} = \\text{Rs. } 9,000\\text{ cr}$$", "structuredSteps": [{"stepNumber": 1, "title": "Calculate CRR Allocation", "explanation": "Multiply NDTL by 4.5%.", "formula": "$$\\text{CRR} = 50000 \\times 0.045 = 2250\\text{ cr}$$", "tip": "Maintained in cash with RBI."}, {"stepNumber": 2, "title": "Calculate SLR Allocation", "explanation": "Multiply NDTL by 18%.", "formula": "$$\\text{SLR} = 50000 \\times 0.18 = 9000\\text{ cr}$$", "tip": "Maintained in G-Secs, T-Bills, cash."}, {"stepNumber": 3, "title": "Determine Pre-Emption Total", "explanation": "Add CRR and SLR amounts.", "formula": "$$2250 + 9000 = 11250\\text{ cr}$$", "tip": "Pre-empts 22.5% of bank resources from lending."}], "keyTakeaways": ["NDTL is calculated as Demand Liabilities + Time Liabilities + Other Demand & Time Liabilities - Interbank Claims.", "Banks face statutory penal interest from RBI if daily CRR falls below minimum prescription."], "quickCheckItems": [{"question": "Which of the following assets qualifies for Statutory Liquidity Ratio (SLR) compliance?", "options": ["Government dated securities", "Commercial paper", "Corporate debentures", "Equity shares"], "correctOptionIndex": 0, "explanation": "SLR can be maintained in cash, gold, and unencumbered Government and other approved securities (G-Secs, T-Bills)."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000032'::uuid,
        'BFA',
        'BFA_NPA_MANAGEMENT',
        'BFA_CAPITAL_ADEQUACY',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'The Basel III framework mandates the Capital to Risk-Weighted Assets Ratio (CRAR) to prevent systemic insolvency by requiring banks to maintain a minimum capital cushion against credit, market, and operational risk. Under RBI norms, scheduled commercial banks must maintain a minimum CRAR of $$9.0\%$$ ($$11.5\%$$ including the $$2.5\%$$ Capital Conservation Buffer - CCB), with Common Equity Tier-1 (CET-1) representing the highest-quality loss-absorbing core capital ($$\ge 5.5\%$$).',
        'Confusing Tier 1 capital (gone-concern vs going-concern): Tier 1 capital absorbs losses on a going-concern basis, while Tier 2 capital absorbs losses only upon liquidation/resolution (gone-concern).',
        '["CRAR Formula: $$\\text{CRAR} = \\frac{\\text{Tier 1 Capital} + \\text{Tier 2 Capital}}{\\text{Total Risk Weighted Assets (RWA)}} \\times 100\\%$$", "RBI Minimum Baseline: $$\\text{CRAR} \\ge 9.0\\% \\quad (11.5\\% \\text{ with } 2.5\\% \\text{ CCB})$$", "CET-1 Baseline: $$\\text{CET-1} \\ge 5.5\\% \\quad (8.0\\% \\text{ with } 2.5\\% \\text{ CCB})$$"]',
        '{"questionStem": "A public sector bank has Tier 1 Capital of $$\\text{Rs. } 8,000\\text{ crores}$$, Tier 2 Capital of $$\\text{Rs. } 4,000\\text{ crores}$$, and Total Risk-Weighted Assets (RWA) of $$\\text{Rs. } 100,000\\text{ crores}$$. Calculate the bank''s CRAR and evaluate whether it meets RBI''s minimum regulatory requirement including the Capital Conservation Buffer (CCB).", "steps": ["Step 1: Compute Total Regulatory Capital: $$\\text{Total Capital} = \\text{Tier 1} + \\text{Tier 2} = 8000 + 4000 = \\text{Rs. } 12,000\\text{ crores}$$.", "Step 2: Apply the CRAR formula: $$\\text{CRAR} = \\frac{\\text{Total Capital}}{\\text{RWA}} \\times 100\\% = \\frac{12000}{100000} \\times 100\\% = 12.00\\%$$.", "Step 3: Evaluate compliance: The RBI mandated minimum CRAR is $$9.0\\%$$ (or $$11.50\\%$$ including the $$2.50\\%$$ Capital Conservation Buffer). Since the bank''s CRAR of $$12.00\\%$$ exceeds $$11.50\\%$$, the bank comfortably complies with RBI capital adequacy regulations."], "finalAnswer": "$$\\text{CRAR} = 12.00\\% \\quad (\\text{Compliant})$$", "structuredSteps": [{"stepNumber": 1, "title": "Sum Eligible Capital", "explanation": "Add Tier 1 and Tier 2 capital.", "formula": "$$\\text{Capital} = 8000 + 4000 = 12000\\text{ cr}$$", "tip": "Tier 2 cannot exceed 100% of Tier 1."}, {"stepNumber": 2, "title": "Divide by RWA", "explanation": "Express capital as percentage of risk-weighted assets.", "formula": "$$\\text{CRAR} = \\frac{12000}{100000} \\times 100 = 12.00\\%$$", "tip": "RWA adjusts asset values by risk weights."}, {"stepNumber": 3, "title": "Verify RBI Benchmark", "explanation": "Compare with 11.5% hurdle rate.", "formula": "$$12.00\\% \\ge 11.50\\% \\implies \\text{Passes}$$", "tip": "Excess capital provides room for asset growth."}], "keyTakeaways": ["Risk-Weighted Assets assign higher risk weights to unrated corporate loans ($$100\\%$$) than sovereign zero-risk government bonds ($$0\\%$$).", "Failure to meet CRAR triggers Prompt Corrective Action (PCA) by RBI."], "quickCheckItems": [{"question": "What is the minimum Capital to Risk-Weighted Assets Ratio (CRAR) prescribed by RBI for commercial banks without CCB?", "options": ["$$9.0\\%$$", "$$8.0\\%$$", "$$11.5\\%$$", "$$6.0\\%$$"], "correctOptionIndex": 0, "explanation": "RBI prescribes minimum CRAR of 9.0% for Indian commercial banks (higher than Basel III global minimum of 8.0%)."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000033'::uuid,
        'GS',
        'INDIAN_POLITY',
        'IP_FUNDAMENTAL_RIGHTS',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Part III of the Constitution of India (Articles 12–35), described as the Magna Carta of India, guarantees justiciable Fundamental Rights. Under Article 32, Dr. B.R. Ambedkar termed the Right to Constitutional Remedies the ''heart and soul of the Constitution''. The Supreme Court (Article 32) and High Courts (Article 226) enforce these rights via five prerogative writs: (1) Habeas Corpus (produce the body against unlawful detention), (2) Mandamus (command a public authority to perform a statutory duty), (3) Prohibition (halt excess jurisdiction by inferior tribunals), (4) Certiorari (quash an illegal order passed without jurisdiction), and (5) Quo Warranto (challenge unlawful usurpation of a public office).',
        'Believing that Article 32 writ jurisdiction is broader than Article 226. High Courts (Article 226) possess broader writ jurisdiction because they can issue writs for Fundamental Rights AND ordinary legal rights, whereas the Supreme Court (Article 32) issues writs strictly for Fundamental Rights.',
        '["Jurisdiction Scope: $$\\text{Art. 226 (HC: FRs + Legal Rights)} \\quad \\succ \\quad \\text{Art. 32 (SC: FRs only)}$$", "Emergency Invariant: $$\\text{Articles 20 and 21 cannot be suspended even during National Emergency (44th Amendment Act, 1978)}$$", "Golden Triangle: $$\\text{Articles 14, 19, and 21 form an indivisible nexus (Maneka Gandhi Case, 1978)}$$"]',
        '{"questionStem": "A private citizen is detained by state police authorities for 48 hours without being informed of the grounds of arrest and without being produced before a Judicial Magistrate. Which prerogative writ is the primary constitutional remedy under Article 32 / 226 to secure the citizen''s immediate release?", "steps": ["Step 1: Identify the violated constitutional protection: Article 22(2) mandates that every person arrested must be produced before the nearest magistrate within 24 hours of arrest, excluding journey time.", "Step 2: Non-compliance with Article 22(2) renders the detention unlawful, directly infringing personal liberty guaranteed under Article 21.", "Step 3: Identify the appropriate judicial writ: The Latin writ ''Habeas Corpus'' literally translates to ''to have the body''. It is issued by the Supreme Court or High Courts directing the detaining authority to produce the incarcerated person before the court to inspect the legality of detention and order immediate release if found unlawful."], "finalAnswer": "$$\\text{Writ of Habeas Corpus}$$", "structuredSteps": [{"stepNumber": 1, "title": "Analyze Constitutional Breach", "explanation": "Failure to produce within 24 hours violates Article 22(2).", "formula": "$$\\text{Breach of Article 21 and 22(2)}$$", "tip": "Magistrate production within 24 hours is mandatory."}, {"stepNumber": 2, "title": "Identify Writ Mechanism", "explanation": "Habeas Corpus commands production of the detainee.", "formula": "$$\\text{Habeas Corpus = ''You may have the body''}$$", "tip": "Can be issued against both public and private entities."}, {"stepNumber": 3, "title": "Judicial Remedy", "explanation": "Court orders immediate liberation.", "formula": "$$\\text{Release from Unlawful Custody}$$", "tip": "Mandamus cannot lie against purely private persons, but Habeas Corpus can."}], "keyTakeaways": ["Habeas Corpus can be issued against both public authorities and private individuals.", "Mandamus cannot be issued against the President of India or State Governors for executive duties.", "Quo Warranto prevents illegal usurpation of a public substantive office."], "quickCheckItems": [{"question": "Which Fundamental Rights cannot be suspended even during a National Emergency proclaimed under Article 352?", "options": ["Articles 20 and 21", "Articles 19 and 20", "Articles 14 and 19", "Articles 21 and 22"], "correctOptionIndex": 0, "explanation": "The 44th Constitutional Amendment Act 1978 prevents suspension of Articles 20 (protection in respect of conviction) and 21 (right to life and personal liberty)."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000034'::uuid,
        'GS',
        'INDIAN_POLITY',
        'IP_PREAMBLE',
        'CONCEPT',
        'FOUNDATION',
        1,
        NULL::uuid,
        4,
        'The Preamble to the Constitution of India is based on the ''Objective Resolution'' moved by Jawaharlal Nehru on December 13, 1946, and adopted by the Constituent Assembly on January 22, 1947. It declares India to be a ''Sovereign Socialist Secular Democratic Republic'' committed to securing Justice, Liberty, Equality, and Fraternity for all citizens. In the landmark Kesavananda Bharati case (1973), the Supreme Court ruled that the Preamble IS an integral part of the Constitution and can be amended under Article 368, provided its basic structure remains intact. The Preamble has been amended only once: by the 42nd Constitutional Amendment Act of 1976, which added three words: ''Socialist'', ''Secular'', and ''Integrity''.',
        'Believing that the Preamble is enforceable in a court of law. The Preamble is non-justiciable: its provisions cannot be enforced in courts of law independently.',
        '["Opening Source: \\text{''We, the People of India'' establishes popular sovereignty}", "42nd Amendment Additions: \\text{''Socialist'', ''Secular'', and ''and Integrity'' (1976)}", "Preamble Jurisprudence: \\text{Berubari (1960: Not a part) } \\to \\text{Kesavananda Bharati (1973: Integral part & amendable under Basic Structure)}"]',
        '{"questionStem": "Which three words were inserted into the Preamble of the Constitution of India by the 42nd Constitutional Amendment Act of 1976?", "steps": ["Step 1: Examine the historical wording prior to 1976: India was originally declared a ''Sovereign Democratic Republic''.", "Step 2: Identify the amendments made under the 42nd Amendment Act 1976: In paragraph 1, ''Sovereign Democratic Republic'' was substituted with ''Sovereign Socialist Secular Democratic Republic''.", "Step 3: In paragraph 6, the phrase ''unity of the Nation'' was substituted with ''unity and integrity of the Nation''. Thus, the three added words are ''Socialist'', ''Secular'', and ''Integrity''."], "finalAnswer": "$$\\text{Socialist, Secular, and Integrity}$$", "structuredSteps": [{"stepNumber": 1, "title": "Examine Original 1950 Text", "explanation": "Original text had Sovereign Democratic Republic.", "formula": "$$\\text{Sovereign Democratic Republic (1950)}$$", "tip": "Objective Resolution root."}, {"stepNumber": 2, "title": "Review 42nd Amendment 1976", "explanation": "Mini-Constitution added three specific terms.", "formula": "$$\\text{Added: Socialist, Secular, Integrity}$$", "tip": "Enacted during Emergency."}, {"stepNumber": 3, "title": "Current Formulation", "explanation": "Sovereign Socialist Secular Democratic Republic.", "formula": "$$\\text{Unity and Integrity of the Nation}$$", "tip": "Only amended once in history."}], "keyTakeaways": ["The Preamble indicates the source of constitutional authority: The People of India.", "Date of adoption recorded in the Preamble: November 26, 1949."], "quickCheckItems": [{"question": "In which landmark verdict did the Supreme Court explicitly rule that the Preamble is an integral part of the Constitution?", "options": ["Kesavananda Bharati case (1973)", "Berubari Union case (1960)", "Golaknath case (1967)", "Minerva Mills case (1980)"], "correctOptionIndex": 0, "explanation": "In Kesavananda Bharati (1973), the 13-judge constitutional bench overruled Berubari (1960) and declared the Preamble an integral part of the Constitution."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000035'::uuid,
        'ENG',
        'ERROR_DETECTION',
        'ED_MODIFIERS',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        4,
        'Subject-verb agreement requires concord between the grammatical subject and its finite verb, regardless of intervening prepositional phrases, parenthetical clauses, or inverted order. The most deceptive exam trap is the Proximity Fallacy: placing a plural noun right next to the verb (e.g., ''The quality of the mangoes was/were'') tricking the ear into selecting the plural verb. When subjects are joined by ''with'', ''together with'', ''as well as'', ''along with'', or ''in addition to'', the verb agrees exclusively with the FIRST subject.',
        'Allowing intervening prepositional objects (like ''of the mangoes'') to dictate the number of the verb instead of the true head noun (''quality'').',
        '["First-Subject Concord: $$\\text{Subj}_1 + \\{\\text{as well as, along with, together with}\\} + \\text{Subj}_2 \\implies \\text{Verb agrees with } \\text{Subj}_1$$", "Proximity Rule: $$\\text{Neither...nor / Either...or} \\implies \\text{Verb agrees with the CLOSEST subject}$$", "Each / Every Rule: $$\\text{Each / Every / Either / Neither of + Plural Noun} \\implies \\text{SINGULAR VERB}$$"]',
        '{"questionStem": "Identify the grammatical error in the sentence: ''The quality of the freshly harvested Alphonso mangoes from the coastal orchards were outstanding.''", "steps": ["Step 1: Identify the main clause subject and verb: The sentence discusses ''The quality ... were outstanding''.", "Step 2: Parse the prepositional modifiers: ''of the freshly harvested Alphonso mangoes'' and ''from the coastal orchards'' are prepositional phrases modifying the singular head noun ''quality''.", "Step 3: Concord analysis: The head noun ''quality'' is an uncountable singular abstract noun. Therefore, the plural auxiliary verb ''were'' is grammatically incorrect and must be replaced by the singular verb ''was''."], "finalAnswer": "$$\\text{Replace ''were'' with ''was'' (Singular subject ''quality'')}$$", "structuredSteps": [{"stepNumber": 1, "title": "Isolate the Head Noun", "explanation": "Strip away all intervening prepositional phrases.", "formula": "$$\\text{Head Noun: ''quality'' (Singular)}$$", "tip": "Never let ''mangoes'' trick your ear."}, {"stepNumber": 2, "title": "Locate Governing Verb", "explanation": "The finite verb in predicate is ''were''.", "formula": "$$\\text{Verb: ''were'' (Plural)}$$", "tip": "Singular subject requires singular verb."}, {"stepNumber": 3, "title": "Enforce Concord", "explanation": "Substitute singular verb form.", "formula": "$$\\text{The quality was outstanding}$$", "tip": "Proximity trap successfully neutralized."}], "keyTakeaways": ["Always identify the head noun before the first preposition.", "Words joined by ''as well as'' or ''along with'' are parenthetical and do not compound the subject."], "quickCheckItems": [{"question": "Choose the grammatically correct sentence:", "options": ["The captain, along with all his crew members, was rescued.", "The captain, along with all his crew members, were rescued.", "Neither the teacher nor the students was present.", "Each of the participants have received a certificate."], "correctOptionIndex": 0, "explanation": "With ''along with'', the verb agrees strictly with the first subject (''The captain'', singular), so ''was rescued'' is correct."}]}'
    ),
    (
        'b1010000-0000-0000-0000-000000000036'::uuid,
        'CS',
        'NETWORKING_CYBER_SECURITY',
        'NCS_DEVICES',
        'CONCEPT',
        'CORE',
        1,
        NULL::uuid,
        5,
        'Computer networking equipment maps strictly to the OSI 7-Layer reference architecture: (1) Hubs and Repeaters operate at Layer 1 (Physical) by blindly broadcasting bit streams; (2) Switches and Bridges operate at Layer 2 (Data Link) using hardware MAC addresses ($$48\text{ bits}$$) to create isolated collision domains; (3) Routers operate at Layer 3 (Network) using logical IP addresses to direct packets across broadcast domains and networks; (4) Gateways operate at Layer 4–7 to translate protocols. An IPv4 address comprises $$32\text{ bits}$$, segmented into Network ID and Host ID by the subnet mask.',
        'Confusing collision domains with broadcast domains: A Layer 2 switch breaks collision domains per port, but maintains a single shared broadcast domain across all ports.',
        '["Layer-Device Mapping: $$\\text{L1: Hub}, \\quad \\text{L2: Switch (MAC)}, \\quad \\text{L3: Router (IP)}, \\quad \\text{L4-7: Gateway}$$", "Subnet Host Capacity: $$\\text{Usable Hosts} = 2^{(32 - \\text{CIDR})} - 2$$", "Address Metrics: $$\\text{MAC: 48 bits (6 octets)}, \\quad \\text{IPv4: 32 bits (4 octets)}, \\quad \\text{IPv6: 128 bits (16 octets)}$$"]',
        '{"questionStem": "A computer network is configured with the classless IPv4 subnet $$192.168.10.0/26$$. Calculate the maximum number of assignable, usable host IP addresses in this subnet.", "steps": ["Step 1: Determine the total number of host bits available: IPv4 addresses have $$32\\text{ bits}$$. With prefix length $$CIDR = 26$$, the host bits are $$h = 32 - 26 = 6\\text{ bits}$$.", "Step 2: Calculate total possible IP addresses in the subnet: $$2^h = 2^6 = 64\\text{ addresses}$$.", "Step 3: Subtract reserved non-assignable addresses: Exactly $$2$$ addresses in every subnet cannot be assigned to hosts: the first address ($$192.168.10.0$$) is the Network ID, and the last address ($$192.168.10.63$$) is the Directed Broadcast Address. Maximum usable hosts $$= 2^6 - 2 = 64 - 2 = 62$$."], "finalAnswer": "$$\\text{Usable Hosts} = 62$$", "structuredSteps": [{"stepNumber": 1, "title": "Calculate Host Bits", "explanation": "Subtract prefix from 32 bits.", "formula": "$$h = 32 - 26 = 6\\text{ bits}$$", "tip": "Prefix /26 leaves 6 bits for hosts."}, {"stepNumber": 2, "title": "Compute Total Space", "explanation": "Evaluate 2 to the power of host bits.", "formula": "$$2^6 = 64\\text{ addresses}$$", "tip": "Subnet mask is 255.255.255.192."}, {"stepNumber": 3, "title": "Deduct Network & Broadcast", "explanation": "Subtract 2 reserved addresses.", "formula": "$$64 - 2 = 62\\text{ hosts}$$", "tip": "Always subtract 2 for network and broadcast."}], "keyTakeaways": ["Switches inspect MAC frame headers; Routers inspect IP packet headers.", "Formula for usable hosts is always $$2^h - 2$$."], "quickCheckItems": [{"question": "At which OSI reference model layer does a standard network switch operate?", "options": ["Data Link Layer (Layer 2)", "Physical Layer (Layer 1)", "Network Layer (Layer 3)", "Transport Layer (Layer 4)"], "correctOptionIndex": 0, "explanation": "Standard switches operate at Layer 2 (Data Link Layer), inspecting Ethernet frame destination MAC addresses."}]}'
    )
) AS v(
    id, subject_code, topic_code, subtopic_code,
    lesson_type, difficulty_level, sequence_no, prerequisite_lesson_id,
    estimated_minutes, core_concept, trap, cheat_sheet, step_by_step_breakdown
)
JOIN question_service.subject s
  ON (s.code = v.subject_code OR s.name = v.subject_code) AND (s.tenant_id IS NULL OR s.tenant_id = 'default')
JOIN question_service.topic t
  ON (t.code = v.topic_code OR t.name = v.topic_code) AND t.subject_id = s.id AND (t.tenant_id IS NULL OR t.tenant_id = 'default')
JOIN LATERAL (
    SELECT id FROM question_service.subtopic
    WHERE topic_id = t.id AND (v.subtopic_code IS NULL OR code = v.subtopic_code OR name = v.subtopic_code)
    ORDER BY CASE WHEN code = v.subtopic_code THEN 0 WHEN name = v.subtopic_code THEN 1 ELSE 2 END, display_order ASC
    LIMIT 1
) st ON TRUE
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- Seed Initial Effectiveness Telemetry Baselines
-- =============================================================================
INSERT INTO question_service.lesson_effectiveness (lesson_id, views, completions, post_lesson_accuracy_delta)
SELECT id, 0, 0, 0.0
FROM question_service.subtopic_micro_lesson
WHERE id IN (
    'b1010000-0000-0000-0000-000000000001'::uuid,
    'b1010000-0000-0000-0000-000000000002'::uuid,
    'b1010000-0000-0000-0000-000000000003'::uuid,
    'b1010000-0000-0000-0000-000000000004'::uuid,
    'b1010000-0000-0000-0000-000000000005'::uuid,
    'b1010000-0000-0000-0000-000000000006'::uuid,
    'b1010000-0000-0000-0000-000000000007'::uuid,
    'b1010000-0000-0000-0000-000000000008'::uuid,
    'b1010000-0000-0000-0000-000000000009'::uuid,
    'b1010000-0000-0000-0000-000000000010'::uuid,
    'b1010000-0000-0000-0000-000000000011'::uuid,
    'b1010000-0000-0000-0000-000000000012'::uuid,
    'b1010000-0000-0000-0000-000000000013'::uuid,
    'b1010000-0000-0000-0000-000000000014'::uuid,
    'b1010000-0000-0000-0000-000000000015'::uuid,
    'b1010000-0000-0000-0000-000000000016'::uuid,
    'b1010000-0000-0000-0000-000000000017'::uuid,
    'b1010000-0000-0000-0000-000000000018'::uuid,
    'b1010000-0000-0000-0000-000000000019'::uuid,
    'b1010000-0000-0000-0000-000000000020'::uuid,
    'b1010000-0000-0000-0000-000000000021'::uuid,
    'b1010000-0000-0000-0000-000000000022'::uuid,
    'b1010000-0000-0000-0000-000000000023'::uuid,
    'b1010000-0000-0000-0000-000000000024'::uuid,
    'b1010000-0000-0000-0000-000000000025'::uuid,
    'b1010000-0000-0000-0000-000000000026'::uuid,
    'b1010000-0000-0000-0000-000000000027'::uuid,
    'b1010000-0000-0000-0000-000000000028'::uuid,
    'b1010000-0000-0000-0000-000000000029'::uuid,
    'b1010000-0000-0000-0000-000000000030'::uuid,
    'b1010000-0000-0000-0000-000000000031'::uuid,
    'b1010000-0000-0000-0000-000000000032'::uuid,
    'b1010000-0000-0000-0000-000000000033'::uuid,
    'b1010000-0000-0000-0000-000000000034'::uuid,
    'b1010000-0000-0000-0000-000000000035'::uuid,
    'b1010000-0000-0000-0000-000000000036'::uuid
)
ON CONFLICT (lesson_id) DO NOTHING;

-- =============================================================================
-- Seed Canonical Multilingual Micro-Lesson Translations (Hindi)
-- =============================================================================
INSERT INTO question_service.microlesson_translation (
    id, microlesson_id, tenant_id, language_code,
    core_concept, trap, cheat_sheet, step_by_step_breakdown,
    source_version, status, translator_id, reviewer_id, review_comments
)
VALUES
(
    gen_random_uuid(),
    'b1010000-0000-0000-0000-000000000001'::uuid,
    'default',
    'hi',
    'प्रतिशत एक आयामहीन अनुपात है जो सौ के अंश के रूप में सापेक्ष परिवर्तन को मापता है। भारतीय बाज़ार में खरीदारी की कल्पना करें: जब कोई व्यापारी प्रारंभिक छूट के बाद अतिरिक्त छूट देता है, तो कुल प्रभाव साधारण जोड़ नहीं होता, क्योंकि प्रत्येक आगामी छूट घटे हुए मूल्य पर लागू होती है। क्रमिक प्रतिशत परिवर्तनों का प्रभावी शुद्ध मान सूत्र $$\text{Net Change} = \left(a + b + \frac{ab}{100}\right)\%$$ द्वारा निर्धारित होता है।',
    'सबसे आम परीक्षा त्रुटि क्रमिक प्रतिशत परिवर्तनों को सीधे जोड़ना है: यह मानना कि $$20\%$$ की वृद्धि के बाद $$20\%$$ की कमी मूल्य को अपरिवर्तित रखती है, जबकि वास्तव में यह $$4\%$$ की शुद्ध हानि उत्पन्न करती है।',
    '["क्रमिक शुद्ध परिवर्तन: $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$", "प्रतिशत परिवर्तन: $$\\text{प्रतिशत परिवर्तन} = \\left(\\frac{\\text{अंतिम} - \\text{प्रारंभिक}}{\\text{प्रारंभिक}}\\right) \\times 100\\%$$", "गुणक रूप: $$\\text{गुणक} = 1 + \\frac{x}{100} \\quad (\\text{वृद्धि}), \\quad 1 - \\frac{x}{100} \\quad (\\text{कमी})$$"]'::jsonb,
    '{"questionStem": "किसी वस्तु के मूल्य में पहले $$25\\%$$ की वृद्धि की जाती है और बाद में $$20\\%$$ की कमी की जाती है। वस्तु के अंतिम मूल्य में शुद्ध प्रतिशत परिवर्तन ज्ञात कीजिए।", "steps": ["चरण 1: दो क्रमिक प्रतिशत मानों की पहचान करें: $$a = +25\\%$$ और $$b = -20\\%$$.", "चरण 2: क्रमिक शुद्ध प्रतिशत सूत्र लागू करें: $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$.", "चरण 3: मान प्रतिस्थापित करें: $$\\text{Net Change} = \\left(25 - 20 + \\frac{25 \\times (-20)}{100}\\right)\\% = (5 - 5)\\% = 0\\%$$. अतः अंतिम मूल्य अपरिवर्तित रहता है।"], "finalAnswer": "$$\\text{शुद्ध परिवर्तन} = 0\\% \\quad (\\text{मूल्य अपरिवर्तित})$$", "structuredSteps": [{"stepNumber": 1, "title": "दरों की पहचान", "explanation": "प्रथम वृद्धि को +25% और कमी को -20% मानें।", "formula": "$$a = +25, \\quad b = -20$$", "tip": "कमी के लिए ऋणात्मक चिह्न अनिवार्य है।"}, {"stepNumber": 2, "title": "सूत्र प्रयोग", "explanation": "क्रमिक परिवर्तन सूत्र में मान रखें।", "formula": "$$\\text{Net Change} = a + b + \\frac{ab}{100}$$", "tip": "गुणनफल पद संयुक्त प्रभाव दर्शाता है।"}, {"stepNumber": 3, "title": "गणना", "explanation": "अंतिम मान ज्ञात करें।", "formula": "$$25 - 20 - 5 = 0\\%$$", "tip": "25% की वृद्धि 20% की कमी से संतुलित होती है क्योंकि आधार बड़ा हो चुका था।"}], "keyTakeaways": ["प्रतिशत परिवर्तन आगामी सभी गणनाओं के लिए आधार मूल्य को बदल देता है।", "समान प्रतिशत की वृद्धि और कमी सदैव शुद्ध हानि की ओर ले जाती है।"]}'::jsonb,
    1,
    'PUBLISHED',
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Verified high-quality Hindi translation for canonical micro-tutorial.'
),
(
    gen_random_uuid(),
    'b1010000-0000-0000-0000-000000000005'::uuid,
    'default',
    'hi',
    'लाभ एवं हानि की गणना सदैव क्रय मूल्य ($$\text{CP}$$) के आधार पर की जाती है, जबकि छूट की गणना सदैव अंकित मूल्य ($$\text{MP}$$) पर की जाती है। स्थानीय दुकान की कल्पना करें: दुकानदार मोलभाव की गुंजाइश रखने के लिए लागत से अधिक अंकित करता है और फिर छूट देकर भी लाभ अर्जित करता है। यह संबंध इस संतुलन समीकरण द्वारा बंधा है: $$\text{MP} \times \left(1 - \frac{d\%}{100}\right) = \text{CP} \times \left(1 + \frac{p\%}{100}\right) = \text{SP}$$.',
    'अंकित मूल्य के स्थान पर क्रय मूल्य पर छूट की गणना करना, अथवा क्रय मूल्य के स्थान पर विक्रय मूल्य पर लाभ प्रतिशत की गणना करना।',
    '["लाभ/हानि प्रतिशत: $$\\text{लाभ}\\% = \\left(\\frac{\\text{SP} - \\text{CP}}{\\text{CP}}\\right) \\times 100\\%$$", "स्वर्ण संतुलन समीकरण: $$\\text{MP} \\times \\left(1 - \\frac{d}{100}\\right) = \\text{CP} \\times \\left(1 + \\frac{p}{100}\\right)$$", "अंकित/क्रय अनुपात: $$\\frac{\\text{MP}}{\\text{CP}} = \\frac{100 + p\\%}{100 - d\\%}$$"]'::jsonb,
    '{"questionStem": "एक व्यापारी $$\\text{Rs. } 1200$$ में एक घड़ी खरीदता है। $$10\\%$$ की छूट देने के बाद भी $$20\\%$$ का लाभ कमाने के लिए उसे घड़ी पर कितना मूल्य अंकित करना चाहिए?", "steps": ["चरण 1: क्रय मूल्य ($$\\text{CP} = 1200$$) पर $$20\\%$$ लाभ प्राप्त करने हेतु आवश्यक विक्रय मूल्य ($$\\text{SP}$$) की गणना करें: $$\\text{SP} = 1200 \\times 1.20 = \\text{Rs. } 1440$$.", "चरण 2: $$10\\%$$ छूट के आधार पर विक्रय मूल्य को अंकित मूल्य से जोड़ें: $$\\text{SP} = \\text{MP} \\times 0.90$$.", "चरण 3: अंकित मूल्य ज्ञात करें: $$\\text{MP} = \\frac{1440}{0.90} = \\text{Rs. } 1600$$."], "finalAnswer": "$$\\text{अंकित मूल्य} = \\text{Rs. } 1600$$", "structuredSteps": [{"stepNumber": 1, "title": "विक्रय मूल्य गणना", "explanation": "1200 पर 20% लाभ जोड़ें।", "formula": "$$\\text{SP} = 1200 \\times 1.2 = 1440$$", "tip": "लाभ सदैव CP पर होता है।"}, {"stepNumber": 2, "title": "छूट संबंध", "explanation": "अंकित मूल्य और SP का समीकरण बनाएं।", "formula": "$$\\text{SP} = 0.90 \\times \\text{MP}$$", "tip": "छूट सदैव MP पर होती है।"}, {"stepNumber": 3, "title": "अंकित मूल्य समाधान", "explanation": "1440 को 0.90 से विभाजित करें।", "formula": "$$\\text{MP} = 1600$$", "tip": "अनुपात 120/90 = 4/3 प्रयोग करें।"}], "keyTakeaways": ["क्रय मूल्य से विक्रय मूल्य का संबंध लाभ या हानि प्रतिशत से है।", "अंकित मूल्य से विक्रय मूल्य का संबंध छूट प्रतिशत से है।"]}'::jsonb,
    1,
    'PUBLISHED',
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Verified high-quality Hindi translation for canonical micro-tutorial.'
),
(
    gen_random_uuid(),
    'b1010000-0000-0000-0000-000000000033'::uuid,
    'default',
    'hi',
    'भारतीय संविधान का भाग III (अनुच्छेद 12–35), जिसे भारत का मैग्ना कार्टा कहा जाता है, वादयोग्य मूल अधिकारों की गारंटी देता है। अनुच्छेद 32 के तहत संवैधानिक उपचारों के अधिकार को डॉ. बी.आर. अम्बेडकर ने ''संविधान की आत्मा एवं हृदय'' कहा था। सर्वोच्च न्यायालय (अनुच्छेद 32) और उच्च न्यायालय (अनुच्छेद 226) इन अधिकारों के प्रवर्तन के लिए पाँच प्रकार की रिट जारी करते हैं: (1) बंदी प्रत्यक्षीकरण (अवैध हिरासत से मुक्ति), (2) परमादेश (लोक प्राधिकारी को वैधानिक कर्तव्य का आदेश), (3) प्रतिषेध (अधीनस्थ न्यायाधिकरण को अधिकार क्षेत्र अतिक्रमण से रोकना), (4) उत्प्रेषण (अवैध आदेश को रद्द करना), और (5) अधिकार पृच्छा (सार्वजनिक पद के अवैध दावे की जांच)।',
    'यह मानना कि अनुच्छेद 32 का क्षेत्र अनुच्छेद 226 से व्यापक है। वास्तव में उच्च न्यायालयों (अनुच्छेद 226) का रिट क्षेत्राधिकार व्यापक है क्योंकि वे मूल अधिकारों के साथ-साथ अन्य विधिक अधिकारों के लिए भी रिट जारी कर सकते हैं।',
    '["रिट क्षेत्राधिकार: $$\\text{अनु. 226 (उच्च न्यायालय: मूल + विधिक अधिकार)} \\succ \\text{अनु. 32 (सर्वोच्च न्यायालय: केवल मूल अधिकार)}$$", "आपातकालीन सुरक्षा: $$\\text{अनुच्छेद 20 और 21 राष्ट्रीय आपातकाल में भी निलंबित नहीं हो सकते (44वाँ संशोधन)}$$", "स्वर्ण त्रिकोण: $$\\text{अनुच्छेद 14, 19, एवं 21 का अविभाज्य संबंध (मेनका गांधी वाद, 1978)}$$"]'::jsonb,
    '{"questionStem": "यदि किसी नागरिक को बिना किसी विधिक आधार और 24 घंटे के भीतर मजिस्ट्रेट के समक्ष प्रस्तुत किए बिना पुलिस द्वारा अवैध रूप से हिरासत में रखा जाता है, तो उसकी तत्काल रिहाई के लिए कौन सी संवैधानिक रिट प्राथमिक उपचार है?", "steps": ["चरण 1: अनुच्छेद 22(2) के तहत संवैधानिक उल्लंघन की पहचान करें, जो प्रत्येक गिरफ्तार व्यक्ति को 24 घंटे के भीतर निकटतम मजिस्ट्रेट के समक्ष प्रस्तुत करने का आदेश देता है।", "चरण 2: 24 घंटे में मजिस्ट्रेट के समक्ष प्रस्तुत न करना हिरासत को अवैध बना देता है, जो अनुच्छेद 21 द्वारा प्रदत्त व्यक्तिगत स्वतंत्रता का प्रत्यक्ष हनन है।", "चरण 3: लैटिन शब्द ''बंदी प्रत्यक्षीकरण'' (Habeas Corpus) का शाब्दिक अर्थ ''सशरीर प्रस्तुत करना'' है। न्यायालय हिरासत में लेने वाले अधिकारी को बंदी को न्यायालय के समक्ष प्रस्तुत करने और अवैध पाए जाने पर तत्काल मुक्त करने का आदेश देता है।"], "finalAnswer": "$$\\text{बंदी प्रत्यक्षीकरण रिट (Writ of Habeas Corpus)}$$", "structuredSteps": [{"stepNumber": 1, "title": "संवैधानिक उल्लंघन की पहचान", "explanation": "24 घंटे के भीतर मजिस्ट्रेट के समक्ष न ले जाना अनुच्छेद 22(2) का उल्लंघन है।", "formula": "$$\\text{अनुच्छेद 21 एवं 22(2) का हनन}$$", "tip": "24 घंटे की समय सीमा कठोर है।"}, {"stepNumber": 2, "title": "उचित रिट का निर्धारण", "explanation": "हिरासत की वैधता की न्यायिक समीक्षा।", "formula": "$$\\text{Habeas Corpus = सशरीर उपस्थित करना}$$", "tip": "सरकारी एवं निजी दोनों के विरुद्ध जारी हो सकती है।"}, {"stepNumber": 3, "title": "संवैधानिक उपचार", "explanation": "अवैध हिरासत से तत्काल मुक्ति का न्यायिक आदेश।", "formula": "$$\\text{तत्काल रिहाई}$$", "tip": "व्यक्तिगत स्वतंत्रता का सर्वोच्च रक्षक।"}], "keyTakeaways": ["बंदी प्रत्यक्षीकरण सार्वजनिक प्राधिकारी और निजी व्यक्ति दोनों के विरुद्ध जारी हो सकती है।", "परमादेश राष्ट्रपति या राज्यपाल के विरुद्ध उनके शासकीय कर्तव्यों के संबंध में जारी नहीं की जा सकती।"]}'::jsonb,
    1,
    'PUBLISHED',
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Verified high-quality Hindi translation for canonical micro-tutorial.'
)
ON CONFLICT (microlesson_id, language_code, tenant_id) DO NOTHING;
