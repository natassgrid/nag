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

import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.NotificationPreferenceRequest;
import com.examplatform.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceService Unit Tests")
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private UUID userId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = "test-tenant";
    }

    @Test
    @DisplayName("getPreferences returns default preferences if none exist in repository")
    void getPreferences_returnsDefaultWhenNoneExist() {
        when(preferenceRepository.findByUserIdAndTenantId(eq(userId), eq(tenantId)))
                .thenReturn(Optional.empty());
        when(preferenceRepository.findByUserId(eq(userId)))
                .thenReturn(Optional.empty());

        NotificationPreference result = preferenceService.getPreferences(userId, tenantId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPreferredChannel()).isEqualTo("EMAIL");
        assertThat(result.isPushEnabled()).isTrue();
        assertThat(result.isSmsEnabled()).isTrue();
        assertThat(result.isWhatsappEnabled()).isTrue();
    }

    @Test
    @DisplayName("updatePreferences updates existing preferences and saves to repository")
    void updatePreferences_updatesAndSaves() {
        NotificationPreference existing = NotificationPreference.builder()
                .userId(userId)
                .preferredChannel("EMAIL")
                .build();
        existing.setTenantId(tenantId);

        when(preferenceRepository.findByUserIdAndTenantId(eq(userId), eq(tenantId)))
                .thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .preferredChannel("WHATSAPP")
                .phoneNumber("+919876543210")
                .email("test@example.com")
                .whatsappEnabled(true)
                .smsEnabled(false)
                .build();

        NotificationPreference updated = preferenceService.updatePreferences(userId, tenantId, request);

        assertThat(updated.getPreferredChannel()).isEqualTo("WHATSAPP");
        assertThat(updated.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(updated.getEmail()).isEqualTo("test@example.com");
        assertThat(updated.isWhatsappEnabled()).isTrue();
        assertThat(updated.isSmsEnabled()).isFalse();
        verify(preferenceRepository).save(existing);
    }
}
