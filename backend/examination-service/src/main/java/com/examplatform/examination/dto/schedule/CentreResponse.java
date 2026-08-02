package com.examplatform.examination.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an examination centre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentreResponse {

    private UUID id;
    private String region;
    private String state;
    private String district;
    private String city;
    private String centreName;
    private String building;
    private String floor;
    private String laboratoryIdentifier;
    private int totalCapacity;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
