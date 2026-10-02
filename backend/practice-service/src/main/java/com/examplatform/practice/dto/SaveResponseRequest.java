// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record SaveResponseRequest(
    @NotNull UUID questionId,
    String selectedOptionIds,
    String enteredValue,
    long timeSpentMs,
    boolean markedForReview
) {}
