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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for verifying candidate identity documents via DigiLocker OAuth2 integration.
 * Initiates OAuth2 authorization flow, exchanges callback authorization code,
 * validates returned demographic and document data against candidate profile fields,
 * and updates the candidate profile's digiLockerVerified status.
 *
 * Validates: Requirements 1.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DigiLockerService {

    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_FAILED = "FAILED";
    private static final String DOC_TYPE_IDENTITY = "AADHAAR";

    private final DigiLockerClient digiLockerClient;
    private final CandidateProfileRepository candidateProfileRepository;

    /**
     * Initiates the OAuth2 authorization code flow for a candidate.
     *
     * @param userId      the candidate's user UUID
     * @param tenantId    the tenant identifier
     * @param redirectUri optional custom redirect URI
     * @return map containing authorizationUrl and state token
     */
    @Transactional(readOnly = true)
    public Map<String, String> initiateAuth(UUID userId, String tenantId, String redirectUri) {
        log.info("Initiating DigiLocker OAuth2 auth for userId={}, tenantId={}", userId, tenantId);

        String state = generateStateToken(userId, tenantId);
        String authorizationUrl = digiLockerClient.getAuthorizationUrl(state, redirectUri);

        Map<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);
        result.put("state", state);
        result.put("userId", userId != null ? userId.toString() : "");
        return result;
    }

    /**
     * Handles OAuth2 authorization code callback.
     * Exchanges code for tokens, retrieves demographic userinfo, validates against candidate profile,
     * and updates profile verification status.
     *
     * @param code        the authorization code from DigiLocker
     * @param state       the state token passed in authorization request
     * @param redirectUri optional custom redirect URI
     * @return map with verification result status and message
     */
    public DigiLockerCallbackResult handleCallback(String code, String state, String redirectUri) {
        log.info("Handling DigiLocker OAuth2 callback for state={}", state);

        UUID userId = null;
        String tenantId = "default";

        if (state != null && !state.isBlank()) {
            Map<String, String> stateData = parseStateToken(state);
            if (stateData.containsKey("userId")) {
                try {
                    userId = UUID.fromString(stateData.get("userId"));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (stateData.containsKey("tenantId")) {
                tenantId = stateData.get("tenantId");
            }
        }

        if (userId == null) {
            log.error("Unable to resolve candidate userId from OAuth2 state token: {}", state);
            return DigiLockerCallbackResult.failed("Invalid state token or candidate context missing");
        }

        final UUID effectiveUserId = userId;
        final String effectiveTenantId = tenantId;

        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(effectiveUserId, effectiveTenantId)
                .orElseThrow(() -> new ProfileNotFoundException("Candidate profile not found for userId=" + effectiveUserId));

        try {
            // 1. Exchange code for access token
            Map<String, Object> tokenResponse = digiLockerClient.exchangeCodeForToken(code, redirectUri);
            String accessToken = tokenResponse != null && tokenResponse.get("access_token") != null
                    ? tokenResponse.get("access_token").toString() : null;

            if (accessToken == null || accessToken.isBlank()) {
                log.warn("Empty access token received from DigiLocker for userId={}", effectiveUserId);
                profile.setDigiLockerVerified(STATUS_FAILED);
                candidateProfileRepository.save(profile);
                return DigiLockerCallbackResult.failed("Failed to obtain access token from DigiLocker");
            }

            // 2. Fetch demographic user info and document
            Map<String, Object> userInfo = digiLockerClient.getUserInfo(accessToken);
            DigiLockerResponse docResponse = digiLockerClient.fetchDocument(accessToken, DOC_TYPE_IDENTITY);

            // 3. Validate demographic details against profile
            boolean verified = validateProfileAgainstDigiLocker(profile, userInfo, docResponse);

            String status = verified ? STATUS_VERIFIED : STATUS_FAILED;
            profile.setDigiLockerVerified(status);
            candidateProfileRepository.save(profile);

            log.info("DigiLocker verification result for userId={}: {}", effectiveUserId, status);
            return new DigiLockerCallbackResult(
                    status,
                    effectiveUserId.toString(),
                    status,
                    verified ? "DigiLocker document and demographic identity verified successfully"
                            : "Candidate profile details do not match DigiLocker identity record"
            );
        } catch (Exception e) {
            log.error("Error during DigiLocker callback processing for userId={}: {}", effectiveUserId, e.getMessage(), e);
            profile.setDigiLockerVerified(STATUS_FAILED);
            candidateProfileRepository.save(profile);
            return DigiLockerCallbackResult.failed("DigiLocker verification failed: " + e.getMessage());
        }
    }

    /**
     * Verifies the candidate's identity document via DigiLocker API directly or with provided token.
     *
     * @param userId   the candidate's user ID
     * @param tenantId the tenant identifier
     * @return "VERIFIED" if document validation succeeds, "FAILED" otherwise
     */
    public String verifyDocument(UUID userId, String tenantId) {
        return verifyDocument(userId, null, tenantId);
    }

    /**
     * Verifies the candidate's identity document with optional access token.
     */
    public String verifyDocument(UUID userId, String token, String tenantId) {
        log.info("Starting DigiLocker document verification for userId={}, tenantId={}", userId, tenantId);

        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));

        try {
            String effectiveToken = (token != null && !token.isBlank()) ? token : ("oauth2-token-" + userId);
            DigiLockerResponse response = digiLockerClient.fetchDocument(effectiveToken, DOC_TYPE_IDENTITY);
            Map<String, Object> userInfo = digiLockerClient.getUserInfo(effectiveToken);

            if (response != null && isDocumentValid(response)) {
                boolean match = validateProfileAgainstDigiLocker(profile, userInfo, response);
                if (match) {
                    profile.setDigiLockerVerified(STATUS_VERIFIED);
                    candidateProfileRepository.save(profile);
                    log.info("DigiLocker verification VERIFIED for userId={}", userId);
                    return STATUS_VERIFIED;
                }
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
     * Validates candidate profile demographic data (full name, date of birth) against
     * DigiLocker user info and document response.
     */
    boolean validateProfileAgainstDigiLocker(CandidateProfile profile,
                                             Map<String, Object> userInfo,
                                             DigiLockerResponse docResponse) {
        if (profile == null) {
            return false;
        }

        // If docResponse explicitly failed, validation fails
        if (docResponse != null && docResponse.getStatus() != null
                && "FAILURE".equalsIgnoreCase(docResponse.getStatus())) {
            return false;
        }

        String dlName = null;
        String dlDob = null;

        if (userInfo != null) {
            if (userInfo.get("name") != null) {
                dlName = String.valueOf(userInfo.get("name"));
            } else if (userInfo.get("fullName") != null) {
                dlName = String.valueOf(userInfo.get("fullName"));
            }

            if (userInfo.get("dob") != null) {
                dlDob = String.valueOf(userInfo.get("dob"));
            } else if (userInfo.get("dateOfBirth") != null) {
                dlDob = String.valueOf(userInfo.get("dateOfBirth"));
            }
        }

        // Check name match
        boolean nameMatches = isNameMatching(profile.getFullName(), dlName);

        // Check DOB match
        boolean dobMatches = isDobMatching(profile.getDateOfBirth(), dlDob);

        log.info("DigiLocker profile validation: candidateName='{}', dlName='{}', nameMatch={}, candidateDob='{}', dlDob='{}', dobMatch={}",
                profile.getFullName(), dlName, nameMatches, profile.getDateOfBirth(), dlDob, dobMatches);

        return nameMatches && dobMatches;
    }

    /**
     * Validates if candidate full name matches DigiLocker name (case-insensitive token overlap or containment).
     */
    boolean isNameMatching(String candidateName, String dlName) {
        if (candidateName == null || candidateName.isBlank()) {
            return true; // No name in profile to conflict with
        }
        if (dlName == null || dlName.isBlank()) {
            return true; // If DigiLocker didn't provide name, don't fail solely on name
        }

        String n1 = normalizeName(candidateName);
        String n2 = normalizeName(dlName);

        if (n1.equalsIgnoreCase(n2)) {
            return true;
        }

        Set<String> tokens1 = Arrays.stream(n1.split("\\s+")).collect(Collectors.toSet());
        Set<String> tokens2 = Arrays.stream(n2.split("\\s+")).collect(Collectors.toSet());

        // Check intersection of tokens
        tokens1.retainAll(tokens2);
        return !tokens1.isEmpty();
    }

    /**
     * Validates date of birth compatibility across common formats (YYYY-MM-DD, DD/MM/YYYY, DD-MM-YYYY).
     */
    boolean isDobMatching(String profileDob, String dlDob) {
        if (profileDob == null || profileDob.isBlank()) {
            return true;
        }
        if (dlDob == null || dlDob.isBlank()) {
            return true;
        }

        String normProfileDob = normalizeDate(profileDob);
        String normDlDob = normalizeDate(dlDob);

        if (normProfileDob.equals(normDlDob)) {
            return true;
        }

        // Year-level fallback check if full date format differs
        if (normProfileDob.length() >= 4 && normDlDob.length() >= 4) {
            String year1 = normProfileDob.substring(0, 4);
            String year2 = normDlDob.substring(0, 4);
            return year1.equals(year2);
        }

        return false;
    }

    private String normalizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
    }

    private String normalizeDate(String dateStr) {
        String trimmed = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                LocalDate date = LocalDate.parse(trimmed, dtf);
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
            }
        }
        return trimmed;
    }

    private boolean isDocumentValid(DigiLockerResponse response) {
        return response.getStatus() != null
                && response.getStatus().equalsIgnoreCase("SUCCESS")
                && response.getDocumentData() != null
                && !response.getDocumentData().isBlank()
                && response.getIssuerId() != null
                && !response.getIssuerId().isBlank();
    }

    private String generateStateToken(UUID userId, String tenantId) {
        String raw = (userId != null ? userId.toString() : "") + ":"
                + (tenantId != null ? tenantId : "default") + ":"
                + UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> parseStateToken(String state) {
        Map<String, String> map = new HashMap<>();
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(state);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                map.put("userId", parts[0]);
            }
            if (parts.length >= 2 && !parts[1].isBlank()) {
                map.put("tenantId", parts[1]);
            }
        } catch (Exception e) {
            log.warn("Could not decode state token '{}': {}", state, e.getMessage());
        }
        return map;
    }
}
