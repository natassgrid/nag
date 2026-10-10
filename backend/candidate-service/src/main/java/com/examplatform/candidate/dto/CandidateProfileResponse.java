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

package com.examplatform.candidate.dto;

import com.examplatform.candidate.domain.CandidateProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

/**
 * DTO returned to candidates or admin callers representing a candidate profile.
 * Plaintext PII fields are included here only after being decrypted by the service layer.
 *
 * Validates: Requirements 1.6, 25.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileResponse {

    private UUID userId;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String nationality;
    private String category;
    private String mobile;
    private String email;
    private String address;
    private String country;
    private String state;
    private String district;
    private String city;
    private String pinCode;
    private String reservationCategory;
    private String digiLockerVerified;
    private String faceVerificationStatus;
    private boolean consentRecorded;
    private UUID photoAssetId;
    private UUID signatureAssetId;
    private UUID idProofAssetId;

    public static CandidateProfileResponse fromEntity(CandidateProfile profile) {
        if (profile == null) {
            return null;
        }
        CandidateProfileResponse resp = new CandidateProfileResponse();
        BeanUtils.copyProperties(profile, resp);
        return resp;
    }
}
