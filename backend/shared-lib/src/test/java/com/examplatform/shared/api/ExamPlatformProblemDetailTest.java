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

package com.examplatform.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ExamPlatformProblemDetailTest {

    @Test
    void build_withAllFields_populatesCorrectly() {
        ProblemDetail problem = ExamPlatformProblemDetail
                .forStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .type(URI.create("https://errors.example.com/invalid-transition"))
                .title("Invalid State Transition")
                .detail("Cannot move from APPROVED to DRAFT")
                .instance(URI.create("/api/v1/questions/123/transition"))
                .property("currentState", "APPROVED")
                .property("traceId", "abc-123")
                .build();

        assertEquals(422, problem.getStatus());
        assertEquals("Invalid State Transition", problem.getTitle());
        assertEquals("Cannot move from APPROVED to DRAFT", problem.getDetail());
        assertEquals("APPROVED", problem.getProperties().get("currentState"));
        assertEquals("abc-123", problem.getProperties().get("traceId"));
    }

    @Test
    void build_withStatusCode_resolves() {
        ProblemDetail problem = ExamPlatformProblemDetail
                .forStatus(404)
                .title("Not Found")
                .build();

        assertEquals(404, problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
    }

    @Test
    void build_withMinimalFields_doesNotThrow() {
        assertDoesNotThrow(() ->
                ExamPlatformProblemDetail.forStatus(HttpStatus.BAD_REQUEST).build()
        );
    }
}
