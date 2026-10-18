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
 * Standard string constants for question evaluation and taxonomy.
 */
public final class QuestionTaxonomyConstants {

    private QuestionTaxonomyConstants() {
        // Utility / Constants class
    }

    public static final class Difficulty {
        private Difficulty() {}

        public static final String EASY = "EASY";
        public static final String MEDIUM = "MEDIUM";
        public static final String HARD = "HARD";
    }

    public static final class CognitiveLevel {
        private CognitiveLevel() {}

        public static final String REMEMBER = "REMEMBER";
        public static final String UNDERSTAND = "UNDERSTAND";
        public static final String APPLY = "APPLY";
        public static final String ANALYZE = "ANALYZE";
        public static final String EVALUATE = "EVALUATE";
        public static final String CREATE = "CREATE";
    }
}
