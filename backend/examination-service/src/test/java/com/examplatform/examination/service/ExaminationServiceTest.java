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

package com.examplatform.examination.service;

import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.repository.ExaminationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExaminationService")
class ExaminationServiceTest {

    @Mock
    private ExaminationRepository examinationRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ExaminationService examinationService;

    private static final String TENANT_ID = "tenant-authority-1";

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist examination in DRAFT status with valid sections")
        void shouldCreateExamInDraftStatus() {
            // Given: 2 sections, each with 10 questions × 5 marks = 50 marks each → total 100
            List<Section> sections = List.of(
                    Section.builder()
                            .name("Physics")
                            .subject("Physics")
                            .questionCount(10)
                            .marksPerQuestion(5.0)
                            .negativeMarksPerQuestion(1.0)
                            .build(),
                    Section.builder()
                            .name("Chemistry")
                            .subject("Chemistry")
                            .questionCount(10)
                            .marksPerQuestion(5.0)
                            .negativeMarksPerQuestion(1.0)
                            .build()
            );

            CreateExaminationRequest request = CreateExaminationRequest.builder()
                    .name("JEE Main 2025")
                    .durationMinutes(180)
                    .totalMarks(100)
                    .negativeMarkingEnabled(true)
                    .negativeMarkingValue(1.0)
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .calculatorPolicy(CalculatorPolicy.SCIENTIFIC)
                    .reviewFlagEnabled(true)
                    .sections(sections)
                    .build();

            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> {
                Examination exam = invocation.getArgument(0);
                // Simulate BaseEntity prePersist
                try {
                    var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(exam, UUID.randomUUID());
                    var createdAtField = exam.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(exam, Instant.now());
                } catch (Exception e) {
                    // reflection-based setup for unit test
                }
                return exam;
            });

            // When
            ExaminationResponse response = examinationService.create(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo("DRAFT");
            assertThat(response.getName()).isEqualTo("JEE Main 2025");
            assertThat(response.getDurationMinutes()).isEqualTo(180);
            assertThat(response.getTotalMarks()).isEqualTo(100);
            assertThat(response.isNegativeMarkingEnabled()).isTrue();
            assertThat(response.getNegativeMarkingValue()).isEqualTo(1.0);
            assertThat(response.getNavigationPolicy()).isEqualTo("FLEXIBLE");
            assertThat(response.getCalculatorPolicy()).isEqualTo("SCIENTIFIC");
            assertThat(response.isReviewFlagEnabled()).isTrue();
            assertThat(response.getSections()).hasSize(2);

            // Verify save was called with correct tenant
            ArgumentCaptor<Examination> captor = ArgumentCaptor.forClass(Examination.class);
            verify(examinationRepository).save(captor.capture());
            Examination saved = captor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getStatus()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("should throw SectionMarksValidationException when section marks do not match totalMarks")
        void shouldThrowWhenSectionMarksMismatch() {
            // Given: 1 section with 10 questions × 5 marks = 50, but totalMarks declared as 100
            List<Section> sections = List.of(
                    Section.builder()
                            .name("Mathematics")
                            .subject("Mathematics")
                            .questionCount(10)
                            .marksPerQuestion(5.0)
                            .build()
            );

            CreateExaminationRequest request = CreateExaminationRequest.builder()
                    .name("Mismatch Exam")
                    .durationMinutes(60)
                    .totalMarks(100)
                    .negativeMarkingEnabled(false)
                    .negativeMarkingValue(0.0)
                    .navigationPolicy(NavigationPolicy.SEQUENTIAL)
                    .calculatorPolicy(CalculatorPolicy.NONE)
                    .reviewFlagEnabled(false)
                    .sections(sections)
                    .build();

            // When / Then
            assertThatThrownBy(() -> examinationService.create(request, TENANT_ID))
                    .isInstanceOf(SectionMarksValidationException.class)
                    .hasMessageContaining("expected totalMarks=100")
                    .hasMessageContaining("50.00");
        }

        @Test
        @DisplayName("should pass when section marks exactly equal totalMarks")
        void shouldPassWhenMarksExactlyEqual() {
            // Given: single section 25 questions × 4 marks = 100 total
            List<Section> sections = List.of(
                    Section.builder()
                            .name("General Knowledge")
                            .subject("GK")
                            .questionCount(25)
                            .marksPerQuestion(4.0)
                            .build()
            );

            CreateExaminationRequest request = CreateExaminationRequest.builder()
                    .name("Exact Match Exam")
                    .durationMinutes(90)
                    .totalMarks(100)
                    .negativeMarkingEnabled(false)
                    .negativeMarkingValue(0.0)
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .calculatorPolicy(CalculatorPolicy.BASIC)
                    .reviewFlagEnabled(true)
                    .sections(sections)
                    .build();

            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> {
                Examination exam = invocation.getArgument(0);
                try {
                    var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(exam, UUID.randomUUID());
                    var createdAtField = exam.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(exam, Instant.now());
                } catch (Exception e) {
                    // reflection-based setup
                }
                return exam;
            });

            // When
            ExaminationResponse response = examinationService.create(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalMarks()).isEqualTo(100);
            assertThat(response.getSections()).hasSize(1);
        }

