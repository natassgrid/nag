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

package com.examplatform.candidate.grpc;

import com.examplatform.candidate.service.CandidateProfileService;
import com.examplatform.shared.grpc.CandidateMetricsGrpcRequest;
import com.examplatform.shared.grpc.CandidateMetricsGrpcResponse;
import com.examplatform.shared.grpc.CandidateMetricsGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * gRPC service implementation for Candidate operational metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateMetricsGrpcServiceImpl extends CandidateMetricsGrpcServiceGrpc.CandidateMetricsGrpcServiceImplBase {

    private final CandidateProfileService candidateProfileService;

    @Override
    public void getCandidateMetrics(CandidateMetricsGrpcRequest request,
                                    StreamObserver<CandidateMetricsGrpcResponse> responseObserver) {
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId()
                : "default";
        log.info("gRPC getCandidateMetrics request for tenant={}", tenantId);

        try {
            Map<String, Object> metrics = candidateProfileService.getCandidateMetrics(tenantId);
            long total = ((Number) metrics.getOrDefault("totalRegisteredCandidates", 0L)).longValue();
            long active = ((Number) metrics.getOrDefault("activeCandidates", 0L)).longValue();

            CandidateMetricsGrpcResponse response = CandidateMetricsGrpcResponse.newBuilder()
                    .setTotalRegisteredCandidates(total)
                    .setActiveCandidates(active)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get candidate metrics via gRPC", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get candidate metrics: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
