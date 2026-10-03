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

package com.examplatform.questionbank.client;

import com.examplatform.questionbank.dto.ReviewerDto;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewerPoolClientImpl Tests")
class ReviewerPoolClientImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ServiceAccountTokenProvider tokenProvider;

    private ReviewerPoolClientImpl reviewerPoolClient;

    private static final String DUMMY_IDENTITY_URL = "http://127.0.0.1:59999";
    private static final String JWT_SECRET = "test-secret-key-that-is-at-least-32-chars-long-for-hmac";

    @BeforeEach
    void setUp() {
        reviewerPoolClient = new ReviewerPoolClientImpl(
                jdbcTemplate,
                tokenProvider,
                DUMMY_IDENTITY_URL,
                JWT_SECRET
        );
    }

    @Test
    @DisplayName("Falls back to JDBC when REST endpoint is unreachable and filters by subject")
    @SuppressWarnings("unchecked")
    void getReviewers_fallsBackToJdbcAndFiltersSubject() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        ReviewerDto mathReviewer = ReviewerDto.builder()
                .id(u1)
                .username("math_expert")
                .specialization("Mathematics")
                .accountStatus("ACTIVE")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        ReviewerDto physicsReviewer = ReviewerDto.builder()
                .id(u2)
                .username("physics_expert")
                .specialization("Physics")
                .accountStatus("ACTIVE")
                .roles(List.of("REVIEWER"))
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("tenant-1")))
                .thenReturn(List.of(mathReviewer, physicsReviewer));

        List<ReviewerDto> result = reviewerPoolClient.getReviewers("Mathematics", "tenant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("math_expert");
        assertThat(result.get(0).getSpecialization()).isEqualTo("Mathematics");
    }

    @Test
    @DisplayName("JDBC fallback returns general reviewers if no exact subject match found")
    @SuppressWarnings("unchecked")
    void getReviewers_fallsBackToAllWhenNoSpecialistMatch() {
        UUID u1 = UUID.randomUUID();

        ReviewerDto generalReviewer = ReviewerDto.builder()
                .id(u1)
                .username("general_reviewer")
                .specialization(null)
                .accountStatus("ACTIVE")
                .roles(List.of("EXAM_CONTROLLER"))
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("tenant-1")))
                .thenReturn(List.of(generalReviewer));

        List<ReviewerDto> result = reviewerPoolClient.getReviewers("Economics", "tenant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("general_reviewer");
    }

    @Test
    @DisplayName("Returns empty list when jdbcTemplate is null and REST fails")
    void getReviewers_nullJdbcTemplateReturnsEmptyList() {
        ReviewerPoolClientImpl clientWithoutDb = new ReviewerPoolClientImpl(
                null,
                null,
                DUMMY_IDENTITY_URL,
                JWT_SECRET
        );

        List<ReviewerDto> result = clientWithoutDb.getReviewers("Mathematics", "tenant-1");
        assertThat(result).isEmpty();
    }
}
