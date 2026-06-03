package com.examplatform.shared.auth;

/**
 * Client interface for requesting service-account tokens from the identity provider (Keycloak).
 * Implementations use the OAuth2 client_credentials grant to obtain short-lived JWTs
 * for inter-service authentication.
 */
public interface ServiceAccountTokenClient {

    /**
     * Requests a new access token for the specified service using the client_credentials grant.
     *
     * @param serviceId the service identifier (maps to a Keycloak client ID)
     * @return the token response containing the access token and expiry information
     */
    TokenResponse requestToken(String serviceId);
}
