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

package com.examplatform.result.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * DTO representing per-question analytics for an exam.
 * Includes difficulty index, discrimination index, and response distribution.
 *
 * Validates: Requirements 26.1, 26.5
 */
@Data
@Builder
public class QuestionAnalyticsResult {

    /**
     * The question identifier.
     */
    private UUID questionId;

    /**
     * Difficulty index: proportion of candidates who answered correctly.
     * Range: 0.0 (hardest) to 1.0 (easiest).
     */
    private double difficultyIndex;

    /**
     * Discrimination index: difference between top 27% and bottom 27% correct rates.
     * Range: -1.0 to 1.0. Higher values indicate better discrimination.
     */
    private double discriminationIndex;

    /**
     * Response distribution: count per option selected.
     * Key is the option identifier (e.g., "A", "B", "C", "D"),
     * value is the number of candidates who selected that option.
     */
    private Map<String, Integer> responseDistribution;
}
