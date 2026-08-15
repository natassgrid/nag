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
import com.examplatform.asset.domain.enums.ReferenceType;
import com.examplatform.asset.dto.AssetMetadataUpdateRequest;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.dto.AssetReferenceResponse;
import com.examplatform.asset.dto.AssetSearchRequest;
import com.examplatform.asset.dto.AssetUploadResponse;
import com.examplatform.asset.service.AssetService;
import com.examplatform.asset.service.ReferenceService;
import com.examplatform.shared.api.ApiResponse;
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
 * REST controller for multimedia asset management.
 *
 * <p>Endpoints:
 * <pre>
 *   POST   /api/v1/assets              — Upload a new asset
 *   GET    /api/v1/assets/{id}         — Get asset metadata
 *   GET    /api/v1/assets              — List assets (paginated)
 *   PUT    /api/v1/assets/{id}/metadata— Update user metadata
 *   DELETE /api/v1/assets/{id}         — Soft-delete asset
 *   GET    /api/v1/assets/search       — Search with filters
 *   GET    /api/v1/assets/{id}/download— Download binary content
 *   PUT    /api/v1/assets/{id}/archive — Archive asset
 *   PUT    /api/v1/assets/{id}/restore — Restore archived asset
 *   POST   /api/v1/assets/references   — Add reference
 *   GET    /api/v1/assets/{id}/references — List references for asset
 *   DELETE /api/v1/assets/references/{refId} — Remove reference
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final ReferenceService referenceService;

    /**
     * Upload a new multimedia asset.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) throws IOException {

        UUID userId = UUID.fromString(jwt.getSubject());

        log.info("Upload request: filename={}, size={}, contentType={}, user={}, tenant={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), userId, tenantId);

        AssetUploadResponse response = assetService.upload(file, userId, tenantId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Asset uploaded successfully"));
    }

    /**
     * Retrieve asset metadata by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> getAsset(@PathVariable UUID id) {
        AssetUploadResponse response = assetService.getAsset(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset retrieved successfully"));
    }

    /**
     * List assets with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AssetUploadResponse>>> listAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        Page<AssetUploadResponse> results = assetService.listAssets(page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.success(results, "Assets retrieved successfully"));
    }

    /**
     * Search assets with optional filters.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AssetUploadResponse>>> searchAssets(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant uploadDateFrom,
            @RequestParam(required = false) Instant uploadDateTo,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Boolean referenced,
            @RequestParam(required = false) String storageProvider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        AssetSearchRequest searchRequest = AssetSearchRequest.builder()
                .filename(filename)
                .assetType(assetType)
                .contentType(contentType)
                .tags(tags)
                .createdBy(createdBy)
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
     * Update user-supplied metadata on an existing asset.
     */
    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetUploadResponse>> updateMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody AssetMetadataUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetUploadResponse response = assetService.updateMetadata(id, request, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Asset metadata updated successfully"));
    }

    /**
     * Soft-delete an asset. Fails if the asset is still referenced.
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
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<InputStreamResource> downloadAsset(@PathVariable UUID id) {
        MediaAsset asset = assetService.getAssetEntity(id);
        Optional<InputStream> content = assetService.downloadAsset(id);

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asset.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .contentLength(asset.getFileSize())
                .body(new InputStreamResource(content.get()));
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

    // ── Reference Management ─────────────────────────────────────────────────

    /**
     * Create a reference between an asset and a platform entity.
     */
    @PostMapping("/references")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<AssetReferenceResponse>> addReference(
            @Valid @RequestBody AssetReferenceRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        AssetReferenceResponse response = referenceService.addReference(request, userId, tenantId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reference created successfully"));
    }

    /**
     * List all references for a given asset.
     */
    @GetMapping("/{id}/references")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<AssetReferenceResponse>>> getReferences(@PathVariable UUID id) {
        List<AssetReferenceResponse> references = referenceService.getReferencesForAsset(id);
        return ResponseEntity.ok(ApiResponse.success(references, "References retrieved successfully"));
    }

    /**
     * Remove a reference.
     */
    @DeleteMapping("/references/{refId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeReference(@PathVariable UUID refId) {
        referenceService.removeReference(refId);
        return ResponseEntity.ok(ApiResponse.success("Reference removed successfully"));
    }
}
