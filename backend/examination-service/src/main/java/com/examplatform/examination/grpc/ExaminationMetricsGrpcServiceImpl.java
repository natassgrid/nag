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

package com.examplatform.examination.grpc;

import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.grpc.ExamBreakdownGrpcRequest;
import com.examplatform.shared.grpc.ExamBreakdownGrpcResponse;
import com.examplatform.shared.grpc.ExaminationMetricsGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * gRPC service implementation for Examination status breakdown and operational metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExaminationMetricsGrpcServiceImpl extends ExaminationMetricsGrpcServiceGrpc.ExaminationMetricsGrpcServiceImplBase {

    private final ExaminationService examinationService;

    @Override
    public void getExaminationStatusBreakdown(ExamBreakdownGrpcRequest request,
                                             StreamObserver<ExamBreakdownGrpcResponse> responseObserver) {
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId()
                : "default";
        log.info("gRPC getExaminationStatusBreakdown request for tenant={}", tenantId);

        try {
            Map<String, Object> breakdown = examinationService.getExaminationStatusBreakdown(tenantId);

            long scheduled = ((Number) breakdown.getOrDefault("scheduled", 0L)).longValue();
            long liveInProgress = ((Number) breakdown.getOrDefault("liveInProgress", 0L)).longValue();
            long completed = ((Number) breakdown.getOrDefault("completed", 0L)).longValue();
            long cancelled = ((Number) breakdown.getOrDefault("cancelled", 0L)).longValue();

            ExamBreakdownGrpcResponse response = ExamBreakdownGrpcResponse.newBuilder()
                    .setScheduled(scheduled)
                    .setLiveInProgress(liveInProgress)
                    .setCompleted(completed)
                    .setCancelled(cancelled)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get examination status breakdown via gRPC", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get examination breakdown: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
