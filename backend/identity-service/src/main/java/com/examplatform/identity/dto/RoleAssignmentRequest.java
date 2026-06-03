package com.examplatform.identity.dto;

import com.examplatform.identity.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {

    @NotNull
    private UserRole role;

    @NotNull
    private RoleAction action;
}
