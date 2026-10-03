// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.CreatePracticeSetRequest;
import com.examplatform.practice.dto.UpdatePracticeSetRequest;
import com.examplatform.practice.exception.GlobalExceptionHandler;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.service.PracticeSetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminPracticeSetControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PracticeSetRepository practiceSetRepository;

    @Mock
    private PracticeSetService practiceSetService;

    private AdminPracticeSetController controller;

    private UUID practiceSetId;
    private PracticeSet sampleSet;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new AdminPracticeSetController(practiceSetRepository, practiceSetService, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        practiceSetId = UUID.randomUUID();
        sampleSet = PracticeSet.builder()
                .name("Sample Math Practice")
                .description("Practice questions for mathematics")
                .durationMinutes(45)
                .totalQuestions(10)
                .subjectSlug("math")
                .source("EXAM_CLONE")
                .published(true)
                .questionIds("[\"" + UUID.randomUUID() + "\"]")
                .createdBy(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(sampleSet, "id", practiceSetId);
    }

    @Test
    @DisplayName("GET /api/admin/practice/sets - Returns all practice sets")
    void testListAll() throws Exception {
        when(practiceSetRepository.findAll()).thenReturn(List.of(sampleSet));

        mockMvc.perform(get("/api/admin/practice/sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sample Math Practice"));

        verify(practiceSetRepository).findAll();
    }

    @Test
    @DisplayName("POST /api/admin/practice/sets - Creates practice set with attached question IDs")
    void testCreatePracticeSetWithQuestions() throws Exception {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        CreatePracticeSetRequest req = new CreatePracticeSetRequest(
                "Science Practice Set",
                "Physics and Chemistry",
                60,
                "science",
                List.of(q1, q2),
                "EXAM_CLONE",
                2
        );

        when(practiceSetRepository.save(any(PracticeSet.class))).thenAnswer(invocation -> {
            PracticeSet ps = invocation.getArgument(0);
            ReflectionTestUtils.setField(ps, "id", practiceSetId);
            return ps;
        });

        mockMvc.perform(post("/api/admin/practice/sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Science Practice Set"))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.source").value("EXAM_CLONE"));

        verify(practiceSetRepository).save(any(PracticeSet.class));
    }

    @Test
    @DisplayName("PUT /api/admin/practice/sets/{id} - Updates practice set")
    void testUpdatePracticeSet() throws Exception {
        UpdatePracticeSetRequest updateReq = new UpdatePracticeSetRequest(
                "Updated Science Practice",
                "Updated description",
                50,
                "science-advanced",
                List.of(UUID.randomUUID()),
                1
        );

        when(practiceSetRepository.findById(practiceSetId)).thenReturn(Optional.of(sampleSet));
        when(practiceSetRepository.save(any(PracticeSet.class))).thenReturn(sampleSet);

        mockMvc.perform(put("/api/admin/practice/sets/{id}", practiceSetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Science Practice"));

        verify(practiceSetRepository).save(sampleSet);
    }

    @Test
    @DisplayName("POST /api/admin/practice/sets/{id}/publish - Publishes practice set")
    void testPublishPracticeSet() throws Exception {
        sampleSet.setPublished(false);
        when(practiceSetRepository.findById(practiceSetId)).thenReturn(Optional.of(sampleSet));
        when(practiceSetRepository.save(any(PracticeSet.class))).thenReturn(sampleSet);

        mockMvc.perform(post("/api/admin/practice/sets/{id}/publish", practiceSetId))
                .andExpect(status().isNoContent());

        verify(practiceSetRepository).save(sampleSet);
    }

    @Test
    @DisplayName("POST /api/admin/practice/sets/{id}/unpublish - Unpublishes practice set")
    void testUnpublishPracticeSet() throws Exception {
        sampleSet.setPublished(true);
        when(practiceSetRepository.findById(practiceSetId)).thenReturn(Optional.of(sampleSet));
        when(practiceSetRepository.save(any(PracticeSet.class))).thenReturn(sampleSet);

        mockMvc.perform(post("/api/admin/practice/sets/{id}/unpublish", practiceSetId))
                .andExpect(status().isNoContent());

        verify(practiceSetRepository).save(sampleSet);
    }

    @Test
    @DisplayName("DELETE /api/admin/practice/sets/{id} - Deletes practice set")
    void testDeletePracticeSet() throws Exception {
        mockMvc.perform(delete("/api/admin/practice/sets/{id}", practiceSetId))
                .andExpect(status().isNoContent());

        verify(practiceSetRepository).deleteById(practiceSetId);
    }
}
