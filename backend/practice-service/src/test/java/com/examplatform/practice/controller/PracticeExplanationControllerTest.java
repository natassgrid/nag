// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.controller;

import com.examplatform.practice.client.AnvayaPrajnaClient;
import com.examplatform.practice.dto.explanation.ExplanationHintDto;
import com.examplatform.practice.dto.explanation.ExplanationIrDto;
import com.examplatform.practice.dto.explanation.ExplanationStepDto;
import com.examplatform.practice.exception.GlobalExceptionHandler;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PracticeExplanationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnvayaPrajnaClient anvayaPrajnaClient;

    @InjectMocks
    private PracticeExplanationController controller;

    private UUID questionId;
    private ExplanationIrDto sampleExplanation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        questionId = UUID.randomUUID();
        sampleExplanation = ExplanationIrDto.builder()
                .explanationId("exp-123")
                .questionId(questionId.toString())
                .status("APPROVED")
                .concepts(List.of("Kinematics", "Newton Laws"))
                .steps(List.of(
                        ExplanationStepDto.builder()
                                .id("s1")
                                .sequence(1)
                                .type("GIVEN")
                                .after("m = 10 kg, F = 50 N")
                                .build(),
                        ExplanationStepDto.builder()
                                .id("s2")
                                .sequence(2)
                                .type("CONCLUDE")
                                .after("a = 5 m/s^2")
                                .build()
                ))
                .hints(List.of(
                        ExplanationHintDto.builder()
                                .id("h1")
                                .level(1)
                                .text("Apply Newton's second law: F = m * a")
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("GET /api/practice/questions/{questionId}/explanation - Returns 200 OK with explanation")
    void testGetExplanationSuccess() throws Exception {
        when(anvayaPrajnaClient.getExplanation(questionId)).thenReturn(Optional.of(sampleExplanation));

        mockMvc.perform(get("/api/practice/questions/{questionId}/explanation", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanationId").value("exp-123"))
                .andExpect(jsonPath("$.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].type").value("GIVEN"))
                .andExpect(jsonPath("$.hints[0].text").value("Apply Newton's second law: F = m * a"));

        verify(anvayaPrajnaClient).getExplanation(questionId);
    }


    @Test
    @DisplayName("GET /api/practice/questions/{questionId}/explanation - Returns 404 when explanation not found")
    void testGetExplanationNotFound() throws Exception {
        when(anvayaPrajnaClient.getExplanation(questionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/practice/questions/{questionId}/explanation", questionId))
                .andExpect(status().isNotFound());

        verify(anvayaPrajnaClient).getExplanation(questionId);
    }
}
