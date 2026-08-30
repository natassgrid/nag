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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Handles question updates with automatic versioning.
 * On every update, the old state is captured, the question is updated,
 * and a QuestionVersion record is created with the diff.
 *
 * Validates: Requirements 4.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionUpdateService {

    private final QuestionRepository questionRepository;
    private final QuestionVersioningService questionVersioningService;
    private final QuestionService questionService;
    private final EmbeddingService embeddingService;

    /**
     * Updates an existing question and creates a version record tracking the changes.
     *
     * @param questionId the question UUID to update
     * @param request    the update payload
     * @param authorId   UUID of the user performing the update
     * @param tenantId   tenant identifier
     * @return the updated question response
     * @throws EntityNotFoundException if the question does not exist
     */
    public QuestionResponse updateQuestion(UUID questionId, CreateQuestionRequest request, UUID authorId, String tenantId) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        // Clone current state for diff comparison
        Question oldState = cloneQuestionState(existing);

        // Resolve and validate the numeric hierarchy, then update id + denormalized names
        QuestionService.ResolvedHierarchy hierarchy = questionService.resolveHierarchy(request, tenantId);
        existing.setSubjectId(hierarchy.subjectId());
        existing.setTopicId(hierarchy.topicId());
        existing.setSubtopicId(hierarchy.subtopicId());
        existing.setSubject(hierarchy.subjectName());
        existing.setTopic(hierarchy.topicName());
        existing.setSubtopic(hierarchy.subtopicName());
        existing.setChapter(request.getChapter());
        existing.setDifficulty(request.getDifficulty().name());
        existing.setCognitiveLevel(request.getCognitiveLevel().name());
        existing.setQuestionType(request.getQuestionType().name());
        existing.setContent(request.getContent());
        existing.setAnswerKey(request.getAnswerKey());

        // Save updated question
        Question updated = questionRepository.save(existing);

        // Regenerate embedding if content changed (keeps similarity search accurate)
        // NFR-2: If LLM/embedding service is unavailable, update still succeeds without new embedding
        if (!java.util.Objects.equals(oldState.getContent(), updated.getContent())) {
            try {
                float[] embedding = embeddingService.embed(updated.getContent());
                if (embedding != null && embedding.length > 0) {
                    questionRepository.updateEmbedding(updated.getId(), EmbeddingUtils.embeddingToString(embedding));
                    log.debug("Embedding regenerated for updated question: id={}", updated.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to regenerate embedding for question id={}. " +
                        "Update succeeded without new embedding. Reason: {}", updated.getId(), e.getMessage());
            }
        }

        // Create version record
        questionVersioningService.createVersion(oldState, updated, authorId, tenantId);

        log.info("Question updated: id={}, author={}, tenant={}", questionId, authorId, tenantId);

        return toResponse(updated);
    }

    private Question cloneQuestionState(Question source) {
        return Question.builder()
                .subjectId(source.getSubjectId())
                .topicId(source.getTopicId())
                .subtopicId(source.getSubtopicId())
                .subject(source.getSubject())
                .topic(source.getTopic())
                .subtopic(source.getSubtopic())
                .chapter(source.getChapter())
                .difficulty(source.getDifficulty())
                .cognitiveLevel(source.getCognitiveLevel())
                .questionType(source.getQuestionType())
                .content(source.getContent())
                .answerKey(source.getAnswerKey())
                .state(source.getState())
                .authorId(source.getAuthorId())
                .build();
    }

    private QuestionResponse toResponse(Question question) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneOffset.UTC)
                : null;

        return QuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .subject(question.getSubject())
                .topic(question.getTopic())
                .subtopic(question.getSubtopic())
                .chapter(question.getChapter())
                .difficulty(question.getDifficulty())
                .cognitiveLevel(question.getCognitiveLevel())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .answerKey(question.getAnswerKey())
                .explanation(question.getExplanation())
                .references(question.getReferences())
                .state(question.getState())
                .authorId(question.getAuthorId())
                .createdAt(createdAt)
                .build();
    }
}
