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
public class ExplanationPolicyDto {
    private String level;
    private Boolean showWhy;
    private Boolean showVerification;
    private String animation;
    private Boolean showEveryCalculation;
    private Boolean showFormula;
}
