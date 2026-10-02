// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.exception;
import java.util.UUID;
public class PracticeSessionNotFoundException extends RuntimeException {
    public PracticeSessionNotFoundException(UUID id) {
        super("Practice session not found: " + id);
    }
}
