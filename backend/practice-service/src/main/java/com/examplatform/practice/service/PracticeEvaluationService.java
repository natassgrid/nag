// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticeEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PracticeEvaluationService.class);
    private static final int DEFAULT_CORRECT_MARKS = 2;
    private static final int DEFAULT_WRONG_PENALTY = -1;

    private final PracticeResponseRepository practiceResponseRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void evaluateSession(UUID sessionId) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.examplatform.practice.exception.PracticeSessionNotFoundException(sessionId));

        List<PracticeResponse> responses = practiceResponseRepository.findByPracticeSessionId(sessionId);

        List<UUID> questionIds = responses.stream()
                .map(PracticeResponse::getQuestionId)
                .distinct()
                .toList();

        Map<UUID, AnswerKeyDto> answerKeys = questionBankClient.getAnswerKeys(questionIds);

        Map<UUID, PracticeResponse> latestResponses = new LinkedHashMap<>();
        for (PracticeResponse r : responses) {
            latestResponses.merge(r.getQuestionId(), r,
                    (existing, incoming) -> incoming.getRevisionSequence() > existing.getRevisionSequence() ? incoming : existing);
        }

        int correct = 0, incorrect = 0, skipped = 0, obtained = 0;
        int totalMarks = (session.getTotalQuestions() > 0 ? session.getTotalQuestions() : questionIds.size()) * DEFAULT_CORRECT_MARKS;
        Map<String, Map<String, Integer>> topicBreakdown = new HashMap<>();
        Map<String, Integer[]> diffBreakdown = new HashMap<>();
        Map<String, Long> timingBreakdown = new HashMap<>();

        for (Map.Entry<UUID, PracticeResponse> entry : latestResponses.entrySet()) {
            UUID qId = entry.getKey();
            PracticeResponse resp = entry.getValue();
            AnswerKeyDto ak = answerKeys.get(qId);

            timingBreakdown.put(qId.toString(), resp.getTimeSpentMs());

            boolean hasAnswer = (resp.getSelectedOptionIds() != null && !resp.getSelectedOptionIds().isBlank() && !resp.getSelectedOptionIds().equals("[]"))
                    || (resp.getEnteredValue() != null && !resp.getEnteredValue().isBlank());

            if (!hasAnswer) {
                skipped++;
                resp.setCorrect(false);
                resp.setMarksAwarded(0);
            } else if (ak == null) {
                skipped++;
                resp.setCorrect(false);
                resp.setMarksAwarded(0);
            } else {
                boolean isCorrect = evaluateAnswer(resp, ak);
                resp.setCorrect(isCorrect);
                int marks = ak.marks() > 0 ? ak.marks() : DEFAULT_CORRECT_MARKS;
                int penalty = DEFAULT_WRONG_PENALTY;

                if (isCorrect) {
                    correct++;
                    resp.setMarksAwarded(marks);
                    obtained += marks;
                    String topic = ak.topicName() != null ? ak.topicName() : "General";
                    topicBreakdown.computeIfAbsent(topic, k -> new HashMap<>())
                            .merge("correct", 1, Integer::sum);
                    topicBreakdown.computeIfAbsent(topic, k -> new HashMap<>())
                            .merge("total", 1, Integer::sum);
                    String diff = ak.difficulty() != null ? ak.difficulty() : "MEDIUM";
                    diffBreakdown.computeIfAbsent(diff, k -> new Integer[]{0, 0});
                    diffBreakdown.get(diff)[0]++;
                    diffBreakdown.get(diff)[1]++;
                } else {
                    incorrect++;
                    resp.setMarksAwarded(penalty);
                    obtained += penalty;
                    String topic = ak.topicName() != null ? ak.topicName() : "General";
                    topicBreakdown.computeIfAbsent(topic, k -> new HashMap<>())
                            .merge("total", 1, Integer::sum);
                    String diff = ak.difficulty() != null ? ak.difficulty() : "MEDIUM";
                    diffBreakdown.computeIfAbsent(diff, k -> new Integer[]{0, 0});
                    diffBreakdown.get(diff)[1]++;
                }
            }
            practiceResponseRepository.save(resp);
        }

        int noResponseCount = session.getTotalQuestions() - latestResponses.size();
        skipped += Math.max(0, noResponseCount);

        String topicJson = serialize(topicBreakdown);
        String diffJson = serialize(diffBreakdown);
        String timingJson = serialize(timingBreakdown);

        session.setCorrectCount(correct);
        session.setIncorrectCount(incorrect);
        session.setSkippedCount(skipped);
        session.setTotalMarks(totalMarks);
        session.setObtainedMarks(Math.max(0, obtained));
        session.setTopicWiseBreakdown(topicJson);
        session.setDifficultyBreakdown(diffJson);
        session.setTimingBreakdown(timingJson);
        practiceSessionRepository.save(session);

        log.info("Practice session {} evaluated: correct={}, incorrect={}, skipped={}, obtained={}/{}",
                sessionId, correct, incorrect, skipped, obtained, totalMarks);
    }

    private boolean evaluateAnswer(PracticeResponse resp, AnswerKeyDto ak) {
        String answerKey = ak.answerKey();
        if (answerKey == null || answerKey.isBlank()) return false;
        String normalizedKey = answerKey.trim();

        if (resp.getEnteredValue() != null && !resp.getEnteredValue().isBlank()) {
            return normalizedKey.equalsIgnoreCase(resp.getEnteredValue().trim());
        }

        if (resp.getSelectedOptionIds() != null && !resp.getSelectedOptionIds().isBlank()) {
            String selected = resp.getSelectedOptionIds().trim();
            if (selected.equalsIgnoreCase(normalizedKey)) {
                return true;
            }
            // Strip JSON array brackets if present
            if (selected.startsWith("[") && selected.endsWith("]")) {
                String stripped = selected.substring(1, selected.length() - 1).replace("\"", "").replace("'", "").trim();
                for (String part : stripped.split(",")) {
                    if (part.trim().equalsIgnoreCase(normalizedKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize breakdown", e);
            return "{}";
        }
    }
}
