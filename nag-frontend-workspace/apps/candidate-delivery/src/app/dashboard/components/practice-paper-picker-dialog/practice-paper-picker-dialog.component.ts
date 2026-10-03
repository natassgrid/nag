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
import { PracticePaperSummary } from '../../models';

@Component({
  selector: 'app-practice-paper-picker-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './practice-paper-picker-dialog.component.html',
  styleUrl: './practice-paper-picker-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticePaperPickerDialogComponent {
  readonly examTitle = input<string>('Practice Assessment');
  readonly examId = input.required<string>();
  readonly papers = input.required<PracticePaperSummary[]>();

  readonly closeDialog = output<void>();
  readonly selectPaper = output<PracticePaperSummary>();

  @HostListener('window:keydown.escape')
  handleEscape(): void {
    this.closeDialog.emit();
  }
}
