// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record StartSessionRequest(
    @NotNull UUID practiceSetId,
    String mode
) {}
