/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU practical General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.ShiftAssignment;
import com.examplatform.examination.grpc.ShiftAssignmentGrpcRequest;
import com.examplatform.examination.grpc.ShiftAssignmentGrpcResponse;
import com.examplatform.examination.grpc.ShiftAssignmentGrpcServiceGrpc;
import com.examplatform.shared.grpc.GrpcChannelFactory;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client implementation of ShiftAssignmentClient communicating with examination-service.
 * Includes resilience fallback when the examination-service gRPC server is unreachable.
 */
@Slf4j
@Component
public class ShiftAssignmentClientImpl implements ShiftAssignmentClient {

    private final String host;
    private final int port;
    private final long timeoutMs;

    public ShiftAssignmentClientImpl(
            @Value("${grpc.client.examination.host:localhost}") String host,
            @Value("${grpc.client.examination.port:9085}") int port,
            @Value("${grpc.client.examination.timeout-ms:5000}") long timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ShiftAssignment getShiftAssignment(UUID candidateId, UUID examId, UUID shiftId, String tenantId) {
        log.info("Fetching shift assignment via gRPC from {}:{} - candidate={}, exam={}, shift={}, tenant={}",
                host, port, candidateId, examId, shiftId, tenantId);

        try {
            ManagedChannel channel = GrpcChannelFactory.getChannel(host, port);
            ShiftAssignmentGrpcServiceGrpc.ShiftAssignmentGrpcServiceBlockingStub stub =
                    ShiftAssignmentGrpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            ShiftAssignmentGrpcRequest request = ShiftAssignmentGrpcRequest.newBuilder()
                    .setCandidateId(candidateId != null ? candidateId.toString() : "")
                    .setExamId(examId != null ? examId.toString() : "")
                    .setShiftId(shiftId != null ? shiftId.toString() : "")
                    .setTenantId(tenantId != null ? tenantId : "")
                    .build();

            ShiftAssignmentGrpcResponse response = stub.getShiftAssignment(request);

            UUID paperId = null;
            if (response.getPaperId() != null && !response.getPaperId().isBlank()) {
                try {
                    paperId = UUID.fromString(response.getPaperId());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid paperId UUID format from gRPC: {}", response.getPaperId());
                }
            }

            return ShiftAssignment.builder()
                    .paperId(paperId != null ? paperId : UUID.randomUUID())
                    .durationMinutes(response.getDurationMinutes() > 0 ? response.getDurationMinutes() : 180)
                    .extraTimeMinutes(response.getExtraTimeMinutes())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to get shift assignment via gRPC from {}:{} ({}). Using fallback.",
                    host, port, e.getMessage());
            return ShiftAssignment.builder()
                    .paperId(UUID.randomUUID())
                    .durationMinutes(180)
                    .extraTimeMinutes(0)
                    .build();
        }
    }
}
