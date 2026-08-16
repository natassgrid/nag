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

package com.examplatform.questionbank.repository;

import java.util.UUID;

/**
 * Projection interface for cosine similarity search results from pgvector.
 * Used by native queries that return question id, subject, content, and similarity score.
 *
 * Validates: Requirements FR-2 (Duplicate Detection)
 */
public interface SimilarityResult {

    UUID getId();

    String getSubject();

    String getContent();

    /**
     * Cosine similarity score (1 - cosine distance).
     * Range: 0.0 (completely dissimilar) to 1.0 (identical).
     * Thresholds: >0.92 = near-duplicate (reject), 0.85-0.92 = flag for review.
     */
    Double getSimilarity();
}
