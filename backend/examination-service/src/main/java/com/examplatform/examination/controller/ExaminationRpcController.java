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

package com.examplatform.examination.controller;

import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.rpc.JsonRpcRequest;
import com.examplatform.shared.rpc.JsonRpcResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JSON-RPC 2.0 controller and REST backup endpoint for Examination status breakdown and operational data.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExaminationRpcController {

    private final ExaminationService examinationService;

    /**
     * Primary JSON-RPC 2.0 endpoint for Examination service-to-service queries.
     * POST /rpc/v1/examination
     */
    @PostMapping("/rpc/v1/examination")
    public ResponseEntity<JsonRpcResponse> handleJsonRpc(@RequestBody JsonRpcRequest request) {
        log.debug("Received Examination JSON-RPC method=[{}]", request.method());

        if ("getExaminationStatusBreakdown".equalsIgnoreCase(request.method())) {
            String tenantId = request.params() != null && request.params().containsKey("tenantId")
                    ? String.valueOf(request.params().get("tenantId"))
                    : "default";

            Map<String, Object> breakdown = examinationService.getExaminationStatusBreakdown(tenantId);
            return ResponseEntity.ok(JsonRpcResponse.success(request.id(), breakdown));
        }

        return ResponseEntity.ok(JsonRpcResponse.error(request.id(), -32601, "Method not found: " + request.method()));
    }

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
