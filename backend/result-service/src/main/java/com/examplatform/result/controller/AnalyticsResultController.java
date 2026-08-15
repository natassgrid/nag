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

import com.examplatform.result.dto.QuestionAnalyticsResult;
import com.examplatform.result.service.QuestionAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for per-question analytics.
 * Provides difficulty index, discrimination index, and response distribution
 * for each question in an exam.
 *
 * Validates: Requirements 26.1, 26.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/results/analytics")
@RequiredArgsConstructor
public class AnalyticsResultController {

    private final QuestionAnalyticsService questionAnalyticsService;

    /**
     * Retrieves per-question analytics for an exam.
     * Accessible only by EXAM_CONTROLLER role.
     *
     * @param examId the exam UUID
     * @param auth   the authentication principal
     * @return list of per-question analytics results
     */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasRole('EXAM_CONTROLLER')")
    public ResponseEntity<List<QuestionAnalyticsResult>> getExamAnalytics(
            @PathVariable UUID examId,
            Authentication auth) {

        String tenantId = extractTenantId(auth);
        log.info("GET question analytics for exam={}, tenant={}", examId, tenantId);

        List<QuestionAnalyticsResult> analytics = questionAnalyticsService.computeAnalytics(examId, tenantId);
        return ResponseEntity.ok(analytics);
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
        return "default";
    }
}
