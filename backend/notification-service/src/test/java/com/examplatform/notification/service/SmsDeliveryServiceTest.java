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
@DisplayName("SmsDeliveryService Unit Tests")
class SmsDeliveryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SmsDeliveryService smsDeliveryService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smsDeliveryService, "smsEndpoint", "http://localhost:3000/msg91/api/v5/otp");
        ReflectionTestUtils.setField(smsDeliveryService, "authKey", "test-auth-key");
        ReflectionTestUtils.setField(smsDeliveryService, "senderId", "NAGGov");
        ReflectionTestUtils.setField(smsDeliveryService, "restTemplate", restTemplate);

        notification = Notification.builder()
                .userId(UUID.randomUUID())
                .recipientPhone("+919876543210")
                .type(NotificationType.SMS)
                .subject("SESSION_SUBMITTED")
                .body("Your exam session has been submitted. Reference: SESSION-12345")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("Successful SMS send on first attempt sets status=SENT, retryCount=1 and records externalMessageId")
    void deliver_successOnFirstAttempt() {
        String mockResponse = "{\"type\":\"success\",\"request_id\":\"msg91_req_abc123\",\"otp_code\":\"000000\"}";
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        smsDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getExternalMessageId()).isEqualTo("msg91_req_abc123");
        assertThat(notification.getSentAt()).isNotNull();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Failure on first two attempts and success on third sets status=SENT, retryCount=3")
    void deliver_retrySuccessOnThirdAttempt() {
        String mockResponse = "{\"type\":\"success\",\"request_id\":\"msg91_retry_ok\"}";
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection timeout"))
                .thenThrow(new RestClientException("503 Gateway Timeout"))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        smsDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        assertThat(notification.getExternalMessageId()).isEqualTo("msg91_retry_ok");
        assertThat(notification.getSentAt()).isNotNull();
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("Failure on all 3 attempts sets status=UNDELIVERED, retryCount=3")
    void deliver_failureAllAttempts_setsUndelivered() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("500 Internal Server Error"));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        smsDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(notification.getRetryCount()).isEqualTo(3);
        assertThat(notification.getSentAt()).isNull();
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Missing recipient phone immediately marks notification as UNDELIVERED")
    void deliver_missingPhone_immediatelyUndelivered() {
        notification.setRecipientPhone(null);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        smsDeliveryService.deliver(notification);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        verify(restTemplate, times(0)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
}
