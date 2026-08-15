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

import com.examplatform.papergenerator.dto.GapDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * Thrown when a paper generation blueprint cannot be satisfied due to
 * insufficient questions available in the question bank.
 * Returns HTTP 422 Unprocessable Entity with gap details.
 *
 * Validates: Requirements 8.5
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientQuestionsException extends RuntimeException {

    private final List<GapDetail> gapDetails;

    public InsufficientQuestionsException(String message, List<GapDetail> gapDetails) {
        super(message);
        this.gapDetails = gapDetails;
    }

    public List<GapDetail> getGapDetails() {
        return gapDetails;
    }
}
