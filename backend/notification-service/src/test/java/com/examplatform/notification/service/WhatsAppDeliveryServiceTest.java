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
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsAppDeliveryService Unit Tests")
class WhatsAppDeliveryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WhatsAppDeliveryService whatsAppDeliveryService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(whatsAppDeliveryService, "whatsappEndpoint", "http://localhost:3000/whatsapp/v1/messages");
        ReflectionTestUtils.setField(whatsAppDeliveryService, "accessToken", "test-wa-token");
        ReflectionTestUtils.setField(whatsAppDeliveryService, "restTemplate", restTemplate);

        notification = Notification.builder()
                .userId(UUID.randomUUID())
                .recipientPhone("+919876543210")
                .type(NotificationType.WHATSAPP)
                .templateId("nag_exam_alert")
                .subject("RESULT_PUBLISHED")
                .body("Your examination result is now available. Reference: RESULT-5678")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("Successful WhatsApp delivery on first attempt sets status=SENT and records message ID")
    void deliver_successOnFirstAttempt() {
        String mockResponse = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLMTE\"}]}";
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        whatsAppDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getExternalMessageId()).isEqualTo("wamid.HBgLMTE");
        assertThat(notification.getSentAt()).isNotNull();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Failure across all 3 attempts sets status=UNDELIVERED")
    void deliver_allAttemptsFail_setsUndelivered() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("500 Upstream Service Unavailable"));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        whatsAppDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Missing recipient phone marks notification as UNDELIVERED")
    void deliver_missingPhone_immediatelyUndelivered() {
        notification.setRecipientPhone(null);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        whatsAppDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        verify(restTemplate, times(0)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
}
