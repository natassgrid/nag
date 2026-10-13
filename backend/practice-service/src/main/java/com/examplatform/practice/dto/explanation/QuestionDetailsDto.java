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
public class QuestionDetailsDto {
    private String questionId;
    private String statement;
    private String domain;
    private String difficulty;
    private List<String> concepts;
    private String authoritativeAnswer;
    private List<String> options;
    private Map<String, Object> metadata;
}
