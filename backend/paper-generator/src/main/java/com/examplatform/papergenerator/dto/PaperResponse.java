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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed DTO for single paper response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperResponse {

    private UUID id;
    private UUID examId;
    private String examName;
    private String shiftId;
    private String shiftName;
    private String status;
    private String paperDefinitionJson;
    private double difficultyScore;
    private String topicDistributionJson;
    private String encryptedPackageRef;
    private String encryptionKeyId;
    private UUID generatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    // Enriched paper summary fields
    private Integer totalQuestions;
    private Map<String, Integer> topicDistribution;
    private List<QuestionSummary> questions;
}
