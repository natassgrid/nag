package com.examplatform.candidate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a candidate profile cannot be found for the given user ID and tenant.
 *
 * Validates: Requirements 1.6
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(String message) {
        super(message);
    }
}
