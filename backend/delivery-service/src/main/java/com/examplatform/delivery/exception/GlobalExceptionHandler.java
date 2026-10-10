/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.delivery.exception;

import com.examplatform.shared.error.BaseErrorEnvelopeExceptionHandler;
import com.examplatform.shared.error.ErrorEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the delivery-service REST API.
 * Maps domain exceptions to appropriate HTTP status codes and structured ErrorEnvelope responses.
 */
@RestControllerAdvice(basePackages = "com.examplatform.delivery")
public class GlobalExceptionHandler extends BaseErrorEnvelopeExceptionHandler {

    @ExceptionHandler(ConcurrentSessionException.class)
    public ResponseEntity<ErrorEnvelope> handleConcurrentSession(ConcurrentSessionException ex) {
        log.warn("Concurrent session violation: {}", ex.getMessage());
        String examId = ex.getActiveExamId() != null ? ex.getActiveExamId().toString() : null;
        String sessId = ex.getActiveSessionId() != null ? ex.getActiveSessionId().toString() : null;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorEnvelope.of("CONCURRENT_SESSION", ex.getMessage(), HttpStatus.CONFLICT.value(), examId, sessId));
    }

    @ExceptionHandler(NavigationPolicyViolationException.class)
    public ResponseEntity<ErrorEnvelope> handleNavigationPolicyViolation(NavigationPolicyViolationException ex) {
        log.warn("Navigation policy violation: {}", ex.getMessage());
        return buildEnvelope("NAVIGATION_POLICY_VIOLATION", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ErrorEnvelope> handleMissingRequestValues(Exception ex) {
        log.warn("Missing required request parameter or header: {}", ex.getMessage());
        return buildEnvelope("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
