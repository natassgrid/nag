// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeSessionService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSetRepository practiceSetRepository;
    private final PracticeResponseRepository practiceResponseRepository;
    private final PracticeEvaluationService practiceEvaluationService;
    private final PracticeResultService practiceResultService;
    private final QuestionBankClient questionBankClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public PracticeSessionDto startSession(UUID candidateId, StartSessionRequest req) {
        PracticeSet practiceSet = practiceSetRepository.findById(req.practiceSetId())
                .filter(PracticeSet::isPublished)
                .orElseThrow(() -> new PracticeSetNotFoundException(req.practiceSetId()));

        String mode = req.mode() != null ? req.mode().toUpperCase() : "TIMED";
        String preferredLanguage = (req.preferredLanguage() != null && !req.preferredLanguage().isBlank())
                ? req.preferredLanguage().trim().toLowerCase()
                : "en";

        List<UUID> qIds = resolveAndPersistQuestionIds(practiceSet, "startSession");

        int totalQuestions = practiceSet.getTotalQuestions() > 0
                ? practiceSet.getTotalQuestions()
                : qIds.size();

        PracticeSession session = PracticeSession.builder()
                .candidateId(candidateId)
                .practiceSetId(practiceSet.getId())
                .mode(mode)
                .status("IN_PROGRESS")
                .startedAt(Instant.now())
                .totalQuestions(totalQuestions)
                .durationMinutes(practiceSet.getDurationMinutes())
                .preferredLanguage(preferredLanguage)
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
    public List<PracticeQuestionDto> getSessionQuestions(UUID sessionId, UUID candidateId) {
        return getSessionQuestions(sessionId, candidateId, null);
    }

    @Transactional
    public List<PracticeQuestionDto> getSessionQuestions(UUID sessionId, UUID candidateId, String requestedLang) {
        PracticeSession session = practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));

        PracticeSet set = practiceSetRepository.findById(session.getPracticeSetId())
                .orElseThrow(() -> new PracticeSetNotFoundException(session.getPracticeSetId()));

        String targetLang = (requestedLang != null && !requestedLang.isBlank())
                ? requestedLang.trim().toLowerCase()
                : (session.getPreferredLanguage() != null && !session.getPreferredLanguage().isBlank()
                        ? session.getPreferredLanguage().trim().toLowerCase()
                        : "en");

        List<UUID> questionIds = resolveAndPersistQuestionIds(set, "getSessionQuestions");

        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, AnswerKeyDto> keys = questionBankClient.getAnswerKeys(questionIds);
        List<PracticeQuestionDto> result = new ArrayList<>();
        int order = 1;
        for (UUID qId : questionIds) {
            AnswerKeyDto key = keys.get(qId);
            if (key != null) {
                Map<String, Map<String, Object>> translations = key.translations();
                Map<String, Object> primaryTranslation = null;
                boolean fallbackToEnglish = false;

                if ("en".equalsIgnoreCase(targetLang)) {
                    primaryTranslation = null;
                    fallbackToEnglish = false;
                } else if (translations != null && translations.containsKey(targetLang)) {
                    primaryTranslation = translations.get(targetLang);
                    fallbackToEnglish = false;
                } else {
                    primaryTranslation = null;
                    fallbackToEnglish = true;
                }

                result.add(new PracticeQuestionDto(
                        qId,
                        order,
                        "PRAC-Q" + order,
                        key.content() != null ? key.content() : "",
                        key.optionsJson() != null ? key.optionsJson() : "[]",
                        key.marks() > 0 ? key.marks() : 2,
                        0.5,
                        key.subject(),
                        key.topicName(),
                        key.difficulty(),
                        targetLang,
                        fallbackToEnglish,
                        primaryTranslation,
                        translations
                ));
                order++;
            }
        }
        return result;
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

        return practiceResultService.getResult(sessionId, candidateId);
    }

    @Transactional(readOnly = true)
    public Page<PracticeHistoryItemDto> getHistory(UUID candidateId, Pageable pageable) {
        Page<PracticeSession> sessions = practiceSessionRepository.findByCandidateIdOrderByStartedAtDesc(candidateId, pageable);
        List<UUID> setIds = sessions.stream().map(PracticeSession::getPracticeSetId).distinct().toList();
        Map<UUID, String> setNames = new HashMap<>();
        if (!setIds.isEmpty()) {
            practiceSetRepository.findAllById(setIds).forEach(set -> setNames.put(set.getId(), set.getName()));
        }

        return sessions.map(s -> new PracticeHistoryItemDto(
                s.getId(), s.getPracticeSetId(),
                setNames.getOrDefault(s.getPracticeSetId(), "Practice Mock Test"),
                s.getSubmittedAt() != null ? s.getSubmittedAt() : s.getStartedAt(),
                s.getObtainedMarks(), s.getTotalMarks(),
                s.getTotalQuestions() > 0
                        ? Math.round((double) s.getCorrectCount() / s.getTotalQuestions() * 1000.0) / 10.0
                        : 0.0,
                s.getTotalQuestions(), s.getCorrectCount(), s.getIncorrectCount()
        ));
    }

    private List<UUID> resolveAndPersistQuestionIds(PracticeSet set, String operation) {
        List<UUID> questionIds = parseQuestionIds(set.getQuestionIds());
        if (questionIds.isEmpty() && set.getSubjectSlug() != null && !set.getSubjectSlug().isBlank()) {
            int limit = set.getTotalQuestions() > 0 ? set.getTotalQuestions() : 25;
            questionIds = questionBankClient.findQuestionIdsBySubject(set.getSubjectSlug(), limit);
            if (!questionIds.isEmpty()) {
                try {
                    set.setQuestionIds(objectMapper.writeValueAsString(questionIds));
                    if (set.getTotalQuestions() <= 0) {
                        set.setTotalQuestions(questionIds.size());
                    }
                    practiceSetRepository.save(set);
                } catch (Exception e) {
                    log.warn("Failed to persist resolved question IDs on {} for practice set {}: {}",
                            operation, set.getId(), e.getMessage());
                }
            }
        }
        return questionIds;
    }

    private List<UUID> parseQuestionIds(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private PracticeSessionDto toSessionDto(PracticeSession s) {
        return new PracticeSessionDto(
                s.getId(), s.getPracticeSetId(), s.getMode(), s.getStatus(),
                s.getStartedAt(), s.getTotalQuestions(), s.getDurationMinutes(),
                s.getPreferredLanguage() != null ? s.getPreferredLanguage() : "en"
        );
    }
}
