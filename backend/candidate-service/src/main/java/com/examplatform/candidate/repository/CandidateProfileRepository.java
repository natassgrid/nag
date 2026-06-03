package com.examplatform.candidate.repository;

import com.examplatform.candidate.domain.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    Optional<CandidateProfile> findByUserIdAndTenantId(UUID userId, String tenantId);

    Optional<CandidateProfile> findByMobileHashAndTenantId(String mobileHash, String tenantId);

    boolean existsByIdentityDocHashAndTenantId(String identityDocHash, String tenantId);
}
