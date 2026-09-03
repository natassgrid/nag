/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.delivery.service;

import com.examplatform.delivery.client.ShiftAssignmentClient;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.dto.ShiftAssignment;
import com.examplatform.delivery.exception.ConcurrentSessionException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.examplatform.shared.config.DynamicConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles exam session startup: enforces single concurrent session,
 * decrypts the shift paper package via Vault (in-memory only), and
 * serves the first question to the candidate within the 500ms SLA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionStartService {

    private final ExamSessionRepository examSessionRepository;
    private final ShiftAssignmentClient shiftAssignmentClient;
    private final DisabilityExtensionService disabilityExtensionService;
    private final VaultCryptoService vaultCryptoService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String TOPIC_SESSION_EVENTS = "exam.session.events";

    /**
     * Start a new exam session for the given candidate.
     *
     * @param request     the session start request containing exam/shift details
     * @param candidateId the authenticated candidate's ID (from JWT sub claim)
     * @param tenantId    the tenant identifier (from JWT or header)
     * @return the session start response including decrypted first question
     */
    public SessionStartResponse startSession(SessionStartRequest request, UUID candidateId, String tenantId) {
        // 1. Enforce single concurrent session
        List<ExamSession> existingSessions = examSessionRepository.findByCandidateIdAndTenantId(candidateId, tenantId);
        boolean hasActiveSession = existingSessions.stream()
                .anyMatch(s -> s.getStatus() == ExamSessionStatus.ACTIVE);
        if (hasActiveSession) {
            throw new ConcurrentSessionException(
                    "Candidate " + candidateId + " already has an active session in tenant " + tenantId);
        }

        // 2. Look up shift assignment from examination-service
        ShiftAssignment assignment = shiftAssignmentClient.getShiftAssignment(
                candidateId, request.getExamId(), request.getShiftId(), tenantId);

        // 3. Decrypt shift paper package using Vault Transit engine (in-memory only)
        String keyName = "shift-key-" + request.getShiftId();
        String decryptedPaper = vaultCryptoService.decrypt(keyName, assignment.getEncryptedPackageRef());

        // 4. Parse decrypted paper — extract first question and total count
        String firstQuestionContent = extractFirstQuestion(decryptedPaper);
        int totalQuestions = countQuestions(decryptedPaper);

        // 5. Apply disability extension time
        int disabilityExtension = disabilityExtensionService.getExtraTimeMinutes(candidateId, tenantId);

        // 6. Create exam session entity
        Instant now = Instant.now();
        int totalDuration = assignment.getDurationMinutes() + assignment.getExtraTimeMinutes() + disabilityExtension;
        Instant scheduledEnd = now.plus(Duration.ofMinutes(totalDuration));

        ExamSession session = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(candidateId)
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .paperId(assignment.getPaperId())
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .currentQuestionIndex(0)
                .languageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "en")
                .fullScreenExitCount(0)
                .build();
        session.setTenantId(tenantId);

        // 7. Save session to database
        ExamSession savedSession = examSessionRepository.save(session);

        // 8. Cache session in Redis for fast subsequent lookups
        String cacheKey = SESSION_CACHE_PREFIX + savedSession.getSessionId();
        redisTemplate.opsForValue().set(cacheKey, savedSession, Duration.ofHours(6));

        // 9. Publish SESSION_STARTED event to Kafka (fire-and-forget)
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SESSION_STARTED",
                    "sessionId", savedSession.getSessionId().toString(),
                    "candidateId", candidateId.toString(),
                    "examId", request.getExamId().toString(),
                    "shiftId", request.getShiftId(),
                    "startedAt", now.toString(),
                    "tenantId", tenantId
            );
            kafkaTemplate.send(TOPIC_SESSION_EVENTS, savedSession.getSessionId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish SESSION_STARTED event for session [{}]: {}",
                                    savedSession.getSessionId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing SESSION_STARTED event: {}", e.getMessage());
        }

        // 10. Read dynamic delivery parameters
        boolean kioskEnforced = dynamicConfigService.getBoolean("delivery.kiosk.mode.enforced", tenantId, true);
        int heartbeatSec = dynamicConfigService.getInt("delivery.telemetry.heartbeat.seconds", tenantId, 10);
        int autosaveSec = dynamicConfigService.getInt("delivery.autosave.interval.seconds", tenantId, 15);
        int maxDisconnectGraceSec = dynamicConfigService.getInt("delivery.max.disconnect.grace.seconds", tenantId, 180);
        boolean tamperEnabled = dynamicConfigService.getBoolean("delivery.tamper.detection.enabled", tenantId, true);

        // 11. Return response with first question and dynamic parameters
        return SessionStartResponse.builder()
                .sessionId(savedSession.getSessionId())
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .firstQuestionContent(firstQuestionContent)
                .totalQuestions(totalQuestions)
                .kioskModeEnforced(kioskEnforced)
                .heartbeatIntervalSeconds(heartbeatSec)
                .autosaveIntervalSeconds(autosaveSec)
                .maxDisconnectGraceSeconds(maxDisconnectGraceSec)
                .tamperDetectionEnabled(tamperEnabled)
                .build();
    }

    private String extractFirstQuestion(String decryptedPaper) {
        try {
            JsonNode root = objectMapper.readTree(decryptedPaper);
            JsonNode questions = root.path("questions");
            if (questions.isArray() && !questions.isEmpty()) {
                return objectMapper.writeValueAsString(questions.get(0));
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse paper JSON to extract first question: {}", e.getMessage());
        }
        return decryptedPaper;
    }

    private int countQuestions(String decryptedPaper) {
        try {
            JsonNode root = objectMapper.readTree(decryptedPaper);
            JsonNode questions = root.path("questions");
            if (questions.isArray()) {
                return questions.size();
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse paper JSON to count questions: {}", e.getMessage());
        }
        return 0;
    }
}
