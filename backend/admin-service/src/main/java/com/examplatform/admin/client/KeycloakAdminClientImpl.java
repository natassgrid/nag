package com.examplatform.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of KeycloakAdminClient for local development.
 * In production, this would call the Keycloak Admin REST API.
 */
@Slf4j
@Component
public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    @Override
    public void disableUser(UUID userId, String tenantId) {
        log.info("[STUB] Disabling user {} in Keycloak for tenant {}", userId, tenantId);
    }
}
