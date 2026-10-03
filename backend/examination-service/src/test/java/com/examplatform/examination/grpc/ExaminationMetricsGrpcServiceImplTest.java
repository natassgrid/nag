/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.grpc;

import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.grpc.ExamBreakdownGrpcRequest;
import com.examplatform.shared.grpc.ExamBreakdownGrpcResponse;
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
@DisplayName("ExaminationMetricsGrpcServiceImpl Unit Tests")
class ExaminationMetricsGrpcServiceImplTest {

    @Mock
    private ExaminationService examinationService;

    @Mock
    private StreamObserver<ExamBreakdownGrpcResponse> responseObserver;

    @InjectMocks
    private ExaminationMetricsGrpcServiceImpl grpcService;

    @Test
    @DisplayName("getExaminationStatusBreakdown returns live counts from ExaminationService")
    void getExaminationStatusBreakdownSuccess() {
        when(examinationService.getExaminationStatusBreakdown(eq("test-tenant"))).thenReturn(Map.of(
                "scheduled", 15L,
                "liveInProgress", 4L,
                "completed", 90L,
                "cancelled", 2L
        ));

        ExamBreakdownGrpcRequest request = ExamBreakdownGrpcRequest.newBuilder()
                .setTenantId("test-tenant")
                .build();

        grpcService.getExaminationStatusBreakdown(request, responseObserver);

        ArgumentCaptor<ExamBreakdownGrpcResponse> captor = ArgumentCaptor.forClass(ExamBreakdownGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        ExamBreakdownGrpcResponse response = captor.getValue();
        assertThat(response.getScheduled()).isEqualTo(15L);
        assertThat(response.getLiveInProgress()).isEqualTo(4L);
        assertThat(response.getCompleted()).isEqualTo(90L);
        assertThat(response.getCancelled()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getExaminationStatusBreakdown uses 'default' tenant when tenantId is omitted")
    void getExaminationStatusBreakdownDefaultTenant() {
        when(examinationService.getExaminationStatusBreakdown(eq("default"))).thenReturn(Map.of(
                "scheduled", 8L,
                "liveInProgress", 2L,
                "completed", 142L,
                "cancelled", 1L
        ));

        ExamBreakdownGrpcRequest request = ExamBreakdownGrpcRequest.newBuilder().build();

        grpcService.getExaminationStatusBreakdown(request, responseObserver);

        ArgumentCaptor<ExamBreakdownGrpcResponse> captor = ArgumentCaptor.forClass(ExamBreakdownGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        ExamBreakdownGrpcResponse response = captor.getValue();
        assertThat(response.getScheduled()).isEqualTo(8L);
        assertThat(response.getLiveInProgress()).isEqualTo(2L);
    }
}
