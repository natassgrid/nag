// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.ai;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonContentDto;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import com.examplatform.shared.constants.SharedConstants;
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

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(100L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                1L, "Physics",
                10L, "Mechanics",
                100L, "Moment of Inertia",
                "JEE_ADVANCED"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSubtopicId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        assertThat(result.getContentVersion()).isEqualTo(1);
        assertThat(result.getCoreConcept()).contains("atta chakki");

        verify(modelRouter).selectModel(QuestionTaxonomyConstants.Difficulty.HARD);
        verify(requestSpec).options(any());

        MicroLessonResponse response = service.toResponse(result);
        assertThat(response.cheatSheet()).hasSize(2);
        assertThat(response.stepByStepBreakdown().steps()).hasSize(2);
        assertThat(response.subtopicId()).isEqualTo(100L);
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

        assertThat(service.resolveDifficulty("Chemistry", "Physical", "Thermodynamics", SharedConstants.JEE_MAIN))
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
    @DisplayName("Falls back to domain-specific DeterministicMicroLessonAgent template when ChatClient fails")
    void testFallbackOnChatFailure() {
        when(modelRouter.selectModel(anyString())).thenReturn("nova-lite");
        when(chatClient.prompt()).thenThrow(new RuntimeException("LLM network timeout"));

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(200L))
                .thenReturn(Optional.of(SubtopicMicroLesson.builder().contentVersion(1).build()));
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                2L, "Chemistry",
                20L, "Physical",
                200L, "Chemical Equilibrium",
                "NEET_UG"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSubtopicId()).isEqualTo(200L);
        assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        // Incremented version
        assertThat(result.getContentVersion()).isEqualTo(2);
        // Domain-specific chemistry content (mole concept / equilibrium), NOT generic bicycle chain rotational mechanics!
        assertThat(result.getCoreConcept()).doesNotContain("bicycle chain");
        assertThat(result.getCheatSheetJson()).contains("\\Delta G");
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

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(100L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = legacyService.generateForSubtopic(
                1L, "Physics", 10L, "Mechanics", 100L, "Kinematics", SharedConstants.JEE_MAIN
        );

        assertThat(result).isNotNull();
        // options is not called when modelRouter is null
        verify(requestSpec, never()).options(any());
    }

    @Test
    @DisplayName("Successfully parses raw LLM JSON with unescaped LaTeX backslashes without triggering fallback")
    void testParseRawLatexWithUnescapedBackslashes() {
        String rawLlmJson = """
            {
              "coreConcept": "Percentage calculation involves fraction multiplication by 100.",
              "trap": "Forgetting that rate \\(r\\) is divided by 100.",
              "cheatSheet": ["$$P = \\frac{part}{total} \\times 100\\%$$"],
              "stepByStepBreakdown": {
                "questionStem": "What is 20% of 80?",
                "steps": ["Step 1: Write equation \\(x = 0.20 \\times 80\\)", "Step 2: Solve x = 16"],
                "finalAnswer": "16"
              }
            }
            """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(rawLlmJson);

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(176L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                8L, "Mathematics", 80L, "Arithmetic", 176L, "Percentage", SharedConstants.JEE_MAIN
        );

        assertThat(result).isNotNull();
        assertThat(result.getCoreConcept()).contains("Percentage calculation involves fraction");
        assertThat(result.getCoreConcept()).doesNotContain("bicycle chain");
        // Verify \(...\) was deterministically refined to $$...$$
        assertThat(result.getTrap()).contains("$$r$$");
    }

    @Test
    @DisplayName("Recovers deterministically when qwen2.5:1.5b truncates closing JSON braces at line 14 for 'Carbon and its Compounds' and 'Organic'")
    void testTruncatedJsonAtLine14Recovery() {
        // Exact reproduction of the production log error:
        // Unexpected end-of-input: expected close marker for Object (start marker at line 1, col 1) at [line: 14, column: 4]
        String truncatedLlmOutput = "{\n"
                + "  \"coreConcept\": \"Carbon exhibits catenation and tetravalency ($$Z = 6$$). Like interlocking links of a gold chain, carbon forms long chains and rings. This creates millions of organic compounds.\",\n"
                + "  \"trap\": \"Confusing saturated alkanes ($$\\ce{C_nH_{2n+2}}$$) with unsaturated alkenes ($$\\ce{C_nH_{2n}}$$).\",\n"
                + "  \"cheatSheet\": [\n"
                + "    \"$$\\ce{C_nH_{2n+2}}$$\",\n"
                + "    \"$$\\ce{CH4 + 2O2 -> CO2 + 2H2O}$$\"\n"
                + "  ],\n"
                + "  \"stepByStepBreakdown\": {\n"
                + "    \"questionStem\": \"Find the molecular mass of butane ($$\\ce{C4H10}$$).\",\n"
                + "    \"steps\": [\n"
                + "      \"Step 1: Multiply 4 carbon atoms by 12 u: $$4 \\times 12 = 48\\text{ u}$$.\",\n"
                + "      \"Step 2: Add 10 hydrogen atoms: $$48 + 10 = 58\\text{ u}$$.\"\n"
                + "    ],\n"
                + "    \"finalAnswer\": \"$$58\\text{ u}$$\"\n"
                + "  }"; // <-- Missing root closing '}' at line 14, col 4!

        when(modelRouter.selectModel(anyString())).thenReturn("qwen2.5:1.5b");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(truncatedLlmOutput);

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(301L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                11L, "General Science", 110L, "Chemistry", 301L, "Carbon and its Compounds", "IBPS-PO-PRE-2027"
        );

        assertThat(result).isNotNull();
        assertThat(result.getCoreConcept()).contains("Carbon exhibits catenation");
        assertThat(result.getCheatSheetJson()).contains("\\\\ce{C_nH_{2n+2}}");
        assertThat(result.getStepByStepBreakdownJson()).contains("\\\\frac".substring(0, 2)); // escaped properly
        assertThat(result.getStepByStepBreakdownJson()).contains("58\\\\text{ u}");
    }

    @Test
    @DisplayName("Strips leaked physics schema formula and replaces with domain-specific Percentage formula when Rubric 3 fires")
    void testRubric3LeakedPhysicsFormulaReplacedWithDomainCheatSheet() {
        String llmOutputWithLeakedPhysicsFormula = """
            {
              "coreConcept": "Percentage calculation involves fraction multiplication by 100.",
              "trap": "Forgetting that rate \\(r\\) is divided by 100.",
              "cheatSheet": ["$$P = \\\\frac{part}{total} \\\\times 100\\\\%$$", "$$\\\\Delta K + \\\\Delta U = W_{ext}$$"],
              "stepByStepBreakdown": {
                "questionStem": "What is 20% of 80?",
                "steps": ["Step 1: Write equation \\(x = 0.20 \\\\times 80\\)", "Step 2: Solve x = 16"],
                "finalAnswer": "16"
              }
            }
            """;

        when(modelRouter.selectModel(anyString())).thenReturn("qwen2.5:1.5b");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmOutputWithLeakedPhysicsFormula);

        when(repository.findTopBySubtopicIdOrderByContentVersionDesc(176L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtopicMicroLesson result = service.generateForSubtopic(
                8L, "Quantitative Aptitude", 80L, "Arithmetic", 176L, "Percentage", "IBPS-PO-PRE-2027"
        );

        assertThat(result).isNotNull();
        // Leaked physics formula \Delta K + \Delta U = W_{ext} was stripped and replaced with domain Percentage formula
        assertThat(result.getCheatSheetJson()).doesNotContain("W_{ext}");
        assertThat(result.getCheatSheetJson()).contains("Net Change");
        // Rubric 2 converted \(r\) to $$r$$
        assertThat(result.getTrap()).contains("$$r$$");
    }
}
