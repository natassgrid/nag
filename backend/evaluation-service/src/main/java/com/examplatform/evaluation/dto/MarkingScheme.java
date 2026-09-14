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

package com.examplatform.evaluation.dto;

/**
 * Defines the marking scheme applied during auto-evaluation.
 *
 * Validates: SPEC-E1 (Flexible Marking Scheme)
 */
public enum MarkingScheme {
    /**
     * Standard marking: positive marks for correct, negative marks for wrong.
     * Zero marks for unattempted.
     */
    STANDARD,

    /**
     * Zero-negative marking: positive marks for correct, zero for wrong.
     * Zero marks for unattempted.
     */
    ZERO_NEGATIVE,

    /**
     * Partial credit marking: proportional marks for MULTI_MCQ.
     * Full marks if all correct options selected, partial if subset selected (no wrong options),
     * negative marks if any wrong option selected.
     */
    PARTIAL_CREDIT
}
