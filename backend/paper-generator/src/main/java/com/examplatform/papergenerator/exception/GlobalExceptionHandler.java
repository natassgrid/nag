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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler for the Paper Generator Service.
 * Returns RFC 7807 Problem Detail responses for all error conditions.
 *
 * Validates: Requirements 8.5
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.examplatform.papergenerator")
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
    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String title, String typeSlug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:examplatform:error:" + typeSlug));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "Resource Not Found", "not-found");
    }

    /**
     * Handles access denied (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Access denied", "Forbidden", "forbidden");
    }

    /**
     * Handles validation errors (400).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "Bad Request", "validation");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        return problem;
    }

    /**
     * Handles method argument type mismatch (400), e.g. invalid UUID format in path variable.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: name={}, value={}, requiredType={}",
                ex.getName(), ex.getValue(), ex.getRequiredType());
        String message = String.format("Invalid parameter '%s': value '%s' is not valid for type %s",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildProblemDetail(HttpStatus.BAD_REQUEST, message, "Bad Request", "type-mismatch");
    }

    /**
     * Handles illegal argument exceptions (400).
     * Also covers duplicate-name conflicts for blueprint templates.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        boolean isConflict = ex.getMessage() != null && ex.getMessage().contains("already exists");
        return buildProblemDetail(
                isConflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                isConflict ? "Conflict" : "Bad Request",
                isConflict ? "conflict" : "bad-request"
        );
    }

    /**
     * Handles illegal state exceptions (409).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "Conflict", "conflict");
    }

    /**
     * Handles all other unhandled exceptions (500).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "Internal Server Error", "internal");
    }
}
