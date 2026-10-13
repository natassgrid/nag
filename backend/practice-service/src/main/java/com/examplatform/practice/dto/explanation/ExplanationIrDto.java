// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto.explanation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplanationIrDto {
    @Builder.Default
    private String schemaVersion = "1.0";
    private String explanationId;
    private String questionId;
    private String status;
    private List<String> concepts;
    private List<String> facts;
    private List<String> assumptions;
    private Map<String, Object> reasoningGraph;
    private List<ExplanationStepDto> steps;
    private Map<String, Object> verification;
    private List<ExplanationHintDto> hints;
    private List<ExplanationMisconceptionDto> misconceptions;
    private List<Map<String, Object>> diagrams;
    private Map<String, Object> animation;
    private Map<String, Object> metadata;
}
