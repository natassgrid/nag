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

package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.dto.CandidateProfileResponse;
import com.examplatform.candidate.dto.CreateCandidateProfileRequest;
import com.examplatform.candidate.exception.DuplicateProfileException;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateEducationRepository;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CandidateProfileService}.
 *
 * <p><strong>Validates: Requirements 1.6, 25.2</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateProfileService")
class CandidateProfileServiceTest {

    @Mock
    CandidateProfileRepository candidateProfileRepository;

    @Mock
    CandidateEducationRepository candidateEducationRepository;

    @Mock
    HashingService hashingService;

    @Mock
    VaultCryptoService vaultCryptoService;

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    CandidateProfileService candidateProfileService;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String MOBILE = "9876543210";
    private static final String EMAIL = "candidate@example.com";
    private static final String IDENTITY_DOC = "ABCDE1234F";
    private static final String MOBILE_HASH = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private static final String DOC_HASH = "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3";
    private static final String DOC_HMAC = "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";

    private CreateCandidateProfileRequest validCreateRequest() {
        return CreateCandidateProfileRequest.builder()
                .userId(USER_ID)
                .fullName("Test Candidate")
                .dateOfBirth("1995-01-15")
                .gender("Male")
                .nationality("Indian")
                .category("General")
                .mobile(MOBILE)
                .email(EMAIL)
                .address("123 Test Street")
                .reservationCategory("None")
                .identityDocNumber(IDENTITY_DOC)
                .build();
    }

