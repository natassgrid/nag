/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.CandidateExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateProfileClientImplTest {

    @Test
    @DisplayName("Fallback returns null when gRPC server is unreachable")
    void fallbackWhenGrpcServerUnreachable() {
        // Port 1 is unreachable, should gracefully fallback to null
        CandidateProfileClientImpl client = new CandidateProfileClientImpl("localhost", 1, 500);

        UUID candidateId = UUID.randomUUID();
        String tenantId = "tenant-001";

        CandidateExtension extension = client.getExtension(candidateId, tenantId);

        assertThat(extension).isNull();
    }
}
