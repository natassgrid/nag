package com.examplatform.questionbank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the four-eyes principle is violated — the reviewer
 * cannot also be the approver (the actor who transitions to PUBLISHED).
 *
 * Validates: Requirements 5.5
 */
@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class FourEyesPrincipleViolationException extends RuntimeException {

    public FourEyesPrincipleViolationException() {
        super("Four-eyes principle violation: reviewer cannot also approve publication");
    }
}
