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

package com.examplatform.questionbank.ai.similarity;

import java.util.List;
import java.util.UUID;

/**
 * Result of a similarity check against existing questions in the same subject and tenant.
 *
 * <p>Status semantics (per FR-2):
 * <ul>
 *   <li>{@link Status#PASS} — no near-duplicates found (all similarities &lt; 0.85)</li>
 *   <li>{@link Status#WARN} — at least one question with similarity 0.85–0.92 (flagged for human review)</li>
 *   <li>{@link Status#REJECT} — at least one question with similarity &gt; 0.92 (near-duplicate, reject creation)</li>
 * </ul>
 *
 * Validates: Requirements FR-2 (Duplicate Detection)
 */
public record SimilarityCheckResult(
        Status status,
        List<SimilarQuestion> similarQuestions
) {

    /**
     * Outcome of a similarity check.
     */
    public enum Status {
        /** No duplicates detected — similarity below 0.85 for all matches. */
        PASS,
        /** Warning — at least one match between 0.85 and 0.92 (needs human review). */
        WARN,
        /** Rejected — at least one match above 0.92 (near-duplicate). */
        REJECT
    }

    /**
     * A question identified as similar during the check.
     *
     * @param questionId the UUID of the similar question
     * @param similarity the cosine similarity score (0.0 to 1.0)
     * @param content    a snippet of the similar question's content (for reference)
     */
    public record SimilarQuestion(
            UUID questionId,
            double similarity,
            String content
    ) {
    }

    /**
     * Creates a PASS result with no similar questions.
     */
    public static SimilarityCheckResult pass() {
        return new SimilarityCheckResult(Status.PASS, List.of());
    }
}
