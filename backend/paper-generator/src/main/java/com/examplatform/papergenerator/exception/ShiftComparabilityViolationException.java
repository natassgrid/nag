package com.examplatform.papergenerator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when shift comparability validation fails — i.e. the relative difficulty
 * difference between any pair of papers exceeds the 2% threshold.
 *
 * Validates: Requirements 8.9
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ShiftComparabilityViolationException extends RuntimeException {

    private final UUID examId;
    private final List<String> violations;

    public ShiftComparabilityViolationException(UUID examId, List<String> violations) {
        super("Shift comparability violated for exam " + examId + ": " + violations.size() + " violation(s)");
        this.examId = examId;
        this.violations = violations;
    }

    public UUID getExamId() {
        return examId;
    }

    public List<String> getViolations() {
        return violations;
    }
}
