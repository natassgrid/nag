package com.examplatform.asset.dto;

import com.examplatform.asset.domain.enums.ReferenceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating an asset reference.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetReferenceRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotNull(message = "Reference type is required")
    private ReferenceType referenceType;

    @NotNull(message = "Reference ID is required")
    private UUID referenceId;
}