    private CandidateProfile savedProfile() {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(USER_ID)
                .fullName("Test Candidate")
                .dateOfBirth("1995-01-15")
                .gender("Male")
                .nationality("Indian")
                .category("General")
                .mobile(MOBILE)
                .email(EMAIL)
                .address("123 Test Street")
                .reservationCategory("None")
                .identityDocNumber(IDENTITY_DOC)
                .mobileHash(MOBILE_HASH)
                .identityDocHash(DOC_HASH)
                .identityDocHmac(DOC_HMAC)
                .encryptionKeyId("candidate-dek-" + USER_ID)
                .consentRecorded(false)
                .build();
        profile.setTenantId(TENANT_ID);
        return profile;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves profile with hashes and encryption key reference")
        void savesWithHashesAndEncryptionKey() {
            CreateCandidateProfileRequest request = validCreateRequest();

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(hashingService.sha256(IDENTITY_DOC)).thenReturn(DOC_HASH);
            when(hashingService.hmac(eq(IDENTITY_DOC), anyString())).thenReturn(DOC_HMAC);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(candidateProfileRepository.existsByIdentityDocHashAndTenantId(DOC_HASH, TENANT_ID))
                    .thenReturn(false);
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            CandidateProfileResponse response = candidateProfileService.create(request, TENANT_ID);

            ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
            verify(candidateProfileRepository).save(captor.capture());

            CandidateProfile saved = captor.getValue();
            assertThat(saved.getEncryptionKeyId()).isEqualTo("candidate-dek-" + USER_ID);
            assertThat(saved.getMobileHash()).isEqualTo(MOBILE_HASH);
            assertThat(saved.getIdentityDocHash()).isEqualTo(DOC_HASH);
            assertThat(saved.getIdentityDocHmac()).isEqualTo(DOC_HMAC);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);

            // Response has masked mobile
            assertThat(response.getMobile()).isEqualTo("****3210");
            assertThat(response.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("publishes CANDIDATE_PROFILE_CREATED audit event after successful creation")
        @SuppressWarnings("unchecked")
        void publishesAuditEventOnCreate() {
            CreateCandidateProfileRequest request = validCreateRequest();

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(hashingService.sha256(IDENTITY_DOC)).thenReturn(DOC_HASH);
            when(hashingService.hmac(eq(IDENTITY_DOC), anyString())).thenReturn(DOC_HMAC);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(candidateProfileRepository.existsByIdentityDocHashAndTenantId(DOC_HASH, TENANT_ID))
                    .thenReturn(false);
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            candidateProfileService.create(request, TENANT_ID);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(eq("exam.audit.events"), eq(USER_ID.toString()), eventCaptor.capture());

            Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("CANDIDATE_PROFILE_CREATED");
            assertThat(event.get("actorId")).isEqualTo(USER_ID.toString());
            assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
            assertThat(event.get("occurredAt")).isNotNull();
        }

        @Test
        @DisplayName("throws DuplicateProfileException when mobile already exists")
        void throwsOnDuplicateMobile() {
            CreateCandidateProfileRequest request = validCreateRequest();

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(Optional.of(savedProfile()));

            assertThatThrownBy(() -> candidateProfileService.create(request, TENANT_ID))
                    .isInstanceOf(DuplicateProfileException.class)
                    .hasMessageContaining("mobile number already exists");

            verify(candidateProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateProfileException when identity doc already exists")
        void throwsOnDuplicateIdentityDoc() {
            CreateCandidateProfileRequest request = validCreateRequest();

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(hashingService.sha256(IDENTITY_DOC)).thenReturn(DOC_HASH);
            when(hashingService.hmac(eq(IDENTITY_DOC), anyString())).thenReturn(DOC_HMAC);
            when(candidateProfileRepository.existsByIdentityDocHashAndTenantId(DOC_HASH, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> candidateProfileService.create(request, TENANT_ID))
                    .isInstanceOf(DuplicateProfileException.class)
                    .hasMessageContaining("identity document already exists");

            verify(candidateProfileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getByUserId")
    class GetByUserId {

        @Test
        @DisplayName("returns decrypted and masked response")
        void returnsDecryptedMaskedResponse() {
            CandidateProfile profile = savedProfile();

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(profile));

            CandidateProfileResponse response = candidateProfileService.getByUserId(USER_ID, TENANT_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getFullName()).isEqualTo("Test Candidate");
            // Mobile is masked: last 4 digits only
            assertThat(response.getMobile()).isEqualTo("****3210");
            // Email is masked
            assertThat(response.getEmail()).isEqualTo("ca****@example.com");
            assertThat(response.getGender()).isEqualTo("Male");
        }

        @Test
        @DisplayName("throws ProfileNotFoundException when not found")
        void throwsWhenNotFound() {
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidateProfileService.getByUserId(USER_ID, TENANT_ID))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("erasePii")
    class ErasePii {

        @Test
        @DisplayName("nulls all PII fields, sets hashes to [ERASED], deletes education records, and revokes DEK")
        void erasesAllPiiAndRevokesDek() {
            CandidateProfile profile = savedProfile();

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(profile));
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            candidateProfileService.erasePii(USER_ID, TENANT_ID);

            ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
            verify(candidateProfileRepository).save(captor.capture());

            CandidateProfile erased = captor.getValue();
            assertThat(erased.getFullName()).isNull();
            assertThat(erased.getDateOfBirth()).isNull();
            assertThat(erased.getGender()).isNull();
            assertThat(erased.getNationality()).isNull();
            assertThat(erased.getCategory()).isNull();
            assertThat(erased.getMobile()).isNull();
            assertThat(erased.getEmail()).isNull();
            assertThat(erased.getAddress()).isNull();
            assertThat(erased.getReservationCategory()).isNull();
            assertThat(erased.getIdentityDocNumber()).isNull();
            assertThat(erased.getEncryptionKeyId()).isNull();
            assertThat(erased.getMobileHash()).isEqualTo("[ERASED]");
            assertThat(erased.getIdentityDocHash()).isEqualTo("[ERASED]");
            assertThat(erased.getIdentityDocHmac()).isEqualTo("[ERASED]");

            verify(candidateEducationRepository).deleteByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(vaultCryptoService).revokeKey("candidate-dek-" + USER_ID);
        }

        @Test
        @DisplayName("throws ProfileNotFoundException when profile does not exist")
        void throwsWhenNotFound() {
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidateProfileService.erasePii(USER_ID, TENANT_ID))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("not found");

            verify(vaultCryptoService, never()).revokeKey(anyString());
        }
    }
}
