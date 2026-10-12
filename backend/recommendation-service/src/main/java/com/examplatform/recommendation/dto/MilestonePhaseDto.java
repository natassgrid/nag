// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestonePhaseDto(
        int phaseNumber,
        String phaseName,
        String targetDescription,
        String status, // IN_PROGRESS, LOCKED, COMPLETED
        List<MilestoneItemDto> items
) implements Serializable {}
