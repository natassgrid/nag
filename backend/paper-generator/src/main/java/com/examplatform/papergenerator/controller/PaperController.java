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

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.service.PaperApprovalService;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.papergenerator.service.PaperSerializer;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for paper generation endpoints.
 * Accepts blueprint-driven paper generation requests and returns
 * 202 Accepted with the generated paper ID.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 28.1, 28.2, 28.3, 28.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperAssemblyService paperAssemblyService;
    private final PaperSerializer paperSerializer;
    private final PaperApprovalService paperApprovalService;

    /**
     * Submits an async paper generation job.
     * Requires the EXAM_CONTROLLER role.
     *
     * @param request the paper generation request with blueprint rules
     * @param jwt     the authenticated user's JWT
     * @return 202 Accepted with the paper ID
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> generatePaper(
            @Valid @RequestBody PaperGenerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID generatedBy = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.info("Paper generation requested by user={}, examId={}, shiftId={}",
                generatedBy, request.getExamId(), request.getShiftId());

        Paper paper = paperAssemblyService.generatePaper(request, generatedBy, tenantId);

        Map<String, Object> response = Map.of(
                "paperId", paper.getId(),
                "status", paper.getStatus(),
                "message", "Paper generation submitted successfully"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Validates a paper JSON document against the expected schema.
     * Returns 200 OK if valid, or 422 Unprocessable Entity with validation errors.
     *
     * @param json the paper JSON string to validate
     * @return 200 OK or 422 with validation error details
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> validatePaper(@RequestBody String json) {
        log.info("Paper schema validation requested");

        List<String> errors = paperSerializer.validate(json);

        if (errors.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Paper document is valid"
            ));
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "valid", false,
                "errors", errors
        ));
    }

    /**
     * Approves a paper, encrypts it with a shift-specific key, and transitions
     * through DRAFT → APPROVED → ENCRYPTED.
     *
     * @param paperId the paper ID to approve
     * @return 200 OK with the updated paper details
     */
    @PostMapping("/{paperId}/approve")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> approvePaper(@PathVariable UUID paperId) {
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";
        log.info("Paper approval requested for paperId={}", paperId);

        Paper paper = paperApprovalService.approvePaper(paperId, tenantId);

        return ResponseEntity.ok(Map.of(
                "paperId", paper.getId(),
                "status", paper.getStatus(),
                "encryptionKeyId", paper.getEncryptionKeyId() != null ? paper.getEncryptionKeyId() : "",
                "message", "Paper approved and encrypted successfully"
        ));
    }
}
