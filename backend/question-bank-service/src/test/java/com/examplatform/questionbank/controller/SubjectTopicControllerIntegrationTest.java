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

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.CreateSubjectRequest;
import com.examplatform.questionbank.dto.CreateSubtopicRequest;
import com.examplatform.questionbank.dto.CreateTopicRequest;
import com.examplatform.questionbank.dto.SubjectHierarchyResponse;
import com.examplatform.questionbank.service.SubjectTopicService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SubjectTopicController REST Endpoints E2E Tests (MockMvc)")
class SubjectTopicControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private SubjectTopicService subjectTopicService;

    private static final String TENANT_ID = "default";

    // =========================================================================
    // 1. Subject Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Subject Endpoints")
    class SubjectEndpoints {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR lists subjects - returns 200 OK")
        void authorCanListSubjects() throws Exception {
            Subject subject = Subject.builder()
                    .name("Mathematics")
                    .code("MATH")
                    .description("Higher Mathematics")
                    .build();
            ReflectionTestUtils.setField(subject, "id", 1L);
            subject.setTenantId(TENANT_ID);

            when(subjectTopicService.listSubjects(eq(TENANT_ID))).thenReturn(List.of(subject));

            mockMvc.perform(get("/api/v1/subjects")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Mathematics"));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN creates subject - returns 201 Created")
        void adminCanCreateSubject() throws Exception {
            CreateSubjectRequest request = CreateSubjectRequest.builder()
                    .name("Physics")
                    .code("PHYS")
                    .description("General Physics")
                    .build();

            Subject subject = Subject.builder()
                    .name("Physics")
                    .code("PHYS")
                    .description("General Physics")
                    .build();
            ReflectionTestUtils.setField(subject, "id", 2L);
            subject.setTenantId(TENANT_ID);

            when(subjectTopicService.createSubject(eq("Physics"), eq("PHYS"), eq("General Physics"), eq(TENANT_ID)))
                    .thenReturn(subject);

            mockMvc.perform(post("/api/v1/subjects")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(2))
                    .andExpect(jsonPath("$.data.name").value("Physics"));
        }

        @Test
        @DisplayName("-ve: Blank subject name returns 400 Bad Request")
        void blankSubjectNameReturnsBadRequest() throws Exception {
            CreateSubjectRequest invalidRequest = CreateSubjectRequest.builder()
                    .name("")
                    .code("MATH")
                    .build();

            mockMvc.perform(post("/api/v1/subjects")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from creating subject - returns 403 Forbidden")
        void candidateCannotCreateSubject() throws Exception {
            CreateSubjectRequest request = CreateSubjectRequest.builder()
                    .name("Physics")
                    .code("PHYS")
                    .build();

            mockMvc.perform(post("/api/v1/subjects")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 2. Topic Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Topic Endpoints")
    class TopicEndpoints {

        @Test
        @DisplayName("+ve: REVIEWER lists topics for subject - returns 200 OK")
        void reviewerCanListTopics() throws Exception {
            Topic topic = Topic.builder()
                    .subjectId(1L)
                    .name("Calculus")
                    .build();
            ReflectionTestUtils.setField(topic, "id", 10L);
            topic.setTenantId(TENANT_ID);

            when(subjectTopicService.listTopics(eq(1L), eq(TENANT_ID))).thenReturn(List.of(topic));

            mockMvc.perform(get("/api/v1/subjects/{subjectId}/topics", 1L)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].name").value("Calculus"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates topic - returns 201 Created")
        void examControllerCanCreateTopic() throws Exception {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name("Linear Algebra")
                    .description("Matrix algebra and vector spaces")
                    .build();

            Topic topic = Topic.builder()
                    .subjectId(1L)
                    .name("Linear Algebra")
                    .build();
            ReflectionTestUtils.setField(topic, "id", 11L);
            topic.setTenantId(TENANT_ID);

            when(subjectTopicService.createTopic(eq(1L), eq("Linear Algebra"), any(), eq(TENANT_ID)))
                    .thenReturn(topic);

            mockMvc.perform(post("/api/v1/subjects/{subjectId}/topics", 1L)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(11));
        }
    }

    // =========================================================================
    // 3. Subtopic & Hierarchy Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Subtopic and Hierarchy Endpoints")
    class SubtopicAndHierarchyEndpoints {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR lists subtopics - returns 200 OK")
        void authorCanListSubtopics() throws Exception {
            Subtopic subtopic = Subtopic.builder()
                    .topicId(10L)
                    .name("Integration by Parts")
                    .build();
            ReflectionTestUtils.setField(subtopic, "id", 100L);
            subtopic.setTenantId(TENANT_ID);

            when(subjectTopicService.listSubtopics(eq(10L), eq(TENANT_ID))).thenReturn(List.of(subtopic));

            mockMvc.perform(get("/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics", 1L, 10L)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(100));
        }

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR creates subtopic - returns 201 Created")
        void authorCanCreateSubtopic() throws Exception {
            CreateSubtopicRequest request = CreateSubtopicRequest.builder()
                    .name("Differential Equations")
                    .build();

            Subtopic subtopic = Subtopic.builder()
                    .topicId(10L)
                    .name("Differential Equations")
                    .build();
            ReflectionTestUtils.setField(subtopic, "id", 101L);
            subtopic.setTenantId(TENANT_ID);

            when(subjectTopicService.createSubtopic(eq(10L), eq("Differential Equations"), any(), eq(TENANT_ID)))
                    .thenReturn(subtopic);

            mockMvc.perform(post("/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics", 1L, 10L)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(101));
        }

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR retrieves full hierarchy - returns 200 OK")
        void authorCanGetHierarchy() throws Exception {
            SubjectHierarchyResponse hierarchy = SubjectHierarchyResponse.builder()
                    .id(1L)
                    .name("Mathematics")
                    .code("MATH")
                    .topics(List.of())
                    .build();

            when(subjectTopicService.getHierarchy(eq(TENANT_ID))).thenReturn(List.of(hierarchy));

            mockMvc.perform(get("/api/v1/subjects/hierarchy")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(1));
        }
    }
}
