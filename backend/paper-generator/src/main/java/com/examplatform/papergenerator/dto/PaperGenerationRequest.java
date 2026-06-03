package com.examplatform.papergenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for blueprint-driven paper generation.
 * Submitted by an Exam Controller to generate a question paper
 * satisfying subject/topic/difficulty/cognitive ratios.
 *
 * Validates: Requirements 8.1, 8.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperGenerationRequest {

    @NotNull
    private UUID examId;

    @NotBlank
    private String shiftId;

    @NotEmpty
    private List<BlueprintRule> blueprintRules;
}
