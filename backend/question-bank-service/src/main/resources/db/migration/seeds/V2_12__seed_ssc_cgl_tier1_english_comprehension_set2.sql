-- ============================================================
-- Seed Questions: SSC CGL Tier-1 Standard Blueprint - Section 4: English Language & Comprehension (Set 2: 25 New Qs)
-- UUID Range: a10c0000-0000-0000-0000-000000000001 to a10c0000-0000-0000-0000-000000000025
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
        'a10c0000-0000-0000-0000-000000000001'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Identify the segment with an error: "Neither the supervisor (A) / nor the field inspectors was aware (B) / of the revised safety guidelines. (C) / No error (D)"',
        '[{"id":"A","text":"Neither the supervisor","isCorrect":false},{"id":"B","text":"nor the field inspectors was aware","isCorrect":true},{"id":"C","text":"of the revised safety guidelines.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'B',
        'Rule of Proximity: When two subjects are connected by "neither...nor", the verb agrees in number with the closer subject. Since "field inspectors" is plural, the verb must be "were aware".'
    ),
    -- 2. Spot the Error (MEDIUM, ANALYZE)
    (
        'a10c0000-0000-0000-0000-000000000002'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Identify the part containing an error: "He told to me (A) / that he would definitely arrive (B) / on the midnight train. (C) / No error (D)"',
        '[{"id":"A","text":"He told to me","isCorrect":true},{"id":"B","text":"that he would definitely arrive","isCorrect":false},{"id":"C","text":"on the midnight train.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        '"Told" is a transitive verb that takes an indirect object directly without the preposition "to". Write "He told me" or "He said to me".'
    ),
    -- 3. Spot the Error (MEDIUM, ANALYZE)
    (
        'a10c0000-0000-0000-0000-000000000003'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Find the error in the sentence: "Each of the participants (A) / were presented with a certificate (B) / of outstanding achievement. (C) / No error (D)"',
        '[{"id":"A","text":"Each of the participants","isCorrect":false},{"id":"B","text":"were presented with a certificate","isCorrect":true},{"id":"C","text":"of outstanding achievement.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'B',
        'Distributive Pronoun Rule: "Each of + Plural Noun" takes a singular verb. It should be "was presented with a certificate".'
    ),
    -- 4. Fill in the Blanks (EASY, UNDERSTAND)
    (
        'a10c0000-0000-0000-0000-000000000004'::uuid,
        'Fill in the Blanks',
        'EASY',
        'UNDERSTAND',
        'Fill in the blank: "He is senior ________ me in service by three years."',
        '[{"id":"A","text":"to","isCorrect":true},{"id":"B","text":"than","isCorrect":false},{"id":"C","text":"from","isCorrect":false},{"id":"D","text":"with","isCorrect":false}]',
        'A',
        'Comparative adjectives of Latin origin ending in "-ior" (senior, junior, superior, inferior) are followed by "to", not "than".'
    ),
    -- 5. Fill in the Blanks (EASY, UNDERSTAND)
    (
        'a10c0000-0000-0000-0000-000000000005'::uuid,
        'Fill in the Blanks',
        'EASY',
        'UNDERSTAND',
        'Choose the appropriate preposition: "The passenger was prevented ________ boarding the flight due to an invalid passport."',
        '[{"id":"A","text":"from","isCorrect":true},{"id":"B","text":"to","isCorrect":false},{"id":"C","text":"against","isCorrect":false},{"id":"D","text":"with","isCorrect":false}]',
        'A',
        'The verb "prevent" is followed by the preposition "from" + gerund (e.g. prevented from boarding).'
    ),
    -- 6. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000006'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **LUCID**',
        '[{"id":"A","text":"Clear","isCorrect":true},{"id":"B","text":"Ambiguous","isCorrect":false},{"id":"C","text":"Murky","isCorrect":false},{"id":"D","text":"Confusing","isCorrect":false}]',
        'A',
        '"Lucid" means expressed clearly and easy to understand. "Clear" is its direct synonym.'
    ),
    -- 7. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000007'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **OBSTINATE**',
        '[{"id":"A","text":"Stubborn","isCorrect":true},{"id":"B","text":"Pliable","isCorrect":false},{"id":"C","text":"Docile","isCorrect":false},{"id":"D","text":"Yielding","isCorrect":false}]',
        'A',
        '"Obstinate" means stubbornly refusing to change one opinion or course of action. "Stubborn" is the correct synonym.'
    ),
    -- 8. Synonyms and Homonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000008'::uuid,
        'Synonyms and Homonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **AFFLUENT**',
        '[{"id":"A","text":"Wealthy","isCorrect":true},{"id":"B","text":"Impoverished","isCorrect":false},{"id":"C","text":"Destitute","isCorrect":false},{"id":"D","text":"Indigent","isCorrect":false}]',
        'A',
        '"Affluent" means having a great deal of money; prosperous or wealthy.'
    ),
    -- 9. Antonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000009'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **METICULOUS**',
        '[{"id":"A","text":"Careless","isCorrect":true},{"id":"B","text":"Thorough","isCorrect":false},{"id":"C","text":"Precise","isCorrect":false},{"id":"D","text":"Scrupulous","isCorrect":false}]',
        'A',
        '"Meticulous" means showing great attention to detail. Its opposite is "careless" or "sloppy".'
    ),
    -- 10. Antonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000010'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **TRANSIENT**',
        '[{"id":"A","text":"Permanent","isCorrect":true},{"id":"B","text":"Fleeting","isCorrect":false},{"id":"C","text":"Ephemeral","isCorrect":false},{"id":"D","text":"Momentary","isCorrect":false}]',
        'A',
        '"Transient" means lasting only for a short time. Its antonym is "permanent" or "enduring".'
    ),
    -- 11. Antonyms (MEDIUM, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000011'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **CORDIAL**',
        '[{"id":"A","text":"Cold","isCorrect":true},{"id":"B","text":"Warm","isCorrect":false},{"id":"C","text":"Affable","isCorrect":false},{"id":"D","text":"Genial","isCorrect":false}]',
        'A',
        '"Cordial" means warm and friendly. Its antonym is "cold" or "hostile".'
    ),
    -- 12. Spellings (EASY, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000012'::uuid,
        'Spellings',
        'EASY',
        'REMEMBER',
        'Select the incorrectly spelt word from the options given below:',
        '[{"id":"A","text":"Embarrassment","isCorrect":false},{"id":"B","text":"Occurrence","isCorrect":false},{"id":"C","text":"Mischeivous","isCorrect":true},{"id":"D","text":"Conscientious","isCorrect":false}]',
        'C',
        'The correct spelling is "Mischievous" (with "ie", not "ei").'
    ),
    -- 13. Spellings (EASY, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000013'::uuid,
        'Spellings',
        'EASY',
        'REMEMBER',
        'Select the correctly spelt word from the options given below:',
        '[{"id":"A","text":"Pneumonia","isCorrect":true},{"id":"B","text":"Pneumoniae","isCorrect":false},{"id":"C","text":"Neumonia","isCorrect":false},{"id":"D","text":"Pnemonia","isCorrect":false}]',
        'A',
        'The correct standard medical spelling is "Pneumonia".'
    ),
    -- 14. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a10c0000-0000-0000-0000-000000000014'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'Select the meaning of the underlined idiom: "The detective left no stone unturned to find the missing documents."',
        '[{"id":"A","text":"Tried every possible means to achieve something","isCorrect":true},{"id":"B","text":"Searched inside stone quarries","isCorrect":false},{"id":"C","text":"Caused destruction to property","isCorrect":false},{"id":"D","text":"Acted in haste and made errors","isCorrect":false}]',
        'A',
        '"To leave no stone unturned" means to do everything possible to achieve a good result or discover the truth.'
    ),
    -- 15. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a10c0000-0000-0000-0000-000000000015'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'Select the meaning of the idiom: **"To bite the bullet"**',
        '[{"id":"A","text":"To face a painful or difficult situation with courage","isCorrect":true},{"id":"B","text":"To be fatally wounded in combat","isCorrect":false},{"id":"C","text":"To speak harshly to subordinates","isCorrect":false},{"id":"D","text":"To waste ammunition carelessly","isCorrect":false}]',
        'A',
        '"To bite the bullet" means to endure a painful experience or face an inevitable hardship with fortitude.'
    ),
    -- 16. Idioms and Phrases (MEDIUM, UNDERSTAND)
    (
        'a10c0000-0000-0000-0000-000000000016'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'What is the meaning of the idiom: **"At the eleventh hour"**?',
        '[{"id":"A","text":"At the very last possible moment","isCorrect":true},{"id":"B","text":"Late at night","isCorrect":false},{"id":"C","text":"Far ahead of the schedule","isCorrect":false},{"id":"D","text":"At a time of sudden crisis","isCorrect":false}]',
        'A',
        '"At the eleventh hour" means happening at the latest possible time before a deadline or catastrophe.'
    ),
    -- 17. One Word Substitution (EASY, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000017'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the word which can substitute: **"A person who cannot make a mistake; infallible"**',
        '[{"id":"A","text":"Infallible","isCorrect":true},{"id":"B","text":"Invincible","isCorrect":false},{"id":"C","text":"Incorrigible","isCorrect":false},{"id":"D","text":"Indelible","isCorrect":false}]',
        'A',
        '"Infallible" means incapable of making mistakes or being wrong.'
    ),
    -- 18. One Word Substitution (EASY, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000018'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the one word for: **"An imaginary ideal society where everything is perfect"**',
        '[{"id":"A","text":"Utopia","isCorrect":true},{"id":"B","text":"Dystopia","isCorrect":false},{"id":"C","text":"Arcadia","isCorrect":false},{"id":"D","text":"Elysium","isCorrect":false}]',
        'A',
        '"Utopia" denotes an imagined place or state of things in which everything is perfect.'
    ),
    -- 19. One Word Substitution (EASY, REMEMBER)
    (
        'a10c0000-0000-0000-0000-000000000019'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the word for: **"A person who is indifferent to both pleasure and pain"**',
        '[{"id":"A","text":"Stoic","isCorrect":true},{"id":"B","text":"Epicurean","isCorrect":false},{"id":"C","text":"Cynic","isCorrect":false},{"id":"D","text":"Hedonist","isCorrect":false}]',
        'A',
        'A "Stoic" endures pain or adversity without displaying feelings and without complaint.'
    ),
    -- 20. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a10c0000-0000-0000-0000-000000000020'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined part: "Hardly had the alarm sounded **than everyone evacuated the building**."',
        '[{"id":"A","text":"when everyone evacuated the building","isCorrect":true},{"id":"B","text":"then everyone evacuated the building","isCorrect":false},{"id":"C","text":"while everyone evacuated the building","isCorrect":false},{"id":"D","text":"No improvement","isCorrect":false}]',
        'A',
        '"Hardly" and "Scarcely" are followed by "when", while "No sooner" is followed by "than".'
    ),
    -- 21. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a10c0000-0000-0000-0000-000000000021'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined part: "She told me that she **has finished the task yesterday**."',
        '[{"id":"A","text":"had finished the task yesterday","isCorrect":true},{"id":"B","text":"has been finishing the task yesterday","isCorrect":false},{"id":"C","text":"finished the task yesterday","isCorrect":false},{"id":"D","text":"No improvement required","isCorrect":false}]',
        'A',
        'Indirect Speech Sequence of Tenses: In reported speech with past reporting verb "told", past events before that time use Past Perfect "had finished".'
    ),
    -- 22. Improvement of Sentences (MEDIUM, APPLY)
    (
        'a10c0000-0000-0000-0000-000000000022'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Improve the underlined part: "The climate of Shimla is **colder than Delhi**."',
        '[{"id":"A","text":"colder than that of Delhi","isCorrect":true},{"id":"B","text":"more colder than Delhi","isCorrect":false},{"id":"C","text":"colder from Delhi","isCorrect":false},{"id":"D","text":"No improvement","isCorrect":false}]',
        'A',
        'Illogical Comparison: Comparison must be between the climate of Shimla and the climate of Delhi ("that of Delhi"), not between climate and a city.'
    ),
    -- 23. Cloze Passage (HARD, ANALYZE)
    (
        'a10c0000-0000-0000-0000-000000000023'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "Fiscal policy must strike a delicate balance between containing inflation and stimulating economic growth. Excessive monetary tightening can inadvertently ________ (1) capital investments and industrial productivity." Select the word that best fills blank (1):',
        '[{"id":"A","text":"stifle","isCorrect":true},{"id":"B","text":"bolster","isCorrect":false},{"id":"C","text":"accelerate","isCorrect":false},{"id":"D","text":"propel","isCorrect":false}]',
        'A',
        '"Stifle" means to restrain, suppress, or prevent development/growth, fitting the negative consequence of excessive tightening.'
    ),
    -- 24. Cloze Passage (HARD, ANALYZE)
    (
        'a10c0000-0000-0000-0000-000000000024'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "To sustain competitiveness in modern digital ecosystems, organizations must remain nimble and proactively ________ (2) emerging machine learning workflows." Select the word that best fills blank (2):',
        '[{"id":"A","text":"embrace","isCorrect":true},{"id":"B","text":"repudiate","isCorrect":false},{"id":"C","text":"forgo","isCorrect":false},{"id":"D","text":"dismiss","isCorrect":false}]',
        'A',
        '"Embrace" means to accept or support a new technology or methodology willingly and enthusiastically.'
    ),
    -- 25. Cloze Passage (HARD, ANALYZE)
    (
        'a10c0000-0000-0000-0000-000000000025'::uuid,
        'Cloze Passage',
        'HARD',
        'ANALYZE',
        'Passage: "Urban sustainability relies on public transit networks that are both environmentally friendly and economically ________ (3) for the lower-income demographic." Select the word that best fills blank (3):',
        '[{"id":"A","text":"viable","isCorrect":true},{"id":"B","text":"prohibitive","isCorrect":false},{"id":"C","text":"exorbitant","isCorrect":false},{"id":"D","text":"untenable","isCorrect":false}]',
        'A',
        '"Viable" means capable of working successfully, feasible, and affordable in practice.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'English Language and Comprehension' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
