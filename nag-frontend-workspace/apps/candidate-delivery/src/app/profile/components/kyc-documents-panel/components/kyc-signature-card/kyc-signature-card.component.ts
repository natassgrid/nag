import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-kyc-signature-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './kyc-signature-card.component.html',
  styleUrl: './kyc-signature-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KycSignatureCardComponent {
  readonly signatureUrl = input<string | undefined>(undefined);
  readonly signatureAssetId = input<string | undefined>(undefined);

  readonly uploadSignature = output<void>();
  readonly removeSignature = output<void>();

  onUpload(): void {
    this.uploadSignature.emit();
  }

  onRemove(): void {
    this.removeSignature.emit();
  }
}
