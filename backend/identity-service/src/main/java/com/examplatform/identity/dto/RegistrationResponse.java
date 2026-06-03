package com.examplatform.identity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponse {

    private String message;
    private String userId;
}
