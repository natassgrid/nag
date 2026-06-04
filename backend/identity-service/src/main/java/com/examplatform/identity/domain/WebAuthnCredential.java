package com.examplatform.identity.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "webauthn_credential",
    schema = "identity_service",
    uniqueConstraints = @UniqueConstraint(columnNames = "credential_id")
)
public class WebAuthnCredential extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credential_id", nullable = false, unique = true)
    private String credentialId;

    @Column(name = "public_key_cose", columnDefinition = "bytea")
    private byte[] publicKeyCose;

    @Builder.Default
    @Column(name = "sign_count")
    private long signCount = 0L;

    @Column(name = "aaguid")
    private String aaguid;
}
