// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.dto.PracticeResultDto;
import com.examplatform.practice.dto.QuestionResultDto;
import com.examplatform.practice.exception.PracticeSessionNotFoundException;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.util.PracticeQuestionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeResultService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeResponseRepository practiceResponseRepository;
    private final PracticeSetRepository practiceSetRepository;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PracticeResultDto getResult(UUID sessionId, UUID candidateId) {
        return getResult(sessionId, candidateId, null);
    }

    @Transactional(readOnly = true)
    public PracticeResultDto getResult(UUID sessionId, UUID candidateId, String requestedLang) {
        PracticeSession session = practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId)
                .orElseThrow(() -> new PracticeSessionNotFoundException(sessionId));

        if (!"SUBMITTED".equals(session.getStatus())) {
            throw new IllegalStateException("Session is not yet submitted");
        }

        String targetLang = (requestedLang != null && !requestedLang.isBlank())
                ? requestedLang.trim().toLowerCase()
                : (session.getPreferredLanguage() != null && !session.getPreferredLanguage().isBlank()
                        ? session.getPreferredLanguage().trim().toLowerCase()
                        : "en");

        PracticeSet practiceSet = practiceSetRepository.findById(session.getPracticeSetId()).orElse(null);
        String practiceSetName = practiceSet != null ? practiceSet.getName() : "Practice Assessment";

        List<PracticeResponse> responses = practiceResponseRepository.findByPracticeSessionId(sessionId);
        Map<UUID, PracticeResponse> latestByQuestion = PracticeQuestionUtils.getLatestResponsesByQuestion(responses);

        String rawQuestionIds = practiceSet != null ? practiceSet.getQuestionIds() : null;
        List<UUID> orderedQuestionIds = PracticeQuestionUtils.resolveOrderedQuestionIds(rawQuestionIds, latestByQuestion, objectMapper);

        Map<UUID, AnswerKeyDto> answerKeyMap = questionBankClient.getAnswerKeys(orderedQuestionIds);

        List<QuestionResultDto> qResults = new ArrayList<>();
        int flaggedCount = 0;
        for (UUID qId : orderedQuestionIds) {
            PracticeResponse r = latestByQuestion.get(qId);
            AnswerKeyDto ak = answerKeyMap.get(qId);

            String candidateAns = null;
            boolean isCorrect = false;
            int marks = 0;
            long timeSpent = 0;
            boolean markedForReview = false;

            if (r != null) {
                candidateAns = r.getSelectedOptionIds() != null && !r.getSelectedOptionIds().isBlank() ? r.getSelectedOptionIds()
                        : r.getEnteredValue();
                isCorrect = r.isCorrect();
                marks = r.getMarksAwarded();
                timeSpent = r.getTimeSpentMs();
                markedForReview = r.isMarkedForReview();
                if (markedForReview) {
                    flaggedCount++;
                }
            }

            String correctAns = ak != null ? ak.answerKey() : null;
            String content = ak != null ? ak.content() : null;
            String optionsJson = ak != null ? ak.optionsJson() : null;
            String explanation = ak != null ? ak.explanation() : null;
            String topic = ak != null ? ak.topicName() : null;
            String subject = ak != null ? ak.subject() : null;
            String questionType = ak != null ? ak.questionType() : null;

            Map<String, Map<String, Object>> translations = ak != null ? ak.translations() : null;
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

            qResults.add(new QuestionResultDto(
                    qId,
                    candidateAns,
                    correctAns,
                    isCorrect,
                    marks,
                    timeSpent,
                    markedForReview,
                    content,
                    optionsJson,
                    explanation,
                    topic,
                    subject,
                    questionType,
                    targetLang,
                    fallbackToEnglish,
                    primaryTranslation,
                    translations
            ));
        }

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
                qResults,
                practiceSetName,
                session.getMode(),
                flaggedCount
        );
    }


}
