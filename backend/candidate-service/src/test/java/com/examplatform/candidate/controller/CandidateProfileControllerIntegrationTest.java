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

package com.examplatform.candidate.controller;

import com.examplatform.candidate.dto.*;
import com.examplatform.candidate.exception.DuplicateProfileException;
import com.examplatform.candidate.exception.EducationNotFoundException;
import com.examplatform.candidate.exception.ProfileNotFoundException;
import com.examplatform.candidate.service.CandidateEducationService;
import com.examplatform.candidate.service.CandidateProfileService;
import com.examplatform.candidate.service.DigiLockerService;
import com.examplatform.candidate.service.FaceVerificationService;
import com.examplatform.candidate.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CandidateProfileController REST Endpoints E2E Tests (MockMvc)")
class CandidateProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private CandidateProfileService candidateProfileService;

    @MockitoBean
    private CandidateEducationService candidateEducationService;

    @MockitoBean
    private DigiLockerService digiLockerService;

    @MockitoBean
    private FaceVerificationService faceVerificationService;

    private static final String TENANT_ID = "default";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EDUCATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // =========================================================================
    // 1. POST /api/v1/candidates (Create Candidate Profile)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/candidates")
    class CreateProfileEndpoint {

        @Test
        @DisplayName("+ve: Candidate creates own profile - returns 201 Created")
        void candidateCanCreateOwnProfile() throws Exception {
            CreateCandidateProfileRequest request = CreateCandidateProfileRequest.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .dateOfBirth("2000-01-15")
                    .gender("Male")
                    .nationality("Indian")
                    .category("GENERAL")
                    .mobile("+919876543210")
                    .email("aarav.sharma@example.com")
                    .identityDocNumber("123456789012")
                    .build();

            CandidateProfileResponse response = CandidateProfileResponse.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .email("a***@example.com")
                    .mobile("******3210")
                    .build();

            when(candidateProfileService.create(any(CreateCandidateProfileRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/candidates")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.fullName").value("Aarav Sharma"));
        }

        @Test
        @DisplayName("-ve: Candidate cannot create profile for another userId - returns 403 Forbidden")
        void candidateCannotCreateProfileForAnotherUser() throws Exception {
            CreateCandidateProfileRequest request = CreateCandidateProfileRequest.builder()
                    .userId(OTHER_CANDIDATE_ID)
                    .fullName("Spoofed Candidate")
                    .dateOfBirth("2000-01-15")
                    .gender("Male")
                    .nationality("Indian")
                    .mobile("+919876543210")
                    .email("spoofed@example.com")
                    .identityDocNumber("123456789012")
                    .build();

            mockMvc.perform(post("/api/v1/candidates")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @DisplayName("-ve: Blank required fields return 400 Bad Request with fieldErrors")
        void blankRequiredFieldsReturnBadRequest() throws Exception {
            CreateCandidateProfileRequest invalidRequest = CreateCandidateProfileRequest.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("") // Blank
                    .dateOfBirth("") // Blank
                    .build();

            mockMvc.perform(post("/api/v1/candidates")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        }

        @Test
        @DisplayName("-ve: Duplicate candidate profile returns 409 Conflict")
        void duplicateProfileReturnsConflict() throws Exception {
            CreateCandidateProfileRequest request = CreateCandidateProfileRequest.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .dateOfBirth("2000-01-15")
                    .gender("Male")
                    .nationality("Indian")
                    .mobile("+919876543210")
                    .email("aarav.sharma@example.com")
                    .identityDocNumber("123456789012")
                    .build();

            when(candidateProfileService.create(any(CreateCandidateProfileRequest.class), eq(TENANT_ID)))
                    .thenThrow(new DuplicateProfileException("Profile already exists for user: " + CANDIDATE_ID));

            mockMvc.perform(post("/api/v1/candidates")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedRequestReturnsUnauthorized() throws Exception {
            CreateCandidateProfileRequest request = CreateCandidateProfileRequest.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .dateOfBirth("2000-01-15")
                    .gender("Male")
                    .nationality("Indian")
                    .mobile("+919876543210")
                    .email("aarav.sharma@example.com")
                    .identityDocNumber("123456789012")
                    .build();

            mockMvc.perform(post("/api/v1/candidates")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/candidates/{userId} (Get Profile)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/candidates/{userId}")
    class GetProfileEndpoint {

        @Test
        @DisplayName("+ve: Candidate retrieves own profile - returns 200 OK")
        void candidateCanGetOwnProfile() throws Exception {
            CandidateProfileResponse response = CandidateProfileResponse.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .build();

            when(candidateProfileService.getByUserId(eq(CANDIDATE_ID), eq(TENANT_ID))).thenReturn(response);

            mockMvc.perform(get("/api/v1/candidates/{userId}", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.fullName").value("Aarav Sharma"));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves any candidate profile - returns 200 OK")
        void superAdminCanGetAnyProfile() throws Exception {
            CandidateProfileResponse response = CandidateProfileResponse.builder()
                    .userId(CANDIDATE_ID)
                    .fullName("Aarav Sharma")
                    .build();

            when(candidateProfileService.getByUserId(eq(CANDIDATE_ID), eq(TENANT_ID))).thenReturn(response);

            mockMvc.perform(get("/api/v1/candidates/{userId}", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.claim("realm_access.roles", List.of("SUPER_ADMIN")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(CANDIDATE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Candidate cannot retrieve another candidate's profile - returns 403 Forbidden")
        void candidateCannotGetOtherProfile() throws Exception {
            mockMvc.perform(get("/api/v1/candidates/{userId}", OTHER_CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Non-existent profile throws ProfileNotFoundException - returns 404 Not Found")
        void nonExistentProfileReturnsNotFound() throws Exception {
            when(candidateProfileService.getByUserId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenThrow(new ProfileNotFoundException("Profile not found for user: " + CANDIDATE_ID));

            mockMvc.perform(get("/api/v1/candidates/{userId}", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // 3. PUT /api/v1/candidates/{userId} (Update Profile)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/candidates/{userId}")
    class UpdateProfileEndpoint {

        @Test
        @DisplayName("+ve: Candidate updates own profile - returns 200 OK")
        void candidateCanUpdateOwnProfile() throws Exception {
            UpdateCandidateProfileRequest request = UpdateCandidateProfileRequest.builder()
                    .address("New Address, Delhi")
                    .build();

            CandidateProfileResponse response = CandidateProfileResponse.builder()
                    .userId(CANDIDATE_ID)
                    .address("New Address, Delhi")
                    .build();

            when(candidateProfileService.update(eq(CANDIDATE_ID), any(UpdateCandidateProfileRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/candidates/{userId}", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address").value("New Address, Delhi"));
        }

        @Test
        @DisplayName("-ve: Candidate cannot update another candidate's profile - returns 403 Forbidden")
        void candidateCannotUpdateOtherProfile() throws Exception {
            UpdateCandidateProfileRequest request = UpdateCandidateProfileRequest.builder()
                    .address("Hacked Address")
                    .build();

            mockMvc.perform(put("/api/v1/candidates/{userId}", OTHER_CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. DELETE /api/v1/candidates/{userId}/pii (DPDP Erasure)
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/v1/candidates/{userId}/pii")
    class ErasePiiEndpoint {

        @Test
        @DisplayName("+ve: Candidate requests erasure of own PII - returns 204 No Content")
        void candidateCanEraseOwnPii() throws Exception {
            doNothing().when(candidateProfileService).erasePii(eq(CANDIDATE_ID), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/candidates/{userId}/pii", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN requests erasure of candidate PII - returns 204 No Content")
        void superAdminCanEraseCandidatePii() throws Exception {
            doNothing().when(candidateProfileService).erasePii(eq(CANDIDATE_ID), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/candidates/{userId}/pii", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.claim("realm_access.roles", List.of("SUPER_ADMIN")))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("-ve: Candidate cannot erase another candidate's PII - returns 403 Forbidden")
        void candidateCannotEraseOtherPii() throws Exception {
            mockMvc.perform(delete("/api/v1/candidates/{userId}/pii", OTHER_CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. Educational Details Endpoints (CRUD)
    // =========================================================================
    @Nested
    @DisplayName("Educational Details Endpoints")
    class EducationEndpoints {

        @Test
        @DisplayName("+ve: Candidate gets own educational records - returns 200 OK")
        void candidateCanGetEducation() throws Exception {
            CandidateEducationResponse response = CandidateEducationResponse.builder()
                    .id(EDUCATION_ID)
                    .userId(CANDIDATE_ID)
                    .qualification("10th Standard")
                    .boardOrUniversity("CBSE")
                    .passingYear(2016)
                    .percentageOrCgpa(new BigDecimal("92.5"))
                    .build();

            when(candidateEducationService.getEducationByUserId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/candidates/{userId}/education", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(EDUCATION_ID.toString()))
                    .andExpect(jsonPath("$[0].qualification").value("10th Standard"));
        }

        @Test
        @DisplayName("+ve: Candidate adds educational qualification - returns 201 Created")
        void candidateCanAddEducation() throws Exception {
            CandidateEducationRequest request = CandidateEducationRequest.builder()
                    .qualification("Bachelor of Technology")
                    .boardOrUniversity("Delhi Technological University")
                    .passingYear(2022)
                    .percentageOrCgpa(new BigDecimal("8.4"))
                    .build();

            CandidateEducationResponse response = CandidateEducationResponse.builder()
                    .id(EDUCATION_ID)
                    .userId(CANDIDATE_ID)
                    .qualification(request.getQualification())
                    .boardOrUniversity(request.getBoardOrUniversity())
                    .passingYear(request.getPassingYear())
                    .build();

            when(candidateEducationService.addEducation(eq(CANDIDATE_ID), any(CandidateEducationRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/candidates/{userId}/education", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(EDUCATION_ID.toString()))
                    .andExpect(jsonPath("$.qualification").value("Bachelor of Technology"));
        }

        @Test
        @DisplayName("-ve: Invalid passing year (< 1950) returns 400 Bad Request")
        void invalidPassingYearReturnsBadRequest() throws Exception {
            CandidateEducationRequest invalidRequest = CandidateEducationRequest.builder()
                    .qualification("Bachelor of Technology")
                    .boardOrUniversity("DTU")
                    .passingYear(1940) // Below min 1950
                    .build();

            mockMvc.perform(post("/api/v1/candidates/{userId}/education", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.passingYear").exists());
        }

        @Test
        @DisplayName("+ve: Candidate updates educational record - returns 200 OK")
        void candidateCanUpdateEducation() throws Exception {
            CandidateEducationRequest request = CandidateEducationRequest.builder()
                    .qualification("Master of Technology")
                    .boardOrUniversity("IIT Delhi")
                    .passingYear(2024)
                    .build();

            CandidateEducationResponse response = CandidateEducationResponse.builder()
                    .id(EDUCATION_ID)
                    .userId(CANDIDATE_ID)
                    .qualification("Master of Technology")
                    .build();

            when(candidateEducationService.updateEducation(eq(CANDIDATE_ID), eq(EDUCATION_ID), any(CandidateEducationRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/candidates/{userId}/education/{educationId}", CANDIDATE_ID, EDUCATION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.qualification").value("Master of Technology"));
        }

        @Test
        @DisplayName("-ve: Non-existent education record returns 404 Not Found")
        void nonExistentEducationReturnsNotFound() throws Exception {
            CandidateEducationRequest request = CandidateEducationRequest.builder()
                    .qualification("Master of Technology")
                    .boardOrUniversity("IIT Delhi")
                    .passingYear(2024)
                    .build();

            when(candidateEducationService.updateEducation(eq(CANDIDATE_ID), eq(EDUCATION_ID), any(CandidateEducationRequest.class), eq(TENANT_ID)))
                    .thenThrow(new EducationNotFoundException("Education record not found: " + EDUCATION_ID));

            mockMvc.perform(put("/api/v1/candidates/{userId}/education/{educationId}", CANDIDATE_ID, EDUCATION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("+ve: Candidate deletes educational record - returns 204 No Content")
        void candidateCanDeleteEducation() throws Exception {
            doNothing().when(candidateEducationService).deleteEducation(eq(CANDIDATE_ID), eq(EDUCATION_ID), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/candidates/{userId}/education/{educationId}", CANDIDATE_ID, EDUCATION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("-ve: Candidate cannot delete another candidate's education - returns 403 Forbidden")
        void candidateCannotDeleteOtherEducation() throws Exception {
            mockMvc.perform(delete("/api/v1/candidates/{userId}/education/{educationId}", OTHER_CANDIDATE_ID, EDUCATION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 6. Verification and Consent Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Verification and Consent Endpoints")
    class VerificationEndpoints {

        @Test
        @DisplayName("+ve: Candidate records biometric consent (consentGiven=true) - returns 200 OK")
        void candidateRecordsConsent() throws Exception {
            ConsentRequest request = ConsentRequest.builder()
                    .consentGiven(true)
                    .build();

            doNothing().when(candidateProfileService).recordConsent(eq(CANDIDATE_ID), eq(TENANT_ID));

            mockMvc.perform(post("/api/v1/candidates/{userId}/consent", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("-ve: Candidate records consent with false - returns 400 Bad Request")
        void consentFalseReturnsBadRequest() throws Exception {
            ConsentRequest request = ConsentRequest.builder()
                    .consentGiven(false)
                    .build();

            mockMvc.perform(post("/api/v1/candidates/{userId}/consent", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("+ve: Candidate verifies DigiLocker document - returns 200 OK")
        void candidateVerifiesDigiLocker() throws Exception {
            when(digiLockerService.verifyDocument(eq(CANDIDATE_ID), eq(TENANT_ID))).thenReturn("VERIFIED");

            mockMvc.perform(post("/api/v1/candidates/{userId}/digilocker/verify", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VERIFIED"));
        }

        @Test
        @DisplayName("+ve: Candidate verifies face against document photo - returns 200 OK")
        void candidateVerifiesFace() throws Exception {
            FaceVerificationRequest request = FaceVerificationRequest.builder()
                    .userId(CANDIDATE_ID)
                    .photoEmbedding(new float[]{0.1f, 0.2f, 0.3f})
                    .docPhotoEmbedding(new float[]{0.1f, 0.2f, 0.3f})
                    .build();

            when(faceVerificationService.verifyFace(eq(CANDIDATE_ID), any(FaceVerificationRequest.class), eq(TENANT_ID)))
                    .thenReturn("MATCH");

            mockMvc.perform(post("/api/v1/candidates/{userId}/face/verify", CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("MATCH"));
        }

        @Test
        @DisplayName("-ve: Candidate attempts face verification for another user - returns 403 Forbidden")
        void candidateCannotVerifyFaceForOther() throws Exception {
            FaceVerificationRequest request = FaceVerificationRequest.builder()
                    .userId(OTHER_CANDIDATE_ID)
                    .photoEmbedding(new float[]{0.1f, 0.2f, 0.3f})
                    .docPhotoEmbedding(new float[]{0.1f, 0.2f, 0.3f})
                    .build();

            mockMvc.perform(post("/api/v1/candidates/{userId}/face/verify", OTHER_CANDIDATE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
