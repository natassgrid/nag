/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.shared.constants;

/**
 * Common shared constants used across services.
 */
public final class SharedConstants {

    private SharedConstants() {
        // Utility / Constants class
    }

    // Tenant & Auth Headers
    public static final String DEFAULT_TENANT = "default";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN = "access_token";

    // Common JSON & Event Payload Keys
    public static final String UNKNOWN = "Unknown";
    public static final String MESSAGE = "message";
    public static final String EVENT_TYPE = "eventType";
    public static final String STATUS = "status";
    public static final String TIMESTAMP = "timestamp";
    public static final String OCCURRED_AT = "occurredAt";
    public static final String CREATED_AT = "createdAt";
    public static final String ACTOR_ID = "actorId";
    public static final String USER_ID = "userId";
    public static final String CANDIDATE_ID = "candidateId";
    public static final String EXAM_ID = "examId";
    public static final String SESSION_ID = "sessionId";
    public static final String ACTION = "action";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String CONTENT = "content";
    public static final String TOPIC = "topic";
    public static final String DIFFICULTY = "difficulty";
    public static final String REVIEWER_ID = "reviewerId";
    public static final String SUBJECT = "subject";
    public static final String QUESTION_ID = "questionId";
    public static final String RECIPIENT_ID = "recipientId";
    public static final String AUTHOR_ID = "authorId";
    public static final String TENANT_ID = "tenantId";
    public static final String YOUR_QUESTION_FOR_SUBJECT = "Your question for subject '";
    public static final String YOUR_QUESTION_FOR_SUBJECT_PREFIX = YOUR_QUESTION_FOR_SUBJECT;

    // Database & System Types
    public static final String JSONB = "jsonb";
    public static final String SHA_256 = "SHA-256";
}
