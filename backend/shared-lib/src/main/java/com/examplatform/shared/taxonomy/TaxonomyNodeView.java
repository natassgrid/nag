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

/**
 * Flattened view of an individual taxonomy node returned by lookup and validation queries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxonomyNodeView(
        Long id,
        TaxonomyNodeType nodeType,
        Long parentId,
        String code,
        String name,
        String description,
        Integer displayOrder,
        boolean active,
        String tenantId
) implements Serializable {}
