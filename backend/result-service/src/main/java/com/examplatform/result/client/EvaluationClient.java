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

package com.examplatform.result.client;

import com.examplatform.result.dto.CandidateEvaluationItemDto;
import com.examplatform.result.dto.CandidateExamResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Client for fetching candidate evaluations and per-question score breakdown from evaluation-service.
 */
public interface EvaluationClient {

    /**
     * Retrieves evaluations and responses for a candidate in an exam.
     *
     * @param candidateId the candidate UUID
     * @param examId      the exam UUID
     * @param tenantId    the tenant identifier
     * @return list of candidate evaluation items
     */
    List<CandidateEvaluationItemDto> getEvaluationsForCandidate(UUID candidateId, UUID examId, String tenantId);

    /**
     * Retrieves all candidate responses and scores for an entire exam.
     *
     * @param examId   the exam UUID
     * @param tenantId the tenant identifier
     * @return list of candidate exam responses
     */
    List<CandidateExamResponseDto> getResponsesForExam(UUID examId, String tenantId);
}
