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
import com.examplatform.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manual evaluation workflow service.
 * Handles dual-evaluator routing for subjective responses,
 * score recording, and arbitration flagging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManualEvaluationService {

    private static final String EVALUATION_EVENTS_TOPIC = "exam.evaluation.events";
    private static final double DEFAULT_SCORE_TOLERANCE = 0.2; // 20% of max marks

    private final EvaluationRepository evaluationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Record an evaluator's score for a manual evaluation.
     * If this is the second evaluator and scores diverge beyond tolerance,
     * the evaluation is flagged for arbitration.
     *
     * @param evaluationId the evaluation to score
     * @param evaluatorId  the evaluator recording the score
     * @param score        the score awarded
     * @param comments     evaluator's comments/feedback
     * @return the updated Evaluation entity
     */
    public Evaluation recordScore(UUID evaluationId, UUID evaluatorId,
                                   double score, String comments) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        if (evaluation.getEvaluationType() != Evaluation.EvaluationType.MANUAL) {
            throw new IllegalStateException("Only MANUAL evaluations can be scored manually");
        }

        if (evaluation.getStatus() != Evaluation.EvaluationStatus.PENDING
                && evaluation.getStatus() != Evaluation.EvaluationStatus.AUTO_EVALUATED) {
            throw new IllegalStateException(
                    "Evaluation is not in a scorable state. Current: " + evaluation.getStatus());
        }

        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setScore(BigDecimal.valueOf(score));
        evaluation.setComments(comments);
        evaluation.setStatus(Evaluation.EvaluationStatus.MANUAL_EVALUATED);

        Evaluation saved = evaluationRepository.save(evaluation);

        // Check for dual-evaluator arbitration
        checkForArbitration(saved);

        // Notify evaluators via Kafka
        publishScoreRecordedEvent(saved);

        log.info("Score recorded for evaluation {} by evaluator {}: {}",
                evaluationId, evaluatorId, score);
        return saved;
    }

    /**
     * Notify evaluators that auto-evaluation is complete and manual review is needed.
     * Assigns 2 evaluators per subjective response via Kafka event.
     *
     * @param sessionId   the session that completed auto-evaluation
     * @param candidateId the candidate
     * @param tenantId    examination authority
     */
    public void notifyEvaluatorsForManualReview(UUID sessionId, UUID candidateId, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "MANUAL_EVALUATION_REQUIRED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "evaluatorsRequired", 2,
                    "tenantId", tenantId,
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(EVALUATION_EVENTS_TOPIC, sessionId.toString(), event);
            log.info("Manual evaluation notification sent for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to publish MANUAL_EVALUATION_REQUIRED event: {}", e.getMessage());
        }
    }

    /**
     * Checks if two evaluators have scored the same question for a candidate
     * and flags for arbitration if scores diverge beyond tolerance.
     */
    private void checkForArbitration(Evaluation evaluation) {
        // Find peer evaluations for same session + question
        List<Evaluation> peerEvaluations = evaluationRepository
                .findBySessionIdAndTenantId(evaluation.getSessionId(), evaluation.getTenantId())
                .stream()
                .filter(e -> e.getQuestionId().equals(evaluation.getQuestionId()))
                .filter(e -> e.getEvaluationType() == Evaluation.EvaluationType.MANUAL)
                .filter(e -> e.getStatus() == Evaluation.EvaluationStatus.MANUAL_EVALUATED)
                .toList();

        if (peerEvaluations.size() >= 2) {
            BigDecimal score1 = peerEvaluations.get(0).getScore();
            BigDecimal score2 = peerEvaluations.get(1).getScore();
            BigDecimal maxMarks = evaluation.getMaxMarks();

            double tolerance = maxMarks.doubleValue() * DEFAULT_SCORE_TOLERANCE;
            double scoreDiff = Math.abs(score1.doubleValue() - score2.doubleValue());

            if (scoreDiff > tolerance) {
                // Flag all peer evaluations for arbitration
                for (Evaluation peer : peerEvaluations) {
                    peer.setStatus(Evaluation.EvaluationStatus.ARBITRATION);
                }
                evaluationRepository.saveAll(peerEvaluations);

                log.warn("Arbitration flagged for question {} in session {}: " +
                         "score1={}, score2={}, tolerance={}",
                        evaluation.getQuestionId(), evaluation.getSessionId(),
                        score1, score2, tolerance);
            }
        }
    }

    private void publishScoreRecordedEvent(Evaluation evaluation) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "MANUAL_SCORE_RECORDED",
                    "evaluationId", evaluation.getId().toString(),
                    "sessionId", evaluation.getSessionId().toString(),
                    "evaluatorId", evaluation.getEvaluatorId().toString(),
                    "score", evaluation.getScore().toString(),
                    "status", evaluation.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(EVALUATION_EVENTS_TOPIC, evaluation.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish MANUAL_SCORE_RECORDED event: {}", e.getMessage());
        }
    }
}
