/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.examination.grpc;

import com.examplatform.examination.domain.ExamShift;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.repository.ExamApplicationRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import com.examplatform.examination.repository.ExaminationRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftAssignmentGrpcServiceImpl extends ShiftAssignmentGrpcServiceGrpc.ShiftAssignmentGrpcServiceImplBase {

    private final ExamShiftRepository examShiftRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamApplicationRepository examApplicationRepository;

    @Override
    public void getShiftAssignment(ShiftAssignmentGrpcRequest request,
                                   StreamObserver<ShiftAssignmentGrpcResponse> responseObserver) {
        log.info("gRPC getShiftAssignment: candidate={}, exam={}, shift={}, tenant={}",
                request.getCandidateId(), request.getExamId(), request.getShiftId(), request.getTenantId());

        try {
            UUID shiftId = !request.getShiftId().isBlank() ? UUID.fromString(request.getShiftId()) : null;
            UUID examId = !request.getExamId().isBlank() ? UUID.fromString(request.getExamId()) : null;

            int durationMinutes = 0;
            int extraTimeMinutes = 0;

            if (shiftId != null) {
                Optional<ExamShift> shiftOpt = examShiftRepository.findById(shiftId);
                if (shiftOpt.isPresent()) {
                    durationMinutes = shiftOpt.get().getDurationMinutes();
                }
            }

            if (examId != null && durationMinutes <= 0) {
                Optional<Examination> examOpt = examinationRepository.findById(examId);
                if (examOpt.isPresent()) {
                    durationMinutes = examOpt.get().getDurationMinutes();
                }
            }

            if (durationMinutes <= 0) {
                durationMinutes = 180;
            }

            String paperId = (examId != null) ? examId.toString() : UUID.randomUUID().toString();

            ShiftAssignmentGrpcResponse response = ShiftAssignmentGrpcResponse.newBuilder()
                    .setAssignmentId(UUID.randomUUID().toString())
                    .setPaperId(paperId)
                    .setExamShiftId(request.getShiftId())
                    .setDurationMinutes(durationMinutes)
                    .setExtraTimeMinutes(extraTimeMinutes)
                    .setIsAssigned(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing getShiftAssignment gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to retrieve shift assignment: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
