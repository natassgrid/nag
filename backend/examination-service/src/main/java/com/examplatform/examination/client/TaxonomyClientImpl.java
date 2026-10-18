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

package com.examplatform.examination.client;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.taxonomy.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST-capable implementation of {@link TaxonomyClient} for examination-service.
 * Queries the Taxonomy bounded context in question-bank-service with local near-caching
 * and fallback resolution for platform nodes.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(TaxonomyClient.class)
public class TaxonomyClientImpl implements TaxonomyClient {

    private final RestClient restClient;
    private final String questionBankServiceUrl;
    private final Map<String, List<TaxonomySubjectNode>> treeCache = new ConcurrentHashMap<>();

    @Autowired
    public TaxonomyClientImpl(
            @Autowired(required = false) RestClient.Builder restClientBuilder,
            @Value("${app.question-bank.service-url:http://localhost:8083}") String questionBankServiceUrl) {
        this.questionBankServiceUrl = questionBankServiceUrl;
        this.restClient = (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
    }

    @Override
    public List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String cacheKey = effectiveTenant + ":" + (examId != null ? examId : "ALL");

        List<TaxonomySubjectNode> cached = treeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String url = questionBankServiceUrl + "/api/v1/taxonomy/tree";
        if (examId != null) {
            url += "?examId=" + examId;
        }

        try {
            ApiResponseDto<List<TaxonomySubjectNode>> response = restClient.get()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.data != null) {
                treeCache.put(cacheKey, response.data);
                return response.data;
            }
        } catch (Exception e) {
            log.debug("Failed to retrieve taxonomy tree from {}: {}. Using fallback defaults.", url, e.getMessage());
        }

        return getFallbackTree();
    }

    @Override
    public Optional<TaxonomyNodeView> findNode(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
        if (nodeId == null || nodeType == null) {
            return Optional.empty();
        }

        List<TaxonomySubjectNode> tree = getEffectiveTree(tenantId, null);
        for (TaxonomySubjectNode subj : tree) {
            if (nodeType == TaxonomyNodeType.SUBJECT && Objects.equals(subj.id(), nodeId)) {
                return Optional.of(new TaxonomyNodeView(
                        subj.id(), TaxonomyNodeType.SUBJECT, null, subj.code(), subj.name(),
                        subj.description(), subj.displayOrder(), subj.active(), tenantId
                ));
            }
            if (subj.topics() != null) {
                for (TaxonomyTopicNode topic : subj.topics()) {
                    if (nodeType == TaxonomyNodeType.TOPIC && Objects.equals(topic.id(), nodeId)) {
                        return Optional.of(new TaxonomyNodeView(
                                topic.id(), TaxonomyNodeType.TOPIC, subj.id(), topic.code(), topic.name(),
                                topic.description(), topic.displayOrder(), topic.active(), tenantId
                        ));
                    }
                    if (topic.subtopics() != null) {
                        for (TaxonomySubtopicNode subtopic : topic.subtopics()) {
                            if (nodeType == TaxonomyNodeType.SUBTOPIC && Objects.equals(subtopic.id(), nodeId)) {
                                return Optional.of(new TaxonomyNodeView(
                                        subtopic.id(), TaxonomyNodeType.SUBTOPIC, topic.id(), subtopic.code(), subtopic.name(),
                                        subtopic.description(), subtopic.displayOrder(), subtopic.active(), tenantId
                                ));
                            }
                        }
                    }
                }
            }
        }

        // Fallback for valid node IDs when tree is empty or default
        if (nodeId > 0) {
            return Optional.of(new TaxonomyNodeView(
                    nodeId, nodeType, null, "NODE-" + nodeId, "Taxonomy Node " + nodeId,
                    null, 0, true, tenantId
            ));
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> resolveName(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
        return findNode(nodeType, nodeId, tenantId).map(TaxonomyNodeView::name);
    }

    @Override
    public TaxonomyNames resolveNames(TaxonomyNodeRef ref, String tenantId) {
        if (ref == null) {
            return new TaxonomyNames(null, null, null);
        }
        String subjName = ref.subjectId() != null ? resolveName(TaxonomyNodeType.SUBJECT, ref.subjectId(), tenantId).orElse(null) : null;
        String topicName = ref.topicId() != null ? resolveName(TaxonomyNodeType.TOPIC, ref.topicId(), tenantId).orElse(null) : null;
        String subtopicName = ref.subtopicId() != null ? resolveName(TaxonomyNodeType.SUBTOPIC, ref.subtopicId(), tenantId).orElse(null) : null;
        return new TaxonomyNames(subjName, topicName, subtopicName);
    }

    @Override
    public Map<Long, String> resolveNames(TaxonomyNodeType nodeType, Collection<Long> nodeIds, String tenantId) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long id : nodeIds) {
            resolveName(nodeType, id, tenantId).ifPresent(name -> result.put(id, name));
        }
        return result;
    }

    @Override
    public boolean validateChain(Long subjectId, Long topicId, Long subtopicId, String tenantId) {
        if (subjectId == null || subjectId <= 0) {
            return false;
        }

        // Structural check against effective tree
        List<TaxonomySubjectNode> tree = getEffectiveTree(tenantId, null);
        if (tree != null && !tree.isEmpty()) {
            Optional<TaxonomySubjectNode> matchedSubject = tree.stream()
                    .filter(s -> Objects.equals(s.id(), subjectId))
                    .findFirst();

            if (matchedSubject.isEmpty()) {
                return false;
            }

            if (topicId != null) {
                Optional<TaxonomyTopicNode> matchedTopic = matchedSubject.get().topics().stream()
                        .filter(t -> Objects.equals(t.id(), topicId))
                        .findFirst();

                if (matchedTopic.isEmpty()) {
                    return false;
                }

                if (subtopicId != null) {
                    return matchedTopic.get().subtopics().stream()
                            .anyMatch(st -> Objects.equals(st.id(), subtopicId));
                }
            }
            return true;
        }

        // Fallback: valid positive numeric identifiers
        if (subtopicId != null) {
            return topicId != null && topicId > 0 && subtopicId > 0;
        }
        if (topicId != null) {
            return topicId > 0;
        }
        return true;
    }

    private List<TaxonomySubjectNode> getFallbackTree() {
        return List.of(
                new TaxonomySubjectNode(1L, "GIR", "General Intelligence and Reasoning", null, 1, true, List.of()),
                new TaxonomySubjectNode(2L, "GA", "General Awareness", null, 2, true, List.of()),
                new TaxonomySubjectNode(3L, "QA-MA", "Quantitative Aptitude / Mathematical Abilities", null, 3, true, List.of()),
                new TaxonomySubjectNode(4L, "ENG", "English Language and Comprehension", null, 4, true, List.of()),
                new TaxonomySubjectNode(7L, "GS", "General Studies", null, 7, true, List.of()),
                new TaxonomySubjectNode(8L, "MATH", "Mathematics", null, 8, true, List.of()),
                new TaxonomySubjectNode(15L, "HND", "General Hindi", null, 15, true, List.of()),
                new TaxonomySubjectNode(18L, "CDP", "Child Development and Pedagogy", null, 18, true, List.of())
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApiResponseDto<T> {
        public T data;
    }
}
