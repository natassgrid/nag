package com.examplatform.papergenerator.exception;

import com.examplatform.papergenerator.dto.GapDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * Thrown when a paper generation blueprint cannot be satisfied due to
 * insufficient questions available in the question bank.
 * Returns HTTP 422 Unprocessable Entity with gap details.
 *
 * Validates: Requirements 8.5
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientQuestionsException extends RuntimeException {

    private final List<GapDetail> gapDetails;

    public InsufficientQuestionsException(String message, List<GapDetail> gapDetails) {
        super(message);
        this.gapDetails = gapDetails;
    }

    public List<GapDetail> getGapDetails() {
        return gapDetails;
    }
}
