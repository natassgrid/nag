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

package com.examplatform.identity.service;

import com.examplatform.identity.domain.Permission;
import com.examplatform.identity.domain.RoleDefinition;
import com.examplatform.identity.repository.PermissionRepository;
import com.examplatform.identity.repository.RoleDefinitionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantBootstrapService Unit Tests")
class TenantBootstrapServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleDefinitionRepository roleDefinitionRepository;

    @InjectMocks
    private TenantBootstrapService tenantBootstrapService;

    @Test
    @DisplayName("Bootstraps baseline permissions and roles when none exist for tenant")
    void bootstrapsAllWhenEmpty() {
        String tenantId = "upsc";

        when(permissionRepository.findByTenantId(tenantId))
                .thenReturn(new ArrayList<>())
                .thenReturn(List.of(
                        Permission.builder().code("IDENTITY:USER_READ").name("View Users").module("IDENTITY").build(),
                        Permission.builder().code("QUESTION:READ").name("View Questions").module("QUESTION_BANK").build()
                ));
        when(roleDefinitionRepository.existsByCodeAndTenantId(anyString(), eq(tenantId)))
                .thenReturn(false);

        tenantBootstrapService.ensureTenantBootstrapped(tenantId);

        verify(permissionRepository).saveAll(any());
        verify(roleDefinitionRepository, atLeastOnce()).save(any(RoleDefinition.class));
    }

    @Test
    @DisplayName("Skips bootstrapping if permissions and roles already exist")
    void skipsBootstrappingWhenAlreadyPresent() {
        String tenantId = "ssc";

        List<Permission> existingPerms = List.of(
                Permission.builder().code("IDENTITY:USER_READ").name("View Users").module("IDENTITY").build(),
                Permission.builder().code("IDENTITY:USER_WRITE").name("Manage Users").module("IDENTITY").build(),
                Permission.builder().code("IDENTITY:ROLE_READ").name("View Roles").module("IDENTITY").build(),
                Permission.builder().code("IDENTITY:ROLE_MANAGE").name("Manage Roles").module("IDENTITY").build(),
                Permission.builder().code("IDENTITY:USER_ROLE_ASSIGN").name("Assign Roles").module("IDENTITY").build(),
                Permission.builder().code("IDENTITY:MFA_MANAGE").name("Manage MFA").module("IDENTITY").build(),
                Permission.builder().code("QUESTION:READ").name("View Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:CREATE").name("Create Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:EDIT").name("Edit Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:DELETE").name("Delete Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:REVIEW").name("Review Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:APPROVE").name("Approve Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:TRANSLATE").name("Translate Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("QUESTION:IMPORT_EXPORT").name("Import/Export Questions").module("QUESTION_BANK").build(),
                Permission.builder().code("EXAM:READ").name("View Examinations").module("EXAM_MANAGEMENT").build(),
                Permission.builder().code("EXAM:CREATE").name("Create Examinations").module("EXAM_MANAGEMENT").build(),
                Permission.builder().code("EXAM:SCHEDULE").name("Schedule Examinations").module("EXAM_MANAGEMENT").build(),
                Permission.builder().code("EXAM:PUBLISH").name("Publish Examinations").module("EXAM_MANAGEMENT").build(),
                Permission.builder().code("EXAM:CANCEL").name("Cancel Examinations").module("EXAM_MANAGEMENT").build(),
                Permission.builder().code("DELIVERY:MONITOR").name("Live Monitoring").module("EXAM_DELIVERY").build(),
                Permission.builder().code("DELIVERY:ATTEND").name("Attend Exam").module("EXAM_DELIVERY").build(),
                Permission.builder().code("DELIVERY:PROCTOR").name("Proctor Exam").module("EXAM_DELIVERY").build(),
                Permission.builder().code("DELIVERY:RETEST").name("Authorize Retest").module("EXAM_DELIVERY").build(),
                Permission.builder().code("EVALUATION:READ").name("View Evaluation").module("ASSESSMENT_EVALUATION").build(),
                Permission.builder().code("EVALUATION:SCORE_AUTO").name("Execute Auto-Grading").module("ASSESSMENT_EVALUATION").build(),
                Permission.builder().code("EVALUATION:SCORE_MANUAL").name("Manual Scoring").module("ASSESSMENT_EVALUATION").build(),
                Permission.builder().code("EVALUATION:PUBLISH_RESULTS").name("Publish Results").module("ASSESSMENT_EVALUATION").build(),
                Permission.builder().code("ADMIN:SETTINGS_READ").name("View Settings").module("SYSTEM_ADMIN").build(),
                Permission.builder().code("ADMIN:SETTINGS_WRITE").name("Manage Settings").module("SYSTEM_ADMIN").build(),
                Permission.builder().code("ADMIN:AUDIT_READ").name("View Audit Logs").module("SYSTEM_ADMIN").build(),
                Permission.builder().code("ADMIN:VAULT_MANAGE").name("Manage Keys").module("SYSTEM_ADMIN").build()
        );

        when(permissionRepository.findByTenantId(tenantId)).thenReturn(existingPerms);
        when(roleDefinitionRepository.existsByCodeAndTenantId(anyString(), eq(tenantId)))
                .thenReturn(true);

        tenantBootstrapService.ensureTenantBootstrapped(tenantId);

        verify(permissionRepository, never()).saveAll(any());
        verify(roleDefinitionRepository, never()).save(any(RoleDefinition.class));
    }
}
