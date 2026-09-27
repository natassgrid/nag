/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
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
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.domain.enums;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum CognitiveLevel {
    @JsonAlias({"KNOWLEDGE", "REMEMBERING", "RECALL", "MEMORY"})
    REMEMBER,

    @JsonAlias({"COMPREHENSION", "UNDERSTANDING", "CONCEPTUAL"})
    UNDERSTAND,

    @JsonAlias({"APPLICATION", "APPLYING"})
    APPLY,

    @JsonAlias({"ANALYSIS", "ANALYZING"})
    ANALYZE,

    @JsonAlias({"EVALUATION", "EVALUATING"})
    EVALUATE,

    @JsonAlias({"SYNTHESIS", "CREATING", "CREATION"})
    CREATE;

    @JsonCreator
    public static CognitiveLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "REMEMBER", "KNOWLEDGE", "REMEMBERING", "RECALL", "MEMORY" -> REMEMBER;
            case "UNDERSTAND", "COMPREHENSION", "UNDERSTANDING", "CONCEPTUAL" -> UNDERSTAND;
            case "APPLY", "APPLICATION", "APPLYING" -> APPLY;
            case "ANALYZE", "ANALYSIS", "ANALYZING" -> ANALYZE;
            case "EVALUATE", "EVALUATION", "EVALUATING" -> EVALUATE;
            case "CREATE", "SYNTHESIS", "CREATING", "CREATION" -> CREATE;
            default -> {
                for (CognitiveLevel level : values()) {
                    if (level.name().equalsIgnoreCase(normalized)) {
                        yield level;
                    }
                }
                throw new IllegalArgumentException("Unknown cognitive level: " + value);
            }
        };
    }
}
