package com.examplatform.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a candidate's assignment to a specific shift.
 * Retrieved from examination-service via {@link com.examplatform.delivery.client.ShiftAssignmentClient}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignment {

    private UUID paperId;
    private String encryptedPackageRef;
    private int durationMinutes;
    private int extraTimeMinutes;
}
