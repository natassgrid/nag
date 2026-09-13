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

package com.examplatform.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single answer option for MCQ/MSQ questions.
 * Option IDs are assigned A-F based on position in the list.
 *
 * Validates: Requirement 30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidOption
public class QuestionOption {

    /** Option identifier: A, B, C, D, E, or F */
    private String id;

    /** The visible text of this option */
    private String text;

    /** True if this option is part of the correct answer */
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    private boolean correct;

    /** CDN/storage URL of a raster image or SVG asset for this option */
    private String imageUrl;

    /** Accessible alt text for the image (for screen-readers and translation) */
    private String imageAltText;

    /**
     * Backward-compatible 3-argument constructor for text-only options.
     */
    public QuestionOption(String id, String text, boolean correct) {
        this(id, text, correct, null, null);
    }
}
