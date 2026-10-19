/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request payload to promote verified extracted questions into the canonical question bank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoteQuestionsRequest {

    /**
     * Optional list of specific extracted question IDs to promote.
     * If null or empty, all APPROVED questions for the paper will be promoted.
     */
    private List<UUID> extractedQuestionIds;

    private Long fallbackSubjectId;
    private Long fallbackTopicId;
    private Long fallbackSubtopicId;

    @Builder.Default
    private String targetQuestionState = "APPROVED";

    @Builder.Default
    private boolean createTranslations = true;
}
