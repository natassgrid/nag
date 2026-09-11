-- ============================================================
-- Seed Questions: RRB NTPC - General Science (Chemistry) (100 Questions)
-- Examination: RRB NTPC (Undergraduate & Graduate Posts)
-- Subject: General Science -> Topic: Chemistry
-- Syllabus Source: https://www.pw.live/railway/exams/rrb-syllabus (10th NCERT/CBSE Level)
-- Format Standard: Valid hex UUIDs, JSONB escaped, LaTeX math ($$..$$)
-- UUID Range: a1100000-0000-0000-0000-000000000001 to a1100000-0000-0000-0000-000000000100
-- ============================================================

-- Step 1: Ensure required topic and subtopics exist under 'General Science'
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, 'Chemistry', 'Fundamental chemical principles and everyday applications'
FROM question_service.subject s
WHERE s.name = 'General Science' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
SELECT 'default', t.id, v.name, v.description
FROM question_service.topic t
JOIN question_service.subject s ON s.id = t.subject_id AND s.name = 'General Science'
CROSS JOIN (VALUES
    ('Matter and States of Matter', 'States of matter, changes of state, colloids and solutions'),
    ('Atomic Structure and Mole Concept', 'Dalton theory, atomic mass, mole calculations and stoichiometry'),
    ('Subatomic Particles and Radioactivity', 'Electrons, protons, neutrons, atomic models and nuclear isotopes'),
    ('Periodic Classification of Elements', 'Mendeleev, modern periodic law, groups, periods and trends'),
    ('Chemical Reactions and Equations', 'Types of chemical reactions, redox, corrosion and rancidity'),
    ('Acids, Bases, Salts and pH Scale', 'Arrhenius acids and bases, pH scale, salts and indicators'),
    ('Metals, Non-Metals and Metallurgy', 'Physical and chemical properties, reactivity series, ores and alloys'),
    ('Carbon and its Compounds', 'Allotropes, hydrocarbons, functional groups, soaps and detergents'),
    ('Environmental Chemistry and Gases', 'Atmospheric layers, greenhouse effect, ozone layer and acid rain'),
    ('Everyday Chemistry and Polymers', 'Polymers, cement, glass, fertilizers, medicines and explosives')
) AS v(name, description)
WHERE t.name = 'Chemistry' AND t.tenant_id = 'default'
ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

