package com.examplatform.result.service;

import com.examplatform.result.client.DigiLockerClient;
import com.examplatform.result.domain.Result;
import com.examplatform.result.repository.ResultRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResultPublicationService.
 * Validates: Requirements 13.3, 13.5, 13.6, 13.8
 */
@ExtendWith(MockitoExtension.class)
class ResultPublicationServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private DigiLockerClient digiLockerClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ResultPublicationService publicationService;

    private UUID candidateId;
    private UUID examId;
    private String tenantId;
    private Result sampleResult;

    @BeforeEach
    void setUp() {
        publicationService = new ResultPublicationService(resultRepository, digiLockerClient, kafkaTemplate);

        candidateId = UUID.randomUUID();
        examId = UUID.randomUUID();
        tenantId = "tenant-test";

        sampleResult = Result.builder()
                .candidateId(candidateId)
                .examId(examId)
                .totalScore(BigDecimal.valueOf(85.50))
                .overallRank(3)
                .overallPercentile(BigDecimal.valueOf(92.500))
                .scorecardPdfRef("s3://bucket/scorecard-" + candidateId + ".pdf")
                .digiLockerPushed(false)
                .build();
        sampleResult.setTenantId(tenantId);
    }

    @Test
    @DisplayName("publishResult sends notification event to Kafka")
    void publishResult_sendsNotificationEvent() {
        ReflectionTestUtils.setField(publicationService, "digiLockerEnabled", false);

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId))
                .thenReturn(Optional.of(sampleResult));
        when(resultRepository.save(any(Result.class))).thenReturn(sampleResult);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publicationService.publishResult(candidateId, examId, tenantId);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("exam.notifications.outbound"), eq(candidateId.toString()), eventCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        assertThat(event.get("eventType")).isEqualTo("RESULT_PUBLISHED_NOTIFICATION");
        assertThat(event.get("candidateId")).isEqualTo(candidateId.toString());
        assertThat(event.get("examId")).isEqualTo(examId.toString());
    }

    @Test
    @DisplayName("publishResult calls DigiLocker when enabled and PDF exists")
    void publishResult_callsDigiLocker_whenEnabled() {
        ReflectionTestUtils.setField(publicationService, "digiLockerEnabled", true);

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId))
                .thenReturn(Optional.of(sampleResult));
        when(resultRepository.save(any(Result.class))).thenReturn(sampleResult);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Result result = publicationService.publishResult(candidateId, examId, tenantId);

        verify(digiLockerClient).pushScorecard(candidateId, sampleResult.getScorecardPdfRef());
        assertThat(result.getDigiLockerPushed()).isTrue();
    }

    @Test
    @DisplayName("publishResult does not call DigiLocker when disabled")
    void publishResult_doesNotCallDigiLocker_whenDisabled() {
        ReflectionTestUtils.setField(publicationService, "digiLockerEnabled", false);

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId))
                .thenReturn(Optional.of(sampleResult));
        when(resultRepository.save(any(Result.class))).thenReturn(sampleResult);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publicationService.publishResult(candidateId, examId, tenantId);

        verify(digiLockerClient, never()).pushScorecard(any(), anyString());
    }

    @Test
    @DisplayName("publishResult throws EntityNotFoundException when result not found")
    void publishResult_throwsException_whenNotFound() {
        ReflectionTestUtils.setField(publicationService, "digiLockerEnabled", false);

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.publishResult(candidateId, examId, tenantId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
