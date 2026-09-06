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
 * Request payload for applying to an examination with centre and shift preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamApplicationRequest {

    private UUID firstChoiceCentreId;
    private UUID secondChoiceCentreId;
    private UUID thirdChoiceCentreId;
    private UUID preferredShiftId;
    private Boolean pwdRequired;
    private Boolean scribeRequired;
}
