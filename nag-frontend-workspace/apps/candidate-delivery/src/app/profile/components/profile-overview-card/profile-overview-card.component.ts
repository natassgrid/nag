import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-profile-overview-card',
  standalone: true,
  imports: [CommonModule, MatIconModule, StatusBadgeComponent],
  templateUrl: './profile-overview-card.component.html',
  styleUrl: './profile-overview-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileOverviewCardComponent {
  readonly profile = input.required<CandidateProfile>();

  readonly initials = computed(() => {
    const name = this.profile()?.fullName || '';
    return name.slice(0, 2).toUpperCase() || 'NA';
  });

  readonly isKycVerified = computed(() => {
    return this.profile()?.kycStatus === 'VERIFIED';
  });
}
