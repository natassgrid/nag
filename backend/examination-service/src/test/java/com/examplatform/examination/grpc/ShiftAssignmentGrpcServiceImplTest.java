/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.grpc;

import com.examplatform.examination.domain.ExamShift;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.repository.ExamApplicationRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentGrpcServiceImplTest {

    @Mock
    private ExamShiftRepository examShiftRepository;

    @Mock
    private ExaminationRepository examinationRepository;

    @Mock
    private ExamApplicationRepository examApplicationRepository;

    @Mock
    private StreamObserver<ShiftAssignmentGrpcResponse> responseObserver;

    @InjectMocks
    private ShiftAssignmentGrpcServiceImpl grpcService;

    private UUID shiftId;
    private UUID examId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        shiftId = UUID.randomUUID();
        examId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getShiftAssignment returns correct shift details when shift exists")
    void getShiftAssignmentSuccess() {
        ExamShift shift = ExamShift.builder()
                .durationMinutes(120)
                .build();
        when(examShiftRepository.findById(eq(shiftId))).thenReturn(Optional.of(shift));

        ShiftAssignmentGrpcRequest request = ShiftAssignmentGrpcRequest.newBuilder()
                .setCandidateId(candidateId.toString())
                .setExamId(examId.toString())
                .setShiftId(shiftId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getShiftAssignment(request, responseObserver);

        ArgumentCaptor<ShiftAssignmentGrpcResponse> captor = ArgumentCaptor.forClass(ShiftAssignmentGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        ShiftAssignmentGrpcResponse response = captor.getValue();
        assertThat(response.getDurationMinutes()).isEqualTo(120);
        assertThat(response.getIsAssigned()).isTrue();
        assertThat(response.getPaperId()).isEqualTo(examId.toString());
    }

    @Test
    @DisplayName("getShiftAssignment falls back to examination duration when shift not found")
    void getShiftAssignmentFallbackToExam() {
        when(examShiftRepository.findById(eq(shiftId))).thenReturn(Optional.empty());
        Examination exam = Examination.builder()
                .durationMinutes(150)
                .build();
        when(examinationRepository.findById(eq(examId))).thenReturn(Optional.of(exam));

        ShiftAssignmentGrpcRequest request = ShiftAssignmentGrpcRequest.newBuilder()
                .setCandidateId(candidateId.toString())
                .setExamId(examId.toString())
                .setShiftId(shiftId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getShiftAssignment(request, responseObserver);

        ArgumentCaptor<ShiftAssignmentGrpcResponse> captor = ArgumentCaptor.forClass(ShiftAssignmentGrpcResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        ShiftAssignmentGrpcResponse response = captor.getValue();
        assertThat(response.getDurationMinutes()).isEqualTo(150);
        assertThat(response.getIsAssigned()).isTrue();
    }
}
