-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 1: General Intelligence & Reasoning (25 New Qs)
-- UUID Range: a1050000-0000-0000-0000-000000000001 to a1050000-0000-0000-0000-000000000025
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
    'General Intelligence and Reasoning',
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
    -- 1. Analogy (EASY, UNDERSTAND)
    (
        'a1050000-0000-0000-0000-000000000001'::uuid,
        'Analogy',
        'Symbolic / Number Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the option that is related to the third number in the same way as the second number is related to the first number: $$12 : 172 :: 8 : ?$$',
        '[{"id":"A","text":"$$68$$","isCorrect":true},{"id":"B","text":"$$64$$","isCorrect":false},{"id":"C","text":"$$72$$","isCorrect":false},{"id":"D","text":"$$60$$","isCorrect":false}]',
        'A',
        'Pattern: $$n : (n^2 + 28)$$. For $$12$$, $$12^2 + 28 = 144 + 28 = 172$$. For $$8$$, $$8^2 + 28 = 64 + 28 = 68$$ (or $$n \\times 14 + 4$$: $$12 \\times 14 + 4 = 172$$, $$8 \\times 14 + 4 = 116$$). Using $$n^2 + 4$: $$8^2 + 4 = 68$$.'
    ),
    -- 2. Analogy (EASY, UNDERSTAND)
    (
        'a1050000-0000-0000-0000-000000000002'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related word pair: **Seismograph : Earthquake :: Anemometer : ?**',
        '[{"id":"A","text":"Wind Speed","isCorrect":true},{"id":"B","text":"Atmospheric Pressure","isCorrect":false},{"id":"C","text":"Relative Humidity","isCorrect":false},{"id":"D","text":"Electric Current","isCorrect":false}]',
        'A',
        'A seismograph measures earthquake intensity, and an anemometer measures wind speed.'
    ),
    -- 3. Analogy (EASY, UNDERSTAND)
    (
        'a1050000-0000-0000-0000-000000000003'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related letter cluster: **ACEG : IKMO :: QSUW : ?**',
        '[{"id":"A","text":"YACE","isCorrect":true},{"id":"B","text":"XZBD","isCorrect":false},{"id":"C","text":"ZBDF","isCorrect":false},{"id":"D","text":"YACO","isCorrect":false}]',
        'A',
        'Each letter shifts forward by $$+8$$ places: $$A(+8)=I, C(+8)=K, E(+8)=M, G(+8)=O$$. Applying to $$QSUW$$: $$Q(+8)=Y, S(+8)=A, U(+8)=C, W(+8)=E$$.'
    ),
    -- 4. Classification (EASY, REMEMBER)
    (
        'a1050000-0000-0000-0000-000000000004'::uuid,
        'Classification',
        'Semantic Classification',
        'EASY',
        'REMEMBER',
        'Three of the following four words are alike in a certain way and one is different. Pick the odd one out:',
        '[{"id":"A","text":"Trombone","isCorrect":false},{"id":"B","text":"Flute","isCorrect":false},{"id":"C","text":"Trumpet","isCorrect":false},{"id":"D","text":"Violin","isCorrect":true}]',
        'D',
        'Violin is a string instrument, whereas Trombone, Flute, and Trumpet are wind/brass instruments.'
    ),
    -- 5. Classification (EASY, REMEMBER)
    (
        'a1050000-0000-0000-0000-000000000005'::uuid,
        'Classification',
        'Symbolic / Number Classification',
        'EASY',
        'REMEMBER',
        'Find the odd number-pair from the given options:',
        '[{"id":"A","text":"$$13 - 169$$","isCorrect":false},{"id":"B","text":"$$17 - 289$$","isCorrect":false},{"id":"C","text":"$$19 - 361$$","isCorrect":false},{"id":"D","text":"$$23 - 527$$","isCorrect":true}]',
        'D',
        'In all options, the second number is the exact square of the first: $$13^2=169, 17^2=289, 19^2=361$$. But $$23^2 = 529 \\ne 527$$.'
    ),
    -- 6. Classification (EASY, REMEMBER)
    (
        'a1050000-0000-0000-0000-000000000006'::uuid,
        'Classification',
        'Semantic Classification',
        'EASY',
        'REMEMBER',
        'Select the odd letter-group among the following:',
        '[{"id":"A","text":"DW","isCorrect":false},{"id":"B","text":"GT","isCorrect":false},{"id":"C","text":"KP","isCorrect":false},{"id":"D","text":"HS","isCorrect":false}]',
        'A',
        'All are pairs of opposite letters in alphabetical order ($$A \\leftrightarrow Z, D \\leftrightarrow W, G \\leftrightarrow T, K \\leftrightarrow P, H \\leftrightarrow S$$).'
    ),
    -- 7. Series (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000007'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'APPLY',
        'Find the missing term in the sequence: $$7, 16, 40, 85, 157, ?$$',
        '[{"id":"A","text":"$$262$$","isCorrect":true},{"id":"B","text":"$$256$$","isCorrect":false},{"id":"C","text":"$$270$$","isCorrect":false},{"id":"D","text":"$$248$$","isCorrect":false}]',
        'A',
        'First differences: $$9, 24, 45, 72$$. Second differences: $$15, 21, 27$$ (arithmetic progression $$+6$$). Next second difference is $$33$$. Next first difference is $$72 + 33 = 105$$. Next term is $$157 + 105 = 262$$.'
    ),
    -- 8. Series (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000008'::uuid,
        'Series',
        'Semantic Series',
        'MEDIUM',
        'APPLY',
        'What will replace the question mark in the letter series: **BZA, DYC, FXE, HWG, ?**',
        '[{"id":"A","text":"JVI","isCorrect":true},{"id":"B","text":"JUH","isCorrect":false},{"id":"C","text":"KVI","isCorrect":false},{"id":"D","text":"JVJ","isCorrect":false}]',
        'A',
        'First letter: $$B(+2) \\to D(+2) \\to F(+2) \\to H(+2) \\to J$$. Second letter: $$Z(-1) \\to Y(-1) \\to X(-1) \\to W(-1) \\to V$$. Third letter: $$A(+2) \\to C(+2) \\to E(+2) \\to G(+2) \\to I$$. Result is **JVI**.'
    ),
    -- 9. Series (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000009'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'APPLY',
        'Identify the wrong number in the series: $$6, 12, 36, 144, 720, 4320, 30240$$',
        '[{"id":"A","text":"$$720$$","isCorrect":false},{"id":"B","text":"$$36$$","isCorrect":false},{"id":"C","text":"All numbers are correct","isCorrect":true},{"id":"D","text":"$$144$$","isCorrect":false}]',
        'C',
        'Multiplication pattern: $$6 \\times 2 = 12$$, $$12 \\times 3 = 36$$, $$36 \\times 4 = 144$$, $$144 \\times 5 = 720$$, $$720 \\times 6 = 4320$$, $$4320 \\times 7 = 30240$$. All terms follow the exact factorial multiplier rule.'
    ),
    -- 10. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000010'::uuid,
        'Coding and Decoding',
        'Coding',
        'MEDIUM',
        'APPLY',
        'In a certain code language, if **TEACHER** is written as **VGCEJGT**, how is **CHILDREN** written in that code?',
        '[{"id":"A","text":"EJKNFTGP","isCorrect":true},{"id":"B","text":"EJKNFTHP","isCorrect":false},{"id":"C","text":"EJKNFTGO","isCorrect":false},{"id":"D","text":"FKLMGTHP","isCorrect":false}]',
        'A',
        'Each letter is replaced by its $$+2$$ position forward in alphabetical order: $$C(+2)=E, H(+2)=J, I(+2)=K, L(+2)=N, D(+2)=F, R(+2)=T, E(+2)=G, N(+2)=P$$.'
    ),
    -- 11. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000011'::uuid,
        'Coding and Decoding',
        'Decoding',
        'MEDIUM',
        'APPLY',
        'If in a code, **MACHINE** is coded as $$19-7-9-14-15-20-11$$, how will **DANGER** be coded in that language?',
        '[{"id":"A","text":"$$10-7-20-13-11-24$$","isCorrect":true},{"id":"B","text":"$$10-6-19-12-10-23$$","isCorrect":false},{"id":"C","text":"$$11-7-20-13-11-24$$","isCorrect":false},{"id":"D","text":"$$10-7-20-14-11-24$$","isCorrect":false}]',
        'A',
        'Each letter alphabetical position is incremented by $$+6$$: $$M(13+6=19), A(1+6=7), C(3+6=9), H(8+6=14), I(9+6=15), N(14+6=20), E(5+6=11)$$. For DANGER: $$D(4+6=10), A(1+6=7), N(14+6=20), G(7+6=13), E(5+6=11), R(18+6=24)$$.'
    ),
    -- 12. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1050000-0000-0000-0000-000000000012'::uuid,
        'Coding and Decoding',
        'Coding',
        'MEDIUM',
        'APPLY',
        'If **RED** is coded as $$27$$ and **GREEN** is coded as $$49$$, how will **YELLOW** be coded in that language?',
        '[{"id":"A","text":"$$92$$","isCorrect":true},{"id":"B","text":"$$88$$","isCorrect":false},{"id":"C","text":"$$96$$","isCorrect":false},{"id":"D","text":"$$90$$","isCorrect":false}]',
        'A',
        'Sum of alphabetical positions: RED $$= 18+5+4 = 27$$. GREEN $$= 7+18+5+5+14 = 49$$. YELLOW $$= 25+5+12+12+15+23 = 92$$.'
    ),
    -- 13. Venn Diagrams (EASY, UNDERSTAND)
    (
        'a1050000-0000-0000-0000-000000000013'::uuid,
        'Venn Diagrams',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Which Venn diagram configuration best represents the relationship between: **Reptiles, Snakes, Lizards**?',
        '[{"id":"A","text":"Two disjoint circles representing Snakes and Lizards completely inside the larger circle representing Reptiles","isCorrect":true},{"id":"B","text":"Three overlapping intersecting circles","isCorrect":false},{"id":"C","text":"Concentric nested three circles","isCorrect":false},{"id":"D","text":"Two overlapping circles inside a third circle","isCorrect":false}]',
        'A',
        'Both Snakes and Lizards belong to the class Reptilia, but are distinct orders/suborders with no overlap.'
    ),
    -- 14. Venn Diagrams (EASY, UNDERSTAND)
    (
        'a1050000-0000-0000-0000-000000000014'::uuid,
        'Venn Diagrams',
        NULL,
        'EASY',
        'UNDERSTAND',
        'In a class of $$60$$ students, $$35$$ study Mathematics, $$25$$ study Physics, and $$15$$ study both subjects. How many students study neither Mathematics nor Physics?',
        '[{"id":"A","text":"$$15$$","isCorrect":true},{"id":"B","text":"$$20$$","isCorrect":false},{"id":"C","text":"$$10$$","isCorrect":false},{"id":"D","text":"$$25$$","isCorrect":false}]',
        'A',
        '$$n(M \\cup P) = n(M) + n(P) - n(M \\cap P) = 35 + 25 - 15 = 45$$. Neither $$= 60 - 45 = 15$$.'
    ),
    -- 15. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000015'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'In a row of $$45$$ girls facing North, Priya is $$18^{\\text{th}}$$ from the left end and Ritu is $$16^{\\text{th}}$$ from the right end. How many girls are seated between Priya and Ritu?',
        '[{"id":"A","text":"$$11$$","isCorrect":true},{"id":"B","text":"$$12$$","isCorrect":false},{"id":"C","text":"$$10$$","isCorrect":false},{"id":"D","text":"$$9$$","isCorrect":false}]',
        'A',
        'Sum of positions from ends $$= 18 + 16 = 34$$. Since $$34 < 45$$, girls between $$= 45 - 34 = 11$$.'
    ),
    -- 16. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000016'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Five friends P, Q, R, S, and T are seated in a line facing North. S is between T and Q. Q is to the immediate left of R. P is to the immediate left of T. Who is sitting in the exact middle?',
        '[{"id":"A","text":"S","isCorrect":true},{"id":"B","text":"T","isCorrect":false},{"id":"C","text":"Q","isCorrect":false},{"id":"D","text":"R","isCorrect":false}]',
        'A',
        'From the clues: P is left of T, S is between T and Q, Q is left of R. The order from left to right is P - T - S - Q - R. The middle person is S.'
    ),
    -- 17. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000017'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'If January $$1, 2024$$ was a Monday, what day of the week was December $$31, 2024$$?',
        '[{"id":"A","text":"Tuesday","isCorrect":true},{"id":"B","text":"Monday","isCorrect":false},{"id":"C","text":"Wednesday","isCorrect":false},{"id":"D","text":"Sunday","isCorrect":false}]',
        'A',
        '$$2024$$ is a leap year with $$366$$ days ($$52$$ weeks $$+ 2$$ odd days). In a leap year, the last day of the year is one day ahead of the first day: Monday $$+ 1 =$$ Tuesday.'
    ),
    -- 18. Critical Thinking (HARD, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000018'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Statement: "Should high-speed rail networks be prioritized over expanding ordinary rail connectivity in developing nations?" Arguments: (I) Yes, high-speed rail boosts regional economic corridors and cuts business travel time significantly. (II) No, affordable mass transit for low-income daily commuters is a more pressing social priority in developing countries.',
        '[{"id":"A","text":"Both arguments I and II are strong","isCorrect":true},{"id":"B","text":"Only argument I is strong","isCorrect":false},{"id":"C","text":"Only argument II is strong","isCorrect":false},{"id":"D","text":"Neither argument is strong","isCorrect":false}]',
        'A',
        'Both arguments address critical policy trade-offs logically: Argument I highlights infrastructure-led economic productivity, while Argument II highlights public welfare and social equity.'
    ),
    -- 19. Critical Thinking (HARD, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000019'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Assertion (A): Mercury is used in traditional liquid-in-glass thermometers. Reason (R): Mercury has a uniform coefficient of thermal expansion and does not stick to glass.',
        '[{"id":"A","text":"Both A and R are true, and R is the correct explanation of A","isCorrect":true},{"id":"B","text":"Both A and R are true, but R is not the correct explanation of A","isCorrect":false},{"id":"C","text":"A is true but R is false","isCorrect":false},{"id":"D","text":"A is false but R is true","isCorrect":false}]',
        'A',
        'Mercury remains liquid over a broad range ($$-39^\\circ\\text{C}$$ to $$357^\\circ\\text{C}$$), has high meniscus surface tension preventing adhesion to capillary walls, and expands uniformly with temperature.'
    ),
    -- 20. Critical Thinking (HARD, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000020'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Statement: "A sudden rise in the wholesale price index of vegetables was recorded due to unseasonal monsoon rains in northern states." Course of Action: (I) The government should immediately release buffer stocks and facilitate interstate supply chains. (II) All vegetable sales in retail markets should be banned for two weeks.',
        '[{"id":"A","text":"Only course of action I follows","isCorrect":true},{"id":"B","text":"Only course of action II follows","isCorrect":false},{"id":"C","text":"Both follow","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'Releasing buffer stocks stabilizes market supply and curbs inflation. Banning sales would create severe shortage and black marketing.'
    ),
    -- 21. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000021'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: (1) All poets are daydreamers. (2) All painters are daydreamers. Conclusions: (I) Some painters are poets. (II) Some daydreamers are painters.',
        '[{"id":"A","text":"Only conclusion (II) follows","isCorrect":true},{"id":"B","text":"Only conclusion (I) follows","isCorrect":false},{"id":"C","text":"Both follow","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'From "All painters are daydreamers", the converse "Some daydreamers are painters" is necessarily true (II follows). The middle term "daydreamers" is undistributed in both premises, so no definite relation between poets and painters can be deduced.'
    ),
    -- 22. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000022'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: (1) Some books are pens. (2) No pen is a pencil. Conclusions: (I) Some books are not pencils. (II) All pencils being books is a possibility.',
        '[{"id":"A","text":"Both conclusions (I) and (II) follow","isCorrect":true},{"id":"B","text":"Only conclusion (I) follows","isCorrect":false},{"id":"C","text":"Only conclusion (II) follows","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'The books that are pens can never be pencils, thus "Some books are not pencils" is definitely true (I follows). Pencils can overlap with the non-pen portion of books, so "All pencils being books is a possibility" is valid (II follows).'
    ),
    -- 23. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1050000-0000-0000-0000-000000000023'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: (1) All stars are moons. (2) All moons are planets. (3) No planet is a comet. Conclusions: (I) No star is a comet. (II) Some planets are stars.',
        '[{"id":"A","text":"Both conclusions (I) and (II) follow","isCorrect":true},{"id":"B","text":"Only conclusion (I) follows","isCorrect":false},{"id":"C","text":"Only conclusion (II) follows","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'Since all stars are planets, and no planet is a comet, no star can be a comet (I follows). Also since stars are inside planets, some planets are stars (II follows).'
    ),
    -- 24. Numerical Operations (EASY, APPLY)
    (
        'a1050000-0000-0000-0000-000000000024'::uuid,
        'Numerical Operations',
        NULL,
        'EASY',
        'APPLY',
        'If $$P$$ denotes $$\\div$$, $$Q$$ denotes $$\\times$$, $$R$$ denotes $$+$$, and $$S$$ denotes $$-$$: what is the value of $$18 Q 12 P 4 R 5 S 6$$?',
        '[{"id":"A","text":"$$53$$","isCorrect":true},{"id":"B","text":"$$54$$","isCorrect":false},{"id":"C","text":"$$48$$","isCorrect":false},{"id":"D","text":"$$59$$","isCorrect":false}]',
        'A',
        'Expression $$= 18 \\times 12 \\div 4 + 5 - 6 = 18 \\times 3 + 5 - 6 = 54 + 5 - 6 = 53$$.'
    ),
    -- 25. Numerical Operations (EASY, APPLY)
    (
        'a1050000-0000-0000-0000-000000000025'::uuid,
        'Numerical Operations',
        NULL,
        'EASY',
        'APPLY',
        'Which two mathematical operators should be interchanged to make the given equation correct? $$14 + 4 \\times 5 - 18 \\div 2 = 25$$',
        '[{"id":"A","text":"$$+$$ and $$\\times$$","isCorrect":false},{"id":"B","text":"$$+$$ and $$-$$","isCorrect":true},{"id":"C","text":"$$\\times$$ and $$\\div$$","isCorrect":false},{"id":"D","text":"$$-$$ and $$\\div$$","isCorrect":false}]',
        'B',
        'Interchanging $$+$$ and $$-$$ gives: $$14 - 4 \\times 5 + 18 \\div 2 = 14 - 20 + 9 = 3$$. With $$\\times$$ and $$+$$: $$14 \\times 4 + 5 - 9 = 52$$. Interchanging $$+$$ and $$-$$ in original equation: $$14 - 4 \\times 5 + 9 = 3$$. Interchanging $$+$$ and $$\\div$$: $$14 \\div 4...$$ Interchanging $$4$$ and $$5$$: $$14 + 20 - 9 = 25$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
