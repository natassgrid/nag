// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.domain.MicroLessonStatus;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateMicroLessonsRequest;
import com.examplatform.questionbank.microlesson.dto.BatchGenerateResponse;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.ReviewMicroLessonRequest;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.service.MicroLessonBatchRunner;
import com.examplatform.questionbank.microlesson.service.MicroLessonCacheService;
import com.examplatform.questionbank.microlesson.service.MicroLessonGeneratorService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminMicroLessonController Review & Moderation Desk E2E Tests (MockMvc)")
class AdminMicroLessonControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SubtopicMicroLessonRepository repository;

    @MockitoBean
    private MicroLessonGeneratorService generatorService;

    @MockitoBean
    private MicroLessonBatchRunner batchRunner;

    @MockitoBean
    private MicroLessonCacheService cacheService;

    private static final UUID LESSON_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID REVIEWER_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private SubtopicMicroLesson buildSampleEntity(String status) {
        return SubtopicMicroLesson.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .subject("Physics")
                .topic("Mechanics")
                .subtopic("Moment of Inertia")
                .targetExam("JEE_MAIN")
                .coreConcept("Rotational mass distribution concept.")
                .trap("Neglecting the axis perpendicular distance.")
                .cheatSheetJson("[\"$I = m r^2$\"]")
                .stepByStepBreakdownJson("{\"questionStem\": \"Find I\", \"steps\": [\"Step 1\"], \"finalAnswer\": \"2 kg m^2\"}")
                .status(status)
                .contentVersion(1)
                .build();
    }

    private MicroLessonResponse buildSampleResponse(String status) {
        return new MicroLessonResponse(
                LESSON_ID,
                1L,
                10L,
                100L,
                "Physics",
                "Mechanics",
                "Moment of Inertia",
                "JEE_MAIN",
                "Rotational mass distribution concept.",
                "Neglecting the axis perpendicular distance.",
                List.of("$I = m r^2$"),
                new StepByStepBreakdownDto("Find I", List.of("Step 1"), "2 kg m^2"),
                status,
                null,
                null,
                null,
                1,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/admin/micro-lessons")
    class ListEndpoint {

        @Test
        @DisplayName("200 OK - Retrieves paginated micro-lessons awaiting review")
        void testListAwaitingReview() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            MicroLessonResponse response = buildSampleResponse("AI_GENERATED");

            when(repository.findByStatus(eq("AI_GENERATED"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(generatorService.toResponse(any())).thenReturn(response);

            mockMvc.perform(get("/api/admin/micro-lessons")
                            .param("status", "AI_GENERATED")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].subtopic").value("Moment of Inertia"))
                    .andExpect(jsonPath("$.content[0].status").value("AI_GENERATED"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/micro-lessons/{id}/review")
    class ReviewEndpoint {

        @Test
        @DisplayName("200 OK - Faculty approves and publishes micro-lesson, caching it in Redis")
        void testApproveAndPublish() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            MicroLessonResponse approvedResponse = buildSampleResponse("PUBLISHED");

            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(generatorService.toResponse(any())).thenReturn(approvedResponse);

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "PUBLISHED",
                    "Pedagogically verified and ready for candidate remediation.",
                    null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/{id}/review", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));

            verify(cacheService).put(eq(100L), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST - Rejects review when KaTeX syntax is malformed")
        void testRejectMalformedKaTex() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));

            // Malformed unclosed dollar delimiter
            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "APPROVED",
                    "Comment",
                    "Valid core concept with three sentences.",
                    "Valid trap.",
                    List.of("$I = m r^2 without closing dollar"),
                    null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/{id}/review", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/micro-lessons/batch-generate")
    class BatchGenerateEndpoint {

        @Test
        @DisplayName("200 OK - Admin triggers syllabus batch generation")
        void testBatchGenerate() throws Exception {
            BatchGenerateResponse batchResponse = new BatchGenerateResponse(
                    "COMPLETED", 5, 5, 0, List.of("Moment of Inertia", "Friction"), "Batch completed successfully"
            );
            when(batchRunner.runBatchGeneration(any())).thenReturn(batchResponse);

            BatchGenerateMicroLessonsRequest req = new BatchGenerateMicroLessonsRequest(1L, 10L, null, "JEE_MAIN", 5);

            mockMvc.perform(post("/api/admin/micro-lessons/batch-generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.totalGenerated").value(5));
        }
    }
}
