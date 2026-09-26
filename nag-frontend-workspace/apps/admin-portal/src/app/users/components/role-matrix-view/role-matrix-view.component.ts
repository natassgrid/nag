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
  RoleDefinition,
  PermissionDefinition,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-role-matrix-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './role-matrix-view.component.html',
  styleUrl: './role-matrix-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleMatrixViewComponent {
  readonly roles = input<RoleDefinition[]>([]);
  readonly permissions = input<PermissionDefinition[]>([]);
  readonly loading = input<boolean>(false);

  readonly createRole = output<void>();
  readonly editRole = output<RoleDefinition>();
  readonly deleteRole = output<RoleDefinition>();

  selectedCategory = signal<string>('ALL');

  readonly categories = computed(() => {
    const list = this.permissions();
    const cats = new Set<string>();
    list.forEach((p) => cats.add(p.category));
    return Array.from(cats);
  });

  readonly filteredPermissions = computed(() => {
    const cat = this.selectedCategory();
    if (cat === 'ALL') return this.permissions();
    return this.permissions().filter((p) => p.category === cat);
  });

  hasPermission(role: RoleDefinition, permName: string): boolean {
    if (role.permissions.includes('ALL_PERMISSIONS')) return true;
    return role.permissions.includes(permName);
  }
}
