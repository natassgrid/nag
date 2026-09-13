-- ============================================================
-- Seed Questions: Visual Diagrams, Geometry, Non-Verbal Reasoning & Geography (Issue #100)
-- Examination: SSC CGL / RRB NTPC Tier 1 & 2 Visual Aptitude Standards
-- Features Covered:
--   1. Non-Verbal Reasoning: Mirror Image with SVG diagram options (A/B/C/D)
--   2. Non-Verbal Reasoning: Paper Folding & Pattern Completion with 4 image options
--   3. Geometry: Circle with Tangents and Angle Subtended Diagram
--   4. Geometry: Right-Angled Triangle with Altitude to Hypotenuse Diagram
--   5. Physical Geography: Topographical Contour Line Elevation Map
--   6. Physical Geography: Subduction Zone Tectonic Plate Boundary Cross-Section
-- Format Standard: Valid UUIDs, JSONB options with imageUrl/imageAltText, LaTeX math, has_images = TRUE
-- UUID Range: a1270000-0000-0000-0000-000000000001 to a1270000-0000-0000-0000-000000000006
-- ============================================================

-- Step 1: Ensure Subjects exist
INSERT INTO question_service.subject (tenant_id, name, code, description)
VALUES
    ('default', 'General Intelligence and Reasoning', 'GIR', 'Logical, analytical, visual, and non-verbal reasoning'),
    ('default', 'Quantitative Aptitude / Mathematical Abilities', 'QAMA', 'Quantitative aptitude, geometry, mensuration, and arithmetic'),
    ('default', 'General Awareness', 'GA', 'Physical geography, environmental sciences, polity, and general sciences')
ON CONFLICT (name, tenant_id) DO NOTHING;

-- Step 2: Ensure Topics exist under respective Subjects
INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Non-Verbal Reasoning', 'Visual patterns, mirror reflections, spatial folding, and diagrammatic series')
) AS v(name, description)
WHERE s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Geometry', 'Euclidean plane geometry, circles, tangents, right triangles, and similarity')
) AS v(name, description)
WHERE s.name = 'Quantitative Aptitude / Mathematical Abilities' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
SELECT 'default', s.id, v.name, v.description
FROM question_service.subject s
CROSS JOIN (VALUES
    ('Physical Geography', 'Geomorphology, plate tectonics, topographic contour maps, and earth structure')
) AS v(name, description)
WHERE s.name = 'General Awareness' AND s.tenant_id = 'default'
ON CONFLICT (name, subject_id, tenant_id) DO NOTHING;

-- Step 3: Insert the Visual Assessment Questions
INSERT INTO question_service.question (
    id, tenant_id, subject_id, topic_id, subtopic_id,
    subject, topic, subtopic, difficulty, cognitive_level,
    question_type, content, options, answer_key, explanation,
    state, author_id, has_images
)
SELECT
    v.id,
    'default',
    s.id,
    t.id,
    NULL,
    v.subject_name,
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
    '00000000-0000-0000-0000-000000000001'::uuid,
    TRUE
