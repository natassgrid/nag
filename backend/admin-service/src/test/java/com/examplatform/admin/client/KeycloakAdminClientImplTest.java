/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\n *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.admin.client;

import com.examplatform.admin.config.KeycloakProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminClientImplTest {

    private KeycloakProperties properties;
    private KeycloakAdminClientImpl client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new KeycloakProperties();
        properties.setServerUrl("http://localhost:8080");
        properties.setRealm("exam-realm");
        properties.setAdminClientId("admin-cli");
        properties.setAdminUsername("admin");
        properties.setAdminPassword("admin_secret");

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        client = new KeycloakAdminClientImpl(properties);
        ReflectionTestUtils.setField(client, "restClient", restClientBuilder.build());
    }

    @Test
    @DisplayName("Successfully disables user and revokes active sessions in Keycloak")
    void disableUser_success() {
        UUID userId = UUID.randomUUID();

        // 1. Mock admin token request
        mockServer.expect(requestTo("http://localhost:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"mock-admin-token\"}", APPLICATION_JSON));

        // 2. Mock disable user request
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/tenant-123/users/" + userId))
                .andExpect(method(PUT))
                .andExpect(header("Authorization", "Bearer mock-admin-token"))
                .andRespond(withSuccess());

        // 3. Mock logout user request
        mockServer.expect(requestTo("http://localhost:8080/admin/realms/tenant-123/users/" + userId + "/logout"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer mock-admin-token"))
                .andRespond(withSuccess());

        assertThatCode(() -> client.disableUser(userId, "tenant-123"))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    @Test
    @DisplayName("Throws RuntimeException when Keycloak token request fails")
    void disableUser_tokenFailure_throwsException() {
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo("http://localhost:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.disableUser(userId, "default"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Keycloak admin service unavailable");

        mockServer.verify();
    }
}
