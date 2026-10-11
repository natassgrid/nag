/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.shared.constants;

/**
 * Standard string constants for question types across examination platforms.
 */
public final class QuestionTypeConstants {

    private QuestionTypeConstants() {
        // Utility / Constants class
    }

    public static final String SINGLE_MCQ = "SINGLE_MCQ";
    public static final String MULTI_MCQ = "MULTI_MCQ";
    public static final String NUMERICAL = "NUMERICAL";
    public static final String DESCRIPTIVE = "DESCRIPTIVE";
    public static final String ASSERTION_REASON = "ASSERTION_REASON";
    public static final String PARAGRAPH_SET = "PARAGRAPH_SET";
    public static final String MATRIX_MATCH = "MATRIX_MATCH";
    public static final String CODING = "CODING";
    public static final String CASE_STUDY = "CASE_STUDY";
}
