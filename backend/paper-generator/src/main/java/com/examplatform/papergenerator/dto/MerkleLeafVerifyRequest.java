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

package com.examplatform.papergenerator.dto;

import com.examplatform.papergenerator.crypto.MerkleTree;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO to verify question leaf inclusion in a Paper Merkle Tree.
 *
 * Validates: Issue #156
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerkleLeafVerifyRequest {

    @NotBlank(message = "leafHash is required")
    private String leafHash;

    @NotBlank(message = "rootHash is required")
    private String rootHash;

    private List<MerkleTree.MerkleProofStep> proofSteps;
}
