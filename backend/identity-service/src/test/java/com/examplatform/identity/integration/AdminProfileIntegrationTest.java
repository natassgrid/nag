/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.integration;

import com.examplatform.identity.dto.*;
import com.examplatform.identity.service.*;
import com.examplatform.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AdminProfileController E2E Integration Tests (MockMvc & Spring Security)")
class AdminProfileIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private RoleManagementService roleManagementService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AdminInvitationService adminInvitationService;

    @MockitoBean
    private PersonalAccessTokenService tokenService;

    @MockitoBean
    private AdminActivityService activityService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("Integration: GET and PUT /api/v1/admin/me/profile")
    void testGetAndUpdateProfile() throws Exception {
        UserAccountResponse profileResponse = UserAccountResponse.builder()
                .id(ADMIN_ID)
                .username("admin@nag.gov.in")
                .fullName("Dr. Super Admin")
                .timezone("Asia/Kolkata")
                .dateFormat("DD/MM/YYYY")
                .timeFormat("24h")
                .preferredLanguage("en")
                .themePreference("system")
                .tenantId(TENANT_ID)
                .build();

        when(roleManagementService.getUserProfile(ADMIN_ID.toString(), TENANT_ID))
                .thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/admin/me/profile")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin@nag.gov.in"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"));

        AdminUpdateUserRequest updateReq = AdminUpdateUserRequest.builder()
                .fullName("Dr. Super Admin Updated")
                .designation("Principal Architect")
                .timezone("Asia/Kolkata")
                .dateFormat("YYYY-MM-DD")
                .timeFormat("12h")
                .preferredLanguage("hi")
                .themePreference("dark")
                .build();

        UserAccountResponse updatedProfile = UserAccountResponse.builder()
                .id(ADMIN_ID)
                .username("admin@nag.gov.in")
                .fullName("Dr. Super Admin Updated")
                .designation("Principal Architect")
                .timezone("Asia/Kolkata")
                .dateFormat("YYYY-MM-DD")
                .timeFormat("12h")
                .preferredLanguage("hi")
                .themePreference("dark")
                .tenantId(TENANT_ID)
                .build();

        when(roleManagementService.updateUserProfile(eq(ADMIN_ID.toString()), any(), eq(TENANT_ID)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/v1/admin/me/profile")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Dr. Super Admin Updated"))
                .andExpect(jsonPath("$.data.designation").value("Principal Architect"))
                .andExpect(jsonPath("$.data.dateFormat").value("YYYY-MM-DD"))
                .andExpect(jsonPath("$.data.preferredLanguage").value("hi"))
                .andExpect(jsonPath("$.data.themePreference").value("dark"));
    }

    @Test
    @DisplayName("Integration: PAT lifecycle (Create -> List -> Revoke)")
    void testPersonalAccessTokenLifecycle() throws Exception {
        CreatePersonalAccessTokenRequest createReq = CreatePersonalAccessTokenRequest.builder()
                .name("Integration Pipeline PAT")
                .scopes(List.of("READ", "WRITE"))
                .expiresInDays(60)
                .ipWhitelist("127.0.0.1")
                .build();

        UUID tokenId = UUID.randomUUID();
        PersonalAccessTokenResponse createdToken = PersonalAccessTokenResponse.builder()
                .id(tokenId)
                .name("Integration Pipeline PAT")
                .token("nag_pat_secret1234567890abcdef")
                .tokenPrefix("nag_pat_secret...")
                .scopes(List.of("READ", "WRITE"))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        when(tokenService.createToken(eq(ADMIN_ID), any(), eq(TENANT_ID)))
                .thenReturn(createdToken);

        mockMvc.perform(post("/api/v1/admin/me/tokens")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("nag_pat_secret1234567890abcdef"))
                .andExpect(jsonPath("$.data.name").value("Integration Pipeline PAT"))
                .andExpect(jsonPath("$.data.scopes[0]").value("READ"));

        when(tokenService.listTokens(eq(ADMIN_ID), eq(TENANT_ID)))
                .thenReturn(List.of(createdToken));

        mockMvc.perform(get("/api/v1/admin/me/tokens")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Integration Pipeline PAT"));

        doNothing().when(tokenService).revokeToken(eq(tokenId), eq(ADMIN_ID), eq(TENANT_ID));

        mockMvc.perform(delete("/api/v1/admin/me/tokens/" + tokenId)
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Integration: GET /api/v1/admin/me/activity and export")
    void testActivityTimelineAndExport() throws Exception {
        AdminActivityLogResponse logResponse = AdminActivityLogResponse.builder()
                .id("ACT-001")
                .occurredAt(Instant.now())
                .eventType("LOGIN")
                .category("AUTH")
                .description("Login successful")
                .ipAddress("127.0.0.1")
                .status("SUCCESS")
                .build();

        when(activityService.getActivityLogs(eq(ADMIN_ID), any(), anyInt(), anyInt(), eq(TENANT_ID)))
                .thenReturn(List.of(logResponse));

        mockMvc.perform(get("/api/v1/admin/me/activity")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("ACT-001"));

        when(activityService.exportActivityLogs(eq(ADMIN_ID), eq("csv"), eq(TENANT_ID)))
                .thenReturn("ID,Event Type\nACT-001,LOGIN");

        mockMvc.perform(get("/api/v1/admin/me/activity/export")
                        .with(jwt().jwt(j -> j.subject(ADMIN_ID.toString())
                                .claim("preferred_username", "admin@nag.gov.in"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .param("format", "csv")
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("admin-activity-")))
                .andExpect(content().contentType("text/csv"));
    }
}
