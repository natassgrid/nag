package com.examplatform.asset.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user-supplied metadata on an existing asset.
 * Only metadata fields are mutable; binary content is immutable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetMetadataUpdateRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 1000, message = "Alt text must not exceed 1000 characters")
    private String altText;

    @Size(max = 2000, message = "Tags must not exceed 2000 characters")
    private String tags;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String language;
}
