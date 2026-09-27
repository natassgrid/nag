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
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling candidate profile CRUD operations with per-candidate DEK,
 * SHA-256 hashing for uniqueness, HMAC for duplicate detection, and DPDP erasure.
 *
 * Validates: Requirements 1.6, 25.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CandidateProfileService {

    private static final String HMAC_KEY_PREFIX = "candidate-doc-hmac-";
    private static final String DEK_PREFIX = "candidate-dek-";
    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateEducationRepository candidateEducationRepository;
    private final HashingService hashingService;
    private final VaultCryptoService vaultCryptoService;
    private final EventPublisher eventPublisher;

    /**
     * Creates a new candidate profile with per-candidate DEK reference,
     * mobile hash for uniqueness, and identity doc hash + HMAC for duplicate detection.
     */
    public CandidateProfileResponse create(CreateCandidateProfileRequest request, String tenantId) {
        // 1. Generate per-candidate DEK key name
        String dekKeyName = DEK_PREFIX + request.getUserId();

        // 2. Hash mobile (SHA-256) for uniqueness check against OTHER candidates
        String mobileHash = hashingService.sha256(request.getMobile().trim());
        List<CandidateProfile> existingByMobile = candidateProfileRepository.findByMobileHashAndTenantId(mobileHash, tenantId);
        boolean duplicateMobile = existingByMobile.stream().anyMatch(e -> !e.getUserId().equals(request.getUserId()));
        if (duplicateMobile) {
            throw new DuplicateProfileException(
                    "A profile with this mobile number already exists");
        }

        // 3. Hash + HMAC identity doc for duplicate detection
        String normalizedDoc = request.getIdentityDocNumber().trim().toUpperCase();
        String docHash = hashingService.sha256(normalizedDoc);
        String docHmac = hashingService.hmac(normalizedDoc, HMAC_KEY_PREFIX + tenantId);

        // If checking doc hash, only throw duplicate if it belongs to another user
        Optional<CandidateProfile> existingUserOpt = candidateProfileRepository.findByUserIdAndTenantId(request.getUserId(), tenantId);
        if (existingUserOpt.isEmpty() && candidateProfileRepository.existsByIdentityDocHashAndTenantId(docHash, tenantId)) {
            throw new DuplicateProfileException(
                    "A profile with this identity document already exists");
        }

        // 4. If profile already exists for this user, update it idempotently
        CandidateProfile profile = existingUserOpt.orElseGet(() -> CandidateProfile.builder()
                .userId(request.getUserId())
                .encryptionKeyId(dekKeyName)
                .consentRecorded(false)
                .build());

        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationality(request.getNationality());
        profile.setCategory(request.getCategory());
        profile.setMobile(request.getMobile());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        profile.setReservationCategory(request.getReservationCategory());
        profile.setIdentityDocNumber(request.getIdentityDocNumber());
        profile.setMobileHash(mobileHash);
        profile.setIdentityDocHash(docHash);
        profile.setIdentityDocHmac(docHmac);
        profile.setTenantId(tenantId);

        // 5. Save
        CandidateProfile saved = candidateProfileRepository.save(profile);
        log.info("Created / saved candidate profile for userId={} in tenant={}", request.getUserId(), tenantId);

        // 6. Publish audit event (fire-and-forget — never blocks profile creation)
        publishAuditEvent(AuditEventType.CANDIDATE_PROFILE_CREATED, request.getUserId().toString(), tenantId);

        return toResponse(saved);
    }

    /**
     * Retrieves a candidate profile by userId and tenant, returning masked PII.
     */
    @Transactional(readOnly = true)
    public CandidateProfileResponse getByUserId(UUID userId, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));
        return toResponse(profile);
    }

    /**
     * Partially updates a candidate profile. Only non-null fields from the request are applied.
     * Recomputes hashes if mobile or identity doc changes.
     * Auto-provisions profile if not already present.
     */
    public CandidateProfileResponse update(UUID userId, UpdateCandidateProfileRequest request, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> {
                    log.info("Auto-initializing candidate profile during update for userId={} in tenant={}", userId, tenantId);
                    String dekKeyName = DEK_PREFIX + userId;
                    CandidateProfile newProfile = CandidateProfile.builder()
                            .userId(userId)
                            .encryptionKeyId(dekKeyName)
                            .mobileHash("PENDING-" + userId)
                            .identityDocHash("PENDING-" + userId)
                            .identityDocHmac("PENDING-" + userId)
                            .consentRecorded(false)
                            .build();
                    newProfile.setTenantId(tenantId);
                    return newProfile;
                });

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName().trim());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth().trim());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender().trim());
        }
        if (request.getNationality() != null) {
            profile.setNationality(request.getNationality().trim());
        }
        if (request.getCategory() != null) {
            profile.setCategory(request.getCategory().trim());
        }
        if (request.getMobile() != null && !request.getMobile().isBlank()) {
            // Recompute mobileHash and check uniqueness against OTHER candidates
            String mobileHash = hashingService.sha256(request.getMobile().trim());
            List<CandidateProfile> existingByMobile = candidateProfileRepository.findByMobileHashAndTenantId(mobileHash, tenantId);
            boolean duplicate = existingByMobile.stream().anyMatch(e -> !e.getUserId().equals(userId));
            if (duplicate) {
                throw new DuplicateProfileException("A profile with this mobile number already exists");
            }
            profile.setMobile(request.getMobile().trim());
            profile.setMobileHash(mobileHash);
        } else if (profile.getMobileHash() == null) {
            profile.setMobileHash("PENDING-" + userId);
        }

        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress().trim());
        }
        if (request.getReservationCategory() != null) {
            profile.setReservationCategory(request.getReservationCategory().trim());
        }
        if (request.getIdentityDocNumber() != null && !request.getIdentityDocNumber().isBlank()) {
            // Recompute docHash + docHmac
            String normalizedDoc = request.getIdentityDocNumber().trim().toUpperCase();
            String docHash = hashingService.sha256(normalizedDoc);
            String docHmac = hashingService.hmac(normalizedDoc, HMAC_KEY_PREFIX + tenantId);
            profile.setIdentityDocNumber(request.getIdentityDocNumber().trim());
            profile.setIdentityDocHash(docHash);
            profile.setIdentityDocHmac(docHmac);
        } else if (profile.getIdentityDocHash() == null) {
            profile.setIdentityDocHash("PENDING-" + userId);
            profile.setIdentityDocHmac("PENDING-" + userId);
        }

        if (request.getPhotoAssetId() != null) {
            profile.setPhotoAssetId(request.getPhotoAssetId());
        }
        if (request.getSignatureAssetId() != null) {
            profile.setSignatureAssetId(request.getSignatureAssetId());
        }
        if (request.getIdProofAssetId() != null) {
            profile.setIdProofAssetId(request.getIdProofAssetId());
        }

        CandidateProfile saved = candidateProfileRepository.save(profile);
        log.info("Updated candidate profile for userId={} in tenant={}", userId, tenantId);

        return toResponse(saved);
    }

    /**
     * DPDP erasure: zeroes all PII columns, removes DEK reference, and revokes the DEK.
     * Also erases associated candidate education records.
     */
    public void erasePii(UUID userId, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));

        // 1. Set ALL PII columns to null
        profile.setFullName(null);
        profile.setDateOfBirth(null);
        profile.setGender(null);
        profile.setNationality(null);
        profile.setCategory(null);
        profile.setMobile(null);
        profile.setEmail(null);
        profile.setAddress(null);
        profile.setReservationCategory(null);
        profile.setIdentityDocNumber(null);

        // 2. Set encryptionKeyId to null
        profile.setEncryptionKeyId(null);

        // 3. Set hash fields to "[ERASED]"
        profile.setMobileHash("[ERASED]");
        profile.setIdentityDocHash("[ERASED]");
        profile.setIdentityDocHmac("[ERASED]");

        // 4. Save
        candidateProfileRepository.save(profile);

        // 5. Erase associated educational qualification records
        candidateEducationRepository.deleteByUserIdAndTenantId(userId, tenantId);

        // 6. Revoke the DEK in Vault
        String dekKeyName = DEK_PREFIX + userId;
        vaultCryptoService.revokeKey(dekKeyName);

        log.info("DPDP erasure completed for userId={} in tenant={}", userId, tenantId);
    }

    /**
     * Records explicit consent before biometric data collection.
     * Sets consentRecorded=true and consentTimestamp. Idempotent — overwrites timestamp on re-consent.
     *
     * Validates: Requirements 25.3
     */
    public void recordConsent(UUID userId, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

        profile.setConsentRecorded(true);
        profile.setConsentTimestamp(java.time.LocalDateTime.now());
        candidateProfileRepository.save(profile);
        log.info("Consent recorded for userId={} at {}", userId, profile.getConsentTimestamp());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void publishAuditEvent(AuditEventType type, String actorId, String tenantId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", type.name());
            event.put("actorId", actorId);
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());

            eventPublisher.publish(AUDIT_TOPIC, actorId, event);
            log.debug("Audit event published [type={}, actor={}]", type, actorId);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", type, e.getMessage(), e);
        }
    }

    private CandidateProfileResponse toResponse(CandidateProfile profile) {
        return CandidateProfileResponse.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .nationality(profile.getNationality())
                .category(profile.getCategory())
                .mobile(maskMobile(profile.getMobile()))
                .email(maskEmail(profile.getEmail()))
                .address(profile.getAddress())
                .reservationCategory(profile.getReservationCategory())
                .digiLockerVerified(profile.getDigiLockerVerified())
                .faceVerificationStatus(profile.getFaceVerificationStatus())
                .consentRecorded(profile.isConsentRecorded())
                .photoAssetId(profile.getPhotoAssetId())
                .signatureAssetId(profile.getSignatureAssetId())
                .idProofAssetId(profile.getIdProofAssetId())
                .build();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() <= 4) {
            return mobile;
        }
        return "****" + mobile.substring(mobile.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "**" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "****" + email.substring(atIndex);
    }
}
