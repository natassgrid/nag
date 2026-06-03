package com.examplatform.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a candidate's disability-based time extension details.
 * Retrieved from candidate-service via {@link com.examplatform.delivery.client.CandidateProfileClient}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExtension {

    private int extraTimeMinutes;
    private String disabilityType;
}
