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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response payload for navigation operations.
 * Contains the current question state and allowed navigation actions
 * based on the session's navigation policy.
 *
 * Validates: Requirements 9.2, 9.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationResponse {

    private UUID sessionId;

    /**
     * The current question index after navigation.
     */
    private int currentQuestionIndex;

    /**
     * The content of the current question (JSON string).
     */
    private String questionContent;

    /**
     * The navigation policy applied to this session.
     */
    private NavigationPolicy navigationPolicy;

    /**
     * Actions allowed from the current position given the navigation policy.
     */
    private List<NavigationAction> allowedActions;

    /**
     * Current section index (for section-based rendering).
     */
    private Integer currentSectionIndex;

    /**
     * Total number of questions in the exam.
     */
    private int totalQuestions;

    /**
     * Navigation policies that govern how candidates can move between questions.
     */
    public enum NavigationPolicy {
        /** Only forward movement allowed — no going back */
        SEQUENTIAL,
        /** Free movement in any direction — NEXT, PREV, JUMP */
        FLEXIBLE,
        /** Movement within section only — no cross-section jumps */
        RESTRICTED
    }

    /**
     * Allowed navigation actions for the current session state.
     */
    public enum NavigationAction {
        NEXT,
        PREV,
        JUMP,
        SECTION_SWITCH
    }
}
