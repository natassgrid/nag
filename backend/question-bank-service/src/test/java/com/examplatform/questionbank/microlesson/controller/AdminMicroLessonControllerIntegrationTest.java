// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.ai.MicroLessonBatchRunner;
import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.*;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.service.MicroLessonCacheService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.shared.constants.SharedConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @MockitoBean
    private com.examplatform.questionbank.repository.SubjectRepository subjectRepository;

    @MockitoBean
    private com.examplatform.questionbank.repository.TopicRepository topicRepository;

    @MockitoBean
    private com.examplatform.questionbank.repository.SubtopicRepository subtopicRepository;

    private static final UUID LESSON_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID REVIEWER_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private SubtopicMicroLesson buildSampleEntity(String status) {
        return SubtopicMicroLesson.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
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
            SharedConstants.JEE_MAIN,
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
            when(generatorService.toResponse(entity)).thenReturn(response);

            mockMvc.perform(get("/api/admin/micro-lessons")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].subtopic").value("Moment of Inertia"))
                    .andExpect(jsonPath("$.content[0].status").value("AI_GENERATED"));
        }

        @Test
        @DisplayName("200 OK - Filters by status and subjectId")
        void testListWithFilters() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("APPROVED");
            MicroLessonResponse response = buildSampleResponse("APPROVED");

            when(repository.findByStatusAndSubjectId(eq("APPROVED"), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(generatorService.toResponse(entity)).thenReturn(response);

            mockMvc.perform(get("/api/admin/micro-lessons")
                            .param("status", "APPROVED")
                            .param("subjectId", "1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
        }

        @Test
        @DisplayName("200 OK - Filters with specification when topic/query provided")
        void testListWithSpecification() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("APPROVED");
            MicroLessonResponse response = buildSampleResponse("APPROVED");

            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(generatorService.toResponse(entity)).thenReturn(response);

            mockMvc.perform(get("/api/admin/micro-lessons")
                            .param("topic", "Mechanics")
                            .param("query", "Inertia")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].topic").value("Mechanics"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/micro-lessons/stats")
    class StatsEndpoint {

        @Test
        @DisplayName("200 OK - Returns aggregate micro-lesson statistics")
        void testGetStats() throws Exception {
            when(repository.count()).thenReturn(45L);
            when(repository.countByStatus("AI_GENERATED")).thenReturn(15L);
            when(repository.countByStatus("APPROVED")).thenReturn(20L);
            when(repository.countByStatus("PUBLISHED")).thenReturn(10L);

            mockMvc.perform(get("/api/admin/micro-lessons/stats")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(45))
                    .andExpect(jsonPath("$.aiGenerated").value(15))
                    .andExpect(jsonPath("$.approved").value(20));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/micro-lessons/filter-options")
    class FilterOptionsEndpoint {

        @Test
        @DisplayName("200 OK - Returns unique filter options")
        void testGetFilterOptions() throws Exception {
            com.examplatform.questionbank.domain.Subject s = com.examplatform.questionbank.domain.Subject.builder().name("Physics").build();
            ReflectionTestUtils.setField(s, "id", 1L);
            when(subjectRepository.findByTenantIdIsNullOrderByDisplayOrderAscNameAsc())
                    .thenReturn(List.of(
                            s,
                            com.examplatform.questionbank.domain.Subject.builder().name("Chemistry").build()
                    ));
            when(subjectRepository.findByNameAndTenantIdIsNull("Physics")).thenReturn(Optional.of(s));
            when(topicRepository.findBySubjectIdAndTenantIdIsNullOrderByDisplayOrderAscNameAsc(1L))
                    .thenReturn(List.of(
                            com.examplatform.questionbank.domain.Topic.builder().name("Mechanics").build(),
                            com.examplatform.questionbank.domain.Topic.builder().name("Thermodynamics").build()
                    ));

            mockMvc.perform(get("/api/admin/micro-lessons/filter-options")
                            .param("subject", "Physics")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subjects[0]").value("Physics"))
                    .andExpect(jsonPath("$.topics[0]").value("Mechanics"))
                    .andExpect(jsonPath("$.statuses[0]").value("ALL"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/micro-lessons")
    class DeleteEndpoints {

        @Test
        @DisplayName("204 No Content - Deletes single micro-lesson")
        void testDeleteSingle() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));

            mockMvc.perform(delete("/api/admin/micro-lessons/" + LESSON_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isNoContent());

            verify(cacheService).evict(100L);
            verify(repository).delete(entity);
        }

        @Test
        @DisplayName("200 OK - Bulk deletes micro-lessons by IDs")
        void testBulkDelete() throws Exception {
            SubtopicMicroLesson entity1 = buildSampleEntity("AI_GENERATED");
            when(repository.findAllById(List.of(LESSON_ID))).thenReturn(List.of(entity1));

            BulkDeleteMicroLessonsRequest req = new BulkDeleteMicroLessonsRequest(List.of(LESSON_ID));

            mockMvc.perform(post("/api/admin/micro-lessons/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(1));

            verify(cacheService).evict(100L);
            verify(repository).deleteAll(List.of(entity1));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/micro-lessons/{id}/review")
    class ReviewEndpoint {

        @Test
        @DisplayName("200 OK - Faculty approves micro-lesson with comments")
        void testApproveMicroLesson() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(generatorService.toResponse(any(SubtopicMicroLesson.class)))
                    .thenReturn(buildSampleResponse("APPROVED"));

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "APPROVED", "Excellent native analogy", null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(cacheService).evict(100L);
        }

        @Test
        @DisplayName("200 OK - Faculty publishes micro-lesson and populates near cache")
        void testPublishMicroLesson() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("APPROVED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            MicroLessonResponse pubResponse = buildSampleResponse("PUBLISHED");
            when(generatorService.toResponse(any(SubtopicMicroLesson.class)))
                    .thenReturn(pubResponse);

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "PUBLISHED", "Verified ready for student delivery", null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));

            verify(cacheService).put(eq(100L), any(MicroLessonResponse.class));
        }

        @Test
        @DisplayName("200 OK - Faculty rejects micro-lesson using verb 'REJECT'")
        void testRejectMicroLessonWithActionVerb() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(generatorService.toResponse(any(SubtopicMicroLesson.class)))
                    .thenReturn(buildSampleResponse("REJECTED"));

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "REJECT", "Incomplete KaTeX formulas", null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(cacheService).evict(100L);
        }

        @Test
        @DisplayName("200 OK - Faculty rejects micro-lesson using participle 'REJECTED'")
        void testRejectMicroLessonWithStatusParticiple() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(generatorService.toResponse(any(SubtopicMicroLesson.class)))
                    .thenReturn(buildSampleResponse("REJECTED"));

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "REJECTED", "Content does not meet guidelines", null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(cacheService).evict(100L);
        }

        @Test
        @DisplayName("200 OK - Faculty approves micro-lesson using action verb 'APPROVE'")
        void testApproveMicroLessonWithActionVerb() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));
            when(generatorService.toResponse(any(SubtopicMicroLesson.class)))
                    .thenReturn(buildSampleResponse("APPROVED"));

            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "APPROVE", "Looks great", null, null, null, null
            );

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(cacheService).evict(100L);
        }

        @Test
        @DisplayName("400 Bad Request - When status transition is missing or invalid")
        void testInvalidStatusTransition() throws Exception {
            ReviewMicroLessonRequest req = new ReviewMicroLessonRequest(
                    "INVALID_STATUS", null, null, null, null, null
            );

            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(buildSampleEntity("AI_GENERATED")));

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID + "/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/micro-lessons/generate")
    class GenerateEndpoint {

        @Test
        @DisplayName("200 OK - Generates single micro-lesson with syllabus taxonomy and reference notes")
        void testGenerateSingleMicroLesson() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("AI_GENERATED");
            MicroLessonResponse response = buildSampleResponse("AI_GENERATED");

            when(generatorService.generateForSubtopic(
                    eq(1L), eq("Physics"), eq(10L), eq("Mechanics"),
                    eq(100L), eq("Moment of Inertia"), eq(SharedConstants.JEE_MAIN), eq("Custom notes")
            )).thenReturn(entity);
            when(generatorService.toResponse(entity)).thenReturn(response);

            GenerateMicroLessonRequest req = new GenerateMicroLessonRequest(
                    1L, "Physics", 10L, "Mechanics", 100L, "Moment of Inertia", SharedConstants.JEE_MAIN, "Custom notes", null
            );

            mockMvc.perform(post("/api/admin/micro-lessons/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subtopic").value("Moment of Inertia"))
                    .andExpect(jsonPath("$.status").value("AI_GENERATED"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/micro-lessons/batch-generate")
    class BatchGenerateEndpoint {

        @Test
        @DisplayName("200 OK - Triggers batch runner and returns tracking summary")
        void testTriggerBatchRunner() throws Exception {
            BatchGenerateResponse mockResp = new BatchGenerateResponse(
                    "batch-123",
                SharedConstants.IN_PROGRESS,
                    5,
                    2,
                    0,
                    40,
                    "Subtopic C",
                    List.of("Subtopic A", "Subtopic B"),
                    List.of(),
                    "Batch job queued with 5 subtopics"
            );

            when(batchRunner.startBatchJob(any(BatchGenerateMicroLessonsRequest.class)))
                    .thenReturn(mockResp);

            BatchGenerateMicroLessonsRequest req = new BatchGenerateMicroLessonsRequest(
                    1L, 10L, List.of(100L, 101L), SharedConstants.JEE_MAIN, 10
            );

            mockMvc.perform(post("/api/admin/micro-lessons/batch-generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.batchId").value("batch-123"))
                    .andExpect(jsonPath("$.status").value(SharedConstants.IN_PROGRESS))
                    .andExpect(jsonPath("$.percentComplete").value(40))
                    .andExpect(jsonPath("$.currentSubtopic").value("Subtopic C"))
                    .andExpect(jsonPath("$.totalGenerated").value(2));
        }

        @Test
        @DisplayName("200 OK - Retrieves batch job status by ID")
        void testGetBatchJobStatus() throws Exception {
            BatchGenerateResponse mockResp = new BatchGenerateResponse(
                    "batch-123",
                SharedConstants.IN_PROGRESS,
                    10,
                    5,
                    0,
                    50,
                    "Subtopic 6",
                    List.of("Subtopic 1", "Subtopic 2"),
                    List.of(),
                    "In progress"
            );

            when(batchRunner.getJobStatus("batch-123")).thenReturn(Optional.of(mockResp));

            mockMvc.perform(get("/api/admin/micro-lessons/batch-jobs/batch-123")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.batchId").value("batch-123"))
                    .andExpect(jsonPath("$.percentComplete").value(50));
        }

        @Test
        @DisplayName("200 OK - Retrieves active batch job")
        void testGetActiveBatchJob() throws Exception {
            BatchGenerateResponse mockResp = new BatchGenerateResponse(
                    "batch-active-1",
                SharedConstants.IN_PROGRESS,
                    8,
                    4,
                    0,
                    50,
                    "Subtopic 5",
                    List.of("Subtopic 1"),
                    List.of(),
                    "Running"
            );

            when(batchRunner.getActiveJob()).thenReturn(mockResp);

            mockMvc.perform(get("/api/admin/micro-lessons/batch-jobs/active")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.batchId").value("batch-active-1"))
                    .andExpect(jsonPath("$.status").value(SharedConstants.IN_PROGRESS));
        }

        @Test
        @DisplayName("200 OK - Cancels active batch job")
        void testCancelBatchJob() throws Exception {
            BatchGenerateResponse mockResp = new BatchGenerateResponse(
                    "batch-active-1",
                SharedConstants.CANCELLED,
                    8,
                    4,
                    0,
                    50,
                    null,
                    List.of("Subtopic 1"),
                    List.of(),
                    "Generation cancelled by user."
            );

            when(batchRunner.cancelJob("batch-active-1")).thenReturn(Optional.of(mockResp));

            mockMvc.perform(post("/api/admin/micro-lessons/batch-jobs/batch-active-1/cancel")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(SharedConstants.CANCELLED));
        }
    }

    @Nested
    @DisplayName("Manual Micro-Lesson Authoring Studio Endpoints (GET /{id}, POST /, PUT /{id})")
    class ManualAuthoringEndpoints {

        @Test
        @DisplayName("200 OK - Retrieves a single micro-lesson by ID")
        void testGetMicroLessonById() throws Exception {
            SubtopicMicroLesson entity = buildSampleEntity("APPROVED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(entity));
            when(generatorService.toResponse(entity)).thenReturn(buildSampleResponse("APPROVED"));

            mockMvc.perform(get("/api/admin/micro-lessons/" + LESSON_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(LESSON_ID.toString()))
                    .andExpect(jsonPath("$.subtopic").value("Moment of Inertia"))
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("404 Not Found - Returns 404 when micro-lesson ID does not exist")
        void testGetMicroLessonByIdNotFound() throws Exception {
            when(repository.findById(LESSON_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/admin/micro-lessons/" + LESSON_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("201 Created - Creates a manually authored micro-lesson with structured content")
        void testCreateManualMicroLesson() throws Exception {
            when(subtopicRepository.findById(100L)).thenReturn(Optional.of(
                    com.examplatform.questionbank.domain.Subtopic.builder()
                            .name("Moment of Inertia")
                            .topicId(10L)
                            .build()
            ));
            when(repository.findTopBySubtopicIdOrderByContentVersionDesc(100L))
                    .thenReturn(Optional.empty());
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

            String payload = """
                {
                  "subjectId": 1,
                  "subject": "Physics",
                  "topicId": 10,
                  "topic": "Mechanics",
                  "subtopicId": 100,
                  "subtopic": "Moment of Inertia",
                  "targetExam": "JEE_MAIN",
                  "coreConcept": "Moment of inertia $$I = \\\\sum m_i r_i^2$$ quantifies rotational inertia.",
                  "trapAlert": {
                    "title": "Axis Shift Error",
                    "description": "Always check the reference axis before applying standard formulas.",
                    "commonMistake": "Using $$I_{cm}$$ directly for an offset axis.",
                    "correctApproach": "Apply $$I = I_{cm} + Md^2$$."
                  },
                  "formulaCheatSheet": [
                    {
                      "name": "Parallel Axis Theorem",
                      "latex": "$$I = I_{cm} + Md^2$$",
                      "description": "Shifts axis by perpendicular distance $$d$$",
                      "variables": ["$$I_{cm}$$: COM inertia", "$$M$$: Mass"]
                    }
                  ],
                  "stepByStepBreakdown": {
                    "problemStatement": "Find $$I$$ of a rod of mass $$M$$ and length $$L$$ about one end.",
                    "steps": [
                      {
                        "stepNumber": 1,
                        "title": "COM Formula",
                        "explanation": "Start with $$I_{cm} = \\\\frac{1}{12}ML^2$$.",
                        "formula": "$$I_{cm} = \\\\frac{1}{12}ML^2$$"
                      },
                      {
                        "stepNumber": 2,
                        "title": "Apply Parallel Axis Theorem",
                        "explanation": "Add $$M(L/2)^2$$ to get $$\\\\frac{1}{3}ML^2$$.",
                        "formula": "$$I = \\\\frac{1}{3}ML^2$$"
                      }
                    ],
                    "finalAnswer": "$$\\\\frac{1}{3}ML^2$$"
                  },
                  "keyTakeaways": [
                    "Moment of inertia depends on mass distribution and axis position."
                  ],
                  "quickCheckItems": [
                    {
                      "question": "What is $$I$$ of a point mass $$m$$ at distance $$r$$?",
                      "options": ["$$mr^2$$", "$$\\\\frac{1}{2}mr^2$$", "$$2mr^2$$", "$$mr$$"],
                      "correctAnswer": "$$mr^2$$",
                      "explanation": "By definition $$I = mr^2$$."
                    }
                  ],
                  "status": "APPROVED"
                }
                """;

            mockMvc.perform(post("/api/admin/micro-lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subtopic").value("Moment of Inertia"))
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.contentVersion").value(1))
                    .andExpect(jsonPath("$.content.trapAlert.title").value("Axis Shift Error"))
                    .andExpect(jsonPath("$.content.formulaCheatSheet[0].name").value("Parallel Axis Theorem"))
                    .andExpect(jsonPath("$.content.stepByStepBreakdown[0].title").value("COM Formula"))
                    .andExpect(jsonPath("$.content.keyTakeaways[0]").value("Moment of inertia depends on mass distribution and axis position."))
                    .andExpect(jsonPath("$.content.quickCheckItems[0].correctAnswer").value("$$mr^2$$"));
        }

        @Test
        @DisplayName("400 Bad Request - Rejects manual micro-lesson creation when LaTeX is malformed")
        void testCreateManualMicroLessonInvalidLatex() throws Exception {
            String invalidPayload = """
                {
                  "subjectId": 1,
                  "subject": "Physics",
                  "topicId": 10,
                  "topic": "Mechanics",
                  "subtopicId": 100,
                  "subtopic": "Moment of Inertia",
                  "targetExam": "JEE_MAIN",
                  "coreConcept": "Malformed LaTeX $$\\\\frac{1}{2$$",
                  "trap": "Some trap",
                  "cheatSheet": ["$$I = mr^2$$"],
                  "stepByStepBreakdown": {
                    "questionStem": "Find I",
                    "steps": ["Step 1"],
                    "finalAnswer": "Ans"
                  }
                }
                """;

            mockMvc.perform(post("/api/admin/micro-lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FACULTY"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK - Updates existing micro-lesson and increments contentVersion")
        void testUpdateMicroLesson() throws Exception {
            SubtopicMicroLesson existing = buildSampleEntity("APPROVED");
            when(repository.findById(LESSON_ID)).thenReturn(Optional.of(existing));
            when(repository.findTopBySubtopicIdOrderByContentVersionDesc(100L))
                    .thenReturn(Optional.of(existing));
            when(repository.save(any(SubtopicMicroLesson.class))).thenAnswer(inv -> inv.getArgument(0));

            String updatePayload = """
                {
                  "coreConcept": "Updated rotational concept with $$I = \\\\frac{1}{2}MR^2$$.",
                  "status": "PUBLISHED",
                  "reviewComments": "Reviewed and published from Studio"
                }
                """;

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"))
                    .andExpect(jsonPath("$.contentVersion").value(2))
                    .andExpect(jsonPath("$.coreConcept").value("Updated rotational concept with $$I = \\frac{1}{2}MR^2$$."));

            verify(cacheService).put(eq(100L), any(MicroLessonResponse.class));
        }

        @Test
        @DisplayName("404 Not Found - Returns 404 when updating non-existent micro-lesson")
        void testUpdateMicroLessonNotFound() throws Exception {
            when(repository.findById(LESSON_ID)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/admin/micro-lessons/" + LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"coreConcept\": \"Updated\"}")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound());
        }
    }
}

