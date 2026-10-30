import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminUserProfile, UpdateProfilePayload } from '../../profile.model';

@Component({
  selector: 'nag-personal-info-card',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './personal-info-card.component.html',
  styleUrl: './personal-info-card.component.scss',
})
export class PersonalInfoCardComponent {
  readonly profile = input.required<AdminUserProfile>();
  readonly saving = input<boolean>(false);
  readonly save = output<UpdateProfilePayload>();

  // Editable Form Signals
  readonly fullName = signal<string>('');
  readonly phoneNumber = signal<string>('');
  readonly specialization = signal<string>('');
  readonly department = signal<string>('');
  readonly designation = signal<string>('');
  readonly avatarUrl = signal<string>('');
  readonly timezone = signal<string>('Asia/Kolkata');
  readonly dateFormat = signal<string>('DD/MM/YYYY');
  readonly timeFormat = signal<string>('24h');
  readonly preferredLanguage = signal<string>('en');
  readonly themePreference = signal<string>('system');

  readonly fieldErrors = signal<Record<string, string>>({});
  readonly hasSubmitted = signal<boolean>(false);

  constructor() {
    effect(
      () => {
        const p = this.profile();
        this.fullName.set(p.fullName || '');
        this.phoneNumber.set(p.phoneNumber || '');
        this.specialization.set(p.specialization || '');
        this.department.set(p.department || '');
        this.designation.set(p.designation || '');
        this.avatarUrl.set(p.avatarUrl || '');
        this.timezone.set(p.timezone || 'Asia/Kolkata');
        this.dateFormat.set(p.dateFormat || 'DD/MM/YYYY');
        this.timeFormat.set(p.timeFormat || '24h');
        this.preferredLanguage.set(p.preferredLanguage || 'en');
        this.themePreference.set(p.themePreference || 'system');
      },
      { allowSignalWrites: true }
    );
  }

  onFullNameChange(val: string): void {
    this.fullName.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['fullName']) {
      this.validateField('fullName');
    }
  }

  onPhoneNumberChange(val: string): void {
    this.phoneNumber.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['phoneNumber']) {
      this.validateField('phoneNumber');
    }
  }

  validateField(field: 'fullName' | 'phoneNumber'): void {
    const errors = { ...this.fieldErrors() };
    if (field === 'fullName') {
      const name = this.fullName().trim();
      if (!name) {
        errors['fullName'] = 'Full Name is required.';
      } else {
        delete errors['fullName'];
      }
    } else if (field === 'phoneNumber') {
      const phone = this.phoneNumber().trim();
      const phoneRegex = /^\+?[0-9\s\-]{10,15}$/;
      if (phone && !phoneRegex.test(phone)) {
        errors['phoneNumber'] = 'Please enter a valid phone number (10-15 digits, optional + prefix).';
      } else {
        delete errors['phoneNumber'];
      }
    }
    this.fieldErrors.set(errors);
  }

  validateAll(): boolean {
    const errors: Record<string, string> = {};
    const name = this.fullName().trim();
    if (!name) {
      errors['fullName'] = 'Full Name is required.';
    }

    const phone = this.phoneNumber().trim();
    const phoneRegex = /^\+?[0-9\s\-]{10,15}$/;
    if (phone && !phoneRegex.test(phone)) {
      errors['phoneNumber'] = 'Please enter a valid phone number (10-15 digits, optional + prefix).';
    }

    this.fieldErrors.set(errors);
    return Object.keys(errors).length === 0;
  }

  onReset(): void {
    const p = this.profile();
    this.fullName.set(p.fullName || '');
    this.phoneNumber.set(p.phoneNumber || '');
    this.specialization.set(p.specialization || '');
    this.department.set(p.department || '');
    this.designation.set(p.designation || '');
    this.avatarUrl.set(p.avatarUrl || '');
    this.timezone.set(p.timezone || 'Asia/Kolkata');
    this.dateFormat.set(p.dateFormat || 'DD/MM/YYYY');
    this.timeFormat.set(p.timeFormat || '24h');
    this.preferredLanguage.set(p.preferredLanguage || 'en');
    this.themePreference.set(p.themePreference || 'system');
    this.fieldErrors.set({});
    this.hasSubmitted.set(false);
  }

  onSubmit(): void {
    this.hasSubmitted.set(true);
    if (!this.validateAll()) return;

    this.save.emit({
      fullName: this.fullName().trim(),
      phoneNumber: this.phoneNumber().trim(),
      specialization: this.specialization().trim(),
      department: this.department().trim(),
      designation: this.designation().trim(),
      avatarUrl: this.avatarUrl().trim(),
      timezone: this.timezone(),
      dateFormat: this.dateFormat(),
      timeFormat: this.timeFormat(),
      preferredLanguage: this.preferredLanguage(),
      themePreference: this.themePreference(),
    });
  }
}
