/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when an unverified candidate/user attempts authentication before completing OTP verification.
 */
@Getter
public class AccountNotVerifiedException extends AuthenticationException {

    private final UUID userId;
    private final String email;

    public AccountNotVerifiedException(String message, UUID userId, String email) {
        super(message);
        this.userId = userId;
        this.email = email;
    }
}
