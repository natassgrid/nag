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

import com.examplatform.candidate.client.DigiLockerClient;
import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.dto.DigiLockerCallbackResult;
import com.examplatform.candidate.dto.DigiLockerResponse;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DigiLockerService Unit Tests")
class DigiLockerServiceTest {

    @Mock
    private DigiLockerClient digiLockerClient;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @InjectMocks
    private DigiLockerService digiLockerService;

    private UUID userId;
    private String tenantId;
    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = "tenant-1";
        profile = CandidateProfile.builder()
                .userId(userId)
                .fullName("Aditya Sharma")
                .dateOfBirth("1998-05-15")
                .gender("Male")
                .mobileHash("hash")
                .identityDocHash("docHash")
                .identityDocHmac("hmac")
                .build();
        profile.setTenantId(tenantId);
    }

    @Nested
    @DisplayName("1. OAuth2 Flow Tests")
    class OAuth2FlowTests {

        @Test
        @DisplayName("initiateAuth returns authorization URL and state token")
        void initiateAuth_returnsAuthUrlAndState() {
            when(digiLockerClient.getAuthorizationUrl(anyString(), any()))
                    .thenReturn("http://localhost:8099/digilocker/oauth/authorize?response_type=code&state=xyz");

            Map<String, String> result = digiLockerService.initiateAuth(userId, tenantId, null);

            assertThat(result).containsKey("authorizationUrl");
            assertThat(result).containsKey("state");
            assertThat(result.get("userId")).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("handleCallback with matching profile sets status to VERIFIED")
        void handleCallback_matchingProfile_setsVerified() {
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (userId + ":" + tenantId + ":nonce").getBytes(StandardCharsets.UTF_8));

            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.exchangeCodeForToken(eq("auth_code_123"), any()))
                    .thenReturn(Map.of("access_token", "test_access_token", "token_type", "Bearer"));
            when(digiLockerClient.getUserInfo("test_access_token"))
                    .thenReturn(Map.of("name", "Aditya Sharma", "dob", "1998-05-15", "gender", "M"));
            when(digiLockerClient.fetchDocument("test_access_token", "AADHAAR"))
                    .thenReturn(DigiLockerResponse.builder().status("SUCCESS").documentData("xml").issuerId("in.gov.uidai").build());
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenReturn(profile);

            DigiLockerCallbackResult result = digiLockerService.handleCallback("auth_code_123", state, null);

            assertThat(result.status()).isEqualTo("VERIFIED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("VERIFIED");
            verify(candidateProfileRepository).save(profile);
        }

        @Test
        @DisplayName("handleCallback with mismatched name sets status to FAILED")
        void handleCallback_mismatchedName_setsFailed() {
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (userId + ":" + tenantId + ":nonce").getBytes(StandardCharsets.UTF_8));

            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.exchangeCodeForToken(eq("auth_code_123"), any()))
                    .thenReturn(Map.of("access_token", "test_access_token"));
            when(digiLockerClient.getUserInfo("test_access_token"))
                    .thenReturn(Map.of("name", "Completely Different Person", "dob", "1998-05-15"));
            when(digiLockerClient.fetchDocument("test_access_token", "AADHAAR"))
                    .thenReturn(DigiLockerResponse.builder().status("SUCCESS").documentData("xml").issuerId("in.gov.uidai").build());
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenReturn(profile);

            DigiLockerCallbackResult result = digiLockerService.handleCallback("auth_code_123", state, null);

            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("handleCallback with missing access token returns FAILED")
        void handleCallback_missingToken_returnsFailed() {
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (userId + ":" + tenantId + ":nonce").getBytes(StandardCharsets.UTF_8));

            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.exchangeCodeForToken(anyString(), any()))
                    .thenReturn(Map.of()); // No access token

            DigiLockerCallbackResult result = digiLockerService.handleCallback("auth_code_123", state, null);

            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("2. Direct Document Verification Tests")
    class DocumentVerificationTests {

        @Test
        @DisplayName("Successful DigiLocker verification updates status to VERIFIED")
        void verifyDocument_success_setsVerified() {
            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.fetchDocument(anyString(), anyString()))
                    .thenReturn(DigiLockerResponse.builder()
                            .status("SUCCESS")
                            .documentData("document-content-data")
                            .issuerId("UIDAI")
                            .build());
            when(digiLockerClient.getUserInfo(anyString()))
                    .thenReturn(Map.of("name", "Aditya Sharma", "dob", "1998-05-15"));
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenReturn(profile);

            String result = digiLockerService.verifyDocument(userId, tenantId);

            assertThat(result).isEqualTo("VERIFIED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("VERIFIED");
            verify(candidateProfileRepository).save(profile);
        }

        @Test
        @DisplayName("Failed DigiLocker verification sets status to FAILED")
        void verifyDocument_failure_setsFailed() {
            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.fetchDocument(anyString(), anyString()))
                    .thenReturn(DigiLockerResponse.builder()
                            .status("FAILURE")
                            .documentData(null)
                            .issuerId(null)
                            .build());
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenReturn(profile);

            String result = digiLockerService.verifyDocument(userId, tenantId);

            assertThat(result).isEqualTo("FAILED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("FAILED");
            verify(candidateProfileRepository).save(profile);
        }

        @Test
        @DisplayName("DigiLocker API exception sets status to FAILED")
        void verifyDocument_exception_setsFailed() {
            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.of(profile));
            when(digiLockerClient.fetchDocument(anyString(), anyString()))
                    .thenThrow(new RuntimeException("API timeout"));
            when(candidateProfileRepository.save(any(CandidateProfile.class)))
                    .thenReturn(profile);

            String result = digiLockerService.verifyDocument(userId, tenantId);

            assertThat(result).isEqualTo("FAILED");
            assertThat(profile.getDigiLockerVerified()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("Profile not found throws ProfileNotFoundException")
        void verifyDocument_profileNotFound_throwsException() {
            when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> digiLockerService.verifyDocument(userId, tenantId))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("3. Matching Logic Unit Tests")
    class MatchingLogicTests {

        @Test
        @DisplayName("Matches case-insensitive name and token subsets")
        void isNameMatching_matchesVariants() {
            assertThat(digiLockerService.isNameMatching("Aditya Sharma", "ADITYA SHARMA")).isTrue();
            assertThat(digiLockerService.isNameMatching("Aditya Kumar Sharma", "Aditya Sharma")).isTrue();
            assertThat(digiLockerService.isNameMatching("Priya Patel", "Priya")).isTrue();
            assertThat(digiLockerService.isNameMatching("Aditya Sharma", "Rahul Verma")).isFalse();
        }

        @Test
        @DisplayName("Matches date of birth across standard formats")
        void isDobMatching_matchesFormats() {
            assertThat(digiLockerService.isDobMatching("1998-05-15", "1998-05-15")).isTrue();
            assertThat(digiLockerService.isDobMatching("1998-05-15", "15/05/1998")).isTrue();
            assertThat(digiLockerService.isDobMatching("1998-05-15", "15-05-1998")).isTrue();
            assertThat(digiLockerService.isDobMatching("1998-05-15", "2000-01-01")).isFalse();
        }
    }
}
