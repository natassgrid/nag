package com.examplatform.asset.repository;

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
              AND (:filename IS NULL OR LOWER(a.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%')))
              AND (:assetType IS NULL OR a.assetType = :assetType)
              AND (:contentType IS NULL OR a.contentType = :contentType)
              AND (:tags IS NULL OR LOWER(a.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
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
