import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type ModerationVerdict = 'APPROVED' | 'REQUIRES_REVALUATION' | 'ESCALATED';

@Component({
  selector: 'nag-evaluation-moderation-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluation-moderation-card.component.html',
  styleUrl: './evaluation-moderation-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationModerationCardComponent {
  readonly moderationRequired = input<boolean>(false);
  readonly moderationVerdict = input<ModerationVerdict>('APPROVED');
  readonly moderationNotes = input<string>('');

  readonly moderationRequiredChange = output<boolean>();
  readonly moderationVerdictChange = output<ModerationVerdict>();
  readonly moderationNotesChange = output<string>();

  onRequiredToggle(value: boolean): void {
    this.moderationRequiredChange.emit(value);
  }

  onVerdictSelect(value: ModerationVerdict): void {
    this.moderationVerdictChange.emit(value);
  }

  onNotesInput(value: string): void {
    this.moderationNotesChange.emit(value);
  }
}
