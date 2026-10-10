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

package com.examplatform.evaluation.grpc;

import com.examplatform.evaluation.service.ManualEvaluationService;
import com.examplatform.shared.grpc.EvaluationMetricsGrpcRequest;
import com.examplatform.shared.grpc.EvaluationMetricsGrpcResponse;
import com.examplatform.shared.grpc.EvaluationMetricsGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * gRPC service implementation for Evaluation queue metrics and operational data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationMetricsGrpcServiceImpl extends EvaluationMetricsGrpcServiceGrpc.EvaluationMetricsGrpcServiceImplBase {

    private final ManualEvaluationService manualEvaluationService;

    @Override
    public void getEvaluationQueueMetrics(EvaluationMetricsGrpcRequest request,
                                         StreamObserver<EvaluationMetricsGrpcResponse> responseObserver) {
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId()
                : "default";
        log.info("gRPC getEvaluationQueueMetrics request for tenant={}", tenantId);

        try {
            Map<String, Object> metrics = manualEvaluationService.getEvaluationQueueMetrics(tenantId);

            EvaluationMetricsGrpcResponse response = EvaluationMetricsGrpcResponse.newBuilder()
                    .setPending(metricLong(metrics, "pending"))
                    .setInProgress(metricLong(metrics, "inProgress"))
                    .setCompleted(metricLong(metrics, "completed"))
                    .setFlagged(metricLong(metrics, "flagged"))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get evaluation queue metrics via gRPC", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get evaluation queue metrics: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
    private long metricLong(Map<String, Object> metrics, String key) {
        Object val = metrics != null ? metrics.get(key) : null;
        return (val instanceof Number n) ? n.longValue() : 0L;
    }
}
