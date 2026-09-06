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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed admit card / hall ticket representation for candidates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmitCardResponse {

    private UUID applicationId;
    private String hallTicketNumber;
    private UUID candidateId;
    private String candidateName;
    private UUID examId;
    private String examName;
    private String examCode;
    private String conductingAuthority;
    private String examinationMode;
    private Integer durationMinutes;
    private Integer totalMarks;

    // Schedule & Shift
    private LocalDate examDate;
    private String shiftName;
    private Integer shiftNumber;
    private LocalTime reportingTime;
    private LocalTime gateClosingTime;
    private LocalTime loginStartTime;
    private LocalTime examStartTime;
    private LocalTime examEndTime;

    // Centre / Venue Details
    private UUID centreId;
    private String centreName;
    private String building;
    private String floor;
    private String city;
    private String state;
    private String laboratoryIdentifier;

    // Verification & Rules
    private String qrData;
    private String verificationHash;
    private Boolean pwdRequired;
    private Boolean scribeRequired;
    private List<String> instructions;
}
