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

package com.examplatform.questionbank.exception;

import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.error.BaseApiResponseExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for the question-bank-service.
 * Converts exceptions into structured ApiResponse error envelopes.
 *
 * Validates: Requirements 4.1, 4.2
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.examplatform.questionbank")
public class GlobalExceptionHandler extends BaseApiResponseExceptionHandler {

    /**
     * Handles illegal state exceptions (e.g. invalid state for deletion) (409 Conflict).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles entity not found (404 Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Spring ResponseStatusException.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            log.debug("Resource not found: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        } else {
            log.warn("Response status exception: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        }
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    /**
     * Handles invalid lifecycle state transitions (422 Unprocessable Entity).
     */
    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransition(InvalidTransitionException ex) {
        log.warn("Invalid transition: from='{}' to='{}'", ex.getCurrentState(), ex.getTargetState());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles four-eyes principle violations (403 Forbidden).
     */
    @ExceptionHandler(FourEyesPrincipleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleFourEyesViolation(FourEyesPrincipleViolationException ex) {
        log.warn("Four-eyes principle violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
