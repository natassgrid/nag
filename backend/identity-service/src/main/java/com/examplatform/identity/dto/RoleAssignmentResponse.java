package com.examplatform.identity.dto;

import com.examplatform.identity.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoleAssignmentResponse {

    private UUID userId;
    private UserRole role;
    private RoleAction action;
    private String message;
}
