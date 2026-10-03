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

package com.examplatform.identity.controller;

import com.examplatform.identity.dto.AcceptInviteRequest;
import com.examplatform.identity.dto.AdminInviteRequest;
import com.examplatform.identity.dto.AdminInviteResponse;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.ValidateInviteResponse;
import com.examplatform.identity.exception.InvitationExpiredException;
import com.examplatform.identity.exception.InvitationNotFoundException;
import com.examplatform.identity.service.AdminInvitationService;
import com.examplatform.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminInvitationController REST Endpoints E2E Tests (MockMvc)")
class AdminInvitationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AdminInvitationService adminInvitationService;

    private static final String TENANT_ID = "default";
    private static final UUID INVITER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVITATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("POST /api/v1/identity/admin/invite - Issue Invitation")
    class IssueInvitationEndpoint {

        @Test
        @DisplayName("201 CREATED - SUPER_ADMIN sends invitation to new staff member")
        void superAdminCanInviteStaff() throws Exception {
            AdminInviteRequest request = AdminInviteRequest.builder()
                    .email("examiner@nag.gov.in")
                    .fullName("Dr. Vikram Sarabhai")
                    .roles(List.of("QUESTION_AUTHOR"))
                    .build();

            AdminInviteResponse response = AdminInviteResponse.builder()
                    .invitationId(INVITATION_ID)
                    .email("examiner@nag.gov.in")
                    .status("PENDING")
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            when(adminInvitationService.inviteAdmin(any(AdminInviteRequest.class), eq(INVITER_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/identity/admin/invite")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(INVITER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.invitationId").value(INVITATION_ID.toString()))
                    .andExpect(jsonPath("$.data.email").value("examiner@nag.gov.in"));
        }

        @Test
        @DisplayName("403 FORBIDDEN - CANDIDATE cannot send invitations")
        void candidateCannotInvite() throws Exception {
            AdminInviteRequest request = AdminInviteRequest.builder()
                    .email("examiner@nag.gov.in")
                    .fullName("Dr. Vikram Sarabhai")
                    .roles(List.of("QUESTION_AUTHOR"))
                    .build();

            mockMvc.perform(post("/api/v1/identity/admin/invite")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(INVITER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 BAD REQUEST - Validation fails on invalid email or empty roles")
        void invalidPayloadReturns400() throws Exception {
            AdminInviteRequest request = AdminInviteRequest.builder()
                    .email("not-an-email")
                    .roles(List.of())
                    .build();

            mockMvc.perform(post("/api/v1/identity/admin/invite")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(INVITER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/identity/admin/invite/validate - Validate Token")
    class ValidateTokenEndpoint {

        @Test
        @DisplayName("200 OK - Valid invitation token returns invite details")
        void validTokenReturnsDetails() throws Exception {
            ValidateInviteResponse response = ValidateInviteResponse.builder()
                    .valid(true)
                    .email("examiner@nag.gov.in")
                    .fullName("Dr. Vikram Sarabhai")
                    .roles(List.of("QUESTION_AUTHOR"))
                    .tenantId(TENANT_ID)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .message("Invitation token is valid.")
                    .build();

            when(adminInvitationService.validateInvitationToken("valid-token-xyz")).thenReturn(response);

            mockMvc.perform(get("/api/v1/identity/admin/invite/validate")
                            .param("token", "valid-token-xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.email").value("examiner@nag.gov.in"))
                    .andExpect(jsonPath("$.data.valid").value(true));
        }

        @Test
        @DisplayName("404 NOT FOUND - Non-existent token returns 404")
        void nonExistentTokenReturns404() throws Exception {
            when(adminInvitationService.validateInvitationToken("invalid-token"))
                    .thenThrow(new InvitationNotFoundException("Invalid or expired invitation token"));

            mockMvc.perform(get("/api/v1/identity/admin/invite/validate")
                            .param("token", "invalid-token"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 BAD REQUEST - Expired invitation token returns 400")
        void expiredTokenReturns400() throws Exception {
            when(adminInvitationService.validateInvitationToken("expired-token"))
                    .thenThrow(new InvitationExpiredException("Invitation has expired"));

            mockMvc.perform(get("/api/v1/identity/admin/invite/validate")
                            .param("token", "expired-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/identity/admin/invite/accept - Accept Invitation")
    class AcceptInvitationEndpoint {

        @Test
        @DisplayName("200 OK - Candidate/admin completes onboarding and receives authentication tokens")
        void acceptInviteSuccess() throws Exception {
            AcceptInviteRequest request = AcceptInviteRequest.builder()
                    .token("valid-token-xyz")
                    .password("Str0ngP@ssw0rd!")
                    .totpSecret("JBSWY3DPEHPK3PXP")
                    .totpCode("123456")
                    .build();

            AuthTokenResponse authTokens = AuthTokenResponse.builder()
                    .accessToken("jwt-access-token")
                    .refreshToken("jwt-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .build();

            when(adminInvitationService.acceptInvitation(any(AcceptInviteRequest.class), eq(TENANT_ID)))
                    .thenReturn(authTokens);

            mockMvc.perform(post("/api/v1/identity/admin/invite/accept")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"));
        }
    }
}
