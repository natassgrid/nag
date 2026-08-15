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

package com.examplatform.asset.service;

import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.asset.dto.AssetMetadataUpdateRequest;
import com.examplatform.asset.dto.AssetSearchRequest;
import com.examplatform.asset.dto.AssetUploadResponse;
import com.examplatform.asset.metadata.MediaMetadata;
import com.examplatform.asset.metadata.MetadataExtractionService;
import com.examplatform.asset.repository.AssetReferenceRepository;
import com.examplatform.asset.repository.MediaAssetRepository;
import com.examplatform.asset.storage.StorageProperties;
import com.examplatform.asset.storage.StorageProvider;
import com.examplatform.asset.storage.StorageProviderRegistry;
import com.examplatform.asset.validation.AssetValidationException;
import com.examplatform.asset.validation.SecurityValidationPipeline;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service orchestrating asset upload, retrieval, metadata update,
 * lifecycle management, and deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final MediaAssetRepository assetRepository;
    private final AssetReferenceRepository referenceRepository;
    private final SecurityValidationPipeline validationPipeline;
    private final MetadataExtractionService metadataExtractionService;
    private final StorageProviderRegistry storageProviderRegistry;
    private final StorageProperties storageProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AUDIT_TOPIC = "exam.audit.events";

    /**
     * Upload a new asset: validate, compute hash, check for duplicates,
     * extract metadata, store binary, persist metadata.
     */
    @Transactional
    public AssetUploadResponse upload(MultipartFile file, UUID userId, String tenantId) throws IOException {
        log.info("Uploading asset: filename={}, size={}, contentType={}, user={}, tenant={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), userId, tenantId);

        byte[] fileBytes = file.getBytes();

        // 1. Run security validation pipeline
        SecurityValidationPipeline.ValidationResult validationResult = validationPipeline.validate(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                new ByteArrayInputStream(fileBytes));

        // 2. Compute SHA-256 hash
        String sha256Hash = SecurityValidationPipeline.computeSha256(fileBytes);

        // 3. Check for duplicate (same hash + tenant)
        Optional<MediaAsset> existing = assetRepository.findBySha256HashAndTenantId(sha256Hash, tenantId);
        if (existing.isPresent()) {
            MediaAsset existingAsset = existing.get();
            if (existingAsset.getStatus() != AssetStatus.DELETED) {
                log.info("Duplicate asset detected: existingId={}, hash={}", existingAsset.getId(), sha256Hash);
                return mapToResponse(existingAsset);
            }
        }

        // 4. Extract metadata
        MediaMetadata metadata = metadataExtractionService.extract(
                validationResult.getAssetType(),
                new ByteArrayInputStream(fileBytes),
                validationResult.getDetectedContentType());

        // 5. Store binary via configured provider
        StorageProvider provider = storageProviderRegistry.requireProvider(storageProperties.getProvider());
        String storagePath = buildStoragePath(tenantId, validationResult.getSanitizedFilename(), sha256Hash);
        String storageLocation = provider.upload(storagePath, new ByteArrayInputStream(fileBytes),
                validationResult.getDetectedContentType(), fileBytes.length);

        // 6. Persist metadata entity
        TenantContext.set(tenantId);
        MediaAsset asset = MediaAsset.builder()
                .originalFilename(validationResult.getSanitizedFilename())
                .contentType(validationResult.getDetectedContentType())
                .extension(validationResult.getExtension())
                .fileSize((long) fileBytes.length)
                .sha256Hash(sha256Hash)
                .assetType(validationResult.getAssetType())
                .status(AssetStatus.ACTIVE)
                .width(metadata.getWidth())
                .height(metadata.getHeight())
                .dpi(metadata.getDpi())
                .orientation(metadata.getOrientation())
                .durationSeconds(metadata.getDurationSeconds())
                .codec(metadata.getCodec())
                .bitrate(metadata.getBitrate())
                .sampleRate(metadata.getSampleRate())
                .channels(metadata.getChannels())
                .frameRate(metadata.getFrameRate())
                .storageProvider(provider.name())
                .storageLocation(storageLocation)
                .createdBy(userId)
                .build();

        asset = assetRepository.save(asset);

        // 7. Publish audit event
        publishAuditEvent("ASSET_UPLOADED", asset.getId(), userId, tenantId);

        log.info("Asset uploaded successfully: id={}, type={}, size={}", asset.getId(), asset.getAssetType(), asset.getFileSize());
        return mapToResponse(asset);
    }

    /**
     * Retrieve an asset by ID.
     */
    @Transactional(readOnly = true)
    public AssetUploadResponse getAsset(UUID assetId) {
        MediaAsset asset = findAssetOrThrow(assetId);
        return mapToResponse(asset);
    }

    /**
     * List assets with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AssetUploadResponse> listAssets(int page, int size, String tenantId) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return assetRepository.findByTenantIdAndStatusNot(tenantId, AssetStatus.DELETED, pageRequest)
                .map(this::mapToResponse);
    }

    /**
     * Search assets with filters.
     */
    @Transactional(readOnly = true)
    public Page<AssetUploadResponse> searchAssets(AssetSearchRequest searchRequest, int page, int size, String tenantId) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return assetRepository.searchAssets(
                tenantId,
                searchRequest.getFilename(),
                searchRequest.getAssetType(),
                searchRequest.getContentType(),
                searchRequest.getTags(),
                searchRequest.getStatus(),
                searchRequest.getStorageProvider(),
                pageRequest
        ).map(this::mapToResponse);
    }

    /**
     * Update user-supplied metadata (title, description, altText, tags, language).
     */
    @Transactional
    public AssetUploadResponse updateMetadata(UUID assetId, AssetMetadataUpdateRequest request, UUID userId, String tenantId) {
        MediaAsset asset = findAssetOrThrow(assetId);

        if (request.getTitle() != null) asset.setTitle(request.getTitle());
        if (request.getDescription() != null) asset.setDescription(request.getDescription());
        if (request.getAltText() != null) asset.setAltText(request.getAltText());
        if (request.getTags() != null) asset.setTags(request.getTags());
        if (request.getLanguage() != null) asset.setLanguage(request.getLanguage());

        asset = assetRepository.save(asset);

        publishAuditEvent("ASSET_METADATA_UPDATED", asset.getId(), userId, tenantId);

        log.info("Asset metadata updated: id={}", assetId);
        return mapToResponse(asset);
    }

    /**
     * Soft-delete an asset. Rejects deletion if the asset is still referenced.
     */
    @Transactional
    public void deleteAsset(UUID assetId, UUID userId, String tenantId) {
        MediaAsset asset = findAssetOrThrow(assetId);

        // Prevent deletion of referenced assets
        long refCount = referenceRepository.countByAssetId(assetId);
        if (refCount > 0) {
            throw new AssetValidationException(
                    "Cannot delete asset " + assetId + ": still referenced by " + refCount + " entities");
        }

        asset.setStatus(AssetStatus.DELETED);
        assetRepository.save(asset);

        publishAuditEvent("ASSET_DELETED", assetId, userId, tenantId);

        log.info("Asset soft-deleted: id={}", assetId);
    }

    /**
     * Archive an active asset.
     */
    @Transactional
    public AssetUploadResponse archiveAsset(UUID assetId, UUID userId, String tenantId) {
        MediaAsset asset = findAssetOrThrow(assetId);

        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new AssetValidationException("Only ACTIVE assets can be archived. Current status: " + asset.getStatus());
        }

        asset.setStatus(AssetStatus.ARCHIVED);
        assetRepository.save(asset);

        publishAuditEvent("ASSET_ARCHIVED", assetId, userId, tenantId);

        log.info("Asset archived: id={}", assetId);
        return mapToResponse(asset);
    }

    /**
     * Restore an archived asset to ACTIVE status.
     */
    @Transactional
    public AssetUploadResponse restoreAsset(UUID assetId, UUID userId, String tenantId) {
        MediaAsset asset = findAssetOrThrow(assetId);

        if (asset.getStatus() != AssetStatus.ARCHIVED) {
            throw new AssetValidationException("Only ARCHIVED assets can be restored. Current status: " + asset.getStatus());
        }

        asset.setStatus(AssetStatus.ACTIVE);
        assetRepository.save(asset);

        publishAuditEvent("ASSET_RESTORED", assetId, userId, tenantId);

        log.info("Asset restored: id={}", assetId);
        return mapToResponse(asset);
    }

    /**
     * Download binary content for the given asset.
     *
     * @return the InputStream, or empty if the storage file is missing
     */
    @Transactional(readOnly = true)
    public Optional<java.io.InputStream> downloadAsset(UUID assetId) {
        MediaAsset asset = findAssetOrThrow(assetId);
        StorageProvider provider = storageProviderRegistry.requireProvider(asset.getStorageProvider());
        return provider.download(asset.getStorageLocation());
    }

    /**
     * Get the MediaAsset entity (for content-type headers in controller).
     */
    @Transactional(readOnly = true)
    public MediaAsset getAssetEntity(UUID assetId) {
        return findAssetOrThrow(assetId);
    }

    // ────────────────────────────────────────────────────────────────────────

    private MediaAsset findAssetOrThrow(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset not found: " + assetId));
    }

    private String buildStoragePath(String tenantId, String filename, String sha256) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // Use hash prefix to distribute files across directories
        String hashPrefix = sha256.substring(0, 4);
        return tenantId + "/" + datePath + "/" + hashPrefix + "/" + sha256 + "_" + filename;
    }

    private void publishAuditEvent(String eventType, UUID assetId, UUID userId, String tenantId) {
        try {
            var event = java.util.Map.of(
                    "eventType", eventType,
                    "assetId", assetId.toString(),
                    "userId", userId.toString(),
                    "tenantId", tenantId,
                    "timestamp", java.time.Instant.now().toString()
            );
            kafkaTemplate.send(AUDIT_TOPIC, assetId.toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish audit event: {}", e.getMessage());
        }
    }

    private AssetUploadResponse mapToResponse(MediaAsset asset) {
        return AssetUploadResponse.builder()
                .id(asset.getId())
                .originalFilename(asset.getOriginalFilename())
                .contentType(asset.getContentType())
                .extension(asset.getExtension())
                .fileSize(asset.getFileSize())
                .sha256Hash(asset.getSha256Hash())
                .assetType(asset.getAssetType())
                .status(asset.getStatus())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .dpi(asset.getDpi())
                .orientation(asset.getOrientation())
                .durationSeconds(asset.getDurationSeconds())
                .codec(asset.getCodec())
                .bitrate(asset.getBitrate())
                .sampleRate(asset.getSampleRate())
                .channels(asset.getChannels())
                .frameRate(asset.getFrameRate())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .altText(asset.getAltText())
                .tags(asset.getTags())
                .language(asset.getLanguage())
                .storageProvider(asset.getStorageProvider())
                .storageLocation(asset.getStorageLocation())
                .createdBy(asset.getCreatedBy())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .tenantId(asset.getTenantId())
                .build();
    }
}
