package com.examplatform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountResponse {
    private UUID id;
    private String username;
    private String accountStatus;
    private boolean mfaEnabled;
    private List<String> roles;
    private Instant createdAt;
}
