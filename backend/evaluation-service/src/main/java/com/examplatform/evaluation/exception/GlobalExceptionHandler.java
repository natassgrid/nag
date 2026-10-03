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

package com.examplatform.evaluation.exception;

import com.examplatform.shared.error.ErrorEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Global exception handler for the evaluation-service REST API.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.examplatform.evaluation")
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorEnvelope> handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorEnvelope.of("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorEnvelope> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorEnvelope.of("INVALID_STATE", ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorEnvelope.of("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorEnvelope.of("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
