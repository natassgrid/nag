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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-process implementation of {@link TaxonomyClient} for question-bank-service and monolith/macro deployments.
 * Merges platform-default nodes ({@code tenant_id IS NULL}) with tenant overrides and custom tenant nodes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonomyClientImpl implements TaxonomyClient {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final TaxonomyTenantOverrideRepository overrideRepository;

    @Override
    public List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId) {
        // 1. Fetch platform subjects, topics, and subtopics (tenant_id IS NULL)
        List<Subject> platformSubjects = subjectRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc();
        List<Long> platformSubjectIds = platformSubjects.stream().map(Subject::getId).toList();

        List<Topic> platformTopics = platformSubjectIds.isEmpty() ? List.of()
                : topicRepository.findBySubjectIdInAndTenantIdIsNull(platformSubjectIds);
        List<Long> platformTopicIds = platformTopics.stream().map(Topic::getId).toList();

        List<Subtopic> platformSubtopics = platformTopicIds.isEmpty() ? List.of()
                : subtopicRepository.findByTopicIdInAndTenantIdIsNull(platformTopicIds);

        // 2. Fetch tenant-specific nodes and overrides if a tenantId is provided
        List<Subject> tenantSubjects = Collections.emptyList();
        List<Topic> tenantTopics = Collections.emptyList();
        List<Subtopic> tenantSubtopics = Collections.emptyList();
        Map<Long, TaxonomyTenantOverride> subjectOverrides = new HashMap<>();
        Map<Long, TaxonomyTenantOverride> topicOverrides = new HashMap<>();
        Map<Long, TaxonomyTenantOverride> subtopicOverrides = new HashMap<>();

        if (tenantId != null && !tenantId.isBlank()) {
            tenantSubjects = subjectRepository.findByTenantId(tenantId);
            List<Long> allSubjectIds = new ArrayList<>(platformSubjectIds);
            tenantSubjects.stream().map(Subject::getId).forEach(allSubjectIds::add);

            tenantTopics = topicRepository.findBySubjectIdInAndTenantId(allSubjectIds, tenantId);
            List<Long> allTopicIds = new ArrayList<>(platformTopicIds);
            tenantTopics.stream().map(Topic::getId).forEach(allTopicIds::add);

            tenantSubtopics = subtopicRepository.findByTopicIdInAndTenantId(allTopicIds, tenantId);

            List<TaxonomyTenantOverride> overrides = overrideRepository.findByTenantId(tenantId);
            for (TaxonomyTenantOverride o : overrides) {
                if (o.getNodeType() == TaxonomyNodeType.SUBJECT) {
                    subjectOverrides.put(o.getNodeId(), o);
                } else if (o.getNodeType() == TaxonomyNodeType.TOPIC) {
                    topicOverrides.put(o.getNodeId(), o);
                } else if (o.getNodeType() == TaxonomyNodeType.SUBTOPIC) {
                    subtopicOverrides.put(o.getNodeId(), o);
                }
            }
        }

        // 3. Assemble subtopics map grouped by topicId
        Map<Long, List<TaxonomySubtopicNode>> subtopicsByTopic = new HashMap<>();
        List<Subtopic> allSubtopics = new ArrayList<>(platformSubtopics);
        allSubtopics.addAll(tenantSubtopics);

        for (Subtopic st : allSubtopics) {
            if (!st.isActive()) {
                continue;
            }
            TaxonomyTenantOverride o = subtopicOverrides.get(st.getId());
            if (o != null && o.isHidden()) {
                continue;
            }
            String effName = (o != null && o.getNameOverride() != null && !o.getNameOverride().isBlank())
                    ? o.getNameOverride() : st.getName();
            Integer effOrder = (o != null && o.getOrderOverride() != null)
                    ? o.getOrderOverride() : st.getDisplayOrder();

            TaxonomySubtopicNode node = new TaxonomySubtopicNode(
                    st.getId(), st.getTopicId(), st.getCode(), effName, st.getDescription(), effOrder, st.isActive()
            );
            subtopicsByTopic.computeIfAbsent(st.getTopicId(), k -> new ArrayList<>()).add(node);
        }

        // Sort subtopics
        subtopicsByTopic.values().forEach(list -> list.sort(
                Comparator.comparing(TaxonomySubtopicNode::displayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaxonomySubtopicNode::name, Comparator.nullsLast(String::compareToIgnoreCase))
        ));

        // 4. Assemble topics map grouped by subjectId
        Map<Long, List<TaxonomyTopicNode>> topicsBySubject = new HashMap<>();
        List<Topic> allTopics = new ArrayList<>(platformTopics);
        allTopics.addAll(tenantTopics);

        for (Topic t : allTopics) {
            if (!t.isActive()) {
                continue;
            }
            TaxonomyTenantOverride o = topicOverrides.get(t.getId());
            if (o != null && o.isHidden()) {
                continue;
            }
            String effName = (o != null && o.getNameOverride() != null && !o.getNameOverride().isBlank())
                    ? o.getNameOverride() : t.getName();
            Integer effOrder = (o != null && o.getOrderOverride() != null)
                    ? o.getOrderOverride() : t.getDisplayOrder();

            List<TaxonomySubtopicNode> children = subtopicsByTopic.getOrDefault(t.getId(), List.of());
            TaxonomyTopicNode node = new TaxonomyTopicNode(
                    t.getId(), t.getSubjectId(), t.getCode(), effName, t.getDescription(), effOrder, t.isActive(), children
            );
            topicsBySubject.computeIfAbsent(t.getSubjectId(), k -> new ArrayList<>()).add(node);
        }

        // Sort topics
        topicsBySubject.values().forEach(list -> list.sort(
                Comparator.comparing(TaxonomyTopicNode::displayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TaxonomyTopicNode::name, Comparator.nullsLast(String::compareToIgnoreCase))
        ));

        // 5. Assemble subjects
        List<Subject> allSubjects = new ArrayList<>(platformSubjects);
        allSubjects.addAll(tenantSubjects);

        List<TaxonomySubjectNode> subjectNodes = new ArrayList<>();
        for (Subject s : allSubjects) {
            if (!s.isActive()) {
                continue;
            }
            TaxonomyTenantOverride o = subjectOverrides.get(s.getId());
            if (o != null && o.isHidden()) {
                continue;
            }
            String effName = (o != null && o.getNameOverride() != null && !o.getNameOverride().isBlank())
                    ? o.getNameOverride() : s.getName();
            Integer effOrder = (o != null && o.getOrderOverride() != null)
                    ? o.getOrderOverride() : s.getDisplayOrder();

            List<TaxonomyTopicNode> children = topicsBySubject.getOrDefault(s.getId(), List.of());
            TaxonomySubjectNode node = new TaxonomySubjectNode(
                    s.getId(), s.getCode(), effName, s.getDescription(), effOrder, s.isActive(), children
            );
            subjectNodes.add(node);
        }

        // Sort subjects
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

        switch (nodeType) {
            case SUBJECT: {
                Optional<Subject> opt = subjectRepository.findById(nodeId);
                if (opt.isEmpty() || !opt.get().isActive()) {
                    return Optional.empty();
                }
                Subject s = opt.get();
                if (s.getTenantId() != null && (tenantId == null || !s.getTenantId().equals(tenantId))) {
                    return Optional.empty();
                }
                if (tenantId != null && !tenantId.isBlank()) {
                    Optional<TaxonomyTenantOverride> ov = overrideRepository
                            .findByTenantIdAndNodeTypeAndNodeId(tenantId, TaxonomyNodeType.SUBJECT, nodeId);
                    if (ov.isPresent()) {
                        TaxonomyTenantOverride o = ov.get();
                        if (o.isHidden()) {
                            return Optional.empty();
                        }
                        String name = (o.getNameOverride() != null && !o.getNameOverride().isBlank())
                                ? o.getNameOverride() : s.getName();
                        Integer order = o.getOrderOverride() != null ? o.getOrderOverride() : s.getDisplayOrder();
                        return Optional.of(new TaxonomyNodeView(s.getId(), TaxonomyNodeType.SUBJECT, null, s.getCode(),
                                name, s.getDescription(), order, s.isActive(), s.getTenantId()));
                    }
                }
                return Optional.of(new TaxonomyNodeView(s.getId(), TaxonomyNodeType.SUBJECT, null, s.getCode(),
                        s.getName(), s.getDescription(), s.getDisplayOrder(), s.isActive(), s.getTenantId()));
            }
            case TOPIC: {
                Optional<Topic> opt = topicRepository.findById(nodeId);
                if (opt.isEmpty() || !opt.get().isActive()) {
                    return Optional.empty();
                }
                Topic t = opt.get();
                if (t.getTenantId() != null && (tenantId == null || !t.getTenantId().equals(tenantId))) {
                    return Optional.empty();
                }
                if (tenantId != null && !tenantId.isBlank()) {
                    Optional<TaxonomyTenantOverride> ov = overrideRepository
                            .findByTenantIdAndNodeTypeAndNodeId(tenantId, TaxonomyNodeType.TOPIC, nodeId);
                    if (ov.isPresent()) {
                        TaxonomyTenantOverride o = ov.get();
                        if (o.isHidden()) {
                            return Optional.empty();
                        }
                        String name = (o.getNameOverride() != null && !o.getNameOverride().isBlank())
                                ? o.getNameOverride() : t.getName();
                        Integer order = o.getOrderOverride() != null ? o.getOrderOverride() : t.getDisplayOrder();
                        return Optional.of(new TaxonomyNodeView(t.getId(), TaxonomyNodeType.TOPIC, t.getSubjectId(), t.getCode(),
                                name, t.getDescription(), order, t.isActive(), t.getTenantId()));
                    }
                }
                return Optional.of(new TaxonomyNodeView(t.getId(), TaxonomyNodeType.TOPIC, t.getSubjectId(), t.getCode(),
                        t.getName(), t.getDescription(), t.getDisplayOrder(), t.isActive(), t.getTenantId()));
            }
            case SUBTOPIC: {
                Optional<Subtopic> opt = subtopicRepository.findById(nodeId);
                if (opt.isEmpty() || !opt.get().isActive()) {
                    return Optional.empty();
                }
                Subtopic st = opt.get();
                if (st.getTenantId() != null && (tenantId == null || !st.getTenantId().equals(tenantId))) {
                    return Optional.empty();
                }
                if (tenantId != null && !tenantId.isBlank()) {
                    Optional<TaxonomyTenantOverride> ov = overrideRepository
                            .findByTenantIdAndNodeTypeAndNodeId(tenantId, TaxonomyNodeType.SUBTOPIC, nodeId);
                    if (ov.isPresent()) {
                        TaxonomyTenantOverride o = ov.get();
                        if (o.isHidden()) {
                            return Optional.empty();
                        }
                        String name = (o.getNameOverride() != null && !o.getNameOverride().isBlank())
                                ? o.getNameOverride() : st.getName();
                        Integer order = o.getOrderOverride() != null ? o.getOrderOverride() : st.getDisplayOrder();
                        return Optional.of(new TaxonomyNodeView(st.getId(), TaxonomyNodeType.SUBTOPIC, st.getTopicId(), st.getCode(),
                                name, st.getDescription(), order, st.isActive(), st.getTenantId()));
                    }
                }
                return Optional.of(new TaxonomyNodeView(st.getId(), TaxonomyNodeType.SUBTOPIC, st.getTopicId(), st.getCode(),
                        st.getName(), st.getDescription(), st.getDisplayOrder(), st.isActive(), st.getTenantId()));
            }
            default:
                return Optional.empty();
        }
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
        if (subjectId == null) {
            return false;
        }
        if (!isValidNode(TaxonomyNodeType.SUBJECT, subjectId, tenantId)) {
            return false;
        }

        if (topicId != null) {
            Optional<Topic> topOpt = topicRepository.findById(topicId);
            if (topOpt.isEmpty() || !topOpt.get().isActive()) {
                return false;
            }
            Topic t = topOpt.get();
            if (!subjectId.equals(t.getSubjectId())) {
                return false;
            }
            if (!isValidNode(TaxonomyNodeType.TOPIC, topicId, tenantId)) {
                return false;
            }
        }

        if (subtopicId != null) {
            if (topicId == null) {
                return false;
            }
            Optional<Subtopic> subOpt = subtopicRepository.findById(subtopicId);
            if (subOpt.isEmpty() || !subOpt.get().isActive()) {
                return false;
            }
            Subtopic st = subOpt.get();
            if (!topicId.equals(st.getTopicId())) {
                return false;
            }
            if (!isValidNode(TaxonomyNodeType.SUBTOPIC, subtopicId, tenantId)) {
                return false;
            }
        }

        return true;
    }
}
