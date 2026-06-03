package com.examplatform.identity.repository;

import com.examplatform.identity.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailHashAndTenantId(String emailHash, String tenantId);

    Optional<UserAccount> findByIdentityDocHashAndTenantId(String identityDocHash, String tenantId);

    boolean existsByEmailHashAndTenantId(String emailHash, String tenantId);

    boolean existsByIdentityDocHashAndTenantId(String identityDocHash, String tenantId);

    Optional<UserAccount> findByMobileHashAndTenantId(String mobileHash, String tenantId);
}
