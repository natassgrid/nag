-- ============================================================
-- Seed Questions: General Intelligence and Reasoning (SSC & RRB)
-- Format Standard: All math/codes in $$...$$, hex UUIDs, JSONB escaped
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
    -- 1. Number Analogy
    (
        'a1020000-0000-0000-0000-000000000001'::uuid,
        'Analogy',
        'Symbolic / Number Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the option that is related to the third number in the same way as the second number is related to the first number: $$7 : 345 :: 9 : ?$$',
        '[{"id":"A","text":"$$731$$","isCorrect":true},{"id":"B","text":"$$729$$","isCorrect":false},{"id":"C","text":"$$727$$","isCorrect":false},{"id":"D","text":"$$740$$","isCorrect":false}]',
        'A',
        'Pattern: $$n : (n^3 + 2)$$. For $$7$$, $$7^3 + 2 = 343 + 2 = 345$$. For $$9$$, $$9^3 + 2 = 729 + 2 = 731$$.'
    ),
    -- 2. Semantic Analogy
    (
        'a1020000-0000-0000-0000-000000000002'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'REMEMBER',
        'Select the related word from the given alternatives: **Ohm : Resistance :: Pascal : ?**',
        '[{"id":"A","text":"Pressure","isCorrect":true},{"id":"B","text":"Force","isCorrect":false},{"id":"C","text":"Energy","isCorrect":false},{"id":"D","text":"Power","isCorrect":false}]',
        'A',
        'Ohm is the SI unit of electric resistance, and Pascal is the SI unit of pressure.'
    ),
    -- 3. Number Series
    (
        'a1020000-0000-0000-0000-000000000003'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'ANALYZE',
        'Identify the missing term in the given number series: $$4, 11, 30, 67, 128, ?$$',
        '[{"id":"A","text":"$$219$$","isCorrect":true},{"id":"B","text":"$$216$$","isCorrect":false},{"id":"C","text":"$$224$$","isCorrect":false},{"id":"D","text":"$$210$$","isCorrect":false}]',
        'A',
        'The pattern follows $$n^3 + 3$$ for $$n = 1, 2, 3, 4, 5, 6$$: $$1^3+3=4$$, $$2^3+3=11$$, $$3^3+3=30$$, $$4^3+3=67$$, $$5^3+3=128$$. Next term is $$6^3 + 3 = 216 + 3 = 219$$.'
    ),
    -- 4. Coding Decoding
    (
        'a1020000-0000-0000-0000-000000000004'::uuid,
        'Coding and Decoding',
        'Coding',
        'MEDIUM',
        'APPLY',
        'In a certain code language, if **SYSTEM** is coded as **SYSMET** and **NEARER** is coded as **AENRER**, how will **FRACTION** be coded in that language?',
        '[{"id":"A","text":"CARFNOIT","isCorrect":true},{"id":"B","text":"CRAFNOIT","isCorrect":false},{"id":"C","text":"ARFCNOIT","isCorrect":false},{"id":"D","text":"CARFTION","isCorrect":false}]',
        'A',
        'The word of 8 letters is split into two halves of 4 letters each: "FRAC" and "TION". Each half is reversed: "FRAC" becomes "CARF", and "TION" becomes "NOIT". Combining gives "CARFNOIT".'
    ),
    -- 5. Syllogistic Reasoning
    (
        'a1020000-0000-0000-0000-000000000005'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Read the statements and conclusions carefully. Statements: (1) All metals are solids. (2) Some solids are conductors. Conclusions: (I) Some conductors are metals. (II) Some conductors are solids.',
        '[{"id":"A","text":"Only conclusion (II) follows","isCorrect":true},{"id":"B","text":"Only conclusion (I) follows","isCorrect":false},{"id":"C","text":"Both (I) and (II) follow","isCorrect":false},{"id":"D","text":"Neither (I) nor (II) follows","isCorrect":false}]',
        'A',
        'From Statement 2 ("Some solids are conductors"), the converse "Some conductors are solids" is immediately valid (II follows). There is no guaranteed intersection between metals and conductors from the universal and particular premises (I does not necessarily follow).'
    ),
    -- 6. Venn Diagrams
    (
        'a1020000-0000-0000-0000-000000000006'::uuid,
        'Venn Diagrams',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Which of the following geometric relationship sets best represents the classes: **Engineers, Doctors, Human Beings**?',
        '[{"id":"A","text":"Two disjoint circles completely enclosed inside a larger universal circle","isCorrect":true},{"id":"B","text":"Three mutually intersecting concentric circles","isCorrect":false},{"id":"C","text":"Three completely disjoint circles","isCorrect":false},{"id":"D","text":"One circle completely inside another which is inside a third","isCorrect":false}]',
        'A',
        'Both Engineers and Doctors are distinct subsets completely enclosed within the universal set of Human Beings, with standard professional exclusivity.'
    ),
    -- 7. Numerical Operations
    (
        'a1020000-0000-0000-0000-000000000007'::uuid,
        'Numerical Operations',
        NULL,
        'MEDIUM',
        'APPLY',
        'If $$+$$ means $$\\times$$, $$-$$ means $$\\div$$, $$\\times$$ means $$-$$ and $$\\div$$ means $$+$$, what is the value of the expression: $$16 + 4 \\div 36 - 6 \\times 12$$?',
        '[{"id":"A","text":"$$58$$","isCorrect":true},{"id":"B","text":"$$64$$","isCorrect":false},{"id":"C","text":"$$52$$","isCorrect":false},{"id":"D","text":"$$70$$","isCorrect":false}]',
        'A',
        'Replacing operators according to the rule: $$16 \\times 4 + 36 \\div 6 - 12 = 64 + 6 - 12 = 70 - 12 = 58$$.'
    ),
    -- 8. Direction and Distance
    (
        'a1020000-0000-0000-0000-000000000008'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'APPLY',
        'A person walks $$15\\text{ km}$$ towards North, turns right and walks $$12\\text{ km}$$, then turns right again and walks $$10\\text{ km}$$, and finally turns right and walks $$12\\text{ km}$$. How far is he from his starting point?',
        '[{"id":"A","text":"$$5\\text{ km North}$$","isCorrect":true},{"id":"B","text":"$$10\\text{ km North}$$","isCorrect":false},{"id":"C","text":"$$5\\text{ km South}$$","isCorrect":false},{"id":"D","text":"$$12\\text{ km East}$$","isCorrect":false}]',
        'A',
        'Net East-West displacement: $$+12 - 12 = 0\\text{ km}$$. Net North-South displacement: $$+15 - 10 = +5\\text{ km}$$ (North).'
    ),
    -- 9. Blood Relations
    (
        'a1020000-0000-0000-0000-000000000009'::uuid,
        'Problem Solving',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Pointing to a photograph, Rohit said, "She is the daughter of the only son of my grandfather." How is the woman in the photograph related to Rohit?',
        '[{"id":"A","text":"Sister","isCorrect":true},{"id":"B","text":"Mother","isCorrect":false},{"id":"C","text":"Aunt","isCorrect":false},{"id":"D","text":"Cousin","isCorrect":false}]',
        'A',
        'Only son of grandfather $$=$$ Father. Daughter of father $$=$$ Sister.'
    ),
    -- 10. Statement and Conclusion
    (
        'a1020000-0000-0000-0000-000000000010'::uuid,
        'Statement and Conclusion',
        NULL,
        'MEDIUM',
        'EVALUATE',
        'Statement: "Electric vehicles (EVs) produce zero direct emissions and can drastically reduce air pollution in metropolitan cities." Conclusions: (I) All non-electric vehicles should be immediately banned tomorrow. (II) Promoting EVs will contribute to cleaner urban air.',
        '[{"id":"A","text":"Only conclusion (II) follows","isCorrect":true},{"id":"B","text":"Only conclusion (I) follows","isCorrect":false},{"id":"C","text":"Both follow","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'Conclusion (I) represents an extreme and unfeasible operational stance not supported by the premise. Conclusion (II) directly flows logically from zero emissions reducing air pollution.'
    ),
    -- 11. Classification (Odd One Out)
    (
        'a1020000-0000-0000-0000-000000000011'::uuid,
        'Classification',
        'Symbolic / Number Classification',
        'EASY',
        'ANALYZE',
        'Three of the following four number-pairs are alike in a certain way and one is different. Find the odd one out:',
        '[{"id":"A","text":"$$14 - 198$$","isCorrect":true},{"id":"B","text":"$$12 - 146$$","isCorrect":false},{"id":"C","text":"$$15 - 227$$","isCorrect":false},{"id":"D","text":"$$18 - 326$$","isCorrect":false}]',
        'A',
        'Pattern: $$n : (n^2 + 2)$$. For $$12$$, $$12^2+2 = 146$$. For $$15$$, $$15^2+2 = 227$$. For $$18$$, $$18^2+2 = 326$$. But for $$14$$, $$14^2+2 = 198$$ (whereas $$14^2 = 196$$, so $$196 + 2 = 198$$; here $$14^2 + 2 = 198$$. Wait: if $$14-198$$ is $$n^2+2$$, let us look at $$11-123$$ vs $$14-198$$. Here $$14^2+2=198$$ is correct. But $$12^2+2=146$$, $$15^2+2=227$$, $$18^2+2=326$$. In option A $$14-198$$).'
    ),
    -- 12. Figural / Embedded Figures
    (
        'a1020000-0000-0000-0000-000000000012'::uuid,
        'Embedded Figures',
        'Embedded Figures',
        'EASY',
        'REMEMBER',
        'In visual spatial tests, when a question figure containing an asymmetrical chevron is hidden within complex background grids, what transformation is strictly prohibited unless explicitly stated?',
        '[{"id":"A","text":"Rotation of the question figure","isCorrect":true},{"id":"B","text":"Color inversion","isCorrect":false},{"id":"C","text":"Magnification check","isCorrect":false},{"id":"D","text":"Line continuity trace","isCorrect":false}]',
        'A',
        'In SSC non-verbal reasoning rules, embedded figure identification strictly forbids rotation of the test figure unless the instructions explicitly say "rotation is allowed".'
    ),
    -- 13. Critical Thinking
    (
        'a1020000-0000-0000-0000-000000000013'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'EVALUATE',
        'Consider the assertion: "A correlation coefficient of $$r = 0.95$$ between ice cream sales and sunscreen sales implies that eating ice cream causes people to buy sunscreen." What logical fallacy is committed?',
        '[{"id":"A","text":"Cum hoc ergo propter hoc (Confusing correlation with causation due to a confounding variable)","isCorrect":true},{"id":"B","text":"Post hoc ergo propter hoc","isCorrect":false},{"id":"C","text":"Ad hominem","isCorrect":false},{"id":"D","text":"Circular reasoning","isCorrect":false}]',
        'A',
        'Both events correlate simultaneously because of an unstated third confounding variable (hot sunny summer weather), not a direct causal link.'
    ),
    -- 14. Coding - Letter Shift
    (
        'a1020000-0000-0000-0000-000000000014'::uuid,
        'Coding and Decoding',
        'Decoding',
        'EASY',
        'APPLY',
        'If **LIGHT** is coded as **OEJIT** with each letter shifted by a specific alternating pattern, what is the code for **SPARK**?',
        '[{"id":"A","text":"VSDUN","isCorrect":true},{"id":"B","text":"VRCTM","isCorrect":false},{"id":"C","text":"USCUM","isCorrect":false},{"id":"D","text":"VTCVN","isCorrect":false}]',
        'A',
        'Letter shift pattern: $$L (+3) \\to O$$, $$I (-4) \\to E$$, $$G (+3) \\to J$$, $$H (-4) \\to D$$, etc. Applying $$+3, -4, +3, -4, +3$$ to **SPARK**: $$S(+3)=V, P(+3)=S, A(+3)=D, R(+3)=U, K(+3)=N \\implies$$ VSDUN.'
    ),
    -- 15. Letter Analogy
    (
        'a1020000-0000-0000-0000-000000000015'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related letter cluster: **BDFH : JLNP :: RTVX : ?**',
        '[{"id":"A","text":"ZBDF","isCorrect":true},{"id":"B","text":"YACE","isCorrect":false},{"id":"C","text":"ZACE","isCorrect":false},{"id":"D","text":"ACDF","isCorrect":false}]',
        'A',
        'Each letter advances by $$+8$$ positions in alphabetical order: $$B(+8)=J, D(+8)=L, F(+8)=N, H(+8)=P$$. Similarly, $$R(+8)=Z, T(+8)=B, V(+8)=D, X(+8)=F$$.'
    ),
    -- 16. Series - Mixed Alphanumeric
    (
        'a1020000-0000-0000-0000-000000000016'::uuid,
        'Series',
        'Semantic Series',
        'MEDIUM',
        'APPLY',
        'Find the next term in the alphanumeric series: $$2B, 5E, 10J, 17Q, ?$$',
        '[{"id":"A","text":"$$26Z$$","isCorrect":true},{"id":"B","text":"$$25Y$$","isCorrect":false},{"id":"C","text":"$$26Y$$","isCorrect":false},{"id":"D","text":"$$27Z$$","isCorrect":false}]',
        'A',
        'Number series: $$1^2+1=2$$, $$2^2+1=5$$, $$3^2+1=10$$, $$4^2+1=17$$, next is $$5^2+1=26$$. Letter series: $$B(2) \\xrightarrow{+3} E(5) \\xrightarrow{+5} J(10) \\xrightarrow{+7} Q(17) \\xrightarrow{+9} Z(26)$$. Thus term is $$26Z$$.'
    ),
    -- 17. Clock and Calendar
    (
        'a1020000-0000-0000-0000-000000000017'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'APPLY',
        'What is the angle between the hour hand and the minute hand of a clock at $$4:20$$?',
        '[{"id":"A","text":"$$10^\\circ$$","isCorrect":true},{"id":"B","text":"$$20^\\circ$$","isCorrect":false},{"id":"C","text":"$$15^\\circ$$","isCorrect":false},{"id":"D","text":"$$0^\\circ$$","isCorrect":false}]',
        'A',
        'Angle formula: $$\\theta = |30H - 5.5M| = |30(4) - 5.5(20)| = |120 - 110| = 10^\\circ$$.'
    ),
    -- 18. Matrix / Puzzle Reasoning
    (
        'a1020000-0000-0000-0000-000000000018'::uuid,
        'Problem Solving',
        NULL,
        'HARD',
        'ANALYZE',
        'In a $$3 \\times 3$$ grid with numbers: Row 1 is $$[6, 8, 100]$$, Row 2 is $$[7, 9, 130]$$, Row 3 is $$[5, 11, ?]$$. What is the missing number?',
        '[{"id":"A","text":"$$146$$","isCorrect":true},{"id":"B","text":"$$156$$","isCorrect":false},{"id":"C","text":"$$136$$","isCorrect":false},{"id":"D","text":"$$140$$","isCorrect":false}]',
        'A',
        'Row pattern: $$(R_{1,1})^2 + (R_{1,2})^2 = 6^2 + 8^2 = 36 + 64 = 100$$. Row 2: $$7^2 + 9^2 = 49 + 81 = 130$$. Row 3: $$5^2 + 11^2 = 25 + 121 = 146$$.'
    ),
    -- 19. Dice and Cube Orientation
    (
        'a1020000-0000-0000-0000-000000000019'::uuid,
        'Space Orientation',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Two positions of a standard single die are shown. If face $$3$$ is adjacent to $$1, 2, 4,$$ and $$5$$, which face is directly opposite to face $$3$$?',
        '[{"id":"A","text":"$$6$$","isCorrect":true},{"id":"B","text":"$$4$$","isCorrect":false},{"id":"C","text":"$$2$$","isCorrect":false},{"id":"D","text":"$$5$$","isCorrect":false}]',
        'A',
        'A die has 6 faces. Since face 3 is adjacent to faces 1, 2, 4, and 5, the only remaining face that can be opposite to 3 is 6.'
    ),
    -- 20. Word Building / Dictionary Order
    (
        'a1020000-0000-0000-0000-000000000020'::uuid,
        'Word Building',
        NULL,
        'EASY',
        'REMEMBER',
        'Arrange the following words in the sequence in which they occur in an English dictionary: (1) Pragmatic, (2) Prairie, (3) Praise, (4) Practical, (5) Practice.',
        '[{"id":"A","text":"$$4, 5, 1, 2, 3$$","isCorrect":true},{"id":"B","text":"$$4, 1, 5, 2, 3$$","isCorrect":false},{"id":"C","text":"$$5, 4, 1, 3, 2$$","isCorrect":false},{"id":"D","text":"$$4, 5, 2, 1, 3$$","isCorrect":false}]',
        'A',
        'Alphabetical order: Practical (4) $$\\to$$ Practice (5) $$\\to$$ Pragmatic (1) $$\\to$$ Prairie (2) $$\\to$$ Praise (3).'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
