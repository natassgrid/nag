package com.examplatform.evaluation.service;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-evaluation service for objective question types (Single_MCQ, Multi_MCQ, Numerical).
 * Evaluates candidate responses against the answer key and applies the configured marking scheme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AutoEvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Auto-evaluate all responses for a finalized session.
     *
     * @param sessionId   the session that was submitted
     * @param candidateId the candidate
     * @param answerKeys  list of answer keys for all questions in the paper
     * @param responses   list of candidate's final responses
     * @param tenantId    tenant context
     * @return list of Evaluation records created
     */
    public List<Evaluation> evaluateSession(UUID sessionId, UUID candidateId,
                                             List<AnswerKey> answerKeys,
                                             List<CandidateResponse> responses,
                                             String tenantId) {

        // Build a map of questionId → CandidateResponse for O(1) lookup
        Map<UUID, CandidateResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(CandidateResponse::getQuestionId, Function.identity()));

        List<Evaluation> evaluations = new ArrayList<>();

        for (AnswerKey answerKey : answerKeys) {
            CandidateResponse resp = responseMap.get(answerKey.getQuestionId());

            BigDecimal score;
            if (resp == null || !resp.isAttempted()) {
                // Unattempted → zero marks
                score = BigDecimal.ZERO;
            } else if ("MULTI_MCQ".equals(answerKey.getQuestionType())) {
                // Partial marking for Multi MCQ
                double partialScore = evaluateMultiMcqPartial(
                        answerKey.getCorrectAnswer(),
                        resp.getSelectedOptionIds(),
                        answerKey.getMarksPerQuestion());
                score = BigDecimal.valueOf(partialScore);
            } else {
                // Evaluate based on question type (Single MCQ, Numerical, etc.)
                boolean correct = evaluateAnswer(answerKey, resp);
                score = correct
                        ? BigDecimal.valueOf(answerKey.getMarksPerQuestion())
                        : BigDecimal.valueOf(-answerKey.getNegativeMarks());
            }

            Evaluation eval = Evaluation.builder()
                    .sessionId(sessionId)
                    .questionId(answerKey.getQuestionId())
                    .candidateId(candidateId)
                    .evaluationType(Evaluation.EvaluationType.AUTO)
                    .score(score)
                    .maxMarks(BigDecimal.valueOf(answerKey.getMarksPerQuestion()))
                    .negativeMarks(BigDecimal.valueOf(answerKey.getNegativeMarks()))
                    .status(Evaluation.EvaluationStatus.AUTO_EVALUATED)
                    .build();
            eval.setTenantId(tenantId);
            evaluations.add(eval);
        }

        return evaluationRepository.saveAll(evaluations);
    }

    /**
     * Determines whether the candidate's answer is correct based on question type.
     */
    boolean evaluateAnswer(AnswerKey answerKey, CandidateResponse response) {
        return switch (answerKey.getQuestionType()) {
            case "SINGLE_MCQ" -> evaluateSingleMcq(answerKey.getCorrectAnswer(), response.getSelectedOptionIds());
            case "MULTI_MCQ" -> evaluateMultiMcq(answerKey.getCorrectAnswer(), response.getSelectedOptionIds());
            case "NUMERICAL" -> evaluateNumerical(answerKey.getCorrectAnswer(), response.getEnteredValue());
            default -> false; // Descriptive, Coding, etc. require manual evaluation
        };
    }

    /**
     * Single MCQ: exact match of the selected option against the correct option.
     * Both are JSON arrays with a single element e.g. ["opt-2"].
     */
    boolean evaluateSingleMcq(String correctAnswer, String selectedOptionIds) {
        if (correctAnswer == null || selectedOptionIds == null) return false;
        return normalizeJsonArray(correctAnswer).equals(normalizeJsonArray(selectedOptionIds));
    }

    /**
     * Multi MCQ: selected set must exactly equal the correct set for full marks.
     * (Partial marking is handled separately via evaluateMultiMcqPartial)
     */
    boolean evaluateMultiMcq(String correctAnswer, String selectedOptionIds) {
        if (correctAnswer == null || selectedOptionIds == null) return false;
        Set<String> correct = parseOptionIds(correctAnswer);
        Set<String> selected = parseOptionIds(selectedOptionIds);
        return correct.equals(selected);
    }

    /**
     * Partial marking for Multi_MCQ:
     * - If selection ⊆ answerKey (all selected are correct, possibly not all):
     *   score = (|selection ∩ answerKey| / |answerKey|) × marksPerQuestion
     * - If selection contains ANY incorrect option (not in answerKey):
     *   score = 0.0 (zero marks, no negative)
     * - If selection is empty/null:
     *   score = 0.0 (unattempted)
     */
    double evaluateMultiMcqPartial(String correctAnswer, String selectedOptionIds, double marksPerQuestion) {
        Set<String> correct = parseOptionIds(correctAnswer);
        Set<String> selected = parseOptionIds(selectedOptionIds);

        if (selected.isEmpty()) return 0.0;

        // Check if selection contains any incorrect option
        Set<String> incorrectSelections = new HashSet<>(selected);
        incorrectSelections.removeAll(correct);
        if (!incorrectSelections.isEmpty()) {
            return 0.0; // Contains wrong option → zero marks
        }

        // selection ⊆ answerKey → partial marks
        Set<String> intersection = new HashSet<>(selected);
        intersection.retainAll(correct);
        return ((double) intersection.size() / correct.size()) * marksPerQuestion;
    }

    /**
     * Numerical: parse both values as doubles and compare with tolerance of 0.001.
     */
    boolean evaluateNumerical(String correctAnswer, String enteredValue) {
        if (correctAnswer == null || enteredValue == null) return false;
        try {
            double expected = Double.parseDouble(correctAnswer.trim());
            double actual = Double.parseDouble(enteredValue.trim());
            return Math.abs(expected - actual) < 0.001;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numerical answer: correct='{}', entered='{}'", correctAnswer, enteredValue);
            return false;
        }
    }

    /**
     * Parse a JSON array of strings (e.g. ["opt-1","opt-3"]) into a Set.
     */
    Set<String> parseOptionIds(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptySet();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new TreeSet<>(list);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse option IDs JSON: '{}'", json, e);
            return Collections.emptySet();
        }
    }

    /**
     * Normalize a JSON array by sorting its elements for consistent comparison.
     * Returns a canonical sorted JSON string.
     */
    String normalizeJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return "[]";
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            Collections.sort(list);
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("Failed to normalize JSON array: '{}'", json, e);
            return json.trim();
        }
    }
}
