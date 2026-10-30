import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
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
  readonly bulkResetMfa = output<string[]>();
  readonly bulkRevoke = output<string[]>();
  readonly bulkReactivate = output<string[]>();

  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions: number[] = [10, 20, 50];
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly filterRoles = computed<RoleDefinition[]>(() => {
    return resolveAvailableRoles(this.roles(), this.users());
  });

  readonly totalPages = computed(() => {
    const list = this.users() || [];
    return Math.ceil(list.length / this.pageSize()) || 1;
  });

  readonly safePageIndex = computed(() => {
    const total = this.totalPages();
    const current = this.pageIndex();
    return current >= total ? Math.max(0, total - 1) : current;
  });

  readonly paginatedUsers = computed(() => {
    const list = this.users() || [];
    const page = this.safePageIndex();
    const size = this.pageSize();
    return list.slice(page * size, (page + 1) * size);
  });

  readonly displayStart = computed(() => {
    const list = this.users() || [];
    if (list.length === 0) return 0;
    return this.safePageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const list = this.users() || [];
    return Math.min((this.safePageIndex() + 1) * this.pageSize(), list.length);
  });

  readonly isAllSelected = computed(() => {
    const pageUsers = this.paginatedUsers();
    if (pageUsers.length === 0) return false;
    const sel = this.selectedIds();
    return pageUsers.every((u) => sel.has(u.id));
  });

  readonly isPartiallySelected = computed(() => {
    const pageUsers = this.paginatedUsers();
    if (pageUsers.length === 0) return false;
    const sel = this.selectedIds();
    const count = pageUsers.filter((u) => sel.has(u.id)).length;
    return count > 0 && count < pageUsers.length;
  });

  readonly selectedCount = computed(() => this.selectedIds().size);

  getRoleName(roleKey: string): string {
    const norm = normalizeRoleKey(roleKey);
    const list = this.filterRoles();
    const r = list.find((item) => normalizeRoleKey(item.name) === norm);
    return r ? (r.displayName || r.name) : formatRoleDisplayName(roleKey);
  }

  onPageSizeChange(newSize: number): void {
    this.pageSize.set(newSize);
    this.pageIndex.set(0);
  }

  goToFirstPage(): void {
    this.pageIndex.set(0);
  }

  goToPrevPage(): void {
    if (this.safePageIndex() > 0) {
      this.pageIndex.set(this.safePageIndex() - 1);
    }
  }

  goToNextPage(): void {
    if (this.safePageIndex() < this.totalPages() - 1) {
      this.pageIndex.set(this.safePageIndex() + 1);
    }
  }

  goToLastPage(): void {
    this.pageIndex.set(Math.max(0, this.totalPages() - 1));
  }

  toggleSelectAll(checked: boolean): void {
    const current = new Set(this.selectedIds());
    const pageUsers = this.paginatedUsers();
    if (checked) {
      pageUsers.forEach((u) => current.add(u.id));
    } else {
      pageUsers.forEach((u) => current.delete(u.id));
    }
    this.selectedIds.set(current);
  }

  toggleSelect(id: string): void {
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkResetMfa(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.bulkResetMfa.emit(ids);
    this.deselectAll();
  }

  onBulkRevoke(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.bulkRevoke.emit(ids);
    this.deselectAll();
  }

  onBulkReactivate(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.bulkReactivate.emit(ids);
    this.deselectAll();
  }
}
