package com.examplatform.examination.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the sum of section marks (marksPerQuestion × questionCount)
 * does not equal the declared totalMarks of the examination.
 *
 * Validates: Requirement 7.6
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SectionMarksValidationException extends RuntimeException {

    private final int expectedTotalMarks;
    private final double actualTotalMarks;

    public SectionMarksValidationException(int expected, double actual) {
        super(String.format(
                "Section marks mismatch: expected totalMarks=%d, but sum(marksPerQuestion × questionCount) = %.2f",
                expected, actual));
        this.expectedTotalMarks = expected;
        this.actualTotalMarks = actual;
    }
}
