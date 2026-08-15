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

package com.examplatform.examination.exception;

import lombok.Getter;

/**
 * Thrown when a shift's timing fields violate one of the ordering invariants
 * defined in Requirement 7b.3.
 */
@Getter
public class ShiftTimingViolationException extends RuntimeException {

    /** The name of the constraint that was violated (e.g. "reportingTime < gateClosingTime"). */
    private final String violatedConstraint;

    public ShiftTimingViolationException(String violatedConstraint, String detail) {
        super("Shift timing invariant violated [" + violatedConstraint + "]: " + detail);
        this.violatedConstraint = violatedConstraint;
    }
}