        @Test
        @DisplayName("should handle floating point marks correctly (e.g., 3 sections × 2.5 marks × 4 questions = 30)")
        void shouldHandleFloatingPointMarks() {
            // Given: 3 sections each with 4 questions × 2.5 marks = 10 marks per section → total 30
            List<Section> sections = List.of(
                    Section.builder()
                            .name("Section A")
                            .subject("Maths")
                            .questionCount(4)
                            .marksPerQuestion(2.5)
                            .build(),
                    Section.builder()
                            .name("Section B")
                            .subject("Physics")
                            .questionCount(4)
                            .marksPerQuestion(2.5)
                            .build(),
                    Section.builder()
                            .name("Section C")
                            .subject("Chemistry")
                            .questionCount(4)
                            .marksPerQuestion(2.5)
                            .build()
            );

            CreateExaminationRequest request = CreateExaminationRequest.builder()
                    .name("Floating Point Exam")
                    .durationMinutes(45)
                    .totalMarks(30)
                    .negativeMarkingEnabled(false)
                    .negativeMarkingValue(0.0)
                    .navigationPolicy(NavigationPolicy.RESTRICTED)
                    .calculatorPolicy(CalculatorPolicy.NONE)
                    .reviewFlagEnabled(false)
                    .sections(sections)
                    .build();

            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> {
                Examination exam = invocation.getArgument(0);
                try {
                    var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(exam, UUID.randomUUID());
                    var createdAtField = exam.getClass().getSuperclass().getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(exam, Instant.now());
                } catch (Exception e) {
                    // reflection-based setup
                }
                return exam;
            });

