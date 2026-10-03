/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.examination.controller;

import com.examplatform.examination.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST analytics backup endpoint for Examination metrics.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExaminationAnalyticsController {

    private final ExaminationService examinationService;

    /**
     * REST backup endpoint for analytics summary.
     * GET /api/v1/examinations/analytics/summary
     */
    @GetMapping("/api/v1/examinations/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary(
            @RequestParam(required = false, defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(examinationService.getExaminationStatusBreakdown(tenantId));
    }
}
