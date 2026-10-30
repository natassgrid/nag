/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.service;

import com.examplatform.candidate.client.ExaminationClient;
import com.examplatform.candidate.domain.DiagnosticSession;
import com.examplatform.candidate.domain.LearnerEnrollment;
import com.examplatform.candidate.domain.LearnerProfile;
import com.examplatform.candidate.dto.*;
import com.examplatform.candidate.exception.LearnerDomainException;
import com.examplatform.candidate.repository.DiagnosticSessionRepository;
import com.examplatform.candidate.repository.LearnerEnrollmentRepository;
import com.examplatform.candidate.repository.LearnerProfileRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearnerService Unit Tests")
class LearnerServiceTest {

    @Mock
    private LearnerProfileRepository profileRepository;

    @Mock
    private LearnerEnrollmentRepository enrollmentRepository;

    @Mock
    private DiagnosticSessionRepository diagnosticRepository;

    @Mock
    private ExaminationClient examinationClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private LearnerService learnerService;

    private final UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID examId = UUID.fromString("11111111-2222-3333-4444-555555555551");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        learnerService = new LearnerService(
                profileRepository,
                enrollmentRepository,
                diagnosticRepository,
                examinationClient,
                eventPublisher,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Learner Profile Tests")
    class ProfileTests {

        @Test
        @DisplayName("Returns uncompleted empty profile if not found")
        void shouldReturnEmptyProfileWhenNotFound() {
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());

            LearnerProfileDto profile = learnerService.getProfile(candidateId, "default");

            assertThat(profile).isNotNull();
            assertThat(profile.candidateId()).isEqualTo(candidateId);
            assertThat(profile.isComplete()).isFalse();
        }

        @Test
        @DisplayName("Updates profile and marks completed when required fields provided")
        void shouldUpdateProfileAndMarkComplete() {
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());
            when(profileRepository.save(any(LearnerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateLearnerProfileRequest req = new UpdateLearnerProfileRequest(
                    "Alice Walker",
                    "GRADUATION",
                    "en",
                    60,
                    List.of("CONCEPT", "WORKED_EXAMPLES"),
                    "Moderate",
                    LocalDate.now().plusMonths(6)
            );

            LearnerProfileDto result = learnerService.updateProfile(candidateId, req, "default");

            assertThat(result.displayName()).isEqualTo("Alice Walker");
            assertThat(result.educationLevel()).isEqualTo("GRADUATION");
            assertThat(result.preferredLanguage()).isEqualTo("en");
            assertThat(result.dailyStudyMinutes()).isEqualTo(60);
            assertThat(result.isComplete()).isTrue();
            assertThat(result.completedAt()).isNotNull();
        }

        @Test
        @DisplayName("Updates profile without complete flag when required fields missing")
        void shouldUpdateProfileWithoutCompleteFlagWhenIncomplete() {
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());
            when(profileRepository.save(any(LearnerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateLearnerProfileRequest req = new UpdateLearnerProfileRequest(
                    "Bob",
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null
            );

            LearnerProfileDto result = learnerService.updateProfile(candidateId, req, "default");

            assertThat(result.displayName()).isEqualTo("Bob");
            assertThat(result.isComplete()).isFalse();
            assertThat(result.completedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Enrollment Lifecycle Tests")
    class EnrollmentTests {

        @Test
        @DisplayName("Fails enrollment when learner profile does not exist")
        void shouldFailEnrollmentWhenProfileMissing() {
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());

            CreateEnrollmentRequest req = new CreateEnrollmentRequest(examId, null);

            assertThatThrownBy(() -> learnerService.createEnrollment(candidateId, req, "default"))
                    .isInstanceOf(LearnerDomainException.class)
                    .hasMessageContaining("profile does not exist");
        }

        @Test
        @DisplayName("Fails enrollment when learner profile is incomplete")
        void shouldFailEnrollmentWhenProfileIncomplete() {
            LearnerProfile incomplete = LearnerProfile.builder()
                    .candidateId(candidateId)
                    .displayName("Charlie")
                    .completedAt(null)
                    .build();
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(incomplete));

            CreateEnrollmentRequest req = new CreateEnrollmentRequest(examId, null);

            assertThatThrownBy(() -> learnerService.createEnrollment(candidateId, req, "default"))
                    .isInstanceOf(LearnerDomainException.class)
                    .hasMessageContaining("must be complete");
        }

        @Test
        @DisplayName("Fails enrollment when target exam is not published")
        void shouldFailEnrollmentWhenExamNotPublished() {
            LearnerProfile complete = LearnerProfile.builder()
                    .candidateId(candidateId)
                    .educationLevel("GRADUATION")
                    .preferredLanguage("en")
                    .dailyStudyMinutes(45)
                    .completedAt(Instant.now())
                    .build();
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(complete));

            PublishedExamDto draftExam = new PublishedExamDto(
                    examId, "DRAFT_EXAM", "Draft Exam", "Desc", "DRAFT", 120, 100, UUID.randomUUID(), 1, 10
            );
            when(examinationClient.getExam(examId, "default")).thenReturn(Optional.of(draftExam));

            CreateEnrollmentRequest req = new CreateEnrollmentRequest(examId, null);

            assertThatThrownBy(() -> learnerService.createEnrollment(candidateId, req, "default"))
                    .isInstanceOf(LearnerDomainException.class)
                    .hasMessageContaining("must be published");
        }

        @Test
        @DisplayName("Pauses old active enrollment and activates new enrollment")
        void shouldPauseOldActiveAndActivateNewEnrollment() {
            LearnerProfile complete = LearnerProfile.builder()
                    .candidateId(candidateId)
                    .educationLevel("GRADUATION")
                    .preferredLanguage("en")
                    .dailyStudyMinutes(60)
                    .completedAt(Instant.now())
                    .build();
            when(profileRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(complete));

            PublishedExamDto publishedExam = new PublishedExamDto(
                    examId, SharedConstants.JEE_MAIN, "JEE Main", "Desc", "PUBLISHED", 180, 300, UUID.randomUUID(), 3, 30
            );
            when(examinationClient.getExam(examId, "default")).thenReturn(Optional.of(publishedExam));

            LearnerEnrollment oldActive = LearnerEnrollment.builder()
                    .candidateId(candidateId)
                    .examinationId(UUID.randomUUID())
                    .status("ACTIVE")
                    .build();
            when(enrollmentRepository.findByCandidateIdAndStatusIn(candidateId, List.of("ACTIVE")))
                    .thenReturn(List.of(oldActive));

            when(enrollmentRepository.save(any(LearnerEnrollment.class))).thenAnswer(i -> {
                LearnerEnrollment e = i.getArgument(0);
                if (e.getId() == null) {
                    e.setId(UUID.randomUUID());
                }
                return e;
            });

            CreateEnrollmentRequest req = new CreateEnrollmentRequest(examId, LocalDate.now().plusMonths(3));
            LearnerEnrollmentResponse res = learnerService.createEnrollment(candidateId, req, "default");

            assertThat(oldActive.getStatus()).isEqualTo("PAUSED");
            assertThat(res.status()).isEqualTo("ACTIVE");
            assertThat(res.examinationId()).isEqualTo(examId);
            assertThat(res.examName()).isEqualTo("JEE Main");
        }
    }

    @Nested
    @DisplayName("Diagnostic Session Tests")
    class DiagnosticTests {

        private final UUID enrollmentId = UUID.randomUUID();

        @Test
        @DisplayName("Starts diagnostic assessment with sampled questions")
        void shouldStartDiagnosticAssessment() {
            LearnerEnrollment enrollment = LearnerEnrollment.builder()
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .status("ACTIVE")
                    .build();
            enrollment.setId(enrollmentId);
            when(enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId))
                    .thenReturn(Optional.of(enrollment));
            when(diagnosticRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.empty());

            when(diagnosticRepository.save(any(DiagnosticSession.class))).thenAnswer(i -> {
                DiagnosticSession s = i.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });

            DiagnosticSessionResponse res = learnerService.startDiagnostic(candidateId, enrollmentId, "default");

            assertThat(res).isNotNull();
            assertThat(res.status()).isEqualTo(SharedConstants.IN_PROGRESS);
            assertThat(res.questions()).isNotEmpty();
            assertThat(res.totalQuestions()).isGreaterThan(0);
            assertThat(enrollment.getDiagnosticStatus()).isEqualTo(SharedConstants.IN_PROGRESS);
        }

        @Test
        @DisplayName("Submitting diagnostic with skip sets SKIPPED status")
        void shouldSkipDiagnosticAssessment() {
            LearnerEnrollment enrollment = LearnerEnrollment.builder()
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .status("ACTIVE")
                    .build();
            enrollment.setId(enrollmentId);

            DiagnosticSession session = DiagnosticSession.builder()
                    .enrollmentId(enrollmentId)
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .status(SharedConstants.IN_PROGRESS)
                    .build();
            session.setId(UUID.randomUUID());

            when(enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId))
                    .thenReturn(Optional.of(enrollment));
            when(diagnosticRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.of(session));
            when(diagnosticRepository.save(any(DiagnosticSession.class))).thenAnswer(i -> i.getArgument(0));

            SubmitDiagnosticRequest req = new SubmitDiagnosticRequest(Collections.emptyList(), true);
            DiagnosticResultResponse res = learnerService.submitDiagnostic(candidateId, enrollmentId, req, "default");

            assertThat(res.status()).isEqualTo("SKIPPED");
            assertThat(res.initialReadinessScore()).isEqualTo(50);
            assertThat(res.neutralPriorPercent()).isEqualTo(50);
            assertThat(res.baselineLabel()).isEqualTo("Neutral Baseline (50% Prior)");
            assertThat(session.getStatus()).isEqualTo("SKIPPED");
            assertThat(enrollment.getDiagnosticStatus()).isEqualTo("SKIPPED");
        }

        @Test
        @DisplayName("Submitting diagnostic with answers evaluates score, readiness baseline, strengths and weaknesses")
        void shouldSubmitAndGradeDiagnosticAssessment() {
            LearnerEnrollment enrollment = LearnerEnrollment.builder()
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .status("ACTIVE")
                    .build();
            enrollment.setId(enrollmentId);

            DiagnosticSession session = DiagnosticSession.builder()
                    .enrollmentId(enrollmentId)
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .totalQuestions(2)
                    .status(SharedConstants.IN_PROGRESS)
                    .build();
            session.setId(UUID.randomUUID());

            when(enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId))
                    .thenReturn(Optional.of(enrollment));
            when(diagnosticRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.of(session));
            when(diagnosticRepository.save(any(DiagnosticSession.class))).thenAnswer(i -> i.getArgument(0));

            SubmitDiagnosticRequest req = new SubmitDiagnosticRequest(
                    List.of(
                            new SubmitDiagnosticRequest.DiagnosticAnswerDto(
                                    UUID.randomUUID(), 101L, List.of("opt-1"), 45000
                            ),
                            new SubmitDiagnosticRequest.DiagnosticAnswerDto(
                                    UUID.randomUUID(), 102L, List.of("opt-3"), 30000
                            )
                    ),
                    false
            );

            DiagnosticResultResponse res = learnerService.submitDiagnostic(candidateId, enrollmentId, req, "default");

            assertThat(res.status()).isEqualTo(SharedConstants.COMPLETED);
            assertThat(res.correctCount()).isEqualTo(1);
            assertThat(res.correctAnswers()).isEqualTo(1);
            assertThat(res.score()).isEqualTo(2.0);
            assertThat(res.initialReadinessScore()).isEqualTo(50);
            assertThat(res.seededSubtopicsCount()).isEqualTo(2);
            assertThat(res.strengths()).containsExactly("Syllogisms");
            assertThat(res.weaknesses()).containsExactly("Logical Deductions");
            assertThat(res.baselineLabel()).isEqualTo("Calibrated Diagnostic Baseline");
            assertThat(enrollment.getDiagnosticStatus()).isEqualTo(SharedConstants.COMPLETED);
        }

        @Test
        @DisplayName("Submitting diagnostic with frontend answers payload maps questionId to session subtopics dynamically")
        void shouldSupportFrontendAnswersPayloadFormat() throws Exception {
            LearnerEnrollment enrollment = LearnerEnrollment.builder()
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .status("ACTIVE")
                    .build();
            enrollment.setId(enrollmentId);

            UUID q1Id = UUID.fromString("d1010000-0000-0000-0000-000000000001");
            UUID q2Id = UUID.fromString("d1010000-0000-0000-0000-000000000002");
            List<DiagnosticQuestionDto> sessionQuestions = List.of(
                    new DiagnosticQuestionDto(q1Id, 1, 1L, "Reasoning", 10L, "Logic", 101L, "Syllogisms", "Q1", List.of(), "MEDIUM"),
                    new DiagnosticQuestionDto(q2Id, 2, 2L, "Quant", 20L, "Arithmetic", 201L, "Percentages", "Q2", List.of(), "EASY")
            );

            DiagnosticSession session = DiagnosticSession.builder()
                    .enrollmentId(enrollmentId)
                    .candidateId(candidateId)
                    .examinationId(examId)
                    .questionsJson(objectMapper.writeValueAsString(sessionQuestions))
                    .totalQuestions(2)
                    .status(SharedConstants.IN_PROGRESS)
                    .build();
            session.setId(UUID.randomUUID());

            when(enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId))
                    .thenReturn(Optional.of(enrollment));
            when(diagnosticRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.of(session));
            when(diagnosticRepository.save(any(DiagnosticSession.class))).thenAnswer(i -> i.getArgument(0));

            SubmitDiagnosticRequest req = new SubmitDiagnosticRequest(
                    null,
                    List.of(
                            new SubmitDiagnosticRequest.DiagnosticAnswerDto(q1Id, null, null, "opt-1", 0, 22),
                            new SubmitDiagnosticRequest.DiagnosticAnswerDto(q2Id, null, null, "opt-1", 0, 18)
                    ),
                    false
            );

            DiagnosticResultResponse res = learnerService.submitDiagnostic(candidateId, enrollmentId, req, "default");

            assertThat(res.status()).isEqualTo(SharedConstants.COMPLETED);
            assertThat(res.correctAnswers()).isEqualTo(2);
            assertThat(res.initialReadinessScore()).isEqualTo(100);
            assertThat(res.strengths()).containsExactly("Syllogisms", "Percentages");
            assertThat(res.weaknesses()).isEmpty();
        }
    }
}
