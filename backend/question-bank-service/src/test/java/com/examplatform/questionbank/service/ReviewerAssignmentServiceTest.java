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

package com.examplatform.questionbank.service;

import com.examplatform.questionbank.client.ReviewerPoolClient;
import com.examplatform.questionbank.dto.ReviewerAssignment;
import com.examplatform.questionbank.dto.ReviewerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewerAssignmentServiceTest {

    @Mock
    private ReviewerPoolClient reviewerPoolClient;

    private ReviewerAssignmentService reviewerAssignmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID authorId;
    private UUID reviewer1Id;
    private UUID reviewer2Id;
    private UUID reviewerPhysicsId;
    private UUID controllerId;
    private final String tenantId = "default";

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        reviewer1Id = UUID.randomUUID();
        reviewer2Id = UUID.randomUUID();
        reviewerPhysicsId = UUID.randomUUID();
        controllerId = UUID.randomUUID();

        // ObjectProvider returning null for RedisTemplate to use in-memory cache/load tracking
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);

        reviewerAssignmentService = new ReviewerAssignmentService(reviewerPoolClient, redisProvider, objectMapper);
    }

    @Test
    @DisplayName("Single review assignment assigns specialist matching subject")
    void assignReviewers_assignsSubjectSpecialist() {
        ReviewerDto mathReviewer = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_sme")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        ReviewerDto physicsReviewer = ReviewerDto.builder()
                .id(reviewerPhysicsId)
                .username("physics_sme")
                .specialization("Physics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(mathReviewer, physicsReviewer));

        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                "Mathematics", authorId, tenantId, false);

        assertThat(assignment.getPrimaryReviewerId()).isEqualTo(reviewer1Id);
        assertThat(assignment.getSecondaryReviewerId()).isNull();
        assertThat(assignment.getAssignedReviewerIds()).containsExactly(reviewer1Id);
        assertThat(assignment.isEscalatedToController()).isFalse();
    }

    @Test
    @DisplayName("Conflict of interest check excludes author from being assigned as reviewer")
    void assignReviewers_excludesAuthor() {
        ReviewerDto authorAsReviewer = ReviewerDto.builder()
                .id(authorId)
                .username("author_user")
                .specialization("Mathematics")
                .roles(List.of("REVIEWER"))
                .build();

        ReviewerDto otherReviewer = ReviewerDto.builder()
                .id(reviewer2Id)
                .username("math_reviewer2")
                .specialization("Mathematics")
                .roles(List.of("REVIEWER"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(authorAsReviewer, otherReviewer));

        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                "Mathematics", authorId, tenantId, false);

        assertThat(assignment.getPrimaryReviewerId()).isEqualTo(reviewer2Id);
        assertThat(assignment.getAssignedReviewerIds()).doesNotContain(authorId);
    }

    @Test
    @DisplayName("Dual review assignment selects two distinct reviewers")
    void assignReviewers_dualReview_selectsTwoDistinctReviewers() {
        ReviewerDto reviewer1 = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_1")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        ReviewerDto reviewer2 = ReviewerDto.builder()
                .id(reviewer2Id)
                .username("math_2")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(reviewer1, reviewer2));

        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                "Mathematics", authorId, tenantId, true);

        assertThat(assignment.getPrimaryReviewerId()).isNotNull();
        assertThat(assignment.getSecondaryReviewerId()).isNotNull();
        assertThat(assignment.getPrimaryReviewerId()).isNotEqualTo(assignment.getSecondaryReviewerId());
        assertThat(assignment.getAssignedReviewerIds()).containsExactlyInAnyOrder(reviewer1Id, reviewer2Id);
    }

    @Test
    @DisplayName("Fallback escalation to controller / general pool when no subject specialist exists")
    void assignReviewers_escalatesWhenNoSpecialist() {
        ReviewerDto controller = ReviewerDto.builder()
                .id(controllerId)
                .username("controller1")
                .specialization(null)
                .roles(List.of("EXAM_CONTROLLER"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(controller));

        ReviewerAssignment assignment = reviewerAssignmentService.assignReviewers(
                "Geology", authorId, tenantId, false);

        assertThat(assignment.getPrimaryReviewerId()).isEqualTo(controllerId);
        assertThat(assignment.isEscalatedToController()).isTrue();
    }

    @Test
    @DisplayName("Least-loaded assignment selects reviewer with fewer active reviews")
    void assignReviewers_leastLoadedSelection() {
        ReviewerDto rev1 = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_1")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        ReviewerDto rev2 = ReviewerDto.builder()
                .id(reviewer2Id)
                .username("math_2")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(rev1, rev2));

        // First assignment assigns one and increments its load
        ReviewerAssignment first = reviewerAssignmentService.assignReviewers("Mathematics", authorId, tenantId, false);
        UUID firstAssigned = first.getPrimaryReviewerId();

        // Second assignment should prefer the other reviewer who has lower load
        ReviewerAssignment second = reviewerAssignmentService.assignReviewers("Mathematics", authorId, tenantId, false);
        UUID secondAssigned = second.getPrimaryReviewerId();

        assertThat(secondAssigned).isNotEqualTo(firstAssigned);
    }

    @Test
    @DisplayName("Reviewer load can be released on review completion")
    void releaseReviewerLoad_decrementsLoad() {
        ReviewerDto rev1 = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_1")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(rev1));

        reviewerAssignmentService.assignReviewers("Mathematics", authorId, tenantId, false);
        assertThat(reviewerAssignmentService.getReviewerLoad(reviewer1Id, tenantId)).isEqualTo(1);

        reviewerAssignmentService.releaseReviewerLoad(reviewer1Id, tenantId);
        assertThat(reviewerAssignmentService.getReviewerLoad(reviewer1Id, tenantId)).isEqualTo(0);
    }

    @Test
    @DisplayName("Reviewer pool lookup is cached across multiple calls")
    void getCachedReviewerPool_cachesResults() {
        ReviewerDto rev1 = ReviewerDto.builder()
                .id(reviewer1Id)
                .username("math_1")
                .specialization("Mathematics")
                .roles(List.of("SUBJECT_MATTER_EXPERT"))
                .build();

        when(reviewerPoolClient.getReviewers(anyString(), anyString()))
                .thenReturn(List.of(rev1));

        reviewerAssignmentService.getCachedReviewerPool("Mathematics", tenantId);
        reviewerAssignmentService.getCachedReviewerPool("Mathematics", tenantId);

        verify(reviewerPoolClient, times(1)).getReviewers("Mathematics", tenantId);
    }
}
