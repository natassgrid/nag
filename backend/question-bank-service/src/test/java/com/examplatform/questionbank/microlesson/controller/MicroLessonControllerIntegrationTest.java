// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.controller;

import com.examplatform.questionbank.microlesson.ai.MicroLessonGeneratorService;
import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.service.MicroLessonCacheService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.shared.constants.SharedConstants;
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
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private com.examplatform.questionbank.microlesson.repository.LessonEffectivenessRepository effectivenessRepository;

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
            SharedConstants.JEE_MAIN,
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
        @DisplayName("200 OK - Delivers list of micro-lessons for subtopic")
        void testGetMicroLessonsForSubtopic() throws Exception {
            SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                    .subtopicId(100L)
                    .lessonType("CONCEPT")
                    .difficultyLevel("CORE")
                    .sequenceNo(1)
                    .status("PUBLISHED")
                    .coreConcept("Torque produces angular acceleration.")
                    .trap("Sign convention trap")
                    .cheatSheetJson("[]")
                    .stepByStepBreakdownJson("{}")
                    .build();
            ReflectionTestUtils.setField(entity, "id", LESSON_ID);

            when(repository.findBySubtopicIdAndStatusOrderBySequenceNoAsc(100L, "PUBLISHED"))
                    .thenReturn(List.of(entity));

            MicroLessonResponse response = buildSampleResponse();
            when(generatorService.toResponse(entity)).thenReturn(response);

            mockMvc.perform(get("/api/micro-lessons/subtopic/100")
                            .param("lessonType", "CONCEPT")
                            .param("difficulty", "CORE")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].subtopic").value("Rotational Motion"))
                    .andExpect(jsonPath("$[0].coreConcept").isNotEmpty())
                    .andExpect(jsonPath("$[0].cheatSheet").isArray());

            verify(repository).findBySubtopicIdAndStatusOrderBySequenceNoAsc(100L, "PUBLISHED");
        }

        @Test
        @DisplayName("200 OK - Filters micro-lessons by lessonType and difficulty")
        void testFilterMicroLessons() throws Exception {
            SubtopicMicroLesson conceptLesson = SubtopicMicroLesson.builder()
                    .subtopicId(100L)
                    .lessonType("CONCEPT")
                    .difficultyLevel("CORE")
                    .sequenceNo(1)
                    .status("PUBLISHED")
                    .coreConcept("Concept lesson")
                    .trap("Trap")
                    .cheatSheetJson("[]")
                    .stepByStepBreakdownJson("{}")
                    .build();
            ReflectionTestUtils.setField(conceptLesson, "id", LESSON_ID);

            SubtopicMicroLesson workedExample = SubtopicMicroLesson.builder()
                    .subtopicId(100L)
                    .lessonType("WORKED_EXAMPLE")
                    .difficultyLevel("ADVANCED")
                    .sequenceNo(2)
                    .status("PUBLISHED")
                    .coreConcept("Worked example")
                    .trap("Trap")
                    .cheatSheetJson("[]")
                    .stepByStepBreakdownJson("{}")
                    .build();
            UUID workedExampleId = UUID.randomUUID();
            ReflectionTestUtils.setField(workedExample, "id", workedExampleId);

            when(repository.findBySubtopicIdAndStatusOrderBySequenceNoAsc(100L, "PUBLISHED"))
                    .thenReturn(List.of(conceptLesson, workedExample));

            MicroLessonResponse conceptResponse = buildSampleResponse();
            when(generatorService.toResponse(conceptLesson)).thenReturn(conceptResponse);

            mockMvc.perform(get("/api/micro-lessons/subtopic/100")
                            .param("lessonType", "CONCEPT")
                            .param("difficulty", "CORE")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].subtopic").value("Rotational Motion"));
        }

        @Test
        @DisplayName("200 OK - Returns empty array when no micro-lessons exist")
        void testEmptyWhenNoLessons() throws Exception {
            when(repository.findBySubtopicIdAndStatusOrderBySequenceNoAsc(999L, "PUBLISHED"))
                    .thenReturn(List.of());
            when(repository.findBySubtopicIdOrderBySequenceNoAsc(999L))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/micro-lessons/subtopic/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("200 OK - Resolves lang alias and matches difficulty alias (CORE -> MEDIUM)")
        void testGetMicroLessonsForSubtopicWithLangAndDifficultyAlias() throws Exception {
            SubtopicMicroLesson entity = SubtopicMicroLesson.builder()
                    .subtopicId(37L)
                    .lessonType("CONCEPT")
                    .difficultyLevel("MEDIUM")
                    .sequenceNo(1)
                    .status("AI_GENERATED")
                    .coreConcept("Sphere concept")
                    .trap("Trap")
                    .cheatSheetJson("[]")
                    .stepByStepBreakdownJson("{}")
                    .language("en")
                    .build();
            ReflectionTestUtils.setField(entity, "id", LESSON_ID);

            when(repository.findBySubtopicIdAndStatusOrderBySequenceNoAsc(37L, "PUBLISHED"))
                    .thenReturn(List.of());
            when(repository.findBySubtopicIdOrderBySequenceNoAsc(37L))
                    .thenReturn(List.of(entity));

            mockMvc.perform(get("/api/micro-lessons/subtopic/37")
                            .param("lang", "en")
                            .param("lessonType", "CONCEPT")
                            .param("difficulty", "CORE")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].difficultyLevel").value("CORE"));
        }

    }

    @Nested
    @DisplayName("POST /api/micro-lessons/{id}/progress")
    class RecordProgress {

        @Test
        @DisplayName("200 OK - Records lesson completion telemetry successfully")
        void testRecordProgressCompletion() throws Exception {
            mockMvc.perform(post("/api/micro-lessons/" + LESSON_ID + "/progress")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"COMPLETED\",\"postAccuracyDelta\":0.25}")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 OK - Records lesson view telemetry successfully")
        void testRecordProgressView() throws Exception {
            mockMvc.perform(post("/api/micro-lessons/" + LESSON_ID + "/progress")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"VIEW\"}")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk());
        }
    }
}
