/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.dto.ActivityCompletionResponse;
import com.examplatform.recommendation.dto.CompleteActivityRequest;
import com.examplatform.recommendation.dto.LearnerProgressResponse;
import com.examplatform.recommendation.dto.NextActivityResponse;
import com.examplatform.recommendation.service.NextBestActivityEngine;
import com.examplatform.recommendation.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LearnerActivityController REST Endpoints E2E Tests (MockMvc)")
class LearnerActivityControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private NextBestActivityEngine nextBestActivityEngine;

    private static final UUID ENROLLMENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTIVITY_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final UUID CANDIDATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("GET /api/v1/learning/enrollments/{id}/next-activity - Returns next best activity")
    void shouldReturnNextActivity() throws Exception {
        NextActivityResponse response = new NextActivityResponse(
                ACTIVITY_ID,
                "LESSON",
                1L,
                "Reasoning",
                10L,
                "Logical Deductions",
                101L,
                "Syllogisms",
                UUID.randomUUID(),
                "CONCEPT",
                "Conceptual Primer",
                null,
                0.85,
                "Targeted foundation building"
        );

        when(nextBestActivityEngine.determineNextActivity(eq(ENROLLMENT_ID))).thenReturn(response);

        mockMvc.perform(get("/api/v1/learning/enrollments/{id}/next-activity", ENROLLMENT_ID)
                        .with(jwt().jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityType").value("LESSON"))
                .andExpect(jsonPath("$.subtopicId").value(101))
                .andExpect(jsonPath("$.lessonType").value("CONCEPT"));
    }

    @Test
    @DisplayName("POST /api/v1/learning/enrollments/{id}/activities/{aid}/complete - Submits completion and updates mastery")
    void shouldCompleteActivity() throws Exception {
        CompleteActivityRequest req = new CompleteActivityRequest(
                "PRACTICE", 101L, null, null, null, 10, 8, 90000L
        );
        ActivityCompletionResponse response = new ActivityCompletionResponse(
                ACTIVITY_ID, "PRACTICE", 101L, 0.75, 0.60, "PRACTISING", Instant.now(), 0.65
        );

        when(nextBestActivityEngine.completeActivity(eq(ENROLLMENT_ID), eq(ACTIVITY_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/learning/enrollments/{id}/activities/{aid}/complete", ENROLLMENT_ID, ACTIVITY_ID)
                        .with(jwt().jwt(j -> j.subject(CANDIDATE_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityType").value("PRACTICE"))
                .andExpect(jsonPath("$.newMasteryScore").value(0.75))
                .andExpect(jsonPath("$.updatedReadinessScore").value(0.65));
    }

    @Test
    @DisplayName("GET /api/v1/learning/enrollments/{id}/progress - Returns syllabus mastery tree and readiness")
    void shouldReturnProgress() throws Exception {
        LearnerProgressResponse response = new LearnerProgressResponse(
                ENROLLMENT_ID, CANDIDATE_ID, UUID.randomUUID(), 0.72, 5, 2, 3, 1, Collections.emptyList()
        );

        when(nextBestActivityEngine.getProgress(eq(ENROLLMENT_ID))).thenReturn(response);

        mockMvc.perform(get("/api/v1/learning/enrollments/{id}/progress", ENROLLMENT_ID)
                        .with(jwt().jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(ENROLLMENT_ID.toString()))
                .andExpect(jsonPath("$.readinessScore").value(0.72))
                .andExpect(jsonPath("$.totalSubtopics").value(5))
                .andExpect(jsonPath("$.masteredSubtopics").value(2));
    }
}
