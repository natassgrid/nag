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

package com.examplatform.response.service;

import com.examplatform.response.domain.Response;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving the full revision history of responses for an exam session.
 * Returns all responses ordered by questionId + revisionSequence for audit/evaluation purposes.
 *
 * Validates: Requirements 10.5
 */
@Service
@RequiredArgsConstructor
public class ResponseHistoryService {

    private final ResponseRepository responseRepository;

    /**
     * Retrieves all responses for a session ordered by questionId + revisionSequence.
     *
     * @param sessionId the exam session ID
     * @param tenantId  the tenant identifier
     * @return all responses for the session in order
     */
    public List<Response> getSessionResponses(UUID sessionId, String tenantId) {
        List<Response> responses = responseRepository.findBySessionIdAndTenantId(sessionId, tenantId);

        // Sort by questionId then revisionSequence ascending
        return responses.stream()
                .sorted(Comparator
                        .comparing(Response::getQuestionId)
                        .thenComparingInt(Response::getRevisionSequence))
                .toList();
    }
}
