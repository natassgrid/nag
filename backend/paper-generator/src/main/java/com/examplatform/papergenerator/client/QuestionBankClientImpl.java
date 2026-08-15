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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of QuestionBankClient for local development.
 * In production, this would make REST calls to the question-bank-service.
 */
@Slf4j
@Component
public class QuestionBankClientImpl implements QuestionBankClient {

    @Override
    public List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        log.info("[STUB] Finding questions: subject={}, topic={}, difficulty={}, tenant={}",
                subject, topic, difficulty, tenantId);
        return Collections.emptyList();
    }
}
