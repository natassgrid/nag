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

package com.examplatform.asset.controller;

import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import com.examplatform.asset.dto.AssetMetadataUpdateRequest;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.dto.AssetReferenceResponse;
import com.examplatform.asset.dto.AssetSearchRequest;
import com.examplatform.asset.dto.AssetUploadResponse;
import com.examplatform.asset.service.AssetService;
import com.examplatform.asset.service.ReferenceService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for asset upload, retrieval, metadata, references, and lifecycle.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final ReferenceService referenceService;

    /**
     * Upload a new media asset (multipart/form-data).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'ADMIN', 'CONTENT_MANAGER', 'CANDIDATE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) throws IOException {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.upload(file, userId, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Asset uploaded successfully"));
    }

    /**
     * Replace the binary content of an existing asset.
     */
    @PostMapping(value = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'CONTENT_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> replaceContent(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) throws IOException {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.replaceContent(id, file, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset content replaced successfully"));
    }

    /**
     * Retrieve asset metadata by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER', 'CANDIDATE')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> getAsset(@PathVariable UUID id) {
        AssetUploadResponse response = assetService.getAsset(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset retrieved successfully"));
    }

    /**
     * List assets for tenant (paginated).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AssetUploadResponse>>> listAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        Page<AssetUploadResponse> results = assetService.listAssets(page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(results, "Assets retrieved successfully"));
    }

    /**
     * Search and list assets with filters and pagination.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AssetUploadResponse>>> searchAssets(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) Instant uploadDateFrom,
            @RequestParam(required = false) Instant uploadDateTo,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Boolean referenced,
            @RequestParam(required = false) String storageProvider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        AssetSearchRequest searchRequest = AssetSearchRequest.builder()
                .filename(filename)
                .assetType(assetType)
                .contentType(contentType)
                .tags(tags)
                .createdBy(createdBy != null ? createdBy.toString() : null)
                .uploadDateFrom(uploadDateFrom)
                .uploadDateTo(uploadDateTo)
                .status(status)
                .referenced(referenced)
                .storageProvider(storageProvider)
                .build();

        Page<AssetUploadResponse> results = assetService.searchAssets(searchRequest, page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed successfully"));
    }

    /**
     * Update user-supplied metadata for an asset.
     */
    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'CONTENT_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> updateMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody AssetMetadataUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.updateMetadata(id, request, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset metadata updated successfully"));
    }

    /**
     * Create a reference between an asset and a platform entity.
     */
    @PostMapping("/references")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetReferenceResponse>> addReference(
            @Valid @RequestBody AssetReferenceRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetReferenceResponse response = referenceService.addReference(request, userId, tenantId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reference created successfully"));
    }

    /**
     * List all references for an asset.
     */
    @GetMapping("/{id}/references")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<AssetReferenceResponse>>> getReferences(@PathVariable UUID id) {
        List<AssetReferenceResponse> references = referenceService.getReferencesForAsset(id);
        return ResponseEntity.ok(ApiResponse.success(references, "References retrieved successfully"));
    }

    /**
     * Delete a specific reference.
     */
    @DeleteMapping("/references/{refId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeReference(@PathVariable UUID refId) {
        referenceService.removeReference(refId);
        return ResponseEntity.ok(ApiResponse.success("Reference removed successfully"));
    }

    /**
     * Soft-delete an asset. Rejected if the asset has active references.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        assetService.deleteAsset(id, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Asset deleted successfully"));
    }

    /**
     * Download the binary content of an asset.
     * Public access permitted so standard HTML <img>, <audio>, and <video> elements
     * can stream media without custom auth headers.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadAsset(@PathVariable UUID id) {
        MediaAsset asset;
        try {
            asset = assetService.getAssetEntity(id);
        } catch (EntityNotFoundException e) {
            log.debug("Asset not found for download: {}", id);
            return ResponseEntity.notFound().build();
        }

        Optional<InputStream> content;
        try {
            content = assetService.downloadAsset(id);
        } catch (Exception e) {
            log.error("Failed to load binary stream for asset {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType;
        try {
            mediaType = (asset.getContentType() != null && !asset.getContentType().isBlank())
                    ? MediaType.parseMediaType(asset.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asset.getOriginalFilename() + "\"")
                .contentType(mediaType)
                .contentLength(asset.getFileSize())
                .body(new InputStreamResource(content.get()));
    }

    /**
     * Get accessible public or direct URL for an asset.
     */
    @GetMapping("/{id}/url")
    public ResponseEntity<ApiResponse<String>> getPublicUrl(@PathVariable UUID id) {
        String publicUrl = assetService.getPublicUrl(id);
        return ResponseEntity.ok(ApiResponse.success(publicUrl, "Asset URL resolved successfully"));
    }

    /**
     * Archive an active asset.
     */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> archiveAsset(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.archiveAsset(id, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset archived successfully"));
    }

    /**
     * Restore an archived asset to ACTIVE.
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> restoreAsset(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.restoreAsset(id, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset restored successfully"));
    }
}
