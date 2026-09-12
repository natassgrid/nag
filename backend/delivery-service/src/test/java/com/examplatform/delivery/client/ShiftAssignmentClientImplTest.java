/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.ShiftAssignment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftAssignmentClientImplTest {

    @Test
    @DisplayName("Fallback returns valid ShiftAssignment when gRPC server is unreachable")
    void fallbackWhenGrpcServerUnreachable() {
        // Port 1 is unreachable, should gracefully fallback
        ShiftAssignmentClientImpl client = new ShiftAssignmentClientImpl("localhost", 1, 500);

        UUID candidateId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        String tenantId = "tenant-001";

        ShiftAssignment assignment = client.getShiftAssignment(candidateId, examId, shiftId, tenantId);

        assertThat(assignment).isNotNull();
        assertThat(assignment.getPaperId()).isNotNull();
        assertThat(assignment.getDurationMinutes()).isEqualTo(180);
        assertThat(assignment.getExtraTimeMinutes()).isEqualTo(0);
    }
}
