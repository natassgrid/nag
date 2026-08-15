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
public class CandidateProfile extends BaseEntity {

    // ── Encrypted PII fields ─────────────────────────────────────────────────

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "full_name")
    private String fullName;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "gender")
    private String gender;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "nationality")
    private String nationality;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "category")
    private String category;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "mobile")
    private String mobile;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "email")
    private String email;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "address")
    private String address;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "reservation_category")
    private String reservationCategory;

    @Convert(converter = EncryptedFieldConverter.class)
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

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
