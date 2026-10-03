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

  constructor() {
    effect(
      () => {
        const p = this.profile();
        this.fullName.set(p.fullName || '');
        this.phoneNumber.set(p.phoneNumber || '');
        this.specialization.set(p.specialization || '');
        this.department.set(p.department || '');
      },
      { allowSignalWrites: true }
    );
  }

  onReset(): void {
    const p = this.profile();
    this.fullName.set(p.fullName || '');
    this.phoneNumber.set(p.phoneNumber || '');
    this.specialization.set(p.specialization || '');
    this.department.set(p.department || '');
  }

  onSubmit(): void {
    this.save.emit({
      fullName: this.fullName().trim(),
      phoneNumber: this.phoneNumber().trim(),
      specialization: this.specialization().trim(),
      department: this.department().trim(),
    });
  }
}
