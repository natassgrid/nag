package com.examplatform.evaluation.service;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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

    private final EvaluationRepository evaluationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
    public Map<String, Object> aggregateScores(UUID sessionId, UUID candidateId, String tenantId) {
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
            kafkaTemplate.send(EVALUATION_EVENTS_TOPIC, key, event);
        } catch (Exception e) {
            log.error("Failed to publish SCORES_AGGREGATED event: {}", e.getMessage());
        }
    }
}
