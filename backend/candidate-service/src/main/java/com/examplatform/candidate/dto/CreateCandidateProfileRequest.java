package com.examplatform.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new candidate profile.
 *
 * Validates: Requirements 1.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCandidateProfileRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String dateOfBirth;

    @NotBlank
    private String gender;

    @NotBlank
    private String nationality;

    private String category;

    @NotBlank
    private String mobile;

    @NotBlank
    private String email;

    private String address;

    private String reservationCategory;

    @NotBlank
    private String identityDocNumber;
}
