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

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight Spring lifecycle manager for gRPC Server in microservices.
 * Automatically discovers any Spring-managed BindableService beans and registers them.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "grpc.server.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerRunner {

    private final int port;
    private final List<BindableService> services;
    private Server server;

    public GrpcServerRunner(
            @Value("${grpc.server.port:0}") int port,
            @Autowired(required = false) List<BindableService> services) {
        this.port = port;
        this.services = services != null ? services : List.of();
    }

    @PostConstruct
    public void start() throws IOException {
        if (port <= 0 || services.isEmpty()) {
            log.info("gRPC Server disabled or no BindableService beans found (port={})", port);
            return;
        }

        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        for (BindableService service : services) {
            builder.addService(service);
            log.info("Registered gRPC service: {}", service.getClass().getSimpleName());
        }

        this.server = builder.build().start();
        log.info("gRPC Server started on port {}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Shutting down gRPC Server on port {}...", port);
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("gRPC Server shutdown complete.");
        }
    }

    public int getPort() {
        return server != null ? server.getPort() : port;
    }

    public boolean isRunning() {
        return server != null && !server.isShutdown() && !server.isTerminated();
    }
}
