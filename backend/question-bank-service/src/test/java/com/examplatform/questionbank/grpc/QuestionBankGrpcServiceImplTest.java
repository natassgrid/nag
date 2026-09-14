/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.grpc;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankGrpcServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private StreamObserver<PaperQuestionsGrpcResponse> paperQuestionsObserver;

    @Mock
    private StreamObserver<BatchFindQuestionsGrpcResponse> batchObserver;

    private QuestionBankGrpcServiceImpl grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new QuestionBankGrpcServiceImpl(questionRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("getQuestionsForPaper returns questions from repository")
    void getQuestionsForPaperSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .content("What is the capital of India?")
                .questionType("MCQ")
                .difficulty("EASY")
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findByTenantId(eq("tenant-1"))).thenReturn(List.of(q));

        PaperQuestionsGrpcRequest request = PaperQuestionsGrpcRequest.newBuilder()
                .setPaperId(UUID.randomUUID().toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getQuestionsForPaper(request, paperQuestionsObserver);

        ArgumentCaptor<PaperQuestionsGrpcResponse> captor = ArgumentCaptor.forClass(PaperQuestionsGrpcResponse.class);
        verify(paperQuestionsObserver).onNext(captor.capture());
        verify(paperQuestionsObserver).onCompleted();

        PaperQuestionsGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getId()).isEqualTo(qId.toString());
        assertThat(response.getQuestions(0).getContent()).isEqualTo("What is the capital of India?");
    }

    @Test
    @DisplayName("batchFindQuestions returns matching questions")
    void batchFindQuestionsSuccess() {
        UUID qId = UUID.randomUUID();
        Question q = Question.builder()
                .content("Sample question")
                .build();
        ReflectionTestUtils.setField(q, "id", qId);

        when(questionRepository.findQuestionsByIdsIn(eq(List.of(qId)), eq("tenant-1"))).thenReturn(List.of(q));

        BatchFindQuestionsGrpcRequest request = BatchFindQuestionsGrpcRequest.newBuilder()
                .addQuestionIds(qId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.batchFindQuestions(request, batchObserver);

        ArgumentCaptor<BatchFindQuestionsGrpcResponse> captor = ArgumentCaptor.forClass(BatchFindQuestionsGrpcResponse.class);
        verify(batchObserver).onNext(captor.capture());
        verify(batchObserver).onCompleted();

        BatchFindQuestionsGrpcResponse response = captor.getValue();
        assertThat(response.getQuestionsCount()).isEqualTo(1);
        assertThat(response.getQuestions(0).getContent()).isEqualTo("Sample question");
    }
}
