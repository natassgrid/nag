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
import java.util.UUID;

/**
 * Feasibility and sufficiency analysis response for a blueprint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintFeasibilityResponse {

    private boolean feasible;
    private UUID examId;
    private String shiftId;
    private int totalQuestionsNeeded;
    private int totalQuestionsAvailable;
    private int deficitRuleCount;
    private List<RuleFeasibilityDetail> ruleDetails;
    private List<GapDetail> gaps;
    private boolean notificationDispatched;
    private String summary;
    private Instant checkedAt;
}
