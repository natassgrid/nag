// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.dto.PracticeResultDto;
import com.examplatform.practice.dto.QuestionResultDto;
import com.examplatform.practice.exception.PracticeSessionNotFoundException;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticeResultService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeResponseRepository practiceResponseRepository;

    @Transactional(readOnly = true)
    public PracticeResultDto getResult(UUID sessionId, UUID candidateId) {
        PracticeSession session = practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));

        if (!"SUBMITTED".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not yet submitted");
        }

        List<PracticeResponse> responses = practiceResponseRepository.findByPracticeSessionId(sessionId);
        Map<UUID, PracticeResponse> latestByQuestion = new LinkedHashMap<>();
        for (PracticeResponse r : responses) {
            latestByQuestion.merge(r.getQuestionId(), r,
                    (e, i) -> i.getRevisionSequence() > e.getRevisionSequence() ? i : e);
        }

        List<QuestionResultDto> qResults = latestByQuestion.values().stream()
                .map(r -> new QuestionResultDto(
                        r.getQuestionId(),
                        r.getSelectedOptionIds() != null ? r.getSelectedOptionIds() : r.getEnteredValue(),
                        null,
                        r.isCorrect(),
                        r.getMarksAwarded(),
                        r.getTimeSpentMs(),
                        r.isMarkedForReview()
                ))
                .toList();

        double accuracy = session.getTotalQuestions() > 0
                ? (double) session.getCorrectCount() / session.getTotalQuestions() * 100
                : 0.0;

        return new PracticeResultDto(
                session.getId(),
                session.getCorrectCount(),
                session.getIncorrectCount(),
                session.getSkippedCount(),
                session.getObtainedMarks(),
                session.getTotalMarks(),
                Math.round(accuracy * 10.0) / 10.0,
                session.getTopicWiseBreakdown(),
                session.getDifficultyBreakdown(),
                session.getTimingBreakdown(),
                qResults
        );
    }
}
