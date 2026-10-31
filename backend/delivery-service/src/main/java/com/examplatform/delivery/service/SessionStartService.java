/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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
import com.examplatform.shared.crypto.VaultCryptoService;
import com.examplatform.shared.event.SessionEvents;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
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
@Transactional
public class SessionStartService {

    private static final String DEFAULT_TENANT = "default";
    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String TOPIC_SESSION_EVENTS = "exam.session.events";

    private static final Map<String, String> STANDARD_EXAM_TITLES = Map.of(
            "e1000000-0000-0000-0000-000000000001", "Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026",
            "e1000000-0000-0000-0000-000000000002", "IBPS Probationary Officer (PO) Preliminary Examination 2026",
            "e1000000-0000-0000-0000-000000000003", "UPSC Civil Services (Preliminary) Examination 2026 - Paper 1 (General Studies)",
            "e1000000-0000-0000-0000-000000000004", "Railway Recruitment Board Non-Technical Popular Categories (RRB NTPC) CBT-1 2026",
            "e1000000-0000-0000-0000-000000000005", "Central Teacher Eligibility Test (CTET) Paper-1 (Primary Teacher) 2026"
    );

    private final ExamSessionRepository examSessionRepository;
    private final ShiftAssignmentClient shiftAssignmentClient;
    private final DisabilityExtensionService disabilityExtensionService;
    private final VaultCryptoService vaultCryptoService;
    private final ExamQuestionDeliveryService examQuestionDeliveryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final DynamicConfigService dynamicConfigService;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final MockExamAccessValidator mockExamAccessValidator;

    public SessionStartService(
            ExamSessionRepository examSessionRepository,
            ShiftAssignmentClient shiftAssignmentClient,
            DisabilityExtensionService disabilityExtensionService,
            VaultCryptoService vaultCryptoService,
            ExamQuestionDeliveryService examQuestionDeliveryService,
            RedisTemplate<String, Object> redisTemplate,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            DynamicConfigService dynamicConfigService) {
        this(examSessionRepository, shiftAssignmentClient, disabilityExtensionService, vaultCryptoService,
                examQuestionDeliveryService, redisTemplate, eventPublisher, objectMapper, dynamicConfigService, null, null);
    }

    public SessionStartService(
            ExamSessionRepository examSessionRepository,
            ShiftAssignmentClient shiftAssignmentClient,
            DisabilityExtensionService disabilityExtensionService,
            VaultCryptoService vaultCryptoService,
            ExamQuestionDeliveryService examQuestionDeliveryService,
            RedisTemplate<String, Object> redisTemplate,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            DynamicConfigService dynamicConfigService,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this(examSessionRepository, shiftAssignmentClient, disabilityExtensionService, vaultCryptoService,
                examQuestionDeliveryService, redisTemplate, eventPublisher, objectMapper, dynamicConfigService, jdbcTemplateProvider, null);
    }

    @Autowired
    public SessionStartService(
            ExamSessionRepository examSessionRepository,
            ShiftAssignmentClient shiftAssignmentClient,
            DisabilityExtensionService disabilityExtensionService,
            VaultCryptoService vaultCryptoService,
            ExamQuestionDeliveryService examQuestionDeliveryService,
            RedisTemplate<String, Object> redisTemplate,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper,
            DynamicConfigService dynamicConfigService,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Autowired(required = false) MockExamAccessValidator mockExamAccessValidator) {
        this.examSessionRepository = examSessionRepository;
        this.shiftAssignmentClient = shiftAssignmentClient;
        this.disabilityExtensionService = disabilityExtensionService;
        this.vaultCryptoService = vaultCryptoService;
        this.examQuestionDeliveryService = examQuestionDeliveryService;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.dynamicConfigService = dynamicConfigService;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.mockExamAccessValidator = mockExamAccessValidator;
    }

