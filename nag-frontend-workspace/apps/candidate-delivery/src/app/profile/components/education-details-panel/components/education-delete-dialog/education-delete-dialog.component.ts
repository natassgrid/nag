import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../../../models';

@Component({
  selector: 'nag-education-delete-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './education-delete-dialog.component.html',
  styleUrl: './education-delete-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationDeleteDialogComponent {
  readonly isOpen = input<boolean>(false);
  readonly entry = input<EducationEntry | null>(null);

  readonly confirm = output<void>();
  readonly cancel = output<void>();

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
