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

package com.examplatform.admin.controller;

import com.examplatform.admin.domain.SystemConfig;
import com.examplatform.admin.dto.BulkConfigUpdateRequest;
import com.examplatform.admin.dto.SingleConfigUpdateRequest;
import com.examplatform.admin.dto.SystemConfigResponse;
import com.examplatform.admin.service.ConfigChangeService;
import com.examplatform.admin.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminConfigController REST Endpoints E2E Tests (MockMvc)")
class AdminConfigControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ConfigChangeService configChangeService;

    private static final String TENANT_ID = "default";
    private static final UUID ACTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("GET /api/v1/admin/config")
    class GetConfigEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves configs - returns 200 OK")
        void superAdminCanGetConfigs() throws Exception {
            SystemConfig config = SystemConfig.builder()
                    .paramName("auth.mfa.enforced")
                    .paramValue("true")
                    .build();
            SystemConfigResponse response = new SystemConfigResponse(
                    UUID.randomUUID(), "auth.mfa.enforced", "true", TENANT_ID, ACTOR_ID, Instant.now(), Instant.now(), Instant.now());

            when(configChangeService.getConfigs(eq(TENANT_ID))).thenReturn(List.of(config));
            when(configChangeService.toResponse(config)).thenReturn(response);

            mockMvc.perform(get("/api/v1/admin/config")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].paramName").value("auth.mfa.enforced"))
                    .andExpect(jsonPath("$[0].paramValue").value("true"));
        }

        @Test
        @DisplayName("+ve: ADMIN retrieves configs - returns 200 OK")
        void adminCanGetConfigs() throws Exception {
            when(configChangeService.getConfigs(anyString())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/admin/config")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/config")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/config"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/config/map")
    class GetConfigMapEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN retrieves config map - returns 200 OK")
        void securityAdminCanGetConfigMap() throws Exception {
            when(configChangeService.getConfigMap(eq(TENANT_ID)))
                    .thenReturn(Map.of("auth.mfa.enforced", "true"));

            mockMvc.perform(get("/api/v1/admin/config/map")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['auth.mfa.enforced']").value("true"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/config/map")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/config/map"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/config")
    class UpdateConfigEndpoint {

        @Test
        @DisplayName("+ve: ADMIN updates single config - returns 200 OK")
        void adminCanUpdateConfig() throws Exception {
            SingleConfigUpdateRequest request = new SingleConfigUpdateRequest("auth.mfa.enforced", "false");
            SystemConfig config = SystemConfig.builder()
                    .paramName("auth.mfa.enforced")
                    .paramValue("false")
                    .build();
            SystemConfigResponse response = new SystemConfigResponse(
                    UUID.randomUUID(), "auth.mfa.enforced", "false", TENANT_ID, ACTOR_ID, Instant.now(), Instant.now(), Instant.now());

            when(configChangeService.updateConfig(eq("auth.mfa.enforced"), eq("false"), any(), anyString()))
                    .thenReturn(config);
            when(configChangeService.toResponse(config)).thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/config")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paramName").value("auth.mfa.enforced"))
                    .andExpect(jsonPath("$.paramValue").value("false"));
        }

        @Test
        @DisplayName("-ve: Missing paramName returns 400 Bad Request")
        void missingParamNameReturnsBadRequest() throws Exception {
            SingleConfigUpdateRequest request = new SingleConfigUpdateRequest("", "false");

            mockMvc.perform(put("/api/v1/admin/config")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            SingleConfigUpdateRequest request = new SingleConfigUpdateRequest("param", "val");

            mockMvc.perform(put("/api/v1/admin/config")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            SingleConfigUpdateRequest request = new SingleConfigUpdateRequest("param", "val");

            mockMvc.perform(put("/api/v1/admin/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/config/bulk")
    class UpdateBulkConfigEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN updates bulk configs - returns 200 OK")
        void superAdminCanBulkUpdate() throws Exception {
            BulkConfigUpdateRequest request = new BulkConfigUpdateRequest(
                    Map.of("auth.mfa.enforced", "true", "auth.session.timeout.minutes", "45"));

            when(configChangeService.updateBulkConfigs(any(), any(), anyString()))
                    .thenReturn(Map.of("auth.mfa.enforced", "true", "auth.session.timeout.minutes", "45"));

            mockMvc.perform(put("/api/v1/admin/config/bulk")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['auth.mfa.enforced']").value("true"))
                    .andExpect(jsonPath("$.['auth.session.timeout.minutes']").value("45"));
        }

        @Test
        @DisplayName("-ve: Empty configs map returns 400 Bad Request")
        void emptyConfigsReturnsBadRequest() throws Exception {
            BulkConfigUpdateRequest request = new BulkConfigUpdateRequest(Collections.emptyMap());

            mockMvc.perform(put("/api/v1/admin/config/bulk")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            BulkConfigUpdateRequest request = new BulkConfigUpdateRequest(Map.of("k", "v"));

            mockMvc.perform(put("/api/v1/admin/config/bulk")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            BulkConfigUpdateRequest request = new BulkConfigUpdateRequest(Map.of("k", "v"));

            mockMvc.perform(put("/api/v1/admin/config/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/config/reset")
    class ResetConfigEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN resets configs to defaults - returns 200 OK")
        void superAdminCanReset() throws Exception {
            when(configChangeService.resetToDefaults(any(), anyString()))
                    .thenReturn(Map.of("auth.mfa.enforced", "false"));

            mockMvc.perform(post("/api/v1/admin/config/reset")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['auth.mfa.enforced']").value("false"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/admin/config/reset")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/admin/config/reset"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
