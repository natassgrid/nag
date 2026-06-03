package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.dto.CandidateProfileResponse;
import com.examplatform.candidate.dto.CreateCandidateProfileRequest;
import com.examplatform.candidate.dto.UpdateCandidateProfileRequest;
import com.examplatform.candidate.exception.DuplicateProfileException;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CandidateProfileRepository candidateProfileRepository;
    private final HashingService hashingService;
    private final VaultCryptoService vaultCryptoService;

    /**
     * Creates a new candidate profile with per-candidate DEK reference,
     * mobile hash for uniqueness, and identity doc hash + HMAC for duplicate detection.
     */
    public CandidateProfileResponse create(CreateCandidateProfileRequest request, String tenantId) {
        // 1. Generate per-candidate DEK key name
        String dekKeyName = DEK_PREFIX + request.getUserId();

        // 2. Hash mobile (SHA-256) for uniqueness check
        String mobileHash = hashingService.sha256(request.getMobile().trim());
        candidateProfileRepository.findByMobileHashAndTenantId(mobileHash, tenantId)
                .ifPresent(existing -> {
                    throw new DuplicateProfileException(
                            "A profile with this mobile number already exists");
                });

        // 3. Hash + HMAC identity doc for duplicate detection
        String normalizedDoc = request.getIdentityDocNumber().trim().toUpperCase();
        String docHash = hashingService.sha256(normalizedDoc);
        String docHmac = hashingService.hmac(normalizedDoc, HMAC_KEY_PREFIX + tenantId);

        if (candidateProfileRepository.existsByIdentityDocHashAndTenantId(docHash, tenantId)) {
            throw new DuplicateProfileException(
                    "A profile with this identity document already exists");
        }

        // 4. Build CandidateProfile entity (PII fields auto-encrypted by EncryptedFieldConverter)
        CandidateProfile profile = CandidateProfile.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .nationality(request.getNationality())
                .category(request.getCategory())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .address(request.getAddress())
                .reservationCategory(request.getReservationCategory())
                .identityDocNumber(request.getIdentityDocNumber())
                .mobileHash(mobileHash)
                .identityDocHash(docHash)
                .identityDocHmac(docHmac)
                .encryptionKeyId(dekKeyName)
                .consentRecorded(false)
                .build();

        // 5. Set tenant
        profile.setTenantId(tenantId);

        // 6. Save
        CandidateProfile saved = candidateProfileRepository.save(profile);
        log.info("Created candidate profile for userId={} in tenant={}", request.getUserId(), tenantId);

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
     */
    public CandidateProfileResponse update(UUID userId, UpdateCandidateProfileRequest request, String tenantId) {
        CandidateProfile profile = candidateProfileRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId=" + userId));

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getNationality() != null) {
            profile.setNationality(request.getNationality());
        }
        if (request.getCategory() != null) {
            profile.setCategory(request.getCategory());
        }
        if (request.getMobile() != null) {
            // Recompute mobileHash
            String mobileHash = hashingService.sha256(request.getMobile().trim());
            profile.setMobile(request.getMobile());
            profile.setMobileHash(mobileHash);
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getReservationCategory() != null) {
            profile.setReservationCategory(request.getReservationCategory());
        }
        if (request.getIdentityDocNumber() != null) {
            // Recompute docHash + docHmac
            String normalizedDoc = request.getIdentityDocNumber().trim().toUpperCase();
            String docHash = hashingService.sha256(normalizedDoc);
            String docHmac = hashingService.hmac(normalizedDoc, HMAC_KEY_PREFIX + tenantId);
            profile.setIdentityDocNumber(request.getIdentityDocNumber());
            profile.setIdentityDocHash(docHash);
            profile.setIdentityDocHmac(docHmac);
        }

        CandidateProfile saved = candidateProfileRepository.save(profile);
        log.info("Updated candidate profile for userId={} in tenant={}", userId, tenantId);

        return toResponse(saved);
    }

    /**
     * DPDP erasure: zeroes all PII columns, removes DEK reference, and revokes the DEK.
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

        // 5. Revoke the DEK in Vault
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
