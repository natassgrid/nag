package com.examplatform.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a candidate attempts to start a new exam session while
 * already having an ACTIVE session within the same tenant.
 * Enforces the single concurrent session invariant.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentSessionException extends RuntimeException {

    public ConcurrentSessionException(String message) {
        super(message);
    }
}
