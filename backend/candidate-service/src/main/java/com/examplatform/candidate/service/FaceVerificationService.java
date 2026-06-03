package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.dto.FaceVerificationRequest;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for face verification by comparing embedding vectors.
 * Computes cosine similarity between the submitted photograph and the
 * identity document photograph, and sets verification status accordingly.
 *
 * Validates: Requirements 1.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FaceVerificationService {

    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_FAILED = "FAILED";

    @Value("${face.verification.similarity-threshold:0.85}")
    private double similarityThreshold;

    private final CandidateProfileRepository candidateProfileRepository;

    /**
     * Verifies face by computing cosine similarity between photo embeddings.
     *
     * @param userId   the candidate's user ID
     * @param request  the face verification request containing embedding vectors
     * @param tenantId the tenant identifier
     * @return "VERIFIED" if similarity >= threshold, "FAILED" otherwise
     */
    public String verifyFace(UUID userId, FaceVerificationRequest request, String tenantId) {
        log.info("Starting face verification for userId={}, tenantId={}", userId, tenantId);

        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));

        double similarity = computeCosineSimilarity(
                request.getPhotoEmbedding(), request.getDocPhotoEmbedding());

        log.debug("Cosine similarity for userId={}: {}", userId, similarity);

        if (similarity >= similarityThreshold) {
            profile.setFaceVerificationStatus(STATUS_VERIFIED);
            candidateProfileRepository.save(profile);
            log.info("Face verification VERIFIED for userId={}, similarity={}", userId, similarity);
            return STATUS_VERIFIED;
        }

        profile.setFaceVerificationStatus(STATUS_FAILED);
        candidateProfileRepository.save(profile);
        log.info("Face verification FAILED for userId={}, similarity={}", userId, similarity);
        return STATUS_FAILED;
    }

    /**
     * Computes cosine similarity between two embedding vectors.
     * cosine_similarity = (A · B) / (||A|| * ||B||)
     *
     * @param vectorA first embedding vector
     * @param vectorB second embedding vector
     * @return cosine similarity in range [-1, 1]
     */
    double computeCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        if (vectorA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }
}
