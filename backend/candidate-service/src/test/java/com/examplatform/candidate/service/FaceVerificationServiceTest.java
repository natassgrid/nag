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
import com.examplatform.candidate.dto.FaceVerificationRequest;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FaceVerificationService.
 *
 * Validates: Requirements 1.4
 */
@ExtendWith(MockitoExtension.class)
class FaceVerificationServiceTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    private FaceVerificationService faceVerificationService;

    private UUID userId;
    private String tenantId;
    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        faceVerificationService = new FaceVerificationService(candidateProfileRepository);
        ReflectionTestUtils.setField(faceVerificationService, "similarityThreshold", 0.85);

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
    @DisplayName("Similarity above threshold returns VERIFIED")
    void verifyFace_aboveThreshold_returnsVerified() {
        when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class)))
                .thenReturn(profile);

        // Identical vectors → cosine similarity = 1.0
        float[] embedding = {0.5f, 0.8f, 0.3f, 0.9f};
        FaceVerificationRequest request = FaceVerificationRequest.builder()
                .userId(userId)
                .photoEmbedding(embedding)
                .docPhotoEmbedding(embedding)
                .build();

        String result = faceVerificationService.verifyFace(userId, request, tenantId);

        assertThat(result).isEqualTo("VERIFIED");
        assertThat(profile.getFaceVerificationStatus()).isEqualTo("VERIFIED");
        verify(candidateProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Similarity below threshold returns FAILED")
    void verifyFace_belowThreshold_returnsFailed() {
        when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class)))
                .thenReturn(profile);

        // Orthogonal vectors → cosine similarity = 0.0
        float[] photoEmbedding = {1.0f, 0.0f, 0.0f, 0.0f};
        float[] docPhotoEmbedding = {0.0f, 1.0f, 0.0f, 0.0f};
        FaceVerificationRequest request = FaceVerificationRequest.builder()
                .userId(userId)
                .photoEmbedding(photoEmbedding)
                .docPhotoEmbedding(docPhotoEmbedding)
                .build();

        String result = faceVerificationService.verifyFace(userId, request, tenantId);

        assertThat(result).isEqualTo("FAILED");
        assertThat(profile.getFaceVerificationStatus()).isEqualTo("FAILED");
        verify(candidateProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Profile not found throws ProfileNotFoundException")
    void verifyFace_profileNotFound_throwsException() {
        when(candidateProfileRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.empty());

        FaceVerificationRequest request = FaceVerificationRequest.builder()
                .userId(userId)
                .photoEmbedding(new float[]{1.0f})
                .docPhotoEmbedding(new float[]{1.0f})
                .build();

        assertThatThrownBy(() -> faceVerificationService.verifyFace(userId, request, tenantId))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    @DisplayName("Cosine similarity computation is correct")
    void computeCosineSimilarity_correctResult() {
        // vectors with known cosine similarity
        float[] a = {1.0f, 0.0f};
        float[] b = {1.0f, 1.0f};
        // cos(45°) ≈ 0.7071
        double similarity = faceVerificationService.computeCosineSimilarity(a, b);
        assertThat(similarity).isCloseTo(0.7071, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Empty vectors return zero similarity")
    void computeCosineSimilarity_emptyVectors_returnsZero() {
        double similarity = faceVerificationService.computeCosineSimilarity(new float[0], new float[0]);
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Null vectors return zero similarity")
    void computeCosineSimilarity_nullVectors_returnsZero() {
        double similarity = faceVerificationService.computeCosineSimilarity(null, new float[]{1.0f});
        assertThat(similarity).isEqualTo(0.0);
    }
}
