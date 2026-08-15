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

package com.examplatform.papergenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO representing a question retrieved from the Question Bank.
 * Used during paper assembly to evaluate reuse policies and compute
 * difficulty scores.
 *
 * Validates: Requirements 8.3, 8.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSummary {

    private UUID questionId;

    private String subject;

    private String topic;

    /**
     * Difficulty level: EASY, MEDIUM, or HARD.
     */
    private String difficulty;

    private String cognitiveLevel;

    private int usageCount;

    private Instant lastUsedAt;

    /**
     * Reuse policy: NEVER, 1_YEAR, 2_YEARS, or CUSTOM.
     */
    private String reusePolicy;
}
