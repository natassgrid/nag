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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for submitting a new translation or resubmitting after rejection.
 *
 * <p>The translator provides:
 * <ul>
 *   <li>{@code translatedContent} — the question stem/body in the target language</li>
 *   <li>{@code translatedOptions} — one entry per option in the source question,
 *       keyed by the same option id (A–F); required when the source has options</li>
 *   <li>{@code translatedExplanation} — optional explanation in the target language</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {

    @NotNull(message = "questionId is required")
    private UUID questionId;

    @NotBlank(message = "languageCode is required")
    private String languageCode;

    @NotNull(message = "translatorId is required")
    private UUID translatorId;

    /** Translated question body (stem). */
    @NotBlank(message = "translatedContent is required")
    private String translatedContent;

    /**
     * Translated answer options.  Each entry must supply the same {@code id} as
     * the corresponding source option so correctness mapping is unambiguous.
     * May be null/empty for question types that have no options (e.g. SHORT_ANSWER).
     */
    @Valid
    private List<TranslatedOptionDto> translatedOptions;

    /** Translated explanation. Optional. */
    private String translatedExplanation;
}
