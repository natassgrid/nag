// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.service.MicroLessonCacheService;
import com.examplatform.questionbank.microlesson.service.MicroLessonGeneratorService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MicroLessonController Candidate-Facing Delivery E2E Tests (MockMvc)")
class MicroLessonControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SubtopicMicroLessonRepository repository;

    @MockitoBean
    private MicroLessonGeneratorService generatorService;

    @MockitoBean
    private MicroLessonCacheService cacheService;

    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final Long SUBTOPIC_ID = 101L;

    private MicroLessonResponse buildSampleResponse() {
        return new MicroLessonResponse(
                UUID.randomUUID(),
                1L,
                10L,
                SUBTOPIC_ID,
                "Physics",
                "Mechanics",
                "Work Energy Theorem",
                "JEE_MAIN",
                "Net work done equals change in kinetic energy.",
                "Ignoring negative work done by non-conservative frictional forces.",
                List.of("$W_{net} = \\Delta K$"),
                new StepByStepBreakdownDto("Find speed", List.of("Step 1"), "10 m/s"),
                "PUBLISHED",
                UUID.randomUUID(),
                Instant.now(),
                "Approved",
                1,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/micro-lessons/subtopic/{subtopicId}")
    class CandidateGetMicroLesson {

        @Test
        @DisplayName("200 OK - Candidate retrieves cached micro-lesson from Redis (<20ms)")
        void testCacheHitReturnsImmediately() throws Exception {
            MicroLessonResponse cachedResp = buildSampleResponse();
            when(cacheService.getCached(SUBTOPIC_ID)).thenReturn(Optional.of(cachedResp));

            mockMvc.perform(get("/api/micro-lessons/subtopic/{subtopicId}", SUBTOPIC_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subtopicId").value(SUBTOPIC_ID))
                    .andExpect(jsonPath("$.subtopic").value("Work Energy Theorem"))
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));

            // Verify DB repository was never touched on cache hit
            verify(repository, never()).findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(any(), any(), any());
        }

        @Test
        @DisplayName("200 OK - On cache miss, loads from DB and populates Redis cache")
        void testCacheMissLoadsAndCaches() throws Exception {
            when(cacheService.getCached(SUBTOPIC_ID)).thenReturn(Optional.empty());

            SubtopicMicroLesson dbLesson = SubtopicMicroLesson.builder()
                    .subtopicId(SUBTOPIC_ID)
                    .subtopic("Work Energy Theorem")
                    .status("PUBLISHED")
                    .build();
            MicroLessonResponse resp = buildSampleResponse();

            when(repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(eq(SUBTOPIC_ID), eq("JEE_MAIN"), eq("PUBLISHED")))
                    .thenReturn(Optional.of(dbLesson));
            when(generatorService.toResponse(dbLesson)).thenReturn(resp);

            mockMvc.perform(get("/api/micro-lessons/subtopic/{subtopicId}", SUBTOPIC_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subtopicId").value(SUBTOPIC_ID));

            // Verify cache was populated
            verify(cacheService).put(eq(SUBTOPIC_ID), eq(resp));
        }

        @Test
        @DisplayName("404 NOT FOUND - Unapproved / AI_GENERATED lessons are not exposed to candidates")
        void testUnpublishedLessonsReturn404() throws Exception {
            when(cacheService.getCached(SUBTOPIC_ID)).thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(any(), any(), eq("PUBLISHED")))
                    .thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(any(), eq("PUBLISHED")))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/micro-lessons/subtopic/{subtopicId}", SUBTOPIC_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - Unauthenticated request is rejected")
        void testUnauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/micro-lessons/subtopic/{subtopicId}", SUBTOPIC_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
