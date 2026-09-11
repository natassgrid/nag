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

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.service.InAppNotificationService;
import com.examplatform.notification.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("InAppNotificationController REST Endpoints E2E Tests (MockMvc)")
class InAppNotificationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private InAppNotificationService inAppNotificationService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotificationsEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user retrieves notifications - returns 200 OK")
        void authenticatedUserCanGetNotifications() throws Exception {
            Notification notification = Notification.builder()
                    .userId(USER_ID)
                    .type(Notification.NotificationType.IN_APP)
                    .subject("Exam Scheduled")
                    .body("Your exam starts tomorrow at 10:00 AM")
                    .status(Notification.NotificationStatus.SENT)
                    .retryCount(0)
                    .read(false)
                    .sentAt(Instant.now())
                    .build();
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);

            Page<Notification> page = new PageImpl<>(List.of(notification));

            when(inAppNotificationService.getNotifications(eq(USER_ID), anyString(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/notifications")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(NOTIFICATION_ID.toString()))
                    .andExpect(jsonPath("$.content[0].subject").value("Exam Scheduled"))
                    .andExpect(jsonPath("$.content[0].read").value(false));
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/stream")
    class StreamNotificationsEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user establishes SSE connection - returns 200 OK with event stream")
        void authenticatedUserCanConnectSse() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/notifications/stream")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("connected");
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedSseReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/stream"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read")
    class MarkAsReadEndpoint {

        @Test
        @DisplayName("+ve: Authenticated user marks notification as read - returns 200 OK")
        void authenticatedUserCanMarkAsRead() throws Exception {
            Notification notification = Notification.builder()
                    .userId(USER_ID)
                    .type(Notification.NotificationType.IN_APP)
                    .subject("Exam Scheduled")
                    .body("Your exam starts tomorrow at 10:00 AM")
                    .status(Notification.NotificationStatus.SENT)
                    .retryCount(0)
                    .read(true)
                    .readAt(Instant.now())
                    .build();
            ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);

            when(inAppNotificationService.markAsRead(eq(NOTIFICATION_ID), eq(USER_ID), anyString()))
                    .thenReturn(notification);

            mockMvc.perform(patch("/api/v1/notifications/{id}/read", NOTIFICATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()))
                    .andExpect(jsonPath("$.read").value(true));
        }

        @Test
        @DisplayName("-ve: Nonexistent notification returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(inAppNotificationService.markAsRead(eq(NOTIFICATION_ID), eq(USER_ID), anyString()))
                    .thenThrow(new IllegalArgumentException("Notification not found: " + NOTIFICATION_ID));

            mockMvc.perform(patch("/api/v1/notifications/{id}/read", NOTIFICATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(patch("/api/v1/notifications/{id}/read", NOTIFICATION_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
