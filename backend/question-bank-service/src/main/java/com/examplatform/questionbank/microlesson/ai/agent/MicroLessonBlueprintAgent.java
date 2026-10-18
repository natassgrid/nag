// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
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

    public MicroLessonBlueprintAgent() {
        this(null);
    }

    @Autowired
    public MicroLessonBlueprintAgent(@Autowired(required = false) QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Classifies a syllabus subject, topic, and subtopic into a specific {@link DomainCategory}.
     */
    public DomainCategory classifyDomain(String subject, String topic, String subtopic) {
        String s = (subject != null ? subject : "").toLowerCase(Locale.ROOT);
        String t = (topic != null ? topic : "").toLowerCase(Locale.ROOT);
        String st = (subtopic != null ? subtopic : "").toLowerCase(Locale.ROOT);
        String combined = (s + " " + t + " " + st).trim();

        // 1. Pedagogy & Child Development (CDP)
        if (combined.contains("pedagogy") || combined.contains("cdp") || combined.contains("child development")
                || combined.contains("piaget") || combined.contains("vygotsky") || combined.contains("kohlberg")
                || combined.contains("inclusive education") || combined.contains("cce") || combined.contains("assessment")
                || combined.contains("child psych") || combined.contains("teaching aptitude") || combined.contains("bloom")
                || combined.contains("scaffolding") || combined.contains("learning theor")) {
            return DomainCategory.PEDAGOGY_CDP;
        }

        // 2. Language & Verbal Comprehension
        if (combined.contains("english") || combined.contains("hindi") || combined.contains("grammar")
                || combined.contains("vocabulary") || combined.contains("synonym") || combined.contains("antonym")
                || combined.contains("voice") || combined.contains("narration") || combined.contains("comprehension")
                || combined.contains("cloze") || combined.contains("idiom") || combined.contains("para jumble")
                || combined.contains("error spotting") || combined.contains("संधि") || combined.contains("समास")
                || combined.contains("मुहावरे") || combined.contains("व्याकरण") || combined.contains("विलोम")
                || combined.contains("पर्यायवाची") || combined.contains("तद्भव") || combined.contains("तत्सम")
                || combined.contains("अलंकार") || combined.contains("लोकोक्ति")) {
            return DomainCategory.LANGUAGE_COMPREHENSION;
        }

        // 3. Computer Science & IT
        if (combined.contains("computer") || combined.contains("data structure") || combined.contains("algorithm")
                || combined.contains("dbms") || combined.contains("database") || combined.contains("networking")
                || combined.contains("cyber security") || combined.contains("operating system") || combined.contains(" os ")
                || combined.endsWith(" os") || combined.contains("computer basics") || combined.contains("ms office")
                || combined.contains("internet") || combined.contains("programming") || combined.contains("sql")
                || combined.contains("software engineering") || combined.contains("compiler") || combined.contains("automata")
                || combined.contains("dynamic programming") || s.contains("computer science") || t.contains("computer science")) {
            return DomainCategory.COMPUTER_SCIENCE;
        }

        // 4. Organic Chemistry (check before general chemistry/physics)
        if (combined.contains("carbon and its compounds") || combined.contains("carbon & its compounds")
                || combined.contains("organic chemistry") || combined.contains("organic")
                || combined.contains("hydrocarbon") || combined.contains("alkane") || combined.contains("alkene")
                || combined.contains("alkyne") || combined.contains("functional group") || combined.contains("polymer")
                || combined.contains("iupac") || combined.contains("isomer") || combined.contains("haloalkane")
                || combined.contains("alcohol") || combined.contains("phenol") || combined.contains("ether")
                || combined.contains("aldehyde") || combined.contains("ketone") || combined.contains("carboxylic")
                || combined.contains("amine") || combined.contains("diazonium") || combined.contains("ester")
                || combined.contains("saponif") || combined.contains("biomolecule") || combined.contains("catenation")
                || combined.contains("tetravalen") || combined.contains("homologous series") || combined.contains("micelle")
                || combined.contains("soap")) {
            return DomainCategory.CHEMISTRY_ORGANIC;
        }

        // 5. Inorganic & Physical Chemistry
        if (combined.contains("acid") || combined.contains("base") || combined.contains("salt")
                || combined.contains("ph ") || combined.contains(" ph") || combined.contains("ph scale") || combined.contains("ph-")
                || combined.contains("atomic structure") || combined.contains("mole concept") || combined.contains("periodic table")
                || combined.contains("periodic") || combined.contains("chemical reaction") || combined.contains("redox")
                || combined.contains("metallurgy") || combined.contains("electrochemistry") || combined.contains("chemical bonding")
                || combined.contains("coordination") || combined.contains("p-block") || combined.contains("d-block")
                || combined.contains("f-block") || combined.contains("s-block") || combined.contains("inorganic")
                || combined.contains("physical chemistry") || combined.contains("colligative") || combined.contains("titration")
                || (combined.contains("equilibrium") && !s.contains("physic"))
                || (combined.contains("thermodynamics") && (s.contains("chem") || t.contains("chem")))) {
            return DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL;
        }

        // If subject explicitly contains chemistry and not classified as organic:
        if (s.contains("chem")) {
            return DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL;
        }

        // 6. Physics
        if (s.contains("physic") || combined.contains("mechanics") || combined.contains("rotational")
                || combined.contains("moment of inertia") || combined.contains("torque") || combined.contains("optics")
                || combined.contains("electricity") || combined.contains("circuit") || combined.contains("magnetism")
                || combined.contains("magnetic") || combined.contains("electromagnet") || combined.contains("waves")
                || combined.contains("ray optics") || combined.contains("wave optics") || combined.contains("modern physics")
                || combined.contains("kinematics") || combined.contains("gravitation") || combined.contains("work & energy")
                || combined.contains("work and energy") || combined.contains("sound") || combined.contains("electrostatics")
                || combined.contains("current electricity") || combined.contains("semiconductor") || combined.contains("fluid")
                || combined.contains("oscillation") || combined.contains("shm") || combined.contains("thermodynamics")
                || combined.contains("circular motion") || combined.contains("units and dimension") || combined.contains("newton")) {
            return DomainCategory.PHYSICS;
        }

        // 7. Mathematics & Quantitative Aptitude
        if (s.contains("math") || s.contains("quant") || combined.contains("arithmetic")
                || combined.contains("percentage") || combined.contains("ratio") || combined.contains("proportion")
                || combined.contains("profit") || combined.contains("loss") || combined.contains("discount")
                || combined.contains("simple interest") || combined.contains("compound interest") || combined.contains("si/ci")
                || combined.contains("time & work") || combined.contains("time and work") || combined.contains("pipe")
                || combined.contains("time & distance") || combined.contains("time and distance") || combined.contains("speed")
                || combined.contains("train") || combined.contains("boat") || combined.contains("number system")
                || combined.contains("algebra") || combined.contains("geometry") || combined.contains("mensuration")
                || combined.contains("trigonometry") || combined.contains("statistics") || combined.contains("probability")
                || combined.contains("calculus") || combined.contains("data interpretation") || combined.contains("quadratic")
                || combined.contains("matrix") || combined.contains("determinant") || combined.contains("alligation")
                || combined.contains("mixture") || combined.contains("average") || combined.contains("partnership")
                || combined.contains("permutation") || combined.contains("combination") || combined.contains("coordinate geometry")) {
            return DomainCategory.MATHEMATICS_QUANT;
        }

        // 8. Biology & Ecology
        if (s.contains("bio") || combined.contains("botany") || combined.contains("zoology")
                || combined.contains("ecology") || combined.contains("cell") || combined.contains("genetics")
                || combined.contains("physiology") || combined.contains("biodiversity") || combined.contains("climate change")
                || combined.contains("environment") || combined.contains("nutrition") || combined.contains("disease")
                || combined.contains("photosynthesis") || combined.contains("respiration") || combined.contains("dna")
                || combined.contains("rna") || combined.contains("evolution") || combined.contains("reproduction")
                || combined.contains("ecosystem") || combined.contains("biotechnology") || combined.contains("lindeman")
                || combined.contains("trophic")) {
            return DomainCategory.BIOLOGY_ECOLOGY;
        }

        // 9. Banking & Financial Economy
        if (s.contains("bank") || s.contains("econ") || combined.contains("rbi") || combined.contains("monetary policy")
                || combined.contains("repo rate") || combined.contains("reverse repo") || combined.contains("npa")
                || combined.contains("basel") || combined.contains("upi") || combined.contains("digital banking")
                || combined.contains("financial market") || combined.contains("inflation") || combined.contains("cpi")
                || combined.contains("wpi") || combined.contains("government schemes") || combined.contains("budget")
                || combined.contains("fiscal deficit") || combined.contains("crr") || combined.contains("slr")
                || combined.contains("capital adequacy") || combined.contains("crar") || combined.contains("macroeconomic")
                || combined.contains("microeconomic") || combined.contains("gdp") || combined.contains("national income")) {
            return DomainCategory.BANKING_ECONOMY;
        }

        // 10. Polity, History & General Studies
        if (s.contains("polity") || s.contains("history") || s.contains("gs") || s.contains("general studies")
                || s.contains("geography") || combined.contains("constitution") || combined.contains("fundamental rights")
                || combined.contains("parliament") || combined.contains("panchayati raj") || combined.contains("public administration")
                || combined.contains("ancient history") || combined.contains("medieval history") || combined.contains("modern history")
                || combined.contains("art & culture") || combined.contains("art and culture") || combined.contains("international relations")
                || combined.contains("current affairs") || combined.contains("supreme court") || combined.contains("governor")
                || combined.contains("president") || combined.contains("directive principles") || combined.contains("dpsp")
                || combined.contains("monsoon") || combined.contains("freedom struggle") || combined.contains("article 368")
                || combined.contains("special majority") || combined.contains("preamble")) {
            return DomainCategory.POLITY_HISTORY_GS;
        }

        // 11. Reasoning & Aptitude
        if (s.contains("reasoning") || s.contains("aptitude") || combined.contains("analogy")
                || combined.contains("syllogism") || combined.contains("statement & conclusion") || combined.contains("statement and conclusion")
                || combined.contains("coding-decoding") || combined.contains("coding decoding") || combined.contains("series")
                || combined.contains("blood relation") || combined.contains("seating arrangement") || combined.contains("venn diagram")
                || combined.contains("critical thinking") || combined.contains("data sufficiency") || combined.contains("direction sense")
                || combined.contains("puzzle") || combined.contains("ranking") || combined.contains("cubes and dice")
                || combined.contains("dice") || combined.contains("clock") || combined.contains("calendar")) {
            return DomainCategory.REASONING_APTITUDE;
        }

        // Subject-based default fallback
        if (s.contains("sci")) return DomainCategory.PHYSICS;
        return DomainCategory.REASONING_APTITUDE;
    }

    /**
     * Resolves pedagogical directive tailored for the specific domain category.
     */
    public String resolveDomainDirective(DomainCategory category) {
        return switch (category) {
            case CHEMISTRY_ORGANIC ->
                "Focus on catenation, tetravalency ($$Z = 6$$), homologous series ($$\\ce{C_nH_{2n+2}}$$, $$\\ce{C_nH_{2n}}$$, $$\\ce{C_nH_{2n-2}}$$), functional groups ($$\\ce{-OH}$$, $$\\ce{-CHO}$$, $$\\ce{-COOH}$$), and mechanism steps. Indian analogies: interlocking links of an heirloom gold chain for carbon catenation; traditional clay kulhad vs soap micelles trapping haldi/oil stains for saponification. Strictly enclose all molecular structures, functional groups, and chemical reactions in $$...$$ KaTeX notation (e.g. $$\\ce{CH3COOH + C2H5OH <=> CH3COOC2H5 + H2O}$$).";

            case CHEMISTRY_INORGANIC_PHYSICAL ->
                "Focus on stoichiometry, Sorensen pH scale ($$\\text{pH} = -\\log_{10}[\\ce{H+}]$$, $$\\text{pH} + \\text{pOH} = 14$$), mole concept ($$n = \\frac{m}{M}$$), redox reactions, and chemical equilibrium ($$\\Delta G = \\Delta G^\\circ + RT \\ln Q$$). Indian analogies: relieving indigestion with meetha soda ($$\\ce{NaHCO3}$$) or tamarind water neutralizing lime/chuna paste. Strictly enclose all chemical formulas, equilibria, and calculations in $$...$$ KaTeX notation.";

            case PHYSICS ->
                "Emphasize conservation laws, free-body diagrams, kinematics ($$v^2 - u^2 = 2as$$), and rotational dynamics ($$\\tau = I\\alpha$$, $$I = \\sum m_i r_i^2$$). Indian analogies: heavy stone atta chakki flywheel resisting rotational speed changes, balancing a cycle rickshaw on village turns, or steam pressure in an idli cooker. Strictly enclose all physical equations, variables, and units in $$...$$ KaTeX notation.";

            case MATHEMATICS_QUANT ->
                "Focus on shortcut ratio-multipliers, unitary methods, successive net change ($$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$), and compound interest differences ($$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2$$). Indian analogies: traditional kirana store markup vs festival discount bargaining, or train crossing speeds on Indian Railways. Strictly enclose all mathematical expressions, variables, and percentages in $$...$$ KaTeX notation.";

            case BIOLOGY_ECOLOGY ->
                "Detail structural hierarchies, physiological pathways, Hardy-Weinberg equilibrium ($$p^2 + 2pq + q^2 = 1$$), and Lindeman's $$10\\%$$ ecological energy transfer law ($$E_n = E_0 \\times (0.1)^{n-1}$$). Indian analogies: trophic pyramid in Jim Corbett National Park from forest grasses to deer to apex royal Bengal tiger, or cellular organelles functioning like a bustling community langar kitchen. Strictly enclose all biological formulas and ratios in $$...$$ KaTeX notation.";

            case BANKING_ECONOMY ->
                "Emphasize RBI monetary transmission tools, CRR/SLR, Repo Rate dynamics, and NPAs ($$\\text{CRAR} = \\frac{\\text{Tier 1} + \\text{Tier 2 Capital}}{\\text{Risk-Weighted Assets}} \\times 100\\% \\ge 11.5\\%$$). Indian analogies: regulating a canal irrigation sluice gate to prevent both agricultural drought (credit crunch) and flooding (runaway inflation). Strictly enclose all interest rates, reserve ratios, and financial metrics in $$...$$ KaTeX notation.";

            case POLITY_HISTORY_GS ->
                "Structure constitutional provisions, governance mechanisms, Article 368 majorities ($$\\text{Special Majority} \\ge \\frac{2}{3}$$ present and voting and $$> 50\\%$$ total strength), and fiscal indicators ($$\\text{Fiscal Deficit} = \\text{Total Expenditure} - \\text{Non-Debt Receipts}$$). Indian analogies: multi-tiered Gram Panchayat consensus building reflecting federal cooperative balance. Strictly enclose all parliamentary majority formulas and fiscal indicators in $$...$$ KaTeX notation.";

            case REASONING_APTITUDE ->
                "Deconstruct logical premises, syllogistic validity, pattern recognition, and conditional relations. Indian analogies: nested stainless steel tiffin dabba containers illustrating subset-superset containment ($$A \\subset B \\land B \\subset C \\implies A \\subset C$$). Strictly enclose all symbolic logical notations, sets, and patterns in $$...$$ KaTeX notation.";

            case LANGUAGE_COMPREHENSION ->
                "Clarify grammatical rules, syntactic parsing, and contextual nuances. Contrast colloquial spoken habits against standard academic registers. For Hindi (दीर्घ संधि $$a + a = \\bar{a}$$, गुण संधि $$a + i = e$$, समास विग्रह), explain euphonic sound combination and compound classification rules with $$...$$ notation for grammatical rules.";

            case COMPUTER_SCIENCE ->
                "Explain algorithmic complexity, data structures, and systemic constraints ($$\\mathcal{O}(n \\log n)$$, Master Theorem $$T(n) = aT(n/b) + \\Theta(n^k \\log^p n)$$). Indian analogies: searching for a reserved seat in an unreserved train coach (linear scan) vs checking computerized PNR chart at the station entrance (hash map/binary search). Strictly enclose all asymptotic complexities and recurrences in $$...$$ KaTeX notation.";

            case PEDAGOGY_CDP ->
                "Focus on developmental milestones, scaffolding, and formative assessment within the Zone of Proximal Development ($$\\text{ZPD} = \\text{Potential Level} - \\text{Actual Level}$$). Indian analogies: traditional potter gently guiding a child's hands on the spinning wheel (Vygotsky's scaffolding). Strictly enclose all developmental ranges and assessment formulas in $$...$$ KaTeX notation.";
        };
    }

    /**
     * Safely queries sample questions from QuestionRepository matching subject, topic, and subtopic.
     */
    public List<Question> findSampleQuestions(String subject, String topic, String subtopic, String difficulty) {
        if (questionRepository == null) {
            return Collections.emptyList();
        }
        try {
            // First attempt: exact match for subject, topic, and subtopic
            List<Question> questions = questionRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (difficulty != null && !difficulty.isBlank()) {
                    predicates.add(cb.equal(root.get("difficulty"), difficulty));
                }
                predicates.add(root.get("state").in(QuestionStateConstants.PUBLISHED, QuestionStateConstants.APPROVED));
                return cb.and(predicates.toArray(new Predicate[0]));
            });

            if (questions != null) {
                return questions.stream()
                        .filter(q -> q.getContent() != null && !q.getContent().isBlank())
                        .limit(3)
                        .toList();
            }
            return Collections.emptyList();
        } catch (Throwable t) {
            log.debug("Sample question lookup gracefully skipped or failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Formulates formatted prompt block to inject into the LLM prompt.
     */
    public String formatBlueprintPrompt(
            DomainCategory domainCategory,
            String subject,
            String topic,
            String subtopic,
            String targetExam,
            String difficulty,
            String domainDirective,
            List<Question> sampleQuestions) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                DOMAIN BLUEPRINT & PEDAGOGICAL DIRECTIVES:
                - Domain Category: %s
                - Subject: %s
                - Topic: %s
                - Subtopic: %s
                - Target Exam: %s
                - Difficulty Level: %s

                PEDAGOGICAL DIRECTIVE & FORMULA / ANALOGY GUIDELINES:
                %s
                """,
                domainCategory.name(),
                subject != null ? subject : "General",
                topic != null ? topic : "General",
                subtopic != null ? subtopic : "General",
                targetExam != null ? targetExam : "JEE_MAIN",
                difficulty != null ? difficulty : "MEDIUM",
                domainDirective));

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            sb.append("\nREAL EXAM QUESTIONS FROM QUESTION BANK (Analyze style, depth & notation; do not copy verbatim):\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                Question q = sampleQuestions.get(i);
                sb.append(String.format("--- Sample Question %d ---\n", i + 1));
                sb.append("Stem: ").append(q.getContent()).append("\n");
                if (q.getAnswerKey() != null && !q.getAnswerKey().isBlank()) {
                    sb.append("Answer Key: ").append(q.getAnswerKey()).append("\n");
                }
                if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                    sb.append("Explanation: ").append(q.getExplanation()).append("\n");
                }
            }
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
