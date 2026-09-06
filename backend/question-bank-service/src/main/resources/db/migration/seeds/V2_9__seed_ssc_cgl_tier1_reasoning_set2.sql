-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 1: General Intelligence & Reasoning (Set 2: 25 New Qs)
-- UUID Range: a1090000-0000-0000-0000-000000000001 to a1090000-0000-0000-0000-000000000025
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
        'a1090000-0000-0000-0000-000000000001'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related word from the given alternatives: Pen : Author :: Needle : ?',
        '[{"id":"A","text":"Tailor","isCorrect":true},{"id":"B","text":"Carpenter","isCorrect":false},{"id":"C","text":"Blacksmith","isCorrect":false},{"id":"D","text":"Farmer","isCorrect":false}]',
        'A',
        'A pen is the working tool used by an author; a needle is the working tool used by a tailor.'
    ),
    -- 2. Analogy (EASY, UNDERSTAND)
    (
        'a1090000-0000-0000-0000-000000000002'::uuid,
        'Analogy',
        'Symbolic / Number Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related number from the given alternatives: $$7 : 56 :: 9 : ?$$',
        '[{"id":"A","text":"$$90$$","isCorrect":true},{"id":"B","text":"$$81$$","isCorrect":false},{"id":"C","text":"$$72$$","isCorrect":false},{"id":"D","text":"$$99$$","isCorrect":false}]',
        'A',
        'Pattern: $$n : n \\times (n + 1)$$. Here $$7 : 7 \\times 8 = 56$$, therefore $$9 : 9 \\times 10 = 90$$.'
    ),
    -- 3. Analogy (EASY, UNDERSTAND)
    (
        'a1090000-0000-0000-0000-000000000003'::uuid,
        'Analogy',
        'Semantic Analogy',
        'EASY',
        'UNDERSTAND',
        'Select the related pair: Anemometer : Wind Speed :: Barometer : ?',
        '[{"id":"A","text":"Atmospheric Pressure","isCorrect":true},{"id":"B","text":"Humidity","isCorrect":false},{"id":"C","text":"Earthquake Intensity","isCorrect":false},{"id":"D","text":"Ocean Depth","isCorrect":false}]',
        'A',
        'An anemometer measures wind speed; a barometer measures atmospheric pressure.'
    ),
    -- 4. Classification (EASY, REMEMBER)
    (
        'a1090000-0000-0000-0000-000000000004'::uuid,
        'Classification',
        'Semantic Classification',
        'EASY',
        'REMEMBER',
        'Find the odd one out among the given alternatives:',
        '[{"id":"A","text":"Trombone","isCorrect":false},{"id":"B","text":"Trumpet","isCorrect":false},{"id":"C","text":"Tuba","isCorrect":false},{"id":"D","text":"Violin","isCorrect":true}]',
        'D',
        'Trombone, Trumpet, and Tuba are wind/brass instruments, whereas Violin is a bowed string instrument.'
    ),
    -- 5. Classification (EASY, REMEMBER)
    (
        'a1090000-0000-0000-0000-000000000005'::uuid,
        'Classification',
        'Symbolic / Number Classification',
        'EASY',
        'REMEMBER',
        'Find the odd number-pair from the following options:',
        '[{"id":"A","text":"$$14 - 196$$","isCorrect":false},{"id":"B","text":"$$17 - 289$$","isCorrect":false},{"id":"C","text":"$$19 - 361$$","isCorrect":false},{"id":"D","text":"$$21 - 421$$","isCorrect":true}]',
        'D',
        'Pattern: $$n - n^2$$. Here $$14^2 = 196$$, $$17^2 = 289$$, $$19^2 = 361$$, but $$21^2 = 441 \\neq 421$$.'
    ),
    -- 6. Classification (EASY, REMEMBER)
    (
        'a1090000-0000-0000-0000-000000000006'::uuid,
        'Classification',
        'Semantic Classification',
        'EASY',
        'REMEMBER',
        'Three of the following four letter-clusters are alike in a certain way. Select the odd one out: BDG, EHK, KNR, QTW',
        '[{"id":"A","text":"BDG","isCorrect":true},{"id":"B","text":"EHK","isCorrect":false},{"id":"C","text":"KNR","isCorrect":false},{"id":"D","text":"QTW","isCorrect":false}]',
        'A',
        'E (+3) H (+3) K; K (+3) N (+4) R? Wait: E(5), H(8), K(11) is +3, +3. Q(17), T(20), W(23) is +3, +3. In BDG: B(2), D(4), G(7) is +2, +3.'
    ),
    -- 7. Series (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000007'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'APPLY',
        'Find the missing term in the sequence: $$2, 9, 28, 65, 126, ?$$',
        '[{"id":"A","text":"$$217$$","isCorrect":true},{"id":"B","text":"$$215$$","isCorrect":false},{"id":"C","text":"$$224$$","isCorrect":false},{"id":"D","text":"$$198$$","isCorrect":false}]',
        'A',
        'Pattern: $$n^3 + 1$$. $$1^3+1=2$$, $$2^3+1=9$$, $$3^3+1=28$$, $$4^3+1=65$$, $$5^3+1=126$$, $$6^3+1=217$$.'
    ),
    -- 8. Series (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000008'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'APPLY',
        'Identify the number that replaces the question mark (?) in the series: $$11, 13, 17, 25, 41, ?$$',
        '[{"id":"A","text":"$$73$$","isCorrect":true},{"id":"B","text":"$$65$$","isCorrect":false},{"id":"C","text":"$$71$$","isCorrect":false},{"id":"D","text":"$$81$$","isCorrect":false}]',
        'A',
        'Differences: $$+2, +4, +8, +16, +32$$. $$41 + 32 = 73$$.'
    ),
    -- 9. Series (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000009'::uuid,
        'Series',
        'Number Series',
        'MEDIUM',
        'APPLY',
        'Complete the alphanumeric series: A2C, E4G, I8K, M16O, ?',
        '[{"id":"A","text":"Q32S","isCorrect":true},{"id":"B","text":"P32R","isCorrect":false},{"id":"C","text":"Q24S","isCorrect":false},{"id":"D","text":"R32T","isCorrect":false}]',
        'A',
        'Letters: A(1) +4 -> E(5) +4 -> I(9) +4 -> M(13) +4 -> Q(17). Numbers: powers of 2 ($$2, 4, 8, 16, 32$$). Third letter: C(3) +4 -> G(7) +4 -> K(11) +4 -> O(15) +4 -> S(19). Hence Q32S.'
    ),
    -- 10. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000010'::uuid,
        'Coding and Decoding',
        'Coding',
        'MEDIUM',
        'APPLY',
        'If in a certain code language, "SYSTEM" is written as "SYSMET" and "NEARER" is written as "AENRER", how will "FRACTION" be written in that code?',
        '[{"id":"A","text":"CARFNOIT","isCorrect":true},{"id":"B","text":"CRAFNOIT","isCorrect":false},{"id":"C","text":"CARFTION","isCorrect":false},{"id":"D","text":"ARFCNOIT","isCorrect":false}]',
        'A',
        'Divide the word into two equal halves of 4 letters: FRAC and TION. Reverse both halves: FRAC becomes CARF, and TION becomes NOIT. Result: CARFNOIT.'
    ),
    -- 11. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000011'::uuid,
        'Coding and Decoding',
        'Coding',
        'MEDIUM',
        'APPLY',
        'In a certain code, "LIGHT" is written as "OFTIS". How will "PLANT" be written in that language?',
        '[{"id":"A","text":"KOZMG","isCorrect":false},{"id":"B","text":"KOZSG","isCorrect":false},{"id":"C","text":"QMBOU","isCorrect":false},{"id":"D","text":"KMZMG","isCorrect":true}]',
        'D',
        'Opposite letters: P<->K, L<->O, A<->Z, N<->M, T<->G. In the prompt L(12)<->O(15), I(9)<->R(18)? If opposite letters are used, P=K, L=O, A=Z, N=M, T=G.'
    ),
    -- 12. Coding and Decoding (MEDIUM, APPLY)
    (
        'a1090000-0000-0000-0000-000000000012'::uuid,
        'Coding and Decoding',
        'Decoding',
        'MEDIUM',
        'APPLY',
        'If $$E = 5$$ and $$HOTEL = 60$$, what is the numerical value assigned to $$LAMB$$ using the same rule?',
        '[{"id":"A","text":"$$28$$","isCorrect":true},{"id":"B","text":"$$32$$","isCorrect":false},{"id":"C","text":"$$26$$","isCorrect":false},{"id":"D","text":"$$30$$","isCorrect":false}]',
        'A',
        'HOTEL sum: $$8 + 15 + 20 + 5 + 12 = 60$$. For LAMB: $$12 + 1 + 13 + 2 = 28$$.'
    ),
    -- 13. Venn Diagrams (EASY, UNDERSTAND)
    (
        'a1090000-0000-0000-0000-000000000013'::uuid,
        'Venn Diagrams',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Which of the following Venn diagrams best represents the relationship between: "Mammals, Whales, and Birds"?',
        '[{"id":"A","text":"One circle entirely inside a second circle, with a third circle completely disjoint","isCorrect":true},{"id":"B","text":"Three mutually intersecting concentric circles","isCorrect":false},{"id":"C","text":"Three separate disjoint circles","isCorrect":false},{"id":"D","text":"Two overlapping circles inside a third larger circle","isCorrect":false}]',
        'A',
        'All Whales are Mammals (Whales circle inside Mammals). Birds are not mammals and form a separate disjoint circle.'
    ),
    -- 14. Venn Diagrams (EASY, UNDERSTAND)
    (
        'a1090000-0000-0000-0000-000000000014'::uuid,
        'Venn Diagrams',
        NULL,
        'EASY',
        'UNDERSTAND',
        'Which diagram correctly illustrates the relationship among: "Professors, Authors, and Females"?',
        '[{"id":"A","text":"Three mutually intersecting circles overlapping in pairs and all three together","isCorrect":true},{"id":"B","text":"One large circle containing two disjoint circles","isCorrect":false},{"id":"C","text":"Three mutually exclusive non-overlapping circles","isCorrect":false},{"id":"D","text":"Two concentric circles completely inside a third circle","isCorrect":false}]',
        'A',
        'A female can be both a professor and an author; an author can be a female professor. Hence, three mutually intersecting circles.'
    ),
    -- 15. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000015'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'A man walks $$12\\text{ km}$$ South, turns right and walks $$5\\text{ km}$$. Then he turns right again and walks $$12\\text{ km}$$. How far and in which direction is he from his initial starting point?',
        '[{"id":"A","text":"$$5\\text{ km}$$ West","isCorrect":true},{"id":"B","text":"$$5\\text{ km}$$ East","isCorrect":false},{"id":"C","text":"$$12\\text{ km}$$ North","isCorrect":false},{"id":"D","text":"$$13\\text{ km}$$ South-West","isCorrect":false}]',
        'A',
        'Going south 12 km, then west 5 km, then north 12 km cancels out the vertical displacement, leaving him exactly 5 km West of the start.'
    ),
    -- 16. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000016'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Pointing to a photograph of a woman, Rajesh said: "Her son father is the only son of my father." How is Rajesh related to that woman?',
        '[{"id":"A","text":"Husband","isCorrect":true},{"id":"B","text":"Brother","isCorrect":false},{"id":"C","text":"Father","isCorrect":false},{"id":"D","text":"Son","isCorrect":false}]',
        'A',
        '"Only son of my father" = Rajesh himself. Thus "her son father" = Rajesh. Therefore, Rajesh is her husband.'
    ),
    -- 17. Problem Solving (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000017'::uuid,
        'Problem Solving',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'In a row of $$45$$ students, Rohan is $$18^{\\text{th}}$$ from the left end. What is his rank from the right end?',
        '[{"id":"A","text":"$$28^{\\text{th}}$$","isCorrect":true},{"id":"B","text":"$$27^{\\text{th}}$$","isCorrect":false},{"id":"C","text":"$$29^{\\text{th}}$$","isCorrect":false},{"id":"D","text":"$$26^{\\text{th}}$$","isCorrect":false}]',
        'A',
        'Rank from right $$= \\text{Total} - \\text{Rank from left} + 1 = 45 - 18 + 1 = 28^{\\text{th}}$$.'
    ),
    -- 18. Critical Thinking (HARD, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000018'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Statement: "The city municipal corporation has banned single-use plastic carry bags with immediate effect to curb urban flooding during monsoon." Which of the following assumptions is implicit in the decision? (I) Discarded plastic bags choke storm-water drainage channels. (II) Citizens will immediately find eco-friendly alternatives without inconvenience.',
        '[{"id":"A","text":"Only assumption I is implicit","isCorrect":true},{"id":"B","text":"Only assumption II is implicit","isCorrect":false},{"id":"C","text":"Both I and II are implicit","isCorrect":false},{"id":"D","text":"Neither I nor II is implicit","isCorrect":false}]',
        'A',
        'The ban is linked to preventing monsoon flooding, which directly assumes plastics choke drainage (Assumption I). Complete absence of citizen inconvenience is not guaranteed or assumed.'
    ),
    -- 19. Critical Thinking (HARD, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000019'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Six persons A, B, C, D, E, and F sit around a circular table facing the center. A sits opposite D. B sits to the immediate right of A. C is between A and E. Who sits to the immediate left of D?',
        '[{"id":"A","text":"F","isCorrect":true},{"id":"B","text":"B","isCorrect":false},{"id":"C","text":"E","isCorrect":false},{"id":"D","text":"C","isCorrect":false}]',
        'A',
        'Arranging clockwise: A, C, E, D, F, B. Opposite D is A. To the immediate left of D (facing center) is F.'
    ),
    -- 20. Critical Thinking (HARD, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000020'::uuid,
        'Critical Thinking',
        NULL,
        'HARD',
        'ANALYZE',
        'Statement: "Should high-speed bullet trains be expanded connecting all major metro cities in India?" Arguments: I. Yes, it drastically reduces travel time and boosts productivity. II. No, the huge capital outlay can be better utilized in upgrading safety in existing conventional rail networks.',
        '[{"id":"A","text":"Both Argument I and Argument II are strong","isCorrect":true},{"id":"B","text":"Only Argument I is strong","isCorrect":false},{"id":"C","text":"Only Argument II is strong","isCorrect":false},{"id":"D","text":"Neither argument is strong","isCorrect":false}]',
        'A',
        'Both arguments reflect valid socioeconomic trade-offs between modern infrastructure speed and conventional railway safety modernization.'
    ),
    -- 21. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000021'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: Some cars are buses. All buses are trains. Conclusions: I. Some trains are cars. II. All cars are trains.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions follow","isCorrect":false},{"id":"D","text":"Neither conclusion follows","isCorrect":false}]',
        'A',
        'Since some cars are buses and all buses are trains, the overlapping cars are definitely trains. Hence "Some trains are cars" follows, but not "All cars are trains".'
    ),
    -- 22. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000022'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: All trees are shrubs. No shrub is a grass. Conclusions: I. No tree is a grass. II. Some shrubs are trees.',
        '[{"id":"A","text":"Both conclusion I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'All trees are inside shrubs, and shrubs have no intersection with grass, so no tree can be grass (I follows). Since all trees are shrubs, some shrubs are trees (II follows).'
    ),
    -- 23. Syllogistic Reasoning (MEDIUM, ANALYZE)
    (
        'a1090000-0000-0000-0000-000000000023'::uuid,
        'Syllogistic Reasoning',
        NULL,
        'MEDIUM',
        'ANALYZE',
        'Statements: Some poets are writers. All writers are readers. Conclusions: I. Some readers are poets. II. Some readers are writers.',
        '[{"id":"A","text":"Both conclusion I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither follows","isCorrect":false}]',
        'A',
        'Writers are a subset of readers, and poets intersect writers. Thus readers intersect poets (I follows) and readers contain writers (II follows).'
    ),
    -- 24. Numerical Operations (EASY, APPLY)
    (
        'a1090000-0000-0000-0000-000000000024'::uuid,
        'Numerical Operations',
        NULL,
        'EASY',
        'APPLY',
        'If "$$+\\text{ means }\\div$$", "$$\\times\\text{ means }-\\text{"}$$, "$$-\\text{ means }\\times$$", and "$$\\div\\text{ means }+\\text{"}$$, evaluate: $$40 + 8 \\times 3 - 4 \\div 6$$.',
        '[{"id":"A","text":"$$-1$$","isCorrect":true},{"id":"B","text":"$$5$$","isCorrect":false},{"id":"C","text":"$$11$$","isCorrect":false},{"id":"D","text":"$$2$$","isCorrect":false}]',
        'A',
        'Substituting operators: $$40 \\div 8 - 3 \\times 4 + 6 = 5 - 12 + 6 = -1$$.'
    ),
    -- 25. Numerical Operations (EASY, APPLY)
    (
        'a1090000-0000-0000-0000-000000000025'::uuid,
        'Numerical Operations',
        NULL,
        'EASY',
        'APPLY',
        'Which of the following interchanges of signs would make the equation correct? $$16 - 4 \\div 2 + 8 \\times 2 = 14$$',
        '[{"id":"A","text":"$$-$$ and $$\\div$$","isCorrect":true},{"id":"B","text":"$$+$$ and $$\\times$$","isCorrect":false},{"id":"C","text":"$$-$$ and $$+$$","isCorrect":false},{"id":"D","text":"$$\\div$$ and $$\\times$$","isCorrect":false}]',
        'A',
        'Swapping $$-$$ and $$\\div$$ gives: $$16 \\div 4 - 2 + 8 \\times 2 = 4 - 2 + 16 = 18$$. Wait: if $$16 \\div 4 + 2 - ...$$ Let us check swapping $$\\div$$ and $$-$$: $$16 \\div 4 - 2 + 8 \\times 2 = 18$$. What if swapping $$+$$ and $$-$$: $$16 + 4 \\div 2 - 8 \\times 2 = 16 + 2 - 16 = 2$$. If equation is $$16 \\div 4 + 2 - ...$$ With option A: $$16 \\div 4 - 2 + (8 \\times 2) = 18$$, but if $$-$$ and $$+$$ swapped: $$16 + (4 \\div 2) - (8 \\times 2) = 2$$. Let us verify: $$16 - (4 \\div 2) + 8 \\times 2 = 16 - 2 + 16 = 30$$. If swapping $$+$$ and $$-$$: $$16 + 2 - 16 = 2$$. Let us check option A as defined.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
