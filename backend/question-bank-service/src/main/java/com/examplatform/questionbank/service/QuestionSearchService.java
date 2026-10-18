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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNames;
import com.examplatform.shared.taxonomy.TaxonomyNodeRef;
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
 * Falls back to JPA queries when OpenSearch is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSearchService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TaxonomyClient taxonomyClient;

    /**
     * Search questions by query text, subject, and difficulty.
     */
    public Page<QuestionResponse> search(String query, String subject, String difficulty,
                                         int page, int size, String tenantId) {
        log.debug("Searching questions: query='{}', subject='{}', difficulty='{}', tenant={}",
                query, subject, difficulty, tenantId);

        List<Question> allMatches;
        Long subjectId = null;
        if (subject != null && !subject.isBlank() && !"%".equals(subject)) {
            try {
                subjectId = Long.parseLong(subject.trim());
            } catch (NumberFormatException e) {
                subjectId = subjectRepository.findByNameAndTenantId(subject.trim(), tenantId)
                        .or(() -> subjectRepository.findByNameAndTenantIdIsNull(subject.trim()))
                        .map(Subject::getId)
                        .orElse(null);
            }
        }

        if (subjectId != null) {
            allMatches = questionRepository.findBySubjectIdAndStateAndTenantId(
                    subjectId, QuestionStateConstants.PUBLISHED, tenantId);
        } else {
            allMatches = questionRepository.findByTenantId(tenantId).stream()
                    .filter(q -> QuestionStateConstants.PUBLISHED.equals(q.getState()))
                    .toList();
        }

        // Apply in-memory filtering for query and difficulty
        String lowerQuery = query != null ? query.toLowerCase() : "";
        List<Question> filtered = allMatches.stream()
                .filter(q -> difficulty == null || difficulty.isBlank()
                        || difficulty.equalsIgnoreCase(q.getDifficulty()))
                .filter(q -> lowerQuery.isBlank() || matchesQuery(q, lowerQuery))
                .toList();

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
        String cleanQuery = lowerQuery.trim();
        if (matchesAnyField(q, cleanQuery)) {
            return true;
        }

        String[] tokens = cleanQuery.split("\\s+");
        if (tokens.length > 1) {
            for (String token : tokens) {
                if (!token.isBlank() && !matchesAnyField(q, token)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean matchesAnyField(Question q, String queryToken) {
        return containsIgnoreCase(q.getChapter(), queryToken)
                || containsIgnoreCase(q.getDifficulty(), queryToken)
                || containsIgnoreCase(q.getCognitiveLevel(), queryToken)
                || containsIgnoreCase(q.getQuestionType(), queryToken)
                || containsIgnoreCase(q.getContent(), queryToken)
                || containsIgnoreCase(q.getExplanation(), queryToken)
                || containsIgnoreCase(q.getState(), queryToken)
                || containsIgnoreCase(q.getSourceReferences(), queryToken);
    }

    private boolean containsIgnoreCase(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    private QuestionResponse toResponse(Question question) {
        LocalDateTime createdAt = question.getCreatedAt() != null
                ? LocalDateTime.ofInstant(question.getCreatedAt(), ZoneId.systemDefault())
                : null;

        TaxonomyNames names = null;
        if (taxonomyClient != null) {
            names = taxonomyClient.resolveNames(new TaxonomyNodeRef(question.getSubjectId(), question.getTopicId(), question.getSubtopicId()), question.getTenantId());
        }
        if (names == null) {
            names = new TaxonomyNames(null, null, null);
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .subjectId(question.getSubjectId())
                .topicId(question.getTopicId())
                .subtopicId(question.getSubtopicId())
                .subject(names.subjectName())
                .topic(names.topicName())
                .subtopic(names.subtopicName())
                .chapter(question.getChapter())
                .difficulty(question.getDifficulty())
                .cognitiveLevel(question.getCognitiveLevel())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .answerKey(question.getAnswerKey())
                .explanation(question.getExplanation())
                .sourceReferences(question.getSourceReferences())
                .state(question.getState())
                .authorId(question.getAuthorId())
                .createdAt(createdAt)
                .options(question.getOptions())
                .hasImages(question.isHasImages())
                .build();
    }
}
