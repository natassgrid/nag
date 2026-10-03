/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.evaluation.grpc;

import com.examplatform.evaluation.service.ManualEvaluationService;
import com.examplatform.shared.grpc.EvaluationMetricsGrpcRequest;
import com.examplatform.shared.grpc.EvaluationMetricsGrpcResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationMetricsGrpcServiceImpl Unit Tests")
class EvaluationMetricsGrpcServiceImplTest {

    @Mock
    private ManualEvaluationService manualEvaluationService;

    @Mock
    private StreamObserver<EvaluationMetricsGrpcResponse> responseObserver;

    @InjectMocks
    private EvaluationMetricsGrpcServiceImpl grpcService;

    @Test
    @DisplayName("getEvaluationQueueMetrics returns queue statistics")
    void getEvaluationQueueMetricsSuccess() {
        when(manualEvaluationService.getEvaluationQueueMetrics(eq("test-tenant"))).thenReturn(Map.of(
                "pending", 500L,
                "inProgress", 120L,
                "completed", 15000L,
                "flagged", 25L
        ));

        EvaluationMetricsGrpcRequest request = EvaluationMetricsGrpcRequest.newBuilder()
                .setTenantId("test-tenant")
                .build();

        grpcService.getEvaluationQueueMetrics(request, responseObserver);

        ArgumentCaptor<EvaluationMetricsGrpcResponse> captor = ArgumentCaptor.forClass(EvaluationMetricsGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        EvaluationMetricsGrpcResponse response = captor.getValue();
        assertThat(response.getPending()).isEqualTo(500L);
        assertThat(response.getInProgress()).isEqualTo(120L);
        assertThat(response.getCompleted()).isEqualTo(15000L);
        assertThat(response.getFlagged()).isEqualTo(25L);
    }
}
