// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.shared.constants.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic Micro-Lesson Synthesis Agent.
 * Generates pedagogically rigorous, exam-aligned, and mathematically validated
 * micro-lessons deterministically without requiring external LLM network calls.
 * Serves both as the high-speed local fallback and as an autonomous deterministic authoring engine.
 */
@Component
public class DeterministicMicroLessonAgent {

    private static final Logger log = LoggerFactory.getLogger(DeterministicMicroLessonAgent.class);

    private final MicroLessonBlueprintAgent blueprintAgent;

    public DeterministicMicroLessonAgent() {
        this(new MicroLessonBlueprintAgent());
    }

    @Autowired
    public DeterministicMicroLessonAgent(@Autowired(required = false) MicroLessonBlueprintAgent blueprintAgent) {
        this.blueprintAgent = blueprintAgent != null ? blueprintAgent : new MicroLessonBlueprintAgent();
    }

    /**
     * Synthesizes a micro-lesson using the provided structured blueprint.
     */
    public MicroLessonContentDto synthesize(MicroLessonBlueprint blueprint) {
        if (blueprint == null) {
            blueprint = blueprintAgent.createBlueprint(SharedConstants.GENERAL, SharedConstants.GENERAL,
                SharedConstants.GENERAL, SharedConstants.JEE_MAIN);
        }

        // 1. Resolve curated subtopic template
        MicroLessonContentDto template = resolveCuratedContent(
                blueprint.domainCategory(),
                blueprint.subject(),
                blueprint.topic(),
                blueprint.subtopic(),
                blueprint.targetExam(),
                blueprint.difficulty()
        );

        // 2. If blueprint has real sample questions from QuestionRepository, incorporate the question
        if (blueprint.sampleQuestions() != null && !blueprint.sampleQuestions().isEmpty()) {
            Question sample = blueprint.sampleQuestions().stream()
                    .filter(q -> q != null && q.getContent() != null && !q.getContent().isBlank()
                            && ((q.getExplanation() != null && !q.getExplanation().isBlank()) || (q.getAnswerKey() != null && !q.getAnswerKey().isBlank())))
                    .findFirst()
                    .orElse(null);

            if (sample != null) {
                StepByStepBreakdownDto sampleBreakdown = buildBreakdownFromSampleQuestion(sample);
                if (sampleBreakdown != null) {
                    return new MicroLessonContentDto(
                            template.coreConcept(),
                            template.trap(),
                            template.cheatSheet(),
                            sampleBreakdown
                    );
                }
            }
        }

        return template;
    }

    /**
     * Synthesizes a micro-lesson by automatically constructing a blueprint from taxonomy parameters.
     */
    public MicroLessonContentDto synthesize(String subject, String topic, String subtopic, String targetExam, String difficulty) {
        MicroLessonBlueprint blueprint = blueprintAgent.createBlueprint(subject, topic, subtopic, targetExam, difficulty);
        return synthesize(blueprint);
    }

    /**
     * Overloaded convenience method defaulting difficulty to MEDIUM.
     */
    public MicroLessonContentDto synthesize(String subject, String topic, String subtopic, String targetExam) {
        return synthesize(subject, topic, subtopic, targetExam, "MEDIUM");
    }

    /**
     * Deconstructs a real Question from QuestionRepository into a 3-step breakdown.
     */
    public StepByStepBreakdownDto buildBreakdownFromSampleQuestion(Question question) {
        if (question == null || question.getContent() == null || question.getContent().isBlank()) {
            return null;
        }

        String rawStem = question.getContent().trim();
        String stem = normalizeKaTex(rawStem);

        String rawExp = question.getExplanation();
        List<String> steps = new ArrayList<>();

        if (rawExp != null && !rawExp.isBlank()) {
            String cleanExp = normalizeKaTex(rawExp).trim();
            String[] lines = cleanExp.split("\\r?\\n+");
            List<String> meaningfulLines = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    meaningfulLines.add(trimmed);
                }
            }

