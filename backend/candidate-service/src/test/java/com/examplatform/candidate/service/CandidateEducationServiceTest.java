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
 * GNU标志 Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.candidate.service;

import com.examplatform.candidate.domain.CandidateEducation;
import com.examplatform.candidate.dto.CandidateEducationRequest;
import com.examplatform.candidate.dto.CandidateEducationResponse;
import com.examplatform.candidate.exception.EducationNotFoundException;
import com.examplatform.candidate.repository.CandidateEducationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateEducationService")
class CandidateEducationServiceTest {

    @Mock
    CandidateEducationRepository candidateEducationRepository;

    @InjectMocks
    CandidateEducationService candidateEducationService;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EDUCATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CandidateEducation sampleEducation() {
        CandidateEducation edu = CandidateEducation.builder()
                .userId(USER_ID)
                .qualification("GRADUATE")
                .courseName("B.Tech Computer Science")
                .boardOrUniversity("Delhi Technological University")
                .institutionName("Department of Computer Science")
                .passingYear(2022)
                .percentageOrCgpa(new BigDecimal("85.50"))
                .gradeOrDivision("First Class with Distinction")
                .specialization("Computer Science")
                .rollNumber("2K18/CO/100")
                .build();
        edu.setTenantId(TENANT_ID);
        return edu;
    }

    private CandidateEducationRequest sampleRequest() {
        return CandidateEducationRequest.builder()
                .qualification("GRADUATE")
                .courseName("B.Tech Computer Science")
                .boardOrUniversity("Delhi Technological University")
                .institutionName("Department of Computer Science")
                .passingYear(2022)
                .percentageOrCgpa(new BigDecimal("85.50"))
                .gradeOrDivision("First Class with Distinction")
                .specialization("Computer Science")
                .rollNumber("2K18/CO/100")
                .build();
    }

    @Nested
    @DisplayName("getEducationByUserId")
    class GetEducationByUserId {

        @Test
        @DisplayName("returns list of candidate educational records")
        void returnsListOfRecords() {
            when(candidateEducationRepository.findByUserIdAndTenantIdOrderByPassingYearAsc(USER_ID, TENANT_ID))
                    .thenReturn(List.of(sampleEducation()));

            List<CandidateEducationResponse> result = candidateEducationService.getEducationByUserId(USER_ID, TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQualification()).isEqualTo("GRADUATE");
            assertThat(result.get(0).getPassingYear()).isEqualTo(2022);
            assertThat(result.get(0).getBoardOrUniversity()).isEqualTo("Delhi Technological University");
        }
    }

    @Nested
    @DisplayName("addEducation")
    class AddEducation {

        @Test
        @DisplayName("saves and returns new education record")
        void savesNewEducationRecord() {
            CandidateEducationRequest request = sampleRequest();
            when(candidateEducationRepository.save(any(CandidateEducation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateEducationResponse response = candidateEducationService.addEducation(USER_ID, request, TENANT_ID);

            ArgumentCaptor<CandidateEducation> captor = ArgumentCaptor.forClass(CandidateEducation.class);
            verify(candidateEducationRepository).save(captor.capture());

            CandidateEducation saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getQualification()).isEqualTo("GRADUATE");
            assertThat(saved.getPassingYear()).isEqualTo(2022);
            assertThat(saved.getPercentageOrCgpa()).isEqualTo(new BigDecimal("85.50"));

            assertThat(response.getQualification()).isEqualTo("GRADUATE");
            assertThat(response.getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("updateEducation")
    class UpdateEducation {

        @Test
        @DisplayName("updates existing education record")
        void updatesExistingRecord() {
            CandidateEducation existing = sampleEducation();
            when(candidateEducationRepository.findByIdAndUserIdAndTenantId(EDUCATION_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(candidateEducationRepository.save(any(CandidateEducation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CandidateEducationRequest updateReq = sampleRequest();
            updateReq.setPercentageOrCgpa(new BigDecimal("90.00"));
            updateReq.setCourseName("B.Tech CSE (Hons)");

            CandidateEducationResponse response = candidateEducationService.updateEducation(USER_ID, EDUCATION_ID, updateReq, TENANT_ID);

            assertThat(response.getCourseName()).isEqualTo("B.Tech CSE (Hons)");
            assertThat(response.getPercentageOrCgpa()).isEqualTo(new BigDecimal("90.00"));
        }

        @Test
        @DisplayName("throws EducationNotFoundException when record does not exist")
        void throwsWhenNotFound() {
            when(candidateEducationRepository.findByIdAndUserIdAndTenantId(EDUCATION_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidateEducationService.updateEducation(USER_ID, EDUCATION_ID, sampleRequest(), TENANT_ID))
                    .isInstanceOf(EducationNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("deleteEducation")
    class DeleteEducation {

        @Test
        @DisplayName("deletes existing education record")
        void deletesExistingRecord() {
            CandidateEducation existing = sampleEducation();
            when(candidateEducationRepository.findByIdAndUserIdAndTenantId(EDUCATION_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            candidateEducationService.deleteEducation(USER_ID, EDUCATION_ID, TENANT_ID);

            verify(candidateEducationRepository).delete(existing);
        }

        @Test
        @DisplayName("throws EducationNotFoundException when trying to delete non-existent record")
        void throwsWhenDeletingNonExistent() {
            when(candidateEducationRepository.findByIdAndUserIdAndTenantId(EDUCATION_ID, USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> candidateEducationService.deleteEducation(USER_ID, EDUCATION_ID, TENANT_ID))
                    .isInstanceOf(EducationNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }
}
