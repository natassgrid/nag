// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;

/**
 * Collection-based domain classifier for syllabus subjects, topics, and subtopics.
 * Replaces hardcoded cascading if-contains chains with an ordered collection of rules.
 */
@Component
public class DomainClassifier {

    public record DomainClassificationContext(
            String subject,
            String topic,
            String subtopic,
            String combined
    ) {
        public static DomainClassificationContext of(String subject, String topic, String subtopic) {
            String s = (subject != null ? subject : "").toLowerCase(Locale.ROOT);
            String t = (topic != null ? topic : "").toLowerCase(Locale.ROOT);
            String st = (subtopic != null ? subtopic : "").toLowerCase(Locale.ROOT);
            String combined = (s + " " + t + " " + st).trim();
            return new DomainClassificationContext(s, t, st, combined);
        }

        public boolean subjectContainsAny(String... keywords) {
            for (String kw : keywords) {
                if (subject.contains(kw)) {
                    return true;
                }
            }
            return false;
        }

        public boolean combinedContainsAny(Collection<String> keywords) {
            for (String kw : keywords) {
                if (combined.contains(kw)) {
                    return true;
                }
            }
            return false;
        }
    }

    public record DomainRule(
            DomainCategory category,
            Predicate<DomainClassificationContext> predicate
    ) {
        public static DomainRule forCombined(DomainCategory category, Set<String> keywords) {
            return new DomainRule(category, ctx -> ctx.combinedContainsAny(keywords));
        }

        public static DomainRule forSubjectOrCombined(
                DomainCategory category,
                Set<String> subjectKeywords,
                Set<String> combinedKeywords) {
            return new DomainRule(category, ctx ->
                    ctx.subjectContainsAny(subjectKeywords.toArray(new String[0]))
                            || ctx.combinedContainsAny(combinedKeywords)
            );
        }
    }

    private static final Set<String> PEDAGOGY_CDP_KEYWORDS = Set.of(
            "pedagogy", "cdp", "child development", "piaget", "vygotsky", "kohlberg",
            "inclusive education", "cce", "assessment", "child psych", "teaching aptitude",
            "bloom", "scaffolding", "learning theor"
    );

    private static final Set<String> LANGUAGE_KEYWORDS = Set.of(
            "english", "hindi", "grammar", "vocabulary", "synonym", "antonym",
            "voice", "narration", "comprehension", "cloze", "idiom", "para jumble",
            "error spotting", "संधि", "समास", "मुहावरे", "व्याकरण", "विलोम",
            "पर्यायवाची", "तद्भव", "तत्सम", "अलंकार", "लोकोक्ति"
    );

    private static final Set<String> COMPUTER_SCIENCE_KEYWORDS = Set.of(
            "computer", "data structure", "algorithm", "dbms", "database", "networking",
            "cyber security", "operating system", " os ", "computer basics", "ms office",
            "internet", "programming", "sql", "software engineering", "compiler",
            "automata", "dynamic programming"
    );

    private static final Set<String> ORGANIC_CHEM_KEYWORDS = Set.of(
            "carbon and its compounds", "carbon & its compounds", "organic chemistry",
            "organic", "hydrocarbon", "alkane", "alkene", "alkyne", "functional group",
            "polymer", "iupac", "isomer", "haloalkane", "alcohol", "phenol", "ether",
            "aldehyde", "ketone", "carboxylic", "amine", "diazonium", "ester",
            "saponif", "biomolecule", "catenation", "tetravalen", "homologous series",
            "micelle", "soap"
    );

    private static final Set<String> INORGANIC_PHYSICAL_CHEM_KEYWORDS = Set.of(
            "acid", "base", "salt", "ph ", " ph", "ph scale", "ph-", "atomic structure",
            "mole concept", "periodic table", "periodic", "chemical reaction", "redox",
            "metallurgy", "electrochemistry", "chemical bonding", "coordination",
            "p-block", "d-block", "f-block", "s-block", "inorganic", "physical chemistry",
            "colligative", "titration"
    );

    private static final Set<String> PHYSICS_KEYWORDS = Set.of(
            "mechanics", "rotational", "moment of inertia", "torque", "optics",
            "electricity", "circuit", "magnetism", "magnetic", "electromagnet",
            "waves", "ray optics", "wave optics", "modern physics", "kinematics",
            "gravitation", "work & energy", "work and energy", "sound", "electrostatics",
            "current electricity", "semiconductor", "fluid", "oscillation", "shm",
            "thermodynamics", "circular motion", "units and dimension", "newton"
    );

    private static final Set<String> MATH_QUANT_KEYWORDS = Set.of(
            "arithmetic", "percentage", "ratio", "proportion", "profit", "loss",
            "discount", "simple interest", "compound interest", "si/ci", "time & work",
            "time and work", "pipe", "time & distance", "time and distance", "speed",
            "train", "boat", "number system", "algebra", "geometry", "mensuration",
            "trigonometry", "statistics", "probability", "calculus", "data interpretation",
            "quadratic", "matrix", "determinant", "alligation", "mixture", "average",
            "partnership", "permutation", "combination", "coordinate geometry"
    );

