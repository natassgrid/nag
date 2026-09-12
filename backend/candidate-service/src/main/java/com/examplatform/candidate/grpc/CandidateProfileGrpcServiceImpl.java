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

package com.examplatform.candidate.grpc;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.repository.CandidateProfileRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateProfileGrpcServiceImpl extends CandidateProfileGrpcServiceGrpc.CandidateProfileGrpcServiceImplBase {

    private final CandidateProfileRepository candidateProfileRepository;

    @Override
    public void getCandidateExtension(CandidateExtensionGrpcRequest request,
                                      StreamObserver<CandidateExtensionGrpcResponse> responseObserver) {
        log.info("gRPC getCandidateExtension: candidate={}, tenant={}", request.getCandidateId(), request.getTenantId());

        try {
            UUID candidateId = !request.getCandidateId().isBlank() ? UUID.fromString(request.getCandidateId()) : null;
            Optional<CandidateProfile> profileOpt = Optional.empty();

            if (candidateId != null) {
                profileOpt = candidateProfileRepository.findById(candidateId);
                if (profileOpt.isEmpty()) {
                    profileOpt = candidateProfileRepository.findByUserIdAndTenantId(candidateId, request.getTenantId());
                }
            }

            boolean hasExtension = false;
            int extraTimeMinutes = 0;
            boolean scribeRequired = false;
            String specialAccommodations = "";

            if (profileOpt.isPresent()) {
                CandidateProfile profile = profileOpt.get();
                String cat = profile.getReservationCategory() != null ? profile.getReservationCategory().toUpperCase() : "";
                String genCat = profile.getCategory() != null ? profile.getCategory().toUpperCase() : "";
                if (cat.contains("PWD") || genCat.contains("PWD") || cat.contains("DISABILITY")) {
                    hasExtension = true;
                    extraTimeMinutes = 60; // Standard 20 mins per hour for 3h exam
                    scribeRequired = true;
                    specialAccommodations = "PwD Extra Time & Scribe Approved";
                }
            }

            CandidateExtensionGrpcResponse response = CandidateExtensionGrpcResponse.newBuilder()
                    .setHasExtension(hasExtension)
                    .setCandidateId(request.getCandidateId())
                    .setExtraTimeMinutes(extraTimeMinutes)
                    .setScribeRequired(scribeRequired)
                    .setSpecialAccommodations(specialAccommodations)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing getCandidateExtension gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to retrieve candidate extension: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getCandidateProfile(CandidateProfileGrpcRequest request,
                                    StreamObserver<CandidateProfileGrpcResponse> responseObserver) {
        log.info("gRPC getCandidateProfile: candidate={}, tenant={}", request.getCandidateId(), request.getTenantId());

        try {
            UUID candidateId = !request.getCandidateId().isBlank() ? UUID.fromString(request.getCandidateId()) : null;
            Optional<CandidateProfile> profileOpt = Optional.empty();

            if (candidateId != null) {
                profileOpt = candidateProfileRepository.findById(candidateId);
                if (profileOpt.isEmpty()) {
                    profileOpt = candidateProfileRepository.findByUserIdAndTenantId(candidateId, request.getTenantId());
                }
            }

            CandidateProfileGrpcResponse.Builder builder = CandidateProfileGrpcResponse.newBuilder()
                    .setCandidateId(request.getCandidateId());

            if (profileOpt.isPresent()) {
                CandidateProfile profile = profileOpt.get();
                if (profile.getFullName() != null) builder.setFirstName(profile.getFullName());
                if (profile.getEmail() != null) builder.setEmail(profile.getEmail());
                if (profile.getMobile() != null) builder.setPhone(profile.getMobile());
                if (profile.getDateOfBirth() != null) builder.setDateOfBirth(profile.getDateOfBirth());
                if (profile.getGender() != null) builder.setGender(profile.getGender());
                if (profile.getReservationCategory() != null) builder.setCategory(profile.getReservationCategory());
                boolean isPwd = (profile.getReservationCategory() != null && profile.getReservationCategory().toUpperCase().contains("PWD"));
                builder.setIsPwd(isPwd);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing getCandidateProfile gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to retrieve candidate profile: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
