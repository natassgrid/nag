/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

package com.examplatform.evaluation.service;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.shared.config.DynamicConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DynamicConfigService dynamicConfigService;

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

        boolean instantGrade = dynamicConfigService.getBoolean(
                "evaluation.auto.grade.instant", tenantId, true);
        boolean anonymizeSheets = dynamicConfigService.getBoolean(
                "evaluation.anonymize.candidate.sheets", tenantId, true);

        // Build a map of questionId -> CandidateResponse for O(1) lookup
        Map<UUID, CandidateResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(CandidateResponse::getQuestionId, Function.identity()));

        List<Evaluation> evaluations = new ArrayList<>();

        for (AnswerKey answerKey : answerKeys) {
            CandidateResponse resp = responseMap.get(answerKey.getQuestionId());

            BigDecimal score;
            if (resp == null || !resp.isAttempted()) {
                // Unattempted -> zero marks
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
                    .status(instantGrade ? Evaluation.EvaluationStatus.AUTO_EVALUATED : Evaluation.EvaluationStatus.PENDING)
                    .build();
            eval.setTenantId(tenantId);
            evaluations.add(eval);
        }

        List<Evaluation> savedEvaluations = evaluationRepository.saveAll(evaluations);

        // Publish EVALUATION_CREATED audit event (one per session, fire-and-forget)
        publishEvaluationAuditEvent(sessionId, candidateId, savedEvaluations.size(), tenantId, anonymizeSheets);

        return savedEvaluations;
    }

    /**
     * Publishes an EVALUATION_CREATED audit event after auto-evaluation (one per session).
     */
    private void publishEvaluationAuditEvent(UUID sessionId, UUID candidateId,
                                              int evaluationCount, String tenantId, boolean anonymized) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "EVALUATION_CREATED",
                    "sessionId", sessionId.toString(),
                    "candidateId", anonymized ? ("ANON-" + UUID.nameUUIDFromBytes(candidateId.toString().getBytes())) : candidateId.toString(),
                    "evaluationCount", evaluationCount,
                    "evaluationType", "AUTO",
                    "tenantId", tenantId,
                    "anonymized", anonymized,
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(AUDIT_TOPIC, sessionId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish EVALUATION_CREATED audit event for session [{}]: {}",
                                    sessionId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing EVALUATION_CREATED audit event: {}", e.getMessage());
        }
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
        return parseOptionSet(correctAnswer).equals(parseOptionSet(selectedOptionIds));
    }

    /**
     * Numerical: parsed double comparison within EPSILON (1e-6) tolerance.
     */
    boolean evaluateNumerical(String correctAnswer, String enteredValue) {
        if (correctAnswer == null || enteredValue == null) return false;
        try {
            double expected = Double.parseDouble(correctAnswer.trim());
            double actual = Double.parseDouble(enteredValue.trim());
            return Math.abs(expected - actual) < 1e-6;
        } catch (NumberFormatException e) {
            log.debug("Failed to parse numerical answer: expected='{}', actual='{}'", correctAnswer, enteredValue);
            return false;
        }
    }

    /**
     * Partial marking scheme for MULTI_MCQ questions (JEE Advanced style):
     * - Full marks: all correct options selected, no incorrect options
     * - Partial marks: subset of correct options selected, no incorrect options
     *   formula: (marksPerQuestion / totalCorrectOptions) * countOfCorrectSelected
     * - Negative marks: if any incorrect option is selected -> -negativeMarks
     * - Zero marks: unattempted (handled upstream)
     *
     * @param correctAnswerJson  JSON array of correct option IDs e.g. ["opt-1", "opt-2"]
     * @param selectedOptionsJson JSON array of candidate's selected option IDs
     * @param maxMarks           full marks for this question
     * @return awarded score (positive fractional, full, zero, or negative)
     */
    public double evaluateMultiMcqPartial(String correctAnswerJson, String selectedOptionsJson, double maxMarks) {
        if (correctAnswerJson == null || selectedOptionsJson == null) return 0.0;

        Set<String> correct = parseOptionSet(correctAnswerJson);
        Set<String> selected = parseOptionSet(selectedOptionsJson);

        if (selected.isEmpty() || correct.isEmpty()) return 0.0;

        // Check for any incorrect options selected
        Set<String> incorrectSelected = new HashSet<>(selected);
        incorrectSelected.removeAll(correct);

        if (!incorrectSelected.isEmpty()) {
            // Negative marking for incorrect option selection (-2.0 default / negative marks)
            return -2.0;
        }

        // Only correct options were selected
        if (selected.equals(correct)) {
            return maxMarks; // Full marks
        }

        // Partial marks: proportional to correct options chosen
        return (maxMarks / (double) correct.size()) * (double) selected.size();
    }

    private Set<String> parseOptionSet(String json) {
        if (json == null || json.isBlank()) return Collections.emptySet();
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new TreeSet<>(list);
        } catch (Exception e) {
            log.debug("Could not parse JSON array for options: '{}'", json);
            return Collections.emptySet();
        }
    }

    private String normalizeJsonArray(String json) {
        Set<String> set = parseOptionSet(json);
        try {
            return objectMapper.writeValueAsString(set);
        } catch (Exception e) {
            return json.trim();
        }
    }
}
