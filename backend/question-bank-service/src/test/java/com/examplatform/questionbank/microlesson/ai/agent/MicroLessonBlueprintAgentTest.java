// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai.agent;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MicroLessonBlueprintAgent Tests")
class MicroLessonBlueprintAgentTest {

    @Mock
    private QuestionRepository questionRepository;

    private MicroLessonBlueprintAgent agentWithRepo;
    private MicroLessonBlueprintAgent agentWithoutRepo;

    @BeforeEach
    void setUp() {
        agentWithRepo = new MicroLessonBlueprintAgent(questionRepository);
        agentWithoutRepo = new MicroLessonBlueprintAgent();
    }

    @Test
    @DisplayName("Classifies domain taxonomy into appropriate DomainCategory")
    void testDomainClassification() {
        assertThat(agentWithoutRepo.classifyDomain("Science", "Chemistry", "Carbon and its Compounds"))
                .isEqualTo(DomainCategory.CHEMISTRY_ORGANIC);
        assertThat(agentWithoutRepo.classifyDomain("Chemistry", "Organic", "Hydrocarbons"))
                .isEqualTo(DomainCategory.CHEMISTRY_ORGANIC);
        assertThat(agentWithoutRepo.classifyDomain("Chemistry", "Physical", "Acids, Bases, Salts and pH"))
                .isEqualTo(DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL);
        assertThat(agentWithoutRepo.classifyDomain("Chemistry", "Physical", "Mole Concept"))
                .isEqualTo(DomainCategory.CHEMISTRY_INORGANIC_PHYSICAL);
        assertThat(agentWithoutRepo.classifyDomain("Physics", "Mechanics", "Rotational Dynamics"))
                .isEqualTo(DomainCategory.PHYSICS);
        assertThat(agentWithoutRepo.classifyDomain("Physics", "Optics", "Ray Optics"))
                .isEqualTo(DomainCategory.PHYSICS);
        assertThat(agentWithoutRepo.classifyDomain("Mathematics", "Arithmetic", "Percentage"))
                .isEqualTo(DomainCategory.MATHEMATICS_QUANT);
        assertThat(agentWithoutRepo.classifyDomain("Quantitative Aptitude", "Commercial Math", "Profit and Loss"))
                .isEqualTo(DomainCategory.MATHEMATICS_QUANT);
        assertThat(agentWithoutRepo.classifyDomain("Biology", "Zoology", "Ecology and Energy Flow"))
                .isEqualTo(DomainCategory.BIOLOGY_ECOLOGY);
        assertThat(agentWithoutRepo.classifyDomain("Banking Awareness", "Economy", "RBI Monetary Policy"))
                .isEqualTo(DomainCategory.BANKING_ECONOMY);
        assertThat(agentWithoutRepo.classifyDomain("General Studies", "Polity", "Indian Constitution Article 368"))
                .isEqualTo(DomainCategory.POLITY_HISTORY_GS);
        assertThat(agentWithoutRepo.classifyDomain("General Intelligence", "Reasoning", "Syllogism"))
                .isEqualTo(DomainCategory.REASONING_APTITUDE);
        assertThat(agentWithoutRepo.classifyDomain("English", "Grammar", "Subject-Verb Agreement"))
                .isEqualTo(DomainCategory.LANGUAGE_COMPREHENSION);
        assertThat(agentWithoutRepo.classifyDomain("सामान्य हिंदी", "व्याकरण", "संधि"))
                .isEqualTo(DomainCategory.LANGUAGE_COMPREHENSION);
        assertThat(agentWithoutRepo.classifyDomain("Computer Science", "Algorithms", "Master Theorem"))
                .isEqualTo(DomainCategory.COMPUTER_SCIENCE);
        assertThat(agentWithoutRepo.classifyDomain("Child Development", "CDP", "Vygotsky Scaffolding"))
                .isEqualTo(DomainCategory.PEDAGOGY_CDP);
    }

