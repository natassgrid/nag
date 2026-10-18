/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.exception;

import com.examplatform.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class LearnerDomainException extends DomainException {

    public LearnerDomainException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "LEARNER_VALIDATION_ERROR");
    }

    public LearnerDomainException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }
}
