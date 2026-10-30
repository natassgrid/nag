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
import com.examplatform.questionbank.dto.MergeTaxonomyRequest;
import com.examplatform.questionbank.dto.NodeOrderItem;
import com.examplatform.questionbank.dto.ReorderTaxonomyRequest;
import com.examplatform.questionbank.dto.TaxonomyOverrideRequest;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TaxonomyTenantOverrideRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.shared.constants.MessagingConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.taxonomy.TaxonomyChanged;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import com.examplatform.shared.taxonomy.TaxonomySubjectNode;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating taxonomy operations: effective tree resolution,
 * tenant overrides, node merging, reordering, and publishing {@link TaxonomyChanged} domain events.
 */
@Slf4j
@Service
public class TaxonomyService {

    private static final String PARAM_TARGET_ID = "targetId";
    private static final String PARAM_SOURCE_ID = "sourceId";

    private final TaxonomyClient taxonomyClient;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final TaxonomyTenantOverrideRepository overrideRepository;
    private final EventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final TaxonomyService self;

    @Autowired
    public TaxonomyService(
            TaxonomyClient taxonomyClient,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository,
            TaxonomyTenantOverrideRepository overrideRepository,
            EventPublisher eventPublisher,
            EntityManager entityManager,
            @Lazy TaxonomyService self) {
        this.taxonomyClient = taxonomyClient;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.subtopicRepository = subtopicRepository;
        this.overrideRepository = overrideRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.self = self != null ? self : this;
    }

    public TaxonomyService(
            TaxonomyClient taxonomyClient,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            SubtopicRepository subtopicRepository,
            TaxonomyTenantOverrideRepository overrideRepository,
            EventPublisher eventPublisher,
            EntityManager entityManager) {
        this(taxonomyClient, subjectRepository, topicRepository, subtopicRepository,
                overrideRepository, eventPublisher, entityManager, null);
    }

