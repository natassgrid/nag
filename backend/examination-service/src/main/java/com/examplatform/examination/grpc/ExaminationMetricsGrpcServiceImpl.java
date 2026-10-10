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
import java.util.Optional;

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

            ExamBreakdownGrpcResponse.Builder builder = ExamBreakdownGrpcResponse.newBuilder();
            Optional.ofNullable((Number) breakdown.get("scheduled")).ifPresent(n -> builder.setScheduled(n.longValue()));
            Optional.ofNullable((Number) breakdown.get("liveInProgress")).ifPresent(n -> builder.setLiveInProgress(n.longValue()));
            Optional.ofNullable((Number) breakdown.get("completed")).ifPresent(n -> builder.setCompleted(n.longValue()));
            Optional.ofNullable((Number) breakdown.get("cancelled")).ifPresent(n -> builder.setCancelled(n.longValue()));

            responseObserver.onNext(builder.build());
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
