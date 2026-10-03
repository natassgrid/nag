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

package com.examplatform.admin.controller;

import com.examplatform.admin.dto.DashboardSummaryResponse;
import com.examplatform.admin.service.AdminDashboardService;
import com.examplatform.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Admin Dashboard operations and KPI aggregations.
 * Aggregates live domain metrics from question-bank, examination, and evaluation microservices
 * via JSON-RPC (with automatic REST fallback) for authenticated administrative and operational staff.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR', 'REVIEWER', 'EVALUATOR')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Retrieve aggregated dashboard summary metrics and status feeds for the active tenant.
     * GET /api/v1/admin/dashboard/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false) String tenantId) {
        String effectiveTenant = resolveTenant(tenantId);
        log.info("Fetching operational dashboard summary for tenant=[{}]", effectiveTenant);

        DashboardSummaryResponse response = adminDashboardService.getDashboardSummary(effectiveTenant);
        return ResponseEntity.ok(response);
    }

    private String resolveTenant(String paramTenant) {
        if (paramTenant != null && !paramTenant.isBlank()) {
            return paramTenant;
        }
        String contextTenant = TenantContext.get();
        if (contextTenant != null && !contextTenant.isBlank()) {
            return contextTenant;
        }
        return "default";
    }
}