    @Test
    @DisplayName("Creates comprehensive blueprint with domain directives and prompt formatting")
    void testCreateBlueprint() {
        MicroLessonBlueprint blueprint = agentWithoutRepo.createBlueprint(
                "Physics", "Mechanics", "Moment of Inertia", "JEE_ADVANCED", "HARD"
        );

        assertThat(blueprint).isNotNull();
        assertThat(blueprint.domainCategory()).isEqualTo(DomainCategory.PHYSICS);
        assertThat(blueprint.subject()).isEqualTo("Physics");
        assertThat(blueprint.topic()).isEqualTo("Mechanics");
        assertThat(blueprint.subtopic()).isEqualTo("Moment of Inertia");
        assertThat(blueprint.targetExam()).isEqualTo("JEE_ADVANCED");
        assertThat(blueprint.difficulty()).isEqualTo("HARD");
        assertThat(blueprint.domainDirective()).contains("atta chakki");
        assertThat(blueprint.domainDirective()).contains("$$\\tau = I\\alpha$$");
        assertThat(blueprint.sampleQuestions()).isEmpty();
        assertThat(blueprint.formattedBlueprintPrompt()).contains("DOMAIN BLUEPRINT & PEDAGOGICAL DIRECTIVES");
        assertThat(blueprint.formattedBlueprintPrompt()).contains("PHYSICS");
    }

    @Test
    @DisplayName("Gracefully queries QuestionRepository and includes real questions in blueprint")
    @SuppressWarnings("unchecked")
    void testBlueprintWithSampleQuestions() {
        Question q = Question.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .content("Calculate the moment of inertia of a disc.")
                .answerKey("0.5 kg m^2")
                .explanation("Step 1: Formula is I = 0.5 * M * R^2.")
                .state("PUBLISHED")
                .build();

        when(questionRepository.findAll(any(Specification.class))).thenReturn(List.of(q));

        MicroLessonBlueprint blueprint = agentWithRepo.createBlueprint(
                "Physics", "Mechanics", "Moment of Inertia", "JEE_MAIN"
        );

        assertThat(blueprint.sampleQuestions()).hasSize(1);
        assertThat(blueprint.formattedBlueprintPrompt()).contains("Calculate the moment of inertia of a disc.");
        assertThat(blueprint.formattedBlueprintPrompt()).contains("0.5 kg m^2");
    }

    @Test
    @DisplayName("Repository failure is handled gracefully without throwing exception")
    @SuppressWarnings("unchecked")
    void testRepositoryFailureHandledGracefully() {
        when(questionRepository.findAll(any(Specification.class))).thenThrow(new RuntimeException("DB Connection Timeout"));

        MicroLessonBlueprint blueprint = agentWithRepo.createBlueprint(
                "Chemistry", "Organic", "Carbon and its Compounds", "NEET_UG"
        );

        assertThat(blueprint).isNotNull();
        assertThat(blueprint.domainCategory()).isEqualTo(DomainCategory.CHEMISTRY_ORGANIC);
        assertThat(blueprint.sampleQuestions()).isEmpty();
    }

    @Test
    @DisplayName("Determines Part 3 formula cheat sheet requirement accurately across subjects and topics")
    void testFormulaSheetRequiredBySubjectAndTopic() {
        // Mandatory for Math, Science, Statistics, Data Interpretation, and Numerical topics
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Mathematics", "Algebra", "Quadratic Equations", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Science", "Physics", "Optics", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Science", "Chemistry", "Mole Concept", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Statistics", "Probability Theory", "Bayes Theorem", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Data Interpretation and Logical Analysis", "Pie Charts", "Degree Conversion", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Reasoning", "Numerical Operations", "Operator Substitution", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Reasoning", "Inequalities", "Direct Inequalities", "CONCEPT")).isTrue();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Studies", "Everyday Science", "Basic Physics", "CONCEPT")).isTrue();

        // Mandatory when lessonType is explicitly CHEAT_SHEET regardless of subject
        assertThat(agentWithoutRepo.isFormulaSheetRequired("English", "Grammar", "Voice Conversion", "CHEAT_SHEET")).isTrue();

        // Optional for English, Hindi, Polity, History, Public Admin, CDP, Banking, Verbal Reasoning
        assertThat(agentWithoutRepo.isFormulaSheetRequired("English", "Grammar", "Subject-Verb Agreement", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Studies", "Indian Polity", "Fundamental Rights", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Studies", "Indian History", "Modern India", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Public Administration and Governance", "Public Policy", "RTI Act", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("General Hindi", "मुहावरे एवं लोकोक्तियां", "प्रचलित मुहावरे", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Reasoning", "Critical Thinking", "Statement and Argument", "CONCEPT")).isFalse();
        assertThat(agentWithoutRepo.isFormulaSheetRequired("Child Development and Pedagogy", "Inclusive Education", "Diverse Learners", "CONCEPT")).isFalse();
    }
}
