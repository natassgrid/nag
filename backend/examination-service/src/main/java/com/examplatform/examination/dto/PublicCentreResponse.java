/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Publicly viewable examination centre information for candidate preference selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCentreResponse {

    private UUID id;
    private String centreName;
    private String region;
    private String state;
    private String district;
    private String city;
    private String building;
    private Integer totalCapacity;
}
