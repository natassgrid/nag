// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MicroLessonGeneratorService AI Generation & Parsing Tests")
class MicroLessonGeneratorServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private SubtopicMicroLessonRepository repository;

    @Mock
    private ModelRouter modelRouter;

    private MicroLessonPromptBuilder promptBuilder;
    private KaTexValidator kaTexValidator;
    private ObjectMapper objectMapper;
    private MicroLessonGeneratorService service;

    @BeforeEach
    void setUp() {
        promptBuilder = new MicroLessonPromptBuilder();
        kaTexValidator = new KaTexValidator();
        objectMapper = new ObjectMapper();
        service = new MicroLessonGeneratorService(chatClient, promptBuilder, kaTexValidator, repository, objectMapper, modelRouter);
    }

    @Test
    @DisplayName("Generates, validates and persists micro-lesson with ModelRouter complexity routing")
    void testSuccessfulGenerationWithModelRouter() throws Exception {
        MicroLessonContentDto mockContent = new MicroLessonContentDto(
                "Moment of Inertia measures rotational sluggishness. Imagine turning a heavy stone atta chakki vs a lightweight plastic disk. It depends strictly on both mass and distance from the rotation axis.",
                "Students often use point mass formulas for distributed continuous bodies without integrating.",
                List.of("$I = m r^2$", "$$I_{disk} = \\frac{1}{2} M R^2$$"),
                new StepByStepBreakdownDto(
                        "Calculate moment of inertia of a uniform solid cylinder of mass 2kg and radius 0.5m.",
                        List.of("Step 1: Formula is $I = \\frac{1}{2} M R^2$", "Step 2: Substitute $M=2, R=0.5$"),
                        "0.25 \\text{ kg}\\cdot\\text{m}^2"
                )
        );
        String jsonPayload = objectMapper.writeValueAsString(mockContent);

        when(modelRouter.selectModel(QuestionTaxonomyConstants.Difficulty.HARD)).thenReturn("nova-lite");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(jsonPayload);

        when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_ADVANCED"))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                1L, "Physics",
                10L, "Mechanics",
                100L, "Moment of Inertia",
                "JEE_ADVANCED"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSubtopic()).isEqualTo("Moment of Inertia");
        assertThat(result.getTargetExam()).isEqualTo("JEE_ADVANCED");
        assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        assertThat(result.getContentVersion()).isEqualTo(1);
        assertThat(result.getCoreConcept()).contains("atta chakki");

        verify(modelRouter).selectModel(QuestionTaxonomyConstants.Difficulty.HARD);
        verify(requestSpec).options(any());

        MicroLessonResponse response = service.toResponse(result);
        assertThat(response.cheatSheet()).hasSize(2);
        assertThat(response.stepByStepBreakdown().steps()).hasSize(2);
    }

    @Test
    @DisplayName("Difficulty resolver correctly maps exam and topic complexity")
    void testDifficultyResolution() {
        assertThat(service.resolveDifficulty("Physics", "Mechanics", "Rotational Dynamics", "JEE_ADVANCED"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.HARD);
        assertThat(service.resolveDifficulty("Civil Services", "General Studies", "Polity", "UPSC_CSE"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.HARD);
        assertThat(service.resolveDifficulty("Engineering", "Computer Science", "Algorithms", "GATE_CS"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.HARD);

        assertThat(service.resolveDifficulty("Chemistry", "Physical", "Thermodynamics", "JEE_MAIN"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.MEDIUM);
        assertThat(service.resolveDifficulty("Biology", "Zoology", "Human Physiology", "NEET_UG"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.MEDIUM);
        assertThat(service.resolveDifficulty("Quantitative Aptitude", "Arithmetic", "Percentages", "SSC_CGL"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.MEDIUM);

        assertThat(service.resolveDifficulty("Physics", "Quantum Physics", "Wave Particle Duality", "STATE_CET"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.HARD);
        assertThat(service.resolveDifficulty("Civics", "Basics", "Preamble Introduction", "FOUNDATION"))
                .isEqualTo(QuestionTaxonomyConstants.Difficulty.EASY);
    }

    @Test
    @DisplayName("Falls back to high-yield pedagogical template when ChatClient fails")
    void testFallbackOnChatFailure() {
        when(modelRouter.selectModel(anyString())).thenReturn("nova-lite");
        when(chatClient.prompt()).thenThrow(new RuntimeException("LLM network timeout"));

        when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(200L, "NEET_UG"))
                .thenReturn(Optional.of(SubtopicMicroLesson.builder().contentVersion(1).build()));
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                2L, "Chemistry",
                20L, "Physical",
                200L, "Chemical Equilibrium",
                "NEET_UG"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSubtopic()).isEqualTo("Chemical Equilibrium");
        assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        // Incremented version
        assertThat(result.getContentVersion()).isEqualTo(2);
        assertThat(result.getCoreConcept()).contains("Chemical Equilibrium");
    }

    @Test
    @DisplayName("Backward compatibility preserved when ModelRouter is not provided")
    void testBackwardCompatibilityWithoutModelRouter() throws Exception {
        MicroLessonGeneratorService legacyService = new MicroLessonGeneratorService(
                chatClient, promptBuilder, kaTexValidator, repository, objectMapper
        );

        MicroLessonContentDto mockContent = new MicroLessonContentDto(
                "Core concept text",
                "Trap text",
                List.of("$formula$"),
                new StepByStepBreakdownDto("Problem statement", List.of("Step 1"), "Answer")
        );
        String jsonPayload = objectMapper.writeValueAsString(mockContent);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(jsonPayload);

        when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_MAIN"))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = legacyService.generateForSubtopic(
                1L, "Physics", 10L, "Mechanics", 100L, "Kinematics", "JEE_MAIN"
        );

        assertThat(result).isNotNull();
        // options is not called when modelRouter is null
        verify(requestSpec, never()).options(any());
    }
}
