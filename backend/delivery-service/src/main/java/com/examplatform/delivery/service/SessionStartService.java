package com.examplatform.delivery.service;

import com.examplatform.delivery.client.ShiftAssignmentClient;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.dto.ShiftAssignment;
import com.examplatform.delivery.exception.ConcurrentSessionException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final VaultCryptoService vaultCryptoService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_CACHE_PREFIX = "session:";

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

        // 5. Create exam session entity
        Instant now = Instant.now();
        int totalDuration = assignment.getDurationMinutes() + assignment.getExtraTimeMinutes();
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

        // 6. Save session to database
        ExamSession savedSession = examSessionRepository.save(session);

        // 7. Cache session in Redis for fast subsequent lookups
        String cacheKey = SESSION_CACHE_PREFIX + savedSession.getSessionId();
        redisTemplate.opsForValue().set(cacheKey, savedSession, Duration.ofHours(6));

        // 8. Return response with first question
        return SessionStartResponse.builder()
                .sessionId(savedSession.getSessionId())
                .examId(request.getExamId())
                .shiftId(request.getShiftId())
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .firstQuestionContent(firstQuestionContent)
                .totalQuestions(totalQuestions)
                .build();
    }

    /**
     * Extracts the first question content from the decrypted paper JSON.
     * Expected structure: {"questions": [{"content": "...", ...}, ...]}
     */
    private String extractFirstQuestion(String decryptedPaper) {
        try {
            JsonNode root = objectMapper.readTree(decryptedPaper);
            JsonNode questions = root.path("questions");
            if (questions.isArray() && !questions.isEmpty()) {
                JsonNode firstQuestion = questions.get(0);
                return firstQuestion.has("content")
                        ? firstQuestion.get("content").asText()
                        : firstQuestion.toString();
            }
            return "";
        } catch (JsonProcessingException e) {
            log.error("Failed to parse decrypted paper content", e);
            return "";
        }
    }

    /**
     * Counts total questions in the decrypted paper JSON.
     */
    private int countQuestions(String decryptedPaper) {
        try {
            JsonNode root = objectMapper.readTree(decryptedPaper);
            JsonNode questions = root.path("questions");
            return questions.isArray() ? questions.size() : 0;
        } catch (JsonProcessingException e) {
            log.error("Failed to count questions in decrypted paper", e);
            return 0;
        }
    }
}
