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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.asset.controller;

import com.examplatform.asset.validation.AssetValidationException;
import com.examplatform.shared.api.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for the Asset Service REST layer.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.examplatform.asset")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetExceptionHandler {

    private ResponseEntity<ApiResponse<Void>> errorResponse(HttpStatus status, String message, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletResponse response) {
        log.warn("Access denied: {}", ex.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, "Access denied", response);
    }

    @ExceptionHandler(AssetValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(AssetValidationException ex, HttpServletResponse response) {
        log.warn("Asset validation failed: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(EntityNotFoundException ex, HttpServletResponse response) {
        log.warn("Entity not found: {}", ex.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletResponse response) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds maximum allowed upload size", response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletResponse response) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Request validation failed: {}", message);
        return errorResponse(HttpStatus.BAD_REQUEST, message, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletResponse response) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletResponse response) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", response);
    }
}
