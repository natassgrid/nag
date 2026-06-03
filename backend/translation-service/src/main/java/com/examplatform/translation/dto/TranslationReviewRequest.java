package com.examplatform.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for rejecting a translation with comments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationReviewRequest {

    @NotNull(message = "reviewerId is required")
    private UUID reviewerId;

    @NotBlank(message = "comments are required for rejection")
    private String comments;
}
