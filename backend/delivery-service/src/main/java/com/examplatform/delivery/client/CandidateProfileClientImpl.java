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

package com.examplatform.delivery.client;

import com.examplatform.candidate.grpc.CandidateExtensionGrpcRequest;
import com.examplatform.candidate.grpc.CandidateExtensionGrpcResponse;
import com.examplatform.candidate.grpc.CandidateProfileGrpcServiceGrpc;
import com.examplatform.delivery.dto.CandidateExtension;
import com.examplatform.shared.grpc.GrpcChannelFactory;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client implementation of CandidateProfileClient communicating with candidate-service.
 * Retrieves candidate accommodation and extension information with graceful fallback.
 */
@Slf4j
@Component
public class CandidateProfileClientImpl implements CandidateProfileClient {

    private final String host;
    private final int port;
    private final long timeoutMs;

    public CandidateProfileClientImpl(
            @Value("${grpc.client.candidate.host:localhost}") String host,
            @Value("${grpc.client.candidate.port:9082}") int port,
            @Value("${grpc.client.candidate.timeout-ms:5000}") long timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public CandidateExtension getExtension(UUID candidateId, String tenantId) {
        log.info("Fetching candidate extension via gRPC from {}:{} - candidate={}, tenant={}",
                host, port, candidateId, tenantId);

        try {
            ManagedChannel channel = GrpcChannelFactory.getChannel(host, port);
            CandidateProfileGrpcServiceGrpc.CandidateProfileGrpcServiceBlockingStub stub =
                    CandidateProfileGrpcServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            CandidateExtensionGrpcRequest request = CandidateExtensionGrpcRequest.newBuilder()
                    .setCandidateId(candidateId != null ? candidateId.toString() : "")
                    .setTenantId(tenantId != null ? tenantId : "")
                    .build();

            CandidateExtensionGrpcResponse response = stub.getCandidateExtension(request);

            if (response.getHasExtension()) {
                return CandidateExtension.builder()
                        .extraTimeMinutes(response.getExtraTimeMinutes())
                        .disabilityType(response.getSpecialAccommodations())
                        .build();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get candidate extension via gRPC from {}:{} ({}). Returning null fallback.",
                    host, port, e.getMessage());
            return null;
        }
    }
}
