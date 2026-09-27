import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  RoleDefinition,
  AdminInvitePayload,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-user-invite-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './user-invite-drawer.component.html',
  styleUrl: './user-invite-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserInviteDrawerComponent {
  readonly isOpen = input<boolean>(false);
  readonly isSubmitting = input<boolean>(false);
  readonly roles = input<RoleDefinition[]>([]);

  readonly closeDrawer = output<void>();
  readonly submitInvite = output<AdminInvitePayload>();

  formFullName = signal<string>('');
  formEmail = signal<string>('');
  formSelectedRoles = signal<string[]>(['QUESTION_AUTHOR']);
  formExpiryDays = signal<number>(5);

  toggleRole(roleName: string): void {
    const current = this.formSelectedRoles();
    if (current.includes(roleName)) {
      if (current.length > 1) {
        this.formSelectedRoles.set(current.filter((r) => r !== roleName));
      }
    } else {
      this.formSelectedRoles.set([...current, roleName]);
    }
  }

  isRoleSelected(roleName: string): boolean {
    return this.formSelectedRoles().includes(roleName);
  }

  onSubmit(): void {
    if (!this.formFullName().trim() || !this.formEmail().trim()) return;

    this.submitInvite.emit({
      fullName: this.formFullName().trim(),
      email: this.formEmail().trim(),
      assignedRoles: this.formSelectedRoles(),
      expiryDays: this.formExpiryDays(),
    });
  }

  reset(): void {
    this.formFullName.set('');
    this.formEmail.set('');
    this.formSelectedRoles.set(['QUESTION_AUTHOR']);
    this.formExpiryDays.set(5);
  }
}
