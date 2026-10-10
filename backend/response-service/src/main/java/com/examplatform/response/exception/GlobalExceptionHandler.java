/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU标志 Affero General Public License as published
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

package com.examplatform.response.exception;

import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.error.BaseApiResponseExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler for response-service.
 * Converts exceptions into structured ApiResponse error envelopes.
 *
 * Validates: Requirements 10.1, 20.3
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.examplatform.response")
public class GlobalExceptionHandler extends BaseApiResponseExceptionHandler {

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
     * Handles Kafka timeout / send failures (503 Service Unavailable).
     * This covers scenarios where Kafka acks=all cannot be confirmed within the timeout.
     */
    @ExceptionHandler({TimeoutException.class})
    public ResponseEntity<ApiResponse<Void>> handleKafkaTimeout(TimeoutException ex) {
        log.error("Kafka timeout: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Service temporarily unavailable — message broker timeout"));
    }

    /**
     * Handles Kafka execution failures (503 Service Unavailable).
     */
    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaExecutionFailure(ExecutionException ex) {
        log.error("Kafka send failed: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Service temporarily unavailable — message broker error"));
    }

    /**
     * Handles response integrity violations (422 Unprocessable Entity).
     */
    @ExceptionHandler(ResponseIntegrityException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrityViolation(ResponseIntegrityException ex) {
        log.warn("Response integrity violation [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getErrorCode() + ": " + ex.getMessage()));
    }

    /**
     * Handles duplicate session submissions (409 Conflict).
     */
    @ExceptionHandler(AlreadySubmittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadySubmitted(AlreadySubmittedException ex) {
        log.warn("Duplicate submission detected: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("ALREADY_SUBMITTED: " + ex.getMessage()));
    }

    /**
     * Handles session expiry on submission (422 Unprocessable Entity).
     */
    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionExpired(SessionExpiredException ex) {
        log.warn("Session expired submission rejected: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("SESSION_EXPIRED: " + ex.getMessage()));
    }

    /**
     * Handles RuntimeException wrapping Kafka failures (503 Service Unavailable).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Kafka")) {
            log.error("Kafka runtime error: {}", ex.getMessage(), ex);
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Service temporarily unavailable — message broker error"));
        }
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
