import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'nag-exam-form-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './exam-form-rules.component.html',
  styleUrl: './exam-form-rules.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormRulesComponent {
  readonly duration = input<number>(180);
  readonly totalMarks = input<number>(300);
  readonly navPolicy = input<string>('FREE_NAVIGATION');
  readonly calcPolicy = input<string>('VIRTUAL_SCIENTIFIC');
  readonly negMarking = input<boolean>(true);
  readonly negMarkingValue = input<number>(1.0);
  readonly reviewFlag = input<boolean>(true);
  readonly isPractice = input<boolean>(false);

  readonly durationChange = output<number>();
  readonly totalMarksChange = output<number>();
  readonly navPolicyChange = output<string>();
  readonly calcPolicyChange = output<string>();
  readonly negMarkingChange = output<boolean>();
  readonly negMarkingValueChange = output<number>();
  readonly reviewFlagChange = output<boolean>();
  readonly isPracticeChange = output<boolean>();
}
