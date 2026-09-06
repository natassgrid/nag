/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for exam application submitted by a candidate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamApplicationResponse {

    private UUID applicationId;
    private UUID examId;
    private UUID candidateId;

    /** APPLIED | CONFIRMED | REJECTED */
    private String status;

    private LocalDateTime applicationDate;
    private String hallTicketNumber;
    private String examName;
    private String examCode;
    private String conductingAuthority;
    private Integer durationMinutes;
    private Integer totalMarks;

    // Allocated Details
    private UUID allocatedCentreId;
    private String centreName;
    private String city;
    private String state;
    private LocalDate examDate;
    private String shiftName;
}
