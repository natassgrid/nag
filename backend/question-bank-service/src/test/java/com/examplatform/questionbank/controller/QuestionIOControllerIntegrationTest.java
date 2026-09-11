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

import com.examplatform.questionbank.io.ImportResult;
import com.examplatform.questionbank.io.QuestionExportService;
import com.examplatform.questionbank.io.QuestionImportService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("QuestionIOController REST Endpoints E2E Tests (MockMvc)")
class QuestionIOControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private QuestionExportService exportService;

    @MockitoBean
    private QuestionImportService importService;

    private static final String TENANT_ID = "default";
    private static final UUID AUTHOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Nested
    @DisplayName("GET /api/v1/questions/export")
    class ExportEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR exports ZIP - returns 200 OK with ZIP content-type")
        void authorCanExportQuestions() throws Exception {
            mockMvc.perform(get("/api/v1/questions/export")
                            .header("X-Tenant-Id", TENANT_ID)
                            .param("format", "json")
                            .param("subject", "Physics")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/zip"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from exporting - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/questions/export")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/questions/import")
    class ImportEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR imports questions ZIP - returns 200 OK")
        void authorCanImportQuestions() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "questions.zip",
                    "application/zip",
                    "dummy zip binary content".getBytes()
            );

            ImportResult result = ImportResult.builder()
                    .imported(10)
                    .failed(0)
                    .failures(List.of())
                    .build();

            when(importService.importFromZip(any(byte[].class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(result);

            mockMvc.perform(multipart("/api/v1/questions/import")
                            .file(file)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.imported").value(10));
        }

        @Test
        @DisplayName("-ve: Uploading empty file returns 400 Bad Request")
        void emptyFileReturnsBadRequest() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.zip",
                    "application/zip",
                    new byte[0]
            );

            mockMvc.perform(multipart("/api/v1/questions/import")
                            .file(emptyFile)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }
}
