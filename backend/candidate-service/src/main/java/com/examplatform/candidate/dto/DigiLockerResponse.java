package com.examplatform.candidate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the response from DigiLocker API.
 *
 * Validates: Requirements 1.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerResponse {

    private String status;

    private String documentData;

    private String issuerId;
}
