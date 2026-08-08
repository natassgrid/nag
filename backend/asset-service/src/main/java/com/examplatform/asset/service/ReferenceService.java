package com.examplatform.asset.service;

import com.examplatform.asset.domain.entity.AssetReference;
import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.ReferenceType;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.dto.AssetReferenceResponse;
import com.examplatform.asset.repository.AssetReferenceRepository;
import com.examplatform.asset.repository.MediaAssetRepository;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages references between assets and platform entities (questions, passages, etc.).
 * Prevents deletion of assets that are still in use.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceService {

    private final AssetReferenceRepository referenceRepository;
    private final MediaAssetRepository assetRepository;

    /**
     * Create a reference between an asset and a platform entity.
     */
    @Transactional
    public AssetReferenceResponse addReference(AssetReferenceRequest request, UUID userId, String tenantId) {
        // Verify the asset exists
        if (!assetRepository.existsById(request.getAssetId())) {
            throw new EntityNotFoundException("Asset not found: " + request.getAssetId());
        }

        // Check for duplicate reference
        if (referenceRepository.existsByAssetIdAndReferenceTypeAndReferenceId(
                request.getAssetId(), request.getReferenceType(), request.getReferenceId())) {
            log.info("Reference already exists: asset={}, type={}, ref={}",
                    request.getAssetId(), request.getReferenceType(), request.getReferenceId());
            return referenceRepository
                    .findByAssetIdAndReferenceTypeAndReferenceId(
                            request.getAssetId(), request.getReferenceType(), request.getReferenceId())
                    .map(this::mapToResponse)
                    .orElseThrow();
        }

        TenantContext.set(tenantId);
        AssetReference reference = AssetReference.builder()
                .assetId(request.getAssetId())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .createdBy(userId)
                .build();

        reference = referenceRepository.save(reference);

        log.info("Reference created: id={}, asset={}, type={}, ref={}",
                reference.getId(), request.getAssetId(), request.getReferenceType(), request.getReferenceId());
        return mapToResponse(reference);
    }

    /**
     * Remove a reference between an asset and a platform entity.
     */
    @Transactional
    public void removeReference(UUID referenceId) {
        if (!referenceRepository.existsById(referenceId)) {
            throw new EntityNotFoundException("Reference not found: " + referenceId);
        }
        referenceRepository.deleteById(referenceId);
        log.info("Reference removed: id={}", referenceId);
    }

    /**
     * List all references for a given asset.
     */
    @Transactional(readOnly = true)
    public List<AssetReferenceResponse> getReferencesForAsset(UUID assetId) {
        return referenceRepository.findByAssetId(assetId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * List all assets referenced by a given entity.
     */
    @Transactional(readOnly = true)
    public List<AssetReferenceResponse> getReferencesForEntity(ReferenceType type, UUID referenceId) {
        return referenceRepository.findByReferenceTypeAndReferenceId(type, referenceId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Check if an asset is referenced anywhere.
     */
    @Transactional(readOnly = true)
    public boolean isReferenced(UUID assetId) {
        return referenceRepository.countByAssetId(assetId) > 0;
    }

    private AssetReferenceResponse mapToResponse(AssetReference ref) {
        return AssetReferenceResponse.builder()
                .id(ref.getId())
                .assetId(ref.getAssetId())
                .referenceType(ref.getReferenceType())
                .referenceId(ref.getReferenceId())
                .createdBy(ref.getCreatedBy())
                .createdAt(ref.getCreatedAt())
                .build();
    }
}
