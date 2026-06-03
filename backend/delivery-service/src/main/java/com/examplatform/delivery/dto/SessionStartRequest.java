package com.examplatform.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for starting an exam session.
 * The candidate JWT provides the candidateId — this DTO carries the exam/shift selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartRequest {

    @NotNull
    private UUID examId;

    @NotNull
    private UUID shiftId;

    @Builder.Default
    private String languageCode = "en";
}
