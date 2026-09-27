/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.admin.controller;

import com.examplatform.admin.dto.MfaPolicySettingsRequest;
import com.examplatform.admin.dto.MfaPolicySettingsResponse;
import com.examplatform.admin.service.ConfigChangeService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for security and MFA policy management in Admin Portal.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings/security/mfa")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN')")
public class AdminSecuritySettingsController {

    private final ConfigChangeService configChangeService;

    /**
     * Retrieves current cluster MFA security policies.
     */
    @GetMapping
    public ResponseEntity<MfaPolicySettingsResponse> getMfaSecurityPolicy(
            @RequestParam(required = false) String tenantId) {
        String effectiveTenant = resolveTenant(tenantId);
        Map<String, String> configMap = configChangeService.getConfigMap(effectiveTenant);

        String adminPolicy = configMap.getOrDefault("auth.mfa.admin.policy", "OPTIONAL");
        String candidatePolicy = configMap.getOrDefault("auth.mfa.candidate.policy", "OPTIONAL");
        String allowedMethodsStr = configMap.getOrDefault("auth.mfa.allowed.methods", "TOTP,EMAIL_OTP,RECOVERY_CODES");
        boolean globalMfaEnforced = "true".equalsIgnoreCase(configMap.getOrDefault("auth.mfa.enforced", "false"));

        List<String> allowedMethods = Arrays.stream(allowedMethodsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        MfaPolicySettingsResponse response = MfaPolicySettingsResponse.builder()
                .adminMfaPolicy(adminPolicy)
                .candidateMfaPolicy(candidatePolicy)
                .allowedMethods(allowedMethods)
                .globalMfaEnforced(globalMfaEnforced)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Updates cluster MFA security policies.
     */
    @PutMapping
    public ResponseEntity<MfaPolicySettingsResponse> updateMfaSecurityPolicy(
            @Valid @RequestBody MfaPolicySettingsRequest request,
            @RequestParam(required = false) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = extractActorId(jwt);
        String effectiveTenant = resolveTenant(tenantId);

        Map<String, String> updates = new HashMap<>();
        if (request.adminMfaPolicy() != null) {
            updates.put("auth.mfa.admin.policy", request.adminMfaPolicy().toUpperCase());
        }
        if (request.candidateMfaPolicy() != null) {
            updates.put("auth.mfa.candidate.policy", request.candidateMfaPolicy().toUpperCase());
        }
        if (request.allowedMethods() != null && !request.allowedMethods().isEmpty()) {
            updates.put("auth.mfa.allowed.methods", String.join(",", request.allowedMethods()));
        }
        if (request.globalMfaEnforced() != null) {
            updates.put("auth.mfa.enforced", String.valueOf(request.globalMfaEnforced()));
        }

        configChangeService.updateBulkConfigs(updates, actorId, effectiveTenant);
        return getMfaSecurityPolicy(effectiveTenant);
    }

    private String resolveTenant(String paramTenant) {
        if (paramTenant != null && !paramTenant.isBlank()) {
            return paramTenant;
        }
        String contextTenant = TenantContext.get();
        if (contextTenant != null && !contextTenant.isBlank()) {
            return contextTenant;
        }
        return "default";
    }

    private UUID extractActorId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(jwt.getSubject().getBytes());
        }
    }
}
