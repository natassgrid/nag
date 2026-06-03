package com.examplatform.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a client exceeds the rate limit for authentication attempts.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
