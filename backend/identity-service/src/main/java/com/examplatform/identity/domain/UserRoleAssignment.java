package com.examplatform.identity.domain;

import com.examplatform.identity.domain.enums.UserRole;
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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_role_assignment", schema = "identity_service")
public class UserRoleAssignment extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
