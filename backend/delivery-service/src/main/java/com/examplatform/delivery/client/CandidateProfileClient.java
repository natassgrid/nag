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

package com.examplatform.delivery.client;

import com.examplatform.delivery.dto.CandidateExtension;

import java.util.UUID;

/**
 * Client interface for retrieving candidate profile details from the candidate-service.
 * Implementations may use REST (WebClient/RestClient) or gRPC for inter-service communication.
 */
public interface CandidateProfileClient {

    /**
     * Retrieve the disability extension configuration for a candidate within a tenant.
     *
     * @param candidateId the candidate's unique identifier
     * @param tenantId    the tenant (examination authority) identifier
     * @return the candidate's extension details, or null if no extension is configured
     */
    CandidateExtension getExtension(UUID candidateId, String tenantId);
}
