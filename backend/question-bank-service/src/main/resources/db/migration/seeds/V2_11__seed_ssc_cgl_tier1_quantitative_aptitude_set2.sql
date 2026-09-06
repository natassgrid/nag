-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 3: Quantitative Aptitude (Set 2: 25 New Qs)
-- UUID Range: a10b0000-0000-0000-0000-000000000001 to a10b0000-0000-0000-0000-000000000025
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
    st.id,
    'Quantitative Aptitude / Mathematical Abilities',
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
    -- 1. Number Systems (EASY, UNDERSTAND)
    (
        'a10b0000-0000-0000-0000-000000000001'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'UNDERSTAND',
        'What least number must be subtracted from $$427398$$ so that the remaining number is divisible by $$15$$?',
        '[{"id":"A","text":"$$3$$","isCorrect":true},{"id":"B","text":"$$6$$","isCorrect":false},{"id":"C","text":"$$8$$","isCorrect":false},{"id":"D","text":"$$5$$","isCorrect":false}]',
        'A',
        '$$427398 = 15 \\times 28493 + 3$$. The remainder is $$3$$, so subtracting $$3$$ yields a number divisible by $$15$$.'
    ),
    -- 2. Number Systems (EASY, UNDERSTAND)
    (
        'a10b0000-0000-0000-0000-000000000002'::uuid,
        'Number Systems',
        'Fractions',
        'EASY',
        'UNDERSTAND',
        'Arrange the following fractions in ascending order: $$\\frac{3}{5}, \\frac{7}{10}, \\frac{11}{15}, \\frac{13}{20}$$.',
        '[{"id":"A","text":"$$\\frac{3}{5} < \\frac{13}{20} < \\frac{7}{10} < \\frac{11}{15}$$","isCorrect":true},{"id":"B","text":"$$\\frac{3}{5} < \\frac{7}{10} < \\frac{13}{20} < \\frac{11}{15}$$","isCorrect":false},{"id":"C","text":"$$\\frac{13}{20} < \\frac{3}{5} < \\frac{7}{10} < \\frac{11}{15}$$","isCorrect":false},{"id":"D","text":"$$\\frac{11}{15} < \\frac{7}{10} < \\frac{13}{20} < \\frac{3}{5}$$","isCorrect":false}]',
        'A',
        'Decimal values: $$\\frac{3}{5}=0.60$$, $$\\frac{13}{20}=0.65$$, $$\\frac{7}{10}=0.70$$, $$\\frac{11}{15}\\approx 0.733$$.'
    ),
    -- 3. Number Systems (EASY, UNDERSTAND)
    (
        'a10b0000-0000-0000-0000-000000000003'::uuid,
        'Number Systems',
        'Whole Numbers',
        'EASY',
        'UNDERSTAND',
        'Find the value of $$\\left(1 - \\frac{1}{2}\\right)\\left(1 - \\frac{1}{3}\\right)\\left(1 - \\frac{1}{4}\\right)\\cdots\\left(1 - \\frac{1}{50}\\right)$$.',
        '[{"id":"A","text":"$$\\frac{1}{50}$$","isCorrect":true},{"id":"B","text":"$$\\frac{2}{49}$$","isCorrect":false},{"id":"C","text":"$$\\frac{1}{25}$$","isCorrect":false},{"id":"D","text":"$$\\frac{49}{50}$$","isCorrect":false}]',
        'A',
        'Telescoping product: $$\\frac{1}{2} \\times \\frac{2}{3} \\times \\frac{3}{4} \\times \\cdots \\times \\frac{49}{50} = \\frac{1}{50}$$.'
    ),
    -- 4. Fundamental Arithmetical Operations - Percentages (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000004'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'If the price of petrol is increased by $$25\\%$$, by what percentage must a car owner reduce fuel consumption so that the overall expenditure remains constant?',
        '[{"id":"A","text":"$$20\\%$$","isCorrect":true},{"id":"B","text":"$$25\\%$$","isCorrect":false},{"id":"C","text":"$$16\\frac{2}{3}\\%$$","isCorrect":false},{"id":"D","text":"$$15\\%$$","isCorrect":false}]',
        'A',
        'Reduction percentage $$= \\frac{r}{100 + r} \\times 100\\% = \\frac{25}{125} \\times 100 = 20\\%$$.'
    ),
    -- 5. Fundamental Arithmetical Operations - Profit and Loss (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000005'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'A shopkeeper offers two successive discounts of $$20\\%$$ and $$10\\%$$ on an article marked at $$\\text{₹}1,500$$. What is the final selling price?',
        '[{"id":"A","text":"$$\\text{₹}1,080$$","isCorrect":true},{"id":"B","text":"$$\\text{₹}1,050$$","isCorrect":false},{"id":"C","text":"$$\\text{₹}1,120$$","isCorrect":false},{"id":"D","text":"$$\\text{₹}1,150$$","isCorrect":false}]',
        'A',
        'Equivalent discount $$= 20 + 10 - \\frac{200}{100} = 28\\%$$. Selling price $$= 1500 \\times (1 - 0.28) = 1500 \\times 0.72 = \\text{₹}1,080$$.'
    ),
    -- 6. Fundamental Arithmetical Operations - Ratio and Proportion (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000006'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'In a mixture of $$60\\text{ litres}$$, the ratio of milk and water is $$2 : 1$$. How much water must be added to make the ratio of milk and water $$1 : 2$$?',
        '[{"id":"A","text":"$$60\\text{ litres}$$","isCorrect":true},{"id":"B","text":"$$40\\text{ litres}$$","isCorrect":false},{"id":"C","text":"$$50\\text{ litres}$$","isCorrect":false},{"id":"D","text":"$$30\\text{ litres}$$","isCorrect":false}]',
        'A',
        'Initial: Milk $$= 40\\text{ L}$$, Water $$= 20\\text{ L}$$. To make ratio $$1:2$$, Water must be $$2 \\times 40 = 80\\text{ L}$$. Water to add $$= 80 - 20 = 60\\text{ L}$$.'
    ),
    -- 7. Fundamental Arithmetical Operations - Compound Interest (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000007'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'What is the difference between compound interest (compounded annually) and simple interest on a principal of $$\\text{₹}15,000$$ for $$2\\text{ years}$$ at an annual rate of $$8\\%$$?',
        '[{"id":"A","text":"$$\\text{₹}96$$","isCorrect":true},{"id":"B","text":"$$\\text{₹}120$$","isCorrect":false},{"id":"C","text":"$$\\text{₹}80$$","isCorrect":false},{"id":"D","text":"$$\\text{₹}104$$","isCorrect":false}]',
        'A',
        'Difference for 2 years $$= P \\left(\\frac{R}{100}\\right)^2 = 15000 \\times \\left(\\frac{8}{100}\\right)^2 = 15000 \\times \\frac{64}{10000} = \\text{₹}96$$.'
    ),
    -- 8. Fundamental Arithmetical Operations - Time and Work (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000008'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'A and B together can complete a piece of work in $$12\\text{ days}$$, while B alone can finish it in $$30\\text{ days}$$. In how many days can A alone complete the work?',
        '[{"id":"A","text":"$$20\\text{ days}$$","isCorrect":true},{"id":"B","text":"$$18\\text{ days}$$","isCorrect":false},{"id":"C","text":"$$24\\text{ days}$$","isCorrect":false},{"id":"D","text":"$$15\\text{ days}$$","isCorrect":false}]',
        'A',
        'A daily work $$= \\frac{1}{12} - \\frac{1}{30} = \\frac{5 - 2}{60} = \\frac{3}{60} = \\frac{1}{20}$$. Hence A takes $$20\\text{ days}$$.'
    ),
    -- 9. Fundamental Arithmetical Operations - Time and Distance (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000009'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'A boat travels $$24\\text{ km}$$ upstream in $$6\\text{ hours}$$ and $$36\\text{ km}$$ downstream in $$4\\text{ hours}$$. What is the speed of the water current?',
        '[{"id":"A","text":"$$2.5\\text{ km/h}$$","isCorrect":true},{"id":"B","text":"$$2.0\\text{ km/h}$$","isCorrect":false},{"id":"C","text":"$$3.0\\text{ km/h}$$","isCorrect":false},{"id":"D","text":"$$1.5\\text{ km/h}$$","isCorrect":false}]',
        'A',
        'Downstream speed $$v_d = \\frac{36}{4} = 9\\text{ km/h}$$. Upstream speed $$v_u = \\frac{24}{6} = 4\\text{ km/h}$$. Stream speed $$= \\frac{9 - 4}{2} = 2.5\\text{ km/h}$$.'
    ),
    -- 10. Fundamental Arithmetical Operations - Averages (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000010'::uuid,
        'Fundamental Arithmetical Operations',
        'Averages',
        'MEDIUM',
        'APPLY',
        'The average score of $$30$$ students in a class test is $$52$$. If the highest and lowest scores (which differ by $$56$$) are excluded, the average of the remaining $$28$$ students drops to $$51$$. What is the highest score?',
        '[{"id":"A","text":"$$94$$","isCorrect":true},{"id":"B","text":"$$88$$","isCorrect":false},{"id":"C","text":"$$92$$","isCorrect":false},{"id":"D","text":"$$96$$","isCorrect":false}]',
        'A',
        'Sum of 30 $$= 30 \\times 52 = 1560$$. Sum of 28 $$= 28 \\times 51 = 1428$$. High + Low $$= 1560 - 1428 = 132$$. Given High - Low $$= 56$$. Adding: $$2 \\times \\text{High} = 188 \\implies \\text{High} = 94$$.'
    ),
    -- 11. Algebra (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000011'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x + \\frac{1}{x} = 5$$, find the value of $$x^4 + \\frac{1}{x^4}$$.',
        '[{"id":"A","text":"$$527$$","isCorrect":true},{"id":"B","text":"$$529$$","isCorrect":false},{"id":"C","text":"$$525$$","isCorrect":false},{"id":"D","text":"$$574$$","isCorrect":false}]',
        'A',
        '$$x^2 + \\frac{1}{x^2} = 5^2 - 2 = 23$$. Then $$x^4 + \\frac{1}{x^4} = 23^2 - 2 = 529 - 2 = 527$$.'
    ),
    -- 12. Algebra (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000012'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'Find the value of $$\\frac{(0.87)^3 + (0.13)^3}{(0.87)^2 - 0.87 \\times 0.13 + (0.13)^2}$$.',
        '[{"id":"A","text":"$$1$$","isCorrect":true},{"id":"B","text":"$$0.74$$","isCorrect":false},{"id":"C","text":"$$0.91$$","isCorrect":false},{"id":"D","text":"$$0.5$$","isCorrect":false}]',
        'A',
        'Identity: $$\\frac{a^3 + b^3}{a^2 - ab + b^2} = a + b$$. Here $$a + b = 0.87 + 0.13 = 1$$.'
    ),
    -- 13. Algebra (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000013'::uuid,
        'Algebra',
        'Elementary Surds',
        'MEDIUM',
        'APPLY',
        'Simplify the surd expression: $$\\sqrt{7 + 2\\sqrt{10}}$$.',
        '[{"id":"A","text":"$$\\sqrt{5} + \\sqrt{2}$$","isCorrect":true},{"id":"B","text":"$$\\sqrt{6} + 1$$","isCorrect":false},{"id":"C","text":"$$\\sqrt{7} + \\sqrt{3}$$","isCorrect":false},{"id":"D","text":"$$2 + \\sqrt{3}$$","isCorrect":false}]',
        'A',
        '$$(\\sqrt{5} + \\sqrt{2})^2 = 5 + 2 + 2\\sqrt{10} = 7 + 2\\sqrt{10}$$. Taking square root gives $$\\sqrt{5} + \\sqrt{2}$$.'
    ),
    -- 14. Geometry - Cyclic Quadrilateral (HARD, ANALYZE)
    (
        'a10b0000-0000-0000-0000-000000000014'::uuid,
        'Geometry',
        'Circles',
        'HARD',
        'ANALYZE',
        'In a cyclic quadrilateral $$ABCD$$, if $$\\angle A = (2x + 4)^\\circ$$ and $$\\angle C = (3x + 16)^\\circ$$, what is the measure of $$\\angle C$$?',
        '[{"id":"A","text":"$$112^\\circ$$","isCorrect":true},{"id":"B","text":"$$68^\\circ$$","isCorrect":false},{"id":"C","text":"$$108^\\circ$$","isCorrect":false},{"id":"D","text":"$$116^\\circ$$","isCorrect":false}]',
        'A',
        'Opposite angles sum to $$180^\\circ$$: $$(2x + 4) + (3x + 16) = 180 \\implies 5x + 20 = 180 \\implies 5x = 160 \\implies x = 32$$. Then $$\\angle C = 3(32) + 16 = 96 + 16 = 112^\\circ$$.'
    ),
    -- 15. Geometry - Chords (HARD, ANALYZE)
    (
        'a10b0000-0000-0000-0000-000000000015'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'Two chords $$AB$$ and $$CD$$ of a circle intersect internally at point $$P$$. If $$AP = 6\\text{ cm}$$, $$PB = 8\\text{ cm}$$, and $$CP = 4\\text{ cm}$$, what is the length of chord $$CD$$?',
        '[{"id":"A","text":"$$16\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$12\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$14\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$18\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Intersecting Chords Theorem: $$AP \\times PB = CP \\times PD \\implies 6 \\times 8 = 4 \\times PD \\implies PD = 12\\text{ cm}$$. Total length $$CD = CP + PD = 4 + 12 = 16\\text{ cm}$$.'
    ),
    -- 16. Geometry - Triangles / Angle Bisector (HARD, ANALYZE)
    (
        'a10b0000-0000-0000-0000-000000000016'::uuid,
        'Geometry',
        'Triangles',
        'HARD',
        'ANALYZE',
        'In $$\\Delta ABC$$, the internal bisector of $$\\angle A$$ meets $$BC$$ at $$D$$. If $$AB = 10\\text{ cm}$$, $$AC = 14\\text{ cm}$$, and $$BC = 12\\text{ cm}$$, find the length of segment $$BD$$.',
        '[{"id":"A","text":"$$5\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$7\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$6\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$4.5\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Angle Bisector Theorem: $$\\frac{BD}{DC} = \\frac{AB}{AC} = \\frac{10}{14} = \\frac{5}{7}$$. Hence $$BD = \\frac{5}{12} \\times 12 = 5\\text{ cm}$$.'
    ),
    -- 17. Geometry - Direct Common Tangent (HARD, ANALYZE)
    (
        'a10b0000-0000-0000-0000-000000000017'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'Two circles have radii $$9\\text{ cm}$$ and $$4\\text{ cm}$$, with their centers separated by $$13\\text{ cm}$$. What is the length of their direct common tangent?',
        '[{"id":"A","text":"$$12\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$10\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$14\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$\\sqrt{153}\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Direct Common Tangent $$D = \\sqrt{d^2 - (R - r)^2} = \\sqrt{13^2 - (9 - 4)^2} = \\sqrt{169 - 25} = \\sqrt{144} = 12\\text{ cm}$$.'
    ),
    -- 18. Mensuration - Cylinder (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000018'::uuid,
        'Mensuration',
        'Right Circular Cylinder',
        'MEDIUM',
        'APPLY',
        'A hollow cylindrical iron pipe is $$21\\text{ cm}$$ long with an external radius of $$8\\text{ cm}$$ and an internal radius of $$6\\text{ cm}$$. Find the volume of iron in the pipe. (Take $$\\pi = \\frac{22}{7}$$)',
        '[{"id":"A","text":"$$1,848\\text{ cm}^3$$","isCorrect":true},{"id":"B","text":"$$1,540\\text{ cm}^3$$","isCorrect":false},{"id":"C","text":"$$1,980\\text{ cm}^3$$","isCorrect":false},{"id":"D","text":"$$2,156\\text{ cm}^3$$","isCorrect":false}]',
        'A',
        'Volume $$= \\pi h (R^2 - r^2) = \\frac{22}{7} \\times 21 \\times (64 - 36) = 66 \\times 28 = 1,848\\text{ cm}^3$$.'
    ),
    -- 19. Mensuration - Hemisphere (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000019'::uuid,
        'Mensuration',
        'Hemisphere',
        'MEDIUM',
        'APPLY',
        'What is the total surface area of a solid hemisphere of radius $$7\\text{ cm}$$? (Take $$\\pi = \\frac{22}{7}$$)',
        '[{"id":"A","text":"$$462\\text{ cm}^2$$","isCorrect":true},{"id":"B","text":"$$308\\text{ cm}^2$$","isCorrect":false},{"id":"C","text":"$$616\\text{ cm}^2$$","isCorrect":false},{"id":"D","text":"$$154\\text{ cm}^2$$","isCorrect":false}]',
        'A',
        'Total surface area of solid hemisphere $$= 3\\pi r^2 = 3 \\times \\frac{22}{7} \\times 49 = 3 \\times 154 = 462\\text{ cm}^2$$.'
    ),
    -- 20. Mensuration - Rhombus (MEDIUM, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000020'::uuid,
        'Mensuration',
        'Quadrilaterals',
        'MEDIUM',
        'APPLY',
        'The diagonals of a rhombus are of lengths $$16\\text{ cm}$$ and $$12\\text{ cm}$$. Find the perimeter of the rhombus.',
        '[{"id":"A","text":"$$40\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$48\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$32\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$56\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Side of rhombus $$a = \\sqrt{(d_1/2)^2 + (d_2/2)^2} = \\sqrt{8^2 + 6^2} = \\sqrt{64+36} = 10\\text{ cm}$$. Perimeter $$= 4 \\times 10 = 40\\text{ cm}$$.'
    ),
    -- 21. Trigonometry - Complementary Angles (HARD, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000021'::uuid,
        'Trigonometry',
        'Complementary Angles',
        'HARD',
        'APPLY',
        'Evaluate: $$\\tan 10^\\circ \\times \\tan 25^\\circ \\times \\tan 45^\\circ \\times \\tan 65^\\circ \\times \\tan 80^\\circ$$.',
        '[{"id":"A","text":"$$1$$","isCorrect":true},{"id":"B","text":"$$0$$","isCorrect":false},{"id":"C","text":"$$\\sqrt{3}$$","isCorrect":false},{"id":"D","text":"$$\\frac{1}{\\sqrt{3}}$$","isCorrect":false}]',
        'A',
        'Pairs: $$\\tan 10^\\circ \\tan 80^\\circ = 1$$, $$\\tan 25^\\circ \\tan 65^\\circ = 1$$, and $$\\tan 45^\\circ = 1$$. Total product $$= 1 \\times 1 \\times 1 = 1$$.'
    ),
    -- 22. Trigonometry - Identities (HARD, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000022'::uuid,
        'Trigonometry',
        'Standard Identities',
        'HARD',
        'APPLY',
        'If $$\\sec \\theta + \\tan \\theta = 3$$, find the value of $$\\sin \\theta$$.',
        '[{"id":"A","text":"$$\\frac{4}{5}$$","isCorrect":true},{"id":"B","text":"$$\\frac{3}{5}$$","isCorrect":false},{"id":"C","text":"$$\\frac{1}{3}$$","isCorrect":false},{"id":"D","text":"$$\\frac{2}{3}$$","isCorrect":false}]',
        'A',
        '$$\\sec \\theta - \\tan \\theta = \\frac{1}{3}$$. Adding gives $$2\\sec \\theta = \\frac{10}{3} \\implies \\sec \\theta = \\frac{5}{3} \\implies \\cos \\theta = \\frac{3}{5}$$. Then $$\\sin \\theta = \\sqrt{1 - (3/5)^2} = \\frac{4}{5}$$.'
    ),
    -- 23. Trigonometry - Heights & Distances (HARD, APPLY)
    (
        'a10b0000-0000-0000-0000-000000000023'::uuid,
        'Trigonometry',
        'Heights and Distances',
        'HARD',
        'APPLY',
        'The shadow of a vertical tower on level ground increases by $$20\\text{ m}$$ when the altitude of the sun decreases from $$60^\\circ$$ to $$30^\\circ$$. What is the height of the tower?',
        '[{"id":"A","text":"$$10\\sqrt{3}\\text{ m}$$","isCorrect":true},{"id":"B","text":"$$20\\sqrt{3}\\text{ m}$$","isCorrect":false},{"id":"C","text":"$$15\\sqrt{3}\\text{ m}$$","isCorrect":false},{"id":"D","text":"$$30\\text{ m}$$","isCorrect":false}]',
        'A',
        '$$h = \\frac{d}{\\cot 30^\\circ - \\cot 60^\\circ} = \\frac{20}{\\sqrt{3} - 1/\\sqrt{3}} = \\frac{20}{2/\\sqrt{3}} = 10\\sqrt{3}\\text{ m}$$.'
    ),
    -- 24. Statistics - Median (MEDIUM, UNDERSTAND)
    (
        'a10b0000-0000-0000-0000-000000000024'::uuid,
        'Statistics and Probability',
        'Median',
        'MEDIUM',
        'UNDERSTAND',
        'If the mode and mean of an empirical moderately skewed distribution are $$60$$ and $$66$$ respectively, find the median using Pearson empirical relationship.',
        '[{"id":"A","text":"$$64$$","isCorrect":true},{"id":"B","text":"$$63$$","isCorrect":false},{"id":"C","text":"$$65$$","isCorrect":false},{"id":"D","text":"$$62$$","isCorrect":false}]',
        'A',
        'Empirical formula: $$\\text{Mode} = 3\\text{Median} - 2\\text{Mean} \\implies 60 = 3\\text{Median} - 2(66) \\implies 3\\text{Median} = 60 + 132 = 192 \\implies \\text{Median} = 64$$.'
    ),
    -- 25. Probability - Coin Tossing (MEDIUM, UNDERSTAND)
    (
        'a10b0000-0000-0000-0000-000000000025'::uuid,
        'Statistics and Probability',
        'Simple Probability',
        'MEDIUM',
        'UNDERSTAND',
        'Three unbiased fair coins are tossed simultaneously. What is the probability of obtaining at least two heads?',
        '[{"id":"A","text":"$$\\frac{1}{2}$$","isCorrect":true},{"id":"B","text":"$$\\frac{3}{8}$$","isCorrect":false},{"id":"C","text":"$$\\frac{5}{8}$$","isCorrect":false},{"id":"D","text":"$$\\frac{1}{4}$$","isCorrect":false}]',
        'A',
        'Total outcomes $$= 2^3 = 8$$. Outcomes with at least 2 heads: HHT, HTH, THH, HHH (4 outcomes). Probability $$= \\frac{4}{8} = \\frac{1}{2}$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
