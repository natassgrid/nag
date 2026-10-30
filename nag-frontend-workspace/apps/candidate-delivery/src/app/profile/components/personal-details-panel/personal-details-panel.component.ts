import {
  ChangeDetectionStrategy,
  Component,
  computed,
  model,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { SUPPORTED_LANGUAGES } from '@nag-frontend-workspace/shared-util-i18n';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-personal-details-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './personal-details-panel.component.html',
  styleUrl: './personal-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonalDetailsPanelComponent {
  readonly profile = model.required<CandidateProfile>();
  readonly supportedLanguages = SUPPORTED_LANGUAGES;

  readonly touched = signal<Record<string, boolean>>({});

  markTouched(field: string): void {
    this.touched.update((t) => ({ ...t, [field]: true }));
  }

  readonly fullNameError = computed(() => {
    const name = (this.profile()?.fullName || '').trim();
    if (!this.touched()['fullName'] && !name) {
      return null;
    }
    if (!name) {
      return 'Full legal name is required.';
    }
    if (name.length < 3 || name.length > 100) {
      return 'Full legal name must be between 3 and 100 characters.';
    }
    return null;
  });

  readonly identityDocNumberError = computed(() => {
    const idNum = (this.profile()?.identityDocNumber || '').trim();
    if (!this.touched()['identityDocNumber'] && !idNum) {
      return null;
    }
    if (!idNum) {
      return 'Identity document number is required.';
    }
    return null;
  });
}
