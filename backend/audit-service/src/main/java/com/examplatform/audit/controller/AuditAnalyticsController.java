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

package com.examplatform.audit.controller;

import com.examplatform.audit.service.AuditQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST analytics backup endpoint for Audit events.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuditAnalyticsController {

    private final AuditQueryService auditQueryService;

    /**
     * REST backup endpoint for recent ledger events.
     * GET /api/v1/audit/events/recent
     */
    @GetMapping("/api/v1/audit/events/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentEvents(
            @RequestParam(required = false, defaultValue = "default") String tenantId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditQueryService.getRecentLedgerEvents(tenantId, limit));
    }
}
