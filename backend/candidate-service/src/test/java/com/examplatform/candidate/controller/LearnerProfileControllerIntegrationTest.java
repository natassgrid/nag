/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU标志 Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.controller;

import com.examplatform.candidate.client.ExaminationClient;
import com.examplatform.candidate.dto.*;
import com.examplatform.candidate.service.LearnerService;
import com.examplatform.candidate.support.AbstractIntegrationTest;
import com.examplatform.shared.constants.SharedConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LearnerProfileController Integration Tests")
class LearnerProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private LearnerService learnerService;

    @MockitoBean
    private ExaminationClient examinationClient;

    private final UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID examId = UUID.fromString("11111111-2222-3333-4444-555555555551");
    private final UUID enrollmentId = UUID.fromString("22222222-3333-4444-5555-666666666661");

    @Test
    @DisplayName("GET /api/v1/learner/profile returns learner profile")
    void shouldGetProfile() throws Exception {
        LearnerProfileDto dto = new LearnerProfileDto(
                candidateId, "Student One", "GRADUATION", "en", 60,
                List.of("CONCEPT"), "Fresher", LocalDate.now(), Instant.now(), true
        );
        when(learnerService.getProfile(eq(candidateId), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/learner/profile")
                        .with(jwt().jwt(j -> j.subject(candidateId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(candidateId.toString()))
                .andExpect(jsonPath("$.displayName").value("Student One"))
                .andExpect(jsonPath("$.isComplete").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/learner/profile updates profile")
    void shouldUpdateProfile() throws Exception {
        UpdateLearnerProfileRequest req = new UpdateLearnerProfileRequest(
                "Student Updated", "POST_GRADUATION", "hi", 90,
                List.of("WORKED_EXAMPLES"), "Intermediate", LocalDate.now().plusMonths(6)
        );
        LearnerProfileDto updated = new LearnerProfileDto(
                candidateId, "Student Updated", "POST_GRADUATION", "hi", 90,
                List.of("WORKED_EXAMPLES"), "Intermediate", LocalDate.now().plusMonths(6), Instant.now(), true
        );
        when(learnerService.updateProfile(eq(candidateId), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/learner/profile")
                        .with(jwt().jwt(j -> j.subject(candidateId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Student Updated"))
                .andExpect(jsonPath("$.educationLevel").value("POST_GRADUATION"));
    }

    @Test
    @DisplayName("GET /api/v1/learner/exams returns published exams catalogue")
    void shouldGetExams() throws Exception {
        PublishedExamDto exam = new PublishedExamDto(
                examId, SharedConstants.JEE_MAIN, "JEE Main Examination", "Description", "PUBLISHED", 180, 300, UUID.randomUUID(), 3, 30
        );
        when(learnerService.getPublishedExams(any())).thenReturn(List.of(exam));

        mockMvc.perform(get("/api/v1/learner/exams")
                        .with(jwt().jwt(j -> j.subject(candidateId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(SharedConstants.JEE_MAIN))
                .andExpect(jsonPath("$[0].name").value("JEE Main Examination"));
    }

    @Test
    @DisplayName("POST /api/v1/learner/enrollments creates target exam enrollment")
    void shouldCreateEnrollment() throws Exception {
        CreateEnrollmentRequest req = new CreateEnrollmentRequest(examId, LocalDate.now().plusMonths(3));
        LearnerEnrollmentResponse res = new LearnerEnrollmentResponse(
                enrollmentId, candidateId, examId, "JEE Main",
                LocalDate.now().plusMonths(3), "ACTIVE", "NOT_STARTED", null, Instant.now()
        );
        when(learnerService.createEnrollment(eq(candidateId), any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/learner/enrollments")
                        .with(jwt().jwt(j -> j.subject(candidateId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(enrollmentId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.examinationId").value(examId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/learner/enrollments/{id}/diagnostic starts diagnostic session")
    void shouldStartDiagnostic() throws Exception {
        DiagnosticSessionResponse res = new DiagnosticSessionResponse(
                UUID.randomUUID(), enrollmentId, candidateId, examId, SharedConstants.IN_PROGRESS,
                Collections.emptyList(), 10, Instant.now()
        );
        when(learnerService.startDiagnostic(eq(candidateId), eq(enrollmentId), any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/learner/enrollments/{id}/diagnostic", enrollmentId)
                        .with(jwt().jwt(j -> j.subject(candidateId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(enrollmentId.toString()))
                .andExpect(jsonPath("$.status").value(SharedConstants.IN_PROGRESS));
    }

    @Test
    @DisplayName("POST /api/v1/learner/enrollments/{id}/diagnostic/submit submits diagnostic results")
    void shouldSubmitDiagnostic() throws Exception {
        SubmitDiagnosticRequest req = new SubmitDiagnosticRequest(Collections.emptyList(), true);
        DiagnosticResultResponse res = new DiagnosticResultResponse(
                UUID.randomUUID(), enrollmentId, "SKIPPED", 0.0, 0, 0, 0.0, Collections.emptyMap(), Instant.now()
        );
        when(learnerService.submitDiagnostic(eq(candidateId), eq(enrollmentId), any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/learner/enrollments/{id}/diagnostic/submit", enrollmentId)
                        .with(jwt().jwt(j -> j.subject(candidateId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }
}
