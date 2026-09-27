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
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.repository.PermissionRepository;
import com.examplatform.identity.repository.RoleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for dynamic tenant bootstrapping.
 * Automatically provisions baseline platform permissions and system roles
 * for newly onboarded examination authorities / tenants.
 *
 * <p><strong>Validates: Requirement Multi-Tenancy Hardening (Issue #133)</strong></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBootstrapService {

    private final PermissionRepository permissionRepository;
    private final RoleDefinitionRepository roleDefinitionRepository;

    private static final List<PermissionSeed> BASELINE_PERMISSIONS = List.of(
            // IDENTITY Module
            new PermissionSeed("IDENTITY:USER_READ", "View Users", "View user accounts, statuses, and profiles", "IDENTITY"),
            new PermissionSeed("IDENTITY:USER_WRITE", "Manage Users", "Create, update, and deactivate user accounts", "IDENTITY"),
            new PermissionSeed("IDENTITY:ROLE_READ", "View Roles", "View role definitions, details, and permission mappings", "IDENTITY"),
            new PermissionSeed("IDENTITY:ROLE_MANAGE", "Manage Roles", "Create, update, and delete custom roles and assign permissions", "IDENTITY"),
            new PermissionSeed("IDENTITY:USER_ROLE_ASSIGN", "Assign Roles", "Assign and revoke roles for platform users", "IDENTITY"),
            new PermissionSeed("IDENTITY:MFA_MANAGE", "Manage MFA", "Configure and reset Multi-Factor Authentication for users", "IDENTITY"),

            // QUESTION_BANK Module
            new PermissionSeed("QUESTION:READ", "View Questions", "Browse, search, and inspect question items", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:CREATE", "Create Questions", "Author and draft new questions in question bank", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:EDIT", "Edit Questions", "Modify drafted questions, choices, and metadata", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:DELETE", "Delete Questions", "Remove questions from the question bank", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:REVIEW", "Review Questions", "Perform peer review, rubric check, and question validation", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:APPROVE", "Approve Questions", "Approve reviewed questions for publication in blueprints", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:TRANSLATE", "Translate Questions", "Translate questions and options into regional languages", "QUESTION_BANK"),
            new PermissionSeed("QUESTION:IMPORT_EXPORT", "Import/Export Questions", "Bulk import and export question banks (CSV, JSON, QTI)", "QUESTION_BANK"),

            // EXAM_MANAGEMENT Module
            new PermissionSeed("EXAM:READ", "View Examinations", "Browse and view exam blueprints and schedules", "EXAM_MANAGEMENT"),
            new PermissionSeed("EXAM:CREATE", "Create Examinations", "Define new examination blueprints and paper templates", "EXAM_MANAGEMENT"),
            new PermissionSeed("EXAM:SCHEDULE", "Schedule Examinations", "Configure time slots, test centers, and candidate allocations", "EXAM_MANAGEMENT"),
            new PermissionSeed("EXAM:PUBLISH", "Publish Examinations", "Publish exam schedules for candidate registration", "EXAM_MANAGEMENT"),
            new PermissionSeed("EXAM:CANCEL", "Cancel Examinations", "Cancel scheduled examination sessions and notify candidates", "EXAM_MANAGEMENT"),

            // EXAM_DELIVERY Module
            new PermissionSeed("DELIVERY:MONITOR", "Live Monitoring", "Monitor live exam sessions, heartbeats, and delivery status", "EXAM_DELIVERY"),
            new PermissionSeed("DELIVERY:ATTEND", "Attend Exam", "Launch candidate exam interface and submit responses", "EXAM_DELIVERY"),
            new PermissionSeed("DELIVERY:PROCTOR", "Proctor Exam", "Live proctoring oversight, anomaly flagging, and session controls", "EXAM_DELIVERY"),
            new PermissionSeed("DELIVERY:RETEST", "Authorize Retest", "Issue retest authorization for affected candidates", "EXAM_DELIVERY"),

            // ASSESSMENT_EVALUATION Module
            new PermissionSeed("EVALUATION:READ", "View Evaluation", "Access candidate response sheets and answer keys", "ASSESSMENT_EVALUATION"),
            new PermissionSeed("EVALUATION:SCORE_AUTO", "Execute Auto-Grading", "Run automated grading pipeline on objective responses", "ASSESSMENT_EVALUATION"),
            new PermissionSeed("EVALUATION:SCORE_MANUAL", "Manual Scoring", "Evaluate subjective and descriptive candidate responses", "ASSESSMENT_EVALUATION"),
            new PermissionSeed("EVALUATION:PUBLISH_RESULTS", "Publish Results", "Approve and publish finalized scorecards", "ASSESSMENT_EVALUATION"),

            // SYSTEM_ADMIN & SECURITY Module
            new PermissionSeed("ADMIN:SETTINGS_READ", "View Settings", "View system settings and tenant configurations", "SYSTEM_ADMIN"),
            new PermissionSeed("ADMIN:SETTINGS_WRITE", "Manage Settings", "Update security parameters, rate limits, and integrations", "SYSTEM_ADMIN"),
            new PermissionSeed("ADMIN:AUDIT_READ", "View Audit Logs", "Inspect security logs, event streams, and access trails", "SYSTEM_ADMIN"),
            new PermissionSeed("ADMIN:VAULT_MANAGE", "Manage Keys", "Manage Vault transit keys and encryption rotation", "SYSTEM_ADMIN")
    );

    private static final Map<UserRole, RoleSeed> SYSTEM_ROLES = Map.ofEntries(
            Map.entry(UserRole.SUPER_ADMIN, new RoleSeed("Super Admin", "Full system access with all permissions", List.of("*"))),
            Map.entry(UserRole.SECURITY_ADMIN, new RoleSeed("Security Admin", "Manages security policies and user access", List.of("IDENTITY:", "ADMIN:"))),
            Map.entry(UserRole.QUESTION_AUTHOR, new RoleSeed("Question Author", "Creates and edits examination questions", List.of("QUESTION:READ", "QUESTION:CREATE", "QUESTION:EDIT"))),
            Map.entry(UserRole.REVIEWER, new RoleSeed("Reviewer", "Reviews and validates questions", List.of("QUESTION:READ", "QUESTION:REVIEW"))),
            Map.entry(UserRole.SUBJECT_MATTER_EXPERT, new RoleSeed("Subject Matter Expert", "Subject matter expert for authoring and reviewing questions", List.of("QUESTION:READ", "QUESTION:CREATE", "QUESTION:EDIT", "QUESTION:REVIEW", "QUESTION:APPROVE"))),
            Map.entry(UserRole.APPROVER, new RoleSeed("Approver", "Approves questions and papers for publication", List.of("QUESTION:READ", "QUESTION:APPROVE", "EXAM:READ"))),
            Map.entry(UserRole.EXAM_CONTROLLER, new RoleSeed("Exam Controller", "Manages examination scheduling and delivery", List.of("EXAM:", "DELIVERY:", "EVALUATION:READ"))),
            Map.entry(UserRole.TRANSLATOR, new RoleSeed("Translator", "Translates questions to regional languages", List.of("QUESTION:READ", "QUESTION:TRANSLATE"))),
            Map.entry(UserRole.EVALUATOR, new RoleSeed("Evaluator", "Evaluates subjective responses", List.of("EVALUATION:"))),
            Map.entry(UserRole.AUDITOR, new RoleSeed("Auditor", "Views audit trails and compliance reports", List.of("ADMIN:AUDIT_READ", "IDENTITY:USER_READ", "EXAM:READ"))),
            Map.entry(UserRole.CANDIDATE, new RoleSeed("Candidate", "Takes examinations and views results", List.of("DELIVERY:ATTEND", "EXAM:READ")))
    );

    /**
     * Ensures all baseline platform permissions and system roles exist for the given tenant.
     *
     * @param tenantId the tenant to bootstrap
     */
    @Transactional
    public void ensureTenantBootstrapped(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }

        // 1. Check / Bootstrap Permissions
        List<Permission> existingPermissions = permissionRepository.findByTenantId(tenantId);
        Set<String> existingPermCodes = existingPermissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        List<Permission> toSavePerms = new ArrayList<>();
        for (PermissionSeed seed : BASELINE_PERMISSIONS) {
            if (!existingPermCodes.contains(seed.code)) {
                Permission p = Permission.builder()
                        .code(seed.code)
                        .name(seed.name)
                        .description(seed.description)
                        .module(seed.module)
                        .build();
                p.setTenantId(tenantId);
                toSavePerms.add(p);
            }
        }
        if (!toSavePerms.isEmpty()) {
            permissionRepository.saveAll(toSavePerms);
            log.info("Bootstrapped {} permissions for tenant [{}]", toSavePerms.size(), tenantId);
        }

        // Fetch all permissions for this tenant to map to roles
        List<Permission> allTenantPerms = permissionRepository.findByTenantId(tenantId);
        Map<String, Permission> permByCode = allTenantPerms.stream()
                .collect(Collectors.toMap(Permission::getCode, p -> p, (a, b) -> a));

        // 2. Check / Bootstrap System Roles
        for (Map.Entry<UserRole, RoleSeed> entry : SYSTEM_ROLES.entrySet()) {
            UserRole roleEnum = entry.getKey();
            RoleSeed roleSeed = entry.getValue();

            if (!roleDefinitionRepository.existsByCodeAndTenantId(roleEnum.name(), tenantId)) {
                Set<Permission> assignedPerms = new HashSet<>();
                for (String pattern : roleSeed.permissionPatterns) {
                    if ("*".equals(pattern)) {
                        assignedPerms.addAll(allTenantPerms);
                    } else if (pattern.endsWith(":")) {
                        allTenantPerms.stream()
                                .filter(p -> p.getCode().startsWith(pattern))
                                .forEach(assignedPerms::add);
                    } else if (permByCode.containsKey(pattern)) {
                        assignedPerms.add(permByCode.get(pattern));
                    }
                }

                RoleDefinition roleDef = RoleDefinition.builder()
                        .name(roleSeed.name)
                        .code(roleEnum.name())
                        .description(roleSeed.description)
                        .active(true)
                        .systemRole(true)
                        .permissions(assignedPerms)
                        .build();
                roleDef.setTenantId(tenantId);
                roleDefinitionRepository.save(roleDef);
                log.info("Bootstrapped system role [{}] for tenant [{}] with {} permissions",
                        roleEnum.name(), tenantId, assignedPerms.size());
            }
        }
    }

    private record PermissionSeed(String code, String name, String description, String module) {}
    private record RoleSeed(String name, String description, List<String> permissionPatterns) {}
}