-- Step 2: Insert 100 RRB NTPC General Science (Chemistry) Questions
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
    'Chemistry',
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
        'a1100000-0000-0000-0000-000000000001'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'EASY',
        'REMEMBER',
        'Which state of matter is considered the fourth state of matter, consisting of superheated, highly ionized gas?',
        '[{"id": "A", "text": "Plasma", "isCorrect": true}, {"id": "B", "text": "Bose-Einstein Condensate", "isCorrect": false}, {"id": "C", "text": "Superfluid", "isCorrect": false}, {"id": "D", "text": "Colloid", "isCorrect": false}]',
        'A',
        'Plasma consists of ionized gases with equal numbers of free electrons and positive ions (present in stars, lightning, and fluorescent tubes). The 5th state is Bose-Einstein Condensate.'
    ),
    (
        'a1100000-0000-0000-0000-000000000002'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'EASY',
        'REMEMBER',
        'What is the process called when a solid turns directly into vapour without passing through the liquid phase?',
        '[{"id": "A", "text": "Sublimation", "isCorrect": true}, {"id": "B", "text": "Evaporation", "isCorrect": false}, {"id": "C", "text": "Condensation", "isCorrect": false}, {"id": "D", "text": "Deposition", "isCorrect": false}]',
        'A',
        'Sublimation is the direct transition from solid to gas (e.g. camphor, dry ice, naphthalene, ammonium chloride). Direct transition from gas to solid is deposition.'
    ),
    (
        'a1100000-0000-0000-0000-000000000003'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'EASY',
        'REMEMBER',
        'Dry ice is the solid state of which chemical compound?',
        '[{"id": "A", "text": "Carbon dioxide ($$\\text{CO}_2$$)", "isCorrect": true}, {"id": "B", "text": "Sulfur dioxide ($$\\text{SO}_2$$)", "isCorrect": false}, {"id": "C", "text": "Carbon monoxide ($$\\text{CO}$$)", "isCorrect": false}, {"id": "D", "text": "Nitrous oxide ($$\\text{N}_2\\text{O}$$)", "isCorrect": false}]',
        'A',
        'Solid carbon dioxide is called ''dry ice'' because it sublimes directly to gas at $$-78.5^\circ\text{C}$$ without leaving any liquid residue.'
    ),
    (
        'a1100000-0000-0000-0000-000000000004'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'Why does the temperature of water remain constant at $$100^\circ\text{C}$$ during boiling, even when heat is continuously supplied?',
        '[{"id": "A", "text": "The heat supplied is consumed as latent heat of vaporization to break intermolecular bonds", "isCorrect": true}, {"id": "B", "text": "Heat escapes instantaneously to the atmosphere", "isCorrect": false}, {"id": "C", "text": "The container absorbs all excess heat", "isCorrect": false}, {"id": "D", "text": "Water molecules decompose into hydrogen and oxygen", "isCorrect": false}]',
        'A',
        'During phase transition, heat energy is utilized as latent heat of vaporization to overcome intermolecular forces of attraction, so temperature remains unchanged.'
    ),
    (
        'a1100000-0000-0000-0000-000000000005'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'Why does water kept in an earthen pot (matka) become cool during hot summer days?',
        '[{"id": "A", "text": "Continuous evaporation of water oozing through microscopic porous clay walls", "isCorrect": true}, {"id": "B", "text": "Clay acts as an active chemical refrigerant", "isCorrect": false}, {"id": "C", "text": "The earthen pot absorbs moisture from outside air", "isCorrect": false}, {"id": "D", "text": "Thermal conduction of clay is extremely high", "isCorrect": false}]',
        'A',
        'Pores in the clay allow water to seep to the surface and evaporate. Evaporation draws latent heat from the remaining water, producing a cooling effect.'
    ),
    (
        'a1100000-0000-0000-0000-000000000006'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'MEDIUM',
        'UNDERSTAND',
        'Which of the following factors increases the rate of evaporation of a liquid?',
        '[{"id": "A", "text": "Increase in surface area, temperature, and wind speed", "isCorrect": true}, {"id": "B", "text": "Increase in humidity and decrease in temperature", "isCorrect": false}, {"id": "C", "text": "Decrease in surface area and increase in air pressure", "isCorrect": false}, {"id": "D", "text": "Increase in humidity and increase in liquid volume", "isCorrect": false}]',
        'A',
        'Rate of evaporation increases with an increase in surface area, temperature, and wind speed, and decreases with an increase in ambient humidity.'
    ),
    (
        'a1100000-0000-0000-0000-000000000007'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'HARD',
        'ANALYZE',
        'At what conditions of temperature and pressure does a real gas behave most like an ideal gas?',
        '[{"id": "A", "text": "Low pressure and high temperature", "isCorrect": true}, {"id": "B", "text": "High pressure and low temperature", "isCorrect": false}, {"id": "C", "text": "High pressure and high temperature", "isCorrect": false}, {"id": "D", "text": "Low pressure and low temperature", "isCorrect": false}]',
        'A',
        'At low pressure, molecular volume is negligible compared to total volume. At high temperature, kinetic energy dominates over intermolecular attractions, adhering closely to $$PV = nRT$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000008'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'EASY',
        'UNDERSTAND',
        'The Brownian motion observed in colloidal solutions is caused by:',
        '[{"id": "A", "text": "Unbalanced bombardment of colloidal particles by molecules of the dispersion medium", "isCorrect": true}, {"id": "B", "text": "Gravitational settling of particles", "isCorrect": false}, {"id": "C", "text": "Electrostatic repulsion between identical charges", "isCorrect": false}, {"id": "D", "text": "Convection currents created by temperature gradients", "isCorrect": false}]',
        'A',
        'Brownian movement is the continuous random zig-zag motion of colloidal particles caused by uneven collisions with dispersion medium molecules.'
    ),
    (
        'a1100000-0000-0000-0000-000000000009'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'EASY',
        'REMEMBER',
        'Milk is an example of which type of colloidal system?',
        '[{"id": "A", "text": "Emulsion (liquid dispersed in liquid)", "isCorrect": true}, {"id": "B", "text": "Aerosol (liquid dispersed in gas)", "isCorrect": false}, {"id": "C", "text": "Gel (liquid dispersed in solid)", "isCorrect": false}, {"id": "D", "text": "Sol (solid dispersed in liquid)", "isCorrect": false}]',
        'A',
        'Milk is an oil-in-water emulsion where liquid butterfat globules are dispersed in an aqueous medium.'
    ),
    (
        'a1100000-0000-0000-0000-000000000010'::uuid,
        'Chemistry',
        'Matter and States of Matter',
        'HARD',
        'UNDERSTAND',
        'The scattering of a beam of light as it passes through a colloidal solution is known as:',
        '[{"id": "A", "text": "Tyndall Effect", "isCorrect": true}, {"id": "B", "text": "Raman Effect", "isCorrect": false}, {"id": "C", "text": "Zeeman Effect", "isCorrect": false}, {"id": "D", "text": "Compton Effect", "isCorrect": false}]',
        'A',
        'Tyndall effect is the scattering of light by colloidal particles, illuminating the path of light (e.g. sunlight streaming through dense forest canopy or mist).'
    ),
    (
        'a1100000-0000-0000-0000-000000000011'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'EASY',
        'REMEMBER',
        'What is the numerical value of Avogadro''s constant ($$N_A$$)?',
        '[{"id": "A", "text": "$$6.022 \\times 10^{23}\\text{ mol}^{-1}$$", "isCorrect": true}, {"id": "B", "text": "$$6.022 \\times 10^{22}\\text{ mol}^{-1}$$", "isCorrect": false}, {"id": "C", "text": "$$3.00 \\times 10^{8}\\text{ mol}^{-1}$$", "isCorrect": false}, {"id": "D", "text": "$$1.602 \\times 10^{-19}\\text{ mol}^{-1}$$", "isCorrect": false}]',
        'A',
        'Avogadro''s number is $$6.022 \times 10^{23}$$, representing the number of elementary entities in one mole of a substance.'
    ),
    (
        'a1100000-0000-0000-0000-000000000012'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'MEDIUM',
        'APPLY',
        'How many moles are present in $$44\text{ g}$$ of Carbon dioxide ($$\text{CO}_2$$)? (Atomic masses: $$\text{C} = 12\text{ u}, \text{O} = 16\text{ u}$$)',
        '[{"id": "A", "text": "$$1\\text{ mole}$$", "isCorrect": true}, {"id": "B", "text": "$$2\\text{ moles}$$", "isCorrect": false}, {"id": "C", "text": "$$0.5\\text{ moles}$$", "isCorrect": false}, {"id": "D", "text": "$$44\\text{ moles}$$", "isCorrect": false}]',
        'A',
        'Molar mass of $$\text{CO}_2 = 12 + 2(16) = 44\text{ g/mol}$$. Moles $$n = \frac{\text{mass}}{\text{molar mass}} = \frac{44}{44} = 1\text{ mole}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000013'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'MEDIUM',
        'APPLY',
        'What is the mass of $$0.5\text{ moles}$$ of water ($$\text{H}_2\text{O}$$)? (Atomic masses: $$\text{H} = 1\text{ u}, \text{O} = 16\text{ u}$$)',
        '[{"id": "A", "text": "$$9\\text{ grams}$$", "isCorrect": true}, {"id": "B", "text": "$$18\\text{ grams}$$", "isCorrect": false}, {"id": "C", "text": "$$36\\text{ grams}$$", "isCorrect": false}, {"id": "D", "text": "$$4.5\\text{ grams}$$", "isCorrect": false}]',
        'A',
        'Molar mass of $$\text{H}_2\text{O} = 2(1) + 16 = 18\text{ g/mol}$$. Mass $$= n \times M = 0.5 \times 18 = 9\text{ g}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000014'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'HARD',
        'APPLY',
        'What is the volume occupied by $$1\text{ mole}$$ of any ideal gas at Standard Temperature and Pressure ($$\text{STP}$$: $$0^\circ\text{C}$$ and $$1\text{ atm}$$)?',
        '[{"id": "A", "text": "$$22.4\\text{ litres}$$", "isCorrect": true}, {"id": "B", "text": "$$24.0\\text{ litres}$$", "isCorrect": false}, {"id": "C", "text": "$$11.2\\text{ litres}$$", "isCorrect": false}, {"id": "D", "text": "$$44.8\\text{ litres}$$", "isCorrect": false}]',
        'A',
        'At STP ($$273.15\text{ K}, 1\text{ atm}$$), one mole of any ideal gas occupies a molar volume of $$22.4\text{ L}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000015'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'EASY',
        'REMEMBER',
        'Who is credited with formulating the first scientific Atomic Theory in 1808?',
        '[{"id": "A", "text": "John Dalton", "isCorrect": true}, {"id": "B", "text": "J.J. Thomson", "isCorrect": false}, {"id": "C", "text": "Ernest Rutherford", "isCorrect": false}, {"id": "D", "text": "Antoine Lavoisier", "isCorrect": false}]',
        'A',
        'John Dalton proposed that all matter is composed of indivisible, indestructible particles called atoms.'
    ),
    (
        'a1100000-0000-0000-0000-000000000016'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'MEDIUM',
        'UNDERSTAND',
        'The Law of Definite Proportions (Constant Composition) was discovered by:',
        '[{"id": "A", "text": "Joseph Proust", "isCorrect": true}, {"id": "B", "text": "Antoine Lavoisier", "isCorrect": false}, {"id": "C", "text": "John Dalton", "isCorrect": false}, {"id": "D", "text": "Amedeo Avogadro", "isCorrect": false}]',
        'A',
        'Joseph Proust stated that in a given chemical compound, the elements are always combined in the same fixed proportion by mass.'
    ),
    (
        'a1100000-0000-0000-0000-000000000017'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'HARD',
        'ANALYZE',
        'How many oxygen atoms are present in $$0.1\text{ moles}$$ of sulfuric acid ($$\text{H}_2\text{SO}_4$$)?',
        '[{"id": "A", "text": "$$2.4088 \\times 10^{23}\\text{ atoms}$$", "isCorrect": true}, {"id": "B", "text": "$$6.022 \\times 10^{22}\\text{ atoms}$$", "isCorrect": false}, {"id": "C", "text": "$$6.022 \\times 10^{23}\\text{ atoms}$$", "isCorrect": false}, {"id": "D", "text": "$$1.2044 \\times 10^{23}\\text{ atoms}$$", "isCorrect": false}]',
        'A',
        '$$1\text{ molecule of }\text{H}_2\text{SO}_4$$ has $$4$$ oxygen atoms. In $$0.1\text{ mol}$$, oxygen atoms $$= 0.1 \times 4 \times 6.022 \times 10^{23} = 2.4088 \times 10^{23}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000018'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'EASY',
        'REMEMBER',
        'What is the standard reference isotope currently chosen for defining the unified atomic mass unit ($$\text{u}$$)?',
        '[{"id": "A", "text": "Carbon-12 ($$^{12}\\text{C}$$)", "isCorrect": true}, {"id": "B", "text": "Oxygen-16 ($$^{16}\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Hydrogen-1 ($$^{1}\\text{H}$$)", "isCorrect": false}, {"id": "D", "text": "Carbon-14 ($$^{14}\\text{C}$$)", "isCorrect": false}]',
        'A',
        'One atomic mass unit ($$\text{u}$$) is defined as exactly $$\frac{1}{12}\text{th}$$ the mass of an unbound neutral carbon-12 atom.'
    ),
    (
        'a1100000-0000-0000-0000-000000000019'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'MEDIUM',
        'APPLY',
        'What is the percentage of nitrogen by mass in urea ($$\text{NH}_2\text{CONH}_2$$)? (Atomic masses: $$\text{N}=14, \text{C}=12, \text{O}=16, \text{H}=1$$)',
        '[{"id": "A", "text": "$$\\approx 46.6\\%$$", "isCorrect": true}, {"id": "B", "text": "$$\\approx 35.0\\%$$", "isCorrect": false}, {"id": "C", "text": "$$\\approx 28.0\\%$$", "isCorrect": false}, {"id": "D", "text": "$$\\approx 52.3\\%$$", "isCorrect": false}]',
        'A',
        'Molar mass of urea $$= 2(14) + 4(1) + 12 + 16 = 60\text{ g/mol}$$. Mass of nitrogen $$= 28\text{ g}$$. Percentage $$= \frac{28}{60} \times 100 \approx 46.67\%$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000020'::uuid,
        'Chemistry',
        'Atomic Structure and Mole Concept',
        'HARD',
        'ANALYZE',
        'Which of the following samples contains the largest number of molecules?',
        '[{"id": "A", "text": "$$1\\text{ g of Hydrogen gas (H}_2\\text{)}$$", "isCorrect": true}, {"id": "B", "text": "$$1\\text{ g of Methane (CH}_4\\text{)}$$", "isCorrect": false}, {"id": "C", "text": "$$1\\text{ g of Nitrogen gas (N}_2\\text{)}$$", "isCorrect": false}, {"id": "D", "text": "$$1\\text{ g of Oxygen gas (O}_2\\text{)}$$", "isCorrect": false}]',
        'A',
        'Number of molecules $$N = \frac{m}{M} \times N_A$$. Since mass $$m = 1\text{ g}$$ is identical for all samples, the substance with the lowest molar mass has the highest number of molecules. Molar mass of $$\text{H}_2 = 2\text{ g/mol}$$, giving $$0.5 N_A$$ molecules, far exceeding $$\text{CH}_4$$ ($$0.0625 N_A$$), $$\text{N}_2$$ ($$0.0357 N_A$$), and $$\text{O}_2$$ ($$0.03125 N_A$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000021'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'EASY',
        'REMEMBER',
        'Who discovered the neutron in the year 1932 by bombarding beryllium with alpha particles?',
        '[{"id": "A", "text": "James Chadwick", "isCorrect": true}, {"id": "B", "text": "J.J. Thomson", "isCorrect": false}, {"id": "C", "text": "Ernest Rutherford", "isCorrect": false}, {"id": "D", "text": "Niels Bohr", "isCorrect": false}]',
        'A',
        'James Chadwick discovered the neutron, a subatomic particle having no electrical charge and mass roughly equal to a proton.'
    ),
    (
        'a1100000-0000-0000-0000-000000000022'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'EASY',
        'REMEMBER',
        'Rutherford''s alpha-particle scattering experiment led to the discovery of the:',
        '[{"id": "A", "text": "Atomic Nucleus", "isCorrect": true}, {"id": "B", "text": "Electron", "isCorrect": false}, {"id": "C", "text": "Neutron", "isCorrect": false}, {"id": "D", "text": "Proton", "isCorrect": false}]',
        'A',
        'When alpha particles deflected at large angles through thin gold foil, Rutherford deduced that the mass and positive charge are concentrated in a tiny dense nucleus.'
    ),
    (
        'a1100000-0000-0000-0000-000000000023'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'MEDIUM',
        'UNDERSTAND',
        'Atoms of different chemical elements having the same mass number ($$A$$) but different atomic numbers ($$Z$$) are called:',
        '[{"id": "A", "text": "Isobars", "isCorrect": true}, {"id": "B", "text": "Isotopes", "isCorrect": false}, {"id": "C", "text": "Isotones", "isCorrect": false}, {"id": "D", "text": "Allotropes", "isCorrect": false}]',
        'A',
        'Isobars have the same mass number ($$A$$) but different atomic numbers ($$Z$$) (e.g. $$^{40}_{18}\text{Ar}$$ and $$^{40}_{20}\text{Ca}$$). Isotopes have same $$Z$$ but different $$A$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000024'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'MEDIUM',
        'APPLY',
        'What is the maximum number of electrons that can be accommodated in the M shell ($$n = 3$$) of an atom?',
        '[{"id": "A", "text": "$$18$$", "isCorrect": true}, {"id": "B", "text": "$$8$$", "isCorrect": false}, {"id": "C", "text": "$$32$$", "isCorrect": false}, {"id": "D", "text": "$$2$$", "isCorrect": false}]',
        'A',
        'According to the Bohr-Bury rule, maximum capacity of shell $$n$$ is $$2n^2$$. For M shell ($$n=3$$), $$2(3^2) = 2(9) = 18$$ electrons.'
    ),
    (
        'a1100000-0000-0000-0000-000000000025'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'EASY',
        'REMEMBER',
        'Which radioactive isotope of carbon is widely used in radiocarbon dating to determine the age of archaeological artifacts?',
        '[{"id": "A", "text": "Carbon-14 ($$^{14}\\text{C}$$)", "isCorrect": true}, {"id": "B", "text": "Carbon-12 ($$^{12}\\text{C}$$)", "isCorrect": false}, {"id": "C", "text": "Carbon-13 ($$^{13}\\text{C}$$)", "isCorrect": false}, {"id": "D", "text": "Carbon-11 ($$^{11}\\text{C}$$)", "isCorrect": false}]',
        'A',
        'Carbon-14 (half-life $$\approx 5730\text{ years}$$) decays into nitrogen-14 and is utilized to date organic archaeological remains up to $$\approx 50,000\text{ years}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000026'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'MEDIUM',
        'REMEMBER',
        'Which radioactive isotope is used in the treatment of thyroid cancer?',
        '[{"id": "A", "text": "Iodine-131 ($$^{131}\\text{I}$$)", "isCorrect": true}, {"id": "B", "text": "Cobalt-60 ($$^{60}\\text{Co}$$)", "isCorrect": false}, {"id": "C", "text": "Sodium-24 ($$^{24}\\text{Na}$$)", "isCorrect": false}, {"id": "D", "text": "Phosphorus-32 ($$^{32}\\text{P}$$)", "isCorrect": false}]',
        'A',
        'Iodine-131 is actively absorbed by the thyroid gland and destroys cancerous thyroid tissue through beta and gamma emission. Cobalt-60 is used in general cancer radiotherapy.'
    ),
    (
        'a1100000-0000-0000-0000-000000000027'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'EASY',
        'UNDERSTAND',
        'An alpha ($$\alpha$$) particle emitted during radioactive decay is identical to the nucleus of:',
        '[{"id": "A", "text": "Helium ($$\\text{He}^{2+}$$)", "isCorrect": true}, {"id": "B", "text": "Hydrogen ($$\\text{H}^+$$)", "isCorrect": false}, {"id": "C", "text": "Lithium ($$\\text{Li}^{3+}$$)", "isCorrect": false}, {"id": "D", "text": "Deuterium", "isCorrect": false}]',
        'A',
        'An alpha particle consists of two protons and two neutrons, identical to a doubly ionized helium nucleus ($$^{4}_{2}\text{He}^{2+}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000028'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'HARD',
        'ANALYZE',
        'When a radioactive nucleus emits a beta ($$\beta^-$$) particle, how do its atomic number ($$Z$$) and mass number ($$A$$) change?',
        '[{"id": "A", "text": "$$Z$$ increases by $$1$$, while $$A$$ remains unchanged", "isCorrect": true}, {"id": "B", "text": "$$Z$$ decreases by $$1$$, while $$A$$ remains unchanged", "isCorrect": false}, {"id": "C", "text": "$$Z$$ decreases by $$2$$, and $$A$$ decreases by $$4$$", "isCorrect": false}, {"id": "D", "text": "Both $$Z$$ and $$A$$ remain unchanged", "isCorrect": false}]',
        'A',
        'In beta-minus decay, a neutron converts to a proton, electron, and antineutrino ($$n \to p + e^- + \bar{\nu}_e$$). Thus, atomic number $$Z \to Z+1$$ while mass number $$A$$ is unchanged.'
    ),
    (
        'a1100000-0000-0000-0000-000000000029'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'MEDIUM',
        'REMEMBER',
        'Who discovered natural radioactivity in uranium salts in 1896?',
        '[{"id": "A", "text": "Henri Becquerel", "isCorrect": true}, {"id": "B", "text": "Marie Curie", "isCorrect": false}, {"id": "C", "text": "Wilhelm Röntgen", "isCorrect": false}, {"id": "D", "text": "Enrico Fermi", "isCorrect": false}]',
        'A',
        'Henri Becquerel discovered radioactivity in 1896 when uranium crystals exposed wrapped photographic plates. Marie and Pierre Curie later discovered Polonium and Radium.'
    ),
    (
        'a1100000-0000-0000-0000-000000000030'::uuid,
        'Chemistry',
        'Subatomic Particles and Radioactivity',
        'HARD',
        'APPLY',
        'A radioactive sample has a half-life of $$10\text{ days}$$. What fraction of the original radioactive material remains undecayed after $$30\text{ days}$$?',
        '[{"id": "A", "text": "$$\\frac{1}{8}$$ ($$12.5\\%$$)", "isCorrect": true}, {"id": "B", "text": "$$\\frac{1}{4}$$ ($$25\\%$$)", "isCorrect": false}, {"id": "C", "text": "$$\\frac{1}{16}$$ ($$6.25\\%$$)", "isCorrect": false}, {"id": "D", "text": "$$\\frac{1}{3}$$", "isCorrect": false}]',
        'A',
        'Number of half-lives $$n = \frac{30}{10} = 3$$. Remaining fraction $$= \left(\frac{1}{2}\right)^3 = \frac{1}{8} = 12.5\%$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000031'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'EASY',
        'REMEMBER',
        'On what fundamental atomic property is the Modern Periodic Table arranged, as formulated by Henry Moseley in 1913?',
        '[{"id": "A", "text": "Atomic number (number of protons)", "isCorrect": true}, {"id": "B", "text": "Atomic weight (mass number)", "isCorrect": false}, {"id": "C", "text": "Atomic radius", "isCorrect": false}, {"id": "D", "text": "Electronegativity", "isCorrect": false}]',
        'A',
        'The Modern Periodic Law states that properties of elements are periodic functions of their atomic numbers ($$Z$$), rectifying anomalies in Mendeleev''s mass-based table.'
    ),
    (
        'a1100000-0000-0000-0000-000000000032'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'EASY',
        'REMEMBER',
        'How many vertical columns (groups) and horizontal rows (periods) are present in the standard Modern Periodic Table?',
        '[{"id": "A", "text": "$$18\\text{ groups and } 7\\text{ periods}$$", "isCorrect": true}, {"id": "B", "text": "$$16\\text{ groups and } 8\\text{ periods}$$", "isCorrect": false}, {"id": "C", "text": "$$8\\text{ groups and } 7\\text{ periods}$$", "isCorrect": false}, {"id": "D", "text": "$$18\\text{ groups and } 9\\text{ periods}$$", "isCorrect": false}]',
        'A',
        'The modern long form of the periodic table consists of 18 vertical columns (groups) and 7 horizontal rows (periods).'
    ),
    (
        'a1100000-0000-0000-0000-000000000033'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'MEDIUM',
        'UNDERSTAND',
        'How does the atomic radius generally vary as you move from left to right across a period in the periodic table?',
        '[{"id": "A", "text": "Decreases due to increasing effective nuclear charge", "isCorrect": true}, {"id": "B", "text": "Increases due to addition of extra electron shells", "isCorrect": false}, {"id": "C", "text": "Remains completely constant", "isCorrect": false}, {"id": "D", "text": "First decreases and then increases sharply", "isCorrect": false}]',
        'A',
        'Electrons are added into the same energy level while nuclear charge increases, pulling outer electrons closer and causing atomic radius to decrease across a period.'
    ),
    (
        'a1100000-0000-0000-0000-000000000034'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'EASY',
        'REMEMBER',
        'Which element in the entire periodic table has the highest electronegativity?',
        '[{"id": "A", "text": "Fluorine ($$\\text{F}$$)", "isCorrect": true}, {"id": "B", "text": "Chlorine ($$\\text{Cl}$$)", "isCorrect": false}, {"id": "C", "text": "Oxygen ($$\\text{O}$$)", "isCorrect": false}, {"id": "D", "text": "Cesium ($$\\text{Cs}$$)", "isCorrect": false}]',
        'A',
        'Fluorine has the highest Pauling electronegativity value ($$3.98 \approx 4.0$$), making it the most powerful non-metallic electron attractor.'
    ),
    (
        'a1100000-0000-0000-0000-000000000035'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'MEDIUM',
        'UNDERSTAND',
        'Which group of elements in the periodic table is famously known as the ''Halogens''?',
        '[{"id": "A", "text": "Group 17 ($$\\text{F, Cl, Br, I, At}$$)", "isCorrect": true}, {"id": "B", "text": "Group 18 (Noble gases)", "isCorrect": false}, {"id": "C", "text": "Group 1 (Alkali metals)", "isCorrect": false}, {"id": "D", "text": "Group 2 (Alkaline earth metals)", "isCorrect": false}]',
        'A',
        'Group 17 elements are halogens (''salt-formers''), possessing 7 valence electrons and high reactivity with metals to form salts.'
    ),
    (
        'a1100000-0000-0000-0000-000000000036'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'EASY',
        'REMEMBER',
        'What are Group 18 elements commonly called due to their completely filled valence electron shells?',
        '[{"id": "A", "text": "Noble gases (Inert gases)", "isCorrect": true}, {"id": "B", "text": "Alkali metals", "isCorrect": false}, {"id": "C", "text": "Chalcogens", "isCorrect": false}, {"id": "D", "text": "Transition metals", "isCorrect": false}]',
        'A',
        'Group 18 elements (He, Ne, Ar, Kr, Xe, Rn) have stable octets (or duet for He) and exhibit negligible chemical reactivity, hence called noble or inert gases.'
    ),
    (
        'a1100000-0000-0000-0000-000000000037'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'MEDIUM',
        'UNDERSTAND',
        'Which element has the highest electron gain enthalpy (electron affinity) in the periodic table?',
        '[{"id": "A", "text": "Chlorine ($$\\text{Cl}$$)", "isCorrect": true}, {"id": "B", "text": "Fluorine ($$\\text{F}$$)", "isCorrect": false}, {"id": "C", "text": "Bromine ($$\\text{Br}$$)", "isCorrect": false}, {"id": "D", "text": "Oxygen ($$\\text{O}$$)", "isCorrect": false}]',
        'A',
        'Although fluorine is more electronegative, its compact 2p subshell suffers interelectronic repulsion. Chlorine has a larger 3p subshell and thus the highest exothermic electron gain enthalpy.'
    ),
    (
        'a1100000-0000-0000-0000-000000000038'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'HARD',
        'ANALYZE',
        'How does ionization energy change as one moves down a group from top to bottom?',
        '[{"id": "A", "text": "Decreases because atomic size increases and shielding effect increases", "isCorrect": true}, {"id": "B", "text": "Increases because nuclear charge increases", "isCorrect": false}, {"id": "C", "text": "Remains constant down all groups", "isCorrect": false}, {"id": "D", "text": "First increases and then decreases", "isCorrect": false}]',
        'A',
        'As new principal quantum shells are added, valence electrons are further from the nucleus and shielded by inner core electrons, requiring less energy to remove.'
    ),
    (
        'a1100000-0000-0000-0000-000000000039'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'EASY',
        'REMEMBER',
        'Which of the following is a metalloid (semimetal) exhibiting properties of both metals and non-metals?',
        '[{"id": "A", "text": "Silicon ($$\\text{Si}$$)", "isCorrect": true}, {"id": "B", "text": "Sodium ($$\\text{Na}$$)", "isCorrect": false}, {"id": "C", "text": "Sulfur ($$\\text{S}$$)", "isCorrect": false}, {"id": "D", "text": "Iron ($$\\text{Fe}$$)", "isCorrect": false}]',
        'A',
        'Silicon, Germanium, Arsenic, Antimony, and Tellurium are recognized metalloids positioned along the zigzag boundary dividing metals from non-metals.'
    ),
    (
        'a1100000-0000-0000-0000-000000000040'::uuid,
        'Chemistry',
        'Periodic Classification of Elements',
        'MEDIUM',
        'UNDERSTAND',
        'Elements in the same vertical group of the periodic table possess identical:',
        '[{"id": "A", "text": "Number of valence electrons in the outermost shell", "isCorrect": true}, {"id": "B", "text": "Total number of electron shells", "isCorrect": false}, {"id": "C", "text": "Total number of protons", "isCorrect": false}, {"id": "D", "text": "Atomic weight", "isCorrect": false}]',
        'A',
        'Elements in the same group have the same valence shell electron configuration, which explains why they share similar chemical properties.'
    ),
    (
        'a1100000-0000-0000-0000-000000000041'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'EASY',
        'REMEMBER',
        'What type of chemical reaction is represented by: $$2\text{H}_2 + \text{O}_2 \to 2\text{H}_2\text{O}$$?',
        '[{"id": "A", "text": "Combination reaction (Synthesis)", "isCorrect": true}, {"id": "B", "text": "Decomposition reaction", "isCorrect": false}, {"id": "C", "text": "Displacement reaction", "isCorrect": false}, {"id": "D", "text": "Double displacement reaction", "isCorrect": false}]',
        'A',
        'A reaction in which two or more reactants combine to form a single product is a combination reaction.'
    ),
    (
        'a1100000-0000-0000-0000-000000000042'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'EASY',
        'UNDERSTAND',
        'When an iron nail is dipped into a blue copper sulfate solution ($$\text{CuSO}_4$$), the solution turns green and reddish-brown copper deposits on the nail. This is an example of a:',
        '[{"id": "A", "text": "Single displacement reaction", "isCorrect": true}, {"id": "B", "text": "Combination reaction", "isCorrect": false}, {"id": "C", "text": "Thermal decomposition reaction", "isCorrect": false}, {"id": "D", "text": "Neutralization reaction", "isCorrect": false}]',
        'A',
        '$$\text{Fe} + \text{CuSO}_4 \to \text{FeSO}_4 + \text{Cu}$$. Iron is more reactive than copper, so it displaces copper from its salt solution.'
    ),
    (
        'a1100000-0000-0000-0000-000000000043'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'MEDIUM',
        'UNDERSTAND',
        'In a redox chemical reaction, oxidation is defined as:',
        '[{"id": "A", "text": "Loss of electrons or addition of oxygen", "isCorrect": true}, {"id": "B", "text": "Gain of electrons or addition of hydrogen", "isCorrect": false}, {"id": "C", "text": "Gain of protons", "isCorrect": false}, {"id": "D", "text": "Loss of mass", "isCorrect": false}]',
        'A',
        'Oxidation involves the loss of electrons (OIL: Oxidation Is Loss) or gain of oxygen / loss of hydrogen. Reduction is gain of electrons (RIG: Reduction Is Gain).'
    ),
    (
        'a1100000-0000-0000-0000-000000000044'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'EASY',
        'UNDERSTAND',
        'Respiration in living organisms is classified as which type of chemical process?',
        '[{"id": "A", "text": "Exothermic redox reaction", "isCorrect": true}, {"id": "B", "text": "Endothermic combination reaction", "isCorrect": false}, {"id": "C", "text": "Photochemical decomposition reaction", "isCorrect": false}, {"id": "D", "text": "Electrochemical synthesis", "isCorrect": false}]',
        'A',
        'Glucose is oxidized ($$\text{C}_6\text{H}_{12}\text{O}_6 + 6\text{O}_2 \to 6\text{CO}_2 + 6\text{H}_2\text{O} + \text{Energy}$$) releasing energy in the form of ATP, making respiration an exothermic reaction.'
    ),
    (
        'a1100000-0000-0000-0000-000000000045'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'MEDIUM',
        'REMEMBER',
        'What is the chemical formula of rust formed on iron in the presence of moist air?',
        '[{"id": "A", "text": "Hydrated ferric oxide ($$\\text{Fe}_2\\text{O}_3 \\cdot x\\text{H}_2\\text{O}$$)", "isCorrect": true}, {"id": "B", "text": "Ferrous oxide ($$\\text{FeO}$$)", "isCorrect": false}, {"id": "C", "text": "Iron sulfide ($$\\text{FeS}$$)", "isCorrect": false}, {"id": "D", "text": "Ferric carbonate ($$\\text{Fe}_2(\\text{CO}_3)_3$$)", "isCorrect": false}]',
        'A',
        'Rust is hydrated iron(III) oxide ($$\text{Fe}_2\text{O}_3 \cdot x\text{H}_2\text{O}$$) formed when iron reacts with oxygen in the presence of water/moisture.'
    ),
    (
        'a1100000-0000-0000-0000-000000000046'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'EASY',
        'UNDERSTAND',
        'Why are chips and snack packets flushed with nitrogen gas before sealing?',
        '[{"id": "A", "text": "To prevent oxidation and rancidity of oils and fats", "isCorrect": true}, {"id": "B", "text": "To add artificial crispiness to chips", "isCorrect": false}, {"id": "C", "text": "To sanitize the packaging from bacteria", "isCorrect": false}, {"id": "D", "text": "To cool down fried snacks during packing", "isCorrect": false}]',
        'A',
        'Nitrogen is an unreactive inert gas that displaces oxygen, preventing oils and fats from oxidizing and turning rancid (foul odor and bad taste).'
    ),
    (
        'a1100000-0000-0000-0000-000000000047'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'MEDIUM',
        'UNDERSTAND',
        'The black coating that develops on silver ornaments exposed to air is caused by the formation of:',
        '[{"id": "A", "text": "Silver sulfide ($$\\text{Ag}_2\\text{S}$$)", "isCorrect": true}, {"id": "B", "text": "Silver oxide ($$\\text{Ag}_2\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Silver chloride ($$\\text{AgCl}$$)", "isCorrect": false}, {"id": "D", "text": "Silver nitrate ($$\\text{AgNO}_3$$)", "isCorrect": false}]',
        'A',
        'Silver reacts with trace hydrogen sulfide ($$\text{H}_2\text{S}$$) present in polluted air to form a tarnished black layer of silver sulfide ($$\text{Ag}_2\text{S}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000048'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'HARD',
        'APPLY',
        'In the reaction: $$\text{MnO}_2 + 4\text{HCl} \to \text{MnCl}_2 + 2\text{H}_2\text{O} + \text{Cl}_2$$, which substance is oxidized and which acts as the oxidizing agent?',
        '[{"id": "A", "text": "$$\\text{HCl}$$ is oxidized; $$\\text{MnO}_2$$ is the oxidizing agent", "isCorrect": true}, {"id": "B", "text": "$$\\text{MnO}_2$$ is oxidized; $$\\text{HCl}$$ is the oxidizing agent", "isCorrect": false}, {"id": "C", "text": "Both $$\\text{HCl}$$ and $$\\text{MnO}_2$$ are oxidized", "isCorrect": false}, {"id": "D", "text": "$$\\text{Cl}_2$$ is oxidized; $$\\text{H}_2\\text{O}$$ is the oxidizing agent", "isCorrect": false}]',
        'A',
        'Chloride ion in $$\text{HCl}$$ ($$\text{Cl}^-$$ to $$\text{Cl}_2$$) loses electrons and is oxidized. $$\text{MnO}_2$$ ($$\text{Mn}^{+4}$$ to $$\text{Mn}^{+2}$$) gains electrons and acts as the oxidizing agent.'
    ),
    (
        'a1100000-0000-0000-0000-000000000049'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'MEDIUM',
        'UNDERSTAND',
        'When lead nitrate powder is heated in a dry boiling tube, brown fumes are emitted. These brown fumes belong to:',
        '[{"id": "A", "text": "Nitrogen dioxide ($$\\text{NO}_2$$)", "isCorrect": true}, {"id": "B", "text": "Nitrous oxide ($$\\text{N}_2\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Nitric oxide ($$\\text{NO}$$)", "isCorrect": false}, {"id": "D", "text": "Oxygen ($$\\text{O}_2$$)", "isCorrect": false}]',
        'A',
        '$$2\text{Pb}(\text{NO}_3)_2 \xrightarrow{\Delta} 2\text{PbO} + 4\text{NO}_2\uparrow + \text{O}_2\uparrow$$. $$\text{NO}_2$$ is a reddish-brown, toxic gas.'
    ),
    (
        'a1100000-0000-0000-0000-000000000050'::uuid,
        'Chemistry',
        'Chemical Reactions and Equations',
        'HARD',
        'ANALYZE',
        'Why does a chemical equation always need to be balanced in accordance with the Law of Conservation of Mass?',
        '[{"id": "A", "text": "Total number of atoms of each element must remain equal on both reactant and product sides", "isCorrect": true}, {"id": "B", "text": "Total volume of gas must remain constant before and after reaction", "isCorrect": false}, {"id": "C", "text": "Number of molecules must be identical on both sides", "isCorrect": false}, {"id": "D", "text": "Temperature must not fluctuate during reaction", "isCorrect": false}]',
        'A',
        'Mass can neither be created nor destroyed in a chemical reaction, meaning the total mass and the number of atoms of each element must be conserved.'
    ),
    (
        'a1100000-0000-0000-0000-000000000051'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'REMEMBER',
        'What is the pH value of pure distilled water at $$25^\circ\text{C}$$?',
        '[{"id": "A", "text": "$$7.0$$ (Neutral)", "isCorrect": true}, {"id": "B", "text": "$$0.0$$", "isCorrect": false}, {"id": "C", "text": "$$14.0$$", "isCorrect": false}, {"id": "D", "text": "$$5.6$$", "isCorrect": false}]',
        'A',
        'Pure water has equal concentrations of $$[\text{H}^+] = [\text{OH}^-] = 10^{-7}\text{ M}$$ at $$25^\circ\text{C}$$, giving $$\text{pH} = -\log(10^{-7}) = 7$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000052'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'REMEMBER',
        'What is the normal pH range of healthy human blood?',
        '[{"id": "A", "text": "$$7.35$$ to $$7.45$$ (Slightly basic)", "isCorrect": true}, {"id": "B", "text": "$$6.00$$ to $$6.50$$ (Slightly acidic)", "isCorrect": false}, {"id": "C", "text": "$$8.50$$ to $$9.00$$ (Moderately basic)", "isCorrect": false}, {"id": "D", "text": "$$5.00$$ to $$5.50$$", "isCorrect": false}]',
        'A',
        'Human arterial blood pH is tightly regulated by bicarbonate buffer systems within a narrow physiological range of $$7.35 - 7.45$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000053'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'REMEMBER',
        'Which natural organic acid is present in citrus fruits such as lemons and oranges?',
        '[{"id": "A", "text": "Citric acid", "isCorrect": true}, {"id": "B", "text": "Acetic acid", "isCorrect": false}, {"id": "C", "text": "Tartaric acid", "isCorrect": false}, {"id": "D", "text": "Oxalic acid", "isCorrect": false}]',
        'A',
        'Lemons and oranges contain citric acid. Vinegar contains acetic acid, tamarind contains tartaric acid, and tomatoes contain oxalic acid.'
    ),
    (
        'a1100000-0000-0000-0000-000000000054'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'UNDERSTAND',
        'Which acid is injected into human skin during an ant bite or bee sting, causing acute pain and burning sensation?',
        '[{"id": "A", "text": "Methanoic acid (Formic acid, $$\\text{HCOOH}$$)", "isCorrect": true}, {"id": "B", "text": "Lactic acid", "isCorrect": false}, {"id": "C", "text": "Hydrochloric acid", "isCorrect": false}, {"id": "D", "text": "Nitric acid", "isCorrect": false}]',
        'A',
        'Ant stings inject formic acid (methanoic acid). Applying a mild base such as baking soda ($$\text{NaHCO}_3$$) or calamine neutralizes the pain.'
    ),
    (
        'a1100000-0000-0000-0000-000000000055'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'REMEMBER',
        'What is the common chemical name and formula of Baking Soda?',
        '[{"id": "A", "text": "Sodium bicarbonate ($$\\text{NaHCO}_3$$)", "isCorrect": true}, {"id": "B", "text": "Sodium carbonate decahydrate ($$\\text{Na}_2\\text{CO}_3 \\cdot 10\\text{H}_2\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Calcium oxychloride ($$\\text{CaOCl}_2$$)", "isCorrect": false}, {"id": "D", "text": "Sodium hydroxide ($$\\text{NaOH}$$)", "isCorrect": false}]',
        'A',
        'Baking soda is Sodium hydrogen carbonate ($$\text{NaHCO}_3$$). Washing soda is $$\text{Na}_2\text{CO}_3 \cdot 10\text{H}_2\text{O}$$, and Caustic soda is $$\text{NaOH}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000056'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'MEDIUM',
        'REMEMBER',
        'What is the chemical name and formula of Plaster of Paris ($$\text{POP}$$)?',
        '[{"id": "A", "text": "Calcium sulfate hemihydrate ($$\\text{CaSO}_4 \\cdot \\frac{1}{2}\\text{H}_2\\text{O}$$)", "isCorrect": true}, {"id": "B", "text": "Calcium sulfate dihydrate ($$\\text{CaSO}_4 \\cdot 2\\text{H}_2\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Calcium carbonate ($$\\text{CaCO}_3$$)", "isCorrect": false}, {"id": "D", "text": "Calcium oxide ($$\\text{CaO}$$)", "isCorrect": false}]',
        'A',
        'POP is $$\text{CaSO}_4 \cdot \frac{1}{2}\text{H}_2\text{O}$$, prepared by heating gypsum ($$\text{CaSO}_4 \cdot 2\text{H}_2\text{O}$$) at $$373\text{ K}$$ ($$100^\circ\text{C}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000057'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'MEDIUM',
        'REMEMBER',
        'Bleaching powder ($$\text{CaOCl}_2$$) is chemically manufactured by passing chlorine gas over:',
        '[{"id": "A", "text": "Dry slaked lime ($$\\text{Ca(OH)}_2$$)", "isCorrect": true}, {"id": "B", "text": "Quicklime ($$\\text{CaO}$$)", "isCorrect": false}, {"id": "C", "text": "Limestone ($$\\text{CaCO}_3$$)", "isCorrect": false}, {"id": "D", "text": "Calcium chloride ($$\\text{CaCl}_2$$)", "isCorrect": false}]',
        'A',
        '$$\text{Ca(OH)}_2 + \text{Cl}_2 \to \text{CaOCl}_2 + \text{H}_2\text{O}$$. Passing chlorine over dry slaked lime produces calcium oxychloride (bleaching powder).'
    ),
    (
        'a1100000-0000-0000-0000-000000000058'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'EASY',
        'REMEMBER',
        'Aqua Regia (royal water), capable of dissolving noble metals like gold and platinum, is a fresh mixture of concentrated:',
        '[{"id": "A", "text": "$$\\text{HCl}$$ and $$\\text{HNO}_3$$ in a $$3 : 1$$ volume ratio", "isCorrect": true}, {"id": "B", "text": "$$\\text{HNO}_3$$ and $$\\text{HCl}$$ in a $$3 : 1$$ volume ratio", "isCorrect": false}, {"id": "C", "text": "$$\\text{H}_2\\text{SO}_4$$ and $$\\text{HNO}_3$$ in a $$1 : 1$$ volume ratio", "isCorrect": false}, {"id": "D", "text": "$$\\text{HCl}$$ and $$\\text{H}_2\\text{SO}_4$$ in a $$2 : 1$$ volume ratio", "isCorrect": false}]',
        'A',
        'Aqua Regia consists of 3 parts concentrated Hydrochloric acid ($$\text{HCl}$$) and 1 part concentrated Nitric acid ($$\text{HNO}_3$$) by volume.'
    ),
    (
        'a1100000-0000-0000-0000-000000000059'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'MEDIUM',
        'UNDERSTAND',
        'What gas is evolved when a metal reacts with a dilute mineral acid such as hydrochloric acid or sulfuric acid?',
        '[{"id": "A", "text": "Hydrogen gas ($$\\text{H}_2$$), which burns with a ''pop'' sound", "isCorrect": true}, {"id": "B", "text": "Oxygen gas ($$\\text{O}_2$$)", "isCorrect": false}, {"id": "C", "text": "Carbon dioxide ($$\\text{CO}_2$$)", "isCorrect": false}, {"id": "D", "text": "Chlorine gas ($$\\text{Cl}_2$$)", "isCorrect": false}]',
        'A',
        '$$\text{Zn} + 2\text{HCl} \to \text{ZnCl}_2 + \text{H}_2\uparrow$$. Hydrogen gas burns with a characteristic squeaky ''pop'' sound when tested with a lighted splint.'
    ),
    (
        'a1100000-0000-0000-0000-000000000060'::uuid,
        'Chemistry',
        'Acids, Bases, Salts and pH Scale',
        'HARD',
        'ANALYZE',
        'An aqueous solution turns red litmus paper blue. What is the expected pH range of this solution?',
        '[{"id": "A", "text": "Greater than $$7.0$$ (Alkaline)", "isCorrect": true}, {"id": "B", "text": "Less than $$7.0$$ (Acidic)", "isCorrect": false}, {"id": "C", "text": "Exactly equal to $$7.0$$", "isCorrect": false}, {"id": "D", "text": "Between $$1.0$$ and $$3.0$$", "isCorrect": false}]',
        'A',
        'Bases turn red litmus paper blue, indicating a basic (alkaline) solution with a pH greater than 7.'
    ),
    (
        'a1100000-0000-0000-0000-000000000061'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'Which metal is the only metal that remains in a liquid state at room temperature ($$25^\circ\text{C}$$)?',
        '[{"id": "A", "text": "Mercury ($$\\text{Hg}$$)", "isCorrect": true}, {"id": "B", "text": "Bromine ($$\\text{Br}$$)", "isCorrect": false}, {"id": "C", "text": "Gallium ($$\\text{Ga}$$)", "isCorrect": false}, {"id": "D", "text": "Cesium ($$\\text{Cs}$$)", "isCorrect": false}]',
        'A',
        'Mercury is the only liquid metal at room temperature. Bromine is the only liquid non-metal. Gallium and Cesium melt just above room temperature ($$\approx 30^\circ\text{C}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000062'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'Which non-metal is an excellent conductor of electricity due to free mobile delocalized electrons in its hexagonal layer structure?',
        '[{"id": "A", "text": "Graphite (Carbon allotrope)", "isCorrect": true}, {"id": "B", "text": "Diamond", "isCorrect": false}, {"id": "C", "text": "Sulfur", "isCorrect": false}, {"id": "D", "text": "Phosphorus", "isCorrect": false}]',
        'A',
        'In graphite, each carbon atom is covalently bonded to three others in planar sheets, leaving one free delocalized electron per carbon atom to conduct electricity.'
    ),
    (
        'a1100000-0000-0000-0000-000000000063'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'Why are sodium and potassium metals always stored immersed under kerosene oil?',
        '[{"id": "A", "text": "They react vigorously with moisture and oxygen in air to catch fire", "isCorrect": true}, {"id": "B", "text": "To prevent them from subliming into vapour", "isCorrect": false}, {"id": "C", "text": "To protect them from melting in ambient heat", "isCorrect": false}, {"id": "D", "text": "To maintain their metallic luster from dust", "isCorrect": false}]',
        'A',
        'Sodium and potassium are highly reactive alkali metals that react violently and exothermically with atmospheric oxygen and water vapour, evolving hydrogen that ignites instantly.'
    ),
    (
        'a1100000-0000-0000-0000-000000000064'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'What is Bauxite the primary natural ore of?',
        '[{"id": "A", "text": "Aluminium ($$\\text{Al}$$)", "isCorrect": true}, {"id": "B", "text": "Iron ($$\\text{Fe}$$)", "isCorrect": false}, {"id": "C", "text": "Copper ($$\\text{Cu}$$)", "isCorrect": false}, {"id": "D", "text": "Zinc ($$\\text{Zn}$$)", "isCorrect": false}]',
        'A',
        'Bauxite ($$\text{Al}_2\text{O}_3 \cdot 2\text{H}_2\text{O}$$) is the principal ore of aluminium, purified by the Bayer process and electrolyzed by the Hall-Héroult process.'
    ),
    (
        'a1100000-0000-0000-0000-000000000065'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'MEDIUM',
        'REMEMBER',
        'What is the primary ore of mercury called?',
        '[{"id": "A", "text": "Cinnabar ($$\\text{HgS}$$)", "isCorrect": true}, {"id": "B", "text": "Galena ($$\\text{PbS}$$)", "isCorrect": false}, {"id": "C", "text": "Haematite ($$\\text{Fe}_2\\text{O}_3$$)", "isCorrect": false}, {"id": "D", "text": "Sphalerite ($$\\text{ZnS}$$)", "isCorrect": false}]',
        'A',
        'Cinnabar (mercury(II) sulfide, $$\text{HgS}$$) is the chief ore of mercury. Roasting cinnabar in air yields metallic mercury: $$\text{HgS} + \text{O}_2 \to \text{Hg} + \text{SO}_2$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000066'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'Brass is an alloy composed of which two metals?',
        '[{"id": "A", "text": "Copper and Zinc ($$\\approx 70\\%\\text{ Cu}, 30\\%\\text{ Zn}$$)", "isCorrect": true}, {"id": "B", "text": "Copper and Tin", "isCorrect": false}, {"id": "C", "text": "Lead and Tin", "isCorrect": false}, {"id": "D", "text": "Iron and Chromium", "isCorrect": false}]',
        'A',
        'Brass is an alloy of Copper and Zinc. Bronze is an alloy of Copper and Tin ($$\text{Cu} + \text{Sn}$$). Solder is Lead and Tin ($$\text{Pb} + \text{Sn}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000067'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'MEDIUM',
        'UNDERSTAND',
        'What is Galvanization, widely used to protect iron from rusting?',
        '[{"id": "A", "text": "Coating iron with a thin sacrificial protective layer of Zinc", "isCorrect": true}, {"id": "B", "text": "Electroplating iron with Chromium", "isCorrect": false}, {"id": "C", "text": "Dipping iron in concentrated molten tin", "isCorrect": false}, {"id": "D", "text": "Heat treatment in an atmosphere of nitrogen", "isCorrect": false}]',
        'A',
        'Galvanization coats iron with molten zinc. Zinc is more reactive than iron, acting as a sacrificial anode that oxidizes preferentially even if the coating is scratched.'
    ),
    (
        'a1100000-0000-0000-0000-000000000068'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'MEDIUM',
        'UNDERSTAND',
        'Amalgam is a special metallurgical alloy in which one of the constituent metals is always:',
        '[{"id": "A", "text": "Mercury", "isCorrect": true}, {"id": "B", "text": "Silver", "isCorrect": false}, {"id": "C", "text": "Gold", "isCorrect": false}, {"id": "D", "text": "Platinum", "isCorrect": false}]',
        'A',
        'An amalgam is an alloy of mercury with another metal (e.g. dental amalgam of silver, tin, and mercury). Iron does not form an amalgam and is used to store mercury.'
    ),
    (
        'a1100000-0000-0000-0000-000000000069'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'HARD',
        'ANALYZE',
        'Which metal oxide among the following is an amphoteric oxide (reacting with both acids and strong bases to produce salt and water)?',
        '[{"id": "A", "text": "Aluminium oxide ($$\\text{Al}_2\\text{O}_3$$) and Zinc oxide ($$\\text{ZnO}$$)", "isCorrect": true}, {"id": "B", "text": "Sodium oxide ($$\\text{Na}_2\\text{O}$$)", "isCorrect": false}, {"id": "C", "text": "Magnesium oxide ($$\\text{MgO}$$)", "isCorrect": false}, {"id": "D", "text": "Calcium oxide ($$\\text{CaO}$$)", "isCorrect": false}]',
        'A',
        '$$\text{Al}_2\text{O}_3$$ and $$\text{ZnO}$$ show both acidic and basic behavior, reacting with both acids (forming $$\text{AlCl}_3$$) and bases (forming sodium aluminate $$\text{NaAlO}_2$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000070'::uuid,
        'Chemistry',
        'Metals, Non-Metals and Metallurgy',
        'EASY',
        'REMEMBER',
        'Which metal is the best electrical and thermal conductor among all known metals?',
        '[{"id": "A", "text": "Silver ($$\\text{Ag}$$)", "isCorrect": true}, {"id": "B", "text": "Copper ($$\\text{Cu}$$)", "isCorrect": false}, {"id": "C", "text": "Gold ($$\\text{Au}$$)", "isCorrect": false}, {"id": "D", "text": "Aluminium ($$\\text{Al}$$)", "isCorrect": false}]',
        'A',
        'Silver possesses the highest electrical and thermal conductivity of all metals, followed closely by copper and gold.'
    ),
    (
        'a1100000-0000-0000-0000-000000000071'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'EASY',
        'REMEMBER',
        'What unique property enables carbon to form exceptionally long chains and rings of millions of organic compounds?',
        '[{"id": "A", "text": "Catenation (self-linking ability) and tetravalency", "isCorrect": true}, {"id": "B", "text": "High electronegativity and radioactivity", "isCorrect": false}, {"id": "C", "text": "Large atomic radius and low ionization energy", "isCorrect": false}, {"id": "D", "text": "High metallic character and ductility", "isCorrect": false}]',
        'A',
        'Catenation is the unique capability of carbon atoms to form stable covalent bonds with other carbon atoms, aided by its small size and tetravalency.'
    ),
    (
        'a1100000-0000-0000-0000-000000000072'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'EASY',
        'REMEMBER',
        'What is the hardest naturally occurring substance known on Earth?',
        '[{"id": "A", "text": "Diamond (Allotrope of Carbon)", "isCorrect": true}, {"id": "B", "text": "Graphite", "isCorrect": false}, {"id": "C", "text": "Silicon carbide (Carborundum)", "isCorrect": false}, {"id": "D", "text": "Tungsten carbide", "isCorrect": false}]',
        'A',
        'Diamond has a rigid three-dimensional giant covalent network where each carbon is tetrahedral $$\text{sp}^3$$ bonded to four other carbons, giving it a score of 10 on Mohs scale.'
    ),
    (
        'a1100000-0000-0000-0000-000000000073'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'MEDIUM',
        'REMEMBER',
        'What is the soccer-ball shaped carbon allotrope discovered by Kroto, Curl, and Smalley consisting of 60 carbon atoms called?',
        '[{"id": "A", "text": "Buckminsterfullerene ($$\\text{C}_{60}$$)", "isCorrect": true}, {"id": "B", "text": "Carbon Nanotube", "isCorrect": false}, {"id": "C", "text": "Graphene", "isCorrect": false}, {"id": "D", "text": "Carbyne", "isCorrect": false}]',
        'A',
        'Buckminsterfullerene ($$\text{C}_{60}$$) consists of 20 hexagons and 12 pentagons arranged like a geodesic dome soccer ball.'
    ),
    (
        'a1100000-0000-0000-0000-000000000074'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'EASY',
        'REMEMBER',
        'What is the primary constituent gas of Compressed Natural Gas ($$\text{CNG}$$) and Biogas (Gobar gas)?',
        '[{"id": "A", "text": "Methane ($$\\text{CH}_4$$)", "isCorrect": true}, {"id": "B", "text": "Propane ($$\\text{C}_3\\text{H}_8$$)", "isCorrect": false}, {"id": "C", "text": "Butane ($$\\text{C}_4\\text{H}_{10}$$)", "isCorrect": false}, {"id": "D", "text": "Ethane ($$\\text{C}_2\\text{H}_6$$)", "isCorrect": false}]',
        'A',
        'Methane ($$\text{CH}_4$$) constitutes $$75-95\%$$ of CNG and $$55-75\%$$ of biogas. Liquefied Petroleum Gas (LPG) consists primarily of butane and propane.'
    ),
    (
        'a1100000-0000-0000-0000-000000000075'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'MEDIUM',
        'REMEMBER',
        'Which foul-smelling sulfur compound is deliberately blended into domestic LPG cylinders to detect dangerous gas leaks?',
        '[{"id": "A", "text": "Ethyl mercaptan (Ethanethiol, $$\\text{C}_2\\text{H}_5\\text{SH}$$)", "isCorrect": true}, {"id": "B", "text": "Hydrogen sulfide ($$\\text{H}_2\\text{S}$$)", "isCorrect": false}, {"id": "C", "text": "Sulfur dioxide ($$\\text{SO}_2$$)", "isCorrect": false}, {"id": "D", "text": "Carbon disulfide ($$\\text{CS}_2$$)", "isCorrect": false}]',
        'A',
        'Liquefied petroleum gas (LPG) is naturally odorless; ethyl mercaptan is added as an odorant so leaks can be identified instantly by human smell.'
    ),
    (
        'a1100000-0000-0000-0000-000000000076'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'MEDIUM',
        'UNDERSTAND',
        'What is the functional group present in alcohols such as ethanol ($$\text{C}_2\text{H}_5\text{OH}$$)?',
        '[{"id": "A", "text": "Hydroxyl group ($$-\\text{OH}$$)", "isCorrect": true}, {"id": "B", "text": "Aldehyde group ($$-\\text{CHO}$$)", "isCorrect": false}, {"id": "C", "text": "Carboxyl group ($$-\\text{COOH}$$)", "isCorrect": false}, {"id": "D", "text": "Ketone group ($$-\\text{CO}-$$)", "isCorrect": false}]',
        'A',
        'Alcohols have the functional group $$-\text{OH}$$ (hydroxyl). Carboxylic acids have $$-\text{COOH}$$, aldehydes have $$-\text{CHO}$$, and ketones have $$>\text{C}=\text{O}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000077'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'EASY',
        'REMEMBER',
        'What is an aqueous solution of $$5\%$$ to $$8\%$$ acetic acid in water commercially known as?',
        '[{"id": "A", "text": "Vinegar", "isCorrect": true}, {"id": "B", "text": "Formalin", "isCorrect": false}, {"id": "C", "text": "Rectified spirit", "isCorrect": false}, {"id": "D", "text": "Power alcohol", "isCorrect": false}]',
        'A',
        'Vinegar is a dilute $$5-8\%$$ aqueous solution of ethanoic (acetic) acid, extensively used as a food preservative and culinary flavoring.'
    ),
    (
        'a1100000-0000-0000-0000-000000000078'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'MEDIUM',
        'UNDERSTAND',
        'Saponification is the chemical reaction used to manufacture soaps by hydrolyzing vegetable oils or animal fats with:',
        '[{"id": "A", "text": "Sodium hydroxide ($$\\text{NaOH}$$) or Potassium hydroxide ($$\\text{KOH}$$)", "isCorrect": true}, {"id": "B", "text": "Hydrochloric acid ($$\\text{HCl}$$)", "isCorrect": false}, {"id": "C", "text": "Sulfuric acid ($$\\text{H}_2\\text{SO}_4$$)", "isCorrect": false}, {"id": "D", "text": "Sodium chloride ($$\\text{NaCl}$$)", "isCorrect": false}]',
        'A',
        'Saponification is alkaline hydrolysis of esters (triglycerides) using strong alkalis ($$\text{NaOH}$$ for hard bar soaps, $$\text{KOH}$$ for liquid/shaving soaps), yielding glycerol and fatty acid salts.'
    ),
    (
        'a1100000-0000-0000-0000-000000000079'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'HARD',
        'UNDERSTAND',
        'Why do synthetic detergents lather effectively in hard water while ordinary soaps form an insoluble scum?',
        '[{"id": "A", "text": "Detergent molecules form soluble calcium and magnesium sulfonates rather than insoluble curd", "isCorrect": true}, {"id": "B", "text": "Detergents contain active enzymes that precipitate water hardness immediately", "isCorrect": false}, {"id": "C", "text": "Detergents alter the pH of water to acidic immediately", "isCorrect": false}, {"id": "D", "text": "Detergents are completely inorganic mineral powders", "isCorrect": false}]',
        'A',
        'Soaps react with $$\text{Ca}^{2+}$$ and $$\text{Mg}^{2+}$$ in hard water to form insoluble curds (scum). Synthetic detergent sulfonates remain water-soluble and retain foaming efficiency.'
    ),
    (
        'a1100000-0000-0000-0000-000000000080'::uuid,
        'Chemistry',
        'Carbon and its Compounds',
        'HARD',
        'ANALYZE',
        'The catalytic hydrogenation of vegetable oils into solid vegetable ghee (vanaspati) is carried out using which metal catalyst?',
        '[{"id": "A", "text": "Nickel ($$\\text{Ni}$$)", "isCorrect": true}, {"id": "B", "text": "Iron ($$\\text{Fe}$$)", "isCorrect": false}, {"id": "C", "text": "Copper ($$\\text{Cu}$$)", "isCorrect": false}, {"id": "D", "text": "Lead ($$\\text{Pb}$$)", "isCorrect": false}]',
        'A',
        'Unsaturated vegetable oils with double bonds undergo addition of hydrogen across double bonds in the presence of finely divided Nickel catalyst at $$\approx 200^\circ\text{C}$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000081'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'EASY',
        'REMEMBER',
        'Which gas is the most abundant component of Earth''s atmosphere by volume?',
        '[{"id": "A", "text": "Nitrogen ($$\\approx 78.08\\%$$)", "isCorrect": true}, {"id": "B", "text": "Oxygen ($$\\approx 20.95\\%$$)", "isCorrect": false}, {"id": "C", "text": "Argon ($$\\approx 0.93\\%$$)", "isCorrect": false}, {"id": "D", "text": "Carbon dioxide ($$\\approx 0.04\\%$$)", "isCorrect": false}]',
        'A',
        'Nitrogen constitutes about $$78\%$$ of dry air by volume, followed by Oxygen ($$21\%$$), Argon ($$0.93\%$$), and Carbon dioxide ($$0.04\%$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000082'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'EASY',
        'REMEMBER',
        'Which atmospheric layer contains the maximum concentration of the protective Ozone layer ($$\text{O}_3$$)?',
        '[{"id": "A", "text": "Stratosphere", "isCorrect": true}, {"id": "B", "text": "Troposphere", "isCorrect": false}, {"id": "C", "text": "Mesosphere", "isCorrect": false}, {"id": "D", "text": "Thermosphere", "isCorrect": false}]',
        'A',
        'About $$90\%$$ of Earth''s ozone is located in the stratosphere between $$15\text{ km}$$ and $$35\text{ km}$$ above the Earth, shielding life from harmful solar ultraviolet (UV-B) rays.'
    ),
    (
        'a1100000-0000-0000-0000-000000000083'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'MEDIUM',
        'UNDERSTAND',
        'Which synthetic chemical substances were primarily responsible for depleting the stratospheric ozone layer over Antarctica?',
        '[{"id": "A", "text": "Chlorofluorocarbons (CFCs / Freons)", "isCorrect": true}, {"id": "B", "text": "Carbon dioxide ($$\\text{CO}_2$$)", "isCorrect": false}, {"id": "C", "text": "Methane ($$\\text{CH}_4$$)", "isCorrect": false}, {"id": "D", "text": "Sulfur hexafluoride ($$\\text{SF}_6$$)", "isCorrect": false}]',
        'A',
        'CFCs released chlorine free radicals under UV irradiation in the stratosphere, catalytically decomposing ozone molecules ($$\text{Cl}^\bullet + \text{O}_3 \to \text{ClO}^\bullet + \text{O}_2$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000084'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'EASY',
        'UNDERSTAND',
        'Which atmospheric pollutants are the primary chemical causes of Acid Rain?',
        '[{"id": "A", "text": "Sulfur dioxide ($$\\text{SO}_2$$) and Nitrogen oxides ($$\\text{NO}_x$$)", "isCorrect": true}, {"id": "B", "text": "Carbon monoxide ($$\\text{CO}$$) and Hydrogen gas", "isCorrect": false}, {"id": "C", "text": "Chlorofluorocarbons and Methane", "isCorrect": false}, {"id": "D", "text": "Ozone and Particulate matter", "isCorrect": false}]',
        'A',
        '$$\text{SO}_2$$ and $$\text{NO}_2$$ react with atmospheric moisture to form sulfuric acid ($$\text{H}_2\text{SO}_4$$) and nitric acid ($$\text{HNO}_3$$), lowering rainwater pH below $$5.6$$.'
    ),
    (
        'a1100000-0000-0000-0000-000000000085'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'MEDIUM',
        'UNDERSTAND',
        'Why is the marble of the Taj Mahal getting discolored and corroded (phenomenon termed ''Marble Cancer'')?',
        '[{"id": "A", "text": "Acid rain containing $$\\text{H}_2\\text{SO}_4$$ reacts with marble ($$\\text{CaCO}_3$$) to form calcium sulfate", "isCorrect": true}, {"id": "B", "text": "Direct solar UV radiation bleaching", "isCorrect": false}, {"id": "C", "text": "Deposition of carbon soot from local domestic cooking fires only", "isCorrect": false}, {"id": "D", "text": "Oxidation of iron impurities present naturally in Makrana marble", "isCorrect": false}]',
        'A',
        '$$\text{CaCO}_3 + \text{H}_2\text{SO}_4 \to \text{CaSO}_4 + \text{H}_2\text{O} + \text{CO}_2$$. Acid rain dissolves marble to form gypsum, causing flaking and yellowing.'
    ),
    (
        'a1100000-0000-0000-0000-000000000086'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'MEDIUM',
        'UNDERSTAND',
        'Why is Carbon monoxide ($$\text{CO}$$) highly poisonous and lethal when inhaled?',
        '[{"id": "A", "text": "It binds to hemoglobin $$200-300\\text{ times}$$ more strongly than oxygen, forming carboxyhemoglobin", "isCorrect": true}, {"id": "B", "text": "It burns respiratory tracts through caustic acid formation", "isCorrect": false}, {"id": "C", "text": "It coagulates blood platelets instantaneously", "isCorrect": false}, {"id": "D", "text": "It destroys lung alveoli membranes on contact", "isCorrect": false}]',
        'A',
        'Carbon monoxide exhibits an affinity for hemoglobin $$\approx 250\text{ times}$$ higher than oxygen, forming stable carboxyhemoglobin and causing tissue hypoxia (cellular suffocation).'
    ),
    (
        'a1100000-0000-0000-0000-000000000087'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'EASY',
        'REMEMBER',
        'Which gas is universally known as ''Laughing Gas'' for its mild euphoric and anesthetic properties?',
        '[{"id": "A", "text": "Nitrous oxide ($$\\text{N}_2\\text{O}$$)", "isCorrect": true}, {"id": "B", "text": "Nitric oxide ($$\\text{NO}$$)", "isCorrect": false}, {"id": "C", "text": "Nitrogen dioxide ($$\\text{NO}_2$$)", "isCorrect": false}, {"id": "D", "text": "Dinitrogen pentoxide ($$\\text{N}_2\\text{O}_5$$)", "isCorrect": false}]',
        'A',
        'Dinitrogen monoxide (Nitrous oxide, $$\text{N}_2\text{O}$$) is called laughing gas, discovered by Joseph Priestley and popularized as an anesthetic by Humphry Davy.'
    ),
    (
        'a1100000-0000-0000-0000-000000000088'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'MEDIUM',
        'UNDERSTAND',
        'Which gas has the highest contribution to the enhanced anthropogenic Greenhouse Effect causing global warming?',
        '[{"id": "A", "text": "Carbon dioxide ($$\\text{CO}_2$$)", "isCorrect": true}, {"id": "B", "text": "Methane ($$\\text{CH}_4$$)", "isCorrect": false}, {"id": "C", "text": "Nitrous oxide ($$\\text{N}_2\\text{O}$$)", "isCorrect": false}, {"id": "D", "text": "Ozone ($$\\text{O}_3$$)", "isCorrect": false}]',
        'A',
        'While methane is a more potent greenhouse gas per molecule, carbon dioxide accounts for roughly $$66\%$$ of total anthropogenic radiative forcing due to massive emissions from fossil fuel combustion.'
    ),
    (
        'a1100000-0000-0000-0000-000000000089'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'HARD',
        'ANALYZE',
        'Bhopal Gas Tragedy (December 1984), one of the world''s worst industrial chemical disasters, occurred due to the leakage of:',
        '[{"id": "A", "text": "Methyl isocyanate (MIC, $$\\text{CH}_3\\text{NCO}$$)", "isCorrect": true}, {"id": "B", "text": "Phosgene ($$\\text{COCl}_2$$)", "isCorrect": false}, {"id": "C", "text": "Potassium cyanide ($$\\text{KCN}$$)", "isCorrect": false}, {"id": "D", "text": "Carbon tetrachloride ($$\\text{CCl}_4$$)", "isCorrect": false}]',
        'A',
        'The Union Carbide pesticide plant leaked approximately 40 tonnes of highly toxic Methyl Isocyanate (MIC) gas into the atmosphere of Bhopal.'
    ),
    (
        'a1100000-0000-0000-0000-000000000090'::uuid,
        'Chemistry',
        'Environmental Chemistry and Gases',
        'MEDIUM',
        'REMEMBER',
        'Which chemical substance is commonly added to municipal drinking water supplies for chemical disinfection?',
        '[{"id": "A", "text": "Chlorine ($$\\text{Cl}_2$$) or Bleaching powder", "isCorrect": true}, {"id": "B", "text": "Potassium permanganate crystals only", "isCorrect": false}, {"id": "C", "text": "Copper sulfate", "isCorrect": false}, {"id": "D", "text": "Sulfur powder", "isCorrect": false}]',
        'A',
        'Chlorination using chlorine gas or hypochlorite kills pathogenic bacteria and viruses by oxidizing essential bacterial enzymes.'
    ),
    (
        'a1100000-0000-0000-0000-000000000091'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'Teflon, universally used as a non-stick coating for culinary cookware, is chemically known as:',
        '[{"id": "A", "text": "Polytetrafluoroethylene (PTFE)", "isCorrect": true}, {"id": "B", "text": "Polyvinyl chloride (PVC)", "isCorrect": false}, {"id": "C", "text": "Polyethylene terephthalate (PET)", "isCorrect": false}, {"id": "D", "text": "Polystyrene", "isCorrect": false}]',
        'A',
        'Teflon is Polytetrafluoroethylene ($$(-\text{CF}_2-\text{CF}_2-)_n$$), highly resistant to heat, acids, and chemical attack, with an exceptionally low friction coefficient.'
    ),
    (
        'a1100000-0000-0000-0000-000000000092'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'Which synthetic polymer was the first fully synthetic fiber produced entirely without natural raw materials in 1935?',
        '[{"id": "A", "text": "Nylon (Nylon-6,6)", "isCorrect": true}, {"id": "B", "text": "Rayon (Artificial silk)", "isCorrect": false}, {"id": "C", "text": "Polyester (Terylene)", "isCorrect": false}, {"id": "D", "text": "Acrylic", "isCorrect": false}]',
        'A',
        'Nylon-6,6 synthesized by Wallace Carothers was the first 100% synthetic fiber made from coal, water, and air. Rayon is regenerated cellulose (semi-synthetic).'
    ),
    (
        'a1100000-0000-0000-0000-000000000093'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'MEDIUM',
        'REMEMBER',
        'What is Bakelite, used for manufacturing electrical switches and cooker handles, classified as?',
        '[{"id": "A", "text": "Thermosetting polymer (Phenol-formaldehyde resin)", "isCorrect": true}, {"id": "B", "text": "Thermoplastic polymer", "isCorrect": false}, {"id": "C", "text": "Elastomer", "isCorrect": false}, {"id": "D", "text": "Natural biodegradable biopolymer", "isCorrect": false}]',
        'A',
        'Bakelite is a thermosetting cross-linked polymer that does not soften upon heating and is an excellent electrical and thermal insulator.'
    ),
    (
        'a1100000-0000-0000-0000-000000000094'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'MEDIUM',
        'UNDERSTAND',
        'Vulcanization of natural rubber is carried out by heating natural latex rubber with:',
        '[{"id": "A", "text": "Sulfur", "isCorrect": true}, {"id": "B", "text": "Carbon black only", "isCorrect": false}, {"id": "C", "text": "Phosphorus", "isCorrect": false}, {"id": "D", "text": "Chlorine gas", "isCorrect": false}]',
        'A',
        'Charles Goodyear discovered that heating natural rubber with sulfur creates disulfide cross-links between polyisoprene chains, increasing mechanical strength, elasticity, and heat resistance.'
    ),
    (
        'a1100000-0000-0000-0000-000000000095'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'What does the abbreviation NPK denote on commercial fertilizer packaging?',
        '[{"id": "A", "text": "Nitrogen ($$\\text{N}$$), Phosphorus ($$\\text{P}$$), and Potassium ($$\\text{K}$$)", "isCorrect": true}, {"id": "B", "text": "Nickel, Phosphorus, and Krypton", "isCorrect": false}, {"id": "C", "text": "Nitrogen, Potassium, and Calcium", "isCorrect": false}, {"id": "D", "text": "Sodium, Phosphorus, and Potassium", "isCorrect": false}]',
        'A',
        'NPK represents the three essential primary macronutrients required for plant growth: Nitrogen ($$\text{N}$$), Phosphorus ($$\text{P}$$), and Potassium ($$\text{K}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000096'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'What is the key chemical raw material added to Ordinary Portland Cement ($$\text{OPC}$$) clinker to retard its initial setting time?',
        '[{"id": "A", "text": "Gypsum ($$\\text{CaSO}_4 \\cdot 2\\text{H}_2\\text{O}$$)", "isCorrect": true}, {"id": "B", "text": "Alumina ($$\\text{Al}_2\\text{O}_3$$)", "isCorrect": false}, {"id": "C", "text": "Silica ($$\\text{SiO}_2$$)", "isCorrect": false}, {"id": "D", "text": "Magnesia ($$\\text{MgO}$$)", "isCorrect": false}]',
        'A',
        'About $$2-3\%$$ gypsum is interground with cement clinker to prevent flash setting by retarding the hydration of tricalcium aluminate ($$\text{C}_3\text{A}$$).'
    ),
    (
        'a1100000-0000-0000-0000-000000000097'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'MEDIUM',
        'REMEMBER',
        'What chemical compound was invented by Alfred Nobel in 1867 to stabilize nitroglycerin, creating Dynamite?',
        '[{"id": "A", "text": "Kieselguhr (diatomaceous earth) absorbing nitroglycerin", "isCorrect": true}, {"id": "B", "text": "Trinitrotoluene (TNT)", "isCorrect": false}, {"id": "C", "text": "Picric acid", "isCorrect": false}, {"id": "D", "text": "Gunpowder (Black powder)", "isCorrect": false}]',
        'A',
        'Alfred Nobel absorbed volatile liquid nitroglycerin into porous siliceous diatomaceous earth (kieselguhr), creating manageable and safe sticks of dynamite.'
    ),
    (
        'a1100000-0000-0000-0000-000000000098'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'Which class of pharmaceutical drugs is used to relieve bodily fever and reduce abnormally elevated body temperature?',
        '[{"id": "A", "text": "Antipyretics (e.g. Paracetamol)", "isCorrect": true}, {"id": "B", "text": "Antibiotics", "isCorrect": false}, {"id": "C", "text": "Antiseptics", "isCorrect": false}, {"id": "D", "text": "Tranquilizers", "isCorrect": false}]',
        'A',
        'Antipyretics (such as Paracetamol/Acetaminophen and Aspirin) act on the hypothalamus to lower elevated body temperature during fever. Analgesics relieve pain.'
    ),
    (
        'a1100000-0000-0000-0000-000000000099'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'EASY',
        'REMEMBER',
        'Who discovered the world''s first true antibiotic, Penicillin, from the mold Penicillium notatum in 1928?',
        '[{"id": "A", "text": "Alexander Fleming", "isCorrect": true}, {"id": "B", "text": "Louis Pasteur", "isCorrect": false}, {"id": "C", "text": "Robert Koch", "isCorrect": false}, {"id": "D", "text": "Edward Jenner", "isCorrect": false}]',
        'A',
        'Sir Alexander Fleming discovered penicillin in 1928 when staphylococci bacterial culture plates were cleared by accidental mold contamination.'
    ),
    (
        'a1100000-0000-0000-0000-000000000100'::uuid,
        'Chemistry',
        'Everyday Chemistry and Polymers',
        'MEDIUM',
        'UNDERSTAND',
        'Milk of Magnesia, widely consumed as an over-the-counter antacid to relieve stomach acidity and indigestion, is chemically:',
        '[{"id": "A", "text": "Magnesium hydroxide ($$\\text{Mg(OH)}_2$$)", "isCorrect": true}, {"id": "B", "text": "Magnesium sulfate ($$\\text{MgSO}_4$$)", "isCorrect": false}, {"id": "C", "text": "Magnesium carbonate ($$\\text{MgCO}_3$$)", "isCorrect": false}, {"id": "D", "text": "Magnesium chloride ($$\\text{MgCl}_2$$)", "isCorrect": false}]',
        'A',
        'Milk of Magnesia is a mild basic suspension of Magnesium hydroxide ($$\text{Mg(OH)}_2$$), which neutralizes hyperacidity in gastric juice ($$\text{HCl}$$) without damaging esophageal lining.'
    )
) AS v(id, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Science' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Chemistry' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
