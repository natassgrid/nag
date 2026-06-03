package com.examplatform.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a candidate attempts a navigation action that violates
 * the exam session's navigation policy (Sequential, Flexible, Restricted).
 * Returns HTTP 422 Unprocessable Entity.
 *
 * Validates: Requirements 9.2, 9.5
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class NavigationPolicyViolationException extends RuntimeException {

    public NavigationPolicyViolationException(String message) {
        super(message);
    }
}
