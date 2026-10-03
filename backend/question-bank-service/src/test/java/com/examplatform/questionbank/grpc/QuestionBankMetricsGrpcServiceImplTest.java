/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.shared.grpc.QuestionBankMetricsGrpcRequest;
import com.examplatform.shared.grpc.QuestionBankMetricsGrpcResponse;
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
@DisplayName("QuestionBankMetricsGrpcServiceImpl Unit Tests")
class QuestionBankMetricsGrpcServiceImplTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private StreamObserver<QuestionBankMetricsGrpcResponse> responseObserver;

    @InjectMocks
    private QuestionBankMetricsGrpcServiceImpl grpcService;

    @Test
    @DisplayName("getQuestionBankMetrics returns metrics from QuestionService")
    void getQuestionBankMetricsSuccess() {
        when(questionService.getQuestionBankMetrics(eq("test-tenant"))).thenReturn(Map.of(
                "total", 50000L,
                "draft", 400L,
                "submitted", 150L,
                "approved", 49200L,
                "rejected", 250L
        ));

        QuestionBankMetricsGrpcRequest request = QuestionBankMetricsGrpcRequest.newBuilder()
                .setTenantId("test-tenant")
                .build();

        grpcService.getQuestionBankMetrics(request, responseObserver);

        ArgumentCaptor<QuestionBankMetricsGrpcResponse> captor = ArgumentCaptor.forClass(QuestionBankMetricsGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        QuestionBankMetricsGrpcResponse response = captor.getValue();
        assertThat(response.getTotal()).isEqualTo(50000L);
        assertThat(response.getDraft()).isEqualTo(400L);
        assertThat(response.getSubmitted()).isEqualTo(150L);
        assertThat(response.getApproved()).isEqualTo(49200L);
        assertThat(response.getRejected()).isEqualTo(250L);
    }
}
