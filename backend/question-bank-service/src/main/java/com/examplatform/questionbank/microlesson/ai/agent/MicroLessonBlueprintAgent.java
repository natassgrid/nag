// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pedagogical Blueprint Agent.
 * Analyzes syllabus taxonomy, classifies domain categories, queries real exam questions
 * from {@link QuestionRepository}, and formulates structured {@link MicroLessonBlueprint} directives
 * anchoring the micro-lesson synthesis process.
 */
@Component
public class MicroLessonBlueprintAgent {

    private static final Logger log = LoggerFactory.getLogger(MicroLessonBlueprintAgent.class);

    private final QuestionRepository questionRepository;
    private final DomainClassifier domainClassifier;

    public MicroLessonBlueprintAgent() {
        this(null, new DomainClassifier());
    }

    public MicroLessonBlueprintAgent(QuestionRepository questionRepository) {
        this(questionRepository, new DomainClassifier());
    }

    @Autowired
    public MicroLessonBlueprintAgent(
            @Autowired(required = false) QuestionRepository questionRepository,
            @Autowired(required = false) DomainClassifier domainClassifier) {
        this.questionRepository = questionRepository;
        this.domainClassifier = domainClassifier != null ? domainClassifier : new DomainClassifier();
    }

    /**
     * Classifies a syllabus subject, topic, and subtopic into a specific {@link DomainCategory}
     * delegating to the collection-based {@link DomainClassifier}.
     */
    public DomainCategory classifyDomain(String subject, String topic, String subtopic) {
        return domainClassifier.classify(subject, topic, subtopic);
    }

    /**
     * Determines whether Part 3 (KaTeX Formula Cheat Sheet) is strictly required for the given
     * subject, topic, subtopic, and lessonType.
     *
     * <p>Subject &amp; Topic-Wise Rules:
     * <ul>
     *   <li><b>Mandatory</b>: Mathematics ({@code MATH}), Quantitative Aptitude, Statistics ({@code STATS}),
     *       Data Interpretation ({@code DILA}), General Science ({@code GSCI} - Physics, Chemistry, Biology),
     *       explicitly numerical topics in Reasoning/GS (e.g. Numerical Operations, Number Series,
     *       Symbolic Operations, Inequalities, Everyday Science), or when {@code lessonType} is {@code CHEAT_SHEET}.</li>
     *   <li><b>Optional</b>: English ({@code ENG}), General Hindi ({@code HND}), History, Polity, Geography,
     *       General Studies ({@code GS} non-science), Public Administration ({@code PAG}),
     *       Child Development &amp; Pedagogy ({@code CDP}), Environmental Ecology ({@code EEB}),
     *       Banking &amp; Financial Awareness ({@code BFA}), Computer Science ({@code CS}),
     *       and Verbal/Critical/Non-Verbal Reasoning.</li>
     * </ul>
     */
    public boolean isFormulaSheetRequired(String subject, String topic, String subtopic, String lessonType) {
        if (lessonType != null && "CHEAT_SHEET".equalsIgnoreCase(lessonType.trim())) {
            return true;
        }

        String s = (subject != null ? subject : "").toLowerCase(Locale.ROOT).trim();
        String t = (topic != null ? topic : "").toLowerCase(Locale.ROOT).trim();
        String st = (subtopic != null ? subtopic : "").toLowerCase(Locale.ROOT).trim();
        String combined = (s + " " + t + " " + st).trim();

        if (combined.isEmpty()) {
            return true;
        }

        // 1. Core Mathematical, Statistical, and Quantitative Subjects -> MANDATORY
        if (s.contains("math") || s.equals("quant") || s.contains("quantitative")
                || s.contains("numerical") || s.contains("arithmetic")
                || s.contains("statistics") || s.equals("stats")
                || s.contains("data interpretation") || s.equals("dila")) {
            return true;
        }

        // 2. Core Science Subjects (General Science, Physics, Chemistry, Biology) -> MANDATORY
        if (s.contains("physic") || s.contains("chem") || s.contains("general science")
                || s.equals("gsci") || s.equals("science") || s.contains("physical science")) {
            return true;
        }

        // 3. Topic-level check for Science / Math / Numerical topics inside hybrid subjects (e.g. GS, Reasoning)
        if (t.contains("physics") || t.contains("chemistry") || t.contains("everyday science")
                || t.contains("numerical operation") || t.contains("number series")
                || t.contains("symbolic operation") || t.contains("inequalit")
                || t.contains("quadratic") || t.contains("data interpretation")
                || t.contains("arithmetic") || t.contains("mensuration")
                || t.contains("trigonometry") || t.contains("algebra")
                || t.contains("geometry") || t.contains("number system")
                || st.contains("mathematical step operation") || st.contains("number coding")) {
            return true;
        }

        // 4. All other subjects/topics (English, Hindi, Polity, History, Geography, Public Admin,
        //    Pedagogy/CDP, Banking/Economy, Computer Science, Ecology, Verbal/Analytical Reasoning) -> OPTIONAL
        return false;
    }

