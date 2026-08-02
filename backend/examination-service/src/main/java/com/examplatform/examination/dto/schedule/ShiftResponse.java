package com.examplatform.examination.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for a single shift within an examination schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private UUID id;
    private UUID scheduleId;
    private int shiftNumber;
    private String shiftName;
    private LocalTime reportingTime;
    private LocalTime gateClosingTime;
    private LocalTime loginStartTime;
    private LocalTime examStartTime;
    private LocalTime examEndTime;
    private LocalTime exitTime;
    private int durationMinutes;
    private int bufferMinutes;
    private Instant createdAt;
    private Instant updatedAt;
}
