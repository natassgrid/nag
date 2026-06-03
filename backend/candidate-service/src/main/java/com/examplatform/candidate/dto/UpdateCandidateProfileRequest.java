package com.examplatform.candidate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for partial update of a candidate profile.
 * All fields are nullable — only non-null fields are applied.
 *
 * Validates: Requirements 1.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidateProfileRequest {

    private String fullName;

    private String dateOfBirth;

    private String gender;

    private String nationality;

    private String category;

    private String mobile;

    private String email;

    private String address;

    private String reservationCategory;

    private String identityDocNumber;
}
