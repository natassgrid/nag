import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-kyc-identity-proof-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './kyc-identity-proof-card.component.html',
  styleUrl: './kyc-identity-proof-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KycIdentityProofCardComponent {
  readonly identityDocType = input<string | undefined>(undefined);
  readonly identityDocNumber = input<string | undefined>(undefined);
  readonly idProofUrl = input<string | undefined>(undefined);

  readonly uploadIdProof = output<void>();
  readonly removeIdProof = output<void>();

  onUpload(): void {
    this.uploadIdProof.emit();
  }

  onRemove(): void {
    this.removeIdProof.emit();
  }
}
