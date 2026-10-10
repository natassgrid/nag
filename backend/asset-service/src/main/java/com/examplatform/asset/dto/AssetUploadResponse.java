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

package com.examplatform.asset.dto;

import com.examplatform.asset.domain.entity.MediaAsset;
import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO returned after a successful asset upload or retrieval.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetUploadResponse {

    private UUID id;
    private String originalFilename;
    private String contentType;
    private String extension;
    private Long fileSize;
    private String sha256Hash;
    private AssetType assetType;
    private AssetStatus status;

    /** Accessible public or direct download URL for embedding media in questions/options. */
    private String publicUrl;

    // Media metadata
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String orientation;
    private Double durationSeconds;
    private String codec;
    private Integer bitrate;
    private Integer sampleRate;
    private Integer channels;
    private Double frameRate;

    // User metadata
    private String title;
    private String description;
    private String altText;
    private String tags;
    private String language;

    // Storage
    private String storageProvider;
    private String storageLocation;

    // Audit
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private String tenantId;

    public static AssetUploadResponse fromEntity(MediaAsset asset, String publicUrl) {
        if (asset == null) {
            return null;
        }
        AssetUploadResponse resp = new AssetUploadResponse();
        resp.setId(asset.getId());
        resp.setOriginalFilename(asset.getOriginalFilename());
        resp.setContentType(asset.getContentType());
        resp.setExtension(asset.getExtension());
        resp.setFileSize(asset.getFileSize());
        resp.setSha256Hash(asset.getSha256Hash());
        resp.setAssetType(asset.getAssetType());
        resp.setStatus(asset.getStatus());
        resp.setPublicUrl(publicUrl);
        resp.setWidth(asset.getWidth());
        resp.setHeight(asset.getHeight());
        resp.setDpi(asset.getDpi());
        resp.setOrientation(asset.getOrientation());
        resp.setDurationSeconds(asset.getDurationSeconds());
        resp.setCodec(asset.getCodec());
        resp.setBitrate(asset.getBitrate());
        resp.setSampleRate(asset.getSampleRate());
        resp.setChannels(asset.getChannels());
        resp.setFrameRate(asset.getFrameRate());
        resp.setTitle(asset.getTitle());
        resp.setDescription(asset.getDescription());
        resp.setAltText(asset.getAltText());
        resp.setTags(asset.getTags());
        resp.setLanguage(asset.getLanguage());
        resp.setStorageProvider(asset.getStorageProvider());
        resp.setStorageLocation(asset.getStorageLocation());
        resp.setCreatedBy(asset.getCreatedBy());
        resp.setCreatedAt(asset.getCreatedAt());
        resp.setUpdatedAt(asset.getUpdatedAt());
        resp.setTenantId(asset.getTenantId());
        return resp;
    }
}
