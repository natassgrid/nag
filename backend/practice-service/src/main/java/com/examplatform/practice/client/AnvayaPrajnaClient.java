// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.examplatform.practice.dto.explanation.GenerateExplanationRequestDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for interacting with the Anvaya-Prajna Explanation Engine (Issue #348).
 */
public interface AnvayaPrajnaClient {

    Optional<ExplanationIrDto> getExplanation(UUID questionId);

    Optional<ExplanationIrDto> generateExplanation(GenerateExplanationRequestDto request);
}
