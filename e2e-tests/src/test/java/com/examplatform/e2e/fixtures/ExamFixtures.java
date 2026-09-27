/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) — Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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
package com.examplatform.e2e.fixtures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class providing reusable examination-related request payloads for E2E tests.
 *
 * <p>All methods return {@code Map<String, Object>} instances that REST Assured
 * serialises to JSON automatically when the Content-Type is {@code application/json}.
 */
public final class ExamFixtures {

    /**
     * Stable UUID for the E2E reference examination.
     * Follows the NAG seed-data UUID pattern: {@code e2e0exam-NNNN-0000-0000-000000000001}.
     */
    public static final String E2E_EXAM_UUID = "e2e0exam-0001-0000-0000-000000000001";

    /**
     * Stable UUID for the E2E reference candidate.
     * Follows the NAG seed-data UUID pattern: {@code e2e0cand-NNNN-0000-0000-000000000001}.
     */
    public static final String E2E_CANDIDATE_UUID = "e2e0cand-0001-0000-0000-000000000001";

    private ExamFixtures() {
        // factory class — no instances
    }

    /**
     * Builds an examination creation request payload.
     *
     * @param title display title for the examination
     * @return map representing the exam creation request body
     */
    public static Map<String, Object> examPayload(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("durationMinutes", 120);
        body.put("totalMarks", 100);
        body.put("passingMarks", 40);
        body.put("examType", "OBJECTIVE");
        return body;
    }

    /**
     * Builds a question creation request payload for a single-choice MCQ.
     *
     * <p>The generated question has four options (A–D); option A is always
     * marked as the correct answer.
     *
     * @param index numeric index used to make the question content unique
     * @return map representing the question creation request body
     */
    public static Map<String, Object> questionPayload(int index) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", "E2E Question #" + index + ": What is the correct answer?");
        body.put("type", "SINGLE_MCQ");
        body.put("difficulty", "MEDIUM");
        body.put("marks", 2);

        List<Map<String, Object>> options = new ArrayList<>();
        String[] labels = {"A", "B", "C", "D"};
        for (String label : labels) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", label);
            option.put("content", "Option " + label + " for question #" + index);
            // Only option A is the correct answer
            option.put("isCorrect", "A".equals(label));
            options.add(option);
        }
        body.put("options", options);
        return body;
    }
}
