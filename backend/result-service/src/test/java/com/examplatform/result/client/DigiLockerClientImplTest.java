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

package com.examplatform.result.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("DigiLockerClientImpl Unit Tests (result-service)")
class DigiLockerClientImplTest {

    @Test
    @DisplayName("pushScorecard gracefully handles unreachable endpoint without throwing unhandled exceptions")
    void pushScorecard_handlesUnreachableEndpoint() {
        DigiLockerClientImpl client = new DigiLockerClientImpl();
        ReflectionTestUtils.setField(client, "digiLockerPushUrl", "http://localhost:19999/unreachable/push");

        UUID candidateId = UUID.randomUUID();
        String pdfRef = "s3://bucket/scorecards/candidate-123.pdf";

        assertThatCode(() -> client.pushScorecard(candidateId, pdfRef))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("pushScorecard skips push when URL is blank")
    void pushScorecard_skipsWhenUrlIsBlank() {
        DigiLockerClientImpl client = new DigiLockerClientImpl();
        ReflectionTestUtils.setField(client, "digiLockerPushUrl", "");

        UUID candidateId = UUID.randomUUID();
        String pdfRef = "s3://bucket/scorecards/candidate-123.pdf";

        assertThatCode(() -> client.pushScorecard(candidateId, pdfRef))
                .doesNotThrowAnyException();
    }
}
