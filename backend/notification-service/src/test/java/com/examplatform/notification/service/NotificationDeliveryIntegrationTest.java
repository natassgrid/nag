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
import com.examplatform.notification.dto.NotificationPreferenceRequest;
import com.examplatform.notification.dto.NotificationSendRequest;
import com.examplatform.notification.repository.NotificationPreferenceRepository;
import com.examplatform.notification.repository.NotificationRepository;
import com.examplatform.notification.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("Notification Multi-Channel Delivery Integration Tests")
class NotificationDeliveryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationProcessingService processingService;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private SmsDeliveryService smsDeliveryService;

    @Autowired
    private WhatsAppDeliveryService whatsAppDeliveryService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private RestTemplate mockRestTemplate;

    private static final String TENANT_ID = "nag-integration-tenant";

    @BeforeEach
    void setUp() {
        smsDeliveryService.setRestTemplate(mockRestTemplate);
        whatsAppDeliveryService.setRestTemplate(mockRestTemplate);
        pushNotificationService.setRestTemplate(mockRestTemplate);
    }

    @Test
    @DisplayName("E2E SMS Delivery: Dispatches SMS, saves entity, records external provider message ID")
    void e2eSmsDelivery_success() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        String smsResponse = "{\"type\":\"success\",\"request_id\":\"msg91_e2e_9988\",\"otp_code\":\"000000\"}";

        when(mockRestTemplate.postForEntity(contains("msg91"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(smsResponse, HttpStatus.OK));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .recipientPhone("+919876543210")
                .channel(NotificationType.SMS)
                .eventType("SESSION_SUBMITTED")
                .referenceId("SESSION-2026-X1")
                .tenantId(TENANT_ID)
                .build();

        Notification saved = processingService.sendNotification(request);

        Notification delivered = waitForStatus(saved.getId(), NotificationStatus.SENT);

        assertThat(delivered).isNotNull();
        assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(delivered.getType()).isEqualTo(NotificationType.SMS);
        assertThat(delivered.getExternalMessageId()).isEqualTo("msg91_e2e_9988");
        assertThat(delivered.getRetryCount()).isEqualTo(1);
        assertThat(delivered.getSentAt()).isNotNull();
        assertThat(delivered.getRecipientPhone()).isEqualTo("+919876543210");
        assertThat(delivered.getBody()).contains("SESSION-2026-X1");
    }

    @Test
    @DisplayName("E2E WhatsApp Delivery: Dispatches template message, saves entity and updates SENT status")
    void e2eWhatsAppDelivery_success() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        String waResponse = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgL999\"}]}";

        when(mockRestTemplate.postForEntity(contains("whatsapp"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(waResponse, HttpStatus.OK));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .recipientPhone("+919123456780")
                .channel(NotificationType.WHATSAPP)
                .eventType("RESULT_PUBLISHED")
                .referenceId("RES-4040")
                .tenantId(TENANT_ID)
                .build();

        Notification saved = processingService.sendNotification(request);

        Notification delivered = waitForStatus(saved.getId(), NotificationStatus.SENT);

        assertThat(delivered).isNotNull();
        assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(delivered.getType()).isEqualTo(NotificationType.WHATSAPP);
        assertThat(delivered.getExternalMessageId()).isEqualTo("wamid.HBgL999");
        assertThat(delivered.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("E2E Push Notification Delivery: Dispatches FCM push, updates SENT status")
    void e2ePushDelivery_success() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        String fcmResponse = "{\"multicast_id\":9911,\"success\":1,\"failure\":0,\"results\":[{\"message_id\":\"fcm_push_7788\"}]}";

        when(mockRestTemplate.postForEntity(contains("fcm"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(fcmResponse, HttpStatus.OK));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .fcmToken("fcm_device_token_xyz")
                .channel(NotificationType.PUSH)
                .eventType("ACCOUNT_LOCKED")
                .referenceId("ACC-LOCK-99")
                .tenantId(TENANT_ID)
                .build();

        Notification saved = processingService.sendNotification(request);

        Notification delivered = waitForStatus(saved.getId(), NotificationStatus.SENT);

        assertThat(delivered).isNotNull();
        assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(delivered.getType()).isEqualTo(NotificationType.PUSH);
        assertThat(delivered.getExternalMessageId()).isEqualTo("fcm_push_7788");
    }

    @Test
    @DisplayName("E2E Gateway Failure: Retries 3 times and transitions to UNDELIVERED")
    void e2eGatewayFailure_transitionsToUndelivered() throws InterruptedException {
        UUID userId = UUID.randomUUID();

        when(mockRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("503 Upstream Service Unavailable"));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .recipientPhone("+919876543210")
                .channel(NotificationType.SMS)
                .eventType("PASSWORD_RESET")
                .referenceId("PR-001")
                .tenantId(TENANT_ID)
                .build();

        Notification saved = processingService.sendNotification(request);

        Notification delivered = waitForStatus(saved.getId(), NotificationStatus.UNDELIVERED);

        assertThat(delivered).isNotNull();
        assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.UNDELIVERED);
        assertThat(delivered.getRetryCount()).isEqualTo(3);
        assertThat(delivered.getSentAt()).isNull();
    }

    @Test
    @DisplayName("E2E Preference-Based Intelligent Routing: Automatically routes according to candidate preference")
    void e2ePreferenceBasedRouting_success() throws InterruptedException {
        UUID userId = UUID.randomUUID();

        // Setup candidate preference for WhatsApp
        NotificationPreferenceRequest prefReq = NotificationPreferenceRequest.builder()
                .preferredChannel("WHATSAPP")
                .phoneNumber("+919988112233")
                .whatsappEnabled(true)
                .build();
        preferenceService.updatePreferences(userId, TENANT_ID, prefReq);

        String waResponse = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLpref\"}]}";
        when(mockRestTemplate.postForEntity(contains("whatsapp"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(waResponse, HttpStatus.OK));

        // Dispatch without specifying channel
        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .eventType("EVALUATION_COMPLETE")
                .referenceId("EVAL-111")
                .tenantId(TENANT_ID)
                .build();

        Notification saved = processingService.sendNotification(request);

        Notification delivered = waitForStatus(saved.getId(), NotificationStatus.SENT);

        assertThat(delivered.getType()).isEqualTo(NotificationType.WHATSAPP);
        assertThat(delivered.getRecipientPhone()).isEqualTo("+919988112233");
        assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(delivered.getExternalMessageId()).isEqualTo("wamid.HBgLpref");
    }

    private Notification waitForStatus(UUID notificationId, NotificationStatus targetStatus) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            Notification n = notificationRepository.findById(notificationId).orElse(null);
            if (n != null && n.getStatus() == targetStatus) {
                return n;
            }
            Thread.sleep(50);
        }
        return notificationRepository.findById(notificationId).orElseThrow();
    }
}
