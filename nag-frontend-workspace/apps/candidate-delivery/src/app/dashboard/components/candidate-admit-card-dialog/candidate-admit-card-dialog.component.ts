import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { QrCodeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { DigitalAdmitCard } from '../../models';

@Component({
  selector: 'app-candidate-admit-card-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, QrCodeComponent],
  templateUrl: './candidate-admit-card-dialog.component.html',
  styleUrl: './candidate-admit-card-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateAdmitCardDialogComponent {
  readonly card = input.required<DigitalAdmitCard>();

  readonly closeDialog = output<void>();
  readonly printCard = output<void>();

  @HostListener('window:keydown.escape')
  handleEscape(): void {
    this.closeDialog.emit();
  }
}
