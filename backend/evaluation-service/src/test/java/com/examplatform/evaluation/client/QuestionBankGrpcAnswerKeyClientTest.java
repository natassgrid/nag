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
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionBankGrpcAnswerKeyClientTest {

    private ObjectMapper objectMapper;
    private QuestionBankGrpcAnswerKeyClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new QuestionBankGrpcAnswerKeyClient("localhost", 19083, 1000, objectMapper);
    }

    @Test
    @DisplayName("Throws UpstreamServiceUnavailableException when gRPC server is unreachable")
    void shouldThrowUpstreamExceptionWhenServerUnreachable() {
        UUID paperId = UUID.randomUUID();

        assertThatThrownBy(() -> client.getAnswerKeysForPaper(paperId, "tenant-1"))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("Failed to fetch answer keys for paper");
    }

    @Test
    @DisplayName("Throws UpstreamServiceUnavailableException on batch fetch when gRPC server is unreachable")
    void shouldThrowUpstreamExceptionOnBatchFetchWhenServerUnreachable() {
        List<UUID> questionIds = List.of(UUID.randomUUID());

        assertThatThrownBy(() -> client.batchGetAnswerKeys(questionIds, "tenant-1"))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("Failed to batch fetch answer keys");
    }

    @Test
    @DisplayName("Returns empty list when batchGetAnswerKeys is passed empty or null list")
    void shouldReturnEmptyListForEmptyQuestionIds() {
        List<AnswerKey> resultNull = client.batchGetAnswerKeys(null, "tenant-1");
        List<AnswerKey> resultEmpty = client.batchGetAnswerKeys(List.of(), "tenant-1");

        assertThat(resultNull).isEmpty();
        assertThat(resultEmpty).isEmpty();
    }

    @Test
    @DisplayName("Fallback throws UpstreamServiceUnavailableException when circuit is open")
    void shouldThrowFromFallbackMethods() {
        UUID paperId = UUID.randomUUID();
        Throwable cause = new RuntimeException("Circuit breaker open");

        assertThatThrownBy(() -> client.fetchAnswerKeysForPaperFallback(paperId, "tenant-1", cause))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("QuestionBank service unavailable for paper");

        assertThatThrownBy(() -> client.batchGetAnswerKeysFallback(List.of(paperId), "tenant-1", cause))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("QuestionBank service unavailable for batch questions");
    }
}
