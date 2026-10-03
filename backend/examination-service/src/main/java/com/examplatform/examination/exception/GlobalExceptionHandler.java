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

package com.examplatform.examination.exception;

import com.examplatform.shared.error.ApiErrorResponse;
import com.examplatform.shared.error.BaseGlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for examination-service REST endpoints.
 */
@RestControllerAdvice(basePackages = "com.examplatform.examination")
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(SectionMarksValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleSectionMarksValidation(SectionMarksValidationException ex) {
        log.warn("Section marks validation failed: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .property("expectedTotalMarks", ex.getExpectedTotalMarks())
                .property("actualTotalMarks", ex.getActualTotalMarks())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ShiftTimingViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleShiftTiming(ShiftTimingViolationException ex) {
        log.warn("Shift timing violation: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Shift Timing Violation")
                .message(ex.getMessage())
                .property("violatedConstraint", ex.getViolatedConstraint())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ScheduleWorkflowException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleWorkflow(ScheduleWorkflowException ex) {
        log.warn("Schedule workflow violation: {}", ex.getMessage());
        ApiErrorResponse.Builder builder = ApiErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Invalid Schedule Transition")
                .message(ex.getMessage());
        if (ex.getCurrentStatus() != null) builder.property("currentStatus", ex.getCurrentStatus());
        if (ex.getTargetStatus() != null)  builder.property("targetStatus",  ex.getTargetStatus());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(builder.build());
    }

    @ExceptionHandler(ScheduleDateConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDateConflict(ScheduleDateConflictException ex) {
        log.warn("Schedule date conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SeatAllocationException.class)
    public ResponseEntity<ApiErrorResponse> handleSeatAllocation(SeatAllocationException ex) {
        log.warn("Seat allocation error: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({ScheduleNotFoundException.class, ShiftNotFoundException.class,
                        CentreNotFoundException.class, ExaminationNotFoundException.class,
                        EntityNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
