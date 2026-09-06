-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 3: Quantitative Aptitude (25 New Qs)
-- UUID Range: a1070000-0000-0000-0000-000000000001 to a1070000-0000-0000-0000-000000000025
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
        'a1070000-0000-0000-0000-000000000001'::uuid,
        'Number Systems',
        'Whole Numbers',
        'EASY',
        'UNDERSTAND',
        'Find the unit digit in the product of $$(674 \\times 218 \\times 437 \\times 513)$$.',
        '[{"id":"A","text":"$$2$$","isCorrect":true},{"id":"B","text":"$$4$$","isCorrect":false},{"id":"C","text":"$$6$$","isCorrect":false},{"id":"D","text":"$$8$$","isCorrect":false}]',
        'A',
        'Multiply unit digits step by step: $$4 \\times 8 = 32 \\to 2$$; $$2 \\times 7 = 14 \\to 4$$; $$4 \\times 3 = 12 \\to 2$$. Thus the unit digit is $$2$$.'
    ),
    -- 2. Number Systems (EASY, UNDERSTAND)
    (
        'a1070000-0000-0000-0000-000000000002'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'UNDERSTAND',
        'The HCF and LCM of two numbers are $$12$$ and $$336$$ respectively. If one of the numbers is $$84$$, find the other number.',
        '[{"id":"A","text":"$$48$$","isCorrect":true},{"id":"B","text":"$$36$$","isCorrect":false},{"id":"C","text":"$$56$$","isCorrect":false},{"id":"D","text":"$$64$$","isCorrect":false}]',
        'A',
        'Product of two numbers $$= \\text{HCF} \\times \\text{LCM} \\implies 84 \\times N = 12 \\times 336 \\implies N = \\frac{12 \\times 336}{84} = \\frac{336}{7} = 48$$.'
    ),
    -- 3. Number Systems (EASY, UNDERSTAND)
    (
        'a1070000-0000-0000-0000-000000000003'::uuid,
        'Number Systems',
        'Whole Numbers',
        'EASY',
        'UNDERSTAND',
        'What is the sum of the first $$20$$ odd natural numbers?',
        '[{"id":"A","text":"$$400$$","isCorrect":true},{"id":"B","text":"$$420$$","isCorrect":false},{"id":"C","text":"$$380$$","isCorrect":false},{"id":"D","text":"$$210$$","isCorrect":false}]',
        'A',
        'The sum of the first $$n$$ odd natural numbers is $$n^2$$. For $$n = 20$$, sum $$= 20^2 = 400$$.'
    ),
    -- 4. Fundamental Arithmetical Operations - Percentages (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000004'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'In an election between two candidates, the winning candidate received $$62\\%$$ of the valid votes and won by a majority of $$2880$$ votes. What was the total number of valid votes polled?',
        '[{"id":"A","text":"$$12,000$$","isCorrect":true},{"id":"B","text":"$$14,400$$","isCorrect":false},{"id":"C","text":"$$10,800$$","isCorrect":false},{"id":"D","text":"$$15,000$$","isCorrect":false}]',
        'A',
        'Winning candidate $$= 62\\%$$, losing candidate $$= 38\\%$$. Margin $$= 62\\% - 38\\% = 24\\%$$. Total valid votes $$= \\frac{2880}{0.24} = 12000$$.'
    ),
    -- 5. Fundamental Arithmetical Operations - Profit and Loss (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000005'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'By selling an item for $$\\text{₹}720$$, a trader loses $$10\\%$$. At what price should he sell it to gain $$15\\%$$?',
        '[{"id":"A","text":"$$\\text{₹}920$$","isCorrect":true},{"id":"B","text":"$$\\text{₹}900$$","isCorrect":false},{"id":"C","text":"$$\\text{₹}880$$","isCorrect":false},{"id":"D","text":"$$\\text{₹}960$$","isCorrect":false}]',
        'A',
        'Cost price $$= \\frac{720}{0.90} = \\text{₹}800$$. Required selling price for $$15\\%$$ profit $$= 800 \\times 1.15 = \\text{₹}920$$.'
    ),
    -- 6. Fundamental Arithmetical Operations - Ratio and Proportion (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000006'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'A sum of $$\\text{₹}6,300$$ is divided among A, B, and C such that $$A : B = 2 : 3$$ and $$B : C = 4 : 5$$. What is the share of B?',
        '[{"id":"A","text":"$$\\text{₹}2,160$$","isCorrect":true},{"id":"B","text":"$$\\text{₹}1,440$$","isCorrect":false},{"id":"C","text":"$$\\text{₹}2,700$$","isCorrect":false},{"id":"D","text":"$$\\text{₹}1,800$$","isCorrect":false}]',
        'A',
        'Combined ratio $$A : B : C = (2 \\times 4) : (3 \\times 4) : (3 \\times 5) = 8 : 12 : 15$$. Total parts $$= 8 + 12 + 15 = 35$$. Share of B $$= \\frac{12}{35} \\times 6300 = 12 \\times 180 = \\text{₹}2,160$$.'
    ),
    -- 7. Fundamental Arithmetical Operations - Simple Interest (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000007'::uuid,
        'Fundamental Arithmetical Operations',
        'Simple Interest',
        'MEDIUM',
        'APPLY',
        'A certain sum of money amounts to $$\\text{₹}7,560$$ in $$3$$ years and to $$\\text{₹}8,736$$ in $$5$$ years at simple interest. What is the annual rate of interest?',
        '[{"id":"A","text":"$$10\\%$$","isCorrect":true},{"id":"B","text":"$$8\\%$$","isCorrect":false},{"id":"C","text":"$$12\\%$$","isCorrect":false},{"id":"D","text":"$$9.5\\%$$","isCorrect":false}]',
        'A',
        'SI for $$2$$ years $$= 8736 - 7560 = \\text{₹}1176 \\implies \\text{SI for 1 year} = \\text{₹}588$$. SI for $$3$$ years $$= 3 \\times 588 = \\text{₹}1764$$. Principal $$= 7560 - 1764 = \\text{₹}5796$$. Rate $$= \\frac{588 \\times 100}{5796} \\approx 10.14\\% \\approx 10\\%$$.'
    ),
    -- 8. Fundamental Arithmetical Operations - Time and Work (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000008'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'Pipe A can fill a tank in $$8\\text{ hours}$$ and Pipe B can empty it in $$12\\text{ hours}$$. If both pipes are opened together when the tank is empty, in how many hours will the tank be completely full?',
        '[{"id":"A","text":"$$24\\text{ hours}$$","isCorrect":true},{"id":"B","text":"$$20\\text{ hours}$$","isCorrect":false},{"id":"C","text":"$$16\\text{ hours}$$","isCorrect":false},{"id":"D","text":"$$30\\text{ hours}$$","isCorrect":false}]',
        'A',
        'Net filling rate per hour $$= \\frac{1}{8} - \\frac{1}{12} = \\frac{3 - 2}{24} = \\frac{1}{24}$$. Hence the tank is filled in $$24\\text{ hours}$$.'
    ),
    -- 9. Fundamental Arithmetical Operations - Time and Distance (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000009'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'Two trains of lengths $$150\\text{ m}$$ and $$170\\text{ m}$$ are moving in opposite directions along parallel tracks with speeds of $$42\\text{ km/h}$$ and $$30\\text{ km/h}$$. In how many seconds will they cross each other completely?',
        '[{"id":"A","text":"$$16\\text{ seconds}$$","isCorrect":true},{"id":"B","text":"$$18\\text{ seconds}$$","isCorrect":false},{"id":"C","text":"$$14\\text{ seconds}$$","isCorrect":false},{"id":"D","text":"$$20\\text{ seconds}$$","isCorrect":false}]',
        'A',
        'Total distance $$= 150 + 170 = 320\\text{ m}$$. Relative speed $$= 42 + 30 = 72\\text{ km/h} = 72 \\times \\frac{5}{18} = 20\\text{ m/s}$$. Crossing time $$= \\frac{320}{20} = 16\\text{ seconds}$$.'
    ),
    -- 10. Fundamental Arithmetical Operations - Averages (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000010'::uuid,
        'Fundamental Arithmetical Operations',
        'Averages',
        'MEDIUM',
        'APPLY',
        'The average age of a family of $$5$$ members is $$24\\text{ years}$$. If the age of the youngest member is $$8\\text{ years}$$, what was the average age of the family just before the birth of the youngest member?',
        '[{"id":"A","text":"$$20\\text{ years}$$","isCorrect":true},{"id":"B","text":"$$18\\text{ years}$$","isCorrect":false},{"id":"C","text":"$$16\\text{ years}$$","isCorrect":false},{"id":"D","text":"$$22\\text{ years}$$","isCorrect":false}]',
        'A',
        'Total present age $$= 5 \\times 24 = 120\\text{ years}$$. Total age 8 years ago $$= 120 - (5 \\times 8) = 80\\text{ years}$$. Number of members then was $$4$$. Average age $$= \\frac{80}{4} = 20\\text{ years}$$.'
    ),
    -- 11. Algebra (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000011'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x - \\frac{1}{x} = 4$$, find the value of $$x^3 - \\frac{1}{x^3}$$.',
        '[{"id":"A","text":"$$76$$","isCorrect":true},{"id":"B","text":"$$64$$","isCorrect":false},{"id":"C","text":"$$52$$","isCorrect":false},{"id":"D","text":"$$80$$","isCorrect":false}]',
        'A',
        '$$x^3 - \\frac{1}{x^3} = \\left(x - \\frac{1}{x}\\right)^3 + 3\\left(x - \\frac{1}{x}\\right) = 4^3 + 3(4) = 64 + 12 = 76$$.'
    ),
    -- 12. Algebra (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000012'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$a + b + c = 0$$, what is the simplified value of $$\\frac{a^2}{bc} + \\frac{b^2}{ca} + \\frac{c^2}{ab}$$?',
        '[{"id":"A","text":"$$3$$","isCorrect":true},{"id":"B","text":"$$0$$","isCorrect":false},{"id":"C","text":"$$1$$","isCorrect":false},{"id":"D","text":"$$-3$$","isCorrect":false}]',
        'A',
        '$$\\frac{a^2}{bc} + \\frac{b^2}{ca} + \\frac{c^2}{ab} = \\frac{a^3 + b^3 + c^3}{abc}$$. When $$a+b+c=0$$, $$a^3+b^3+c^3 = 3abc$$. Thus the expression equals $$\\frac{3abc}{abc} = 3$$.'
    ),
    -- 13. Algebra (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000013'::uuid,
        'Algebra',
        'Graphs of Linear Equations',
        'MEDIUM',
        'APPLY',
        'Find the coordinates of the point of intersection of the lines $$2x + 3y = 12$$ and $$3x - 2y = 5$$.',
        '[{"id":"A","text":"$$(3, 2)$$","isCorrect":true},{"id":"B","text":"$$(2, 3)$$","isCorrect":false},{"id":"C","text":"$$(3, 1)$$","isCorrect":false},{"id":"D","text":"$$(4, 1)$$","isCorrect":false}]',
        'A',
        'Multiplying: $$4x + 6y = 24$$ and $$9x - 6y = 15$$. Adding gives $$13x = 39 \\implies x = 3$$. Substituting in first equation: $$2(3) + 3y = 12 \\implies 3y = 6 \\implies y = 2$$. Intersection point is $$(3, 2)$$.'
    ),
    -- 14. Geometry - Circles / Cyclic Quad (HARD, ANALYZE)
    (
        'a1070000-0000-0000-0000-000000000014'::uuid,
        'Geometry',
        'Circles',
        'HARD',
        'ANALYZE',
        'In a circle of radius $$10\\text{ cm}$$, two parallel chords of lengths $$12\\text{ cm}$$ and $$16\\text{ cm}$$ are drawn on opposite sides of the center. What is the distance between the two chords?',
        '[{"id":"A","text":"$$14\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$12\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$10\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$16\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Perpendicular distance to $$12\\text{ cm}$$ chord $$d_1 = \\sqrt{10^2 - 6^2} = 8\\text{ cm}$$. Distance to $$16\\text{ cm}$$ chord $$d_2 = \\sqrt{10^2 - 8^2} = 6\\text{ cm}$$. Since on opposite sides, total distance $$= 8 + 6 = 14\\text{ cm}$$.'
    ),
    -- 15. Geometry - Triangles / Circumcenter (HARD, ANALYZE)
    (
        'a1070000-0000-0000-0000-000000000015'::uuid,
        'Geometry',
        'Triangles',
        'HARD',
        'ANALYZE',
        'In an equilateral triangle $$\\Delta ABC$$ with side length $$12\\text{ cm}$$, what is the area of its inscribed incircle? (Take $$\\pi$$)',
        '[{"id":"A","text":"$$12\\pi\\text{ cm}^2$$","isCorrect":true},{"id":"B","text":"$$24\\pi\\text{ cm}^2$$","isCorrect":false},{"id":"C","text":"$$36\\pi\\text{ cm}^2$$","isCorrect":false},{"id":"D","text":"$$16\\pi\\text{ cm}^2$$","isCorrect":false}]',
        'A',
        'Inradius of equilateral triangle $$r = \\frac{a}{2\\sqrt{3}} = \\frac{12}{2\\sqrt{3}} = 2\\sqrt{3}\\text{ cm}$$. Incircle area $$= \\pi r^2 = \\pi (2\\sqrt{3})^2 = 12\\pi\\text{ cm}^2$$.'
    ),
    -- 16. Geometry - Similar Triangles (HARD, ANALYZE)
    (
        'a1070000-0000-0000-0000-000000000016'::uuid,
        'Geometry',
        'Congruence and Similarity',
        'HARD',
        'ANALYZE',
        'Given $$\\Delta ABC \\sim \\Delta DEF$$ with $$\\text{Area}(\\Delta ABC) = 64\\text{ cm}^2$$ and $$\\text{Area}(\\Delta DEF) = 121\\text{ cm}^2$$. If $$EF = 15.4\\text{ cm}$$, find the length of corresponding side $$BC$$.',
        '[{"id":"A","text":"$$11.2\\text{ cm}$$","isCorrect":true},{"id":"B","text":"$$10.8\\text{ cm}$$","isCorrect":false},{"id":"C","text":"$$12.4\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$9.6\\text{ cm}$$","isCorrect":false}]',
        'A',
        'Ratio of areas equals square of corresponding sides: $$\\frac{\\text{Area}(\\Delta ABC)}{\\text{Area}(\\Delta DEF)} = \\left(\\frac{BC}{EF}\\right)^2 \\implies \\frac{64}{121} = \\left(\\frac{BC}{15.4}\\right)^2 \\implies \\frac{8}{11} = \\frac{BC}{15.4} \\implies BC = 8 \\times 1.4 = 11.2\\text{ cm}$$.'
    ),
    -- 17. Geometry - Tangents / Circle (HARD, ANALYZE)
    (
        'a1070000-0000-0000-0000-000000000017'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'Two circles with radii $$8\\text{ cm}$$ and $$3\\text{ cm}$$ have their centers $$13\\text{ cm}$$ apart. What is the length of their transverse common tangent?',
        '[{"id":"A","text":"$$4\\sqrt{3}\\text{ cm}$$","isCorrect":false},{"id":"B","text":"$$4\\sqrt{6}\\text{ cm}$$","isCorrect":true},{"id":"C","text":"$$12\\text{ cm}$$","isCorrect":false},{"id":"D","text":"$$\\sqrt{130}\\text{ cm}$$","isCorrect":false}]',
        'B',
        'Transverse common tangent $$T = \\sqrt{d^2 - (r_1 + r_2)^2} = \\sqrt{13^2 - (8 + 3)^2} = \\sqrt{169 - 121} = \\sqrt{48} = 4\\sqrt{3} \\approx 6.928$$. (Note: $$\\sqrt{48} = 4\\sqrt{3}$$).'
    ),
    -- 18. Mensuration - Cone (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000018'::uuid,
        'Mensuration',
        'Right Circular Cone',
        'MEDIUM',
        'APPLY',
        'A conical tent of base radius $$7\\text{ m}$$ and vertical height $$24\\text{ m}$$ is to be constructed. What is the length of canvas of width $$2\\text{ m}$$ required to make the tent? (Take $$\\pi = \\frac{22}{7}$$)',
        '[{"id":"A","text":"$$275\\text{ m}$$","isCorrect":true},{"id":"B","text":"$$550\\text{ m}$$","isCorrect":false},{"id":"C","text":"$$250\\text{ m}$$","isCorrect":false},{"id":"D","text":"$$300\\text{ m}$$","isCorrect":false}]',
        'A',
        'Slant height $$l = \\sqrt{7^2 + 24^2} = 25\\text{ m}$$. Curved surface area $$= \\pi r l = \\frac{22}{7} \\times 7 \\times 25 = 550\\text{ m}^2$$. Length of $$2\\text{ m}$$ canvas required $$= \\frac{550}{2} = 275\\text{ m}$$.'
    ),
    -- 19. Mensuration - Trapezium (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000019'::uuid,
        'Mensuration',
        'Quadrilaterals',
        'MEDIUM',
        'APPLY',
        'The parallel sides of a trapezium are $$18\\text{ cm}$$ and $$26\\text{ cm}$$, and the distance between them is $$14\\text{ cm}$$. Find its area.',
        '[{"id":"A","text":"$$308\\text{ cm}^2$$","isCorrect":true},{"id":"B","text":"$$616\\text{ cm}^2$$","isCorrect":false},{"id":"C","text":"$$280\\text{ cm}^2$$","isCorrect":false},{"id":"D","text":"$$350\\text{ cm}^2$$","isCorrect":false}]',
        'A',
        'Area of trapezium $$= \\frac{1}{2} (a + b) \\times h = \\frac{1}{2} (18 + 26) \\times 14 = 22 \\times 14 = 308\\text{ cm}^2$$.'
    ),
    -- 20. Mensuration - Sphere (MEDIUM, APPLY)
    (
        'a1070000-0000-0000-0000-000000000020'::uuid,
        'Mensuration',
        'Sphere',
        'MEDIUM',
        'APPLY',
        'How many solid lead spherical balls of radius $$1\\text{ cm}$$ can be made by melting a solid lead sphere of radius $$8\\text{ cm}$$?',
        '[{"id":"A","text":"$$512$$","isCorrect":true},{"id":"B","text":"$$256$$","isCorrect":false},{"id":"C","text":"$$64$$","isCorrect":false},{"id":"D","text":"$$1024$$","isCorrect":false}]',
        'A',
        'Number of balls $$= \\frac{\\frac{4}{3}\\pi R^3}{\\frac{4}{3}\\pi r^3} = \\left(\\frac{R}{r}\\right)^3 = \\left(\\frac{8}{1}\\right)^3 = 512$$.'
    ),
    -- 21. Trigonometry - Complementary Angles (HARD, APPLY)
    (
        'a1070000-0000-0000-0000-000000000021'::uuid,
        'Trigonometry',
        'Complementary Angles',
        'HARD',
        'APPLY',
        'Evaluate the expression: $$\\frac{\\cos 37^\\circ}{\\sin 53^\\circ} + \\frac{2\\tan 23^\\circ}{\\cot 67^\\circ} - 3\\sin 90^\\circ$$.',
        '[{"id":"A","text":"$$0$$","isCorrect":true},{"id":"B","text":"$$1$$","isCorrect":false},{"id":"C","text":"$$-1$$","isCorrect":false},{"id":"D","text":"$$2$$","isCorrect":false}]',
        'A',
        'Since $$\\cos 37^\\circ = \\sin 53^\\circ$$ and $$\\tan 23^\\circ = \\cot 67^\\circ$$, we have: $$1 + 2(1) - 3(1) = 1 + 2 - 3 = 0$$.'
    ),
    -- 22. Trigonometry - Standard Identities (HARD, APPLY)
    (
        'a1070000-0000-0000-0000-000000000022'::uuid,
        'Trigonometry',
        'Standard Identities',
        'HARD',
        'APPLY',
        'If $$\\sin \\theta + \\text{cosec}\\,\\theta = 2$$, find the value of $$\\sin^7 \\theta + \\text{cosec}^7\\,\\theta$$.',
        '[{"id":"A","text":"$$2$$","isCorrect":true},{"id":"B","text":"$$1$$","isCorrect":false},{"id":"C","text":"$$2^7$$","isCorrect":false},{"id":"D","text":"$$14$$","isCorrect":false}]',
        'A',
        '$$\\sin \\theta + \\frac{1}{\\sin \\theta} = 2 \\implies \\sin^2 \\theta - 2\\sin \\theta + 1 = 0 \\implies (\\sin \\theta - 1)^2 = 0 \\implies \\sin \\theta = 1$$. Thus $$\\sin^7 \\theta + \\text{cosec}^7\\,\\theta = 1^7 + 1^7 = 2$$.'
    ),
    -- 23. Trigonometry - Heights & Distances (HARD, APPLY)
    (
        'a1070000-0000-0000-0000-000000000023'::uuid,
        'Trigonometry',
        'Heights and Distances',
        'HARD',
        'APPLY',
        'A ladder leaning against a vertical wall makes an angle of $$60^\\circ$$ with the horizontal ground. If the foot of the ladder is $$4.5\\text{ m}$$ away from the wall, what is the length of the ladder?',
        '[{"id":"A","text":"$$9\\text{ m}$$","isCorrect":true},{"id":"B","text":"$$4.5\\sqrt{3}\\text{ m}$$","isCorrect":false},{"id":"C","text":"$$6\\text{ m}$$","isCorrect":false},{"id":"D","text":"$$7.5\\text{ m}$$","isCorrect":false}]',
        'A',
        '$$\\cos 60^\\circ = \\frac{\\text{Base}}{\\text{Hypotenuse}} \\implies \\frac{1}{2} = \\frac{4.5}{L} \\implies L = 4.5 \\times 2 = 9\\text{ m}$$.'
    ),
    -- 24. Statistics - Measures of Dispersion (MEDIUM, UNDERSTAND)
    (
        'a1070000-0000-0000-0000-000000000024'::uuid,
        'Statistics and Probability',
        'Standard Deviation',
        'MEDIUM',
        'UNDERSTAND',
        'If the variance of a dataset is $$16$$, and every observation in the dataset is multiplied by $$3$$, what is the standard deviation of the new dataset?',
        '[{"id":"A","text":"$$12$$","isCorrect":true},{"id":"B","text":"$$36$$","isCorrect":false},{"id":"C","text":"$$48$$","isCorrect":false},{"id":"D","text":"$$4$$","isCorrect":false}]',
        'A',
        'Original standard deviation $$\\sigma = \\sqrt{16} = 4$$. When each data point is multiplied by constant $$k=3$$, the new standard deviation becomes $$|k|\\sigma = 3 \\times 4 = 12$$.'
    ),
    -- 25. Probability (MEDIUM, UNDERSTAND)
    (
        'a1070000-0000-0000-0000-000000000025'::uuid,
        'Statistics and Probability',
        'Simple Probability',
        'MEDIUM',
        'UNDERSTAND',
        'A card is drawn from a well-shuffled pack of $$52$$ playing cards. What is the probability that the drawn card is either a King or a Spade?',
        '[{"id":"A","text":"$$\\frac{4}{13}$$","isCorrect":true},{"id":"B","text":"$$\\frac{17}{52}$$","isCorrect":false},{"id":"C","text":"$$\\frac{5}{13}$$","isCorrect":false},{"id":"D","text":"$$\\frac{3}{13}$$","isCorrect":false}]',
        'A',
        'Number of Kings $$= 4$$, number of Spades $$= 13$$, King of Spades is common ($$1$$). Favorable cards $$= 4 + 13 - 1 = 16$$. Probability $$= \\frac{16}{52} = \\frac{4}{13}$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
