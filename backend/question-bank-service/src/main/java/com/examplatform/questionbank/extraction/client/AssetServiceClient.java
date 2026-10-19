/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.client;

import com.examplatform.asset.domain.enums.ReferenceType;
import com.examplatform.asset.dto.AssetReferenceRequest;
import com.examplatform.asset.service.AssetService;
import com.examplatform.asset.service.ReferenceService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.constants.SharedConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client service to interact with asset-service for retrieving uploaded question paper
 * binaries, querying asset metadata, and creating entity references.
 * Automatically utilizes in-memory AssetService beans when running inside monolith/content apps,
 * and seamlessly falls back to local disk storage and REST endpoints in distributed setups.
 */
@Slf4j
@Service
public class AssetServiceClient {

    private final RestClient restClient;
    private final String assetStorageBasePath;
    private final ObjectProvider<AssetService> assetServiceProvider;
    private final ObjectProvider<ReferenceService> referenceServiceProvider;

    public AssetServiceClient(
            @Value("${app.asset-service.url:http://localhost:${SERVER_PORT:9000}}") String assetServiceUrl,
            @Value("${asset.storage.filesystem.base-path:./asset-storage}") String assetStorageBasePath,
            RestClient.Builder restClientBuilder) {
        this(assetServiceUrl, assetStorageBasePath, restClientBuilder, null, null);
    }

    @Autowired
    public AssetServiceClient(
            @Value("${app.asset-service.url:http://localhost:${SERVER_PORT:9000}}") String assetServiceUrl,
            @Value("${asset.storage.filesystem.base-path:./asset-storage}") String assetStorageBasePath,
            RestClient.Builder restClientBuilder,
            ObjectProvider<AssetService> assetServiceProvider,
            ObjectProvider<ReferenceService> referenceServiceProvider) {
        this.restClient = restClientBuilder
                .baseUrl(assetServiceUrl)
                .build();
        this.assetStorageBasePath = assetStorageBasePath;
        this.assetServiceProvider = assetServiceProvider;
        this.referenceServiceProvider = referenceServiceProvider;
    }

    /**
     * Retrieves asset metadata from asset-service.
     *
     * @param assetId UUID of the uploaded asset
     * @param tenantId current tenant identifier
     * @return AssetMetadataDto containing filename, hash, size, and content type
     */
    public AssetMetadataDto getAssetMetadata(UUID assetId, String tenantId) {
        if (assetId == null) {
            return null;
        }

        // 1. Direct in-memory bean check (monolith mode)
        if (assetServiceProvider != null) {
            AssetService directAssetService = assetServiceProvider.getIfAvailable();
            if (directAssetService != null) {
                try {
                    var asset = directAssetService.getAsset(assetId);
                    if (asset != null) {
                        return AssetMetadataDto.builder()
                                .id(asset.getId())
                                .originalFilename(asset.getOriginalFilename())
                                .contentType(asset.getContentType())
                                .fileSize(asset.getFileSize())
                                .sha256Hash(asset.getSha256Hash())
                                .build();
                    }
                } catch (Exception ex) {
                    log.warn("Direct in-memory AssetService getAsset failed for assetId={}: {}", assetId, ex.getMessage());
                }
            }
        }

        // 2. REST fallback
        try {
            ApiResponse<AssetMetadataDto> response = restClient.get()
                    .uri("/api/v1/assets/{id}", assetId)
                    .header(SharedConstants.HEADER_TENANT_ID, tenantId != null ? tenantId : SharedConstants.DEFAULT_TENANT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<AssetMetadataDto>>() {});

            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve asset metadata from asset-service for assetId={}: {}", assetId, ex.getMessage());
        }
        return AssetMetadataDto.builder()
                .id(assetId)
                .originalFilename("paper-" + assetId + ".pdf")
                .contentType("application/pdf")
                .build();
    }

