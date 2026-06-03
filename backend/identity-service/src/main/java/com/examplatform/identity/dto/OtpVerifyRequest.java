package com.examplatform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    @NotBlank
    private String mobile;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;
}
