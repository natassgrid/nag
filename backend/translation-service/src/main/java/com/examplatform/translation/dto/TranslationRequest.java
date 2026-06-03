package com.examplatform.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for requesting a new translation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequest {

    @NotNull(message = "questionId is required")
    private UUID questionId;

    @NotBlank(message = "languageCode is required")
    private String languageCode;

    @NotNull(message = "translatorId is required")
    private UUID translatorId;
}
