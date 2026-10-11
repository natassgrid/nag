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

    public static final String UNKNOWN = "Unknown";
    public static final String MESSAGE = "message";
    public static final String YOUR_QUESTION_FOR_SUBJECT = "Your question for subject '";
    public static final String YOUR_QUESTION_FOR_SUBJECT_PREFIX = YOUR_QUESTION_FOR_SUBJECT;
    public static final String REVIEWER_ID = "reviewerId";
    public static final String SUBJECT = "subject";
    public static final String QUESTION_ID = "questionId";
    public static final String RECIPIENT_ID = "recipientId";
    public static final String AUTHOR_ID = "authorId";
    public static final String TENANT_ID = "tenantId";
}
