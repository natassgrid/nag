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

package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryHelper Unit Tests")
class NotificationDeliveryHelperTest {

    @Mock
    private NotificationRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("findCurrent returns repository entity when present, otherwise fallback")
    void testFindCurrent() {
        UUID id = UUID.randomUUID();
        Notification persisted = Notification.builder().subject("Persisted").build();
        Notification fallback = Notification.builder().subject("Fallback").build();

        when(repository.findById(id)).thenReturn(Optional.of(persisted));
        assertThat(NotificationDeliveryHelper.findCurrent(repository, id, fallback)).isEqualTo(persisted);

        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());
        assertThat(NotificationDeliveryHelper.findCurrent(repository, missingId, fallback)).isEqualTo(fallback);

        assertThat(NotificationDeliveryHelper.findCurrent(null, id, fallback)).isEqualTo(fallback);
        assertThat(NotificationDeliveryHelper.findCurrent(repository, null, fallback)).isEqualTo(fallback);
    }

    @Test
    @DisplayName("recordSuccess updates status to SENT, sets retryCount and externalId")
    void testRecordSuccess() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().subject("Test").build();
        when(repository.findById(id)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = NotificationDeliveryHelper.recordSuccess(repository, id, notification, 2, "EXT-123", "TMPL-99");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isNotNull();
        assertThat(result.getRetryCount()).isEqualTo(2);
        assertThat(result.getExternalMessageId()).isEqualTo("EXT-123");
        assertThat(result.getTemplateId()).isEqualTo("TMPL-99");
        verify(repository, times(1)).save(notification);
    }

    @Test
    @DisplayName("recordFailure updates status to UNDELIVERED and sets retryCount")
    void testRecordFailure() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().subject("Test").build();
        when(repository.findById(id)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = NotificationDeliveryHelper.recordFailure(repository, id, notification, 3);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(result.getRetryCount()).isEqualTo(3);
        verify(repository, times(1)).save(notification);
    }

    @Test
    @DisplayName("executeHttpDeliveryWithRetry succeeds on first attempt")
    void testExecuteHttpDeliverySuccess() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().subject("Test").build();
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", id);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger callCount = new AtomicInteger();
        NotificationDeliveryHelper.executeHttpDeliveryWithRetry(
                repository,
                notification,
                "TestChannel",
                3,
                () -> {
                    callCount.incrementAndGet();
                    return new ResponseEntity<>("{\"messageId\":\"msg-123\"}", HttpStatus.OK);
                },
                body -> NotificationDeliveryHelper.extractMessageId(objectMapper, body, "TEST"),
                null
        );

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getExternalMessageId()).isEqualTo("msg-123");
        verify(repository, times(1)).save(notification);
    }

    @Test
    @DisplayName("executeHttpDeliveryWithRetry retries until max attempts and records failure")
    void testExecuteHttpDeliveryPermanentFailure() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().subject("Test").build();
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", id);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger callCount = new AtomicInteger();
        NotificationDeliveryHelper.executeHttpDeliveryWithRetry(
                repository,
                notification,
                "TestChannel",
                3,
                () -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Network down");
                },
                body -> "id",
                null
        );

        assertThat(callCount.get()).isEqualTo(3);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        verify(repository, times(1)).save(notification);
    }

    @Test
    @DisplayName("extractMessageId extracts from various JSON schemas and falls back to prefix")
    void testExtractMessageId() {
        // FCM style results array
        assertThat(NotificationDeliveryHelper.extractMessageId(
                objectMapper,
                "{\"results\":[{\"message_id\":\"fcm-abc\"}]}",
                "PUSH"
        )).isEqualTo("fcm-abc");

        // WhatsApp style messages array
        assertThat(NotificationDeliveryHelper.extractMessageId(
                objectMapper,
                "{\"messages\":[{\"id\":\"wa-xyz\"}]}",
                "WA"
        )).isEqualTo("wa-xyz");

        // SMS style request_id
        assertThat(NotificationDeliveryHelper.extractMessageId(
                objectMapper,
                "{\"request_id\":\"sms-req-456\"}",
                "SMS"
        )).isEqualTo("sms-req-456");

        // Common messageId property
        assertThat(NotificationDeliveryHelper.extractMessageId(
                objectMapper,
                "{\"messageId\":\"generic-id-789\"}",
                "GEN"
        )).isEqualTo("generic-id-789");

        // Fallback on blank or null
        assertThat(NotificationDeliveryHelper.extractMessageId(objectMapper, "", "TEST"))
                .startsWith("TEST-");
        assertThat(NotificationDeliveryHelper.extractMessageId(objectMapper, null, "TEST"))
                .startsWith("TEST-");

        // Fallback on empty or invalid json
        assertThat(NotificationDeliveryHelper.extractMessageId(objectMapper, "{}", "TEST"))
                .startsWith("TEST-");
        assertThat(NotificationDeliveryHelper.extractMessageId(objectMapper, "not-json", "TEST"))
                .startsWith("TEST-");
    }
}
