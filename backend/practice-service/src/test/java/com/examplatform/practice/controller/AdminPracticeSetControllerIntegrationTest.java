/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package com.examplatform.practice.controller;

import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.CreatePracticeSetRequest;
import com.examplatform.practice.dto.UpdatePracticeSetRequest;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AdminPracticeSetController Testcontainers Integration Tests")
class AdminPracticeSetControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PracticeSetRepository practiceSetRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PracticeSet existingSet;

    @BeforeEach
    void setUp() {
        if (testcontainersAvailable) {
            practiceSetRepository.deleteAll();

            existingSet = PracticeSet.builder()
                    .name("Mathematics Practice Set")
                    .description("High school calculus and algebra")
                    .durationMinutes(45)
                    .subjectSlug("math")
                    .source("EXAM_CLONE")
                    .totalQuestions(15)
                    .questionIds("[\"" + UUID.randomUUID() + "\"]")
                    .published(false)
                    .createdBy(ADMIN_ID)
                    .build();
            existingSet.setTenantId(TENANT_ID);
            existingSet = practiceSetRepository.save(existingSet);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/practice/sets")
    class ListPracticeSets {

        @Test
        @DisplayName("+ve: ADMIN lists all practice sets - returns 200 OK")
        void adminCanListSets() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/admin/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Mathematics Practice Set"))
                    .andExpect(jsonPath("$[0].source").value("EXAM_CLONE"));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN lists all practice sets - returns 200 OK")
        void superAdminCanListSets() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/admin/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("-ve: Unauthorized CANDIDATE role - returns 403 Forbidden")
        void candidateCannotListAdminSets() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/admin/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request - returns 401 Unauthorized")
        void unauthenticatedCannotListAdminSets() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/admin/practice/sets"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/practice/sets")
    class CreatePracticeSet {

        @Test
        @DisplayName("+ve: ADMIN creates practice set with attached questions from practice paper - returns 201 Created")
        void adminCanCreatePracticeSetWithAttachedQuestions() throws Exception {
            if (!testcontainersAvailable) return;

            UUID q1 = UUID.randomUUID();
            UUID q2 = UUID.randomUUID();
            UUID q3 = UUID.randomUUID();

            CreatePracticeSetRequest request = new CreatePracticeSetRequest(
                    "Physics Mechanics Module",
                    "Cloned from official physics practice paper",
                    60,
                    "physics",
                    List.of(q1, q2, q3),
                    "EXAM_CLONE",
                    3
            );

            mockMvc.perform(post("/api/admin/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Physics Mechanics Module"))
                    .andExpect(jsonPath("$.source").value("EXAM_CLONE"))
                    .andExpect(jsonPath("$.totalQuestions").value(3))
                    .andExpect(jsonPath("$.published").value(false));

            List<PracticeSet> sets = practiceSetRepository.findAll();
            assertThat(sets).hasSize(2);
            PracticeSet created = sets.stream()
                    .filter(s -> "Physics Mechanics Module".equals(s.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(created.getQuestionIds()).contains(q1.toString());
            assertThat(created.getTotalQuestions()).isEqualTo(3);
        }

        @Test
        @DisplayName("-ve: Invalid request missing name - returns 400 Bad Request")
        void missingNameReturnsBadRequest() throws Exception {
            if (!testcontainersAvailable) return;

            CreatePracticeSetRequest request = new CreatePracticeSetRequest(
                    "",
                    "Description",
                    45,
                    "math",
                    List.of(),
                    "MANUAL",
                    0
            );

            mockMvc.perform(post("/api/admin/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/practice/sets/{id}")
    class UpdatePracticeSet {

        @Test
        @DisplayName("+ve: ADMIN updates existing practice set - returns 200 OK")
        void adminCanUpdatePracticeSet() throws Exception {
            if (!testcontainersAvailable) return;

            UpdatePracticeSetRequest request = new UpdatePracticeSetRequest(
                    "Advanced Mathematics Practice",
                    "Updated description for calculus",
                    90,
                    "math-advanced",
                    List.of(UUID.randomUUID()),
                    1
            );

            mockMvc.perform(put("/api/admin/practice/sets/{id}", existingSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTENT_MANAGER"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Advanced Mathematics Practice"))
                    .andExpect(jsonPath("$.durationMinutes").value(90));

            PracticeSet updated = practiceSetRepository.findById(existingSet.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo("Advanced Mathematics Practice");
            assertThat(updated.getDurationMinutes()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("POST /api/admin/practice/sets/{id}/publish and unpublish")
    class PublishAndUnpublishPracticeSet {

        @Test
        @DisplayName("+ve: ADMIN publishes and unpublishes practice set - returns 204 No Content")
        void adminCanPublishAndUnpublish() throws Exception {
            if (!testcontainersAvailable) return;

            // Publish
            mockMvc.perform(post("/api/admin/practice/sets/{id}/publish", existingSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNoContent());

            PracticeSet published = practiceSetRepository.findById(existingSet.getId()).orElseThrow();
            assertThat(published.isPublished()).isTrue();

            // Unpublish
            mockMvc.perform(post("/api/admin/practice/sets/{id}/unpublish", existingSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNoContent());

            PracticeSet unpublished = practiceSetRepository.findById(existingSet.getId()).orElseThrow();
            assertThat(unpublished.isPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/practice/sets/{id}")
    class DeletePracticeSet {

        @Test
        @DisplayName("+ve: ADMIN deletes practice set - returns 204 No Content")
        void adminCanDeletePracticeSet() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(delete("/api/admin/practice/sets/{id}", existingSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ADMIN_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNoContent());

            assertThat(practiceSetRepository.findById(existingSet.getId())).isEmpty();
        }
    }
}
