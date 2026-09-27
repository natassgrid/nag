import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  BlueprintTemplateResponse,
  BlueprintFeasibilityResponse,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-blueprint-sufficiency-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './blueprint-sufficiency-modal.component.html',
  styleUrl: './blueprint-sufficiency-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlueprintSufficiencyModalComponent {
  isOpen = input<boolean>(false);
  template = input<BlueprintTemplateResponse | null>(null);
  auditLoading = input<boolean>(false);
  auditResult = input<BlueprintFeasibilityResponse | null>(null);

  closeModal = output<void>();
  assemblePaper = output<BlueprintTemplateResponse>();

  onClose(): void {
    this.closeModal.emit();
  }

  onAssemble(): void {
    const tpl = this.template();
    if (tpl) {
      this.assemblePaper.emit(tpl);
    }
  }
}
