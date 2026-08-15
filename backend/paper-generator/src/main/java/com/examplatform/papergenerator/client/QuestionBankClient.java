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

package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.QuestionSummary;

import java.util.List;

/**
 * Client interface for inter-service communication with the Question Bank Service.
 * Implementations may use REST, gRPC, or messaging depending on deployment topology.
 *
 * Validates: Requirements 8.1, 8.3
 */
public interface QuestionBankClient {

    /**
     * Finds available questions matching the specified criteria from the question bank.
     *
     * @param subject        the subject to filter by
     * @param topic          the topic to filter by
     * @param difficulty     the difficulty level (EASY/MEDIUM/HARD) or null for any
     * @param cognitiveLevel the cognitive level or null for any
     * @param tenantId       the tenant identifier for multi-tenancy isolation
     * @return list of question summaries matching the criteria
     */
    List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                  String difficulty, String cognitiveLevel, String tenantId);
}
