package com.examplatform.examination.exception;

import java.time.LocalDate;

/**
 * Thrown when a proposed schedule date (exam or reserve) conflicts with
 * an existing schedule in the same tenant (Req 7b.10, 7b.11).
 */
public class ScheduleDateConflictException extends RuntimeException {

    public ScheduleDateConflictException(LocalDate date, String conflictType) {
        super(String.format(
                "Schedule date conflict: %s conflicts with an existing %s for this tenant",
                date, conflictType));
    }
}
