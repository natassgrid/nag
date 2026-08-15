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
import com.examplatform.candidate.dto.DigiLockerResponse;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DigiLockerService.
 *
 * Validates: Requirements 1.3
 */
@ExtendWith(MockitoExtension.class)
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
                .mobileHash("hash")
                .identityDocHash("docHash")
                .identityDocHmac("hmac")
                .build();
        profile.setTenantId(tenantId);
    }

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
