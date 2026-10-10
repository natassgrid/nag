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

import com.examplatform.questionbank.ai.generation.ClarifyRequirementsRequest;
import com.examplatform.questionbank.ai.generation.ClarifyRequirementsResponse;
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.generation.QuestionPromptFormatter;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. [Requirement Analyst Agent]
 * Ingests author parameters, sample questions, and syllabus hierarchy to formulate
 * a precise generation blueprint. Also handles conversational elicitation when author
 * prompts are ambiguous.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementAnalystAgent {

    /**
     * Synthesizes a generation blueprint for authoring.
     */
    public String formulateBlueprint(
            QuestionGenerationRequest request,
            List<NormalizedSampleQuestion> sampleQuestions) {

        StringBuilder blueprint = new StringBuilder();
        blueprint.append("=== GENERATION BLUEPRINT ===\n");
        QuestionPromptFormatter.appendGenerationParameters(
                blueprint, request, "", "Author Question Description/Prompt", "Target Exam Standard");

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            blueprint.append("Sample Guidance:\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                NormalizedSampleQuestion sq = sampleQuestions.get(i);
                blueprint.append("  [Sample ").append(i + 1).append("]: ")
                        .append(sq.getStem() != null ? sq.getStem() : "Visual Diagram")
                        .append(" (Tier=").append(sq.getParsingTier()).append(")\n");
                if (sq.isHasMathOrChemistry()) {
                    blueprint.append("    -> Preserve LaTeX math ($$...$$) and chemistry (\\ce{...}) rigor.\n");
                }
                if (sq.isHasDiagram()) {
                    blueprint.append("    -> Model is diagram-dependent; preserve visual stem clarity.\n");
                }
            }
        }

        blueprint.append("Pedagogical Directives:\n");
        blueprint.append("- Ensure distractor plausibility with no trivial throwaways.\n");
        blueprint.append("- Enforce unambiguous correct answer and self-contained stem.\n");

        return blueprint.toString();
    }

    /**
     * Rapid requirement elicitation when prompt is broad/ambiguous.
     */
    public ClarifyRequirementsResponse clarifyRequirements(ClarifyRequirementsRequest request) {
        List<String> questions = new ArrayList<>();
        List<String> suggestedSubtopics = new ArrayList<>();
        List<String> suggestedFormats = List.of("SINGLE_MCQ", "MULTI_MCQ", "NUMERICAL", "ASSERTION_REASON");

        boolean ambiguous = false;

        if (request.getAuthorPrompt() != null && (request.getAuthorPrompt().length() < 15 || request.getAuthorPrompt().contains("tough") || request.getAuthorPrompt().contains("like sample"))) {
            ambiguous = true;
            questions.add("What specific standard should this question align with (e.g. JEE Advanced, UPSC Prelims, or CBSE Board)?");
            questions.add("Should the distractors test conceptual misconceptions, calculation traps, or formula misapplication?");
        }

        if (request.getTopic() == null || request.getTopic().isBlank()) {
            ambiguous = true;
            questions.add("Which specific topic or core syllabus area would you like to focus on?");
        }

        String targetExam = request.getTargetExam() != null ? request.getTargetExam() : "GENERAL_COMPETITIVE";
        if ("Chemistry".equalsIgnoreCase(request.getSubject())) {
            suggestedSubtopics.addAll(List.of("Nernst Equation & Cell EMF", "Faraday's Laws of Electrolysis", "Kohlrausch's Law"));
        } else if ("Mathematics".equalsIgnoreCase(request.getSubject())) {
            suggestedSubtopics.addAll(List.of("Quadratic Equations & Roots", "Calculus & Derivatives", "Coordinate Geometry"));
        } else {
            suggestedSubtopics.addAll(List.of("Core Principles", "Analytical Application", "Multi-Concept Integration"));
        }

        String blueprint = String.format("Target standard: %s | Subject: %s | Topic: %s",
                targetExam, request.getSubject(), request.getTopic() != null ? request.getTopic() : "General");

        return ClarifyRequirementsResponse.builder()
                .clarificationNeeded(ambiguous)
                .clarificationQuestions(questions)
                .suggestedSubtopics(suggestedSubtopics)
                .suggestedFormats(suggestedFormats)
                .recommendedBlueprint(blueprint)
                .resolvedTargetExam(targetExam)
                .build();
    }
}
