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

package com.examplatform.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event record. Stored in a range-partitioned table
 * by occurred_at (quarterly partitions, 7-year retention).
 * No updates or deletes are permitted on this table.
 */
@Entity
@Table(name = "audit_event", schema = "audit_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "resource", nullable = false, length = 500)
    private String resource;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "hsm_signature", length = 512)
    private String hsmSignature;

    @Column(name = "signing_key_id", length = 100)
    private String signingKeyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload", columnDefinition = "jsonb")
    private String eventPayload;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }
}
