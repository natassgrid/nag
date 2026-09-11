-- ============================================================
-- Seed Questions: RRB NTPC - General Science (Physics) (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Science -> Topic: Physics
-- Syllabus Source: https://www.pw.live/railway/exams/rrb-syllabus (10th NCERT/CBSE Level)
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX math ($$..$$)
-- UUID Range: a10f0000-0000-0000-0000-000000000001 to a10f0000-0000-0000-0000-000000000100
-- ============================================================

-- Step 1: Ensure required topic and subtopics exist under 'General Science'
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, 'Physics', 'Fundamental and applied physics concepts'
FROM question_service.subject s
WHERE s.name = 'General Science' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
SELECT 'default', t.id, v.name, v.description
FROM question_service.topic t
JOIN question_service.subject s ON s.id = t.subject_id AND s.name = 'General Science'
CROSS JOIN (VALUES
    ('Units and Measurements', 'SI units, dimensions, and measuring instruments'),
    ('Kinematics and Motion', 'Speed, velocity, acceleration and equations of motion'),
    ('Laws of Motion and Force', 'Newton laws, inertia, momentum and friction'),
    ('Work, Energy and Power', 'Work, kinetic and potential energy, conservation and power'),
    ('Gravitation', 'Universal law of gravitation, g variation, Kepler laws and escape velocity'),
    ('Fluids and Properties of Matter', 'Pressure, Pascals law, Archimedes principle and surface tension'),
    ('Heat and Thermodynamics', 'Temperature scales, thermal expansion, specific heat and heat transfer'),
    ('Waves and Sound', 'Wave characteristics, speed of sound, echo, SONAR and Doppler effect'),
    ('Optics and Light', 'Reflection, refraction, mirrors, lenses, dispersion and vision defects'),
    ('Electricity and Magnetism', 'Electric current, Ohms law, circuits, magnetism and induction')
) AS v(name, description)
WHERE t.name = 'Physics' AND t.tenant_id = 'default'
ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

-- Step 2: Insert 100 RRB NTPC General Science (Physics) Questions
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
    'General Science',
    'Physics',
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
        'a10f0000-0000-0000-0000-000000000001'::uuid,
        'Physics',
        'Units and Measurements',
        'EASY',
        'REMEMBER',
        'What is the SI unit of electric current?',
        '[{"id": "A", "text": "Ampere ($$\\text{A}$$)", "isCorrect": true}, {"id": "B", "text": "Volt ($$\\text{V}$$)", "isCorrect": false}, {"id": "C", "text": "Ohm ($$\\Omega$$)", "isCorrect": false}, {"id": "D", "text": "Coulomb ($$\\text{C}$$)", "isCorrect": false}]',
        'A',
        'The SI base unit of electric current is the Ampere ($$\text{A}$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000002'::uuid,
        'Physics',
        'Units and Measurements',
        'EASY',
        'REMEMBER',
        'Which of the following is a fundamental SI base quantity?',
        '[{"id": "A", "text": "Luminous intensity", "isCorrect": true}, {"id": "B", "text": "Velocity", "isCorrect": false}, {"id": "C", "text": "Force", "isCorrect": false}, {"id": "D", "text": "Pressure", "isCorrect": false}]',
        'A',
        'Luminous intensity (unit: Candela, $$\text{cd}$$) is one of the seven base SI quantities. Force, pressure, and velocity are derived quantities.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000003'::uuid,
        'Physics',
        'Units and Measurements',
        'EASY',
        'REMEMBER',
        'One light year is a unit of which physical quantity?',
        '[{"id": "A", "text": "Distance", "isCorrect": true}, {"id": "B", "text": "Time", "isCorrect": false}, {"id": "C", "text": "Speed", "isCorrect": false}, {"id": "D", "text": "Intensity of light", "isCorrect": false}]',
        'A',
        'A light year is the astronomical distance that light travels in vacuum in one Julian year ($$\approx 9.46 \times 10^{15}\text{ m}$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000004'::uuid,
        'Physics',
        'Units and Measurements',
        'MEDIUM',
        'UNDERSTAND',
        'What is the SI unit of luminous flux?',
        '[{"id": "A", "text": "Lumen ($$\\text{lm}$$)", "isCorrect": true}, {"id": "B", "text": "Lux ($$\\text{lx}$$)", "isCorrect": false}, {"id": "C", "text": "Candela ($$\\text{cd}$$)", "isCorrect": false}, {"id": "D", "text": "Watt ($$\\text{W}$$)", "isCorrect": false}]',
        'A',
        'Lumen ($$\text{lm}$$) is the SI derived unit of luminous flux. Lux ($$\text{lx}$$) is the unit of illuminance ($$\text{lm/m}^2$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000005'::uuid,
        'Physics',
        'Units and Measurements',
        'MEDIUM',
        'REMEMBER',
        'Which scientific instrument is used to measure atmospheric pressure?',
        '[{"id": "A", "text": "Barometer", "isCorrect": true}, {"id": "B", "text": "Hydrometer", "isCorrect": false}, {"id": "C", "text": "Hygrometer", "isCorrect": false}, {"id": "D", "text": "Anemometer", "isCorrect": false}]',
        'A',
        'A barometer (invented by Evangelista Torricelli) measures atmospheric pressure. Hydrometers measure liquid density, hygrometers measure humidity, and anemometers measure wind speed.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000006'::uuid,
        'Physics',
        'Units and Measurements',
        'MEDIUM',
        'UNDERSTAND',
        'Which instrument is used to measure very high temperatures, such as the temperature of a blast furnace or the Sun?',
        '[{"id": "A", "text": "Pyrometer", "isCorrect": true}, {"id": "B", "text": "Bolometer", "isCorrect": false}, {"id": "C", "text": "Calorimeter", "isCorrect": false}, {"id": "D", "text": "Thermopile", "isCorrect": false}]',
        'A',
        'A pyrometer measures high temperatures by detecting thermal radiation emitted by a body without physical contact.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000007'::uuid,
        'Physics',
        'Units and Measurements',
        'HARD',
        'ANALYZE',
        'What is the dimensional formula for the Universal Gravitational Constant ($$G$$)?',
        '[{"id": "A", "text": "$$[\\text{M}^{-1} \\text{L}^3 \\text{T}^{-2}]$$", "isCorrect": true}, {"id": "B", "text": "$$[\\text{M}^1 \\text{L}^3 \\text{T}^{-2}]$$", "isCorrect": false}, {"id": "C", "text": "$$[\\text{M}^{-1} \\text{L}^2 \\text{T}^{-2}]$$", "isCorrect": false}, {"id": "D", "text": "$$[\\text{M}^0 \\text{L}^3 \\text{T}^{-1}]$$", "isCorrect": false}]',
        'A',
        'From Newton''s law: $$F = G \frac{m_1 m_2}{r^2} \implies G = \frac{F r^2}{m^2}$$. Dimensions: $$\frac{[\text{M} \text{L} \text{T}^{-2}][\text{L}^2]}{[\text{M}^2]} = [\text{M}^{-1} \text{L}^3 \text{T}^{-2}]$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000008'::uuid,
        'Physics',
        'Units and Measurements',
        'EASY',
        'REMEMBER',
        'One astronomical unit ($$\text{AU}$$) is defined as the average distance between:',
        '[{"id": "A", "text": "The Earth and the Sun", "isCorrect": true}, {"id": "B", "text": "The Earth and the Moon", "isCorrect": false}, {"id": "C", "text": "Jupiter and the Sun", "isCorrect": false}, {"id": "D", "text": "Pluto and the Sun", "isCorrect": false}]',
        'A',
        'One astronomical unit ($$\text{AU} \approx 1.496 \times 10^{11}\text{ m}$$) is the mean distance between the Earth and the Sun.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000009'::uuid,
        'Physics',
        'Units and Measurements',
        'MEDIUM',
        'UNDERSTAND',
        'What is the SI unit of magnetic flux?',
        '[{"id": "A", "text": "Weber ($$\\text{Wb}$$)", "isCorrect": true}, {"id": "B", "text": "Tesla ($$\\text{T}$$)", "isCorrect": false}, {"id": "C", "text": "Henry ($$\\text{H}$$)", "isCorrect": false}, {"id": "D", "text": "Gauss ($$\\text{G}$$)", "isCorrect": false}]',
        'A',
        'Weber ($$\text{Wb}$$) is the SI unit of magnetic flux ($$\Phi = B \cdot A$$). Tesla ($$\text{T}$$) is the unit of magnetic flux density.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000010'::uuid,
        'Physics',
        'Units and Measurements',
        'EASY',
        'REMEMBER',
        'Which device is used to convert mechanical energy into electrical energy?',
        '[{"id": "A", "text": "Dynamo / Electric Generator", "isCorrect": true}, {"id": "B", "text": "Electric Motor", "isCorrect": false}, {"id": "C", "text": "Transformer", "isCorrect": false}, {"id": "D", "text": "Inverter", "isCorrect": false}]',
        'A',
        'A dynamo (generator) operates on electromagnetic induction to convert mechanical energy into electrical energy, while an electric motor does the reverse.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000011'::uuid,
        'Physics',
        'Kinematics and Motion',
        'EASY',
        'REMEMBER',
        'Which of the following physical quantities is a scalar quantity?',
        '[{"id": "A", "text": "Speed", "isCorrect": true}, {"id": "B", "text": "Velocity", "isCorrect": false}, {"id": "C", "text": "Acceleration", "isCorrect": false}, {"id": "D", "text": "Displacement", "isCorrect": false}]',
        'A',
        'Speed has magnitude only and no specific direction, making it a scalar quantity. Velocity, acceleration, and displacement are vectors.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000012'::uuid,
        'Physics',
        'Kinematics and Motion',
        'EASY',
        'UNDERSTAND',
        'What does the slope of a velocity-time ($$v-t$$) graph represent?',
        '[{"id": "A", "text": "Acceleration", "isCorrect": true}, {"id": "B", "text": "Displacement", "isCorrect": false}, {"id": "C", "text": "Speed", "isCorrect": false}, {"id": "D", "text": "Distance travelled", "isCorrect": false}]',
        'A',
        'The slope of a velocity-time graph is $$\frac{\Delta v}{\Delta t}$$, which equals acceleration. The area under the graph represents displacement.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000013'::uuid,
        'Physics',
        'Kinematics and Motion',
        'MEDIUM',
        'APPLY',
        'A car accelerates uniformly from rest to a speed of $$20\text{ m/s}$$ in $$5\text{ seconds}$$. What is the distance travelled by the car during this time?',
        '[{"id": "A", "text": "$$50\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$100\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$40\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$25\\text{ m}$$", "isCorrect": false}]',
        'A',
        'Acceleration $$a = \frac{v - u}{t} = \frac{20 - 0}{5} = 4\text{ m/s}^2$$. Distance $$s = ut + \frac{1}{2}at^2 = 0 + \frac{1}{2}(4)(5^2) = 50\text{ m}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000014'::uuid,
        'Physics',
        'Kinematics and Motion',
        'MEDIUM',
        'APPLY',
        'A ball is thrown vertically upwards with a velocity of $$20\text{ m/s}$$. What is the maximum height attained by the ball? (Take $$g = 10\text{ m/s}^2$$)',
        '[{"id": "A", "text": "$$20\\text{ m}$$", "isCorrect": true}, {"id": "B", "text": "$$40\\text{ m}$$", "isCorrect": false}, {"id": "C", "text": "$$10\\text{ m}$$", "isCorrect": false}, {"id": "D", "text": "$$15\\text{ m}$$", "isCorrect": false}]',
        'A',
        'At maximum height, $$v = 0$$. Using $$v^2 = u^2 - 2gh \implies 0 = 20^2 - 2(10)h \implies 20h = 400 \implies h = 20\text{ m}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000015'::uuid,
        'Physics',
        'Kinematics and Motion',
        'EASY',
        'UNDERSTAND',
        'When an object moves in a circular path at a constant speed, its motion is called:',
        '[{"id": "A", "text": "Uniform circular motion with constant acceleration towards the center", "isCorrect": true}, {"id": "B", "text": "Motion with zero acceleration", "isCorrect": false}, {"id": "C", "text": "Motion with constant velocity", "isCorrect": false}, {"id": "D", "text": "Non-accelerated motion", "isCorrect": false}]',
        'A',
        'Even though speed is constant, the direction of velocity changes continuously, creating a centripetal acceleration ($$a = \frac{v^2}{r}$$) directed towards the center.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000016'::uuid,
        'Physics',
        'Kinematics and Motion',
        'MEDIUM',
        'UNDERSTAND',
        'At what angle with the horizontal should a projectile be launched to achieve maximum horizontal range on level ground?',
        '[{"id": "A", "text": "$$45^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$30^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$60^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$90^\\circ$$", "isCorrect": false}]',
        'A',
        'Range $$R = \frac{u^2 \sin 2\theta}{g}$$. Range is maximum when $$\sin 2\theta = 1 \implies 2\theta = 90^\circ \implies \theta = 45^\circ$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000017'::uuid,
        'Physics',
        'Kinematics and Motion',
        'HARD',
        'ANALYZE',
        'A bullet fired into a wooden plank loses half of its velocity after penetrating $$3\text{ cm}$$. How much further will it penetrate before coming to rest, assuming uniform retardation?',
        '[{"id": "A", "text": "$$1\\text{ cm}$$", "isCorrect": true}, {"id": "B", "text": "$$2\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$1.5\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$0.5\\text{ cm}$$", "isCorrect": false}]',
        'A',
        '$$v_1^2 = u^2 - 2as_1 \implies \frac{u^2}{4} = u^2 - 2a(3) \implies 6a = \frac{3}{4}u^2 \implies a = \frac{u^2}{8}$$. Total penetration $$s = \frac{u^2}{2a} = \frac{u^2}{2(u^2/8)} = 4\text{ cm}$$. Further distance $$= 4 - 3 = 1\text{ cm}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000018'::uuid,
        'Physics',
        'Kinematics and Motion',
        'EASY',
        'REMEMBER',
        'The odometer of an automobile measures which physical parameter?',
        '[{"id": "A", "text": "Distance travelled", "isCorrect": true}, {"id": "B", "text": "Instantaneous speed", "isCorrect": false}, {"id": "C", "text": "Average acceleration", "isCorrect": false}, {"id": "D", "text": "Fuel efficiency", "isCorrect": false}]',
        'A',
        'An odometer records total distance travelled, while a speedometer indicates instantaneous speed.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000019'::uuid,
        'Physics',
        'Kinematics and Motion',
        'MEDIUM',
        'APPLY',
        'A stone is dropped from a cliff of height $$45\text{ m}$$. How long does it take to reach the ground? (Take $$g = 10\text{ m/s}^2$$)',
        '[{"id": "A", "text": "$$3\\text{ seconds}$$", "isCorrect": true}, {"id": "B", "text": "$$4.5\\text{ seconds}$$", "isCorrect": false}, {"id": "C", "text": "$$2\\text{ seconds}$$", "isCorrect": false}, {"id": "D", "text": "$$5\\text{ seconds}$$", "isCorrect": false}]',
        'A',
        'For free fall from rest: $$h = \frac{1}{2}gt^2 \implies 45 = \frac{1}{2}(10)t^2 \implies 5t^2 = 45 \implies t^2 = 9 \implies t = 3\text{ seconds}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000020'::uuid,
        'Physics',
        'Kinematics and Motion',
        'HARD',
        'UNDERSTAND',
        'If the displacement of a particle is proportional to the square of time ($$s \propto t^2$$), the particle is moving with:',
        '[{"id": "A", "text": "Uniform acceleration", "isCorrect": true}, {"id": "B", "text": "Uniform velocity", "isCorrect": false}, {"id": "C", "text": "Increasing acceleration", "isCorrect": false}, {"id": "D", "text": "Decreasing acceleration", "isCorrect": false}]',
        'A',
        '$$s = k t^2 \implies v = \frac{ds}{dt} = 2kt \implies a = \frac{dv}{dt} = 2k = \text{constant}$$. Thus acceleration is uniform.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000021'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'EASY',
        'REMEMBER',
        'Newton''s first law of motion gives the qualitative definition of which concept?',
        '[{"id": "A", "text": "Inertia and Force", "isCorrect": true}, {"id": "B", "text": "Momentum", "isCorrect": false}, {"id": "C", "text": "Work", "isCorrect": false}, {"id": "D", "text": "Energy", "isCorrect": false}]',
        'A',
        'Newton''s first law (the Law of Inertia) states that a body remains in its state of rest or uniform motion unless acted upon by an external unbalanced force.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000022'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'EASY',
        'UNDERSTAND',
        'When a passenger is standing in a stationary bus that suddenly starts moving forward, the passenger tends to fall backward due to:',
        '[{"id": "A", "text": "Inertia of rest", "isCorrect": true}, {"id": "B", "text": "Inertia of motion", "isCorrect": false}, {"id": "C", "text": "Inertia of direction", "isCorrect": false}, {"id": "D", "text": "Gravitational pull", "isCorrect": false}]',
        'A',
        'The lower part of the body moves forward with the bus, while the upper body tends to remain at rest due to inertia of rest.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000023'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'MEDIUM',
        'APPLY',
        'A constant force of $$50\text{ N}$$ acts on a body of mass $$10\text{ kg}$$ initially at rest. What velocity does the body acquire after $$4\text{ seconds}$$?',
        '[{"id": "A", "text": "$$20\\text{ m/s}$$", "isCorrect": true}, {"id": "B", "text": "$$10\\text{ m/s}$$", "isCorrect": false}, {"id": "C", "text": "$$25\\text{ m/s}$$", "isCorrect": false}, {"id": "D", "text": "$$15\\text{ m/s}$$", "isCorrect": false}]',
        'A',
        'Acceleration $$a = \frac{F}{m} = \frac{50}{10} = 5\text{ m/s}^2$$. Final velocity $$v = u + at = 0 + 5(4) = 20\text{ m/s}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000024'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'EASY',
        'UNDERSTAND',
        'Recoil of a gun when a bullet is fired is an application of which law?',
        '[{"id": "A", "text": "Conservation of linear momentum (Newton''s third law)", "isCorrect": true}, {"id": "B", "text": "Conservation of angular momentum", "isCorrect": false}, {"id": "C", "text": "Newton''s first law only", "isCorrect": false}, {"id": "D", "text": "Conservation of energy only", "isCorrect": false}]',
        'A',
        'By conservation of linear momentum, the forward momentum of the bullet equals the backward momentum of the gun ($$m_1 v_1 = -m_2 v_2$$), directly manifesting Newton''s third law (action-reaction).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000025'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'MEDIUM',
        'UNDERSTAND',
        'Why does a cricket fielder pull his hands backwards while catching a fast-moving ball?',
        '[{"id": "A", "text": "To increase the contact time and thereby reduce the impact force", "isCorrect": true}, {"id": "B", "text": "To decrease the momentum of the ball to zero faster", "isCorrect": false}, {"id": "C", "text": "To increase the impulse exerted by the ball", "isCorrect": false}, {"id": "D", "text": "To reduce friction between the hands and the ball", "isCorrect": false}]',
        'A',
        'Impulse $$J = F \times \Delta t = \Delta p$$. By pulling hands back, $$\Delta t$$ increases, so the force $$F = \frac{\Delta p}{\Delta t}$$ exerted on the hands decreases.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000026'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'EASY',
        'REMEMBER',
        'Which type of friction has the lowest magnitude among the following?',
        '[{"id": "A", "text": "Rolling friction", "isCorrect": true}, {"id": "B", "text": "Sliding friction", "isCorrect": false}, {"id": "C", "text": "Static friction (limiting)", "isCorrect": false}, {"id": "D", "text": "Kinetic friction", "isCorrect": false}]',
        'A',
        'Rolling friction is significantly smaller than sliding or static friction, which is why ball bearings and wheels are used to minimize resistive losses.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000027'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'MEDIUM',
        'APPLY',
        'A body of mass $$5\text{ kg}$$ is resting on a horizontal surface where the coefficient of static friction is $$0.4$$. What is the minimum horizontal force required to just start moving the body? (Take $$g = 10\text{ m/s}^2$$)',
        '[{"id": "A", "text": "$$20\\text{ N}$$", "isCorrect": true}, {"id": "B", "text": "$$50\\text{ N}$$", "isCorrect": false}, {"id": "C", "text": "$$25\\text{ N}$$", "isCorrect": false}, {"id": "D", "text": "$$10\\text{ N}$$", "isCorrect": false}]',
        'A',
        'Normal reaction $$R = mg = 5 \times 10 = 50\text{ N}$$. Limiting friction $$f_s = \mu_s R = 0.4 \times 50 = 20\text{ N}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000028'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'HARD',
        'ANALYZE',
        'A rocket works on the principle of conservation of:',
        '[{"id": "A", "text": "Linear momentum", "isCorrect": true}, {"id": "B", "text": "Mass", "isCorrect": false}, {"id": "C", "text": "Angular momentum", "isCorrect": false}, {"id": "D", "text": "Energy only", "isCorrect": false}]',
        'A',
        'As high-velocity exhaust gases are ejected backward, the rocket gains equal forward momentum in accordance with the law of conservation of linear momentum.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000029'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'MEDIUM',
        'UNDERSTAND',
        'What is the apparent weight of a person of mass $$m$$ standing inside an elevator accelerating downwards with acceleration $$a$$ ($$a < g$$)?',
        '[{"id": "A", "text": "$$m(g - a)$$", "isCorrect": true}, {"id": "B", "text": "$$m(g + a)$$", "isCorrect": false}, {"id": "C", "text": "$$mg$$", "isCorrect": false}, {"id": "D", "text": "$$ma$$", "isCorrect": false}]',
        'A',
        'Equation of motion: $$mg - N = ma \implies N = m(g - a)$$. Apparent weight is less than actual weight.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000030'::uuid,
        'Physics',
        'Laws of Motion and Force',
        'HARD',
        'ANALYZE',
        'When a horse pulls a cart, what is the force that actually causes the horse and cart system to move forward?',
        '[{"id": "A", "text": "The horizontal reaction force exerted by the ground on the horse''s hooves", "isCorrect": true}, {"id": "B", "text": "The force exerted by the horse on the cart", "isCorrect": false}, {"id": "C", "text": "The force exerted by the cart on the horse", "isCorrect": false}, {"id": "D", "text": "The force exerted by the horse on the ground", "isCorrect": false}]',
        'A',
        'The horse pushes backward on the ground; by Newton''s third law, the ground exerts an equal and opposite forward reaction force on the horse, accelerating the whole system forward.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000031'::uuid,
        'Physics',
        'Work, Energy and Power',
        'EASY',
        'UNDERSTAND',
        'Under what condition is the work done by a force on an object zero?',
        '[{"id": "A", "text": "When the force is perpendicular to the direction of displacement ($$\\theta = 90^\\circ$$)", "isCorrect": true}, {"id": "B", "text": "When the force is in the same direction as displacement ($$\\theta = 0^\\circ$$)", "isCorrect": false}, {"id": "C", "text": "When the force is opposite to displacement ($$\\theta = 180^\\circ$$)", "isCorrect": false}, {"id": "D", "text": "When both force and displacement are non-zero and parallel", "isCorrect": false}]',
        'A',
        'Work $$W = F s \cos \theta$$. When $$\theta = 90^\circ$$, $$\cos 90^\circ = 0$$, so $$W = 0$$ (e.g. coolie carrying luggage on horizontal ground, work done by gravity is zero).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000032'::uuid,
        'Physics',
        'Work, Energy and Power',
        'EASY',
        'REMEMBER',
        'How many Joules are there in $$1\text{ kilowatt-hour (kWh)}$$?',
        '[{"id": "A", "text": "$$3.6 \\times 10^6\\text{ J}$$", "isCorrect": true}, {"id": "B", "text": "$$3.6 \\times 10^5\\text{ J}$$", "isCorrect": false}, {"id": "C", "text": "$$1.0 \\times 10^3\\text{ J}$$", "isCorrect": false}, {"id": "D", "text": "$$3.6 \\times 10^3\\text{ J}$$", "isCorrect": false}]',
        'A',
        '$$1\text{ kWh} = 1000\text{ W} \times 3600\text{ s} = 3.6 \times 10^6\text{ Joules}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000033'::uuid,
        'Physics',
        'Work, Energy and Power',
        'MEDIUM',
        'APPLY',
        'If the momentum of a moving body is doubled, by what factor does its kinetic energy increase?',
        '[{"id": "A", "text": "$$4\\text{ times}$$", "isCorrect": true}, {"id": "B", "text": "$$2\\text{ times}$$", "isCorrect": false}, {"id": "C", "text": "$$8\\text{ times}$$", "isCorrect": false}, {"id": "D", "text": "$$16\\text{ times}$$", "isCorrect": false}]',
        'A',
        '$$E_k = \frac{p^2}{2m}$$. If momentum $$p$$ doubles ($$2p$$), new kinetic energy becomes $$\frac{(2p)^2}{2m} = 4 \frac{p^2}{2m} = 4 E_k$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000034'::uuid,
        'Physics',
        'Work, Energy and Power',
        'MEDIUM',
        'APPLY',
        'An electric crane lifts a load of $$1000\text{ kg}$$ to a height of $$30\text{ m}$$ in $$1\text{ minute}$$. What is the average power developed by the crane? (Take $$g = 10\text{ m/s}^2$$)',
        '[{"id": "A", "text": "$$5000\\text{ W}$$ ($$5\\text{ kW}$$)", "isCorrect": true}, {"id": "B", "text": "$$3000\\text{ W}$$ ($$3\\text{ kW}$$)", "isCorrect": false}, {"id": "C", "text": "$$6000\\text{ W}$$ ($$6\\text{ kW}$$)", "isCorrect": false}, {"id": "D", "text": "$$10000\\text{ W}$$ ($$10\\text{ kW}$$)", "isCorrect": false}]',
        'A',
        'Work $$W = mgh = 1000 \times 10 \times 30 = 300,000\text{ J}$$. Time $$t = 60\text{ s}$$. Power $$P = \frac{W}{t} = \frac{300,000}{60} = 5000\text{ W} = 5\text{ kW}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000035'::uuid,
        'Physics',
        'Work, Energy and Power',
        'EASY',
        'REMEMBER',
        'What is the commercial unit of electric power represented as Horsepower ($$\text{HP}$$) equal to in Watts?',
        '[{"id": "A", "text": "$$746\\text{ Watts}$$", "isCorrect": true}, {"id": "B", "text": "$$750\\text{ Watts}$$", "isCorrect": false}, {"id": "C", "text": "$$1000\\text{ Watts}$$", "isCorrect": false}, {"id": "D", "text": "$$500\\text{ Watts}$$", "isCorrect": false}]',
        'A',
        '$$1\text{ Mechanical Horsepower (HP)} = 746\text{ Watts}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000036'::uuid,
        'Physics',
        'Work, Energy and Power',
        'MEDIUM',
        'UNDERSTAND',
        'When a spring is compressed or stretched by a distance $$x$$, what form of potential energy is stored in it?',
        '[{"id": "A", "text": "Elastic potential energy ($$U = \\frac{1}{2} k x^2$$)", "isCorrect": true}, {"id": "B", "text": "Gravitational potential energy", "isCorrect": false}, {"id": "C", "text": "Kinetic energy", "isCorrect": false}, {"id": "D", "text": "Chemical energy", "isCorrect": false}]',
        'A',
        'Work done against the restoring force is stored as elastic potential energy, given by $$U = \frac{1}{2}kx^2$$, where $$k$$ is the spring constant.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000037'::uuid,
        'Physics',
        'Work, Energy and Power',
        'HARD',
        'ANALYZE',
        'A body of mass $$2\text{ kg}$$ falls freely from a height of $$20\text{ m}$$. What is its kinetic energy when it has fallen through a distance of $$15\text{ m}$$? (Take $$g = 10\text{ m/s}^2$$)',
        '[{"id": "A", "text": "$$300\\text{ J}$$", "isCorrect": true}, {"id": "B", "text": "$$400\\text{ J}$$", "isCorrect": false}, {"id": "C", "text": "$$100\\text{ J}$$", "isCorrect": false}, {"id": "D", "text": "$$200\\text{ J}$$", "isCorrect": false}]',
        'A',
        'Loss in potential energy equals gain in kinetic energy: $$\Delta E_k = mg \Delta h = 2 \times 10 \times 15 = 300\text{ J}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000038'::uuid,
        'Physics',
        'Work, Energy and Power',
        'EASY',
        'UNDERSTAND',
        'Which of the following energy transformations takes place in a hydroelectric power station?',
        '[{"id": "A", "text": "Potential energy of water $$\\to$$ Kinetic energy $$\\to$$ Electrical energy", "isCorrect": true}, {"id": "B", "text": "Chemical energy $$\\to$$ Heat energy $$\\to$$ Electrical energy", "isCorrect": false}, {"id": "C", "text": "Electrical energy $$\\to$$ Mechanical energy", "isCorrect": false}, {"id": "D", "text": "Nuclear energy $$\\to$$ Heat energy $$\\to$$ Electricity", "isCorrect": false}]',
        'A',
        'Water stored in a dam has potential energy, which turns into kinetic energy when falling to drive turbines, generating electrical energy.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000039'::uuid,
        'Physics',
        'Work, Energy and Power',
        'MEDIUM',
        'APPLY',
        'What is the work done in stopping a moving car of mass $$1000\text{ kg}$$ travelling at a speed of $$72\text{ km/h}$$?',
        '[{"id": "A", "text": "$$2 \\times 10^5\\text{ J}$$", "isCorrect": true}, {"id": "B", "text": "$$4 \\times 10^5\\text{ J}$$", "isCorrect": false}, {"id": "C", "text": "$$1 \\times 10^5\\text{ J}$$", "isCorrect": false}, {"id": "D", "text": "$$5 \\times 10^4\\text{ J}$$", "isCorrect": false}]',
        'A',
        'Speed $$v = 72 \times \frac{5}{18} = 20\text{ m/s}$$. By Work-Energy Theorem: $$W = \Delta E_k = \frac{1}{2}mv^2 = \frac{1}{2}(1000)(20^2) = 200,000\text{ J} = 2 \times 10^5\text{ J}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000040'::uuid,
        'Physics',
        'Work, Energy and Power',
        'HARD',
        'ANALYZE',
        'If the velocity of a moving object increases by $$50\%$$, by what percentage does its kinetic energy increase?',
        '[{"id": "A", "text": "$$125\\%$$", "isCorrect": true}, {"id": "B", "text": "$$100\\%$$", "isCorrect": false}, {"id": "C", "text": "$$150\\%$$", "isCorrect": false}, {"id": "D", "text": "$$75\\%$$", "isCorrect": false}]',
        'A',
        'New velocity $$v'' = 1.5v$$. New kinetic energy $$E''_k = \frac{1}{2}m(1.5v)^2 = 2.25 E_k$$. Percentage increase $$= \frac{2.25 - 1}{1} \times 100 = 125\%$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000041'::uuid,
        'Physics',
        'Gravitation',
        'EASY',
        'REMEMBER',
        'What is the standard value of the acceleration due to gravity ($$g$$) on the surface of the Earth?',
        '[{"id": "A", "text": "$$9.8\\text{ m/s}^2$$", "isCorrect": true}, {"id": "B", "text": "$$6.67 \\times 10^{-11}\\text{ m/s}^2$$", "isCorrect": false}, {"id": "C", "text": "$$8.9\\text{ m/s}^2$$", "isCorrect": false}, {"id": "D", "text": "$$11.2\\text{ m/s}^2$$", "isCorrect": false}]',
        'A',
        'The standard acceleration due to gravity on Earth''s surface at sea level is approximately $$9.8\text{ m/s}^2$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000042'::uuid,
        'Physics',
        'Gravitation',
        'EASY',
        'UNDERSTAND',
        'Where is the acceleration due to gravity ($$g$$) maximum on the surface of the Earth?',
        '[{"id": "A", "text": "At the Poles", "isCorrect": true}, {"id": "B", "text": "At the Equator", "isCorrect": false}, {"id": "C", "text": "At the Center of the Earth", "isCorrect": false}, {"id": "D", "text": "At latitude $$45^\\circ$$", "isCorrect": false}]',
        'A',
        'Because Earth is an oblate spheroid flattened at poles ($$R_{\text{polar}} < R_{\text{equatorial}}$$) and centrifugal force is zero at poles, $$g$$ is maximum at the poles.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000043'::uuid,
        'Physics',
        'Gravitation',
        'MEDIUM',
        'UNDERSTAND',
        'What is the acceleration due to gravity at the exact center of the Earth?',
        '[{"id": "A", "text": "Zero ($$0$$)", "isCorrect": true}, {"id": "B", "text": "$$9.8\\text{ m/s}^2$$", "isCorrect": false}, {"id": "C", "text": "Infinite", "isCorrect": false}, {"id": "D", "text": "$$4.9\\text{ m/s}^2$$", "isCorrect": false}]',
        'A',
        'At depth $$d = R$$, $$g'' = g\left(1 - \frac{d}{R}\right) = g(1 - 1) = 0$$. A body at the Earth''s center is completely weightless.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000044'::uuid,
        'Physics',
        'Gravitation',
        'EASY',
        'REMEMBER',
        'What is the value of acceleration due to gravity on the Moon compared to Earth?',
        '[{"id": "A", "text": "$$\\frac{1}{6}\\text{th}$$ of Earth''s $$g$$", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{4}\\text{th}$$ of Earth''s $$g$$", "isCorrect": false}, {"id": "C", "text": "$$\\frac{1}{2}\\text{nd}$$ of Earth''s $$g$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{8}\\text{th}$$ of Earth''s $$g$$", "isCorrect": false}]',
        'A',
        'The gravitational acceleration on the lunar surface is approximately $$\frac{1}{6}$$ of that on Earth ($$\approx 1.63\text{ m/s}^2$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000045'::uuid,
        'Physics',
        'Gravitation',
        'MEDIUM',
        'REMEMBER',
        'What is the escape velocity from the surface of the Earth?',
        '[{"id": "A", "text": "$$11.2\\text{ km/s}$$", "isCorrect": true}, {"id": "B", "text": "$$8.0\\text{ km/s}$$", "isCorrect": false}, {"id": "C", "text": "$$9.8\\text{ km/s}$$", "isCorrect": false}, {"id": "D", "text": "$$2.4\\text{ km/s}$$", "isCorrect": false}]',
        'A',
        'Escape velocity from Earth is $$v_e = \sqrt{2gR} \approx 11.2\text{ km/s}$$. Escape velocity on the Moon is $$\approx 2.38\text{ km/s}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000046'::uuid,
        'Physics',
        'Gravitation',
        'MEDIUM',
        'UNDERSTAND',
        'Kepler''s second law (Law of Areas) states that the line connecting a planet to the Sun sweeps out equal areas in equal intervals of time. This law is a direct consequence of:',
        '[{"id": "A", "text": "Conservation of angular momentum", "isCorrect": true}, {"id": "B", "text": "Conservation of linear momentum", "isCorrect": false}, {"id": "C", "text": "Conservation of mechanical energy", "isCorrect": false}, {"id": "D", "text": "Newton''s third law", "isCorrect": false}]',
        'A',
        'Because gravitational force is a central force, the torque on the planet about the Sun is zero ($\tau = 0$), so angular momentum $$L = m r v_\perp$$ is conserved.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000047'::uuid,
        'Physics',
        'Gravitation',
        'HARD',
        'APPLY',
        'If the distance between two masses is halved, how does the gravitational force between them change?',
        '[{"id": "A", "text": "It becomes $$4\\text{ times}$$ greater", "isCorrect": true}, {"id": "B", "text": "It is doubled", "isCorrect": false}, {"id": "C", "text": "It becomes half", "isCorrect": false}, {"id": "D", "text": "It remains unchanged", "isCorrect": false}]',
        'A',
        'From Newton''s universal law: $$F \propto \frac{1}{r^2}$$. If $$r \to \frac{r}{2}$$, $$F'' \propto \frac{1}{(r/2)^2} = \frac{4}{r^2} = 4F$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000048'::uuid,
        'Physics',
        'Gravitation',
        'EASY',
        'UNDERSTAND',
        'What is the orbital period of a geostationary satellite orbiting the Earth?',
        '[{"id": "A", "text": "$$24\\text{ hours}$$", "isCorrect": true}, {"id": "B", "text": "$$12\\text{ hours}$$", "isCorrect": false}, {"id": "C", "text": "$$365\\text{ days}$$", "isCorrect": false}, {"id": "D", "text": "$$90\\text{ minutes}$$", "isCorrect": false}]',
        'A',
        'A geostationary satellite revolves in the equatorial plane from west to east with a period of $$24\text{ hours}$$, matching Earth''s rotation period so it appears stationary.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000049'::uuid,
        'Physics',
        'Gravitation',
        'MEDIUM',
        'REMEMBER',
        'At what altitude above the Earth''s surface are geostationary satellites typically positioned?',
        '[{"id": "A", "text": "$$\\approx 36000\\text{ km}$$", "isCorrect": true}, {"id": "B", "text": "$$\\approx 6400\\text{ km}$$", "isCorrect": false}, {"id": "C", "text": "$$\\approx 400\\text{ km}$$", "isCorrect": false}, {"id": "D", "text": "$$\\approx 100000\\text{ km}$$", "isCorrect": false}]',
        'A',
        'Geostationary satellites are placed at an altitude of approximately $$35,786\text{ km}$$ (commonly rounded to $$36,000\text{ km}$$) above the equator.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000050'::uuid,
        'Physics',
        'Gravitation',
        'HARD',
        'ANALYZE',
        'If the radius of the Earth were to shrink by $$1\%$$ while its mass remains constant, the acceleration due to gravity on its surface would:',
        '[{"id": "A", "text": "Increase by approximately $$2\\%$$", "isCorrect": true}, {"id": "B", "text": "Decrease by approximately $$2\\%$$", "isCorrect": false}, {"id": "C", "text": "Increase by approximately $$1\\%$$", "isCorrect": false}, {"id": "D", "text": "Remain unchanged", "isCorrect": false}]',
        'A',
        '$$g = \frac{GM}{R^2}$$. Differentiating: $$\frac{\Delta g}{g} \approx -2\frac{\Delta R}{R}$$. If $$R$$ decreases by $$1\%$$, $$g$$ increases by $$-2(-1\%) = +2\%$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000051'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'EASY',
        'REMEMBER',
        'What is the SI unit of pressure?',
        '[{"id": "A", "text": "Pascal ($$\\text{Pa}$$ or $$\\text{N/m}^2$$)", "isCorrect": true}, {"id": "B", "text": "Joule ($$\\text{J}$$)", "isCorrect": false}, {"id": "C", "text": "Newton ($$\\text{N}$$)", "isCorrect": false}, {"id": "D", "text": "Bar", "isCorrect": false}]',
        'A',
        'Pressure is force per unit area. The SI unit is Pascal ($$1\text{ Pa} = 1\text{ N/m}^2$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000052'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'EASY',
        'UNDERSTAND',
        'The working principle of hydraulic brakes in automobiles is based on:',
        '[{"id": "A", "text": "Pascal''s Law", "isCorrect": true}, {"id": "B", "text": "Archimedes'' Principle", "isCorrect": false}, {"id": "C", "text": "Bernoulli''s Theorem", "isCorrect": false}, {"id": "D", "text": "Torricelli''s Law", "isCorrect": false}]',
        'A',
        'Pascal''s Law states that pressure applied to an enclosed fluid is transmitted undiminished in all directions, forming the basis of hydraulic lifts and hydraulic brakes.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000053'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'Archimedes'' Principle states that the buoyant force exerted on an object submerged in a fluid is equal to:',
        '[{"id": "A", "text": "The weight of the fluid displaced by the object", "isCorrect": true}, {"id": "B", "text": "The total weight of the submerged object", "isCorrect": false}, {"id": "C", "text": "The volume of the fluid displaced", "isCorrect": false}, {"id": "D", "text": "The density of the submerged object", "isCorrect": false}]',
        'A',
        'Buoyant force $$F_b = \rho_{\text{fluid}} V_{\text{displaced}} g$$, which equals the weight of the displaced fluid.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000054'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'Why do raindrops and soap bubbles assume a spherical shape?',
        '[{"id": "A", "text": "Due to surface tension which minimizes surface area", "isCorrect": true}, {"id": "B", "text": "Due to atmospheric pressure pushing equally from outside", "isCorrect": false}, {"id": "C", "text": "Due to gravitational attraction towards the droplet center", "isCorrect": false}, {"id": "D", "text": "Due to viscous drag of air", "isCorrect": false}]',
        'A',
        'Surface tension pulls liquid molecules together to minimize surface area for a given volume. The geometrical shape with minimum surface area is a sphere.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000055'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'EASY',
        'REMEMBER',
        'At what temperature does pure water have its maximum density?',
        '[{"id": "A", "text": "$$4^\\circ\\text{C}$$ ($$277\\text{ K}$$)", "isCorrect": true}, {"id": "B", "text": "$$0^\\circ\\text{C}$$ ($$273\\text{ K}$$)", "isCorrect": false}, {"id": "C", "text": "$$100^\\circ\\text{C}$$ ($$373\\text{ K}$$)", "isCorrect": false}, {"id": "D", "text": "$$-4^\\circ\\text{C}$$", "isCorrect": false}]',
        'A',
        'Due to anomalous expansion of water, water contracts as it is warmed from $$0^\circ\text{C}$$ to $$4^\circ\text{C}$$, reaching its maximum density of $$1000\text{ kg/m}^3$$ at $$4^\circ\text{C}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000056'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'When an ice cube floating in a glass of water melts completely, the water level in the glass:',
        '[{"id": "A", "text": "Remains unchanged", "isCorrect": true}, {"id": "B", "text": "Rises", "isCorrect": false}, {"id": "C", "text": "Falls", "isCorrect": false}, {"id": "D", "text": "First rises and then falls", "isCorrect": false}]',
        'A',
        'A floating ice cube displaces a volume of water whose weight equals the weight of the ice. When melted, the resulting water has exactly the same volume as the displaced water, so the level remains unchanged.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000057'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'HARD',
        'UNDERSTAND',
        'Bernoulli''s Principle is based on the law of conservation of:',
        '[{"id": "A", "text": "Energy", "isCorrect": true}, {"id": "B", "text": "Momentum", "isCorrect": false}, {"id": "C", "text": "Mass", "isCorrect": false}, {"id": "D", "text": "Angular momentum", "isCorrect": false}]',
        'A',
        'Bernoulli''s equation ($$P + \frac{1}{2}\rho v^2 + \rho gh = \text{constant}$$) is an expression of the law of conservation of mechanical energy for an incompressible, non-viscous streamline fluid flow.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000058'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'The dynamic lift of an airplane wing and the spinning curve of a tennis ball (Magnus effect) are explained by:',
        '[{"id": "A", "text": "Bernoulli''s Theorem", "isCorrect": true}, {"id": "B", "text": "Stokes'' Law", "isCorrect": false}, {"id": "C", "text": "Kepler''s Law", "isCorrect": false}, {"id": "D", "text": "Hooke''s Law", "isCorrect": false}]',
        'A',
        'Higher airflow speed over the top of the cambered airfoil causes lower pressure above compared to below, creating dynamic lift per Bernoulli''s principle.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000059'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'EASY',
        'UNDERSTAND',
        'Kerosene oil rises up through the wick of a lantern due to the phenomenon of:',
        '[{"id": "A", "text": "Capillarity", "isCorrect": true}, {"id": "B", "text": "Viscosity", "isCorrect": false}, {"id": "C", "text": "Osmosis", "isCorrect": false}, {"id": "D", "text": "Diffusion", "isCorrect": false}]',
        'A',
        'The fine pores in the wick act as capillary tubes, causing oil to rise due to adhesive forces and surface tension (capillary action).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000060'::uuid,
        'Physics',
        'Fluids and Properties of Matter',
        'HARD',
        'ANALYZE',
        'A metallic sphere of radius $$r$$ falling through a viscous liquid reaches a steady velocity known as terminal velocity ($$v_t$$). According to Stokes'' Law, $$v_t$$ is proportional to:',
        '[{"id": "A", "text": "$$r^2$$", "isCorrect": true}, {"id": "B", "text": "$$r$$", "isCorrect": false}, {"id": "C", "text": "$$r^3$$", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{r}$$", "isCorrect": false}]',
        'A',
        'Equating gravity minus buoyancy to viscous drag ($$6\pi \eta r v_t$$) gives $$v_t = \frac{2}{9}\frac{r^2 (\rho - \sigma)g}{\eta}$$, meaning $$v_t \propto r^2$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000061'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'EASY',
        'REMEMBER',
        'At what temperature do the Celsius and Fahrenheit temperature scales show the exact same numerical reading?',
        '[{"id": "A", "text": "$$-40^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$0^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$40^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$-32^\\circ$$", "isCorrect": false}]',
        'A',
        'Using $$\frac{C}{5} = \frac{F-32}{9}$$, set $$C = F = x$$: $$\frac{x}{5} = \frac{x-32}{9} \implies 9x = 5x - 160 \implies 4x = -160 \implies x = -40^\circ$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000062'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'EASY',
        'REMEMBER',
        'What is the theoretical temperature known as Absolute Zero on the Celsius scale?',
        '[{"id": "A", "text": "$$-273.15^\\circ\\text{C}$$ ($$0\\text{ K}$$)", "isCorrect": true}, {"id": "B", "text": "$$0^\\circ\\text{C}$$", "isCorrect": false}, {"id": "C", "text": "$$-100^\\circ\\text{C}$$", "isCorrect": false}, {"id": "D", "text": "$$-373.15^\\circ\\text{C}$$", "isCorrect": false}]',
        'A',
        'Absolute zero ($$0\text{ Kelvin}$$) is $$-273.15^\circ\text{C}$$, the temperature at which molecular translational motion ceases.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000063'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'MEDIUM',
        'UNDERSTAND',
        'Which mode of heat transfer does not require any material medium for propagation?',
        '[{"id": "A", "text": "Radiation", "isCorrect": true}, {"id": "B", "text": "Conduction", "isCorrect": false}, {"id": "C", "text": "Convection", "isCorrect": false}, {"id": "D", "text": "Advection", "isCorrect": false}]',
        'A',
        'Thermal radiation travels via electromagnetic waves (infrared) and can propagate through a vacuum (e.g. solar energy reaching Earth).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000064'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'EASY',
        'UNDERSTAND',
        'Why does a pressure cooker cook food much faster than an open pot?',
        '[{"id": "A", "text": "Increased pressure inside raises the boiling point of water", "isCorrect": true}, {"id": "B", "text": "Higher pressure lowers the boiling point of water", "isCorrect": false}, {"id": "C", "text": "Heat cannot escape from the tightly sealed pot", "isCorrect": false}, {"id": "D", "text": "Food absorbs heat directly without boiling water", "isCorrect": false}]',
        'A',
        'At high pressure ($$\approx 2\text{ atm}$$), the boiling point of water increases to $$\approx 120^\circ\text{C}$$. Food cooks at this higher temperature, significantly accelerating the cooking process.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000065'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'MEDIUM',
        'UNDERSTAND',
        'Why do severe burns caused by steam at $$100^\circ\text{C}$$ feel much more painful and damaging than burns caused by boiling water at the same $$100^\circ\text{C}$$?',
        '[{"id": "A", "text": "Steam contains additional latent heat of vaporization ($$2.26 \\times 10^6\\text{ J/kg}$$)", "isCorrect": true}, {"id": "B", "text": "Steam has a higher temperature than boiling water", "isCorrect": false}, {"id": "C", "text": "Steam is a gas and penetrates skin deeper", "isCorrect": false}, {"id": "D", "text": "Boiling water cools down instantaneously on skin", "isCorrect": false}]',
        'A',
        'When steam condenses on skin, it releases an enormous amount of latent heat of vaporization ($$\approx 540\text{ cal/g}$$ or $$22.6 \times 10^5\text{ J/kg}$$) before cooling as water.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000066'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'MEDIUM',
        'APPLY',
        'How much heat energy is required to raise the temperature of $$2\text{ kg}$$ of water from $$20^\circ\text{C}$$ to $$70^\circ\text{C}$$? (Specific heat capacity of water $$= 4200\text{ J/(kg}\cdot^\circ\text{C)}$$)',
        '[{"id": "A", "text": "$$4.2 \\times 10^5\\text{ J}$$ ($$420\\text{ kJ}$$)", "isCorrect": true}, {"id": "B", "text": "$$2.1 \\times 10^5\\text{ J}$$ ($$210\\text{ kJ}$$)", "isCorrect": false}, {"id": "C", "text": "$$8.4 \\times 10^5\\text{ J}$$ ($$840\\text{ kJ}$$)", "isCorrect": false}, {"id": "D", "text": "$$5.0 \\times 10^5\\text{ J}$$ ($$500\\text{ kJ}$$)", "isCorrect": false}]',
        'A',
        '$$Q = mc\Delta T = 2 \times 4200 \times (70 - 20) = 2 \times 4200 \times 50 = 420,000\text{ J} = 4.2 \times 10^5\text{ J}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000067'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'HARD',
        'ANALYZE',
        'According to Stefan-Boltzmann Law, the total radiant energy emitted per second per unit area of a black body is proportional to:',
        '[{"id": "A", "text": "$$T^4$$", "isCorrect": true}, {"id": "B", "text": "$$T^2$$", "isCorrect": false}, {"id": "C", "text": "$$T^3$$", "isCorrect": false}, {"id": "D", "text": "$$T$$", "isCorrect": false}]',
        'A',
        'Stefan-Boltzmann Law states $$E = \sigma T^4$$, where $$T$$ is absolute temperature in Kelvin and $$\sigma$$ is Stefan''s constant.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000068'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'EASY',
        'UNDERSTAND',
        'Why are clinical thermometers provided with a small constriction (kink) near the bulb?',
        '[{"id": "A", "text": "To prevent the mercury thread from falling on its own when removed from the patient''s mouth", "isCorrect": true}, {"id": "B", "text": "To increase the sensitivity of temperature expansion", "isCorrect": false}, {"id": "C", "text": "To avoid breaking the glass tube under mouth pressure", "isCorrect": false}, {"id": "D", "text": "To allow easy sterilization in boiling water", "isCorrect": false}]',
        'A',
        'The kink prevents mercury from flowing back into the bulb when the thermometer is taken out, allowing accurate reading of patient temperature.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000069'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'MEDIUM',
        'UNDERSTAND',
        'Newton''s Law of Cooling states that the rate of loss of heat of a body is directly proportional to:',
        '[{"id": "A", "text": "The temperature difference between the body and its surroundings", "isCorrect": true}, {"id": "B", "text": "The square of the temperature of the body", "isCorrect": false}, {"id": "C", "text": "The mass of the cooling body", "isCorrect": false}, {"id": "D", "text": "The fourth power of absolute temperature difference", "isCorrect": false}]',
        'A',
        '$$\frac{dQ}{dt} = -k(T - T_0)$$, where $$(T - T_0)$$ is the excess temperature of the body over the surroundings.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000070'::uuid,
        'Physics',
        'Heat and Thermodynamics',
        'HARD',
        'ANALYZE',
        'What is the maximum theoretical efficiency of a heat engine operating between a source at $$400\text{ K}$$ and a sink at $$300\text{ K}$$?',
        '[{"id": "A", "text": "$$25\\%$$", "isCorrect": true}, {"id": "B", "text": "$$33.3\\%$$", "isCorrect": false}, {"id": "C", "text": "$$50\\%$$", "isCorrect": false}, {"id": "D", "text": "$$20\\%$$", "isCorrect": false}]',
        'A',
        'Carnot efficiency $$\eta = 1 - \frac{T_{\text{sink}}}{T_{\text{source}}} = 1 - \frac{300}{400} = 1 - 0.75 = 0.25 = 25\%$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000071'::uuid,
        'Physics',
        'Waves and Sound',
        'EASY',
        'REMEMBER',
        'Sound waves in air are classified as which type of waves?',
        '[{"id": "A", "text": "Longitudinal mechanical waves", "isCorrect": true}, {"id": "B", "text": "Transverse electromagnetic waves", "isCorrect": false}, {"id": "C", "text": "Transverse mechanical waves", "isCorrect": false}, {"id": "D", "text": "Stationary matter waves", "isCorrect": false}]',
        'A',
        'Sound waves in air propagate through compressions and rarefactions where air particles oscillate parallel to the direction of wave propagation, making them longitudinal mechanical waves.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000072'::uuid,
        'Physics',
        'Waves and Sound',
        'EASY',
        'REMEMBER',
        'What is the audible frequency range for the human ear?',
        '[{"id": "A", "text": "$$20\\text{ Hz}$$ to $$20000\\text{ Hz}$$", "isCorrect": true}, {"id": "B", "text": "$$20\\text{ Hz}$$ to $$2000\\text{ Hz}$$", "isCorrect": false}, {"id": "C", "text": "$$200\\text{ Hz}$$ to $$20000\\text{ Hz}$$", "isCorrect": false}, {"id": "D", "text": "$$2\\text{ Hz}$$ to $$20000\\text{ Hz}$$", "isCorrect": false}]',
        'A',
        'Human audible range is $$20\text{ Hz}$$ to $$20,000\text{ Hz}$$. Frequencies below $$20\text{ Hz}$$ are infrasonic; above $$20\text{ kHz}$$ are ultrasonic.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000073'::uuid,
        'Physics',
        'Waves and Sound',
        'MEDIUM',
        'UNDERSTAND',
        'Through which of the following media does sound travel with the greatest velocity?',
        '[{"id": "A", "text": "Steel (solid)", "isCorrect": true}, {"id": "B", "text": "Water (liquid)", "isCorrect": false}, {"id": "C", "text": "Air (gas)", "isCorrect": false}, {"id": "D", "text": "Vacuum", "isCorrect": false}]',
        'A',
        'Speed of sound depends on elasticity and density: $$v_{\text{solid}} > v_{\text{liquid}} > v_{\text{gas}}$$. In steel $$v \approx 5100\text{ m/s}$$, in water $$\approx 1480\text{ m/s}$$, in air $$\approx 343\text{ m/s}$$. Sound cannot travel in a vacuum.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000074'::uuid,
        'Physics',
        'Waves and Sound',
        'MEDIUM',
        'APPLY',
        'A sound wave has a frequency of $$440\text{ Hz}$$ and a wavelength of $$0.75\text{ m}$$. What is the speed of the wave?',
        '[{"id": "A", "text": "$$330\\text{ m/s}$$", "isCorrect": true}, {"id": "B", "text": "$$340\\text{ m/s}$$", "isCorrect": false}, {"id": "C", "text": "$$320\\text{ m/s}$$", "isCorrect": false}, {"id": "D", "text": "$$350\\text{ m/s}$$", "isCorrect": false}]',
        'A',
        'Wave speed $$v = f \lambda = 440 \times 0.75 = 330\text{ m/s}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000075'::uuid,
        'Physics',
        'Waves and Sound',
        'EASY',
        'UNDERSTAND',
        'What is the minimum distance required between a source of sound and an obstacle to hear a distinct echo in air at $$20^\circ\text{C}$$ (speed of sound $$= 344\text{ m/s}$$)?',
        '[{"id": "A", "text": "$$17.2\\text{ meters}$$", "isCorrect": true}, {"id": "B", "text": "$$34.4\\text{ meters}$$", "isCorrect": false}, {"id": "C", "text": "$$10.0\\text{ meters}$$", "isCorrect": false}, {"id": "D", "text": "$$25.0\\text{ meters}$$", "isCorrect": false}]',
        'A',
        'Persistence of hearing is $$0.1\text{ second}$$. Total round-trip distance $$2d = v \times t = 344 \times 0.1 = 34.4\text{ m} \implies d = 17.2\text{ m}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000076'::uuid,
        'Physics',
        'Waves and Sound',
        'MEDIUM',
        'REMEMBER',
        'What technology do bats and marine submarines use to detect obstacles and measure underwater depths?',
        '[{"id": "A", "text": "SONAR using ultrasonic waves", "isCorrect": true}, {"id": "B", "text": "RADAR using microwaves", "isCorrect": false}, {"id": "C", "text": "Infrasonic echolocation", "isCorrect": false}, {"id": "D", "text": "Optical laser telemetry", "isCorrect": false}]',
        'A',
        'SONAR (Sound Navigation and Ranging) utilizes ultrasound waves ($$> 20\text{ kHz}$$) because of their high frequency, short wavelength, and ability to penetrate deep water without excessive diffraction.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000077'::uuid,
        'Physics',
        'Waves and Sound',
        'EASY',
        'UNDERSTAND',
        'Which characteristic of a sound wave determines its loudness?',
        '[{"id": "A", "text": "Amplitude", "isCorrect": true}, {"id": "B", "text": "Frequency (Pitch)", "isCorrect": false}, {"id": "C", "text": "Wavelength", "isCorrect": false}, {"id": "D", "text": "Wave velocity", "isCorrect": false}]',
        'A',
        'Loudness is proportional to the square of amplitude ($$\text{Loudness} \propto A^2$$). Frequency determines pitch, and waveform determines quality/timbre.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000078'::uuid,
        'Physics',
        'Waves and Sound',
        'HARD',
        'ANALYZE',
        'When a train blows its whistle while approaching a platform observer, the observed pitch sounds higher than normal. This frequency shift is known as:',
        '[{"id": "A", "text": "The Doppler Effect", "isCorrect": true}, {"id": "B", "text": "The Tyndall Effect", "isCorrect": false}, {"id": "C", "text": "The Raman Effect", "isCorrect": false}, {"id": "D", "text": "The Zeeman Effect", "isCorrect": false}]',
        'A',
        'The apparent change in frequency due to relative motion between source and observer is the Doppler effect ($$f'' = f \frac{v}{v - v_s}$$ for an approaching source).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000079'::uuid,
        'Physics',
        'Waves and Sound',
        'MEDIUM',
        'UNDERSTAND',
        'Why does sound travel faster on a humid day than on a dry day?',
        '[{"id": "A", "text": "Water vapour reduces the density of air, increasing sound speed", "isCorrect": true}, {"id": "B", "text": "Water molecules absorb sound less than nitrogen", "isCorrect": false}, {"id": "C", "text": "Atmospheric pressure increases significantly in humidity", "isCorrect": false}, {"id": "D", "text": "Humid air increases the amplitude of sound", "isCorrect": false}]',
        'A',
        'Molar mass of water vapour ($$18\text{ g/mol}$$) is less than dry air ($$\approx 29\text{ g/mol}$$). Humid air is less dense ($$\rho$$ decreases), and since $$v = \sqrt{\frac{\gamma P}{\rho}}$$, speed of sound increases.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000080'::uuid,
        'Physics',
        'Waves and Sound',
        'HARD',
        'ANALYZE',
        'A SONAR device on a ship sends out an ultrasound pulse to the seabed and receives the echo $$1.6\text{ seconds}$$ later. If the speed of sound in seawater is $$1500\text{ m/s}$$, what is the depth of the sea?',
        '[{"id": "A", "text": "$$1200\\text{ meters}$$", "isCorrect": true}, {"id": "B", "text": "$$2400\\text{ meters}$$", "isCorrect": false}, {"id": "C", "text": "$$960\\text{ meters}$$", "isCorrect": false}, {"id": "D", "text": "$$1500\\text{ meters}$$", "isCorrect": false}]',
        'A',
        '$$2d = v \times t = 1500 \times 1.6 = 2400\text{ m} \implies d = \frac{2400}{2} = 1200\text{ m}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000081'::uuid,
        'Physics',
        'Optics and Light',
        'EASY',
        'REMEMBER',
        'What is the speed of light in vacuum?',
        '[{"id": "A", "text": "$$3 \\times 10^8\\text{ m/s}$$", "isCorrect": true}, {"id": "B", "text": "$$3 \\times 10^6\\text{ m/s}$$", "isCorrect": false}, {"id": "C", "text": "$$3 \\times 10^{10}\\text{ m/s}$$", "isCorrect": false}, {"id": "D", "text": "$$1.5 \\times 10^8\\text{ m/s}$$", "isCorrect": false}]',
        'A',
        'The speed of light in vacuum is $$c \approx 3 \times 10^8\text{ m/s}$$ (or $$300,000\text{ km/s}$$).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000082'::uuid,
        'Physics',
        'Optics and Light',
        'EASY',
        'UNDERSTAND',
        'Which type of mirror is commonly used as a rear-view mirror in motor vehicles?',
        '[{"id": "A", "text": "Convex mirror", "isCorrect": true}, {"id": "B", "text": "Concave mirror", "isCorrect": false}, {"id": "C", "text": "Plane mirror", "isCorrect": false}, {"id": "D", "text": "Parabolic mirror", "isCorrect": false}]',
        'A',
        'Convex mirrors always form erect, virtual, and diminished images, providing a much wider field of view for the driver.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000083'::uuid,
        'Physics',
        'Optics and Light',
        'MEDIUM',
        'UNDERSTAND',
        'Optical fibres used in modern telecommunications transmit light signals based on which phenomenon?',
        '[{"id": "A", "text": "Total Internal Reflection", "isCorrect": true}, {"id": "B", "text": "Diffuse Reflection", "isCorrect": false}, {"id": "C", "text": "Refraction only", "isCorrect": false}, {"id": "D", "text": "Polarization", "isCorrect": false}]',
        'A',
        'Light entering the core hits the cladding interface at an angle greater than the critical angle ($$i > \theta_c$$), undergoing repeated total internal reflection with virtually no loss.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000084'::uuid,
        'Physics',
        'Optics and Light',
        'MEDIUM',
        'APPLY',
        'What is the power of a convex lens having a focal length of $$+25\text{ cm}$$?',
        '[{"id": "A", "text": "$$+4.0\\text{ D}$$ (Dioptres)", "isCorrect": true}, {"id": "B", "text": "$$+0.25\\text{ D}$$", "isCorrect": false}, {"id": "C", "text": "$$-4.0\\text{ D}$$", "isCorrect": false}, {"id": "D", "text": "$$+2.5\\text{ D}$$", "isCorrect": false}]',
        'A',
        'Power $$P = \frac{1}{f\text{ (in meters)}} = \frac{1}{0.25\text{ m}} = +4.0\text{ D}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000085'::uuid,
        'Physics',
        'Optics and Light',
        'EASY',
        'UNDERSTAND',
        'A person suffering from Myopia (short-sightedness) is prescribed spectacles containing which lens?',
        '[{"id": "A", "text": "Concave lens (diverging lens)", "isCorrect": true}, {"id": "B", "text": "Convex lens (converging lens)", "isCorrect": false}, {"id": "C", "text": "Cylindrical lens", "isCorrect": false}, {"id": "D", "text": "Bifocal lens", "isCorrect": false}]',
        'A',
        'In myopia, parallel rays from a distant object focus in front of the retina. A diverging (concave) lens spreads the rays so they focus sharply on the retina.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000086'::uuid,
        'Physics',
        'Optics and Light',
        'EASY',
        'UNDERSTAND',
        'Why does the clear sky appear blue during the daytime?',
        '[{"id": "A", "text": "Rayleigh scattering of shorter wavelengths (blue light) by air molecules", "isCorrect": true}, {"id": "B", "text": "Refraction of sunlight in upper atmospheric layers", "isCorrect": false}, {"id": "C", "text": "Reflection of ocean water by the clouds", "isCorrect": false}, {"id": "D", "text": "Total internal reflection in air particles", "isCorrect": false}]',
        'A',
        'Rayleigh scattering intensity is inversely proportional to the fourth power of wavelength ($$I \propto \frac{1}{\lambda^4}$$). Blue light has a shorter wavelength than red light and is scattered much more strongly across the sky.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000087'::uuid,
        'Physics',
        'Optics and Light',
        'MEDIUM',
        'UNDERSTAND',
        'When white light passes through a glass triangular prism, which colour of the visible spectrum undergoes the maximum deviation?',
        '[{"id": "A", "text": "Violet", "isCorrect": true}, {"id": "B", "text": "Red", "isCorrect": false}, {"id": "C", "text": "Yellow", "isCorrect": false}, {"id": "D", "text": "Green", "isCorrect": false}]',
        'A',
        'Violet has the shortest wavelength and highest refractive index ($$\mu$$) in glass, so it bends the most. Red has the longest wavelength and bends the least.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000088'::uuid,
        'Physics',
        'Optics and Light',
        'MEDIUM',
        'APPLY',
        'An object is placed at a distance of $$20\text{ cm}$$ in front of a concave mirror of focal length $$15\text{ cm}$$. What is the image distance?',
        '[{"id": "A", "text": "$$-60\\text{ cm}$$ (real image)", "isCorrect": true}, {"id": "B", "text": "$$-30\\text{ cm}$$", "isCorrect": false}, {"id": "C", "text": "$$+60\\text{ cm}$$", "isCorrect": false}, {"id": "D", "text": "$$-45\\text{ cm}$$", "isCorrect": false}]',
        'A',
        'Mirror formula: $$\frac{1}{f} = \frac{1}{v} + \frac{1}{u}$$. Here $$f = -15\text{ cm}, u = -20\text{ cm}$$. $$\frac{1}{v} = -\frac{1}{15} - \left(-\frac{1}{20}\right) = -\frac{1}{15} + \frac{1}{20} = \frac{-4 + 3}{60} = -\frac{1}{60} \implies v = -60\text{ cm}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000089'::uuid,
        'Physics',
        'Optics and Light',
        'HARD',
        'ANALYZE',
        'What is the critical angle for a medium of refractive index $$\sqrt{2}$$ relative to air?',
        '[{"id": "A", "text": "$$45^\\circ$$", "isCorrect": true}, {"id": "B", "text": "$$30^\\circ$$", "isCorrect": false}, {"id": "C", "text": "$$60^\\circ$$", "isCorrect": false}, {"id": "D", "text": "$$90^\\circ$$", "isCorrect": false}]',
        'A',
        '$$\sin \theta_c = \frac{1}{\mu} = \frac{1}{\sqrt{2}} \implies \theta_c = 45^\circ$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000090'::uuid,
        'Physics',
        'Optics and Light',
        'HARD',
        'ANALYZE',
        'Why do stars twinkle at night when viewed from Earth, while planets do not?',
        '[{"id": "A", "text": "Continuous atmospheric refraction of light from point-sized stars through varying air layers", "isCorrect": true}, {"id": "B", "text": "Stars emit pulsating bursts of light continuously", "isCorrect": false}, {"id": "C", "text": "Interstellar dust blocks star light intermittently", "isCorrect": false}, {"id": "D", "text": "Diffraction through moisture droplets in the clouds", "isCorrect": false}]',
        'A',
        'Stars are point sources far away; atmospheric turbulence causes fluctuating refractive indices, causing the apparent position and brightness to flicker. Planets are extended sources where variations average out.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000091'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'EASY',
        'REMEMBER',
        'Ohm''s Law states that the current flowing through a conductor is directly proportional to the potential difference across its ends, provided that:',
        '[{"id": "A", "text": "Temperature and other physical conditions remain constant", "isCorrect": true}, {"id": "B", "text": "The length of the conductor continuously increases", "isCorrect": false}, {"id": "C", "text": "Magnetic fields are constantly applied", "isCorrect": false}, {"id": "D", "text": "Alternating current is used", "isCorrect": false}]',
        'A',
        'Ohm''s law ($$V = IR$$) holds true provided temperature and physical dimensions of the conductor remain constant.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000092'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'EASY',
        'APPLY',
        'Three resistors of resistances $$2\,\Omega, 3\,\Omega$$, and $$6\,\Omega$$ are connected in parallel. What is their equivalent resistance?',
        '[{"id": "A", "text": "$$1\\,\\Omega$$", "isCorrect": true}, {"id": "B", "text": "$$11\\,\\Omega$$", "isCorrect": false}, {"id": "C", "text": "$$2\\,\\Omega$$", "isCorrect": false}, {"id": "D", "text": "$$0.5\\,\\Omega$$", "isCorrect": false}]',
        'A',
        '$$\frac{1}{R_{\text{eq}}} = \frac{1}{2} + \frac{1}{3} + \frac{1}{6} = \frac{3 + 2 + 1}{6} = \frac{6}{6} = 1 \implies R_{\text{eq}} = 1\,\Omega$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000093'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'MEDIUM',
        'APPLY',
        'A wire of resistance $$R$$ is stretched uniformly so that its length becomes twice its original length. What is its new resistance?',
        '[{"id": "A", "text": "$$4R$$", "isCorrect": true}, {"id": "B", "text": "$$2R$$", "isCorrect": false}, {"id": "C", "text": "$$R/2$$", "isCorrect": false}, {"id": "D", "text": "$$R/4$$", "isCorrect": false}]',
        'A',
        'Since volume $$V = A \cdot l$$ is constant, doubling $$l$$ halves the cross-sectional area $$A$$ ($$A'' = A/2$$). $$R'' = \rho \frac{2l}{A/2} = 4\left(\rho \frac{l}{A}\right) = 4R$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000094'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'MEDIUM',
        'APPLY',
        'An electric iron rated at $$1000\text{ W}$$ operates for $$2\text{ hours}$$ daily. How many units (kWh) of electricity does it consume in $$30\text{ days}$$?',
        '[{"id": "A", "text": "$$60\\text{ units}$$", "isCorrect": true}, {"id": "B", "text": "$$30\\text{ units}$$", "isCorrect": false}, {"id": "C", "text": "$$120\\text{ units}$$", "isCorrect": false}, {"id": "D", "text": "$$50\\text{ units}$$", "isCorrect": false}]',
        'A',
        'Daily consumption $$= 1\text{ kW} \times 2\text{ h} = 2\text{ kWh}$$. In $$30\text{ days}$$: $$2 \times 30 = 60\text{ kWh} = 60\text{ units}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000095'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'EASY',
        'REMEMBER',
        'What material is electric fuse wire made of, and why?',
        '[{"id": "A", "text": "An alloy of Tin and Lead with high resistance and low melting point", "isCorrect": true}, {"id": "B", "text": "Pure copper with low resistance and high melting point", "isCorrect": false}, {"id": "C", "text": "Nichrome with very high melting point", "isCorrect": false}, {"id": "D", "text": "Tungsten with high tensile strength", "isCorrect": false}]',
        'A',
        'Fuse wire is made of a lead-tin alloy ($$63\%\text{ Sn}, 37\%\text{ Pb}$$) with a low melting point so it melts safely during current overload.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000096'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'EASY',
        'REMEMBER',
        'Why is tungsten metal used almost exclusively for the filament of incandescent electric lamps?',
        '[{"id": "A", "text": "Very high melting point ($$\\approx 3422^\\circ\\text{C}$$) and high resistivity", "isCorrect": true}, {"id": "B", "text": "Low cost and easy malleability", "isCorrect": false}, {"id": "C", "text": "Superconductivity at room temperature", "isCorrect": false}, {"id": "D", "text": "Zero electrical resistance", "isCorrect": false}]',
        'A',
        'Tungsten has the highest melting point ($$\approx 3422^\circ\text{C}$$) among metals, allowing it to glow white-hot without melting.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000097'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'MEDIUM',
        'UNDERSTAND',
        'Fleming''s Left-Hand Rule is used to determine the direction of:',
        '[{"id": "A", "text": "Magnetic force on a current-carrying conductor (Electric Motor principle)", "isCorrect": true}, {"id": "B", "text": "Induced electric current in a generator", "isCorrect": false}, {"id": "C", "text": "Magnetic field around a straight conductor", "isCorrect": false}, {"id": "D", "text": "Electric potential difference", "isCorrect": false}]',
        'A',
        'Fleming''s Left-Hand Rule gives the direction of mechanical force on a conductor in a magnetic field (Thumb: Force, Forefinger: Field, Middle finger: Current).'
    ),
    (
        'a10f0000-0000-0000-0000-000000000098'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'MEDIUM',
        'UNDERSTAND',
        'What is the direction of induced current determined by in electromagnetic induction?',
        '[{"id": "A", "text": "Lenz''s Law and Fleming''s Right-Hand Rule", "isCorrect": true}, {"id": "B", "text": "Ampere''s Circuital Law", "isCorrect": false}, {"id": "C", "text": "Fleming''s Left-Hand Rule", "isCorrect": false}, {"id": "D", "text": "Coulomb''s Inverse Square Law", "isCorrect": false}]',
        'A',
        'Lenz''s law states that induced current opposes the change that created it (conservation of energy), and Fleming''s Right-Hand Rule finds its direction in generators.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000099'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'HARD',
        'ANALYZE',
        'A step-down transformer has a primary to secondary turns ratio of $$10 : 1$$. If an AC voltage of $$220\text{ V}$$ is supplied to the primary coil, what is the output voltage at the secondary coil?',
        '[{"id": "A", "text": "$$22\\text{ V}$$", "isCorrect": true}, {"id": "B", "text": "$$2200\\text{ V}$$", "isCorrect": false}, {"id": "C", "text": "$$110\\text{ V}$$", "isCorrect": false}, {"id": "D", "text": "$$44\\text{ V}$$", "isCorrect": false}]',
        'A',
        '$$\frac{V_s}{V_p} = \frac{N_s}{N_p} \implies V_s = 220 \times \frac{1}{10} = 22\text{ V}$$.'
    ),
    (
        'a10f0000-0000-0000-0000-000000000100'::uuid,
        'Physics',
        'Electricity and Magnetism',
        'HARD',
        'UNDERSTAND',
        'Why does a compass needle get deflected when brought near a current-carrying wire?',
        '[{"id": "A", "text": "An electric current produces a magnetic field around the wire (Oersted''s discovery)", "isCorrect": true}, {"id": "B", "text": "Electrostatic attraction between moving electrons and compass needle", "isCorrect": false}, {"id": "C", "text": "Gravitational pull of the current", "isCorrect": false}, {"id": "D", "text": "Thermal expansion of the needle", "isCorrect": false}]',
        'A',
        'Hans Christian Oersted (1820) discovered that an electric current generates a magnetic field around itself, exerting a magnetic torque on the compass needle.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Science' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Physics' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
