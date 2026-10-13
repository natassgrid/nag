// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto.explanation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplanationHintDto {
    private String id;
    private Integer level;
    private String text;
    private String mathExpression;
}
