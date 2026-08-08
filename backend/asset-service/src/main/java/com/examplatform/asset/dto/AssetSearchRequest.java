package com.examplatform.asset.dto;

import com.examplatform.asset.domain.enums.AssetStatus;
import com.examplatform.asset.domain.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Search/filter criteria for listing media assets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSearchRequest {

    private String filename;
    private AssetType assetType;
    private String contentType;
    private String tags;
    private String createdBy;
    private Instant uploadDateFrom;
    private Instant uploadDateTo;
    private AssetStatus status;
    private Boolean referenced;
    private String storageProvider;
}
