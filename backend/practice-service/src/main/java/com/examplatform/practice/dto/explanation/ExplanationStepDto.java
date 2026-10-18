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
public class ExplanationStepDto {
    private String id;
    private Integer sequence;
    private String type;
    private List<String> inputs;
    private Map<String, Object> operation;
    private String before;
    private String after;
    private List<String> outputs;
    private Map<String, Object> justification;
    private List<String> representations;
    private Map<String, Object> math;
    private Map<String, Object> verification;
    private ExplanationMisconceptionDto misconception;
}
