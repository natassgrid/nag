import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminUserAccount,
  RoleDefinition,
  resolveAvailableRoles,
  formatRoleDisplayName,
  normalizeRoleKey,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-user-table-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './user-table-view.component.html',
  styleUrl: './user-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserTableViewComponent {
  readonly users = input<AdminUserAccount[]>([]);
  readonly roles = input<RoleDefinition[]>([]);
  readonly searchQuery = input<string>('');
  readonly selectedRole = input<string>('ALL');
  readonly loading = input<boolean>(false);

  readonly searchQueryChange = output<string>();
  readonly roleFilterChange = output<string>();
  readonly toggleStatus = output<AdminUserAccount>();
  readonly inviteUser = output<void>();
  readonly resetMfa = output<AdminUserAccount>();

  readonly filterRoles = computed<RoleDefinition[]>(() => {
    return resolveAvailableRoles(this.roles(), this.users());
  });

  getRoleName(roleKey: string): string {
    const norm = normalizeRoleKey(roleKey);
    const list = this.filterRoles();
    const r = list.find((item) => normalizeRoleKey(item.name) === norm);
    return r ? (r.displayName || r.name) : formatRoleDisplayName(roleKey);
  }
}
