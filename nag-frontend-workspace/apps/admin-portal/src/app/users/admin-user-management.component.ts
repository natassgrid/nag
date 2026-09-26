import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  UserRoleService,
  AdminUserAccount,
  RoleDefinition,
  PermissionDefinition,
  AdminInvitationItem,
  AdminInvitePayload,
  CreateRolePayload,
  UserManagementKpiStats,
} from '@nag-frontend-workspace/shared-data-access-auth';
import {
  UserKpiOverviewComponent,
  UserTableViewComponent,
  UserInviteDrawerComponent,
  RoleMatrixViewComponent,
  RoleCreateDrawerComponent,
  InvitationTableViewComponent,
} from './components';

export type UserManagementTab = 'USERS' | 'ROLES' | 'INVITATIONS';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    UserKpiOverviewComponent,
    UserTableViewComponent,
    UserInviteDrawerComponent,
    RoleMatrixViewComponent,
    RoleCreateDrawerComponent,
    InvitationTableViewComponent,
  ],
  templateUrl: './admin-user-management.component.html',
  styleUrl: './admin-user-management.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminUserManagementComponent implements OnInit {
  private readonly userRoleService = inject(UserRoleService);

  activeTab = signal<UserManagementTab>('USERS');
  loading = signal<boolean>(false);
  isSubmittingInvite = signal<boolean>(false);
  isSubmittingRole = signal<boolean>(false);

  searchQuery = signal<string>('');
  selectedRole = signal<string>('ALL');

  isInviteDrawerOpen = signal<boolean>(false);
  isRoleDrawerOpen = signal<boolean>(false);
  editingRole = signal<RoleDefinition | null>(null);

  users = signal<AdminUserAccount[]>([]);
  roles = signal<RoleDefinition[]>([]);
  permissions = signal<PermissionDefinition[]>([]);
  invitations = signal<AdminInvitationItem[]>([]);

  readonly kpiStats = computed<UserManagementKpiStats>(() => {
    const userList = this.users();
    const total = userList.length;
    const active = userList.filter((u) => u.status === 'ACTIVE').length;
    const mfaCount = userList.filter((u) => u.twoFactorEnabled).length;
    const mfaPercent = total > 0 ? Math.round((mfaCount / total) * 100) : 100;
    const pendingInv = this.invitations().filter((i) => i.status === 'PENDING').length;

    return {
      totalUsers: total,
      activeUsers: active,
      mfaEnforcedPercent: mfaPercent,
      totalRoles: this.roles().length,
      pendingInvitations: pendingInv,
    };
  });

  readonly filteredUsers = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const roleFilter = this.selectedRole();

    return this.users().filter((user) => {
      const matchRole =
        roleFilter === 'ALL' || user.roles.includes(roleFilter);
      const matchQ =
        !q ||
        user.fullName.toLowerCase().includes(q) ||
        user.email.toLowerCase().includes(q) ||
        (user.username && user.username.toLowerCase().includes(q)) ||
        user.roles.some((r) => r.toLowerCase().includes(q));

      return matchRole && matchQ;
    });
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.userRoleService.getUsers().subscribe({
      next: (data) => this.users.set(data),
      error: () => this.loading.set(false),
    });

    this.userRoleService.getRoles().subscribe({
      next: (data) => this.roles.set(data),
    });

    this.userRoleService.getPermissions().subscribe({
      next: (data) => this.permissions.set(data),
    });

    this.userRoleService.getInvitations().subscribe({
      next: (data) => {
        this.invitations.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openInviteDrawer(): void {
    this.isInviteDrawerOpen.set(true);
  }

  closeInviteDrawer(): void {
    this.isInviteDrawerOpen.set(false);
  }

  onSubmitInvite(payload: AdminInvitePayload): void {
    this.isSubmittingInvite.set(true);
    this.userRoleService.sendInvitation(payload).subscribe({
      next: (newInv) => {
        this.invitations.update((list) => [newInv, ...list]);
        this.isSubmittingInvite.set(false);
        this.closeInviteDrawer();
      },
      error: () => {
        this.isSubmittingInvite.set(false);
      },
    });
  }

  onToggleUserStatus(user: AdminUserAccount): void {
    this.userRoleService.toggleUserStatus(user.id).subscribe({
      next: (updated) => {
        this.users.update((list) =>
          list.map((u) => (u.id === user.id ? updated : u))
        );
      },
    });
  }

  onResetMfa(user: AdminUserAccount): void {
    this.userRoleService
      .updateUser(user.id, { twoFactorEnabled: false })
      .subscribe({
        next: (updated) => {
          this.users.update((list) =>
            list.map((u) => (u.id === user.id ? updated : u))
          );
        },
      });
  }

  openCreateRole(): void {
    this.editingRole.set(null);
    this.isRoleDrawerOpen.set(true);
  }

  openEditRole(role: RoleDefinition): void {
    this.editingRole.set(role);
    this.isRoleDrawerOpen.set(true);
  }

  closeRoleDrawer(): void {
    this.isRoleDrawerOpen.set(false);
    this.editingRole.set(null);
  }

  onSubmitRole(payload: CreateRolePayload): void {
    this.isSubmittingRole.set(true);
    const existing = this.editingRole();

    if (existing) {
      this.userRoleService
        .updateRole(existing.id, {
          displayName: payload.displayName,
          description: payload.description,
          permissions: payload.permissions,
        })
        .subscribe({
          next: (updated) => {
            this.roles.update((list) =>
              list.map((r) => (r.id === existing.id ? updated : r))
            );
            this.isSubmittingRole.set(false);
            this.closeRoleDrawer();
          },
          error: () => this.isSubmittingRole.set(false),
        });
    } else {
      this.userRoleService.createRole(payload).subscribe({
        next: (newRole) => {
          this.roles.update((list) => [...list, newRole]);
          this.isSubmittingRole.set(false);
          this.closeRoleDrawer();
        },
        error: () => this.isSubmittingRole.set(false),
      });
    }
  }

  onDeleteRole(role: RoleDefinition): void {
    if (role.systemRole) return;
    this.userRoleService.deleteRole(role.id).subscribe({
      next: () => {
        this.roles.update((list) => list.filter((r) => r.id !== role.id));
      },
    });
  }

  onResendInvite(inv: AdminInvitationItem): void {
    this.userRoleService
      .sendInvitation({
        email: inv.email,
        fullName: inv.fullName,
        assignedRoles: inv.assignedRoles,
      })
      .subscribe({
        next: (res) => {
          this.invitations.update((list) =>
            list.map((i) => (i.id === inv.id ? res : i))
          );
        },
      });
  }

  onRevokeInvite(inv: AdminInvitationItem): void {
    this.userRoleService.revokeInvitation(inv.id).subscribe({
      next: () => {
        this.invitations.update((list) =>
          list.map((i) => (i.id === inv.id ? { ...i, status: 'REVOKED' } : i))
        );
      },
    });
  }
}
