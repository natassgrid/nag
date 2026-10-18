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

package com.examplatform.shared.taxonomy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for accessing and validating canonical taxonomy data across service boundaries.
 *
 * <p><b>Tri-Mode Deployment Compatibility:</b>
 * <ul>
 *   <li><b>Microservices:</b> Implemented via gRPC with REST fallback, backed by a local near-cache
 *       invalidated via {@link TaxonomyChanged} events.</li>
 *   <li><b>Macro / Monolith:</b> Implemented by an in-process Spring bean directly querying the
 *       taxonomy repository.</li>
 * </ul>
 *
 * <p><b>Tenant Overrides:</b>
 * The "effective tree" merges the platform-default nodes ({@code tenant_id IS NULL}) with
 * tenant-specific overrides (hidden, renamed, reordered) and tenant-created custom nodes.
 */
public interface TaxonomyClient {

    /**
     * Retrieves the effective taxonomy tree for the platform default.
     *
     * @return hierarchical list of effective subjects with their topics and subtopics
     */
    default List<TaxonomySubjectNode> getEffectiveTree() {
        return getEffectiveTree(null, null);
    }

    /**
     * Retrieves the effective taxonomy tree for the given tenant.
     *
     * @param tenantId examination authority identifier (null or blank implies platform default)
     * @return hierarchical list of effective subjects with their topics and subtopics
     */
    default List<TaxonomySubjectNode> getEffectiveTree(String tenantId) {
        return getEffectiveTree(tenantId, null);
    }

    /**
     * Retrieves the effective taxonomy tree for the given tenant, optionally scoped to the
     * syllabus nodes of a specific examination.
     *
     * @param tenantId examination authority identifier (null or blank implies platform default)
     * @param examId   optional examination ID to scope the tree to its declared syllabus nodes
     * @return hierarchical list of effective subjects scoped to the syllabus
     */
    List<TaxonomySubjectNode> getEffectiveTree(String tenantId, UUID examId);

    /**
     * Looks up an individual taxonomy node by its numeric ID and node type using platform defaults.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @return node view if present and active, or empty
     */
    default Optional<TaxonomyNodeView> findNode(TaxonomyNodeType nodeType, Long nodeId) {
        return findNode(nodeType, nodeId, null);
    }

    /**
     * Looks up an individual taxonomy node by its numeric ID and node type.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @param tenantId tenant identifier (null or blank implies platform default)
     * @return node view if present and active for the tenant, or empty
     */
    Optional<TaxonomyNodeView> findNode(TaxonomyNodeType nodeType, Long nodeId, String tenantId);

    /**
     * Validates that a node exists, is active, and is visible to the platform default.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @return true if the node exists and is visible; false otherwise
     */
    default boolean isValidNode(TaxonomyNodeType nodeType, Long nodeId) {
        return isValidNode(nodeType, nodeId, null);
    }

    /**
     * Validates that a node exists, is active, and is visible to the given tenant.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @param tenantId tenant identifier
     * @return true if the node exists and is visible; false otherwise
     */
    default boolean isValidNode(TaxonomyNodeType nodeType, Long nodeId, String tenantId) {
        if (nodeId == null || nodeType == null) {
            return false;
        }
        return findNode(nodeType, nodeId, tenantId).isPresent();
    }

    /**
     * Resolves the human-readable display name for a specific taxonomy node using platform defaults.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @return resolved display name, or empty if not found
     */
    default Optional<String> resolveName(TaxonomyNodeType nodeType, Long nodeId) {
        return resolveName(nodeType, nodeId, null);
    }

    /**
     * Resolves the human-readable display name for a specific taxonomy node.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeId   numeric identifier of the node
     * @param tenantId tenant identifier
     * @return resolved display name (applying tenant name overrides if present), or empty if not found
     */
    Optional<String> resolveName(TaxonomyNodeType nodeType, Long nodeId, String tenantId);

    /**
     * Resolves the display names for an entire Subject → Topic → Subtopic reference chain using platform defaults.
     *
     * @param ref taxonomy node reference
     * @return resolved display names container
     */
    default TaxonomyNames resolveNames(TaxonomyNodeRef ref) {
        return resolveNames(ref, null);
    }

    /**
     * Resolves the display names for an entire Subject → Topic → Subtopic reference chain.
     *
     * @param ref      taxonomy node reference
     * @param tenantId tenant identifier
     * @return resolved display names container
     */
    TaxonomyNames resolveNames(TaxonomyNodeRef ref, String tenantId);

    /**
     * Bulk resolves display names for multiple node IDs of a given node type using platform defaults.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeIds  collection of numeric node identifiers
     * @return map of nodeId -> resolved display name
     */
    default Map<Long, String> resolveNames(TaxonomyNodeType nodeType, Collection<Long> nodeIds) {
        return resolveNames(nodeType, nodeIds, null);
    }

    /**
     * Bulk resolves display names for multiple node IDs of a given node type.
     *
     * @param nodeType type of node (SUBJECT, TOPIC, or SUBTOPIC)
     * @param nodeIds  collection of numeric node identifiers
     * @param tenantId tenant identifier
     * @return map of nodeId -> resolved display name
     */
    Map<Long, String> resolveNames(TaxonomyNodeType nodeType, Collection<Long> nodeIds, String tenantId);

    /**
     * Validates the structural consistency of a taxonomy hierarchy chain:
     * <ul>
     *   <li>If {@code subtopicId} is provided, verifies that it belongs to {@code topicId}, and
     *       that {@code topicId} belongs to {@code subjectId}.</li>
     *   <li>If {@code topicId} is provided (with {@code subtopicId == null}), verifies that
     *       {@code topicId} belongs to {@code subjectId}.</li>
     *   <li>If only {@code subjectId} is provided, verifies that the subject exists.</li>
     * </ul>
     *
     * @param subjectId  numeric subject identifier (mandatory)
     * @param topicId    numeric topic identifier (optional if scoping to subject level)
     * @param subtopicId numeric subtopic identifier (optional if scoping to topic level)
     * @param tenantId   tenant identifier
     * @return true if the chain is structurally valid and visible; false otherwise
     */
    boolean validateChain(Long subjectId, Long topicId, Long subtopicId, String tenantId);

    /**
     * Validates the structural consistency of a taxonomy hierarchy chain using platform defaults.
     *
     * @param subjectId  numeric subject identifier
     * @param topicId    numeric topic identifier
     * @param subtopicId numeric subtopic identifier
     * @return true if valid
     */
    default boolean validateChain(Long subjectId, Long topicId, Long subtopicId) {
        return validateChain(subjectId, topicId, subtopicId, null);
    }

    /**
     * Validates the structural consistency of a {@link TaxonomyNodeRef}.
     *
     * @param ref      taxonomy node reference
     * @param tenantId tenant identifier
     * @return true if valid
     */
    default boolean validateChain(TaxonomyNodeRef ref, String tenantId) {
        if (ref == null) {
            return false;
        }
        return validateChain(ref.subjectId(), ref.topicId(), ref.subtopicId(), tenantId);
    }

    /**
     * Validates the structural consistency of a {@link TaxonomyNodeRef} using platform defaults.
     *
     * @param ref taxonomy node reference
     * @return true if valid
     */
    default boolean validateChain(TaxonomyNodeRef ref) {
        return validateChain(ref, null);
    }
}
