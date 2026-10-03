/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\n *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.service.QuestionService;
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
 * JSON-RPC 2.0 controller and REST backup endpoint for Question Bank metrics and operational data.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class QuestionBankRpcController {

    private final QuestionService questionService;

    /**
     * Primary JSON-RPC 2.0 endpoint for Question Bank service-to-service queries.
     * POST /rpc/v1/questionbank
     */
    @PostMapping("/rpc/v1/questionbank")
    public ResponseEntity<JsonRpcResponse> handleJsonRpc(@RequestBody JsonRpcRequest request) {
        log.debug("Received QuestionBank JSON-RPC method=[{}]", request.method());

        if ("getQuestionBankMetrics".equalsIgnoreCase(request.method())) {
            String tenantId = request.params() != null && request.params().containsKey("tenantId")
                    ? String.valueOf(request.params().get("tenantId"))
                    : "default";

            Map<String, Object> metrics = questionService.getQuestionBankMetrics(tenantId);
            return ResponseEntity.ok(JsonRpcResponse.success(request.id(), metrics));
        }

        return ResponseEntity.ok(JsonRpcResponse.error(request.id(), -32601, "Method not found: " + request.method()));
    }

    /**
     * REST backup endpoint for analytics summary.
     * GET /api/v1/questions/analytics/summary
     */
    @GetMapping("/api/v1/questions/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary(
            @RequestParam(required = false, defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(questionService.getQuestionBankMetrics(tenantId));
    }
}
