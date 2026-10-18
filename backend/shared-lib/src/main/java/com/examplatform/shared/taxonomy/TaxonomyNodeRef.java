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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Immutable reference identifying a node or sub-tree within the taxonomy hierarchy:
 * Subject → Topic → Subtopic.
 *
 * <p>Under the Unified Exam Spine architecture, taxonomy nodes use compact numeric ({@code BIGINT})
 * identifiers. Depending on context (e.g. question classification vs blueprint rule scope),
 * finer-granularity fields may be null. For example, a blueprint rule scoped to an
 * entire subject will have null topicId and subtopicId, while a question must have
 * all three populated.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaxonomyNodeRef(
        Long subjectId,
        Long topicId,
        Long subtopicId
) implements Serializable {

    public static TaxonomyNodeRef of(Long subjectId, Long topicId, Long subtopicId) {
        return new TaxonomyNodeRef(subjectId, topicId, subtopicId);
    }

    public static TaxonomyNodeRef forSubject(Long subjectId) {
        return new TaxonomyNodeRef(subjectId, null, null);
    }

    public static TaxonomyNodeRef forTopic(Long subjectId, Long topicId) {
        return new TaxonomyNodeRef(subjectId, topicId, null);
    }

    public static TaxonomyNodeRef forSubtopic(Long subjectId, Long topicId, Long subtopicId) {
        return new TaxonomyNodeRef(subjectId, topicId, subtopicId);
    }

    /**
     * Checks if this reference is fully specified down to the subtopic level.
     */
    @JsonIgnore
    public boolean isComplete() {
        return subjectId != null && topicId != null && subtopicId != null;
    }

    /**
     * Returns the narrowest non-null node ID in the chain.
     */
    @JsonIgnore
    public Long narrowestId() {
        if (subtopicId != null) return subtopicId;
        if (topicId != null) return topicId;
        return subjectId;
    }

    /**
     * Returns the node type corresponding to the narrowest non-null node in this reference.
     */
    @JsonIgnore
    public TaxonomyNodeType narrowestType() {
        if (subtopicId != null) return TaxonomyNodeType.SUBTOPIC;
        if (topicId != null) return TaxonomyNodeType.TOPIC;
        if (subjectId != null) return TaxonomyNodeType.SUBJECT;
        return null;
    }
}
