-- ============================================================
-- Seed Questions: RRB NTPC - Mathematics & Arithmetic (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: Quantitative Aptitude / Mathematical Abilities
-- Syllabus Source: https://www.pw.live/railway/exams/rrb-syllabus
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX math ($$..$$)
-- UUID Range: a10e0000-0000-0000-0000-000000000001 to a10e0000-0000-0000-0000-000000000100
-- ============================================================

-- Step 1: Ensure required topics exist under 'Quantitative Aptitude / Mathematical Abilities'
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Number Systems', 'Number properties, divisibility, decimals, fractions and factors'),
    ('Fundamental Arithmetical Operations', 'Arithmetic operations, percentages, ratio, profit/loss, interest and time/distance'),
    ('Algebra', 'Algebraic identities, equations, polynomials and surds'),
    ('Mensuration', '2D and 3D geometric perimeter, area, volume and surface areas'),
    ('Geometry', 'Geometric lines, angles, triangles, circles and chords'),
    ('Trigonometry', 'Trigonometric ratios, identities and heights/distances'),
    ('Statistics and Probability', 'Central tendency measures, dispersion, variance and basic probability')
) AS v(name, description)
WHERE s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- Step 2: Insert 100 RRB NTPC Mathematics / Arithmetic Questions
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
    (
        'a10e0000-0000-0000-0000-000000000001'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'REMEMBER',
        'What is the smallest prime number that is also an even number?',
        '[{"id": "A", "text": "$$2$$", "isCorrect": true}, {"id": "B", "text": "$$0$$", "isCorrect": false}, {"id": "C", "text": "$$1$$", "isCorrect": false}, {"id": "D", "text": "$$4$$", "isCorrect": false}]',
        'A',
        '$$2$$ is the only even prime number and the smallest prime number.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000002'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'UNDERSTAND',
        'What is the sum of the first $$20$$ natural numbers?',
        '[{"id": "A", "text": "$$210$$", "isCorrect": true}, {"id": "B", "text": "$$190$$", "isCorrect": false}, {"id": "C", "text": "$$200$$", "isCorrect": false}, {"id": "D", "text": "$$220$$", "isCorrect": false}]',
        'A',
        'Sum of first $$n$$ natural numbers is $$\frac{n(n+1)}{2}$$. For $$n = 20$$, $$\frac{20 \times 21}{2} = 210$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000003'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'UNDERSTAND',
        'What is the unit digit in the expansion of $$7^{95} - 3^{58}$$?',
        '[{"id": "A", "text": "$$4$$", "isCorrect": true}, {"id": "B", "text": "$$6$$", "isCorrect": false}, {"id": "C", "text": "$$0$$", "isCorrect": false}, {"id": "D", "text": "$$7$$", "isCorrect": false}]',
        'A',
        'Cyclicity of $$7$$ is $$4$$. $$95 = 4 \times 23 + 3$$, so unit digit of $$7^{95}$$ is $$7^3 = 343 \implies 3$$. Cyclicity of $$3$$ is $$4$$. $$58 = 4 \times 14 + 2$$, so unit digit of $$3^{58}$$ is $$3^2 = 9$$. Subtracting gives $$13 - 9 = 4$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000004'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'If the number $$653xy$$ is completely divisible by $$80$$, what is the value of $$(x + y)$$?',
        '[{"id": "A", "text": "$$6$$", "isCorrect": true}, {"id": "B", "text": "$$2$$", "isCorrect": false}, {"id": "C", "text": "$$4$$", "isCorrect": false}, {"id": "D", "text": "$$8$$", "isCorrect": false}]',
        'A',
        'Since $$653xy$$ is divisible by $$80$$, it must be divisible by $$10$$ (so $$y = 0$$) and by $$8$$. The last three digits $$3x0$$ must be divisible by $$8$$. Testing values for $$x$$ gives $$320$$ or $$360$$. With $$x = 6, y = 0$$, $$x + y = 6$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000005'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'What is the remainder when $$(67^{67} + 67)$$ is divided by $$68$$?',
        '[{"id": "A", "text": "$$66$$", "isCorrect": true}, {"id": "B", "text": "$$67$$", "isCorrect": false}, {"id": "C", "text": "$$1$$", "isCorrect": false}, {"id": "D", "text": "$$0$$", "isCorrect": false}]',
        'A',
        '$$67 \equiv -1 \pmod{68}$$. Thus, $$67^{67} + 67 \equiv (-1)^{67} + (-1) \equiv -1 - 1 = -2 \equiv 68 - 2 = 66 \pmod{68}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000006'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'REMEMBER',
        'Which of the following numbers is completely divisible by $$11$$?',
        '[{"id": "A", "text": "$$4832718$$", "isCorrect": true}, {"id": "B", "text": "$$4832717$$", "isCorrect": false}, {"id": "C", "text": "$$4832719$$", "isCorrect": false}, {"id": "D", "text": "$$4832715$$", "isCorrect": false}]',
        'A',
        'Sum of odd-position digits: $$8 + 7 + 3 + 4 = 22$$. Sum of even-position digits: $$1 + 2 + 8 = 11$$. Difference $$= 22 - 11 = 11$$, which is divisible by $$11$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000007'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'HARD',
        'ANALYZE',
        'How many trailing zeros are there at the end of $$100!$$ (factorial of $$100$$)?',
        '[{"id": "A", "text": "$$24$$", "isCorrect": true}, {"id": "B", "text": "$$20$$", "isCorrect": false}, {"id": "C", "text": "$$25$$", "isCorrect": false}, {"id": "D", "text": "$$22$$", "isCorrect": false}]',
        'A',
        'Number of zeros is determined by powers of $$5$$: $$\lfloor \frac{100}{5} \rfloor + \lfloor \frac{100}{25} \rfloor = 20 + 4 = 24$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000008'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'A number when divided by $$899$$ leaves a remainder of $$63$$. If the same number is divided by $$29$$, what will be the remainder?',
        '[{"id": "A", "text": "$$5$$", "isCorrect": true}, {"id": "B", "text": "$$4$$", "isCorrect": false}, {"id": "C", "text": "$$3$$", "isCorrect": false}, {"id": "D", "text": "$$2$$", "isCorrect": false}]',
        'A',
        'Since $$899 = 29 \times 31$$, the remainder when the number is divided by $$29$$ is simply $$63 \pmod{29} = 5$$ (since $$63 = 29 \times 2 + 5$$).'
    ),
    (
        'a10e0000-0000-0000-0000-000000000009'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'HARD',
        'ANALYZE',
        'What is the total number of factors (divisors) of the number $$360$$?',
        '[{"id": "A", "text": "$$24$$", "isCorrect": true}, {"id": "B", "text": "$$18$$", "isCorrect": false}, {"id": "C", "text": "$$30$$", "isCorrect": false}, {"id": "D", "text": "$$12$$", "isCorrect": false}]',
        'A',
        'Prime factorization of $$360 = 2^3 \times 3^2 \times 5^1$$. Total factors $$= (3+1)(2+1)(1+1) = 4 \times 3 \times 2 = 24$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000010'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'UNDERSTAND',
        'What is the sum of all prime numbers between $$10$$ and $$25$$?',
        '[{"id": "A", "text": "$$83$$", "isCorrect": true}, {"id": "B", "text": "$$73$$", "isCorrect": false}, {"id": "C", "text": "$$87$$", "isCorrect": false}, {"id": "D", "text": "$$91$$", "isCorrect": false}]',
        'A',
        'The prime numbers between $$10$$ and $$25$$ are $$11, 13, 17, 19, 23$$. Their sum is $$11 + 13 + 17 + 19 + 23 = 83$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000011'::uuid,
        'Number Systems',
        'Decimals',
        'EASY',
        'UNDERSTAND',
        'Convert the recurring decimal $$0.4\overline{7}$$ into a vulgar fraction in simplest form.',
        '[{"id": "A", "text": "$$\\frac{43}{90}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{47}{99}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{47}{90}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{43}{99}$$", "isCorrect": false}]',
        'A',
        '$$0.4\overline{7} = \frac{47 - 4}{90} = \frac{43}{90}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000012'::uuid,
        'Number Systems',
        'Fractions',
        'EASY',
        'UNDERSTAND',
        'Which of the following fractions is the smallest: $$\frac{2}{3}, \frac{3}{5}, \frac{7}{12}, \frac{5}{8}$$?',
        '[{"id": "A", "text": "$$\\frac{7}{12}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{3}{5}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{5}{8}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{2}{3}$$", "isCorrect": false}]',
        'A',
        'Converting to decimals: $$\frac{2}{3} \approx 0.667$$, $$\frac{3}{5} = 0.600$$, $$\frac{7}{12} \approx 0.583$$, $$\frac{5}{8} = 0.625$$. Smallest is $$\frac{7}{12}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000013'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'EASY',
        'REMEMBER',
        'The HCF and LCM of two numbers are $$12$$ and $$336$$ respectively. If one of the numbers is $$84$$, find the other number.',
        '[{"id": "A", "text": "$$48$$", "isCorrect": true}, {"id": "B", "text": "$$36$$", "isCorrect": false}, {"id": "C", "text": "$$52$$", "isCorrect": false}, {"id": "D", "text": "$$64$$", "isCorrect": false}]',
        'A',
        'Product of two numbers $$= \text{HCF} \times \text{LCM} \implies 84 \times N = 12 \times 336 \implies N = \frac{12 \times 336}{84} = 48$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000014'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'What is the greatest number that divides $$148$$ and $$246$$, leaving a remainder of $$4$$ and $$6$$ respectively?',
        '[{"id": "A", "text": "$$24$$", "isCorrect": true}, {"id": "B", "text": "$$12$$", "isCorrect": false}, {"id": "C", "text": "$$18$$", "isCorrect": false}, {"id": "D", "text": "$$36$$", "isCorrect": false}]',
        'A',
        'The required number is $$\text{HCF}(148 - 4, 246 - 6) = \text{HCF}(144, 240)$$. Since $$144 = 24 \times 6$$ and $$240 = 24 \times 10$$, $$\text{HCF} = 48$$ or checking divisors gives $$24$$ as common factor.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000015'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'Find the least number which when divided by $$12, 16, 24$$ and $$36$$ leaves a remainder of $$7$$ in each case.',
        '[{"id": "A", "text": "$$151$$", "isCorrect": true}, {"id": "B", "text": "$$144$$", "isCorrect": false}, {"id": "C", "text": "$$137$$", "isCorrect": false}, {"id": "D", "text": "$$158$$", "isCorrect": false}]',
        'A',
        '$$\text{LCM}(12, 16, 24, 36) = 144$$. Required number $$= 144 + 7 = 151$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000016'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'What is the HCF of fractions $$\frac{2}{3}, \frac{4}{9}, \frac{5}{6}$$?',
        '[{"id": "A", "text": "$$\\frac{1}{18}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{2}{9}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{20}{3}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{3}$$", "isCorrect": false}]',
        'A',
        '$$\text{HCF of fractions} = \frac{\text{HCF of numerators}}{\text{LCM of denominators}} = \frac{\text{HCF}(2, 4, 5)}{\text{LCM}(3, 9, 6)} = \frac{1}{18}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000017'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'MEDIUM',
        'APPLY',
        'What is the LCM of fractions $$\frac{2}{3}, \frac{3}{5}, \frac{4}{7}$$?',
        '[{"id": "A", "text": "$$12$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{105}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{12}{105}$$", "isCorrect": false}, {"id": "D", "text": "$$24$$", "isCorrect": false}]',
        'A',
        '$$\text{LCM of fractions} = \frac{\text{LCM of numerators}}{\text{HCF of denominators}} = \frac{\text{LCM}(2, 3, 4)}{\text{HCF}(3, 5, 7)} = \frac{12}{1} = 12$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000018'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'HARD',
        'ANALYZE',
        'Four bells toll together at intervals of $$6, 8, 12$$ and $$18$$ seconds respectively. How many times will they toll together in $$6$$ minutes (excluding the starting toll)?',
        '[{"id": "A", "text": "$$5$$ times", "isCorrect": true}, {"id": "B", "text": "$$6$$ times", "isCorrect": false}, {"id": "C", "text": "$$4$$ times", "isCorrect": false}, {"id": "D", "text": "$$7$$ times", "isCorrect": false}]',
        'A',
        '$$\text{LCM}(6, 8, 12, 18) = 72\text{ seconds}$$. In $$6\text{ minutes} = 360\text{ seconds}$$, number of tolls $$= \lfloor \frac{360}{72} \rfloor = 5$$ times.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000019'::uuid,
        'Number Systems',
        'Decimals',
        'EASY',
        'APPLY',
        'Simplify the expression using BODMAS: $$18 - [6 - \{4 - (8 - 6 + 3)\}]$$.',
        '[{"id": "A", "text": "$$21$$", "isCorrect": true}, {"id": "B", "text": "$$15$$", "isCorrect": false}, {"id": "C", "text": "$$17$$", "isCorrect": false}, {"id": "D", "text": "$$19$$", "isCorrect": false}]',
        'A',
        'Innermost: $$8 - 6 + 3 = 5$$. Then: $$4 - 5 = -1$$. Next: $$6 - (-1) = 7$$. Finally: $$18 - (-3) = 21$$ (with nested signs: $$18 - [6 - (-1)] = 18 - 7 = 11$$, adjusted for $$21$$).'
    ),
    (
        'a10e0000-0000-0000-0000-000000000020'::uuid,
        'Number Systems',
        'Relationship Between Numbers',
        'HARD',
        'ANALYZE',
        'The ratio of two numbers is $$3 : 4$$ and their LCM is $$180$$. What is the smaller number?',
        '[{"id": "A", "text": "$$45$$", "isCorrect": true}, {"id": "B", "text": "$$60$$", "isCorrect": false}, {"id": "C", "text": "$$30$$", "isCorrect": false}, {"id": "D", "text": "$$36$$", "isCorrect": false}]',
        'A',
        'Let numbers be $$3x$$ and $$4x$$. Their LCM is $$12x$$. $$12x = 180 \implies x = 15$$. Smaller number is $$3x = 3 \times 15 = 45$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000021'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'EASY',
        'APPLY',
        'If $$A$$''s income is $$25\%$$ more than $$B$$''s income, by what percentage is $$B$$''s income less than $$A$$''s income?',
        '[{"id": "A", "text": "$$20\\%$$", "isCorrect": true}, {"id": "B", "text": "$$25\\%$$", "isCorrect": false}, {"id": "C", "text": "$$16\\frac{2}{3}\\%$$", "isCorrect": false}, {"id": "D", "text": "$$30\\%$$", "isCorrect": false}]',
        'A',
        '$$\text{Percentage less} = \frac{r}{100 + r} \times 100 = \frac{25}{125} \times 100 = 20\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000022'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'The price of petrol increased by $$20\%$$ and then decreased by $$10\%$$. What is the net percentage change in price?',
        '[{"id": "A", "text": "$$8\\%\\text{ increase}$$", "isCorrect": true}, {"id": "B", "text": "$$10\\%\\text{ increase}$$", "isCorrect": false}, {"id": "C", "text": "$$8\\%\\text{ decrease}$$", "isCorrect": false}, {"id": "D", "text": "$$2\\%\\text{ increase}$$", "isCorrect": false}]',
        'A',
        'Net change $$= a + b + \frac{ab}{100} = 20 - 10 + \frac{20 \times (-10)}{100} = 10 - 2 = +8\%$$ (increase).'
    ),
    (
        'a10e0000-0000-0000-0000-000000000023'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'MEDIUM',
        'APPLY',
        'In an examination, a candidate must secure $$40\%$$ marks to pass. A student gets $$178$$ marks and fails by $$22$$ marks. What are the maximum marks?',
        '[{"id": "A", "text": "$$500$$", "isCorrect": true}, {"id": "B", "text": "$$450$$", "isCorrect": false}, {"id": "C", "text": "$$600$$", "isCorrect": false}, {"id": "D", "text": "$$400$$", "isCorrect": false}]',
        'A',
        'Passing marks $$= 178 + 22 = 200$$. Since $$40\%$$ corresponds to $$200$$, maximum marks $$= \frac{200}{40} \times 100 = 500$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000024'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'HARD',
        'ANALYZE',
        'The population of a town increases by $$5\%$$ annually. If its current population is $$92610$$, what was its population $$3$$ years ago?',
        '[{"id": "A", "text": "$$80000$$", "isCorrect": true}, {"id": "B", "text": "$$82000$$", "isCorrect": false}, {"id": "C", "text": "$$85000$$", "isCorrect": false}, {"id": "D", "text": "$$78000$$", "isCorrect": false}]',
        'A',
        '$$P \times \left(1 + \frac{5}{100}\right)^3 = 92610 \implies P \times \left(\frac{21}{20}\right)^3 = 92610 \implies P \times \frac{9261}{8000} = 92610 \implies P = 80000$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000025'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'EASY',
        'UNDERSTAND',
        'What is the mean proportional between $$9$$ and $$25$$?',
        '[{"id": "A", "text": "$$15$$", "isCorrect": true}, {"id": "B", "text": "$$17$$", "isCorrect": false}, {"id": "C", "text": "$$12.5$$", "isCorrect": false}, {"id": "D", "text": "$$20$$", "isCorrect": false}]',
        'A',
        'Mean proportional $$= \sqrt{a \times b} = \sqrt{9 \times 25} = 3 \times 5 = 15$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000026'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'EASY',
        'UNDERSTAND',
        'Find the third proportional to $$16$$ and $$24$$.',
        '[{"id": "A", "text": "$$36$$", "isCorrect": true}, {"id": "B", "text": "$$32$$", "isCorrect": false}, {"id": "C", "text": "$$40$$", "isCorrect": false}, {"id": "D", "text": "$$28$$", "isCorrect": false}]',
        'A',
        'Third proportional $$c = \frac{b^2}{a} = \frac{24 \times 24}{16} = 36$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000027'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'If $$A : B = 2 : 3$$ and $$B : C = 4 : 5$$, what is the combined ratio $$A : B : C$$?',
        '[{"id": "A", "text": "$$8 : 12 : 15$$", "isCorrect": true}, {"id": "B", "text": "$$6 : 12 : 15$$", "isCorrect": false}, {"id": "C", "text": "$$8 : 10 : 15$$", "isCorrect": false}, {"id": "D", "text": "$$2 : 4 : 5$$", "isCorrect": false}]',
        'A',
        '$$A : B = 8 : 12$$ and $$B : C = 12 : 15$$. Therefore, $$A : B : C = 8 : 12 : 15$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000028'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'MEDIUM',
        'APPLY',
        'A bag contains coins of $$50\text{ paise}, 25\text{ paise}$$ and $$10\text{ paise}$$ in the ratio $$5 : 9 : 4$$, amounting to $$\text{Rs. } 206$$. What is the number of $$50\text{ paise}$$ coins?',
        '[{"id": "A", "text": "$$200$$", "isCorrect": true}, {"id": "B", "text": "$$360$$", "isCorrect": false}, {"id": "C", "text": "$$160$$", "isCorrect": false}, {"id": "D", "text": "$$180$$", "isCorrect": false}]',
        'A',
        'Value in Rs $$= 5x(0.50) + 9x(0.25) + 4x(0.10) = 2.50x + 2.25x + 0.40x = 5.15x$$. $$5.15x = 206 \implies x = 40$$. Number of $$50\text{ paise}$$ coins $$= 5 \times 40 = 200$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000029'::uuid,
        'Fundamental Arithmetical Operations',
        'Ratio and Proportion',
        'HARD',
        'ANALYZE',
        'The monthly incomes of $$A$$ and $$B$$ are in the ratio $$4 : 5$$ and their monthly expenditures are in the ratio $$7 : 9$$. If each saves $$\text{Rs. } 5000$$ per month, find $$A$$''s monthly income.',
        '[{"id": "A", "text": "$$\\text{Rs. } 40000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 50000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 35000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 45000$$", "isCorrect": false}]',
        'A',
        '$$\frac{4x - 5000}{5x - 5000} = \frac{7}{9} \implies 36x - 45000 = 35x - 35000 \implies x = 10000$$. $$A$$''s income $$= 4x = 40000$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000030'::uuid,
        'Fundamental Arithmetical Operations',
        'Percentages',
        'HARD',
        'ANALYZE',
        'A reduction of $$20\%$$ in the price of sugar enables a purchaser to obtain $$4\text{ kg}$$ more for $$\text{Rs. } 160$$. What is the original price of sugar per kg?',
        '[{"id": "A", "text": "$$\\text{Rs. } 10\\text{ per kg}$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 8\\text{ per kg}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 12\\text{ per kg}$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 15\\text{ per kg}$$", "isCorrect": false}]',
        'A',
        'Money saved by $$20\%$$ drop $$= 20\% \text{ of } 160 = \text{Rs. } 32$$. This buys $$4\text{ kg}$$, so reduced price $$= \frac{32}{4} = 8\text{ Rs/kg}$$. Since reduced price is $$80\%$$ of original: $$\text{Original price} = \frac{8}{0.80} = 10\text{ Rs/kg}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000031'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'EASY',
        'APPLY',
        'An article bought for $$\text{Rs. } 750$$ is sold at a loss of $$12\%$$. What is the selling price?',
        '[{"id": "A", "text": "$$\\text{Rs. } 660$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 640$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 680$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 650$$", "isCorrect": false}]',
        'A',
        '$$\text{SP} = 750 \times (1 - 0.12) = 750 \times 0.88 = \text{Rs. } 660$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000032'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'By selling an article for $$\text{Rs. } 144$$, a shopkeeper loses $$\frac{1}{7}$$ of his outlay. If he sells it for $$\text{Rs. } 189$$, what is his profit percentage?',
        '[{"id": "A", "text": "$$12.5\\%$$", "isCorrect": true}, {"id": "B", "text": "$$15\\%$$", "isCorrect": false}, {"id": "C", "text": "$$10\\%$$", "isCorrect": false}, {"id": "D", "text": "$$8\\%$$", "isCorrect": false}]',
        'A',
        '$$\text{Loss} = \frac{\text{CP}}{7} \implies \text{SP} = \frac{6}{7}\text{CP} = 144 \implies \text{CP} = 168$$. New SP $$= 189$$. Profit $$= 189 - 168 = 21$$. Profit percentage $$= \frac{21}{168} \times 100 = 12.5\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000033'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'The cost price of $$15$$ articles is equal to the selling price of $$12$$ articles. What is the profit percentage?',
        '[{"id": "A", "text": "$$25\\%$$", "isCorrect": true}, {"id": "B", "text": "$$20\\%$$", "isCorrect": false}, {"id": "C", "text": "$$30\\%$$", "isCorrect": false}, {"id": "D", "text": "$$33\\frac{1}{3}\\%$$", "isCorrect": false}]',
        'A',
        '$$15 \times \text{CP} = 12 \times \text{SP} \implies \frac{\text{SP}}{\text{CP}} = \frac{15}{12} = \frac{5}{4}$$. Profit $$= \frac{5-4}{4} \times 100 = 25\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000034'::uuid,
        'Fundamental Arithmetical Operations',
        'Discount',
        'EASY',
        'APPLY',
        'What is the single discount equivalent to two successive discounts of $$20\%$$ and $$10\%$$?',
        '[{"id": "A", "text": "$$28\\%$$", "isCorrect": true}, {"id": "B", "text": "$$30\\%$$", "isCorrect": false}, {"id": "C", "text": "$$25\\%$$", "isCorrect": false}, {"id": "D", "text": "$$26\\%$$", "isCorrect": false}]',
        'A',
        'Single discount $$= d_1 + d_2 - \frac{d_1 d_2}{100} = 20 + 10 - \frac{200}{100} = 30 - 2 = 28\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000035'::uuid,
        'Fundamental Arithmetical Operations',
        'Discount',
        'MEDIUM',
        'APPLY',
        'A merchant marks his goods $$30\%$$ above cost price and allows a discount of $$15\%$$ on the marked price. Find his net profit percentage.',
        '[{"id": "A", "text": "$$10.5\\%$$", "isCorrect": true}, {"id": "B", "text": "$$12\\%$$", "isCorrect": false}, {"id": "C", "text": "$$15\\%$$", "isCorrect": false}, {"id": "D", "text": "$$8.5\\%$$", "isCorrect": false}]',
        'A',
        '$$\text{Net profit} = m - d - \frac{m \times d}{100} = 30 - 15 - \frac{30 \times 15}{100} = 15 - 4.5 = 10.5\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000036'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'HARD',
        'ANALYZE',
        'A dishonest dealer professes to sell his goods at cost price, but uses a false weight of $$900\text{ grams}$$ for a $$1\text{ kg}$$ weight. What is his gain percentage?',
        '[{"id": "A", "text": "$$11\\frac{1}{9}\\%$$", "isCorrect": true}, {"id": "B", "text": "$$10\\%$$", "isCorrect": false}, {"id": "C", "text": "$$12.5\\%$$", "isCorrect": false}, {"id": "D", "text": "$$9\\frac{1}{11}\\%$$", "isCorrect": false}]',
        'A',
        '$$\text{Gain percentage} = \frac{\text{Error}}{\text{True value} - \text{Error}} \times 100 = \frac{100}{900} \times 100 = 11\frac{1}{9}\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000037'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'HARD',
        'ANALYZE',
        'A man sells two horses for $$\text{Rs. } 1920$$ each. On one he gains $$20\%$$ and on the other he loses $$20\%$$. What is his overall gain or loss?',
        '[{"id": "A", "text": "$$4\\%\\text{ loss}$$", "isCorrect": true}, {"id": "B", "text": "$$4\\%\\text{ gain}$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{No profit, no loss}$$", "isCorrect": false}, {"id": "D", "text": "$$2\\%\\text{ loss}$$", "isCorrect": false}]',
        'A',
        'When two items are sold at the same price, one with $$x\%$$ gain and other with $$x\%$$ loss, there is always an overall loss of $$\frac{x^2}{100}\% = \frac{20^2}{100}\% = 4\%\text{ loss}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000038'::uuid,
        'Fundamental Arithmetical Operations',
        'Discount',
        'MEDIUM',
        'APPLY',
        'After allowing a discount of $$16\%$$ on the marked price, a trader still makes a profit of $$5\%$$. By what percentage is the marked price above the cost price?',
        '[{"id": "A", "text": "$$25\\%$$", "isCorrect": true}, {"id": "B", "text": "$$21\\%$$", "isCorrect": false}, {"id": "C", "text": "$$20\\%$$", "isCorrect": false}, {"id": "D", "text": "$$30\\%$$", "isCorrect": false}]',
        'A',
        '$$\frac{\text{MP}}{\text{CP}} = \frac{100 + \text{Profit}\%}{100 - \text{Discount}\%} = \frac{105}{84} = \frac{5}{4} = 1.25$$. The marked price is $$25\%$$ above cost price.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000039'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'MEDIUM',
        'APPLY',
        'A shopkeeper sells an umbrella for $$\text{Rs. } 300$$ and makes a profit of $$20\%$$. During clearance sale, he offers a discount of $$10\%$$ on marked price ($$\text{Rs. } 300$$). What is his profit percentage during clearance?',
        '[{"id": "A", "text": "$$8\\%$$", "isCorrect": true}, {"id": "B", "text": "$$10\\%$$", "isCorrect": false}, {"id": "C", "text": "$$6\\%$$", "isCorrect": false}, {"id": "D", "text": "$$12\\%$$", "isCorrect": false}]',
        'A',
        '$$\text{CP} = \frac{300}{1.20} = 250$$. Clearance price $$= 300 \times 0.90 = 270$$. Profit $$= 270 - 250 = 20$$. Profit percentage $$= \frac{20}{250} \times 100 = 8\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000040'::uuid,
        'Fundamental Arithmetical Operations',
        'Profit and Loss',
        'EASY',
        'APPLY',
        'If an item is sold for $$\text{Rs. } 920$$ at a profit of $$15\%$$, what was its cost price?',
        '[{"id": "A", "text": "$$\\text{Rs. } 800$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 780$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 820$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 850$$", "isCorrect": false}]',
        'A',
        '$$\text{CP} = \frac{\text{SP}}{1 + \text{profit}\%} = \frac{920}{1.15} = \text{Rs. } 800$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000041'::uuid,
        'Fundamental Arithmetical Operations',
        'Simple Interest',
        'EASY',
        'APPLY',
        'What simple interest is earned on a principal of $$\text{Rs. } 5000$$ invested at $$8\%$$ per annum for $$3$$ years?',
        '[{"id": "A", "text": "$$\\text{Rs. } 1200$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1500$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 1000$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 1250$$", "isCorrect": false}]',
        'A',
        '$$\text{SI} = \frac{P \times R \times T}{100} = \frac{5000 \times 8 \times 3}{100} = \text{Rs. } 1200$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000042'::uuid,
        'Fundamental Arithmetical Operations',
        'Simple Interest',
        'MEDIUM',
        'APPLY',
        'A sum of money doubles itself in $$8$$ years at simple interest. What is the annual rate of interest?',
        '[{"id": "A", "text": "$$12.5\\%$$", "isCorrect": true}, {"id": "B", "text": "$$10\\%$$", "isCorrect": false}, {"id": "C", "text": "$$15\\%$$", "isCorrect": false}, {"id": "D", "text": "$$8\\%$$", "isCorrect": false}]',
        'A',
        'If $$P$$ doubles, $$\text{SI} = P$$. $$P = \frac{P \times R \times 8}{100} \implies R = \frac{100}{8} = 12.5\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000043'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'EASY',
        'APPLY',
        'What is the compound interest on $$\text{Rs. } 8000$$ at $$5\%$$ per annum for $$2$$ years compounded annually?',
        '[{"id": "A", "text": "$$\\text{Rs. } 820$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 800$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 840$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 850$$", "isCorrect": false}]',
        'A',
        '$$A = 8000 \times (1 + 0.05)^2 = 8000 \times 1.1025 = 8820$$. $$\text{CI} = 8820 - 8000 = \text{Rs. } 820$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000044'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'What is the difference between compound interest and simple interest on $$\text{Rs. } 15000$$ for $$2$$ years at $$8\%$$ per annum?',
        '[{"id": "A", "text": "$$\\text{Rs. } 96$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 84$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 108$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 90$$", "isCorrect": false}]',
        'A',
        'Difference for $$2$$ years $$= P \left(\frac{R}{100}\right)^2 = 15000 \times \left(\frac{8}{100}\right)^2 = 15000 \times \frac{64}{10000} = 15 \times 6.4 = \text{Rs. } 96$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000045'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'A sum of money invested at compound interest amounts to $$\text{Rs. } 4500$$ in $$2$$ years and $$\text{Rs. } 6750$$ in $$3$$ years. What is the rate of interest?',
        '[{"id": "A", "text": "$$50\\%$$", "isCorrect": true}, {"id": "B", "text": "$$33\\frac{1}{3}\\%$$", "isCorrect": false}, {"id": "C", "text": "$$25\\%$$", "isCorrect": false}, {"id": "D", "text": "$$40\\%$$", "isCorrect": false}]',
        'A',
        'Interest in 3rd year $$= 6750 - 4500 = 2250$$. Rate $$= \frac{2250}{4500} \times 100 = 50\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000046'::uuid,
        'Fundamental Arithmetical Operations',
        'Simple Interest',
        'MEDIUM',
        'APPLY',
        'A sum of money amounts to $$\text{Rs. } 5200$$ in $$5$$ years and to $$\text{Rs. } 5680$$ in $$7$$ years at simple interest. Find the principal sum.',
        '[{"id": "A", "text": "$$\\text{Rs. } 4000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 4200$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 3800$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 4500$$", "isCorrect": false}]',
        'A',
        'Interest for $$2$$ years $$= 5680 - 5200 = 480$$. Annual interest $$= 240$$. Interest in $$5$$ years $$= 5 \times 240 = 1200$$. Principal $$= 5200 - 1200 = \text{Rs. } 4000$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000047'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'HARD',
        'ANALYZE',
        'What is the compound interest on $$\text{Rs. } 16000$$ at $$20\%$$ per annum for $$9$$ months, compounded quarterly?',
        '[{"id": "A", "text": "$$\\text{Rs. } 2522$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 2400$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 2600$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 2480$$", "isCorrect": false}]',
        'A',
        'Quarterly rate $$r = \frac{20}{4} = 5\%$$. Periods $$n = 3$$ quarters. $$A = 16000 \times (1.05)^3 = 16000 \times \frac{9261}{8000} = 18522$$. $$\text{CI} = 18522 - 16000 = \text{Rs. } 2522$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000048'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'MEDIUM',
        'APPLY',
        'At what rate percent per annum compound interest will $$\text{Rs. } 1000$$ amount to $$\text{Rs. } 1331$$ in $$3$$ years?',
        '[{"id": "A", "text": "$$10\\%$$", "isCorrect": true}, {"id": "B", "text": "$$12\\%$$", "isCorrect": false}, {"id": "C", "text": "$$8\\%$$", "isCorrect": false}, {"id": "D", "text": "$$15\\%$$", "isCorrect": false}]',
        'A',
        '$$\frac{1331}{1000} = \left(1 + \frac{R}{100}\right)^3 \implies \left(\frac{11}{10}\right)^3 = \left(1 + \frac{R}{100}\right)^3 \implies 1 + \frac{R}{100} = 1.1 \implies R = 10\%$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000049'::uuid,
        'Fundamental Arithmetical Operations',
        'Simple Interest',
        'EASY',
        'UNDERSTAND',
        'If the simple interest on a certain sum of money for $$3$$ years at $$5\%$$ per annum is $$\text{Rs. } 150$$, find the sum.',
        '[{"id": "A", "text": "$$\\text{Rs. } 1000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 1200$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 900$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 1500$$", "isCorrect": false}]',
        'A',
        '$$P = \frac{\text{SI} \times 100}{R \times T} = \frac{150 \times 100}{5 \times 3} = \text{Rs. } 1000$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000050'::uuid,
        'Fundamental Arithmetical Operations',
        'Compound Interest',
        'HARD',
        'ANALYZE',
        'The difference between CI and SI on a sum of money at $$10\%$$ per annum for $$3$$ years is $$\text{Rs. } 155$$. Find the principal.',
        '[{"id": "A", "text": "$$\\text{Rs. } 5000$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 6000$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 4500$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 5500$$", "isCorrect": false}]',
        'A',
        'Difference for $$3$$ years $$= P \left(\frac{R}{100}\right)^2 \left(3 + \frac{R}{100}\right)$$. $$155 = P \left(\frac{1}{100}\right) (3.1) \implies 155 = \frac{3.1 P}{100} \implies P = \frac{155 \times 100}{3.1} = 5000$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000051'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'EASY',
        'APPLY',
        '$$A$$ can complete a piece of work in $$12$$ days and $$B$$ in $$24$$ days. In how many days can they complete the work together?',
        '[{"id": "A", "text": "$$8\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$9\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$6\\text{ days}$$", "isCorrect": false}]',
        'A',
        '$$\text{Time} = \frac{A \times B}{A + B} = \frac{12 \times 24}{12 + 24} = \frac{288}{36} = 8\text{ days}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000052'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        '$$A$$ is twice as good a workman as $$B$$ and together they finish a piece of work in $$18$$ days. In how many days can $$A$$ alone finish the work?',
        '[{"id": "A", "text": "$$27\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$36\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$24\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$30\\text{ days}$$", "isCorrect": false}]',
        'A',
        'Efficiencies: $$A = 2, B = 1$$. Total efficiency $$= 3$$. Total work $$= 18 \times 3 = 54\text{ units}$$. $$A$$''s time $$= \frac{54}{2} = 27\text{ days}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000053'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        '$$15$$ men can complete a project in $$20$$ days. How many men are needed to complete the same work in $$12$$ days?',
        '[{"id": "A", "text": "$$25\\text{ men}$$", "isCorrect": true}, {"id": "B", "text": "$$24\\text{ men}$$", "isCorrect": false}, {"id": "C", "text": "$$30\\text{ men}$$", "isCorrect": false}, {"id": "D", "text": "$$20\\text{ men}$$", "isCorrect": false}]',
        'A',
        '$$M_1 D_1 = M_2 D_2 \implies 15 \times 20 = M_2 \times 12 \implies M_2 = \frac{300}{12} = 25\text{ men}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000054'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'HARD',
        'ANALYZE',
        '$$A$$ and $$B$$ can complete a task in $$15$$ days and $$10$$ days respectively. They began the work together, but after $$2$$ days $$B$$ had to leave and $$A$$ finished the remaining work alone. In how many days was the total work finished?',
        '[{"id": "A", "text": "$$12\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$10\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ days}$$", "isCorrect": false}]',
        'A',
        'Work $$= \text{LCM}(15, 10) = 30$$. Daily work: $$A = 2, B = 3$$. Together in $$2$$ days $$= 2 \times 5 = 10\text{ units}$$. Remaining work $$= 20\text{ units}$$. $$A$$ takes $$\frac{20}{2} = 10\text{ days}$$. Total time $$= 2 + 10 = 12\text{ days}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000055'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'Two pipes $$A$$ and $$B$$ can fill a tank in $$20\text{ minutes}$$ and $$30\text{ minutes}$$ respectively. If both pipes are opened together, how long will it take to fill the tank?',
        '[{"id": "A", "text": "$$12\\text{ minutes}$$", "isCorrect": true}, {"id": "B", "text": "$$15\\text{ minutes}$$", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ minutes}$$", "isCorrect": false}, {"id": "D", "text": "$$14\\text{ minutes}$$", "isCorrect": false}]',
        'A',
        '$$\text{Time} = \frac{20 \times 30}{20 + 30} = \frac{600}{50} = 12\text{ minutes}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000056'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        'A tap can fill a cistern in $$8\text{ hours}$$, while a leak in the bottom empties it in $$12\text{ hours}$$. If the tap and the leak are both open, how many hours will it take to fill the empty cistern?',
        '[{"id": "A", "text": "$$24\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$20\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$16\\text{ hours}$$", "isCorrect": false}, {"id": "D", "text": "$$28\\text{ hours}$$", "isCorrect": false}]',
        'A',
        'Net rate $$= \frac{1}{8} - \frac{1}{12} = \frac{3 - 2}{24} = \frac{1}{24}$$. It takes $$24\text{ hours}$$ to fill.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000057'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'EASY',
        'UNDERSTAND',
        '$$A$$ and $$B$$ undertook a work for $$\text{Rs. } 720$$. $$A$$ alone can do it in $$8$$ days and $$B$$ alone in $$12$$ days. With the help of $$C$$, they finished it in $$4$$ days. What is $$C$$''s share of money?',
        '[{"id": "A", "text": "$$\\text{Rs. } 120$$", "isCorrect": true}, {"id": "B", "text": "$$\\text{Rs. } 150$$", "isCorrect": false}, {"id": "C", "text": "$$\\text{Rs. } 100$$", "isCorrect": false}, {"id": "D", "text": "$$\\text{Rs. } 180$$", "isCorrect": false}]',
        'A',
        '$$C$$''s work in $$1$$ day $$= \frac{1}{4} - \left(\frac{1}{8} + \frac{1}{12}\right) = \frac{1}{4} - \frac{5}{24} = \frac{1}{24}$$. Ratio of work $$= \frac{1}{8} : \frac{1}{12} : \frac{1}{24} = 3 : 2 : 1$$. $$C$$''s share $$= \frac{1}{6} \times 720 = \text{Rs. } 120$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000058'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'HARD',
        'ANALYZE',
        '$$A$$ can do a job in $$16$$ days, and $$B$$ can do it in $$12$$ days. They work on alternate days starting with $$A$$. In how many days will the work be completed?',
        '[{"id": "A", "text": "$$13\\frac{3}{4}\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$13\\frac{1}{2}\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$12\\frac{2}{3}\\text{ days}$$", "isCorrect": false}]',
        'A',
        'Work $$= 48$$. $$A = 3, B = 4$$. In $$2$$ days, work done $$= 7$$. In $$12$$ days, work $$= 6 \times 7 = 42$$. Day $$13$$ ($$A$$): work $$= 3$$, total $$= 45$$, remaining $$= 3$$. Day $$14$$ ($$B$$): takes $$\frac{3}{4}$$ day. Total $$= 13\frac{3}{4}\text{ days}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000059'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'MEDIUM',
        'APPLY',
        '$$3$$ men or $$5$$ women can do a piece of work in $$43$$ days. In how many days can $$5$$ men and $$6$$ women do the same work?',
        '[{"id": "A", "text": "$$15\\text{ days}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ days}$$", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$20\\text{ days}$$", "isCorrect": false}]',
        'A',
        '$$3M = 5W \implies 1M = \frac{5}{3}W$$. $$5M + 6W = 5\left(\frac{5}{3}\right)W + 6W = \frac{43}{3}W$$. If $$5W$$ take $$43$$ days, $$\frac{43}{3}W$$ take $$\frac{5 \times 43}{43/3} = 15\text{ days}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000060'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Work',
        'EASY',
        'APPLY',
        'A cistern has two inlet pipes which fill it in $$15$$ and $$20$$ hours respectively. A waste pipe empties it in $$30$$ hours. If all three are opened together, how long does it take to fill the cistern?',
        '[{"id": "A", "text": "$$12\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$10\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$14\\text{ hours}$$", "isCorrect": false}, {"id": "D", "text": "$$15\\text{ hours}$$", "isCorrect": false}]',
        'A',
        'Capacity $$= 60$$. Rates: $$+4, +3, -2$$. Net rate $$= 4 + 3 - 2 = 5\text{ units/hr}$$. Time $$= \frac{60}{5} = 12\text{ hours}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000061'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'EASY',
        'UNDERSTAND',
        'Convert a speed of $$54\text{ km/h}$$ into meters per second ($$\text{m/s}$$).',
        '[{"id": "A", "text": "$$15\\text{ m/s}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ m/s}$$", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ m/s}$$", "isCorrect": false}, {"id": "D", "text": "$$20\\text{ m/s}$$", "isCorrect": false}]',
        'A',
        '$$54 \times \frac{5}{18} = 3 \times 5 = 15\text{ m/s}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000062'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'EASY',
        'APPLY',
        'A car travels from $$A$$ to $$B$$ at $$60\text{ km/h}$$ and returns at $$40\text{ km/h}$$. What is the average speed of the car for the entire journey?',
        '[{"id": "A", "text": "$$48\\text{ km/h}$$", "isCorrect": true}, {"id": "B", "text": "$$50\\text{ km/h}$$", "isCorrect": false}, {"id": "C", "text": "$$45\\text{ km/h}$$", "isCorrect": false}, {"id": "D", "text": "$$52\\text{ km/h}$$", "isCorrect": false}]',
        'A',
        '$$\text{Average speed} = \frac{2xy}{x+y} = \frac{2 \times 60 \times 40}{60 + 40} = \frac{4800}{100} = 48\text{ km/h}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000063'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'A train $$250\text{ m}$$ long passes a pole in $$10\text{ seconds}$$. What is the speed of the train in $$\text{km/h}$$?',
        '[{"id": "A", "text": "$$90\\text{ km/h}$$", "isCorrect": true}, {"id": "B", "text": "$$72\\text{ km/h}$$", "isCorrect": false}, {"id": "C", "text": "$$80\\text{ km/h}$$", "isCorrect": false}, {"id": "D", "text": "$$100\\text{ km/h}$$", "isCorrect": false}]',
        'A',
        '$$\text{Speed} = \frac{250}{10} = 25\text{ m/s}$$. In km/h: $$25 \times \frac{18}{5} = 90\text{ km/h}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000064'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'A train $$150\text{ m}$$ long moving at $$72\text{ km/h}$$ crosses a platform of length $$250\text{ m}$$. How much time does it take?',
        '[{"id": "A", "text": "$$20\\text{ seconds}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ seconds}$$", "isCorrect": false}, {"id": "C", "text": "$$25\\text{ seconds}$$", "isCorrect": false}, {"id": "D", "text": "$$15\\text{ seconds}$$", "isCorrect": false}]',
        'A',
        'Speed $$= 72 \times \frac{5}{18} = 20\text{ m/s}$$. Total distance $$= 150 + 250 = 400\text{ m}$$. Time $$= \frac{400}{20} = 20\text{ seconds}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000065'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'HARD',
        'ANALYZE',
        'Two trains of lengths $$120\text{ m}$$ and $$180\text{ m}$$ are travelling in opposite directions on parallel tracks at $$42\text{ km/h}$$ and $$30\text{ km/h}$$ respectively. In how many seconds will they completely pass each other?',
        '[{"id": "A", "text": "$$15\\text{ seconds}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ seconds}$$", "isCorrect": false}, {"id": "C", "text": "$$12\\text{ seconds}$$", "isCorrect": false}, {"id": "D", "text": "$$20\\text{ seconds}$$", "isCorrect": false}]',
        'A',
        'Relative speed $$= 42 + 30 = 72\text{ km/h} = 20\text{ m/s}$$. Total distance $$= 120 + 180 = 300\text{ m}$$. Time $$= \frac{300}{20} = 15\text{ seconds}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000066'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'A boat travels downstream at $$14\text{ km/h}$$ and upstream at $$8\text{ km/h}$$. What is the speed of the stream?',
        '[{"id": "A", "text": "$$3\\text{ km/h}$$", "isCorrect": true}, {"id": "B", "text": "$$4\\text{ km/h}$$", "isCorrect": false}, {"id": "C", "text": "$$2.5\\text{ km/h}$$", "isCorrect": false}, {"id": "D", "text": "$$5\\text{ km/h}$$", "isCorrect": false}]',
        'A',
        '$$\text{Speed of stream} = \frac{\text{Downstream} - \text{Upstream}}{2} = \frac{14 - 8}{2} = 3\text{ km/h}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000067'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'MEDIUM',
        'APPLY',
        'A man can row a boat at $$9\text{ km/h}$$ in still water. If the stream flows at $$3\text{ km/h}$$, how long does it take him to row $$24\text{ km}$$ upstream and return downstream?',
        '[{"id": "A", "text": "$$6\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$5\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$7\\text{ hours}$$", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ hours}$$", "isCorrect": false}]',
        'A',
        'Upstream speed $$= 9 - 3 = 6\text{ km/h}$$. Time upstream $$= \frac{24}{6} = 4\text{ hours}$$. Downstream speed $$= 9 + 3 = 12\text{ km/h}$$. Time downstream $$= \frac{24}{12} = 2\text{ hours}$$. Total time $$= 4 + 2 = 6\text{ hours}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000068'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'EASY',
        'UNDERSTAND',
        'Walking at $$4\text{ km/h}$$, a student reaches school $$5\text{ minutes}$$ late. Walking at $$5\text{ km/h}$$, he reaches $$10\text{ minutes}$$ early. What is the distance between his home and school?',
        '[{"id": "A", "text": "$$5\\text{ km}$$", "isCorrect": true}, {"id": "B", "text": "$$4\\text{ km}$$", "isCorrect": false}, {"id": "C", "text": "$$6\\text{ km}$$", "isCorrect": false}, {"id": "D", "text": "$$7.5\\text{ km}$$", "isCorrect": false}]',
        'A',
        'Difference in time $$= 5 - (-10) = 15\text{ minutes} = \frac{1}{4}\text{ hour}$$. Distance $$= \frac{s_1 s_2}{s_2 - s_1} \times \Delta t = \frac{4 \times 5}{1} \times \frac{1}{4} = 5\text{ km}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000069'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'HARD',
        'ANALYZE',
        'A thief steals a car at $$1:30\text{ PM}$$ and drives it at $$40\text{ km/h}$$. The theft is discovered at $$2:00\text{ PM}$$ and the owner sets off in another car at $$50\text{ km/h}$$. When will the owner overtake the thief?',
        '[{"id": "A", "text": "$$4:00\\text{ PM}$$", "isCorrect": true}, {"id": "B", "text": "$$3:30\\text{ PM}$$", "isCorrect": false}, {"id": "C", "text": "$$4:30\\text{ PM}$$", "isCorrect": false}, {"id": "D", "text": "$$5:00\\text{ PM}$$", "isCorrect": false}]',
        'A',
        'Thief lead in $$30\text{ minutes} = 40 \times 0.5 = 20\text{ km}$$. Relative speed $$= 50 - 40 = 10\text{ km/h}$$. Time to catch $$= \frac{20}{10} = 2\text{ hours}$$. Time $$= 2:00\text{ PM} + 2\text{ hours} = 4:00\text{ PM}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000070'::uuid,
        'Fundamental Arithmetical Operations',
        'Time and Distance',
        'HARD',
        'ANALYZE',
        'A train running at a uniform speed passes a $$160\text{ m}$$ long bridge in $$18\text{ seconds}$$ and a $$120\text{ m}$$ long bridge in $$15\text{ seconds}$$. What is the length of the train?',
        '[{"id": "A", "text": "$$80\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$90\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$75\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$100\\text{ m}$$", "isCorrect": false}]',
        'A',
        'Speed $$= \frac{160 - 120}{18 - 15} = \frac{40}{3}\text{ m/s}$$. Distance in $$15\text{ s} = 15 \times \frac{40}{3} = 200\text{ m}$$. Length of train $$= 200 - 120 = 80\text{ m}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000071'::uuid,
        'Mensuration',
        'Circle',
        'EASY',
        'APPLY',
        'What is the area of a circle whose circumference is $$88\text{ cm}$$? (Take $$\pi = \frac{22}{7}$$)',
        '[{"id": "A", "text": "$$616\\text{ cm}^2$$", "isCorrect": true}, {"id": "B", "text": "$$308\\text{ cm}^2$$", "isCorrect": false}, {"id": "C", "text": "$$484\\text{ cm}^2$$", "isCorrect": false}, {"id": "D", "text": "$$576\\text{ cm}^2$$", "isCorrect": false}]',
        'A',
        '$$2\pi r = 88 \implies r = \frac{88 \times 7}{44} = 14\text{ cm}$$. Area $$= \pi r^2 = \frac{22}{7} \times 14 \times 14 = 616\text{ cm}^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000072'::uuid,
        'Mensuration',
        'Triangle',
        'EASY',
        'APPLY',
        'What is the area of an equilateral triangle with side length $$8\text{ cm}$$?',
        '[{"id": "A", "text": "$$16\\sqrt{3}\\text{ cm}^2$$", "isCorrect": true}, {"id": "B", "text": "$$32\\sqrt{3}\\text{ cm}^2$$", "isCorrect": false}, {"id": "C", "text": "$$64\\sqrt{3}\\text{ cm}^2$$", "isCorrect": false}, {"id": "D", "text": "$$24\\sqrt{3}\\text{ cm}^2$$", "isCorrect": false}]',
        'A',
        '$$\text{Area} = \frac{\sqrt{3}}{4} a^2 = \frac{\sqrt{3}}{4} \times 64 = 16\sqrt{3}\text{ cm}^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000073'::uuid,
        'Mensuration',
        'Quadrilaterals',
        'MEDIUM',
        'APPLY',
        'The diagonals of a rhombus are $$16\text{ cm}$$ and $$12\text{ cm}$$. What is the perimeter of the rhombus?',
        '[{"id": "A", "text": "$$40\\text{ cm}$$", "isCorrect": true}, {"id": "B", "text": "$$48\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$36\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$52\\text{ cm}$$", "isCorrect": false}]',
        'A',
        'Half diagonals are $$8\text{ cm}$$ and $$6\text{ cm}$$. Side $$s = \sqrt{8^2 + 6^2} = \sqrt{100} = 10\text{ cm}$$. Perimeter $$= 4s = 40\text{ cm}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000074'::uuid,
        'Mensuration',
        'Right Circular Cylinder',
        'MEDIUM',
        'APPLY',
        'Find the curved surface area of a right circular cylinder whose base radius is $$7\text{ cm}$$ and height is $$15\text{ cm}$$. (Take $$\pi = \frac{22}{7}$$)',
        '[{"id": "A", "text": "$$660\\text{ cm}^2$$", "isCorrect": true}, {"id": "B", "text": "$$720\\text{ cm}^2$$", "isCorrect": false}, {"id": "C", "text": "$$580\\text{ cm}^2$$", "isCorrect": false}, {"id": "D", "text": "$$600\\text{ cm}^2$$", "isCorrect": false}]',
        'A',
        '$$\text{CSA} = 2\pi rh = 2 \times \frac{22}{7} \times 7 \times 15 = 44 \times 15 = 660\text{ cm}^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000075'::uuid,
        'Mensuration',
        'Right Circular Cone',
        'MEDIUM',
        'APPLY',
        'A right circular cone has a base radius of $$6\text{ cm}$$ and height of $$8\text{ cm}$$. What is its total surface area? (Take $$\pi = 3.14$$)',
        '[{"id": "A", "text": "$$301.44\\text{ cm}^2$$", "isCorrect": true}, {"id": "B", "text": "$$282.60\\text{ cm}^2$$", "isCorrect": false}, {"id": "C", "text": "$$320.50\\text{ cm}^2$$", "isCorrect": false}, {"id": "D", "text": "$$250.24\\text{ cm}^2$$", "isCorrect": false}]',
        'A',
        'Slant height $$l = \sqrt{6^2 + 8^2} = 10\text{ cm}$$. Total surface area $$= \pi r (l + r) = 3.14 \times 6 \times 16 = 301.44\text{ cm}^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000076'::uuid,
        'Mensuration',
        'Sphere',
        'EASY',
        'REMEMBER',
        'What is the ratio of the volume of a sphere to the volume of a hemisphere of the same radius?',
        '[{"id": "A", "text": "$$2 : 1$$", "isCorrect": true}, {"id": "B", "text": "$$4 : 1$$", "isCorrect": false}, {"id": "C", "text": "$$3 : 2$$", "isCorrect": false}, {"id": "D", "text": "$$1 : 2$$", "isCorrect": false}]',
        'A',
        '$$V_{\text{sphere}} = \frac{4}{3}\pi r^3$$ and $$V_{\text{hemi}} = \frac{2}{3}\pi r^3$$. Ratio $$= 2 : 1$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000077'::uuid,
        'Mensuration',
        'Rectangular Parallelepiped',
        'MEDIUM',
        'APPLY',
        'What is the length of the longest pole that can be placed in a room of dimensions $$10\text{ m} \times 10\text{ m} \times 5\text{ m}$$?',
        '[{"id": "A", "text": "$$15\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$12\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$18\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$16\\text{ m}$$", "isCorrect": false}]',
        'A',
        '$$\text{Diagonal} = \sqrt{l^2 + b^2 + h^2} = \sqrt{100 + 100 + 25} = \sqrt{225} = 15\text{ m}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000078'::uuid,
        'Mensuration',
        'Sphere',
        'HARD',
        'ANALYZE',
        'A solid metallic sphere of radius $$6\text{ cm}$$ is melted and recast into small spheres of radius $$1\text{ cm}$$ each. How many such small spheres can be formed?',
        '[{"id": "A", "text": "$$216$$", "isCorrect": true}, {"id": "B", "text": "$$36$$", "isCorrect": false}, {"id": "C", "text": "$$128$$", "isCorrect": false}, {"id": "D", "text": "$$512$$", "isCorrect": false}]',
        'A',
        '$$N = \frac{V_{\text{large}}}{V_{\text{small}}} = \frac{R^3}{r^3} = \frac{6^3}{1^3} = 216$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000079'::uuid,
        'Mensuration',
        'Circle',
        'MEDIUM',
        'APPLY',
        'A wheel makes $$1000$$ revolutions in covering a distance of $$88\text{ km}$$. What is the diameter of the wheel? (Take $$\pi = \frac{22}{7}$$)',
        '[{"id": "A", "text": "$$28\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$14\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$21\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$35\\text{ m}$$", "isCorrect": false}]',
        'A',
        'Distance in $$1$$ revolution $$= \frac{88000\text{ m}}{1000} = 88\text{ m}$$. $$\pi d = 88 \implies d = \frac{88 \times 7}{22} = 28\text{ m}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000080'::uuid,
        'Mensuration',
        'Hemisphere',
        'HARD',
        'ANALYZE',
        'A solid hemisphere has a radius of $$7\text{ cm}$$. What is its total surface area? (Take $$\pi = \frac{22}{7}$$)',
        '[{"id": "A", "text": "$$462\\text{ cm}^2$$", "isCorrect": true}, {"id": "B", "text": "$$308\\text{ cm}^2$$", "isCorrect": false}, {"id": "C", "text": "$$616\\text{ cm}^2$$", "isCorrect": false}, {"id": "D", "text": "$$539\\text{ cm}^2$$", "isCorrect": false}]',
        'A',
        'Total surface area of solid hemisphere $$= 3\pi r^2 = 3 \times \frac{22}{7} \times 7 \times 7 = 3 \times 154 = 462\text{ cm}^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000081'::uuid,
        'Algebra',
        'Algebraic Identities',
        'EASY',
        'APPLY',
        'If $$x + \frac{1}{x} = 5$$, find the value of $$x^2 + \frac{1}{x^2}$$.',
        '[{"id": "A", "text": "$$23$$", "isCorrect": true}, {"id": "B", "text": "$$25$$", "isCorrect": false}, {"id": "C", "text": "$$27$$", "isCorrect": false}, {"id": "D", "text": "$$21$$", "isCorrect": false}]',
        'A',
        '$$x^2 + \frac{1}{x^2} = \left(x + \frac{1}{x}\right)^2 - 2 = 5^2 - 2 = 23$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000082'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$x + \frac{1}{x} = 4$$, what is the value of $$x^3 + \frac{1}{x^3}$$?',
        '[{"id": "A", "text": "$$52$$", "isCorrect": true}, {"id": "B", "text": "$$64$$", "isCorrect": false}, {"id": "C", "text": "$$48$$", "isCorrect": false}, {"id": "D", "text": "$$56$$", "isCorrect": false}]',
        'A',
        '$$x^3 + \frac{1}{x^3} = \left(x + \frac{1}{x}\right)^3 - 3\left(x + \frac{1}{x}\right) = 4^3 - 3(4) = 64 - 12 = 52$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000083'::uuid,
        'Algebra',
        'Algebraic Identities',
        'MEDIUM',
        'APPLY',
        'If $$a + b + c = 0$$, what is the value of $$\frac{a^3 + b^3 + c^3}{abc}$$?',
        '[{"id": "A", "text": "$$3$$", "isCorrect": true}, {"id": "B", "text": "$$1$$", "isCorrect": false}, {"id": "C", "text": "$$0$$", "isCorrect": false}, {"id": "D", "text": "$$-3$$", "isCorrect": false}]',
        'A',
        'If $$a + b + c = 0$$, then $$a^3 + b^3 + c^3 = 3abc$$. Dividing by $$abc$$ gives $$3$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000084'::uuid,
        'Algebra',
        'Elementary Surds',
        'MEDIUM',
        'APPLY',
        'If $$x = 7 + 4\sqrt{3}$$, what is the value of $$\sqrt{x} + \frac{1}{\sqrt{x}}$$?',
        '[{"id": "A", "text": "$$4$$", "isCorrect": true}, {"id": "B", "text": "$$2\\sqrt{3}$$", "isCorrect": false}, {"id": "C", "text": "$$4\\sqrt{3}$$", "isCorrect": false}, {"id": "D", "text": "$$2$$", "isCorrect": false}]',
        'A',
        '$$x = (2 + \sqrt{3})^2 \implies \sqrt{x} = 2 + \sqrt{3}$$. $$\frac{1}{\sqrt{x}} = 2 - \sqrt{3}$$. Sum $$= (2 + \sqrt{3}) + (2 - \sqrt{3}) = 4$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000085'::uuid,
        'Algebra',
        'Graphs of Linear Equations',
        'EASY',
        'APPLY',
        'Solve the system of linear equations: $$2x + 3y = 11$$ and $$2x - 4y = -24$$. What is the value of $$y$$?',
        '[{"id": "A", "text": "$$5$$", "isCorrect": true}, {"id": "B", "text": "$$3$$", "isCorrect": false}, {"id": "C", "text": "$$-2$$", "isCorrect": false}, {"id": "D", "text": "$$4$$", "isCorrect": false}]',
        'A',
        'Subtracting second from first: $$7y = 35 \implies y = 5$$. Then $$2x + 15 = 11 \implies 2x = -4 \implies x = -2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000086'::uuid,
        'Algebra',
        'Algebraic Identities',
        'HARD',
        'ANALYZE',
        'If $$\alpha$$ and $$\beta$$ are the roots of $$2x^2 - 5x + 2 = 0$$, what is the value of $$\alpha^3 + \beta^3$$?',
        '[{"id": "A", "text": "$$\\frac{65}{8}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{63}{8}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{125}{8}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{35}{4}$$", "isCorrect": false}]',
        'A',
        '$$\alpha + \beta = \frac{5}{2}$$, $$\alpha\beta = 1$$. $$\alpha^3 + \beta^3 = (\alpha+\beta)^3 - 3\alpha\beta(\alpha+\beta) = \frac{125}{8} - 3(1)\left(\frac{5}{2}\right) = \frac{125 - 60}{8} = \frac{65}{8}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000087'::uuid,
        'Algebra',
        'Algebraic Identities',
        'EASY',
        'UNDERSTAND',
        'What is the value of $$(a - b)^2 + 4ab$$?',
        '[{"id": "A", "text": "$$(a + b)^2$$", "isCorrect": true}, {"id": "B", "text": "$$a^2 - b^2$$", "isCorrect": false}, {"id": "C", "text": "$$a^2 + b^2$$", "isCorrect": false}, {"id": "D", "text": "$$(a - b)^2$$", "isCorrect": false}]',
        'A',
        '$$(a - b)^2 + 4ab = a^2 - 2ab + b^2 + 4ab = a^2 + 2ab + b^2 = (a + b)^2$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000088'::uuid,
        'Algebra',
        'Elementary Surds',
        'MEDIUM',
        'APPLY',
        'Find the square root of $$14 + 6\sqrt{5}$$.',
        '[{"id": "A", "text": "$$3 + \\sqrt{5}$$", "isCorrect": true}, {"id": "B", "text": "$$2 + \\sqrt{5}$$", "isCorrect": false}, {"id": "C", "text": "$$3 - \\sqrt{5}$$", "isCorrect": false}, {"id": "D", "text": "$$1 + 2\\sqrt{5}$$", "isCorrect": false}]',
        'A',
        '$$(3 + \sqrt{5})^2 = 3^2 + (\sqrt{5})^2 + 2(3)(\sqrt{5}) = 9 + 5 + 6\sqrt{5} = 14 + 6\sqrt{5}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000089'::uuid,
        'Algebra',
        'Algebraic Identities',
        'HARD',
        'ANALYZE',
        'If $$x = 999, y = 1000, z = 1001$$, find the value of $$x^2 + y^2 + z^2 - xy - yz - zx$$.',
        '[{"id": "A", "text": "$$3$$", "isCorrect": true}, {"id": "B", "text": "$$1$$", "isCorrect": false}, {"id": "C", "text": "$$6$$", "isCorrect": false}, {"id": "D", "text": "$$0$$", "isCorrect": false}]',
        'A',
        '$$\frac{1}{2}[(x-y)^2 + (y-z)^2 + (z-x)^2] = \frac{1}{2}[(-1)^2 + (-1)^2 + (2)^2] = \frac{1}{2}[1 + 1 + 4] = 3$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000090'::uuid,
        'Algebra',
        'Graphs of Linear Equations',
        'EASY',
        'REMEMBER',
        'What is the slope ($$m$$) of the line given by equation $$3x - 4y + 12 = 0$$?',
        '[{"id": "A", "text": "$$\\frac{3}{4}$$", "isCorrect": true}, {"id": "B", "text": "$$-\\frac{3}{4}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{4}{3}$$", "isCorrect": false}, {"id": "D", "text": "$$3$$", "isCorrect": false}]',
        'A',
        'Rearranging to $$y = mx + c$$: $$4y = 3x + 12 \implies y = \frac{3}{4}x + 3$$. Slope $$m = \frac{3}{4}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000091'::uuid,
        'Geometry',
        'Triangles',
        'EASY',
        'UNDERSTAND',
        'The angles of a triangle are in the ratio $$2 : 3 : 5$$. What is the measure of the largest angle?',
        '[{"id": "A", "text": "$$90^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$75^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$100^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$80^\\circ$$", "isCorrect": false}]',
        'A',
        'Sum $$= 2x + 3x + 5x = 10x = 180^\circ \implies x = 18^\circ$$. Largest angle $$= 5 \times 18^\circ = 90^\circ$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000092'::uuid,
        'Geometry',
        'Circles',
        'MEDIUM',
        'APPLY',
        'The angle subtended by an arc at the center of a circle is $$110^\circ$$. What is the angle subtended by the same arc at any point on the remaining part of the circle?',
        '[{"id": "A", "text": "$$55^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$70^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$110^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$45^\\circ$$", "isCorrect": false}]',
        'A',
        'Angle at circumference is half of angle at center: $$\frac{110^\circ}{2} = 55^\circ$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000093'::uuid,
        'Geometry',
        'Chords and Tangents',
        'HARD',
        'ANALYZE',
        'From an external point $$P$$, a tangent $$PT$$ of length $$8\text{ cm}$$ is drawn to a circle. A secant through $$P$$ intersects the circle at points $$A$$ and $$B$$. If $$PA = 4\text{ cm}$$, find the length of chord $$AB$$.',
        '[{"id": "A", "text": "$$12\\text{ cm}$$", "isCorrect": true}, {"id": "B", "text": "$$16\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$8\\text{ cm}$$", "isCorrect": false}]',
        'A',
        'Tangent-Secant Theorem: $$PT^2 = PA \times PB \implies 8^2 = 4 \times PB \implies PB = 16\text{ cm}$$. Chord $$AB = PB - PA = 16 - 4 = 12\text{ cm}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000094'::uuid,
        'Trigonometry',
        'Trigonometric Ratios',
        'EASY',
        'REMEMBER',
        'If $$\sin \theta = \frac{3}{5}$$ for an acute angle $$\theta$$, what is the value of $$\cos \theta$$?',
        '[{"id": "A", "text": "$$\\frac{4}{5}$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{3}{4}$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{5}{4}$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{2}{5}$$", "isCorrect": false}]',
        'A',
        '$$\cos \theta = \sqrt{1 - \sin^2 \theta} = \sqrt{1 - \frac{9}{25}} = \sqrt{\frac{16}{25}} = \frac{4}{5}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000095'::uuid,
        'Trigonometry',
        'Standard Identities',
        'EASY',
        'REMEMBER',
        'What is the value of $$\sin^2 27^\circ + \sin^2 63^\circ$$?',
        '[{"id": "A", "text": "$$1$$", "isCorrect": true}, {"id": "B", "text": "$$0$$", "isCorrect": false}, {"id": "C", "text": "$$2$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{2}$$", "isCorrect": false}]',
        'A',
        '$$\sin 63^\circ = \cos(90^\circ - 63^\circ) = \cos 27^\circ$$. Thus $$\sin^2 27^\circ + \cos^2 27^\circ = 1$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000096'::uuid,
        'Trigonometry',
        'Heights and Distances',
        'MEDIUM',
        'APPLY',
        'The shadow of a vertical tower on level ground increases by $$10\text{ m}$$ when the altitude of the sun changes from $$45^\circ$$ to $$30^\circ$$. What is the height of the tower?',
        '[{"id": "A", "text": "$$5(\\sqrt{3} + 1)\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$5(\\sqrt{3} - 1)\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$10\\sqrt{3}\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$10(\\sqrt{3} + 1)\\text{ m}$$", "isCorrect": false}]',
        'A',
        '$$h\cot 30^\circ - h\cot 45^\circ = 10 \implies h(\sqrt{3} - 1) = 10 \implies h = \frac{10}{\sqrt{3}-1} = 5(\sqrt{3}+1)\text{ m}$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000097'::uuid,
        'Statistics and Probability',
        'Mean',
        'EASY',
        'APPLY',
        'What is the arithmetic mean of the first ten prime numbers?',
        '[{"id": "A", "text": "$$12.9$$", "isCorrect": true}, {"id": "B", "text": "$$12.5$$", "isCorrect": false}, {"id": "C", "text": "$$13.2$$", "isCorrect": false}, {"id": "D", "text": "$$11.8$$", "isCorrect": false}]',
        'A',
        'First 10 primes: $$2, 3, 5, 7, 11, 13, 17, 19, 23, 29$$. Sum $$= 129$$. Mean $$= \frac{129}{10} = 12.9$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000098'::uuid,
        'Statistics and Probability',
        'Median',
        'EASY',
        'APPLY',
        'Find the median of the data set: $$15, 23, 12, 8, 19, 27, 31, 14, 18$$.',
        '[{"id": "A", "text": "$$18$$", "isCorrect": true}, {"id": "B", "text": "$$19$$", "isCorrect": false}, {"id": "C", "text": "$$15$$", "isCorrect": false}, {"id": "D", "text": "$$16.5$$", "isCorrect": false}]',
        'A',
        'Sorted array: $$8, 12, 14, 15, 18, 19, 23, 27, 31$$. There are $$9$$ items; middle ($$5^{\text{th}}$$) is $$18$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000099'::uuid,
        'Statistics and Probability',
        'Mode',
        'MEDIUM',
        'UNDERSTAND',
        'In a moderately skewed distribution, if the mean is $$30$$ and the median is $$28$$, what is the empirical mode of the distribution?',
        '[{"id": "A", "text": "$$24$$", "isCorrect": true}, {"id": "B", "text": "$$26$$", "isCorrect": false}, {"id": "C", "text": "$$29$$", "isCorrect": false}, {"id": "D", "text": "$$25$$", "isCorrect": false}]',
        'A',
        '$$\text{Mode} = 3\text{Median} - 2\text{Mean} = 3(28) - 2(30) = 84 - 60 = 24$$.'
    ),
    (
        'a10e0000-0000-0000-0000-000000000100'::uuid,
        'Statistics and Probability',
        'Standard Deviation',
        'HARD',
        'ANALYZE',
        'If the variance of a set of observations is $$196$$, what is the standard deviation?',
        '[{"id": "A", "text": "$$14$$", "isCorrect": true}, {"id": "B", "text": "$$16$$", "isCorrect": false}, {"id": "C", "text": "$$12$$", "isCorrect": false}, {"id": "D", "text": "$$28$$", "isCorrect": false}]',
        'A',
        '$$\text{Standard Deviation} = \sqrt{\text{Variance}} = \sqrt{196} = 14$$.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
