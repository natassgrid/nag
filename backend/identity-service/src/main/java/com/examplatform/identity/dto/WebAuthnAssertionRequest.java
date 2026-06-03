package com.examplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the WebAuthn / FIDO2 assertion from the client.
 * All byte-array fields are transmitted as Base64URL-encoded strings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnAssertionRequest {

    /** Base64URL-encoded credential ID. */
    @NotBlank
    private String credentialId;

    /** Base64URL-encoded authenticator data. */
    @NotBlank
    private String authenticatorData;

    /** Base64URL-encoded client data JSON. */
    @NotBlank
    private String clientDataJSON;

    /** Base64URL-encoded signature. */
    @NotBlank
    private String signature;

    /** Optional: Base64URL user handle (userId). */
    private String userHandle;
}
