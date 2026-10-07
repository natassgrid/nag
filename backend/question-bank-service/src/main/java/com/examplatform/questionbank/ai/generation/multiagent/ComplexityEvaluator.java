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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Intelligent complexity triage router implementing "Agent Only When Needed".
 *
 * <p>Escalates to a specialized multi-agent workflow only when complexity,
 * high-stakes examination requirements, or advanced notation necessitate it.
 * Otherwise defaults to Single-Model Fast Path (90% of requests) to bound token
 * expenditure and maintain sub-2-second generation latency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplexityEvaluator {

    private static final Set<String> HIGH_COGNITIVE_LEVELS = Set.of(
            "ANALYZE", "EVALUATE", "CREATE");

    private static final Set<String> COMPLEX_QUESTION_TYPES = Set.of(
            "PARAGRAPH_SET", "ASSERTION_REASON", "MATRIX_MATCH");

    private final ModelRouter modelRouter;

    /**
     * Evaluates generation parameters and parsed sample questions to determine execution path.
     */
    public ComplexityEvaluationResult evaluate(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions) {

        List<String> triggers = new ArrayList<>();

        // Explicit user execution mode overrides
        if (request.getExecutionMode() == ExecutionMode.MULTI_AGENT) {
            triggers.add("EXPLICIT_EXECUTION_MODE_MULTI_AGENT");
        }

        // Trigger 4: Explicit flags
        if (Boolean.TRUE.equals(request.getRequireCriticReview())) {
            triggers.add("REQUIRE_CRITIC_REVIEW_FLAG");
        }
        if ("EXAM_READY".equalsIgnoreCase(request.getGenerationQuality())) {
            triggers.add("GENERATION_QUALITY_EXAM_READY");
        }

        // Trigger 1: High cognitive levels
        if (request.getCognitiveLevel() != null &&
                HIGH_COGNITIVE_LEVELS.contains(request.getCognitiveLevel().toUpperCase().trim())) {
            triggers.add("HIGH_COGNITIVE_LEVEL_" + request.getCognitiveLevel().toUpperCase());
        }

        // Trigger 2: Complex question types
        if (request.getQuestionType() != null) {
            String qType = request.getQuestionType().toUpperCase().trim();
            if (COMPLEX_QUESTION_TYPES.contains(qType)) {
                triggers.add("COMPLEX_QUESTION_TYPE_" + qType);
            } else if ("NUMERICAL".equals(qType) && "HARD".equalsIgnoreCase(request.getDifficulty())) {
                triggers.add("MULTI_STEP_NUMERICAL");
            }
        }

        // High-stakes target exams (e.g. JEE_ADV, UPSC_CSE, GATE)
        if (request.getTargetExam() != null) {
            String exam = request.getTargetExam().toUpperCase().trim();
            if (exam.contains("JEE_ADV") || exam.contains("UPSC") || exam.contains("GATE")) {
                triggers.add("HIGH_STAKES_TARGET_EXAM_" + exam);
            }
        }

        // Trigger 3: Chemistry, advanced math, or diagrams detected in sample questions
        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            boolean hasChemOrMath = sampleQuestions.stream().anyMatch(NormalizedSampleQuestion::isHasMathOrChemistry);
            boolean hasDiagram = sampleQuestions.stream().anyMatch(NormalizedSampleQuestion::isHasDiagram);

            if (hasChemOrMath) {
                triggers.add("SAMPLE_MATH_OR_CHEMISTRY_FORMULAS");
            }
            if (hasDiagram) {
                triggers.add("SAMPLE_EMBEDDED_DIAGRAM_REASONING");
            }
        }

        // Forced FAST mode if user explicitly selected FAST and no critic was forced
        if (request.getExecutionMode() == ExecutionMode.FAST && !Boolean.TRUE.equals(request.getRequireCriticReview())) {
            return ComplexityEvaluationResult.builder()
                    .useMultiAgent(false)
                    .rationale("Single-Model Fast Path selected via explicit FAST execution mode override")
                    .triggeredFactors(List.of("EXPLICIT_FAST_MODE"))
                    .targetModel(modelRouter.selectModel(request.getDifficulty()))
                    .build();
        }

        boolean shouldEscalate = !triggers.isEmpty();

        String rationale = shouldEscalate
                ? "Escalated to Multi-Agent collaborative pipeline due to: " + String.join(", ", triggers)
                : "Single-Model Fast Path selected for routine question generation (" +
                  request.getQuestionType() + " / " + request.getCognitiveLevel() + ")";

        String selectedModel = shouldEscalate
                ? modelRouter.selectModel("HARD")
                : modelRouter.selectModel(request.getDifficulty());

        log.info("Complexity triage completed: useMultiAgent={}, triggersCount={}, model={}",
                shouldEscalate, triggers.size(), selectedModel);

        return ComplexityEvaluationResult.builder()
                .useMultiAgent(shouldEscalate)
                .rationale(rationale)
                .triggeredFactors(triggers)
                .targetModel(selectedModel)
                .build();
    }
}
