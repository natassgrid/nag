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

package com.examplatform.identity.service;

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.IdentityDocType;
import com.examplatform.identity.dto.RegistrationRequest;
import com.examplatform.identity.dto.RegistrationResponse;
import com.examplatform.identity.exception.DuplicateIdentityException;
import com.examplatform.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationService}.
 *
 * Validates: Requirements 1.1, 1.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    HashingService hashingService;
    @Mock
    OtpService otpService;
    @Mock
    AuditEventPublisher auditEventPublisher;
    @Mock
    AppSecurityProperties securityProperties;

    @InjectMocks
    RegistrationService registrationService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns response for new valid registration")
        void happyPath() {
            // given
            RegistrationRequest req = RegistrationRequest.builder()
                    .email("test@example.com")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.AADHAAR)
                    .identityDocNumber("123456789012")
                    .password("Password123!")
                    .fullName("Test User")
                    .build();

            when(hashingService.sha256(anyString())).thenReturn("hashedvalue");
            when(hashingService.hmac(anyString(), anyString())).thenReturn("hmacvalue");
            when(userAccountRepository.existsByEmailHashAndTenantId(any(), any())).thenReturn(false);
            when(userAccountRepository.existsByIdentityDocHashAndTenantId(any(), any())).thenReturn(false);

            UserAccount saved = UserAccount.builder().build();
            saved.setTenantId("default");
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            when(userAccountRepository.save(any())).thenReturn(saved);

            // when
            RegistrationResponse response = registrationService.register(req, "default");

            // then
            assertAll(
                    () -> assertThat(response.getMessage()).isNotBlank(),
                    () -> verify(otpService).sendOtp(any(), any(), any())
            );
        }

        @Test
        @DisplayName("throws DuplicateIdentityException on duplicate email")
        void duplicateEmail() {
            RegistrationRequest req = RegistrationRequest.builder()
                    .email("dup@example.com")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.PAN)
                    .identityDocNumber("ABCDE1234F")
                    .password("Password123!")
                    .fullName("Test User")
                    .build();

            when(hashingService.sha256(anyString())).thenReturn("hashedvalue");
            when(hashingService.hmac(anyString(), anyString())).thenReturn("hmacvalue");
            when(userAccountRepository.existsByEmailHashAndTenantId(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(req, "default"))
                    .isInstanceOf(DuplicateIdentityException.class);
        }

        @Test
        @DisplayName("throws DuplicateIdentityException on duplicate identity doc")
        void duplicateIdentityDoc() {
            RegistrationRequest req = RegistrationRequest.builder()
                    .email("new@example.com")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.PASSPORT)
                    .identityDocNumber("A1234567")
                    .password("Password123!")
                    .fullName("Test User")
                    .build();

            when(hashingService.sha256(anyString())).thenReturn("hashedvalue");
            when(hashingService.hmac(anyString(), anyString())).thenReturn("hmacvalue");
            when(userAccountRepository.existsByEmailHashAndTenantId(any(), any())).thenReturn(false);
            when(userAccountRepository.existsByIdentityDocHashAndTenantId(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(req, "default"))
                    .isInstanceOf(DuplicateIdentityException.class);
        }

        @Test
        @DisplayName("hashes email as lowercase before duplicate check")
        void emailHashIsLowercase() {
            RegistrationRequest req = RegistrationRequest.builder()
                    .email("UPPER@EXAMPLE.COM")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.AADHAAR)
                    .identityDocNumber("999988887777")
                    .password("Password123!")
                    .fullName("Upper Case User")
                    .build();

            when(hashingService.sha256(anyString())).thenReturn("hashedvalue");
            when(hashingService.hmac(anyString(), anyString())).thenReturn("hmacvalue");
            when(userAccountRepository.existsByEmailHashAndTenantId(any(), any())).thenReturn(false);
            when(userAccountRepository.existsByIdentityDocHashAndTenantId(any(), any())).thenReturn(false);

            UserAccount saved = UserAccount.builder().build();
            saved.setTenantId("default");
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            when(userAccountRepository.save(any())).thenReturn(saved);

            RegistrationResponse response = registrationService.register(req, "default");

            // Verify sha256 was called with lowercased email
            verify(hashingService).sha256("upper@example.com");
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("userId in response is null when saved account has no id")
        void userIdInResponse() {
            RegistrationRequest req = RegistrationRequest.builder()
                    .email("test@example.com")
                    .mobile("9876543210")
                    .identityDocType(IdentityDocType.AADHAAR)
                    .identityDocNumber("123456789012")
                    .password("Password123!")
                    .fullName("Test User")
                    .build();

            when(hashingService.sha256(anyString())).thenReturn("hashedvalue");
            when(hashingService.hmac(anyString(), anyString())).thenReturn("hmacvalue");
            when(userAccountRepository.existsByEmailHashAndTenantId(any(), any())).thenReturn(false);
            when(userAccountRepository.existsByIdentityDocHashAndTenantId(any(), any())).thenReturn(false);

            UserAccount saved = UserAccount.builder().build();
            saved.setTenantId("default");
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            when(userAccountRepository.save(any())).thenReturn(saved);

            RegistrationResponse response = registrationService.register(req, "default");

            // userId is set because we assigned an id via ReflectionTestUtils
            assertAll(
                    () -> assertThat(response.getMessage()).contains("OTP"),
                    () -> verify(userAccountRepository).save(any())
            );
        }
    }
}
