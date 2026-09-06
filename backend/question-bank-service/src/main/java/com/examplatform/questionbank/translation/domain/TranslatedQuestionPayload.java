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

package com.examplatform.questionbank.translation.domain;

import java.util.List;

/**
 * Value object representing the structured translated content of a question.
 * This record is Jackson-serialized to JSON and stored in the
 * {@code translated_payload} column of {@code question_service.translation}.
 *
 * <p>Only the human-readable fields that need translation are included:
 * <ul>
 *   <li>{@code content} — translated question stem / body</li>
 *   <li>{@code options} — translated option texts; option IDs (A–F) are
 *       preserved from the source question so correctness mapping is unambiguous</li>
 *   <li>{@code explanation} — translated explanation (may be null)</li>
 * </ul>
 *
 * <p>Answer correctness ({@code isCorrect}) is intentionally omitted — it
 * never changes with translation and is always read from the source
 * {@link com.examplatform.questionbank.dto.QuestionOption}.
 */
public record TranslatedQuestionPayload(
        String content,
        List<TranslatedOption> options,
        String explanation
) {

    /**
     * A single translated answer option.
     *
     * @param id   must match the source question's option id (A, B, C, D, E, or F)
     * @param text the translated option text
     */
    public record TranslatedOption(String id, String text) {}
}
