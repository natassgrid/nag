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
import com.examplatform.shared.crypto.HashingService;
import com.examplatform.shared.crypto.VaultCryptoService;
import com.examplatform.shared.event.UserAuditEvent;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service handling candidate profile CRUD operations with per-candidate DEK,
 * SHA-256 hashing for uniqueness, HMAC for duplicate detection, and DPDP erasure.
 *
 * Validates: Requirements 1.6, 25.2
 */
@Slf4j
@Service
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
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public CandidateProfileService(
            CandidateProfileRepository candidateProfileRepository,
            CandidateEducationRepository candidateEducationRepository,
            HashingService hashingService,
            VaultCryptoService vaultCryptoService,
            EventPublisher eventPublisher,
            @Autowired(required = false) ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateEducationRepository = candidateEducationRepository;
        this.hashingService = hashingService;
        this.vaultCryptoService = vaultCryptoService;
        this.eventPublisher = eventPublisher;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    /**
     * Creates a new candidate profile or updates an existing one (upsert).
     * Computes SHA-256 hashes and HMACs for uniqueness and duplicate detection.
     * Generates a DEK reference via Vault.
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

        profile.applyDetails(request);
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
     * Retrieves a candidate profile by userId and tenant.
     * Auto-provisions a default profile if one does not exist yet.
     */
    public CandidateProfileResponse getByUserId(UUID userId, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> {
                    log.info("No profile found for userId={}. Auto-initializing default profile in tenant={}", userId, tenantId);
                    return candidateProfileRepository.save(initDefaultProfile(userId, tenantId));
                });

        // Backfill email / username from identity_service.user_account if email is missing
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider != null ? jdbcTemplateProvider.getIfAvailable() : null;
        if (jdbcTemplate != null && (profile.getEmail() == null || profile.getEmail().isBlank())) {
            try {
                String username = jdbcTemplate.queryForObject(
                        "SELECT username FROM identity_service.user_account WHERE id = ?",
                        String.class,
                        userId
                );
                if (username != null && !username.isBlank()) {
                    profile.setEmail(username);
                    if (profile.getFullName() == null || profile.getFullName().isBlank()) {
                        if (!username.contains("@")) {
                            profile.setFullName(username);
                        }
                    }
                    candidateProfileRepository.save(profile);
                }
            } catch (Exception e) {
                log.debug("Could not lookup user_account for candidate profile backfill: {}", e.getMessage());
            }
        }

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
                    return initDefaultProfile(userId, tenantId);
                });

        applyTextUpdates(profile, request);

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

        Optional.ofNullable(request.getPhotoAssetId()).ifPresent(profile::setPhotoAssetId);
        Optional.ofNullable(request.getSignatureAssetId()).ifPresent(profile::setSignatureAssetId);
        Optional.ofNullable(request.getIdProofAssetId()).ifPresent(profile::setIdProofAssetId);

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
        profile.setCountry(null);
        profile.setState(null);
        profile.setDistrict(null);
        profile.setCity(null);
        profile.setPinCode(null);
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

    /**
     * Operational candidate metrics for dashboards and inter-service queries.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCandidateMetrics(String tenantId) {
        long total;
        if (tenantId == null || "default".equalsIgnoreCase(tenantId) || tenantId.isBlank()) {
            total = candidateProfileRepository.count();
        } else {
            total = candidateProfileRepository.countByTenantId(tenantId);
        }
        return Map.of(
                "totalRegisteredCandidates", total,
                "activeCandidates", total
        );
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void applyTextUpdates(CandidateProfile profile, UpdateCandidateProfileRequest request) {
        setTrimmedIfPresent(profile::setFullName, request.getFullName());
        setTrimmedIfPresent(profile::setDateOfBirth, request.getDateOfBirth());
        setTrimmedIfPresent(profile::setGender, request.getGender());
        setTrimmedIfPresent(profile::setNationality, request.getNationality());
        setTrimmedIfPresent(profile::setCategory, request.getCategory());
        setTrimmedIfPresent(profile::setEmail, request.getEmail());
        setTrimmedIfPresent(profile::setAddress, request.getAddress());
        setTrimmedIfPresent(profile::setCountry, request.getCountry());
        setTrimmedIfPresent(profile::setState, request.getState());
        setTrimmedIfPresent(profile::setDistrict, request.getDistrict());
        setTrimmedIfPresent(profile::setCity, request.getCity());
        setTrimmedIfPresent(profile::setPinCode, request.getPinCode());
        setTrimmedIfPresent(profile::setReservationCategory, request.getReservationCategory());
    }

    private static void setTrimmedIfPresent(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private void publishAuditEvent(AuditEventType type, String actorId, String tenantId) {
        try {
            UserAuditEvent event = UserAuditEvent.of(type.name(), actorId, tenantId);
            eventPublisher.publish(AUDIT_TOPIC, actorId, event);
            log.debug("Audit event published [type={}, actor={}]", type, actorId);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", type, e.getMessage(), e);
        }
    }

    private CandidateProfileResponse toResponse(CandidateProfile profile) {
        return CandidateProfileResponse.fromEntity(profile);
    }

    private CandidateProfile initDefaultProfile(UUID userId, String tenantId) {
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
    }
}
