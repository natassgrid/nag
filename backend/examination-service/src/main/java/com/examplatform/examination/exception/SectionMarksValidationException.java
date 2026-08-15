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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the sum of section marks (marksPerQuestion × questionCount)
 * does not equal the declared totalMarks of the examination.
 *
 * Validates: Requirement 7.6
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SectionMarksValidationException extends RuntimeException {

    private final int expectedTotalMarks;
    private final double actualTotalMarks;

    public SectionMarksValidationException(int expected, double actual) {
        super(String.format(
                "Section marks mismatch: expected totalMarks=%d, but sum(marksPerQuestion × questionCount) = %.2f",
                expected, actual));
        this.expectedTotalMarks = expected;
        this.actualTotalMarks = actual;
    }
}