    /**
     * Resolves the pedagogical instruction directives tailored specifically for the {@link DomainCategory}.
     */
    public String resolveDomainDirective(DomainCategory category) {
        return switch (category) {
            case PHYSICS -> """
                    PHYSICS PEDAGOGICAL BLUEPRINT:
                    - Bridge the physical intuition first using intuitive analogies (e.g., rotating an atta chakki stone at outer rim vs center for torque & moment of inertia, water flowing in pipes for electric current).
                    - Provide formal SI units and dimension formulas in the cheat sheet alongside KaTeX equations: e.g. $$\\tau = I\\alpha$$, with $$[I] = \\text{M}\\text{L}^2$$.
                    - Warn about sign conventions, vector directions (cross product right-hand rule), and reference frames in the Trap Alert.
                    """;
            case CHEMISTRY_ORGANIC -> """
                    ORGANIC CHEMISTRY PEDAGOGICAL BLUEPRINT:
                    - Highlight functional groups, IUPAC nomenclature rules, and structural hybridisation (sp3, sp2, sp).
                    - In the Trap Alert, emphasize stability of carbocation intermediates (Markovnikov vs anti-Markovnikov, carbocation rearrangements), electronic effects (+I, -I, +M, -M), and nucleophilic vs electrophilic attacks.
                    - Provide general formula or functional group transformations in Part 3.
                    """;
            case CHEMISTRY_INORGANIC_PHYSICAL -> """
                    INORGANIC & PHYSICAL CHEMISTRY BLUEPRINT:
                    - For Physical Chemistry: state the fundamental thermodynamic, kinetic, or equilibrium equation (e.g. $$\\Delta G = \\Delta H - T\\Delta S$$, $$PV=nRT$$, $$pH = -\\log_{10}[H^+]$$), state constraints, and show unit consistency.
                    - For Inorganic Chemistry: explain periodic trends (electronegativity, ionisation energy, atomic radii) and highlight anomalous exceptions (e.g. electron gain enthalpy of Cl vs F, inert pair effect) in the Trap Alert.
                    """;
            case MATHEMATICS_QUANT -> """
                    MATHEMATICS & QUANTITATIVE APTITUDE BLUEPRINT:
                    - Provide both the conceptual algebraic derivation and the competitive exam shortcut/Vedic ratio trick (e.g. effective percentage formula $$a + b + \\frac{ab}{100}$$, alligation cross-method).
                    - In the Trap Alert, warn against calculation traps (e.g. dividing by zero, extraneous roots in radical equations, mixing units like km/h vs m/s, or confusing simple interest with compound interest intervals).
                    """;
            case BIOLOGY_ECOLOGY -> """
                    BIOLOGY & ECOLOGY BLUEPRINT:
                    - Explain biological mechanisms, biochemical flow pathways, and ecological energy transfers (e.g. Lindeman's 10% law, Central Dogma DNA -> RNA -> Protein).
                    - Provide memory mnemonics for biological classifications, cell division phases (PMAT), or cranial nerves.
                    - In the Trap Alert, point out commonly confused anatomical or physiological terms (e.g. ureotelic vs uricotelic vs ammonotelic, xylem vs phloem transport direction).
                    """;
            case COMPUTER_SCIENCE -> """
                    COMPUTER SCIENCE & IT BLUEPRINT:
                    - Detail asymptotic time and space complexity using Big-O notation: e.g. $$O(N \\log N)$$, $$O(1)$$.
                    - Detail edge cases in the Trap Alert: off-by-one errors, null pointers, integer overflow, cyclic graph traversal without visited sets, and recursion depth stack overflow.
                    """;
            case BANKING_ECONOMY -> """
                    BANKING & FINANCIAL ECONOMY BLUEPRINT:
                    - Explain economic transmission mechanisms (e.g., how an RBI repo rate hike dampens inflation and reduces liquidity).
                    - Provide precise statutory ratios, formulas (e.g., $$\\text{CRAR} = \\frac{\\text{Tier 1} + \\text{Tier 2 Capital}}{\\text{Risk Weighted Assets}}$$), and financial regulations.
                    - Highlight exam-critical updates, regulatory mandates, and terminology distinctions in the Trap Alert.
                    """;
            case POLITY_HISTORY_GS -> """
                    POLITY, HISTORY & GENERAL STUDIES BLUEPRINT:
                    - Quote relevant Constitutional Articles, Schedules, Landmark Supreme Court Amendments/Judgments (e.g. Kesavananda Bharati basic structure, Article 368 special majorities).
                    - For History: provide chronological anchors, cause-and-effect geopolitical dynamics, and key administrative acts.
                    - In the Trap Alert, clear up common misconceptions between Union vs State powers, ordinance durations, or conflicting treaty dates.
                    """;
            case REASONING_APTITUDE -> """
                    REASONING & LOGICAL APTITUDE BLUEPRINT:
                    - Demystify the structural pattern, Venn diagram intersection, or deductive syllogism rule (e.g. Venn representation, blood relations family tree diagramming, seating arrangement corner cases).
                    - In the Trap Alert, warn against unwarranted real-world assumptions in syllogisms (treat statements as absolute truth even if defying known facts).
                    """;
            case LANGUAGE_COMPREHENSION -> """
                    LANGUAGE & VERBAL COMPREHENSION BLUEPRINT:
                    - Highlight grammatical syntax rules, subject-verb agreement concord, tense consistency, and etymological roots (prefixes/suffixes).
                    - In the Trap Alert, spotlight commonly tested trap words, false cognates, idioms taken literally, and subtle subject modifiers that mislead in error-spotting.
                    """;
            case PEDAGOGY_CDP -> """
                    PEDAGOGY & CHILD DEVELOPMENT BLUEPRINT:
                    - Anchor explanations in recognized developmental psychologist frameworks (Piaget's stages of cognitive development, Vygotsky's Zone of Proximal Development & scaffolding, Kohlberg's moral stages).
                    - Focus on learner-centered, inclusive, constructive pedagogy.
                    - In the Trap Alert, caution against teacher-centric rote memorization options, rigid punishment-based disciplinary answers, or confusing assimilation with accommodation.
                    """;
        };
    }

