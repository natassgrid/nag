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

package com.examplatform.papergenerator.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for the Paper Generator Service.
 * Returns RFC 7807 Problem Detail responses for all error conditions.
 *
 * Validates: Requirements 8.5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles insufficient questions (422) with gap details.
     */
    @ExceptionHandler(InsufficientQuestionsException.class)
    public ProblemDetail handleInsufficientQuestions(InsufficientQuestionsException ex) {
        log.warn("Insufficient questions for paper generation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Insufficient Questions");
        problem.setType(URI.create("urn:examplatform:error:insufficient-questions"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("gapDetails", ex.getGapDetails());
        return problem;
    }

    /**
     * Handles entity not found (404).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("urn:examplatform:error:not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles access denied (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Forbidden");
        problem.setType(URI.create("urn:examplatform:error:forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles validation errors (400).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Bad Request");
        problem.setType(URI.create("urn:examplatform:error:validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        return problem;
    }

    /**
     * Handles illegal argument exceptions (400).
     * Also covers duplicate-name conflicts for blueprint templates.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        // Treat duplicate-name errors as 409 Conflict
        boolean isConflict = ex.getMessage() != null && ex.getMessage().contains("already exists");
        HttpStatus status = isConflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(isConflict ? "Conflict" : "Bad Request");
        problem.setType(URI.create(isConflict
                ? "urn:examplatform:error:conflict"
                : "urn:examplatform:error:bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles illegal state exceptions (409).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("urn:examplatform:error:conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles all other unhandled exceptions (500).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:examplatform:error:internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
