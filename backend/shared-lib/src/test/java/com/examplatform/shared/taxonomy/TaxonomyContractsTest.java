/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 */

package com.examplatform.shared.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Taxonomy Contracts & Serialization Tests")
class TaxonomyContractsTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("TaxonomyNodeRef construction, helper methods, and serialization")
    void testTaxonomyNodeRef() throws Exception {
        TaxonomyNodeRef complete = TaxonomyNodeRef.of(10L, 20L, 30L);
        assertThat(complete.isComplete()).isTrue();
        assertThat(complete.narrowestId()).isEqualTo(30L);
        assertThat(complete.narrowestType()).isEqualTo(TaxonomyNodeType.SUBTOPIC);

        TaxonomyNodeRef topicOnly = TaxonomyNodeRef.forTopic(10L, 20L);
        assertThat(topicOnly.isComplete()).isFalse();
        assertThat(topicOnly.narrowestId()).isEqualTo(20L);
        assertThat(topicOnly.narrowestType()).isEqualTo(TaxonomyNodeType.TOPIC);

        TaxonomyNodeRef subjectOnly = TaxonomyNodeRef.forSubject(10L);
        assertThat(subjectOnly.isComplete()).isFalse();
        assertThat(subjectOnly.narrowestId()).isEqualTo(10L);
        assertThat(subjectOnly.narrowestType()).isEqualTo(TaxonomyNodeType.SUBJECT);

        String json = objectMapper.writeValueAsString(complete);
        TaxonomyNodeRef deserialized = objectMapper.readValue(json, TaxonomyNodeRef.class);
        assertThat(deserialized).isEqualTo(complete);
    }

    @Test
    @DisplayName("TaxonomyChanged event factory methods and serialization")
    void testTaxonomyChangedEvent() throws Exception {
        TaxonomyChanged created = TaxonomyChanged.nodeCreated("tenant-a", TaxonomyNodeType.TOPIC, 101L);
        assertThat(created.eventType()).isEqualTo(TaxonomyChanged.EVENT_TYPE_TAXONOMY_CHANGED);
        assertThat(created.action()).isEqualTo("CREATED");
        assertThat(created.tenantId()).isEqualTo("tenant-a");
        assertThat(created.nodeType()).isEqualTo(TaxonomyNodeType.TOPIC);
        assertThat(created.nodeId()).isEqualTo(101L);
        assertThat(created.occurredAt()).isNotNull();

        TaxonomyChanged merged = TaxonomyChanged.nodeMerged("tenant-a", TaxonomyNodeType.SUBTOPIC, 201L, 202L);
        assertThat(merged.action()).isEqualTo("MERGED");
        assertThat(merged.nodeId()).isEqualTo(201L);
        assertThat(merged.targetNodeId()).isEqualTo(202L);

        String json = objectMapper.writeValueAsString(merged);
        TaxonomyChanged deserialized = objectMapper.readValue(json, TaxonomyChanged.class);
        assertThat(deserialized.action()).isEqualTo("MERGED");
        assertThat(deserialized.nodeId()).isEqualTo(201L);
        assertThat(deserialized.targetNodeId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("Taxonomy hierarchy nodes and views serialize cleanly")
    void testTaxonomyHierarchyModels() throws Exception {
        TaxonomySubtopicNode subtopic = new TaxonomySubtopicNode(301L, 201L, "ST_ALGEBRA", "Algebra Basics", "Intro", 1, true);
        TaxonomyTopicNode topic = new TaxonomyTopicNode(201L, 101L, "T_MATH", "Mathematics", "Core", 1, true, List.of(subtopic));
        TaxonomySubjectNode subject = new TaxonomySubjectNode(101L, "S_QUANT", "Quantitative Aptitude", "Quant section", 1, true, List.of(topic));

        String json = objectMapper.writeValueAsString(subject);
        TaxonomySubjectNode deserialized = objectMapper.readValue(json, TaxonomySubjectNode.class);
        assertThat(deserialized.id()).isEqualTo(101L);
        assertThat(deserialized.topics()).hasSize(1);
        assertThat(deserialized.topics().get(0).subtopics()).hasSize(1);
        assertThat(deserialized.topics().get(0).subtopics().get(0).name()).isEqualTo("Algebra Basics");
    }

    @Test
    @DisplayName("TaxonomyClient default methods behave predictably")
    void testTaxonomyClientDefaultMethods() {
        TaxonomyClient client = new TaxonomyClient() {
            @Override
            public List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId) {
                return List.of(new TaxonomySubjectNode(1L, "MATH", "Math", null, 1, true, List.of()));
            }

            @Override
            public Optional<TaxonomyNodeView> findNode(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
                if (nodeId == 1L) {
                    return Optional.of(new TaxonomyNodeView(1L, nodeType, null, "CODE1", "Node1", null, 1, true, tenantId));
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> resolveName(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
                return findNode(nodeType, nodeId, tenantId).map(TaxonomyNodeView::name);
            }

            @Override
            public TaxonomyNames resolveNames(TaxonomyNodeRef ref, String tenantId) {
                return new TaxonomyNames("Math", "Algebra", "Linear Equations");
            }

            @Override
            public Map<Long, String> resolveNames(TaxonomyNodeType nodeType, Collection<Long> nodeIds, String tenantId) {
                return Map.of(1L, "Node1");
            }

            @Override
            public boolean validateChain(Long subjectId, Long topicId, Long subtopicId, String tenantId) {
                return subjectId != null && subjectId == 1L;
            }
        };

        assertThat(client.getEffectiveTree()).hasSize(1);
        assertThat(client.getEffectiveTree("t1")).hasSize(1);
        assertThat(client.isValidNode(TaxonomyNodeType.SUBJECT, 1L)).isTrue();
        assertThat(client.isValidNode(TaxonomyNodeType.SUBJECT, 999L)).isFalse();
        assertThat(client.resolveName(TaxonomyNodeType.SUBJECT, 1L)).contains("Node1");
        assertThat(client.resolveNames(TaxonomyNodeType.SUBJECT, List.of(1L))).containsEntry(1L, "Node1");

        TaxonomyNodeRef validRef = TaxonomyNodeRef.of(1L, 2L, 3L);
        TaxonomyNodeRef invalidRef = TaxonomyNodeRef.of(99L, 2L, 3L);
        assertThat(client.validateChain(validRef)).isTrue();
        assertThat(client.validateChain(invalidRef)).isFalse();
        assertThat(client.validateChain(1L, 2L, 3L)).isTrue();
    }
}
