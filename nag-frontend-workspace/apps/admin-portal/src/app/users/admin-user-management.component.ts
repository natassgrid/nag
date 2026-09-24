import {
  Component,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  SearchInputComponent,
  StatusBadgeComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string;
  lastLogin: string;
  status: 'ACTIVE' | 'REVOKED';
}

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    SearchInputComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './admin-user-management.component.html',
  styleUrl: './admin-user-management.component.scss',
})
export class AdminUserManagementComponent {
  searchQuery = signal<string>('');
  selectedRole = signal<string>('ALL');
  isInviteModalOpen = signal<boolean>(false);

  newUserName = '';
  newUserEmail = '';
  newUserRole = 'QUESTION_AUTHOR';

  users = signal<AdminUser[]>([
    {
      id: 'u-1',
      name: 'Dr. Amitabh Verma',
      email: 'exam_controller@nag.gov.in',
      role: 'EXAM_CONTROLLER',
      lastLogin: '2026-09-24 17:40:02',
      status: 'ACTIVE',
    },
    {
      id: 'u-2',
      name: 'Prof. Sunita Sharma',
      email: 'author_sharma@nag.gov.in',
      role: 'QUESTION_AUTHOR',
      lastLogin: '2026-09-24 16:15:30',
      status: 'ACTIVE',
    },
    {
      id: 'u-3',
      name: 'Dr. K. S. Reddy',
      email: 'evaluator_reddy@nag.gov.in',
      role: 'EVALUATOR',
      lastLogin: '2026-09-24 15:45:12',
      status: 'ACTIVE',
    },
    {
      id: 'u-4',
      name: 'Priya Sundaram',
      email: 'auditor_sundaram@nag.gov.in',
      role: 'AUDITOR',
      lastLogin: '2026-09-23 11:20:00',
      status: 'ACTIVE',
    },
    {
      id: 'u-5',
      name: 'System Root Admin',
      email: 'admin_root@nag.gov.in',
      role: 'SUPER_ADMIN',
      lastLogin: '2026-09-24 17:50:00',
      status: 'ACTIVE',
    },
  ]);

  filteredUsers = () => {
    const q = this.searchQuery().toLowerCase().trim();
    const role = this.selectedRole();
    return this.users().filter((user) => {
      const matchRole = role === 'ALL' || user.role === role;
      const matchQ =
        !q ||
        user.name.toLowerCase().includes(q) ||
        user.email.toLowerCase().includes(q) ||
        user.role.toLowerCase().includes(q);
      return matchRole && matchQ;
    });
  };

  openInviteModal(): void {
    this.newUserName = '';
    this.newUserEmail = '';
    this.newUserRole = 'QUESTION_AUTHOR';
    this.isInviteModalOpen.set(true);
  }

  submitInvite(): void {
    if (!this.newUserName.trim() || !this.newUserEmail.trim()) return;

    const newUser: AdminUser = {
      id: `u-${Date.now()}`,
      name: this.newUserName.trim(),
      email: this.newUserEmail.trim(),
      role: this.newUserRole,
      lastLogin: 'Pending 2FA Setup',
      status: 'ACTIVE',
    };

    this.users.update((list) => [newUser, ...list]);
    this.isInviteModalOpen.set(false);
    alert(`Invitation dispatched to ${newUser.email} with TOTP registration key!`);
  }

  toggleUserStatus(user: AdminUser): void {
    this.users.update((list) =>
      list.map((u) =>
        u.id === user.id
          ? { ...u, status: u.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE' }
          : u
      )
    );
  }
}
