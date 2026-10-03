/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.admin.controller;

import com.examplatform.admin.dto.MfaPolicySettingsRequest;
import com.examplatform.admin.service.ConfigChangeService;
import com.examplatform.admin.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminSecuritySettingsController REST Endpoints E2E Tests (MockMvc)")
class AdminSecuritySettingsControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ConfigChangeService configChangeService;

    private static final String TENANT_ID = "default";
    private static final UUID ACTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("GET /api/v1/admin/settings/security/mfa - MFA Policy Retrieval")
    class GetMfaPolicyEndpoint {

        @Test
        @DisplayName("200 OK - SUPER_ADMIN retrieves MFA policy settings")
        void superAdminCanGetMfaPolicy() throws Exception {
            when(configChangeService.getConfigMap(eq(TENANT_ID))).thenReturn(Map.of(
                    "auth.mfa.admin.policy", "MANDATORY",
                    "auth.mfa.candidate.policy", "OPTIONAL",
                    "auth.mfa.allowed.methods", "TOTP,EMAIL_OTP",
                    "auth.mfa.enforced", "true"
            ));

            mockMvc.perform(get("/api/v1/admin/settings/security/mfa")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adminMfaPolicy").value("MANDATORY"))
                    .andExpect(jsonPath("$.candidateMfaPolicy").value("OPTIONAL"))
                    .andExpect(jsonPath("$.allowedMethods.length()").value(2))
                    .andExpect(jsonPath("$.allowedMethods[0]").value("TOTP"))
                    .andExpect(jsonPath("$.allowedMethods[1]").value("EMAIL_OTP"))
                    .andExpect(jsonPath("$.globalMfaEnforced").value(true));
        }

        @Test
        @DisplayName("200 OK - SECURITY_ADMIN retrieves default MFA policy settings when config map empty")
        void securityAdminGetsDefaultsWhenEmpty() throws Exception {
            when(configChangeService.getConfigMap(eq(TENANT_ID))).thenReturn(Map.of());

            mockMvc.perform(get("/api/v1/admin/settings/security/mfa")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adminMfaPolicy").value("OPTIONAL"))
                    .andExpect(jsonPath("$.candidateMfaPolicy").value("OPTIONAL"))
                    .andExpect(jsonPath("$.globalMfaEnforced").value(false));
        }

        @Test
        @DisplayName("403 FORBIDDEN - ROLE_CANDIDATE is forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/settings/security/mfa")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - Unauthenticated request returns 401")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/settings/security/mfa"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/settings/security/mfa - MFA Policy Update")
    class UpdateMfaPolicyEndpoint {

        @Test
        @DisplayName("200 OK - SUPER_ADMIN updates MFA security policies")
        void superAdminUpdatesMfaPolicy() throws Exception {
            MfaPolicySettingsRequest request = new MfaPolicySettingsRequest(
                    "MANDATORY",
                    "MANDATORY",
                    List.of("TOTP", "RECOVERY_CODES"),
                    true
            );

            when(configChangeService.updateBulkConfigs(any(), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenReturn(Map.of());
            when(configChangeService.getConfigMap(eq(TENANT_ID))).thenReturn(Map.of(
                    "auth.mfa.admin.policy", "MANDATORY",
                    "auth.mfa.candidate.policy", "MANDATORY",
                    "auth.mfa.allowed.methods", "TOTP,RECOVERY_CODES",
                    "auth.mfa.enforced", "true"
            ));

            mockMvc.perform(put("/api/v1/admin/settings/security/mfa")
                            .param("tenantId", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.adminMfaPolicy").value("MANDATORY"))
                    .andExpect(jsonPath("$.candidateMfaPolicy").value("MANDATORY"))
                    .andExpect(jsonPath("$.globalMfaEnforced").value(true));
        }

        @Test
        @DisplayName("403 FORBIDDEN - ROLE_CANDIDATE cannot update MFA policies")
        void candidateForbiddenFromUpdating() throws Exception {
            MfaPolicySettingsRequest request = new MfaPolicySettingsRequest(
                    "OPTIONAL",
                    null,
                    null,
                    null
            );

            mockMvc.perform(put("/api/v1/admin/settings/security/mfa")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