    @Transactional(readOnly = true)
    public List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId) {
        return taxonomyClient.getEffectiveTree(tenantId, examId);
    }

    @Transactional
    public TaxonomyTenantOverride applyOverride(TaxonomyOverrideRequest request, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required to apply taxonomy overrides");
        }

        TaxonomyTenantOverride override = overrideRepository
                .findByTenantIdAndNodeTypeAndNodeId(tenantId, request.getNodeType(), request.getNodeId())
                .orElseGet(() -> TaxonomyTenantOverride.builder()
                        .tenantId(tenantId)
                        .nodeType(request.getNodeType())
                        .nodeId(request.getNodeId())
                        .build());

        if (request.getHidden() != null) {
            override.setHidden(request.getHidden());
        }
        if (request.getNameOverride() != null) {
            override.setNameOverride(request.getNameOverride().trim());
        }
        if (request.getOrderOverride() != null) {
            override.setOrderOverride(request.getOrderOverride());
        }

        TaxonomyTenantOverride saved = overrideRepository.save(override);

        TaxonomyChanged event = TaxonomyChanged.nodeUpdated(
                tenantId, request.getNodeType(), request.getNodeId());
        eventPublisher.publish(MessagingConstants.TAXONOMY_EVENTS_TOPIC, tenantId, event);

        log.info("Applied taxonomy override: tenant={}, nodeType={}, nodeId={}, hidden={}, name={}",
                tenantId, request.getNodeType(), request.getNodeId(), saved.isHidden(), saved.getNameOverride());
        return saved;
    }

    @Transactional
    public void applyOverrides(List<TaxonomyOverrideRequest> requests, String tenantId) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (TaxonomyOverrideRequest req : requests) {
            self.applyOverride(req, tenantId);
        }
        TaxonomyChanged event = TaxonomyChanged.treeInvalidated(tenantId);
        eventPublisher.publish(MessagingConstants.TAXONOMY_EVENTS_TOPIC, tenantId, event);
    }

    @Transactional
    public void mergeNodes(MergeTaxonomyRequest request, String tenantId) {
        TaxonomyNodeType nodeType = request.getNodeType();
        Long sourceId = request.getSourceNodeId();
        Long targetId = request.getTargetNodeId();

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Source and target node IDs cannot be identical");
        }

        log.info("Merging {} node {} into {} for tenant {}", nodeType, sourceId, targetId, tenantId);

        switch (nodeType) {
            case SUBTOPIC -> {
                Subtopic source = subtopicRepository.findById(sourceId)
                        .orElseThrow(() -> new IllegalArgumentException("Source subtopic not found: " + sourceId));
                Subtopic target = subtopicRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Target subtopic not found: " + targetId));

                entityManager.createNativeQuery(
                        "UPDATE question_service.question SET subtopic_id = :targetId WHERE subtopic_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                entityManager.createNativeQuery(
                        "UPDATE question_service.subtopic_micro_lesson SET subtopic_id = :targetId WHERE subtopic_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                source.setActive(false);
                subtopicRepository.save(source);
            }
            case TOPIC -> {
                Topic source = topicRepository.findById(sourceId)
                        .orElseThrow(() -> new IllegalArgumentException("Source topic not found: " + sourceId));
                Topic target = topicRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Target topic not found: " + targetId));

                entityManager.createNativeQuery(
                        "UPDATE question_service.subtopic SET topic_id = :targetId WHERE topic_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                entityManager.createNativeQuery(
                        "UPDATE question_service.question SET topic_id = :targetId WHERE topic_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                entityManager.createNativeQuery(
                        "UPDATE question_service.subtopic_micro_lesson SET topic_id = :targetId WHERE topic_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                source.setActive(false);
                topicRepository.save(source);
            }
            case SUBJECT -> {
                Subject source = subjectRepository.findById(sourceId)
                        .orElseThrow(() -> new IllegalArgumentException("Source subject not found: " + sourceId));
                Subject target = subjectRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Target subject not found: " + targetId));

                entityManager.createNativeQuery(
                        "UPDATE question_service.topic SET subject_id = :targetId WHERE subject_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                entityManager.createNativeQuery(
                        "UPDATE question_service.question SET subject_id = :targetId WHERE subject_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                entityManager.createNativeQuery(
                        "UPDATE question_service.subtopic_micro_lesson SET subject_id = :targetId WHERE subject_id = :sourceId")
                        .setParameter(PARAM_TARGET_ID, target.getId())
                        .setParameter(PARAM_SOURCE_ID, source.getId())
                        .executeUpdate();

                source.setActive(false);
                subjectRepository.save(source);
            }
        }

        TaxonomyChanged event = TaxonomyChanged.nodeMerged(tenantId, nodeType, sourceId, targetId);
        eventPublisher.publish(MessagingConstants.TAXONOMY_EVENTS_TOPIC, tenantId != null ? tenantId : "platform", event);
    }

    @Transactional
    public void reorderNodes(ReorderTaxonomyRequest request, String tenantId) {
        TaxonomyNodeType nodeType = request.getNodeType();
        List<NodeOrderItem> items = request.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        boolean isTenantSpecific = (tenantId != null && !tenantId.isBlank());

        for (NodeOrderItem item : items) {
            if (isTenantSpecific) {
                TaxonomyTenantOverride override = overrideRepository
                        .findByTenantIdAndNodeTypeAndNodeId(tenantId, nodeType, item.getNodeId())
                        .orElseGet(() -> TaxonomyTenantOverride.builder()
                                .tenantId(tenantId)
                                .nodeType(nodeType)
                                .nodeId(item.getNodeId())
                                .build());
                override.setOrderOverride(item.getDisplayOrder());
                overrideRepository.save(override);
            } else {
                switch (nodeType) {
                    case SUBJECT -> subjectRepository.findById(item.getNodeId()).ifPresent(s -> {
                        s.setDisplayOrder(item.getDisplayOrder());
                        subjectRepository.save(s);
                    });
                    case TOPIC -> topicRepository.findById(item.getNodeId()).ifPresent(t -> {
                        t.setDisplayOrder(item.getDisplayOrder());
                        topicRepository.save(t);
                    });
                    case SUBTOPIC -> subtopicRepository.findById(item.getNodeId()).ifPresent(st -> {
                        st.setDisplayOrder(item.getDisplayOrder());
                        subtopicRepository.save(st);
                    });
                }
            }
        }

        TaxonomyChanged event = TaxonomyChanged.treeInvalidated(tenantId);
        eventPublisher.publish(MessagingConstants.TAXONOMY_EVENTS_TOPIC, tenantId != null ? tenantId : "platform", event);
        log.info("Reordered {} nodes for tenant={}", nodeType, tenantId);
    }
}
