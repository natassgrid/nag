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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * POJO representing an answer key entry for a single question.
 * Used during auto-evaluation to determine correct answers and marking scheme.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerKey {

    private UUID questionId;

    /**
     * Question type: SINGLE_MCQ, MULTI_MCQ, NUMERICAL
     */
    private String questionType;

    /**
     * For MCQ: JSON array of correct option IDs e.g. ["opt-2"]
     * For Numerical: the correct numeric value as a string e.g. "3.14"
     */
    private String correctAnswer;

    private double marksPerQuestion;

    /**
     * Negative marks to deduct for a wrong answer. 0 if no negative marking.
     */
    private double negativeMarks;
}
