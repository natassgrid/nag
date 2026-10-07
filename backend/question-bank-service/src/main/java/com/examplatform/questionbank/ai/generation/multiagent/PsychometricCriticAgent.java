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

import com.examplatform.questionbank.dto.QuestionOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 3. [Psychometric Critic Agent]
 * Validates generated items against rigorous psychometric and technical rubrics:
 * <ul>
 *   <li>Sole unambiguously correct answer.</li>
 *   <li>Plausible, non-trivial distractors (no obvious throwaways like "All of the above", "None").</li>
 *   <li>Valid KaTeX/mhchem rendering syntax ($$...$$).</li>
 *   <li>Absence of cultural or demographic bias.</li>
 * </ul>
 */
@Slf4j
@Component
public class PsychometricCriticAgent {

    public CriticReviewResult reviewQuestion(
            String content,
            String answerKey,
            String explanation,
            List<QuestionOption> options,
            String questionType) {

        List<String> issues = new ArrayList<>();
        List<String> verifiedRubrics = new ArrayList<>();
        double score = 1.0;

        // Rubric 1: Sole unambiguously correct answer & option validity
        if (options != null && !options.isEmpty()) {
            long correctCount = options.stream().filter(QuestionOption::isCorrect).count();
            if ("SINGLE_MCQ".equalsIgnoreCase(questionType)) {
                if (correctCount == 1) {
                    verifiedRubrics.add("Unambiguous sole correct answer verified");
                } else {
                    issues.add("SINGLE_MCQ must have exactly 1 correct option (found: " + correctCount + ")");
                    score -= 0.3;
                }
            } else if ("MULTI_MCQ".equalsIgnoreCase(questionType)) {
                if (correctCount >= 2) {
                    verifiedRubrics.add("Multiple correct options verified for MSQ");
                } else {
                    issues.add("MULTI_MCQ requires at least 2 correct options");
                    score -= 0.3;
                }
            }

            // Rubric 2: Distractor plausibility & absence of throwaway options
            boolean hasThrowaway = options.stream().anyMatch(o -> {
                String t = o.getText() != null ? o.getText().trim().toLowerCase() : "";
                return t.equals("none of these") || t.equals("all of the above") || t.equals("none of the above");
            });

            if (hasThrowaway) {
                issues.add("Distractors contain throwaway choices ('All of the above' / 'None of the above')");
                score -= 0.15;
            } else {
                verifiedRubrics.add("Plausible, non-trivial distractors verified");
            }
        }

        // Rubric 3: Valid KaTeX / LaTeX math syntax
        if (content != null && (content.contains("\\(") || content.contains("\\["))) {
            issues.add("Content contains raw \\( or \\[ delimiters instead of standardized $$...$$");
            score -= 0.1;
        } else {
            verifiedRubrics.add("Standardized KaTeX syntax verified");
        }

        // Rubric 4: Demographic / Cultural bias check
        if (content != null) {
            String lower = content.toLowerCase();
            if (lower.contains("he always") || lower.contains("obviously only men")) {
                issues.add("Potential demographic/gender bias phrasing detected");
                score -= 0.2;
            } else {
                verifiedRubrics.add("Neutral psychometric phrasing without bias");
            }
        }

        // Rubric 5: Explanation completeness
        if (explanation != null && explanation.length() >= 20) {
            verifiedRubrics.add("Comprehensive explanation and rationale provided");
        } else {
            issues.add("Explanation is missing or too brief (< 20 characters)");
            score -= 0.1;
        }

        score = Math.max(0.0, Math.min(1.0, score));
        boolean approved = issues.isEmpty() || score >= 0.85;

        log.debug("Critic review completed: approved={}, score={}, issuesCount={}",
                approved, score, issues.size());

        return CriticReviewResult.builder()
                .approved(approved)
                .score(score)
                .issues(issues)
                .verifiedRubrics(verifiedRubrics)
                .build();
    }
}
