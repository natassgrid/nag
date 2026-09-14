-- SPDX-License-Identifier: AGPL-3.0-only
-- Flyway Migration: V2_24
-- Seed Data: General Intelligence and Reasoning - Statement and Conclusion (50 HARD Questions)
-- Resolves PaperAssemblyService insufficient questions gap for rule:
-- subject='General Intelligence and Reasoning', topic='Statement and Conclusion', difficulty='HARD'

SET search_path TO question_service, public;

-- Step 1: Ensure Subject, Topic, and Subtopics exist
DO $$
DECLARE
    v_tenant_id VARCHAR := 'default';
    v_subj_id BIGINT;
    v_top_id BIGINT;
BEGIN
    -- Subject: General Intelligence and Reasoning
    SELECT id INTO v_subj_id FROM question_service.subject WHERE name = 'General Intelligence and Reasoning' AND tenant_id = v_tenant_id LIMIT 1;
    IF v_subj_id IS NULL THEN
        INSERT INTO question_service.subject (tenant_id, name, code, description)
        VALUES (v_tenant_id, 'General Intelligence and Reasoning', 'GIR', 'General Intelligence and Reasoning covering verbal and non-verbal reasoning, deductive logic, and critical evaluation')
        RETURNING id INTO v_subj_id;
    END IF;

    -- Topic: Statement and Conclusion
    SELECT id INTO v_top_id FROM question_service.topic WHERE name = 'Statement and Conclusion' AND subject_id = v_subj_id AND tenant_id = v_tenant_id LIMIT 1;
    IF v_top_id IS NULL THEN
        INSERT INTO question_service.topic (tenant_id, subject_id, name, description)
        VALUES (v_tenant_id, v_subj_id, 'Statement and Conclusion', 'Analytical, deductive, and formal logic conclusions derived strictly from given statements')
        RETURNING id INTO v_top_id;
    END IF;

    -- Subtopics
    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Direct & Indirect Inferences', 'Definite deductions, transitive relationships, and strict implication')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Logical Fallacies & Conditional Deductions', 'Contrapositive reasoning, affirming the consequent, and denial of antecedent')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Cause, Effect and Assertion-Reasoning', 'Distinguishing necessary vs sufficient causality and validating causal conclusions')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;

    INSERT INTO question_service.subtopic (tenant_id, topic_id, name, description)
    VALUES (v_tenant_id, v_top_id, 'Multi-Statement Analytical Conclusions', 'Complex multi-premise reasoning under universal and existential quantifiers')
    ON CONFLICT (name, topic_id, tenant_id) DO NOTHING;
END $$;

-- Step 2: Insert 50 HARD Questions for Statement and Conclusion
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
    'Statement and Conclusion',
    v.subtopic_name,
    'HARD',
    v.cognitive_level,
    'SINGLE_MCQ',
    v.content,
    v.options::jsonb,
    v.answer_key,
    v.explanation,
    'APPROVED',
    '00000000-0000-0000-0000-000000000001'::uuid
