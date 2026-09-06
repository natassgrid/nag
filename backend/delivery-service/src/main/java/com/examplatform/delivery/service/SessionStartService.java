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
import com.examplatform.delivery.dto.QuestionDeliveryDto;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles exam session startup and seamless resumption:
 * - Enforces single active concurrent session per candidate
 * - Seamlessly resumes an ongoing active session when the candidate reconnects
 * - Decrypts the shift paper package via Vault (in-memory only)
 * - Delivers questions with randomized options seeded per session
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
    private final ExamQuestionDeliveryService examQuestionDeliveryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String TOPIC_SESSION_EVENTS = "exam.session.events";

    private static final Map<String, String> STANDARD_EXAM_TITLES = Map.of(
            "e1000000-0000-0000-0000-000000000001", "Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026",
            "e2000000-0000-0000-0000-000000000002", "Union Public Service Commission Civil Services Examination (Prelims) 2026",
            "e3000000-0000-0000-0000-000000000003", "Railway Recruitment Board Non-Technical Popular Categories (RRB NTPC) 2026"
    );

    /**
     * Start a new exam session or seamlessly resume an active session for the candidate.
     *
     * @param request     the session start request containing exam/shift details
     * @param candidateId the authenticated candidate's ID (from JWT sub claim)
     * @param tenantId    the tenant identifier (from JWT or header)
     * @return the session start/resume response including questions with randomized options
     */
    public SessionStartResponse startSession(SessionStartRequest request, UUID candidateId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        Instant now = Instant.now();

        // 1. Check for existing active session for this candidate
        List<ExamSession> existingSessions = examSessionRepository.findByCandidateIdAndTenantId(candidateId, effectiveTenant);
        Optional<ExamSession> activeSessionOpt = existingSessions.stream()
                .filter(s -> s.getStatus() == ExamSessionStatus.ACTIVE)
                .findFirst();

        if (activeSessionOpt.isPresent()) {
            ExamSession activeSession = activeSessionOpt.get();

            // Check if active session has expired past its scheduledEndAt
            if (activeSession.getScheduledEndAt() != null && now.isAfter(activeSession.getScheduledEndAt())) {
                log.info("Active session [{}] for candidate [{}] expired at [{}]. Marking EXPIRED.",
                        activeSession.getSessionId(), candidateId, activeSession.getScheduledEndAt());
                activeSession.setStatus(ExamSessionStatus.EXPIRED);
                examSessionRepository.save(activeSession);
            } else if (activeSession.getExamId() != null && activeSession.getExamId().equals(request.getExamId())) {
                // RESUME FEATURE: Candidate reconnected or refreshed exam page
                log.info("Resuming active session [{}] for candidate [{}] on exam [{}]",
                        activeSession.getSessionId(), candidateId, request.getExamId());
                return resumeSession(activeSession, effectiveTenant);
            } else {
                // Active session belongs to a different exam
                throw new ConcurrentSessionException(
                        "Candidate " + candidateId + " already has an active session for exam " + activeSession.getExamId() + " in tenant " + effectiveTenant);
            }
        }

        // Resolve effective shiftId
        UUID shiftId = request.getShiftId() != null
                ? request.getShiftId()
                : UUID.nameUUIDFromBytes(("shift-" + request.getExamId()).getBytes(StandardCharsets.UTF_8));

        // 2. Look up shift assignment from examination-service
        ShiftAssignment assignment = shiftAssignmentClient.getShiftAssignment(
                candidateId, request.getExamId(), shiftId, effectiveTenant);

        // 3. Decrypt shift paper package using Vault Transit engine (in-memory only)
        String decryptedPaper = null;
        if (assignment != null && assignment.getEncryptedPackageRef() != null && !assignment.getEncryptedPackageRef().isBlank()) {
            try {
                String keyName = "shift-key-" + shiftId;
                decryptedPaper = vaultCryptoService.decrypt(keyName, assignment.getEncryptedPackageRef());
            } catch (Exception e) {
                log.warn("Vault decryption failed for shift paper key [shift-key-{}]: {}", shiftId, e.getMessage());
            }
        }

        UUID paperId = assignment != null && assignment.getPaperId() != null
                ? assignment.getPaperId()
                : UUID.nameUUIDFromBytes(("paper-" + request.getExamId()).getBytes(StandardCharsets.UTF_8));

        // 4. Resolve delivery questions
        List<QuestionDeliveryDto> baseQuestions = examQuestionDeliveryService != null
                ? examQuestionDeliveryService.getDeliveryQuestions(request.getExamId(), paperId, decryptedPaper, effectiveTenant)
                : List.of();

        UUID sessionId = UUID.randomUUID();

        // 5. Randomize options per session
        List<QuestionDeliveryDto> questions = (examQuestionDeliveryService != null && baseQuestions != null && !baseQuestions.isEmpty())
                ? examQuestionDeliveryService.randomizeOptions(baseQuestions, sessionId)
                : baseQuestions;

        String firstQuestionContent = extractFirstQuestion(decryptedPaper);
        if (firstQuestionContent == null && questions != null && !questions.isEmpty()) {
            try {
                firstQuestionContent = objectMapper.writeValueAsString(questions.get(0));
            } catch (JsonProcessingException ignored) {}
        }
        int totalQuestions = (questions != null && !questions.isEmpty()) ? questions.size() : countQuestions(decryptedPaper);

        // 6. Apply disability extension time
        int disabilityExtension = disabilityExtensionService.getExtraTimeMinutes(candidateId, effectiveTenant);

        // 7. Create exam session entity
        int baseDuration = assignment != null ? assignment.getDurationMinutes() : 60;
        int extraTime = assignment != null ? assignment.getExtraTimeMinutes() : 0;
        int totalDuration = baseDuration + extraTime + disabilityExtension;
        Instant scheduledEnd = now.plus(Duration.ofMinutes(totalDuration));

        ExamSession session = ExamSession.builder()
                .sessionId(sessionId)
                .candidateId(candidateId)
                .examId(request.getExamId())
                .shiftId(shiftId)
                .paperId(paperId)
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .currentQuestionIndex(0)
                .languageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "en")
                .fullScreenExitCount(0)
                .build();
        session.setTenantId(effectiveTenant);

        // 8. Save session to database
        ExamSession savedSession = examSessionRepository.save(session);

        // 9. Cache session in Redis for fast subsequent lookups
        String cacheKey = SESSION_CACHE_PREFIX + savedSession.getSessionId();
        try {
            redisTemplate.opsForValue().set(cacheKey, savedSession, Duration.ofHours(6));
        } catch (Exception e) {
            log.warn("Failed to cache session in Redis: {}", e.getMessage());
        }

        // 10. Publish SESSION_STARTED event to Kafka (fire-and-forget)
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SESSION_STARTED",
                    "sessionId", savedSession.getSessionId().toString(),
                    "candidateId", candidateId.toString(),
                    "examId", request.getExamId().toString(),
                    "shiftId", shiftId.toString(),
                    "startedAt", now.toString(),
                    "tenantId", effectiveTenant
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

        // 11. Read dynamic delivery parameters
        boolean kioskEnforced = dynamicConfigService.getBoolean("delivery.kiosk.mode.enforced", effectiveTenant, true);
        int heartbeatSec = dynamicConfigService.getInt("delivery.telemetry.heartbeat.seconds", effectiveTenant, 10);
        int autosaveSec = dynamicConfigService.getInt("delivery.autosave.interval.seconds", effectiveTenant, 15);
        int maxDisconnectGraceSec = dynamicConfigService.getInt("delivery.max.disconnect.grace.seconds", effectiveTenant, 180);
        boolean tamperEnabled = dynamicConfigService.getBoolean("delivery.tamper.detection.enabled", effectiveTenant, true);

        String examTitle = STANDARD_EXAM_TITLES.getOrDefault(
                request.getExamId().toString(),
                "Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026"
        );

        // 12. Return response with first question, full questions list and dynamic parameters
        return SessionStartResponse.builder()
                .sessionId(savedSession.getSessionId())
                .examId(request.getExamId())
                .examTitle(examTitle)
                .shiftId(shiftId)
                .candidateId(candidateId)
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .serverTime(now)
                .expiresAt(scheduledEnd)
                .durationSeconds(totalDuration * 60)
                .navigationMode("FLEXIBLE")
                .firstQuestionContent(firstQuestionContent)
                .totalQuestions(totalQuestions)
                .questions(questions)
                .kioskModeEnforced(kioskEnforced)
                .heartbeatIntervalSeconds(heartbeatSec)
                .autosaveIntervalSeconds(autosaveSec)
                .maxDisconnectGraceSeconds(maxDisconnectGraceSec)
                .tamperDetectionEnabled(tamperEnabled)
                .build();
    }

    /**
     * Resume an existing active session for a candidate.
     *
     * @param session  the active exam session to resume
     * @param tenantId the tenant identifier
     * @return the session start/resume response
     */
    public SessionStartResponse resumeSession(ExamSession session, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        Instant now = Instant.now();

        // 1. Look up shift assignment for paper decryption if available
        ShiftAssignment assignment = null;
        try {
            assignment = shiftAssignmentClient.getShiftAssignment(
                    session.getCandidateId(), session.getExamId(), session.getShiftId(), effectiveTenant);
        } catch (Exception e) {
            log.warn("Could not retrieve shift assignment on resume for session [{}]: {}", session.getSessionId(), e.getMessage());
        }

        String decryptedPaper = null;
        if (assignment != null && assignment.getEncryptedPackageRef() != null && !assignment.getEncryptedPackageRef().isBlank()) {
            try {
                String keyName = "shift-key-" + session.getShiftId();
                decryptedPaper = vaultCryptoService.decrypt(keyName, assignment.getEncryptedPackageRef());
            } catch (Exception e) {
                log.warn("Vault decryption failed on resume for shift-key-{}: {}", session.getShiftId(), e.getMessage());
            }
        }

        UUID paperId = session.getPaperId() != null
                ? session.getPaperId()
                : (assignment != null && assignment.getPaperId() != null
                    ? assignment.getPaperId()
                    : UUID.nameUUIDFromBytes(("paper-" + session.getExamId()).getBytes(StandardCharsets.UTF_8)));

        // 2. Fetch delivery questions (randomized options seeded with the existing session ID)
        List<QuestionDeliveryDto> baseQuestions = examQuestionDeliveryService != null
                ? examQuestionDeliveryService.getDeliveryQuestions(session.getExamId(), paperId, decryptedPaper, effectiveTenant)
                : List.of();

        List<QuestionDeliveryDto> questions = (examQuestionDeliveryService != null && baseQuestions != null && !baseQuestions.isEmpty())
                ? examQuestionDeliveryService.randomizeOptions(baseQuestions, session.getSessionId())
                : baseQuestions;

        String firstQuestionContent = extractFirstQuestion(decryptedPaper);
        if (firstQuestionContent == null && questions != null && !questions.isEmpty()) {
            try {
                firstQuestionContent = objectMapper.writeValueAsString(questions.get(0));
            } catch (JsonProcessingException ignored) {}
        }
        int totalQuestions = (questions != null && !questions.isEmpty()) ? questions.size() : countQuestions(decryptedPaper);

        // 3. Publish SESSION_RESUMED telemetry event to Kafka
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SESSION_RESUMED",
                    "sessionId", session.getSessionId().toString(),
                    "candidateId", session.getCandidateId().toString(),
                    "examId", session.getExamId().toString(),
                    "shiftId", session.getShiftId().toString(),
                    "resumedAt", now.toString(),
                    "tenantId", effectiveTenant
            );
            kafkaTemplate.send(TOPIC_SESSION_EVENTS, session.getSessionId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish SESSION_RESUMED event: {}", e.getMessage());
        }

        // 4. Read dynamic delivery parameters
        boolean kioskEnforced = dynamicConfigService.getBoolean("delivery.kiosk.mode.enforced", effectiveTenant, true);
        int heartbeatSec = dynamicConfigService.getInt("delivery.telemetry.heartbeat.seconds", effectiveTenant, 10);
        int autosaveSec = dynamicConfigService.getInt("delivery.autosave.interval.seconds", effectiveTenant, 15);
        int maxDisconnectGraceSec = dynamicConfigService.getInt("delivery.max.disconnect.grace.seconds", effectiveTenant, 180);
        boolean tamperEnabled = dynamicConfigService.getBoolean("delivery.tamper.detection.enabled", effectiveTenant, true);

        String examTitle = STANDARD_EXAM_TITLES.getOrDefault(
                session.getExamId().toString(),
                "Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026"
        );

        int remainingSeconds = session.getScheduledEndAt() != null
                ? (int) Math.max(0, Duration.between(now, session.getScheduledEndAt()).toSeconds())
                : 3600;

        int totalDurationSeconds = (session.getScheduledEndAt() != null && session.getStartedAt() != null)
                ? (int) Duration.between(session.getStartedAt(), session.getScheduledEndAt()).toSeconds()
                : 3600;

        return SessionStartResponse.builder()
                .sessionId(session.getSessionId())
                .examId(session.getExamId())
                .examTitle(examTitle)
                .shiftId(session.getShiftId())
                .candidateId(session.getCandidateId())
                .startedAt(session.getStartedAt())
                .scheduledEndAt(session.getScheduledEndAt())
                .serverTime(now)
                .expiresAt(session.getScheduledEndAt())
                .durationSeconds(remainingSeconds > 0 ? remainingSeconds : totalDurationSeconds)
                .navigationMode("FLEXIBLE")
                .firstQuestionContent(firstQuestionContent)
                .totalQuestions(totalQuestions)
                .questions(questions)
                .kioskModeEnforced(kioskEnforced)
                .heartbeatIntervalSeconds(heartbeatSec)
                .autosaveIntervalSeconds(autosaveSec)
                .maxDisconnectGraceSeconds(maxDisconnectGraceSec)
                .tamperDetectionEnabled(tamperEnabled)
                .build();
    }

    /**
     * Resume an existing active session by session ID.
     *
     * @param sessionId   the exam session UUID
     * @param candidateId the candidate UUID
     * @param tenantId    the tenant identifier
     * @return the session start/resume response
     */
    public SessionStartResponse resumeSessionById(UUID sessionId, UUID candidateId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getCandidateId().equals(candidateId)) {
            throw new IllegalArgumentException("Candidate " + candidateId + " is not authorized for session " + sessionId);
        }
        if (session.getStatus() != ExamSessionStatus.ACTIVE) {
            throw new IllegalStateException("Session " + sessionId + " is not in ACTIVE state (current: " + session.getStatus() + ")");
        }

        return resumeSession(session, effectiveTenant);
    }

    private String extractFirstQuestion(String decryptedPaper) {
        if (decryptedPaper == null || decryptedPaper.isBlank()) {
            return null;
        }
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
        if (decryptedPaper == null || decryptedPaper.isBlank()) {
            return 0;
        }
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
