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
import java.util.List;

/**
 * Root-level node in the taxonomy hierarchy tree representing a Subject with its child Topics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxonomySubjectNode(
        Long id,
        String code,
        String name,
        String description,
        Integer displayOrder,
        boolean active,
        List<TaxonomyTopicNode> topics
) implements Serializable {

    public TaxonomySubjectNode {
        topics = (topics != null) ? List.copyOf(topics) : List.of();
    }
}
