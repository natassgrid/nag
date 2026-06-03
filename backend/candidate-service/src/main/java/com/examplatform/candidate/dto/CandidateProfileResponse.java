package com.examplatform.candidate.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for candidate profile with masked PII fields.
 *
 * Validates: Requirements 1.6
 */
@Data
@Builder
public class CandidateProfileResponse {

    private UUID userId;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String category;
    private String mobile;       // masked: last 4 digits only
    private String email;        // masked
    private String address;
    private String reservationCategory;
    private String digiLockerVerified;
    private String faceVerificationStatus;
    private boolean consentRecorded;
}
