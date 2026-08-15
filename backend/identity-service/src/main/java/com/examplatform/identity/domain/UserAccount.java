/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.identity.domain;

import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.domain.enums.IdentityDocType;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_account", schema = "identity_service")
public class UserAccount extends BaseEntity {

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email_hash", nullable = false)
    private String emailHash;

    @Column(name = "mobile_hash", nullable = false)
    private String mobileHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_doc_type")
    private IdentityDocType identityDocType;

    @Column(name = "identity_doc_hash")
    private String identityDocHash;

    @Column(name = "identity_doc_hmac")
    private String identityDocHmac;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "account_status")
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Builder.Default
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret_ref")
    private String mfaSecretRef;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Builder.Default
    @Column(name = "failed_attempt_count")
    private int failedAttemptCount = 0;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;
}
