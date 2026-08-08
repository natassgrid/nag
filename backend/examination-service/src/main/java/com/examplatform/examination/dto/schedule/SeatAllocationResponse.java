package com.examplatform.examination.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for seat allocation for a shift at a centre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationResponse {

    private UUID id;
    private UUID shiftId;
    private UUID centreId;
    private int totalSeats;
    private int availableSeats;
    private int reservedSeats;
    private int pwdSeats;
    private int emergencyBufferSeats;
    private int femaleReservedSeats;
    private int specialCategorySeats;
    private Instant createdAt;
    private Instant updatedAt;
}
