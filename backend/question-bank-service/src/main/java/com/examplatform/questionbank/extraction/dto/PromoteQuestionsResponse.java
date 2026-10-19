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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response payload summarizing the outcome of a question promotion batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoteQuestionsResponse {

    private UUID paperMetadataId;
    private int totalRequested;
    private int totalPromoted;
    private int totalSkipped;
    private int totalFailed;

    @Builder.Default
    private List<UUID> promotedQuestionIds = new ArrayList<>();

    @Builder.Default
    private Map<UUID, String> failureReasons = new HashMap<>();
}
