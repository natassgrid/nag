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
import com.examplatform.result.dto.ExamReviewResponse;
import com.examplatform.result.service.ExamReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for post-exam question review and QR scorecard verification.
 * Returns candidate's question-by-question performance with real upstream solutions and peer benchmarks.\n *
 * Validates: SPEC-UI3, Issue #110
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ReviewController {

    private final ExamReviewService examReviewService;

    /**
     * Returns the full post-exam review for a candidate in an exam.
     * Includes each question, candidate's answer, correct answer, explanation, and peer statistics.
     *
     * Access: CANDIDATE role (own results only), EXAM_CONTROLLER, ADMIN, SUPER_ADMIN can view any.
     *
     * @param candidateId the candidate UUID
     * @param examId      the exam UUID
     * @param tenantId    the tenant identifier from header
     * @param auth        authentication principal
     * @return the exam review response
     */
    @GetMapping("/{candidateId}/review")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EXAM_CONTROLLER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ExamReviewResponse> getExamReview(
            @PathVariable UUID candidateId,
            @RequestParam UUID examId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            Authentication auth) {

        log.info("GET exam review for candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);
        validateReviewAccess(candidateId, auth);

        ExamReviewResponse reviewResponse = examReviewService.getExamReview(candidateId, examId, tenantId);
        return ResponseEntity.ok(reviewResponse);
    }

    /**
     * Public QR code verification endpoint.
     * Returns redacted result information (name-less) for a given QR verification code.
     *
     * @param code the QR verification UUID token from the scorecard
     * @return redacted result or 404 if code not found, 410 if invalidated
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyScorecard(@RequestParam(required = false) String code) {
        log.info("GET QR verification for code={}", code);

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "valid", false,
                    "error", "Verification code is required",
                    "code", ""
            ));
        }

        String trimmedCode = code.trim();
        Optional<Result> resultOpt = examReviewService.findByQrVerificationCode(trimmedCode);

        if (resultOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "valid", false,
                    "error", "Scorecard verification code not found: " + trimmedCode,
                    "code", trimmedCode
            ));
        }

        Result result = resultOpt.get();

        // Check if result has been invalidated / revoked
        if (result.getSectionalStatusJson() != null &&
                (result.getSectionalStatusJson().toUpperCase().contains("INVALIDATED") ||
                 result.getSectionalStatusJson().toUpperCase().contains("REVOKED"))) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                    "valid", false,
                    "error", "Scorecard has been invalidated",
                    "code", trimmedCode
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", true);
        response.put("code", trimmedCode);
        response.put("candidateId", result.getCandidateId());
        response.put("examId", result.getExamId());
        response.put("totalScore", result.getTotalScore());
        response.put("overallRank", result.getOverallRank());
        response.put("overallPercentile", result.getOverallPercentile());
        if (result.getCategoryRank() != null) {
            response.put("categoryRank", result.getCategoryRank());
        }
        if (result.getAccuracyRate() != null) {
            response.put("accuracyRate", result.getAccuracyRate());
        }
        response.put("status", "VALID");
        response.put("message", "Scorecard verified successfully");

        return ResponseEntity.ok(response);
    }

    private void validateReviewAccess(UUID candidateId, Authentication auth) {
        if (auth == null) {
            return;
        }

        boolean isElevated = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_EXAM_CONTROLLER"));

        if (!isElevated && auth.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            if (userId != null && !userId.equals(candidateId.toString())) {
                throw new AccessDeniedException("Candidates can only access their own exam reviews");
            }
        }
    }
}
