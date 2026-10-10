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
import com.examplatform.candidate.dto.UpdateCandidateProfileRequest;
import com.examplatform.candidate.exception.DuplicateProfileException;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateEducationRepository;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import com.examplatform.shared.crypto.HashingService;
import com.examplatform.shared.crypto.VaultCryptoService;
import com.examplatform.shared.event.UserAuditEvent;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateProfileService Unit Tests")
class CandidateProfileServiceTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private CandidateEducationRepository candidateEducationRepository;

    @Mock
    private HashingService hashingService;

    @Mock
    private VaultCryptoService vaultCryptoService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    private CandidateProfileService candidateProfileService;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("018f4e2a-0000-7000-8000-000000000001");
    private static final String MOBILE = "9876543210";
    private static final String MOBILE_HASH = "mocked-mobile-hash-64chars-long-value-00000000000000000000000000000";
    private static final String IDENTITY_DOC = "ABCDE1234F";
    private static final String DOC_HASH = "mocked-doc-hash-64chars-long-value-00000000000000000000000000000000";
    private static final String DOC_HMAC = "mocked-doc-hmac-64chars-long-value-0000000000000000000000000000000";

    @BeforeEach
    void setUp() {
        candidateProfileService = new CandidateProfileService(
                candidateProfileRepository,
                candidateEducationRepository,
                hashingService,
                vaultCryptoService,
                eventPublisher,
                jdbcTemplateProvider
        );
    }

    private CreateCandidateProfileRequest validCreateRequest() {
        return CreateCandidateProfileRequest.builder()
                .userId(USER_ID)
                .fullName("Test Candidate")
                .dateOfBirth("2000-01-01")
                .gender("Male")
                .nationality("Indian")
                .category("General")
                .mobile(MOBILE)
                .email("candidate@example.com")
                .address("123 Main St, New Delhi")
                .reservationCategory("None")
                .identityDocNumber(IDENTITY_DOC)
                .build();
    }

    private CandidateProfile savedProfile() {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(USER_ID)
                .fullName("Test Candidate")
                .dateOfBirth("2000-01-01")
                .gender("Male")
                .nationality("Indian")
                .category("General")
                .mobile(MOBILE)
                .email("candidate@example.com")
                .address("123 Main St, New Delhi")
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
                    .thenReturn(Collections.emptyList());
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(candidateProfileRepository.existsByIdentityDocHashAndTenantId(DOC_HASH, TENANT_ID))
                    .thenReturn(false);
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

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

            // Response has profile mobile & email
            assertThat(response.getMobile()).isEqualTo(MOBILE);
            assertThat(response.getEmail()).isEqualTo("candidate@example.com");
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
                    .thenReturn(Collections.emptyList());
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(candidateProfileRepository.existsByIdentityDocHashAndTenantId(DOC_HASH, TENANT_ID))
                    .thenReturn(false);
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            candidateProfileService.create(request, TENANT_ID);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publish(eq("exam.audit.events"), eq(USER_ID.toString()), eventCaptor.capture());

            Object eventObj = eventCaptor.getValue();
            if (eventObj instanceof UserAuditEvent auditEvent) {
                assertThat(auditEvent.eventType()).isEqualTo("CANDIDATE_PROFILE_CREATED");
                assertThat(auditEvent.actorId()).isEqualTo(USER_ID.toString());
                assertThat(auditEvent.tenantId()).isEqualTo(TENANT_ID);
                assertThat(auditEvent.occurredAt()).isNotNull();
            } else if (eventObj instanceof Map<?, ?> event) {
                assertThat(event.get("eventType")).isEqualTo("CANDIDATE_PROFILE_CREATED");
                assertThat(event.get("actorId")).isEqualTo(USER_ID.toString());
                assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
                assertThat(event.get("occurredAt")).isNotNull();
            }
        }

        @Test
        @DisplayName("updates existing candidate profile idempotently without conflict")
        void updatesExistingCandidateProfileIdempotently() {
            CreateCandidateProfileRequest request = validCreateRequest();
            CandidateProfile existing = CandidateProfile.builder()
                    .userId(USER_ID)
                    .encryptionKeyId("candidate-dek-" + USER_ID)
                    .build();
            existing.setTenantId(TENANT_ID);

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(hashingService.sha256(IDENTITY_DOC)).thenReturn(DOC_HASH);
            when(hashingService.hmac(eq(IDENTITY_DOC), anyString())).thenReturn(DOC_HMAC);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(List.of(existing));
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateProfileResponse response = candidateProfileService.create(request, TENANT_ID);

            assertThat(response.getFullName()).isEqualTo("Test Candidate");
            assertThat(response.getMobile()).isEqualTo(MOBILE);
            assertThat(response.getEmail()).isEqualTo("candidate@example.com");
        }

        @Test
        @DisplayName("throws DuplicateProfileException when mobile already exists for another user")
        void throwsOnDuplicateMobile() {
            CreateCandidateProfileRequest request = validCreateRequest();
            CandidateProfile existingOther = savedProfile();
            existingOther.setUserId(UUID.randomUUID()); // Different user

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(List.of(existingOther));

            assertThatThrownBy(() -> candidateProfileService.create(request, TENANT_ID))
                    .isInstanceOf(DuplicateProfileException.class)
                    .hasMessageContaining("mobile number already exists");

            verify(candidateProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateProfileException when identity doc already exists for another user")
        void throwsOnDuplicateIdentityDoc() {
            CreateCandidateProfileRequest request = validCreateRequest();

            when(hashingService.sha256(MOBILE)).thenReturn(MOBILE_HASH);
            when(candidateProfileRepository.findByMobileHashAndTenantId(MOBILE_HASH, TENANT_ID))
                    .thenReturn(Collections.emptyList());
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
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
        @DisplayName("returns full profile response")
        void returnsFullProfileResponse() {
            CandidateProfile profile = savedProfile();

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(profile));

            CandidateProfileResponse response = candidateProfileService.getByUserId(USER_ID, TENANT_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getFullName()).isEqualTo("Test Candidate");
            assertThat(response.getMobile()).isEqualTo(MOBILE);
            assertThat(response.getEmail()).isEqualTo("candidate@example.com");
            assertThat(response.getGender()).isEqualTo("Male");
        }

        @Test
        @DisplayName("auto-initializes default candidate profile when not found")
        void autoInitializesWhenNotFound() {
            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateProfileResponse response = candidateProfileService.getByUserId(USER_ID, TENANT_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
            verify(candidateProfileRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getEncryptionKeyId()).isEqualTo("candidate-dek-" + USER_ID);
        }

        @Test
        @DisplayName("backfills email and username from identity_service.user_account when missing")
        void backfillsMissingEmailFromUserAccount() {
            CandidateProfile profileWithoutEmail = CandidateProfile.builder()
                    .userId(USER_ID)
                    .fullName(null)
                    .email(null)
                    .mobile(MOBILE)
                    .build();
            profileWithoutEmail.setTenantId(TENANT_ID);

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(profileWithoutEmail));
            when(jdbcTemplateProvider.getIfAvailable()).thenReturn(jdbcTemplate);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(USER_ID)))
                    .thenReturn("sheel.prabhakar@gmail.com");
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateProfileResponse response = candidateProfileService.getByUserId(USER_ID, TENANT_ID);

            assertThat(response.getEmail()).isEqualTo("sheel.prabhakar@gmail.com");
            verify(candidateProfileRepository).save(profileWithoutEmail);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("auto-initializes and updates candidate profile when profile did not exist")
        void autoInitializesAndUpdatesWhenNotFound() {
            UpdateCandidateProfileRequest request = UpdateCandidateProfileRequest.builder()
                    .fullName("Updated Candidate")
                    .mobile("9123456780")
                    .build();

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(hashingService.sha256("9123456780")).thenReturn("new-mobile-hash");
            when(candidateProfileRepository.findByMobileHashAndTenantId("new-mobile-hash", TENANT_ID))
                    .thenReturn(Collections.emptyList());
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateProfileResponse response = candidateProfileService.update(USER_ID, request, TENANT_ID);

            assertThat(response.getFullName()).isEqualTo("Updated Candidate");
            assertThat(response.getMobile()).isEqualTo("9123456780");
        }

        @Test
        @DisplayName("recomputes hashes when identity doc or mobile is updated")
        void recomputesHashesOnUpdate() {
            CandidateProfile profile = savedProfile();

            UpdateCandidateProfileRequest request = UpdateCandidateProfileRequest.builder()
                    .mobile("9111111111")
                    .identityDocNumber("XYZ9876543")
                    .build();

            when(candidateProfileRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(profile));
            when(hashingService.sha256("9111111111")).thenReturn("new-mobile-hash");
            when(hashingService.sha256("XYZ9876543")).thenReturn("new-doc-hash");
            when(hashingService.hmac(eq("XYZ9876543"), anyString())).thenReturn("new-doc-hmac");
            when(candidateProfileRepository.findByMobileHashAndTenantId("new-mobile-hash", TENANT_ID))
                    .thenReturn(List.of(profile)); // belongs to same user
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateProfileResponse response = candidateProfileService.update(USER_ID, request, TENANT_ID);

            assertThat(response.getMobile()).isEqualTo("9111111111");
            assertThat(profile.getMobileHash()).isEqualTo("new-mobile-hash");
            assertThat(profile.getIdentityDocHash()).isEqualTo("new-doc-hash");
            assertThat(profile.getIdentityDocHmac()).isEqualTo("new-doc-hmac");
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
