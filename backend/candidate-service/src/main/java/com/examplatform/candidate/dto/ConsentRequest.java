package com.examplatform.candidate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for recording candidate consent before biometric data collection.
 *
 * Validates: Requirements 25.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    @NotNull
    private boolean consentGiven; // must be true to proceed
}
