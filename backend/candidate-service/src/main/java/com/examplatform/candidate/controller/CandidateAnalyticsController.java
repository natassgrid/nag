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

package com.examplatform.candidate.controller;

import com.examplatform.candidate.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller providing operational metrics and candidate analytics for dashboards.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/candidates/analytics")
@RequiredArgsConstructor
public class CandidateAnalyticsController {

    private final CandidateProfileService candidateProfileService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(value = "tenantId", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(candidateProfileService.getCandidateMetrics(tenantId));
    }
}
