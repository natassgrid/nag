-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 4: English Language & Comprehension (25 New Qs)
-- UUID Range: a1080000-0000-0000-0000-000000000001 to a1080000-0000-0000-0000-000000000025
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
    NULL,
    'English Language and Comprehension',
    v.topic_name,
    NULL,
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
    -- 1. Spot the Error (MEDIUM, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000001'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'In the following sentence, find the part containing an error: "One of the most essential factor (A) / for succeeding in competitive exams (B) / is consistent daily practice. (C) / No error (D)"',
        '[{"id":"A","text":"One of the most essential factor","isCorrect":true},{"id":"B","text":"for succeeding in competitive exams","isCorrect":false},{"id":"C","text":"is consistent daily practice.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Rule: "One of the + Plural Noun + Singular Verb". Hence "essential factor" must be pluralized to "essential factors".'
    ),
    -- 2. Spot the Error (MEDIUM, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000002'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Identify the segment with an error: "Supposing if it rains heavily (A) / will the outdoor tournament (B) / be postponed until tomorrow? (C) / No error (D)"',
        '[{"id":"A","text":"Supposing if it rains heavily","isCorrect":true},{"id":"B","text":"will the outdoor tournament","isCorrect":false},{"id":"C","text":"be postponed until tomorrow?","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Superfluous Expression: "Supposing" and "if" carry identical conditional meanings and cannot be used together. Use either "Supposing it rains" or "If it rains".'
    ),
    -- 3. Spot the Error (MEDIUM, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000003'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Identify the segment with an error: "The senior diplomat discussed about (A) / the bilateral border protocols (B) / with foreign delegates. (C) / No error (D)"',
        '[{"id":"A","text":"The senior diplomat discussed about","isCorrect":true},{"id":"B","text":"the bilateral border protocols","isCorrect":false},{"id":"C","text":"with foreign delegates.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Transitive Verb Rule: "Discuss", "describe", and "order" take direct objects without prepositions like "about".'
    ),
    -- 4. Fill in the Blanks (EASY, UNDERSTAND)
    (
        'a1080000-0000-0000-0000-000000000004'::uuid,
        'Fill in the Blanks',
        'EASY',
        'UNDERSTAND',
        'Fill in the blank with the appropriate word: "The minister was accompanied ________ his private secretary during the press conference."',
        '[{"id":"A","text":"by","isCorrect":true},{"id":"B","text":"with","isCorrect":false},{"id":"C","text":"from","isCorrect":false},{"id":"D","text":"along","isCorrect":false}]',
        'A',
        'When a person accompanies another person in passive constructions, the standard preposition is "accompanied by".'
    ),
    -- 5. Fill in the Blanks (EASY, UNDERSTAND)
    (
        'a1080000-0000-0000-0000-000000000005'::uuid,
        'Fill in the Blanks',
        'EASY',
        'UNDERSTAND',
        'Fill in the blank: "She has been working as a research analyst in this institute ________ 2018."',
        '[{"id":"A","text":"since","isCorrect":true},{"id":"B","text":"for","isCorrect":false},{"id":"C","text":"from","isCorrect":false},{"id":"D","text":"during","isCorrect":false}]',
        'A',
        '"Since" denotes a specific starting point in time in perfect continuous tenses (e.g., since 2018, since morning).'
    ),
    -- 6. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000006'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **CANDID**',
        '[{"id":"A","text":"Frank","isCorrect":true},{"id":"B","text":"Deceptive","isCorrect":false},{"id":"C","text":"Secretive","isCorrect":false},{"id":"D","text":"Guarded","isCorrect":false}]',
        'A',
        '"Candid" means truthful, straightforward, and outspoken. "Frank" is its exact synonym.'
    ),
    -- 7. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000007'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **ZEALOUS**',
        '[{"id":"A","text":"Enthusiastic","isCorrect":true},{"id":"B","text":"Apathetic","isCorrect":false},{"id":"C","text":"Indifferent","isCorrect":false},{"id":"D","text":"Reluctant","isCorrect":false}]',
        'A',
        '"Zealous" means having or showing great energy or enthusiasm in pursuit of a cause or objective.'
    ),
    -- 8. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000008'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **PRAGMATIC**',
        '[{"id":"A","text":"Practical","isCorrect":true},{"id":"B","text":"Idealistic","isCorrect":false},{"id":"C","text":"Theoretical","isCorrect":false},{"id":"D","text":"Visionary","isCorrect":false}]',
        'A',
        '"Pragmatic" means dealing with things sensibly and realistically based on practical rather than theoretical considerations.'
    ),
    -- 9. Antonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000009'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **GARRULOUS**',
        '[{"id":"A","text":"Taciturn","isCorrect":true},{"id":"B","text":"Talkative","isCorrect":false},{"id":"C","text":"Loquacious","isCorrect":false},{"id":"D","text":"Voluble","isCorrect":false}]',
        'A',
        '"Garrulous" means excessively talkative. Its antonym is "taciturn" or "reticent" (quiet, untalkative).'
    ),
    -- 10. Antonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000010'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **AMICABLE**',
        '[{"id":"A","text":"Hostile","isCorrect":true},{"id":"B","text":"Cordial","isCorrect":false},{"id":"C","text":"Harmonious","isCorrect":false},{"id":"D","text":"Polite","isCorrect":false}]',
        'A',
        '"Amicable" means friendly and agreeable. Its direct antonym is "hostile" or "antagonistic".'
    ),
    -- 11. Antonyms (MEDIUM, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000011'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **BENEVOLENT**',
        '[{"id":"A","text":"Malevolent","isCorrect":true},{"id":"B","text":"Generous","isCorrect":false},{"id":"C","text":"Kindhearted","isCorrect":false},{"id":"D","text":"Altruistic","isCorrect":false}]',
        'A',
        '"Benevolent" means well-meaning and kindly. Its antonym is "malevolent" (wishing to do evil to others).'
    ),
    -- 12. Spellings (EASY, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000012'::uuid,
        'Spellings',
        'EASY',
        'REMEMBER',
        'Select the incorrectly spelt word from the options given below:',
        '[{"id":"A","text":"Bureaucracy","isCorrect":false},{"id":"B","text":"Millennium","isCorrect":false},{"id":"C","text":"Accomodation","isCorrect":true},{"id":"D","text":"Privilege","isCorrect":false}]',
        'C',
        'The correct spelling is "Accommodation" with double "c" and double "m".'
    ),
    -- 13. Spellings (EASY, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000013'::uuid,
        'Spellings',
        'EASY',
        'REMEMBER',
        'Select the correctly spelt word from the options given below:',
        '[{"id":"A","text":"Superintendent","isCorrect":true},{"id":"B","text":"Superintendant","isCorrect":false},{"id":"C","text":"Superentendent","isCorrect":false},{"id":"D","text":"Superentendant","isCorrect":false}]',
        'A',
        'The standard correct spelling is "Superintendent".'
    ),
    -- 14. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a1080000-0000-0000-0000-000000000014'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'Select the meaning of the underlined idiom: "The defense lawyer tried to **pull wool over the jury eyes** with false evidence."',
        '[{"id":"A","text":"To deceive or mislead someone","isCorrect":true},{"id":"B","text":"To present overwhelming evidence","isCorrect":false},{"id":"C","text":"To appeal for sympathy","isCorrect":false},{"id":"D","text":"To keep the jury warm and comfortable","isCorrect":false}]',
        'A',
        '"To pull wool over someone eyes" means to deceive or trick them by preventing them from discovering the truth.'
    ),
    -- 15. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a1080000-0000-0000-0000-000000000015'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'What is the meaning of the idiom: **"A blessing in disguise"**?',
        '[{"id":"A","text":"An apparent misfortune that eventually results in something good","isCorrect":true},{"id":"B","text":"A hidden supernatural omen","isCorrect":false},{"id":"C","text":"A praise offered by a stranger","isCorrect":false},{"id":"D","text":"A reward given without merit","isCorrect":false}]',
        'A',
        '"A blessing in disguise" is something that appears unfortunate or troublesome at first, but ultimately produces beneficial results.'
    ),
    -- 16. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a1080000-0000-0000-0000-000000000016'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'Select the meaning of the idiom: **"To burn the candle at both ends"**',
        '[{"id":"A","text":"To work excessively hard from early morning until late at night","isCorrect":true},{"id":"B","text":"To waste fuel recklessly","isCorrect":false},{"id":"C","text":"To be financially frugal","isCorrect":false},{"id":"D","text":"To lose one temper easily","isCorrect":false}]',
        'A',
        '"To burn the candle at both ends" means to exhaust oneself by doing too much, especially going to bed late and getting up early.'
    ),
    -- 17. One Word Substitution (EASY, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000017'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the word that best substitutes: **"A person who loves, collects, or is fond of books"**',
        '[{"id":"A","text":"Bibliophile","isCorrect":true},{"id":"B","text":"Philatelist","isCorrect":false},{"id":"C","text":"Numismatist","isCorrect":false},{"id":"D","text":"Somnambulist","isCorrect":false}]',
        'A',
        'A "Bibliophile" is a lover of books. "Philatelist" collects stamps, "Numismatist" collects coins, "Somnambulist" walks in sleep.'
    ),
    -- 18. One Word Substitution (EASY, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000018'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the one word for: **"An incurable fear of confined or enclosed spaces"**',
        '[{"id":"A","text":"Claustrophobia","isCorrect":true},{"id":"B","text":"Acrophobia","isCorrect":false},{"id":"C","text":"Agoraphobia","isCorrect":false},{"id":"D","text":"Hydrophobia","isCorrect":false}]',
        'A',
        '"Claustrophobia" is fear of closed spaces. "Acrophobia" is fear of heights, "Agoraphobia" is fear of open/crowded places.'
    ),
    -- 19. One Word Substitution (EASY, REMEMBER)
    (
        'a1080000-0000-0000-0000-000000000019'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the one word for: **"A government ruled by the wealthy class"**',
        '[{"id":"A","text":"Plutocracy","isCorrect":true},{"id":"B","text":"Autocracy","isCorrect":false},{"id":"C","text":"Aristocracy","isCorrect":false},{"id":"D","text":"Theocracy","isCorrect":false}]',
        'A',
        '"Plutocracy" is governance by the rich. "Aristocracy" is rule by noble nobility, "Theocracy" by religious authorities.'
    ),
    -- 20. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a1080000-0000-0000-0000-000000000020'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined segment: "No sooner had the teacher entered the classroom **when all students stood up**."',
        '[{"id":"A","text":"than all students stood up","isCorrect":true},{"id":"B","text":"then all students stood up","isCorrect":false},{"id":"C","text":"while all students stood up","isCorrect":false},{"id":"D","text":"No improvement required","isCorrect":false}]',
        'A',
        'Correlative Rule: "No sooner" is followed by "than", whereas "Hardly/Scarcely" is followed by "when".'
    ),
    -- 21. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a1080000-0000-0000-0000-000000000021'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined segment: "If I **was having** wings, I would fly across the oceans."',
        '[{"id":"A","text":"had","isCorrect":true},{"id":"B","text":"am having","isCorrect":false},{"id":"C","text":"have had","isCorrect":false},{"id":"D","text":"No improvement","isCorrect":false}]',
        'A',
        'Second Conditional (Hypothetical/Unreal Present): "If + Subject + Simple Past (had/were), Subject + would + V1". Also, "have" expressing possession is a stative verb not used in continuous aspect.'
    ),
    -- 22. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a1080000-0000-0000-0000-000000000022'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined segment: "The new residential tower is **superior than the older apartment complex**."',
        '[{"id":"A","text":"superior to the older apartment complex","isCorrect":true},{"id":"B","text":"more superior than the older apartment complex","isCorrect":false},{"id":"C","text":"superior from the older apartment complex","isCorrect":false},{"id":"D","text":"No improvement required","isCorrect":false}]',
        'A',
        'Latin Adjectives Rule: Adjectives ending in "-ior" (superior, inferior, senior, junior, prior) take the preposition "to", never "than".'
    ),
    -- 23. Cloze Passage (HARD, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000023'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "Renewable energy adoption has accelerated globally due to technological breakthroughs and climate mandates. However, integrating intermittent sources like wind and solar into national power grids requires massive investments in energy storage systems to ensure grid ________ (1)." Select the most appropriate option for blank (1):',
        '[{"id":"A","text":"stability","isCorrect":true},{"id":"B","text":"fragility","isCorrect":false},{"id":"C","text":"redundancy","isCorrect":false},{"id":"D","text":"stagnation","isCorrect":false}]',
        'A',
        'Energy storage buffers intermittent generation to maintain network balance and electrical "stability".'
    ),
    -- 24. Cloze Passage (HARD, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000024'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "To mitigate supply chain bottlenecks, governments are offering fiscal subsidies to ________ (2) domestic semiconductor fabrication facilities." Select the most appropriate option for blank (2):',
        '[{"id":"A","text":"incentivize","isCorrect":true},{"id":"B","text":"discourage","isCorrect":false},{"id":"C","text":"penalize","isCorrect":false},{"id":"D","text":"dissuade","isCorrect":false}]',
        'A',
        '"Incentivize" means to provide motivation or financial stimulus to encourage an action (e.g. setting up fabrication plants).'
    ),
    -- 25. Cloze Passage (HARD, ANALYZE)
    (
        'a1080000-0000-0000-0000-000000000025'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "A sound regulatory architecture is indispensable to ensure that artificial intelligence innovations do not ________ (3) upon individual privacy and democratic norms." Select the most appropriate option for blank (3):',
        '[{"id":"A","text":"infringe","isCorrect":true},{"id":"B","text":"enhance","isCorrect":false},{"id":"C","text":"reinforce","isCorrect":false},{"id":"D","text":"safeguard","isCorrect":false}]',
        'A',
        'The verb "infringe" (often followed by "on" or "upon") means to act so as to limit, encroach upon, or violate someone rights/privacy.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'English Language and Comprehension' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
