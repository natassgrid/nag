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
import {
  PermissionDefinition,
  CreateRolePayload,
  RoleDefinition,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-role-create-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './role-create-drawer.component.html',
  styleUrl: './role-create-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleCreateDrawerComponent {
  readonly isOpen = input<boolean>(false);
  readonly isSubmitting = input<boolean>(false);
  readonly permissions = input<PermissionDefinition[]>([]);
  readonly editingRole = input<RoleDefinition | null>(null);

  readonly closeDrawer = output<void>();
  readonly saveRole = output<CreateRolePayload>();

  formName = signal<string>('');
  formDisplayName = signal<string>('');
  formDescription = signal<string>('');
  formPermissions = signal<string[]>([]);

  readonly permissionsByCategory = computed(() => {
    const map = new Map<string, PermissionDefinition[]>();
    for (const p of this.permissions()) {
      if (!map.has(p.category)) {
        map.set(p.category, []);
      }
      map.get(p.category)!.push(p);
    }
    return Array.from(map.entries()).map(([category, items]) => ({
      category,
      items,
    }));
  });

  setFormFromRole(role: RoleDefinition | null): void {
    if (role) {
      this.formName.set(role.name);
      this.formDisplayName.set(role.displayName);
      this.formDescription.set(role.description);
      this.formPermissions.set([...role.permissions]);
    } else {
      this.formName.set('');
      this.formDisplayName.set('');
      this.formDescription.set('');
      this.formPermissions.set([]);
    }
  }

  togglePermission(permName: string): void {
    const cur = this.formPermissions();
    if (cur.includes(permName)) {
      this.formPermissions.set(cur.filter((p) => p !== permName));
    } else {
      this.formPermissions.set([...cur, permName]);
    }
  }

  isPermSelected(permName: string): boolean {
    return this.formPermissions().includes(permName);
  }

  toggleAllInCategory(categoryItems: PermissionDefinition[]): void {
    const itemNames = categoryItems.map((i) => i.name);
    const allSelected = itemNames.every((name) => this.isPermSelected(name));

    if (allSelected) {
      this.formPermissions.set(
        this.formPermissions().filter((p) => !itemNames.includes(p))
      );
    } else {
      const set = new Set([...this.formPermissions(), ...itemNames]);
      this.formPermissions.set(Array.from(set));
    }
  }

  onSubmit(): void {
    if (!this.formDisplayName().trim() || !this.formName().trim()) return;

    this.saveRole.emit({
      name: this.formName().trim(),
      displayName: this.formDisplayName().trim(),
      description: this.formDescription().trim(),
      permissions: this.formPermissions(),
    });
  }
}
