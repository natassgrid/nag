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

package com.examplatform.candidate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for face verification.
 * Contains embedding vectors for the submitted photograph and the identity document photograph.
 *
 * Validates: Requirements 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerificationRequest {

    @NotNull
    private UUID userId;

    /**
     * Embedding vector of the submitted candidate photograph.
     */
    @NotNull
    private float[] photoEmbedding;

    /**
     * Embedding vector of the photograph from the identity document.
     */
    @NotNull
    private float[] docPhotoEmbedding;
}
