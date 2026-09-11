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

package com.examplatform.identity.controller;

import com.examplatform.identity.service.AuditEventPublisher;
import com.examplatform.identity.service.KeyRevocationScheduler;
import com.examplatform.identity.service.VaultCryptoService;
import com.examplatform.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("VaultAdminController REST Endpoints E2E Tests (MockMvc)")
class VaultAdminControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private KeyRevocationScheduler keyRevocationScheduler;

    @MockitoBean
    private VaultCryptoService vaultCryptoService;

    @MockitoBean
    private AuditEventPublisher auditEventPublisher;

    private static final String KEY_NAME = "transit-exam-key-v1";
    private static final UUID ADMIN_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    // =========================================================================
    // 1. POST /api/v1/identity/admin/vault/keys/{keyName}/revoke
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/admin/vault/keys/{keyName}/revoke")
    class RevokeKeyEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN schedules key revocation - returns 202 Accepted")
        void securityAdminCanScheduleRevocation() throws Exception {
            doNothing().when(keyRevocationScheduler).scheduleRevocation(KEY_NAME);

            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/revoke", KEY_NAME)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Key " + KEY_NAME + " scheduled for revocation within 60 seconds."));
        }

        @Test
        @DisplayName("-ve: CANDIDATE receives 403 Forbidden")
        void candidateCannotRevokeKey() throws Exception {
            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/revoke", KEY_NAME)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request receives 401 Unauthorized")
        void unauthenticatedCannotRevokeKey() throws Exception {
            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/revoke", KEY_NAME))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. POST /api/v1/identity/admin/vault/keys/{keyName}/rotate
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/identity/admin/vault/keys/{keyName}/rotate")
    class RotateKeyEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN rotates key - returns 200 OK")
        void securityAdminCanRotateKey() throws Exception {
            doNothing().when(vaultCryptoService).rotateKey(KEY_NAME);

            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/rotate", KEY_NAME)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Key " + KEY_NAME + " rotated successfully."));
        }

        @Test
        @DisplayName("-ve: CANDIDATE receives 403 Forbidden")
        void candidateCannotRotateKey() throws Exception {
            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/rotate", KEY_NAME)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")).jwt(j -> j.subject(ADMIN_ID.toString()))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request receives 401 Unauthorized")
        void unauthenticatedCannotRotateKey() throws Exception {
            mockMvc.perform(post("/api/v1/identity/admin/vault/keys/{keyName}/rotate", KEY_NAME))
                    .andExpect(status().isUnauthorized());
        }
    }
}
