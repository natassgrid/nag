package com.examplatform.candidate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for face verification.
 * Contains embedding vectors for the submitted photograph and the identity document photograph.
 *
 * Validates: Requirements 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerificationRequest {

    @NotNull
    private UUID userId;

    /**
     * Embedding vector of the submitted candidate photograph.
     */
    @NotNull
    private float[] photoEmbedding;

    /**
     * Embedding vector of the photograph from the identity document.
     */
    @NotNull
    private float[] docPhotoEmbedding;
}
