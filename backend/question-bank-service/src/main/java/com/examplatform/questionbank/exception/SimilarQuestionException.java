package com.examplatform.questionbank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a new question is too similar to an existing PUBLISHED question.
 * Returns HTTP 422 Unprocessable Entity with the ID of the similar question.
 *
 * Validates: Requirement 4.7
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SimilarQuestionException extends RuntimeException {

    private final UUID similarQuestionId;

    public SimilarQuestionException(UUID similarQuestionId) {
        super("Question is too similar to existing published question: " + similarQuestionId);
        this.similarQuestionId = similarQuestionId;
    }

    public UUID getSimilarQuestionId() {
        return similarQuestionId;
    }
}
