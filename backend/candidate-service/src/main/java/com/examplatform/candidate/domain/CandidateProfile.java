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

package com.examplatform.candidate.domain;

import com.examplatform.candidate.crypto.EncryptedFieldConverter;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Candidate profile entity with AES-256 column encryption for all PII fields.
 * Non-PII hash fields enable uniqueness and duplicate detection without
 * exposing plaintext values.
 *
 * Validates: Requirements 1.6, 16.1, 25.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "candidate_profile", schema = "candidate_service")
@Converts({
        @Convert(attributeName = "fullName", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "dateOfBirth", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "gender", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "nationality", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "category", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "mobile", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "email", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "address", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "country", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "state", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "district", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "city", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "pinCode", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "reservationCategory", converter = EncryptedFieldConverter.class),
        @Convert(attributeName = "identityDocNumber", converter = EncryptedFieldConverter.class)
})
public class CandidateProfile extends BaseEntity {

    // ── Encrypted PII fields ─────────────────────────────────────────────────

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "category")
    private String category;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "country")
    private String country;

    @Column(name = "state")
    private String state;

    @Column(name = "district")
    private String district;

    @Column(name = "city")
    private String city;

    @Column(name = "pin_code")
    private String pinCode;

    @Column(name = "reservation_category")
    private String reservationCategory;

    @Column(name = "identity_doc_number")
    private String identityDocNumber;

    // ── Non-encrypted fields ─────────────────────────────────────────────────

    @Column(name = "mobile_hash", nullable = false, length = 64)
    private String mobileHash;

    @Column(name = "identity_doc_hash", nullable = false, length = 64)
    private String identityDocHash;

    @Column(name = "identity_doc_hmac", nullable = false, length = 64)
    private String identityDocHmac;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "digi_locker_verified", length = 20)
    private String digiLockerVerified;

    @Column(name = "face_verification_status", length = 20)
    private String faceVerificationStatus;

    @Column(name = "consent_recorded")
    private boolean consentRecorded;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;

    @Column(name = "photo_asset_id")
    private UUID photoAssetId;

    @Column(name = "signature_asset_id")
    private UUID signatureAssetId;

    @Column(name = "id_proof_asset_id")
    private UUID idProofAssetId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
