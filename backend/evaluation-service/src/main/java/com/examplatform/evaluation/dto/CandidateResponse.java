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
 * POJO representing a candidate's finalized response to a single question.
 * Used as input to the auto-evaluation pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponse {

    private UUID questionId;

    /**
     * JSON array of selected option IDs (for MCQ questions).
     * e.g. ["opt-1", "opt-3"]
     */
    private String selectedOptionIds;

    /**
     * The value entered by the candidate (for numerical questions).
     */
    private String enteredValue;

    /**
     * False if both selectedOptionIds and enteredValue are null/blank
     * (i.e. the candidate did not attempt this question).
     */
    private boolean attempted;
}
