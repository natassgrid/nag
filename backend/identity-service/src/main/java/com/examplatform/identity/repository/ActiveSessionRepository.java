package com.examplatform.identity.repository;

import com.examplatform.identity.domain.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, UUID> {

    Optional<ActiveSession> findByUserIdAndTenantId(UUID userId, String tenantId);

    void deleteByUserIdAndTenantId(UUID userId, String tenantId);

    boolean existsByUserIdAndTenantId(UUID userId, String tenantId);

    List<ActiveSession> findAllByExpiresAtBefore(LocalDateTime dateTime);
}