    private static final Set<String> BIOLOGY_KEYWORDS = Set.of(
            "botany", "zoology", "ecology", "cell", "genetics", "physiology",
            "biodiversity", "climate change", "environment", "nutrition", "disease",
            "photosynthesis", "respiration", "dna", "rna", "evolution", "reproduction",
            "ecosystem", "biotechnology", "lindeman", "trophic"
    );

    private static final Set<String> BANKING_ECONOMY_KEYWORDS = Set.of(
            "rbi", "monetary policy", "repo rate", "reverse repo", "npa", "basel",
            "upi", "digital banking", "financial market", "inflation", "cpi", "wpi",
            "government schemes", "budget", "fiscal deficit", "crr", "slr",
            "capital adequacy", "crar", "macroeconomic", "microeconomic", "gdp", "national income"
    );

    private static final Set<String> POLITY_HISTORY_KEYWORDS = Set.of(
            "constitution", "fundamental rights", "parliament", "panchayati raj",
            "public administration", "ancient history", "medieval history", "modern history",
            "art & culture", "art and culture", "international relations", "current affairs",
            "supreme court", "governor", "president", "directive principles", "dpsp",
            "monsoon", "freedom struggle", "article 368", "special majority", "preamble"
    );

    private static final Set<String> REASONING_KEYWORDS = Set.of(
            "analogy", "syllogism", "statement & conclusion", "statement and conclusion",
            "coding-decoding", "coding decoding", "series", "blood relation",
            "seating arrangement", "venn diagram", "critical thinking", "data sufficiency",
            "direction sense", "puzzle", "ranking", "cubes and dice", "dice", "clock", "calendar"
    );

    private final List<DomainRule> classificationRules = List.of(
            // 1. Pedagogy & Child Development (CDP)
            DomainRule.forCombined(DomainCategory.PEDAGOGY_CDP, PEDAGOGY_CDP_KEYWORDS),

            // 2. Language & Verbal Comprehension
            DomainRule.forCombined(DomainCategory.LANGUAGE_COMPREHENSION, LANGUAGE_KEYWORDS),

            // 3. Computer Science & IT
            new DomainRule(DomainCategory.COMPUTER_SCIENCE, ctx ->
                    ctx.combinedContainsAny(COMPUTER_SCIENCE_KEYWORDS)
                            || ctx.combined().endsWith(" os")
                            || ctx.subject().contains("computer science")
                            || ctx.topic().contains("computer science")
            ),

            // 4. Organic Chemistry (checked before inorganic/physics)
            DomainRule.forCombined(DomainCategory.CHEMISTRY_ORGANIC, ORGANIC_CHEM_KEYWORDS),

            // 5. Inorganic & Physical Chemistry
            new DomainRule(DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL, ctx ->
                    ctx.subjectContainsAny("chem")
                            || ctx.combinedContainsAny(INORGANIC_PHYSICAL_CHEM_KEYWORDS)
                            || (ctx.combined().contains("equilibrium") && !ctx.subject().contains("physic"))
                            || (ctx.combined().contains("thermodynamics") && (ctx.subject().contains("chem") || ctx.topic().contains("chem")))
            ),

            // 6. Physics
            DomainRule.forSubjectOrCombined(DomainCategory.PHYSICS, Set.of("physic"), PHYSICS_KEYWORDS),

            // 7. Mathematics & Quantitative Aptitude
            DomainRule.forSubjectOrCombined(DomainCategory.MATHEMATICS_QUANT, Set.of("math", "quant"), MATH_QUANT_KEYWORDS),

            // 8. Biology & Ecology
            DomainRule.forSubjectOrCombined(DomainCategory.BIOLOGY_ECOLOGY, Set.of("bio"), BIOLOGY_KEYWORDS),

            // 9. Banking & Financial Economy
            DomainRule.forSubjectOrCombined(DomainCategory.BANKING_ECONOMY, Set.of("bank", "econ"), BANKING_ECONOMY_KEYWORDS),

            // 10. Polity, History & General Studies
            DomainRule.forSubjectOrCombined(
                    DomainCategory.POLITY_HISTORY_GS,
                    Set.of("polity", "history", "gs", "general studies", "geography"),
                    POLITY_HISTORY_KEYWORDS
            ),

            // 11. Reasoning & Aptitude
            DomainRule.forSubjectOrCombined(DomainCategory.REASONING_APTITUDE, Set.of("reasoning", "aptitude"), REASONING_KEYWORDS)
    );

    /**
     * Classifies a syllabus subject, topic, and subtopic into a specific {@link DomainCategory}
     * using the ordered rule collection.
     */
    public DomainCategory classify(String subject, String topic, String subtopic) {
        DomainClassificationContext ctx = DomainClassificationContext.of(subject, topic, subtopic);

        return classificationRules.stream()
                .filter(rule -> rule.predicate().test(ctx))
                .map(DomainRule::category)
                .findFirst()
                .orElseGet(() -> ctx.subject().contains("sci")
                        ? DomainCategory.PHYSICS
                        : DomainCategory.REASONING_APTITUDE);
    }
}
