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

package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transitioning an examination schedule through the
 * approval workflow. Optionally includes an approval comment.
 * Validates: Requirements 7b.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTransitionRequest {

    /**
     * Target status to transition to.
     * Must be a valid next state per the approval FSM:
     * DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED →
     * SECURITY_REVIEW → CHAIRMAN_APPROVED → PUBLISHED | CANCELLED
     */
    @NotBlank
    private String targetStatus;

    /** Optional free-text comment recorded with the approval action. */
    private String comment;
}
