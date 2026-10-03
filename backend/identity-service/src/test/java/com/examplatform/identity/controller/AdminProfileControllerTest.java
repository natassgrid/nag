/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.controller;

import com.examplatform.identity.dto.*;
import com.examplatform.identity.service.AdminActivityService;
import com.examplatform.identity.service.AdminInvitationService;
import com.examplatform.identity.service.AuthenticationService;
import com.examplatform.identity.service.PersonalAccessTokenService;
import com.examplatform.identity.service.RoleManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminProfileControllerTest {

    @Mock
    private RoleManagementService roleManagementService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AdminInvitationService adminInvitationService;

    @Mock
    private PersonalAccessTokenService tokenService;

    @Mock
    private AdminActivityService activityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AdminProfileController controller = new AdminProfileController(
                roleManagementService, authenticationService, adminInvitationService, tokenService, activityService);

        HandlerMethodArgumentResolver jwtArgumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return Jwt.withTokenValue("mock-jwt-token")
                        .header("alg", "none")
                        .claim("sub", "admin@nag.gov.in")
                        .claim("preferred_username", "admin@nag.gov.in")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtArgumentResolver)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/admin/me/profile - should return current admin profile")
    void shouldReturnAdminProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccountResponse response = UserAccountResponse.builder()
                .id(userId)
                .username("admin@nag.gov.in")
                .fullName("Dr. Admin User")
                .tenantId("tenant-01")
                .timezone("Asia/Kolkata")
                .build();

        when(roleManagementService.getUserProfile(anyString(), eq("tenant-01")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/me/profile")
                        .header("X-Tenant-Id", "tenant-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin@nag.gov.in"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/me/profile - should update admin profile")
    void shouldUpdateAdminProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .fullName("Updated Admin")
                .timezone("Asia/Kolkata")
                .preferredLanguage("hi")
                .build();

        UserAccountResponse response = UserAccountResponse.builder()
                .id(userId)
                .username("admin@nag.gov.in")
                .fullName("Updated Admin")
                .tenantId("tenant-01")
                .timezone("Asia/Kolkata")
                .preferredLanguage("hi")
                .build();

        when(roleManagementService.updateUserProfile(anyString(), any(), eq("tenant-01")))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/me/profile")
                        .header("X-Tenant-Id", "tenant-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Admin"))
                .andExpect(jsonPath("$.data.preferredLanguage").value("hi"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/me/tokens - should return list of tokens")
    void shouldReturnTokens() throws Exception {
        PersonalAccessTokenResponse tokenResp = PersonalAccessTokenResponse.builder()
                .id(UUID.randomUUID())
                .name("CI Pipeline")
                .tokenPrefix("nag_pat_123")
                .scopes(List.of("READ", "WRITE"))
                .revoked(false)
                .build();

        when(tokenService.listTokens(any(), eq("tenant-01")))
                .thenReturn(List.of(tokenResp));

        mockMvc.perform(get("/api/v1/admin/me/tokens")
                        .header("X-Tenant-Id", "tenant-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CI Pipeline"))
                .andExpect(jsonPath("$.data[0].scopes[0]").value("READ"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/me/tokens - should create token and return raw secret")
    void shouldCreateToken() throws Exception {
        CreatePersonalAccessTokenRequest req = CreatePersonalAccessTokenRequest.builder()
                .name("New Token")
                .scopes(List.of("READ"))
                .build();

        PersonalAccessTokenResponse tokenResp = PersonalAccessTokenResponse.builder()
                .id(UUID.randomUUID())
                .name("New Token")
                .token("nag_pat_secret12345")
                .tokenPrefix("nag_pat_new")
                .scopes(List.of("READ"))
                .revoked(false)
                .build();

        when(tokenService.createToken(any(), any(), eq("tenant-01")))
                .thenReturn(tokenResp);

        mockMvc.perform(post("/api/v1/admin/me/tokens")
                        .header("X-Tenant-Id", "tenant-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("nag_pat_secret12345"))
                .andExpect(jsonPath("$.data.name").value("New Token"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/me/tokens/{tokenId} - should revoke token")
    void shouldRevokeToken() throws Exception {
        UUID tokenId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/me/tokens/" + tokenId)
                        .header("X-Tenant-Id", "tenant-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(tokenService).revokeToken(eq(tokenId), any(), eq("tenant-01"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/me/activity - should return paginated activity logs")
    void shouldReturnActivityLogs() throws Exception {
        AdminActivityLogResponse logResp = AdminActivityLogResponse.builder()
                .id("ACT-01")
                .occurredAt(Instant.now())
                .eventType("LOGIN")
                .category("AUTH")
                .description("Logged in")
                .ipAddress("127.0.0.1")
                .status("SUCCESS")
                .build();

        when(activityService.getActivityLogs(any(), any(), anyInt(), anyInt(), eq("tenant-01")))
                .thenReturn(List.of(logResp));

        mockMvc.perform(get("/api/v1/admin/me/activity")
                        .header("X-Tenant-Id", "tenant-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("LOGIN"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/me/activity/export - should export logs")
    void shouldExportLogs() throws Exception {
        when(activityService.exportActivityLogs(any(), eq("csv"), eq("tenant-01")))
                .thenReturn("header,col\n1,2");

        mockMvc.perform(get("/api/v1/admin/me/activity/export")
                        .param("format", "csv")
                        .header("X-Tenant-Id", "tenant-01"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("admin-activity-")))
                .andExpect(content().contentType("text/csv"));
    }
}
