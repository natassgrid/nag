/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.candidate.grpc;

import com.examplatform.candidate.domain.CandidateProfile;
import com.examplatform.candidate.repository.CandidateProfileRepository;
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
class CandidateProfileGrpcServiceImplTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private StreamObserver<CandidateExtensionGrpcResponse> extensionObserver;

    @Mock
    private StreamObserver<CandidateProfileGrpcResponse> profileObserver;

    @InjectMocks
    private CandidateProfileGrpcServiceImpl grpcService;

    private UUID candidateId;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getCandidateExtension returns extra time for PwD candidate")
    void getCandidateExtensionForPwd() {
        CandidateProfile profile = CandidateProfile.builder()
                .reservationCategory("PWD_VISUAL")
                .build();
        when(candidateProfileRepository.findById(eq(candidateId))).thenReturn(Optional.of(profile));

        CandidateExtensionGrpcRequest request = CandidateExtensionGrpcRequest.newBuilder()
                .setCandidateId(candidateId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getCandidateExtension(request, extensionObserver);

        ArgumentCaptor<CandidateExtensionGrpcResponse> captor = ArgumentCaptor.forClass(CandidateExtensionGrpcResponse.class);
        verify(extensionObserver).onNext(captor.capture());
        verify(extensionObserver).onCompleted();

        CandidateExtensionGrpcResponse response = captor.getValue();
        assertThat(response.getHasExtension()).isTrue();
        assertThat(response.getExtraTimeMinutes()).isEqualTo(60);
        assertThat(response.getScribeRequired()).isTrue();
    }

    @Test
    @DisplayName("getCandidateExtension returns false for regular candidate")
    void getCandidateExtensionForRegular() {
        CandidateProfile profile = CandidateProfile.builder()
                .reservationCategory("GEN")
                .build();
        when(candidateProfileRepository.findById(eq(candidateId))).thenReturn(Optional.of(profile));

        CandidateExtensionGrpcRequest request = CandidateExtensionGrpcRequest.newBuilder()
                .setCandidateId(candidateId.toString())
                .setTenantId("tenant-1")
                .build();

        grpcService.getCandidateExtension(request, extensionObserver);

        ArgumentCaptor<CandidateExtensionGrpcResponse> captor = ArgumentCaptor.forClass(CandidateExtensionGrpcResponse.class);
        verify(extensionObserver).onNext(captor.capture());
        verify(extensionObserver).onCompleted();

        CandidateExtensionGrpcResponse response = captor.getValue();
        assertThat(response.getHasExtension()).isFalse();
        assertThat(response.getExtraTimeMinutes()).isEqualTo(0);
    }
}
