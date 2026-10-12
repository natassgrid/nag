// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.service;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

    private MicroLessonPromptBuilder promptBuilder;
    private KaTexValidator kaTexValidator;
    private ObjectMapper objectMapper;
    private MicroLessonGeneratorService service;

    @BeforeEach
    void setUp() {
        promptBuilder = new MicroLessonPromptBuilder();
        kaTexValidator = new KaTexValidator();
        objectMapper = new ObjectMapper();
        service = new MicroLessonGeneratorService(chatClient, promptBuilder, kaTexValidator, repository, objectMapper);
    }

    @Test
    @DisplayName("Generates, validates and persists micro-lesson from structured AI chat response")
    void testSuccessfulGeneration() throws Exception {
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

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(jsonPayload);

        when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(100L, "JEE_MAIN"))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                1L, "Physics",
                10L, "Mechanics",
                100L, "Moment of Inertia",
                "JEE_MAIN"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSubtopic()).isEqualTo("Moment of Inertia");
        assertThat(result.getTargetExam()).isEqualTo("JEE_MAIN");
        assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        assertThat(result.getContentVersion()).isEqualTo(1);
        assertThat(result.getCoreConcept()).contains("atta chakki");

        MicroLessonResponse response = service.toResponse(result);
        assertThat(response.cheatSheet()).hasSize(2);
        assertThat(response.stepByStepBreakdown().steps()).hasSize(2);
    }

    @Test
    @DisplayName("Falls back to high-yield pedagogical template when ChatClient fails")
    void testFallbackOnChatFailure() {
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
}
