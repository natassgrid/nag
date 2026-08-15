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

package com.examplatform.asset.repository;

import com.examplatform.asset.domain.entity.AssetReference;
import com.examplatform.asset.domain.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link AssetReference} entities.
 */
@Repository
public interface AssetReferenceRepository extends JpaRepository<AssetReference, UUID> {

    /**
     * Count how many references exist for a given asset.
     */
    long countByAssetId(UUID assetId);

    /**
     * Find all references for a given asset.
     */
    List<AssetReference> findByAssetId(UUID assetId);

    /**
     * Find all assets referenced by a specific entity.
     */
    List<AssetReference> findByReferenceTypeAndReferenceId(ReferenceType referenceType, UUID referenceId);

    /**
     * Check if a specific reference already exists (deduplication).
     */
    boolean existsByAssetIdAndReferenceTypeAndReferenceId(UUID assetId, ReferenceType referenceType, UUID referenceId);

    /**
     * Find a specific reference (for returning existing on duplicate).
     */
    Optional<AssetReference> findByAssetIdAndReferenceTypeAndReferenceId(
            UUID assetId, ReferenceType referenceType, UUID referenceId);
}
