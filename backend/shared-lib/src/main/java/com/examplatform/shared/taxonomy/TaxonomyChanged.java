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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.Instant;

/**
 * Domain event broadcast whenever taxonomy nodes or tenant overrides are mutated.
 *
 * <p>Consumed across microservices (via Kafka/RabbitMQ) and monolith/macro deployments
 * (via Spring event bus) to invalidate cached effective taxonomy trees and node lookups.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxonomyChanged(
        String eventType,
        String action,
        String tenantId,
        TaxonomyNodeType nodeType,
        Long nodeId,
        Long targetNodeId,
        String details,
        Instant occurredAt
) implements Serializable {

    public static final String EVENT_TYPE_TAXONOMY_CHANGED = "TAXONOMY_CHANGED";

    public static TaxonomyChanged nodeCreated(String tenantId, TaxonomyNodeType nodeType, Long nodeId) {
        return new TaxonomyChanged(
                EVENT_TYPE_TAXONOMY_CHANGED,
                "CREATED",
                tenantId,
                nodeType,
                nodeId,
                null,
                null,
                Instant.now()
        );
    }

    public static TaxonomyChanged nodeUpdated(String tenantId, TaxonomyNodeType nodeType, Long nodeId) {
        return new TaxonomyChanged(
                EVENT_TYPE_TAXONOMY_CHANGED,
                "UPDATED",
                tenantId,
                nodeType,
                nodeId,
                null,
                null,
                Instant.now()
        );
    }

    public static TaxonomyChanged nodeDeleted(String tenantId, TaxonomyNodeType nodeType, Long nodeId) {
        return new TaxonomyChanged(
                EVENT_TYPE_TAXONOMY_CHANGED,
                "DELETED",
                tenantId,
                nodeType,
                nodeId,
                null,
                null,
                Instant.now()
        );
    }

    public static TaxonomyChanged nodeMerged(String tenantId, TaxonomyNodeType nodeType, Long sourceNodeId, Long targetNodeId) {
        return new TaxonomyChanged(
                EVENT_TYPE_TAXONOMY_CHANGED,
                "MERGED",
                tenantId,
                nodeType,
                sourceNodeId,
                targetNodeId,
                "Merged node " + sourceNodeId + " into " + targetNodeId,
                Instant.now()
        );
    }

    public static TaxonomyChanged treeInvalidated(String tenantId) {
        return new TaxonomyChanged(
                EVENT_TYPE_TAXONOMY_CHANGED,
                "TREE_INVALIDATED",
                tenantId,
                null,
                null,
                null,
                "Full taxonomy tree invalidated",
                Instant.now()
        );
    }
}
