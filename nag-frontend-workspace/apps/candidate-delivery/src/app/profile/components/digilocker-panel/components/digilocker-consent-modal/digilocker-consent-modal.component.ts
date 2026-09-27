import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-digilocker-consent-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './digilocker-consent-modal.component.html',
  styleUrl: './digilocker-consent-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DigiLockerConsentModalComponent {
  readonly otpStep = input<boolean>(false);
  readonly otpValue = input<string>('948210');
  readonly isConnecting = input<boolean>(false);

  readonly otpValueChange = output<string>();
  readonly proceedToOtp = output<void>();
  readonly verifyAndSync = output<void>();
  readonly closeModal = output<void>();

  onOtpChange(value: string): void {
    this.otpValueChange.emit(value);
  }

  onProceed(): void {
    this.proceedToOtp.emit();
  }

  onVerify(): void {
    this.verifyAndSync.emit();
  }

  onClose(): void {
    this.closeModal.emit();
  }
}
