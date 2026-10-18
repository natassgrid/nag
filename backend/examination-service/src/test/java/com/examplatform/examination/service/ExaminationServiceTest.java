/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.examination.client.PaperBlueprintClient;
import com.examplatform.examination.domain.ExamSyllabusNode;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.ExaminationSection;
import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExamSyllabusNodeDto;
import com.examplatform.examination.dto.ExamSyllabusResponse;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.repository.ExamSyllabusNodeRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.examination.repository.ExaminationSectionRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExaminationService")
class ExaminationServiceTest {

    @Mock
    private ExaminationRepository examinationRepository;

    @Mock
    private ExaminationSectionRepository examinationSectionRepository;

    @Mock
    private ExamSyllabusNodeRepository examSyllabusNodeRepository;

    @Mock
    private TaxonomyClient taxonomyClient;

    @Mock
    private PaperBlueprintClient paperBlueprintClient;

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ExaminationService examinationService;

    private static final String TENANT_ID = "tenant-authority-1";
    private static final UUID BLUEPRINT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist examination in DRAFT status with valid sections")
        void shouldCreateExamInDraftStatus() {
            List<Section> sections = List.of(
                    Section.builder()
                            .subjectId(1L)
                            .name("Physics")
                            .subject("Physics")
                            .questionCount(10)
                            .marksPerQuestion(5.0)
                            .negativeMarksPerQuestion(1.0)
                            .build(),
                    Section.builder()
                            .subjectId(2L)
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
                    .blueprintId(BLUEPRINT_ID)
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
                } catch (Exception ignored) {}
                return exam;
            });
            when(examinationSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ExaminationResponse response = examinationService.create(request, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(QuestionStateConstants.DRAFT);
            assertThat(response.getName()).isEqualTo("JEE Main 2025");
            assertThat(response.getDurationMinutes()).isEqualTo(180);
            assertThat(response.getTotalMarks()).isEqualTo(100);
            assertThat(response.isNegativeMarkingEnabled()).isTrue();
            assertThat(response.getNegativeMarkingValue()).isEqualTo(1.0);
            assertThat(response.getNavigationPolicy()).isEqualTo("FLEXIBLE");
            assertThat(response.getCalculatorPolicy()).isEqualTo("SCIENTIFIC");
            assertThat(response.isReviewFlagEnabled()).isTrue();
            assertThat(response.getSections()).hasSize(2);

            ArgumentCaptor<Examination> captor = ArgumentCaptor.forClass(Examination.class);
            verify(examinationRepository).save(captor.capture());
            Examination saved = captor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getStatus()).isEqualTo(QuestionStateConstants.DRAFT);
            assertThat(saved.getBlueprintId()).isEqualTo(BLUEPRINT_ID);
        }

        @Test
        @DisplayName("should throw SectionMarksValidationException when section marks do not match totalMarks")
        void shouldThrowWhenSectionMarksMismatch() {
            List<Section> sections = List.of(
                    Section.builder()
                            .subjectId(1L)
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
                    .blueprintId(BLUEPRINT_ID)
                    .sections(sections)
                    .build();

            assertThatThrownBy(() -> examinationService.create(request, TENANT_ID))
                    .isInstanceOf(SectionMarksValidationException.class)
                    .hasMessageContaining("expected totalMarks=100")
                    .hasMessageContaining("50.00");
        }

        @Test
        @DisplayName("should pass when section marks exactly equal totalMarks")
        void shouldPassWhenMarksExactlyEqual() {
            List<Section> sections = List.of(
                    Section.builder()
                            .subjectId(1L)
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
                    .blueprintId(BLUEPRINT_ID)
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
                } catch (Exception ignored) {}
                return exam;
            });
            when(examinationSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ExaminationResponse response = examinationService.create(request, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getTotalMarks()).isEqualTo(100);
            assertThat(response.getSections()).hasSize(1);
        }

        @Test
        @DisplayName("should handle floating point marks correctly")
        void shouldHandleFloatingPointMarks() {
            List<Section> sections = List.of(
                    Section.builder()
                            .subjectId(1L)
                            .name("Section A")
                            .subject("Maths")
                            .questionCount(4)
                            .marksPerQuestion(2.5)
                            .build(),
                    Section.builder()
                            .subjectId(2L)
                            .name("Section B")
                            .subject("Physics")
                            .questionCount(4)
                            .marksPerQuestion(2.5)
                            .build(),
                    Section.builder()
                            .subjectId(3L)
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
                    .blueprintId(BLUEPRINT_ID)
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
                } catch (Exception ignored) {}
                return exam;
            });
            when(examinationSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ExaminationResponse response = examinationService.create(request, TENANT_ID);

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
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();

            try {
                var idField = existing.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(existing, examId);
                var tenantField = existing.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(existing, TENANT_ID);
                var createdAtField = existing.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(existing, Instant.now());
            } catch (Exception ignored) {}

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(existing));
            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(examinationSectionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            List<Section> newSections = List.of(
                    Section.builder()
                            .subjectId(1L)
                            .name("Section 1")
                            .subject("Maths")
                            .questionCount(20)
                            .marksPerQuestion(5.0)
                            .build(),
                    Section.builder()
                            .subjectId(2L)
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
                    .blueprintId(BLUEPRINT_ID)
                    .sections(newSections)
                    .build();

            ExaminationResponse response = examinationService.update(examId, updateRequest, TENANT_ID);

            assertThat(response.getName()).isEqualTo("Updated Exam");
            assertThat(response.getDurationMinutes()).isEqualTo(150);
            assertThat(response.isNegativeMarkingEnabled()).isTrue();
            assertThat(response.getNegativeMarkingValue()).isEqualTo(0.5);
            assertThat(response.getNavigationPolicy()).isEqualTo("FLEXIBLE");
            assertThat(response.getCalculatorPolicy()).isEqualTo("SCIENTIFIC");
            assertThat(response.isReviewFlagEnabled()).isTrue();

            verify(examinationRepository).save(any(Examination.class));
            verify(examinationSectionRepository).deleteByExaminationId(examId);
            verify(examinationSectionRepository).saveAll(any());
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
                    .blueprintId(BLUEPRINT_ID)
                    .sections(List.of(Section.builder()
                            .subjectId(1L)
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
        @DisplayName("should return examination with relational sections")
        void shouldReturnExamWithSections() {
            UUID examId = UUID.randomUUID();

            Examination examination = Examination.builder()
                    .name("Retrieve Exam")
                    .durationMinutes(180)
                    .totalMarks(120)
                    .negativeMarkingEnabled(true)
                    .negativeMarkingValue(1.0)
                    .navigationPolicy("FLEXIBLE")
                    .calculatorPolicy("SCIENTIFIC")
                    .reviewFlagEnabled(true)
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();

            try {
                var idField = examination.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(examination, examId);
                var createdAtField = examination.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(examination, Instant.now());
                var tenantField = examination.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(examination, TENANT_ID);
            } catch (Exception ignored) {}

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(examination));

            ExaminationSection sec = ExaminationSection.builder()
                    .examination(examination)
                    .subjectId(1L)
                    .name("Physics")
                    .orderIndex(0)
                    .questionCount(30)
                    .marksPerQuestion(4.0)
                    .build();
            when(examinationSectionRepository.findByExaminationIdOrderByOrderIndexAsc(examId))
                    .thenReturn(List.of(sec));

            ExaminationResponse response = examinationService.getById(examId);

            assertThat(response.getId()).isEqualTo(examId);
            assertThat(response.getName()).isEqualTo("Retrieve Exam");
            assertThat(response.getSections()).hasSize(1);
            assertThat(response.getSections().get(0).getName()).isEqualTo("Physics");
            assertThat(response.getSections().get(0).getQuestionCount()).isEqualTo(30);
            assertThat(response.getSections().get(0).getMarksPerQuestion()).isEqualTo(4.0);
        }
    }

    @Nested
    @DisplayName("publish and syllabus validation")
    class PublishAndSyllabus {

        @Test
        @DisplayName("publish fails if syllabus nodes are empty")
        void publish_failsIfSyllabusEmpty() {
            UUID examId = UUID.randomUUID();
            Examination exam = Examination.builder()
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();
            try {
                var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(exam, examId);
                var tenantField = exam.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(exam, TENANT_ID);
            } catch (Exception ignored) {}

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(examSyllabusNodeRepository.findByExaminationId(examId)).thenReturn(List.of());

            assertThatThrownBy(() -> examinationService.publish(examId, TENANT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("syllabus nodes must be explicitly defined");
        }

        @Test
        @DisplayName("publish fails if syllabus node is invalid in TaxonomyClient")
        void publish_failsIfTaxonomyNodeInvalid() {
            UUID examId = UUID.randomUUID();
            Examination exam = Examination.builder()
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();
            try {
                var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(exam, examId);
                var tenantField = exam.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(exam, TENANT_ID);
            } catch (Exception ignored) {}

            ExamSyllabusNode node = ExamSyllabusNode.builder()
                    .nodeType(TaxonomyNodeType.SUBJECT)
                    .nodeId(999L)
                    .build();

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(examSyllabusNodeRepository.findByExaminationId(examId)).thenReturn(List.of(node));
            when(taxonomyClient.isValidNode(TaxonomyNodeType.SUBJECT, 999L, TENANT_ID)).thenReturn(false);

            assertThatThrownBy(() -> examinationService.publish(examId, TENANT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid or inactive syllabus node");
        }

        @Test
        @DisplayName("publish succeeds when syllabus, rule coverage, and sufficiency pass")
        void publish_succeedsWhenAllInvariantsPass() {
            UUID examId = UUID.randomUUID();
            Examination exam = Examination.builder()
                    .name("Valid Exam")
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();
            try {
                var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(exam, examId);
                var tenantField = exam.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(exam, TENANT_ID);
                var createdAtField = exam.getClass().getSuperclass().getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(exam, Instant.now());
            } catch (Exception ignored) {}

            ExamSyllabusNode node = ExamSyllabusNode.builder()
                    .nodeType(TaxonomyNodeType.SUBJECT)
                    .nodeId(1L)
                    .build();

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(examSyllabusNodeRepository.findByExaminationId(examId)).thenReturn(List.of(node));
            when(taxonomyClient.isValidNode(TaxonomyNodeType.SUBJECT, 1L, TENANT_ID)).thenReturn(true);

            PaperBlueprintClient.BlueprintRuleInfo rule = new PaperBlueprintClient.BlueprintRuleInfo(
                    UUID.randomUUID(), 1L, null, null, "EASY", null, 10, null);
            PaperBlueprintClient.BlueprintSectionInfo sec = new PaperBlueprintClient.BlueprintSectionInfo(
                    UUID.randomUUID(), "Section 1", 0, 1L, List.of(rule));
            PaperBlueprintClient.BlueprintInfo bp = new PaperBlueprintClient.BlueprintInfo(
                    BLUEPRINT_ID, "BP-1", "Blueprint 1", 1, "ACTIVE", List.of(sec));

            when(paperBlueprintClient.getBlueprint(BLUEPRINT_ID, TENANT_ID)).thenReturn(Optional.of(bp));
            when(paperBlueprintClient.checkSufficiency(BLUEPRINT_ID, examId, 1, TENANT_ID))
                    .thenReturn(new PaperBlueprintClient.SufficiencyResult(true, 10, 50, 0, "OK"));
            when(examinationRepository.save(any(Examination.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExaminationResponse response = examinationService.publish(examId, TENANT_ID);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(QuestionStateConstants.PUBLISHED);
        }

        @Test
        @DisplayName("updateSyllabus replaces nodes and returns response")
        void updateSyllabus_replacesNodes() {
            UUID examId = UUID.randomUUID();
            Examination exam = Examination.builder()
                    .blueprintId(BLUEPRINT_ID)
                    .status(QuestionStateConstants.DRAFT)
                    .build();
            try {
                var idField = exam.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(exam, examId);
                var tenantField = exam.getClass().getSuperclass().getDeclaredField(SharedConstants.TENANT_ID);
                tenantField.setAccessible(true);
                tenantField.set(exam, TENANT_ID);
            } catch (Exception ignored) {}

            when(examinationRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(examSyllabusNodeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            List<ExamSyllabusNodeDto> nodes = List.of(
                    ExamSyllabusNodeDto.builder().nodeType(TaxonomyNodeType.SUBJECT).nodeId(1L).weightagePct(100.0).build()
            );

            ExamSyllabusResponse resp = examinationService.updateSyllabus(examId, nodes, TENANT_ID);

            assertThat(resp).isNotNull();
            assertThat(resp.getExaminationId()).isEqualTo(examId);
            verify(examSyllabusNodeRepository).deleteByExaminationId(examId);
            verify(examSyllabusNodeRepository).saveAll(any());
        }
    }
}
