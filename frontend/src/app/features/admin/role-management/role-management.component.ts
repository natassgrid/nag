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

import { Component, OnInit, ViewChild, signal, computed, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { map } from 'rxjs/operators';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  PaginatedResponse,
  ColumnDef,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { NotificationService } from '../../../core/services/notification.service';
import {
  AdminService,
  RoleDefinitionResponse,
  PermissionResponse,
  CreateRoleRequest,
  UpdateRoleRequest
} from '../services/admin.service';

export interface ModulePermissionGroup {
  module: string;
  moduleLabel: string;
  moduleIcon: string;
  permissions: PermissionResponse[];
}

export const FALLBACK_PERMISSIONS: PermissionResponse[] = [
  // IDENTITY Module
  { id: '018f4e2b-0001-7000-8000-000000000001', code: 'IDENTITY:USER_READ', name: 'View Users', description: 'View user accounts, statuses, and profiles', module: 'IDENTITY' },
  { id: '018f4e2b-0001-7000-8000-000000000002', code: 'IDENTITY:USER_WRITE', name: 'Manage Users', description: 'Create, update, and deactivate user accounts', module: 'IDENTITY' },
  { id: '018f4e2b-0001-7000-8000-000000000003', code: 'IDENTITY:ROLE_READ', name: 'View Roles', description: 'View role definitions, details, and permission mappings', module: 'IDENTITY' },
  { id: '018f4e2b-0001-7000-8000-000000000004', code: 'IDENTITY:ROLE_MANAGE', name: 'Manage Roles', description: 'Create, update, and delete custom roles and assign permissions', module: 'IDENTITY' },
  { id: '018f4e2b-0001-7000-8000-000000000005', code: 'IDENTITY:USER_ROLE_ASSIGN', name: 'Assign Roles', description: 'Assign and revoke roles for platform users', module: 'IDENTITY' },
  { id: '018f4e2b-0001-7000-8000-000000000006', code: 'IDENTITY:MFA_MANAGE', name: 'Manage MFA', description: 'Configure and reset Multi-Factor Authentication for users', module: 'IDENTITY' },

  // QUESTION_BANK Module
  { id: '018f4e2b-0002-7000-8000-000000000001', code: 'QUESTION:READ', name: 'View Questions', description: 'Browse, search, and inspect question items', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000002', code: 'QUESTION:CREATE', name: 'Create Questions', description: 'Author and draft new questions in question bank', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000003', code: 'QUESTION:EDIT', name: 'Edit Questions', description: 'Modify drafted questions, choices, and metadata', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000004', code: 'QUESTION:DELETE', name: 'Delete Questions', description: 'Remove questions from the question bank', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000005', code: 'QUESTION:REVIEW', name: 'Review Questions', description: 'Perform peer review, rubric check, and question validation', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000006', code: 'QUESTION:APPROVE', name: 'Approve Questions', description: 'Approve reviewed questions for publication in blueprints', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000007', code: 'QUESTION:TRANSLATE', name: 'Translate Questions', description: 'Translate questions and options into regional languages', module: 'QUESTION_BANK' },
  { id: '018f4e2b-0002-7000-8000-000000000008', code: 'QUESTION:IMPORT_EXPORT', name: 'Import/Export Questions', description: 'Bulk import and export question banks (CSV, JSON, QTI)', module: 'QUESTION_BANK' },

  // EXAM_MANAGEMENT Module
  { id: '018f4e2b-0003-7000-8000-000000000001', code: 'EXAM:READ', name: 'View Examinations', description: 'Browse and view exam blueprints and schedules', module: 'EXAM_MANAGEMENT' },
  { id: '018f4e2b-0003-7000-8000-000000000002', code: 'EXAM:CREATE', name: 'Create Examinations', description: 'Define new examination blueprints and paper templates', module: 'EXAM_MANAGEMENT' },
  { id: '018f4e2b-0003-7000-8000-000000000003', code: 'EXAM:SCHEDULE', name: 'Schedule Examinations', description: 'Configure time slots, test centers, and candidate allocations', module: 'EXAM_MANAGEMENT' },
  { id: '018f4e2b-0003-7000-8000-000000000004', code: 'EXAM:PUBLISH', name: 'Publish Examinations', description: 'Publish exam schedules for candidate registration', module: 'EXAM_MANAGEMENT' },
  { id: '018f4e2b-0003-7000-8000-000000000005', code: 'EXAM:CANCEL', name: 'Cancel Examinations', description: 'Cancel scheduled examination sessions and notify candidates', module: 'EXAM_MANAGEMENT' },

  // EXAM_DELIVERY Module
  { id: '018f4e2b-0004-7000-8000-000000000001', code: 'DELIVERY:MONITOR', name: 'Live Monitoring', description: 'Monitor live exam sessions, heartbeats, and delivery status', module: 'EXAM_DELIVERY' },
  { id: '018f4e2b-0004-7000-8000-000000000002', code: 'DELIVERY:ATTEND', name: 'Attend Exam', description: 'Launch candidate exam interface and submit responses', module: 'EXAM_DELIVERY' },
  { id: '018f4e2b-0004-7000-8000-000000000003', code: 'DELIVERY:PROCTOR', name: 'Proctor Exam', description: 'Live proctoring oversight, anomaly flagging, and session controls', module: 'EXAM_DELIVERY' },
  { id: '018f4e2b-0004-7000-8000-000000000004', code: 'DELIVERY:RETEST', name: 'Authorize Retest', description: 'Issue retest authorization for affected candidates', module: 'EXAM_DELIVERY' },

  // ASSESSMENT_EVALUATION Module
  { id: '018f4e2b-0005-7000-8000-000000000001', code: 'EVALUATION:READ', name: 'View Evaluation', description: 'Access candidate response sheets and answer keys', module: 'ASSESSMENT_EVALUATION' },
  { id: '018f4e2b-0005-7000-8000-000000000002', code: 'EVALUATION:SCORE_AUTO', name: 'Execute Auto-Grading', description: 'Run automated grading pipeline on objective responses', module: 'ASSESSMENT_EVALUATION' },
  { id: '018f4e2b-0005-7000-8000-000000000003', code: 'EVALUATION:SCORE_MANUAL', name: 'Manual Scoring', description: 'Evaluate subjective and descriptive candidate responses', module: 'ASSESSMENT_EVALUATION' },
  { id: '018f4e2b-0005-7000-8000-000000000004', code: 'EVALUATION:RESULT_PUBLISH', name: 'Publish Results', description: 'Approve final scorecards and publish merit lists', module: 'ASSESSMENT_EVALUATION' },

  // AUDIT_SECURITY Module
  { id: '018f4e2b-0006-7000-8000-000000000001', code: 'AUDIT:READ', name: 'View Audit Logs', description: 'Inspect immutable audit trails and system event logs', module: 'AUDIT_SECURITY' },
  { id: '018f4e2b-0006-7000-8000-000000000002', code: 'AUDIT:EXPORT', name: 'Export Audit Logs', description: 'Export compliance and security audit logs', module: 'AUDIT_SECURITY' },
  { id: '018f4e2b-0006-7000-8000-000000000003', code: 'SECURITY:POLICY_MANAGE', name: 'Manage Security Policies', description: 'Configure password rules, IP restrictions, and session parameters', module: 'AUDIT_SECURITY' },

  // ANALYTICS_REPORTS Module
  { id: '018f4e2b-0007-7000-8000-000000000001', code: 'ANALYTICS:VIEW', name: 'View Analytics', description: 'Access platform metrics, exam statistics, and item analysis', module: 'ANALYTICS_REPORTS' },
  { id: '018f4e2b-0007-7000-8000-000000000002', code: 'REPORTS:EXPORT', name: 'Export Reports', description: 'Generate and download administrative reports and insights', module: 'ANALYTICS_REPORTS' }
];

@Component({
  selector: 'app-role-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatTabsModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatDividerModule,
    MatBadgeModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    RightDrawerComponent,
    StatusBadgeComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './role-management.component.html',
  styleUrls: ['./role-management.component.scss']
})
export class RoleManagementComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<RoleDefinitionResponse>;
  @ViewChild('statusTmpl', { static: true }) statusTmpl!: any;
  @ViewChild('permsTmpl', { static: true }) permsTmpl!: any;

  @ViewChild('permTable') permTable?: PaginatedTableComponent<PermissionResponse>;
  @ViewChild('permModuleTmpl', { static: true }) permModuleTmpl!: any;

  columns: ColumnDef<RoleDefinitionResponse>[] = [];
  permColumns: ColumnDef<PermissionResponse>[] = [];

  // Active Tab Index (0 = Roles, 1 = Permissions, 2 = Matrix)
  readonly activeTab = signal<number>(0);

  // All available permissions & roles
  readonly allPermissions = signal<PermissionResponse[]>(FALLBACK_PERMISSIONS);
  readonly allRoles = signal<RoleDefinitionResponse[]>([]);
  readonly permSearchQuery = signal<string>('');

  // Signals for Local State
  readonly filters = signal<Record<string, any>>({});
  readonly createDrawerOpen = signal<boolean>(false);
  readonly editDrawerOpen = signal<boolean>(false);
  readonly viewDrawerOpen = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Form Signals - Create
  readonly newName = signal<string>('');
  readonly newCode = signal<string>('');
  readonly newDescription = signal<string>('');
  readonly newSelectedPermIds = signal<Set<string>>(new Set());

  // Form Signals - Edit
  readonly editingRole = signal<RoleDefinitionResponse | null>(null);
  readonly editName = signal<string>('');
  readonly editDescription = signal<string>('');
  readonly editActive = signal<boolean>(true);
  readonly editSelectedPermIds = signal<Set<string>>(new Set());

  // Form Signals - View
  readonly viewingRole = signal<RoleDefinitionResponse | null>(null);

  // Validation
  readonly isCreateValid = computed(() => {
    return this.newName().trim().length > 0 && this.newCode().trim().length > 0;
  });

  // Grouped Permissions computed for Drawer
  readonly groupedPermissions = computed<ModulePermissionGroup[]>(() => {
    const search = this.permSearchQuery().toLowerCase().trim();
    const perms = this.allPermissions().filter(p =>
      !search ||
      p.name.toLowerCase().includes(search) ||
      p.code.toLowerCase().includes(search) ||
      p.module.toLowerCase().includes(search) ||
      (p.description && p.description.toLowerCase().includes(search))
    );

    const map = new Map<string, PermissionResponse[]>();
    for (const p of perms) {
      const group = map.get(p.module) || [];
      group.push(p);
      map.set(p.module, group);
    }

    const moduleOrder = [
      'IDENTITY',
      'QUESTION_BANK',
      'EXAM_MANAGEMENT',
      'EXAM_DELIVERY',
      'ASSESSMENT_EVALUATION',
      'AUDIT_SECURITY',
      'ANALYTICS_REPORTS'
    ];

    const groups: ModulePermissionGroup[] = [];
    map.forEach((items, mod) => {
      groups.push({
        module: mod,
        moduleLabel: this.formatModuleLabel(mod),
        moduleIcon: this.getModuleIcon(mod),
        permissions: items
      });
    });

    return groups.sort((a, b) => {
      const idxA = moduleOrder.indexOf(a.module);
      const idxB = moduleOrder.indexOf(b.module);
      if (idxA !== -1 && idxB !== -1) return idxA - idxB;
      return a.moduleLabel.localeCompare(b.moduleLabel);
    });
  });

  filterCategories: FilterCategory[] = [
    {
      key: 'active',
      label: 'Status',
      expanded: true,
      options: [
        { label: 'Active', value: 'true' },
        { label: 'Inactive', value: 'false' }
      ]
    },
    {
      key: 'systemRole',
      label: 'Type',
      expanded: true,
      options: [
        { label: 'System Role', value: 'true' },
        { label: 'Custom Role', value: 'false' }
      ]
    }
  ];

  permFilterCategories: FilterCategory[] = [
    {
      key: 'module',
      label: 'Module',
      expanded: true,
      options: [
        { label: 'Identity & Access', value: 'IDENTITY' },
        { label: 'Question Bank', value: 'QUESTION_BANK' },
        { label: 'Exam Management', value: 'EXAM_MANAGEMENT' },
        { label: 'Exam Delivery', value: 'EXAM_DELIVERY' },
        { label: 'Evaluation', value: 'ASSESSMENT_EVALUATION' },
        { label: 'Audit & Security', value: 'AUDIT_SECURITY' },
        { label: 'Analytics & Reports', value: 'ANALYTICS_REPORTS' }
      ]
    }
  ];

  fetcher: PaginatedDataFetcher<RoleDefinitionResponse> = (req) => {
    return this.adminService.getRoleDefinitions(req.page, req.size, req.search || '').pipe(
      map(page => {
        if (req.page === 0) {
          this.allRoles.set(page.content);
        }
        return {
          content: page.content,
          totalElements: page.totalElements,
          totalPages: page.totalPages,
          size: page.size,
          number: page.number
        } as PaginatedResponse<RoleDefinitionResponse>;
      })
    );
  };

  permFetcher: PaginatedDataFetcher<PermissionResponse> = (req) => {
    return this.adminService.getPermissions(req.page, req.size, req.search || '').pipe(
      map(page => {
        if (page && page.content && page.content.length > 0) {
          return {
            content: page.content,
            totalElements: page.totalElements,
            totalPages: page.totalPages,
            size: page.size,
            number: page.number
          } as PaginatedResponse<PermissionResponse>;
        }
        // Fallback filtering if backend permissions table is empty
        const search = (req.search || '').toLowerCase();
        const filtered = FALLBACK_PERMISSIONS.filter(p =>
          !search ||
          p.name.toLowerCase().includes(search) ||
          p.code.toLowerCase().includes(search) ||
          p.module.toLowerCase().includes(search)
        );
        return {
          content: filtered,
          totalElements: filtered.length,
          totalPages: 1,
          size: req.size,
          number: 0
        } as PaginatedResponse<PermissionResponse>;
      })
    );
  };

  constructor(
    private adminService: AdminService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'name', header: 'Role Name', sortable: true },
      { key: 'code', header: 'Code', sortable: true },
      { key: 'description', header: 'Description', cell: (r) => r.description || '—' },
      { key: 'active', header: 'Status', type: 'custom', template: this.statusTmpl },
      {
        key: 'systemRole',
        header: 'Type',
        cell: (r) => r.systemRole ? 'System' : 'Custom',
        type: 'badge',
        chipClass: (val) => val === 'System' ? 'system-type' : 'custom-type'
      },
      { key: 'permissions', header: 'Permissions', type: 'custom', template: this.permsTmpl },
      { key: 'actions', header: '', type: 'actions' }
    ];

    this.permColumns = [
      { key: 'name', header: 'Permission Name', sortable: true },
      { key: 'code', header: 'Permission Code', sortable: true },
      { key: 'module', header: 'Module', type: 'custom', template: this.permModuleTmpl },
      { key: 'description', header: 'Description', cell: (p) => p.description || '—' }
    ];

    this.loadAllPermissions();
  }

  loadAllPermissions(): void {
    this.adminService.getAllPermissions().subscribe({
      next: (perms) => {
        if (perms && perms.length > 0) {
          this.allPermissions.set(perms);
        } else {
          this.allPermissions.set(FALLBACK_PERMISSIONS);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.allPermissions.set(FALLBACK_PERMISSIONS);
        this.cdr.markForCheck();
      }
    });
  }

  formatModuleLabel(moduleCode: string): string {
    switch (moduleCode) {
      case 'IDENTITY': return 'Identity & Access';
      case 'QUESTION_BANK': return 'Question Bank';
      case 'EXAM_MANAGEMENT': return 'Exam Management';
      case 'EXAM_DELIVERY': return 'Exam Delivery';
      case 'ASSESSMENT_EVALUATION': return 'Assessment & Evaluation';
      case 'AUDIT_SECURITY': return 'Audit & Security';
      case 'ANALYTICS_REPORTS': return 'Analytics & Reports';
      default: return moduleCode.replace(/_/g, ' ');
    }
  }

  getModuleIcon(moduleCode: string): string {
    switch (moduleCode) {
      case 'IDENTITY': return 'badge';
      case 'QUESTION_BANK': return 'quiz';
      case 'EXAM_MANAGEMENT': return 'calendar_month';
      case 'EXAM_DELIVERY': return 'laptop_chromebook';
      case 'ASSESSMENT_EVALUATION': return 'fact_check';
      case 'AUDIT_SECURITY': return 'shield';
      case 'ANALYTICS_REPORTS': return 'analytics';
      default: return 'folder';
    }
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters.set({ ...filters });
  }

  // ── View Role ──

  openViewDrawer(role: RoleDefinitionResponse): void {
    this.viewingRole.set(role);
    this.viewDrawerOpen.set(true);
  }

  // ── Create Role ──

  openCreateDrawer(): void {
    this.newName.set('');
    this.newCode.set('');
    this.newDescription.set('');
    this.newSelectedPermIds.set(new Set());
    this.permSearchQuery.set('');
    this.createDrawerOpen.set(true);
  }

  selectAllCreatePerms(): void {
    const allIds = new Set(this.allPermissions().map(p => p.id));
    this.newSelectedPermIds.set(allIds);
  }

  clearAllCreatePerms(): void {
    this.newSelectedPermIds.set(new Set());
  }

  toggleCreatePerm(permId: string): void {
    const current = new Set(this.newSelectedPermIds());
    if (current.has(permId)) {
      current.delete(permId);
    } else {
      current.add(permId);
    }
    this.newSelectedPermIds.set(current);
  }

  toggleCreateGroup(group: ModulePermissionGroup): void {
    const current = new Set(this.newSelectedPermIds());
    const allSelected = group.permissions.every(p => current.has(p.id));

    if (allSelected) {
      group.permissions.forEach(p => current.delete(p.id));
    } else {
      group.permissions.forEach(p => current.add(p.id));
    }
    this.newSelectedPermIds.set(current);
  }

  isCreateGroupChecked(group: ModulePermissionGroup): boolean {
    const current = this.newSelectedPermIds();
    return group.permissions.length > 0 && group.permissions.every(p => current.has(p.id));
  }

  isCreateGroupIndeterminate(group: ModulePermissionGroup): boolean {
    const current = this.newSelectedPermIds();
    const count = group.permissions.filter(p => current.has(p.id)).length;
    return count > 0 && count < group.permissions.length;
  }

  getCreateGroupSelectedCount(group: ModulePermissionGroup): number {
    const current = this.newSelectedPermIds();
    return group.permissions.filter(p => current.has(p.id)).length;
  }

  saveNewRole(): void {
    if (!this.isCreateValid()) return;
    this.saving.set(true);
    const request: CreateRoleRequest = {
      name: this.newName().trim(),
      code: this.newCode().trim().toUpperCase(),
      description: this.newDescription().trim() || undefined,
      permissionIds: Array.from(this.newSelectedPermIds())
    };

    this.adminService.createRoleDefinition(request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Role created successfully');
        this.createDrawerOpen.set(false);
        this.saving.set(false);
        this.paginatedTable.reload();
        this.cdr.markForCheck();
      },
      error: () => {
        this.saving.set(false);
        this.cdr.markForCheck();
      }
    });
  }

  // ── Edit Role ──

  openEditDrawer(role: RoleDefinitionResponse): void {
    this.editingRole.set(role);
    this.editName.set(role.name);
    this.editDescription.set(role.description || '');
    this.editActive.set(role.active);
    const permIds = new Set((role.permissions || []).map(p => p.id));
    this.editSelectedPermIds.set(permIds);
    this.permSearchQuery.set('');
    this.editDrawerOpen.set(true);
  }

  selectAllEditPerms(): void {
    const allIds = new Set(this.allPermissions().map(p => p.id));
    this.editSelectedPermIds.set(allIds);
  }

  clearAllEditPerms(): void {
    this.editSelectedPermIds.set(new Set());
  }

  toggleEditPerm(permId: string): void {
    const current = new Set(this.editSelectedPermIds());
    if (current.has(permId)) {
      current.delete(permId);
    } else {
      current.add(permId);
    }
    this.editSelectedPermIds.set(current);
  }

  toggleEditGroup(group: ModulePermissionGroup): void {
    const current = new Set(this.editSelectedPermIds());
    const allSelected = group.permissions.every(p => current.has(p.id));

    if (allSelected) {
      group.permissions.forEach(p => current.delete(p.id));
    } else {
      group.permissions.forEach(p => current.add(p.id));
    }
    this.editSelectedPermIds.set(current);
  }

  isEditGroupChecked(group: ModulePermissionGroup): boolean {
    const current = this.editSelectedPermIds();
    return group.permissions.length > 0 && group.permissions.every(p => current.has(p.id));
  }

  isEditGroupIndeterminate(group: ModulePermissionGroup): boolean {
    const current = this.editSelectedPermIds();
    const count = group.permissions.filter(p => current.has(p.id)).length;
    return count > 0 && count < group.permissions.length;
  }

  getEditGroupSelectedCount(group: ModulePermissionGroup): number {
    const current = this.editSelectedPermIds();
    return group.permissions.filter(p => current.has(p.id)).length;
  }

  saveEditRole(): void {
    const role = this.editingRole();
    if (!role) return;
    this.saving.set(true);

    const request: UpdateRoleRequest = {
      name: this.editName().trim(),
      description: this.editDescription().trim(),
      active: this.editActive(),
      permissionIds: Array.from(this.editSelectedPermIds())
    };

    this.adminService.updateRoleDefinition(role.id, request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Role updated successfully');
        this.editDrawerOpen.set(false);
        this.saving.set(false);
        this.paginatedTable.reload();
        this.cdr.markForCheck();
      },
      error: () => {
        this.saving.set(false);
        this.cdr.markForCheck();
      }
    });
  }

  // ── Toggle Active ──

  toggleActive(role: RoleDefinitionResponse): void {
    const newStatus = !role.active;
    this.adminService.updateRoleDefinition(role.id, { active: newStatus }).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Role ${newStatus ? 'activated' : 'deactivated'}`);
        this.paginatedTable.reload();
      },
      error: () => {}
    });
  }

  // ── Delete Role ──

  deleteRole(role: RoleDefinitionResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Role',
        message: `Are you sure you want to delete the role "${role.name}" (${role.code})? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        color: 'warn',
        icon: 'warning'
      } as ConfirmDialogData
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;

      this.adminService.deleteRoleDefinition(role.id).subscribe({
        next: () => {
          this.notificationService.showSuccess('Role deleted successfully');
          this.paginatedTable.reload();
        },
        error: () => {}
      });
    });
  }

  // ── Matrix Helpers ──

  hasPermission(role: RoleDefinitionResponse, permCode: string): boolean {
    return !!role.permissions?.some(p => p.code === permCode);
  }
}
