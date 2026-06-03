package com.examplatform.examination.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an examination is not found by its identifier.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExaminationNotFoundException extends RuntimeException {

    public ExaminationNotFoundException(UUID examId) {
        super(String.format("Examination not found: %s", examId));
    }
}
