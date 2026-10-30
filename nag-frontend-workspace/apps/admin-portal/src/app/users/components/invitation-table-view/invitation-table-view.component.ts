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
  AdminInvitationItem,
  RoleDefinition,
  formatRoleDisplayName,
  normalizeRoleKey,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-invitation-table-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './invitation-table-view.component.html',
  styleUrl: './invitation-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationTableViewComponent {
  readonly invitations = input<AdminInvitationItem[]>([]);
  readonly roles = input<RoleDefinition[]>([]);
  readonly loading = input<boolean>(false);

  readonly inviteNew = output<void>();
  readonly resendInvite = output<AdminInvitationItem>();
  readonly revokeInvite = output<AdminInvitationItem>();
  readonly bulkResend = output<string[]>();
  readonly bulkRevoke = output<string[]>();

  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions: number[] = [10, 20, 50];
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly totalPages = computed(() => {
    const list = this.invitations() || [];
    return Math.ceil(list.length / this.pageSize()) || 1;
  });

  readonly safePageIndex = computed(() => {
    const total = this.totalPages();
    const current = this.pageIndex();
    return current >= total ? Math.max(0, total - 1) : current;
  });

  readonly paginatedInvitations = computed(() => {
    const list = this.invitations() || [];
    const page = this.safePageIndex();
    const size = this.pageSize();
    return list.slice(page * size, (page + 1) * size);
  });

  readonly displayStart = computed(() => {
    const list = this.invitations() || [];
    if (list.length === 0) return 0;
    return this.safePageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const list = this.invitations() || [];
    return Math.min((this.safePageIndex() + 1) * this.pageSize(), list.length);
  });

  readonly isAllSelected = computed(() => {
    const pageItems = this.paginatedInvitations();
    if (pageItems.length === 0) return false;
    const sel = this.selectedIds();
    return pageItems.every((i) => sel.has(i.id));
  });

  readonly isPartiallySelected = computed(() => {
    const pageItems = this.paginatedInvitations();
    if (pageItems.length === 0) return false;
    const sel = this.selectedIds();
    const count = pageItems.filter((i) => sel.has(i.id)).length;
    return count > 0 && count < pageItems.length;
  });

  readonly selectedCount = computed(() => this.selectedIds().size);

  getRoleName(roleKey: string): string {
    const norm = normalizeRoleKey(roleKey);
    const r = this.roles().find((item) => normalizeRoleKey(item.name) === norm);
    return r ? (r.displayName || r.name) : formatRoleDisplayName(roleKey);
  }

  copyToken(token?: string): void {
    if (!token) return;
    navigator.clipboard?.writeText(token);
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
    const pageItems = this.paginatedInvitations();
    if (checked) {
      pageItems.forEach((i) => current.add(i.id));
    } else {
      pageItems.forEach((i) => current.delete(i.id));
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

  onBulkResend(): void {
    const pendingIds = this.invitations()
      .filter((i) => this.selectedIds().has(i.id) && i.status === 'PENDING')
      .map((i) => i.id);
    if (pendingIds.length === 0) return;
    this.bulkResend.emit(pendingIds);
    this.deselectAll();
  }

  onBulkRevoke(): void {
    const pendingIds = this.invitations()
      .filter((i) => this.selectedIds().has(i.id) && i.status === 'PENDING')
      .map((i) => i.id);
    if (pendingIds.length === 0) return;
    this.bulkRevoke.emit(pendingIds);
    this.deselectAll();
  }
}
