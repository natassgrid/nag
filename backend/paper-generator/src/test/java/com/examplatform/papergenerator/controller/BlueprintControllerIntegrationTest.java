/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.dto.*;
import com.examplatform.papergenerator.service.BlueprintService;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.papergenerator.support.AbstractIntegrationTest;
import com.examplatform.shared.constants.QuestionTaxonomyConstants;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BlueprintController REST Endpoints E2E Tests (MockMvc)")
class BlueprintControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private BlueprintService service;

    @MockitoBean
    private PaperAssemblyService paperAssemblyService;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BLUEPRINT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private BlueprintRule sampleRule() {
        return BlueprintRule.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .subject("Reasoning")
                .difficulty(QuestionTaxonomyConstants.Difficulty.EASY)
                .cognitiveLevel(QuestionTaxonomyConstants.CognitiveLevel.UNDERSTAND)
                .questionCount(5)
                .build();
    }

    private BlueprintResponse sampleResponse() {
        return BlueprintResponse.builder()
                .id(BLUEPRINT_ID)
                .code("BP-REAS")
                .name("Reasoning Blueprint")
                .description("Sample reasoning blueprint")
                .version(1)
                .status("ACTIVE")
                .totalQuestions(5)
                .totalMarks(10.0)
                .platformDefault(false)
                .sections(List.of(
                        BlueprintSectionDto.builder()
                                .id(UUID.randomUUID())
                                .name("Reasoning Section")
                                .orderIndex(0)
                                .subjectId(1L)
                                .rules(List.of(sampleRule()))
                                .build()
                ))
                .rules(List.of(sampleRule()))
                .createdBy(USER_ID)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/papers/blueprints")
    class ListEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists blueprints - returns 200 OK")
        void examControllerCanList() throws Exception {
            when(service.listVisible(TENANT_ID)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/v1/papers/blueprints")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(BLUEPRINT_ID.toString()))
                    .andExpect(jsonPath("$[0].name").value("Reasoning Blueprint"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/papers/blueprints/{id}")
    class GetOneEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves blueprint by ID - returns 200 OK")
        void examControllerCanGetById() throws Exception {
            when(service.getById(BLUEPRINT_ID, TENANT_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/papers/blueprints/{id}", BLUEPRINT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(BLUEPRINT_ID.toString()))
                    .andExpect(jsonPath("$.sections[0].subjectId").value(1))
                    .andExpect(jsonPath("$.sections[0].rules[0].subtopicId").value(100));
        }

        @Test
        @DisplayName("-ve: Unknown ID returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(service.getById(BLUEPRINT_ID, TENANT_ID))
                    .thenThrow(new EntityNotFoundException("Blueprint not found: " + BLUEPRINT_ID));

            mockMvc.perform(get("/api/v1/papers/blueprints/{id}", BLUEPRINT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/blueprints")
    class CreateEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates blueprint - returns 201 Created")
        void examControllerCanCreate() throws Exception {
            BlueprintRequest req = BlueprintRequest.builder()
                    .name("New Blueprint")
                    .code("NEW-BP")
                    .sections(List.of(BlueprintSectionDto.builder()
                            .name("Section 1")
                            .subjectId(1L)
                            .rules(List.of(sampleRule()))
                            .build()))
                    .build();

            when(service.create(any(BlueprintRequest.class), any(), eq(TENANT_ID))).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/papers/blueprints")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(BLUEPRINT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/blueprints/{id}/check-sufficiency")
    class CheckSufficiencyEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER checks sufficiency - returns 200 OK")
        void checkSufficiencyReturns200() throws Exception {
            BlueprintFeasibilityResponse feasibility = BlueprintFeasibilityResponse.builder()
                    .feasible(true)
                    .totalQuestionsNeeded(5)
                    .totalQuestionsAvailable(20)
                    .deficitRuleCount(0)
                    .summary("Blueprint is feasible.")
                    .build();

            when(service.checkSufficiency(eq(BLUEPRINT_ID), any(), eq(1), eq(TENANT_ID), anyBoolean()))
                    .thenReturn(feasibility);

            mockMvc.perform(post("/api/v1/papers/blueprints/{id}/check-sufficiency", BLUEPRINT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feasible").value(true))
                    .andExpect(jsonPath("$.totalQuestionsNeeded").value(5))
                    .andExpect(jsonPath("$.totalQuestionsAvailable").value(20));
        }
    }
}
