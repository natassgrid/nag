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

import com.examplatform.notification.domain.DeviceToken;
import com.examplatform.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenService Unit Tests")
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private UUID userId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = "test-tenant";
    }

    @Test
    @DisplayName("registerToken creates and saves new active device token")
    void registerToken_createNewToken() {
        when(deviceTokenRepository.findByUserIdAndToken(eq(userId), eq("token-abc-123")))
                .thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken token = deviceTokenService.registerToken(userId, tenantId, "token-abc-123", "ANDROID");

        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getToken()).isEqualTo("token-abc-123");
        assertThat(token.getDeviceType()).isEqualTo("ANDROID");
        assertThat(token.isActive()).isTrue();
        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    @DisplayName("getActiveTokens returns list of active device tokens")
    void getActiveTokens_returnsActiveList() {
        DeviceToken token = DeviceToken.builder()
                .userId(userId)
                .token("active-tok")
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(eq(userId)))
                .thenReturn(List.of(token));

        List<DeviceToken> result = deviceTokenService.getActiveTokens(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getToken()).isEqualTo("active-tok");
    }
}
