package com.examplatform.asset.dto;

import com.examplatform.asset.domain.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an asset reference.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetReferenceResponse {

    private UUID id;
    private UUID assetId;
    private ReferenceType referenceType;
    private UUID referenceId;
    private UUID createdBy;
    private Instant createdAt;
}
