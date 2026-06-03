package com.examplatform.papergenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Serialized format POJO for paper documents.
 * Represents the JSON schema used for paper serialization/deserialization.
 * Contains only question identifiers (no question content) for security.
 *
 * Validates: Requirements 28.1, 28.2, 28.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperDocument {

    /**
     * Schema version for backward/forward compatibility.
     */
    @NotBlank
    @Builder.Default
    private String schemaVersion = "1.0";

    /**
     * The exam this paper belongs to.
     */
    @NotNull
    private UUID examId;

    /**
     * The shift this paper was generated for.
     */
    @NotNull
    private String shiftId;

    /**
     * Ordered list of question IDs (no content stored).
     */
    @NotEmpty
    private List<UUID> questionIds;

    /**
     * Computed difficulty score for the paper.
     */
    private double difficultyScore;

    /**
     * Timestamp when the paper was generated.
     */
    @NotNull
    private Instant generatedAt;
}
