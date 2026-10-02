// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.exception;
import java.util.UUID;
public class SessionAlreadySubmittedException extends RuntimeException {
    public SessionAlreadySubmittedException(UUID sessionId) {
        super("Practice session already submitted: " + sessionId);
    }
}
