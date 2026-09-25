import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'nag-paper-feasibility-modal',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './paper-feasibility-modal.component.html',
  styleUrl: './paper-feasibility-modal.component.scss',
})
export class PaperFeasibilityModalComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  result = input<any>(null);

  close = output<void>();

  onClose(): void {
    this.close.emit();
  }
}
