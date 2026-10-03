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
import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.NotificationSendRequest;
import com.examplatform.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationProcessingService Multi-Channel Routing Tests")
class NotificationProcessingServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private SmsDeliveryService smsDeliveryService;

    @Mock
    private WhatsAppDeliveryService whatsAppDeliveryService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private DeviceTokenService deviceTokenService;

    private NotificationProcessingService processingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        processingService = new NotificationProcessingService(
                notificationRepository,
                emailDeliveryService,
                smsDeliveryService,
                whatsAppDeliveryService,
                pushNotificationService,
                preferenceService,
                deviceTokenService,
                objectMapper);
    }

    @Test
    @DisplayName("Processes Email event -> creates Notification with PENDING status and calls email delivery")
    void processEvent_emailChannel_deliversViaEmail() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "SESSION_SUBMITTED",
                    "userId": "%s",
                    "recipientEmail": "candidate@example.com",
                    "channel": "EMAIL",
                    "tenantId": "gov-exam-authority",
                    "referenceId": "abc-123",
                    "actionLink": "https://portal.exam-platform.gov.in/sessions/abc-123"
                }
                """.formatted(userId);

        when(preferenceService.getPreferences(eq(userId), eq("gov-exam-authority")))
                .thenReturn(NotificationPreference.builder().userId(userId).preferredChannel("EMAIL").build());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRecipientEmail()).isEqualTo("candidate@example.com");
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getSubject()).isEqualTo("Exam Session Confirmation");
        assertThat(saved.getBody()).contains("SESSION-abc-123");
        assertThat(saved.getBody()).contains("https://portal.exam-platform.gov.in/sessions/abc-123");

        verify(emailDeliveryService).deliver(saved);
        verify(smsDeliveryService, never()).deliver(any());
        verify(whatsAppDeliveryService, never()).deliver(any());
        verify(pushNotificationService, never()).deliver(any());
    }

    @Test
    @DisplayName("Processes SMS event -> dispatches via SmsDeliveryService")
    void processEvent_smsChannel_deliversViaSms() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "PASSWORD_RESET",
                    "userId": "%s",
                    "recipientPhone": "+919876543210",
                    "channel": "SMS",
                    "tenantId": "upsc",
                    "referenceId": "pr-100",
                    "actionLink": ""
                }
                """.formatted(userId);

        when(preferenceService.getPreferences(eq(userId), eq("upsc")))
                .thenReturn(NotificationPreference.builder().userId(userId).phoneNumber("+919876543210").preferredChannel("SMS").build());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.SMS);
        assertThat(saved.getRecipientPhone()).isEqualTo("+919876543210");
        verify(smsDeliveryService).deliver(saved);
        verify(emailDeliveryService, never()).deliver(any());
    }

    @Test
    @DisplayName("Processes WhatsApp event -> dispatches via WhatsAppDeliveryService")
    void processEvent_whatsAppChannel_deliversViaWhatsApp() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "RESULT_PUBLISHED",
                    "userId": "%s",
                    "recipientPhone": "+919123456789",
                    "channel": "WHATSAPP",
                    "tenantId": "upsc",
                    "referenceId": "res-555",
                    "actionLink": "https://portal.nag.gov.in/results"
                }
                """.formatted(userId);

        when(preferenceService.getPreferences(eq(userId), eq("upsc")))
                .thenReturn(NotificationPreference.builder().userId(userId).phoneNumber("+919123456789").preferredChannel("WHATSAPP").build());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.WHATSAPP);
        assertThat(saved.getRecipientPhone()).isEqualTo("+919123456789");
        verify(whatsAppDeliveryService).deliver(saved);
        verify(emailDeliveryService, never()).deliver(any());
    }

    @Test
    @DisplayName("Processes Push event -> dispatches via PushNotificationService")
    void processEvent_pushChannel_deliversViaPush() {
        UUID userId = UUID.randomUUID();
        String eventPayload = """
                {
                    "eventType": "QUESTION_REVIEW",
                    "userId": "%s",
                    "fcmToken": "fcm_token_device_abc",
                    "channel": "PUSH",
                    "tenantId": "nta",
                    "referenceId": "qr-777",
                    "actionLink": ""
                }
                """.formatted(userId);

        when(preferenceService.getPreferences(eq(userId), eq("nta")))
                .thenReturn(NotificationPreference.builder().userId(userId).fcmToken("fcm_token_device_abc").preferredChannel("PUSH").build());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processingService.processEvent(eventPayload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.PUSH);
        assertThat(saved.getFcmToken()).isEqualTo("fcm_token_device_abc");
        verify(pushNotificationService).deliver(saved);
        verify(emailDeliveryService, never()).deliver(any());
    }

    @Test
    @DisplayName("sendNotification dynamically routes based on user preference when channel not specified")
    void sendNotification_routesByPreference() {
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .preferredChannel("WHATSAPP")
                .phoneNumber("+919988776655")
                .whatsappEnabled(true)
                .build();

        when(preferenceService.getPreferences(eq(userId), eq("default"))).thenReturn(pref);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSendRequest request = NotificationSendRequest.builder()
                .userId(userId)
                .eventType("RESULT_PUBLISHED")
                .referenceId("999")
                .build();

        Notification result = processingService.sendNotification(request);

        assertThat(result.getType()).isEqualTo(NotificationType.WHATSAPP);
        assertThat(result.getRecipientPhone()).isEqualTo("+919988776655");
        verify(whatsAppDeliveryService).deliver(result);
    }
}
