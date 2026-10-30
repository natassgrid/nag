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

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.TaxonomyTenantOverride;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TaxonomyTenantOverrideRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.shared.taxonomy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * In-process implementation of {@link TaxonomyClient} for question-bank-service and monolith/macro deployments.
 * Merges platform-default nodes ({@code tenant_id IS NULL}) with tenant overrides and custom tenant nodes.
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonomyClientImpl implements TaxonomyClient {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final TaxonomyTenantOverrideRepository overrideRepository;

    private record TaxonomyNodes(
            List<Subject> subjects,
            List<Topic> topics,
            List<Subtopic> subtopics
    ) {}

    private record Overrides(
            Map<Long, TaxonomyTenantOverride> subjects,
            Map<Long, TaxonomyTenantOverride> topics,
            Map<Long, TaxonomyTenantOverride> subtopics
    ) {
        static Overrides empty() {
            return new Overrides(Map.of(), Map.of(), Map.of());
        }
    }

    @Override
    public List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId) {
        TaxonomyNodes nodes = loadTaxonomyNodes(tenantId);
        Overrides overrides = loadOverrides(tenantId);

        Map<Long, List<TaxonomySubtopicNode>> subtopicsByTopic =
                assembleSubtopicsByTopic(nodes.subtopics(), overrides.subtopics());
        Map<Long, List<TaxonomyTopicNode>> topicsBySubject =
                assembleTopicsBySubject(nodes.topics(), overrides.topics(), subtopicsByTopic);

        return assembleSubjects(nodes.subjects(), overrides.subjects(), topicsBySubject);
    }

    private TaxonomyNodes loadTaxonomyNodes(String tenantId) {
        List<Subject> subjects = new ArrayList<>(subjectRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc());
        List<Long> platformSubjectIds = subjects.stream().map(Subject::getId).toList();

        List<Topic> topics = new ArrayList<>(platformSubjectIds.isEmpty() ? List.of()
                : topicRepository.findBySubjectIdInAndTenantIdIsNull(platformSubjectIds));
        List<Long> platformTopicIds = topics.stream().map(Topic::getId).toList();

        List<Subtopic> subtopics = new ArrayList<>(platformTopicIds.isEmpty() ? List.of()
                : subtopicRepository.findByTopicIdInAndTenantIdIsNull(platformTopicIds));

        if (tenantId != null && !tenantId.isBlank()) {
            List<Subject> tenantSubjects = subjectRepository.findByTenantId(tenantId);
            subjects.addAll(tenantSubjects);

            List<Long> allSubjectIds = subjects.stream().map(Subject::getId).toList();
            List<Topic> tenantTopics = topicRepository.findBySubjectIdInAndTenantId(allSubjectIds, tenantId);
            topics.addAll(tenantTopics);

            List<Long> allTopicIds = topics.stream().map(Topic::getId).toList();
            List<Subtopic> tenantSubtopics = subtopicRepository.findByTopicIdInAndTenantId(allTopicIds, tenantId);
            subtopics.addAll(tenantSubtopics);
        }

        return new TaxonomyNodes(subjects, topics, subtopics);
    }

    private Overrides loadOverrides(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Overrides.empty();
        }
        Map<Long, TaxonomyTenantOverride> subjects = new HashMap<>();
        Map<Long, TaxonomyTenantOverride> topics = new HashMap<>();
        Map<Long, TaxonomyTenantOverride> subtopics = new HashMap<>();
        for (TaxonomyTenantOverride o : overrideRepository.findByTenantId(tenantId)) {
            if (o.getNodeType() == TaxonomyNodeType.SUBJECT) {
                subjects.put(o.getNodeId(), o);
            } else if (o.getNodeType() == TaxonomyNodeType.TOPIC) {
                topics.put(o.getNodeId(), o);
            } else if (o.getNodeType() == TaxonomyNodeType.SUBTOPIC) {
                subtopics.put(o.getNodeId(), o);
            }
        }
        return new Overrides(subjects, topics, subtopics);
    }

    private Map<Long, List<TaxonomySubtopicNode>> assembleSubtopicsByTopic(
            List<Subtopic> allSubtopics,
            Map<Long, TaxonomyTenantOverride> subtopicOverrides) {
        Map<Long, List<TaxonomySubtopicNode>> subtopicsByTopic = new HashMap<>();
        for (Subtopic st : allSubtopics) {
            TaxonomyTenantOverride o = subtopicOverrides.get(st.getId());
            if (st.isActive() && !isHidden(o)) {
                String effName = resolveEffectiveName(o, st.getName());
                Integer effOrder = resolveEffectiveOrder(o, st.getDisplayOrder());

                TaxonomySubtopicNode node = new TaxonomySubtopicNode(
                        st.getId(), st.getTopicId(), st.getCode(), effName, st.getDescription(), effOrder, st.isActive()
                );
                subtopicsByTopic.computeIfAbsent(st.getTopicId(), k -> new ArrayList<>()).add(node);
            }
        }

        subtopicsByTopic.values().forEach(list -> list.sort(
                Comparator.comparing(TaxonomySubtopicNode::displayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaxonomySubtopicNode::name, Comparator.nullsLast(String::compareToIgnoreCase))
        ));
        return subtopicsByTopic;
    }

    private Map<Long, List<TaxonomyTopicNode>> assembleTopicsBySubject(
            List<Topic> allTopics,
            Map<Long, TaxonomyTenantOverride> topicOverrides,
            Map<Long, List<TaxonomySubtopicNode>> subtopicsByTopic) {
        Map<Long, List<TaxonomyTopicNode>> topicsBySubject = new HashMap<>();
        for (Topic t : allTopics) {
            TaxonomyTenantOverride o = topicOverrides.get(t.getId());
            if (t.isActive() && !isHidden(o)) {
                String effName = resolveEffectiveName(o, t.getName());
                Integer effOrder = resolveEffectiveOrder(o, t.getDisplayOrder());

                List<TaxonomySubtopicNode> children = subtopicsByTopic.getOrDefault(t.getId(), List.of());
                TaxonomyTopicNode node = new TaxonomyTopicNode(
                        t.getId(), t.getSubjectId(), t.getCode(), effName, t.getDescription(), effOrder, t.isActive(), children
                );
                topicsBySubject.computeIfAbsent(t.getSubjectId(), k -> new ArrayList<>()).add(node);
            }
        }

        topicsBySubject.values().forEach(list -> list.sort(
                Comparator.comparing(TaxonomyTopicNode::displayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaxonomyTopicNode::name, Comparator.nullsLast(String::compareToIgnoreCase))
        ));
        return topicsBySubject;
    }

    private List<TaxonomySubjectNode> assembleSubjects(
            List<Subject> allSubjects,
            Map<Long, TaxonomyTenantOverride> subjectOverrides,
            Map<Long, List<TaxonomyTopicNode>> topicsBySubject) {
        List<TaxonomySubjectNode> subjectNodes = new ArrayList<>();
        for (Subject s : allSubjects) {
            TaxonomyTenantOverride o = subjectOverrides.get(s.getId());
            if (s.isActive() && !isHidden(o)) {
                String effName = resolveEffectiveName(o, s.getName());
                Integer effOrder = resolveEffectiveOrder(o, s.getDisplayOrder());

                List<TaxonomyTopicNode> children = topicsBySubject.getOrDefault(s.getId(), List.of());
                TaxonomySubjectNode node = new TaxonomySubjectNode(
                        s.getId(), s.getCode(), effName, s.getDescription(), effOrder, s.isActive(), children
                );
                subjectNodes.add(node);
            }
        }

        subjectNodes.sort(
                Comparator.comparing(TaxonomySubjectNode::displayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaxonomySubjectNode::name, Comparator.nullsLast(String::compareToIgnoreCase))
        );
        return subjectNodes;
    }

    @Override
    public Optional<TaxonomyNodeView> findNode(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
        if (nodeType == null || nodeId == null) {
            return Optional.empty();
        }

        return switch (nodeType) {
            case SUBJECT -> findSubjectNode(nodeId, tenantId);
            case TOPIC -> findTopicNode(nodeId, tenantId);
            case SUBTOPIC -> findSubtopicNode(nodeId, tenantId);
        };
    }

    private Optional<TaxonomyNodeView> findSubjectNode(Long nodeId, String tenantId) {
        return subjectRepository.findById(nodeId)
                .filter(Subject::isActive)
                .flatMap(s -> applyOverrideOrBuild(TaxonomyNodeType.SUBJECT, s.getId(), null,
                        s.getCode(), s.getName(), s.getDescription(), s.getDisplayOrder(),
                        true, s.getTenantId(), tenantId));
    }

    private Optional<TaxonomyNodeView> findTopicNode(Long nodeId, String tenantId) {
        return topicRepository.findById(nodeId)
                .filter(Topic::isActive)
                .flatMap(t -> applyOverrideOrBuild(TaxonomyNodeType.TOPIC, t.getId(), t.getSubjectId(),
                        t.getCode(), t.getName(), t.getDescription(), t.getDisplayOrder(),
                        true, t.getTenantId(), tenantId));
    }

    private Optional<TaxonomyNodeView> findSubtopicNode(Long nodeId, String tenantId) {
        return subtopicRepository.findById(nodeId)
                .filter(Subtopic::isActive)
                .flatMap(st -> applyOverrideOrBuild(TaxonomyNodeType.SUBTOPIC, st.getId(), st.getTopicId(),
                        st.getCode(), st.getName(), st.getDescription(), st.getDisplayOrder(),
                        true, st.getTenantId(), tenantId));
    }

    private Optional<TaxonomyNodeView> applyOverrideOrBuild(
            TaxonomyNodeType nodeType, Long id, Long parentId, String code,
            String baseName, String description, Integer baseOrder, boolean active,
            String nodeTenantId, String tenantId) {
        if (nodeTenantId != null && !nodeTenantId.equals(tenantId)) {
            return Optional.empty();
        }

        TaxonomyTenantOverride override = findOverride(tenantId, nodeType, id);
        if (isHidden(override)) {
            return Optional.empty();
        }

        String effName = resolveEffectiveName(override, baseName);
        Integer effOrder = resolveEffectiveOrder(override, baseOrder);

        return Optional.of(new TaxonomyNodeView(id, nodeType, parentId, code,
                effName, description, effOrder, active, nodeTenantId));
    }

    private TaxonomyTenantOverride findOverride(String tenantId, TaxonomyNodeType nodeType, Long nodeId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return overrideRepository.findByTenantIdAndNodeTypeAndNodeId(tenantId, nodeType, nodeId).orElse(null);
    }

    private static boolean isHidden(TaxonomyTenantOverride override) {
        return override != null && override.isHidden();
    }

    private static String resolveEffectiveName(TaxonomyTenantOverride override, String defaultName) {
        if (override != null && override.getNameOverride() != null && !override.getNameOverride().isBlank()) {
            return override.getNameOverride();
        }
        return defaultName;
    }

    private static Integer resolveEffectiveOrder(TaxonomyTenantOverride override, Integer defaultOrder) {
        if (override != null && override.getOrderOverride() != null) {
            return override.getOrderOverride();
        }
        return defaultOrder;
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
        if (nodeType == null || nodeIds == null || nodeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (Long id : nodeIds) {
            if (id != null) {
                resolveName(nodeType, id, tenantId).ifPresent(n -> map.put(id, n));
            }
        }
        return map;
    }

    @Override
    public boolean validateChain(Long subjectId, Long topicId, Long subtopicId, String tenantId) {
        if (subjectId == null || !isValidNode(TaxonomyNodeType.SUBJECT, subjectId, tenantId)) {
            return false;
        }

        if (topicId != null && !isTopicValidForSubject(topicId, subjectId, tenantId)) {
            return false;
        }

        return subtopicId == null || (topicId != null && isSubtopicValidForTopic(subtopicId, topicId, tenantId));
    }

    private boolean isTopicValidForSubject(Long topicId, Long subjectId, String tenantId) {
        if (!isValidNode(TaxonomyNodeType.TOPIC, topicId, tenantId)) {
            return false;
        }
        return topicRepository.findById(topicId)
                .filter(Topic::isActive)
                .map(t -> subjectId.equals(t.getSubjectId()))
                .orElse(false);
    }

    private boolean isSubtopicValidForTopic(Long subtopicId, Long topicId, String tenantId) {
        if (!isValidNode(TaxonomyNodeType.SUBTOPIC, subtopicId, tenantId)) {
            return false;
        }
        return subtopicRepository.findById(subtopicId)
                .filter(Subtopic::isActive)
                .map(st -> topicId.equals(st.getTopicId()))
                .orElse(false);
    }
}
