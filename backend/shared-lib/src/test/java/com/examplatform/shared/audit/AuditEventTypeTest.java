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

package com.examplatform.shared.audit;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTypeTest {

    /**
     * Complete set of all 29 audit event types defined in the platform spec.
     * Any removal or rename of an existing value is a breaking change and requires a
     * database migration.
     */
    private static final Set<String> EXPECTED_VALUES = Set.of(
            // Identity / Access domain
            "LOGIN", "LOGOUT", "ROLE_CHANGE", "DENIED_ACCESS", "ACCOUNT_LOCK",
            "KEY_REVOCATION",
            // Candidate domain
            "CANDIDATE_PROFILE_CREATED",
            // Question Bank domain
            "QUESTION_CREATED", "QUESTION_MODIFIED", "QUESTION_STATE_TRANSITION",
            // Paper domain
            "PAPER_GENERATED", "PAPER_APPROVED",
            // Delivery / Session domain
            "SESSION_STARTED", "SESSION_SUBMITTED", "RESPONSE_SAVED",
            // Evaluation / Result domain
            "EVALUATION_CREATED", "RESULT_PUBLISHED",
            // Admin domain
            "CONFIG_CHANGED",
            // Security domain
            "TAMPER_ATTEMPT",
            // Translation domain
            "TRANSLATION_CREATED", "TRANSLATION_APPROVED",
            // Exam publication domain
            "EXAM_PUBLISHED",
            // Examination scheduling domain
            "SCHEDULE_CREATED", "SCHEDULE_STATUS_CHANGED", "SCHEDULE_AMENDED",
            "SCHEDULE_CANCELLED", "SHIFT_CREATED", "SHIFT_UPDATED",
            "SEAT_ALLOCATION_UPDATED"
    );

    @Test
    void allRequiredEventTypesArePresent() {
        Set<String> actual = Arrays.stream(AuditEventType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(actual.containsAll(EXPECTED_VALUES),
                "AuditEventType enum is missing values. Expected: " + EXPECTED_VALUES
                        + " Actual: " + actual);
    }

    @Test
    void noUnexpectedEventTypesExist() {
        Set<String> actual = Arrays.stream(AuditEventType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_VALUES, actual,
                "AuditEventType enum contains unexpected values. Extra: "
                        + actual.stream()
                                .filter(v -> !EXPECTED_VALUES.contains(v))
                                .collect(Collectors.toSet()));
    }

    @Test
    void enumCountMatchesSpecification() {
        assertEquals(29, AuditEventType.values().length,
                "AuditEventType must define exactly 29 event types per the platform spec");
    }

    @Test
    void valueOfWorksForEachType() {
        for (String name : EXPECTED_VALUES) {
            assertDoesNotThrow(() -> AuditEventType.valueOf(name),
                    "AuditEventType.valueOf should not throw for: " + name);
        }
    }
}
