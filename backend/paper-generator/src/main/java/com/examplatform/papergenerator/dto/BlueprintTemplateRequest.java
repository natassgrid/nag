package com.examplatform.papergenerator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating or updating a blueprint template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintTemplateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    /** Optional: pin this template to a specific exam. */
    private UUID examId;

    @NotEmpty
    @Valid
    private List<BlueprintRule> rules;
}