    /**
     * Downloads the binary content of the pre-uploaded question paper PDF.
     * Checks in-memory AssetService first, then local asset storage directory (zero-network path),
     * and falls back to the HTTP download endpoint of asset-service.
     *
     * @param assetId UUID of the uploaded asset
     * @param tenantId current tenant identifier
     * @return byte array of the PDF binary
     */
    public byte[] downloadAssetBytes(UUID assetId, String tenantId) {
        if (assetId == null) {
            throw new IllegalArgumentException("assetId must not be null");
        }

        // 1. Direct in-memory bean check (monolith mode)
        if (assetServiceProvider != null) {
            AssetService directAssetService = assetServiceProvider.getIfAvailable();
            if (directAssetService != null) {
                try {
                    Optional<InputStream> isOpt = directAssetService.downloadAsset(assetId);
                    if (isOpt.isPresent()) {
                        try (InputStream is = isOpt.get()) {
                            byte[] inMemoryBytes = is.readAllBytes();
                            if (inMemoryBytes != null && inMemoryBytes.length > 0) {
                                log.info("Successfully fetched {} bytes for assetId={} directly via in-memory AssetService",
                                        inMemoryBytes.length, assetId);
                                return inMemoryBytes;
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Direct in-memory AssetService download failed for assetId={}: {}", assetId, ex.getMessage());
                }
            }
        }

        // 2. Direct local filesystem check if running co-located or sharing storage volume
        byte[] localBytes = tryReadFromLocalStorage(assetId);
        if (localBytes != null && localBytes.length > 0) {
            log.debug("Successfully resolved assetId={} from local storage path ({})", assetId, assetStorageBasePath);
            return localBytes;
        }

        // 3. Fetch via asset-service REST endpoint
        try {
            log.info("Fetching binary stream for assetId={} from asset-service", assetId);
            byte[] remoteBytes = restClient.get()
                    .uri("/api/v1/assets/{id}/download", assetId)
                    .header(SharedConstants.HEADER_TENANT_ID, tenantId != null ? tenantId : SharedConstants.DEFAULT_TENANT)
                    .retrieve()
                    .body(byte[].class);

            if (remoteBytes != null && remoteBytes.length > 0) {
                return remoteBytes;
            }
        } catch (Exception ex) {
            log.error("Failed to download asset bytes from asset-service for assetId={}: {}", assetId, ex.getMessage(), ex);
            throw new IllegalStateException("Could not download Question Paper PDF from asset-service for assetId: " + assetId, ex);
        }

        throw new IllegalStateException("Asset-service returned empty payload for assetId: " + assetId);
    }

    /**
     * Registers a reference link in asset-service associating the asset with the Question Paper entity.
     *
     * @param assetId UUID of the asset
     * @param entityType entity type identifier (e.g. QUESTION_PAPER)
     * @param entityId UUID of the entity referencing the asset
     * @param tenantId tenant identifier
     */
    public void createReference(UUID assetId, String entityType, UUID entityId, String tenantId) {
        if (assetId == null || entityId == null) {
            return;
        }

        // 1. In-memory ReferenceService check (monolith mode)
        if (referenceServiceProvider != null) {
            ReferenceService directRefService = referenceServiceProvider.getIfAvailable();
            if (directRefService != null) {
                try {
                    ReferenceType refType = ReferenceType.INSTRUCTION;
                    AssetReferenceRequest refReq = AssetReferenceRequest.builder()
                            .assetId(assetId)
                            .referenceType(refType)
                            .referenceId(entityId)
                            .build();
                    directRefService.addReference(refReq, null, tenantId);
                    log.debug("Registered asset reference in-memory for assetId={} -> entityId={}", assetId, entityId);
                    return;
                } catch (Exception ex) {
                    log.warn("In-memory asset reference registration skipped for assetId={}: {}", assetId, ex.getMessage());
                }
            }
        }

        // 2. REST fallback
        try {
            Map<String, Object> payload = Map.of(
                    "assetId", assetId.toString(),
                    "entityType", entityType != null ? entityType : "QUESTION_PAPER",
                    "entityId", entityId.toString(),
                    "referenceType", "SOURCE_DOCUMENT"
            );
            restClient.post()
                    .uri("/api/v1/assets/references")
                    .header(SharedConstants.HEADER_TENANT_ID, tenantId != null ? tenantId : SharedConstants.DEFAULT_TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Registered asset reference for assetId={} -> entityId={}", assetId, entityId);
        } catch (Exception ex) {
            log.warn("Non-critical: Failed to register asset reference for assetId={}: {}", assetId, ex.getMessage());
        }
    }

    /**
     * Computes the hexadecimal SHA-256 hash string for the provided byte buffer.
     *
     * @param bytes binary data
     * @return 64-character lowercase hexadecimal hash
     */
    public String computeSha256(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest algorithm unavailable", e);
        }
    }

    private byte[] tryReadFromLocalStorage(UUID assetId) {
        if (assetStorageBasePath == null || assetStorageBasePath.isBlank()) {
            return null;
        }
        try {
            Path basePath = Paths.get(assetStorageBasePath);
            if (!Files.exists(basePath)) {
                return null;
            }
            // Check direct file {basePath}/{assetId} or {basePath}/{assetId}.pdf
            Path directPath = basePath.resolve(assetId.toString());
            if (Files.isRegularFile(directPath)) {
                return Files.readAllBytes(directPath);
            }
            Path pdfPath = basePath.resolve(assetId + ".pdf");
            if (Files.isRegularFile(pdfPath)) {
                return Files.readAllBytes(pdfPath);
            }
            // Check within subdirectories (e.g. tenant-prefixed or date-prefixed)
            File[] matching = basePath.toFile().listFiles((dir, name) -> name.startsWith(assetId.toString()));
            if (matching != null && matching.length > 0 && matching[0].isFile()) {
                return Files.readAllBytes(matching[0].toPath());
            }
        } catch (IOException ex) {
            log.trace("Local storage probe for assetId={} skipped: {}", assetId, ex.getMessage());
        }
        return null;
    }
}
