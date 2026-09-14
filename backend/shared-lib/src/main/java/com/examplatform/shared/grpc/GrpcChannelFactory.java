/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.shared.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Factory and cache for gRPC ManagedChannels across microservices.
 */
@Slf4j
public final class GrpcChannelFactory {

    private static final ConcurrentMap<String, ManagedChannel> CHANNELS = new ConcurrentHashMap<>();

    private GrpcChannelFactory() {
    }

    /**
     * Get or create a plaintext ManagedChannel for the target host:port.
     */
    public static ManagedChannel getChannel(String host, int port) {
        String key = host + ":" + port;
        return CHANNELS.computeIfAbsent(key, k -> {
            log.info("Creating gRPC ManagedChannel for {}", key);
            return ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build();
        });
    }

    /**
     * Get or create a plaintext ManagedChannel for target string (e.g. "localhost:9085").
     */
    public static ManagedChannel getChannel(String target) {
        String[] parts = target.split(":", 2);
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;
        return getChannel(host, port);
    }

    /**
     * Shutdown all cached channels.
     */
    public static void shutdownAll() {
        CHANNELS.forEach((key, channel) -> {
            try {
                channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
        CHANNELS.clear();
    }
}
