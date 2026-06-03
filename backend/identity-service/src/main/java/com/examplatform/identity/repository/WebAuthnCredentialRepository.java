package com.examplatform.identity.repository;

import com.examplatform.identity.domain.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, UUID> {

    Optional<WebAuthnCredential> findByCredentialIdAndTenantId(String credentialId, String tenantId);

    List<WebAuthnCredential> findByUserIdAndTenantId(UUID userId, String tenantId);
}
