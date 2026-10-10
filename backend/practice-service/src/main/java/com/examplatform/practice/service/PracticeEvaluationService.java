// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.util.PracticeQuestionUtils;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final PracticeSetRepository practiceSetRepository;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void evaluateSession(UUID sessionId) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.examplatform.practice.exception.PracticeSessionNotFoundException(sessionId));

        PracticeSet practiceSet = session.getPracticeSetId() != null
                ? practiceSetRepository.findById(session.getPracticeSetId()).orElse(null)
                : null;

        List<PracticeResponse> responses = practiceResponseRepository.findByPracticeSessionId(sessionId);

        // Keep only latest response per question using revision sequence
        Map<UUID, PracticeResponse> latestResponses = PracticeQuestionUtils.getLatestResponsesByQuestion(responses);

        // Gather all question IDs from practice set if available, plus any responded questions
        List<UUID> allQuestionIds = new ArrayList<>();
        if (practiceSet != null && practiceSet.getQuestionIds() != null && !practiceSet.getQuestionIds().isBlank()) {
            allQuestionIds.addAll(PracticeQuestionUtils.extractQuestionIds(practiceSet.getQuestionIds(), objectMapper));
        }
        for (UUID qId : latestResponses.keySet()) {
            if (!allQuestionIds.contains(qId)) {
                allQuestionIds.add(qId);
            }
        }

        List<UUID> queryIds = !allQuestionIds.isEmpty() ? allQuestionIds : new ArrayList<>(latestResponses.keySet());
        Map<UUID, AnswerKeyDto> answerKeys = !queryIds.isEmpty()
                ? questionBankClient.getAnswerKeys(queryIds)
                : Collections.emptyMap();

        int totalQuestions = session.getTotalQuestions() > 0 ? session.getTotalQuestions() : allQuestionIds.size();
        if (totalQuestions == 0) {
            totalQuestions = latestResponses.size();
        }
        session.setTotalQuestions(totalQuestions);

        int calculatedTotalMarks = 0;
        for (UUID qId : allQuestionIds) {
            AnswerKeyDto ak = answerKeys.get(qId);
            int m = (ak != null && ak.marks() > 0) ? ak.marks() : DEFAULT_CORRECT_MARKS;
            calculatedTotalMarks += m;
        }
        int totalMarks = calculatedTotalMarks > 0 ? calculatedTotalMarks : (totalQuestions * DEFAULT_CORRECT_MARKS);

        int correct = 0, incorrect = 0, skipped = 0, obtained = 0;
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

        int noResponseCount = totalQuestions - latestResponses.size();
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

    boolean evaluateAnswer(PracticeResponse resp, AnswerKeyDto ak) {
        String answerKey = ak.answerKey();
        if (answerKey == null || answerKey.isBlank()) return false;
        String qType = ak.questionType() != null ? ak.questionType().toUpperCase().trim() : "";

        // 1. Numerical evaluation
        if ("NUMERICAL".equals(qType) || "NUMERIC".equals(qType)) {
            String entered = resp.getEnteredValue();
            if (entered == null || entered.isBlank()) return false;
            return evaluateNumerical(answerKey, entered);
        }

        // If enteredValue is set, evaluate numerical or direct string match
        if (resp.getEnteredValue() != null && !resp.getEnteredValue().isBlank()) {
            String entered = resp.getEnteredValue().trim();
            if (evaluateNumerical(answerKey, entered)) {
                return true;
            }
            return answerKey.trim().equalsIgnoreCase(entered);
        }

        // 2. MCQ option evaluation
        if (resp.getSelectedOptionIds() != null && !resp.getSelectedOptionIds().isBlank()) {
            Set<String> selectedSet = parseOptionIds(resp.getSelectedOptionIds());
            Set<String> correctSet = parseOptionIds(answerKey);

            if (selectedSet.isEmpty() || correctSet.isEmpty()) {
                return false;
            }

            if ("MULTI_MCQ".equals(qType) || correctSet.size() > 1) {
                // Multi-select MCQ: candidate's selected set must exactly match correct set
                return selectedSet.equals(correctSet);
            } else {
                // Single-select MCQ: candidate must have chosen exactly 1 option and it must match
                if (selectedSet.size() != 1) {
                    return false;
                }
                return selectedSet.equals(correctSet);
            }
        }

        return false;
    }

    private boolean evaluateNumerical(String expected, String actual) {
        try {
            double expVal = Double.parseDouble(expected.trim());
            double actVal = Double.parseDouble(actual.trim());
            return Math.abs(expVal - actVal) < 1e-6;
        } catch (NumberFormatException e) {
            return expected.trim().equalsIgnoreCase(actual.trim());
        }
    }

    private Set<String> parseOptionIds(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        String trimmed = raw.trim();
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        String val = item.asText().trim();
                        if (!val.isEmpty()) {
                            result.add(val);
                        }
                    }
                    return result;
                }
            } catch (Exception ignored) {}
            // Fallback stripping brackets
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        for (String part : trimmed.split(",")) {
            String clean = part.replace("\"", "").replace("'", "").trim();
            if (!clean.isEmpty()) {
                result.add(clean);
            }
        }
        return result;
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
