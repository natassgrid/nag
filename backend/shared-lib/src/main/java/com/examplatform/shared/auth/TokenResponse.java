package com.examplatform.shared.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the response from an OAuth2 token endpoint (client_credentials grant).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /** The JWT access token. */
    private String accessToken;

    /** Token expiry in seconds from the time of issuance. */
    private long expiresIn;

    /** The token type (typically "Bearer"). */
    private String tokenType;
}
