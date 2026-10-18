// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StartSessionRequest(
        @NotNull UUID practiceSetId,
        String mode,
        String preferredLanguage,
        UUID enrollmentId,
        UUID examId
) {
    public StartSessionRequest(@NotNull UUID practiceSetId, String mode) {
        this(practiceSetId, mode, "en", null, null);
    }

    public StartSessionRequest(@NotNull UUID practiceSetId, String mode, String preferredLanguage) {
        this(practiceSetId, mode, preferredLanguage, null, null);
    }
}
