/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

import com.examplatform.papergenerator.dto.BlueprintFeasibilityResponse;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.BlueprintTemplateRequest;
import com.examplatform.papergenerator.dto.BlueprintTemplateResponse;
import com.examplatform.papergenerator.service.BlueprintTemplateService;
import com.examplatform.papergenerator.support.AbstractIntegrationTest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BlueprintTemplateController REST Endpoints E2E Tests (MockMvc)")
class BlueprintTemplateControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private BlueprintTemplateService service;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private BlueprintRule sampleRule() {
        return BlueprintRule.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("EASY")
                .cognitiveLevel("APPLY")
                .questionCount(5)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/papers/blueprint-templates")
    class ListTemplatesEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists all templates - returns 200 OK")
        void examControllerCanListTemplates() throws Exception {
            BlueprintTemplateResponse resp = BlueprintTemplateResponse.builder()
                    .id(TEMPLATE_ID)
                    .name("Standard Math Template")
                    .description("Test Description")
                    .rules(List.of(sampleRule()))
                    .build();

            when(service.listAll(anyString())).thenReturn(List.of(resp));

            mockMvc.perform(get("/api/v1/papers/blueprint-templates")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Standard Math Template"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists templates by examId - returns 200 OK")
        void examControllerCanListByExamId() throws Exception {
            UUID examId = UUID.randomUUID();
            when(service.listByExam(eq(examId), anyString())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/papers/blueprint-templates")
                            .param("examId", examId.toString())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/papers/blueprint-templates")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/papers/blueprint-templates/{id}")
    class GetTemplateEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER gets blueprint template by ID - returns 200 OK")
        void examControllerCanGetById() throws Exception {
            BlueprintTemplateResponse resp = BlueprintTemplateResponse.builder()
                    .id(TEMPLATE_ID)
                    .name("Math Template")
                    .rules(List.of(sampleRule()))
                    .build();

            when(service.getById(eq(TEMPLATE_ID), anyString())).thenReturn(resp);

            mockMvc.perform(get("/api/v1/papers/blueprint-templates/{id}", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Math Template"));
        }

        @Test
        @DisplayName("-ve: Non-existent ID returns 404 Not Found")
        void nonExistentIdReturnsNotFound() throws Exception {
            when(service.getById(eq(TEMPLATE_ID), anyString()))
                    .thenThrow(new EntityNotFoundException("Blueprint template not found: " + TEMPLATE_ID));

            mockMvc.perform(get("/api/v1/papers/blueprint-templates/{id}", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/blueprint-templates/{id}/check-sufficiency")
    class CheckSufficiencyEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER checks template sufficiency - returns 200 OK with feasibility details")
        void checkSufficiencyReturnsOk() throws Exception {
            BlueprintFeasibilityResponse resp = BlueprintFeasibilityResponse.builder()
                    .feasible(true)
                    .totalQuestionsNeeded(5)
                    .totalQuestionsAvailable(10)
                    .deficitRuleCount(0)
                    .notificationDispatched(false)
                    .summary("Blueprint is feasible.")
                    .checkedAt(Instant.now())
                    .build();

            when(service.checkTemplateSufficiency(eq(TEMPLATE_ID), anyBoolean(), anyString())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/papers/blueprint-templates/{id}/check-sufficiency", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feasible").value(true))
                    .andExpect(jsonPath("$.totalQuestionsNeeded").value(5))
                    .andExpect(jsonPath("$.totalQuestionsAvailable").value(10));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/blueprint-templates")
    class CreateTemplateEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates blueprint template - returns 201 Created")
        void examControllerCanCreate() throws Exception {
            BlueprintTemplateRequest request = BlueprintTemplateRequest.builder()
                    .name("New Blueprint")
                    .description("Math Blueprint")
                    .rules(List.of(sampleRule()))
                    .build();

            BlueprintTemplateResponse resp = BlueprintTemplateResponse.builder()
                    .id(TEMPLATE_ID)
                    .name("New Blueprint")
                    .description("Math Blueprint")
                    .rules(List.of(sampleRule()))
                    .createdBy(USER_ID)
                    .createdAt(Instant.now())
                    .build();

            when(service.create(any(), any(), anyString())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/papers/blueprint-templates")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()))
                    .andExpect(jsonPath("$.name").value("New Blueprint"));
        }

        @Test
        @DisplayName("-ve: Missing name returns 400 Bad Request")
        void missingNameReturnsBadRequest() throws Exception {
            BlueprintTemplateRequest request = BlueprintTemplateRequest.builder()
                    .name("")
                    .rules(List.of(sampleRule()))
                    .build();

            mockMvc.perform(post("/api/v1/papers/blueprint-templates")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("-ve: Duplicate name returns 409 Conflict")
        void duplicateNameReturnsConflict() throws Exception {
            BlueprintTemplateRequest request = BlueprintTemplateRequest.builder()
                    .name("Existing Blueprint")
                    .rules(List.of(sampleRule()))
                    .build();

            when(service.create(any(), any(), anyString()))
                    .thenThrow(new IllegalArgumentException("Template with name already exists: Existing Blueprint"));

            mockMvc.perform(post("/api/v1/papers/blueprint-templates")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflict"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/papers/blueprint-templates/{id}")
    class UpdateTemplateEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER updates blueprint template - returns 200 OK")
        void examControllerCanUpdate() throws Exception {
            BlueprintTemplateRequest request = BlueprintTemplateRequest.builder()
                    .name("Updated Blueprint")
                    .rules(List.of(sampleRule()))
                    .build();

            BlueprintTemplateResponse resp = BlueprintTemplateResponse.builder()
                    .id(TEMPLATE_ID)
                    .name("Updated Blueprint")
                    .rules(List.of(sampleRule()))
                    .build();

            when(service.update(eq(TEMPLATE_ID), any(), anyString())).thenReturn(resp);

            mockMvc.perform(put("/api/v1/papers/blueprint-templates/{id}", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Blueprint"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/papers/blueprint-templates/{id}")
    class DeleteTemplateEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER deletes blueprint template - returns 204 No Content")
        void examControllerCanDelete() throws Exception {
            doNothing().when(service).delete(eq(TEMPLATE_ID), anyString());

            mockMvc.perform(delete("/api/v1/papers/blueprint-templates/{id}", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/papers/blueprint-templates/{id}", TEMPLATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }
}
