/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resending an OTP to a pending registration or unverified account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResendRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    private String email;

    private String mobile;
}
