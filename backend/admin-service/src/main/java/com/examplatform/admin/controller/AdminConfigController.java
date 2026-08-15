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
import com.examplatform.admin.repository.SystemConfigRepository;
import com.examplatform.admin.service.ConfigChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for system configuration management.
 * Accessible only by SUPER_ADMIN and SECURITY_ADMIN roles.
 * All changes are audited with old/new values and actor identity.
 */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY_ADMIN')")
public class AdminConfigController {

    private final SystemConfigRepository systemConfigRepository;
    private final ConfigChangeService configChangeService;

    /**
     * Retrieves all system configuration parameters for the given tenant.
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getConfig(@RequestParam String tenantId) {
        List<SystemConfig> configs = systemConfigRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Updates a system configuration parameter with full audit trail.
     *
     * @param paramName the configuration parameter name
     * @param newValue  the new value to set
     * @param tenantId  the tenant identifier
     * @param jwt       the authenticated principal's JWT token
     * @return the updated configuration entity
     */
    @PutMapping
    public ResponseEntity<SystemConfig> updateConfig(
            @RequestParam String paramName,
            @RequestParam String newValue,
            @RequestParam String tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        SystemConfig updated = configChangeService.updateConfig(paramName, newValue, actorId, tenantId);
        return ResponseEntity.ok(updated);
    }
}
