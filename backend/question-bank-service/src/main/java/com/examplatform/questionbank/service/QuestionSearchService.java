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
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Full-text search service for questions.
 * Falls back to JPA LIKE queries when OpenSearch is unavailable.
 * Production deployment should use OpenSearch for sub-2-second p95 response.
 *
 * Validates: Requirements 19.3, 26.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSearchService {

    private final QuestionRepository questionRepository;

    /**
     * Search questions by query text, subject, and difficulty.
     * Currently uses JPA fallback with LIKE matching on subject/topic/content.
     * Production: replace with OpenSearch client call for 100M+ question support.
     *
     * @param query      free-text search query
     * @param subject    optional subject filter
     * @param difficulty optional difficulty filter
     * @param page       page number (0-based)
     * @param size       page size
     * @param tenantId   tenant identifier
     * @return paginated question results
     */
    public Page<QuestionResponse> search(String query, String subject, String difficulty,
                                         int page, int size, String tenantId) {
        log.debug("Searching questions: query='{}', subject='{}', difficulty='{}', tenant={}",
                query, subject, difficulty, tenantId);

        // JPA fallback: filter by subject and state=PUBLISHED, then apply LIKE on topic/content
        List<Question> allMatches = questionRepository
                .findBySubjectAndStateAndTenantId(
                        subject != null ? subject : "%",
                        "PUBLISHED",
                        tenantId);

        // Apply in-memory filtering for query and difficulty
        String lowerQuery = query != null ? query.toLowerCase() : "";
        List<Question> filtered = allMatches.stream()
                .filter(q -> difficulty == null || difficulty.isBlank()
                        || difficulty.equalsIgnoreCase(q.getDifficulty()))
                .filter(q -> lowerQuery.isBlank()
                        || matchesQuery(q, lowerQuery))
                .collect(Collectors.toList());

        // Paginate
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<QuestionResponse> pageContent = filtered.subList(
                        Math.min(start, filtered.size()),
                        Math.min(end, filtered.size()))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
    }

    private boolean matchesQuery(Question q, String lowerQuery) {
        return containsIgnoreCase(q.getSubject(), lowerQuery)
                || containsIgnoreCase(q.getTopic(), lowerQuery)
                || containsIgnoreCase(q.getContent(), lowerQuery)
                || containsIgnoreCase(q.getSubtopic(), lowerQuery);
    }

    private boolean containsIgnoreCase(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    private QuestionResponse toResponse(Question question) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneId.systemDefault())
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
