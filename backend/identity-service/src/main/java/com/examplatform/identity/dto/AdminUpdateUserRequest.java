package com.examplatform.identity.dto;

import com.examplatform.identity.domain.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin-initiated user update.
 * All fields are optional — only non-null fields are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    private String fullName;

    private AccountStatus accountStatus;

    private Boolean mfaEnabled;
}
