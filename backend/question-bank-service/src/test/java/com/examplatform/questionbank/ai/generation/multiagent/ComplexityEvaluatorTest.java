/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation.multiagent;

import com.examplatform.questionbank.ai.generation.ExecutionMode;
import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplexityEvaluator Conditional Triage Tests")
class ComplexityEvaluatorTest {

    @Mock
    private ModelRouter modelRouter;

    private ComplexityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ComplexityEvaluator(modelRouter);
    }

    private QuestionGenerationRequest createBaseRequest() {
        return QuestionGenerationRequest.builder()
                .subject("Physics")
                .topic("Kinematics")
                .difficulty("EASY")
                .cognitiveLevel("REMEMBER")
                .questionType("SINGLE_MCQ")
                .executionMode(ExecutionMode.AUTO)
                .build();
    }

    @Test
    @DisplayName("Default routine MCQ routes to Single-Model Fast Path (<2s, minimal token cost)")
    void testFastPathDefault() {
        when(modelRouter.selectModel("EASY")).thenReturn("nova-micro");

        QuestionGenerationRequest req = createBaseRequest();
        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isFalse();
        assertThat(result.getTargetModel()).isEqualTo("nova-micro");
        assertThat(result.getRationale()).contains("Single-Model Fast Path");
    }

    @Test
    @DisplayName("Trigger 1: High cognitive level (ANALYZE / EVALUATE / CREATE) escalates to Multi-Agent")
    void testTrigger1HighCognitiveLevel() {
        when(modelRouter.selectModel("HARD")).thenReturn("nova-lite");

        QuestionGenerationRequest req = createBaseRequest();
        req.setCognitiveLevel("ANALYZE");

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isTrue();
        assertThat(result.getTriggeredFactors()).contains("HIGH_COGNITIVE_LEVEL_ANALYZE");
    }

    @Test
    @DisplayName("Trigger 2: Complex question types (PARAGRAPH_SET, ASSERTION_REASON, MATRIX_MATCH) escalate")
    void testTrigger2ComplexQuestionTypes() {
        when(modelRouter.selectModel("HARD")).thenReturn("nova-lite");

        QuestionGenerationRequest req = createBaseRequest();
        req.setQuestionType("ASSERTION_REASON");

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isTrue();
        assertThat(result.getTriggeredFactors()).contains("COMPLEX_QUESTION_TYPE_ASSERTION_REASON");
    }

    @Test
    @DisplayName("Trigger 3: Chemistry/math notation or diagram in sample escalates to Multi-Agent")
    void testTrigger3SampleFeaturesEscalate() {
        when(modelRouter.selectModel("HARD")).thenReturn("nova-lite");

        QuestionGenerationRequest req = createBaseRequest();
        NormalizedSampleQuestion sample = NormalizedSampleQuestion.builder()
                .stem("Calculate cell potential")
                .hasMathOrChemistry(true)
                .hasDiagram(true)
                .build();

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of(sample));

        assertThat(result.isUseMultiAgent()).isTrue();
        assertThat(result.getTriggeredFactors()).contains("SAMPLE_MATH_OR_CHEMISTRY_FORMULAS");
        assertThat(result.getTriggeredFactors()).contains("SAMPLE_EMBEDDED_DIAGRAM_REASONING");
    }

    @Test
    @DisplayName("Trigger 4: Explicit flag (requireCriticReview or generationQuality: EXAM_READY) escalates")
    void testTrigger4ExplicitFlags() {
        when(modelRouter.selectModel("HARD")).thenReturn("nova-lite");

        QuestionGenerationRequest req = createBaseRequest();
        req.setRequireCriticReview(true);
        req.setGenerationQuality("EXAM_READY");

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isTrue();
        assertThat(result.getTriggeredFactors()).contains("REQUIRE_CRITIC_REVIEW_FLAG");
        assertThat(result.getTriggeredFactors()).contains("GENERATION_QUALITY_EXAM_READY");
    }

    @Test
    @DisplayName("Explicit MULTI_AGENT execution mode forces Multi-Agent committee")
    void testExplicitMultiAgentExecutionMode() {
        when(modelRouter.selectModel("HARD")).thenReturn("nova-lite");

        QuestionGenerationRequest req = createBaseRequest();
        req.setExecutionMode(ExecutionMode.MULTI_AGENT);

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isTrue();
        assertThat(result.getTriggeredFactors()).contains("EXPLICIT_EXECUTION_MODE_MULTI_AGENT");
    }

    @Test
    @DisplayName("Explicit FAST execution mode forces Fast Path when no critic is requested")
    void testExplicitFastExecutionMode() {
        when(modelRouter.selectModel("EASY")).thenReturn("nova-micro");

        QuestionGenerationRequest req = createBaseRequest();
        req.setCognitiveLevel("ANALYZE"); // normally triggers multi-agent
        req.setExecutionMode(ExecutionMode.FAST);

        ComplexityEvaluationResult result = evaluator.evaluate(req, List.of());

        assertThat(result.isUseMultiAgent()).isFalse();
        assertThat(result.getTargetModel()).isEqualTo("nova-micro");
    }
}
