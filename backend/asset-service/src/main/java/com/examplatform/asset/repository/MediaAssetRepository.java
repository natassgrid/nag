package com.examplatform.asset.repository;
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


import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link MediaAsset} entities.
 */
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    /**
     * Find an asset by its SHA-256 hash within a tenant (duplicate detection).
     */
    Optional<MediaAsset> findBySha256HashAndTenantId(String sha256Hash, String tenantId);

    /**
     * List non-deleted assets for a tenant with pagination.
     */
    Page<MediaAsset> findByTenantIdAndStatusNot(String tenantId, AssetStatus status, Pageable pageable);

    /**
     * Search assets with optional filters.
     * All filter parameters are nullable — when null, that criterion is ignored.
     */
    @Query("""
            SELECT a FROM MediaAsset a
            WHERE a.tenantId = :tenantId
              AND (:filename IS NULL OR LOWER(a.originalFilename) LIKE LOWER(CONCAT('%', CAST(:filename AS string), '%')))
              AND (:assetType IS NULL OR a.assetType = :assetType)
              AND (:contentType IS NULL OR a.contentType = :contentType)
              AND (:tags IS NULL OR LOWER(a.tags) LIKE LOWER(CONCAT('%', CAST(:tags AS string), '%')))
              AND (:status IS NULL OR a.status = :status)
              AND (:storageProvider IS NULL OR a.storageProvider = :storageProvider)
              AND a.status <> 'DELETED'
            """)
    Page<MediaAsset> searchAssets(
            @Param("tenantId") String tenantId,
            @Param("filename") String filename,
            @Param("assetType") AssetType assetType,
            @Param("contentType") String contentType,
            @Param("tags") String tags,
            @Param("status") AssetStatus status,
            @Param("storageProvider") String storageProvider,
            Pageable pageable);
}
