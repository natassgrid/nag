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
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.SubjectHierarchyResponse;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing the Subject → Topic → Subtopic hierarchy.
 * Supports platform-level defaults (tenant_id IS NULL) and tenant-specific additions.
 */
@Slf4j
@Service
public class SubjectTopicService {

    private static final String TENANT_PARAM = " tenant=";

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final SubjectTopicService self;

    @Autowired
    public SubjectTopicService(
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository,
            @Lazy SubjectTopicService self) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.subtopicRepository = subtopicRepository;
        this.self = self != null ? self : this;
    }

    public SubjectTopicService(
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository) {
        this(subjectRepository, topicRepository, subtopicRepository, null);
    }

    // -----------------------------------------------------------------------
    // Subjects
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Subject> listSubjects(String tenantId) {
        List<Subject> platform = subjectRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc();
        if (tenantId == null || tenantId.isBlank()) {
            return platform;
        }
        List<Subject> tenantSubs = subjectRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantId);
        List<Subject> combined = new ArrayList<>(platform);
        Set<String> platformNames = platform.stream()
                .map(s -> s.getName().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> platformCodes = platform.stream()
                .filter(s -> s.getCode() != null)
                .map(s -> s.getCode().trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (Subject ts : tenantSubs) {
            String normName = ts.getName() != null ? ts.getName().trim().toLowerCase(Locale.ROOT) : "";
            String normCode = ts.getCode() != null ? ts.getCode().trim().toUpperCase(Locale.ROOT) : "";
            if (!platformNames.contains(normName) && (normCode.isEmpty() || !platformCodes.contains(normCode))) {
                combined.add(ts);
            }
        }
        return combined;
    }

    @Transactional
    public Subject createSubject(String name, String code, String description, String tenantId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subject name is required");
        }
        String cleanName = name.trim();
        if (subjectRepository.existsByNameAndTenantId(cleanName, tenantId)
                || subjectRepository.existsByNameAndTenantIdIsNull(cleanName)) {
            throw new IllegalArgumentException("Subject with name '" + cleanName + "' already exists");
        }

        String effectiveCode = (code != null && !code.isBlank()) ? code.trim() : generateCode(cleanName);

        Subject subject = Subject.builder()
                .name(cleanName)
                .code(effectiveCode)
                .description(description)
                .active(true)
                .build();
        subject.setTenantId(tenantId);

        log.info("Creating subject: name={}, code={}, tenant={}", cleanName, effectiveCode, tenantId);
        return subjectRepository.save(subject);
    }

    // -----------------------------------------------------------------------
    // Topics
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Topic> listTopics(Long subjectId, String tenantId) {
        List<Topic> platform = topicRepository.findBySubjectIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(subjectId);
        if (tenantId == null || tenantId.isBlank()) {
            return platform;
        }
        List<Topic> tenantTopics = topicRepository.findBySubjectIdAndTenantIdOrderByDisplayOrderAscNameAsc(subjectId, tenantId);
        List<Topic> combined = new ArrayList<>(platform);
        combined.addAll(tenantTopics);
        return combined;
    }

    @Transactional
    public Topic createTopic(Long subjectId, String name, String description, String tenantId) {
        return self.createTopic(subjectId, null, name, description, tenantId);
    }

    @Transactional
    public Topic createTopic(Long subjectId, String code, String name, String description, String tenantId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name is required");
        }
        String cleanName = name.trim();
        if (topicRepository.existsByNameAndSubjectIdAndTenantId(cleanName, subjectId, tenantId)
                || topicRepository.existsByNameAndSubjectIdAndTenantIdIsNull(cleanName, subjectId)) {
            throw new IllegalArgumentException("Topic with name '" + cleanName + "' already exists for this subject");
        }

        String effectiveCode = (code != null && !code.isBlank()) ? code.trim() : generateCode(cleanName);

        Topic topic = Topic.builder()
                .subjectId(subjectId)
                .code(effectiveCode)
                .name(cleanName)
                .description(description)
                .active(true)
                .build();
        topic.setTenantId(tenantId);

        log.info("Creating topic: name={}, code={}, subjectId={}, tenant={}", cleanName, effectiveCode, subjectId, tenantId);
        return topicRepository.save(topic);
    }

    // -----------------------------------------------------------------------
    // Subtopics
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Subtopic> listSubtopics(Long topicId, String tenantId) {
        List<Subtopic> platform = subtopicRepository.findByTopicIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(topicId);
        if (tenantId == null || tenantId.isBlank()) {
            return platform;
        }
        List<Subtopic> tenantSubtopics = subtopicRepository.findByTopicIdAndTenantIdOrderByDisplayOrderAscNameAsc(topicId, tenantId);
        List<Subtopic> combined = new ArrayList<>(platform);
        combined.addAll(tenantSubtopics);
        return combined;
    }

    @Transactional
    public Subtopic createSubtopic(Long topicId, String name, String description, String tenantId) {
        return self.createSubtopic(topicId, null, name, description, tenantId);
    }

    @Transactional
    public Subtopic createSubtopic(Long topicId, String code, String name, String description, String tenantId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subtopic name is required");
        }
        String cleanName = name.trim();
        if (subtopicRepository.existsByNameAndTopicIdAndTenantId(cleanName, topicId, tenantId)
                || subtopicRepository.existsByNameAndTopicIdAndTenantIdIsNull(cleanName, topicId)) {
            throw new IllegalArgumentException("Subtopic with name '" + cleanName + "' already exists for this topic");
        }

        String effectiveCode = (code != null && !code.isBlank()) ? code.trim() : generateCode(cleanName);

        Subtopic subtopic = Subtopic.builder()
                .topicId(topicId)
                .code(effectiveCode)
                .name(cleanName)
                .description(description)
                .active(true)
                .build();
        subtopic.setTenantId(tenantId);

        log.info("Creating subtopic: name={}, code={}, topicId={}, tenant={}", cleanName, effectiveCode, topicId, tenantId);
        return subtopicRepository.save(subtopic);
    }

    // -----------------------------------------------------------------------
    // Name-based resolution (for AI generation / import by name)
    // -----------------------------------------------------------------------

    /**
     * Resolved hierarchy ids for a given set of subject/topic/subtopic names.
     */
    public record HierarchyIds(Long subjectId, Long topicId, Long subtopicId) {}

    /**
     * Resolves subject/topic/subtopic <b>ids</b> to validated ids for a tenant.
     * Unlike {@link #resolveOrCreateByName}, this method never creates new nodes.
     *
     * @param subjectId  required subject id
     * @param topicId    required topic id (must belong to subjectId)
     * @param subtopicId required subtopic id (must belong to topicId)
     * @param tenantId   tenant scope
     */
    @Transactional
    public HierarchyIds resolveByIds(Long subjectId, Long topicId, Long subtopicId, String tenantId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("subjectId is required");
        }
        if (topicId == null) {
            throw new IllegalArgumentException("topicId is required");
        }

        Subject subject = subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .or(() -> subjectRepository.findByIdAndTenantIdIsNull(subjectId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subject not found: id=" + subjectId + TENANT_PARAM + tenantId));

        Topic topic = topicRepository.findByIdAndSubjectIdAndTenantId(topicId, subject.getId(), tenantId)
                .or(() -> topicRepository.findByIdAndSubjectIdAndTenantIdIsNull(topicId, subject.getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Topic not found: id=" + topicId + " for subjectId=" + subjectId + TENANT_PARAM + tenantId));

        Long resolvedSubtopicId;
        if (subtopicId != null) {
            Subtopic subtopic = subtopicRepository.findByIdAndTopicIdAndTenantId(subtopicId, topic.getId(), tenantId)
                    .or(() -> subtopicRepository.findByIdAndTopicIdAndTenantIdIsNull(subtopicId, topic.getId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subtopic not found: id=" + subtopicId + " for topicId=" + topicId + TENANT_PARAM + tenantId));
            resolvedSubtopicId = subtopic.getId();
        } else {
            // Pick first subtopic of topic as mandatory subtopic, or create default subtopic if none exist
            List<Subtopic> subs = self.listSubtopics(topic.getId(), tenantId);
            if (!subs.isEmpty()) {
                resolvedSubtopicId = subs.get(0).getId();
            } else {
                Subtopic created = self.createSubtopic(topic.getId(), null, "General " + topic.getName(), null, tenantId);
                resolvedSubtopicId = created.getId();
            }
        }

        return new HierarchyIds(subject.getId(), topic.getId(), resolvedSubtopicId);
    }

    /**
     * Resolves subject/topic/subtopic names to their numeric ids for a tenant,
     * creating any missing nodes on the fly. Used by AI generation and by
     * name-based imports where the caller supplies names rather than ids.
     *
     * @param subjectName required subject name
     * @param topicName   required topic name (under the subject)
     * @param subtopicName optional subtopic name (under the topic)
     */
    @Transactional
    public HierarchyIds resolveOrCreateByName(String subjectName, String topicName,
                                              String subtopicName, String tenantId) {
        if (subjectName == null || subjectName.isBlank()) {
            throw new IllegalArgumentException("Subject name is required to resolve hierarchy");
        }
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("Topic name is required to resolve hierarchy");
        }

        String sName = subjectName.trim();
        Subject subject = subjectRepository.findByNameAndTenantId(sName, tenantId)
                .or(() -> subjectRepository.findByNameAndTenantIdIsNull(sName))
                .orElseGet(() -> self.createSubject(sName, null, null, tenantId));

        String tName = topicName.trim();
        Topic topic = topicRepository.findByNameAndSubjectIdAndTenantId(tName, subject.getId(), tenantId)
                .or(() -> topicRepository.findByNameAndSubjectIdAndTenantIdIsNull(tName, subject.getId()))
                .orElseGet(() -> self.createTopic(subject.getId(), null, tName, null, tenantId));

        Long subtopicId;
        if (subtopicName != null && !subtopicName.isBlank()) {
            String stName = subtopicName.trim();
            Subtopic subtopic = subtopicRepository
                    .findByNameAndTopicIdAndTenantId(stName, topic.getId(), tenantId)
                    .or(() -> subtopicRepository.findByNameAndTopicIdAndTenantIdIsNull(stName, topic.getId()))
                    .orElseGet(() -> self.createSubtopic(topic.getId(), null, stName, null, tenantId));
            subtopicId = subtopic.getId();
        } else {
            List<Subtopic> existing = self.listSubtopics(topic.getId(), tenantId);
            if (!existing.isEmpty()) {
                subtopicId = existing.get(0).getId();
            } else {
                Subtopic created = self.createSubtopic(topic.getId(), null, "General " + topic.getName(), null, tenantId);
                subtopicId = created.getId();
            }
        }

        return new HierarchyIds(subject.getId(), topic.getId(), subtopicId);
    }

    // -----------------------------------------------------------------------
    // Hierarchy
    // -----------------------------------------------------------------------

    /**
     * Returns the full Subject → Topic → Subtopic tree for the given tenant.
     * Used to populate cascading dropdowns in the UI.
     */
    @Transactional(readOnly = true)
    public List<SubjectHierarchyResponse> getHierarchy(String tenantId) {
        List<Subject> subjects = self.listSubjects(tenantId);

        return subjects.stream().map(subject -> {
            List<Topic> topics = self.listTopics(subject.getId(), tenantId);

            List<SubjectHierarchyResponse.TopicNode> topicNodes = topics.stream().map(topic -> {
                List<Subtopic> subtopics = self.listSubtopics(topic.getId(), tenantId);

                List<SubjectHierarchyResponse.SubtopicNode> subtopicNodes = subtopics.stream()
                        .map(st -> SubjectHierarchyResponse.SubtopicNode.builder()
                                .id(st.getId())
                                .name(st.getName())
                                .description(st.getDescription())
                                .active(st.isActive())
                                .build())
                        .collect(Collectors.toList());

                return SubjectHierarchyResponse.TopicNode.builder()
                        .id(topic.getId())
                        .name(topic.getName())
                        .description(topic.getDescription())
                        .active(topic.isActive())
                        .subtopics(subtopicNodes)
                        .build();
            }).collect(Collectors.toList());

            return SubjectHierarchyResponse.builder()
                    .id(subject.getId())
                    .name(subject.getName())
                    .code(subject.getCode())
                    .description(subject.getDescription())
                    .active(subject.isActive())
                    .topics(topicNodes)
                    .build();
        }).collect(Collectors.toList());
    }

    private static String generateCode(String text) {
        String clean = text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (clean.length() > 40) {
            clean = clean.substring(0, 40);
        }
        return clean.replaceAll("(^_++)|(_++$)", "");
    }
}
