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

import com.examplatform.evaluation.dto.AnswerKey;

import java.util.List;
import java.util.UUID;

/**
 * Client interface for retrieving question answer keys from question-bank-service.
 */
public interface AnswerKeyClient {

    /**
     * Retrieve all answer keys for an exam paper.
     *
     * @param paperId  the exam paper UUID
     * @param tenantId the tenant identifier
     * @return list of AnswerKey DTOs
     */
    List<AnswerKey> getAnswerKeysForPaper(UUID paperId, String tenantId);

    /**
     * Batch retrieve answer keys for specific question IDs.
     *
     * @param questionIds list of question UUIDs
     * @param tenantId    the tenant identifier
     * @return list of AnswerKey DTOs
     */
    List<AnswerKey> batchGetAnswerKeys(List<UUID> questionIds, String tenantId);
}
