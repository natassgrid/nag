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

package com.examplatform.identity.exception;

import com.examplatform.shared.error.ProblemDetailBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Global exception handler for the Identity Service.
 * Translates exceptions into RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle authentication failures (bad credentials, locked/deactivated account, device mismatch).
     * Returns HTTP 401 Unauthorized.
     *
     * @param ex the authentication exception
     * @return 401 Unauthorized with problem detail
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.UNAUTHORIZED)
                .withTitle("Unauthorized")
                .withDetail(ex.getMessage())
                .build();
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    /**
     * Handle MFA-required signal — returned when the account requires MFA but no OTP was provided.
     * Returns HTTP 403 Forbidden with {@code mfaRequired: true} to allow clients to prompt for OTP.
     *
     * @param ex the MFA required exception
     * @return 403 Forbidden with {@code mfaRequired} property set to {@code true}
     */
    @ExceptionHandler(MfaRequiredException.class)
    public ResponseEntity<ProblemDetail> handleMfaRequiredException(MfaRequiredException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.FORBIDDEN)
                .withTitle("MFA Required")
                .withDetail(ex.getMessage())
                .withProperty("mfaRequired", true)
                .build();
        log.debug("MFA required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    /**
     * Handle duplicate identity document or email during registration.
     * Returns HTTP 409 Conflict.
     *
     * @param ex the duplicate identity exception
     * @return 409 Conflict with problem detail
     */
    @ExceptionHandler(DuplicateIdentityException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateIdentity(DuplicateIdentityException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.CONFLICT)
                .withTitle("Duplicate Identity")
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    /**
     * Handle Bean Validation failures on {@code @Valid}-annotated request bodies.
     * Returns HTTP 400 with a problem detail listing each field and its error message.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetailBuilder
                .forStatus(HttpStatus.BAD_REQUEST)
                .withTitle("Validation Failed")
                .withDetail("One or more request fields failed validation.")
                .withProperty("fieldErrors", fieldErrors)
                .build();

        log.debug("Validation failure: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle Spring Security access denied (authorisation) failures.
     * Returns HTTP 403 without exposing internal details.
     *
     * @param ex the access-denied exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetailBuilder
                .forStatus(HttpStatus.FORBIDDEN)
                .withTitle("Access Denied")
                .withDetail("You do not have permission to perform this action.")
                .build();

        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Handle OTP validation failure (invalid or expired OTP, already-activated account).
     * Returns HTTP 422 Unprocessable Entity.
     *
     * @param ex the invalid OTP exception
     * @return 422 Unprocessable Entity with problem detail
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ProblemDetail> handleInvalidOtp(InvalidOtpException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .withTitle("Invalid OTP")
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    /**
     * Handle account not found (no matching pending account for the mobile number).
     * Returns HTTP 404 Not Found.
     *
     * @param ex the account not found exception
     * @return 404 Not Found with problem detail
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.NOT_FOUND)
                .withTitle("Account Not Found")
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    /**
     * Handle rate limit exceeded (too many auth attempts from the same IP).
     * Returns HTTP 429 Too Many Requests with a Retry-After header.
     *
     * @param ex the rate limit exceeded exception
     * @return 429 Too Many Requests with problem detail
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {
        ProblemDetail pd = ProblemDetailBuilder.forStatus(HttpStatus.TOO_MANY_REQUESTS)
                .withTitle("Too Many Requests")
                .withDetail(ex.getMessage())
                .withProperty("retryAfterSeconds", 60)
                .build();
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(pd);
    }

    /**
     * Catch-all handler for any unhandled exception.
     * Returns HTTP 500 and logs the full stack trace.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetailBuilder
                .forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .withTitle("Internal Server Error")
                .withDetail("An unexpected error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
