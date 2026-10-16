// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.microlesson.ai.KaTexValidator;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeterministicMicroLessonAgent Deterministic Synthesis & KaTeX Validation Tests")
class DeterministicMicroLessonAgentTest {

    private DeterministicMicroLessonAgent agent;
    private KaTexValidator kaTexValidator;

    @BeforeEach
    void setUp() {
        agent = new DeterministicMicroLessonAgent();
        kaTexValidator = new KaTexValidator();
    }

    private void assertValidAndZeroErrors(MicroLessonContentDto content) {
        assertThat(content).isNotNull();
        assertThat(content.coreConcept()).isNotBlank();
        assertThat(content.trap()).isNotBlank();
        assertThat(content.cheatSheet()).isNotEmpty();
        assertThat(content.stepByStepBreakdown()).isNotNull();
        assertThat(content.stepByStepBreakdown().questionStem()).isNotBlank();
        assertThat(content.stepByStepBreakdown().steps()).isNotEmpty();
        assertThat(content.stepByStepBreakdown().finalAnswer()).isNotBlank();

        List<String> errors = kaTexValidator.validateContent(content);
        assertThat(errors)
                .withFailMessage("KaTeX validation failed with errors: %s", errors)
                .isEmpty();
    }

    @Test
    @DisplayName("Carbon and its Compounds: Produces valid micro-lesson with zero KaTeX errors")
    void testCarbonAndCompoundsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Science", "Chemistry", "Carbon and its Compounds", "CBSE_10");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("tetravalency");
        assertThat(content.coreConcept()).contains("gold chain");
        assertThat(content.cheatSheet().get(0)).contains("\\ce{C_nH_{2n+2}}");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("58");
    }

    @Test
    @DisplayName("Saponification & Micelles: Produces valid micro-lesson with zero KaTeX errors")
    void testSaponificationSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Chemistry", "Organic Chemistry", "Saponification and Micelles", "NEET_UG");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("kulhad");
        assertThat(content.coreConcept()).contains("micelles");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("92");
    }

    @Test
    @DisplayName("Acids, Bases, Salts & pH: Produces valid micro-lesson with zero KaTeX errors")
    void testAcidsBasesPhSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Chemistry", "Physical", "Acids, Bases, Salts and pH Scale", "JEE_MAIN");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("Sørensen");
        assertThat(content.coreConcept()).contains("meetha soda");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("6.98");
    }

    @Test
    @DisplayName("Mole Concept & Stoichiometry: Produces valid micro-lesson with zero KaTeX errors")
    void testMoleConceptSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Chemistry", "Physical Chemistry", "Mole Concept and Stoichiometry", "JEE_MAIN");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("Avogadro");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("5.0");
    }

    @Test
    @DisplayName("Rotational Dynamics & Atta Chakki: Produces valid micro-lesson with zero KaTeX errors")
    void testRotationalDynamicsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Physics", "Mechanics", "Moment of Inertia and Rotational Dynamics", "JEE_ADVANCED");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("atta chakki");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("12");
    }

    @Test
    @DisplayName("Mechanics & Kinematics: Produces valid micro-lesson with zero KaTeX errors")
    void testMechanicsKinematicsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Physics", "Mechanics", "Kinematics in One Dimension", "JEE_MAIN");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("auto-rickshaw");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("50");
    }

    @Test
    @DisplayName("Optics: Produces valid micro-lesson with zero KaTeX errors")
    void testOpticsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Physics", "Optics", "Refraction and Thin Lenses", "NEET_UG");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("nimbu pani");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("60");
    }

    @Test
    @DisplayName("Electricity & Circuits: Produces valid micro-lesson with zero KaTeX errors")
    void testElectricitySynthesis() {
        MicroLessonContentDto content = agent.synthesize("Physics", "Current Electricity", "Electric Circuits and Ohm's Law", "JEE_MAIN");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("toll plaza");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("6");
    }

    @Test
    @DisplayName("Percentages: Produces valid micro-lesson with zero KaTeX errors")
    void testPercentagesSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Mathematics", "Arithmetic", "Percentage and Successive Change", "SSC_CGL");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("bazaar");
        assertThat(content.cheatSheet().get(0)).contains("\\text{Net Change}");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("0\\%");
    }

    @Test
    @DisplayName("Profit and Loss: Produces valid micro-lesson with zero KaTeX errors")
    void testProfitLossSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Quantitative Aptitude", "Arithmetic", "Profit, Loss and Discount", "BANK_PO");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("kirana");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("1600");
    }

    @Test
    @DisplayName("Simple & Compound Interest: Produces valid micro-lesson with zero KaTeX errors")
    void testInterestSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Quantitative Aptitude", "Arithmetic", "Compound Interest and CI-SI Difference", "SSC_CGL");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("banyan tree");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("6500");
    }

    @Test
    @DisplayName("Ratio & Alligation: Produces valid micro-lesson with zero KaTeX errors")
    void testRatioAlligationSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Quantitative Aptitude", "Arithmetic", "Rule of Alligation and Mixtures", "CAT");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("doodhwala");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("2 : 1");
    }

    @Test
    @DisplayName("Time and Work: Produces valid micro-lesson with zero KaTeX errors")
    void testTimeAndWorkSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Quantitative Aptitude", "Arithmetic", "Time and Work and Pipes", "SSC_CGL");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("diyas");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("7.2");
    }

    @Test
    @DisplayName("Time and Distance: Produces valid micro-lesson with zero KaTeX errors")
    void testTimeAndDistanceSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Quantitative Aptitude", "Arithmetic", "Time, Speed and Distance", "RRB_NTPC");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("Mughalsarai");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("26.67");
    }

    @Test
    @DisplayName("Algebra & Quadratics: Produces valid micro-lesson with zero KaTeX errors")
    void testAlgebraQuadraticsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Mathematics", "Algebra", "Quadratic Equations and Roots", "JEE_MAIN");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("parabolic");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("9.25");
    }

    @Test
    @DisplayName("RBI Monetary Policy: Produces valid micro-lesson with zero KaTeX errors")
    void testRbiMonetaryPolicySynthesis() {
        MicroLessonContentDto content = agent.synthesize("Banking Awareness", "Economy", "RBI Monetary Policy and Repo Rate", "RBI_GRADE_B");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("canal irrigation");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("2.3\\%");
    }

    @Test
    @DisplayName("NPA & Capital Adequacy: Produces valid micro-lesson with zero KaTeX errors")
    void testNpaCapitalAdequacySynthesis() {
        MicroLessonContentDto content = agent.synthesize("Banking Awareness", "Financial System", "NPA and Capital Adequacy Ratio (CRAR)", "IBPS_PO");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("grain storage");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("12.0\\%");
    }

    @Test
    @DisplayName("Biology & Ecology (Lindeman's Law): Produces valid micro-lesson with zero KaTeX errors")
    void testBiologyEcologySynthesis() {
        MicroLessonContentDto content = agent.synthesize("Biology", "Ecology", "Trophic Levels and Energy Transfer", "NEET_UG");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("Kaziranga");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("40");
    }

    @Test
    @DisplayName("Polity & Article 368: Produces valid micro-lesson with zero KaTeX errors")
    void testPolityGsSynthesis() {
        MicroLessonContentDto content = agent.synthesize("General Studies", "Polity", "Constitutional Amendment Article 368", "UPSC_CSE");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("joint family council");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("300");
    }

    @Test
    @DisplayName("Reasoning & Syllogism: Produces valid micro-lesson with zero KaTeX errors")
    void testReasoningAptitudeSynthesis() {
        MicroLessonContentDto content = agent.synthesize("General Intelligence", "Logical Reasoning", "Syllogism and Deductive Logic", "SSC_CGL");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("tiffin");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("Valid");
    }

    @Test
    @DisplayName("Hindi Grammar (संधि): Produces valid micro-lesson with zero KaTeX errors")
    void testHindiGrammarSynthesis() {
        MicroLessonContentDto content = agent.synthesize("सामान्य हिंदी", "व्याकरण", "स्वर संधि और भेद", "UP_TET");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("संधि");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("महोत्सव");
    }

    @Test
    @DisplayName("English Grammar: Produces valid micro-lesson with zero KaTeX errors")
    void testEnglishGrammarSynthesis() {
        MicroLessonContentDto content = agent.synthesize("General English", "Grammar", "Subject-Verb Agreement", "BANK_CLERK");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("reservation chart");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("was felicitated");
    }

    @Test
    @DisplayName("Computer Science (Master Theorem): Produces valid micro-lesson with zero KaTeX errors")
    void testComputerScienceSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Computer Science", "Algorithms", "Divide and Conquer and Master Theorem", "GATE_CS");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("New Delhi railway station");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("\\Theta(n \\log n)");
    }

    @Test
    @DisplayName("Pedagogy & CDP (Vygotsky ZPD): Produces valid micro-lesson with zero KaTeX errors")
    void testPedagogyCdpSynthesis() {
        MicroLessonContentDto content = agent.synthesize("Child Development and Pedagogy", "CDP", "Vygotsky Zone of Proximal Development", "CTET");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("spinning wheel");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("Scaffolding");
    }

    @Test
    @DisplayName("Dynamic Fallback: Correctly produces valid micro-lesson with zero KaTeX errors for unknown topics")
    void testDynamicFallback() {
        MicroLessonContentDto content = agent.synthesize("Civil Engineering", "Geotechnical", "Mohr Coulomb Failure Criterion", "GATE_CE");
        assertValidAndZeroErrors(content);
        assertThat(content.coreConcept()).contains("Mohr Coulomb Failure Criterion");
    }

    @Test
    @DisplayName("Incorporate real sample questions from blueprint into StepByStepBreakdown")
    void testIncorporateSampleQuestionFromBlueprint() {
        Question sampleQuestion = Question.builder()
                .subject("Physics")
                .topic("Mechanics")
                .subtopic("Moment of Inertia")
                .content("Calculate the moment of inertia of a solid cylinder of mass $$2\\text{ kg}$$ and radius $$0.4\\text{ m}$$.")
                .answerKey("0.16 \\text{ kg}\\cdot\\text{m}^2")
                .explanation("Step 1: Formula for solid cylinder is $$I = \\frac{1}{2} M R^2$$.\nStep 2: Substitute $$M=2, R=0.4$$ to get $$I = \\frac{1}{2} \\times 2 \\times 0.16 = 0.16$$.\nStep 3: Units are $$\\text{kg}\\cdot\\text{m}^2$$.")
                .state("PUBLISHED")
                .build();

        MicroLessonBlueprint blueprint = new MicroLessonBlueprint(
                DomainCategory.PHYSICS,
                "Physics",
                "Mechanics",
                "Moment of Inertia",
                "JEE_MAIN",
                "MEDIUM",
                "Directive",
                List.of(sampleQuestion),
                "Formatted prompt"
        );

        MicroLessonContentDto content = agent.synthesize(blueprint);
        assertValidAndZeroErrors(content);

        // Core concept and trap are preserved from the curated template
        assertThat(content.coreConcept()).contains("atta chakki");
        assertThat(content.trap()).contains("point-mass formula");

        // Breakdown stem and steps are derived from the sample question
        assertThat(content.stepByStepBreakdown().questionStem()).contains("solid cylinder");
        assertThat(content.stepByStepBreakdown().steps().get(0)).contains("solid cylinder");
        assertThat(content.stepByStepBreakdown().finalAnswer()).contains("0.16");
    }

    @Test
    @DisplayName("KaTeX normalizer handles parentheses and single dollars properly")
    void testNormalizeKaTex() {
        assertThat(DeterministicMicroLessonAgent.normalizeKaTex("Let \\(x = 5\\) and \\[y = 10\\]"))
                .isEqualTo("Let $$x = 5$$ and $$y = 10$$");
        assertThat(DeterministicMicroLessonAgent.normalizeKaTex("Value is $x + y$ here"))
                .isEqualTo("Value is $$x + y$$ here");
    }
}
