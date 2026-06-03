package com.examplatform.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateIdentityException extends RuntimeException {
    public DuplicateIdentityException(String message) {
        super(message);
    }
}
