package com.examplatform.asset.dto;

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
}
