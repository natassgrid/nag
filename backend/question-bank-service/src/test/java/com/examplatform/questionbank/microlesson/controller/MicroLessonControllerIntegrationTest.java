// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.service.MicroLessonCacheService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MicroLessonController Remedial Delivery Integration Tests (MockMvc)")
class MicroLessonControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SubtopicMicroLessonRepository repository;

    @MockitoBean
    private MicroLessonGeneratorService generatorService;

    @MockitoBean
    private MicroLessonCacheService cacheService;

    private static final UUID LESSON_ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    private MicroLessonResponse buildSampleResponse() {
        return new MicroLessonResponse(
                LESSON_ID,
                1L,
                10L,
                100L,
                "Physics",
                "Mechanics",
                "Rotational Motion",
                "JEE_MAIN",
                "Torque produces angular acceleration, analogous to force causing linear acceleration.",
                "Confusing clockwise and counterclockwise torque signs in cross product \\vec{r} \\times \\vec{F}.",
                List.of("$$\\tau = I \\alpha$$", "$$L = I \\omega$$"),
                new StepByStepBreakdownDto(
                        "A uniform disc of mass 2 kg and radius 0.2 m is acted upon by a tangential force of 10 N. Find its angular acceleration.",
                        List.of(
                                "Step 1: Compute torque $\\tau = F \\cdot R = 10 \\times 0.2 = 2\\text{ N}\\cdot\\text{m}$",
                                "Step 2: Moment of inertia $I = \\frac{1}{2} M R^2 = \\frac{1}{2} \\times 2 \\times 0.04 = 0.04\\text{ kg}\\cdot\\text{m}^2$",
                                "Step 3: Angular acceleration $\\alpha = \\frac{\\tau}{I} = \\frac{2}{0.04} = 50\\text{ rad/s}^2$"
                        ),
                        "50 \\text{ rad/s}^2"
                ),
                "PUBLISHED",
                UUID.randomUUID(),
                Instant.now(),
                "Approved by faculty",
                1,
                Instant.now(),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/micro-lessons/subtopic/{subtopicId}")
    class GetMicroLessonForSubtopic {

        @Test
        @DisplayName("200 OK - Delivers micro-lesson from cache when cached")
        void testCacheHitReturnsMicroLesson() throws Exception {
            MicroLessonResponse response = buildSampleResponse();
            when(cacheService.getCached(100L)).thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/micro-lessons/subtopic/100")
                            .param("targetExam", "JEE_MAIN")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subtopic").value("Rotational Motion"))
                    .andExpect(jsonPath("$.targetExam").value("JEE_MAIN"))
                    .andExpect(jsonPath("$.coreConcept").isNotEmpty())
                    .andExpect(jsonPath("$.cheatSheet").isArray())
                    .andExpect(jsonPath("$.stepByStepBreakdown.steps").isArray())
                    .andExpect(jsonPath("$.content.coreConcept").isNotEmpty())
                    .andExpect(jsonPath("$.content.trapAlert.title").isNotEmpty())
                    .andExpect(jsonPath("$.content.formulaCheatSheet").isArray())
                    .andExpect(jsonPath("$.content.stepByStepBreakdown").isArray())
                    .andExpect(jsonPath("$.content.keyTakeaways").isArray());

            verify(cacheService).getCached(100L);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("200 OK - Fetches from database and populates cache on cache miss")
        void testCacheMissFetchesFromDatabase() throws Exception {
            when(cacheService.getCached(100L)).thenReturn(Optional.empty());

            SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                    .subtopicId(100L)
                    .targetExam("JEE_MAIN")
                    .status("PUBLISHED")
                    .build();
            ReflectionTestUtils.setField(entity, "id", LESSON_ID);

            when(repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
                    100L, "JEE_MAIN", "PUBLISHED"))
                    .thenReturn(Optional.of(entity));

            MicroLessonResponse response = buildSampleResponse();
            when(generatorService.toResponse(entity)).thenReturn(response);

            mockMvc.perform(get("/api/micro-lessons/subtopic/100")
                            .param("targetExam", "JEE_MAIN")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subtopic").value("Rotational Motion"))
                    .andExpect(jsonPath("$.content.coreConcept").isNotEmpty());

            verify(cacheService).getCached(100L);
            verify(cacheService).put(eq(100L), any(MicroLessonResponse.class));
        }

        @Test
        @DisplayName("404 Not Found - Returns 404 when no published micro-lesson exists")
        void testNotFoundWhenNoMicroLessonPublished() throws Exception {
            when(cacheService.getCached(999L)).thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
                    999L, "JEE_MAIN", "PUBLISHED"))
                    .thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                    999L, "PUBLISHED"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/micro-lessons/subtopic/999")
                            .param("targetExam", "JEE_MAIN")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("200 OK - Synthesizes micro-lesson on demand when subtopicName is provided")
        void testOnDemandSynthesisWhenTaxonomyProvided() throws Exception {
            when(cacheService.getCached(555L)).thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndTargetExamAndStatusOrderByContentVersionDesc(
                    555L, "JEE_MAIN", "PUBLISHED"))
                    .thenReturn(Optional.empty());
            when(repository.findFirstBySubtopicIdAndStatusOrderByContentVersionDesc(
                    555L, "PUBLISHED"))
                    .thenReturn(Optional.empty());
            when(repository.findTopBySubtopicIdAndTargetExamOrderByContentVersionDesc(
                    555L, "JEE_MAIN"))
                    .thenReturn(Optional.empty());
            when(repository.findTopBySubtopicIdOrderByContentVersionDesc(555L))
                    .thenReturn(Optional.empty());

            SubtopicMicroLesson generatedEntity = SubtopicMicroLesson.builder()
                    .subtopicId(555L)
                    .targetExam("JEE_MAIN")
                    .status("AI_GENERATED")
                    .build();
            when(generatorService.generateForSubtopic(eq(1L), eq("Physics"), eq(1L), eq("Mechanics"), eq(555L), eq("Friction"), eq("JEE_MAIN")))
                    .thenReturn(generatedEntity);

            MicroLessonResponse generatedResponse = buildSampleResponse();
            when(generatorService.toResponse(generatedEntity)).thenReturn(generatedResponse);

            mockMvc.perform(get("/api/micro-lessons/subtopic/555")
                            .param("targetExam", "JEE_MAIN")
                            .param("subtopicName", "Friction")
                            .param("topicName", "Mechanics")
                            .param("subjectName", "Physics")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.coreConcept").isNotEmpty());

            verify(generatorService).generateForSubtopic(eq(1L), eq("Physics"), eq(1L), eq("Mechanics"), eq(555L), eq("Friction"), eq("JEE_MAIN"));
        }
    }
}
