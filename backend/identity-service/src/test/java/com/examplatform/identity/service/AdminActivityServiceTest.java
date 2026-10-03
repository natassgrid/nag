/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.service;

import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.AdminActivityLogResponse;
import com.examplatform.identity.dto.AdminPermissionDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActivityServiceTest {

    @Mock
    private RoleManagementService roleManagementService;

    private AdminActivityService activityService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        activityService = new AdminActivityService(roleManagementService, new ObjectMapper());
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should query activity timeline with pagination")
    void shouldQueryActivityTimeline() {
        List<AdminActivityLogResponse> result = activityService.getActivityLogs(
                userId, null, 0, 10, "tenant-01");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should filter activity timeline by category")
    void shouldFilterActivityByCategory() {
        List<AdminActivityLogResponse> result = activityService.getActivityLogs(
                userId, "AUTH", 0, 10, "tenant-01");

        assertThat(result).isNotNull();
        assertThat(result).allMatch(l -> "AUTH".equalsIgnoreCase(l.getCategory()));
    }

    @Test
    @DisplayName("Should export activity logs as CSV and JSON")
    void shouldExportActivityLogs() {
        String csv = activityService.exportActivityLogs(userId, "csv", "tenant-01");
        assertThat(csv).isNotEmpty();
        assertThat(csv).contains("ID,Event Type,Category,Description,IP Address,Status");

        String json = activityService.exportActivityLogs(userId, "json", "tenant-01");
        assertThat(json).isNotEmpty();
        assertThat(json).startsWith("[");
    }

    @Test
    @DisplayName("Should compute granular system permissions")
    void shouldComputeGranularPermissions() {
        when(roleManagementService.getRoles(userId, "tenant-01"))
                .thenReturn(List.of(UserRole.SUPER_ADMIN));

        List<AdminPermissionDetailResponse> perms = activityService.getEffectivePermissions(userId, "tenant-01");
        assertThat(perms).isNotEmpty();
        assertThat(perms).allMatch(p -> p.getCategory() != null && p.getCode() != null);
    }
}
