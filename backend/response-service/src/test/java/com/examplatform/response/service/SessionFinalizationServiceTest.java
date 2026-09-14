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

import com.examplatform.response.domain.Response;
import com.examplatform.response.exception.AlreadySubmittedException;
import com.examplatform.response.repository.ResponseRepository;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SessionFinalizationService.
 * Validates: SPEC-R3 (Idempotent submission)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionFinalizationService")
class SessionFinalizationServiceTest {

    @Mock
    private ResponseRepository responseRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SessionFinalizationService sessionFinalizationService;

    private UUID sessionId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("SPEC-R3: Idempotent Submission")
    class IdempotencyTests {

        @Test
        @DisplayName("SPEC-R3-T1: First submission → succeeds, Redis key set")
        void firstSubmission_succeeds() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            Response r = Response.builder().sessionId(sessionId).questionId(UUID.randomUUID())
                    .candidateId(candidateId).revisionSequence(1).saveSource("MANUAL").build();
            r.setFinal(false);
            when(responseRepository.findBySessionIdAndTenantId(sessionId, "default")).thenReturn(List.of(r));
            when(responseRepository.saveAll(any())).thenReturn(List.of(r));
            doNothing().when(eventPublisher).publish(anyString(), anyString(), any());

            assertThatNoException().isThrownBy(() ->
                    sessionFinalizationService.submitSession(sessionId, candidateId, "default"));
        }

        @Test
        @DisplayName("SPEC-R3-T2: Second submission (Redis lock exists) → AlreadySubmittedException")
        void secondSubmission_throwsAlreadySubmitted() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
            Response r = Response.builder().sessionId(sessionId).questionId(UUID.randomUUID())
                    .candidateId(candidateId).revisionSequence(1).saveSource("MANUAL").build();
            r.setFinal(false);
            assertThatThrownBy(() ->
                    sessionFinalizationService.submitSession(sessionId, candidateId, "default"))
                    .isInstanceOf(AlreadySubmittedException.class);
        }
    }
}
