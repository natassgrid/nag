package com.examplatform.identity.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "active_session", schema = "identity_service")
public class ActiveSession extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String sessionToken;

    @Column(length = 512)
    private String deviceFp;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
