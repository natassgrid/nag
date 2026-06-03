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
