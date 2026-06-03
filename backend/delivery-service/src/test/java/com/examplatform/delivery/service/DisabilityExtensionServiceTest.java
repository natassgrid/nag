package com.examplatform.delivery.service;

import com.examplatform.delivery.client.CandidateProfileClient;
import com.examplatform.delivery.dto.CandidateExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DisabilityExtensionService}.
 * Validates that disability-based time extensions are correctly retrieved
 * and applied to exam session durations.
 */
@ExtendWith(MockitoExtension.class)
class DisabilityExtensionServiceTest {

    @Mock
    private CandidateProfileClient candidateProfileClient;

    @InjectMocks
    private DisabilityExtensionService disabilityExtensionService;

    private UUID candidateId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        tenantId = "tenant-001";
    }

    @Test
    void getExtraTimeMinutes_withExtensionConfigured_returnsExtraTime() {
        CandidateExtension extension = CandidateExtension.builder()
                .extraTimeMinutes(30)
                .disabilityType("VISUAL_IMPAIRMENT")
                .build();
        when(candidateProfileClient.getExtension(candidateId, tenantId)).thenReturn(extension);

        int result = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        assertThat(result).isEqualTo(30);
    }

    @Test
    void getExtraTimeMinutes_withNoExtension_returnsZero() {
        when(candidateProfileClient.getExtension(candidateId, tenantId)).thenReturn(null);

        int result = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getExtraTimeMinutes_withZeroExtension_returnsZero() {
        CandidateExtension extension = CandidateExtension.builder()
                .extraTimeMinutes(0)
                .disabilityType(null)
                .build();
        when(candidateProfileClient.getExtension(candidateId, tenantId)).thenReturn(extension);

        int result = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getExtraTimeMinutes_withNegativeValue_returnsZero() {
        CandidateExtension extension = CandidateExtension.builder()
                .extraTimeMinutes(-10)
                .disabilityType("UNKNOWN")
                .build();
        when(candidateProfileClient.getExtension(candidateId, tenantId)).thenReturn(extension);

        int result = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getExtraTimeMinutes_whenClientThrowsException_returnsZero() {
        when(candidateProfileClient.getExtension(candidateId, tenantId))
                .thenThrow(new RuntimeException("Service unavailable"));

        int result = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        assertThat(result).isEqualTo(0);
    }
}
