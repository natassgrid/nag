package com.examplatform.identity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    @Builder.Default
    private String tokenType = "Bearer";
}
