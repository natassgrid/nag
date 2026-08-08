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
