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

package com.examplatform.questionbank.util;

/**
 * Utility class for embedding vector operations.
 * Provides conversion between float arrays and pgvector-compatible string format.
 */
public final class EmbeddingUtils {

    private EmbeddingUtils() {
        // Utility class — no instantiation
    }

    /**
     * Converts a float array embedding to a pgvector-compatible string representation.
     * Format: "[0.1,0.2,0.3,...]"
     *
     * @param embedding the float array to convert
     * @return pgvector-compatible string (e.g., "[0.1,0.2,0.3]")
     */
    public static String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
