package com.examplatform.papergenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a saved blueprint template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID examId;
    private List<BlueprintRule> rules;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /** Convenience: total question count across all rules. */
    public int getTotalQuestions() {
        if (rules == null) return 0;
        return rules.stream().mapToInt(BlueprintRule::getQuestionCount).sum();
    }
}
