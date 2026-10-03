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

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.shared.grpc.QuestionBankMetricsGrpcRequest;
import com.examplatform.shared.grpc.QuestionBankMetricsGrpcResponse;
import com.examplatform.shared.grpc.QuestionBankMetricsGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * gRPC service implementation for Question Bank operational statistics and metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankMetricsGrpcServiceImpl extends QuestionBankMetricsGrpcServiceGrpc.QuestionBankMetricsGrpcServiceImplBase {

    private final QuestionService questionService;

    @Override
    public void getQuestionBankMetrics(QuestionBankMetricsGrpcRequest request,
                                       StreamObserver<QuestionBankMetricsGrpcResponse> responseObserver) {
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId()
                : "default";
        log.info("gRPC getQuestionBankMetrics request for tenant={}", tenantId);

        try {
            Map<String, Object> metrics = questionService.getQuestionBankMetrics(tenantId);

            long total = ((Number) metrics.getOrDefault("total", 0L)).longValue();
            long draft = ((Number) metrics.getOrDefault("draft", 0L)).longValue();
            long submitted = ((Number) metrics.getOrDefault("submitted", 0L)).longValue();
            long approved = ((Number) metrics.getOrDefault("approved", 0L)).longValue();
            long rejected = ((Number) metrics.getOrDefault("rejected", 0L)).longValue();

            QuestionBankMetricsGrpcResponse response = QuestionBankMetricsGrpcResponse.newBuilder()
                    .setTotal(total)
                    .setDraft(draft)
                    .setSubmitted(submitted)
                    .setApproved(approved)
                    .setRejected(rejected)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get question bank metrics via gRPC", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get question bank metrics: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
