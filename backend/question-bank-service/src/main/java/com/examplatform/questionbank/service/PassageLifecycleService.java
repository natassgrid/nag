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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Passage;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.PassageResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.repository.PassageRepository;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.shared.messaging.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service implementing the Passage lifecycle finite state machine (FSM).
 * Enforces valid transitions and the four-eyes principle (reviewer != author/approver).
 * Synchronously cascades state changes to all sub-questions in the passage group.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PassageLifecycleService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "DRAFT", Set.of("REVIEW"),
            "REVIEW", Set.of("APPROVED", "DRAFT"),
            "APPROVED", Set.of("PUBLISHED"),
            "PUBLISHED", Set.of("ARCHIVED")
    );

    private final PassageRepository passageRepository;
    private final QuestionRepository questionRepository;
    private final PassageService passageService;
    private final EventPublisher eventPublisher;

    /**
     * Transitions a passage and all its sub-questions to the requested target state.
     */
    public PassageResponse transition(UUID passageId, TransitionRequest request, UUID actorId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        String currentState = passage.getState();
        String targetState = request.getTargetState().toUpperCase();

        Set<String> allowedTargets = VALID_TRANSITIONS.get(currentState);
        if (allowedTargets == null || !allowedTargets.contains(targetState)) {
            throw new InvalidTransitionException(currentState, targetState);
        }

        if ("APPROVED".equals(targetState) && passage.getAuthorId().equals(actorId)) {
            throw new FourEyesPrincipleViolationException();
        }

        List<Question> subQuestions = updatePassageAndSubQuestions(passage, targetState, "APPROVED".equals(targetState) ? actorId : null);

        passageService.publishAuditEvent("PASSAGE_STATE_TRANSITIONED", passageId, actorId, tenantId,
                Map.of("fromState", currentState, "toState", targetState, "subQuestionCount", subQuestions.size()));

        return passageService.toResponse(passage, subQuestions);
    }

    /**
     * Submits a DRAFT passage for review.
     */
    public PassageResponse submitForReview(UUID passageId, UUID authorId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        if (!passage.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("Only the passage author can submit for review");
        }

        if (!"DRAFT".equals(passage.getState())) {
            throw new IllegalStateException("Passage must be in DRAFT state to submit for review. Current state: " + passage.getState());
        }

        List<Question> subQuestions = updatePassageAndSubQuestions(passage, "REVIEW", null);

        log.info("Passage submitted for review: id={}, author={}, tenant={}", passageId, authorId, tenantId);

        passageService.publishAuditEvent("PASSAGE_SUBMITTED_FOR_REVIEW", passage.getId(), authorId, tenantId,
                Map.of("fromState", "DRAFT", "toState", "REVIEW", "subQuestionCount", subQuestions.size()));

        return passageService.toResponse(passage, subQuestions);
    }

    /**
     * Approves a passage currently in REVIEW state.
     */
    public PassageResponse approve(UUID passageId, UUID reviewerId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        if (passage.getAuthorId().equals(reviewerId)) {
            throw new FourEyesPrincipleViolationException();
        }

        if (!"REVIEW".equals(passage.getState())) {
            throw new IllegalStateException("Passage must be in REVIEW state to approve. Current state: " + passage.getState());
        }

        List<Question> subQuestions = updatePassageAndSubQuestions(passage, "APPROVED", reviewerId);

        log.info("Passage approved: id={}, reviewer={}, tenant={}", passageId, reviewerId, tenantId);

        passageService.publishAuditEvent("PASSAGE_APPROVED", passage.getId(), reviewerId, tenantId,
                Map.of("fromState", "REVIEW", "toState", "APPROVED", "subQuestionCount", subQuestions.size()));

        return passageService.toResponse(passage, subQuestions);
    }

    /**
     * Rejects a passage currently in REVIEW state back to DRAFT.
     */
    public PassageResponse reject(UUID passageId, UUID reviewerId, String comments, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        if (!"REVIEW".equals(passage.getState())) {
            throw new IllegalStateException("Passage must be in REVIEW state to reject. Current state: " + passage.getState());
        }

        List<Question> subQuestions = updatePassageAndSubQuestions(passage, "DRAFT", null);

        log.info("Passage rejected: id={}, reviewer={}, tenant={}, comments={}", passageId, reviewerId, tenantId, comments);

        Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("fromState", "REVIEW");
        extra.put("toState", "DRAFT");
        extra.put("subQuestionCount", subQuestions.size());
        if (comments != null) {
            extra.put("comments", comments);
        }

        passageService.publishAuditEvent("PASSAGE_REJECTED", passage.getId(), reviewerId, tenantId, extra);

        return passageService.toResponse(passage, subQuestions);
    }

    private List<Question> updatePassageAndSubQuestions(Passage passage, String targetState, UUID reviewerId) {
        passage.setState(targetState);
        if (reviewerId != null) {
            passage.setReviewerId(reviewerId);
        }
        passageRepository.save(passage);

        List<Question> subQuestions = questionRepository.findByPassageIdOrderByPassageOrderIndexAsc(passage.getId());
        for (Question q : subQuestions) {
            q.setState(targetState);
            if (reviewerId != null) {
                q.setReviewerId(reviewerId);
            }
            questionRepository.save(q);
        }
        return subQuestions;
    }
}
