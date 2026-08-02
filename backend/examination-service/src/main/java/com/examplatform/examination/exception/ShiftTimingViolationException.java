package com.examplatform.examination.exception;

import lombok.Getter;

/**
 * Thrown when a shift's timing fields violate one of the ordering invariants
 * defined in Requirement 7b.3.
 */
@Getter
public class ShiftTimingViolationException extends RuntimeException {

    /** The name of the constraint that was violated (e.g. "reportingTime < gateClosingTime"). */
    private final String violatedConstraint;

    public ShiftTimingViolationException(String violatedConstraint, String detail) {
        super("Shift timing invariant violated [" + violatedConstraint + "]: " + detail);
        this.violatedConstraint = violatedConstraint;
    }
}
