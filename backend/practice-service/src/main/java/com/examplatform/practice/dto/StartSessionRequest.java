// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartSessionRequest(
    @NotNull UUID practiceSetId,
    String mode,
    String preferredLanguage
) {
    public StartSessionRequest(@NotNull UUID practiceSetId, String mode) {
        this(practiceSetId, mode, "en");
    }
}
