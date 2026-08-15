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

package com.examplatform.audit.controller;

import com.examplatform.audit.domain.AuditEvent;
import com.examplatform.audit.service.AuditQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for querying audit events.
 * Provides paginated, filterable access to the audit trail.
 * Accessible only to users with the AUDITOR role.
 *
 * Validates: Requirements 15.3
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit/events")
@RequiredArgsConstructor
public class AuditQueryController {

    private static final String DEFAULT_TENANT_ID = "default";

    private final AuditQueryService auditQueryService;

    /**
     * Query audit events with optional filters.
     *
     * @param userId     filter by actor/user ID
     * @param examId     filter by exam ID (matched against resource field)
     * @param actionType filter by event/action type
     * @param from       filter events from this time (inclusive)
     * @param to         filter events until this time (inclusive)
     * @param page       page number (0-based, default 0)
     * @param size       page size (default 20, max 100)
     * @return paginated audit events
     */
    @GetMapping
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditEvent>> queryAuditEvents(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String examId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        // Cap page size at 100
        int effectiveSize = Math.min(size, 100);
        String tenantId = extractTenantId(auth);

        log.info("Audit query by user={}, filters: userId={}, examId={}, actionType={}, from={}, to={}, page={}, size={}",
                auth.getName(), userId, examId, actionType, from, to, page, effectiveSize);

        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<AuditEvent> results = auditQueryService.queryEvents(
                userId, actionType, examId, from, to, tenantId, pageable);

        return ResponseEntity.ok(results);
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
