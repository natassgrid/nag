-- ============================================================
-- Seed Questions: English Language and Comprehension (SSC & RRB)
-- Format Standard: Valid hex UUIDs, JSONB escaped
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
    -- 1. Spot the Error (Subject-Verb Agreement)
    (
        'a1040000-0000-0000-0000-000000000001'::uuid,
        'Spot the Error',
        'EASY',
        'APPLY',
        'In the following sentence, identify the segment that contains a grammatical error: "Neither the principal nor the teachers (A) / was present in the auditorium (B) / when the chief guest arrived (C) / No error (D)."',
        '[{"id":"A","text":"was present in the auditorium","isCorrect":true},{"id":"B","text":"Neither the principal nor the teachers","isCorrect":false},{"id":"C","text":"when the chief guest arrived","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Rule of Proximity: When two subjects are joined by "neither...nor", the verb agrees with the closer subject. Since "teachers" is plural, the plural auxiliary verb "were present" must replace "was present".'
    ),
    -- 2. Spot the Error (Conditionals / Inversion)
    (
        'a1040000-0000-0000-0000-000000000002'::uuid,
        'Spot the Error',
        'MEDIUM',
        'ANALYZE',
        'Select the segment with a grammatical error: "Scarcely had he entered the room (A) / than the telephone began to ring (B) / loudly on his desk (C) / No error (D)."',
        '[{"id":"A","text":"than the telephone began to ring","isCorrect":true},{"id":"B","text":"Scarcely had he entered the room","isCorrect":false},{"id":"C","text":"loudly on his desk","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Correlative Conjunction Rule: "Scarcely / Hardly" takes "when" or "before", not "than". ("No sooner" takes "than"). Hence, replace "than" with "when".'
    ),
    -- 3. Synonyms
    (
        'a1040000-0000-0000-0000-000000000003'::uuid,
        'Synonyms and Homonyms',
        'EASY',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **EPHEMERAL**',
        '[{"id":"A","text":"Transient","isCorrect":true},{"id":"B","text":"Permanent","isCorrect":false},{"id":"C","text":"Enduring","isCorrect":false},{"id":"D","text":"Perpetual","isCorrect":false}]',
        'A',
        '"Ephemeral" means lasting for a very short time. "Transient" is its exact synonym.'
    ),
    -- 4. Antonyms
    (
        'a1040000-0000-0000-0000-000000000004'::uuid,
        'Antonyms',
        'MEDIUM',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **METICULOUS**',
        '[{"id":"A","text":"Careless","isCorrect":true},{"id":"B","text":"Painstaking","isCorrect":false},{"id":"C","text":"Scrupulous","isCorrect":false},{"id":"D","text":"Fastidious","isCorrect":false}]',
        'A',
        '"Meticulous" means showing great attention to detail and precision. Its antonym is "careless" or "sloppy".'
    ),
    -- 5. Idioms and Phrases
    (
        'a1040000-0000-0000-0000-000000000005'::uuid,
        'Idioms and Phrases',
        'EASY',
        'UNDERSTAND',
        'Select the option that gives the most appropriate meaning of the underlined idiom: "The manager decided to **bite the bullet** and lay off redundant staff."',
        '[{"id":"A","text":"To face a painful or difficult situation with courage","isCorrect":true},{"id":"B","text":"To act rashly without thinking","isCorrect":false},{"id":"C","text":"To delay making an unpleasant decision","isCorrect":false},{"id":"D","text":"To celebrate an achievement prematurely","isCorrect":false}]',
        'A',
        '"Bite the bullet" means to endure a painful, grim, or unavoidable situation with fortitude.'
    ),
    -- 6. One Word Substitution
    (
        'a1040000-0000-0000-0000-000000000006'::uuid,
        'One Word Substitution',
        'EASY',
        'REMEMBER',
        'Select the word which means the same as the group of words given: **"One who compiles or writes dictionaries"**',
        '[{"id":"A","text":"Lexicographer","isCorrect":true},{"id":"B","text":"Cartographer","isCorrect":false},{"id":"C","text":"Calligrapher","isCorrect":false},{"id":"D","text":"Bibliophile","isCorrect":false}]',
        'A',
        'A "Lexicographer" compiles dictionaries. A "Cartographer" draws maps, a "Calligrapher" does decorative handwriting, and a "Bibliophile" loves books.'
    ),
    -- 7. Active and Passive Voice
    (
        'a1040000-0000-0000-0000-000000000007'::uuid,
        'Active and Passive Voice',
        'MEDIUM',
        'APPLY',
        'Select the correct passive form of the given sentence: "The jury is delivering the final verdict today."',
        '[{"id":"A","text":"The final verdict is being delivered by the jury today.","isCorrect":true},{"id":"B","text":"The final verdict was delivered by the jury today.","isCorrect":false},{"id":"C","text":"The final verdict has been delivered by the jury today.","isCorrect":false},{"id":"D","text":"The final verdict is delivered by the jury today.","isCorrect":false}]',
        'A',
        'Present continuous passive voice structure: Subject + is/am/are + being + V3 + by + Object.'
    ),
    -- 8. Direct and Indirect Narration
    (
        'a1040000-0000-0000-0000-000000000008'::uuid,
        'Direct and Indirect Narration',
        'MEDIUM',
        'APPLY',
        'Convert the sentence into indirect speech: She said to him, "Why did you not attend the conference yesterday?"',
        '[{"id":"A","text":"She asked him why he had not attended the conference the previous day.","isCorrect":true},{"id":"B","text":"She asked him why did he not attend the conference yesterday.","isCorrect":false},{"id":"C","text":"She enquired him that why he had not attended the conference the previous day.","isCorrect":false},{"id":"D","text":"She asked him why he did not attend the conference the day before.","isCorrect":false}]',
        'A',
        'Rules: "said to" becomes "asked", Wh-question keeps assertive word order (why + subject + verb), simple past ("did not attend") shifts to past perfect ("had not attended"), and "yesterday" becomes "the previous day".'
    ),
    -- 9. Sentence Improvement
    (
        'a1040000-0000-0000-0000-000000000009'::uuid,
        'Improvement of Sentences',
        'MEDIUM',
        'APPLY',
        'Select the option that will improve the underlined segment: "He had hardly finished his homework **than the lights went out**."',
        '[{"id":"A","text":"when the lights went out","isCorrect":true},{"id":"B","text":"then the lights went out","isCorrect":false},{"id":"C","text":"while the lights went out","isCorrect":false},{"id":"D","text":"No improvement required","isCorrect":false}]',
        'A',
        '"Hardly" is paired with "when", not "than". Therefore, "when the lights went out" is the grammatically correct conjunction.'
    ),
    -- 10. Fill in the Blanks (Prepositions)
    (
        'a1040000-0000-0000-0000-000000000010'::uuid,
        'Fill in the Blanks',
        'EASY',
        'APPLY',
        'Fill in the blank with the most appropriate preposition: "The committee agreed that the accused was totally exempt ________ paying the penalty."',
        '[{"id":"A","text":"from","isCorrect":true},{"id":"B","text":"for","isCorrect":false},{"id":"C","text":"of","isCorrect":false},{"id":"D","text":"with","isCorrect":false}]',
        'A',
        'The adjective "exempt" takes the fixed preposition "from" (e.g., exempt from liability/tax/penalty).'
    ),
    -- 11. Spellings
    (
        'a1040000-0000-0000-0000-000000000011'::uuid,
        'Spellings',
        'EASY',
        'REMEMBER',
        'Select the correctly spelt word from the given options:',
        '[{"id":"A","text":"Conscientious","isCorrect":true},{"id":"B","text":"Conscensious","isCorrect":false},{"id":"C","text":"Consciencious","isCorrect":false},{"id":"D","text":"Conscentious","isCorrect":false}]',
        'A',
        'The correct spelling is "Conscientious", meaning wishing to do what is right, especially to do one work or duty thoroughly.'
    ),
    -- 12. One Word Substitution
    (
        'a1040000-0000-0000-0000-000000000012'::uuid,
        'One Word Substitution',
        'MEDIUM',
        'REMEMBER',
        'Select the one word that best substitutes the given phrase: **"A state of disorder due to absence or non-recognition of authority or other controlling systems"**',
        '[{"id":"A","text":"Anarchy","isCorrect":true},{"id":"B","text":"Monarchy","isCorrect":false},{"id":"C","text":"Oligarchy","isCorrect":false},{"id":"D","text":"Plutocracy","isCorrect":false}]',
        'A',
        '"Anarchy" denotes absence of governance. "Monarchy" is rule by a king, "Oligarchy" by a small group, and "Plutocracy" by the wealthy.'
    ),
    -- 13. Idioms and Phrases
    (
        'a1040000-0000-0000-0000-000000000013'::uuid,
        'Idioms and Phrases',
        'MEDIUM',
        'UNDERSTAND',
        'What is the meaning of the idiom: **"To leave no stone unturned"**?',
        '[{"id":"A","text":"To try every possible course of action in order to achieve something","isCorrect":true},{"id":"B","text":"To excavate an ancient archaeological site","isCorrect":false},{"id":"C","text":"To create unnecessary hurdles for competitors","isCorrect":false},{"id":"D","text":"To abandon a project midway","isCorrect":false}]',
        'A',
        '"To leave no stone unturned" means to do everything possible and make all possible efforts to reach a goal.'
    ),
    -- 14. Cloze Passage / Word Choice
    (
        'a1040000-0000-0000-0000-000000000014'::uuid,
        'Cloze Passage',
        'MEDIUM',
        'ANALYZE',
        'Select the most appropriate word to fill in the blank: "The scientist delivered a ________ argument that left no room for ambiguity."',
        '[{"id":"A","text":"cogent","isCorrect":true},{"id":"B","text":"flimsy","isCorrect":false},{"id":"C","text":"vague","isCorrect":false},{"id":"D","text":"specious","isCorrect":false}]',
        'A',
        '"Cogent" means clear, logical, and convincing. Flimsy and vague mean weak, while specious means superficially plausible but wrong.'
    ),
    -- 15. Synonyms
    (
        'a1040000-0000-0000-0000-000000000015'::uuid,
        'Synonyms and Homonyms',
        'HARD',
        'REMEMBER',
        'Select the most appropriate **SYNONYM** of the given word: **UBIQUITOUS**',
        '[{"id":"A","text":"Omnipresent","isCorrect":true},{"id":"B","text":"Scarce","isCorrect":false},{"id":"C","text":"Sporadic","isCorrect":false},{"id":"D","text":"Obscure","isCorrect":false}]',
        'A',
        '"Ubiquitous" means present, appearing, or found everywhere. Its synonym is "omnipresent".'
    ),
    -- 16. Antonyms
    (
        'a1040000-0000-0000-0000-000000000016'::uuid,
        'Antonyms',
        'HARD',
        'REMEMBER',
        'Select the most appropriate **ANTONYM** of the given word: **TACITURN**',
        '[{"id":"A","text":"Loquacious","isCorrect":true},{"id":"B","text":"Reticent","isCorrect":false},{"id":"C","text":"Reserved","isCorrect":false},{"id":"D","text":"Laconic","isCorrect":false}]',
        'A',
        '"Taciturn" means reserved or uncommunicative in speech. Its antonym is "loquacious" (talkative, garrulous).'
    ),
    -- 17. Spot the Error (Tenses)
    (
        'a1040000-0000-0000-0000-000000000017'::uuid,
        'Spot the Error',
        'MEDIUM',
        'APPLY',
        'Find the erroneous part: "By this time next year, (A) / he will complete (B) / his postgraduate degree in cybersecurity. (C) / No error (D)"',
        '[{"id":"A","text":"he will complete","isCorrect":true},{"id":"B","text":"By this time next year,","isCorrect":false},{"id":"C","text":"his postgraduate degree in cybersecurity.","isCorrect":false},{"id":"D","text":"No error","isCorrect":false}]',
        'A',
        'Future Perfect Rule: An action that will be completed before a certain future point ("By this time next year") requires future perfect: "will have completed".'
    ),
    -- 18. Improvement of Sentences
    (
        'a1040000-0000-0000-0000-000000000018'::uuid,
        'Improvement of Sentences',
        'EASY',
        'APPLY',
        'Select the option that will improve the underlined segment: "The climate of Shimla is **more colder than Delhi**."',
        '[{"id":"A","text":"colder than that of Delhi","isCorrect":true},{"id":"B","text":"more colder than Delhi","isCorrect":false},{"id":"C","text":"colder than Delhi","isCorrect":false},{"id":"D","text":"more cold than Delhi","isCorrect":false}]',
        'A',
        'Rule of Logical Comparison: We must compare the climate of Shimla with the climate of Delhi ("that of Delhi"), not Delhi itself. Also, double comparatives ("more colder") are incorrect.'
    ),
    -- 19. Idioms and Phrases
    (
        'a1040000-0000-0000-0000-000000000019'::uuid,
        'Idioms and Phrases',
        'EASY',
        'UNDERSTAND',
        'Select the correct meaning of the idiom: **"At eleventh hour"**',
        '[{"id":"A","text":"At the last possible moment","isCorrect":true},{"id":"B","text":"At 11:00 AM sharp","isCorrect":false},{"id":"C","text":"During late night hours","isCorrect":false},{"id":"D","text":"Well in advance","isCorrect":false}]',
        'A',
        '"At the eleventh hour" means at the last possible moment or almost too late.'
    ),
    -- 20. Fill in the Blanks
    (
        'a1040000-0000-0000-0000-000000000020'::uuid,
        'Fill in the Blanks',
        'MEDIUM',
        'APPLY',
        'Fill in the blank with the appropriate modal verb: "The doctor advised that he ________ refrain from smoking if he wants to recover quickly."',
        '[{"id":"A","text":"should","isCorrect":true},{"id":"B","text":"might","isCorrect":false},{"id":"C","text":"could","isCorrect":false},{"id":"D","text":"would","isCorrect":false}]',
        'A',
        '"Should" conveys strong advisory recommendation following a physician advice.'
    )
) AS v(id, topic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'English Language and Comprehension' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
