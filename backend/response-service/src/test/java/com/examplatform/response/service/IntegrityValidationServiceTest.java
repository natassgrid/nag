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

package com.examplatform.response.service;

import com.examplatform.response.exception.ResponseIntegrityException;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IntegrityValidationService.
 * Validates: SPEC-R1-T1..T5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrityValidationService")
class IntegrityValidationServiceTest {

    @Mock
    private RestTemplate deliveryRestTemplate;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private IntegrityValidationService service;

    private UUID sessionId;
    private UUID candidateId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "deliveryServiceUrl", "http://localhost:8084");
    }

    @Nested
    @DisplayName("SPEC-R1: Integrity Validation")
    class IntegrityValidationTests {

        @Test
        @DisplayName("SPEC-R1-T1: Valid question and active session → passes validation")
        void validQuestionActiveSession_passesValidation() {
            when(deliveryRestTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(Map.of("valid", true));

            assertThatNoException().isThrownBy(() ->
                    service.validateResponse(sessionId, candidateId, questionId, "default"));
        }

        @Test
        @DisplayName("SPEC-R1-T2: Question not in candidate's paper → ResponseIntegrityException")
        void questionNotInPaper_throwsIntegrityException() {
            when(deliveryRestTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(Map.of("valid", false, "reason", "QUESTION_NOT_IN_PAPER"));

            assertThatThrownBy(() ->
                    service.validateResponse(sessionId, candidateId, questionId, "default"))
                    .isInstanceOf(ResponseIntegrityException.class)
                    .hasMessageContaining("QUESTION_NOT_IN_PAPER");
        }

        @Test
        @DisplayName("SPEC-R1-T3: Session expired → ResponseIntegrityException with SESSION_EXPIRED")
        void sessionExpired_throwsIntegrityException() {
            when(deliveryRestTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(Map.of("valid", false, "reason", "SESSION_EXPIRED"));

            assertThatThrownBy(() ->
                    service.validateResponse(sessionId, candidateId, questionId, "default"))
                    .isInstanceOf(ResponseIntegrityException.class)
                    .extracting(e -> ((ResponseIntegrityException) e).getErrorCode())
                    .isEqualTo("SESSION_EXPIRED");
        }

        @Test
        @DisplayName("SPEC-R1-T5: delivery-service unreachable → fail-open, publishes INTEGRITY_UNKNOWN alert")
        void deliveryServiceUnreachable_failOpen_publishesAlert() {
            when(deliveryRestTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));
            doNothing().when(eventPublisher).publish(anyString(), anyString(), any());

            assertThatNoException().isThrownBy(() ->
                    service.validateResponse(sessionId, candidateId, questionId, "default"));

            verify(eventPublisher).publish(eq("exam.audit.events"), anyString(), any());
        }
    }
}
