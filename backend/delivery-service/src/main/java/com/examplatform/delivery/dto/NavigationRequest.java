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

package com.examplatform.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for question navigation within an exam session.
 * Specifies the target question/section the candidate wants to navigate to.
 *
 * Validates: Requirements 9.2, 9.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationRequest {

    @NotNull
    private UUID sessionId;

    /**
     * Target question index within the exam paper (0-based).
     */
    private Integer targetQuestionIndex;

    /**
     * Target section index for section-based navigation (0-based).
     * Used with RESTRICTED and Section_Mode rendering.
     */
    private Integer targetSectionIndex;
}
