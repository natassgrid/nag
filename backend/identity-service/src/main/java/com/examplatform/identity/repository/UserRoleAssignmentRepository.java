package com.examplatform.identity.repository;

import com.examplatform.identity.domain.UserRoleAssignment;
import com.examplatform.identity.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    List<UserRoleAssignment> findByUserIdAndTenantId(UUID userId, String tenantId);

    List<UserRoleAssignment> findByUserIdIn(List<UUID> userIds);

    void deleteByUserIdAndRoleAndTenantId(UUID userId, UserRole role, String tenantId);
}
