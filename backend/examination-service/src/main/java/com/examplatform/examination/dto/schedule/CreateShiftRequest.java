package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request DTO for adding a shift to an examination schedule.
 * All timing invariants are enforced in the service layer.
 * Validates: Requirements 7b.2, 7b.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftRequest {

    @Min(1)
    private int shiftNumber;

    @Size(max = 100)
    private String shiftName;

    @NotNull
    private LocalTime reportingTime;

    @NotNull
    private LocalTime gateClosingTime;

    @NotNull
    private LocalTime loginStartTime;

    @NotNull
    private LocalTime examStartTime;

    @NotNull
    private LocalTime examEndTime;

    private LocalTime exitTime;

    @Min(1)
    private int durationMinutes;

    @Min(0)
    private int bufferMinutes;
}
