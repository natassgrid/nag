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

package com.examplatform.questionbank.client;

import com.examplatform.questionbank.dto.ReviewerDto;

import java.util.List;

/**
 * Client for fetching available reviewers and subject matter experts from Identity Service.
 */
public interface ReviewerPoolClient {

    /**
     * Retrieve active reviewers for a given subject and tenant.
     *
     * @param subject  target subject domain (optional)
     * @param tenantId tenant identifier
     * @return list of reviewer DTOs
     */
    List<ReviewerDto> getReviewers(String subject, String tenantId);
}
