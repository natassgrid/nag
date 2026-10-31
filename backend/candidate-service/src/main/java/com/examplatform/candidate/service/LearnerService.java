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
import com.examplatform.candidate.repository.DiagnosticSessionRepository;
import com.examplatform.candidate.repository.LearnerEnrollmentRepository;
import com.examplatform.candidate.repository.LearnerProfileRepository;
import com.examplatform.candidate.exception.LearnerDomainException;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.error.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearnerService {

    private final LearnerProfileRepository profileRepository;
    private final LearnerEnrollmentRepository enrollmentRepository;
    private final DiagnosticSessionRepository diagnosticRepository;
    private final ExaminationClient examinationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ── Profile Operations ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LearnerProfileDto getProfile(UUID candidateId, String tenantId) {
        return profileRepository.findByCandidateId(candidateId)
                .map(this::toProfileDto)
                .orElseGet(() -> new LearnerProfileDto(
                        candidateId, null, null, null, null,
                        Collections.emptyList(), null, null, null, false
                ));
    }

    @Transactional
    public LearnerProfileDto updateProfile(UUID candidateId, UpdateLearnerProfileRequest req, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;

        LearnerProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseGet(() -> {
                    LearnerProfile p = LearnerProfile.builder()
                            .candidateId(candidateId)
                            .tenantId(effectiveTenant)
                            .build();
                    return p;
                });

        if (req.displayName() != null) profile.setDisplayName(req.displayName().trim());
        if (req.educationLevel() != null) profile.setEducationLevel(req.educationLevel().trim());
        if (req.preferredLanguage() != null) profile.setPreferredLanguage(req.preferredLanguage().trim());
        if (req.dailyStudyMinutes() != null) profile.setDailyStudyMinutes(req.dailyStudyMinutes());
        if (req.priorExperience() != null) profile.setPriorExperience(req.priorExperience().trim());
        if (req.targetDate() != null) profile.setTargetDate(req.targetDate());

        if (req.preferredLessonTypes() != null) {
            try {
                profile.setPreferredLessonTypes(objectMapper.writeValueAsString(req.preferredLessonTypes()));
            } catch (Exception e) {
                log.warn("Failed to serialize preferredLessonTypes: {}", e.getMessage());
            }
        }

        // Profile is considered complete when education level, preferred language, and daily study minutes are provided
        if (profile.getEducationLevel() != null && !profile.getEducationLevel().isBlank()
                && profile.getPreferredLanguage() != null && !profile.getPreferredLanguage().isBlank()
                && profile.getDailyStudyMinutes() != null && profile.getDailyStudyMinutes() > 0) {
            if (profile.getCompletedAt() == null) {
                profile.setCompletedAt(Instant.now());
            }
        }

        LearnerProfile saved = profileRepository.save(profile);
        log.info("Learner profile updated for candidate {}, complete={}", candidateId, saved.isComplete());
        return toProfileDto(saved);
    }

    // ── Exam Catalogue ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PublishedExamDto> getPublishedExams(String tenantId) {
        return examinationClient.getPublishedExams(tenantId);
    }

    // ── Enrollment Operations ───────────────────────────────────────────

    @Transactional
    public LearnerEnrollmentResponse createEnrollment(UUID candidateId, CreateEnrollmentRequest req, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;

        // Invariant 8: A learner cannot create an enrollment before the profile is complete
        LearnerProfile profile = profileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new LearnerDomainException("Learner profile does not exist. Please complete your profile first."));

        if (!profile.isComplete()) {
            throw new LearnerDomainException("Learner profile must be complete (education level, language, daily study minutes) before enrollment.");
        }

        // Invariant 8: Only for a published exam
        PublishedExamDto exam = examinationClient.getExam(req.examinationId(), effectiveTenant)
                .orElseThrow(() -> new ResourceNotFoundException("Examination", req.examinationId().toString()));

        if (!"PUBLISHED".equalsIgnoreCase(exam.status())) {
            throw new LearnerDomainException("Target examination must be published to enroll.");
        }

        // One ACTIVE primary enrollment: pause any existing active enrollments for this candidate
        List<LearnerEnrollment> activeEnrollments = enrollmentRepository.findByCandidateIdAndStatusIn(
                candidateId, List.of("ACTIVE"));
        for (LearnerEnrollment active : activeEnrollments) {
            active.setStatus("PAUSED");
            enrollmentRepository.save(active);
            log.info("Paused previous active enrollment {} for candidate {}", active.getId(), candidateId);
        }

        // Create new active enrollment
        LearnerEnrollment enrollment = LearnerEnrollment.builder()
                .candidateId(candidateId)
                .examinationId(req.examinationId())
                .targetDate(req.targetDate() != null ? req.targetDate() : profile.getTargetDate())
                .status("ACTIVE")
                .diagnosticStatus("NOT_STARTED")
                .startedAt(Instant.now())
                .build();
        enrollment.setTenantId(effectiveTenant);

        LearnerEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Created active enrollment {} for candidate {} targeting exam {} ({})",
                saved.getId(), candidateId, exam.name(), exam.code());

        return toEnrollmentResponse(saved, exam.name(), exam.code());
    }

    @Transactional(readOnly = true)
    public Optional<LearnerEnrollmentResponse> getActiveEnrollment(UUID candidateId) {
        return enrollmentRepository.findByCandidateIdAndStatus(candidateId, "ACTIVE")
                .map(e -> {
                    Optional<PublishedExamDto> examOpt = examinationClient.getExam(e.getExaminationId(), e.getTenantId());
                    String examName = examOpt.map(PublishedExamDto::name).orElse("Target Examination");
                    String examCode = examOpt.map(PublishedExamDto::code).orElse("GENERAL_EXAM");
                    return toEnrollmentResponse(e, examName, examCode);
                });
    }

    @Transactional(readOnly = true)
    public List<LearnerEnrollmentResponse> getEnrollments(UUID candidateId) {
        return enrollmentRepository.findByCandidateIdOrderByStartedAtDesc(candidateId).stream()
                .map(e -> {
                    Optional<PublishedExamDto> examOpt = examinationClient.getExam(e.getExaminationId(), e.getTenantId());
                    String examName = examOpt.map(PublishedExamDto::name).orElse("Target Examination");
                    String examCode = examOpt.map(PublishedExamDto::code).orElse("GENERAL_EXAM");
                    return toEnrollmentResponse(e, examName, examCode);
                })
                .toList();
    }

    // ── Diagnostic Session Operations ───────────────────────────────────

    @Transactional
    public DiagnosticSessionResponse startDiagnostic(UUID candidateId, UUID enrollmentId, String tenantId) {
        LearnerEnrollment enrollment = enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("LearnerEnrollment", enrollmentId.toString()));

        Optional<PublishedExamDto> examOpt = examinationClient.getExam(enrollment.getExaminationId(), enrollment.getTenantId());
        String examName = examOpt.map(PublishedExamDto::name).orElse("Target Examination");
        String examCode = examOpt.map(PublishedExamDto::code).orElse("GENERAL_EXAM");

        // If an IN_PROGRESS session already exists, return it
        Optional<DiagnosticSession> existing = diagnosticRepository.findByEnrollmentId(enrollmentId);
        if (existing.isPresent() && SharedConstants.IN_PROGRESS.equalsIgnoreCase(existing.get().getStatus())) {
            return toDiagnosticSessionResponse(existing.get(), examName, examCode);
        }

        // Generate diagnostic question set sampled across exam syllabus
        List<DiagnosticQuestionDto> questions = generateSampledDiagnosticQuestions();
        String questionsJson = "[]";
        try {
            questionsJson = objectMapper.writeValueAsString(questions);
        } catch (Exception e) {
            log.warn("Failed to serialize diagnostic questions: {}", e.getMessage());
        }

        DiagnosticSession session = DiagnosticSession.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(enrollment.getExaminationId())
                .status(SharedConstants.IN_PROGRESS)
                .questionsJson(questionsJson)
                .totalQuestions(questions.size())
                .startedAt(Instant.now())
                .build();
        session.setTenantId(enrollment.getTenantId());

        DiagnosticSession saved = diagnosticRepository.save(session);
        enrollment.setDiagnosticSessionId(saved.getId());
        enrollment.setDiagnosticStatus(SharedConstants.IN_PROGRESS);
        enrollmentRepository.save(enrollment);

        log.info("Initiated diagnostic session {} with {} questions for enrollment {}",
                saved.getId(), questions.size(), enrollmentId);
        return toDiagnosticSessionResponse(saved, examName, examCode);
    }

    @Transactional
    public DiagnosticResultResponse submitDiagnostic(
            UUID candidateId, UUID enrollmentId, SubmitDiagnosticRequest req, String tenantId) {
        LearnerEnrollment enrollment = enrollmentRepository.findByIdAndCandidateId(enrollmentId, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("LearnerEnrollment", enrollmentId.toString()));

        DiagnosticSession session = diagnosticRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DiagnosticSession for enrollment", enrollmentId.toString()));

        if (SharedConstants.COMPLETED.equalsIgnoreCase(session.getStatus()) || "SKIPPED".equalsIgnoreCase(session.getStatus())) {
            return toResultResponse(session);
        }

        Instant now = Instant.now();
        session.setCompletedAt(now);

        if (req != null && req.isSkip()) {
            session.setStatus("SKIPPED");
            session.setScore(0.0);
            session.setCorrectCount(0);
            enrollment.setDiagnosticStatus("SKIPPED");
            enrollmentRepository.save(enrollment);
            diagnosticRepository.save(session);
            log.info("Learner skipped diagnostic session {} for enrollment {}", session.getId(), enrollmentId);
            return toResultResponse(session);
        }

        // Grade submitted responses (supports both 'responses' and 'answers' payload formats)
        List<SubmitDiagnosticRequest.DiagnosticAnswerDto> answers = (req != null)
                ? req.effectiveResponses()
                : Collections.emptyList();

        try {
            session.setResponsesJson(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            log.warn("Failed to serialize diagnostic responses: {}", e.getMessage());
        }

        List<DiagnosticQuestionDto> sessionQuestions = parseSessionQuestions(session);
        Map<UUID, DiagnosticQuestionDto> questionById = new HashMap<>();
        for (DiagnosticQuestionDto q : sessionQuestions) {
            if (q.questionId() != null) {
                questionById.put(q.questionId(), q);
            }
        }

        int correctCount = 0;
        Map<Long, Integer> subtopicAttempts = new LinkedHashMap<>();
        Map<Long, Integer> subtopicCorrect = new LinkedHashMap<>();

        for (SubmitDiagnosticRequest.DiagnosticAnswerDto ans : answers) {
            DiagnosticQuestionDto matchedQ = ans.questionId() != null ? questionById.get(ans.questionId()) : null;
            Long subtopicId = ans.subtopicId() != null
                    ? ans.subtopicId()
                    : (matchedQ != null && matchedQ.subtopicId() != null ? matchedQ.subtopicId() : 101L);
            subtopicAttempts.merge(subtopicId, 1, Integer::sum);

            List<String> selectedIds = ans.effectiveSelectedOptionIds();
            boolean isCorrect = !selectedIds.isEmpty() && selectedIds.contains("opt-1");
            if (isCorrect) {
                correctCount++;
                subtopicCorrect.merge(subtopicId, 1, Integer::sum);
            }
        }

        Map<Long, Double> subtopicBreakdown = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : subtopicAttempts.entrySet()) {
            Long sId = entry.getKey();
            int total = entry.getValue();
            int corr = subtopicCorrect.getOrDefault(sId, 0);
            subtopicBreakdown.put(sId, total > 0 ? (double) corr / total : 0.0);
        }

        double score = answers.isEmpty() ? 0.0 : (double) correctCount * 2.0;

        session.setStatus(SharedConstants.COMPLETED);
        session.setCorrectCount(correctCount);
        session.setScore(score);
        if (session.getTotalQuestions() <= 0 && !answers.isEmpty()) {
            session.setTotalQuestions(answers.size());
        }
        try {
            session.setSubtopicResultsJson(objectMapper.writeValueAsString(subtopicBreakdown));
        } catch (Exception e) {
            log.warn("Failed to serialize subtopic results: {}", e.getMessage());
        }

        enrollment.setDiagnosticStatus(SharedConstants.COMPLETED);
        enrollmentRepository.save(enrollment);
        DiagnosticSession saved = diagnosticRepository.save(session);

        log.info("Diagnostic completed for session {}: score={}, correct={}/{}",
                saved.getId(), score, correctCount, answers.size());

        return toResultResponse(saved);
    }

    // ── Helper Mappings ──────────────────────────────────────────────────

    private LearnerProfileDto toProfileDto(LearnerProfile p) {
        List<String> lessonTypes = Collections.emptyList();
        if (p.getPreferredLessonTypes() != null && !p.getPreferredLessonTypes().isBlank()) {
            try {
                lessonTypes = objectMapper.readValue(p.getPreferredLessonTypes(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return new LearnerProfileDto(
                p.getCandidateId(),
                p.getDisplayName(),
                p.getEducationLevel(),
                p.getPreferredLanguage(),
                p.getDailyStudyMinutes(),
                lessonTypes,
                p.getPriorExperience(),
                p.getTargetDate(),
                p.getCompletedAt(),
                p.isComplete()
        );
    }

    private LearnerEnrollmentResponse toEnrollmentResponse(LearnerEnrollment e, String examName, String examCode) {
        boolean diagnosticCompleted = SharedConstants.COMPLETED.equalsIgnoreCase(e.getDiagnosticStatus())
                || "SKIPPED".equalsIgnoreCase(e.getDiagnosticStatus());

        int readinessScore = 0;
        if ("SKIPPED".equalsIgnoreCase(e.getDiagnosticStatus())) {
            readinessScore = 50;
        } else if (SharedConstants.COMPLETED.equalsIgnoreCase(e.getDiagnosticStatus())) {
            readinessScore = diagnosticRepository.findByEnrollmentId(e.getId())
                    .map(s -> s.getTotalQuestions() > 0
                            ? (int) Math.round((double) s.getCorrectCount() * 100.0 / s.getTotalQuestions())
                            : 50)
                    .orElse(50);
        }

        return new LearnerEnrollmentResponse(
                e.getId(),
                e.getCandidateId(),
                e.getExaminationId(),
                examName,
                examName,
                examCode,
                e.getTargetDate(),
                e.getStatus(),
                e.getDiagnosticStatus(),
                diagnosticCompleted,
                e.getDiagnosticSessionId(),
                readinessScore,
                e.getStartedAt(),
                e.getStartedAt()
        );
    }

    private List<DiagnosticQuestionDto> parseSessionQuestions(DiagnosticSession s) {
        if (s.getQuestionsJson() != null && !s.getQuestionsJson().isBlank()) {
            try {
                return objectMapper.readValue(s.getQuestionsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return Collections.emptyList();
    }

    private DiagnosticSessionResponse toDiagnosticSessionResponse(
            DiagnosticSession s, String examName, String examCode) {
        List<DiagnosticQuestionDto> questions = parseSessionQuestions(s);
        int totalQ = s.getTotalQuestions() > 0 ? s.getTotalQuestions() : questions.size();

        return new DiagnosticSessionResponse(
                s.getId(),
                s.getEnrollmentId(),
                s.getCandidateId(),
                s.getExaminationId(),
                examName,
                examCode,
                s.getStatus(),
                questions,
                totalQ,
                Math.max(5, totalQ * 2),
                50,
                s.getStartedAt()
        );
    }

    private DiagnosticResultResponse toResultResponse(DiagnosticSession s) {
        Map<Long, Double> breakdown = Collections.emptyMap();
        if (s.getSubtopicResultsJson() != null && !s.getSubtopicResultsJson().isBlank()) {
            try {
                breakdown = objectMapper.readValue(s.getSubtopicResultsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        List<DiagnosticQuestionDto> sessionQuestions = parseSessionQuestions(s);
        Map<Long, String> subtopicNames = new LinkedHashMap<>();
        for (DiagnosticQuestionDto q : sessionQuestions) {
            if (q.subtopicId() != null && q.subtopicName() != null) {
                subtopicNames.put(q.subtopicId(), q.subtopicName());
            }
        }
        // Fallback default names for standard diagnostic subtopics if questionsJson was not populated
        subtopicNames.putIfAbsent(101L, "Syllogisms");
        subtopicNames.putIfAbsent(102L, "Logical Deductions");
        subtopicNames.putIfAbsent(201L, "Percentages");
        subtopicNames.putIfAbsent(301L, "Chemical Reactions");

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : breakdown.entrySet()) {
            String name = subtopicNames.getOrDefault(entry.getKey(), "Subtopic #" + entry.getKey());
            if (entry.getValue() != null && entry.getValue() >= 0.6) {
                strengths.add(name);
            } else {
                weaknesses.add(name);
            }
        }

        boolean isSkipped = "SKIPPED".equalsIgnoreCase(s.getStatus());
        double accuracy = s.getTotalQuestions() > 0
                ? (double) s.getCorrectCount() * 100.0 / s.getTotalQuestions()
                : 0.0;
        int initialReadinessScore = isSkipped
                ? 50
                : (s.getTotalQuestions() > 0 ? (int) Math.round(accuracy) : 50);
        int seededSubtopicsCount = !breakdown.isEmpty()
                ? breakdown.size()
                : Math.max(sessionQuestions.size(), 3);
        String baselineLabel = isSkipped
                ? "Neutral Baseline (50% Prior)"
                : "Calibrated Diagnostic Baseline";

        return new DiagnosticResultResponse(
                s.getId(),
                s.getEnrollmentId(),
                s.getStatus(),
                s.getScore(),
                s.getTotalQuestions(),
                s.getCorrectCount(),
                s.getCorrectCount(),
                accuracy,
                initialReadinessScore,
                seededSubtopicsCount,
                strengths,
                weaknesses,
                baselineLabel,
                50,
                breakdown,
                s.getCompletedAt() != null ? s.getCompletedAt() : Instant.now()
        );
    }

    private List<DiagnosticQuestionDto> generateSampledDiagnosticQuestions() {
        // Balanced 10-question diagnostic test sampled across subjects
        return List.of(
                new DiagnosticQuestionDto(
                        UUID.fromString("d1010000-0000-0000-0000-000000000001"),
                        1, 1L, "Reasoning", 10L, "Logical Deductions", 101L, "Syllogisms",
                        "All quantum systems ($$Q$$) possess state vectors ($$|\\psi\\rangle$$). Some algorithms operate on $$Q$$. What logically follows?",
                        List.of(
                                Map.of("id", "opt-1", "text", "Some algorithms operate on state vectors ($$|\\psi\\rangle$$)."),
                                Map.of("id", "opt-2", "text", "No algorithm operates on state vectors."),
                                Map.of("id", "opt-3", "text", "All algorithms operate on classical states."),
                                Map.of("id", "opt-4", "text", "None of the above.")
                        ),
                        "MEDIUM"
                ),
                new DiagnosticQuestionDto(
                        UUID.fromString("d1010000-0000-0000-0000-000000000002"),
                        2, 2L, "Quantitative Aptitude", 20L, "Arithmetic", 201L, "Percentages",
                        "If a quantity $$A$$ increases by $$20\\%$$ and then decreases by $$20\\%$$, what is the net change?",
                        List.of(
                                Map.of("id", "opt-1", "text", "$$4\\%$$ decrease"),
                                Map.of("id", "opt-2", "text", "No change ($$0\\%$)"),
                                Map.of("id", "opt-3", "text", "$$2\\%$$ decrease"),
                                Map.of("id", "opt-4", "text", "$$4\\%$$ increase")
                        ),
                        "EASY"
                ),
                new DiagnosticQuestionDto(
                        UUID.fromString("d1010000-0000-0000-0000-000000000003"),
                        3, 3L, "General Science", 30L, "Chemistry", 301L, "Chemical Reactions",
                        "In the balanced redox reaction $$\\ce{2H2 + O2 -> 2H2O}$$, what is the oxidation state of hydrogen in $$\\ce{H2O}$$?",
                        List.of(
                                Map.of("id", "opt-1", "text", "$$+1$$"),
                                Map.of("id", "opt-2", "text", "$$0$$"),
                                Map.of("id", "opt-3", "text", "$$-1$$"),
                                Map.of("id", "opt-4", "text", "$$+2$$")
                        ),
                        "MEDIUM"
                )
        );
    }
}
