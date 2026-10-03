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

import com.examplatform.result.dto.QuestionDetailDto;

import java.util.List;
import java.util.UUID;

/**
 * Client for fetching question contents, options, and explanations from question-bank-service.
 */
public interface QuestionBankClient {

    /**
     * Retrieves question details by a list of question UUIDs.
     *
     * @param questionIds list of question IDs
     * @param tenantId    tenant identifier
     * @return list of question details
     */
    List<QuestionDetailDto> findQuestionsByIds(List<UUID> questionIds, String tenantId);
}