FROM (VALUES
    -- 1. Conditional Implication & Contrapositive
    (
        'a1020024-0000-0000-0000-000000000001'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. If a sovereign nation runs a persistent trade surplus with all trade partners, its foreign exchange reserves invariably expand unless capital flight exceeds the current account surplus.\n2. Nation $$X$$ experienced a marked contraction in its foreign exchange reserves during fiscal year 2025 while maintaining a trade surplus with every partner.\n\n**Conclusions:**\nI. In fiscal year 2025, capital flight from Nation $$X$$ exceeded its current account surplus.\nII. Nation $$X$$ must have devalued its domestic currency during fiscal year 2025.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Let $$P$$ = persistent trade surplus, $$Q$$ = foreign exchange reserves expand, and $$R$$ = capital flight exceeds current account surplus. The premise establishes $$(P \\\\land \\\\neg R) \\\\implies Q$$. By contrapositive: $$(P \\\\land \\\\neg Q) \\\\implies R$$. Given $$P$$ is true and $$Q$$ is false (reserves contracted), $$R$$ must strictly hold. Hence conclusion I is logically valid. No information regarding currency devaluation is provided, so II is ungrounded.'
    ),
    -- 2. Multi-Premise Categorical Syllogism
    (
        'a1020024-0000-0000-0000-000000000002'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. Only entities possessing a valid cryptographic certificate ($$C$$) are permitted to sign verifiable digital assertions ($$V$$).\n2. No entity with an expired revocation list timestamp ($$R$$) possesses a valid cryptographic certificate ($$C$$).\n3. Some distributed consensus nodes ($$D$$) have an expired revocation list timestamp ($$R$$).\n4. All distributed consensus nodes ($$D$$) generate cryptographically sealed blocks ($$B$$).\n\n**Conclusions:**\nI. Some entities that generate cryptographically sealed blocks cannot sign verifiable digital assertions.\nII. No entity with an expired revocation list timestamp can sign verifiable digital assertions.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1), $$V \\\\subseteq C$$. From (2), $$R \\\\cap C = \\\\emptyset$$, which implies $$R \\\\cap V = \\\\emptyset$$ (No $$R$$ can sign $$V$$), proving Conclusion II. From (3) & (4), there exists $$x \\\\in D$$ such that $$x \\\\in R$$ and $$x \\\\in B$$. Since $$x \\\\in R$$, $$x \\\\notin V$$. Thus $$x$$ is an entity generating $$B$$ that cannot sign $$V$$, proving Conclusion I. Both conclusions follow definitely.'
    ),
    -- 3. Necessary vs Sufficient Conditions
    (
        'a1020024-0000-0000-0000-000000000003'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. A necessary and sufficient condition for an operating system kernel to guarantee deterministic real-time scheduling is the total absence of unbounded priority inversions.\n2. Priority inheritance protocol is sufficient to eliminate unbounded priority inversions, but it is not the only mechanism capable of doing so.\n3. Microkernel $$M$$ does not implement priority inheritance protocol.\n\n**Conclusions:**\nI. Microkernel $$M$$ cannot guarantee deterministic real-time scheduling.\nII. If Microkernel $$M$$ implements priority ceiling emulation, it may guarantee deterministic real-time scheduling.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Priority inheritance is sufficient but not necessary to eliminate unbounded priority inversions. The absence of priority inheritance does not preclude other mechanisms (such as priority ceiling emulation) from eliminating unbounded priority inversions and thereby achieving deterministic scheduling. Hence I is false (fallacy of denying the antecedent) and II is a valid possibility.'
    ),
    -- 4. Statistical Deduction & Distribution
    (
        'a1020024-0000-0000-0000-000000000004'::uuid,
        'Direct & Indirect Inferences',
        'EVALUATE',
        '**Statements:**\n1. In a cohort of 500 distributed database nodes, exactly $$85\\\\%$$ have SSD storage and exactly $$75\\\\%$$ have redundant network interfaces ($$NICs$$).\n2. All nodes with both SSD storage and redundant $$NICs$$ maintain zero replication lag under peak write pressure.\n\n**Conclusions:**\nI. At least 300 nodes in the cohort maintain zero replication lag under peak write pressure.\nII. At most 425 nodes in the cohort maintain zero replication lag under peak write pressure.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Using Bonferroni / Fréchet intersection bounds: $$|A \\\\cap B| \\\\ge |A| + |B| - |U| = 0.85(500) + 0.75(500) - 500 = 425 + 375 - 500 = 300$$. Thus, at least 300 nodes have both SSD and redundant NICs and strictly achieve zero lag (Conclusion I follows). However, nodes outside the intersection might also achieve zero lag through other means, so we cannot assert an upper bound of 425 on zero-lag nodes (Conclusion II does not necessarily follow).'
    ),
    -- 5. Exclusive Disjunction & Transitive Implication
    (
        'a1020024-0000-0000-0000-000000000005'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. Every transaction in ledger $$L$$ is either cryptographically validated ($$V$$) or flagged for forensic audit ($$F$$), but never both simultaneously.\n2. All unencrypted cross-border transactions ($$U$$) are flagged for forensic audit ($$F$$).\n3. Transaction $$T_1$$ is an unencrypted cross-border transaction.\n4. Transaction $$T_2$$ is cryptographically validated.\n\n**Conclusions:**\nI. Transaction $$T_1$$ is not cryptographically validated.\nII. Transaction $$T_2$$ is not an unencrypted cross-border transaction.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1), $$V \\\\oplus F$$ (disjoint partition). From (2) and (3), $$T_1 \\\\in U \\\\implies T_1 \\\\in F$$. Since $$F \\\\cap V = \\\\emptyset$$, $$T_1 \\\\notin V$$ (Conclusion I follows). For (4), $$T_2 \\\\in V \\\\implies T_2 \\\\notin F$$. By contrapositive of (2), $$T_2 \\\\notin F \\\\implies T_2 \\\\notin U$$ (Conclusion II follows).'
    ),
    -- 6. Complex Causal Chain & Intermediate Reversal
    (
        'a1020024-0000-0000-0000-000000000006'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. Severe atmospheric particulate pollution ($$PM_{2.5} > 300\\\\ \\\\mu g/m^3$$) triggers statutory emergency factory shutdowns only when accompanied by thermal inversion layers.\n2. In Industrial Sector $$Z$$, statutory emergency factory shutdowns were enacted throughout November.\n3. Thermal inversion layers were consistently present over Industrial Sector $$Z$$ throughout November.\n\n**Conclusions:**\nI. Severe atmospheric particulate pollution occurred over Industrial Sector $$Z$$ throughout November.\nII. The presence of thermal inversion layers alone is insufficient to trigger emergency factory shutdowns.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The statement specifies that severe pollution triggers shutdowns *only when* thermal inversion is present (thermal inversion is a necessary condition for pollution to cause shutdowns, not necessarily the sole cause of shutdowns, as other statutory grounds might exist). Deductively concluding that severe pollution *must* have occurred commits the converse fallacy (I is invalid). But statement 1 explicitly indicates inversion is a conditional modifier for pollution rather than an independent standalone trigger, validating conclusion II.'
    ),
    -- 7. Modus Tollens with Quantifiers
    (
        'a1020024-0000-0000-0000-000000000007'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. Any distributed ledger consensus protocol that satisfies Byzantine Fault Tolerance ($$BFT$$) under asynchronous network partitions must sacrifice deterministic finality time ($$D$$).\n2. Consensus protocol $$Omega$$ guarantees deterministic finality time under all network partitioning conditions.\n\n**Conclusions:**\nI. Consensus protocol $$Omega$$ does not satisfy Byzantine Fault Tolerance under asynchronous network partitions.\nII. Under synchronous networks without partitions, protocol $$Omega$$ cannot achieve Byzantine Fault Tolerance.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Statement 1 is an implication: $$BFT_{async} \\\\implies \\\\neg D$$. By Modus Tollens, $$D \\\\implies \\\\neg BFT_{async}$$. Since protocol $$Omega$$ guarantees $$D$$, it logically follows that $$Omega$$ cannot satisfy $$BFT_{async}$$ (Conclusion I is valid). Premise 1 provides no constraints on synchronous network performance, so Conclusion II is an invalid overextension.'
    ),
    -- 8. Negative Universal and Particular Affirmative
    (
        'a1020024-0000-0000-0000-000000000008'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. No non-deterministic polynomial ($$NP$$) complete problem has a known deterministic polynomial-time algorithm ($$P$$).\n2. Some combinatorial graph partitioning problems ($$G$$) are $$NP$$-complete.\n3. All optimization routines utilized in compiler optimization engine $$K$$ have polynomial-time running algorithms ($$P$$).\n\n**Conclusions:**\nI. None of the optimization routines in compiler engine $$K$$ solve any $$NP$$-complete problem via a known deterministic method.\nII. Some combinatorial graph partitioning problems cannot be solved by any routine in compiler engine $$K$$ via a known deterministic algorithm.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Let $$NPC$$ = set of NP-complete problems. Premise (1): $$NPC \\\\cap P_{known} = \\\\emptyset$$. Premise (3): $$K \\\\subseteq P_{known}$$. Therefore, $$K \\\\cap NPC = \\\\emptyset$$, which establishes Conclusion I. Premise (2) states $$\\exists g \\\\in G$$ such that $$g \\\\in NPC$$. Since no element in $$K$$ can solve an $$NPC$$ problem deterministically in $$P$$, those specific graph problems $$g$$ cannot be solved by $$K$$ via known deterministic polynomial methods. Both conclusions follow.'
    ),
    -- 9. Biconditional Elimination
    (
        'a1020024-0000-0000-0000-000000000009'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. A cryptographic digital signature scheme $$S$$ is existentially unforgeable under chosen-message attacks ($$EUF-CMA$$) if and only if the discrete logarithm problem over the underlying elliptic curve group $$G$$ is computationally intractable.\n2. In group $$G_1$$, a polynomial-time quantum algorithm was discovered that computes discrete logarithms in $$O((\\\\log N)^3)$$ steps.\n\n**Conclusions:**\nI. Signature scheme $$S$$ instantiated over group $$G_1$$ is not $$EUF-CMA$$ secure against quantum adversaries.\nII. Every cryptographic hash function operating over group $$G_1$$ produces collisions in polynomial time.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Biconditional equivalence: $$EUF-CMA \\\\iff \\\\text{Discrete Log is intractable}$$. If a polynomial-time quantum algorithm solves discrete logarithms, the problem is no longer computationally intractable against quantum adversaries. Therefore, by the biconditional equivalence, $$S$$ instantiated over $$G_1$$ loses its $$EUF-CMA$$ security guarantee (Conclusion I follows). No statement is made regarding hash function collision resistance, making II unfounded.'
    ),
    -- 10. Syllogism with Complementary Fallacy
    (
        'a1020024-0000-0000-0000-000000000010'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'EVALUATE',
        '**Statements:**\n1. Most enterprise microservices ($$> 50\\\\%$$) adopt asynchronous event-driven messaging for write operations.\n2. Most enterprise microservices ($$> 50\\\\%$$) utilize distributed cache invalidation hooks.\n\n**Conclusions:**\nI. At least one enterprise microservice utilizes both asynchronous event-driven messaging and distributed cache invalidation hooks.\nII. No microservice that avoids asynchronous messaging can implement distributed cache invalidation hooks.',
        '[{"id":"A","text":"Neither conclusion I nor II follows","isCorrect":false},{"id":"B","text":"Only conclusion I follows","isCorrect":true},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Both conclusions I and II follow","isCorrect":false}]',
        'B',
        'Let $$U$$ be the finite universe of enterprise microservices. If set $$A$$ has $$|A| > 0.5|U|$$ and set $$B$$ has $$|B| > 0.5|U|$$, by the pigeonhole principle / Fréchet lower bound: $$|A \\\\cap B| = |A| + |B| - |A \\\\cup B| > 0.5|U| + 0.5|U| - |U| = 0$$. Hence $$|A \\\\cap B| \\\\ge 1$$, proving Conclusion I. Conclusion II is an unsupported universal negative.'
    ),
    -- 11. Implication with Disjunctive Antecedent
    (
        'a1020024-0000-0000-0000-000000000011'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. If an audit log entry undergoes unauthorized truncation ($$T$$) or cryptographic hash mismatch ($$M$$), the automated compliance supervisor halts pipeline deployments ($$H$$).\n2. The automated compliance supervisor did not halt pipeline deployments during yesterday''s execution cycle ($$\\\\neg H$$).\n\n**Conclusions:**\nI. The audit log entry did not undergo unauthorized truncation yesterday.\nII. The audit log entry did not produce a cryptographic hash mismatch yesterday.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The statement formalizes as $$(T \\\\lor M) \\\\implies H$$. By De Morgan''s law and Modus Tollens: $$((\\\\neg H) \\\\implies \\\\neg(T \\\\lor M) \\\\equiv \\\\neg T \\\\land \\\\neg M)$$. Because $$H$$ is false, both $$T$$ and $$M$$ must simultaneously be false. Thus both conclusions I and II follow necessarily.'
    ),
    -- 12. Multiple Conditions & Cumulative Negation
    (
        'a1020024-0000-0000-0000-000000000012'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. In a multi-tenant cloud environment, candidate exam sessions are preserved during worker node crash only if Redis cluster replication ($$R$$) and persistent write-ahead logging ($$W$$) are simultaneously active.\n2. Tenant $$Alpha$$ lost session progress during a worker node crash.\n\n**Conclusions:**\nI. Neither Redis cluster replication nor persistent write-ahead logging was active during Tenant $$Alpha$$''s session.\nII. At least one among Redis cluster replication or persistent write-ahead logging was inactive during Tenant $$Alpha$$''s session.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The rule states: $$\\text{Session Preserved} \\\\implies (R \\\\land W)$$. By contrapositive: $$\\neg(R \\\\land W) \\\\impliedby \\\\text{Session Lost}$$. Note that $$\\neg(R \\\\land W) \\\\equiv (\\\\neg R \\\\lor \\\\neg W)$$, meaning *at least one* subsystem was inactive. It is not logically required that *both* failed, so conclusion I is too extreme whereas conclusion II is the exact logical contrapositive.'
    ),
    -- 13. Assertion and Reason with Complex Causality
    (
        'a1020024-0000-0000-0000-000000000013'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'ANALYZE',
        '**Statements:**\n1. An increase in central bank reverse repo rate increases the borrowing cost of commercial banks, which suppresses broad money supply ($$M_3$$).\n2. Central Bank $$C$$ raised the reverse repo rate by 75 basis points in Q2.\n3. In Q2, commercial bank lending to corporate borrowers in Country $$C$$ increased by $$14\\\\%$$ year-over-year.\n\n**Conclusions:**\nI. Factors other than the reverse repo rate influenced commercial bank lending in Country $$C$$ during Q2.\nII. Reverse repo rate hikes have zero impact on corporate borrowing behavior.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Premise 1 establishes a theoretical contractionary mechanism. However, when the rate was raised, lending still expanded by $$14\\\\%$$. Logically, this implies confounding variables/external factors (e.g. strong economic expansion, capital inflows, liquidity buffers) counterbalanced or outweighed the monetary rate effect in Q2 (Conclusion I follows). Stating it has zero impact universally (Conclusion II) contradicts the explicit premise of Statement 1.'
    ),
    -- 14. Nested Subsets with Universal Negation
    (
        'a1020024-0000-0000-0000-000000000014'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. All biometric templates stored in the candidate registry ($$B$$) are irreversibly hashed ($$H$$).\n2. No irreversibly hashed data ($$H$$) can be decrypted by any symmetric key ($$K$$).\n3. Some authentication tokens ($$A$$) can be decrypted by symmetric keys ($$K$$).\n\n**Conclusions:**\nI. Some authentication tokens are not biometric templates stored in the candidate registry.\nII. No biometric template stored in the candidate registry can be decrypted by symmetric keys.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1) & (2): $$B \\\\subseteq H$$ and $$H \\\\cap K_{decryptable} = \\\\emptyset \\\\implies B \\\\cap K_{decryptable} = \\\\emptyset$$ (Conclusion II follows). From (3), there exist $$a \\\\in A$$ such that $$a \\\\in K_{decryptable}$$. Since no $$b \\\\in B$$ can be in $$K_{decryptable}$$, these tokens $$a$$ cannot belong to $$B$$, which proves Conclusion I ($$\\exists a \\\\in A, a \\\\notin B$$).'
    ),
    -- 15. Conditional Reversal & Probability
    (
        'a1020024-0000-0000-0000-000000000015'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. Whenever a memory leak ($$L$$) occurs in the JVM garbage collection subsystem, tenured heap memory occupancy exceeds $$92\\\\%$$ within 4 hours ($$T$$).\n2. In server node $$S_4$$, tenured heap memory occupancy exceeded $$95\\\\%$$ within 2 hours of application startup.\n\n**Conclusions:**\nI. Server node $$S_4$$ definitely suffered a memory leak in its JVM garbage collection subsystem.\nII. A heap dump analysis of $$S_4$$ is necessary to conclusively ascertain whether a memory leak occurred.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Statement 1 is an implication: $$L \\\\implies T$$. Given $$T$$ has occurred, inferring $$L$$ commits the fallacy of Affirming the Consequent ($$T$$ could be caused by sudden legitimate burst traffic or under-provisioned heap size). Because $$T$$ is a necessary symptom of $$L$$ but not uniquely caused by $$L$$, $$L$$ cannot be definitely concluded without forensic inspection (Conclusion II is valid, I is invalid).'
    ),
    -- 16. Transitive Non-Symmetric Relations
    (
        'a1020024-0000-0000-0000-000000000016'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. In a microservices dependency graph, Service $$A$$ cannot deploy without Service $$B$$ being active.\n2. Service $$B$$ cannot deploy without Service $$C$$ being active.\n3. Service $$C$$ cannot deploy without Service $$D$$ being active.\n4. Service $$D$$ failed its health check and became inactive.\n\n**Conclusions:**\nI. Neither Service $$A$$ nor Service $$B$$ can deploy.\nII. Service $$C$$ may deploy if it disables strict schema validation.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Transitive dependency chain: $$\\text{Deploy}(A) \\\\implies \\\\text{Active}(B) \\\\implies \\\\text{Active}(C) \\\\implies \\\\text{Active}(D)$$. Since $$D$$ is inactive, by Modus Tollens, neither $$C$$, $$B$$, nor $$A$$ can deploy (Conclusion I is strictly valid). Statement 2 gives no exceptions for disabling schema validation, making II speculative.'
    ),
    -- 17. Disjunctive Syllogism with Negated Components
    (
        'a1020024-0000-0000-0000-000000000017'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'EVALUATE',
        '**Statements:**\n1. An exam session failure is attributable either to an intermittent client WebSocket disconnect ($$W$$), an unhandled Redis connection timeout ($$R$$), or an out-of-memory exception on the gateway ($$G$$).\n2. Extensive telemetry proves the gateway maintained over $$60\\\\%$$ free memory throughout ($$\\\\neg G$$).\n3. Distributed traces indicate all Redis cluster round-trips completed within $$4\\\\text{ ms}$$ ($$\\\\neg R$$).\n\n**Conclusions:**\nI. The session failure was caused by an intermittent client WebSocket disconnect.\nII. Increasing gateway memory allocation would have prevented the session failure.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From premise 1, $$\\text{Failure} \\\\implies (W \\\\lor R \\\\lor G)$$. Premises 2 and 3 establish $$\\neg G$$ and $$\\neg R$$. By disjunctive syllogism: $$(W \\\\lor R \\\\lor G) \\\\land \\\\neg G \\\\land \\\\neg R \\\\implies W$$. Hence Conclusion I follows. Gateway memory was already abundant ($$> 60\\\\%$$), so increasing memory would not prevent a WebSocket failure (II is false).'
    ),
    -- 18. Complex Set Inclusion & Intersection
    (
        'a1020024-0000-0000-0000-000000000018'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. All high-security assessment nodes ($$H$$) are configured with encrypted NVMe drives ($$E$$).\n2. Some encrypted NVMe drives ($$E$$) require hardware-level TPM 2.0 modules ($$T$$).\n3. All hardware-level TPM 2.0 modules ($$T$$) enforce secure boot firmware verification ($$S$$).\n\n**Conclusions:**\nI. Some encrypted NVMe drives enforce secure boot firmware verification.\nII. All high-security assessment nodes enforce secure boot firmware verification.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (2) and (3): Some $$E$$ are $$T$$, and all $$T$$ are $$S$$, which yields $$\\exists x (x \\\\in E \\\\land x \\\\in S)$$ (Conclusion I is valid). However, from (1), $$H \\\\subseteq E$$, but $$H$$ is not guaranteed to intersect with the subset of $$E$$ that has $$T$$. Thus Conclusion II is an invalid universal generalization.'
    ),
    -- 19. Contrapositive Chain
    (
        'a1020024-0000-0000-0000-000000000019'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. A candidate cannot receive a disability compensatory time extension without a certified medical review document ($$C$$).\n2. A certified medical review document is issued only if the applicant undergoes physical verification at an accredited center ($$P$$).\n3. Candidate $$K$$ was awarded a 30-minute compensatory time extension.\n\n**Conclusions:**\nI. Candidate $$K$$ underwent physical verification at an accredited center.\nII. Candidate $$K$$ possessed a certified medical review document.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Premise (1): $$\\text{Extension} \\\\implies C$$. Premise (2): $$C \\\\implies P$$. By hypothetical syllogism: $$\\text{Extension} \\\\implies C \\\\implies P$$. Given Candidate $$K$$ received an extension, both $$C$$ (possesses document) and $$P$$ (underwent verification) follow necessarily.'
    ),
    -- 20. Only-A-Few Construct with Strict Negation
    (
        'a1020024-0000-0000-0000-000000000020'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. Only a few algorithmic question generators ($$G$$) utilize constraint satisfaction solvers ($$C$$).\n2. All constraint satisfaction solvers ($$C$$) guarantee zero blueprint rule violations ($$Z$$).\n3. No heuristic template builder ($$H$$) utilizes constraint satisfaction solvers ($$C$$).\n\n**Conclusions:**\nI. Some algorithmic question generators guarantee zero blueprint rule violations.\nII. Some algorithmic question generators do not guarantee zero blueprint rule violations via constraint satisfaction solvers.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        '"Only a few $$G$$ are $$C$$" logically asserts both "Some $$G$$ are $$C$$" and "Some $$G$$ are not $$C$$". From the first part: Some $$G$$ are $$C$$, and all $$C$$ are $$Z$$ $$\\implies$$ Some $$G$$ are $$Z$$ (Conclusion I follows). From the second part: Some $$G$$ are not $$C$$, meaning they do not utilize $$C$$ to guarantee $$Z$$ (Conclusion II follows).'
    ),
    -- 21. Causal Reversibility & Alternative Explanation
    (
        'a1020024-0000-0000-0000-000000000021'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. When the CPU thermal throttling threshold ($$95^\\\\circ\\\\text{C}$$) is crossed on the primary evaluation cluster, task processing throughput declines by at least $$40\\\\%$$.\n2. On Tuesday, the primary evaluation cluster experienced a $$50\\\\%$$ decline in task processing throughput.\n\n**Conclusions:**\nI. The CPU thermal throttling threshold was crossed on Tuesday on the primary evaluation cluster.\nII. The decline in task processing throughput on Tuesday could have been caused by network latency or database lock contention.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The statement defines $$Thermal \\\\implies Decline$$. Observing $$Decline$$ does not deduce $$Thermal$$ (fallacy of affirming consequent). Alternative factors like database lock contention or network latency are valid possible causes, making Conclusion II correct and I invalid.'
    ),
    -- 22. Incompatibility and Partitioning
    (
        'a1020024-0000-0000-0000-000000000022'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. No stateful session bean ($$S$$) is safe for horizontal auto-scaling without external state storage ($$H$$).\n2. All microservices in Cluster $$K$$ are designed as stateful session beans.\n3. None of the microservices in Cluster $$K$$ utilize external state storage.\n\n**Conclusions:**\nI. No microservice in Cluster $$K$$ is safe for horizontal auto-scaling.\nII. If Cluster $$K$$ introduces Redis for session externalization, some microservices may become safe for auto-scaling.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1), (2), and (3): $$K \\\\subseteq S$$ and $$K \\\\cap \\\\text{ExternalStorage} = \\\\emptyset$$. Since $$S \\\\cap \\\\neg \\\\text{ExternalStorage} \\\\implies \\\\neg \\\\text{AutoScalingSafe}$$, all services in $$K$$ are not safe for auto-scaling (Conclusion I follows). External state storage was the sole blocking requirement noted in premise 1, so fulfilling it allows services to become safe (Conclusion II follows).'
    ),
    -- 23. Double Conditional Negation
    (
        'a1020024-0000-0000-0000-000000000023'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. An exam cannot be certified as compliant ($$C$$) unless all test item difficulty indices are calibrated against empirical item response theory ($$I$$).\n2. Item response theory calibration ($$I$$) is impossible without a normative pilot sample of at least 1,000 distinct responses ($$N$$).\n3. Exam $$E_1$$ was certified as compliant.\n\n**Conclusions:**\nI. Exam $$E_1$$ underwent item response theory calibration.\nII. Exam $$E_1$$ had a normative pilot sample of at least 1,000 distinct responses.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Logical formulation: $$C \\\\implies I$$ and $$I \\\\implies N$$. Thus $$C \\\\implies I \\\\land N$$. Since $$E_1$$ is certified compliant ($$C$$ is true), both $$I$$ (IRT calibration) and $$N$$ (pilot sample $$\\ge 1,000$$) are logically mandatory.'
    ),
    -- 24. Modus Ponens over Quantified Predicates
    (
        'a1020024-0000-0000-0000-000000000024'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. All candidates who score in the top $$1\\\\%$$ in the national aptitude percentile ($$T$$) receive direct interview invitations ($$D$$).\n2. No candidate who received a direct interview invitation ($$D$$) was found to have a disqualified identity verification credential ($$Q$$).\n3. Candidate Priya was found to have a disqualified identity verification credential ($$Q$$).\n\n**Conclusions:**\nI. Candidate Priya did not receive a direct interview invitation.\nII. Candidate Priya did not score in the top $$1\\\\%$$ in the national aptitude percentile.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (2): $$D \\\\cap Q = \\\\emptyset \\\\implies Q \\\\implies \\\\neg D$$. Since Priya has $$Q$$, she cannot have $$D$$ (Conclusion I follows). From (1): $$T \\\\implies D$$, so by contrapositive $$\\neg D \\\\implies \\\\neg T$$. Thus Priya did not score in the top $$1\\\\%$$ (Conclusion II follows).'
    ),
    -- 25. Statistical Fallacy: Ecological Inference
    (
        'a1020024-0000-0000-0000-000000000025'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'ANALYZE',
        '**Statements:**\n1. Region $$A$$ has a higher average literacy rate ($$92\\\\%$$) than Region $$B$$ ($$68\\\\%$$).\n2. Mr. Rao resides in Region $$A$$ and Mr. Sen resides in Region $$B$$.\n\n**Conclusions:**\nI. Mr. Rao is definitely literate.\nII. Mr. Rao is more likely to be literate than Mr. Sen based on regional demographic probability alone.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'An aggregate demographic average ($$92\\\\%$$) does not establish a universal property for every individual member of that population ($$8\\\\%$$ are illiterate), so I is an ecological fallacy. However, statistically speaking, the prior probability of literacy for a randomly drawn individual from Region A ($$0.92$$) is strictly higher than from Region B ($$0.68$$), validating II.'
    ),
    -- 26. Contrapositive with Conjunctive Consequent
    (
        'a1020024-0000-0000-0000-000000000026'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. If an election tallying protocol achieves end-to-end verifiability ($$V$$), it must provide both individual cast-as-intended verification ($$I$$) and universal tallied-as-recorded verification ($$U$$).\n2. Protocol $$Alpha$$ provides universal tallied-as-recorded verification ($$U$$) but lacks individual cast-as-intended verification ($$\\\\neg I$$).\n\n**Conclusions:**\nI. Protocol $$Alpha$$ does not achieve end-to-end verifiability.\nII. Protocol $$Alpha$$ can achieve end-to-end verifiability if voters are provided paper receipts.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The formal premise is $$V \\\\implies (I \\\\land U)$$. Contrapositive: $$\\neg(I \\\\land U) \\\\equiv (\\\\neg I \\\\lor \\\\neg U) \\\\implies \\\\neg V$$. Because Protocol $$Alpha$$ has $$\\neg I$$, it strictly fails $$V$$ (Conclusion I follows). Paper receipts might violate coercion resistance or privacy without necessarily solving cast-as-intended verification, so II is unstated speculation.'
    ),
    -- 27. Universal Affirmative and Sub-contrary
    (
        'a1020024-0000-0000-0000-000000000027'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. All algorithms that solve the Traveling Salesperson Problem optimally in worst-case polynomial time ($$O$$) require $$P = NP$$.\n2. It is currently proven that no such polynomial-time proof exists ($$P \\\\neq NP$$ is widely held, and no $$P = NP$$ proof is confirmed).\n3. Algorithm $$Z$$ solves the Traveling Salesperson Problem optimally for all Euclidean graphs.\n\n**Conclusions:**\nI. Algorithm $$Z$$ does not operate in worst-case polynomial time unless $$P = NP$$.\nII. Algorithm $$Z$$ is a heuristic approximation algorithm.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From premise 1: Optimal worst-case polynomial time for TSP requires $$P = NP$$. Algorithm $$Z$$ claims exact optimality; therefore, unless $$P=NP$$, its worst-case runtime cannot be polynomial (Conclusion I follows). Statement 3 asserts $$Z$$ solves TSP *optimally* (exact), not as an approximation, so Conclusion II contradicts premise 3.'
    ),
    -- 28. Necessary Condition Negation
    (
        'a1020024-0000-0000-0000-000000000028'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. In an examination center, no candidate is permitted entry to the exam lab ($$E$$) without both a printed admit card ($$A$$) and a government-issued photo ID ($$G$$).\n2. Candidate Rohan presented a government-issued photo ID ($$G$$) and an electronic admit card on a smartphone ($$\\\\neg A$$).\n\n**Conclusions:**\nI. Candidate Rohan will not be permitted entry to the exam lab.\nII. Candidate Rohan would be permitted entry if the center supervisor authorizes an override.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The entry condition states $$E \\\\implies (A \\\\land G)$$. Rohan has $$\\neg A$$ (electronic on smartphone is not printed). Therefore by Modus Tollens, $$\\neg E$$ (he will not be permitted entry). No rules regarding supervisor overrides are mentioned in the premises, making II invalid.'
    ),
    -- 29. Disjoint Quantifiers and Transitivity
    (
        'a1020024-0000-0000-0000-000000000029'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. All quantum computers capable of Shor''s algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\\\\%$$ ($$F$$).\n2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\\\\%$$.\n3. System $$Psi$$ is a $$NISQ$$ system.\n\n**Conclusions:**\nI. System $$Psi$$ does not have coherent qubit fidelity exceeding $$99.9\\\\%$$.\nII. System $$Psi$$ is not capable of executing Shor''s algorithm for large integers.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (2) and (3): $$Psi \\\\in NISQ \\\\implies \\\\neg F$$ (Conclusion I follows). From (1): $$Q \\\\implies F$$, so by contrapositive $$\\neg F \\\\implies \\\\neg Q$$. Thus $$Psi$$ is not capable of executing Shor''s algorithm (Conclusion II follows).'
    ),
    -- 30. Affirming the Consequent Fallacy
    (
        'a1020024-0000-0000-0000-000000000030'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. If a distributed cache node encounters an unrecoverable disk storage corrupt error ($$C$$), it emits critical health heartbeat status 503 ($$H$$).\n2. Cache node $$C_2$$ emitted critical health heartbeat status 503.\n\n**Conclusions:**\nI. Cache node $$C_2$$ encountered an unrecoverable disk storage corrupt error.\nII. Cache node $$C_2$$ could have emitted status 503 due to network partition or memory exhaustion.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        '$$C \\\\implies H$$. Given $$H$$, asserting $$C$$ is the invalid formal fallacy of Affirming the Consequent. Status 503 indicates an unhealthy state that could stem from various independent causes (network partition, memory exhaustion), so II is a valid logical possibility and I is invalid.'
    ),
    -- 31. Conditional Chain with Negative Disjunction
    (
        'a1020024-0000-0000-0000-000000000031'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. An exam submission is deemed tampered ($$T$$) if its cryptographic HMAC does not match ($$M$$) or its sequence nonce is repeated ($$N$$).\n2. An exam submission deemed tampered is immediately quarantined and rejected ($$Q$$).\n3. Submission $$S_9$$ was not quarantined or rejected ($$\\\\neg Q$$).\n\n**Conclusions:**\nI. Submission $$S_9$$''s cryptographic HMAC matched.\nII. Submission $$S_9$$''s sequence nonce was not repeated.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        '$$(M \\\\lor N) \\\\implies T \\\\implies Q$$. By contrapositive: $$\\neg Q \\\\implies \\\\neg T \\\\implies \\\\neg(M \\\\lor N) \\\\equiv (\\\\neg M \\\\land \\\\neg N)$$. Therefore, both HMAC matched ($$\\\\neg M$$) and nonce was not repeated ($$\\\\neg N$$).'
    ),
    -- 32. Causal Directionality vs Reciprocal Feedback
    (
        'a1020024-0000-0000-0000-000000000032'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. Countries with high public investment in primary education ($$E$$) exhibit high labor productivity indices ($$L$$) 15 years later.\n2. Country $$Gamma$$ doubled its primary education budget 15 years ago.\n3. In Country $$Gamma$$, labor productivity remained stagnant over the past 15 years.\n\n**Conclusions:**\nI. High public investment in primary education alone does not guarantee high labor productivity without complementary economic factors.\nII. Public spending on primary education in Country $$Gamma$$ was misallocated or embezzled.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'The empirical divergence in Country $$Gamma$$ demonstrates that educational investment is not a single deterministic sufficient guarantee in isolation—it relies on macroeconomic absorption, capital equipment, and infrastructure (Conclusion I is valid). Concluding embezzlement or misallocation without empirical premises is an unwarranted leap (Conclusion II is invalid).'
    ),
    -- 33. Universal Quantifiers & Empty Intersections
    (
        'a1020024-0000-0000-0000-000000000033'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. All zero-knowledge succinct non-interactive arguments of knowledge ($$zk-SNARKs$$) require an elliptic curve pairing or trusted setup.\n2. Transparent zero-knowledge protocols ($$STARKs$$) require neither elliptic curve pairings nor trusted setups.\n3. Protocol $$X$$ is a transparent zero-knowledge protocol ($$STARK$$).\n\n**Conclusions:**\nI. Protocol $$X$$ is not a $$zk-SNARK$$.\nII. Protocol $$X$$ does not require a trusted setup.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Let $$zk-SNARK \\\\subseteq (Pairing \\\\lor TrustedSetup)$$. Since $$STARK \\\\cap (Pairing \\\\lor TrustedSetup) = \\\\emptyset$$, it strictly follows that $$STARK \\\\cap zk-SNARK = \\\\emptyset$$. Since $$X \\\\in STARK$$, $$X \\\\notin zk-SNARK$$ (Conclusion I) and $$X \\\\notin TrustedSetup$$ (Conclusion II). Both follow.'
    ),
    -- 34. Contrapositive with Multiple Disjunctions
    (
        'a1020024-0000-0000-0000-000000000034'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'EVALUATE',
        '**Statements:**\n1. In an AI exam proctoring pipeline, an anomalous proctoring flag ($$F$$) is raised if face tracking confidence drops below $$70\\\\%$$ ($$C$$), background acoustic energy exceeds 85 dB ($$A$$), or gaze angle deviation exceeds $$45^\\\\circ$$ for over 10 seconds ($$G$$).\n2. Candidate Rajesh completed his exam session without any anomalous proctoring flag being raised ($$\\\\neg F$$).\n\n**Conclusions:**\nI. Rajesh''s face tracking confidence never dropped below $$70\\\\%$$.\nII. Rajesh''s background acoustic energy never exceeded 85 dB.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        '$$(C \\\\lor A \\\\lor G) \\\\implies F$$. By contrapositive: $$\\neg F \\\\implies (\\\\neg C \\\\land \\\\neg A \\\\land \\\\neg G)$$. Since $$\\neg F$$ is given, all three antecedent trigger conditions were strictly absent. Both conclusions I and II follow.'
    ),
    -- 35. Transitive Partial Inclusions
    (
        'a1020024-0000-0000-0000-000000000035'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. Some candidate assessment sessions ($$C$$) are processed on edge nodes ($$E$$).\n2. All edge nodes ($$E$$) execute local offline question caching ($$L$$).\n3. No node executing local offline question caching ($$L$$) requires continuous internet connectivity for question navigation ($$N$$).\n\n**Conclusions:**\nI. Some candidate assessment sessions do not require continuous internet connectivity for question navigation.\nII. All candidate assessment sessions processed on edge nodes execute local offline question caching.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1) & (2): $$\\exists c \\\\in C$$ such that $$c \\\\in E \\\\subseteq L$$. From (3): $$L \\\\cap N = \\\\emptyset$$. Thus those sessions $$c$$ satisfy $$c \\\\notin N$$ (Conclusion I follows). From (2), since all edge nodes execute $$L$$, any session processed on an edge node executes $$L$$ (Conclusion II follows).'
    ),
    -- 36. Double Negation Elimination in Modal Logic
    (
        'a1020024-0000-0000-0000-000000000036'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'EVALUATE',
        '**Statements:**\n1. It is false that any cryptographic encryption algorithm without constant-time execution ($$\\\\neg C$$) is invulnerable to timing side-channel attacks ($$I$$).\n2. Algorithm $$AES-NI$$ exhibits hardware-enforced constant-time execution ($$C$$).\n\n**Conclusions:**\nI. All algorithms lacking constant-time execution are vulnerable to timing side-channel attacks.\nII. Algorithm $$AES-NI$$ is guaranteed to be invulnerable to all forms of cryptographic attacks.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Statement 1 is $$\\neg \\\\exists x (\\\\neg C(x) \\\\land I(x)) \\\\equiv \\\\forall x (\\\\neg C(x) \\\\implies \\\\neg I(x))$$, which means every algorithm lacking constant-time execution is vulnerable to timing attacks (Conclusion I follows). Statement 2 shows $$AES-NI$$ has constant-time execution, protecting against timing attacks, but this does not guarantee invulnerability against *all* possible attack vectors (e.g., fault injection, power analysis, algebraic cryptanalysis), so II is invalid.'
    ),
    -- 37. Complex Multi-Premise Categorical
    (
        'a1020024-0000-0000-0000-000000000037'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. No lossless data compression algorithm ($$L$$) can compress all possible bitstrings of length $$n$$ to length $$< n$$ ($$C$$).\n2. Algorithm $$Huffman$$ is a lossless data compression algorithm ($$L$$).\n3. Algorithm $$JPEG$$ is a lossy data compression algorithm ($$\\\\neg L$$).\n\n**Conclusions:**\nI. Algorithm $$Huffman$$ cannot compress all possible bitstrings of length $$n$$ to length $$< n$$.\nII. Algorithm $$JPEG$$ can compress all possible bitstrings of length $$n$$ to length $$< n$$.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1) & (2): By universal instantiation, $$Huffman \\\\in L \\\\implies Huffman$$ cannot compress all bitstrings to shorter lengths by the pigeonhole principle (Conclusion I follows). Premise 3 states $$JPEG$$ is lossy, but no premise states lossy compression can compress *all* bitstrings of any length, making II false.'
    ),
    -- 38. Causal Necessity vs Sufficiency with Modifiers
    (
        'a1020024-0000-0000-0000-000000000038'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. Adequate sleep ($$> 7\\\\text{ hours}$$) is a necessary condition for optimal cognitive processing speed during high-stakes competitive examinations.\n2. Candidate Vikram obtained 8.5 hours of sleep before the national examination.\n\n**Conclusions:**\nI. Candidate Vikram will achieve optimal cognitive processing speed during the examination.\nII. Had Vikram obtained only 4 hours of sleep, he would not have achieved optimal cognitive processing speed.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Adequate sleep is a *necessary* condition ($$\\text{Optimal} \\\\implies \\\\text{Sleep}$$, or contrapositively $$\\neg \\\\text{Sleep} \\\\implies \\\\neg \\\\text{Optimal}$$). Meeting the necessary condition ($$> 7$$ hours) does not *guarantee* optimal performance (stress, illness, or fatigue may interfere), so I is false. But violating the necessary condition ($$4$$ hours) strictly guarantees non-optimal performance, so Conclusion II is logically certain.'
    ),
    -- 39. Contrapositive Syllogism with Quantifiers
    (
        'a1020024-0000-0000-0000-000000000039'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. Every transaction processed in accordance with ACID compliance ($$A$$) guarantees strict isolation levels ($$I$$).\n2. No eventual consistency key-value datastore ($$E$$) guarantees strict isolation levels ($$I$$).\n3. Datastore $$Cassandra$$ is an eventual consistency key-value datastore ($$E$$).\n\n**Conclusions:**\nI. Datastore $$Cassandra$$ does not guarantee strict isolation levels.\nII. Transactions in Datastore $$Cassandra$$ are not ACID-compliant in the strict sense.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (2) and (3): $$Cassandra \\\\in E \\\\implies Cassandra \\\\notin I$$ (Conclusion I follows). From (1): $$A \\\\implies I$$. By contrapositive: $$\\neg I \\\\implies \\\\neg A$$. Since $$Cassandra$$ lacks $$I$$, it cannot guarantee strict ACID compliance (Conclusion II follows).'
    ),
    -- 40. Complex Argument Evaluation
    (
        'a1020024-0000-0000-0000-000000000040'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'EVALUATE',
        '**Statements:**\n1. In a multi-region cloud topology, cross-region replication lag is under $$50\\\\text{ ms}$$ only if dedicated dark fiber interconnects are utilized ($$D$$) and network packet loss remains under $$0.01\\\\%$$ ($$L$$).\n2. Region Pair $$(US-East, EU-West)$$ exhibited a cross-region replication lag of $$120\\\\text{ ms}$$.\n\n**Conclusions:**\nI. Either dedicated dark fiber was not utilized or network packet loss exceeded $$0.01\\\\%$$ between the regions.\nII. Dedicated dark fiber was definitely not utilized between the regions.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Premise 1: $$\\text{Lag} < 50 \\\\implies (D \\\\land L)$$. By contrapositive: $$\\text{Lag} \\\\ge 50 \\\\implies \\\\neg(D \\\\land L) \\\\equiv (\\\\neg D \\\\lor \\\\neg L)$$. Given the lag was $$120\\\\text{ ms}$$, at least one necessary condition failed (Conclusion I follows). Conclusion II assumes solely $$\\neg D$$ without evidence of $$L$$, which is an ungrounded overstatement.'
    ),
    -- 41. Syllogism with Complementary Existential
    (
        'a1020024-0000-0000-0000-000000000041'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. All public key certificates signed by Root CA $$X$$ ($$R$$) use RSA-4096 or ECDSA-P384 keys ($$K$$).\n2. No key using RSA-4096 or ECDSA-P384 ($$K$$) has been factored by classical computers ($$F$$).\n3. Certificate $$C_7$$ was signed by Root CA $$X$$.\n\n**Conclusions:**\nI. Certificate $$C_7$$ uses RSA-4096 or ECDSA-P384 keys.\nII. Certificate $$C_7$$''s key has not been factored by classical computers.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1) and (3): $$C_7 \\\\in R \\\\implies C_7 \\\\in K$$ (Conclusion I follows). From (2): $$K \\\\cap F = \\\\emptyset$$. Since $$C_7 \\\\in K$$, $$C_7 \\\\notin F$$ (Conclusion II follows). Both conclusions follow definitely.'
    ),
    -- 42. Fallacy of Division
    (
        'a1020024-0000-0000-0000-000000000042'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. The National Assessment Grid infrastructure cluster has achieved $$99.999\\\\%$$ uptime over the past 3 calendar years.\n2. Microservice Module $$M$$ is a component of the National Assessment Grid infrastructure cluster.\n\n**Conclusions:**\nI. Microservice Module $$M$$ had individually achieved $$99.999\\\\%$$ uptime over the past 3 calendar years.\nII. The National Assessment Grid as an aggregate system achieved high availability.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Concluding that an individual component inherits the aggregate reliability of a redundant distributed cluster commits the Fallacy of Division (individual components may fail frequently while cluster redundancy masks failures to yield 99.999% system availability). Thus I is invalid and II directly restates premise 1.'
    ),
    -- 43. Biconditional Chain
    (
        'a1020024-0000-0000-0000-000000000043'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. A matrix $$A$$ is invertible if and only if its determinant is non-zero ($$\\\\det(A) \\\\neq 0$$).\n2. A square matrix $$A$$ has a non-zero determinant if and only if its row vectors are linearly independent ($$L$$).\n3. Matrix $$M$$ has row vectors that are linearly dependent ($$\\\\neg L$$).\n\n**Conclusions:**\nI. Matrix $$M$$ is not invertible.\nII. The determinant of Matrix $$M$$ is zero ($$\\\\det(M) = 0$$).',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Chain of biconditionals: $$\\text{Invertible} \\\\iff \\\\det(A) \\\\neq 0 \\\\iff L$$. Since Matrix $$M$$ has linearly dependent rows ($$\\\\neg L$$), both $$\\det(M) = 0$$ (Conclusion II) and non-invertibility (Conclusion I) follow as necessary and sufficient mathematical truths.'
    ),
    -- 44. Universal Negative and Particular Intersections
    (
        'a1020024-0000-0000-0000-000000000044'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. No self-balancing red-black binary search tree ($$R$$) allows duplicate keys with identical hash values ($$D$$).\n2. Some memory-mapped data indexes ($$M$$) allow duplicate keys with identical hash values ($$D$$).\n3. All memory-mapped data indexes ($$M$$) support concurrent lock-free reads ($$C$$).\n\n**Conclusions:**\nI. Some memory-mapped data indexes are not self-balancing red-black trees.\nII. Some data structures supporting concurrent lock-free reads are not self-balancing red-black trees.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (1) and (2): There exists $$m \\\\in M$$ such that $$m \\\\in D$$. Since $$R \\\\cap D = \\\\emptyset$$, these indexes $$m$$ cannot be in $$R$$ (Conclusion I follows). From (3), these same indexes $$m$$ are in $$C$$, meaning there exists $$c \\\\in C$$ such that $$c \\\\notin R$$ (Conclusion II follows).'
    ),
    -- 45. Denying the Antecedent Fallacy
    (
        'a1020024-0000-0000-0000-000000000045'::uuid,
        'Logical Fallacies & Conditional Deductions',
        'ANALYZE',
        '**Statements:**\n1. If an applicant has a cumulative grade point average ($$CGPA$$) above 9.0 ($$G$$), they are exempt from taking the preliminary screening test ($$E$$).\n2. Candidate Ananya has a $$CGPA$$ of 8.4 ($$\\\\neg G$$).\n\n**Conclusions:**\nI. Candidate Ananya is not exempt from taking the preliminary screening test.\nII. Candidate Ananya may still be exempt from taking the preliminary screening test under sports or veteran quota rules.',
        '[{"id":"A","text":"Only conclusion II follows","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        '$$G \\\\implies E$$. Concluding $$\\neg G \\\\implies \\\\neg E$$ is the invalid formal fallacy of Denying the Antecedent ($$G$$ is sufficient for exemption, but other criteria like special quotas might also provide exemption). Thus I is logically invalid, and II is a valid open possibility.'
    ),
    -- 46. Disjunctive Syllogism with Universal Modifiers
    (
        'a1020024-0000-0000-0000-000000000046'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. Every question published in the certified Question Bank is classified under either Bloom''s cognitive level REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, or CREATE.\n2. Question $$Q_{101}$$ is not classified under REMEMBER, UNDERSTAND, or APPLY.\n3. Question $$Q_{101}$$ is not classified under CREATE.\n\n**Conclusions:**\nI. Question $$Q_{101}$$ is classified under either ANALYZE or EVALUATE.\nII. Question $$Q_{101}$$ must be classified under EVALUATE if it involves critique of logical proofs.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'By exhaustive elimination of disjoint alternatives from {REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE}, removing the 4 eliminated levels leaves exactly {ANALYZE, EVALUATE} (Conclusion I follows). Critiquing/verifying solutions matches the definition of EVALUATE under Bloom''s taxonomy (Conclusion II follows).'
    ),
    -- 47. Statistical Sampling and Causation
    (
        'a1020024-0000-0000-0000-000000000047'::uuid,
        'Cause, Effect and Assertion-Reasoning',
        'EVALUATE',
        '**Statements:**\n1. In a controlled trial of 10,000 students, those who used adaptive AI practice tests scored an average of $$18\\\\%$$ higher on standardized exams than those using static mock papers.\n2. Both groups had identical baseline pre-test score distributions and studied for equal total hours.\n\n**Conclusions:**\nI. Adaptive AI practice tests have a positive causal effect on student exam performance under controlled conditions.\nII. Every student who uses adaptive AI practice tests is guaranteed to score higher than every student using static papers.',
        '[{"id":"A","text":"Only conclusion I follows","isCorrect":true},{"id":"B","text":"Only conclusion II follows","isCorrect":false},{"id":"C","text":"Both conclusions I and II follow","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Because the trial controlled for baseline scores and study hours across a large sample (10,000), the statistically significant positive difference ($$18\\\\%$$) establishes a causal advantage under controlled conditions (Conclusion I follows). Asserting universal superiority for *every single student* ignores distribution variance and individual outliers, making II an invalid extreme claim.'
    ),
    -- 48. Transitive Contrapositive with Quantifiers
    (
        'a1020024-0000-0000-0000-000000000048'::uuid,
        'Multi-Statement Analytical Conclusions',
        'ANALYZE',
        '**Statements:**\n1. All robust distributed databases ($$R$$) satisfy linearizability ($$L$$).\n2. Any database that satisfies linearizability ($$L$$) requires a majority consensus quorum ($$Q$$) for write operations.\n3. Datastore $$Volt$$ does not require a majority consensus quorum for write operations ($$\\\\neg Q$$).\n\n**Conclusions:**\nI. Datastore $$Volt$$ does not satisfy linearizability.\nII. Datastore $$Volt$$ is not a robust distributed database.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Implication chain: $$R \\\\implies L \\\\implies Q$$. By contrapositive: $$\\neg Q \\\\implies \\\\neg L \\\\implies \\\\neg R$$. Since $$Volt$$ has $$\\neg Q$$, both conclusions $$\\neg L$$ (Conclusion I) and $$\\neg R$$ (Conclusion II) follow deductively.'
    ),
    -- 49. Complex Set-Theoretic Deduction
    (
        'a1020024-0000-0000-0000-000000000049'::uuid,
        'Direct & Indirect Inferences',
        'ANALYZE',
        '**Statements:**\n1. In a cryptographic key infrastructure, all keys with expiry beyond 2030 ($$E$$) must be post-quantum secure ($$P$$).\n2. No post-quantum secure key ($$P$$) is vulnerable to Shor''s polynomial-time factoring algorithm ($$S$$).\n3. Key $$K_{88}$$ is vulnerable to Shor''s polynomial-time factoring algorithm ($$S$$).\n\n**Conclusions:**\nI. Key $$K_{88}$$ is not post-quantum secure.\nII. Key $$K_{88}$$ does not have an expiry date beyond 2030.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'From (2): $$P \\\\cap S = \\\\emptyset \\\\implies S \\\\implies \\\\neg P$$. Since $$K_{88}$$ has $$S$$, $$K_{88} \\\\notin P$$ (Conclusion I follows). From (1): $$E \\\\implies P$$. By contrapositive: $$\\neg P \\\\implies \\\\neg E$$. Thus Key $$K_{88}$$ cannot have expiry beyond 2030 (Conclusion II follows).'
    ),
    -- 50. Multi-Premise Complex Deductive Conclusion
    (
        'a1020024-0000-0000-0000-000000000050'::uuid,
        'Multi-Statement Analytical Conclusions',
        'EVALUATE',
        '**Statements:**\n1. In the National Assessment Grid, all generated exam papers ($$P$$) are validated against a structural blueprint ($$B$$).\n2. An exam paper satisfies its structural blueprint ($$B$$) only if the Question Bank contains sufficient approved questions for every blueprint rule ($$S$$).\n3. Exam Paper $$P_{Final}$$ was successfully generated and transitioned to DRAFT status ($$P$$).\n\n**Conclusions:**\nI. The Question Bank contained sufficient approved questions for every rule in $$P_{Final}$$''s blueprint.\nII. Exam Paper $$P_{Final}$$ satisfied its structural blueprint.',
        '[{"id":"A","text":"Both conclusions I and II follow","isCorrect":true},{"id":"B","text":"Only conclusion I follows","isCorrect":false},{"id":"C","text":"Only conclusion II follows","isCorrect":false},{"id":"D","text":"Neither conclusion I nor II follows","isCorrect":false}]',
        'A',
        'Premise (1): $$P \\\\implies B$$. Premise (2): $$B \\\\implies S$$. Thus $$P \\\\implies B \\\\land S$$. Given Paper $$P_{Final}$$ was successfully generated ($$P$$ is true), it logically and necessarily follows that $$P_{Final}$$ satisfied its blueprint ($$B$$ is true, Conclusion II) and the Question Bank contained sufficient approved questions for every rule ($$S$$ is true, Conclusion I). Both conclusions follow strictly.'
    )
) AS v(id, subtopic_name, cognitive_level, content, options, answer_key, explanation)
JOIN question_service.subject s
  ON s.name = 'General Intelligence and Reasoning' AND s.tenant_id = 'default'
JOIN question_service.topic t
  ON t.name = 'Statement and Conclusion' AND t.subject_id = s.id AND t.tenant_id = 'default'
LEFT JOIN question_service.subtopic st
  ON st.name = v.subtopic_name AND st.topic_id = t.id AND st.tenant_id = 'default'
ON CONFLICT (id, subject_id) DO NOTHING;
