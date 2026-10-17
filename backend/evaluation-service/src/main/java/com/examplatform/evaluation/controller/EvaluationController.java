/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.evaluation.controller;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AggregatedScoreResult;
import com.examplatform.evaluation.dto.ScoreRecordedResponse;
import com.examplatform.evaluation.dto.ScoreRequest;
import com.examplatform.evaluation.service.ManualEvaluationService;
import com.examplatform.evaluation.service.ScoreAggregationService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for evaluation endpoints.
 * Handles manual scoring, score aggregation requests, and candidate evaluation queries.
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
    public ResponseEntity<ScoreRecordedResponse> recordScore(
            @PathVariable UUID id,
            @Valid @RequestBody ScoreRequest request) {

        log.info("Manual score submission for evaluation={} by evaluator={}",
                id, request.getEvaluatorId());

        Evaluation evaluation = manualEvaluationService.recordScore(
                id, request.getEvaluatorId(), request.getScore(), request.getComments());

        return ResponseEntity.ok(new ScoreRecordedResponse(
                evaluation.getId(),
                evaluation.getStatus().name(),
                evaluation.getScore(),
                "Score recorded successfully"
        ));
    }

    /**
     * Aggregate scores for a candidate's session.
     * POST /api/v1/evaluations/aggregate
     */
    @PostMapping("/aggregate")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<AggregatedScoreResult> aggregateScores(
            @RequestBody Map<String, String> body) {

        UUID sessionId = UUID.fromString(body.get("sessionId"));
        UUID candidateId = UUID.fromString(body.get("candidateId"));
        UUID examId = body.containsKey("examId") && body.get("examId") != null ? UUID.fromString(body.get("examId")) : null;
        String tenantId = TenantContext.get() != null ? TenantContext.get() : SharedConstants.DEFAULT_TENANT;

        log.info("Score aggregation requested for session={}, candidate={}, exam={}", sessionId, candidateId, examId);

        AggregatedScoreResult result = scoreAggregationService.aggregateScores(
                sessionId, candidateId, examId, tenantId);

        return ResponseEntity.ok(result);
    }

    /**
     * Retrieve all evaluations for a candidate.
     * GET /api/v1/evaluations/candidate/{candidateId}
     */
    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EVALUATOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<List<Evaluation>> getCandidateEvaluations(
            @PathVariable UUID candidateId,
            @RequestParam(required = false) UUID sessionId,
            @RequestHeader(value = SharedConstants.HEADER_TENANT_ID, defaultValue = SharedConstants.DEFAULT_TENANT) String tenantId) {

        log.info("Fetching evaluations for candidate={}, sessionId={}, tenant={}", candidateId, sessionId, tenantId);

        List<Evaluation> evaluations = manualEvaluationService.getEvaluations(candidateId, sessionId, tenantId);
        return ResponseEntity.ok(evaluations);
    }
}
