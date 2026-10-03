/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.audit.grpc;

import com.examplatform.audit.service.AuditQueryService;
import com.examplatform.shared.grpc.AuditLedgerGrpcRequest;
import com.examplatform.shared.grpc.AuditLedgerGrpcResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLedgerGrpcServiceImpl Unit Tests")
class AuditLedgerGrpcServiceImplTest {

    @Mock
    private AuditQueryService auditQueryService;

    @Mock
    private StreamObserver<AuditLedgerGrpcResponse> responseObserver;

    @InjectMocks
    private AuditLedgerGrpcServiceImpl grpcService;

    @Test
    @DisplayName("getRecentLedgerEvents returns mapped audit events")
    void getRecentLedgerEventsSuccess() {
        when(auditQueryService.getRecentLedgerEvents(eq("test-tenant"), eq(3))).thenReturn(List.of(
                Map.of(
                        "id", "SEC-101",
                        "action", "EXAM_PUBLISHED",
                        "entityType", "EXAMINATION",
                        "performedBy", "admin@dpi.gov.in",
                        "timestamp", Instant.now().toString(),
                        "status", "SUCCESS"
                )
        ));

        AuditLedgerGrpcRequest request = AuditLedgerGrpcRequest.newBuilder()
                .setTenantId("test-tenant")
                .setLimit(3)
                .build();

        grpcService.getRecentLedgerEvents(request, responseObserver);

        ArgumentCaptor<AuditLedgerGrpcResponse> captor = ArgumentCaptor.forClass(AuditLedgerGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        AuditLedgerGrpcResponse response = captor.getValue();
        assertThat(response.getEventsCount()).isEqualTo(1);
        assertThat(response.getEvents(0).getId()).isEqualTo("SEC-101");
        assertThat(response.getEvents(0).getAction()).isEqualTo("EXAM_PUBLISHED");
    }
}
