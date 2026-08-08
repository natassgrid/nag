package com.examplatform.examination.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transitioning an examination schedule through the
 * approval workflow. Optionally includes an approval comment.
 * Validates: Requirements 7b.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTransitionRequest {

    /**
     * Target status to transition to.
     * Must be a valid next state per the approval FSM:
     * DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED →
     * SECURITY_REVIEW → CHAIRMAN_APPROVED → PUBLISHED | CANCELLED
     */
    @NotBlank
    private String targetStatus;

    /** Optional free-text comment recorded with the approval action. */
    private String comment;
}
