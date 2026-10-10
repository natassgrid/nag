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

import java.util.UUID;

/**
 * Response DTO for candidate profile with masked PII fields.
 *
 * Validates: Requirements 1.6
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
    private String mobile;       // masked: last 4 digits only
    private String email;        // masked
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
        resp.setUserId(profile.getUserId());
        resp.setFullName(profile.getFullName());
        resp.setDateOfBirth(profile.getDateOfBirth());
        resp.setGender(profile.getGender());
        resp.setNationality(profile.getNationality());
        resp.setCategory(profile.getCategory());
        resp.setMobile(profile.getMobile());
        resp.setEmail(profile.getEmail());
        resp.setAddress(profile.getAddress());
        resp.setCountry(profile.getCountry());
        resp.setState(profile.getState());
        resp.setDistrict(profile.getDistrict());
        resp.setCity(profile.getCity());
        resp.setPinCode(profile.getPinCode());
        resp.setReservationCategory(profile.getReservationCategory());
        resp.setDigiLockerVerified(profile.getDigiLockerVerified());
        resp.setFaceVerificationStatus(profile.getFaceVerificationStatus());
        resp.setConsentRecorded(profile.isConsentRecorded());
        resp.setPhotoAssetId(profile.getPhotoAssetId());
        resp.setSignatureAssetId(profile.getSignatureAssetId());
        resp.setIdProofAssetId(profile.getIdProofAssetId());
        return resp;
    }
}