    /**
     * Queries past real exam questions from {@link QuestionRepository} to provide concrete calibration
     * context to the AI generation prompt.
     */
    public List<Question> findSampleQuestions(String subject, String topic, String subtopic, String difficulty) {
        if (questionRepository == null) {
            return Collections.emptyList();
        }

        try {
            List<Question> questions = questionRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("state"), QuestionStateConstants.PUBLISHED));

                if (subtopic != null && !subtopic.isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("content")), "%" + subtopic.trim().toLowerCase(Locale.ROOT) + "%"));
                } else if (topic != null && !topic.isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("content")), "%" + topic.trim().toLowerCase(Locale.ROOT) + "%"));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            });

            if (questions.isEmpty()) {
                return Collections.emptyList();
            }

            // Return up to 2 high-quality calibration questions
            return questions.subList(0, Math.min(2, questions.size()));

        } catch (Exception e) {
            log.warn("Unable to query sample questions from repository for subtopic '{}': {}", subtopic, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Formats the domain blueprint, pedagogical directives, and real sample calibration questions into a
     * prompt section for the LLM.
     */
    public String formatBlueprintPrompt(
            DomainCategory domainCategory,
            String subject,
            String topic,
            String subtopic,
            String targetExam,
            String difficulty,
            String directive,
            List<Question> sampleQuestions) {

        StringBuilder sb = new StringBuilder();
        sb.append("=== DOMAIN BLUEPRINT & PEDAGOGICAL DIRECTIVES ===\n");
        sb.append(String.format("Domain Classification: %s\n", domainCategory.name()));
        sb.append(String.format("Target Subject: %s | Topic: %s | Subtopic: %s\n",
                subject != null ? subject : SharedConstants.GENERAL,
                topic != null ? topic : SharedConstants.GENERAL,
                subtopic != null ? subtopic : SharedConstants.GENERAL));
        sb.append(String.format("Exam Persona: %s | Cognitive Level: %s\n\n",
                targetExam != null ? targetExam : SharedConstants.JEE_MAIN,
                difficulty != null ? difficulty : "MEDIUM"));

        sb.append("--- PEDAGOGICAL DIRECTIVE FOR THIS DOMAIN ---\n");
        sb.append(directive.trim()).append("\n\n");

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            sb.append("--- BENCHMARK REAL EXAM CALIBRATION QUESTIONS ---\n");
            sb.append("Study the style, depth, and KaTeX notation of these authentic exam questions on this subtopic:\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                Question q = sampleQuestions.get(i);
                sb.append(String.format("\n[Benchmark Question %d]\n", i + 1));
                sb.append("Question: ").append(q.getContent()).append("\n");
                if (q.getAnswerKey() != null) {
                    sb.append("Answer Key: ").append(q.getAnswerKey()).append("\n");
                }
                if (q.getExplanation() != null) {
                    sb.append("Explanation: ").append(q.getExplanation()).append("\n");
                }
            }
            sb.append("\nEnsure your generated micro-lesson matches or exceeds this depth and rigor.\n");
        }

        return sb.toString();
    }

    /**
     * Synthesizes a complete {@link MicroLessonBlueprint} for a subtopic.
     */
    public MicroLessonBlueprint createBlueprint(
            String subject,
            String topic,
            String subtopic,
            String targetExam,
            String difficulty) {
        DomainCategory category = classifyDomain(subject, topic, subtopic);
        String directive = resolveDomainDirective(category);
        List<Question> sampleQuestions = findSampleQuestions(subject, topic, subtopic, difficulty);
        String prompt = formatBlueprintPrompt(category, subject, topic, subtopic, targetExam, difficulty, directive, sampleQuestions);
        return new MicroLessonBlueprint(
                category,
                subject,
                topic,
                subtopic,
                targetExam,
                difficulty,
                directive,
                sampleQuestions,
                prompt
        );
    }

    /**
     * Overloaded convenience method defaulting difficulty to MEDIUM.
     */
    public MicroLessonBlueprint createBlueprint(
            String subject,
            String topic,
            String subtopic,
            String targetExam) {
        return createBlueprint(subject, topic, subtopic, targetExam, "MEDIUM");
    }
}
