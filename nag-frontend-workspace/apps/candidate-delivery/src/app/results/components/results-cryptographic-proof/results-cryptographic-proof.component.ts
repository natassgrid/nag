import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { QrCodeComponent, NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { ScorecardRecord } from '../../models';

@Component({
  selector: 'nag-results-cryptographic-proof',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, QrCodeComponent],
  templateUrl: './results-cryptographic-proof.component.html',
  styleUrl: './results-cryptographic-proof.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultsCryptographicProofComponent {
  private readonly notification = inject(NotificationService);

  readonly scorecard = input.required<ScorecardRecord>();
  readonly pushingDigiLocker = input<boolean>(false);

  readonly pushDigiLocker = output<string>();
  readonly downloadPdf = output<void>();

  copiedHash = signal<boolean>(false);

  copyProofHash(hash: string): void {
    navigator.clipboard.writeText(hash).then(() => {
      this.copiedHash.set(true);
      this.notification.success('Copied to Clipboard', 'Merkle ledger commitment hash copied.');
      setTimeout(() => this.copiedHash.set(false), 2500);
    });
  }
}
