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

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.QuestionVersion;
import com.examplatform.questionbank.repository.QuestionVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages question version history.
 * On every update to question content/metadata, creates a QuestionVersion record
 * with the authorId, changedAt timestamp, JSON diff of modified fields,
 * and an encrypted full snapshot.
 *
 * Validates: Requirements 4.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionVersioningService {

    private final QuestionVersionRepository questionVersionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new version record capturing the diff between old and new question states.
     *
     * @param oldQuestion the question state before the update
     * @param newQuestion the question state after the update
     * @param authorId    UUID of the user who made the change
     * @param tenantId    tenant identifier
     * @return the persisted QuestionVersion
     */
    public QuestionVersion createVersion(Question oldQuestion, Question newQuestion, UUID authorId, String tenantId) {
        String diffJson = computeDiff(oldQuestion, newQuestion);
        String snapshotJson = serializeSnapshot(newQuestion);

        int nextVersionNumber = questionVersionRepository
                .findTopByQuestionIdOrderByVersionNumberDesc(newQuestion.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        QuestionVersion version = QuestionVersion.builder()
                .questionId(newQuestion.getId())
                .authorId(authorId)
                .changedAt(Instant.now())
                .diffJson(diffJson)
                .snapshotJson(snapshotJson)
                .versionNumber(nextVersionNumber)
                .build();
        version.setTenantId(tenantId);

        QuestionVersion saved = questionVersionRepository.save(version);

        log.info("Created version {} for question {}, author={}, fields changed={}",
                nextVersionNumber, newQuestion.getId(), authorId, diffJson);

        return saved;
    }

    /**
     * Returns all versions for a given question, ordered by version number descending.
     *
     * @param questionId the question UUID
     * @return list of versions (newest first)
     */
    @Transactional(readOnly = true)
    public List<QuestionVersion> getVersions(UUID questionId) {
        return questionVersionRepository.findByQuestionIdOrderByVersionNumberDesc(questionId);
    }

    /**
     * Computes a JSON diff between old and new question states.
     * Format: {"field": {"old": "...", "new": "..."}} for each changed field.
     */
    String computeDiff(Question oldQuestion, Question newQuestion) {
        Map<String, Map<String, String>> diff = new LinkedHashMap<>();

        addIfChanged(diff, "subject", oldQuestion.getSubject(), newQuestion.getSubject());
        addIfChanged(diff, "topic", oldQuestion.getTopic(), newQuestion.getTopic());
        addIfChanged(diff, "subtopic", oldQuestion.getSubtopic(), newQuestion.getSubtopic());
        addIfChanged(diff, "chapter", oldQuestion.getChapter(), newQuestion.getChapter());
        addIfChanged(diff, "difficulty", oldQuestion.getDifficulty(), newQuestion.getDifficulty());
        addIfChanged(diff, "cognitiveLevel", oldQuestion.getCognitiveLevel(), newQuestion.getCognitiveLevel());
        addIfChanged(diff, "questionType", oldQuestion.getQuestionType(), newQuestion.getQuestionType());
        addIfChanged(diff, "content", oldQuestion.getContent(), newQuestion.getContent());
        addIfChanged(diff, "answerKey", oldQuestion.getAnswerKey(), newQuestion.getAnswerKey());

        try {
            return objectMapper.writeValueAsString(diff);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize diff JSON", e);
            return "{}";
        }
    }

    private void addIfChanged(Map<String, Map<String, String>> diff, String field, String oldVal, String newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("old", oldVal);
            change.put("new", newVal);
            diff.put(field, change);
        }
    }

    /**
     * Serializes the full question state as a JSON snapshot.
     */
    String serializeSnapshot(Question question) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", question.getId() != null ? question.getId().toString() : null);
            node.put("subject", question.getSubject());
            node.put("topic", question.getTopic());
            node.put("subtopic", question.getSubtopic());
            node.put("chapter", question.getChapter());
            node.put("difficulty", question.getDifficulty());
            node.put("cognitiveLevel", question.getCognitiveLevel());
            node.put("questionType", question.getQuestionType());
            node.put("content", question.getContent());
            node.put("answerKey", question.getAnswerKey());
            node.put("state", question.getState());
            node.put("authorId", question.getAuthorId() != null ? question.getAuthorId().toString() : null);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize question snapshot", e);
            return "{}";
        }
    }
}
