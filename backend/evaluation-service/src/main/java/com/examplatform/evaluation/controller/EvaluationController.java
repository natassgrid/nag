package com.examplatform.evaluation.controller;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.ScoreRequest;
import com.examplatform.evaluation.service.ManualEvaluationService;
import com.examplatform.evaluation.service.ScoreAggregationService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for evaluation endpoints.
 * Handles manual scoring and score aggregation requests.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final ManualEvaluationService manualEvaluationService;
    private final ScoreAggregationService scoreAggregationService;

    /**
     * Record a manual evaluator's score for an evaluation.
     * POST /api/v1/evaluations/{id}/score
     */
    @PostMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> recordScore(
            @PathVariable UUID id,
            @Valid @RequestBody ScoreRequest request) {

        log.info("Manual score submission for evaluation={} by evaluator={}",
                id, request.getEvaluatorId());

        Evaluation evaluation = manualEvaluationService.recordScore(
                id, request.getEvaluatorId(), request.getScore(), request.getComments());

        return ResponseEntity.ok(Map.of(
                "evaluationId", evaluation.getId(),
                "status", evaluation.getStatus().name(),
                "score", evaluation.getScore(),
                "message", "Score recorded successfully"
        ));
    }

    /**
     * Aggregate scores for a candidate's session.
     * POST /api/v1/evaluations/aggregate
     */
    @PostMapping("/aggregate")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> aggregateScores(
            @RequestBody Map<String, String> body) {

        UUID sessionId = UUID.fromString(body.get("sessionId"));
        UUID candidateId = UUID.fromString(body.get("candidateId"));
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.info("Score aggregation requested for session={}, candidate={}", sessionId, candidateId);

        Map<String, Object> result = scoreAggregationService.aggregateScores(
                sessionId, candidateId, tenantId);

        return ResponseEntity.ok(result);
    }
}
