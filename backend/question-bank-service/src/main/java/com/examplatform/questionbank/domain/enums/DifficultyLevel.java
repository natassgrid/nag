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

public enum DifficultyLevel {
    @JsonAlias({"BEGINNER", "SIMPLE"})
    EASY,

    @JsonAlias({"INTERMEDIATE", "MODERATE", "NORMAL"})
    MEDIUM,

    @JsonAlias({"ADVANCED", "EXPERT", "DIFFICULT"})
    HARD;

    @JsonCreator
    public static DifficultyLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "EASY", "BEGINNER", "SIMPLE" -> EASY;
            case "MEDIUM", "INTERMEDIATE", "MODERATE", "NORMAL" -> MEDIUM;
            case "HARD", "ADVANCED", "EXPERT", "DIFFICULT" -> HARD;
            default -> {
                for (DifficultyLevel level : values()) {
                    if (level.name().equalsIgnoreCase(normalized)) {
                        yield level;
                    }
                }
                throw new IllegalArgumentException("Unknown difficulty level: " + value);
            }
        };
    }
}
