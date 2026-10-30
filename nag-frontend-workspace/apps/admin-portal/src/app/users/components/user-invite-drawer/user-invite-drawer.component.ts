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

  fieldErrors = signal<Record<string, string>>({});
  hasSubmitted = signal<boolean>(false);

  toggleRole(roleName: string): void {
    const current = this.formSelectedRoles();
    if (current.includes(roleName)) {
      this.formSelectedRoles.set(current.filter((r) => r !== roleName));
    } else {
      this.formSelectedRoles.set([...current, roleName]);
    }
    if (this.hasSubmitted() || this.fieldErrors()['roles']) {
      this.validateField('roles');
    }
  }

  isRoleSelected(roleName: string): boolean {
    return this.formSelectedRoles().includes(roleName);
  }

  onFullNameChange(val: string): void {
    this.formFullName.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['fullName']) {
      this.validateField('fullName');
    }
  }

  onEmailChange(val: string): void {
    this.formEmail.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['email']) {
      this.validateField('email');
    }
  }

  validateField(field: 'fullName' | 'email' | 'roles'): void {
    const errors = { ...this.fieldErrors() };
    if (field === 'fullName') {
      const name = this.formFullName().trim();
      if (!name) {
        errors['fullName'] = 'Full official name is required.';
      } else if (name.length < 2) {
        errors['fullName'] = 'Full official name must be at least 2 characters.';
      } else {
        delete errors['fullName'];
      }
    } else if (field === 'email') {
      const email = this.formEmail().trim();
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email) {
        errors['email'] = 'Official email address is required.';
      } else if (!emailRegex.test(email)) {
        errors['email'] = 'Please enter a valid official email address.';
      } else {
        delete errors['email'];
      }
    } else if (field === 'roles') {
      if (this.formSelectedRoles().length === 0) {
        errors['roles'] = 'At least one authority role must be selected.';
      } else {
        delete errors['roles'];
      }
    }
    this.fieldErrors.set(errors);
  }

  validateAll(): boolean {
    const errors: Record<string, string> = {};
    const name = this.formFullName().trim();
    if (!name) {
      errors['fullName'] = 'Full official name is required.';
    } else if (name.length < 2) {
      errors['fullName'] = 'Full official name must be at least 2 characters.';
    }

    const email = this.formEmail().trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email) {
      errors['email'] = 'Official email address is required.';
    } else if (!emailRegex.test(email)) {
      errors['email'] = 'Please enter a valid official email address.';
    }

    if (this.formSelectedRoles().length === 0) {
      errors['roles'] = 'At least one authority role must be selected.';
    }

    this.fieldErrors.set(errors);
    return Object.keys(errors).length === 0;
  }

  onSubmit(): void {
    this.hasSubmitted.set(true);
    if (!this.validateAll()) return;

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
    this.fieldErrors.set({});
    this.hasSubmitted.set(false);
  }
}
