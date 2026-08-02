package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an examination centre.
 * Validates: Requirements 7b.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCentreRequest {

    @Size(max = 100)
    private String region;

    @NotBlank
    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 255)
    private String centreName;

    @Size(max = 255)
    private String building;

    @Size(max = 50)
    private String floor;

    @Size(max = 100)
    private String laboratoryIdentifier;

    @Min(0)
    private int totalCapacity;

    @Builder.Default
    private boolean active = true;
}
