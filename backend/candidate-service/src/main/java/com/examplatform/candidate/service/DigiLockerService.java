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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for verifying candidate identity documents via DigiLocker integration.
 * Calls DigiLocker API, validates returned document data, and updates
 * the candidate profile's digiLockerVerified status.
 *
 * Validates: Requirements 1.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DigiLockerService {

    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DOC_TYPE_IDENTITY = "AADHAAR";

    private final DigiLockerClient digiLockerClient;
    private final CandidateProfileRepository candidateProfileRepository;

    /**
     * Verifies the candidate's identity document via DigiLocker API.
     *
     * @param userId   the candidate's user ID
     * @param tenantId the tenant identifier
     * @return "VERIFIED" if document validation succeeds, "FAILED" otherwise
     */
    public String verifyDocument(UUID userId, String tenantId) {
        log.info("Starting DigiLocker verification for userId={}, tenantId={}", userId, tenantId);

        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));

        try {
            // Call DigiLocker API with a placeholder token (real implementation would use OAuth2 flow)
            DigiLockerResponse response = digiLockerClient.fetchDocument(
                    "oauth2-token-" + userId, DOC_TYPE_IDENTITY);

            if (response != null && isDocumentValid(response)) {
                profile.setDigiLockerVerified(STATUS_VERIFIED);
                candidateProfileRepository.save(profile);
                log.info("DigiLocker verification VERIFIED for userId={}", userId);
                return STATUS_VERIFIED;
            }
        } catch (Exception e) {
            log.error("DigiLocker verification failed for userId={}: {}", userId, e.getMessage(), e);
        }

        profile.setDigiLockerVerified(STATUS_FAILED);
        candidateProfileRepository.save(profile);
        log.info("DigiLocker verification FAILED for userId={}", userId);
        return STATUS_FAILED;
    }

    /**
     * Validates the document response from DigiLocker.
     * Checks that status is success, document data is present, and issuer is valid.
     */
    private boolean isDocumentValid(DigiLockerResponse response) {
        return response.getStatus() != null
                && response.getStatus().equalsIgnoreCase("SUCCESS")
                && response.getDocumentData() != null
                && !response.getDocumentData().isBlank()
                && response.getIssuerId() != null
                && !response.getIssuerId().isBlank();
    }
}
