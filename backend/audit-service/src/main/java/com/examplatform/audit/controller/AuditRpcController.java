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

import com.examplatform.audit.service.AuditQueryService;
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

import java.util.List;
import java.util.Map;

/**
 * JSON-RPC 2.0 controller and REST backup endpoint for Audit ledger events and operational logs.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuditRpcController {

    private final AuditQueryService auditQueryService;

    /**
     * Primary JSON-RPC 2.0 endpoint for Audit service-to-service queries.
     * POST /rpc/v1/audit
     */
    @PostMapping("/rpc/v1/audit")
    public ResponseEntity<JsonRpcResponse> handleJsonRpc(@RequestBody JsonRpcRequest request) {
        log.debug("Received Audit JSON-RPC method=[{}]", request.method());

        if ("getRecentLedgerEvents".equalsIgnoreCase(request.method())) {
            String tenantId = request.params() != null && request.params().containsKey("tenantId")
                    ? String.valueOf(request.params().get("tenantId"))
                    : "default";
            int limit = request.params() != null && request.params().containsKey("limit")
                    ? Integer.parseInt(String.valueOf(request.params().get("limit")))
                    : 10;

            List<Map<String, Object>> events = auditQueryService.getRecentLedgerEvents(tenantId, limit);
            return ResponseEntity.ok(JsonRpcResponse.success(request.id(), events));
        }

        return ResponseEntity.ok(JsonRpcResponse.error(request.id(), -32601, "Method not found: " + request.method()));
    }

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