FROM (VALUES
    -- =========================================================================
    -- Q1: Non-Verbal Reasoning: Mirror Image (Vertical Mirror Line XY)
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000001'::uuid,
        'General Intelligence and Reasoning',
        'Non-Verbal Reasoning',
        'Mirror Images',
        'MEDIUM',
        'ANALYZE',
        'Choose the correct mirror image of the given figure when the mirror line **XY** is placed vertically to the right of the figure:

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 120" width="220" height="120" style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px;">
  <rect x="20" y="20" width="80" height="80" fill="none" stroke="#1e293b" stroke-width="2"/>
  <polygon points="30,80 60,30 90,80" fill="#3b82f6" stroke="#1d4ed8" stroke-width="2"/>
  <circle cx="80" cy="40" r="10" fill="#ef4444"/>
  <!-- Mirror line XY -->
  <line x1="140" y1="10" x2="140" y2="110" stroke="#64748b" stroke-width="3" stroke-dasharray="6,4"/>
  <text x="145" y="25" font-family="sans-serif" font-size="14" font-weight="bold" fill="#475569">X</text>
  <text x="145" y="105" font-family="sans-serif" font-size="14" font-weight="bold" fill="#475569">Y</text>
</svg>',
        '[
            {"id": "A", "text": "Option A", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%231e293b'' stroke-width=''2''/><polygon points=''20,80 50,30 80,80'' fill=''%233b82f6'' stroke=''%231d4ed8'' stroke-width=''2''/><circle cx=''70'' cy=''30'' r=''10'' fill=''%23ef4444''/></svg>", "imageAltText": "Figure with red circle on top-right", "isCorrect": false},
            {"id": "B", "text": "Option B", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%231e293b'' stroke-width=''2''/><polygon points=''20,80 50,30 80,80'' fill=''%233b82f6'' stroke=''%231d4ed8'' stroke-width=''2''/><circle cx=''30'' cy=''30'' r=''10'' fill=''%23ef4444''/></svg>", "imageAltText": "Correct laterally inverted mirror image with red circle on top-left", "isCorrect": true},
            {"id": "C", "text": "Option C", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%231e293b'' stroke-width=''2''/><polygon points=''20,30 50,80 80,30'' fill=''%233b82f6'' stroke=''%231d4ed8'' stroke-width=''2''/><circle cx=''30'' cy=''70'' r=''10'' fill=''%23ef4444''/></svg>", "imageAltText": "Vertically inverted water image", "isCorrect": false},
            {"id": "D", "text": "Option D", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%231e293b'' stroke-width=''2''/><polygon points=''20,80 50,30 80,80'' fill=''%233b82f6'' stroke=''%231d4ed8'' stroke-width=''2''/><circle cx=''30'' cy=''70'' r=''10'' fill=''%23ef4444''/></svg>", "imageAltText": "Figure with red circle on bottom-left", "isCorrect": false}
        ]',
        'B',
        'When a vertical mirror line XY is placed to the right, lateral inversion occurs: elements on the right side of the object appear on the left side of the reflection, while vertical positions remain unchanged. In the original figure, the red circle is near the right edge (top-right); in the mirror reflection, it must be located near the left edge (top-left).'
    ),

    -- =========================================================================
    -- Q2: Non-Verbal Reasoning: Paper Folding & Punching
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000002'::uuid,
        'General Intelligence and Reasoning',
        'Non-Verbal Reasoning',
        'Paper Folding',
        'MEDIUM',
        'APPLY',
        'A square paper is folded twice along the dashed lines into a quarter-square and two circular holes are punched as shown in the problem figure. How will the paper appear when completely unfolded?

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 340 100" width="340" height="100" style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px;">
  <!-- Step 1 -->
  <rect x="20" y="15" width="70" height="70" fill="#f1f5f9" stroke="#334155" stroke-width="2"/>
  <line x1="55" y1="15" x2="55" y2="85" stroke="#94a3b8" stroke-dasharray="4,3" stroke-width="2"/>
  <!-- Step 2 -->
  <line x1="100" y1="50" x2="120" y2="50" stroke="#334155" stroke-width="2" marker-end="url(#arrow)"/>
  <rect x="130" y="15" width="35" height="70" fill="#e2e8f0" stroke="#334155" stroke-width="2"/>
  <line x1="130" y1="50" x2="165" y2="50" stroke="#94a3b8" stroke-dasharray="4,3" stroke-width="2"/>
  <!-- Step 3 (punched) -->
  <line x1="175" y1="50" x2="195" y2="50" stroke="#334155" stroke-width="2"/>
  <rect x="210" y="15" width="35" height="35" fill="#cbd5e1" stroke="#334155" stroke-width="2"/>
  <circle cx="222" cy="27" r="4" fill="#0f172a"/>
  <circle cx="233" cy="38" r="4" fill="#0f172a"/>
</svg>',
        '[
            {"id": "A", "text": "Option A", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%23334155'' stroke-width=''2''/><circle cx=''35'' cy=''35'' r=''4'' fill=''%230f172a''/><circle cx=''65'' cy=''65'' r=''4'' fill=''%230f172a''/></svg>", "imageAltText": "Two diagonal holes", "isCorrect": false},
            {"id": "B", "text": "Option B", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%23334155'' stroke-width=''2''/><circle cx=''30'' cy=''30'' r=''4'' fill=''%230f172a''/><circle cx=''70'' cy=''30'' r=''4'' fill=''%230f172a''/><circle cx=''30'' cy=''70'' r=''4'' fill=''%230f172a''/><circle cx=''70'' cy=''70'' r=''4'' fill=''%230f172a''/></svg>", "imageAltText": "Four symmetrical corner holes", "isCorrect": false},
            {"id": "C", "text": "Option C", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%23334155'' stroke-width=''2''/><circle cx=''32'' cy=''27'' r=''4'' fill=''%230f172a''/><circle cx=''43'' cy=''38'' r=''4'' fill=''%230f172a''/><circle cx=''68'' cy=''27'' r=''4'' fill=''%230f172a''/><circle cx=''57'' cy=''38'' r=''4'' fill=''%230f172a''/><circle cx=''32'' cy=''73'' r=''4'' fill=''%230f172a''/><circle cx=''43'' cy=''62'' r=''4'' fill=''%230f172a''/><circle cx=''68'' cy=''73'' r=''4'' fill=''%230f172a''/><circle cx=''57'' cy=''62'' r=''4'' fill=''%230f172a''/></svg>", "imageAltText": "Eight holes reflecting across horizontal and vertical axes", "isCorrect": true},
            {"id": "D", "text": "Option D", "imageUrl": "data:image/svg+xml;utf8,<svg xmlns=''http://www.w3.org/2000/svg'' viewBox=''0 0 100 100'' width=''80'' height=''80''><rect x=''10'' y=''10'' width=''80'' height=''80'' fill=''none'' stroke=''%23334155'' stroke-width=''2''/><circle cx=''50'' cy=''30'' r=''4'' fill=''%230f172a''/><circle cx=''50'' cy=''70'' r=''4'' fill=''%230f172a''/><circle cx=''30'' cy=''50'' r=''4'' fill=''%230f172a''/><circle cx=''70'' cy=''50'' r=''4'' fill=''%230f172a''/></svg>", "imageAltText": "Four edge holes", "isCorrect": false}
        ]',
        'C',
        'Folding a paper into 4 layers and punching 2 holes yields $2 \times 4 = 8$ symmetrical holes upon unfolding, reflected symmetrically across both the vertical and horizontal fold axes.'
    ),

    -- =========================================================================
    -- Q3: Geometry: Circle with Tangents from an External Point
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000003'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Geometry',
        'Circles and Tangents',
        'EASY',
        'UNDERSTAND',
        'In the figure below, two tangents $$PT$$ and $$PT''''$$ are drawn to a circle with center $$O$$ from an external point $$P$$. If $$\\angle TPT'''' = 60^\\circ$$, what is the measure of the central angle $$\\angle TOT''''$$?

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 180" width="300" height="180" style="background:#ffffff; border:1px solid #e2e8f0; border-radius:6px;">
  <!-- Circle -->
  <circle cx="180" cy="90" r="60" fill="none" stroke="#2563eb" stroke-width="2"/>
  <circle cx="180" cy="90" r="3" fill="#1e293b"/>
  <text x="185" y="85" font-family="sans-serif" font-size="14" font-weight="bold">O</text>
  <!-- Tangent points and point P -->
  <!-- T at (144.6, 42.1), T'''' at (144.6, 137.9), P at (60, 90) -->
  <line x1="60" y1="90" x2="145" y2="42" stroke="#0f172a" stroke-width="2"/>
  <line x1="60" y1="90" x2="145" y2="138" stroke="#0f172a" stroke-width="2"/>
  <!-- Radii OT and OT'''' -->
  <line x1="180" y1="90" x2="145" y2="42" stroke="#dc2626" stroke-width="2" stroke-dasharray="4,2"/>
  <line x1="180" y1="90" x2="145" y2="138" stroke="#dc2626" stroke-width="2" stroke-dasharray="4,2"/>
  <text x="45" y="95" font-family="sans-serif" font-size="14" font-weight="bold">P</text>
  <text x="140" y="32" font-family="sans-serif" font-size="14" font-weight="bold">T</text>
  <text x="140" y="155" font-family="sans-serif" font-size="14" font-weight="bold">T''''</text>
  <!-- Angle at P -->
  <text x="85" y="95" font-family="sans-serif" font-size="12" fill="#2563eb">60°</text>
</svg>',
        '[
            {"id": "A", "text": "90°", "isCorrect": false},
            {"id": "B", "text": "120°", "isCorrect": true},
            {"id": "C", "text": "150°", "isCorrect": false},
            {"id": "D", "text": "180°", "isCorrect": false}
        ]',
        'B',
        'In quadrilateral $$PTOT''''$$, the tangent is perpendicular to the radius at the point of contact, so $$\\angle PTO = 90^\\circ$$ and $$\\angle PT''''O = 90^\\circ$$. Since the sum of all interior angles of a quadrilateral is $$360^\\circ$$, we have:
$$\\angle TOT'''' + \\angle TPT'''' + \\angle PTO + \\angle PT''''O = 360^\\circ$$
$$\\angle TOT'''' + 60^\\circ + 90^\\circ + 90^\\circ = 360^\\circ \\implies \\angle TOT'''' = 360^\\circ - 240^\\circ = 120^\\circ$$.'
    ),

    -- =========================================================================
    -- Q4: Geometry: Right-Triangle Altitude Theorem
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000004'::uuid,
        'Quantitative Aptitude / Mathematical Abilities',
        'Geometry',
        'Triangles and Similarity',
        'MEDIUM',
        'APPLY',
        'In the right-angled triangle $$\\triangle ABC$$ shown below, $$\\angle B = 90^\\circ$$ and segment $$BD$$ is perpendicular to hypotenuse $$AC$$. If $$AD = 4\\text{ cm}$$ and $$DC = 9\\text{ cm}$$, what is the length of altitude $$BD$$?

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 180" width="320" height="180" style="background:#ffffff; border:1px solid #e2e8f0; border-radius:6px;">
  <!-- Triangle vertices: A(30,140), C(290,140), B(110,36) -->
  <!-- Hypotenuse AC along horizontal baseline y=140 -->
  <polygon points="30,140 290,140 110,36" fill="#f8fafc" stroke="#1e293b" stroke-width="2"/>
  <!-- Altitude BD from B(110,36) to D(110,140) -->
  <line x1="110" y1="36" x2="110" y2="140" stroke="#2563eb" stroke-width="2" stroke-dasharray="4,2"/>
  <!-- Right-angle mark at D -->
  <rect x="110" y="125" width="15" height="15" fill="none" stroke="#2563eb" stroke-width="1.5"/>
  <!-- Right-angle mark at B -->
  <polygon points="100,48 112,58 122,46 110,36" fill="none" stroke="#dc2626" stroke-width="1.5"/>
  <!-- Labels -->
  <text x="15" y="145" font-family="sans-serif" font-size="14" font-weight="bold">A</text>
  <text x="105" y="25" font-family="sans-serif" font-size="14" font-weight="bold">B</text>
  <text x="300" y="145" font-family="sans-serif" font-size="14" font-weight="bold">C</text>
  <text x="105" y="160" font-family="sans-serif" font-size="14" font-weight="bold">D</text>
  <!-- Segment lengths -->
  <text x="60" y="130" font-family="sans-serif" font-size="12" fill="#475569">4 cm</text>
  <text x="190" y="130" font-family="sans-serif" font-size="12" fill="#475569">9 cm</text>
</svg>',
        '[
            {"id": "A", "text": "4.5 cm", "isCorrect": false},
            {"id": "B", "text": "6.0 cm", "isCorrect": true},
            {"id": "C", "text": "6.5 cm", "isCorrect": false},
            {"id": "D", "text": "7.2 cm", "isCorrect": false}
        ]',
        'B',
        'By the Right Triangle Altitude Theorem (Geometric Mean Theorem), the altitude to the hypotenuse is the geometric mean of the two segments into which it divides the hypotenuse:
$$BD^2 = AD \\times DC$$
$$BD^2 = 4 \\times 9 = 36$$
$$BD = \\sqrt{36} = 6\\text{ cm}$$.'
    ),

    -- =========================================================================
    -- Q5: Physical Geography: Topographical Contour Line Elevation Map
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000005'::uuid,
        'General Awareness',
        'Physical Geography',
        'Geomorphology and Cartography',
        'EASY',
        'UNDERSTAND',
        'Examine the topographical contour map illustrated below. What slope characteristic is indicated at location **X** compared to location **Y**?

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 340 180" width="340" height="180" style="background:#f0fdf4; border:1px solid #bbf7d0; border-radius:6px;">
  <!-- Contour lines for a hill -->
  <!-- Outermost 100m -->
  <path d="M 40,90 Q 70,30 170,30 Q 290,30 310,90 Q 290,150 170,150 Q 70,150 40,90 Z" fill="none" stroke="#15803d" stroke-width="1.5"/>
  <text x="45" y="95" font-family="sans-serif" font-size="10" fill="#15803d">100m</text>
  <!-- 200m -->
  <path d="M 70,90 Q 90,45 170,45 Q 260,45 280,90 Q 260,135 170,135 Q 90,135 70,90 Z" fill="none" stroke="#15803d" stroke-width="1.5"/>
  <text x="75" y="95" font-family="sans-serif" font-size="10" fill="#15803d">200m</text>
  <!-- 300m (compressed on left side X, stretched on right side Y) -->
  <path d="M 85,90 Q 100,60 170,60 Q 230,60 245,90 Q 230,120 170,120 Q 100,120 85,90 Z" fill="none" stroke="#15803d" stroke-width="1.5"/>
  <!-- 400m peak -->
  <path d="M 95,90 Q 105,75 140,75 Q 180,75 190,90 Q 180,105 140,105 Q 105,105 95,90 Z" fill="#dcfce7" stroke="#15803d" stroke-width="2"/>
  <text x="135" y="94" font-family="sans-serif" font-size="10" font-weight="bold" fill="#166534">400m</text>
  <!-- Points X and Y -->
  <circle cx="60" cy="90" r="3" fill="#dc2626"/>
  <text x="56" y="80" font-family="sans-serif" font-size="12" font-weight="bold" fill="#dc2626">X</text>
  <circle cx="270" cy="90" r="3" fill="#2563eb"/>
  <text x="266" y="80" font-family="sans-serif" font-size="12" font-weight="bold" fill="#2563eb">Y</text>
</svg>',
        '[
            {"id": "A", "text": "Location X represents a steep slope, while location Y represents a gentle slope.", "isCorrect": true},
            {"id": "B", "text": "Location X represents a gentle slope, while location Y represents a steep cliff.", "isCorrect": false},
            {"id": "C", "text": "Location X represents a river valley, while location Y represents a flat plateau.", "isCorrect": false},
            {"id": "D", "text": "Location X represents an underwater canyon, while location Y represents a sand dune.", "isCorrect": false}
        ]',
        'A',
        'In cartography and contour mapping, closely spaced contour lines denote a rapid change in elevation over a short horizontal distance, indicating a steep slope (as seen at point X). Conversely, widely spaced contour lines indicate a gradual change in elevation, representing a gentle slope (as seen at point Y).'
    ),

    -- =========================================================================
    -- Q6: Physical Geography: Tectonic Plate Boundary (Subduction Zone)
    -- =========================================================================
    (
        'a1270000-0000-0000-0000-000000000006'::uuid,
        'General Awareness',
        'Physical Geography',
        'Plate Tectonics',
        'HARD',
        'ANALYZE',
        'The cross-sectional diagram below depicts an oceanic-continental convergent plate boundary where denser oceanic lithosphere subducts beneath continental crust. Which significant oceanic geomorphic feature is formed at zone **Z**?

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 180" width="360" height="180" style="background:#f8fafc; border:1px solid #cbd5e1; border-radius:6px;">
  <!-- Ocean water -->
  <rect x="10" y="40" width="170" height="40" fill="#bae6fd" opacity="0.7"/>
  <text x="40" y="55" font-family="sans-serif" font-size="11" fill="#0369a1">Oceanic Water</text>
  <!-- Continental Plate -->
  <polygon points="170,40 340,40 340,160 210,160 170,110" fill="#fde68a" stroke="#b45309" stroke-width="2"/>
  <text x="230" y="70" font-family="sans-serif" font-size="11" font-weight="bold" fill="#78350f">Continental Crust</text>
  <!-- Volcanic mountain on continent -->
  <polygon points="230,40 250,15 270,40" fill="#ea580c" stroke="#9a3412" stroke-width="1.5"/>
  <text x="230" y="10" font-family="sans-serif" font-size="10" fill="#9a3412">Volcanic Arc</text>
  <!-- Oceanic Plate Subducting -->
  <polygon points="10,80 160,80 230,160 190,160 140,105 10,105" fill="#94a3b8" stroke="#334155" stroke-width="2"/>
  <text x="40" y="98" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1e293b">Oceanic Crust (Denser)</text>
  <!-- Subduction arrow -->
  <line x1="170" y1="95" x2="200" y2="135" stroke="#ef4444" stroke-width="2.5"/>
  <!-- Zone Z (Trench) -->
  <circle cx="165" cy="80" r="14" fill="none" stroke="#dc2626" stroke-width="2" stroke-dasharray="3,2"/>
  <text x="155" y="72" font-family="sans-serif" font-size="13" font-weight="bold" fill="#dc2626">Z</text>
</svg>',
        '[
            {"id": "A", "text": "Mid-Oceanic Divergent Ridge", "isCorrect": false},
            {"id": "B", "text": "Deep Ocean Trench", "isCorrect": true},
            {"id": "C", "text": "Transform Fault Fracture Zone", "isCorrect": false},
            {"id": "D", "text": "Continental Rift Valley", "isCorrect": false}
        ]',
        'B',
        'At convergent boundaries where a dense oceanic plate collides with and subducts beneath a buoyant continental plate, the bending of the descending slab forms a steep, deep V-shaped depression on the seafloor called a Deep Ocean Trench (such as the Mariana Trench or Peru-Chile Trench at zone Z).'
    )
) AS v(id, subject_name, topic_name, subtopic_name, difficulty, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s ON s.name = v.subject_name AND s.tenant_id = 'default'
JOIN question_service.topic t ON t.name = v.topic_name AND t.subject_id = s.id AND t.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
