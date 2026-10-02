// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.practice.exception.GlobalExceptionHandler;
import com.examplatform.practice.exception.PracticeSetNotFoundException;
import com.examplatform.practice.service.PracticeSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PracticeSetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PracticeSetService practiceSetService;

    @InjectMocks
    private PracticeSetController controller;

    private UUID setId;
    private PracticeSetDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        setId = UUID.randomUUID();
        sampleDto = new PracticeSetDto(
                setId,
                "NES Practice Simulation",
                "Official clone practice set",
                "EXAM_CLONE",
                60,
                "engineering",
                true,
                30
        );
    }

    @Test
    @DisplayName("GET /api/practice/sets - Returns published practice sets")
    void testListPublishedSets() throws Exception {
        when(practiceSetService.getPublishedSets()).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/practice/sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("NES Practice Simulation"))
                .andExpect(jsonPath("$[0].source").value("EXAM_CLONE"))
                .andExpect(jsonPath("$[0].totalQuestions").value(30));

        verify(practiceSetService).getPublishedSets();
    }

    @Test
    @DisplayName("GET /api/practice/sets/{setId} - Returns specific practice set details")
    void testGetSet() throws Exception {
        when(practiceSetService.getSet(setId)).thenReturn(sampleDto);

        mockMvc.perform(get("/api/practice/sets/{setId}", setId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(setId.toString()))
                .andExpect(jsonPath("$.name").value("NES Practice Simulation"));

        verify(practiceSetService).getSet(setId);
    }

    @Test
    @DisplayName("GET /api/practice/sets/{setId} - Returns 404 when practice set not found")
    void testGetSetNotFound() throws Exception {
        when(practiceSetService.getSet(setId)).thenThrow(new PracticeSetNotFoundException(setId));

        mockMvc.perform(get("/api/practice/sets/{setId}", setId))
                .andExpect(status().isNotFound());

        verify(practiceSetService).getSet(setId);
    }
}
