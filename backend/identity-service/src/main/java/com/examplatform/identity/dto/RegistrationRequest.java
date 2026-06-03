package com.examplatform.identity.dto;

import com.examplatform.identity.domain.enums.IdentityDocType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String mobile;

    @NotNull
    private IdentityDocType identityDocType;

    @NotBlank
    private String identityDocNumber;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;
}
