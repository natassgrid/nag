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

import com.examplatform.response.config.MetricsConfig;
import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.repository.ResponseRepository;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResponseSaveService.
 *
 * Validates: Requirements 10.1, 20.3
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResponseSaveService")
class ResponseSaveServiceTest {

    @Mock
    private ResponseRepository responseRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MetricsConfig metricsConfig;

    @Mock
    private Counter responseSaveCounter;

    @InjectMocks
    private ResponseSaveService responseSaveService;

    private UUID sessionId;
    private UUID questionId;
    private UUID candidateId;
    private String tenantId;
    private SaveResponseRequest request;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        tenantId = "tenant-abc";

        request = SaveResponseRequest.builder()
                .questionId(questionId)
                .selectedOptionIds("[\"opt-1\",\"opt-2\"]")
                .enteredValue(null)
                .timestamp(Instant.now())
                .cumulativeTimeSpentMs(5000L)
                .saveSource("MANUAL")
                .build();

        when(metricsConfig.getResponseSaveCounter()).thenReturn(responseSaveCounter);
    }

    @Nested
    @DisplayName("Revision Sequence")
    class RevisionSequenceTests {

        @Test
        @DisplayName("First save for a question → revisionSequence = 1")
        void firstSaveForQuestion_revisionSequenceIsOne() {
            // Given: no previous responses exist
            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(Collections.emptyList());
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            SaveResponseResponse response = responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            assertThat(response.getRevisionSequence()).isEqualTo(1);
        }

        @Test
        @DisplayName("Subsequent save → revisionSequence increments")
        void subsequentSave_revisionSequenceIncrements() {
            // Given: previous response with revision 3 exists
            Response previousResponse = Response.builder()
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .revisionSequence(3)
                    .build();

            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(List.of(previousResponse));
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            SaveResponseResponse response = responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            assertThat(response.getRevisionSequence()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Kafka Integration")
    class KafkaTests {

        @Test
        @DisplayName("Kafka send is called with correct topic and key")
        void kafkaSendCalledWithCorrectTopicAndKey() {
            // Given
            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(Collections.emptyList());
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo("exam.response.saved");
            assertThat(keyCaptor.getValue()).isEqualTo(sessionId.toString());
        }
    }

    @Nested
    @DisplayName("Metrics")
    class MetricsTests {

        @Test
        @DisplayName("MetricsConfig counter is incremented")
        void metricsCounterIsIncremented() {
            // Given
            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(Collections.emptyList());
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            verify(responseSaveCounter).increment();
        }
    }

    @Nested
    @DisplayName("Response Fields")
    class ResponseFieldTests {

        @Test
        @DisplayName("Response fields are correctly set")
        void responseFieldsAreCorrectlySet() {
            // Given
            UUID responseId = UUID.randomUUID();
            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(Collections.emptyList());
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", responseId);
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            SaveResponseResponse response = responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            assertThat(response.getResponseId()).isEqualTo(responseId);
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getQuestionId()).isEqualTo(questionId);
            assertThat(response.getRevisionSequence()).isEqualTo(1);
            assertThat(response.getSaveSource()).isEqualTo("MANUAL");
        }

        @Test
        @DisplayName("Entity is saved with correct fields from request")
        void entitySavedWithCorrectFields() {
            // Given
            when(responseRepository.findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, questionId))
                    .thenReturn(Collections.emptyList());
            when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
                Response r = invocation.getArgument(0);
                ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(r, "createdAt", Instant.now());
                return r;
            });
            when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            responseSaveService.saveResponse(sessionId, request, candidateId, tenantId);

            // Then
            ArgumentCaptor<Response> entityCaptor = ArgumentCaptor.forClass(Response.class);
            verify(responseRepository).save(entityCaptor.capture());

            Response saved = entityCaptor.getValue();
            assertThat(saved.getSessionId()).isEqualTo(sessionId);
            assertThat(saved.getQuestionId()).isEqualTo(questionId);
            assertThat(saved.getCandidateId()).isEqualTo(candidateId);
            assertThat(saved.getSelectedOptionIds()).isEqualTo("[\"opt-1\",\"opt-2\"]");
            assertThat(saved.getEnteredValue()).isNull();
            assertThat(saved.getCumulativeTimeSpentMs()).isEqualTo(5000L);
            assertThat(saved.getSaveSource()).isEqualTo("MANUAL");
            assertThat(saved.isFinal()).isFalse();
            assertThat(saved.getRevisionSequence()).isEqualTo(1);
            assertThat(saved.getTenantId()).isEqualTo(tenantId);
        }
    }
}
