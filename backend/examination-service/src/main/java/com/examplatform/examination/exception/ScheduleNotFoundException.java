package com.examplatform.examination.exception;

import java.util.UUID;

public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException(UUID scheduleId) {
        super("Examination schedule not found: " + scheduleId);
    }
}
