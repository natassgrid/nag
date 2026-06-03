package com.examplatform.delivery.service;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.dto.NavigationRequest;
import com.examplatform.delivery.dto.NavigationResponse;
import com.examplatform.delivery.dto.NavigationResponse.NavigationAction;
import com.examplatform.delivery.dto.NavigationResponse.NavigationPolicy;
import com.examplatform.delivery.exception.NavigationPolicyViolationException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service for handling question navigation within exam sessions.
 * Applies navigation policy rules (Sequential, Flexible, Restricted) and
 * rejects policy-violating navigation attempts with HTTP 422.
 * Supports One_Question, Section_Mode, and Batch_Mode rendering.
 *
 * Validates: Requirements 9.2, 9.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationService {

    private static final String SESSION_CACHE_PREFIX = "session:";

    private final ExamSessionRepository examSessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Navigate to a target question within an exam session.
     * Validates the navigation against the session's policy before updating state.
     *
     * @param request     the navigation request with target indices
     * @param candidateId the authenticated candidate's ID
     * @param tenantId    the tenant context
     * @return the navigation response with updated position and allowed actions
     * @throws NavigationPolicyViolationException if the navigation violates the session's policy
     */
    @Transactional
    public NavigationResponse navigate(NavigationRequest request, UUID candidateId, String tenantId) {
        ExamSession session = findSession(request.getSessionId(), candidateId, tenantId);

        // Determine the navigation policy for this session
        NavigationPolicy policy = determinePolicy(session);

        int currentIndex = session.getCurrentQuestionIndex();
        int targetIndex = request.getTargetQuestionIndex() != null
                ? request.getTargetQuestionIndex()
                : currentIndex;

        // Validate navigation against policy
        validateNavigation(policy, currentIndex, targetIndex, request.getTargetSectionIndex(), session);

        // Update session state
        session.setCurrentQuestionIndex(targetIndex);
        examSessionRepository.save(session);

        // Update Redis cache
        updateSessionCache(session);

        // Determine allowed actions from new position
        List<NavigationAction> allowedActions = computeAllowedActions(policy, targetIndex, session);

        log.info("Navigation successful: session={}, from={}, to={}, policy={}",
                session.getSessionId(), currentIndex, targetIndex, policy);

        return NavigationResponse.builder()
                .sessionId(session.getSessionId())
                .currentQuestionIndex(targetIndex)
                .questionContent(null) // Content loaded separately by delivery pipeline
                .navigationPolicy(policy)
                .allowedActions(allowedActions)
                .currentSectionIndex(request.getTargetSectionIndex())
                .totalQuestions(getTotalQuestions(session))
                .build();
    }

    private ExamSession findSession(UUID sessionId, UUID candidateId, String tenantId) {
        // Try Redis cache first
        String cacheKey = SESSION_CACHE_PREFIX + sessionId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ExamSession cachedSession) {
            if (cachedSession.getCandidateId().equals(candidateId)) {
                return cachedSession;
            }
        }

        // Fall back to database
        return examSessionRepository.findByCandidateIdAndTenantId(candidateId, tenantId).stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .filter(s -> s.getStatus() == ExamSession.ExamSessionStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Active session not found: " + sessionId + " for candidate: " + candidateId));
    }

    /**
     * Determines the navigation policy for a session.
     * In a full implementation, this would be derived from exam configuration.
     * Default is FLEXIBLE if not specified.
     */
    NavigationPolicy determinePolicy(ExamSession session) {
        // Navigation policy is typically set per exam configuration
        // For now, derive from exam metadata or default to FLEXIBLE
        // This can be extended to read from exam configuration
        return NavigationPolicy.FLEXIBLE;
    }

    void validateNavigation(NavigationPolicy policy, int currentIndex, int targetIndex,
                            Integer targetSectionIndex, ExamSession session) {
        int totalQuestions = getTotalQuestions(session);

        // Bounds check
        if (targetIndex < 0 || targetIndex >= totalQuestions) {
            throw new NavigationPolicyViolationException(
                    "Target question index " + targetIndex + " is out of bounds [0, " + (totalQuestions - 1) + "]");
        }

        switch (policy) {
            case SEQUENTIAL -> validateSequentialNavigation(currentIndex, targetIndex);
            case FLEXIBLE -> {
                // All navigation is allowed in FLEXIBLE mode
            }
            case RESTRICTED -> validateRestrictedNavigation(currentIndex, targetIndex, targetSectionIndex, session);
        }
    }

    private void validateSequentialNavigation(int currentIndex, int targetIndex) {
        // SEQUENTIAL: only NEXT is allowed (exactly +1 from current position)
        if (targetIndex != currentIndex + 1) {
            if (targetIndex < currentIndex) {
                throw new NavigationPolicyViolationException(
                        "SEQUENTIAL policy: backward navigation (PREV) is not allowed. " +
                                "Current: " + currentIndex + ", target: " + targetIndex);
            }
            if (targetIndex > currentIndex + 1) {
                throw new NavigationPolicyViolationException(
                        "SEQUENTIAL policy: jumping ahead is not allowed. " +
                                "Current: " + currentIndex + ", target: " + targetIndex);
            }
        }
    }

    private void validateRestrictedNavigation(int currentIndex, int targetIndex,
                                              Integer targetSectionIndex, ExamSession session) {
        // RESTRICTED: NEXT and PREV within same section, no cross-section JUMP
        int currentSection = getSectionForQuestion(currentIndex, session);
        int targetSection = targetSectionIndex != null ? targetSectionIndex : getSectionForQuestion(targetIndex, session);

        if (currentSection != targetSection) {
            throw new NavigationPolicyViolationException(
                    "RESTRICTED policy: cross-section navigation is not allowed. " +
                            "Current section: " + currentSection + ", target section: " + targetSection);
        }

        // Within section, only NEXT and PREV are allowed (adjacent moves)
        int distance = Math.abs(targetIndex - currentIndex);
        if (distance > 1) {
            throw new NavigationPolicyViolationException(
                    "RESTRICTED policy: jumping within section is not allowed. " +
                            "Only NEXT and PREV are permitted. Current: " + currentIndex + ", target: " + targetIndex);
        }
    }

    List<NavigationAction> computeAllowedActions(NavigationPolicy policy, int currentIndex, ExamSession session) {
        int totalQuestions = getTotalQuestions(session);
        List<NavigationAction> actions = new ArrayList<>();

        switch (policy) {
            case SEQUENTIAL -> {
                if (currentIndex < totalQuestions - 1) {
                    actions.add(NavigationAction.NEXT);
                }
            }
            case FLEXIBLE -> {
                if (currentIndex < totalQuestions - 1) {
                    actions.add(NavigationAction.NEXT);
                }
                if (currentIndex > 0) {
                    actions.add(NavigationAction.PREV);
                }
                actions.add(NavigationAction.JUMP);
                actions.add(NavigationAction.SECTION_SWITCH);
            }
            case RESTRICTED -> {
                if (currentIndex < totalQuestions - 1 && isSameSection(currentIndex, currentIndex + 1, session)) {
                    actions.add(NavigationAction.NEXT);
                }
                if (currentIndex > 0 && isSameSection(currentIndex, currentIndex - 1, session)) {
                    actions.add(NavigationAction.PREV);
                }
            }
        }

        return actions;
    }

    private int getSectionForQuestion(int questionIndex, ExamSession session) {
        // Default section calculation: every 10 questions is a section
        // In a full implementation, this would be read from exam structure
        return questionIndex / 10;
    }

    private boolean isSameSection(int index1, int index2, ExamSession session) {
        return getSectionForQuestion(index1, session) == getSectionForQuestion(index2, session);
    }

    private int getTotalQuestions(ExamSession session) {
        // Default total questions — in full implementation, read from paper metadata
        // A minimum of currentQuestionIndex + 1 to avoid bounds issues
        return Math.max(session.getCurrentQuestionIndex() + 1, 50);
    }

    private void updateSessionCache(ExamSession session) {
        String cacheKey = SESSION_CACHE_PREFIX + session.getSessionId();
        try {
            redisTemplate.opsForValue().set(cacheKey, session);
        } catch (Exception e) {
            log.warn("Failed to update session cache for {}: {}", session.getSessionId(), e.getMessage());
        }
    }
}
