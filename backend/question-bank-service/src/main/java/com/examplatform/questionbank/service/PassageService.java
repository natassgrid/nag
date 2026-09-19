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

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.domain.Passage;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.PassageRequest;
import com.examplatform.questionbank.dto.PassageResponse;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.SubQuestionRequest;
import com.examplatform.questionbank.repository.PassageRepository;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.examplatform.shared.messaging.EventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing Comprehension / Case Study passages and their associated sub-questions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PassageService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final PassageRepository passageRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionService questionService;
    private final EmbeddingService embeddingService;
    private final EventPublisher eventPublisher;

    @Value("${app.encryption.enabled:false}")
    private boolean encryptionEnabled;

    /**
     * Creates a new passage with its sub-questions in DRAFT state.
     */
    public PassageResponse createPassage(PassageRequest request, UUID authorId, String tenantId) {
        if (request.getSubQuestions() == null || request.getSubQuestions().size() < 2 || request.getSubQuestions().size() > 6) {
            throw new IllegalArgumentException("Passage must have between 2 and 6 sub-questions");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));

        String topicName = null;
        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .filter(t -> tenantId.equals(t.getTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + request.getTopicId()));
            topicName = topic.getName();
        }

        String dekKeyName = encryptionEnabled ? "passage-dek-" + UUID.randomUUID() : null;

        boolean hasImages = request.isHasImages() || detectPassageImages(request.getContent());

        Passage passage = Passage.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .contentFormat(request.getContentFormat() != null ? request.getContentFormat() : "MIXED")
                .subjectId(subject.getId())
                .topicId(request.getTopicId())
                .subject(subject.getName())
                .topic(topicName != null ? topicName : request.getTopic())
                .hasImages(hasImages)
                .state("DRAFT")
                .encryptionKeyId(dekKeyName)
                .authorId(authorId)
                .build();
        passage.setTenantId(tenantId);

        Passage savedPassage = passageRepository.save(passage);

        // Generate embedding for passage
        try {
            float[] embedding = embeddingService.embed(request.getContent());
            if (embedding != null && embedding.length > 0) {
                savedPassage.setEmbedding(embedding);
            }
        } catch (Exception e) {
            log.warn("Failed to generate embedding for passage id={}. Reason: {}", savedPassage.getId(), e.getMessage());
        }

        // Create sub-questions
        List<Question> createdQuestions = new ArrayList<>();
        int index = 0;
        for (SubQuestionRequest subReq : request.getSubQuestions()) {
            Question subQ = buildSubQuestion(subReq, savedPassage, subject, topicName, authorId, tenantId, index++);
            Question savedSubQ = questionRepository.save(subQ);

            try {
                float[] qEmbedding = embeddingService.embed(subReq.getContent());
                if (qEmbedding != null && qEmbedding.length > 0) {
                    questionRepository.updateEmbedding(savedSubQ.getId(), EmbeddingUtils.embeddingToString(qEmbedding));
                }
            } catch (Exception ignored) {}

            createdQuestions.add(savedSubQ);
        }

        log.info("Created passage id={} with {} sub-questions for author={} tenant={}",
                savedPassage.getId(), createdQuestions.size(), authorId, tenantId);

        publishAuditEvent("PASSAGE_CREATED", savedPassage.getId(), authorId, tenantId,
                Map.of("subQuestionCount", createdQuestions.size(), "state", savedPassage.getState()));

        return toResponse(savedPassage, createdQuestions);
    }

    /**
     * Retrieves a passage by ID with its sub-questions.
     */
    @Transactional(readOnly = true)
    public PassageResponse getPassage(UUID passageId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        List<Question> subQuestions = questionRepository.findByPassageIdOrderByPassageOrderIndexAsc(passageId);
        return toResponse(passage, subQuestions);
    }

    /**
     * Updates an existing passage and synchronizes its sub-questions.
     */
    public PassageResponse updatePassage(UUID passageId, PassageRequest request, UUID authorId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        if (request.getSubQuestions() == null || request.getSubQuestions().size() < 2 || request.getSubQuestions().size() > 6) {
            throw new IllegalArgumentException("Passage must have between 2 and 6 sub-questions");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));

        String topicName = null;
        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .filter(t -> tenantId.equals(t.getTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + request.getTopicId()));
            topicName = topic.getName();
        }

        boolean contentChanged = !Objects.equals(passage.getContent(), request.getContent());
        passage.setTitle(request.getTitle());
        passage.setContent(request.getContent());
        passage.setContentFormat(request.getContentFormat() != null ? request.getContentFormat() : "MIXED");
        passage.setSubjectId(subject.getId());
        passage.setTopicId(request.getTopicId());
        passage.setSubject(subject.getName());
        passage.setTopic(topicName != null ? topicName : request.getTopic());
        passage.setHasImages(request.isHasImages() || detectPassageImages(request.getContent()));

        Passage savedPassage = passageRepository.save(passage);

        if (contentChanged) {
            try {
                float[] embedding = embeddingService.embed(request.getContent());
                if (embedding != null && embedding.length > 0) {
                    savedPassage.setEmbedding(embedding);
                }
            } catch (Exception e) {
                log.warn("Failed to regenerate embedding for passage id={}: {}", passageId, e.getMessage());
            }
        }

        // Synchronize sub-questions
        List<Question> existingQuestions = questionRepository.findByPassageIdOrderByPassageOrderIndexAsc(passageId);
        Map<UUID, Question> existingMap = existingQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Question> resultQuestions = new ArrayList<>();
        int index = 0;
        for (SubQuestionRequest subReq : request.getSubQuestions()) {
            if (subReq.getId() != null && existingMap.containsKey(subReq.getId())) {
                Question existing = existingMap.remove(subReq.getId());
                updateSubQuestionFields(existing, subReq, savedPassage, subject, topicName, index++);
                Question updated = questionRepository.save(existing);
                resultQuestions.add(updated);
            } else {
                Question newQ = buildSubQuestion(subReq, savedPassage, subject, topicName, authorId, tenantId, index++);
                Question savedNew = questionRepository.save(newQ);
                resultQuestions.add(savedNew);
            }
        }

        // Delete any sub-questions that were removed from the request
        for (Question removed : existingMap.values()) {
            questionRepository.delete(removed);
        }

        log.info("Updated passage id={} with {} sub-questions for author={} tenant={}",
                passageId, resultQuestions.size(), authorId, tenantId);

        return toResponse(savedPassage, resultQuestions);
    }

    /**
     * Lists passages with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<PassageResponse> listPassages(
            Long subjectId, String state, String search, int page, int size, String tenantId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Passage> spec = Specification.where((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));

        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectId"), subjectId));
        }
        if (state != null && !state.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("state")), state.toLowerCase()));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("content")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern)
            ));
        }

        Page<Passage> passages = passageRepository.findAll(spec, pageable);
        List<UUID> passageIds = passages.getContent().stream().map(Passage::getId).toList();

        Map<UUID, List<Question>> questionsByPassage = passageIds.isEmpty() ? Map.of() :
                questionRepository.findByPassageIdIn(passageIds).stream()
                        .collect(Collectors.groupingBy(Question::getPassageId));

        return passages.map(p -> toResponse(p, questionsByPassage.getOrDefault(p.getId(), List.of())));
    }

    /**
     * Deletes a passage and its sub-questions if in DRAFT state.
     */
    public void deletePassage(UUID passageId, UUID authorId, String tenantId) {
        Passage passage = passageRepository.findByIdAndTenantId(passageId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Passage not found: " + passageId));

        if (!"DRAFT".equals(passage.getState())) {
            throw new IllegalStateException("Only DRAFT passages can be deleted. Current state: " + passage.getState());
        }

        List<Question> subQuestions = questionRepository.findByPassageIdOrderByPassageOrderIndexAsc(passageId);
        questionRepository.deleteAll(subQuestions);
        passageRepository.delete(passage);

        log.info("Deleted passage id={} and {} sub-questions by author={} tenant={}",
                passageId, subQuestions.size(), authorId, tenantId);
    }

    private Question buildSubQuestion(
            SubQuestionRequest subReq,
            Passage passage,
            Subject subject,
            String topicName,
            UUID authorId,
            String tenantId,
            int orderIndex
    ) {
        String answerKey = subReq.getAnswerKey();
        List<QuestionOption> options = subReq.getOptions();
        if (options != null && !options.isEmpty()) {
            String[] ids = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setId(ids[i]);
            }
            try {
                answerKey = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
            } catch (Exception e) {
                log.warn("Failed to serialize options: {}", e.getMessage());
            }
        }

        boolean hasImages = subReq.isHasImages() || QuestionService.detectHasImages(
                subReq.getContent(), subReq.getExplanation(), options);

        Question q = Question.builder()
                .subjectId(subject.getId())
                .topicId(passage.getTopicId() != null ? passage.getTopicId() : 0L)
                .subject(subject.getName())
                .topic(topicName != null ? topicName : passage.getTopic())
                .chapter(subReq.getChapter())
                .difficulty(subReq.getDifficulty() != null ? subReq.getDifficulty().name() : "MEDIUM")
                .cognitiveLevel(subReq.getCognitiveLevel() != null ? subReq.getCognitiveLevel().name() : "UNDERSTAND")
                .questionType(subReq.getQuestionType() != null ? subReq.getQuestionType().name() : QuestionType.SINGLE_MCQ.name())
                .content(subReq.getContent())
                .answerKey(answerKey)
                .options(options)
                .explanation(subReq.getExplanation())
                .references(subReq.getReferences())
                .hasImages(hasImages)
                .state(passage.getState())
                .passageId(passage.getId())
                .passageOrderIndex(subReq.getPassageOrderIndex() != null ? subReq.getPassageOrderIndex() : orderIndex)
                .authorId(authorId)
                .build();
        q.setTenantId(tenantId);
        return q;
    }

    private void updateSubQuestionFields(
            Question existing,
            SubQuestionRequest subReq,
            Passage passage,
            Subject subject,
            String topicName,
            int orderIndex
    ) {
        String answerKey = subReq.getAnswerKey();
        List<QuestionOption> options = subReq.getOptions();
        if (options != null && !options.isEmpty()) {
            String[] ids = {"A", "B", "C", "D", "E", "F"};
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setId(ids[i]);
            }
            try {
                answerKey = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(options);
            } catch (Exception ignored) {}
        }

        boolean hasImages = subReq.isHasImages() || QuestionService.detectHasImages(
                subReq.getContent(), subReq.getExplanation(), options);

        existing.setSubjectId(subject.getId());
        existing.setSubject(subject.getName());
        if (passage.getTopicId() != null) {
            existing.setTopicId(passage.getTopicId());
        }
        if (topicName != null) {
            existing.setTopic(topicName);
        }
        existing.setChapter(subReq.getChapter());
        if (subReq.getDifficulty() != null) existing.setDifficulty(subReq.getDifficulty().name());
        if (subReq.getCognitiveLevel() != null) existing.setCognitiveLevel(subReq.getCognitiveLevel().name());
        if (subReq.getQuestionType() != null) existing.setQuestionType(subReq.getQuestionType().name());
        existing.setContent(subReq.getContent());
        existing.setAnswerKey(answerKey);
        existing.setOptions(options);
        existing.setExplanation(subReq.getExplanation());
        existing.setReferences(subReq.getReferences());
        existing.setHasImages(hasImages);
        existing.setPassageOrderIndex(subReq.getPassageOrderIndex() != null ? subReq.getPassageOrderIndex() : orderIndex);
    }

    public PassageResponse toResponse(Passage passage, List<Question> subQuestions) {
        LocalDateTime createdAt = passage.getCreatedAt() != null
                ? LocalDateTime.ofInstant(passage.getCreatedAt(), ZoneOffset.UTC)
                : null;
        LocalDateTime updatedAt = passage.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(passage.getUpdatedAt(), ZoneOffset.UTC)
                : null;

        List<QuestionResponse> subResponses = (subQuestions != null) ?
                subQuestions.stream().map(questionService::toResponse).toList() : List.of();

        return PassageResponse.builder()
                .id(passage.getId())
                .tenantId(passage.getTenantId())
                .title(passage.getTitle())
                .content(passage.getContent())
                .contentFormat(passage.getContentFormat())
                .subjectId(passage.getSubjectId())
                .topicId(passage.getTopicId())
                .subject(passage.getSubject())
                .topic(passage.getTopic())
                .hasImages(passage.isHasImages())
                .state(passage.getState())
                .authorId(passage.getAuthorId())
                .reviewerId(passage.getReviewerId())
                .encryptionKeyId(passage.getEncryptionKeyId())
                .subQuestions(subResponses)
                .subQuestionCount(subResponses.size())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(passage.getVersion())
                .build();
    }

    private static boolean detectPassageImages(String text) {
        if (text == null || text.isBlank()) return false;
        return text.contains("<img") || text.contains("<svg") || text.contains("data:image/") || text.matches("(?s).*!\\[.*?\\]\\(.*?\\).*");
    }

    private void publishAuditEvent(String eventType, UUID passageId, UUID actorId,
                                   String tenantId, Map<String, Object> extra) {
        try {
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventType", eventType);
            event.put("passageId", passageId.toString());
            event.put("actorId", actorId.toString());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            if (extra != null) {
                event.putAll(extra);
            }
            eventPublisher.publish(AUDIT_TOPIC, passageId.toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", eventType, e.getMessage());
        }
    }
}
