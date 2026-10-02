// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.*;
import com.examplatform.practice.event.PracticeSessionCompletedEvent;
import com.examplatform.practice.exception.PracticeSessionNotFoundException;
import com.examplatform.practice.exception.PracticeSetNotFoundException;
import com.examplatform.practice.exception.SessionAlreadySubmittedException;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.examplatform.practice.repository.PracticeSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PracticeSessionService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSetRepository practiceSetRepository;
    private final PracticeResponseRepository practiceResponseRepository;
    private final PracticeEvaluationService practiceEvaluationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PracticeSessionDto startSession(UUID candidateId, StartSessionRequest req) {
        PracticeSet practiceSet = practiceSetRepository.findById(req.practiceSetId())
                .filter(PracticeSet::isPublished)
                .orElseThrow(() -> new PracticeSetNotFoundException(req.practiceSetId()));

        String mode = req.mode() != null ? req.mode().toUpperCase() : "TIMED";

        PracticeSession session = PracticeSession.builder()
                .candidateId(candidateId)
                .practiceSetId(practiceSet.getId())
                .mode(mode)
                .status("IN_PROGRESS")
                .startedAt(Instant.now())
                .totalQuestions(practiceSet.getTotalQuestions())
                .durationMinutes(practiceSet.getDurationMinutes())
                .build();

        practiceSessionRepository.save(session);
        return toSessionDto(session);
    }

    @Transactional(readOnly = true)
    public PracticeSessionDto getSession(UUID sessionId, UUID candidateId) {
        return practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .map(this::toSessionDto)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));
    }

    @Transactional
    public void saveResponse(UUID sessionId, UUID candidateId, SaveResponseRequest req) {
        PracticeSession session = practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));

        if ("SUBMITTED".equals(session.getStatus()) || "ABANDONED".equals(session.getStatus())) {
            throw new SessionAlreadySubmittedException(sessionId);
        }

        int nextRevision = practiceResponseRepository
                .findByPracticeSessionIdOrderByRevisionSequenceDesc(sessionId)
                .stream()
                .filter(r -> r.getQuestionId().equals(req.questionId()))
                .mapToInt(PracticeResponse::getRevisionSequence)
                .max()
                .orElse(0) + 1;

        PracticeResponse response = PracticeResponse.builder()
                .practiceSessionId(sessionId)
                .questionId(req.questionId())
                .selectedOptionIds(req.selectedOptionIds())
                .enteredValue(req.enteredValue())
                .timeSpentMs(req.timeSpentMs())
                .markedForReview(req.markedForReview())
                .revisionSequence(nextRevision)
                .build();

        practiceResponseRepository.save(response);
    }

    @Transactional
    public PracticeResultDto submitSession(UUID sessionId, UUID candidateId) {
        PracticeSession session = practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));

        if ("SUBMITTED".equals(session.getStatus())) {
            throw new SessionAlreadySubmittedException(sessionId);
        }

        session.setStatus("SUBMITTED");
        session.setSubmittedAt(Instant.now());
        practiceSessionRepository.save(session);

        practiceEvaluationService.evaluateSession(sessionId);

        PracticeSession evaluated = practiceSessionRepository.findById(sessionId).orElseThrow();

        eventPublisher.publishEvent(new PracticeSessionCompletedEvent(
                sessionId, candidateId, evaluated.getPracticeSetId(),
                evaluated.getCorrectCount(), evaluated.getIncorrectCount(),
                evaluated.getSkippedCount(), evaluated.getObtainedMarks(),
                evaluated.getTotalMarks(), evaluated.getTopicWiseBreakdown()
        ));

        return new com.examplatform.practice.dto.PracticeResultDto(
                sessionId, evaluated.getCorrectCount(), evaluated.getIncorrectCount(),
                evaluated.getSkippedCount(), evaluated.getObtainedMarks(), evaluated.getTotalMarks(),
                evaluated.getTotalQuestions() > 0
                        ? Math.round((double) evaluated.getCorrectCount() / evaluated.getTotalQuestions() * 1000.0) / 10.0
                        : 0.0,
                evaluated.getTopicWiseBreakdown(), evaluated.getDifficultyBreakdown(),
                evaluated.getTimingBreakdown(), java.util.Collections.emptyList()
        );
    }

    @Transactional(readOnly = true)
    public Page<PracticeHistoryItemDto> getHistory(UUID candidateId, Pageable pageable) {
        return practiceSessionRepository.findByCandidateIdOrderByStartedAtDesc(candidateId, pageable)
                .map(s -> new PracticeHistoryItemDto(
                        s.getId(), s.getPracticeSetId(),
                        null,
                        s.getSubmittedAt(), s.getObtainedMarks(), s.getTotalMarks(),
                        s.getTotalQuestions() > 0
                                ? Math.round((double) s.getCorrectCount() / s.getTotalQuestions() * 1000.0) / 10.0
                                : 0.0,
                        s.getTotalQuestions(), s.getCorrectCount(), s.getIncorrectCount()
                ));
    }

    private PracticeSessionDto toSessionDto(PracticeSession s) {
        return new PracticeSessionDto(
                s.getId(), s.getPracticeSetId(), s.getMode(), s.getStatus(),
                s.getStartedAt(), s.getTotalQuestions(), s.getDurationMinutes()
        );
    }
}
