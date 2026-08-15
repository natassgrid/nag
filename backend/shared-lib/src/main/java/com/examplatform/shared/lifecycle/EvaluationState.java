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
 * Lifecycle states for an {@code Evaluation} record.
 *
 * <p>Valid transitions:
 * <pre>
 *   PENDING         → AUTO_EVALUATED      (MCQ/Numerical auto-scored after session submission)
 *   PENDING         → MANUAL_EVALUATED    (Descriptive/Coding sent to first Evaluator)
 *   AUTO_EVALUATED  → FINALIZED           (no manual step required; scores accepted)
 *   MANUAL_EVALUATED→ ARBITRATION         (dual-evaluator score divergence exceeds tolerance)
 *   MANUAL_EVALUATED→ FINALIZED           (scores reconciled within tolerance)
 *   ARBITRATION     → FINALIZED           (arbitrator resolves divergence)
 * </pre>
 */
public enum EvaluationState {

    /**
     * Evaluation has been created but not yet scored.
     * The session has been submitted and evaluation is queued.
     */
    PENDING,

    /**
     * Response has been automatically scored by the platform
     * (applies to Single_MCQ, Multi_MCQ, Numerical question types).
     */
    AUTO_EVALUATED,

    /**
     * Response has been scored by at least one human Evaluator
     * (applies to Descriptive and Coding question types).
     * Awaiting second evaluator or reconciliation check.
     */
    MANUAL_EVALUATED,

    /**
     * Two evaluators produced scores that diverge beyond the configured
     * tolerance threshold. A senior Evaluator (arbitrator) must resolve
     * the discrepancy before the evaluation can be finalised.
     */
    ARBITRATION,

    /**
     * Evaluation is complete and the awarded score is locked.
     * The score feeds into the candidate's total and section-wise scores
     * via the Result Service.
     */
    FINALIZED
}
