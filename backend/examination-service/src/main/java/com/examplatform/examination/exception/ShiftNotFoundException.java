package com.examplatform.examination.exception;

import java.util.UUID;

public class ShiftNotFoundException extends RuntimeException {
    public ShiftNotFoundException(UUID shiftId) {
        super("Exam shift not found: " + shiftId);
    }
}
