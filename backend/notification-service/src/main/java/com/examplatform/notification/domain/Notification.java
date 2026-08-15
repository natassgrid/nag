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

package com.examplatform.notification.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a notification to be delivered to a user via one or more channels.
 * Tracks delivery status and retry attempts.
 */
@Entity
@Table(name = "notification", schema = "notification_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private NotificationType type;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    /**
     * Notification delivery channel type.
     */
    public enum NotificationType {
        EMAIL,
        PUSH,
        IN_APP
    }

    /**
     * Notification delivery lifecycle states.
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        UNDELIVERED
    }
}