    private static String resolveEffectiveTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : DEFAULT_TENANT;
    }

    /**
     * Start a new exam session or seamlessly resume an active session for the candidate.
     *
     * @param request     the session start request containing exam/shift details
     * @param candidateId the authenticated candidate's ID (from JWT sub claim)
     * @param tenantId    the tenant identifier (from JWT or header)
     * @return the session start/resume response including questions with randomized options
     */
    public SessionStartResponse startSession(SessionStartRequest request, UUID candidateId, String tenantId) {
        if (mockExamAccessValidator != null) {
            mockExamAccessValidator.validateCandidateAccess(candidateId, request.getExamId());
        }

        String effectiveTenant = resolveEffectiveTenant(tenantId);
        Instant now = Instant.now();

        // 1. Check for existing active session for this candidate
        Optional<SessionStartResponse> existingSessionResponse =
                handleExistingActiveSession(request, candidateId, effectiveTenant, now);
        if (existingSessionResponse.isPresent()) {
            return existingSessionResponse.get();
        }

        UUID shiftId = resolveShiftId(request);

        // 2. Look up shift assignment from examination-service
        ShiftAssignment assignment = shiftAssignmentClient.getShiftAssignment(
                candidateId, request.getExamId(), shiftId, effectiveTenant);

        // 3. Decrypt shift paper package using Vault Transit engine (in-memory only)
        String decryptedPaper = decryptPaperPackage(assignment, shiftId);
        UUID paperId = resolvePaperId(request.getPaperId(), assignment, request.getExamId());

        UUID sessionId = UUID.randomUUID();

        // 4 & 5. Resolve delivery questions and randomize options per session
        List<QuestionDeliveryDto> questions = prepareDeliveryQuestions(
                request.getExamId(), paperId, decryptedPaper, effectiveTenant, sessionId, request.getLanguageCode());

        QuestionPreparationResult qPrep = prepareQuestionsMetadata(decryptedPaper, questions);

        // 6 & 7. Apply duration and disability extension time
        int disabilityExtension = disabilityExtensionService.getExtraTimeMinutes(candidateId, effectiveTenant);
        int totalDuration = calculateTotalDurationMinutes(request.getExamId(), assignment, disabilityExtension);
        Instant scheduledEnd = now.plus(Duration.ofMinutes(totalDuration));

        // 8. Save session to database
        ExamSession savedSession = createAndSaveSession(
                request, candidateId, effectiveTenant, sessionId, shiftId, paperId, now, scheduledEnd);

        // 9. Cache session in Redis for fast subsequent lookups
        cacheSessionQuietly(savedSession);

        // 10. Publish SESSION_STARTED event (fire-and-forget)
        publishSessionStartedEvent(savedSession, candidateId, request.getExamId(), shiftId, effectiveTenant);

        // 11. Read dynamic delivery parameters
        DynamicDeliveryParams deliveryParams = resolveDynamicDeliveryParams(effectiveTenant);
        String examTitle = resolveExamTitle(request.getExamId());
        String navigationMode = resolveNavigationPolicy(request.getExamId(), assignment);

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
                .navigationMode(navigationMode)
                .firstQuestionContent(qPrep.firstQuestionContent())
                .totalQuestions(qPrep.totalQuestions())
                .questions(questions)
                .sections(examQuestionDeliveryService.buildSections(questions))
                .kioskModeEnforced(deliveryParams.kioskEnforced())
                .heartbeatIntervalSeconds(deliveryParams.heartbeatSec())
                .autosaveIntervalSeconds(deliveryParams.autosaveSec())
                .maxDisconnectGraceSeconds(deliveryParams.maxDisconnectGraceSec())
                .tamperDetectionEnabled(deliveryParams.tamperEnabled())
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
        String effectiveTenant = resolveEffectiveTenant(tenantId);
        Instant now = Instant.now();

        // 1. Look up shift assignment and decrypt paper package if available
        ShiftAssignment assignment = fetchShiftAssignmentQuietly(
                session.getCandidateId(), session.getExamId(), session.getShiftId(), effectiveTenant, session.getSessionId());
        String decryptedPaper = decryptPaperPackage(assignment, session.getShiftId());
        UUID paperId = resolvePaperId(session.getPaperId(), assignment, session.getExamId());

        // 2. Fetch delivery questions (randomized options seeded with the existing session ID)
        List<QuestionDeliveryDto> questions = prepareDeliveryQuestions(
                session.getExamId(), paperId, decryptedPaper, effectiveTenant, session.getSessionId(), session.getLanguageCode());

        QuestionPreparationResult qPrep = prepareQuestionsMetadata(decryptedPaper, questions);

        // 3. Publish SESSION_RESUMED telemetry event
        publishSessionResumedEvent(session, effectiveTenant);

        // 4. Read dynamic delivery parameters
        DynamicDeliveryParams deliveryParams = resolveDynamicDeliveryParams(effectiveTenant);
        String examTitle = resolveExamTitle(session.getExamId());
        int durationSeconds = calculateResumeDurationSeconds(session, now);
        String navigationMode = resolveNavigationPolicy(session.getExamId(), assignment);

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
                .durationSeconds(durationSeconds)
                .navigationMode(navigationMode)
                .firstQuestionContent(qPrep.firstQuestionContent())
                .totalQuestions(qPrep.totalQuestions())
                .questions(questions)
                .sections(examQuestionDeliveryService.buildSections(questions))
                .kioskModeEnforced(deliveryParams.kioskEnforced())
                .heartbeatIntervalSeconds(deliveryParams.heartbeatSec())
                .autosaveIntervalSeconds(deliveryParams.autosaveSec())
                .maxDisconnectGraceSeconds(deliveryParams.maxDisconnectGraceSec())
                .tamperDetectionEnabled(deliveryParams.tamperEnabled())
                .build();
    }

    /**
     * Terminate candidate's current active exam session(s).
     */
    @Transactional
    public int terminateActiveSessionsForCandidate(UUID candidateId, String tenantId) {
        String effectiveTenant = resolveEffectiveTenant(tenantId);
        List<ExamSession> existingSessions = examSessionRepository.findByCandidateIdAndTenantId(candidateId, effectiveTenant);
        int count = 0;
        for (ExamSession s : existingSessions) {
            if (s.getStatus() == ExamSessionStatus.ACTIVE) {
                s.setStatus(ExamSessionStatus.EXPIRED);
                examSessionRepository.save(s);
                count++;
            }
        }
        return count;
    }

    public SessionStartResponse resumeSessionById(UUID sessionId, UUID candidateId, String tenantId) {
        String effectiveTenant = resolveEffectiveTenant(tenantId);
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

    private Optional<SessionStartResponse> handleExistingActiveSession(
            SessionStartRequest request, UUID candidateId, String effectiveTenant, Instant now) {
        List<ExamSession> existingSessions = examSessionRepository.findByCandidateIdAndTenantId(candidateId, effectiveTenant);
        Optional<ExamSession> activeSessionOpt = existingSessions.stream()
                .filter(s -> s.getStatus() == ExamSessionStatus.ACTIVE)
                .findFirst();

        if (activeSessionOpt.isEmpty()) {
            return Optional.empty();
        }

        ExamSession activeSession = activeSessionOpt.get();

        if (activeSession.getScheduledEndAt() != null && now.isAfter(activeSession.getScheduledEndAt())) {
            log.info("Active session [{}] for candidate [{}] expired at [{}]. Marking EXPIRED.",
                    activeSession.getSessionId(), candidateId, activeSession.getScheduledEndAt());
            activeSession.setStatus(ExamSessionStatus.EXPIRED);
            examSessionRepository.save(activeSession);
            return Optional.empty();
        }

        if (Boolean.TRUE.equals(request.getForceNewSession()) || Boolean.TRUE.equals(request.getTerminateExisting())) {
            log.info("Terminating existing active session [{}] on exam [{}] for candidate [{}] to start exam [{}] (forceNewSession=true).",
                    activeSession.getSessionId(), activeSession.getExamId(), candidateId, request.getExamId());
            activeSession.setStatus(ExamSessionStatus.EXPIRED);
            examSessionRepository.save(activeSession);
            return Optional.empty();
        }

        if (activeSession.getExamId() != null && activeSession.getExamId().equals(request.getExamId())) {
            log.info("Resuming active session [{}] for candidate [{}] on exam [{}]",
                    activeSession.getSessionId(), candidateId, request.getExamId());
            return Optional.of(resumeSession(activeSession, effectiveTenant));
        }

        throw new ConcurrentSessionException(
                "Candidate " + candidateId + " already has an active session for exam " + activeSession.getExamId() + " in tenant " + effectiveTenant,
                activeSession.getExamId(),
                activeSession.getSessionId());
    }

    private UUID resolveShiftId(SessionStartRequest request) {
        if (request.getShiftId() != null) {
            return request.getShiftId();
        }
        return UUID.nameUUIDFromBytes(("shift-" + request.getExamId()).getBytes(StandardCharsets.UTF_8));
    }

    private String decryptPaperPackage(ShiftAssignment assignment, UUID shiftId) {
        if (assignment == null || assignment.getEncryptedPackageRef() == null || assignment.getEncryptedPackageRef().isBlank()) {
            return null;
        }
        try {
            String keyName = "shift-key-" + shiftId;
            return vaultCryptoService.decrypt(keyName, assignment.getEncryptedPackageRef());
        } catch (Exception e) {
            log.warn("Vault decryption failed for shift paper key [shift-key-{}]: {}", shiftId, e.getMessage());
            return null;
        }
    }

    private List<QuestionDeliveryDto> prepareDeliveryQuestions(
            UUID examId, UUID paperId, String decryptedPaper, String effectiveTenant, UUID sessionId, String languageCode) {
        if (examQuestionDeliveryService == null) {
            return List.of();
        }
        List<QuestionDeliveryDto> baseQuestions = examQuestionDeliveryService.getDeliveryQuestions(
                examId, paperId, decryptedPaper, effectiveTenant);
        if (baseQuestions == null || baseQuestions.isEmpty()) {
            return List.of();
        }
        List<QuestionDeliveryDto> questions = examQuestionDeliveryService.randomizeOptions(baseQuestions, sessionId);
        if (questions != null && !questions.isEmpty()) {
            examQuestionDeliveryService.applyLanguagePreference(questions, languageCode);
            return questions;
        }
        return baseQuestions;
    }

    private int calculateTotalDurationMinutes(UUID examId, ShiftAssignment assignment, int disabilityExtension) {
        int baseDuration = resolveExamDurationMinutes(examId, assignment);
        int extraTime = assignment != null ? assignment.getExtraTimeMinutes() : 0;
        return baseDuration + extraTime + disabilityExtension;
    }

    private ExamSession createAndSaveSession(
            SessionStartRequest request, UUID candidateId, String effectiveTenant,
            UUID sessionId, UUID shiftId, UUID paperId, Instant now, Instant scheduledEnd) {
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
        return examSessionRepository.save(session);
    }

    private void cacheSessionQuietly(ExamSession session) {
        String cacheKey = SESSION_CACHE_PREFIX + session.getSessionId();
        try {
            redisTemplate.opsForValue().set(cacheKey, session, Duration.ofHours(6));
        } catch (Exception e) {
            log.warn("Failed to cache session in Redis: {}", e.getMessage());
        }
    }

    private void publishSessionStartedEvent(
            ExamSession session, UUID candidateId, UUID examId, UUID shiftId, String tenantId) {
        try {
            SessionEvents.SessionStarted event = SessionEvents.SessionStarted.of(
                    session.getSessionId().toString(),
                    candidateId.toString(),
                    examId.toString(),
                    shiftId.toString(),
                    tenantId
            );
            eventPublisher.publish(TOPIC_SESSION_EVENTS, session.getSessionId().toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing SESSION_STARTED event: {}", e.getMessage());
        }
    }

    private ShiftAssignment fetchShiftAssignmentQuietly(
            UUID candidateId, UUID examId, UUID shiftId, String tenantId, UUID sessionId) {
        try {
            return shiftAssignmentClient.getShiftAssignment(candidateId, examId, shiftId, tenantId);
        } catch (Exception e) {
            log.warn("Could not retrieve shift assignment on resume for session [{}]: {}", sessionId, e.getMessage());
            return null;
        }
    }

    private void publishSessionResumedEvent(ExamSession session, String tenantId) {
        try {
            SessionEvents.SessionResumed event = SessionEvents.SessionResumed.of(
                    session.getSessionId().toString(),
                    session.getCandidateId().toString(),
                    session.getExamId().toString(),
                    session.getShiftId().toString(),
                    tenantId
            );
            eventPublisher.publish(TOPIC_SESSION_EVENTS, session.getSessionId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish SESSION_RESUMED event: {}", e.getMessage());
        }
    }

    private int calculateResumeDurationSeconds(ExamSession session, Instant now) {
        if (session.getScheduledEndAt() == null) {
            return 3600;
        }
        int remainingSeconds = (int) Math.max(0, Duration.between(now, session.getScheduledEndAt()).toSeconds());
        if (remainingSeconds > 0) {
            return remainingSeconds;
        }
        if (session.getStartedAt() != null) {
            return (int) Duration.between(session.getStartedAt(), session.getScheduledEndAt()).toSeconds();
        }
        return 3600;
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

    private String resolveExamTitle(UUID examId) {
        if (examId == null) {
            return "NextGen Assessment Grid Examination";
        }
        String examIdStr = examId.toString();
        if (STANDARD_EXAM_TITLES.containsKey(examIdStr)) {
            return STANDARD_EXAM_TITLES.get(examIdStr);
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider != null ? jdbcTemplateProvider.getIfAvailable() : null;
        if (jdbcTemplate != null) {
            try {
                String title = jdbcTemplate.queryForObject(
                        "SELECT name FROM examination_service.examination WHERE id = ?",
                        String.class,
                        examId
                );
                if (title != null && !title.isBlank()) {
                    return title;
                }
            } catch (Exception e) {
                log.debug("Ignore examination title query failure: {}", e.getMessage());
            }
        }
        return "NextGen Assessment Grid Examination (" + examIdStr.substring(0, 8) + ")";
    }

    private int resolveExamDurationMinutes(UUID examId, ShiftAssignment assignment) {
        if (assignment != null && assignment.getDurationMinutes() > 0) {
            return assignment.getDurationMinutes();
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider != null ? jdbcTemplateProvider.getIfAvailable() : null;
        if (jdbcTemplate != null && examId != null) {
            try {
                Integer duration = jdbcTemplate.queryForObject(
                        "SELECT duration_minutes FROM examination_service.examination WHERE id = ?",
                        Integer.class,
                        examId
                );
                if (duration != null && duration > 0) {
                    return duration;
                }
            } catch (Exception e) {
                log.debug("Ignore duration query failure: {}", e.getMessage());
            }
        }
        return 60;
    }

    private String resolveNavigationPolicy(UUID examId, ShiftAssignment assignment) {
        if (assignment != null && assignment.getNavigationPolicy() != null && !assignment.getNavigationPolicy().isBlank()) {
            return assignment.getNavigationPolicy().trim().toUpperCase();
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider != null ? jdbcTemplateProvider.getIfAvailable() : null;
        if (jdbcTemplate != null && examId != null) {
            try {
                String policy = jdbcTemplate.queryForObject(
                        "SELECT navigation_policy FROM examination_service.examination WHERE id = ?",
                        String.class,
                        examId
                );
                if (policy != null && !policy.isBlank()) {
                    return policy.trim().toUpperCase();
                }
            } catch (Exception e) {
                log.debug("Ignore navigation policy query failure: {}", e.getMessage());
            }
        }
        return "FLEXIBLE";
    }

    record DynamicDeliveryParams(
            boolean kioskEnforced,
            int heartbeatSec,
            int autosaveSec,
            int maxDisconnectGraceSec,
            boolean tamperEnabled
    ) {}

    private DynamicDeliveryParams resolveDynamicDeliveryParams(String effectiveTenant) {
        return new DynamicDeliveryParams(
                dynamicConfigService.getBoolean("delivery.kiosk.mode.enforced", effectiveTenant, true),
                dynamicConfigService.getInt("delivery.telemetry.heartbeat.seconds", effectiveTenant, 10),
                dynamicConfigService.getInt("delivery.autosave.interval.seconds", effectiveTenant, 15),
                dynamicConfigService.getInt("delivery.max.disconnect.grace.seconds", effectiveTenant, 180),
                dynamicConfigService.getBoolean("delivery.tamper.detection.enabled", effectiveTenant, true)
        );
    }

    record QuestionPreparationResult(
            String firstQuestionContent,
            int totalQuestions
    ) {}

    private QuestionPreparationResult prepareQuestionsMetadata(String decryptedPaper, List<QuestionDeliveryDto> questions) {
        String firstQuestionContent = extractFirstQuestion(decryptedPaper);
        if (firstQuestionContent == null && questions != null && !questions.isEmpty()) {
            try {
                firstQuestionContent = objectMapper.writeValueAsString(questions.get(0));
            } catch (JsonProcessingException e) {
                log.debug("Ignore question serialization failure: {}", e.getMessage());
            }
        }
        int totalQuestions = (questions != null && !questions.isEmpty()) ? questions.size() : countQuestions(decryptedPaper);
        return new QuestionPreparationResult(firstQuestionContent, totalQuestions);
    }

    private UUID resolvePaperId(UUID explicitPaperId, ShiftAssignment assignment, UUID examId) {
        if (explicitPaperId != null) {
            return explicitPaperId;
        }
        if (assignment != null && assignment.getPaperId() != null) {
            return assignment.getPaperId();
        }
        return UUID.nameUUIDFromBytes(("paper-" + examId).getBytes(StandardCharsets.UTF_8));
    }
}
