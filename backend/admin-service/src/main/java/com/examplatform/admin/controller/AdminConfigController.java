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

import com.examplatform.admin.domain.SystemConfig;
import com.examplatform.admin.dto.BulkConfigUpdateRequest;
import com.examplatform.admin.dto.SingleConfigUpdateRequest;
import com.examplatform.admin.dto.SystemConfigResponse;
import com.examplatform.admin.service.ConfigChangeService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for system configuration management.
 * Accessible only by SUPER_ADMIN, SECURITY_ADMIN, and ADMIN roles.
 * All changes are audited with old/new values and actor identity.
 */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN', 'ADMIN')")
public class AdminConfigController {

    private final ConfigChangeService configChangeService;

    /**
     * Retrieves all system configuration parameters for the given or current tenant.
     */
    @GetMapping
    public ResponseEntity<List<SystemConfigResponse>> getConfig(
            @RequestParam(required = false) String tenantId) {
        String effectiveTenant = resolveTenant(tenantId);
        List<SystemConfig> configs = configChangeService.getConfigs(effectiveTenant);
        List<SystemConfigResponse> response = configs.stream()
                .map(configChangeService::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all system configuration parameters as a key-value map.
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getConfigMap(
            @RequestParam(required = false) String tenantId) {
        String effectiveTenant = resolveTenant(tenantId);
        Map<String, String> map = configChangeService.getConfigMap(effectiveTenant);
        return ResponseEntity.ok(map);
    }

    /**
     * Updates a single system configuration parameter via JSON body.
     */
    @PutMapping
    public ResponseEntity<SystemConfigResponse> updateConfig(
            @Valid @RequestBody SingleConfigUpdateRequest request,
            @RequestParam(required = false) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = extractActorId(jwt);
        String effectiveTenant = resolveTenant(tenantId);
        SystemConfig updated = configChangeService.updateConfig(
                request.paramName(), request.paramValue(), actorId, effectiveTenant);
        return ResponseEntity.ok(configChangeService.toResponse(updated));
    }

    /**
     * Bulk updates multiple system configuration parameters in a single transaction.
     */
    @PutMapping("/bulk")
    public ResponseEntity<Map<String, String>> updateBulkConfig(
            @Valid @RequestBody BulkConfigUpdateRequest request,
            @RequestParam(required = false) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = extractActorId(jwt);
        String effectiveTenant = resolveTenant(tenantId);
        Map<String, String> updatedMap = configChangeService.updateBulkConfigs(
                request.configs(), actorId, effectiveTenant);
        return ResponseEntity.ok(updatedMap);
    }

    /**
     * Resets all configurations to platform defaults for the tenant.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetConfigToDefaults(
            @RequestParam(required = false) String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = extractActorId(jwt);
        String effectiveTenant = resolveTenant(tenantId);
        Map<String, String> defaults = configChangeService.resetToDefaults(actorId, effectiveTenant);
        return ResponseEntity.ok(defaults);
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
