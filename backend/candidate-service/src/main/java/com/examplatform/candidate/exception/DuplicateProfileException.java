package com.examplatform.candidate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a candidate profile creation is attempted with a duplicate
 * mobile number or identity document number.
 *
 * Validates: Requirements 1.6
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateProfileException extends RuntimeException {

    public DuplicateProfileException(String message) {
        super(message);
    }
}
