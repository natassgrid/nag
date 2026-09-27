import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  ExaminationResponse,
  ScheduleResponse,
  ShiftResponse,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-paper-target-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './paper-target-selector.component.html',
  styleUrl: './paper-target-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperTargetSelectorComponent {
  readonly exams = input<ExaminationResponse[]>([]);
  readonly schedules = input<ScheduleResponse[]>([]);
  readonly shifts = input<ShiftResponse[]>([]);

  readonly examId = input<string>('');
  readonly scheduleId = input<string>('');
  readonly shiftId = input<string>('');
  readonly paperName = input<string>('');
  readonly isPractice = input<boolean>(false);

  readonly examIdChange = output<string>();
  readonly scheduleIdChange = output<string>();
  readonly shiftIdChange = output<string>();
  readonly paperNameChange = output<string>();
  readonly isPracticeChange = output<boolean>();
}
