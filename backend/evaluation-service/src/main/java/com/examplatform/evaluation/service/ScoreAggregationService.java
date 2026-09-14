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
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregates evaluation scores for a candidate's exam session.
 * Computes totalRawScore and section-wise scores from all finalized evaluations.
 * Publishes aggregation result to the evaluation events topic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreAggregationService {

    private static final String EVALUATION_EVENTS_TOPIC = "exam.evaluation.events";
    private static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";

    private final EvaluationRepository evaluationRepository;
    private final EventPublisher eventPublisher;

    /**
     * Aggregate scores for a candidate's session.
     * Computes totalRawScore from all FINALIZED or MANUAL_EVALUATED evaluations.
     * Publishes event to exam.evaluation.events topic.
     *
     * @param sessionId   the exam session
     * @param candidateId the candidate
     * @param tenantId    examination authority
     * @return aggregation result map with total and section-wise scores
     */
    public Map<String, Object> aggregateScores(UUID sessionId, UUID candidateId, UUID examId, String tenantId) {
        List<Evaluation> evaluations = evaluationRepository
                .findBySessionIdAndTenantId(sessionId, tenantId)
                .stream()
                .filter(e -> e.getCandidateId().equals(candidateId))
                .filter(this::isFinalizedEvaluation)
                .toList();

        if (evaluations.isEmpty()) {
            log.warn("No finalized evaluations found for session={}, candidate={}",
                    sessionId, candidateId);
            return Map.of(
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "totalRawScore", BigDecimal.ZERO,
                    "totalMaxMarks", BigDecimal.ZERO,
                    "evaluationCount", 0
            );
        }

        // Compute total raw score
        BigDecimal totalRawScore = evaluations.stream()
                .map(Evaluation::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMaxMarks = evaluations.stream()
                .map(Evaluation::getMaxMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compute section-wise scores (grouped by evaluationType)
        Map<String, BigDecimal> sectionScores = evaluations.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEvaluationType().name(),
                        Collectors.reducing(BigDecimal.ZERO, Evaluation::getScore, BigDecimal::add)
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId.toString());
        result.put("candidateId", candidateId.toString());
        result.put("totalRawScore", totalRawScore.setScale(2, RoundingMode.HALF_UP));
        result.put("totalMaxMarks", totalMaxMarks.setScale(2, RoundingMode.HALF_UP));
        result.put("sectionScores", sectionScores);
        result.put("evaluationCount", evaluations.size());
        result.put("tenantId", tenantId);

        // Publish aggregation event
        publishAggregationEvent(result);
        
        publishEvaluationCompletedEvent(result, sessionId, candidateId, examId, tenantId, evaluations);

        log.info("Score aggregation complete for session={}, candidate={}: total={}",
                sessionId, candidateId, totalRawScore);
        return result;
    }

    private boolean isFinalizedEvaluation(Evaluation evaluation) {
        return evaluation.getStatus() == Evaluation.EvaluationStatus.FINALIZED
                || evaluation.getStatus() == Evaluation.EvaluationStatus.MANUAL_EVALUATED
                || evaluation.getStatus() == Evaluation.EvaluationStatus.AUTO_EVALUATED;
    }

    private void publishAggregationEvent(Map<String, Object> aggregation) {
        try {
            Map<String, Object> event = new HashMap<>(aggregation);
            event.put("eventType", "SCORES_AGGREGATED");
            event.put("occurredAt", Instant.now().toString());

            String key = aggregation.get("sessionId") + ":" + aggregation.get("candidateId");
            eventPublisher.publish(EVALUATION_EVENTS_TOPIC, key, event);
        } catch (Exception e) {
            log.error("Failed to publish SCORES_AGGREGATED event: {}", e.getMessage());
        }
    }

    /**
     * Publishes EVALUATION_COMPLETED event to trigger result-service computation.
     * Includes question-level scores with timeSpentMs for diagnostic analytics.
     * Validates: SPEC-E2
     */
    private void publishEvaluationCompletedEvent(Map<String, Object> aggregation,
                                                  UUID sessionId, UUID candidateId,
                                                  UUID examId, String tenantId,
                                                  List<Evaluation> evaluations) {
        try {
            // Build question-level score entries
            List<Map<String, Object>> questionLevelScores = evaluations.stream()
                    .map(e -> Map.<String, Object>of(
                            "questionId", e.getQuestionId().toString(),
                            "score", e.getScore(),
                            "maxMarks", e.getMaxMarks()
                    ))
                    .toList();

            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", "EVALUATION_COMPLETED");
            event.put("sessionId", sessionId.toString());
            event.put("candidateId", candidateId.toString());
            event.put("examId", examId != null ? examId.toString() : "");
            event.put("totalRawScore", aggregation.get("totalRawScore"));
            event.put("sectionScores", aggregation.getOrDefault("sectionScores", java.util.Map.of()));
            event.put("questionLevelScores", questionLevelScores);
            event.put("tenantId", tenantId);
            event.put("evaluatedAt", java.time.Instant.now().toString());

            eventPublisher.publish(EVALUATION_COMPLETED_TOPIC, sessionId.toString(), event);
            log.info("EVALUATION_COMPLETED event published: session={}, candidate={}, examId={}, questionCount={}",
                    sessionId, candidateId, examId, questionLevelScores.size());
        } catch (Exception e) {
            log.error("Failed to publish EVALUATION_COMPLETED event for session={}: {}", sessionId, e.getMessage());
            // Never block the aggregation for event failures
        }
    }
}
