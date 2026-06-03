package com.examplatform.admin.client;

import java.util.UUID;

/**
 * Client interface for Keycloak admin operations.
 * Implementations will use the Keycloak Admin REST API to manage user accounts.
 */
public interface KeycloakAdminClient {

    /**
     * Disables a user account in Keycloak, preventing further authentication.
     *
     * @param userId   the user's unique identifier (matches Keycloak sub claim)
     * @param tenantId the tenant (examination authority) identifier (maps to Keycloak realm or attribute)
     */
    void disableUser(UUID userId, String tenantId);
}
