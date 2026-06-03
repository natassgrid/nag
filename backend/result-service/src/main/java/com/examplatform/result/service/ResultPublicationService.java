package com.examplatform.result.service;

import com.examplatform.result.client.DigiLockerClient;
import com.examplatform.result.domain.Result;
import com.examplatform.result.repository.ResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for publishing examination results to candidates.
 * Marks results as published, pushes scorecards to DigiLocker when enabled,
 * and sends notification events.
 *
 * Validates: Requirements 13.3, 13.5, 13.6, 13.8
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResultPublicationService {

    private static final String NOTIFICATION_TOPIC = "exam.notifications.outbound";

    private final ResultRepository resultRepository;
    private final DigiLockerClient digiLockerClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${result.digilocker.enabled:false}")
    private boolean digiLockerEnabled;

    /**
     * Publishes the result for a candidate in an exam:
     * 1. Marks the result as published
     * 2. Pushes scorecard to DigiLocker (if integration is enabled)
     * 3. Publishes a notification event to inform the candidate
     *
     * @param candidateId the candidate's UUID
     * @param examId      the exam UUID
     * @param tenantId    the tenant identifier
     * @return the updated Result entity
     */
    public Result publishResult(UUID candidateId, UUID examId, String tenantId) {
        log.info("Publishing result for candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);

        Result result = resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Result not found for candidate=%s, exam=%s, tenant=%s",
                                candidateId, examId, tenantId)));

        // Push scorecard to DigiLocker when enabled and PDF is available
        if (digiLockerEnabled && result.getScorecardPdfRef() != null) {
            try {
                digiLockerClient.pushScorecard(candidateId, result.getScorecardPdfRef());
                result.setDigiLockerPushed(true);
                log.info("Scorecard pushed to DigiLocker for candidate={}", candidateId);
            } catch (Exception e) {
                log.error("Failed to push scorecard to DigiLocker for candidate={}: {}",
                        candidateId, e.getMessage());
                // Don't block publication on DigiLocker failure
            }
        }

        resultRepository.save(result);

        // Publish notification event
        publishNotificationEvent(candidateId, examId, tenantId);

        log.info("Result published successfully for candidate={}, exam={}", candidateId, examId);
        return result;
    }

    /**
     * Publishes a notification event to Kafka to inform the candidate
     * that their result is available.
     */
    private void publishNotificationEvent(UUID candidateId, UUID examId, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "RESULT_PUBLISHED_NOTIFICATION",
                    "candidateId", candidateId.toString(),
                    "examId", examId.toString(),
                    "tenantId", tenantId,
                    "message", "Your examination result is now available.",
                    "occurredAt", Instant.now().toString()
            );
            kafkaTemplate.send(NOTIFICATION_TOPIC, candidateId.toString(), event)
                    .whenComplete((sendResult, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish notification for candidate={}: {}",
                                    candidateId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing notification event: {}", e.getMessage());
        }
    }
}
