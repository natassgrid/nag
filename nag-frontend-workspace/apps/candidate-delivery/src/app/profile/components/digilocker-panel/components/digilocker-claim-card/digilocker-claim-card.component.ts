import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { DigiLockerClaim } from '../../../../models';

@Component({
  selector: 'nag-digilocker-claim-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './digilocker-claim-card.component.html',
  styleUrl: './digilocker-claim-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DigiLockerClaimCardComponent {
  readonly claim = input.required<DigiLockerClaim>();
}
