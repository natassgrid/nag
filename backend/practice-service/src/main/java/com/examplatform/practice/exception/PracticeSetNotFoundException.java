// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.exception;
import java.util.UUID;
public class PracticeSetNotFoundException extends RuntimeException {
    public PracticeSetNotFoundException(UUID id) {
        super("Practice set not found: " + id);
    }
}
