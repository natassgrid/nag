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

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.SubjectHierarchyResponse;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing the Subject → Topic → Subtopic hierarchy.
 * All operations are tenant-scoped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectTopicService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;

    // -----------------------------------------------------------------------
    // Subjects
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Subject> listSubjects(String tenantId) {
        return subjectRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Subject createSubject(String name, String code, String description, String tenantId) {
        if (subjectRepository.existsByNameAndTenantId(name, tenantId)) {
            throw new IllegalArgumentException("Subject with name '" + name + "' already exists");
        }

        Subject subject = Subject.builder()
                .name(name)
                .code(code)
                .description(description)
                .active(true)
                .build();
        subject.setTenantId(tenantId);

        log.info("Creating subject: name={}, tenant={}", name, tenantId);
        return subjectRepository.save(subject);
    }

    // -----------------------------------------------------------------------
    // Topics
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Topic> listTopics(Long subjectId, String tenantId) {
        return topicRepository.findBySubjectIdAndTenantId(subjectId, tenantId);
    }

    @Transactional
    public Topic createTopic(Long subjectId, String name, String description, String tenantId) {
        if (topicRepository.existsByNameAndSubjectIdAndTenantId(name, subjectId, tenantId)) {
            throw new IllegalArgumentException("Topic with name '" + name + "' already exists for this subject");
        }

        Topic topic = Topic.builder()
                .subjectId(subjectId)
                .name(name)
                .description(description)
                .active(true)
                .build();
        topic.setTenantId(tenantId);

        log.info("Creating topic: name={}, subjectId={}, tenant={}", name, subjectId, tenantId);
        return topicRepository.save(topic);
    }

    // -----------------------------------------------------------------------
    // Subtopics
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Subtopic> listSubtopics(Long topicId, String tenantId) {
        return subtopicRepository.findByTopicIdAndTenantId(topicId, tenantId);
    }

    @Transactional
    public Subtopic createSubtopic(Long topicId, String name, String description, String tenantId) {
        if (subtopicRepository.existsByNameAndTopicIdAndTenantId(name, topicId, tenantId)) {
            throw new IllegalArgumentException("Subtopic with name '" + name + "' already exists for this topic");
        }

        Subtopic subtopic = Subtopic.builder()
                .topicId(topicId)
                .name(name)
                .description(description)
                .active(true)
                .build();
        subtopic.setTenantId(tenantId);

        log.info("Creating subtopic: name={}, topicId={}, tenant={}", name, topicId, tenantId);
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

        Subject subject = subjectRepository.findByNameAndTenantId(subjectName, tenantId)
                .orElseGet(() -> createSubject(subjectName, null, null, tenantId));

        Topic topic = topicRepository.findByNameAndSubjectIdAndTenantId(topicName, subject.getId(), tenantId)
                .orElseGet(() -> createTopic(subject.getId(), topicName, null, tenantId));

        Long subtopicId = null;
        if (subtopicName != null && !subtopicName.isBlank()) {
            Subtopic subtopic = subtopicRepository
                    .findByNameAndTopicIdAndTenantId(subtopicName, topic.getId(), tenantId)
                    .orElseGet(() -> createSubtopic(topic.getId(), subtopicName, null, tenantId));
            subtopicId = subtopic.getId();
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
        List<Subject> subjects = subjectRepository.findByTenantId(tenantId);

        return subjects.stream().map(subject -> {
            List<Topic> topics = topicRepository.findBySubjectIdAndTenantId(subject.getId(), tenantId);

            List<SubjectHierarchyResponse.TopicNode> topicNodes = topics.stream().map(topic -> {
                List<Subtopic> subtopics = subtopicRepository.findByTopicIdAndTenantId(topic.getId(), tenantId);

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
}
