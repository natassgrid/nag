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

package com.examplatform.shared.lifecycle;

/**
 * Lifecycle states for a {@code Question} entity.
 *
 * <p>Valid transitions (enforced by Question Bank Service FSM):
 * <pre>
 *   DRAFT  → REVIEW
 *   REVIEW → APPROVED
 *   REVIEW → DRAFT       (returned with comments)
 *   APPROVED → PUBLISHED
 *   PUBLISHED → ARCHIVED
 * </pre>
 *
 * @see com.examplatform.shared.audit.AuditEventType#QUESTION_STATE_TRANSITION
 */
public enum QuestionState {

    /**
     * Initial state. Question is being authored and has not yet entered review.
     * Content may be incomplete.
     */
    DRAFT,

    /**
     * Question has been submitted for peer review.
     * A Reviewer is assigned based on subject specialisation.
     */
    REVIEW,

    /**
     * Question passed review. It is ready for inclusion in a paper.
     * Waiting for Approver sign-off before publication.
     */
    APPROVED,

    /**
     * Question is live and eligible for selection into examination papers.
     * Exposure tracking (usageCount, lastUsedAt) begins here.
     */
    PUBLISHED,

    /**
     * Question has been retired from active use.
     * Cannot be selected for new papers; retained for historical reference.
     */
    ARCHIVED
}
