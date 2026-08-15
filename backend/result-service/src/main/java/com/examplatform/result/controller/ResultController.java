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

package com.examplatform.result.controller;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.ComputeResultsRequest;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.result.service.ResultPublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for result operations.
 * Exposes endpoints for retrieving candidate results and triggering result computation.
 *
 * Validates: Requirements 13.1, 13.2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultController {

    private static final String DEFAULT_TENANT_ID = "default";

    private final ResultComputationService resultComputationService;
    private final ResultPublicationService resultPublicationService;

    /**
     * Retrieves the result for a specific candidate in an exam.
     * Accessible by the candidate themselves (CANDIDATE role) or SUPER_ADMIN.
     *
     * @param candidateId the candidate UUID
     * @param examId      the exam UUID
     * @param auth        the authentication principal
     * @return the result entity
     */
    @GetMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'SUPER_ADMIN')")
    public ResponseEntity<Result> getResult(@PathVariable UUID candidateId,
                                            @RequestParam UUID examId,
                                            Authentication auth) {
        String tenantId = extractTenantId(auth);
        log.info("GET result for candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);
        Result result = resultComputationService.getResult(candidateId, examId, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * Triggers result computation for an exam.
     * Accessible only by EXAM_CONTROLLER role.
     *
     * @param request the computation request containing candidate scores
     * @param auth    the authentication principal
     * @return the list of computed results
     */
    @PostMapping("/compute")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<List<Result>> computeResults(@Valid @RequestBody ComputeResultsRequest request,
                                                       Authentication auth) {
        String tenantId = extractTenantId(auth);
        log.info("POST compute results for exam={}, candidates={}, normalize={}, tenant={}",
                request.getExamId(), request.getCandidateScores().size(),
                request.isNormalizeShifts(), tenantId);

        List<Result> results = resultComputationService.computeResults(
                request.getExamId(),
                request.getCandidateScores(),
                request.isNormalizeShifts(),
                tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * Publishes a candidate's result, triggering DigiLocker push and notification.
     * Accessible only by EXAM_CONTROLLER role.
     *
     * @param candidateId the candidate UUID
     * @param examId      the exam UUID
     * @param auth        the authentication principal
     * @return the published result
     */
    @PostMapping("/{candidateId}/publish")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Result> publishResult(@PathVariable UUID candidateId,
                                                @RequestParam UUID examId,
                                                Authentication auth) {
        String tenantId = extractTenantId(auth);
        log.info("POST publish result for candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);

        Result result = resultPublicationService.publishResult(candidateId, examId, tenantId);
        return ResponseEntity.ok(result);
    }

    private String extractTenantId(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
            Object tenant = details.get("tenant_id");
            if (tenant != null) {
                return tenant.toString();
            }
        }
        return DEFAULT_TENANT_ID;
    }
}
