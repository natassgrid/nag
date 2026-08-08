package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating or updating seat allocation for a shift at a centre.
 * Validates: Requirements 7b.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationRequest {

    @NotNull
    private UUID centreId;

    @Min(0)
    private int totalSeats;

    @Min(0)
    private int availableSeats;

    @Min(0)
    private int reservedSeats;

    @Min(0)
    private int pwdSeats;

    @Min(0)
    private int emergencyBufferSeats;

    @Min(0)
    private int femaleReservedSeats;

    @Min(0)
    private int specialCategorySeats;
}
