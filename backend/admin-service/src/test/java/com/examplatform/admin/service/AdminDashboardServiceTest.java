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

package com.examplatform.admin.service;

import com.examplatform.admin.dto.DashboardSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService Unit Tests")
class AdminDashboardServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private AdminDashboardService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(objectMapper, RestClient.builder(), redisTemplate, null);
    }

    @Test
    @DisplayName("Should aggregate dashboard summary with active sessions from Redis")
    void shouldAggregateWithRedisSessions() {
        when(redisTemplate.keys(eq("session:*"))).thenReturn(Set.of("session:1", "session:2", "session:3"));

        DashboardSummaryResponse summary = service.getDashboardSummary("test-tenant");

        assertThat(summary).isNotNull();
        assertThat(summary.tenantId()).isEqualTo("test-tenant");
        assertThat(summary.kpis()).isNotNull();
        assertThat(summary.kpis().activeSessions()).isEqualTo(3L);
        assertThat(summary.examBreakdown()).isNotNull();
        assertThat(summary.questionBreakdown()).isNotNull();
        assertThat(summary.evaluationBreakdown()).isNotNull();
        assertThat(summary.systemServices()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return clean zero baselines when downstream RPC/REST are unreachable without mock data")
    void shouldFallbackWhenUnreachable() {
        DashboardSummaryResponse summary = service.getDashboardSummary(null);

        assertThat(summary).isNotNull();
        assertThat(summary.tenantId()).isEqualTo("default");
        assertThat(summary.kpis().totalQuestions()).isEqualTo(0L);
        assertThat(summary.kpis().pendingReviewQuestions()).isEqualTo(0L);
        assertThat(summary.kpis().registeredCandidates()).isEqualTo(0L);
    }
}
