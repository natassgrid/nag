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

/**
 * A single rule within a paper generation blueprint.
 * Specifies the subject, topic, difficulty, cognitive level, and
 * the number of questions to select matching these criteria.
 *
 * Validates: Requirements 8.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintRule {

    private String subject;

    private String topic;

    /**
     * Difficulty filter: EASY, MEDIUM, HARD, or null for any difficulty.
     */
    private String difficulty;

    /**
     * Cognitive level filter, or null for any cognitive level.
     */
    private String cognitiveLevel;

    private int questionCount;
}
