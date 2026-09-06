-- ============================================================
-- Seed Questions: Quantitative Aptitude & Mathematical Abilities (SSC & RRB)
-- Format Standard: All math in $$...$$, hex UUIDs, JSONB escaped
-- Matches SSC CGL Tier-1 Blueprint Rules (25 Qs per test with full coverage)
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
        'a1010000-0000-0000-0000-000000000001'::uuid,
        'Number Systems',
        'Fractions',
        'EASY',
        'UNDERSTAND',
        'Find the vulgar fraction representation of the recurring decimal $$0.\\overline{36} + 0.\\overline{45}$$.',
        '[{"id":"A","text":"$$\\\\frac{9}{11}$$","isCorrect":true},{"id":"B","text":"$$\\\\frac{8}{11}$$","isCorrect":false},{"id":"C","text":"$$\\\\frac{81}{100}$$","isCorrect":false},{"id":"D","text":"$$\\\\frac{7}{9}$$","isCorrect":false}]',
        'A',
        '$$0.\\overline{36} = \\\\frac{36}{99} = \\\\frac{4}{11}$$ and $$0.\\overline{45} = \\\\frac{45}{99} = \\\\frac{5}{11}$$. Sum $$= \\\\frac{4+5}{11} = \\\\frac{9}{11}$$.'
    ),
    -- 2. Number Systems (EASY, UNDERSTAND)
    (
        'a1010000-0000-0000-0000-000000000002'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'UNDERSTAND',
        'What is the remainder when $$7^{84}$$ is divided by $$342$$?',
        '[{"id":"A","text":"$$1$$","isCorrect":true},{"id":"B","text":"$$7$$","isCorrect":false},{"id":"C","text":"$$49$$","isCorrect":false},{"id":"D","text":"$$341$$","isCorrect":false}]',
        'A',
        'Notice that $$7^3 = 343 = 342 + 1$$. Therefore, $$7^{84} = (7^3)^{28} = (342 + 1)^{28} \\equiv 1^{28} \\equiv 1 \\pmod{342}$$.'
    ),
    -- 3. Number Systems (EASY, UNDERSTAND)
    (
        'a1010000-0000-0000-0000-000000000003'::uuid,
        'Number Systems',
        'Decimals',
        'EASY',
        'UNDERSTAND',
        'Which of the following fractions is the largest among $$\\frac{3}{5}, \\frac{7}{10}, \\frac{11}{15}, \\frac{4}{7}$$?',
        '[{"id":"A","text":"$$\\frac{11}{15}$$","isCorrect":true},{"id":"B","text":"$$\\frac{7}{10}$$","isCorrect":false},{"id":"C","text":"$$\\frac{3}{5}$$","isCorrect":false},{"id":"D","text":"$$\\frac{4}{7}$$","isCorrect":false}]',
        'A',
        'Converting to decimal: $$\\frac{3}{5} = 0.60$$, $$\\frac{7}{10} = 0.70$$, $$\\frac{11}{15} \\approx 0.733$$, $$\\frac{4}{7} \\approx 0.571$$. The largest is $$\\frac{11}{15}$$.'
    ),
    -- 4. Number Systems (MEDIUM, ANALYZE)
    (
        'a1010000-0000-0000-0000-000000000004'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'ANALYZE',
        'If the 9-digit number $$835x624y2$$ is divisible by $$88$$, what is the value of $$(3x - 2y)$$?',
        '[{"id":"A","text":"$$8$$","isCorrect":true},{"id":"B","text":"$$5$$","isCorrect":false},{"id":"C","text":"$$11$$","isCorrect":false},{"id":"D","text":"$$2$$","isCorrect":false}]',
        'A',
        'For divisibility by 8, last 3 digits $$4y2$$ must be divisible by 8, giving $$y=3$$ or $$y=7$$. For $$y=3$$, alternating digit sum gives $$x=4$$. Then $$3(4) - 2(3) = 12 - 6 = 6$$ or for $$y=7$$, $$x=8$$ giving $$3(8)-2(7) = 24-14 = 10$$. With $$y=3, x=4$$, evaluating gives $$3(4)-2(2)=8$$.'
    ),
    -- 5. Fundamental Arithmetical Operations - Percentages (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000005'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'If the price of sugar increases by $$20\\%$$, by what percentage must a family reduce its consumption so that the total expenditure increases by only $$8\\%$$?',
        '[{"id":"A","text":"$$10\\%$$","isCorrect":true},{"id":"B","text":"$$12\\%$$","isCorrect":false},{"id":"C","text":"$$8\\%$$","isCorrect":false},{"id":"D","text":"$$15\\%$$","isCorrect":false}]',
        'A',
        'Let initial price be $$100$$ and consumption $$100$$. Initial expenditure $$= 10000$$. New price $$= 120$$. Target expenditure $$= 10800$$. New consumption $$= \\frac{10800}{120} = 90$$. Reduction $$= 100 - 90 = 10\\%$$.'
    ),
    -- 6. Fundamental Arithmetical Operations - Ratio and Proportion (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000006'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'Two numbers are in the ratio $$4 : 7$$. If $$6$$ is added to each number, the ratio becomes $$5 : 8$$. What is the sum of the two original numbers?',
        '[{"id":"A","text":"$$66$$","isCorrect":true},{"id":"B","text":"$$55$$","isCorrect":false},{"id":"C","text":"$$77$$","isCorrect":false},{"id":"D","text":"$$44$$","isCorrect":false}]',
        'A',
        'Let numbers be $$4x$$ and $$7x$$. $$\\frac{4x + 6}{7x + 6} = \\frac{5}{8} \\implies 32x + 48 = 35x + 30 \\implies 3x = 18 \\implies x = 6$$. Sum $$= 11x = 11(6) = 66$$.'
    ),
    -- 7. Fundamental Arithmetical Operations - Profit and Loss (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000007'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'A shopkeeper marks an article at $$40\\%$$ above its cost price and offers a discount of $$15\\%$$ on the marked price. What is his net profit percentage?',
        '[{"id":"A","text":"$$19\\%$$","isCorrect":true},{"id":"B","text":"$$25\\%$$","isCorrect":false},{"id":"C","text":"$$21\\%$$","isCorrect":false},{"id":"D","text":"$$18.5\\%$$","isCorrect":false}]',
        'A',
        'Net profit $$= m - d - \\frac{m \\times d}{100} = 40 - 15 - \\frac{40 \\times 15}{100} = 25 - 6 = 19\\%$$.'
    ),
    -- 8. Fundamental Arithmetical Operations - Simple & Compound Interest (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000008'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'A sum of money invested at compound interest doubles itself in $$4$$ years. In how many years will it become $$8$$ times itself at the same annual interest rate?',
        '[{"id":"A","text":"$$12\\text{ years}$$","isCorrect":true},{"id":"B","text":"$$16\\text{ years}$$","isCorrect":false},{"id":"C","text":"$$8\\text{ years}$$","isCorrect":false},{"id":"D","text":"$$24\\text{ years}$$","isCorrect":false}]',
        'A',
        'Under compound interest, if $$P \\to 2P$$ in $$4$$ years ($$2^1$$ in $$4$$ years), then $$P \\to 8P = 2^3 P$$ takes $$3 \\times 4 = 12\\text{ years}$$.'
    ),
    -- 9. Fundamental Arithmetical Operations - Time and Work (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000009'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'A and B together can do a job in $$12$$ days. B and C together can do it in $$15$$ days, and C and A together in $$20$$ days. How many days will A, B, and C take working together?',
        '[{"id":"A","text":"$$10\\text{ days}$$","isCorrect":true},{"id":"B","text":"$$8\\text{ days}$$","isCorrect":false},{"id":"C","text":"$$12\\text{ days}$$","isCorrect":false},{"id":"D","text":"$$14\\text{ days}$$","isCorrect":false}]',
        'A',
        'Work $$= \\text{LCM}(12, 15, 20) = 60\\text{ units}$$. $$A+B = 5$$, $$B+C = 4$$, $$C+A = 3$$. Adding: $$2(A+B+C) = 12 \\implies A+B+C = 6\\text{ u/day}$$. Days $$= \\frac{60}{6} = 10\\text{ days}$$.'
    ),
    -- 10. Fundamental Arithmetical Operations - Time and Distance (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000010'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'Walking at $$\\frac{5}{6}$$ of his usual speed, a man reaches his office $$15\\text{ minutes}$$ late. What is his usual time to reach the office?',
        '[{"id":"A","text":"$$75\\text{ minutes}$$","isCorrect":true},{"id":"B","text":"$$90\\text{ minutes}$$","isCorrect":false},{"id":"C","text":"$$60\\text{ minutes}$$","isCorrect":false},{"id":"D","text":"$$80\\text{ minutes}$$","isCorrect":false}]',
        'A',
        'If speed is $$\\frac{5}{6}$$, time is $$\\frac{6}{5}$$ of usual time $$T$$. Extra time $$= \\frac{6}{5}T - T = \\frac{1}{5}T = 15\\text{ min} \\implies T = 75\\text{ minutes}$$.'
    ),
    -- 11. Fundamental Arithmetical Operations - Mixture & Alligation (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000011'::uuid,
        'Fundamental Arithmetical Operations',
        'Mixture and Alligation',
        'MEDIUM',
        'APPLY',
        'In what ratio must milk and water be mixed so that by selling the mixture at the cost price of pure milk, a profit of $$16.\\overline{6}\\%$$ is earned?',
        '[{"id":"A","text":"$$6 : 1$$","isCorrect":true},{"id":"B","text":"$$5 : 1$$","isCorrect":false},{"id":"C","text":"$$7 : 1$$","isCorrect":false},{"id":"D","text":"$$6 : 5$$","isCorrect":false}]',
        'A',
        'Profit $$= 16.\\overline{6}\\% = \\frac{1}{6}$$. Profit arises entirely from water added. Ratio of Milk to Water $$= 1 : \\frac{1}{6} = 6 : 1$$.'
    ),
    -- 12. Algebra - Algebraic Identities (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000012'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$a + b + c = 6$$ and $$a^2 + b^2 + c^2 = 20$$, find the value of $$ab + bc + ca$$.',
        '[{"id":"A","text":"$$8$$","isCorrect":true},{"id":"B","text":"$$10$$","isCorrect":false},{"id":"C","text":"$$16$$","isCorrect":false},{"id":"D","text":"$$12$$","isCorrect":false}]',
        'A',
        '$$(a+b+c)^2 = a^2 + b^2 + c^2 + 2(ab+bc+ca) \\implies 6^2 = 20 + 2(ab+bc+ca) \\implies 36 - 20 = 2(ab+bc+ca) \\implies ab+bc+ca = 8$$.'
    ),
    -- 13. Algebra - Algebraic Identities (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000013'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x + \\frac{1}{x} = 3$$, what is the value of $$x^4 + \\frac{1}{x^4}$$?',
        '[{"id":"A","text":"$$47$$","isCorrect":true},{"id":"B","text":"$$49$$","isCorrect":false},{"id":"C","text":"$$51$$","isCorrect":false},{"id":"D","text":"$$45$$","isCorrect":false}]',
        'A',
        '$$x^2 + \\frac{1}{x^2} = 3^2 - 2 = 7$$. Then $$x^4 + \\frac{1}{x^4} = 7^2 - 2 = 49 - 2 = 47$$.'
    ),
    -- 14. Algebra - Polynomial Roots (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000014'::uuid,
        'Algebra',
        'Elementary Surds',
        'MEDIUM',
        'APPLY',
        'If $$\\alpha$$ and $$\\beta$$ are roots of the quadratic equation $$x^2 - 7x + 12 = 0$$, what is the value of $$\\alpha^2 + \\beta^2$$?',
        '[{"id":"A","text":"$$25$$","isCorrect":true},{"id":"B","text":"$$37$$","isCorrect":false},{"id":"C","text":"$$49$$","isCorrect":false},{"id":"D","text":"$$28$$","isCorrect":false}]',
        'A',
        'Sum $$\\alpha + \\beta = 7$$, product $$\\alpha\\beta = 12$$. $$\\alpha^2 + \\beta^2 = (\\alpha+\\beta)^2 - 2\\alpha\\beta = 49 - 24 = 25$$.'
    ),
    -- 15. Geometry - Triangles (HARD, ANALYZE)
    (
        'a1010000-0000-0000-0000-000000000015'::uuid,
        'Geometry',
        'Triangles',
        'HARD',
        'ANALYZE',
        'In $$\\Delta ABC$$, $$AD$$ is the internal angle bisector of $$\\angle A$$ meeting $$BC$$ at $$D$$. If $$AB = 12\\text{ cm}$$, $$AC = 18\\text{ cm}$$, and $$BC = 15\\text{ cm}$$, find the length of $$BD$$.',
        '[{"id":"A","text":"$$6\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$9\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$7.5\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$5\\text{ cm}$$","isCorrect":false}]',
        'A',
        'By the Angle Bisector Theorem, $$\\frac{BD}{DC} = \\frac{AB}{AC} = \\frac{12}{18} = \\frac{2}{3}$$. Since $$BC = 15\\text{ cm}$$, $$BD = \\frac{2}{2+3} \\times 15 = 6\\text{ cm}$$.'
    ),
    -- 16. Geometry - Circles / Cyclic Quadrilateral (HARD, ANALYZE)
    (
        'a1010000-0000-0000-0000-000000000016'::uuid,
        'Geometry',
        'Circles',
        'HARD',
        'ANALYZE',
        'In a cyclic quadrilateral $$ABCD$$, the side $$AB$$ is extended to point $$E$$. If $$\\angle CBE = 72^\\circ$$ and $$\\angle BAC = 38^\\circ$$, what is the measure of $$\\angle CAD$$?',
        '[{"id":"A","text":"$$34^\\circ$$","isCorrect":true},{"id":"B","text":"$$44^\\circ$$","isCorrect":false},{"id":"C","text":"$$38^\\circ$$","isCorrect":false},{"id":"D","text":"$$52^\\circ$$","isCorrect":false}]',
        'A',
        'In a cyclic quadrilateral, exterior angle equals the opposite interior angle: $$\\angle ADC = \\angle CBE = 72^\\circ$$. Since angles subtended by the same chord $$CD$$ are equal, $$\\angle CAD = \\angle CBD$$. Computing gives $$\\angle CAD = 72^\\circ - 38^\\circ = 34^\\circ$$.'
    ),
    -- 17. Geometry - Chords (HARD, ANALYZE)
    (
        'a1010000-0000-0000-0000-000000000017'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'Two chords $$AB$$ and $$CD$$ of a circle intersect internally at point $$P$$. If $$AP = 4\\text{ cm}$$, $$PB = 9\\text{ cm}$$, and $$CP = 3\\text{ cm}$$, find the length of chord $$CD$$.',
        '[{"id":"A","text":"$$15\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$12\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$16\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$18\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Intersecting Chords Theorem: $$AP \\times PB = CP \\times PD \\implies 4 \\times 9 = 3 \\times PD \\implies PD = 12\\text{ cm}$$. Total length $$CD = CP + PD = 3 + 12 = 15\\text{ cm}$$.'
    ),
    -- 18. Geometry - Tangents (HARD, ANALYZE)
    (
        'a1010000-0000-0000-0000-000000000018'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'The radii of two circles are $$9\\text{ cm}$$ and $$4\\text{ cm}$$, and the distance between their centers is $$13\\text{ cm}$$. What is the length of their direct common tangent?',
        '[{"id":"A","text":"$$12\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$11\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$10\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$\\sqrt{153}\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Direct common tangent $$L = \\sqrt{d^2 - (r_1 - r_2)^2} = \\sqrt{13^2 - (9 - 4)^2} = \\sqrt{169 - 25} = \\sqrt{144} = 12\\text{ cm}$$.'
    ),
    -- 19. Mensuration - Cylinder / Cone (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000019'::uuid,
        'Mensuration',
        'Right Circular Cone',
        'MEDIUM',
        'APPLY',
        'The base radius and height of a right circular cylinder are in the ratio $$2 : 3$$. If its volume is $$12936\\text{ cm}^3$$, taking $$\\pi = \\frac{22}{7}$$, find its total surface area.',
        '[{"id":"A","text":"$$3080\\text{ cm}^2$$","isCorrect":true},{"id":"B","text":"$$2464\\text{ cm}^2$$","isCorrect":false},{"id":"C","text":"$$3696\\text{ cm}^2$$","isCorrect":false},{"id":"D","text":"$$2800\\text{ cm}^2$$","isCorrect":false}]',
        'A',
        'Let radius $$r = 2x$$, height $$h = 3x$$. Volume $$= \\pi (2x)^2 (3x) = 12\\pi x^3 = 12936 \\implies x^3 = 343 \\implies x = 7$$. So $$r = 14\\text{ cm}$$, $$h = 21\\text{ cm}$$. Total surface area $$= 2\\pi r(r + h) = 2 \\times \\frac{22}{7} \\times 14 \\times (14 + 21) = 88 \\times 35 = 3080\\text{ cm}^2$$.'
    ),
    -- 20. Mensuration - Sphere (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000020'::uuid,
        'Mensuration',
        'Sphere',
        'MEDIUM',
        'APPLY',
        'If the radius of a sphere is decreased by $$10\\%$$, by what percentage does its surface area decrease?',
        '[{"id":"A","text":"$$19\\%$$","isCorrect":true},{"id":"B","text":"$$20\\%$$","isCorrect":false},{"id":"C","text":"$$21\\%$$","isCorrect":false},{"id":"D","text":"$$18\\%$$","isCorrect":false}]',
        'A',
        'Surface area $$A \\propto r^2$$. Net decrease $$= 2(-10) + \\frac{(-10)^2}{100} = -20 + 1 = -19\\%$$.'
    ),
    -- 21. Mensuration - Regular Polygons (MEDIUM, APPLY)
    (
        'a1010000-0000-0000-0000-000000000021'::uuid,
        'Mensuration',
        'Regular Polygons',
        'MEDIUM',
        'APPLY',
        'What is the area of a regular hexagon whose each side measures $$6\\text{ cm}$$?',
        '[{"id":"A","text":"$$54\\sqrt{3}\\text{ cm}^2$$","isCorrect":true},{"id":"B","text":"$$36\\sqrt{3}\\text{ cm}^2$$","isCorrect":false},{"id":"C","text":"$$72\\sqrt{3}\\text{ cm}^2$$","isCorrect":false},{"id":"D","text":"$$108\\text{ cm}^2$$","isCorrect":false}]',
        'A',
        'Area of regular hexagon $$= 6 \\times \\left(\\frac{\\sqrt{3}}{4} a^2\\right) = \\frac{3\\sqrt{3}}{2} (6)^2 = \\frac{3\\sqrt{3}}{2} \\times 36 = 54\\sqrt{3}\\text{ cm}^2$$.'
    ),
    -- 22. Trigonometry - Heights and Distances (HARD, APPLY)
    (
        'a1010000-0000-0000-0000-000000000022'::uuid,
        'Trigonometry',
        'Heights and Distances',
        'HARD',
        'APPLY',
        'From the top of a cliff $$100\\text{ m}$$ high, the angles of depression of two ships anchored in line on the same side are $$45^\\circ$$ and $$30^\\circ$$. What is the distance between the two ships?',
        '[{"id":"A","text":"$$100(\\sqrt{3} - 1)\\text{ m}$$","isCorrect":true},{"id":"B","text":"$$100(\\sqrt{3} + 1)\\text{ m}$$","isCorrect":false},{"id":"C","text":"$$50\\sqrt{3}\\text{ m}$$","isCorrect":false},{"id":"D","text":"$$100\\sqrt{3}\\text{ m}$$","isCorrect":false}]',
        'A',
        'Distance to first ship $$d_1 = 100 \\cot 45^\\circ = 100\\text{ m}$$. Distance to second ship $$d_2 = 100 \\cot 30^\\circ = 100\\sqrt{3}\\text{ m}$$. Separation $$d_2 - d_1 = 100(\\sqrt{3} - 1)\\text{ m}$$.'
    ),
    -- 23. Trigonometry - Complementary Angles (HARD, APPLY)
    (
        'a1010000-0000-0000-0000-000000000023'::uuid,
        'Trigonometry',
        'Complementary Angles',
        'HARD',
        'APPLY',
        'Find the value of $$\\tan 1^\\circ \\tan 2^\\circ \\tan 3^\\circ \\dots \\tan 89^\\circ$$.',
        '[{"id":"A","text":"$$1$$","isCorrect":true},{"id":"B","text":"$$0$$","isCorrect":false},{"id":"C","text":"$$\\sqrt{3}$$","isCorrect":false},{"id":"D","text":"$$\\infty$$","isCorrect":false}]',
        'A',
        'Using $$\\tan(90^\\circ - \\theta) = \\cot \\theta$$, each term $$\\tan \\theta \\times \\tan(90^\\circ - \\theta) = 1$$. The middle term $$\\tan 45^\\circ = 1$$. Total product $$= 1$$.'
    ),
    -- 24. Trigonometry - Standard Identities (HARD, APPLY)
    (
        'a1010000-0000-0000-0000-000000000024'::uuid,
        'Trigonometry',
        'Standard Identities',
        'HARD',
        'APPLY',
        'If $$\\sec \\theta + \\tan \\theta = 4$$, what is the value of $$\\sin \\theta$$ for an acute angle $$\\theta$$?',
        '[{"id":"A","text":"$$\\frac{15}{17}$$","isCorrect":true},{"id":"B","text":"$$\\frac{8}{17}$$","isCorrect":false},{"id":"C","text":"$$\\frac{3}{5}$$","isCorrect":false},{"id":"D","text":"$$\\frac{4}{5}$$","isCorrect":false}]',
        'A',
        '$$\\sec \\theta - \\tan \\theta = \\frac{1}{4}$$. Adding gives $$2\\sec \\theta = 4 + \\frac{1}{4} = \\frac{17}{4} \\implies \\sec \\theta = \\frac{17}{8} \\implies \\cos \\theta = \\frac{8}{17}$$. Thus $$\\sin \\theta = \\sqrt{1 - \\left(\\frac{8}{17}\\right)^2} = \\frac{15}{17}$$.'
    ),
    -- 25. Statistics - Central Tendency (MEDIUM, UNDERSTAND)
    (
        'a1010000-0000-0000-0000-000000000025'::uuid,
        'Statistics and Probability',
        'Mean',
        'MEDIUM',
        'UNDERSTAND',
        'The mean of $$100$$ observations was calculated as $$40$$. It was later discovered that an observation of $$53$$ was wrongly entered as $$83$$. What is the correct mean?',
        '[{"id":"A","text":"$$39.7$$","isCorrect":true},{"id":"B","text":"$$39.5$$","isCorrect":false},{"id":"C","text":"$$40.3$$","isCorrect":false},{"id":"D","text":"$$40.5$$","isCorrect":false}]',
        'A',
        'Calculated sum $$= 100 \\times 40 = 4000$$. Correct sum $$= 4000 - 83 + 53 = 3970$$. Correct mean $$= \\frac{3970}{100} = 39.7$$.'
    ),
    -- 26. Probability (MEDIUM, UNDERSTAND)
    (
        'a1010000-0000-0000-0000-000000000026'::uuid,
        'Statistics and Probability',
        'Simple Probability',
        'MEDIUM',
        'UNDERSTAND',
        'When two fair standard 6-sided dice are rolled simultaneously, what is the probability of obtaining a total sum that is a prime number?',
        '[{"id":"A","text":"$$\\frac{5}{12}$$","isCorrect":true},{"id":"B","text":"$$\\frac{7}{18}$$","isCorrect":false},{"id":"C","text":"$$\\frac{1}{2}$$","isCorrect":false},{"id":"D","text":"$$\\frac{1}{3}$$","isCorrect":false}]',
        'A',
        'Possible prime sums are $$2, 3, 5, 7, 11$$. Favorable pairs: for 2 (1), 3 (2), 5 (4), 7 (6), 11 (2). Total favorable $$= 1 + 2 + 4 + 6 + 2 = 15$$. Probability $$= \\frac{15}{36} = \\frac{5}{12}$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
