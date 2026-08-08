package com.examplatform.examination.exception;

import java.util.UUID;

public class CentreNotFoundException extends RuntimeException {
    public CentreNotFoundException(UUID centreId) {
        super("Examination centre not found: " + centreId);
    }
}
