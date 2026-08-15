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

package com.examplatform.questionbank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a new question is too similar to an existing PUBLISHED question.
 * Returns HTTP 422 Unprocessable Entity with the ID of the similar question.
 *
 * Validates: Requirement 4.7
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SimilarQuestionException extends RuntimeException {

    private final UUID similarQuestionId;

    public SimilarQuestionException(UUID similarQuestionId) {
        super("Question is too similar to existing published question: " + similarQuestionId);
        this.similarQuestionId = similarQuestionId;
    }

    public UUID getSimilarQuestionId() {
        return similarQuestionId;
    }
}
