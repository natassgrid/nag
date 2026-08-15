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

package com.examplatform.papergenerator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when shift comparability validation fails — i.e. the relative difficulty
 * difference between any pair of papers exceeds the 2% threshold.
 *
 * Validates: Requirements 8.9
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ShiftComparabilityViolationException extends RuntimeException {

    private final UUID examId;
    private final List<String> violations;

    public ShiftComparabilityViolationException(UUID examId, List<String> violations) {
        super("Shift comparability violated for exam " + examId + ": " + violations.size() + " violation(s)");
        this.examId = examId;
        this.violations = violations;
    }

    public UUID getExamId() {
        return examId;
    }

    public List<String> getViolations() {
        return violations;
    }
}