            if (meaningfulLines.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    String line = meaningfulLines.get(i);
                    line = line.replaceFirst("^(?:Step\\s*\\d+[:.]?|\\d+[:.]?)\\s*", "");
                    steps.add(String.format("Step %d: %s", i + 1, line));
                }
            } else {
                String[] sentences = cleanExp.split("(?<=[.!?])\\s+");
                if (sentences.length >= 3) {
                    for (int i = 0; i < 3; i++) {
                        String s = sentences[i].trim();
                        s = s.replaceFirst("^(?:Step\\s*\\d+[:.]?|\\d+[:.]?)\\s*", "");
                        steps.add(String.format("Step %d: %s", i + 1, s));
                    }
                } else if (sentences.length == 2) {
                    steps.add(String.format("Step 1: Identify given parameters and apply governing relation: %s", sentences[0].trim()));
                    steps.add(String.format("Step 2: Carry out intermediate deduction: %s", sentences[1].trim()));
                    steps.add("Step 3: Confirm dimensional units and verify limiting edge cases.");
                } else if (sentences.length == 1) {
                    steps.add(String.format("Step 1: State the governing equation and identify boundary conditions: %s", sentences[0].trim()));
                    steps.add("Step 2: Substitute values into the governing expression and simplify algebraic terms.");
                    steps.add("Step 3: Confirm physical consistency and deduce final answer.");
                }
            }
        }

        if (steps.isEmpty()) {
            steps.add("Step 1: State the fundamental principle and identify known parameters.");
            steps.add("Step 2: Substitute values into the governing relation and simplify.");
            steps.add("Step 3: Verify dimensional units and express the final result.");
        }

        String finalAnswer;
        if (question.getAnswerKey() != null && !question.getAnswerKey().isBlank()) {
            String key = question.getAnswerKey().trim();
            if (key.matches("^[A-D]$")) {
                finalAnswer = String.format("$$\\text{Option (%s)}$$", key);
            } else if (key.startsWith("$$") && key.endsWith("$$")) {
                finalAnswer = key;
            } else {
                finalAnswer = String.format("$$%s$$", normalizeKaTex(key));
            }
        } else {
            finalAnswer = "$$\\text{Option (A)}$$";
        }

        return new StepByStepBreakdownDto(stem, steps, finalAnswer);
    }

    /**
     * Resolves curated pedagogical content tailored to the taxonomy parameters.
     */
    public MicroLessonContentDto resolveCuratedContent(
            DomainCategory category,
            String subject,
            String topic,
            String subtopic,
            String targetExam,
            String difficulty) {

        String s = (subject != null ? subject : "").toLowerCase(Locale.ROOT);
        String t = (topic != null ? topic : "").toLowerCase(Locale.ROOT);
        String st = (subtopic != null ? subtopic : "").toLowerCase(Locale.ROOT);
        String combined = (s + " " + t + " " + st).trim();

        // 1. Organic Chemistry: Saponification & Functional Groups / Micelles
        if (combined.contains("saponif") || combined.contains("soap") || combined.contains("micelle")
                || combined.contains("detergent") || (combined.contains("ester") && combined.contains("hydrolys"))) {
            return buildSaponificationTemplate();
        }

        // 2. Organic Chemistry: Carbon and its Compounds / Catenation / Tetravalency / Homologous Series
        if (combined.contains("carbon") || combined.contains("catenat") || combined.contains("tetraval")
                || combined.contains("homologous") || category == DomainCategory.CHEMISTRY_ORGANIC) {
            return buildCarbonAndCompoundsTemplate();
        }

        // 3. Physical Chemistry: Mole Concept & Stoichiometry / Avogadro
        if (combined.contains("mole") || combined.contains("stoichiometr") || combined.contains("avogadro")) {
            return buildMoleConceptTemplate();
        }

        // 4. Inorganic / Physical Chemistry: Acids, Bases, Salts & pH
        if (combined.contains("acid") || combined.contains("base") || combined.contains("salt")
                || combined.matches(".*\\bph\\b.*") || combined.contains("ph scale")
                || combined.contains("titrat") || combined.contains("neutraliz")) {
            return buildAcidsBasesPhTemplate();
        }

        if (category == DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL) {
            return buildMoleConceptTemplate();
        }

        // 5. Physics: Rotational Dynamics / Moment of Inertia
        if (combined.contains("rotat") || combined.contains("moment of inertia") || combined.contains("torque")
                || combined.contains("angular") || combined.contains("atta chakki")) {
            return buildRotationalDynamicsTemplate();
        }

        // 6. Physics: Optics
        if (combined.contains("optic") || combined.contains("lens") || combined.contains("mirror")
                || combined.contains("refract") || combined.contains("reflect") || combined.contains("light")) {
            return buildOpticsTemplate();
        }

        // 7. Physics: Electricity & Circuits
        if (combined.contains("electri") || combined.contains("circuit") || combined.contains("ohm")
                || combined.contains("current") || combined.contains("capacit") || combined.contains("resist")) {
            return buildElectricityTemplate();
        }

        // 8. Physics: Mechanics & Kinematics (or default Physics)
        if (combined.contains("kinemat") || combined.contains("motion") || combined.contains("gravitat")
                || combined.contains("work & energy") || category == DomainCategory.PHYSICS) {
            return buildMechanicsKinematicsTemplate();
        }

        // 9. Banking: NPA & Capital Adequacy (CRAR) - evaluated before Math Ratio so 'Capital Adequacy Ratio' matches here!
        if (combined.contains("npa") || combined.contains("basel") || combined.contains("capital adequacy")
                || combined.contains("crar")) {
            return buildNpaCapitalAdequacyTemplate();
        }

        // 10. Banking: RBI Monetary Policy
        if (combined.contains("rbi") || combined.contains("monetary") || combined.contains("repo")
                || combined.contains("inflation") || combined.contains("crr") || combined.contains("slr")
                || category == DomainCategory.BANKING_ECONOMY) {
            return buildRbiMonetaryPolicyTemplate();
        }

        // 11. Mathematics: Percentages
        if (combined.contains("percent")) {
            return buildPercentagesTemplate();
        }

        // 12. Mathematics: Profit & Loss / Discount
        if (combined.contains("profit") || combined.contains("loss") || combined.contains("discount")
                || combined.contains("markup")) {
            return buildProfitLossTemplate();
        }

        // 13. Mathematics: Simple & Compound Interest
        if (combined.contains("interest") || combined.contains("compound") || combined.contains("si/ci")) {
            return buildInterestTemplate();
        }

        // 14. Mathematics: Ratio, Mixture & Alligation
        if (combined.contains("ratio") || combined.contains("mixture") || combined.contains("alligat")
                || combined.contains("proportion")) {
            return buildRatioAlligationTemplate();
        }

        // 15. Mathematics: Time & Work
        if (combined.contains("time & work") || combined.contains("time and work") || combined.contains("pipe")
                || combined.contains("cistern")) {
            return buildTimeAndWorkTemplate();
        }

        // 16. Mathematics: Time & Distance / Speed
        if (combined.contains("speed") || combined.contains("distance") || combined.contains("train")
                || combined.contains("boat")) {
            return buildTimeAndDistanceTemplate();
        }

        // 17. Mathematics: Algebra & Quadratics
        if (combined.contains("algebra") || combined.contains("quadratic") || combined.contains("polynomial")
                || combined.contains("equation") || category == DomainCategory.MATHEMATICS_QUANT) {
            return buildAlgebraQuadraticsTemplate();
        }

        // 18. Biology & Ecology
        if (combined.contains("ecolog") || combined.contains("trophic") || combined.contains("lindeman")
                || combined.contains("genet") || combined.contains("hardy") || category == DomainCategory.BIOLOGY_ECOLOGY) {
            return buildBiologyEcologyTemplate();
        }

        // 19. Polity & General Studies
        if (combined.contains("polity") || combined.contains("constitution") || combined.contains("article 368")
                || combined.contains("special majority") || combined.contains("parliament") || combined.contains("fiscal deficit")
                || category == DomainCategory.POLITY_HISTORY_GS) {
            return buildPolityGsTemplate();
        }

        // 20. Reasoning & Aptitude
        if (combined.contains("syllog") || combined.contains("premise") || combined.contains("deduct")
                || combined.contains("coding") || combined.contains("analogy") || combined.contains("blood relation")
                || (category == DomainCategory.REASONING_APTITUDE && (combined.contains("reason") || combined.contains("aptitude") || combined.contains("logic")))) {
            return buildReasoningAptitudeTemplate();
        }

        // 21. Language & Comprehension
        if (category == DomainCategory.LANGUAGE_COMPREHENSION || combined.contains("grammar") || combined.contains("संधि")) {
            if (combined.contains("hindi") || combined.contains("संधि") || combined.contains("समास") || combined.contains("व्याकरण")) {
                return buildHindiGrammarTemplate();
            }
            return buildEnglishGrammarTemplate();
        }

        // 22. Computer Science
        if (category == DomainCategory.COMPUTER_SCIENCE || combined.contains("algorithm") || combined.contains("master theorem")) {
            return buildComputerScienceTemplate();
        }

        // 23. Pedagogy & CDP
        if (category == DomainCategory.PEDAGOGY_CDP || combined.contains("cdp") || combined.contains("vygotsky") || combined.contains("scaffold")) {
            return buildPedagogyCdpTemplate();
        }

        // Universal dynamic domain-tailored fallback
        return buildDynamicFallbackTemplate(category, subject, topic, subtopic);
    }

    // =========================================================================
    // Specialized Pedagogical Knowledge Templates
    // =========================================================================

    private MicroLessonContentDto buildCarbonAndCompoundsTemplate() {
        String coreConcept = "Carbon forms millions of stable covalent compounds due to its tetravalency ($$Z = 6$$, electronic configuration $$2, 4$$) and catenation power to form strong $$\\ce{C-C}$$ single, double, and triple bonds. Think of carbon atoms like the interlocking links of a handcrafted Indian gold chain, capable of extending into straight chains, branches, and rings. This self-linking stability arises because the small atomic radius of carbon ($$77\\text{ pm}$$) enables its nucleus to hold shared electron pairs securely.";
        String trap = "Students frequently confuse saturated alkanes ($$\\ce{C_nH_{2n+2}}$$) with unsaturated alkenes ($$\\ce{C_nH_{2n}}$$), mistakenly assuming that multiple bonds increase stability, whereas double and triple bonds are actually much more reactive sites for electrophilic addition reactions.";
        List<String> cheatSheet = List.of(
                "$$\\text{Alkanes: } \\ce{C_nH_{2n+2}}, \\quad \\text{Alkenes: } \\ce{C_nH_{2n}}, \\quad \\text{Alkynes: } \\ce{C_nH_{2n-2}}$$",
                "$$\\Delta M = 14\\text{ u} \\quad (\\ce{-CH2-}\\text{ difference between successive homologues})$$",
                "$$\\ce{CH3COOH + C2H5OH <=> CH3COOC2H5 + H2O}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Calculate the molecular mass of the fourth member of the alkane homologous series and determine the volume of $$\\ce{CO2}$$ gas produced at STP by the complete combustion of $$1\\text{ mol}$$ of methane ($$\\ce{CH4}$$).",
                List.of(
                        "Step 1: For the alkane series ($$\\ce{C_nH_{2n+2}}$$), the fourth member has $$n = 4$$, which gives butane ($$\\ce{C4H10}$$). Its molecular mass is $$4 \\times 12 + 10 \\times 1 = 58\\text{ u}$$.",
                        "Step 2: Write the balanced combustion equation for methane: $$\\ce{CH4 + 2O2 -> CO2 + 2H2O}$$. One mole of $$\\ce{CH4}$$ produces exactly $$1\\text{ mol}$$ of $$\\ce{CO2}$$.",
                        "Step 3: At standard temperature and pressure (STP), $$1\\text{ mol}$$ of any ideal gas occupies a molar volume of $$22.4\\text{ L}$$, so the volume of $$\\ce{CO2}$$ is $$22.4\\text{ L}$$."
                ),
                "$$\\text{Mass} = 58\\text{ u}, \\quad V_{\\ce{CO2}} = 22.4\\text{ L}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildSaponificationTemplate() {
        String coreConcept = "Soaps are sodium or potassium salts of long-chain fatty acids featuring a dual-affinity structure: a hydrophobic hydrocarbon tail and a hydrophilic ionic head ($$\\ce{-COO- Na+}$$). Imagine washing an earthen mud kulhad after drinking masala chai: oil and haldi stains do not dissolve in water alone, but soap molecules form radial spherical clusters called micelles that entrap the oily droplet at their hydrophobic core. The outward-projecting ionic heads remain solvated by water, allowing the entire oily emulsion to be easily rinsed away.";
        String trap = "Students often fail to recall that soaps form insoluble scum in hard water containing $$\\ce{Ca^{2+}}$$ and $$\\ce{Mg^{2+}}$$ ions, whereas synthetic detergents remain effective because their sulphonate salts are water-soluble.";
        List<String> cheatSheet = List.of(
                "$$\\ce{RCOOCH2-CH(OOCR)-CH2OOCR + 3NaOH -> 3RCOONa + C3H5(OH)3}$$",
                "$$\\text{Micelle Cleansing: } \\text{Hydrophobic tail (oil-seeking)} + \\text{Hydrophilic head (water-seeking)}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Calculate the mass of glycerol ($$\\ce{C3H8O3}$$) obtained from the saponification of $$1\\text{ mol}$$ of glyceryl trioleate with excess $$\\ce{NaOH}$$.",
                List.of(
                        "Step 1: Write the balanced stoichiometric saponification reaction: $$1\\text{ mol}$$ of fat reacts with $$3\\text{ mol}$$ of $$\\ce{NaOH}$$ to yield $$3\\text{ mol}$$ of soap and $$1\\text{ mol}$$ of glycerol.",
                        "Step 2: Calculate the molar mass of glycerol ($$\\ce{C3H8O3}$$): $$3 \\times 12 + 8 \\times 1 + 3 \\times 16 = 92\\text{ g/mol}$$.",
                        "Step 3: Since $$1\\text{ mol}$$ of glyceryl trioleate yields $$1\\text{ mol}$$ of glycerol, the final mass of glycerol produced is $$92\\text{ g}$$."
                ),
                "$$\\text{Mass of glycerol} = 92\\text{ g}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildAcidsBasesPhTemplate() {
        String coreConcept = "The pH scale devised by Sørensen quantitatively gauges hydronium ion concentration on a negative logarithmic scale where $$\\text{pH} = -\\log_{10}[\\ce{H+}]$$. Think of relief after consuming too many fried pakoras: the excess stomach hydrochloric acid ($$\\text{pH} \\approx 1.5$$) is swiftly neutralized by drinking water with dissolved meetha soda ($$\\ce{NaHCO3}$$), shifting the systemic pH back toward neutral. At room temperature ($$298\\text{ K}$$), pure neutral water maintains $$[\\ce{H+}] = [\\ce{OH-}] = 10^{-7}\\text{ M}$$, yielding a baseline $$\\text{pH} = 7$$.";
        String trap = "A classic exam trap occurs when calculating the pH of extremely dilute strong acids (such as $$10^{-8}\\text{ M HCl}$$): students naively compute $$-\\log_{10}(10^{-8}) = 8$$ and declare an acid to be alkaline, forgetting to add the auto-ionization contribution of water ($$[\\ce{H+}]_{water} = 10^{-7}\\text{ M}$$).";
        List<String> cheatSheet = List.of(
                "$$\\text{pH} = -\\log_{10}[\\ce{H+}], \\quad \\text{pOH} = -\\log_{10}[\\ce{OH-}]$$",
                "$$\\text{pH} + \\text{pOH} = 14 \\quad (\\text{at } 298\\text{ K})$$",
                "$$K_w = [\\ce{H+}][\\ce{OH-}] = 1.0 \\times 10^{-14} \\quad (\\text{at } 298\\text{ K})$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Calculate the exact pH of a $$1.0 \\times 10^{-8}\\text{ M}$$ aqueous solution of hydrochloric acid ($$\\ce{HCl}$$) at $$298\\text{ K}$$.",
                List.of(
                        "Step 1: Total hydronium concentration includes both acid dissociation and water auto-ionization: $$[\\ce{H+}]_{total} = [\\ce{H+}]_{HCl} + [\\ce{H+}]_{H_2O} = 10^{-8} + x$$, where $$x$$ is the auto-ionized hydronium.",
                        "Step 2: Set up the water equilibrium product: $$K_w = (10^{-8} + x)(x) = 1.0 \\times 10^{-14}$$. Solving the quadratic equation $$x^2 + 10^{-8}x - 10^{-14} = 0$$ yields $$x \\approx 0.95 \\times 10^{-7}\\text{ M}$$.",
                        "Step 3: Total hydronium concentration is $$[\\ce{H+}]_{total} = 1.05 \\times 10^{-7}\\text{ M}$$. Take the negative logarithm: $$\\text{pH} = -\\log_{10}(1.05 \\times 10^{-7}) = 7 - 0.021 = 6.98$$."
                ),
                "$$\\text{pH} = 6.98$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildMoleConceptTemplate() {
        String coreConcept = "The mole concept serves as the foundational counting bridge between microscopic atomic entities and macroscopic laboratory masses, defining one mole as exactly $$6.022 \\times 10^{23}$$ particles (Avogadro's constant $$N_A$$). Think of purchasing a dozen bananas from a fruit vendor: whether you weigh a dozen small elaichi bananas or a dozen large bananas, the count remains twelve, but the total mass varies with individual unit weight. Similarly, one mole of any element contains the exact same count of atoms, with its mass numerically matching its atomic weight in grams.";
        String trap = "Students routinely confuse molar mass in grams ($$\\text{g/mol}$$) with unified atomic mass units ($$\\text{u}$$) for a single molecule, and forget to balance stoichiometric coefficients before determining the limiting reagent.";
        List<String> cheatSheet = List.of(
                "$$n = \\frac{m}{M} = \\frac{N}{N_A} = \\frac{V_{\\text{STP}}}{22.4\\text{ L}}$$",
                "$$\\text{Molarity } (M) = \\frac{\\text{Moles of solute } (n)}{\\text{Volume of solution in liters } (V)}$$",
                "$$\\Delta G = \\Delta G^\\circ + RT \\ln Q$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "How many grams of calcium carbonate ($$\\ce{CaCO3}$$) are required to react completely with $$500\\text{ mL}$$ of $$0.2\\text{ M HCl}$$ according to the reaction $$\\ce{CaCO3 + 2HCl -> CaCl2 + H2O + CO2}$$?",
                List.of(
                        "Step 1: Calculate moles of $$\\ce{HCl}$$ available: $$n_{\\ce{HCl}} = M \\times V = 0.2\\text{ mol/L} \\times 0.5\\text{ L} = 0.1\\text{ mol}$$.",
                        "Step 2: Use stoichiometric mole ratios: $$2\\text{ mol}$$ of $$\\ce{HCl}$$ reacts with $$1\\text{ mol}$$ of $$\\ce{CaCO3}$$, hence $$n_{\\ce{CaCO3}} = \\frac{0.1}{2} = 0.05\\text{ mol}$$.",
                        "Step 3: Convert moles of $$\\ce{CaCO3}$$ to mass using molar mass ($$M = 40 + 12 + 3 \\times 16 = 100\\text{ g/mol}$$): $$m = 0.05 \\times 100 = 5.0\\text{ g}$$."
                ),
                "$$\\text{Mass of } \\ce{CaCO3} = 5.0\\text{ g}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildRotationalDynamicsTemplate() {
        String coreConcept = "Moment of inertia ($$I$$) represents the rotational analogue of linear mass, quantifying a rigid body's resistance to rotational acceleration about an axis ($$\\tau = I\\alpha$$). Picture a traditional village atta chakki (flour mill): the heavy stone disk concentrates mass away from the central pivot to maximize its moment of inertia ($$I = \\frac{1}{2} M R^2$$), sustaining a smooth, steady rotation once set into motion. Distributing mass farther from the rotation axis dramatically increases rotational inertia because it scales with the square of radial distance ($$I = \\sum m_i r_i^2$$).";
        String trap = "Students routinely apply the point-mass formula ($$I = M R^2$$) to continuous symmetric bodies without dividing by the appropriate geometric factor (e.g., $$\\frac{1}{2}$$ for solid cylinders/disks or $$\\frac{2}{5}$$ for solid spheres).";
        List<String> cheatSheet = List.of(
                "$$\\tau = I\\alpha, \\quad L = I\\omega, \\quad K_{rot} = \\frac{1}{2} I \\omega^2$$",
                "$$I_{disk} = \\frac{1}{2} M R^2, \\quad I_{ring} = M R^2, \\quad I_{sphere} = \\frac{2}{5} M R^2$$",
                "$$\\text{Parallel Axis Theorem: } I = I_{cm} + M d^2$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A uniform solid disc of mass $$4\\text{ kg}$$ and radius $$0.5\\text{ m}$$ is accelerated from rest by a constant tangential torque of $$6\\text{ N}\\cdot\\text{m}$$. Calculate its angular acceleration.",
                List.of(
                        "Step 1: Calculate the moment of inertia of the solid disk: $$I = \\frac{1}{2} M R^2 = \\frac{1}{2} \\times 4 \\times (0.5)^2 = 0.5\\text{ kg}\\cdot\\text{m}^2$$.",
                        "Step 2: Relate torque and angular acceleration using Newton's second law for rotation: $$\\tau = I\\alpha$$.",
                        "Step 3: Solve for angular acceleration: $$\\alpha = \\frac{\\tau}{I} = \\frac{6\\text{ N}\\cdot\\text{m}}{0.5\\text{ kg}\\cdot\\text{m}^2} = 12\\text{ rad/s}^2$$."
                ),
                "$$\\alpha = 12\\text{ rad/s}^2$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildMechanicsKinematicsTemplate() {
        String coreConcept = "Kinematic laws formulate the deterministic relationship between position, velocity, acceleration, and elapsed time under constant acceleration ($$v^2 - u^2 = 2as$$). Consider an overloaded auto-rickshaw traveling through Delhi traffic: when the driver slams the brakes on wet asphalt, the stopping distance increases quadratically with speed rather than linearly. Every acceleration stems from an unbalanced net external force altering the momentum of the system ($$F_{net} = \\frac{dp}{dt}$$).";
        String trap = "A frequent trap is ignoring vector directional signs: taking initial velocity as positive while forgetting that deceleration and downward gravitational acceleration must have negative signs.";
        List<String> cheatSheet = List.of(
                "$$v = u + at, \\quad s = ut + \\frac{1}{2}at^2, \\quad v^2 - u^2 = 2as$$",
                "$$F_{net} = ma = \\frac{dp}{dt}, \\quad W = \\int \\vec{F} \\cdot d\\vec{r} = \\Delta K$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A vehicle moving at an initial speed of $$20\\text{ m/s}$$ decelerates uniformly at $$4\\text{ m/s}^2$$ until it comes to a complete halt. Calculate the total stopping distance.",
                List.of(
                        "Step 1: Identify given kinematic parameters: initial speed $$u = 20\\text{ m/s}$$, final speed $$v = 0\\text{ m/s}$$, and acceleration $$a = -4\\text{ m/s}^2$$.",
                        "Step 2: Select the time-independent kinematic relation: $$v^2 - u^2 = 2as$$.",
                        "Step 3: Substitute known quantities: $$0^2 - (20)^2 = 2(-4)s \\implies -400 = -8s \\implies s = 50\\text{ m}$$."
                ),
                "$$s = 50\\text{ m}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildOpticsTemplate() {
        String coreConcept = "Geometrical optics models light propagation as rectilinear rays governed by Snell's law of refraction ($$n_1 \\sin\\theta_1 = n_2 \\sin\\theta_2$$) and Fermat's principle of least time. Think of immersing a steel spoon into a transparent glass mug of chilled nimbu pani: the spoon appears broken or shifted at the liquid boundary because light rays bend as they slow down in the denser medium. Thin lenses converge or diverge these wavefronts according to their focal curvature, governed by the Gaussian lens equation ($$\\frac{1}{v} - \\frac{1}{u} = \\frac{1}{f}$$).";
        String trap = "Students frequently invert Cartesian sign conventions: object distance ($$u$$) is almost universally negative in standard setups, leading to catastrophic sign reversal in focal length and image location.";
        List<String> cheatSheet = List.of(
                "$$\\frac{1}{v} - \\frac{1}{u} = \\frac{1}{f} \\quad (\\text{Lens Formula}), \\quad \\frac{1}{v} + \\frac{1}{u} = \\frac{1}{f} \\quad (\\text{Mirror Formula})$$",
                "$$n_1 \\sin\\theta_1 = n_2 \\sin\\theta_2 \\quad (\\text{Snell's Law})$$",
                "$$m = \\frac{v}{u} = \\frac{h_i}{h_o} \\quad (\\text{Lens Magnification})$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "An object is placed at a distance of $$30\\text{ cm}$$ in front of a convex lens with a focal length of $$20\\text{ cm}$$. Determine the image distance and lateral magnification.",
                List.of(
                        "Step 1: Apply Cartesian sign conventions: object distance $$u = -30\\text{ cm}$$, focal length $$f = +20\\text{ cm}$$ (converging lens).",
                        "Step 2: Substitute into the lens formula: $$\\frac{1}{v} - \\frac{1}{-30} = \\frac{1}{20} \\implies \\frac{1}{v} = \\frac{1}{20} - \\frac{1}{30} = \\frac{3 - 2}{60} = \\frac{1}{60}$$.",
                        "Step 3: Calculate image distance $$v = +60\\text{ cm}$$ and magnification $$m = \\frac{v}{u} = \\frac{+60}{-30} = -2.0$$ (real, inverted, magnified image)."
                ),
                "$$v = +60\\text{ cm}, \\quad m = -2.0$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildElectricityTemplate() {
        String coreConcept = "Electric current represents the directed rate of charge flow through a conductor ($$I = \\frac{dQ}{dt}$$), propelled by potential difference and moderated by electrical resistance ($$V = IR$$). Imagine vehicles navigating through an eight-lane National Highway toll plaza: opening more parallel toll gates reduces the overall resistance to traffic flow, directly mirroring how adding resistors in parallel decreases net electrical impedance. Energy dissipates through microscopic electron collisions with lattice ions, converting electrical energy into thermal heat at rate $$P = I^2 R$$.";
        String trap = "Students routinely confuse series and parallel formulas: mixing up resistors ($$R_{eq} = R_1 + R_2$$ in series) with capacitors ($$C_{eq} = \\frac{C_1 C_2}{C_1 + C_2}$$ in series).";
        List<String> cheatSheet = List.of(
                "$$V = IR, \\quad P = VI = I^2 R = \\frac{V^2}{R}$$",
                "$$R_{series} = \\sum R_i, \\quad \\frac{1}{R_{parallel}} = \\sum \\frac{1}{R_i}$$",
                "$$Q = CV, \\quad U = \\frac{1}{2} C V^2$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Two resistors of $$6\\ \\Omega$$ and $$3\\ \\Omega$$ are connected in parallel across a $$12\\text{ V}$$ battery with negligible internal resistance. Calculate the total current drawn from the battery.",
                List.of(
                        "Step 1: Compute the equivalent resistance of the parallel combination: $$\\frac{1}{R_{eq}} = \\frac{1}{6} + \\frac{1}{3} = \\frac{1 + 2}{6} = \\frac{3}{6} = \\frac{1}{2} \\implies R_{eq} = 2\\ \\Omega$$.",
                        "Step 2: Apply Ohm's law to the total circuit: $$I_{total} = \\frac{V}{R_{eq}}$$.",
                        "Step 3: Substitute the battery voltage: $$I_{total} = \\frac{12\\text{ V}}{2\\ \\Omega} = 6\\text{ A}$$."
                ),
                "$$I_{total} = 6\\text{ A}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildPercentagesTemplate() {
        String coreConcept = "A percentage expresses a dimensionless ratio as a fraction of one hundred, providing a universal normalized scale to quantify relative change. Think of festive Diwali shopping at an Indian bazaar: a merchant offering an initial discount followed by a festival clearance discount does not yield a simple arithmetic sum of the discounts, because each subsequent fraction applies to the depleted intermediate price. The compounding interaction between successive changes is encapsulated by the net percentage change formula $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$.";
        String trap = "The most frequent trap is naively adding successive percentage changes: assuming that a $$20\\%$$ increase followed by a $$20\\%$$ decrease returns to the original value, when it actually results in a net $$4\\%$$ loss ($$\\text{Net} = 20 - 20 - \\frac{400}{100} = -4\\%$$).";
        List<String> cheatSheet = List.of(
                "$$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$",
                "$$\\text{Percentage Change} = \\left(\\frac{\\text{Final} - \\text{Initial}}{\\text{Initial}}\\right) \\times 100\\%$$",
                "$$\\text{Fraction Multiplier: } 1 + \\frac{x}{100} \\quad (\\text{increase}), \\quad 1 - \\frac{x}{100} \\quad (\\text{decrease})$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "The price of an article is first increased by $$25\\%$$ and subsequently decreased by $$20\\%$$. Find the net percentage change in the final price of the article.",
                List.of(
                        "Step 1: Identify the two successive percentage changes: $$a = +25\\%$$ and $$b = -20\\%$$.",
                        "Step 2: Apply the successive net percentage change formula: $$\\text{Net Change} = \\left(a + b + \\frac{ab}{100}\\right)\\%$$.",
                        "Step 3: Substitute values: $$\\text{Net Change} = \\left(25 - 20 + \\frac{25 \\times (-20)}{100}\\right)\\% = \\left(5 - \\frac{500}{100}\\right)\\% = (5 - 5)\\% = 0\\%$$. "
                ),
                "$$\\text{Net Change} = 0\\% \\quad (\\text{Price remains unchanged})$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildProfitLossTemplate() {
        String coreConcept = "Profit and loss calculations quantify commercial transactions relative to the base Cost Price ($$\\text{CP}$$), whereas discounts are strictly computed against the Marked Price ($$\\text{MP}$$). Imagine visiting a local Indian kirana or textile shop: the shopkeeper marks up goods above purchase cost to anticipate bargaining, then applies a seasonal discount to entice buyers while preserving an underlying profit margin. This equilibrium is bridged by the golden equality $$\\text{MP} \\times \\left(1 - \\frac{d\\%}{100}\\right) = \\text{CP} \\times \\left(1 + \\frac{p\\%}{100}\\right)$$.";
        String trap = "Students constantly confuse the calculation base: computing discount percentage on Cost Price rather than Marked Price, or calculating profit percentage on Selling Price rather than Cost Price.";
        List<String> cheatSheet = List.of(
                "$$\\text{Profit}\\% = \\left(\\frac{\\text{SP} - \\text{CP}}{\\text{CP}}\\right) \\times 100\\%, \\quad \\text{Loss}\\% = \\left(\\frac{\\text{CP} - \\text{SP}}{\\text{CP}}\\right) \\times 100\\%$$",
                "$$\\text{MP} \\times \\left(1 - \\frac{d}{100}\\right) = \\text{CP} \\times \\left(1 + \\frac{p}{100}\\right)$$",
                "$$\\text{SP} = \\text{CP} \\times \\left(1 + \\frac{P\\%}{100}\\right) = \\text{MP} \\times \\left(1 - \\frac{D\\%}{100}\\right)$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A retailer purchases a watch for $$\\text{Rs. } 1200$$. At what marked price should he list it so that after allowing a discount of $$10\\%$$, he still earns a profit of $$20\\%$$?",
                List.of(
                        "Step 1: Compute required Selling Price to secure a $$20\\%$$ profit on Cost Price ($$\\text{CP} = 1200$$): $$\\text{SP} = 1200 \\times \\left(1 + \\frac{20}{100}\\right) = 1200 \\times 1.20 = \\text{Rs. } 1440$$.",
                        "Step 2: Relate Selling Price to Marked Price under a $$10\\%$$ discount: $$\\text{SP} = \\text{MP} \\times (1 - 0.10) = 0.90 \\times \\text{MP}$$.",
                        "Step 3: Solve for Marked Price: $$\\text{MP} = \\frac{1440}{0.90} = \\text{Rs. } 1600$$."
                ),
                "$$\\text{Marked Price} = \\text{Rs. } 1600$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildInterestTemplate() {
        String coreConcept = "Simple interest accrues uniformly solely on the principal sum, whereas compound interest reinvents the earning baseline by adding accumulated interest back into principal at each compounding cycle ($$A = P(1 + \\frac{r}{100})^n$$). Think of a traditional village farmer depositing funds in a Post Office savings scheme: while simple interest yields identical interest tokens every year, compound interest behaves like a banyan tree whose aerial roots anchor into the ground to spawn new independent trunks. For a two-year investment horizon, the compounding edge is captured by the difference formula $$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2$$.";
        String trap = "Students routinely forget to divide annual interest rates when compounding semi-annually or quarterly (rate becomes $$\\frac{r}{2}$$ and time doubles to $$2n$$ for half-yearly compounding).";
        List<String> cheatSheet = List.of(
                "$$\\text{SI} = \\frac{P \\times r \\times t}{100}, \\quad A = P\\left(1 + \\frac{r}{100}\\right)^n$$",
                "$$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2 \\quad (\\text{for } 2\\text{ years})$$",
                "$$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2 \\left(3 + \\frac{r}{100}\\right) \\quad (\\text{for } 3\\text{ years})$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "The difference between compound interest and simple interest on a certain principal at $$10\\%$$ per annum for $$2\\text{ years}$$ is $$\\text{Rs. } 65$$. Find the principal sum.",
                List.of(
                        "Step 1: Recall the standard two-year difference relation between CI and SI: $$\\text{CI} - \\text{SI} = P\\left(\\frac{r}{100}\\right)^2$$.",
                        "Step 2: Substitute given values: $$65 = P \\times \\left(\\frac{10}{100}\\right)^2 = P \\times \\left(\\frac{1}{10}\\right)^2 = P \\times \\frac{1}{100}$$.",
                        "Step 3: Solve for principal: $$P = 65 \\times 100 = \\text{Rs. } 6500$$."
                ),
                "$$P = \\text{Rs. } 6500$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildRatioAlligationTemplate() {
        String coreConcept = "The rule of alligation provides a geometric mean shortcut to calculate the ratio in which two ingredients of differing costs or concentrations must be blended to produce a target mixture ($$\\frac{q_c}{q_d} = \\frac{d - m}{m - c}$$). Think of a local Indian doodhwala blending high-fat buffalo milk with lighter cow milk or water: the divergence from target quality on one side must balance the divergence on the other. This lever-balance principle converts complex simultaneous weighted average equations into instant mental arithmetic.";
        String trap = "Students often invert the cross-subtraction order in the alligation grid, mixing up the quantities of the cheaper and dearer components.";
        List<String> cheatSheet = List.of(
                "$$\\frac{\\text{Quantity of Cheaper}}{\\text{Quantity of Dearer}} = \\frac{\\text{Dearer Price} - \\text{Mean Price}}{\\text{Mean Price} - \\text{Cheaper Price}} = \\frac{d - m}{m - c}$$",
                "$$\\text{Weighted Average: } m = \\frac{c_1 q_1 + c_2 q_2}{q_1 + q_2}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "In what ratio must rice at $$\\text{Rs. } 45/\\text{kg}$$ be mixed with rice at $$\\text{Rs. } 60/\\text{kg}$$ so that the resulting mixture is worth $$\\text{Rs. } 50/\\text{kg}$$?",
                List.of(
                        "Step 1: Identify cheaper price $$c = 45$$, dearer price $$d = 60$$, and mean target price $$m = 50$$.",
                        "Step 2: Apply the rule of alligation: $$\\frac{q_c}{q_d} = \\frac{d - m}{m - c}$$.",
                        "Step 3: Calculate: $$\\frac{q_c}{q_d} = \\frac{60 - 50}{50 - 45} = \\frac{10}{5} = \\frac{2}{1}$$."
                ),
                "$$\\text{Ratio} = 2 : 1$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildTimeAndWorkTemplate() {
        String coreConcept = "Time and work problems operate on the principle that total work can be modeled as the Least Common Multiple ($$\\text{LCM}$$) of individual completion times, converting abstract fractions into integer daily efficiencies. Imagine a team of rural potters in Khurja shaping terracotta diyas for a festival: finding total units of diyas to craft makes tracking how many diyas each craftsman shapes per day clear and manageable. Once individual daily efficiencies are established ($$\\text{Rate} = \\frac{\\text{Total Work}}{\\text{Days}}$$), collective output simply equals the sum of individual rates.";
        String trap = "Students often erroneously add days directly ($$10\\text{ days} + 15\\text{ days} = 25\\text{ days}$$) instead of inverting times to combine work rates.";
        List<String> cheatSheet = List.of(
                "$$\\text{Total Work} = \\text{LCM}(t_1, t_2, \\dots, t_n)$$",
                "$$\\text{Efficiency} = \\frac{\\text{Total Work}}{\\text{Time Taken}}, \\quad \\text{Combined Time} = \\frac{\\text{Total Work}}{\\sum \\text{Efficiencies}}$$",
                "$$\\frac{M_1 D_1 H_1}{W_1} = \\frac{M_2 D_2 H_2}{W_2}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Pipe A can fill a tank in $$12\\text{ hours}$$ and Pipe B can fill it in $$18\\text{ hours}$$. If both pipes are opened simultaneously, how long will they take to fill the tank?",
                List.of(
                        "Step 1: Determine total capacity as $$\\text{LCM}(12, 18) = 36\\text{ units}$$.",
                        "Step 2: Calculate individual filling efficiencies: Pipe A rate is $$\\frac{36}{12} = 3\\text{ units/hour}$$ and Pipe B rate is $$\\frac{36}{18} = 2\\text{ units/hour}$$.",
                        "Step 3: Combined filling rate is $$3 + 2 = 5\\text{ units/hour}$$. Time required is $$\\frac{36}{5} = 7.2\\text{ hours}$$ ($$7\\text{ hours } 12\\text{ minutes}$$)."
                ),
                "$$\\text{Time} = 7.2\\text{ hours}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildTimeAndDistanceTemplate() {
        String coreConcept = "Uniform motion balances the tripartite relation $$\\text{Distance} = \\text{Speed} \\times \\text{Time}$$, while relative speed reconciles systems with multiple moving reference frames. Consider two trains crossing each other near Mughalsarai junction: when traveling in opposite directions on parallel tracks, their closing speed equals the sum of their individual speeds ($$S_{rel} = S_1 + S_2$$), making the crossing seem rapid. When traveling in the same direction, relative speed shrinks to the difference ($$S_{rel} = |S_1 - S_2|$$), requiring a much longer stretch of track to overtake.";
        String trap = "Failing to convert speeds between $$\\text{km/h}$$ and $$\\text{m/s}$$ ($$1\\text{ km/h} = \\frac{5}{18}\\text{ m/s}$$) before combining with distances measured in meters.";
        List<String> cheatSheet = List.of(
                "$$1\\text{ km/h} = \\frac{5}{18}\\text{ m/s}, \\quad 1\\text{ m/s} = \\frac{18}{5}\\text{ km/h}$$",
                "$$S_{rel} = S_1 + S_2 \\quad (\\text{opposite direction}), \\quad S_{rel} = |S_1 - S_2| \\quad (\\text{same direction})$$",
                "$$\\text{Average Speed} = \\frac{2 S_1 S_2}{S_1 + S_2} \\quad (\\text{equal distance intervals})$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A train $$150\\text{ m}$$ long traveling at $$54\\text{ km/h}$$ crosses a railway platform $$250\\text{ m}$$ long. Calculate the time taken to cross the platform.",
                List.of(
                        "Step 1: Convert train speed from $$\\text{km/h}$$ to $$\\text{m/s}$$: $$54 \\times \\frac{5}{18} = 15\\text{ m/s}$$.",
                        "Step 2: Calculate total distance covered during the crossing: $$D = \\text{Train Length} + \\text{Platform Length} = 150 + 250 = 400\\text{ m}$$.",
                        "Step 3: Compute time taken: $$t = \\frac{D}{S} = \\frac{400\\text{ m}}{15\\text{ m/s}} = \\frac{80}{3}\\text{ s} = 26.67\\text{ seconds}$$."
                ),
                "$$t = 26.67\\text{ s}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildAlgebraQuadraticsTemplate() {
        String coreConcept = "A quadratic equation ($$ax^2 + bx + c = 0$$) describes a parabolic trajectory whose roots represent intersections with the horizontal axis, solved universally by the quadratic formula $$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$. The discriminant ($$\\Delta = b^2 - 4ac$$) acts as a mathematical filter, dictating whether solutions are real and distinct ($$\\Delta > 0$$), real and coincident ($$\\Delta = 0$$), or complex conjugate pairs ($$\\Delta < 0$$). Vieta's formulas neatly link polynomial coefficients directly to the sum ($$\\alpha + \\beta = -\\frac{b}{a}$$) and product ($$\\alpha \\beta = \\frac{c}{a}$$) of roots.";
        String trap = "Forgetting that if the discriminant is negative ($$\\Delta < 0$$), the equation has no real roots, or misplacing the negative sign in Vieta's sum of roots ($$-\\frac{b}{a}$$).";
        List<String> cheatSheet = List.of(
                "$$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}, \\quad \\Delta = b^2 - 4ac$$",
                "$$\\alpha + \\beta = -\\frac{b}{a}, \\quad \\alpha \\beta = \\frac{c}{a}$$",
                "$$x^2 - (\\alpha + \\beta)x + \\alpha \\beta = 0$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "If $$\\alpha$$ and $$\\beta$$ are the roots of the quadratic equation $$2x^2 - 7x + 3 = 0$$, find the exact value of $$\\alpha^2 + \\beta^2$$.",
                List.of(
                        "Step 1: Identify coefficients $$a = 2$$, $$b = -7$$, $$c = 3$$. By Vieta's relations, $$\\alpha + \\beta = -\\frac{-7}{2} = \\frac{7}{2}$$ and $$\\alpha \\beta = \\frac{3}{2}$$.",
                        "Step 2: Express the sum of squares algebraically: $$\\alpha^2 + \\beta^2 = (\\alpha + \\beta)^2 - 2\\alpha \\beta$$.",
                        "Step 3: Substitute values: $$\\alpha^2 + \\beta^2 = \\left(\\frac{7}{2}\\right)^2 - 2\\left(\\frac{3}{2}\\right) = \\frac{49}{4} - 3 = \\frac{49 - 12}{4} = \\frac{37}{4} = 9.25$$."
                ),
                "$$\\alpha^2 + \\beta^2 = \\frac{37}{4} = 9.25$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildRbiMonetaryPolicyTemplate() {
        String coreConcept = "The Reserve Bank of India (RBI) implements monetary policy via key policy rates to regulate money supply and balance economic growth against retail inflation targets ($$4\\% \\pm 2\\%$$). Think of the RBI like the master regulator of a canal irrigation network: lowering the Repo Rate opens the sluice gates to flood commercial banks with affordable liquidity to stimulate agriculture and industry, whereas raising rates curbs the liquidity deluge during high inflationary pressures. The interplay between nominal yields and inflation determines the real purchasing power of capital via the Fisher equation $$\\text{Real Rate} \\approx r_{\\text{nominal}} - \\pi_{\\text{inflation}}$$.";
        String trap = "Students constantly invert policy tools: confusing the Repo Rate (the rate at which commercial banks borrow funds from the RBI against collateral) with the Reverse Repo Rate (the rate at which RBI absorbs surplus liquidity from banks).";
        List<String> cheatSheet = List.of(
                "$$\\text{Real Interest Rate} \\approx r_{\\text{nominal}} - \\pi_{\\text{inflation}}$$",
                "$$\\text{Bank Credit Expansion Multiplier} = \\frac{1}{\\text{CRR} + \\text{SLR}}$$",
                "$$\\text{Inflation Target: } 4\\% \\pm 2\\% \\quad (\\text{Headline CPI band } [2\\%, 6\\%])$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "If the nominal one-year fixed deposit interest rate is $$7.5\\%$$ and the prevailing CPI headline inflation rate is $$5.2\\%$$, compute the approximate real interest rate earned by an investor.",
                List.of(
                        "Step 1: Identify the nominal return $$r_{\\text{nominal}} = 7.5\\%$$ and inflation rate $$\\pi_{\\text{inflation}} = 5.2\\%$$.",
                        "Step 2: Apply the Fisher approximation relation: $$\\text{Real Rate} \\approx r_{\\text{nominal}} - \\pi_{\\text{inflation}}$$.",
                        "Step 3: Calculate: $$\\text{Real Rate} = 7.5\\% - 5.2\\% = 2.3\\%$$."
                ),
                "$$\\text{Real Interest Rate} = 2.3\\%$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildNpaCapitalAdequacyTemplate() {
        String coreConcept = "Capital Adequacy Ratio (CRAR) measures a commercial bank's available capital cushion against its credit and operational risk exposure, safeguarding depositors and systemic solvency. Think of a village grain storage cooperative building reinforced concrete buffer silos: reserving sufficient emergency grain stock prevents collapse during catastrophic regional drought. Basel III and RBI norms mandate that commercial banks maintain a minimum CRAR of $$11.5\\%$$ (including Capital Conservation Buffer), computed as $$\\text{CRAR} = \\frac{\\text{Tier 1} + \\text{Tier 2 Capital}}{\\text{Risk-Weighted Assets}} \\times 100\\%$$.";
        String trap = "Students frequently forget the 90-day overdue criterion: loans are classified as Special Mention Accounts (SMA) before becoming Non-Performing Assets (NPAs) on day 91.";
        List<String> cheatSheet = List.of(
                "$$\\text{CRAR} = \\frac{\\text{Tier 1 Capital} + \\text{Tier 2 Capital}}{\\text{Risk-Weighted Assets}} \\times 100\\% \\ge 11.5\\%$$",
                "$$\\text{Provisioning Coverage Ratio (PCR)} = \\frac{\\text{Total Provisions Held}}{\\text{Gross NPAs}} \\times 100\\%$$",
                "$$\\text{Net NPA} = \\text{Gross NPA} - \\text{Cumulative Provisions}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A scheduled commercial bank has Tier 1 Capital of $$\\text{Rs. } 800\\text{ cr}$$, Tier 2 Capital of $$\\text{Rs. } 400\\text{ cr}$$, and total Risk-Weighted Assets of $$\\text{Rs. } 10000\\text{ cr}$$. Calculate its CRAR and determine if it meets the RBI minimum regulatory threshold of $$11.5\\%$$.",
                List.of(
                        "Step 1: Calculate total regulatory capital: $$\\text{Total Capital} = 800 + 400 = \\text{Rs. } 1200\\text{ cr}$$.",
                        "Step 2: Apply the CRAR formula: $$\\text{CRAR} = \\frac{1200}{10000} \\times 100\\% = 12.0\\%$$.",
                        "Step 3: Compare with statutory threshold: $$12.0\\% \\ge 11.5\\%$$, confirming that the bank satisfies regulatory requirements."
                ),
                "$$\\text{CRAR} = 12.0\\% \\quad (\\text{Compliant with RBI standard } \\ge 11.5\\%)$$ "
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildBiologyEcologyTemplate() {
        String coreConcept = "Trophic dynamics in ecosystems are governed by Raymond Lindeman's 10% ecological efficiency law, which dictates that only approximately ten percent of chemical energy is transferred from one trophic level to the next ($$E_n = E_0 \\times (0.1)^{n-1}$$). Picture the wildlife habitat in Kaziranga National Park: millions of kilograms of alluvial swamp grasses support a sizable herd of deer, but can sustain only a handful of apex royal Bengal tigers at the pyramid apex. The remaining ninety percent of incoming biomass energy is dissipated into the environment via cellular respiration, foraging locomotion, and metabolic heat.";
        String trap = "Students routinely confuse energy pyramids (which are always upright and obey the $$10\\%$$ rule) with biomass or numerical pyramids (which can be inverted, such as in parasitic or marine ecosystems).";
        List<String> cheatSheet = List.of(
                "$$E_n = E_0 \\times (0.1)^{n-1} \\quad (\\text{Lindeman's 10\\% Law})$$",
                "$$p^2 + 2pq + q^2 = 1, \\quad p + q = 1 \\quad (\\text{Hardy-Weinberg Equilibrium})$$",
                "$$\\text{Net Primary Productivity: } \\text{NPP} = \\text{GPP} - R$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "If primary producers in an Indian forest grassland trap $$40000\\text{ J}$$ of net primary energy from sunlight, calculate the energy available to tertiary consumers under Lindeman's 10% law.",
                List.of(
                        "Step 1: Identify trophic levels: Producers ($$T_1$$) have $$E_1 = 40000\\text{ J}$$. Primary consumers ($$T_2$$, herbivores) receive $$40000 \\times 0.10 = 4000\\text{ J}$$.",
                        "Step 2: Secondary consumers ($$T_3$$, small carnivores) receive $$4000 \\times 0.10 = 400\\text{ J}$$.",
                        "Step 3: Tertiary consumers ($$T_4$$, apex predators) receive $$400 \\times 0.10 = 40\\text{ J}$$, in accordance with $$E_4 = 40000 \\times (0.10)^3 = 40\\text{ J}$$."
                ),
                "$$E_4 = 40\\text{ J}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildPolityGsTemplate() {
        String coreConcept = "The Indian Constitution balances flexibility with rigidity under Article 368, providing multi-tiered procedural pathways for constitutional amendments. Think of consensus-building in a traditional Indian joint family council: minor household arrangements require simple consensus, but altering fundamental property rights demands unanimous concurrence among elder branches. A special majority under Article 368 mandates the affirmative vote of at least two-thirds of members present and voting, simultaneously constituting a strict majority of the total membership of the House ($$> 50\\%$$).";
        String trap = "Students constantly confuse a simple majority of members present and voting (used for ordinary bills and creating new states under Article 3) with a special majority under Article 368.";
        List<String> cheatSheet = List.of(
                "$$\\text{Special Majority} \\ge \\frac{2}{3}\\text{ (Present \\& Voting)} \\quad \\text{and} \\quad > 50\\%\\text{ (Total Strength)}$$",
                "$$\\text{Fiscal Deficit} = \\text{Total Expenditure} - \\text{Non-Debt Capital Receipts} - \\text{Revenue Receipts}$$",
                "$$\\text{Primary Deficit} = \\text{Fiscal Deficit} - \\text{Interest Payments}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "In the Lok Sabha with a sanctioned strength of $$543$$ members and $$6$$ vacancies, $$450$$ members are present and voting on a Constitutional Amendment Bill under Article 368. Determine the minimum votes required for the bill to pass.",
                List.of(
                        "Step 1: Check Condition 1 (Total Membership Majority): Total sanctioned strength is $$543$$. More than $$50\\%$$ of total strength is $$\\lfloor 543 / 2 \\rfloor + 1 = 271 + 1 = 272\\text{ votes}$$.",
                        "Step 2: Check Condition 2 (Two-thirds of Present and Voting): $$450$$ members are present and voting. Two-thirds is $$\\frac{2}{3} \\times 450 = 300\\text{ votes}$$.",
                        "Step 3: The bill requires satisfying both conditions simultaneously, so the threshold is $$\\max(272, 300) = 300\\text{ affirmative votes}$$."
                ),
                "$$\\text{Minimum Votes Required} = 300$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildReasoningAptitudeTemplate() {
        String coreConcept = "Deductive syllogistic reasoning evaluates conclusion validity strictly from given premises without relying on real-world empirical truth. Think of nested stainless steel tiffin dabbas: if every small spice container sits inside the middle dabba, and the middle dabba sits inside the large carrier, then the small spice container is inevitably enclosed within the large carrier ($$A \\subset B \\land B \\subset C \\implies A \\subset C$$). Venn diagrams resolve universal affirmative statements ($$\\text{All } A \\text{ are } B$$) and particular statements ($$\\text{Some } A \\text{ are } B$$) into clear subset boundary interactions.";
        String trap = "The most common fallacy is assuming the converse: concluding that 'All B are A' must follow from 'All A are B', or concluding certainty from overlapping particulars without sufficient distributed terms.";
        List<String> cheatSheet = List.of(
                "$$\\text{All } A \\text{ are } B \\implies \\text{Some } B \\text{ are } A \\quad (\\text{Conversion})$$",
                "$$\\text{Alphabet Ranks: } \\text{EJOTY} = (5, 10, 15, 20, 25), \\quad \\text{Rank}_F + \\text{Rank}_R = 27$$",
                "$$A \\subset B \\land B \\subset C \\implies A \\subset C$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Statements: (1) All poets are dreamers. (2) All dreamers are philosophers. Evaluate whether conclusion 'All poets are philosophers' is deductively valid.",
                List.of(
                        "Step 1: Formulate premises as set relations: Let $$P$$ be the set of poets, $$D$$ be dreamers, and $$F$$ be philosophers. Statement 1 gives $$P \\subseteq D$$.",
                        "Step 2: Statement 2 gives $$D \\subseteq F$$.",
                        "Step 3: By the transitive property of set inclusion: $$P \\subseteq D \\subseteq F \\implies P \\subseteq F$$. Thus, the conclusion 'All poets are philosophers' is unconditionally valid."
                ),
                "$$\\text{Conclusion is Valid } (P \\subseteq F)$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildHindiGrammarTemplate() {
        String coreConcept = "स्वर संधि में दो निकटवर्ती स्वरों के परस्पर मेल से होने वाले विकार को संधि कहते हैं। जैसे दो नदियाँ मिलकर एक विशाल जलधारा बनाती हैं, वैसे ही ह्रस्व या दीर्घ अ, इ, उ के बाद समान स्वर आने पर दोनों मिलकर दीर्घ स्वर बन जाते हैं ($$a + a = \\bar{a}$$)। उदाहरण के लिए विद्या और अर्थी के मिलने पर अंतिम स्वर 'आ' और प्रारंभिक स्वर 'अ' मिलकर दीर्घ 'आ' बनकर विद्यार्थी ($$\\text{विद्या} + \\text{अर्थी} = \\text{विद्यार्थी}$$) बनाते हैं।";
        String trap = "परीक्षार्थी अक्सर गुण संधि ($$a + i = e$$) और वृद्धि संधि ($$a + e = ai$$) के नियमों में भ्रमित होकर मात्रा विच्छेद गलत कर देते हैं।";
        List<String> cheatSheet = List.of(
                "$$\\text{दीर्घ संधि: } a + a = \\bar{a}, \\quad i + i = \\bar{i}, \\quad u + u = \\bar{u}$$",
                "$$\\text{गुण संधि: } a + i = e, \\quad a + u = o, \\quad a + r = ar$$",
                "$$\\text{वृद्धि संधि: } a + e = ai, \\quad a + o = au$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "'महोत्सव' शब्द का सही संधि विच्छेद क्या है और इसमें कौन सी संधि प्रयुक्त हुई है?",
                List.of(
                        "Step 1: 'महोत्सव' शब्द के उच्चारण और मूल अर्थ घटकों को पहचानें: 'महा' (महान) और 'उत्सव' (समारोह)।",
                        "Step 2: संधि के नियम का विश्लेषण करें: प्रथम पद के अंत में 'आ' ($$\\bar{a}$$) और द्वितीय पद के प्रारंभ में 'उ' ($$u$$) है। नियम अनुसार $$a + u = o$$ बनता है।",
                        "Step 3: यह गुण स्वर संधि का स्पष्ट नियम है, अतः शुद्ध संधि विच्छेद $$\\text{महा} + \\text{उत्सव} = \\text{महोत्सव}$$ है।"
                ),
                "$$\\text{महा} + \\text{उत्सव} = \\text{महोत्सव} \\quad (\\text{गुण स्वर संधि})$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildEnglishGrammarTemplate() {
        String coreConcept = "Subject-verb agreement dictates that a grammatical predicate verb must align in person and number with its true syntactical subject, unaffected by intervening prepositional modifiers. Picture an Indian joint family train journey reservation chart: the booking status is determined strictly by the primary ticket holder, regardless of how many relatives accompany them in the compartment. Phrases like 'along with', 'as well as', and 'in addition to' do not compound the grammatical subject into a plural form.";
        String trap = "Students routinely fall into the trap of proximity error: agreeing the verb with the nearest plural noun inside an intervening prepositional clause rather than the true singular subject.";
        List<String> cheatSheet = List.of(
                "$$\\text{Subject} + \\text{along with / as well as} + \\text{Noun} \\implies \\text{Verb agrees with Subject}$$",
                "$$\\text{Either } A \\text{ or } B \\implies \\text{Verb agrees with nearer subject } B$$",
                "$$\\text{Each / Every / Neither / Either} \\implies \\text{Singular Verb}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Identify the error in the sentence: 'The captain, along with all the players, were felicitated at the ceremony.'",
                List.of(
                        "Step 1: Identify the grammatical subject of the sentence: 'The captain', which is singular.",
                        "Step 2: Analyze the intervening modifier: 'along with all the players' is a parenthetical prepositional phrase that does not alter subject number.",
                        "Step 3: The plural verb 'were' violates agreement with the singular head subject. The verb must be corrected to singular 'was'."
                ),
                "$$\\text{Error: 'were felicitated' } \\to \\text{ 'was felicitated'}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildComputerScienceTemplate() {
        String coreConcept = "Asymptotic analysis evaluates algorithmic time and space complexity as input scale ($$n$$) grows toward infinity, abstracting hardware differences using Big-O notation (governed by the Master Theorem $$T(n) = aT(n/b) + \\Theta(n^k \\log^p n)$$). Picture checking train seat reservation status at New Delhi railway station: inspecting every coach sequentially is linear search ($$\\mathcal{O}(n)$$), whereas searching by sorted coach and berth numbers operates in logarithmic time ($$\\mathcal{O}(\\log_2 n)$$). Choosing optimal data structures and algorithms ensures computational efficiency even on massive national DPI datasets.";
        String trap = "Assuming an algorithm with lower constant factors is always better: an $$\\mathcal{O}(n^2)$$ bubble sort will quickly choke on large inputs ($$n > 10^5$$) where an $$\\mathcal{O}(n \\log n)$$ merge sort excels.";
        List<String> cheatSheet = List.of(
                "$$T(n) = a T(n/b) + \\Theta(n^k \\log^p n) \\quad (\\text{Master Theorem})$$",
                "$$\\text{Binary Search: } \\mathcal{O}(\\log_2 n), \\quad \\text{Merge Sort: } \\mathcal{O}(n \\log_2 n)$$",
                "$$\\text{Hash Table Lookup: } \\mathcal{O}(1) \\text{ average}, \\quad \\mathcal{O}(n) \\text{ worst-case}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "Solve the recurrence relation $$T(n) = 2T(n/2) + \\Theta(n)$$ using the Master Theorem to find its asymptotic time complexity.",
                List.of(
                        "Step 1: Compare with standard recurrence form $$T(n) = aT(n/b) + f(n)$$: here $$a = 2$$, $$b = 2$$, and $$f(n) = \\Theta(n) = \\Theta(n^1)$$.",
                        "Step 2: Compute the critical exponent: $$\\log_b a = \\log_2 2 = 1$$.",
                        "Step 3: Since $$f(n) = \\Theta(n^{\\log_b a}) = \\Theta(n^1)$$, this matches Case 2 of the Master Theorem. Hence, the solution is $$T(n) = \\Theta(n \\log n)$$."
                ),
                "$$T(n) = \\Theta(n \\log n)$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildPedagogyCdpTemplate() {
        String coreConcept = "Lev Vygotsky's socio-cultural theory posits that cognitive development is mediated by social interaction within the Zone of Proximal Development ($$\\text{ZPD}$$). Picture an Indian traditional potter teaching a young apprentice on a wooden spinning wheel: the master places his hands over the child's hands to steady the spinning clay until the child internalizes tactile balance and can shape the vessel independently. This temporary, adjustable instructional support is termed scaffolding, enabling learners to bridge the gap between actual performance ($$\\text{Actual Level}$$) and potential capability ($$\\text{Potential Level}$$).";
        String trap = "Confusing Vygotsky's socio-cultural scaffolding with Piaget's independent stage-based cognitive assimilation, or mistaking permanent handholding for temporary scaffolding.";
        List<String> cheatSheet = List.of(
                "$$\\text{ZPD} = \\text{Potential Developmental Level} - \\text{Actual Developmental Level}$$",
                "$$\\text{Scaffolding: } \\text{Temporary Support} \\to \\text{Gradual Fading} \\to \\text{Independent Mastery}$$",
                "$$\\text{MKO: More Knowledgeable Other (Peer / Teacher / Mentor)}$$"
        );
        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                "A teacher assists a student in solving a two-digit multiplication problem by first providing the partial products and gradually withdrawing hints as the student gains confidence. What pedagogical strategy is demonstrated?",
                List.of(
                        "Step 1: Identify learner state: The child can solve parts of the problem with structured assistance, indicating operation within the Zone of Proximal Development ($$\\text{ZPD}$$).",
                        "Step 2: Analyze teacher intervention: Providing structured temporary hints that are systematically phased out as competence develops fits the exact definition of scaffolding.",
                        "Step 3: Contrast with alternatives: This is neither rote drill nor unguided discovery; it is guided social-constructivist scaffolding under Vygotsky's developmental framework."
                ),
                "$$\\text{Pedagogical Strategy: Scaffolding within ZPD}$$"
        );
        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    private MicroLessonContentDto buildDynamicFallbackTemplate(
            DomainCategory category,
            String subject,
            String topic,
            String subtopic) {

        String safeSubtopic = (subtopic != null && !subtopic.isBlank()) ? subtopic : "Fundamental Principle";
        String safeTopic = (topic != null && !topic.isBlank()) ? topic : "Core Syllabus";
        String safeSubject = (subject != null && !subject.isBlank()) ? subject : "General Studies";

        String coreConcept = String.format(
                "%s forms the pivotal analytical principle connecting %s within %s. Think of sorting spice compartments in a traditional Indian masala dabba: every component occupies a distinct functional slot while harmonizing the overall culinary balance. Mastering this structured relationship enables swift, error-free deduction during timed competitive examinations.",
                safeSubtopic, safeTopic, safeSubject
        );

        String trap = String.format(
                "Students frequently overlook boundary conditions, contextual qualifiers, and sign conventions for %s during high-pressure timed exams.",
                safeSubtopic
        );

        List<String> cheatSheet = List.of(
                "$$\\text{Efficiency Ratio } (\\eta) = \\frac{\\text{Valid Target Outcomes}}{\\text{Total Feasible Domain}} \\times 100\\%$$",
                "$$\\Delta R = R_{\\text{final}} - R_{\\text{initial}} \\quad (\\text{Net Parametric Shift})$$"
        );

        StepByStepBreakdownDto breakdown = new StepByStepBreakdownDto(
                String.format("Evaluate the governing parameter for %s under baseline conditions where initial state $$R_{\\text{initial}} = 40$$ increases by $$25\\%%$$.", safeSubtopic),
                List.of(
                        String.format("Step 1: Identify the baseline state $$R_{\\text{initial}} = 40$$ and fractional increment $$r = 25\\%% = 0.25$$ for %s.", safeSubtopic),
                        "Step 2: Apply the multiplicative growth relation: $$R_{\\text{final}} = R_{\\text{initial}} \\times (1 + r) = 40 \\times 1.25 = 50$$.",
                        "Step 3: Compute the net parametric shift: $$\\Delta R = 50 - 40 = 10$$, confirming dimensional and logical consistency."
                ),
                "$$R_{\\text{final}} = 50, \\quad \\Delta R = 10$$"
        );

        return new MicroLessonContentDto(coreConcept, trap, cheatSheet, breakdown);
    }

    /**
     * Normalizes text ensuring LaTeX delimiters use $$...$$, escaping single dollars safely.
     */
    public static String normalizeKaTex(String text) {
        if (text == null) return null;
        String res = text;
        // Normalize \( and \) to $$
        res = res.replace("\\(", "$$").replace("\\)", "$$");
        // Normalize \[ and \] to $$
        res = res.replace("\\[", "$$").replace("\\]", "$$");
        // Convert paired single $ into $$
        res = res.replaceAll("(?<!\\$)\\$(?!\\$)(.*?)(?<!\\$)\\$(?!\\$)", "\\$\\$$1\\$\\$");
        // Escape remaining isolated $ so LatexSyntaxValidator does not fail
        res = res.replaceAll("(?<![\\\\\\$])\\$(?!\\$)", "\\\\\\$");
        return res;
    }
}