            // When
            ExaminationResponse response = examinationService.create(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalMarks()).isEqualTo(30);
            assertThat(response.getSections()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should re-validate and update examination fields")
        void shouldUpdateExamination() {
            // Given: existing exam
            UUID examId = UUID.randomUUID();
            Examination existing = Examination.builder()
                    .name("Old Exam")
                    .durationMinutes(120)
                    .totalMarks(200)
                    .negativeMarkingEnabled(false)
                    .negativeMarkingValue(0.0)
                    .navigationPolicy("SEQUENTIAL")
                    .calculatorPolicy("NONE")
                    .reviewFlagEnabled(false)
                    .sectionsJson("[]")
                    .status("DRAFT")
                    .build();
            // Set base entity fields via reflection
            try {
                var idField = existing.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(existing, examId);
                var tenantField = existing.getClass().getSuperclass().getDeclaredField("tenantId");
                tenantField.setAccessible(true);
                tenantField.set(existing, TENANT_ID);
                var createdAtField = existing.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(existing, Instant.now());
            } catch (Exception e) {
                // reflection-based test setup
            }

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(existing));
            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // New request: 2 sections, 20 questions × 5 marks = 100 each → total 200
            List<Section> newSections = List.of(
                    Section.builder()
                            .name("Section 1")
                            .subject("Maths")
                            .questionCount(20)
                            .marksPerQuestion(5.0)
                            .build(),
                    Section.builder()
                            .name("Section 2")
                            .subject("Science")
                            .questionCount(20)
                            .marksPerQuestion(5.0)
                            .build()
            );

            CreateExaminationRequest updateRequest = CreateExaminationRequest.builder()
                    .name("Updated Exam")
                    .durationMinutes(150)
                    .totalMarks(200)
                    .negativeMarkingEnabled(true)
                    .negativeMarkingValue(0.5)
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .calculatorPolicy(CalculatorPolicy.SCIENTIFIC)
                    .reviewFlagEnabled(true)
                    .sections(newSections)
                    .build();

            // When
            ExaminationResponse response = examinationService.update(examId, updateRequest, TENANT_ID);

            // Then
            assertThat(response.getName()).isEqualTo("Updated Exam");
            assertThat(response.getDurationMinutes()).isEqualTo(150);
            assertThat(response.isNegativeMarkingEnabled()).isTrue();
            assertThat(response.getNegativeMarkingValue()).isEqualTo(0.5);
            assertThat(response.getNavigationPolicy()).isEqualTo("FLEXIBLE");
            assertThat(response.getCalculatorPolicy()).isEqualTo("SCIENTIFIC");
            assertThat(response.isReviewFlagEnabled()).isTrue();

            verify(examinationRepository).save(any(Examination.class));
        }

        @Test
        @DisplayName("should throw ExaminationNotFoundException when exam does not exist")
        void shouldThrowWhenExamNotFound() {
            UUID examId = UUID.randomUUID();
            when(examinationRepository.findById(examId)).thenReturn(Optional.empty());

            CreateExaminationRequest request = CreateExaminationRequest.builder()
                    .name("Non-existent")
                    .durationMinutes(60)
                    .totalMarks(50)
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .calculatorPolicy(CalculatorPolicy.NONE)
                    .sections(List.of(Section.builder()
                            .name("S1")
                            .subject("S")
                            .questionCount(10)
                            .marksPerQuestion(5.0)
                            .build()))
                    .build();

            assertThatThrownBy(() -> examinationService.update(examId, request, TENANT_ID))
                    .isInstanceOf(ExaminationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return examination with deserialized sections")
        void shouldReturnExamWithSections() throws Exception {
            UUID examId = UUID.randomUUID();
            List<Section> sections = List.of(
                    Section.builder()
                            .name("Physics")
                            .subject("Physics")
                            .questionCount(30)
                            .marksPerQuestion(4.0)
                            .build()
            );
            String sectionsJson = objectMapper.writeValueAsString(sections);

            Examination examination = Examination.builder()
                    .name("Retrieve Exam")
                    .durationMinutes(180)
                    .totalMarks(120)
                    .negativeMarkingEnabled(true)
                    .negativeMarkingValue(1.0)
                    .navigationPolicy("FLEXIBLE")
                    .calculatorPolicy("SCIENTIFIC")
                    .reviewFlagEnabled(true)
                    .sectionsJson(sectionsJson)
                    .status("DRAFT")
                    .build();

            try {
                var idField = examination.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(examination, examId);
                var createdAtField = examination.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(examination, Instant.now());
                var tenantField = examination.getClass().getSuperclass().getDeclaredField("tenantId");
                tenantField.setAccessible(true);
                tenantField.set(examination, TENANT_ID);
            } catch (Exception e) {
                // reflection-based test setup
            }

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(examination));

            // When
            ExaminationResponse response = examinationService.getById(examId);

            // Then
            assertThat(response.getId()).isEqualTo(examId);
            assertThat(response.getName()).isEqualTo("Retrieve Exam");
            assertThat(response.getSections()).hasSize(1);
            assertThat(response.getSections().get(0).getName()).isEqualTo("Physics");
            assertThat(response.getSections().get(0).getQuestionCount()).isEqualTo(30);
            assertThat(response.getSections().get(0).getMarksPerQuestion()).isEqualTo(4.0);
        }
    }
}
