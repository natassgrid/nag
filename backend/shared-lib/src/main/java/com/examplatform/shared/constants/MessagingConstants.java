/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.shared.constants;

/**
 * Standard messaging topic and channel names across the platform.
 */
public final class MessagingConstants {

    private MessagingConstants() {
        // Utility / Constants class
    }

    public static final String AUDIT_EVENTS_TOPIC = "exam.audit.events";
    public static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";
    public static final String EXAM_EVENTS_TOPIC = "exam.events";
    public static final String NOTIFICATIONS_OUTBOUND_TOPIC = "exam.notifications.outbound";
    public static final String TAXONOMY_EVENTS_TOPIC = "exam.taxonomy.events";
}
