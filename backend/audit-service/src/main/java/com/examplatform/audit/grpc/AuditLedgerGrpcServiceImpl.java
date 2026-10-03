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

package com.examplatform.audit.grpc;

import com.examplatform.audit.service.AuditQueryService;
import com.examplatform.shared.grpc.AuditLedgerEventGrpc;
import com.examplatform.shared.grpc.AuditLedgerGrpcRequest;
import com.examplatform.shared.grpc.AuditLedgerGrpcResponse;
import com.examplatform.shared.grpc.AuditLedgerGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * gRPC service implementation for Audit ledger operational event streaming and recent logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLedgerGrpcServiceImpl extends AuditLedgerGrpcServiceGrpc.AuditLedgerGrpcServiceImplBase {

    private final AuditQueryService auditQueryService;

    @Override
    public void getRecentLedgerEvents(AuditLedgerGrpcRequest request,
                                      StreamObserver<AuditLedgerGrpcResponse> responseObserver) {
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId()
                : "default";
        int limit = request.getLimit() > 0 ? request.getLimit() : 10;
        log.info("gRPC getRecentLedgerEvents request: tenantId={}, limit={}", tenantId, limit);

        try {
            List<Map<String, Object>> events = auditQueryService.getRecentLedgerEvents(tenantId, limit);

            AuditLedgerGrpcResponse.Builder builder = AuditLedgerGrpcResponse.newBuilder();
            for (Map<String, Object> ev : events) {
                AuditLedgerEventGrpc eventGrpc = AuditLedgerEventGrpc.newBuilder()
                        .setId(String.valueOf(ev.getOrDefault("id", "")))
                        .setAction(String.valueOf(ev.getOrDefault("action", "")))
                        .setEntityType(String.valueOf(ev.getOrDefault("entityType", "")))
                        .setPerformedBy(String.valueOf(ev.getOrDefault("performedBy", "")))
                        .setTimestamp(String.valueOf(ev.getOrDefault("timestamp", "")))
                        .setStatus(String.valueOf(ev.getOrDefault("status", "SUCCESS")))
                        .build();
                builder.addEvents(eventGrpc);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get recent audit ledger events via gRPC", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get audit events: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
