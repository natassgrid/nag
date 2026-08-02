package com.examplatform.examination.exception;

import lombok.Getter;

/**
 * Thrown when a schedule state transition is invalid (wrong role, wrong current
 * state, or missing mandatory field such as changeReason on amendment).
 */
@Getter
public class ScheduleWorkflowException extends RuntimeException {

    private final String currentStatus;
    private final String targetStatus;

    public ScheduleWorkflowException(String currentStatus, String targetStatus) {
        super(String.format(
                "Invalid schedule transition: %s → %s is not permitted",
                currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public ScheduleWorkflowException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }
}
