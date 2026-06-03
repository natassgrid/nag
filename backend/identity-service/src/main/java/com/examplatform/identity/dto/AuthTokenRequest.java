package com.examplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String otpCode;

    private String deviceFingerprint;
}
