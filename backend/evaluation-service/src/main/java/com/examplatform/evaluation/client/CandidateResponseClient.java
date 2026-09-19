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

package com.examplatform.evaluation.client;

import com.examplatform.evaluation.dto.CandidateResponse;

import java.util.List;
import java.util.UUID;

/**
 * Client interface for retrieving submitted candidate responses from response-service.
 */
public interface CandidateResponseClient {

    /**
     * Retrieve all candidate responses for an exam session.
     *
     * @param sessionId the session UUID
     * @param tenantId  the tenant identifier
     * @return list of CandidateResponse DTOs
     */
    List<CandidateResponse> getCandidateResponses(UUID sessionId, String tenantId);
}
