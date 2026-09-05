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

package com.examplatform.questionbank.translation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for a single translated answer option submitted by a translator.
 *
 * <p>The {@code id} must match one of the source question's option identifiers
 * (A–F).  The {@code text} carries the translated wording.
 * Correctness ({@code isCorrect}) is never submitted here — it is always
 * derived from the source question.
 */
public record TranslatedOptionDto(

        /** Must match source option id: A, B, C, D, E, or F. */
        @NotBlank(message = "option id is required")
        String id,

        /** Translated option text. */
        @NotBlank(message = "option text is required")
        String text
) {}
