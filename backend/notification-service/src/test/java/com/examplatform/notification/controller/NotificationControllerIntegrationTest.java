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

package com.examplatform.notification.controller;

import com.examplatform.notification.domain.DeviceToken;
import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.domain.Notification.NotificationType;
import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.DeviceTokenRequest;
import com.examplatform.notification.dto.NotificationPreferenceRequest;
import com.examplatform.notification.dto.NotificationSendRequest;
import com.examplatform.notification.service.DeviceTokenService;
import com.examplatform.notification.service.NotificationPreferenceService;
import com.examplatform.notification.service.NotificationProcessingService;
import com.examplatform.notification.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("NotificationController REST Endpoints Tests (MockMvc)")
class NotificationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private NotificationProcessingService notificationProcessingService;

    @MockitoBean
    private NotificationPreferenceService preferenceService;

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID NOTIFICATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Nested
    @DisplayName("POST /api/v1/notifications/send")
    class SendNotificationEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user dispatches multi-channel notification - returns 201 Created")
        void authenticatedUserCanSendNotification() throws Exception {
            Notification notification = Notification.builder()
                    .userId(USER_ID)
                    .recipientPhone("+919876543210")
                    .type(NotificationType.WHATSAPP)
                    .channel("WHATSAPP")
                    .subject("Exam Alert")
                    .body("Your examination slot is confirmed.")
                    .status(NotificationStatus.PENDING)
                    .retryCount(0)
                    .sentAt(Instant.now())
                    .build();
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);

            when(notificationProcessingService.sendNotification(any(NotificationSendRequest.class)))
                    .thenReturn(notification);

            NotificationSendRequest request = NotificationSendRequest.builder()
                    .userId(USER_ID)
                    .recipientPhone("+919876543210")
                    .channel(NotificationType.WHATSAPP)
                    .eventType("SESSION_SUBMITTED")
                    .referenceId("12345")
                    .build();

            mockMvc.perform(post("/api/v1/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()))
                    .andExpect(jsonPath("$.type").value("WHATSAPP"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("-ve: Unauthenticated send request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/notifications/push/register-token")
    class RegisterDeviceTokenEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user registers push device token - returns 201 Created")
        void authenticatedUserCanRegisterPushToken() throws Exception {
            DeviceToken deviceToken = DeviceToken.builder()
                    .userId(USER_ID)
                    .token("pwa_fcm_token_xyz")
                    .deviceType("WEB_PWA")
                    .active(true)
                    .build();
            ReflectionTestUtils.setField(deviceToken, "id", UUID.randomUUID());

            when(deviceTokenService.registerToken(eq(USER_ID), anyString(), eq("pwa_fcm_token_xyz"), eq("WEB_PWA")))
                    .thenReturn(deviceToken);

            DeviceTokenRequest request = DeviceTokenRequest.builder()
                    .userId(USER_ID)
                    .token("pwa_fcm_token_xyz")
                    .deviceType("WEB_PWA")
                    .build();

            mockMvc.perform(post("/api/v1/notifications/push/register-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("pwa_fcm_token_xyz"))
                    .andExpect(jsonPath("$.deviceType").value("WEB_PWA"));
        }
    }

    @Nested
    @DisplayName("GET & PUT /api/v1/notifications/preferences/{userId}")
    class PreferencesEndpoints {

        @Test
        @DisplayName("+ve: Authenticated user retrieves notification preferences")
        void getPreferences_returns200() throws Exception {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(USER_ID)
                    .preferredChannel("SMS")
                    .phoneNumber("+919876543210")
                    .smsEnabled(true)
                    .whatsappEnabled(true)
                    .build();

            when(preferenceService.getPreferences(eq(USER_ID), anyString())).thenReturn(pref);

            mockMvc.perform(get("/api/v1/notifications/preferences/" + USER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.preferredChannel").value("SMS"))
                    .andExpect(jsonPath("$.phoneNumber").value("+919876543210"));
        }

        @Test
        @DisplayName("+ve: Authenticated user updates notification preferences")
        void updatePreferences_returns200() throws Exception {
            NotificationPreference updated = NotificationPreference.builder()
                    .userId(USER_ID)
                    .preferredChannel("WHATSAPP")
                    .phoneNumber("+919876543210")
                    .whatsappEnabled(true)
                    .build();

            when(preferenceService.updatePreferences(eq(USER_ID), anyString(), any(NotificationPreferenceRequest.class)))
                    .thenReturn(updated);

            NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                    .preferredChannel("WHATSAPP")
                    .phoneNumber("+919876543210")
                    .whatsappEnabled(true)
                    .build();

            mockMvc.perform(put("/api/v1/notifications/preferences/" + USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.preferredChannel").value("WHATSAPP"));
        }
    }
}
